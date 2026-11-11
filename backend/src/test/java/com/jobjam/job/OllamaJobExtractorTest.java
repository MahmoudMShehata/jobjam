package com.jobjam.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OllamaJobExtractorTest {

    @Test
    void schemaAsksForProblemLast() {
        assertThat(OllamaJobExtractor.EXTRACTION_SCHEMA.get("properties").propertyNames())
                .containsExactly("title", "description", "requirements", "benefits", "details", "problem");
    }
}
