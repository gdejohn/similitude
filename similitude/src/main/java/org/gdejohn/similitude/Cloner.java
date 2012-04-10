package org.gdejohn.similitude;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.set;
import static org.gdejohn.similitude.TypeToken.getTypeOf;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
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
@SuppressWarnings("javadoc")
public final class Cloner
{
	static final Logger LOGGER = getLogger(Cloner.class);
	
	/**
	 * Wrapper types and {@code String} mapped to default values.
	 * 
	 * Wrapper types are mapped to the default values of their respective
	 * primitive types as per <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.12.5">JLS 4.12.5</a>,
	 * and {@code String} is mapped to the empty string. Primitives types
	 * aren't included here because they're autoboxed during cloning.
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
		
		// http://stackoverflow.com/questions/5124012/examples-of-immutable-classes
	}
	
	private final boolean DETERMINE_IMMUTABLE;
	
	/**
	 * Instantiates types that need to be deep-copied.
	 */
	final Builder BUILDER = new Builder( );
	
	/**
	 * Immutable types, can be shallow-copied.
	 */
	final Set<Class<?>> IMMUTABLE;
	
	/**
	 * Initializes all instance variables.
	 * 
	 * @param KNOWN_IMMUTABLE_TYPES Types known to be immutable, which can therefore be safely shallow-copied.
	 * @param DETERMINE_IMMUTABLE Whether the resulting instance should attempt to reflectively determine immutability during cloning.
	 */
	private Cloner(final Set<Class<?>> KNOWN_IMMUTABLE_TYPES, final boolean DETERMINE_IMMUTABLE)
	{
		IMMUTABLE = new LinkedHashSet<Class<?>>(KNOWN_IMMUTABLE_TYPES);
		
		this.DETERMINE_IMMUTABLE = DETERMINE_IMMUTABLE;
	}
	
	/**
	 * Default constructor.
	 * 
	 * Immutable types are initialized to {@link #BASIC_TYPES}. The resulting
	 * instance will not attempt to reflectively determine immutability during
	 * cloning.
	 */
	public Cloner( )
	{
		this(BASIC_TYPES.keySet( ), false);
	}
	
