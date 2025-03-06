/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RG;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

class Sequence implements Step {
    @JsonProperty private final String kind = "sequence";
    @JsonProperty private final String id;
    @JsonProperty private final List<Step> steps;

    Sequence(String id, List<Step> steps) {
        this.id = id;
        this.steps = steps;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "id", id);

        for(Step step: steps) {
            element.appendChild(step.toXml(document, element));
        }

        return element;
    }
}
