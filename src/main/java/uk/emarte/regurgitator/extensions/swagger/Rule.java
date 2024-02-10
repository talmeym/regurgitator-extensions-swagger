/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class Rule {
    @JsonProperty private final String step;
    @JsonProperty private final List<Condition> conditions;

    Rule(String step, List<Condition> conditions) {
        this.step = step;
        this.conditions = conditions;
    }
}