	/**
	 * Creates a new cloner that will attempt to determine immutablity.
	 * 
	 * The resulting cloner has the same registered immutable types as
	 * {@code this} cloner.
	 * 
	 * @return A new cloner that will attempt to reflectively determine immutablity.
	 */
	public Cloner determineImmutable( )
	{
		return new Cloner(IMMUTABLE, true);
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
			CLASS.getSimpleName( ),
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
	 * 
	 * @throws IllegalArgumentException If {@code CLASS} is an array type.
	 */
	public <T, U extends T> boolean register(final Class<T> CLASS, final U VALUE)
	{
		if (CLASS.isArray( ))
		{
			throw new IllegalArgumentException("Arrays aren't immutable.");
		}
		else
		{
			BUILDER.addDefault(CLASS, VALUE);
			
			return register(CLASS);
		}
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
	 * When cloning a given object, a new instance of that object's type is
	 * created to be used as the clone, which that object is then associated
	 * with in this map. Whenever that same object is encountered again, the
	 * reference to its associated clone is simply reused. This also handles
	 * any-dimensional arrays that contain themselves any number of times.
	 * Overriding implementations of {@link java.lang.Object#equals(Object)}
	 * and {@link java.lang.Object#hashCode()} are ignored. Rather, identity is
	 * used.
	 */
	private Map<Object, Object> CLONES = new IdentityHashMap<Object, Object>( );
	
	/**
	 * Does all of the work for {@link #toClone(Object)}.
	 * 
	 * This method calls itself recursively to clone each element in a given
	 * array, or each instance field in a given instance of a class type, and
	 * in addition to the original object to be cloned, it passes itself a
	 * potential instance to use for the resulting clone that may have already
	 * been instantiated higher in the call stack. If suitable, creating a new
	 * instance of the original object's type, which can be very expensive, is
	 * skipped. This isn't called directly, since {@link #CLONES} is only
	 * cleared in {@code toClone(Object)} once recursion is finished.
	 * 
	 * @param ORIGINAL The object to create a deep copy of.
	 * @param INSTANCE The potential instance to use for the resulting clone.
	 * 
	 * @return A deep copy of {@code ORIGINAL}.
	 * 
	 * @throws CloningFailedException If cloning {@code ORIGINAL} fails for any other reason.
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
			final TypeToken<? extends T> TYPE = getTypeOf(ORIGINAL); // new TypeToken2<Object>(CLASS, DETERMINE_IMMUTABLE);
			
			final Class<? extends T> CLASS = TYPE.getRawType( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(CLASS))
			{ // Base case, safe to shallow-copy.
				CLONE = ORIGINAL;
				
				LOGGER.debug
				(
					"Shallow-copying value of type {}: \"{}\"",
					CLASS.getSimpleName( ),
					ORIGINAL
				);
			}
			else if (CLONES.containsKey(ORIGINAL))
			{
				LOGGER.debug
				(
					"Already cloned, reusing reference to clone."
				);
				
				CLONE = CLASS.cast(CLONES.get(ORIGINAL));
			}
			else if (CLASS.isArray( ))
			{ // Recursively clone each element into new array.
				final int LENGTH = getLength(ORIGINAL);
				
				LOGGER.debug
				(
					"Cloning array of type {} and length {}.",
					CLASS.getSimpleName( ),
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
							CLASS.getSimpleName( ),
							index
						);
					}
					catch (CloningFailedException e)
					{
						throw
						(
							new CloningFailedException
							(
								e,
								"Cloning array element at index %d failed.",
								index
							)
						);
					}
				}
			}
			else
			{ // Instantiate CLASS, recursively clone fields.
				LOGGER.debug
				(
					"Cloning class type: {}", CLASS.getSimpleName( )
				);
				
				if (INSTANCE != null && INSTANCE != ORIGINAL && INSTANCE.getClass( ).isAssignableFrom(CLASS))
				{
					CLONE = INSTANCE;
					
					LOGGER.debug
					(
						"Already instantiated class type: {}",
						CLASS.getSimpleName( )
					);
				}
				else
				{
					try
					{
						CLONE = BUILDER.instantiate(CLASS);
						
						LOGGER.debug
						(
							"Successfully instantiated class type: {}",
							CLASS.getSimpleName( )
						);
					}
					catch (InstantiationFailedException e)
					{ // Instantiating CLASS failed.
						throw
						(
							new CloningFailedException
							(
								e,
								"Couldn't instantiate class %s.",
								CLASS.getSimpleName( )
							)
						);
					}
				}
				
				CLONES.put(ORIGINAL, CLONE);
				
				for (final Field FIELD : TYPE.getAllInstanceFields( ))
				{ // Clone instance fields in ORIGINAL, set results in CLONE.
					try
					{
						FIELD.setAccessible(true);
						
						final Object CLONE_FIELD =
						(
							toClone(FIELD.get(ORIGINAL), FIELD.get(CLONE))
						);
						
						FIELD.set(CLONE, CLONE_FIELD);
						
						LOGGER.debug
						(
							"Successfully cloned and set field: {}", FIELD
						);
					}
					catch (CloningFailedException e)
					{ // FIELD couldn't be cloned.
						throw
						(
							new CloningFailedException
							(
								e,
								"Couldn't clone field \"%s\" in class %s.",
								FIELD,
								CLASS.getSimpleName( )
							)
						);
					}
					catch (SecurityException e)
					{ // FIELD couldn't be made accessible.
						throw
						(
							new CloningFailedException
							(
								e,
								"Field \"%s\" in class %s couldn't be set accessible.",
								FIELD,
								CLASS.getSimpleName( )
							)
						);
					}
					catch (IllegalAccessException e)
					{ // SecurityException should always be thrown before this.
						throw
						(
							new CloningFailedException
							(
								e,
								"Field \"%s\" in class %s couldn't be accessed.",
								FIELD,
								CLASS.getSimpleName( )
							)
						);
					}
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
	
	static class Foo<E>
	{
		class Bar<F extends E>
		{
			
		}
	}
	
	static abstract class Baz
	{
		abstract Foo<Number>.Bar<Integer> doStuff( );
	}
	
	@SuppressWarnings("unused")
	public static void main(String[ ] args) throws Exception
	{
		String o = "xyzzy";
		CharSequence c = new Cloner( ).toClone((CharSequence)o);
		System.out.println(java.util.Arrays.toString(new Builder( ).getTypeArguments(Baz.class.getDeclaredMethod("doStuff").getGenericReturnType( ), Collections.<TypeVariable<?>, Type>emptyMap( )).entrySet( ).toArray( )));
		System.out.println(((ParameterizedType)new Object( ){void foo(java.util.List<String> l){ }}.getClass( ).getDeclaredMethod("foo", java.util.List.class).getGenericParameterTypes( )[0]).getRawType( ));
	}
}