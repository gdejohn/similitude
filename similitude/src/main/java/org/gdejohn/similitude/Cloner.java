/**
 * Utilities for creating and deep copying instances of arbitrary classes.
 */
package org.gdejohn.similitude;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Deep copying instances of arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public class Cloner
{
	private final Set<Class<?>> IMMUTABLE;
	
	{
		IMMUTABLE = new LinkedHashSet<Class<?>>( );
		
		IMMUTABLE.add(byte.class);
		IMMUTABLE.add(short.class);
		IMMUTABLE.add(int.class);
		IMMUTABLE.add(long.class);
		IMMUTABLE.add(float.class);
		IMMUTABLE.add(double.class);
		IMMUTABLE.add(char.class);
		IMMUTABLE.add(boolean.class);
		
		IMMUTABLE.add(Byte.class);
		IMMUTABLE.add(Short.class);
		IMMUTABLE.add(Integer.class);
		IMMUTABLE.add(Long.class);
		IMMUTABLE.add(Float.class);
		IMMUTABLE.add(Double.class);
		IMMUTABLE.add(Character.class);
		IMMUTABLE.add(Boolean.class);
		
		IMMUTABLE.add(String.class);
	}
	
	/**
	 * Creates a deep copy of the given object.
	 * 
	 * @param original The object to create a deep copy of.
	 * 
	 * @return A deep copy of {@code original}.
	 */
	public <T> T toClone(final T ORIGINAL)
	{
		final T CLONE;
		
		if (ORIGINAL == null)
		{
			CLONE = null;
		}
		else
		{
			final Class<?> CLASS = ORIGINAL.getClass( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(CLASS))
			{
				CLONE = ORIGINAL;
			}
			else
			{
				throw new IllegalArgumentException( );
			}
		}
		
		return CLONE;
	}
}