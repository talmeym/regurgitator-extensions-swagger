/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.*;

class RegurgitatorConfiguration implements XmlAware {
    @JsonProperty private final String kind = "regurgitator-configuration";
    @JsonProperty private final List<Step> steps;

    RegurgitatorConfiguration(List<Step> steps) {
        this.steps = steps;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + kind);
        element.setAttributeNS(XMLNS_URL, "xmlns:rg", REGURG_CORE_URL);
        element.setAttributeNS(XMLNS_URL, "xmlns:rge", REGURG_EXT_URL);
        element.setAttributeNS(XMLNS_URL, "xmlns:rgw", REGURG_EXT_WEB_URL);
        element.setAttributeNS(XMLNS_URL, "xmlns:xsi", XML_SCHEMA_URL);
        element.setAttributeNS(XML_SCHEMA_URL, "xsi:schemaLocation", REGURG_CORE_URL + " regurgitatorCore.xsd " + REGURG_EXT_URL + " regurgitatorExtensions.xsd " + REGURG_EXT_WEB_URL + " regurgitatorExtensionsWeb.xsd");

        for(Step step: steps) {
            element.appendChild(step.toXml(document, element));
        }

        return element;
    }
}
