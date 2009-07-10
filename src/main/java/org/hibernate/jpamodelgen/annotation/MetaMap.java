package org.hibernate.jpamodelgen.annotation;

import javax.lang.model.element.Element;

public class MetaMap extends MetaCollection {

	private final String keyType;

	public MetaMap(MetaEntity parent, Element element, String collectionType,
			String keyType, String elementType) {
		super(parent, element, collectionType, elementType);
		this.keyType = keyType;		
	}
	
	public String getDeclarationString() {
		return "public static volatile " + parent.importType(getMetaType()) + "<" + parent.importType(parent.getQualifiedName()) + ", " + parent.importType(keyType) + ", " + parent.importType(getTypeDeclaration()) + "> " + getPropertyName() + ";";  
	}

}
