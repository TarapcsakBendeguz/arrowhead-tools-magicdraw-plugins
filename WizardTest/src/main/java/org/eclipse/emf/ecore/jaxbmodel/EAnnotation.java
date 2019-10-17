//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.08 at 01:05:25 PM CEST 
//

package org.eclipse.emf.ecore.jaxbmodel;

import com.incquerylabs.arrowhead.tools.magic.Wizard;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EAnnotation", propOrder = {"details", "contents"})
public class EAnnotation extends EModelElement {

    protected List<EStringToStringMapEntry> details;
    protected List<Object> contents;
    @XmlAttribute(name = "source")
    protected String source;
    @XmlAttribute(name = "references")
    protected List<String> references;

    public void subCompartmentalize(Path parent, Element topParent, Path topPath) throws IOException {
        String name = "Annotation" + Wizard.annotationSuffix++;
        String filename = Wizard.sanitizeFilename(name);
        Path xml = parent.resolve(filename + ".xml");
        if(Files.exists(xml)){
            filename = Wizard.degenarilzeName(parent, name);
            xml = parent.resolve(filename + ".xml");
        }
        Path dir = parent.resolve(filename);
        Files.createDirectory(dir);
        Files.createFile(xml);

        Element ref = topParent.addElement(Wizard.REF);
        ref.addAttribute(Wizard.HREF, topPath.relativize(xml).toString());

        Document doc = DocumentHelper.createDocument();
        Element me = doc.addElement("eAnnotations");
        me.addAttribute("source", source);
        Wizard.addListAttribute(me, "references", references);

        if (eAnnotations != null) {
            for (EAnnotation an : eAnnotations) {
                an.subCompartmentalize(dir, me, xml);
            }
        }
        if (details != null) {
            for (EStringToStringMapEntry ss : details) {
                ss.subCompartmentalize(dir, me, xml);
            }
        }
        if (contents != null) {
            for (Object o : contents) {
                Wizard.subCompartmentalize(o, dir, me, xml);
            }
        }

        Wizard.writeDocument(xml, doc);
    }

    public List<EStringToStringMapEntry> getDetails() {
        if (details == null) {
            details = new ArrayList<EStringToStringMapEntry>();
        }
        return this.details;
    }

    public List<Object> getContents() {
        if (contents == null) {
            contents = new ArrayList<Object>();
        }
        return this.contents;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String value) {
        this.source = value;
    }

    public List<String> getReferences() {
        if (references == null) {
            references = new ArrayList<String>();
        }
        return this.references;
    }
}