package com.dao.rjobhunt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatformUsageDto {
    private String platformName; // e.g. "LinkedIn"
    private long scrapeCount;    // e.g. 125
}