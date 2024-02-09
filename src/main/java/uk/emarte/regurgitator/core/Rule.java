package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Rule {
    @JsonProperty private final String step;
    @JsonProperty private final List<Condition> conditions;

    public Rule(String step, List<Condition> conditions) {
        this.step = step;
        this.conditions = conditions;
    }
}
