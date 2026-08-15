package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** A registered passkey shown on the Security page. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredentialInfo {

    /** Base64url credential id (also the id used to remove the passkey). */
    private String id;

    private String nickname;
    private List<String> transports;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
