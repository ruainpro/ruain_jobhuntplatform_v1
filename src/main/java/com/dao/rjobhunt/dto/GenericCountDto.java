package com.dao.rjobhunt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for aggregation result mappings.
 * It captures a generic grouping _id (e.g. field name like "email", "platformId", etc.)
 * and the corresponding count.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericCountDto {

    private String id;   // MongoDB group _id
    private Long count;
}