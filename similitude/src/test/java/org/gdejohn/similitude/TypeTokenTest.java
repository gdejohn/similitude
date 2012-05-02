package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static org.gdejohn.similitude.TypeToken.typeOf;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
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
	public static void nonGenericType( )
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
	public static void nonGenericTypeToString( )
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
	public static void nonGenericObject( )
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
	public static void nonGenericEquality( )
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
	public static void genericArrayType( )
	{
		class Foo<F>
		{
			@SuppressWarnings("unused")
			Foo(F[ ] arg)
			{
				
			}
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<Foo<String>> parent = new TypeToken<Foo<String>>( ) { };
			TypeToken<?> token = typeOf(Foo.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0], parent);
			
			assertEquals(token.getRawType( ), String[ ].class);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void wildcardType( )
	{
		class Foo<F> { }
		
		class Bar<B>
		{
			@SuppressWarnings("unused")
			Bar(Foo<? extends B> arg)
			{
				
			}
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<Bar<String>> parent = new TypeToken<Bar<String>>( ) { };
			TypeToken<?> token = typeOf(Bar.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0], parent);
			
			assertEquals(token.getRawType( ), Foo.class);
			assertEquals(token.getTypeArgument(Foo.class.getTypeParameters( )[0]).getRawType( ), String.class);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void superTypeToken( )
	{
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<Map<Integer, Set<? extends char[ ]>>> token = new TypeToken<Map<Integer, Set<? extends char[ ]>>>( ) { };
			TypeToken<?> valueType = token.getTypeArgument(Map.class.getTypeParameters( )[1]);
			
			assertEquals(token.toString( ), "Map<Integer,Set<char[]>>");
			assertEquals(token.getRawType( ), Map.class);
			assertEquals(token.getTypeArgument(Map.class.getTypeParameters( )[0]).getRawType( ), Integer.class);
			assertEquals(valueType.getRawType( ), Set.class);
			assertEquals(valueType.getTypeArgument(Set.class.getTypeParameters( )[0]).getRawType( ), char[ ].class);
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
			private Zeroth<? extends F> field;
			
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
			
			class Inner { }
		}
		
		try
		{
			// ROOT_LOGGER.setLevel(DEBUG);
			
			String string = "xyzzy";
			Outer<String> outer = new Outer<String>(string);
			Outer<String>.Inner inner = outer.new Inner( );
			
			TypeToken<? extends Outer<String>.Inner> token = typeOf(inner);
			TypeToken<?> enclosing = token.getEnclosingType( );
			TypeToken<Outer<String>.Inner> returnType = new TypeToken<Outer<String>.Inner>( ) { };
			
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
	
	@Test
	public static void nonGenericSuperType( )
	{
		class Foo { }
		
		class Bar extends Foo { }
		
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<Bar> bar = typeOf(Bar.class);
			TypeToken<?> foo = bar.getSuperType( );
			TypeToken<?> object = foo.getSuperType( );
			
			assertEquals(foo.getRawType( ), Foo.class);
			assertEquals(object.getRawType( ), Object.class);
			assertNull(object.getSuperType( ));
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void genericSuperType( )
	{
		class Foo<E, F> { }
		
		class Bar<R> extends Foo<R, Set<? extends String[ ]>> { }
		
		class Baz<Z> extends Bar<Z> { }
		
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			TypeVariable<?> E = Foo.class.getTypeParameters( )[0];
			TypeVariable<?> F = Foo.class.getTypeParameters( )[1];
			TypeVariable<?> R = Bar.class.getTypeParameters( )[0];
			TypeVariable<?> T = Set.class.getTypeParameters( )[0];
			
			TypeToken<Baz<Integer>> baz = new TypeToken<Baz<Integer>>( ) { };
			TypeToken<?> bar = baz.getSuperType( );
			TypeToken<?> foo = bar.getSuperType( );
			TypeToken<?> fooF = foo.getTypeArgument(F);
			TypeToken<?> object = foo.getSuperType( );

			assertEquals(bar.getRawType( ), Bar.class);
			assertEquals(bar.getTypeArgument(R).getRawType( ), Integer.class);
			assertEquals(foo.getRawType( ), Foo.class);
			assertEquals(foo.getTypeArgument(E).getRawType( ), Integer.class);
			assertEquals(fooF.getRawType( ), Set.class);
			assertEquals(fooF.getTypeArgument(T).getRawType( ), String[ ].class);
			assertEquals(object.getRawType( ), Object.class);
			assertNull(object.getSuperType( ));
			assertEquals(bar.getTypeArgument(E).getRawType( ), Integer.class);
			assertEquals(bar.getTypeArgument(F).getRawType( ), Set.class);
			
			try
			{
				bar.getTypeArgument(T);
				
				fail("Failed.");
			}
			catch (Exception e)
			{
				return;
			}
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	interface IFirst { }
	
	interface ISecond { }
	
	interface IThird extends IFirst { }
	
	interface IFourth extends ISecond { }
	
	// @Test
	public static void nonGenericInterfaces( )
	{
		class Impl implements IThird, IFourth { }
		
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken<Impl> token = typeOf(Impl.class);
			
			Iterator<TypeToken<?>> iterator = token.getInterfaces( ).iterator( );
			
			assertEquals(token.getInterfaces( ).size( ), 2);
			
			TypeToken<?> third = iterator.next( );
			
			assertEquals(third.getRawType( ), IThird.class);
			assertNull(third.getSuperType( ));
			
			TypeToken<?> fourth = iterator.next( );
			
			assertEquals(fourth.getRawType( ), IFourth.class);
			assertNull(fourth.getSuperType( ));
			
			iterator = third.getInterfaces( ).iterator( );
			
			assertEquals(iterator.next( ).getRawType( ), IFirst.class);
			assertFalse(iterator.hasNext( ));
			
			iterator = fourth.getInterfaces( ).iterator( );
			
			assertEquals(iterator.next( ).getRawType( ), ISecond.class);
			assertFalse(iterator.hasNext( ));
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
}