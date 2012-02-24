package org.gdejohn.similitude;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

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
	
	public static final class Immutable
	{
		private final String field;
		
		public Immutable(String argument)
		{
			this.field = argument;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Immutable)
			{
				return this.field.equals(((Immutable)that).field);
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
		
		assertTrue(cloner.register(Immutable.class));
	}
	
	@Test
	public void testReregister( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable.class);
		
		assertFalse(cloner.register(Immutable.class));
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
		cloner.register(Immutable.class);
		
		assertTrue(cloner.reset( ));
		assertTrue(cloner.register(Immutable.class));
	}
	
	@Test
	public void testImmutable( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable.class);
		Immutable original = new Immutable("xyzzy");
		Immutable clone = cloner.toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	public static class NoArgs
	{
		public String field;
		
		public NoArgs( )
		{
			this.field = "";
		}
		
		public NoArgs(String arg)
		{
			this.field = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof NoArgs)
			{
				return this.field.equals(((NoArgs)that).field);
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
		NoArgs original = new NoArgs("xyzzy");
		NoArgs clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	public static class Subclass extends NoArgs
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
		cloner.register(NoArgs.class, original);
		NoArgs clone = cloner.toClone(original);
		
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
	
	public static class Outer
	{
		private class Inner
		{
			private final String STRING;
			
			private Inner( )
			{
				STRING = Outer.this.STRING_HOLDER.field;
			}
			
			public Outer get( )
			{
				return Outer.this;
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
		
		public Outer(Subclass arg)
		{
			STRING_HOLDER = arg;
		}
		
		@Override
		public boolean equals(Object that)
		{
			if (that instanceof Outer)
			{
				return
				(
					this.STRING_HOLDER.equals(((Outer)that).STRING_HOLDER)
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
		Outer.Inner original = new Outer(new Subclass("xyzzy")).new Inner( );
		Outer.Inner clone = new Cloner( ).toClone(original);
		
		Outer originalOuter = original.get( );
		Outer cloneOuter = clone.get( );
		
		assertNotSame(clone, original);
		assertNotSame(cloneOuter, originalOuter);
		assertEquals(cloneOuter, originalOuter);
		assertNotSame(cloneOuter.STRING_HOLDER, originalOuter.STRING_HOLDER);
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
		ROOT_LOGGER.setLevel(Level.DEBUG);
		
		ArrayField original = new ArrayField(new Integer[ ] {0, 1});
		ArrayField clone = new Cloner( ).toClone(original);
		
		ROOT_LOGGER.setLevel(Level.WARN);
		
		assertNotSame(clone, original);
		assertNotSame(clone.numArray, original.numArray);
		assertEquals(clone, original);
	}
}