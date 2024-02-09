package uk.emarte.regurgitator.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RegurgitatorConfiguration {
    @JsonProperty private final String kind = "regurgitator-configuration";
    @JsonProperty private final List<Step> steps;

    public RegurgitatorConfiguration(List<Step> steps) {
        this.steps = steps;
    }
}
