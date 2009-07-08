package org.hibernate.jpa.metamodel.ap.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.tools.Diagnostic.Kind;
import javax.tools.Diagnostic;

import org.hibernate.jpa.metamodel.ap.IMetaEntity;
import org.hibernate.jpa.metamodel.ap.IMetaAttribute;
import org.hibernate.jpa.metamodel.ap.ImportContext;
import org.hibernate.jpa.metamodel.ap.ImportContextImpl;
import org.hibernate.jpa.metamodel.ap.TypeUtils;
import org.hibernate.jpa.metamodel.ap.Context;

public class MetaEntity implements IMetaEntity {

	final TypeElement element;
	final protected ProcessingEnvironment pe;

	final ImportContext importContext;
	private Context context;

	public MetaEntity(ProcessingEnvironment pe, TypeElement element, Context context) {
		this.element = element;
		this.pe = pe;
		importContext = new ImportContextImpl( getPackageName() );
		this.context = context;
	}

	public String getSimpleName() {
		return element.getSimpleName().toString();
	}

	public Element getOriginalElement() {
		return element;
	}

	public String getQualifiedName() {
		return element.getQualifiedName().toString();
	}

	public String getPackageName() {
		PackageElement packageOf = pe.getElementUtils().getPackageOf( element );
		return pe.getElementUtils().getName( packageOf.getQualifiedName() ).toString();
	}

	public List<IMetaAttribute> getMembers() {

		List<IMetaAttribute> members = new ArrayList<IMetaAttribute>();

		if ( useFields() ) {

			List<? extends Element> myMembers = ElementFilter.fieldsIn( element.getEnclosedElements() );

			pe.getMessager()
					.printMessage( Kind.NOTE, "Scanning " + myMembers.size() + " fields for " + element.toString() );

			for ( Element mymember : myMembers ) {

				MetaAttribute result = mymember.asType().accept( new TypeVisitor( this ), mymember );
				if ( result != null ) {
					members.add( result );
				}
				else {
					//pe.getMessager().printMessage( Kind.WARNING, "Could not find valid info for JPA property", mymember );
				}
			}

		}
		else {
			List<? extends Element> myMembers = ElementFilter.methodsIn( element.getEnclosedElements() );

			pe.getMessager()
					.printMessage( Kind.NOTE, "Scanning " + myMembers.size() + " methods for " + element.toString() );
			for ( Element mymember : myMembers ) {

				MetaAttribute result = mymember.asType().accept( new TypeVisitor( this ), mymember );
				if ( result != null ) {
					members.add( result );
				}
				else {
					//pe.getMessager().printMessage(Kind.WARNING, "Not a valid JPA property", mymember);
				}
			}

		}

		if ( members.size() == 0 ) {
			pe.getMessager().printMessage( Kind.WARNING, "No properties found on " + element, element );
		}
		return members;
	}

	private boolean useFields() {
		AccessType accessType = getAccessTypeForClass( element );
		if (accessType != null) return accessType == AccessType.FIELD;

		//we dont' know
		//if an enity go up
		//TODO if a superclass go down till you find entity
		//TODO if in the superclass case you're still unsure, go up
		TypeElement superClass = element;
		do {
			superClass = TypeUtils.getSuperclass( superClass );
			if (superClass != null) {
				if ( superClass.getAnnotation( Entity.class ) != null ) {
					//FIXME make it work for XML
					accessType = getAccessTypeForClass(superClass);
					if ( accessType != null ) return accessType == AccessType.FIELD;
				}
				else if ( superClass.getAnnotation( MappedSuperclass.class ) != null ) {
					accessType = getAccessTypeForClass(superClass);
					if ( accessType != null ) return accessType == AccessType.FIELD;
				}
			}
		}
		while ( superClass != null );
		//this is a subclass so caching is OK
		context.addAccessType( this.element, AccessType.PROPERTY );
		return false;
	}

