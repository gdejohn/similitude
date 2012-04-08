package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Handles calls to methods on proxy objects.
 * 
 * When an interface type is passed to {@link Builder#instantiate(Class)},
 * the resulting instance is a proxy object that will use an instance of
 * this class to handle methods invoked on it.
 * 
 * @author Griffin DeJohn
 */
class Handler implements InvocationHandler
{
	static final Logger LOGGER = getLogger(Handler.class);
	
	/**
	 * The {@code Builder} instance that created {@code this}.
	 */
	private final Builder BUILDER;
	
	/**
	 * The type arguments parameterizing the type being proxied.
	 */
	private final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS;
	
	/**
	 * Initializes the associated {@code Builder} instance.
	 * 
	 * @param BUILDER The {@code Builder} instance that created {@code this}.
	 */
	Handler(final Builder BUILDER, final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS)
	{
		this.BUILDER = BUILDER;
		this.TYPE_ARGUMENTS = TYPE_ARGUMENTS;
	}
	
	Class<?> reify(final Type TYPE, final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS)
	{
		if (TYPE instanceof Class)
		{
			return (Class<?>)TYPE;
		}
		else if (TYPE_ARGUMENTS.containsKey(TYPE))
		{
			return reify(TYPE_ARGUMENTS.get(TYPE), TYPE_ARGUMENTS);
		}
		else if (TYPE instanceof GenericArrayType)
		{
			return
			(
				reify
				(
					((GenericArrayType)TYPE).getGenericComponentType( ),
					TYPE_ARGUMENTS
				)
			);
		}
		
		return null;
	}
	
	/**
	 * Processes a method invocation on a proxy instance.
	 * 
	 * The result is an instance of the invoked method's declared return
	 * type, as created by {@link Builder#instantiate(Class)}.
	 * 
	 * @param PROXY The proxy object on which {@code METHOD} was invoked.
	 * @param METHOD The method invoked on {@code PROXY}.
	 * @param ARGUMENTS The arguments passed to satisfy the parameters of {@code METHOD}.
	 * 
	 * @return An instance of the return type of {@code METHOD}.
	 * 
	 * @throws InstantiationFailedException If the return type of {@code METHOD} can't be instantiated.
	 */
	@Override
	public Object invoke(final Object PROXY, final Method METHOD, final Object[ ] ARGUMENTS)
	{
		LOGGER.debug
		(
			"Method \"{}\" invoked on proxy instance implementing: {}",
			METHOD.toGenericString( ),
			PROXY.getClass( ).getInterfaces( )
		);
		
		final Type RETURN_TYPE = METHOD.getGenericReturnType( );
		
		if (RETURN_TYPE instanceof Class)
		{
			return BUILDER.instantiate(METHOD.getReturnType( ));
		}
		else if (RETURN_TYPE instanceof GenericArrayType)
		{
			LOGGER.debug
			(
				"Generic array return type for method: {}",
				METHOD.toGenericString( )
			);
			
			Type componentType = RETURN_TYPE;
			
			int dimensions = 0;
			
			do
			{
				componentType =
				(
					((GenericArrayType)componentType).getGenericComponentType( )
				);
				
				dimensions++;
			}
			while (componentType instanceof GenericArrayType);
			
			final Class<?> ACTUAL_COMPONENT_TYPE;
			
			if (componentType instanceof ParameterizedType)
			{
				ACTUAL_COMPONENT_TYPE =
				(
					(Class<?>)((ParameterizedType)componentType).getRawType( )
				);
			}
			else if (componentType instanceof TypeVariable)
			{
				ACTUAL_COMPONENT_TYPE =
				(
					ARGUMENTS[0].getClass( )
				);
			}
			else
			{
				throw
				(
					new InstantiationFailedException
					(
						"Couldn't reify generic return type."
					)
				);
			}
			
			return
			(
				Array.newInstance
				(
					ACTUAL_COMPONENT_TYPE, new int[dimensions] // (Class<?>)componentType
				)
			);
		}
		else if (RETURN_TYPE instanceof ParameterizedType)
		{
			LOGGER.debug
			(
				"Paramaterized return type for method: {}",
				METHOD.toGenericString( )
			);
			
			return
			(
				BUILDER.instantiate
				(
					(Class<?>)((ParameterizedType)RETURN_TYPE).getRawType( ),
					BUILDER.getTypeArguments
					(
						RETURN_TYPE, TYPE_ARGUMENTS, ARGUMENTS
					)
				)
			);
		}
		else if (RETURN_TYPE instanceof TypeVariable)
		{
			LOGGER.debug
			(
				"Type variable return type for method: {}",
				METHOD.toGenericString( )
			);
			
			if (TYPE_ARGUMENTS != null && TYPE_ARGUMENTS.containsKey(RETURN_TYPE))
			{
				return
				(
					BUILDER.instantiate
					(
						(Class<?>)TYPE_ARGUMENTS.get(RETURN_TYPE),
						TYPE_ARGUMENTS
					)
				);
			}
			else
			{
				final Type[ ] PARAMETERS = METHOD.getGenericParameterTypes( );
				
				for (int index = 0; index < PARAMETERS.length; index++)
				{ // Check arguments for parameterization of return type.
					try
					{
						LOGGER.debug("Parameter: {}, Argument: {}", PARAMETERS[index], ARGUMENTS[index]); /////////////////////////////////////////////////////////////////////////////
						
						if(ARGUMENTS[index] != null)
						{
							if (RETURN_TYPE.equals(PARAMETERS[index]))
							{
								LOGGER.debug
								(
									"Generic return type {} parameterized by argument type {}.",
									RETURN_TYPE,
									ARGUMENTS[index].getClass( ).getSimpleName( )
								);
								
								return
								(
									BUILDER.instantiate(ARGUMENTS[index].getClass( ))
								);
							}
							else if(PARAMETERS[index] instanceof GenericArrayType)
							{
								if (RETURN_TYPE.equals(((GenericArrayType)PARAMETERS[index]).getGenericComponentType( )))
								{
									return BUILDER.instantiate(ARGUMENTS[index].getClass( ).getComponentType( ));
								}
							}
						}
					}
					catch (InstantiationFailedException e)
					{
						LOGGER.debug
						(
							"Instantiating argument type failed.",
							e
						);
					}
				}
				
				throw
				(
					new InstantiationFailedException
					(
						"Couldn't find parameterization of generic return type."
					)
				);
			}
		}
		else
		{
			throw
			(
				new InstantiationFailedException
				(
					"Couldn't instantiate return type."
				)
			);
		}
	}
	
	static abstract class Foo<E, F extends E, G extends F, H extends Number & Comparable<Comparable<Comparable<H>>> & java.util.List<? super G>>
	{
		abstract H bar( );
		
		abstract java.util.List<? super G>[ ] foo(java.util.List<? super Comparable<G>> arg);
		
		abstract <T> T[ ][ ] baz( );
		
		class Inner
		{
			H method( )
			{
				return null;
			}
		}
	}
	
	public static void main(String[ ] args) throws Exception
	{
		System.out.println("" instanceof CharSequence);
		Type type = Handler.Foo.class.getDeclaredMethod("baz").getGenericReturnType( );
		System.out.println(((GenericArrayType) ((GenericArrayType) type).getGenericComponentType( )).getGenericComponentType( ));
		//System.out.println(java.util.Arrays.toString(((TypeVariable<?>)((TypeVariable<?>)(((TypeVariable<?>)((ParameterizedType)((TypeVariable<?>)type).getBounds( )[2]).getActualTypeArguments( )[0])).getBounds( )[0]).getBounds( )[0]).getBounds( )));
	}
}