package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Instantiate arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public final class Builder
{
	static final Logger LOGGER = getLogger(Builder.class);
	
	/**
	 * Default length for new arrays.
	 * 
	 * Arrays created by {@link #instantiateArray(Class, int)} are initialized
	 * to this length.
	 */
	private final int DEFAULT_ARRAY_LENGTH = 0;
	
	/**
	 * Immutable types, mapped to default values.
	 * 
	 * Initialized to include the primitive types, mapped to their default
	 * values as per <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.12.5">JLS 4.12.5</a>,
	 * and {@link Cloner#BASIC_TYPES}.
	 */
	private final Map<Class<?>, Object> DEFAULTS;
	
	{ // Instance initializer, executes at the beginning of every constructor.
		DEFAULTS = new LinkedHashMap<Class<?>, Object>( );
		
		DEFAULTS.put(byte.class, Byte.valueOf((byte)0));
		DEFAULTS.put(short.class, Short.valueOf((short)0));
		DEFAULTS.put(int.class, Integer.valueOf(0));
		DEFAULTS.put(long.class, Long.valueOf(0L));
		DEFAULTS.put(float.class, Float.valueOf(0.0f));
		DEFAULTS.put(double.class, Double.valueOf(0.0d));
		DEFAULTS.put(char.class, Character.valueOf('\u0000'));
		DEFAULTS.put(boolean.class, Boolean.valueOf(false));
		
		DEFAULTS.putAll(Cloner.BASIC_TYPES);
	}
	
	/**
	 * Maps an immutable class to a default value.
	 * 
	 * The bound type parameter {@code U} statically ensures that {@code VALUE}
	 * is assignable to type {@code T}.
	 * 
	 * @param CLASS The immutable class.
	 * @param VALUE The default value for {@code CLASS}.
	 * 
	 * @return {@code null} if {@code CLASS} wasn't already mapped, else the previous mapping.
	 */
	public <T, U extends T> Object addDefault(final Class<T> CLASS, final U VALUE) // final Class<? extends T> CLASS?
	{
		LOGGER.debug
		(
			"Adding default value \"{}\" for type {}.",
			VALUE,
			CLASS.getCanonicalName( )
		);
		
		return DEFAULTS.put(CLASS, VALUE);
	}

	/**
	 * Gets the default value for the given class.
	 * 
	 * The class must have been previously registered as immutable with
	 * {@link #addDefault}, or {@code null} will be returned.
	 *
	 * @param CLASS The class for which to get the default value.
	 *
	 * @return Default value associated with {@code CLASS}, or {@code null}.
	 */
	public <T, U extends T> U getDefault(final Class<T> CLASS) // final Class<? extends T> CLASS?
	{
		/*
		 * Classes are only added to DEFAULTS through addDefault( ), which uses
		 * a bound type parameter to statically ensure that the value to which
		 * a class is mapped is assignable to that class. Therefore, this cast
		 * will always succeed.
		 */
		@SuppressWarnings("unchecked")
		final U VALUE = (U)DEFAULTS.get(CLASS);
		
		LOGGER.debug
		(
			"Getting default value for type {}.",
			CLASS.getCanonicalName( )
		);
		
		return VALUE;
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
			"Instantiating array of type {} and length {}.",
			TYPE.getCanonicalName( ),
			LENGTH
		);
		
		return TYPE.cast(Array.newInstance(TYPE.getComponentType( ), LENGTH));
	}
	
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
					CLASS.getCanonicalName( )
				);
				
				return CLASS.getConstructors( );
			}
		}
		catch (SecurityException e)
		{
			LOGGER.debug
			(
				"Public constructors not available for: {}",
				CLASS.getCanonicalName( )
			);
			
			throw
			(
				new InstantiationFailedException
				(
					"No available constructors.", e
				)
			);
		}
	}
	
	/**
	 * Creates an instance of the given class.
	 * 
	 * Instantiation can fail for a variety of reasons. If a fatal exception
	 * occurs, an {@code InstantiationFailedException} wrapping it is thrown to
	 * the caller. If instantiation fails simply because a working constructor
	 * couldn't be found, the problems that were encountered are detailed in
	 * logging messages. See {@link http://www.slf4j.org/codes.html#StaticLoggerBinder}
	 * for instructions on how to enable logging.
	 * 
	 * @param CLASS The class to instantiate.
	 * 
	 * @return An instance of {@code CLASS}.
	 * 
	 * @throws InstantiationFailedException If instantiating {@code CLASS} fails for any reason.
	 */
	public <T> T instantiate(final Class<T> CLASS)
	{
		if (CLASS == null)
		{
			LOGGER.debug("Class argument is null.");
			
			return null;
		}
		else if (DEFAULTS.containsKey(CLASS))
		{ // Base case (previously added immutable type), return default value.
			return getDefault(CLASS);
		}
		else if (CLASS.isEnum( ))
		{ // Base case, return first declared constant.
			LOGGER.debug
			(
				"Instantiating enum type: {}", CLASS.getCanonicalName( )
			);
			
			try
			{
				return CLASS.getEnumConstants( )[0];
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				throw
				(
					new InstantiationFailedException
					(
						"Enum type declares no constants.", e
					)
				);
			}
		}
		else if (CLASS.isArray( ))
		{ // Base case, return empty array of CLASS's component type.
			return instantiateArray(CLASS, DEFAULT_ARRAY_LENGTH);
		}
		else
		{ // Class type, reflectively invoke each constructor until one works.
			LOGGER.debug
			(
				"Instantiating class type: {}", CLASS.getCanonicalName( )
			);
			
			for (final Constructor<?> CONSTRUCTOR : getConstructors(CLASS))
			{
				LOGGER.debug("Found constructor: {}", CONSTRUCTOR);
				
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
						CLASS.getDeclaredConstructor(PARAMETERS)
					);
					
					/*
					 * Constructors that wouldn't normally be accessible (e.g.
					 * private) need to be made accessible before they can be
					 * invoked.
					 */
					GENERIC_CONSTRUCTOR.setAccessible(true);
					
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
							ARGUMENTS[index] = instantiate(PARAMETERS[index]);
							
							LOGGER.debug
							(
								"Successfully instantiated {} parameter.",
								PARAMETERS[index].getCanonicalName( )
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
						GENERIC_CONSTRUCTOR,
						ARGUMENTS
					);
					
					/*
					 * If newInstance( ) completes normally, then instantiation
					 * was successful, and the result is returned, skipping the
					 * rest of the constructors.
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
							"Static initialization failed.",
							e
						)
					);
				}
			}
			
			/*
			 * If the above for-each loop finishes without a constructor
			 * completing normally, then instantiation has failed.
			 */
			throw
			(
				new InstantiationFailedException
				(
					String.format
					(
						"No working constructor was found for class %s.",
						CLASS.getCanonicalName( )
					)
				)
			);
		}
	}
}