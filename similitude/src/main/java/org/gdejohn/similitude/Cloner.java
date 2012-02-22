/**
 * Utilities for creating and deep copying instances of arbitrary classes.
 */
package org.gdejohn.similitude;

import static java.lang.reflect.AccessibleObject.setAccessible;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Deep copy instances of arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public class Cloner
{
	/**
	 * Instantiates classes that need to be deep copied.
	 */
	private final Builder BUILDER = new Builder( );
	
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
	 * 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
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
			{ // Base case, safe to shallow-copy.
				CLONE = ORIGINAL;
			}
			else if (CLASS.isArray( ))
			{ // Clone each element into new array.
				final int LENGTH = getLength(ORIGINAL);
				final Class<?> COMPONENT_TYPE = CLASS.getComponentType( );
				
				CLONE = CLASS.cast(newInstance(COMPONENT_TYPE, LENGTH));
				
				for (int index = 0; index < LENGTH; index++)
				{ // Clone element at index in ORIGINAL, set at index in CLONE.
					set(CLONE, index, this.toClone(get(ORIGINAL, index)));
				}
			}
			else
			{ // Create new CLASS instance, clone declared and inherited fields.
				try
				{
					CLONE = BUILDER.instantiate(CLASS);
					
					Class<? super T> current = CLASS;
					
					do
					{
						final Field[ ] FIELDS = current.getDeclaredFields( );
						
						setAccessible(FIELDS, true);
						
						for (final Field FIELD : FIELDS)
						{
							if (isStatic(FIELD.getModifiers( )))
							{ // If static, ignore and skip to the next one.
								continue;
							}
							else
							{ // Clone field in ORIGINAL, set result in CLONE.
								FIELD.set(CLONE, this.toClone(FIELD.get(ORIGINAL)));
							}
						}
						
						current = current.getSuperclass( );
					}
					while (current != null);
				}
				catch (IllegalAccessException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		
		return CLONE;
	}
}