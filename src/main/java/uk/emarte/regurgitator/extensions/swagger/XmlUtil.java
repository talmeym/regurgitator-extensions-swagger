/*
 * Copyright (C) 2017 Miles Talmey.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package uk.emarte.regurgitator.extensions.swagger;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class XmlUtil {
    static final String RG = "rg:";
    static final String RGW = "rgw:";
    static final String REGURG_CORE_URL = "http://core.regurgitator.emarte.uk";
    static final String REGURG_EXT_WEB_URL = "http://web.extensions.regurgitator.emarte.uk";
    static final String XMLNS_URL = "http://www.w3.org/2000/xmlns/";
    static final String XML_SCHEMA_URL = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String STRING = "string", INTEGER = "integer", DOUBLE = "double", FLOAT = "float", NUMBER = "number", OBJECT = "object", BOOLEAN = "boolean", INT_32 = "int32", INT_64 = "int64", ARRAY = "array";
    private static final String FOOBAR = "foobar", ZERO = "0";

    static void addAttributeIfPresent(Element element, String name, String value) {
        if(value != null) {
            element.setAttribute(name, value);
        }
    }

    static void saveToXml(RegurgitatorConfiguration configuration, FileOutputStream fileOutputStream) throws ParserConfigurationException, TransformerException {
        Document document = newDocument();
        saveToXml(configuration.toXml(document, null), document, fileOutputStream);
    }

    static Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    static String saveToXml(Element element, Document document, FileOutputStream fileOutputStream) throws TransformerException {
        document.appendChild(element);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource domSource = new DOMSource(document);
        transformer.transform(domSource, new StreamResult(fileOutputStream));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        transformer.transform(domSource, new StreamResult(byteArrayOutputStream));
        return byteArrayOutputStream.toString();
    }

    @SuppressWarnings("rawtypes")
    static Element buildXmlObject(String schemaName, Schema<?> schema, Document document, Map<String, Schema> componentSchemas) {
        if (schema.get$ref() != null) {
            String $ref = schema.get$ref();
            $ref = $ref.contains("/") ? $ref.substring($ref.lastIndexOf("/") + 1) : $ref;
            schema = componentSchemas.get($ref);
        }

        XML xml = schema.getXml();
        String elementName = xml != null ? xml.getName() : schema.getName() != null ? schema.getName() : schemaName;
        String prefix = xml != null ? xml.getPrefix() : null;
        String namespace = xml != null ? xml.getNamespace() : null;

        if (schema.getProperties() != null || schema.getAdditionalProperties() != null) {
            if (elementName == null) {
                throw new IllegalStateException("no element name defined");
            }

            Element element = document.createElement(prefix != null && prefix.length() > 0 ? prefix + ":" + elementName : elementName);

            if (prefix != null && namespace != null) {
                element.setAttributeNS(XMLNS_URL, "xmlns:" + prefix, namespace);
            }

            Map<String, Schema> properties = schema.getProperties() != null ? schema.getProperties() : ((ObjectSchema) schema.getAdditionalProperties()).getProperties();

            for (String name : properties.keySet()) {
                Schema<?> propertySchema = properties.get(name);
                String type = propertySchema.getType();
                XML propertyXml = propertySchema.getXml();
                String xmlName = propertyXml != null && propertyXml.getName() != null ? propertyXml.getName() : name;

                if (ARRAY.equals(type)) {
                    boolean wrapped = propertyXml != null && propertyXml.getWrapped() != null && propertyXml.getWrapped();
                    Element elementToUse = element;

                    if (wrapped) {
                        Element child = document.createElement(xmlName);
                        element.appendChild(child);
                        elementToUse = child;
                    }

                    io.swagger.v3.oas.models.media.XML itemsXml = propertySchema.getItems().getXml();
                    String itemName = itemsXml != null && itemsXml.getName() != null ? itemsXml.getName() : name;
                    elementToUse.appendChild(buildXmlObject(itemName, propertySchema.getItems(), document, componentSchemas));
                } else if (INTEGER.equals(type)) {
                    String value;

                    switch(propertySchema.getFormat()) {
                        case INT_64: value = "" + Long.parseLong(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO); break;
                        case INT_32:
                        default: value = "" + Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO);
                    }

                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if (NUMBER.equals(type)) {
                    String value;

                    switch(propertySchema.getFormat()) {
                        case FLOAT: value = "" + Float.parseFloat(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO); break;
                        case DOUBLE: value = "" + Double.parseDouble(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO); break;
                        default: value = "" + Integer.parseInt(propertySchema.getExample() != null ? "" + propertySchema.getExample() : ZERO);
                    }

                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if (OBJECT.equals(type)) {
                    element.appendChild(buildXmlObject(xmlName, propertySchema, document, componentSchemas));
                } else if (BOOLEAN.equals(type)) {
                    String value = "" + Boolean.parseBoolean("" + (propertySchema.getExample() != null ? propertySchema.getExample() : true));
                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                } else if (propertySchema.get$ref() != null) {
                    element.appendChild(buildXmlObject(xmlName, propertySchema, document, componentSchemas));
                } else { // assume string
                    String value = "" + (propertySchema.getExample() != null ? propertySchema.getExample() : FOOBAR);
                    xmlChildElementOrAttribute(xmlName, value, propertyXml, document, element);
                }
            }

            return element;
        } else if (STRING.equals(schema.getType()) || INTEGER.equals(schema.getType())) {
            Element element = document.createElement(prefix != null && prefix.length() > 0 ? prefix + ":" + elementName : elementName);
            element.appendChild(document.createTextNode("" + (schema.getExample() != null ? schema.getExample() : (INTEGER.equals(schema.getType()) ? ZERO : FOOBAR))));
            return element;
        }

        throw new IllegalStateException("Cannot construct object for schema");
    }

    private static void xmlChildElementOrAttribute(String name, String value, XML propertyXml, Document document, Element element) {
        if (propertyXml != null && propertyXml.getAttribute()) {
            element.setAttribute(name, value);
        } else {
            Element child = document.createElement(name);
            element.appendChild(child);
            child.appendChild(document.createTextNode(value));
        }
    }
}
