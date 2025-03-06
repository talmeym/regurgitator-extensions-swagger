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

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RG;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

@JsonInclude(Include.NON_NULL)
class CreateParameter implements Step {
    @JsonProperty private final String kind = "create-parameter";
    @JsonProperty private final String name;
    @JsonProperty private final String source;
    @JsonProperty private final String value;
    @JsonProperty private final ValueProcessor processor;
    @JsonProperty private final Boolean optional;

    CreateParameter(String name, String source, String value, ValueProcessor processor, Boolean optional) {
        this.name = name;
        this.source = source;
        this.value = value;
        this.processor = processor;
        this.optional = optional != null && optional ? optional : null;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "name", name);
        addAttributeIfPresent(element, "source", source);
        addAttributeIfPresent(element, "value", value);
        addAttributeIfPresent(element, "optional", nullableToString(optional));

        if(processor != null) {
            element.appendChild(processor.toXml(document, element));
        }

        return element;
    }

    private String nullableToString(Boolean bool) {
        return bool != null ? String.valueOf(bool) : null;
    }

    public String getName() {
        return name;
    }
}
