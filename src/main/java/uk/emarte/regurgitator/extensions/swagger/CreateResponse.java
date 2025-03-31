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
class CreateResponse implements Step {
    @JsonProperty private final String kind = "create-response";
    @JsonProperty private final String id;
    @JsonProperty private final String value;
    @JsonProperty private final String file;

    CreateResponse(String id, String value, String file) {
        this.id = id;
        this.value = value;
        this.file = file;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "id", id);
        addAttributeIfPresent(element, "value", value);
        addAttributeIfPresent(element, "file", file);
        return element;
    }
}