	private AccessType getAccessTypeForClass(TypeElement searchedElement) {
		pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "check class" + searchedElement );
		final AccessType accessType = context.getAccessTypes().get( searchedElement );
		if ( accessType != null ) {
			pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "Found in cache" + searchedElement + ":" + accessType );
			return accessType;
		}

		List<? extends Element> myMembers = searchedElement.getEnclosedElements();
		for ( Element subElement : myMembers ) {
			List<? extends AnnotationMirror> entityAnnotations =
					pe.getElementUtils().getAllAnnotationMirrors( subElement );

			for ( Object entityAnnotation : entityAnnotations ) {
				AnnotationMirror annotationMirror = ( AnnotationMirror ) entityAnnotation;

				final String annotationType = annotationMirror.getAnnotationType().toString();

				if ( annotationType.equals( Id.class.getName() )
						|| annotationType.equals( EmbeddedId.class.getName() ) ) {
					pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "Found id on" + searchedElement );
					if ( subElement.getKind() == ElementKind.FIELD ) {
						context.addAccessType( searchedElement, AccessType.FIELD );
						pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + accessType );
						return AccessType.FIELD;
					}
					else {
						context.addAccessType( searchedElement, AccessType.PROPERTY );
						pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "access type " + searchedElement + ":" + accessType );
						return AccessType.PROPERTY;
					}
				}
			}
		}
		pe.getMessager().printMessage( Diagnostic.Kind.NOTE, "No access type found: " + searchedElement );
		return null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MetaEntity" );
		sb.append( "{element=" ).append( element );
		sb.append( '}' );
		return sb.toString();
	}

	static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	class TypeVisitor extends SimpleTypeVisitor6<MetaAttribute, Element> {

		MetaEntity parent;

		TypeVisitor(MetaEntity parent) {
			this.parent = parent;
		}

		@Override
		protected MetaAttribute defaultAction(TypeMirror e, Element p) {
			return super.defaultAction( e, p );
		}

		@Override
		public MetaAttribute visitPrimitive(PrimitiveType t, Element p) {
			if ( p.getAnnotation( Transient.class ) == null ) {
				return new MetaSingleAttribute( parent, p, TypeUtils.toTypeString( t ) );
			}
			else {
				return null;
			}
		}


		@Override
		public MetaAttribute visitDeclared(DeclaredType t, Element p) {
			if ( p.getAnnotation( Transient.class ) == null ) {
				TypeElement e = ( TypeElement ) pe.getTypeUtils().asElement( t );
				String collection = COLLECTIONS.get( e.getQualifiedName().toString() ); // WARNING: .toString() is necessary here since Name equals does not compare to String
				if ( collection != null ) {
					if ( collection.equals( "javax.persistence.metamodel.MapAttribute" ) ) {
						return new MetaMap( parent, p, collection, getKeyType( t ), getElementType( t ) );
					}
					else {
						return new MetaCollection( parent, p, collection, getElementType( t ) );
					}
				}
				else {
					return new MetaSingleAttribute( parent, p, e.getQualifiedName().toString() );
				}
			}
			else {
				return null;
			}
		}


		@Override
		public MetaAttribute visitExecutable(ExecutableType t, Element p) {
			String string = p.getSimpleName().toString();

			// TODO: implement proper property get/is/boolean detection
			if ( string.startsWith( "get" ) || string.startsWith( "is" ) ) {
				TypeMirror returnType = t.getReturnType();

				return returnType.accept( this, p );
			}
			else {
				return null;
			}
		}
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType( fqcn );
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport( fqcn, member );
	}

	public String importType(Name qualifiedName) {
		return importType( qualifiedName.toString() );
	}

	public TypeElement getTypeElement() {
		return element;
	}

	private String getKeyType(DeclaredType t) {
		return t.getTypeArguments().get( 0 ).toString();
	}


	private String getElementType(DeclaredType declaredType) {
		if ( declaredType.getTypeArguments().size() == 1 ) {
			return declaredType.getTypeArguments().get( 0 ).toString();
		}
		else {
			return declaredType.getTypeArguments().get( 1 ).toString();
		}
	}
}
