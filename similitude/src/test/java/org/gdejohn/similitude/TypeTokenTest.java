package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static org.gdejohn.similitude.TypeToken.typeOf;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("javadoc")
public class TypeTokenTest
{
	private static final Logger ROOT_LOGGER =
	(
		(Logger)getLogger(ROOT_LOGGER_NAME)
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
		class Zeroth<Z>
		{
			@SuppressWarnings("unused")
			private Z field;
			
			Zeroth(Z arg)
			{
				field = arg;
			}
		}
		
		class First<F>
		{
			@SuppressWarnings("unused")
			private Zeroth<F> field;
			
			First(F arg)
			{
				field = new Zeroth<F>(arg);
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
			// ROOT_LOGGER.setLevel(DEBUG);
			
			String string = "xyzzy";
			
			Third<String> third = new Third<String>(string);
			Second<Third<String>> second = new Second<Third<String>>(third);
			First<Second<Third<String>>> first = new First<Second<Third<String>>>(second);
			
			TypeToken<? extends First<Second<Third<String>>>> firstToken = typeOf(first);
			
			TypeToken<?> secondToken = firstToken.getTypeArgument(First.class.getTypeParameters( )[0]);
			TypeToken<?> thirdToken = secondToken.getTypeArgument(Second.class.getTypeParameters( )[0]);
			TypeToken<?> stringToken = thirdToken.getTypeArgument(Third.class.getTypeParameters( )[0]);
			
			assertEquals(firstToken.getRawType( ), first.getClass( ));
			assertEquals(firstToken.toString( ), "First<Second<Third<String>>>");
			
			assertEquals(secondToken.getRawType( ), second.getClass( ));
			assertEquals(secondToken.toString( ), "Second<Third<String>>");
			assertEquals(secondToken, typeOf(second));

			assertEquals(thirdToken.getRawType( ), third.getClass( ));
			assertEquals(thirdToken.toString( ), "Third<String>");
			assertEquals(thirdToken, typeOf(third));

			assertEquals(stringToken.getRawType( ), string.getClass( ));
			assertEquals(stringToken.toString( ), "String");
			assertEquals(stringToken, typeOf(string));
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
	public static void privateConstructor( )
	{
		class Private
		{
			private Private( )
			{
				super( );
			}
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Set<Constructor<Private>> constructors = typeOf(Private.class).getAccessibleConstructors( );
			
			assertEquals(constructors.size( ), 1);
			assertEquals(constructors.iterator( ).next( ), Private.class.getDeclaredConstructor( ));
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
	public static void parameterizedType( )
	{
		class First<F>
		{
			@SuppressWarnings("unused")
			F field;
			
			First(F arg)
			{
				field = arg;
			}
		}
		
		class Second<S>
		{
			@SuppressWarnings("unused")
			First<S> field;
			
			public Second(First<S> arg)
			{
				field = arg;
			}
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			String string = "xyzzy";
			First<String> first = new First<String>(string);
			Second<String> second = new Second<String>(first);
			
			Type parameterType = Second.class.getConstructor(First.class).getGenericParameterTypes( )[0];
			
			TypeToken<?> constructorParameter = typeOf(parameterType, typeOf(second));
			
			assertEquals(constructorParameter, typeOf(first));
			assertEquals(constructorParameter.getTypeArgument(First.class.getTypeParameters( )[0]), typeOf(string));
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
	public static void genericMethodReturnType( )
	{
		class Parameter<P>
		{
			@SuppressWarnings("unused")
			P field;
			
			Parameter(P arg)
			{
				field = arg;
			}
		}
		
		class ReturnType<R, S> { }
		
		class Clazz<C>
		{
			@SuppressWarnings("unused")
			C field;
			
			Clazz(C arg)
			{
				field = arg;
			}
			
			@SuppressWarnings("unused")
			public <T> ReturnType<T, C> method(Parameter<T> arg)
			{
				return null;
			}
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Method method = Clazz.class.getMethod("method", Parameter.class);
			
			TypeToken<?> returnType = typeOf(new Clazz<String>("xyzzy")).getReturnType(method, new Parameter<Integer>(valueOf(1)));
			
			assertEquals(returnType.getRawType( ), ReturnType.class);
			assertEquals(returnType.getTypeArgument(ReturnType.class.getTypeParameters( )[0]).getRawType( ), Integer.class);
			assertEquals(returnType.getTypeArgument(ReturnType.class.getTypeParameters( )[1]).getRawType( ), String.class);
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
	public static void genericEnclosingType( )
	{
		class Outer<O>
		{
			@SuppressWarnings("unused")
			O field;
			
			Outer(O arg)
			{
				field = arg;
			}
			
			class Inner
			{
				@SuppressWarnings("unused")
				public Outer<String>.Inner method( )
				{
					return null;
				}
			}
		}
		
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			String string = "xyzzy";
			Outer<String> outer = new Outer<String>(string);
			Outer<String>.Inner inner = outer.new Inner( );
			
			TypeToken<? extends Outer<String>.Inner> token = typeOf(inner);
			TypeToken<?> enclosing = token.getEnclosingType( );
			TypeToken<?> returnType = typeOf(Outer.Inner.class.getMethod("method").getGenericReturnType( ));
			
			assertEquals(token.getRawType( ), Outer.Inner.class);
			
			assertEquals(enclosing.getRawType( ), Outer.class);
			assertEquals(enclosing.getTypeArgument(Outer.class.getTypeParameters( )[0]).getRawType( ), string.getClass( ));
			assertEquals(enclosing, typeOf(outer));
			
			assertEquals(token.toString( ), "Outer<String>.Inner");
			
			assertEquals(token, returnType);
			
			assertEquals(returnType.getRawType( ), Outer.Inner.class);
			assertEquals(returnType.toString( ), "Outer<String>.Inner");
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