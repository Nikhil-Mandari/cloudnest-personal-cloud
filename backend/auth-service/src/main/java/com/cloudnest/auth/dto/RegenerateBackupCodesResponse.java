package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Fresh backup codes returned after regeneration. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegenerateBackupCodesResponse {

    private List<String> backupCodes;
}
