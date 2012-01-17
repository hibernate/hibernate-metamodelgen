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


package org.hibernate.jpamodelgen.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;

import org.hibernate.jpamodelgen.AccessTypeInformation;
import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.MetaModelGenerationException;

/**
 * Utility class.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class TypeUtils {

	public static final String DEFAULT_ANNOTATION_PARAMETER_NAME = "value";
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

	private TypeUtils() {
	}

	public static String toTypeString(TypeMirror type) {
		if ( type.getKind().isPrimitive() ) {
			return PRIMITIVES.get( type.toString() );
		}
		return type.toString();
	}

	public static TypeElement getSuperclassTypeElement(TypeElement element) {
		final TypeMirror superClass = element.getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		if ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( (DeclaredType) superClass ).asElement();
			return (TypeElement) superClassElement;
		}
		else {
			return null;
		}
	}

	public static String extractClosestRealTypeAsString(TypeMirror type, Context context) {
		if ( type instanceof TypeVariable ) {
			final TypeMirror compositeUpperBound = ( (TypeVariable) type ).getUpperBound();
			return extractClosestRealTypeAsString( compositeUpperBound, context );
		}
		else {
			return context.getTypeUtils().erasure( type ).toString();
		}
	}

	public static boolean containsAnnotation(Element element, Class<?>... annotations) {
		assert element != null;
		assert annotations != null;

		List<String> annotationClassNames = new ArrayList<String>();
		for ( Class<?> clazz : annotations ) {
			annotationClassNames.add( clazz.getName() );
		}

		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		for ( AnnotationMirror mirror : annotationMirrors ) {
			if ( annotationClassNames.contains( mirror.getAnnotationType().toString() ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 * This method uses the string class names for comparison. See also {@link http://www.retep.org/2009/02/getting-class-values-from-annotations.html}.
	 *
	 * @param annotationMirror The annotation mirror
	 * @param clazz the class name to check against
	 *
	 * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 */
	public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, Class<? extends Annotation> clazz) {
		assert clazz != null;
		return isAnnotationMirrorOfType( annotationMirror, clazz.getName() );
	}

	/**
	 * Returns {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 * This method uses the string class names for comparison. See also {@link http://www.retep.org/2009/02/getting-class-values-from-annotations.html}.
	 *
	 * @param annotationMirror The annotation mirror
	 * @param fqcn the fully qualified class name to check against
	 *
	 * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
	 */
	public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, String fqcn) {
		assert annotationMirror != null;
		assert fqcn != null;
		String annotationClassName = annotationMirror.getAnnotationType().toString();

		return annotationClassName.equals( fqcn );
	}

	public static boolean isTypeElementOfType(TypeElement element, Class<?> clazz) {
		assert element != null;
		assert clazz != null;
		String elementClassName = element.getQualifiedName().toString();
		String className = clazz.getName();

		return elementClassName.equals( className );
	}

	/**
	 * Checks whether the {@code Element} hosts the annotation with the given fully qualified class name.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param fqcn the fully qualified class name of the annotation to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation.
	 */
	public static AnnotationMirror getAnnotationMirror(Element element, String fqcn) {
		assert element != null;
		assert fqcn != null;

		AnnotationMirror mirror = null;
		for ( AnnotationMirror am : element.getAnnotationMirrors() ) {
			if ( isAnnotationMirrorOfType( am, fqcn ) ) {
				mirror = am;
				break;
			}
		}
		return mirror;
	}

	/**
	 * Checks whether the {@code Element} hosts an annotation of the specified class.
	 *
	 * @param element the element to check for the hosted annotation
	 * @param clazz the annotation class to check for
	 *
	 * @return the annotation mirror for the specified annotation class from the {@code Element} or {@code null} in case
	 *         the {@code TypeElement} does not host the specified annotation.
	 */
	public static AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> clazz) {
		assert clazz != null;
		return getAnnotationMirror( element, clazz.getName() );
	}

	public static Object getAnnotationValue(AnnotationMirror annotationMirror, String parameterValue) {
		assert annotationMirror != null;
		assert parameterValue != null;

		Object returnValue = null;
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues()
				.entrySet() ) {
			if ( parameterValue.equals( entry.getKey().getSimpleName().toString() ) ) {
				returnValue = entry.getValue().getValue();
				break;
			}
		}
		return returnValue;
	}

	public static void determineAccessTypeForHierarchy(TypeElement searchedElement, Context context) {
		String fqcn = searchedElement.getQualifiedName().toString();
		context.logMessage( Diagnostic.Kind.OTHER, "Determining access type for " + fqcn );
		AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( fqcn );

		if ( accessTypeInfo != null && accessTypeInfo.isAccessTypeResolved() ) {
			context.logMessage(
					Diagnostic.Kind.OTHER,
					"AccessType for " + searchedElement.toString() + " found in cache: " + accessTypeInfo
			);
			return;
		}

		// check for explicit access type
		AccessType forcedAccessType = determineAnnotationSpecifiedAccessType( searchedElement );
		if ( forcedAccessType != null ) {
			context.logMessage(
					Diagnostic.Kind.OTHER, "Explicit access type on " + searchedElement + ":" + forcedAccessType
			);
			accessTypeInfo = new AccessTypeInformation( fqcn, forcedAccessType, null );
			context.addAccessTypeInformation( fqcn, accessTypeInfo );
			updateEmbeddableAccessType( searchedElement, context, forcedAccessType );
			return;
		}

		// need to find the default access type for this class
		// let's check first if this entity is the root of the class hierarchy and defines an id. If so the
		// placement of the id annotation determines the access type
		AccessType defaultAccessType = getAccessTypeInCaseElementIsRoot( searchedElement, context );
		if ( defaultAccessType != null ) {
			accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
			context.addAccessTypeInformation( fqcn, accessTypeInfo );
			updateEmbeddableAccessType( searchedElement, context, defaultAccessType );
			return;
		}

		// if we end up here we need to recursively look for superclasses
		defaultAccessType = getDefaultAccessForHierarchy( searchedElement, context );
		if ( defaultAccessType == null ) {
			defaultAccessType = AccessType.PROPERTY;
		}
		accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
		context.addAccessTypeInformation( fqcn, accessTypeInfo );
	}

	public static TypeMirror getCollectionElementType(DeclaredType t, String fqNameOfReturnedType, String explicitTargetEntityName, Context context) {
		TypeMirror collectionElementType;
		if ( explicitTargetEntityName != null ) {
			Elements elements = context.getElementUtils();
			TypeElement element = elements.getTypeElement( explicitTargetEntityName );
			collectionElementType = element.asType();
		}
		else {
			List<? extends TypeMirror> typeArguments = t.getTypeArguments();
			if ( typeArguments.size() == 0 ) {
				throw new MetaModelGenerationException( "Unable to determine collection type" );
			}
			else if ( Map.class.getCanonicalName().equals( fqNameOfReturnedType ) ) {
				collectionElementType = t.getTypeArguments().get( 1 );
			}
			else {
				collectionElementType = t.getTypeArguments().get( 0 );
			}
		}
		return collectionElementType;
	}

	private static void updateEmbeddableAccessType(TypeElement element, Context context, AccessType defaultAccessType) {
		List<? extends Element> fieldsOfClass = ElementFilter.fieldsIn( element.getEnclosedElements() );
		for ( Element field : fieldsOfClass ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, field );
		}

		List<? extends Element> methodOfClass = ElementFilter.methodsIn( element.getEnclosedElements() );
		for ( Element method : methodOfClass ) {
			updateEmbeddableAccessTypeForMember( context, defaultAccessType, method );
		}
	}

	private static void updateEmbeddableAccessTypeForMember(Context context, AccessType defaultAccessType, Element member) {
		EmbeddedAttributeVisitor visitor = new EmbeddedAttributeVisitor( context );
		String embeddedClassName = member.asType().accept( visitor, member );
		if ( embeddedClassName != null ) {
			AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( embeddedClassName );
			if ( accessTypeInfo == null ) {
				accessTypeInfo = new AccessTypeInformation( embeddedClassName, null, defaultAccessType );
				context.addAccessTypeInformation( embeddedClassName, accessTypeInfo );
			}
			else {
				accessTypeInfo.setDefaultAccessType( defaultAccessType );
			}
		}
	}

	private static AccessType getDefaultAccessForHierarchy(TypeElement element, Context context) {
		AccessType defaultAccessType = null;
		TypeElement superClass = element;
		do {
			superClass = TypeUtils.getSuperclassTypeElement( superClass );
			if ( superClass != null ) {
				String fqcn = superClass.getQualifiedName().toString();
				AccessTypeInformation accessTypeInfo = context.getAccessTypeInfo( fqcn );
				if ( accessTypeInfo != null && accessTypeInfo.getDefaultAccessType() != null ) {
					return accessTypeInfo.getDefaultAccessType();
				}
				if ( TypeUtils.containsAnnotation( superClass, Entity.class, MappedSuperclass.class ) ) {
					defaultAccessType = getAccessTypeInCaseElementIsRoot( superClass, context );
					if ( defaultAccessType != null ) {
						accessTypeInfo = new AccessTypeInformation( fqcn, null, defaultAccessType );
						context.addAccessTypeInformation( fqcn, accessTypeInfo );
						defaultAccessType = accessTypeInfo.getAccessType();
					}
					else {
						defaultAccessType = getDefaultAccessForHierarchy( superClass, context );
					}
				}
			}
		}
		while ( superClass != null && defaultAccessType == null );
		return defaultAccessType;
	}

	private static AccessType getAccessTypeInCaseElementIsRoot(TypeElement searchedElement, Context context) {
		AccessType defaultAccessType = null;
		List<? extends Element> myMembers = searchedElement.getEnclosedElements();
		for ( Element subElement : myMembers ) {
			List<? extends AnnotationMirror> entityAnnotations =
					context.getElementUtils().getAllAnnotationMirrors( subElement );
			for ( Object entityAnnotation : entityAnnotations ) {
				AnnotationMirror annotationMirror = (AnnotationMirror) entityAnnotation;
				if ( isIdAnnotation( annotationMirror ) ) {
					defaultAccessType = getAccessTypeOfIdAnnotation( subElement );
					break;
				}
				if (defaultAccessType != null) {
					break;
				}
			}
		}
		return defaultAccessType;
	}

	private static AccessType getAccessTypeOfIdAnnotation(Element element) {
		AccessType accessType = null;
		final ElementKind kind = element.getKind();
		if ( kind == ElementKind.FIELD || kind == ElementKind.METHOD ) {
			accessType = kind == ElementKind.FIELD ? AccessType.FIELD : AccessType.PROPERTY;
		}
		return accessType;
	}

	private static boolean isIdAnnotation(AnnotationMirror annotationMirror) {
		return TypeUtils.isAnnotationMirrorOfType( annotationMirror, Id.class )
				|| TypeUtils.isAnnotationMirrorOfType( annotationMirror, EmbeddedId.class );
	}

	public static AccessType determineAnnotationSpecifiedAccessType(Element element) {
		final AnnotationMirror accessAnnotationMirror = TypeUtils.getAnnotationMirror( element, Access.class );
		AccessType forcedAccessType = null;
		if ( accessAnnotationMirror != null ) {
			Element accessElement = (Element) TypeUtils.getAnnotationValue(
					accessAnnotationMirror,
					DEFAULT_ANNOTATION_PARAMETER_NAME
			);
			if ( accessElement.getKind().equals( ElementKind.ENUM_CONSTANT ) ) {
				if ( accessElement.getSimpleName().toString().equals( AccessType.PROPERTY.toString() ) ) {
					forcedAccessType = AccessType.PROPERTY;
				}
				else if ( accessElement.getSimpleName().toString().equals( AccessType.FIELD.toString() ) ) {
					forcedAccessType = AccessType.FIELD;
				}
			}
		}
		return forcedAccessType;
	}

	public static ElementKind getElementKindForAccessType(AccessType accessType) {
		if ( AccessType.FIELD.equals( accessType ) ) {
			return ElementKind.FIELD;
		}
		else {
			return ElementKind.METHOD;
		}
	}

	public static String getKeyType(DeclaredType t, Context context) {
		List<? extends TypeMirror> typeArguments = t.getTypeArguments();
		if ( typeArguments.size() == 0 ) {
			context.logMessage( Diagnostic.Kind.ERROR, "Unable to determine type argument for " + t );
		}
		return extractClosestRealTypeAsString( typeArguments.get( 0 ), context );
	}

	static class EmbeddedAttributeVisitor extends SimpleTypeVisitor6<String, Element> {
		private Context context;

		EmbeddedAttributeVisitor(Context context) {
			this.context = context;
		}

		@Override
		public String visitDeclared(DeclaredType declaredType, Element element) {
			TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement( declaredType );
			String fqNameOfReturnType = null;
			if ( containsAnnotation( returnedElement, Embeddable.class ) ) {
				fqNameOfReturnType = returnedElement.getQualifiedName().toString();
			}
			return fqNameOfReturnType;
		}

		@Override
		public String visitExecutable(ExecutableType t, Element p) {
			if ( !p.getKind().equals( ElementKind.METHOD ) ) {
				return null;
			}

			String string = p.getSimpleName().toString();
			if ( !StringUtil.isPropertyName( string ) ) {
				return null;
			}

			TypeMirror returnType = t.getReturnType();
			return returnType.accept( this, p );
		}
	}
}
