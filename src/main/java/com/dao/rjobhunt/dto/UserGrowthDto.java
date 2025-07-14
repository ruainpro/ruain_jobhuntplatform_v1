package com.dao.rjobhunt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthDto {
    private String date;     // now maps to "date", not "_id"
    private long count;
}