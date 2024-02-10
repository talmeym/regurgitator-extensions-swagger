/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class Decision implements Step {
    @JsonProperty String kind = "decision";
    @JsonProperty String id;
    @JsonProperty List<Step> steps;
    @JsonProperty List<Rule> rules;
    @JsonProperty("default-step") Object defaultStep;

    Decision(String id, List<Step> steps, List<Rule> rules, Object defaultStep) {
        this.id = id;
        this.steps = steps;
        this.rules = rules;
        this.defaultStep = defaultStep;
    }
}