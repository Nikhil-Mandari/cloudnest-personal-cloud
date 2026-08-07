package com.cloudnest.file.service;

import com.cloudnest.file.dto.AdminStorageOverviewResponse;
import com.cloudnest.file.dto.StorageOverviewResponse;

/**
 * Service for the storage analytics dashboard.
 */
public interface StorageAnalyticsService {

    /**
     * Computes the full storage overview for a user: totals, trash, largest
     * files, file-type breakdown and weekly / monthly usage.
     *
     * @param ownerId the authenticated user's ID
     * @return the computed overview
     */
    StorageOverviewResponse getOverview(Long ownerId);

    /**
     * Computes the platform-wide storage overview for the admin dashboard:
     * totals across all users, largest files, file-type breakdown and usage
     * over time.
     *
     * @return the computed admin overview
     */
    AdminStorageOverviewResponse getAdminOverview();
}
