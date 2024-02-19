package uk.emarte.regurgitator.extensions.swagger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface XmlAware {
    Element toXml(Document document, Element parentElement);

}
