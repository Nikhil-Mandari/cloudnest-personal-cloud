package com.cloudnest.file.service.impl;

import com.cloudnest.file.dto.AuditLogResponse;
import com.cloudnest.file.dto.PagedAuditLogsResponse;
import com.cloudnest.file.entity.AuditLog;
import com.cloudnest.file.entity.AuditLog.AuditAction;
import com.cloudnest.file.repository.AuditLogRepository;
import com.cloudnest.file.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

/**
 * Implementation of {@link AuditLogService}.
 * <p>
 * Best-effort by design: if writing an audit entry fails (e.g. a transient DB
 * error) the failure is logged and the audited operation still completes.
 */
@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void record(Long ownerId, AuditAction action, String resourceType,
                       String resourceId, String resourceName, String details) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .ownerId(ownerId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .details(details);

            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest request = servletAttributes.getRequest();
                builder.ipAddress(resolveClientIp(request));
                builder.userAgent(truncate(request.getHeader("User-Agent"), 512));
            }

            auditLogRepository.save(builder.build());
        } catch (Exception e) {
            // Auditing is best-effort — never propagate into the audited flow.
            log.error("Failed to record audit log (owner={}, action={}, resource={}): {}",
                    ownerId, action, resourceId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedAuditLogsResponse getLogs(Long ownerId, int page, int size, String action) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLog> result;
        if (action != null && !action.isBlank()) {
            AuditAction filter = parseAction(action);
            if (filter == null) {
                return PagedAuditLogsResponse.builder()
                        .content(java.util.List.of())
                        .page(safePage)
                        .size(safeSize)
                        .totalElements(0)
                        .totalPages(0)
                        .build();
            }
            result = auditLogRepository.findByOwnerIdAndAction(ownerId, filter, pageable);
        } else {
            result = auditLogRepository.findByOwnerId(ownerId, pageable);
        }

        return PagedAuditLogsResponse.builder()
                .content(result.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedAuditLogsResponse getAllLogs(int page, int size, String action, Long userId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLog> result;
        AuditAction filter = (action != null && !action.isBlank()) ? parseAction(action) : null;
        if (action != null && !action.isBlank() && filter == null) {
            // Unknown action filter — empty result rather than a broad leak.
            return PagedAuditLogsResponse.builder()
                    .content(java.util.List.of())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        if (filter != null && userId != null) {
            result = auditLogRepository.findByOwnerIdAndAction(userId, filter, pageable);
        } else if (filter != null) {
            result = auditLogRepository.findByAction(filter, pageable);
        } else if (userId != null) {
            result = auditLogRepository.findByOwnerId(userId, pageable);
        } else {
            result = auditLogRepository.findAll(pageable);
        }

        return PagedAuditLogsResponse.builder()
                .content(result.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .ownerId(log.getOwnerId())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .resourceName(log.getResourceName())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private AuditAction parseAction(String action) {
        try {
            return AuditAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Best-effort client IP resolution honouring common proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 45);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp, 45);
        }
        return truncate(request.getRemoteAddr(), 45);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
