package tests;

import java.lang.reflect.Field;

import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.Assert;

/**
 * @author Emmanuel Bernard
 */
@Test
public class AccessTypeTest {
	@Test
	public void testDefaultAccessTypeOnEntity() throws Exception{
		absenceOfField( "model.User_", "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForSubclassOfEntity() throws Exception{
		absenceOfField( "model.Customer_", "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForEmbeddable() throws Exception{
		absenceOfField( "model.Detail_", "nonPersistent	" );
	}


	private void absenceOfField(String className, String fieldName) throws ClassNotFoundException {
		Class<?> user_ = Class.forName( className );
		try {

			final Field nonPersistentField = user_.getField( fieldName );
			Assert.fail( "field should not be persistent" );
		}
		catch (NoSuchFieldException e) {}
	}
}
