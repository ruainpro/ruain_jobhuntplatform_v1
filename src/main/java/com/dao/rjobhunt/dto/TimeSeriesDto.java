package com.dao.rjobhunt.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSeriesDto {
    private LocalDate date;
    private long count;
}
