package com.jobjam.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        String requirements,
        String benefits,
        @Size(max = 2048) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String url,
        String details) {

    Job toEntity() {
        return new Job(title.strip(), description, requirements, benefits, blankToNull(url), details);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
