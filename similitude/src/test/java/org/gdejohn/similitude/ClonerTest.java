package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.util.Arrays.deepEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.gdejohn.similitude.TypeToken.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("javadoc")
public class ClonerTest
{
	static final Logger ROOT_LOGGER;
	
	static
	{
		ROOT_LOGGER = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		
		ROOT_LOGGER.setLevel(WARN);
	}
	
	//@Test
	public void testTemplate( )
	{
		try
		{
			//ROOT_LOGGER.setLevel(DEBUG);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public void testNonGenericClassTypeToken( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			String instance = "\"xyzzy\"";
			Class<? extends String> expected = instance.getClass( );
			TypeToken<? extends String> token = typeOf(instance);
			Class<? extends String> actual = token.getRawType( );
			
			assertEquals(actual, expected);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	interface Test1<E>
	{
		E foo( );
		<T> T bar(T arg);
		<T> Test1<T> baz(List<T> arg);
		<T> Test1<T> spam(T arg);
		<T> T ham(Test8<T> arg);
		<T> T lam(Test8<Test8<Test8<Test8<T>>>> arg);
	}
	
	interface Boogaloo<B>
	{
		B method( );
	}
	
	static class Enclosing<E>
	{
		E field;
		
		Enclosing(E arg)
		{
			field = arg;
		}
		
		class Nested implements Boogaloo<E>
		{
			@Override
			public E method( )
			{
				return Enclosing.this.field;
			}
			
			class DoubleNested
			{
				E field = Enclosing.this.field;
			}
		}
	}
	
	@Test
	public void testEnclosingGeneric( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			String s = "xyzzy";
			assertEquals(typeOf(new Enclosing<String>(s).new Nested( ).new DoubleNested( )).toString( ), "ClonerTest.Enclosing<String>.Nested.DoubleNested");
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
	public void testFindTypeArgument( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			Method method = Test1.class.getDeclaredMethod("lam", Test8.class);
			
			System.out.println(TypeToken.traceTypeVariable((TypeVariable<?>)method.getGenericReturnType( ), method.getGenericParameterTypes( )[0]));
		}
		catch (Exception e)
		{
			fail("", e);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	static class Test8<G>
	{
		G field;
		
		Test8(G arg)
		{
			field = arg;
		}
	}
	
	static class Test3
	{
		String field;
		
		Test3(Test1<String> arg)
		{
			field = arg.baz(Arrays.asList(arg.bar("").toUpperCase( ))).foo( ).toUpperCase( );
		}
		
		Test3(List<String>[ ] arg)
		{
			arg[0].get(0).toUpperCase( );
		}
	}
	
	static class Test9<E>
	{
		Test9(Test1<E> arg)
		{
			
		}
	}
	
	static Test9<String> foobar( )
	{
		return null;
	}
	
	@Test
	public void testTypeToken( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			Constructor<?> cons = Test9.class.getDeclaredConstructor(Test1.class);
			
			TypeToken<?> token = typeOf(cons.getGenericParameterTypes( )[0], typeOf(ClonerTest.class.getDeclaredMethod("foobar").getGenericReturnType( ), null));
			
			assertEquals(token.getTypeArgument(Test1.class.getTypeParameters( )[0]).getRawType( ), String.class);
		}
		catch (Exception e)
		{
			fail("", e);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	//@Test
	public void testTypeLiteral( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			assertEquals(typeOf((Type)String.class).getRawType( ), String.class);
			
			Constructor<?> cons = Test3.class.getDeclaredConstructor(Test1.class);
			
			TypeLiteral<?> test1 = TypeLiteral.getTypeOf(cons.getGenericParameterTypes( )[0]);
			
			assertEquals(test1.getRawType( ), Test1.class);
			assertEquals(Test1.class.getTypeParameters( ).length, 1);
			assertEquals(test1.getTypeArgument(Test1.class.getTypeParameters( )[0]).getRawType( ), String.class);
			assertEquals(TypeLiteral.getTypeOf(Test1.class.getMethod("foo").getGenericReturnType( ), test1).getRawType( ), String.class);
			assertEquals(TypeLiteral.getTypeOf(Test1.class.getMethod("bar", Object.class).getGenericReturnType( ), "").getRawType( ), String.class);
			
			test1 = TypeLiteral.getTypeOf(Test1.class.getMethod("spam", Object.class).getGenericReturnType( ), "");
			
			assertEquals(test1.getRawType( ), Test1.class);
			assertEquals(test1.getTypeArgument(Test1.class.getTypeParameters( )[0]).getRawType( ), String.class);
			
			test1 = TypeLiteral.getTypeOf(Test1.class.getMethod("ham", Test8.class).getGenericReturnType( ), new Test8<String>("xyzzy"));
			
			assertEquals(test1.getRawType( ), String.class);
			
			//test1 = getTypeOf(Test1.class.getMethod("baz", List.class).getGenericReturnType( ), Arrays.asList(""));
			
			//assertEquals(test1.getRawType( ), Test1.class);
			//assertEquals(test1.getArgumentFor(Test1.class.getTypeParameters( )[0]).getRawType( ), String.class);
			
			//new TypeRef<Object>(Test3.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0]);
			
			//TypeRef<?> ref = new TypeRef<Object>(Test1.class.getMethod("bar", Object.class).getGenericReturnType( ));
			
			//ParameterizedType type = (ParameterizedType)ref.getType( );
			
			//assertEquals(type.getRawType( ), List.class);
			//assertEquals(type.getActualTypeArguments( ).length, 1);
			//assertEquals(type.getActualTypeArguments( )[0], String.class);
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
	public void testNull( )
	{
		assertNull(new Cloner( ).toClone(null));
	}
	
	@Test
	public void testByte( )
	{
		ROOT_LOGGER.setLevel(DEBUG);
		
		byte original = new Builder( ).instantiate(byte.class);
		byte clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testShort( )
	{
		short original = new Builder( ).instantiate(short.class);
		short clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testInt( )
	{
		int original = new Builder( ).instantiate(int.class);
		int clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testLong( )
	{
		long original = new Builder( ).instantiate(long.class);
		long clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testFloat( )
	{
		float original = new Builder( ).instantiate(float.class);
		float clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original, 0f);
	}
	
	@Test
	public void testDouble( )
	{
		double original = new Builder( ).instantiate(double.class);
		double clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original, 0d);
	}
	
	@Test
	public void testChar( )
	{
		char original = new Builder( ).instantiate(char.class);
		char clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testBoolean( )
	{
		boolean original = new Builder( ).instantiate(boolean.class);
		boolean clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testByteWrapper( )
	{
		Byte original = new Builder( ).instantiate(Byte.class);
		Byte clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testShortWrapper( )
	{
		Short original = new Builder( ).instantiate(Short.class);
		Short clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testIntWrapper( )
	{
		Integer original = new Builder( ).instantiate(Integer.class);
		Integer clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testLongWrapper( )
	{
		Long original = new Builder( ).instantiate(Long.class);
		Long clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testFloatWrapper( )
	{
		Float original = new Builder( ).instantiate(Float.class);
		Float clone = new Cloner( ).toClone(original);

		assertEquals(clone, original, 0.0f);
		assertSame(clone, original);
	}
	
	@Test
	public void testDoubleWrapper( )
	{
		Double original = new Builder( ).instantiate(Double.class);
		Double clone = new Cloner( ).toClone(original);

		assertEquals(clone, original, 0.0d);
		assertSame(clone, original);
	}
	
	@Test
	public void testCharWrapper( )
	{
		Character original = new Builder( ).instantiate(Character.class);
		Character clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testBooleanWrapper( )
	{
		Boolean original = new Builder( ).instantiate(Boolean.class);
		Boolean clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testString( )
	{
		String original = new Builder( ).instantiate(String.class);
		String clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	public enum Enum
	{
		CONSTANT
	}
	
	@Test
	public void testEnum( )
	{
		Enum original = Enum.CONSTANT;
		Enum clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertSame(clone, original);
	}
	
	@Test
	public void testPrimitiveArray( )
	{
		byte[ ] original = {0, 1, 2, 3};
		byte[ ] clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testMultidimensionalArray( )
	{
		byte[ ][ ] original = {{0, 1, 2}, null, {3, 4, 5, 6}};
		byte[ ][ ] clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone.length, original.length);
		
		for (int index = 0; index < original.length; index++)
		{
			if (original[index] == null)
			{
				assertNull(clone[index]);
			}
			else
			{
				assertNotSame(clone[index], original[index]);
				assertEquals(clone[index].length, original[index].length);
				assertEquals(clone[index], original[index]);
			}
		}
	}
	
	@Test
	public void testArrayContainingItself( )
	{
		Object[ ][ ] original = new Object[2][1];
		
		original[0][0] = original;
		original[1] = original;
		
		Object[ ][ ] clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertNotSame(clone[0], original[0]);
		assertNotSame(clone[0][0], original[0][0]);
		assertNotSame(clone[1], original[1]);
		assertNotSame(clone[0][0], original);
		assertNotSame(clone[1], original);
		assertNotSame(clone, original[0][0]);
		assertNotSame(clone, original[1]);
		assertSame(original[0][0], original);
		assertSame(original[1], original);
		assertSame(clone[0][0], clone);
		assertSame(clone[1], clone);
	}
	
	static final class Immutable2
	{
		final String field;
		
		public Immutable2(String argument)
		{
			this.field = argument;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Immutable2)
			{
				return this.field.equals(((Immutable2)that).field);
			}
			else
			{
				return false;
			}
		}
	}
	
	@Test
	public void testRegisteredImmutable( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			Builder builder = cloner.BUILDER;
			
			//ROOT_LOGGER.setLevel(DEBUG);
			
			assertFalse(cloner.reset( ));
			assertFalse(cloner.register(typeOf(String.class)));
			assertTrue(cloner.register(typeOf(Immutable2.class)));
			assertFalse(cloner.register(typeOf(Immutable2.class)));
			assertTrue(cloner.reset( ));
			assertFalse(cloner.reset( ));
			assertTrue(cloner.register(typeOf(Immutable2.class)));
			
			Immutable2 original = new Immutable2("xyzzy");
			Immutable2 clone = cloner.toClone(original);
			
			assertEquals(clone, original);
			assertSame(clone, original);
			
			Immutable2 instance = builder.instantiate(Immutable2.class);
			
			assertNull(builder.addDefault(typeOf(Immutable2.class), original));
			
			Immutable2 defaultValue = builder.instantiate(Immutable2.class);
			
			assertEquals(defaultValue, original);
			assertSame(defaultValue, original);
			assertNotEquals(defaultValue, instance, defaultValue.field + " " + instance.field);
			assertNotSame(defaultValue, instance);
		}
		catch (Exception e)
		{
			ROOT_LOGGER.setLevel(WARN);
			
			fail("Failed.", e);
		}
	}
	
	static final class Test4
	{
		public final int intField = 0;
	}
	
	//@Test
	public void testDetermineImmutable( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			TypeToken2<?> type = new TypeToken2<Object>(Test4.class, true);
			
			for (TypeToken<?> primitive : Builder.WRAPPERS.keySet( ))
			{
				assertTrue(new TypeToken2<Object>(primitive.getRawType( ), true).isImmutable( ));
			}
			
			for (TypeToken<?> immutable : type.IMMUTABLE)
			{
				assertTrue(new TypeToken2<Object>(immutable.getRawType( ), true).isImmutable( ));
			}
			
			assertFalse(type.IMMUTABLE.contains(Test4.class));
			assertTrue(type.isImmutable( ));
			assertTrue(type.IMMUTABLE.contains(Test4.class));
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	static class Superclass2
	{
		public String stringField;
		
		public Superclass2( )
		{
			this.stringField = "";
		}
		
		public Superclass2(String arg)
		{
			this.stringField = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Superclass2)
			{
				return this.stringField.equals(((Superclass2)that).stringField);
			}
			else
			{
				return false;
			}
		}
	}
	
	static class Subclass extends Superclass2
	{
		Subclass(String arg)
		{
			this.stringField = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Subclass)
			{
				return this.stringField.equals(((Subclass)that).stringField);
			}
			else
			{
				return false;
			}
		}
		
		class Inner
		{
			Inner( )
			{
				Subclass.this.stringField =
				(
					Subclass.this.stringField.toUpperCase( )
				);
			}
			
			Subclass getEnclosing( )
			{
				return Subclass.this;
			}
			
			@Override
			public boolean equals(Object that)
			{
				if (that instanceof Inner)
				{
					return getEnclosing( ).equals(((Inner)that).getEnclosing( ));
				}
				else
				{
					return false;
				}
			}
		}
	}
	
	@Test
	public void testNullaryConstructor( )
	{
		Superclass2 original = new Superclass2("xyzzy");
		Superclass2 clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testInheritedField( )
	{
		Subclass original = new Subclass("xyzzy");
		Subclass clone = new Cloner( ).toClone(original);
		
		assertSame(clone.stringField, original.stringField);
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testFieldInEnclosingClass( )
	{
		Subclass.Inner original = new Subclass("xyzzy").new Inner( );
		Subclass.Inner clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertNotSame(clone.getEnclosing( ), original.getEnclosing( ));
		assertEquals(clone, original);
	}
	
	@Test
	public void testAddDefaultSubclass( )
	{
		Cloner cloner = new Cloner( );
		Subclass original = new Subclass("xyzzy");
		cloner.register(typeOf(Superclass2.class), original);
		Superclass2 clone = cloner.toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testStringBuilder( )
	{
		StringBuilder original = new StringBuilder("xyzzy");
		StringBuilder clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone.toString( ), original.toString( ));
	}
	
	public static class Picky
	{
		private final int NON_ZERO;
		
		public Picky(int arg)
		{
			if (arg == 0)
			{
				throw
				(
					new IllegalArgumentException
					(
						String.format("%d (must be non-zero)", arg)
					)
				);
			}
			else
			{
				NON_ZERO = arg;
			}
		}
		
		public int getNumber( )
		{
			return NON_ZERO;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Picky)
			{
				return this.NON_ZERO == ((Picky)that).NON_ZERO;
			}
			else
			{
				return false;
			}
		}
	}
	
	public static class TakesPicky
	{
		private final Subclass STRING_HOLDER;
		
		public TakesPicky(Picky number, int addend)
		{
			STRING_HOLDER =
			(
				new Subclass
				(
					String.valueOf
					(
						addend + (number == null ? 0 : number.getNumber( ))
					)
				)
			);
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof TakesPicky)
			{
				return
				(
					this.STRING_HOLDER.equals(((TakesPicky)that).STRING_HOLDER)
				);
			}
			else
			{
				return false;
			}
		}
	}
	
	@Test
	public void testFailToInstantiateConstructorArg( )
	{
		TakesPicky original = new TakesPicky(new Picky(3), 2);
		TakesPicky clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	public static class Outer2
	{
		private final Subclass STRING_HOLDER;
		
		public Outer2(Subclass arg)
		{
			STRING_HOLDER = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Outer2)
			{
				return
				(
					this.STRING_HOLDER.equals(((Outer2)that).STRING_HOLDER)
				);
			}
			else
			{
				return false;
			}
		}
		
		private class Inner
		{
			private final String STRING;
			
			private Inner( )
			{
				STRING = Outer2.this.STRING_HOLDER.stringField;
			}
			
			public Outer2 get( )
			{
				return Outer2.this;
			}
			
			@Override
			public boolean equals(Object that)
			{
				if (that instanceof Inner)
				{
					return
					(
						this.STRING.equals(((Inner)that).STRING)
					);
				}
				else
				{
					return false;
				}
			}
		}
	}
	
	@Test
	public void testInnerClass( )
	{
		Outer2.Inner original = new Outer2(new Subclass("xyzzy")).new Inner( );
		Outer2.Inner clone = new Cloner( ).toClone(original);
		
		Outer2 originalOuter = original.get( );
		Outer2 cloneOuter = clone.get( );
		
		assertNotSame(clone, original);
		assertNotSame(cloneOuter, originalOuter);
		assertEquals(cloneOuter, originalOuter);
		assertNotSame(cloneOuter.STRING_HOLDER, originalOuter.STRING_HOLDER);
		assertEquals(cloneOuter.STRING_HOLDER, originalOuter.STRING_HOLDER);
		assertEquals(clone, original);
	}
	
	public static class ArrayField
	{
		Number[ ] numArray;
		
		public ArrayField( )
		{
			numArray = new Number[2];
		}
		
		public ArrayField(Number[ ] arg)
		{
			numArray = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof ArrayField)
			{
				return
				(
					Arrays.equals(this.numArray, ((ArrayField)that).numArray)
				);
			}
			else
			{
				return false;
			}
		}
	}
	
	@Test
	public void testCovariantArrays( )
	{
		ArrayField original = new ArrayField(new Integer[ ] {0, 1});
		ArrayField clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertNotSame(clone.numArray, original.numArray);
		assertEquals(clone, original);
	}
	
	@Test
	public void testInterface( )
	{
		Builder builder = new Builder( );
		
		List<?> list = builder.instantiate(List.class);
		
		assertEquals(list.add(null), (boolean)builder.getDefault(typeOf(Boolean.class)));
	}
	
	@Test
	public void testListOfStrings( )
	{
		List<String> original = Arrays.asList("foo", "bar", "baz");
		List<String> clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
		assertNotSame(clone, original);
		
		clone.set(0, clone.get(0).toUpperCase( ));
		
		assertEquals(clone.get(0), "FOO");
		assertEquals(original.get(0), "foo");
	}
	
	interface GenericInterface<E>
	{
		E withArg(E arg);
		
		E withoutArg( );
	}
	
	static class Impl implements GenericInterface<String>
	{
		@Override
		public String withArg(String arg)
		{
			return arg;
		}
		
		@Override
		public String withoutArg( )
		{
			return "xyzzy";
		}
	}
	
	static class Impl2 implements GenericInterface<GenericInterface<String>>
	{
		@Override
		public GenericInterface<String> withArg(GenericInterface<String> arg)
		{
			return arg;
		}
		
		@Override
		public GenericInterface<String> withoutArg( )
		{
			return new Impl( );
		}
	}
	
	static class InterfaceParam
	{
		String stringField;
		
		public InterfaceParam(GenericInterface<String> arg)
		{
			stringField = arg.withArg(arg.withoutArg( )).toUpperCase( );
		}
		
		public InterfaceParam(GenericInterface<GenericInterface<String>> arg0, String arg1)
		{
			stringField = arg0.withArg(arg0.withoutArg( )).withArg(arg1).toUpperCase( );
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof InterfaceParam)
			{
				return
				(
					this.stringField.equals
					(
						((InterfaceParam)that).stringField
					)
				);
			}
			else
			{
				return false;
			}
		}
	}
	
	@Test
	public void testGenericInterface( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			ROOT_LOGGER.setLevel(DEBUG);
			
			InterfaceParam original = new InterfaceParam(new Impl( ));
			InterfaceParam clone = cloner.toClone(original);
			
			assertNotSame(clone, original);
			assertEquals(clone, original);
		}
		catch (Exception e)
		{
			fail("Test didn't complete normally.", e);
		}
	}
	
	@Test
	public void testNestedTypeParameters( )
	{
		//ROOT_LOGGER.setLevel(Level.DEBUG);
		
		try
		{
			InterfaceParam original = new InterfaceParam(new Impl2( ), "Hello, world!");
			InterfaceParam clone = new Cloner( ).toClone(original);
			
			assertNotSame(clone, original);
			assertEquals(clone, original);
		}
		catch (Exception e)
		{
			fail("Test didn't complete normally.", e);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	static class Foo<E>
	{
		E genericField;
		
		Foo(List<E> list)
		{
			genericField = list.get(0);
		}
		
		E getField( )
		{
			return genericField;
		}
	}
	
	static class Bar
	{
		Bar(Foo<String> arg)
		{
			arg.getField( ).toUpperCase( );
		}
	}
	
	@Test
	public void testParameterizedByTypeVariable( )
	{
		//ROOT_LOGGER.setLevel(DEBUG);
		
		try
		{
			new Builder( ).instantiate(Bar.class);
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
	
	interface ParameterizedReturnType<S>
	{
		List<S> getGenericList( );
		<T> List<T> declaredByMethod(T arg);
		<T> T foo(T arg);
	}
	
	static class Test5
	{
		Test5(ParameterizedReturnType<String> arg)
		{
			arg.getGenericList( ).get(0).toUpperCase( );
			//arg.foo(Arrays.asList("xyzzy")).get(0).toUpperCase( );
			//arg.declaredByMethod("xyzzy").get(0).toUpperCase( );
		}
	}
	
	@Test
	public void testParameterizedReturnType( )
	{
		//ROOT_LOGGER.setLevel(DEBUG);
		
		try
		{
			new Builder( ).instantiate(Test5.class);
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
	
	static <T extends Comparable<T>> Object local(T arg)
	{
		class Local
		{
			T field;
			
			Local(T arg)
			{
				field = arg;
			}
			
			@Override
			public boolean equals(Object that)
			{
				if (that instanceof Local)
				{
					return
					(
						this.field.compareTo(((Local)that).field) == 0
					);
				}
				else
				{
					return false;
				}
			}
		}
		
		return new Local(arg);
	}
	
	@Test
	public void testLocalClass( )
	{
		Cloner cloner = new Cloner( );
		
		ROOT_LOGGER.setLevel(DEBUG);
		
		Object original = local("xyzzy");
		Object clone = cloner.toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
		
		ROOT_LOGGER.setLevel(WARN);
	}
	
	interface ReturnsGenericArray
	{
		List<String>[ ][ ] foo( );
		<T> List<T>[ ][ ] bar(T arg);
		<T> T[ ][ ] baz(T arg);
	}
	
	@Test
	public void testReturnsGenericArray( )
	{
		try
		{
			//ROOT_LOGGER.setLevel(DEBUG);
			
			assertEquals(new Builder( ).instantiate(ReturnsGenericArray.class).foo( ).length, 0);
			assertEquals(new Builder( ).instantiate(ReturnsGenericArray.class).bar("").length, 0);
			assertEquals(new Builder( ).instantiate(ReturnsGenericArray.class).baz("").length, 0);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	interface VarargsInterface<E>
	{
		E foo(E... args);
		<T> T bar(T... args);
	}
	
	static class Test2
	{
		Test2(VarargsInterface<String> s)
		{
			s.foo("xyzzy").length( );
			s.bar(0L).compareTo(111111111111L);
		}
	}
	
	@Test
	public void testVarargsInterface( )
	{
		try
		{
			new Builder( ).instantiate(Test2.class);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	class Test6<E>
	{
		E field;
		
		Test6(E arg)
		{
			field = arg;
		}
	}
	
	interface Test7
	{
		<T> T foo(Test6<T> arg);
	}
	
	//@Test
	public void testResolveTypeVariable( )
	{
		try
		{
			ROOT_LOGGER.setLevel(DEBUG);
			
			Test6<String> arg = new Test6<String>("xyzzy");
			
			assertEquals(new TypeToken2<Object>(Object.class).resolve((TypeVariable<?>)Test7.class.getMethods( )[0].getGenericReturnType( ), arg).getRawType( ), String.class);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	private static class Mutable
	{
		private final byte[ ][ ] FINAL_FIELD;
		
		private Mutable(byte[ ]... args)
		{
			FINAL_FIELD = new byte[args.length][ ];
			
			for (int i = 0; i < args.length; i++)
			{
				FINAL_FIELD[i] = new byte[args[i].length];
				
				for (int j = 0; j < args[i].length; j++)
				{
					FINAL_FIELD[i][j] = args[i][j];
				}
			}
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Mutable)
			{
				return
				(
					deepEquals
					(
						this.FINAL_FIELD, ((Mutable)that).FINAL_FIELD
					)
				);
			}
			else
			{
				return false;
			}
		}
		
		public boolean isDistinctFrom(Mutable that)
		{
			if (this == that || this.FINAL_FIELD == that.FINAL_FIELD)
			{
				return false;
			}
			else
			{
				for (byte[ ] thisSubArray : this.FINAL_FIELD)
				{
					for (byte[ ] thatSubArray : that.FINAL_FIELD)
					{
						if (thisSubArray == thatSubArray)
						{
							return false;
						}
					}
				}
				
				for (byte[ ] thatSubArray : that.FINAL_FIELD)
				{
					for (byte[ ] thisSubArray : this.FINAL_FIELD)
					{
						if (thisSubArray == thatSubArray)
						{
							return false;
						}
					}
				}
				
				return true;
			}
		}
	}
	
	private static class Superclass
	{
		protected final Mutable INHERITED_FIELD;
		
		protected Superclass(byte[ ]... args)
		{
			INHERITED_FIELD = new Mutable(args);
		}
		
		@Override
		public boolean equals(Object arg)
		{
			if (arg instanceof Superclass)
			{
				Superclass that = (Superclass)arg;
				
				return this.INHERITED_FIELD.equals(that.INHERITED_FIELD);
			}
			else
			{
				return false;
			}
		}
		
		public boolean isDistinctFrom(Superclass that)
		{
			if (this != that)
			{
				if (this.INHERITED_FIELD.isDistinctFrom(that.INHERITED_FIELD))
				{
					return true;
				}
			}
			
			return false;
		}
	}

	private enum Outer
	{
		FIRST_CONSTANT((byte)1),
		SECOND_CONSTANT((byte)2);
		
		private final byte factor;
		
		private Outer(byte arg)
		{
			factor = arg;
		}
		
		private class Inner extends Superclass
		{
			private Inner(byte arg)
			{
				byte[ ][ ] array = INHERITED_FIELD.FINAL_FIELD;
				
				for (int i = 0; i < array.length; i++)
				{
					for (int j = 0; j < array[i].length; j++)
					{
						array[i][j] *= arg + factor;
					}
				}
			}
			
			private Outer getEnclosingInstance( )
			{
				return Outer.this;
			}
			
			@Override
			public boolean equals(Object arg)
			{
				if (arg instanceof Inner)
				{
					Inner that = (Inner)arg;
					
					Outer thisEnclosing = this.getEnclosingInstance( );
					Outer thatEnclosing = that.getEnclosingInstance( );
					
					if (thisEnclosing.equals(thatEnclosing))
					{
						return super.equals(arg);
					}
				}
				
				return false;
			}
		}
	}
	
	//@Test
	public void testEverything( )
	{
		Outer.Inner original = Outer.SECOND_CONSTANT.new Inner((byte)1);
		
		Outer.Inner clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
		assertTrue(clone.isDistinctFrom(original));
	}
	
	static <T, U, V extends Comparable<U> & Cloneable> void foo( )
	{
		
	}
}