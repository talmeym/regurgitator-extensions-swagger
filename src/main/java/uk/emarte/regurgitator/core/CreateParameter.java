package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class CreateParameter implements Step {
    @JsonProperty private final String kind = "create-parameter";
    @JsonProperty private final String name;
    @JsonProperty private final String source;
    @JsonProperty private final ExtractProcessor processor;

    public CreateParameter(String name, String source, ExtractProcessor processor) {
        this.name = name;
        this.source = source;
        this.processor = processor;
    }
}
