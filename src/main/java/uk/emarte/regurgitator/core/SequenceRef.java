package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SequenceRef implements Step {
    @JsonProperty private final String kind = "sequence-ref";
    @JsonProperty private String id;
    @JsonProperty private String file;

    public SequenceRef(String id, String file) {
        this.id = id;
        this.file = file;
    }
}
