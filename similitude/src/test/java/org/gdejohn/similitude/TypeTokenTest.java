package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static org.gdejohn.similitude.TypeToken.typeOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.fail;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

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
	public void nonGenericType( )
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
	public void nonGenericTypeToString( )
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
	public void nonGenericObject( )
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
	public void nonGenericEquality( )
	{
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<? extends String> foo = typeOf("foo");
			
			TypeToken<? extends String> bar = typeOf("bar");
			
			TypeToken<? extends Integer> baz = typeOf(valueOf(0));
			
			assertEquals(foo, foo);
			assertEquals(bar, bar);
			assertEquals(baz, baz);
			
			assertEquals(foo, bar);
			assertEquals(bar, foo);
			
			assertNotEquals(foo, baz);
			assertNotEquals(baz, foo);
			assertNotEquals(bar, baz);
			assertNotEquals(baz, bar);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void inheritedFields( )
	{
		class Parent
		{
			@SuppressWarnings("unused")
			private final String parentField = "parent";
		}
		
		class Child extends Parent
		{
			@SuppressWarnings("unused")
			private final String childField = "child"; 
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Field parentField = Parent.class.getDeclaredField("parentField");
			
			Field childField = Child.class.getDeclaredField("childField");
			
			Set<Field> expected = new LinkedHashSet<Field>(asList(parentField, childField));
			
			TypeToken<Child> token = typeOf(Child.class);
			
			Set<Field> actual = token.getAllInstanceFields( );
			
			assertEquals(actual, expected);
		}
		catch (Exception e)
		{
			fail("Failed.", e);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void instanceOfTopLevelGenericType( )
	{
		class First<F>
		{
			@SuppressWarnings("unused")
			private F field;
			
			First(F arg)
			{
				field = arg;
			}
		}
		
		class Second<S>
		{
			@SuppressWarnings("unused")
			private S field;
			
			Second(S arg)
			{
				field = arg;
			}
		}
		
		class Third<T>
		{
			@SuppressWarnings("unused")
			private T field;
			
			Third(T arg)
			{
				field = arg;
			}
		}
		
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			Third<String> third = new Third<String>("xyzzy");
			
			Second<Third<String>> second = new Second<Third<String>>(third);
			
			First<Second<Third<String>>> first = new First<Second<Third<String>>>(second);
			
			TypeToken<? extends First<Second<Third<String>>>> token = typeOf(first);
			
			assertEquals(token.toString( ), "First<Second<Third<String>>>");
		}
		catch (Exception e)
		{
			fail("Failed.", e);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
}