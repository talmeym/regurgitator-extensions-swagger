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

class ExtractProcessor implements XmlAware {
    @JsonProperty private final String kind = "extract-processor";
    @JsonProperty private final String format;
    @JsonProperty private final int index;

    ExtractProcessor(String format, int index) {
        this.format = format;
        this.index = index;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        addAttributeIfPresent(element, "format", format);
        addAttributeIfPresent(element, "index", "" + index);
        return element;
    }
}
