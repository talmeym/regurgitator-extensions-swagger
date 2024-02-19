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
    @JsonProperty private final ExtractProcessor processor;

    CreateParameter(String name, String source, ExtractProcessor processor) {
        this.name = name;
        this.source = source;
        this.processor = processor;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "name", name);
        addAttributeIfPresent(element, "source", source);

        if(processor != null) {
            element.appendChild(processor.toXml(document, element));
        }

        return element;
    }
}
