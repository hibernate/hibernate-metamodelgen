package org.hibernate.jpa.metamodel.ap.annotation;

import org.hibernate.jpa.metamodel.ap.IMetaMember;

import java.beans.Introspector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public abstract class MetaMember implements IMetaMember {

	final protected Element element;
	final protected MetaEntity parent;
	final protected ProcessingEnvironment pe;
	private final String type;

	public MetaMember(MetaEntity parent, Element element, String type) {
		this.element = element;
		this.parent = parent;
		this.type = type;
		this.pe = parent.pe;
	}

	public String getDeclarationString() {
		return "public static " + parent.importType(getMetaType()) + "<" + parent.importType(parent.getQualifiedName()) + ", " + parent.importType(getTypeDeclaration()) + "> " + getPropertyName() + ";";  
	}

	public String getPropertyName() {
		if(element.getKind()==ElementKind.FIELD) {
			return element.getSimpleName().toString();
		} else if (element.getKind()==ElementKind.METHOD) {
			
			String name = element.getSimpleName().toString();
			if(name.startsWith("get")) {
				return pe.getElementUtils().getName(Introspector.decapitalize(name.substring("get".length()))).toString();
			} else if(name.startsWith("is")) {
				return (pe.getElementUtils().getName(Introspector.decapitalize(name.substring("is".length())))).toString();
			}
			return pe.getElementUtils().getName(Introspector.decapitalize(name)).toString();
		} else {
			return pe.getElementUtils().getName(element.getSimpleName() + "/* " + element.getKind() + " */").toString();
		}
	}

	abstract public String getMetaType();

	public String getTypeDeclaration() {
		return type;		
	}
}
