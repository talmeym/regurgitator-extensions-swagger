package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class Condition {
    @JsonProperty private final String source;
    @JsonProperty private final String equals;
    @JsonProperty private final String matches;

    public Condition(String source, String equals, String matches) {
        this.source = source;
        this.equals = equals;
        this.matches = matches;
    }
}
