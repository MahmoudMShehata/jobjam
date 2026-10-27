package com.jobjam.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        String requirements,
        String benefits) {

    Job toEntity() {
        return new Job(title.strip(), description, requirements, benefits);
    }
}
