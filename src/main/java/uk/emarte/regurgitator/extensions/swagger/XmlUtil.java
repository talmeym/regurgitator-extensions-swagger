/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;

public class XmlUtil {
    static final String RG = "rg:";
    static final String RGW = "rgw:";
    static final String REGURG_CORE_URL = "http://core.regurgitator.emarte.uk";
    static final String REGURG_EXT_WEB_URL = "http://web.extensions.regurgitator.emarte.uk";
    static final String XMLNS_URL = "http://www.w3.org/2000/xmlns/";
    static final String XML_SCHEMA_URL = "http://www.w3.org/2001/XMLSchema-instance";

    static void addAttributeIfPresent(Element element, String name, String value) {
        if(value != null) {
            element.setAttribute(name, value);
        }
    }

    static void saveToXml(RegurgitatorConfiguration configuration, FileOutputStream fileOutputStream) throws ParserConfigurationException, TransformerException {
        Document document = newDocument();
        saveElement(configuration.toXml(document, null), document, fileOutputStream);
    }

    static Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    static void saveElement(Element element, Document document, FileOutputStream fileOutputStream) throws TransformerException {
        document.appendChild(element);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(fileOutputStream));
    }
}
