/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
class Condition {
    @JsonProperty private final String source;
    @JsonProperty private final String equals;
    @JsonProperty private final String matches;

    Condition(String source, String equals, String matches) {
        this.source = source;
        this.equals = equals;
        this.matches = matches;
    }
}
