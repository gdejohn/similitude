package org.gdejohn.similitude;

import static java.lang.Math.nextUp;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Proxy.getProxyClass;
import static java.lang.reflect.Proxy.isProxyClass;
import static java.util.Collections.unmodifiableMap;
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
	@SuppressWarnings("serial")
	static final Map<TypeToken<?>, TypeToken<?>> WRAPPERS =
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
							throw new RuntimeException("WRAPPER must not be primitive.");
						}
						else if (put(typeOf(PRIMITIVE), typeOf(WRAPPER)) == null)
						{ // No previous mapping.
							return;
						}
						else
						{ // If put() didn't return null, then a mapping already existed.
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
	 * Gets the wrapper corresponding to the given primitive type.
	 * 
	 * @param PRIMITIVE The primitive type for which to get the corresponding wrapper.
	 * 
	 * @return The wrapper corresponding to {@code PRIMITIVE}.
	 */
	private static <T> TypeToken<T>	wrap(final TypeToken<T> PRIMITIVE)
	{
		/*
		 * Mappings are only added to WRAPPERS if they would allow this cast to
		 * succeed.
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
			new LinkedHashMap<TypeToken<?>, Object>(Cloner.BASIC_TYPES)
		);
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
	 * Creates an array of the given type and length.
	 * 
	 * Each element is initialized to {@code null}.
	 * 
	 * @param TYPE The type of the array to create (e.g. {@code String[ ]}).
	 * @param DIMENSIONS The dimensions of the array to create.
	 * 
	 * @return An array of the given type and length.
	 * 
	 * @throws NullPointerException If {@code TYPE} is null, or doesn't represent an array type.
	 * @throws NegativeArraySizeException If {@code LENGTH} is negative.
	 */
	public <T> T[ ] instantiateArray(final Class<? extends T[ ]> TYPE, final int... DIMENSIONS)
	{
		LOGGER.debug
		(
			"Instantiating array of type {} and dimensions: {}",
			TYPE.getSimpleName( ),
			DIMENSIONS
		);
		
		Class<?> componentType = TYPE;
		
		for (int index = 0; index < DIMENSIONS.length; index++)
		{
			componentType = componentType.getComponentType( );
		}
		
		return
		(
			TYPE.cast
			(
				newInstance
				(
					componentType,
					DIMENSIONS
				)
			)
		);
	}
	
	/**
	 * Creates an instance of the given type, which may be parameterized.
	 * 
	 * {@link #instantiate(Class)} delegates to this method.
	 * 
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
			return getDefault(wrap(TYPE));
		}
		else if (IMMUTABLE_DEFAULTS.containsKey(TYPE))
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
		else if (isProxyClass(CLASS))
		{
			try
			{
				final Constructor<T> CONSTRUCTOR =
				(
					CLASS.getConstructor(InvocationHandler.class)
				);
				
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
				
				return CONSTRUCTOR.newInstance(HANDLER);
			}
			catch (final Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else if (CLASS.isInterface( ))
		{ // Base case, return dynamic proxy.
			LOGGER.debug
			(
				"Creating proxy for interface {}.", CLASS.getSimpleName( )
			);
			
			final ClassLoader LOADER = CLASS.getClassLoader( );
			
			final Class<?>[ ] INTERFACES = new Class<?>[ ] {CLASS};
			
			final Class<?> PROXY = getProxyClass(LOADER, INTERFACES);;
			
			return CLASS.cast(instantiate(typeOf(PROXY, TYPE)));
		}
		else if (isAbstract(CLASS.getModifiers( )))
		{
			throw
			(
				new InstantiationFailedException
				(
					"Can't instantiate abstract class %s.",
					CLASS.getSimpleName( )
				)
			);
		}
		else
		{ // Class type, reflectively invoke each constructor until one works.
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
	 * logging messages. See {@link "http://www.slf4j.org/codes.html#StaticLoggerBinder"}
	 * for instructions on how to enable logging.
	 * 
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
	
	public static class Foo<F>
	{
		public class Bar<E>
		{
			public Bar(E arg, F farg)
			{
				
			}
		}
	}
	
	public static void main(String[ ] args)
	{
		String[][] s = new Builder( ).instantiateArray(String[][].class, 1, 1, 1);
		System.out.println(java.util.Arrays.deepToString(s));
		//System.out.println("Raw: " + java.util.Arrays.toString(Foo.Bar.class.getConstructors( )[0].getParameterTypes( )));
		//System.out.println("Gen: " + java.util.Arrays.toString(Foo.Bar.class.getConstructors( )[0].getGenericParameterTypes( )));
	}
}