package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtractProcessor {
    @JsonProperty private final String kind = "extract-processor";
    @JsonProperty private final String format;
    @JsonProperty private final int index;

    public ExtractProcessor(String format, int index) {
        this.format = format;
        this.index = index;
    }
}
