package uk.emarte.regurgitator.extensions.swagger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;

public class XmlUtil {
    static String RG = "rg:";
    static String RGW = "rgw:";
    static String REGURG_CORE_NS = "http://core.regurgitator.emarte.uk";
    static String REGURG_EXT_WEB_NS = "http://web.extensions.regurgitator.emarte.uk";

    static void addAttributeIfPresent(Element element, String name, String value) {
        if(value != null) {
            element.setAttribute(name, value);
        }
    }

    static void saveToXml(RegurgitatorConfiguration configuration, FileOutputStream fileOutputStream) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().newDocument();
        document.appendChild(configuration.toXml(document, null));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(fileOutputStream));
    }
}
