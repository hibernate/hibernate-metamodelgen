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
import javax.tools.Diagnostic.Kind;

import org.hibernate.jpa.metamodel.ap.IMetaEntity;
import org.hibernate.jpa.metamodel.ap.IMetaAttribute;
import org.hibernate.jpa.metamodel.ap.ImportContext;
import org.hibernate.jpa.metamodel.ap.ImportContextImpl;
import org.hibernate.jpa.metamodel.ap.TypeUtils;

public class MetaEntity implements IMetaEntity {

	final TypeElement element;
	final protected ProcessingEnvironment pe;

	final ImportContext importContext;

	public MetaEntity(ProcessingEnvironment pe, TypeElement element) {
		this.element = element;
		this.pe = pe;
		importContext = new ImportContextImpl( getPackageName() );
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
					.printMessage( Kind.NOTE, "Scanning " + myMembers.size() + " field s for " + element.toString() );

			for ( Element mymember : myMembers ) {

				MetaAttribute result = mymember.asType().accept( new TypeVisitor( this ), mymember );
				if ( result != null ) {
					members.add( result );
				}
				else {
					pe.getMessager()
							.printMessage( Kind.WARNING, "Could not find valid info for JPA property", mymember );
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


	//TODO: Find more efficient way to identify wether we should use fields or properties
	private boolean useFields() {
		List<? extends Element> myMembers = element.getEnclosedElements();
		for ( Element element : myMembers ) {
			List<? extends AnnotationMirror> entityAnnotations =
					pe.getElementUtils().getAllAnnotationMirrors( element );

			for ( Object entityAnnotation : entityAnnotations ) {
				AnnotationMirror annotationMirror = ( AnnotationMirror ) entityAnnotation;

				final String annotationType = annotationMirror.getAnnotationType().toString();

				if ( annotationType.equals( Id.class.getName() ) ||
						annotationType.equals( EmbeddedId.class.getName() ) ) {
					if ( element.getKind() == ElementKind.FIELD ) {
						return true;
					}
				}
			}
		}

		return false;
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
			return new MetaSingleAttribute( parent, p, TypeUtils.toTypeString( t ) );
		}


		@Override
		public MetaAttribute visitDeclared(DeclaredType t, Element p) {
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
