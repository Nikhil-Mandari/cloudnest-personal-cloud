package com.cloudnest.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Paged view of user profiles (admin users view).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedUsersResponse {

    private List<UserProfileResponse> content;

    /** Zero-based current page. */
    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}
