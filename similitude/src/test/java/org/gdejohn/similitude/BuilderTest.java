package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.lang.Boolean.FALSE;
import static java.lang.Byte.valueOf;
import static java.lang.Character.valueOf;
import static java.lang.Double.valueOf;
import static java.lang.Float.valueOf;
import static java.lang.Integer.valueOf;
import static java.lang.Long.valueOf;
import static java.lang.Short.valueOf;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.*;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@Test(dataProvider="builder")
@SuppressWarnings("javadoc")
public class BuilderTest
{
	private static final Logger ROOT_LOGGER =
	(
		(Logger)getLogger(ROOT_LOGGER_NAME)
	);
	
	@BeforeGroups(groups="debug")
	@SuppressWarnings("unused")
	private static void setLevelDebug( )
	{
		ROOT_LOGGER.setLevel(DEBUG);
	}
	
	@BeforeClass
	@AfterGroups(alwaysRun=true, groups="debug")
	@SuppressWarnings("unused")
	private static void setLevelWarn( )
	{
		ROOT_LOGGER.setLevel(WARN);
	}
	
	@DataProvider
	@SuppressWarnings("unused")
	private static Object[ ][ ] builder( )
	{
		return new Object[ ][ ] {{new Builder( )}};
	}
	
	public static void basicTypes(Builder builder)
	{
		assertEquals(builder.instantiate(byte.class), valueOf((byte)0));
		assertEquals(builder.instantiate(short.class), valueOf((short)0));
		assertEquals(builder.instantiate(int.class), valueOf(0));
		assertEquals(builder.instantiate(long.class), valueOf(0L));
		assertEquals(builder.instantiate(float.class), valueOf(0.0f));
		assertEquals(builder.instantiate(double.class), valueOf(0.0d));
		assertEquals(builder.instantiate(char.class), valueOf('\u0000'));
		assertEquals(builder.instantiate(boolean.class), FALSE);
		assertEquals(builder.instantiate(String.class), "");
	}
	
	private enum EnumType
	{
		FIRST, SECOND
	}
	
	public static void enumType(Builder builder)
	{
		assertEquals(builder.instantiate(EnumType.class), EnumType.FIRST);
	}
	
	public static void array(Builder builder)
	{
		String[ ] array = builder.instantiate(String[ ].class);
		
		assertNotNull(array);
		assertEquals(array.length, 0);
	}
	
	public static void testInterface(Builder builder)
	{
		List<String> list = builder.instantiate(new TypeToken<List<String>>( ) { });
		
		assertFalse(list.add(null));
	}
}