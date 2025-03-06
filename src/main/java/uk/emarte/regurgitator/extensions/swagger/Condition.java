/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.RG;
import static uk.emarte.regurgitator.extensions.swagger.XmlUtil.addAttributeIfPresent;

@JsonInclude(NON_NULL)
class Condition implements XmlAware {
    @JsonProperty private final String source;
    @JsonProperty private final String equals;
    @JsonProperty private final String matches;
    @JsonProperty private final String exists;

    Condition(String source, String equals, String matches, String exists) {
        this.source = source;
        this.equals = equals;
        this.matches = matches;
        this.exists = exists;
    }

    @Override
    public Element toXml(Document document, Element parentElement) {
        Element element = document.createElement(RG + "condition");
        addAttributeIfPresent(element, "source", source);
        addAttributeIfPresent(element, "equals", equals);
        addAttributeIfPresent(element, "matches", matches);
        addAttributeIfPresent(element, "exists", exists);
        return element;
    }
}
