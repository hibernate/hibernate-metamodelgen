package org.hibernate.jpa.metamodel.ap;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;

public class TypeUtils {

	private static final Map<String, String> PRIMITIVES = new HashMap<String, String>();
	static {
		PRIMITIVES.put( "char", "Character" );

		PRIMITIVES.put( "byte", "Byte" );
		PRIMITIVES.put( "short", "Short" );
		PRIMITIVES.put( "int", "Integer" );
		PRIMITIVES.put( "long", "Long" );

		PRIMITIVES.put( "boolean", "Boolean" );

		PRIMITIVES.put( "float", "Float" );
		PRIMITIVES.put( "double", "Double" );

	}
	
	static public String toTypeString(TypeMirror type) {
		if(type.getKind().isPrimitive()) {
			return PRIMITIVES.get(type.toString());
		}
	
		return type.toString();
	}

	static public TypeElement getSuperclass(TypeElement element) {
		final TypeMirror superClass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		String superclassDeclaration = "";
		if (superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( ( DeclaredType ) superClass ).asElement();
			return ( TypeElement ) superClassElement;
		}
		else {
			return null;
		}
	}
}
