package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Decision implements Step {
    @JsonProperty String kind = "decision";
    @JsonProperty String id;
    @JsonProperty List<Step> steps;
    @JsonProperty List<Rule> rules;
    @JsonProperty("default-step") Object defaultStep;

    public Decision(String id, List<Step> steps, List<Rule> rules, Object defaultStep) {
        this.id = id;
        this.steps = steps;
        this.rules = rules;
        this.defaultStep = defaultStep;
    }
}