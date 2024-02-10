/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

class ExtractProcessor {
    @JsonProperty private final String kind = "extract-processor";
    @JsonProperty private final String format;
    @JsonProperty private final int index;

    ExtractProcessor(String format, int index) {
        this.format = format;
        this.index = index;
    }
}
