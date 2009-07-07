package org.hibernate.jpa.metamodel.ap.annotation;

import org.hibernate.jpa.metamodel.ap.IMetaAttribute;

import javax.lang.model.element.Element;

public class MetaAttribute extends MetaMember implements IMetaAttribute {

	public MetaAttribute(MetaEntity parent, Element element, String type) {
		super(parent, element, type);
	}

	@Override
	public String getMetaType() {
		return "javax.persistence.metamodel.Attribute";
	}

}
