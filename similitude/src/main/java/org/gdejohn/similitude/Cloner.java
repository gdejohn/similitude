package org.gdejohn.similitude;

import static java.lang.Boolean.valueOf;
import static java.lang.Byte.valueOf;
import static java.lang.Character.valueOf;
import static java.lang.Double.valueOf;
import static java.lang.Float.valueOf;
import static java.lang.Integer.valueOf;
import static java.lang.Long.valueOf;
import static java.lang.Math.nextUp;
import static java.lang.Short.valueOf;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;
import static java.util.Collections.unmodifiableMap;
import static org.gdejohn.similitude.TypeToken.typeOf;
import static org.slf4j.LoggerFactory.getLogger;

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
	@SuppressWarnings("serial")
	public static final Map<TypeToken<?>, Object> BASIC_TYPES =
	(
		unmodifiableMap
		(
			new LinkedHashMap<TypeToken<?>, Object>(9, nextUp(1.0f))
			{
				<T> void map(final Class<T> CLASS, final T VALUE)
				{
					put(typeOf(CLASS), VALUE);
				}
				
				/*
				 * Instance initializer.
				 */
				{
					map(Byte.class, valueOf((byte)0));
					map(Short.class, valueOf((short)0));
					map(Integer.class, valueOf(0));
					map(Long.class, valueOf(0L));
					map(Float.class, valueOf(0.0f));
					map(Double.class, valueOf(0.0d));
					map(Character.class, valueOf('\u0000'));
					map(Boolean.class, valueOf(false));
					
					map(String.class, "");
				}
			}
		)
	);
	
	@SuppressWarnings("unused")
	private final boolean DETERMINE_IMMUTABLE;
	
	/**
	 * Instantiates types that need to be deep-copied.
	 */
	final Builder BUILDER = new Builder( );
	
	/**
	 * Immutable types, can be shallow-copied.
	 */
	private final Set<TypeToken<?>> IMMUTABLE;
	
	/**
	 * Initializes all instance variables.
	 * 
	 * @param KNOWN_IMMUTABLE_TYPES Types known to be immutable, which can therefore be safely shallow-copied.
	 * @param DETERMINE_IMMUTABLE Whether the resulting instance should attempt to reflectively determine immutability during cloning.
	 */
	private Cloner(final Set<TypeToken<?>> KNOWN_IMMUTABLE_TYPES, final boolean DETERMINE_IMMUTABLE)
	{
		IMMUTABLE = new LinkedHashSet<TypeToken<?>>(KNOWN_IMMUTABLE_TYPES);
		
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
	 * @param TYPE The type to register as immutable.
	 * 
	 * @return {@code true} if {@code CLASS} wasn't already registered.
	 */
	public boolean register(final TypeToken<?> TYPE)
	{
		final boolean CHANGED = IMMUTABLE.add(TYPE);
		
		LOGGER.debug
		(
			"Registering class {} as immutable: {}",
			TYPE.getRawType( ).getSimpleName( ),
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
	 * @param TYPE The type to register as immutable.
	 * @param VALUE The value to map {@code CLASS} to.
	 * 
	 * @return {@code true} if {@code CLASS} wasn't already registered.
	 * 
	 * @throws IllegalArgumentException If {@code CLASS} is an array type.
	 */
	public <T, U extends T> boolean register(final TypeToken<T> TYPE, final U VALUE)
	{
		if (TYPE.getRawType( ).isArray( ))
		{
			throw new IllegalArgumentException("Arrays aren't immutable.");
		}
		else
		{
			BUILDER.addDefault(TYPE, VALUE);
			
			return register(TYPE);
		}
	}
	
	/**
	 * Resets immutable types to default values.
	 * 
	 * After this method returns, any previous user-registered immutable types
	 * will have been removed, leaving {@code String} and the primitive
	 * wrappers.
	 * 
	 * @return {@code true} if the immutable types changed.
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
	private IdentityHashMap<Object, Object> CLONES =
	(
		new IdentityHashMap<Object, Object>( )
	);
	
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
			final TypeToken<? extends T> TYPE = typeOf(ORIGINAL); // new TypeToken2<Object>(CLASS, DETERMINE_IMMUTABLE);
			
			final Class<? extends T> CLASS = TYPE.getRawType( );
			
			if (CLASS.isEnum( ) || IMMUTABLE.contains(TYPE))
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
					CLONE =
					(
						CLASS.cast
						(
							newInstance(CLASS.getComponentType( ), LENGTH)
						)
					);
					
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
						CLONE = BUILDER.instantiate(TYPE);
						
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
}