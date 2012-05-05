package org.gdejohn.similitude;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.WARN;
import static java.lang.Boolean.valueOf;
import static java.lang.Byte.valueOf;
import static java.lang.Character.valueOf;
import static java.lang.Double.valueOf;
import static java.lang.Float.valueOf;
import static java.lang.Integer.valueOf;
import static java.lang.Long.valueOf;
import static java.lang.Short.valueOf;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.*;

import java.util.Arrays;

import org.testng.annotations.Test;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("javadoc")
public class ClonerTest
{
	private static final Logger ROOT_LOGGER;
	
	static
	{
		ROOT_LOGGER = (Logger)getLogger(ROOT_LOGGER_NAME);
		
		ROOT_LOGGER.setLevel(WARN);
	}
	
	@Test
	public static void baseCases( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			assertNull(cloner.toClone(null));
			
			byte b = (byte)4;
			Byte B = valueOf(b);
			assertEquals(cloner.toClone(b).byteValue( ), b);
			assertEquals(cloner.toClone(B), B);
			
			short s = (short)4;
			Short S = valueOf(s);
			assertEquals(cloner.toClone(s).shortValue( ), s);
			assertEquals(cloner.toClone(S), S);
			
			int i = (int)4;
			Integer I = valueOf(i);
			assertEquals(cloner.toClone(i).intValue( ), i);
			assertEquals(cloner.toClone(I), I);
			
			long l = 4L;
			Long L = valueOf(l);
			assertEquals(cloner.toClone(l).longValue( ), l);
			assertEquals(cloner.toClone(L), L);
			
			float f = 4.0f;
			Float F = valueOf(f);
			assertEquals(cloner.toClone(f).floatValue( ), f, 0.0f);
			assertEquals(cloner.toClone(F), F);
			
			double d = 4.0d;
			Double D = valueOf(d);
			assertEquals(cloner.toClone(d).doubleValue( ), d, 0.0d);
			assertEquals(cloner.toClone(D), D);
			
			char c = '4';
			Character C = valueOf(c);
			assertEquals(cloner.toClone(c).charValue( ), c);
			assertEquals(cloner.toClone(C), C);
			
			boolean bool = true;
			Boolean Bool = valueOf(bool);
			assertEquals(cloner.toClone(bool).booleanValue( ), bool);
			assertEquals(cloner.toClone(Bool), Bool);
			
			String str = "xyzzy";
			assertEquals(cloner.toClone(str), str);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	private enum EnumType
	{
		FIRST, SECOND
	}
	
	@Test
	public static void enumType( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			EnumType original = EnumType.SECOND;
			EnumType clone = cloner.toClone(original);
			
			assertEquals(clone, original);
			assertSame(clone, original);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void primitiveArray( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			byte[ ] original = {0, 1, 2, 3};
			byte[ ] clone = cloner.toClone(original);
			
			assertNotSame(clone, original);
			assertEquals(clone, original);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void multidimensionalArray( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			byte[ ][ ] original = {{0, 1, 2}, null, {3, 4, 5, 6}};
			byte[ ][ ] clone = cloner.toClone(original);
			
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
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void selfReferentialArray( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			Object[ ][ ] original = new Object[2][1];
			
			original[0][0] = original;
			original[1] = original;
			
			Object[ ][ ] clone = cloner.toClone(original);
			
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
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void stringBuilder( )
	{
		try
		{
			Cloner cloner = new Cloner( );
			
			// ROOT_LOGGER.setLevel(DEBUG);
			
			StringBuilder original = new StringBuilder("xyzzy");
			StringBuilder clone = cloner.toClone(original);
			
			assertNotSame(clone, original);
			assertEquals(clone.toString( ), original.toString( ));
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
	
	@Test
	public static void covariantArrays( )
	{
		class ArrayField
		{
			Number[ ] numArray;
			
			@SuppressWarnings("unused")
			ArrayField( )
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
		
		try
		{
			Cloner cloner = new Cloner( );
			
			ROOT_LOGGER.setLevel(DEBUG);
			
			ArrayField original = new ArrayField(new Integer[ ] {0, 1});
			ArrayField clone = cloner.toClone(original);
			
			assertNotSame(clone, original);
			assertNotSame(clone.numArray, original.numArray);
			assertEquals(clone, original);
		}
		finally
		{
			ROOT_LOGGER.setLevel(WARN);
		}
	}
}