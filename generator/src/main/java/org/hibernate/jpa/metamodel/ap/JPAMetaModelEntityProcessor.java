package org.hibernate.jpa.metamodel.ap;



import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.*;
import java.util.*;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.hibernate.jpa.metamodel.ap.annotation.MetaEntity;
import org.hibernate.jpa.metamodel.ap.xml.XmlMetaEntity;
import org.hibernate.jpa.metamodel.xml.jaxb.Entity;
import org.hibernate.jpa.metamodel.xml.jaxb.EntityMappings;
import org.hibernate.jpa.metamodel.xml.jaxb.ObjectFactory;

//@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedAnnotationTypes("*") // TODO: this is not very effective
@SupportedSourceVersion(RELEASE_6)
public class JPAMetaModelEntityProcessor extends AbstractProcessor {

    //private static final String ORM_XML_LOCATION = "/META-INF/orm.xml";

    private static final Map<String, IMetaEntity> metaEntities = new HashMap<String, IMetaEntity>();

    private boolean ormProcessed = false;

    public JPAMetaModelEntityProcessor() {
    	System.out.println("Created Processor " + this);
    }

    public void init(ProcessingEnvironment env) {    	
        super.init(env);
        System.out.println("Init Processor " + this);
    }
    
    private void parsingOrmXmls() {
    	//make sure that we process ORM files only once per round 
    	if (ormProcessed) return;
    	parsingOrmXml("/META-INF", "orm.xml");
        //simulate 20 different ORM files to parse
    	//Removed since these causes issues in Eclipse APT
        //for (int i = 1 ; i <= 20 ; i++) parsingOrmXml("/model" + i , "orm.xml");
    	
        ormProcessed = true;
    }

    /**
     * Tries to check whether a orm.xml file exists and parses it using JAXB
     */
    private void parsingOrmXml(String pkg, String name) {
    	String resource = pkg +"/"+name;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "XYZ Checking for " + resource);
        
        InputStream ormStream = null;
        try {
			FileObject resource2 = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, pkg, name);
			ormStream = resource2.openInputStream();
		} catch (IOException e1) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Could not locate " + resource + " via APT api");
			//TODO: possible remove this fallback since it should not be needed.
			ormStream = this.getClass().getResourceAsStream(resource);
		}
        
        if (ormStream == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, resource + " not found.");
            return;
        }
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            EntityMappings mappings = (EntityMappings) unmarshaller.unmarshal(ormStream);
            Collection<Entity> entities = mappings.getEntity();
            String packageName = mappings.getPackage();
            for (Entity entity : entities) {
                String fullyQualifiedClassName = packageName + "." + entity.getClazz();
                Elements utils = processingEnv.getElementUtils();
                XmlMetaEntity metaEntity = new XmlMetaEntity(entity, packageName, utils.getTypeElement(fullyQualifiedClassName));
                writeFile(metaEntity);

                // keep track of alreay processed entities
                metaEntities.put(fullyQualifiedClassName, metaEntity);
            }
        } catch (JAXBException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Error unmarshalling orm.xml");
            e.printStackTrace();
        } catch (Exception e) {
        	processingEnv.getMessager().printMessage(
                       Diagnostic.Kind.ERROR,
                       "Problem while reading " + resource + " " + e.getMessage());
        	e.printStackTrace();
        	//TODO: too bad you can't mark resources as having issues
        }
    }

    /**
     * Process JPA-specific annotations in Java entity classes.
     *
     * @param aAnnotations      Matching annotations to be processed.
     * @param aRoundEnvironment Annotation processing round environment.
     * @return
     */
    @Override
    public boolean process(final Set<? extends TypeElement> aAnnotations,
                           final RoundEnvironment aRoundEnvironment) {

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing annotations:" + aAnnotations);

        StringBuilder sb = new StringBuilder("xxx" + new Date().toLocaleString());
        Set<? extends Element> elements = aRoundEnvironment.getRootElements();
        sb.append("\n\n");
        for (Element element : elements) {
            sb.append(element.toString());
            sb.append("\n");
            handleRootElementAnnotationMirrors(element);
        }

        sb.append("xxx");
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, sb.toString());


        if (aRoundEnvironment.processingOver()) {
        	//assuming that when processing is over, we are done and clear resources like ORM parsing
        	//we could keep some ORM parsing in memory but how to detect that a file has changed / not changed? 
        	ormProcessed = false;
        	metaEntities.clear();
        	processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Clear ORM processing resources");
            return false;	
        }
        parsingOrmXmls();
        for (Element element : elements) {
            handleRootElementAnnotationMirrors(element);
        }

        return true;
    }

    private void handleRootElementAnnotationMirrors(final Element element) {

        List<? extends AnnotationMirror> annotationMirrors = element
                .getAnnotationMirrors();

        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();

            if (element.getKind() == ElementKind.CLASS &&
                    annotationType.equals(javax.persistence.Entity.class.getName())) {
                MetaEntity metaEntity = new MetaEntity(processingEnv, (TypeElement) element);
                writeFile(metaEntity);
            }
        }
    }

    private void writeFile(IMetaEntity entity) {

        try {
            String metaModelPackage = entity.getPackageName();

            StringBuffer body = generateBody(entity);

            FileObject fo = processingEnv.getFiler().createSourceFile(
                    metaModelPackage + "." + entity.getSimpleName() + "_");
            OutputStream os = fo.openOutputStream();
            PrintWriter pw = new PrintWriter(os);

            pw.println("package " + metaModelPackage + ";");

            pw.println();

            pw.println(entity.generateImports());

            pw.println(body);

            pw.flush();
            pw.close();

        } catch (FilerException filerEx) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Problem with Processing Environment Filer: "
                            + filerEx.getMessage());
        } catch (IOException ioEx) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Problem opening file to write MetaModel for " + entity.getSimpleName()
                            + ioEx.getMessage());
        } 
    }

    /**
     * Generate everything after import statements
     *
     * @return body content
     */
    private StringBuffer generateBody(IMetaEntity entity) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = null;
        try {

            pw = new PrintWriter(sw);

            pw.println("@" + entity.importType(Generated.class.getName()) + "(\"JPA MetaModel for " + entity.getQualifiedName() + "\")");

            pw.println("@" + entity.importType("javax.persistence.metamodel.TypesafeMetamodel") + "(" +  entity.getSimpleName() + ".class)");

            pw.println("public abstract class " + entity.getSimpleName() + "_" + " {");

            pw.println();

            List<IMetaMember> members = entity.getMembers();

            for (IMetaMember metaMember : members) {
                pw.println("	" + metaMember.getDeclarationString());
            }
            pw.println();
            pw.println("}");
            return sw.getBuffer();
        } finally {
            if (pw != null) pw.close();
        }
    }
}
