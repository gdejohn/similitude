package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import static java.lang.reflect.AccessibleObject.setAccessible;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.set;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
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
	 * Wrapper types and {@code String} mapped to default values.
	 * 
	 * Wrapper types are mapped to the default values of their respective
	 * primitve types as per <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.12.5">JLS 4.12.5</a>,
	 * and {@code String} is mapped to the empty string. Primitives aren't
	 * included here because primitive types can't be cloned directly by this
	 * class, due to autoboxing.
	 */
	static final Map<Class<?>, Object> BASIC_TYPES;
	
	static
	{
		/*
		 * This map shouldn't change at runtime, so set the initial capacity to
		 * the maximum number of entries that will be added (eight wrappers and
		 * String makes nine) and the load factor to just above one. No wasted
		 * space, no rehashing.
		 */
		BASIC_TYPES =
		(
			new LinkedHashMap<Class<?>, Object>(9, Math.nextUp(1.0f))
		);
		
		BASIC_TYPES.put(Byte.class, Byte.valueOf((byte)0));
		BASIC_TYPES.put(Short.class, Short.valueOf((short)0));
		BASIC_TYPES.put(Integer.class, Integer.valueOf(0));
		BASIC_TYPES.put(Long.class, Long.valueOf(0L));
		BASIC_TYPES.put(Float.class, Float.valueOf(0.0f));
		BASIC_TYPES.put(Double.class, Double.valueOf(0.0d));
		BASIC_TYPES.put(Character.class, Character.valueOf('\u0000'));
		BASIC_TYPES.put(Boolean.class, Boolean.valueOf(false));
		
		BASIC_TYPES.put(String.class, "");
	}
	
	/**
	 * Instantiates types that need to be deep-copied.
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
	public <T, U extends T> boolean register(final Class<T> CLASS, final U VALUE)
	{
		BUILDER.addDefault(CLASS, VALUE);
		
		return register(CLASS);
	}
	
	/**
	 * Resets {@link #IMMUTABLE} to default values.
	 * 
	 * After this method returns, {@code IMMUTABLE} will contain the primitive
	 * wrappers and {@code String}. Any other values that were previously added
	 * by the user are removed.
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
	 * Original objects that have already been cloned, mapped to their clones.
	 * 
	 * {@link #toClone(Object, Object)} maps every encountered object to its
	 * instantiated clone. When the same object is encountered again, the
	 * reference to its previously created clone is simply reused. This also
	 * handles any-dimensional arrays containing themselves an arbitrary number
	 * of times.
	 */
	private Map<Object, Object> CLONES = new IdentityHashMap<Object, Object>( );
	
	/**
	 * Does all of the work for {@link #toClone(Object)}.
	 * 
	 * This method calls itself recursively to clone each element in a given
	 * array, or each field in a given instance of a class type, and in
	 * addition to the original object to be cloned, it passes itself a
	 * potential instance to use for the resulting clone that may have already
	 * been instantiated higher in the call stack. If suitable, creating a new
	 * instance of the original object's type, which can be very expensive, is
	 * skipped. This shouldn't be called directly, since {@link #CLONES} is
	 * only cleared in {@code toClone(Object)} once recursion is finished.
	 * 
	 * @param ORIGINAL The object to create a deep copy of.
	 * @param INSTANCE The potential instance to use for the resulting clone.
	 * 
	 * @return A deep copy of {@code ORIGINAL}.
	 * 
	 * @throws CloningFailedException If cloning {@code ORIGINAL} fails for any reason.
	 */
	private <T> T toClone(final T ORIGINAL, final T INSTANCE)
	{
		final T CLONE;
		
		if (ORIGINAL == null)
		{
			CLONE = null;
			
			LOGGER.debug("Original object is null.");
		}
		else
		{
			// Type-safe, since ORIGINAL is of type T.
			@SuppressWarnings("unchecked")
			final Class<T> CLASS = (Class<T>)ORIGINAL.getClass( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(CLASS))
			{ // Base case, safe to shallow-copy.
				CLONE = ORIGINAL;
				
				LOGGER.debug
				(
					"Shallow-copying value of type {}: \"{}\"",
					CLASS.getCanonicalName( ),
					ORIGINAL
				);
			}
			else if (CLONES.containsKey(ORIGINAL))
			{
				LOGGER.debug
				(
					"This object has already been cloned, shallow-copying."
				);
				
				CLONE = CLASS.cast(CLONES.get(ORIGINAL));
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
				
				if (INSTANCE != null && INSTANCE != ORIGINAL && INSTANCE.getClass( ).isAssignableFrom(CLASS) && getLength(INSTANCE) == LENGTH)
				{
					CLONE = INSTANCE;
					
					LOGGER.debug("Array already instantiated.");
				}
				else
				{
					CLONE = BUILDER.instantiateArray(CLASS, LENGTH);
					
					LOGGER.debug("Successfully instantiated array.");
				}
				
				CLONES.put(ORIGINAL, CLONE);
				
				for (int index = 0; index < LENGTH; index++)
				{
					try
					{
						set
						(
							CLONE,
							index,
							toClone(get(ORIGINAL, index), get(CLONE, index))
						);
						
						LOGGER.debug
						(
							"Successfully cloned element of {} array at index: {}",
							CLASS.getCanonicalName( ),
							index
						);
					}
					catch (CloningFailedException e)
					{
						throw
						(
							new CloningFailedException
							(
								String.format
								(
									"Cloning element at index %d failed.",
									index
								),
								e
							)
						);
					}
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
					if (INSTANCE != null && INSTANCE != ORIGINAL && INSTANCE.getClass( ).isAssignableFrom(CLASS))
					{
						CLONE = INSTANCE;
						
						LOGGER.debug
						(
							"Already instantiated class type: {}",
							CLASS.getCanonicalName( )
						);
					}
					else
					{
						CLONE = BUILDER.instantiate(CLASS);
						
						LOGGER.debug
						(
							"Successfully instantiated class type: {}",
							CLASS.getCanonicalName( )
						);
					}
					
					CLONES.put(ORIGINAL, CLONE);
					
					Class<? super T> current = CLASS;
					
					do
					{ // Traverse up class hierarchy to get inherited fields.
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
								
								FIELD.set
								(
									CLONE,
									toClone
									(
										FIELD.get(ORIGINAL), FIELD.get(CLONE)
									)
								);
								
								LOGGER.debug
								(
									"Successfully cloned field: {}", FIELD
								);
							}
						}
						
						current = current.getSuperclass( );
					}
					while (current != null);
				}
				catch (CloningFailedException e)
				{ // Cloning a field failed somewhere in the object graph.
					throw new CloningFailedException(e);
				}
				catch (InstantiationFailedException e)
				{ // Instantiating CLASS failed.
					throw new CloningFailedException("Cloning failed.", e);
				}
				catch (SecurityException e)
				{ // Fields couldn't be made accessible.
					throw new CloningFailedException(e);
				}
				catch (IllegalAccessException e)
				{ // A SecurityException should always be thrown before this.
					throw new CloningFailedException(e);
				}
			}
		}
		
		return CLONE;
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
		
		try
		{
			CLONE = toClone(ORIGINAL, null);
		}
		finally
		{
			CLONES.clear( );
		}
		
		return CLONE;
	}
}