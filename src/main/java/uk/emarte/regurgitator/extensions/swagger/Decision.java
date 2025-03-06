/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RG;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

@JsonInclude(Include.NON_NULL)
class Decision implements Step {
    @JsonProperty String kind = "decision";
    @JsonProperty String id;
    @JsonProperty List<Step> steps;
    @JsonProperty List<Rule> rules;
    @JsonProperty("default-step") String defaultStep;

    Decision(String id, List<Step> steps, List<Rule> rules, String defaultStep) {
        this.id = id;
        this.steps = steps;
        this.rules = rules;
        this.defaultStep = defaultStep;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "id", id);

        Element stepsElement = document.createElement(RG + "steps");
        element.appendChild(stepsElement);

        for(Step step: this.steps) {
            stepsElement.appendChild(step.toXml(document, stepsElement));
        }

        Element rulesElement = document.createElement(RG + "rules");
        element.appendChild(rulesElement);
        addAttributeIfPresent(rulesElement, "default-step", defaultStep);

        for(Rule rule: rules) {
            rulesElement.appendChild(rule.toXml(document, rulesElement));
        }

        return element;
    }
}