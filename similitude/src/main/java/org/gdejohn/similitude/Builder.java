package org.gdejohn.similitude;

import static java.lang.Math.nextUp;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.unmodifiableMap;
import static org.gdejohn.similitude.Cloner.BASIC_TYPES;
import static org.gdejohn.similitude.TypeToken.typeOf;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Instantiate arbitrary types.
 * 
 * @author Griffin DeJohn
 */
public final class Builder
{
	private static final Logger LOGGER = getLogger(Builder.class);
	
	/**
	 * Primitive types mapped to their respective wrapper types.
	 */
	@SuppressWarnings("serial")
	private static final Map<TypeToken<?>, TypeToken<?>> WRAPPERS =
	(
		unmodifiableMap
		(
			new LinkedHashMap<TypeToken<?>, TypeToken<?>>(8, nextUp(1.0f))
			{
				<T> void map(final Class<T> PRIMITIVE, final Class<T> WRAPPER)
				{
					if (PRIMITIVE.isPrimitive( ))
					{
						if (WRAPPER.isPrimitive( ))
						{
							throw
							(
								new RuntimeException
								(
									"WRAPPER must not be primitive."
								)
							);
						}
						else if (put(typeOf(PRIMITIVE), typeOf(WRAPPER)) == null)
						{ // No previous mapping.
							return;
						}
						else
						{ // If put() didn't return null, then a mapping already existed.
							throw
							(
								new RuntimeException
								(
									"PRIMITIVE already mapped."
								)
							);
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
				
				/*
				 * Instance initializer.
				 */
				{
					map(byte.class, Byte.class);
					map(short.class, Short.class);
					map(int.class, Integer.class);
					map(long.class, Long.class);
					map(float.class, Float.class);
					map(double.class, Double.class);
					map(char.class, Character.class);
					map(boolean.class, Boolean.class);
				}
			}
		)
	);
	
	/**
	 * Gets the wrapper for the given primitive type.
	 * 
	 * @param PRIMITIVE The primitive type for which to get the corresponding wrapper.
	 * 
	 * @return The wrapper corresponding to {@code PRIMITIVE}.
	 */
	private static <T> TypeToken<T>	getWrapper(final TypeToken<T> PRIMITIVE)
	{
		/*
		 * Mappings are only added to WRAPPERS if this cast is appropriate.
		 */
		@SuppressWarnings("unchecked")
		final TypeToken<T> WRAPPER = (TypeToken<T>)WRAPPERS.get(PRIMITIVE);
		
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
	private final Map<TypeToken<?>, Object> IMMUTABLE_DEFAULTS;
	
	{ // Instance initializer, executes at the beginning of every constructor.
		IMMUTABLE_DEFAULTS =
		(
			new LinkedHashMap<TypeToken<?>, Object>(BASIC_TYPES)
		);
	}
	
	/**
	 * Gets a read-only view of {@code this} builder's immutable defaults.
	 * 
	 * @return A read-only view of {@code this} builder's immutable defaults.
	 */
	public Map<TypeToken<?>, Object> getAllDefaults( )
	{
		return unmodifiableMap(IMMUTABLE_DEFAULTS);
	}
	
	/**
	 * Checks if a given type is associated with a default value.
	 * 
	 * @param TYPE The type to check.
	 * 
	 * @return {@code true} if {@code TYPE} is associated with a default value, else {@code false}.
	 */
	public boolean hasDefault(final TypeToken<?> TYPE)
	{
		return IMMUTABLE_DEFAULTS.containsKey(TYPE);
	}

	/**
	 * Gets the default value for the given type.
	 * 
	 * The type must have been previously registered as immutable with {@link
	 * #addDefault}, or {@code null} will be returned. Primitive types aren't
	 * actually mapped. Instead, the default values of their corresponding
	 * wrapper types are used.
	 * 
	 * @param <T> The type represented by {@code TYPE}.
	 * @param TYPE The type for which to get the default value.
	 *
	 * @return Default value associated with {@code TYPE}, or {@code null}.
	 */
	public <T> T getDefault(final TypeToken<T> TYPE)
	{
		final Class<T> CLASS = TYPE.getRawType( );
		
		LOGGER.debug
		(
			"Getting default value for type {}.",
			CLASS.getSimpleName( )
		);
		
		return CLASS.cast(IMMUTABLE_DEFAULTS.get(TYPE));
	}
	
	/**
	 * Maps an immutable class to a default value.
	 * 
	 * The bound type parameter {@code U} statically ensures that {@code VALUE}
	 * is assignable to {@code TYPE}. Primitive types are a special case. They
	 * use the default values of their corresponding wrappers and cannot
	 * themselves be mapped. Attempting to do so will throw an exception.
	 * 
	 * @param <T> The type represented by {@code TYPE}.
	 * @param <V> The type of {@code VALUE}, either {@code T} or a subtype of {@code T}.
	 * @param TYPE The immutable type.
	 * @param VALUE The default value for {@code TYPE}.
	 * 
	 * @return {@code null} if {@code TYPE} wasn't already mapped, else the previous mapping.
	 * 
	 * @throws IllegalArgumentException If {@code TYPE} is primitive.
	 */
	public <T, V extends T> Object addDefault(final TypeToken<T> TYPE, final V VALUE)
	{
		final Class<T> CLASS = TYPE.getRawType( );
		
		LOGGER.debug
		(
			"Adding default value \"{}\" for type {}.",
			VALUE,
			CLASS.getSimpleName( )
		);
		
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
			return IMMUTABLE_DEFAULTS.put(TYPE, VALUE);
		}
	}
	
	/**
	 * Removes the mapping for a given type to its associated default value.
	 * 
	 * @param TYPE The type to remove the mapping to a default value for.
	 * 
	 * @return The default value to which {@code TYPE} was mapped, if the mapping existed.
	 */
	public Object removeDefault(final TypeToken<?> TYPE)
	{
		return IMMUTABLE_DEFAULTS.remove(TYPE);
	}
	
	/**
	 * Resets immutable types to default values.
	 * 
	 * After this method returns, any previous user-added immutable type
	 * default values will have been removed, leaving {@code String} and the
	 * primitive wrappers.
	 * 
	 * @return {@code true} if the immutable defaults are changed.
	 */
	public boolean reset( )
	{
		final boolean CHANGED =
		(
			IMMUTABLE_DEFAULTS.keySet( ).retainAll(BASIC_TYPES.keySet( ))
		);
		
		LOGGER.debug
		(
			"Resetting immutable defaults: {}",
			CHANGED ? "changed" : "unchanged"
		);
		
		return CHANGED;
	}
	
	/**
	 * Creates an instance of the given type.
	 * 
	 * {@link #instantiate(Class)} delegates to this method.
	 * 
	 * @param <T> The type represented by {@code TYPE}.
	 * @param TYPE The type to instantiate.
	 * 
	 * @return An instance of {@code TYPE}.
	 * 
	 * @throws InstantiationFailedException If instantiating {@code TYPE} fails for any reason.
	 */
	public <T> T instantiate(final TypeToken<T> TYPE)
	{
		if (TYPE == null)
		{
			LOGGER.debug("Class argument is null.");
			
			return null;
		}
		
		final Class<T> CLASS = TYPE.getRawType( );
		
		if (CLASS.isPrimitive( ))
		{
			/*
			 * The default values are all reference types, which throw
			 * ClassCastExceptions when reflectively cast to primitive types.
			 * As a workaround, the corresponding wrapper must be used, which
			 * works because for any primitive type, P, and its wrapper, W,
			 * P.class and W.class are both of type Class<W>.
			 */
			return getDefault(getWrapper(TYPE));
		}
		else if (hasDefault(TYPE))
		{ // Base case (previously added immutable type), return default value.
			return getDefault(TYPE);
		}
		else if (CLASS.isEnum( ))
		{ // Base case, return first declared constant.
			LOGGER.debug
			(
				"Instantiating enum type: {}", CLASS.getSimpleName( )
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
						e,
						"Enum type %s declares no constants.",
						CLASS.getSimpleName( )
					)
				);
			}
		}
		else if (CLASS.isArray( ))
		{ // Base case, return empty array of CLASS's component type.
			return CLASS.cast(newInstance(CLASS.getComponentType( ), 0));
		}
		else if (CLASS.isInterface( ))
		{ // Base case, return dynamic proxy.
			LOGGER.debug
			(
				"Creating proxy for interface {}.", CLASS.getSimpleName( )
			);
			
			final ClassLoader LOADER = CLASS.getClassLoader( );
			
			final Class<?>[ ] INTERFACES = new Class<?>[ ] {CLASS};
			
			final InvocationHandler HANDLER =
			(
				new InvocationHandler( )
				{
					@Override
					public Object invoke(final Object PROXY, final Method METHOD, final Object[ ] ARGUMENTS)
					{
						return
						(
							instantiate(TYPE.getReturnType(METHOD, ARGUMENTS))
						);
					}
				}
			);
			
			return CLASS.cast(newProxyInstance(LOADER, INTERFACES, HANDLER));
		}
		else if (isAbstract(CLASS.getModifiers( )))
		{
			throw
			(
				new InstantiationFailedException
				(
					new UnsupportedOperationException("Abstract class."),
					"Can't instantiate class %s.",
					CLASS.getSimpleName( )
				)
			);
		}
		else
		{ // Concrete class type, try constructors until one works.
			LOGGER.debug
			(
				"Instantiating class type: {}", CLASS.getSimpleName( )
			);
			
			for (final Constructor<T> CONSTRUCTOR : TYPE.getAccessibleConstructors( ))
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
					
					final Type[ ] GENERIC_PARAMETERS =
					(
						CONSTRUCTOR.getGenericParameterTypes( )
					);
					
					final Object[ ] ARGUMENTS =
					(
						new Object[PARAMETERS.length]
					);
					
					/*
					 * Workaround for inner class bug where synthetic parameter
					 * for enclosing instance isn't reflected in the generic
					 * parameters. This will continue working when the bug is
					 * fixed, no changes required.
					 * 
					 * http://bugs.sun.com/view_bug.do?bug_id=5087240
					 */
					final int OFFSET =
					(
						PARAMETERS.length - GENERIC_PARAMETERS.length
					);
					
					if (OFFSET == 1)
					{
						ARGUMENTS[0] =
						(
							instantiate(typeOf(PARAMETERS[0], TYPE))
						);
					}
					else if (OFFSET != 0)
					{
						LOGGER.error
						(
							"Unexpected discrepancy between raw parameters and generic parameters.\n{}\n{}",
							PARAMETERS,
							GENERIC_PARAMETERS
						);
						
						throw new RuntimeException("");
					}
					
					/*
					 * Recursively instantiate arguments to satisfy the current
					 * constructor's parameters.
					 */
					for (int index = OFFSET; index < PARAMETERS.length; index++)
					{
						try
						{
							ARGUMENTS[index] =
							(
								instantiate
								(
									typeOf(GENERIC_PARAMETERS[index], TYPE)
								)
							);
							
							LOGGER.debug
							(
								"Successfully instantiated {} parameter.",
								GENERIC_PARAMETERS[index]
							);
						}
						catch (InstantiationFailedException e)
						{
							LOGGER.debug
							(
								"Couldn't instantiate parameter, using null.",
								e
							);
							
							/*
							 * Pass null to the constructor for the current
							 * parameter.
							 */
							ARGUMENTS[index] = null;
							
							continue;
						}
					}
					
					LOGGER.debug
					(
						"Invoking constructor \"{}\" with arguments: {}",
						CONSTRUCTOR.toGenericString( ),
						ARGUMENTS
					);
					
					/*
					 * If newInstance() completes normally, then instantiation
					 * was successful, and the result is returned, skipping the
					 * rest of the loop. If an exception is thrown at any point
					 * in this try block, it's caught and logged, and the loop
					 * continues, trying the next constructor.
					 */
					return CONSTRUCTOR.newInstance(ARGUMENTS);
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
				catch (ExceptionInInitializerError e)
				{
					LOGGER.error("Constructor failed.", e);
					
					throw
					(
						new InstantiationFailedException
						(
							e,
							"Static initialization failed for type %s.",
							CLASS.getSimpleName( )
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
					CLASS.getSimpleName( )
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
	 * logging messages. See <a href="http://www.slf4j.org/codes.html#StaticLoggerBinder">slf4j.org</a>
	 * for instructions on how to enable logging.
	 * 
	 * @param <T> The type represented by {@code CLASS}.
	 * @param CLASS The class to instantiate.
	 * 
	 * @return An instance of {@code TYPE}.
	 * 
	 * @throws InstantiationFailedException If instantiating {@code TYPE} fails for any reason.
	 */
	public <T> T instantiate(final Class<T> CLASS)
	{
		return instantiate(typeOf(CLASS));
	}
}