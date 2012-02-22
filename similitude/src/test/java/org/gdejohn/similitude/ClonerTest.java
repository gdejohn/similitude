package org.gdejohn.similitude;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class ClonerTest
{
	private final byte BYTE = (byte)42;
	
	private final short SHORT = (short)42;
	
	private final int INT = 42;
	
	private final long LONG = 42l;
	
	private final float FLOAT = 42f;
	
	private final double DOUBLE = 42d;
	
	private final char CHAR = '\u0042';
	
	private final boolean BOOLEAN = true;
	
	private final String STRING = "xyzzy";
	
	@Test
	public void testNull( )
	{
		assertNull(new Cloner( ).toClone(null));
	}
	
	@Test
	public void testByte( )
	{
		byte original = BYTE;
		byte clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testShort( )
	{
		short original = SHORT;
		short clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testInt( )
	{
		int original = INT;
		int clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testLong( )
	{
		long original = LONG;
		long clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testFloat( )
	{
		float original = FLOAT;
		float clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original, 0f);
	}
	
	@Test
	public void testDouble( )
	{
		double original = DOUBLE;
		double clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original, 0d);
	}
	
	@Test
	public void testChar( )
	{
		char original = CHAR;
		char clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testBoolean( )
	{
		boolean original = BOOLEAN;
		boolean clone = new Cloner( ).toClone(original);
		
		assertEquals(clone, original);
	}
	
	@Test
	public void testByteWrapper( )
	{
		Byte original = Byte.valueOf(BYTE);
		Byte clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testShortWrapper( )
	{
		Short original = Short.valueOf(SHORT);
		Short clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testIntWrapper( )
	{
		Integer original = Integer.valueOf(INT);
		Integer clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testLongWrapper( )
	{
		Long original = Long.valueOf(LONG);
		Long clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testFloatWrapper( )
	{
		Float original = Float.valueOf(FLOAT);
		Float clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original, 0f);
	}
	
	@Test
	public void testDoubleWrapper( )
	{
		Double original = Double.valueOf(DOUBLE);
		Double clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original, 0d);
	}
	
	@Test
	public void testCharWrapper( )
	{
		Character original = Character.valueOf(CHAR);
		Character clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testBooleanWrapper( )
	{
		Boolean original = Boolean.valueOf(BOOLEAN);
		Boolean clone = new Cloner( ).toClone(original);
		
		assertSame(clone, original);
		assertEquals(clone, original);
	}
	
	@Test
	public void testString( )
	{
		String original = STRING;
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
		byte[ ][ ] original = {{0, 1}, {2, 3}};
		byte[ ][ ] clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone.length, original.length);
		
		for (int index = 0; index < original.length; index++)
		{
			assertNotSame(clone[index], original[index]);
			assertEquals(clone[index], original[index]);
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
		cloner.reset( );
		
		assertTrue(cloner.register(Immutable.class));
	}
	
	@Test
	public void testImmutable( )
	{
		Cloner cloner = new Cloner( );
		cloner.register(Immutable.class);
		Immutable original = new Immutable(STRING);
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
		NoArgs original = new NoArgs(STRING);
		NoArgs clone = new Cloner( ).toClone(original);
		
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
	
	public static class Subclass extends NoArgs
	{
		public Subclass( )
		{
			this.field = "";
		}
		
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
		Subclass original = new Subclass(STRING);
		Subclass clone = new Cloner( ).toClone(original);
		
		assertSame(clone.field, original.field);
		assertNotSame(clone, original);
		assertEquals(clone, original);
	}
}