/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class RegurgitatorConfiguration {
    @JsonProperty private final String kind = "regurgitator-configuration";
    @JsonProperty private final List<Step> steps;

    RegurgitatorConfiguration(List<Step> steps) {
        this.steps = steps;
    }
}
