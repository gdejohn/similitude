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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@Test
@SuppressWarnings("javadoc")
public class TypeTokenTest
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
	
	public static void nonGenericType( )
	{
		Class<String> expected = String.class;
		
		TypeToken<String> token = typeOf(expected);
		
		Class<String> actual = token.getRawType( );
		
		assertEquals(actual, expected);
	}
	
	public static void nonGenericTypeToString( )
	{
		Class<String> clazz = String.class;
		
		String expected = clazz.getSimpleName( );
		
		TypeToken<String> token = typeOf(clazz);
		
		String actual = token.toString( );
		
		assertEquals(actual, expected);
	}
	
	public static void nonGenericObject( )
	{
		String string = "xyzzy";
		
		Class<? extends String> expected = string.getClass( );
		
		TypeToken<? extends String> token = typeOf(string);
		
		Class<? extends String> actual = token.getRawType( );
		
		assertEquals(actual, expected);
	}
	
	public static void nonGenericEquality( )
	{
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
	
	public static void array( )
	{
		TypeToken<? extends String[ ]> token = typeOf(new String[ ] {"foo", "bar", "baz"});
		
		assertEquals(token.getRawType( ), String[ ].class);
		assertEquals(token.toString( ), "String[]");
	}
	
	public static void inheritedFields( ) throws SecurityException, NoSuchFieldException
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
		
		Field parentField = Parent.class.getDeclaredField("parentField");
		
		Field childField = Child.class.getDeclaredField("childField");
		
		Set<Field> expected = new LinkedHashSet<Field>(asList(parentField, childField));
		
		TypeToken<Child> token = typeOf(Child.class);
		
		Set<Field> actual = token.getAllInstanceFields( );
		
		assertEquals(actual, expected);
	}
	
	public static void privateConstructor( ) throws SecurityException, NoSuchMethodException
	{
		class Private
		{
			private Private( )
			{
				super( );
			}
		}
		
		Set<Constructor<Private>> constructors = typeOf(Private.class).getAccessibleConstructors( );
		
		assertEquals(constructors.size( ), 1);
		assertEquals(constructors.iterator( ).next( ), Private.class.getDeclaredConstructor( ));
	}
	
	public static void parameterizedType( ) throws SecurityException, NoSuchMethodException
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
		
		String string = "xyzzy";
		First<String> first = new First<String>(string);
		Second<String> second = new Second<String>(first);
		
		Type parameterType = Second.class.getConstructor(First.class).getGenericParameterTypes( )[0];
		
		TypeToken<?> constructorParameter = typeOf(parameterType, typeOf(second));
		
		assertEquals(constructorParameter, typeOf(first));
		assertEquals(constructorParameter.getTypeArgument(First.class.getTypeParameters( )[0]), typeOf(string));
	}
	
	public static void genericArrayType( )
	{
		class Foo<F>
		{
			@SuppressWarnings("unused")
			Foo(F[ ] arg)
			{
				
			}
		}
		
		TypeToken<Foo<String>> parent = new TypeToken<Foo<String>>( ) { };
		TypeToken<?> token = typeOf(Foo.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0], parent);
		
		assertEquals(token.getRawType( ), String[ ].class);
	}
	
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
		
		TypeToken<Bar<String>> parent = new TypeToken<Bar<String>>( ) { };
		TypeToken<?> token = typeOf(Bar.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0], parent);
		
		assertEquals(token.getRawType( ), Foo.class);
		assertEquals(token.getTypeArgument(Foo.class.getTypeParameters( )[0]).getRawType( ), String.class);
	}
	
	public static void superTypeToken( )
	{
		TypeToken<Map<Integer, Set<? extends char[ ]>>> token = new TypeToken<Map<Integer, Set<? extends char[ ]>>>( ) { };
		TypeToken<?> valueType = token.getTypeArgument(Map.class.getTypeParameters( )[1]);
		
		assertEquals(token.toString( ), "Map<Integer,Set<char[]>>");
		assertEquals(token.getRawType( ), Map.class);
		assertEquals(token.getTypeArgument(Map.class.getTypeParameters( )[0]).getRawType( ), Integer.class);
		assertEquals(valueType.getRawType( ), Set.class);
		assertEquals(valueType.getTypeArgument(Set.class.getTypeParameters( )[0]).getRawType( ), char[ ].class);
	}
	
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
	
	public static void genericMethodReturnType( ) throws SecurityException, NoSuchMethodException
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
		
		Method method = Clazz.class.getMethod("method", Parameter.class);
		
		TypeToken<?> returnType = typeOf(new Clazz<String>("xyzzy")).getReturnType(method, new Parameter<Integer>(valueOf(1)));
		
		assertEquals(returnType.getRawType( ), ReturnType.class);
		assertEquals(returnType.getTypeArgument(ReturnType.class.getTypeParameters( )[0]).getRawType( ), Integer.class);
		assertEquals(returnType.getTypeArgument(ReturnType.class.getTypeParameters( )[1]).getRawType( ), String.class);
	}
	
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
	
	public static void nonGenericSuperType( )
	{
		class Foo { }
		
		class Bar extends Foo { }
		
		TypeToken<Bar> bar = typeOf(Bar.class);
		TypeToken<?> foo = bar.getSuperClass( );
		TypeToken<?> object = foo.getSuperClass( );
		
		assertEquals(foo.getRawType( ), Foo.class);
		assertEquals(object.getRawType( ), Object.class);
		assertNull(object.getSuperClass( ));
	}
	
	public static void genericSuperType( )
	{
		class Foo<E, F> { }
		
		class Bar<R> extends Foo<R, Set<? extends String[ ]>> { }
		
		class Baz<Z> extends Bar<Z> { }
		
		TypeVariable<?> E = Foo.class.getTypeParameters( )[0];
		TypeVariable<?> F = Foo.class.getTypeParameters( )[1];
		TypeVariable<?> R = Bar.class.getTypeParameters( )[0];
		TypeVariable<?> T = Set.class.getTypeParameters( )[0];
		
		TypeToken<Baz<Integer>> baz = new TypeToken<Baz<Integer>>( ) { };
		TypeToken<?> bar = baz.getSuperClass( );
		TypeToken<?> foo = bar.getSuperClass( );
		TypeToken<?> fooF = foo.getTypeArgument(F);
		TypeToken<?> object = foo.getSuperClass( );

		assertEquals(bar.getRawType( ), Bar.class);
		assertEquals(bar.getTypeArgument(R).getRawType( ), Integer.class);
		assertEquals(foo.getRawType( ), Foo.class);
		assertEquals(foo.getTypeArgument(E).getRawType( ), Integer.class);
		assertEquals(fooF.getRawType( ), Set.class);
		assertEquals(fooF.getTypeArgument(T).getRawType( ), String[ ].class);
		assertEquals(object.getRawType( ), Object.class);
		assertNull(object.getSuperClass( ));
		assertEquals(bar.getTypeArgument(E).getRawType( ), Integer.class);
		assertEquals(bar.getTypeArgument(F).getRawType( ), Set.class);
	}
	
	private interface IFirst { }
	
	private interface ISecond { }
	
	private interface IThird extends IFirst { }
	
	private interface IFourth extends ISecond { }
	
	public static void nonGenericInterfaces( )
	{
		class Impl implements IThird, IFourth { }
		
		TypeToken<Impl> token = typeOf(Impl.class);
		
		Iterator<TypeToken<?>> iterator = token.getInterfaces( ).iterator( );
		
		assertEquals(token.getInterfaces( ).size( ), 2);
		
		TypeToken<?> third = iterator.next( );
		
		assertEquals(third.getRawType( ), IThird.class);
		assertNull(third.getSuperClass( ));
		
		TypeToken<?> fourth = iterator.next( );
		
		assertEquals(fourth.getRawType( ), IFourth.class);
		assertNull(fourth.getSuperClass( ));
		
		iterator = third.getInterfaces( ).iterator( );
		
		assertEquals(iterator.next( ).getRawType( ), IFirst.class);
		assertFalse(iterator.hasNext( ));
		
		iterator = fourth.getInterfaces( ).iterator( );
		
		assertEquals(iterator.next( ).getRawType( ), ISecond.class);
		assertFalse(iterator.hasNext( ));
	}
	
	private enum EnumType { }
	
	public static void enumType( )
	{
		TypeVariable<?> E = Enum.class.getTypeParameters( )[0];
		
		TypeToken<EnumType> token = typeOf(EnumType.class);
		TypeToken<?> arg = token.getTypeArgument(E);
		
		assertEquals(arg, token);
		assertSame(arg, token);
	}
	
	public static void recursiveGenericInterface( )
	{
		TypeVariable<?> T = Comparable.class.getTypeParameters( )[0];
		
		assertEquals(typeOf(String.class).getTypeArgument(T).getRawType( ), String.class);
	}
	
	public static void instanceOfNonGenericType( )
	{
		assertTrue(typeOf(String.class).isInstance("xyzzy"));
	}
	
	public static void instanceOfGenericType( )
	{
		abstract class Abstract<A> implements Comparable<A> { }
		
		class Impl<I extends Comparable<? super I>> extends Abstract<I>
		{
			I field;
			
			Impl(I arg)
			{
				field = arg;
			}
			
			@Override
			public int compareTo(I o)
			{
				return field.compareTo(o);
			}
		}
		
		TypeToken<Comparable<CharSequence>> comparableCharSequence = new TypeToken<Comparable<CharSequence>>( ) { };
		TypeToken<Comparable<Number>> comparableNumber = new TypeToken<Comparable<Number>>( ) { };
		
		Impl<String> instance = new Impl<String>("xyzzy");
		
		assertTrue(comparableCharSequence.isInstance(instance));
		assertFalse(comparableNumber.isInstance(instance));
	}
	
	public static void instanceOfInnerClassSubType( ) throws Exception
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
		
		class Sub<S> extends Outer<S>.Inner
		{
			Sub(Outer<S> arg)
			{
				arg.super( );
			}
		}
		
		TypeToken<Outer<CharSequence>.Inner> outerCharSequenceInner = new TypeToken<Outer<CharSequence>.Inner>( ) { };
		TypeToken<Outer<Number>.Inner> outerNumberInner = new TypeToken<Outer<Number>.Inner>( ) { };
		
		Sub<String> instance = new Sub<String>(new Outer<String>("xyzzy"));
		
		assertTrue(outerCharSequenceInner.isInstance(instance));
		assertFalse(outerNumberInner.isInstance(instance));
	}
	
	public static void commonSuperType( )
	{
		class Foo { }
		
		class Bar extends Foo { }
		
		class Baz extends Foo { }
		
		TypeToken<Foo> foo = typeOf(Foo.class);
		TypeToken<Bar> bar = typeOf(Bar.class);
		TypeToken<Baz> baz = typeOf(Baz.class);

		assertEquals(foo.getCommonSuperType(foo), foo);
		assertEquals(foo.getCommonSuperType(bar), foo);
		assertEquals(bar.getCommonSuperType(baz), foo);
		assertEquals(baz.getCommonSuperType(bar), foo);
	}
	
	public static void multipleParameterizations( )
	{
		@SuppressWarnings("unused")
		class Clazz<C>
		{
			C first;
			
			C second;
			
			Clazz(C first, C second)
			{
				this.first = first;
				
				this.second = second;
			}
		}
		
		class Foo { }
		
		class Bar extends Foo { }
		
		class Baz extends Foo { }
		
		Clazz<Foo> list = new Clazz<Foo>(new Bar( ), new Baz( ));
		
		TypeToken<Clazz<Foo>> fooClazz = new TypeToken<Clazz<Foo>>( ) { };
		TypeToken<Clazz<Bar>> barClazz = new TypeToken<Clazz<Bar>>( ) { };
		TypeToken<Clazz<Baz>> bazClazz = new TypeToken<Clazz<Baz>>( ) { };
		
		assertTrue(fooClazz.isInstance(list));
		assertFalse(barClazz.isInstance(list));
		assertFalse(bazClazz.isInstance(list));
	}
	
	public static void genericRecursive( )
	{
		@SuppressWarnings("unused")
		class Recursive<R>
		{
			R data;
			
			Recursive<? extends R> next;
			
			Recursive(R data, Recursive<? extends R> next)
			{
				this.data = data;
				
				this.next = next;
			}
		}
		
		class Foo { }
		
		class Bar extends Foo { }
		
		class Baz extends Foo { }
		
		TypeToken<Recursive<Foo>> foo = new TypeToken<Recursive<Foo>>( ) { };
		TypeToken<Recursive<Bar>> bar = new TypeToken<Recursive<Bar>>( ) { };
		TypeToken<Recursive<Baz>> baz = new TypeToken<Recursive<Baz>>( ) { };
		
		Recursive<Foo> object = new Recursive<Foo>(new Bar( ), new Recursive<Baz>(new Baz( ), null));
		
		assertTrue(foo.isInstance(object));
		assertFalse(bar.isInstance(object));
		assertFalse(baz.isInstance(object));
	}
	
	public static void linkedList( )
	{
		class Zero { }
		class One extends Zero { }
		class Two extends One { }
		class Three extends Two { }
		class Four extends Three { }
		class Five extends Four { }
		class Six extends Five { }
		class Seven extends Six { }
		class Eight extends Seven { }
		class Nine extends Eight { }
		
		TypeToken<List<Zero>> zeroList = new TypeToken<List<Zero>>( ) { };
		TypeToken<List<One>> oneList = new TypeToken<List<One>>( ) { };
		TypeToken<List<Two>> twoList = new TypeToken<List<Two>>( ) { };
		TypeToken<List<Three>> threeList = new TypeToken<List<Three>>( ) { };
		TypeToken<List<Four>> fourList = new TypeToken<List<Four>>( ) { };
		TypeToken<List<Five>> fiveList = new TypeToken<List<Five>>( ) { };
		TypeToken<List<Six>> sixList = new TypeToken<List<Six>>( ) { };
		TypeToken<List<Seven>> sevenList = new TypeToken<List<Seven>>( ) { };
		TypeToken<List<Eight>> eightList = new TypeToken<List<Eight>>( ) { };
		TypeToken<List<Nine>> nineList = new TypeToken<List<Nine>>( ) { };
		
		LinkedList<Zero> list = new LinkedList<Zero>( );
		
		list.add(new One( ));
		list.add(new Two( ));
		list.add(new Three( ));
		list.add(new Four( ));
		list.add(new Five( ));
		list.add(new Six( ));
		list.add(new Seven( ));
		list.add(new Eight( ));
		list.add(new Zero( ));
		list.add(new Nine( ));
		
		assertFalse(oneList.isInstance(list));
		assertFalse(twoList.isInstance(list));
		assertFalse(threeList.isInstance(list));
		assertFalse(fourList.isInstance(list));
		assertFalse(fiveList.isInstance(list));
		assertFalse(sixList.isInstance(list));
		assertFalse(sevenList.isInstance(list));
		assertFalse(eightList.isInstance(list));
		assertFalse(nineList.isInstance(list));
		
		assertTrue(zeroList.isInstance(list));
	}
	
	public static void treeMap( )
	{
		class Foo { }
		
		class Bar extends Foo { }
		
		class Baz extends Foo { }
		
		TypeToken<Map<Integer, Foo>> fooMap = new TypeToken<Map<Integer, Foo>>( ) { };
		TypeToken<Map<Integer, Bar>> barMap = new TypeToken<Map<Integer, Bar>>( ) { };
		TypeToken<Map<Integer, Baz>> bazMap = new TypeToken<Map<Integer, Baz>>( ) { };
		
		TreeMap<Integer, Foo> map = new TreeMap<Integer, Foo>( );
		
		map.put(valueOf(1), new Foo( ));
		map.put(valueOf(2), new Bar( ));
		map.put(valueOf(3), new Baz( ));
		
		assertTrue(fooMap.isInstance(map));
		assertFalse(barMap.isInstance(map));
		assertFalse(bazMap.isInstance(map));
	}
	
	public static void genericArrayField( )
	{
		class Clazz<C>
		{
			@SuppressWarnings("unused")
			C[ ][ ] field;
			
			Clazz(C[ ][ ] arg)
			{
				field = arg;
			}
		}
		
		class Foo { }
		
		class Bar extends Foo { }
		
		class Baz extends Foo { }
		
		TypeVariable<?> C = Clazz.class.getTypeParameters( )[0];
		
		Clazz<Foo> object = new Clazz<Foo>(new Foo[ ][ ] {{new Foo( )}, {new Bar( ), new Baz( )}});
		
		assertEquals(typeOf(object).getTypeArgument(C).getRawType( ), Foo.class);
	}
	
	public static void arrayDeque( )
	{
		class Zero { }
		class One extends Zero { }
		class Two extends One { }
		class Three extends Two { }
		class Four extends Three { }
		class Five extends Four { }
		class Six extends Five { }
		class Seven extends Six { }
		class Eight extends Seven { }
		class Nine extends Eight { }
		
		TypeToken<ArrayDeque<Zero>> zeroDeque = new TypeToken<ArrayDeque<Zero>>( ) { };
		TypeToken<ArrayDeque<One>> oneDeque = new TypeToken<ArrayDeque<One>>( ) { };
		TypeToken<ArrayDeque<Two>> twoDeque = new TypeToken<ArrayDeque<Two>>( ) { };
		TypeToken<ArrayDeque<Three>> threeDeque = new TypeToken<ArrayDeque<Three>>( ) { };
		TypeToken<ArrayDeque<Four>> fourDeque = new TypeToken<ArrayDeque<Four>>( ) { };
		TypeToken<ArrayDeque<Five>> fiveDeque = new TypeToken<ArrayDeque<Five>>( ) { };
		TypeToken<ArrayDeque<Six>> sixDeque = new TypeToken<ArrayDeque<Six>>( ) { };
		TypeToken<ArrayDeque<Seven>> sevenDeque = new TypeToken<ArrayDeque<Seven>>( ) { };
		TypeToken<ArrayDeque<Eight>> eightDeque = new TypeToken<ArrayDeque<Eight>>( ) { };
		TypeToken<ArrayDeque<Nine>> nineDeque = new TypeToken<ArrayDeque<Nine>>( ) { };
		
		ArrayDeque<Zero> deque = new ArrayDeque<Zero>( );
		
		deque.add(new One( ));
		deque.add(new Two( ));
		deque.add(new Three( ));
		deque.add(new Four( ));
		deque.add(new Five( ));
		deque.add(new Zero( ));
		deque.add(new Six( ));
		deque.add(new Seven( ));
		deque.add(new Eight( ));
		deque.add(new Nine( ));
		
		assertFalse(oneDeque.isInstance(deque));
		assertFalse(twoDeque.isInstance(deque));
		assertFalse(threeDeque.isInstance(deque));
		assertFalse(fourDeque.isInstance(deque));
		assertFalse(fiveDeque.isInstance(deque));
		assertFalse(sixDeque.isInstance(deque));
		assertFalse(sevenDeque.isInstance(deque));
		assertFalse(eightDeque.isInstance(deque));
		assertFalse(nineDeque.isInstance(deque));
		
		assertTrue(zeroDeque.isInstance(deque));
	}
	
	@Test(enabled=true, groups="debug")
	public static void unambiguousInterfaceSuperType( )
	{
		class Clazz<C>
		{
			@SuppressWarnings("unused")
			C[ ] field;
			
			Clazz(C[ ] arg)
			{
				field = arg;
			}
		}
		
		@SuppressWarnings("serial")
		class Foo implements Serializable, Cloneable { }
		
		class Bar implements Cloneable { }
		
		TypeVariable<?> C = Clazz.class.getTypeParameters( )[0];
		
		Clazz<Cloneable> instance = new Clazz<Cloneable>(new Cloneable[ ] {new Foo( ), new Bar( )});
		
		assertEquals(typeOf(instance).getRawType(), Clazz.class);
		assertEquals(typeOf(instance).getTypeArgument(C).getRawType(), Cloneable.class);
	}
}