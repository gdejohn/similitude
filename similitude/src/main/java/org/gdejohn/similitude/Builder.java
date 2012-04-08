package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Instantiate arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
@SuppressWarnings("javadoc")
public final class Builder
{
	static final Logger LOGGER = getLogger(Builder.class);
	
	/**
	 * Primitive types mapped to their respective wrapper types.
	 */
	static final Map<Class<?>, Class<?>> WRAPPERS;
	
	static
	{
		/*
		 * This map shouldn't change at runtime, so set the initial capacity to
		 * the maximum number of entries that will be added (eight mappings,
		 * one for each primitive type) and the load factor to just above one.
		 * No wasted space, no rehashing.
		 */
		WRAPPERS = new LinkedHashMap<Class<?>, Class<?>>(8, Math.nextUp(1.0f));
	}
	
	/**
	 * Maps the given primitive type to its corresponding wrapper.
	 * 
	 * The type parameter {@code T} statically ensures that PRIMITIVE and
	 * WRAPPER are of the same type.
	 * 
	 * @param PRIMITIVE The primitive type to map.
	 * @param WRAPPER The corresponding wrapper to map to.
	 */
	private static <T> void add(final Class<T> PRIMITIVE, final Class<T> WRAPPER)
	{
		if (PRIMITIVE.isPrimitive( ))
		{
			if (WRAPPER.isPrimitive( ))
			{
				throw new RuntimeException("WRAPPER must not be primitive.");
			}
			else if (WRAPPERS.put(PRIMITIVE, WRAPPER) == null)
			{ // No previous mapping.
				return;
			}
			else
			{ // If put( ) didn't return null, then a mapping already existed.
				throw new RuntimeException("PRIMITIVE already mapped.");
			}
		}
		else
		{
			throw
			(
				new RuntimeException
				(
					"PRIMITIVE must represent a primitive type."
				)
			);
		}
	}
	
	static
	{ // Map the primitive types to their corresponding wrappers.
		add(byte.class, Byte.class);
		add(short.class, Short.class);
		add(int.class, Integer.class);
		add(long.class, Long.class);
		add(float.class, Float.class);
		add(double.class, Double.class);
		add(char.class, Character.class);
		add(boolean.class, Boolean.class);
	}
	
	/**
	 * Gets the wrapper corresponding to the given primitive type.
	 * 
	 * @param PRIMITIVE The primitive type for which to get the corresponding wrapper.
	 * 
	 * @return The wrapper corresponding to {@code PRIMITIVE}.
	 */
	private static <T> Class<T>	wrap(final Class<T> PRIMITIVE)
	{
		/*
		 * Mappings are only added to WRAPPERS through add( ), which only
		 * allows a given mapping if it would allow this cast to succeed.
		 */
		@SuppressWarnings("unchecked")
		final Class<T> WRAPPER =
		(
			(Class<T>)WRAPPERS.get(PRIMITIVE)
		);
		
		if (WRAPPER == null)
		{
			throw new RuntimeException("No wrapper mapping found.");
		}
		else
		{
			return WRAPPER;
		}
	}
	
	/**
	 * Immutable types, mapped to default values.
	 * 
	 * Initialized to include the wrapper types and {@code String}.
	 * 
	 * @see Cloner#BASIC_TYPES
	 */
	private final Map<Class<?>, Object> IMMUTABLE_DEFAULTS;
	
	{ // Instance initializer, executes at the beginning of every constructor.
		IMMUTABLE_DEFAULTS = new LinkedHashMap<Class<?>, Object>(Cloner.BASIC_TYPES);
	}
	
	/**
	 * Maps an immutable class to a default value.
	 * 
	 * The bound type parameter {@code U} statically ensures that {@code VALUE}
	 * is assignable to {@code TYPE}. Primitive types are a special case. They
	 * use the default values of their corresponding wrappers and cannot
	 * themselves be mapped. Attempting to do so will throw an exception.
	 * 
	 * @param TYPE The immutable type.
	 * @param VALUE The default value for {@code TYPE}.
	 * 
	 * @return {@code null} if {@code TYPE} wasn't already mapped, else the previous mapping.
	 * 
	 * @throws IllegalArgumentException If {@code TYPE} is primitive.
	 */
	public <T, U extends T> Object addDefault(final Class<T> TYPE, final U VALUE)
	{
		LOGGER.debug
		(
			"Adding default value \"{}\" for type {}.",
			VALUE,
			TYPE.getSimpleName( )
		);
		
		if (TYPE.isPrimitive( ))
		{
			throw
			(
				new IllegalArgumentException
				(
					"Can't map primitive type. Use its wrapper."
				)
			);
		}
		else
		{
			return IMMUTABLE_DEFAULTS.put(TYPE, VALUE);
		}
	}

