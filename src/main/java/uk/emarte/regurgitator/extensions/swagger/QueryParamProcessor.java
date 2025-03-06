/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RGW;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

class QueryParamProcessor implements ValueProcessor {
    @JsonProperty private final String kind = "query-param-processor";
    @JsonProperty private final String key;

    QueryParamProcessor(String key) {
        this.key = key;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RGW + kind);
        addAttributeIfPresent(element, "key", key);
        return element;
    }
}
