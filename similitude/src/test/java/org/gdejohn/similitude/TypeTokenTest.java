package org.gdejohn.similitude;

import static java.lang.Integer.valueOf;
import static ch.qos.logback.classic.Level.*;
import static org.gdejohn.similitude.TypeToken.*;
import static org.testng.Assert.*;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("javadoc")
public class TypeTokenTest
{
	private static final Logger ROOT_LOGGER =
	(
		(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
	);
	
	static
	{
		ROOT_LOGGER.setLevel(WARN);
	}
	
	@Test
	public void testNonGenericType( )
	{
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Class<String> expected = String.class;
			
			TypeToken<String> token = typeOf(expected);
			
			Class<String> actual = token.getRawType( );
			
			assertEquals(actual, expected);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public void testNonGenericTypeToString( )
	{
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Class<String> clazz = String.class;
			
			String expected = clazz.getSimpleName( );
			
			TypeToken<String> token = typeOf(clazz);
			
			String actual = token.toString( );
			
			assertEquals(actual, expected);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public void testNonGenericObject( )
	{
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			String string = "xyzzy";
			
			Class<? extends String> expected = string.getClass( );
			
			TypeToken<? extends String> token = typeOf(string);
			
			Class<? extends String> actual = token.getRawType( );
			
			assertEquals(actual, expected);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public void testNonGenericEquality( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<? extends String> foo = typeOf("foo");
			
			TypeToken<? extends String> bar = typeOf("bar");
			
			TypeToken<? extends Integer> baz = typeOf(valueOf(0));
			
			assertTrue(foo.equals(foo));
			assertTrue(bar.equals(bar));
			assertTrue(baz.equals(baz));
			
			assertTrue(foo.equals(bar));
			assertTrue(bar.equals(foo));
			
			assertFalse(foo.equals(baz));
			assertFalse(baz.equals(foo));
			assertFalse(bar.equals(baz));
			assertFalse(baz.equals(bar));
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
}