/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
class CreateParameter implements Step {
    @JsonProperty private final String kind = "create-parameter";
    @JsonProperty private final String name;
    @JsonProperty private final String source;
    @JsonProperty private final ExtractProcessor processor;

    CreateParameter(String name, String source, ExtractProcessor processor) {
        this.name = name;
        this.source = source;
        this.processor = processor;
    }
}
