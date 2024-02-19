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

class Rule implements XmlAware {
    @JsonProperty private final String step;
    @JsonProperty private final List<Condition> conditions;

    Rule(String step, List<Condition> conditions) {
        this.step = step;
        this.conditions = conditions;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + "rule");
        addAttributeIfPresent(element, "step", step);

        for(Condition condition: conditions) {
            element.appendChild(condition.toXml(document, element));
        }

        return element;
    }
}
