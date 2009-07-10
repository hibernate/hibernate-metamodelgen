package org.hibernate.jpa.metamodel.ap.annotation;

import org.hibernate.jpa.metamodel.ap.IMetaCollection;

import javax.lang.model.element.Element;


public class MetaCollection extends MetaAttribute implements IMetaCollection {

	private String collectionType; 
	

	public MetaCollection(MetaEntity parent, Element element, String collectionType, String elementType) {
		super(parent, element, elementType);
		this.collectionType = collectionType;		
	}

	@Override
	public String getMetaType() {		
		return collectionType;
	}

	
}
