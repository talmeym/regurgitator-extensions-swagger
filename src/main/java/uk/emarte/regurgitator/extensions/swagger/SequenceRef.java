/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RG;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

class SequenceRef implements Step {
    @JsonProperty private final String kind = "sequence-ref";
    @JsonProperty private String id;
    @JsonProperty private String file;

    SequenceRef(String id, String file) {
        this.id = id;
        this.file = file;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "id", id);
        addAttributeIfPresent(element, "file", file);
        return element;
    }
}
