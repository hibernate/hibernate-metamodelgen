package org.hibernate.jpa.metamodel.ap;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

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
}
