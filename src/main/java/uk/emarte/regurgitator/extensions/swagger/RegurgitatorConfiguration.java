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
        element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rg", REGURG_CORE_NS);
        element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rgw", REGURG_EXT_WEB_NS);
        element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        element.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", REGURG_CORE_NS + " regurgitatorCore.xsd " + REGURG_EXT_WEB_NS + " regurgitatorExtensionsWeb.xsd");

        for(Step step: steps) {
            element.appendChild(step.toXml(document, element));
        }

        return element;
    }
}
