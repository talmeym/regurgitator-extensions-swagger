/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface XmlAware {
    Element toXml(Document document, Element parentElement);

}
