package com.dao.rjobhunt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceImpactDto {
    private String platformName;       // Name of the platform (e.g., LinkedIn)
    private int preferenceWeight;      // Preference weight
    private long scrapeCount;          // Scrape count based on that weight
}