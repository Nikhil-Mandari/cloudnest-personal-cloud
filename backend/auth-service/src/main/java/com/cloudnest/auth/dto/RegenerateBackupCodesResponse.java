package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Result of regenerating backup codes: the old unused codes are invalidated
 * and the new plaintext codes are returned exactly once.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegenerateBackupCodesResponse {

    private List<String> backupCodes;
}
