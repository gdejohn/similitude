package org.gdejohn.similitude;

import static java.lang.Boolean.FALSE;
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
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
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
public final class Cloner
{
	static final Logger LOGGER = getLogger(Cloner.class);
	
	/**
	 * Wrapper types and {@code String} mapped to default values.
	 * 
	 * Wrapper types are mapped to the default values of their respective
	 * primitive types as per <a href="http://docs.oracle.com/javase/specs/jls/se5.0/html/typesValues.html#4.12.5">JLS 4.12.5</a>,
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
					if (VALUE == null)
					{
						throw new RuntimeException("Value can't be null.");
					}
					else if (put(typeOf(CLASS), VALUE) == null)
					{
						return;
					}
					else
					{
						throw new RuntimeException("Mapping already exists.");
					}
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
					map(Boolean.class, FALSE);
					
					map(String.class, "");
				}
			}
		)
	);
	
	/**
	 * Instantiates types that need to be deep-copied.
	 */
	private final Builder BUILDER = new Builder( );
	
	/**
	 * Immutable types, can be shallow-copied.
	 */
	private final Set<TypeToken<?>> IMMUTABLE_TYPES;
	
	/**
	 * Initializes all instance variables.
	 * 
	 * @param IMMUTABLE_TYPES Types which can be safely shallow-copied.
	 */
	private Cloner(final Set<TypeToken<?>> IMMUTABLE_TYPES)
	{
		this.IMMUTABLE_TYPES =
		(
			new LinkedHashSet<TypeToken<?>>(IMMUTABLE_TYPES)
		);
	}
	
	/**
	 * Default constructor.
	 * 
	 * Immutable types are initialized to {@link #BASIC_TYPES}.
	 */
	public Cloner( )
	{
		this(BASIC_TYPES.keySet( ));
	}
	
	/**
	 * @return {@code this} cloner's builder.
	 */
	public Builder getBuilder( )
	{
		return BUILDER;
	}
	
	/**
	 * @return A read-only view of {@code this} cloner's immutable types.
	 */
	public Set<TypeToken<?>> getImmutableTypes( )
	{
		return unmodifiableSet(IMMUTABLE_TYPES);
	}
	
	/**
	 * Checks if a given type is registered as immutable.
	 * 
	 * @param TYPE The type to check.
	 * 
	 * @return {@code true} if {@code TYPE} is registered as immutable, else {@code false}.
	 */
	public boolean isImmutable(final TypeToken<?> TYPE)
	{
		return IMMUTABLE_TYPES.contains(TYPE);
	}
	
	/**
	 * Registers the given type as immutable, for shallow copying.
	 * 
	 * @param TYPE The type to register as immutable.
	 * 
	 * @return {@code true} if {@code TYPE} wasn't already registered.
	 */
	public boolean register(final TypeToken<?> TYPE)
	{
		final Class<?> CLASS = TYPE.getRawType( );
		
		if (CLASS.isPrimitive( ))
		{
			throw
			(
				new IllegalArgumentException
				(
					"Can't map primitive type. Use its wrapper."
				)
			);
		}
		else if (CLASS.isArray( ))
		{
			throw new IllegalArgumentException("Arrays are mutable.");
		}
		else
		{
			final boolean CHANGED = IMMUTABLE_TYPES.add(TYPE);
			
			LOGGER.debug
			(
				"Registering class {} as immutable: {}",
				TYPE.getRawType( ).getSimpleName( ),
				CHANGED ? "changed" : "unchanged"
			);
			
			return CHANGED;
		}
	}
	
	/**
	 * Registers the given type as immutable and maps it to the given value.
	 * 
	 * When this instance of {@code Cloner} needs an instance of the given
	 * type, it uses the given value instead of creating a new instance.
	 * The type must be immutable, or any resulting clone that relies on it
	 * isn't guaranteed to be a true deep copy.
	 * 
	 * @param <T> The type represented by {@code TYPE}.
	 * @param <U> The type of {@code VALUE}, either {@code T} or a subtype of {@code T}.
	 * @param TYPE The type to register as immutable.
	 * @param VALUE The default value to map {@code TYPE} to.
	 * 
	 * @return {@code true} if {@code TYPE} wasn't already registered, else {@code false}.
	 * 
	 * @throws IllegalArgumentException If {@code TYPE} is an array type.
	 */
	public <T, U extends T> boolean register(final TypeToken<T> TYPE, final U VALUE)
	{
		BUILDER.addDefault(TYPE, VALUE);
		
		return IMMUTABLE_TYPES.add(TYPE);
	}
	
	/**
	 * Unregisters a given type as immutable.
	 * 
	 * @param TYPE The type to unregister as immutable.
	 * 
	 * @return {@code true} if {@code TYPE} was registered, else {@code false}.
	 */
	public boolean unregister(final TypeToken<?> TYPE)
	{
		BUILDER.removeDefault(TYPE);
		
		return IMMUTABLE_TYPES.remove(TYPE);
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
		BUILDER.reset( );
		
		final boolean CHANGED =
		(
			IMMUTABLE_TYPES.retainAll(BASIC_TYPES.keySet( ))
		);
		
		LOGGER.debug
		(
			"Resetting immutable types: {}",
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
	 * 
	 * @see IdentityHashMap
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
		if (ORIGINAL == null)
		{
			LOGGER.debug("Original object is null.");
			
			return null;
		}
		
		final TypeToken<? extends T> TYPE = typeOf(ORIGINAL);
		
		final Class<? extends T> CLASS = TYPE.getRawType( );
		
		if (CLASS.isEnum( ) || isImmutable(TYPE))
		{ // Base case, safe to shallow-copy.
			LOGGER.debug
			(
				"Shallow-copying value of type {}: \"{}\"",
				CLASS.getSimpleName( ),
				ORIGINAL
			);
			
			return ORIGINAL;
		}
		else if (CLONES.containsKey(ORIGINAL))
		{
			LOGGER.debug
			(
				"Already cloned, reusing reference to clone."
			);
			
			return CLASS.cast(CLONES.get(ORIGINAL));
		}
		else
		{
			final T CLONE;
			
			if (CLASS.isArray( ))
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
						if (isPublic(FIELD.getModifiers( )) == false)
						{
							FIELD.setAccessible(true);
						}
						
						final Object VALUE =
						(
							toClone(FIELD.get(ORIGINAL), FIELD.get(CLONE))
						);
						
						FIELD.set(CLONE, VALUE);
						
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
			
			return CLONE;
		}
	}
	
	/**
	 * Clones a given object.
	 * 
	 * The resulting clone is a deep copy of the given object. They will be
	 * equal to each other, and changes to one will not affect the other.
	 * 
	 * @param <T> The type of the object to clone.
	 * @param ORIGINAL The object to clone.
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