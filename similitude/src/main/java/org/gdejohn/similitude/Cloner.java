/**
 * Utilities for creating and deep copying instances of arbitrary classes.
 */
package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import static java.lang.reflect.AccessibleObject.setAccessible;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.set;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.unmodifiableMap;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

/**
 * Deep copy instances of arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public final class Cloner
{
	static final Logger LOGGER = getLogger(Cloner.class);
	
	/**
	 * Primitive types, wrappers, and {@code String} mapped to default values.
	 * 
	 * Primitive types are mapped to their default values as per
	 * {@link http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.12.5},
	 * wrapper types are mapped to the default values of their respective
	 * primitive types, and {@code String} is mapped to the empty string.
	 */
	static final Map<Class<?>, Object> BASIC_TYPES;
	
	static
	{
		final int INITIAL_CAPACITY = 17;
		final float LOAD_FACTOR = Math.nextUp(1.0f);
		
		/*
		 * Initial capacity of 17 is equal to the maximum number of entries
		 * (8 primitive types, 8 wrapper types, and String), ensuring no wasted
		 * space. Load factor of just over 1.0 ensures that no rehashing will
		 * be performed.
		 */
		final Map<Class<?>, Object> MAP =
		(
			new LinkedHashMap<Class<?>, Object>(INITIAL_CAPACITY, LOAD_FACTOR)
		);
		
		MAP.put(byte.class, Byte.valueOf((byte)0));
		MAP.put(short.class, Short.valueOf((short)0));
		MAP.put(int.class, Integer.valueOf(0));
		MAP.put(long.class, Long.valueOf(0L));
		MAP.put(float.class, Float.valueOf(0.0f));
		MAP.put(double.class, Double.valueOf(0.0d));
		MAP.put(char.class, Character.valueOf('\u0000'));
		MAP.put(boolean.class, Boolean.valueOf(false));
		
		MAP.put(Byte.class, Byte.valueOf((byte)0));
		MAP.put(Short.class, Short.valueOf((short)0));
		MAP.put(Integer.class, Integer.valueOf(0));
		MAP.put(Long.class, Long.valueOf(0L));
		MAP.put(Float.class, Float.valueOf(0.0f));
		MAP.put(Double.class, Double.valueOf(0.0d));
		MAP.put(Character.class, Character.valueOf('\u0000'));
		MAP.put(Boolean.class, Boolean.valueOf(false));
		
		MAP.put(String.class, "");

		BASIC_TYPES = unmodifiableMap(MAP);
	}
	
	/**
	 * Instantiates classes that need to be deep-copied.
	 */
	private final Builder BUILDER = new Builder( );
	
	/**
	 * Immutable types, can be shallow-copied.
	 */
	private final Set<Class<?>> IMMUTABLE;
	
	{ // Instance initializer, executes at the beginning of every constructor.
		IMMUTABLE = new LinkedHashSet<Class<?>>(BASIC_TYPES.keySet( ));
	}
	
	/**
	 * Registers the given class as immutable, for shallow copying.
	 * 
	 * @param CLASS The class to register as immutable.
	 * 
	 * @return {@code true} if {@code CLASS} wasn't already registered.
	 */
	public boolean register(final Class<?> CLASS)
	{
		final boolean CHANGED = IMMUTABLE.add(CLASS);
		
		LOGGER.debug
		(
			"Registering class {} as immutable: {}",
			CLASS.getCanonicalName( ),
			CHANGED ? "changed" : "unchanged"
		);
		
		return CHANGED;
	}
	
	/**
	 * Registers the given class as immutable and maps it to the given value.
	 * 
	 * When this instance of {@code Cloner} needs an instance of the given
	 * class, it uses the given value instead of creating a new instance.
	 * The class must be immutable, or any resulting clone that relies on the
	 * class isn't guaranteed to be a true deep copy.
	 * 
	 * @param CLASS The class to register as immutable.
	 * @param VALUE The value to map {@code CLASS} to.
	 * 
	 * @return {@code true} if {@code CLASS} wasn't already registered.
	 */
	public <T, U extends T> boolean register(final Class<T> CLASS, final U VALUE) // final Class<? extends T> CLASS?
	{
		BUILDER.addDefault(CLASS, VALUE);
		
		return register(CLASS);
	}
	
	/**
	 * Resets {@link #IMMUTABLE} to default values.
	 * 
	 * After this method returns, {@code IMMUTABLE} will contain the primitive
	 * types, their respective wrappers, and {@code String}. Any other values
	 * that were previously added by the user are removed.
	 */
	public boolean reset( )
	{
		final boolean CHANGED = IMMUTABLE.retainAll(BASIC_TYPES.keySet( ));
		
		LOGGER.debug
		(
			"Resetting immutable classes: {}",
			CHANGED ? "changed" : "unchanged"
		);
		
		return CHANGED;
	}
	
	/**
	 * Creates a deep copy of the given object.
	 * 
	 * @param ORIGINAL The object to create a deep copy of.
	 * 
	 * @return A deep copy of {@code ORIGINAL}.
	 * 
	 * @throws CloningFailedException If cloning {@code ORIGINAL} fails for any reason.
	 */
	public <T> T toClone(final T ORIGINAL)
	{
		final T CLONE;
		
		if (ORIGINAL == null)
		{
			LOGGER.debug("Original object is null.");
			
			CLONE = null;
		}
		else
		{
			// This cast will always work, since ORIGINAL is of type T.
			@SuppressWarnings("unchecked")
			final Class<T> CLASS = (Class<T>)ORIGINAL.getClass( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(CLASS))
			{ // Base case, safe to shallow-copy.
				LOGGER.debug
				(
					"Shallow-copying value of type {}: \"{}\"",
					CLASS.getCanonicalName( ),
					ORIGINAL
				);
				
				CLONE = ORIGINAL;
			}
			else if (CLASS.isArray( ))
			{ // Recursively clone each element into new array.
				final int LENGTH = getLength(ORIGINAL);
				
				LOGGER.debug
				(
					"Cloning array of type {} and length {}.",
					CLASS.getCanonicalName( ),
					LENGTH
				);
				
				CLONE = BUILDER.instantiateArray(CLASS, LENGTH);
				
				for (int index = 0; index < LENGTH; index++)
				{ // Clone element at index in ORIGINAL, set at index in CLONE.
					set(CLONE, index, this.toClone(get(ORIGINAL, index)));
					
					LOGGER.debug
					(
						"Successfully cloned element at index {}.", index
					);
				}
			}
			else
			{ // Instantiate CLASS, recursively clone fields.
				LOGGER.debug
				(
					"Cloning class type: {}", CLASS.getCanonicalName( )
				);
				
				try
				{
					CLONE = BUILDER.instantiate(CLASS);
					
					LOGGER.debug
					(
						"Successfully instantiated class type {}.",
						CLASS.getCanonicalName( )
					);
					
					Class<? super T> current = CLASS;
					
					do
					{
						final Field[ ] FIELDS = current.getDeclaredFields( );
						
						setAccessible(FIELDS, true);
						
						for (final Field FIELD : FIELDS)
						{
							if (isStatic(FIELD.getModifiers( )))
							{ // If static, ignore and skip to the next one.
								LOGGER.debug
								(
									"Skipping static field: {}", FIELD
								);
							}
							else
							{ // Clone field in ORIGINAL, set result in CLONE.
								LOGGER.debug
								(
									"Found instance field: {}", FIELD
								);
								
								// Don't create new instance of field if !=.
								
								FIELD.set
								(
									CLONE, this.toClone(FIELD.get(ORIGINAL))
								);
								
								LOGGER.debug
								(
									"Successfully cloned field: {}", FIELD
								);
							}
						}
						
						current = current.getSuperclass( );
					} // Traverse up class hierarchy to get inherited fields.
					while (current != null);
				}
				catch (CloningFailedException e)
				{ // Cloning a field failed somewhere in the object graph.
					throw new CloningFailedException(e);
				}
				catch (InstantiationFailedException e)
				{ // Instantiating CLASS failed.
					throw new CloningFailedException(e);
				}
				catch (SecurityException e)
				{ // Fields couldn't be made accessible.
					throw new CloningFailedException(e);
				}
				catch (IllegalAccessException e)
				{
					throw new CloningFailedException(e);
				}
				catch (ExceptionInInitializerError e)
				{
					throw new CloningFailedException(e);
				}
			}
		}
		
		return CLONE;
	}
}