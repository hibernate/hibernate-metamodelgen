package test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * @author Hardy Ferentschik
 */
public class XmlMappingTest {
	@Test
	public void testXmlConfiguredEmbeddedClassGenerated() throws Exception {
		assertNotNull( Class.forName( "model.xmlmapped.Address_" ) );
	}

	@Test
	public void testXmlConfiguredMappedSuperclassGenerated() throws Exception {
		Class<?> building = Class.forName( "model.xmlmapped.Building_" );
		assertNotNull( building );
		assertNotNull( building.getField( "address" ) );
	}

	@Test
	public void testClassHierarchy() throws Exception {
		Class<?> mammal = Class.forName( "model.xmlmapped.Mammal_" );
		assertNotNull( mammal );

		Class<?> being = Class.forName( "model.xmlmapped.LivingBeing_" );
		assertNotNull( being );

		assertTrue( mammal.getSuperclass().equals( being ) );
	}

	@Test(expectedExceptions = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "model.Dummy_" );
	}
}