	/**
	 * Gets the default value for the given type.
	 * 
	 * The type must have been previously registered as immutable with {@link
	 * #addDefault}, or {@code null} will be returned. Primitive types aren't
	 * actually mapped. Instead, the default values of their corresponding
	 * wrapper types are used.
	 *
	 * @param TYPE The type for which to get the default value.
	 *
	 * @return Default value associated with {@code TYPE}, or {@code null}.
	 */
	public <T> T getDefault(final Class<T> TYPE)
	{
		LOGGER.debug
		(
			"Getting default value for type {}.",
			TYPE.getSimpleName( )
		);
		
		return TYPE.cast(IMMUTABLE_DEFAULTS.get(TYPE));
	}
	
	/**
	 * Creates an array of the given type and length.
	 * 
	 * Each element is initialized to {@code null}.
	 * 
	 * @param TYPE The type of the array to create (e.g. {@code String[ ]}).
	 * @param LENGTH The length of the array to create.
	 * 
	 * @return An array of the given type and length.
	 * 
	 * @throws NullPointerException If {@code TYPE} is null, or doesn't represent an array type.
	 * @throws NegativeArraySizeException If {@code LENGTH} is negative.
	 */
	public <T> T instantiateArray(final Class<T> TYPE, final int LENGTH)
	{
		LOGGER.debug
		(
			"Instantiating array of type {} and dimensions: {}",
			TYPE.getSimpleName( ),
			LENGTH
		);
		
		return
		(
			TYPE.cast
			(
				Array.newInstance
				(
					TYPE.getComponentType( ),
					LENGTH
				)
			)
		);
	}
	
	/**
	 * Default length for new arrays.
	 * 
	 * Arrays created by {@link #instantiateArray(Class, int)} are initialized
	 * to this length.
	 */
	final int DEFAULT_ARRAY_LENGTH = 0;
	
	/**
	 * Gets the constructors for the given class.
	 * 
	 * The result depends on whether there's a SecurityManager present and how
	 * restrictive it is. Declared constructors are tried first. Failing that,
	 * public constructors are tried next. Failing that, an exception is
	 * thrown.
	 * 
	 * @param CLASS The class for which to get the constructors.
	 * 
	 * @return Either all of the declared constructors for {@code CLASS}, or the public constructors.
	 * 
	 * @throws InstantiationFailedException If neither the declared nor public constructors are accessible.
	 */
	private Constructor<?>[ ] getConstructors(final Class<?> CLASS)
	{
		try
		{
			try
			{
				return CLASS.getDeclaredConstructors( );
			}
			catch (SecurityException e)
			{
				LOGGER.debug
				(
					"Declared constructors not available for type: {}",
					CLASS.getSimpleName( )
				);
				
				return CLASS.getConstructors( );
			}
		}
		catch (SecurityException e)
		{
			LOGGER.debug
			(
				"Public constructors not available for: {}",
				CLASS.getSimpleName( )
			);
			
			throw
			(
				new InstantiationFailedException
				(
					e,
					"No available constructors for: {}",
					CLASS.getSimpleName( )
				)
			);
		}
	}
	
	/**
	 * The empty map of type variables to types.
	 * 
	 * Used for types that don't declare type parameters.
	 */
	private static final Map<TypeVariable<?>, Type> EMPTY_MAP = Collections.emptyMap( );

