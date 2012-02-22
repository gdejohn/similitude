/**
 * Utilities for creating and deep copying instances of arbitrary classes.
 */
package org.gdejohn.similitude;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.set;

import java.lang.reflect.Array;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Deep copying instances of arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public class Cloner
{
	/**
	 * Immutable types, can be shallow-copied.
	 */
	private final Set<Class<?>> IMMUTABLE = new LinkedHashSet<Class<?>>( );
	
	/**
	 * Registers a given class as immutable, so that it will be shallow-copied.
	 * 
	 * @param CLASS The class to register as immutable.
	 * 
	 * @return {@code true} if {@code CLASS} wasn't already registered.
	 */
	public boolean register(final Class<?> CLASS)
	{
		return IMMUTABLE.add(CLASS);
	}
	
	/**
	 * Resets {@link #IMMUTABLE} to default values.
	 * 
	 * After this method returns, {@code IMMUTABLE} will contain the primitive
	 * types, their respective wrappers, and {@code String}. Any other values
	 * that were previously added by the user are removed.
	 */
	public void reset( )
	{
		IMMUTABLE.clear( );
		
		this.register(byte.class);
		this.register(short.class);
		this.register(int.class);
		this.register(long.class);
		this.register(float.class);
		this.register(double.class);
		this.register(char.class);
		this.register(boolean.class);
		
		this.register(Byte.class);
		this.register(Short.class);
		this.register(Integer.class);
		this.register(Long.class);
		this.register(Float.class);
		this.register(Double.class);
		this.register(Character.class);
		this.register(Boolean.class);
		
		this.register(String.class);
	}
	
	{ // Instance initializer, executes at the beginning of every constructor.
		this.reset( );
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
			// This will always work, since ORIGINAL is of type T.
			@SuppressWarnings("unchecked")
			final Class<T> CLASS = (Class<T>)ORIGINAL.getClass( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(CLASS))
			{ // Safe to shallow-copy.
				CLONE = ORIGINAL;
			}
			else if (CLASS.isArray( ))
			{ // Recursively clone each element.
				final int LENGTH = Array.getLength(ORIGINAL);
				final Class<?> COMPONENT_TYPE = CLASS.getComponentType( );
				
				CLONE = CLASS.cast(Array.newInstance(COMPONENT_TYPE, LENGTH));
				
				for (int index = 0; index < LENGTH; index++)
				{ // Clone element at index in ORIGINAL, set at index in CLONE.
					set(CLONE, index, this.toClone(get(ORIGINAL, index)));
				}
			}
			else
			{
				throw new IllegalArgumentException( );
			}
		}
		
		return CLONE;
	}
}