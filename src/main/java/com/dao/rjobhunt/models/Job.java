package com.dao.rjobhunt.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Document(collection = "jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Job", description = "Represents a scraped or stored job posting")
public class Job {

    @Id
    @JsonIgnore
    @Schema(hidden = true, description = "MongoDB internal job ID")
    private String jobId;

    @Schema(description = "Public UUID for external reference")
    private UUID publicId;

    @NotNull
    @Schema(description = "Job title, e.g., Software Engineer")
    private String title;

    @Schema(description = "Name of the company offering the job")
    private String company;

    @Schema(description = "Job location, e.g., Toronto, ON")
    private String location;

    @Schema(description = "Estimated or listed salary")
    private String salary;

    @Schema(description = "Type of job, e.g., Full-time, Contract")
    private String jobType;

    @Schema(description = "Public ID of the platform where this job was scraped")
    private UUID platformId;

    @Schema(description = "Full job description text")
    private String description;

    @Schema(description = "Date the job was posted")
    private Date postedDate;

    @Schema(description = "Direct URL to the original job posting")
    private String url;

    @Schema(description = "Remote status: Onsite, Remote, or Hybrid")
    private String remoteType;

    @Schema(description = "Industry category of the job")
    private String industry;

    @Schema(description = "Experience level: Entry, Mid, Senior")
    private String experienceLevel;

    @CreatedDate
    @Schema(description = "Timestamp when this job was scraped or saved")
    private Date scrapedDate;
}