	/**
	 * Maps type variables to supplied type arguments.
	 * 
	 * @param GENERIC_TYPE The generic type with its type arguments.
	 * @param OLD Other type arguments in scope.
	 * 
	 * @return Type variables mapped to their arguments, or the empty map if no type variables are declared.
	 */
	public Map<TypeVariable<?>, Type> getTypeArguments(final Type TYPE, final Map<TypeVariable<?>, Type> OLD, final Object... ARGS)
	{
		if (TYPE instanceof ParameterizedType)
		{
			final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, Type>( )
			);
			
			Type current = TYPE;
			
			do
			{
				LOGGER.debug("Current type: {}", current);
				
				final TypeVariable<?>[ ] PARAMETERS =
				(
					((Class<?>)((ParameterizedType)current).getRawType( )).getTypeParameters( )
				);
				
				final Type[ ] ARGUMENTS =
				(
					((ParameterizedType)current).getActualTypeArguments( )
				);
				
				for (int index = 0; index < PARAMETERS.length; index++)
				{
					if (ARGUMENTS[index] instanceof Class)
					{
						LOGGER.debug("{} parameterized by class type: {}", PARAMETERS[index], ARGUMENTS[index]);
						
						TYPE_ARGUMENTS.put
						(
							PARAMETERS[index],
							(Class<?>)ARGUMENTS[index]
						);
					}
					else if (ARGUMENTS[index] instanceof TypeVariable)
					{
						LOGGER.debug("{} parameterized by type argument: {}", PARAMETERS[index], OLD.get(ARGUMENTS[index]));
						
						TYPE_ARGUMENTS.put
						(
							PARAMETERS[index],
							OLD.get(ARGUMENTS[index])
						);
					}
					else
					{
						// Need to look through constructor args. GenericArrayType? If WildcardType, use bounds?
					}
				}
				
				current = ((ParameterizedType)current).getOwnerType( );
			}
			while (current instanceof ParameterizedType);
			
			return Collections.unmodifiableMap(TYPE_ARGUMENTS);
		}
		else
		{
			return EMPTY_MAP;
		}
	}
	
	/**
	 * Creates an instance of the given type, which may be parameterized.
	 * 
	 * {@link #instantiate(Class)} delegates to this method.
	 * 
	 * @param TYPE The type to instantiate.
	 * @param TYPE_ARGUMENTS Type variables declared or inherited by {@code TYPE}, mapped to their respective arguments.
	 * 
	 * @return An instance of {@code TYPE}.
	 * 
	 * @throws InstantiationFailedException If instantiating {@code TYPE} fails for any reason.
	 */
	<T> T instantiate(final Class<T> TYPE, final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS)
	{
		if (TYPE == null)
		{
			LOGGER.debug("Class argument is null.");
			
			return null;
		}
		else if (TYPE.isPrimitive( ))
		{
			/*
			 * The default values are all reference types, which throw
			 * ClassCastExceptions when reflectively cast to primitive types.
			 * As a workaround, the corresponding wrapper must be used, which
			 * works because for any primitive type, P, and its wrapper, W,
			 * P.class and W.class are both of type Class<W>.
			 */
			return getDefault(wrap(TYPE));
		}
		else if (IMMUTABLE_DEFAULTS.containsKey(TYPE))
		{ // Base case (previously added immutable type), return default value.
			return getDefault(TYPE);
		}
		else if (TYPE.isEnum( ))
		{ // Base case, return first declared constant.
			LOGGER.debug
			(
				"Instantiating enum type: {}", TYPE.getSimpleName( )
			);
			
			try
			{
				return TYPE.getEnumConstants( )[0];
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				throw
				(
					new InstantiationFailedException
					(
						e,
						"Enum type %s declares no constants.",
						TYPE.getSimpleName( )
					)
				);
			}
		}
		else if (TYPE.isArray( ))
		{ // Base case, return empty array of CLASS's component type.
			return instantiateArray(TYPE, DEFAULT_ARRAY_LENGTH);
		}
		else if (TYPE.isInterface( ))
		{ // Base case, return dynamic proxy.
			LOGGER.debug
			(
				"Creating proxy for interface {}.", TYPE.getSimpleName( )
			);
			
			return
			(
				TYPE.cast
				(
					Proxy.newProxyInstance
					(
						TYPE.getClassLoader( ),
						new Class<?>[ ] {TYPE},
						new Handler(this, TYPE_ARGUMENTS)
					)
				)
			);
		}
		else
		{ // Class type, reflectively invoke each constructor until one works.
			LOGGER.debug
			(
				"Instantiating class type: {}", TYPE.getSimpleName( )
			);
			
			for (final Constructor<?> CONSTRUCTOR : getConstructors(TYPE))
			{
				LOGGER.debug
				(
					"Found constructor: {}", CONSTRUCTOR.toGenericString( )
				);
				
				try
				{
					final Class<?>[ ] PARAMETERS =
					(
						CONSTRUCTOR.getParameterTypes( )
					);
					
					/*
					 * The constructor to be invoked must be parameterized by
					 * type T to return a value of type T. Passing the array of
					 * Class instances representing the parameter types
					 * uniquely identifying the current constructor to
					 * Class.getDeclaredConstructor( ) returns the same
					 * constructor, but with the required type information.
					 */
					final Constructor<T> GENERIC_CONSTRUCTOR =
					(
						TYPE.getDeclaredConstructor(PARAMETERS)
					);
					
					/*
					 * Constructors that wouldn't normally be accessible (e.g.
					 * private) need to be made accessible before they can be
					 * invoked.
					 */
					GENERIC_CONSTRUCTOR.setAccessible(true);
					
					final Type[ ] GENERIC_PARAMETERS =
					(
						GENERIC_CONSTRUCTOR.getGenericParameterTypes( )
					);
					
					final Object[ ] ARGUMENTS =
					(
						new Object[PARAMETERS.length]
					);
					
					/*
					 * Recursively instantiate arguments to satisfy the current
					 * constructor's parameters.
					 */
					for (int index = 0; index < PARAMETERS.length; index++)
					{
						try
						{
							ARGUMENTS[index] =
							(
								instantiate
								(
									PARAMETERS[index],
									getTypeArguments
									(
										GENERIC_PARAMETERS[index],
										TYPE_ARGUMENTS
									)
								)
							);
							
							LOGGER.debug
							(
								"Successfully instantiated {} parameter.",
								PARAMETERS[index].getSimpleName( )
							);
						}
						catch (InstantiationFailedException e)
						{
							/*
							 * The compiler initializes every element in
							 * ARGUMENTS to null, so for any parameter that
							 * can't be instantiated, null is passed to the
							 * constructor.
							 */
							LOGGER.debug("Couldn't instantiate parameter.", e);
						}
					}
					
					LOGGER.debug
					(
						"Invoking constructor \"{}\" with arguments: {}",
						GENERIC_CONSTRUCTOR.toGenericString( ),
						ARGUMENTS
					);
					
					/*
					 * If newInstance( ) completes normally, then instantiation
					 * was successful, and the result is returned, skipping the
					 * rest of the loop. If an exception is thrown at any point
					 * in this try block, it's caught and logged, and the loop
					 * continues, trying the next constructor.
					 */
					return GENERIC_CONSTRUCTOR.newInstance(ARGUMENTS);
				}
				catch (InvocationTargetException e)
				{
					LOGGER.debug("Constructor failed.", e);
					
					continue;
				}
				catch (SecurityException e)
				{
					LOGGER.warn("Constructor failed.", e);
					
					continue;
				}
				catch (IllegalAccessException e)
				{
					LOGGER.warn("Constructor failed.", e);
					
					continue;
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.error("Constructor failed.", e);
					
					continue;
				}
				catch (InstantiationException e)
				{
					LOGGER.error("Constructor failed.", e);
					
					continue;
				}
				catch (NoSuchMethodException e)
				{
					LOGGER.error("Constructor failed.", e);
					
					continue;
				}
				catch (ExceptionInInitializerError e)
				{
					LOGGER.error("Constructor failed.", e);
					
					throw
					(
						new InstantiationFailedException
						(
							e,
							"Static initialization failed for type %s.",
							TYPE.getSimpleName( )
						)
					);
				}
			}
			
			/*
			 * If the above loop finished without a constructor completing
			 * normally, then instantiation has failed.
			 */
			throw
			(
				new InstantiationFailedException
				(
					"No working constructor was found for class %s.",
					TYPE.getSimpleName( )
				)
			);
		}
	}
	
	/**
	 * Creates an instance of the given type.
	 * 
	 * Instantiation can fail for a variety of reasons. If a fatal exception
	 * occurs, an {@code InstantiationFailedException} wrapping it is thrown to
	 * the caller. If instantiation fails simply because a working constructor
	 * couldn't be found, the problems that were encountered are detailed in
	 * logging messages. See {@link http://www.slf4j.org/codes.html#StaticLoggerBinder}
	 * for instructions on how to enable logging.
	 * 
	 * @param TYPE The type to instantiate.
	 * 
	 * @return An instance of {@code TYPE}.
	 * 
	 * @throws InstantiationFailedException If instantiating {@code TYPE} fails for any reason.
	 */
	public <T> T instantiate(final Class<T> TYPE)
	{
		return instantiate(TYPE, EMPTY_MAP);
	}
}