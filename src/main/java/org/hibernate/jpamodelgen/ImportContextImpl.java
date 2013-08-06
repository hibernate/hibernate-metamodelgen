/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.jpamodelgen.model.ImportContext;


/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class ImportContextImpl implements ImportContext {

	private Set<String> imports = new TreeSet<String>();
	private Set<String> staticImports = new TreeSet<String>();
	private Map<String, String> simpleNames = new HashMap<String, String>();

	private String basePackage = "";

	private static String LINE_SEPARATOR = System.getProperty( "line.separator" );
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

	public ImportContextImpl(String basePackage) {
		this.basePackage = basePackage;
	}

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * <p/>
	 * e.g.
	 * java.util.Collection<org.marvel.Hulk> imports java.util.Collection and returns Collection
	 * org.marvel.Hulk[] imports org.marvel.Hulk and returns Hulk
	 *
	 * @param fqcn Fully qualified class name
	 *
	 * @return import string
	 */
	public String importType(String fqcn) {
		String result = fqcn;

		//if(fqcn==null) return "/** (null) **/"; 

		String additionalTypePart = null;
		if ( fqcn.indexOf( '<' ) >= 0 ) {
			additionalTypePart = result.substring( fqcn.indexOf( '<' ) );
			result = result.substring( 0, fqcn.indexOf( '<' ) );
			fqcn = result;
		}
		else if ( fqcn.indexOf( '[' ) >= 0 ) {
			additionalTypePart = result.substring( fqcn.indexOf( '[' ) );
			result = result.substring( 0, fqcn.indexOf( '[' ) );
			fqcn = result;
		}

		String pureFqcn = fqcn.replace( '$', '.' );

		boolean canBeSimple;

		String simpleName = unqualify( fqcn );
		if ( simpleNames.containsKey( simpleName ) ) {
			String existingFqcn = simpleNames.get( simpleName );
			if ( existingFqcn.equals( pureFqcn ) ) {
				canBeSimple = true;
			}
			else {
				canBeSimple = false;
			}
		}
		else {
			canBeSimple = true;
			simpleNames.put( simpleName, pureFqcn );
			imports.add( pureFqcn );
		}


		if ( inSamePackage( fqcn ) || ( imports.contains( pureFqcn ) && canBeSimple ) ) {
			result = unqualify( result ); // de-qualify
		}
		else if ( inJavaLang( fqcn ) ) {
			result = result.substring( "java.lang.".length() );
		}

		if ( additionalTypePart != null ) {
			result = result + additionalTypePart;
		}

		result = result.replace( '$', '.' );
		return result;
	}

	public String staticImport(String fqcn, String member) {
		String local = fqcn + "." + member;
		imports.add( local );
		staticImports.add( local );

		if ( member.equals( "*" ) ) {
			return "";
		}
		else {
			return member;
		}
	}

	private boolean inDefaultPackage(String className) {
		return className.indexOf( "." ) < 0;
	}

	private boolean isPrimitive(String className) {
		return PRIMITIVES.containsKey( className );
	}

	private boolean inSamePackage(String className) {
		String other = qualifier( className );
		return other == basePackage
				|| ( other != null && other.equals( basePackage ) );
	}

	private boolean inJavaLang(String className) {
		return "java.lang".equals( qualifier( className ) );
	}

	public String generateImports() {
		StringBuilder builder = new StringBuilder();

		for ( String next : imports ) {
			// don't add automatically "imported" stuff
			if ( !isAutoImported( next ) ) {
				if ( staticImports.contains( next ) ) {
					builder.append( "import static " ).append( next ).append( ";" ).append( LINE_SEPARATOR );
				}
				else {
					builder.append( "import " ).append( next ).append( ";" ).append( LINE_SEPARATOR );
				}
			}
		}

		if ( builder.indexOf( "$" ) >= 0 ) {
			return builder.toString();
		}
		return builder.toString();
	}

	private boolean isAutoImported(String next) {
		return isPrimitive( next ) || inDefaultPackage( next ) || inJavaLang( next ) || inSamePackage( next );
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( qualifiedName.lastIndexOf( '.' ) + 1 );
	}

	public static String qualifier(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( "." );
		return ( loc < 0 ) ? "" : qualifiedName.substring( 0, loc );
	}
}
