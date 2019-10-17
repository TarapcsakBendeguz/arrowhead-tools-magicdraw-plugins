//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.08.08 at 01:05:25 PM CEST 
//

package org.eclipse.emf.ecore.jaxbmodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EClassifier", propOrder = {"eTypeParameters"})
@XmlSeeAlso({EDataType.class, EClass.class})
public abstract class EClassifier extends ENamedElement {

    protected List<ETypeParameter> eTypeParameters;
    @XmlAttribute(name = "instanceClassName")
    protected String instanceClassName;
    @XmlAttribute(name = "instanceClass")
    protected String instanceClass;
    @XmlAttribute(name = "defaultValue")
    protected String defaultValue;
    @XmlAttribute(name = "instanceTypeName")
    protected String instanceTypeName;

    public List<ETypeParameter> getETypeParameters() {
        if (eTypeParameters == null) {
            eTypeParameters = new ArrayList<ETypeParameter>();
        }
        return this.eTypeParameters;
    }

    public String getInstanceClassName() {
        return instanceClassName;
    }

    public void setInstanceClassName(String value) {
        this.instanceClassName = value;
    }

    public String getInstanceClass() {
        return instanceClass;
    }

    public void setInstanceClass(String value) {
        this.instanceClass = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
    }

    public String getInstanceTypeName() {
        return instanceTypeName;
    }

    public void setInstanceTypeName(String value) {
        this.instanceTypeName = value;
    }
}