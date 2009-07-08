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
	public void testDefaultAccessType() throws Exception{
		Class<?> user_ = Class.forName( "model.User_" );
		try {
			final Field nonPersistentField = user_.getField( "nonPersistent" );
			Assert.fail( "field should not be persistent" );
		}
		catch (NoSuchFieldException e) {}

	}
}
