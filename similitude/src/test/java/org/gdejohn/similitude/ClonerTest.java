package org.gdejohn.similitude;

import static java.util.Arrays.deepEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class ClonerTest
{
	static final Logger ROOT_LOGGER;
	
	static
	{
		ROOT_LOGGER = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	}
	
	static
	{
		ROOT_LOGGER.setLevel(Level.WARN);
	}
	
	@Test
	public void testNull( )
	{
		assertNull(new Cloner( ).toClone(null));
	}
	
	@Test
	public void testByte( )
	{
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
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testShortWrapper( )
	{
		Short original = new Builder( ).instantiate(Short.class);
		Short clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testIntWrapper( )
	{
		Integer original = new Builder( ).instantiate(Integer.class);
		Integer clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testLongWrapper( )
	{
		Long original = new Builder( ).instantiate(Long.class);
		Long clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testFloatWrapper( )
	{
		Float original = new Builder( ).instantiate(Float.class);
		Float clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original, 0f);
	}
	
	@Test
	public void testDoubleWrapper( )
	{
		Double original = new Builder( ).instantiate(Double.class);
		Double clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original, 0d);
	}
	
	@Test
	public void testCharWrapper( )
	{
		Character original = new Builder( ).instantiate(Character.class);
		Character clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testBooleanWrapper( )
	{
		Boolean original = new Builder( ).instantiate(Boolean.class);
		Boolean clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testString( )
	{
		String original = new Builder( ).instantiate(String.class);
		String clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
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
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testArray( )
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
				assertEquals(clone[index], original[index]);
			}
		}
	}
	
	@Test
	public void testArrayContainingItself( )
	{
		Object[ ][ ] original = new Object[1][1];
		
		original[0][0] = original;
		
		Object[ ][ ] clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertNotSame(clone[0], original[0]);
		assertNotSame(clone[0][0], original[0][0]);
		assertNotSame(clone[0][0], original);
		assertSame(original[0][0], original);
		assertSame(clone[0][0], clone);
	}
	
	public static final class Immutable2
	{
		private final String field;
		
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
	public void testRegister( )
	{
		Cloner cloner = new Cloner( );
		
		assertTrue(cloner.register(Immutable2.class));
	}
	
	@Test
	public void testReregister( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable2.class);
		
		assertFalse(cloner.register(Immutable2.class));
	}
	
	@Test
	public void testRegisterDefault( )
	{
		Cloner cloner = new Cloner( );
		
		assertFalse(cloner.register(String.class));
	}
	
	@Test
	public void testReset( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable2.class);
		
		assertTrue(cloner.reset( ));
		assertTrue(cloner.register(Immutable2.class));
	}
	
	@Test
	public void testImmutable( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable2.class);
		Immutable2 original = new Immutable2("xyzzy");
		Immutable2 clone = cloner.toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	public static class Superclass2
	{
		public String field;
		
		public Superclass2( )
		{
			this.field = "";
		}
		
		public Superclass2(String arg)
		{
			this.field = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Superclass2)
			{
				return this.field.equals(((Superclass2)that).field);
			}
			else
			{
				return false;
			}
		}
	}
	
	public static class Subclass extends Superclass2
	{
		public Subclass(String arg)
		{
			this.field = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Subclass)
			{
				return this.field.equals(((Subclass)that).field);
			}
			else
			{
				return false;
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
		
		assertSame(clone.field, original.field);
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testAddDefaultSubclass( )
	{
		Cloner cloner = new Cloner( );
		Subclass original = new Subclass("xyzzy");
		cloner.register(Superclass2.class, original);
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
		private class Inner
		{
			private final String STRING;
			
			private Inner( )
			{
				STRING = Outer2.this.STRING_HOLDER.field;
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
		
		assertEquals(list.add(null), (boolean)builder.getDefault(boolean.class));
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
			InterfaceParam original = new InterfaceParam(new Impl( ));
			InterfaceParam clone = new Cloner( ).toClone(original);
			
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
			ROOT_LOGGER.setLevel(Level.WARN);
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
		ROOT_LOGGER.setLevel(Level.DEBUG);
		
		try
		{
			new Builder( ).instantiate(Test2.class);
		}
		finally
		{
			ROOT_LOGGER.setLevel(Level.WARN);
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
	
	@Test
	public void testEverything( )
	{
		Outer.Inner original = Outer.SECOND_CONSTANT.new Inner((byte)1);
		
		Outer.Inner clone = new Cloner( ).toClone(original);

		assertEquals(clone, original);
		assertTrue(clone.isDistinctFrom(original));
	}
}