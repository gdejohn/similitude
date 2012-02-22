package org.gdejohn.similitude;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class ClonerTest
{
	enum Enum
	{
		CONSTANT
	}
	
	static final class Immutable
	{
		private final String field;
		
		public Immutable(String argument)
		{
			field = argument;
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
		byte clone = new Cloner( ).toClone(BYTE);
		
		assertEquals(BYTE, clone);
	}
	
	@Test
	public void testShort( )
	{
		short clone = new Cloner( ).toClone(SHORT);
		
		assertEquals(SHORT, clone);
	}
	
	@Test
	public void testInt( )
	{
		int clone = new Cloner( ).toClone(INT);
		
		assertEquals(INT, clone);
	}
	
	@Test
	public void testLong( )
	{
		long clone = new Cloner( ).toClone(LONG);
		
		assertEquals(LONG, clone);
	}
	
	@Test
	public void testFloat( )
	{
		float clone = new Cloner( ).toClone(FLOAT);
		
		assertEquals(FLOAT, clone, 0f);
	}
	
	@Test
	public void testDouble( )
	{
		double clone = new Cloner( ).toClone(DOUBLE);
		
		assertEquals(DOUBLE, clone, 0d);
	}
	
	@Test
	public void testChar( )
	{
		char clone = new Cloner( ).toClone(CHAR);
		
		assertEquals(CHAR, clone);
	}
	
	@Test
	public void testBoolean( )
	{
		boolean clone = new Cloner( ).toClone(BOOLEAN);
		
		assertEquals(BOOLEAN, clone);
	}
	
	@Test
	public void testByteWrapper( )
	{
		Byte original = Byte.valueOf(BYTE);
		Byte clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testShortWrapper( )
	{
		Short original = Short.valueOf(SHORT);
		Short clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testIntWrapper( )
	{
		Integer original = Integer.valueOf(INT);
		Integer clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testLongWrapper( )
	{
		Long original = Long.valueOf(LONG);
		Long clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testFloatWrapper( )
	{
		Float original = Float.valueOf(FLOAT);
		Float clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone, 0f);
	}
	
	@Test
	public void testDoubleWrapper( )
	{
		Double original = Double.valueOf(DOUBLE);
		Double clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone, 0d);
	}
	
	@Test
	public void testCharWrapper( )
	{
		Character original = Character.valueOf(CHAR);
		Character clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testBooleanWrapper( )
	{
		Boolean original = Boolean.valueOf(BOOLEAN);
		Boolean clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
	}
	
	@Test
	public void testString( )
	{
		String clone = new Cloner( ).toClone(STRING);
		
		assertEquals(STRING, clone);
	}
	
	@Test
	public void testEnum( )
	{
		Enum original = Enum.CONSTANT;
		Enum clone = new Cloner( ).toClone(original);
		
		assertEquals(original, clone);
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
		
		assertEquals(original, clone);
	}
}