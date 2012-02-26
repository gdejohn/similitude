package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

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
	 * Initializes the associated {@code Builder} instance.
	 * 
	 * @param BUILDER The {@code Builder} instance that created {@code this}.
	 */
	Handler(final Builder BUILDER)
	{
		this.BUILDER = BUILDER;
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
			"Method \"{}\" invoked on proxy object.",
			METHOD.toGenericString( )
		);
		
		final Type GENERIC_RETURN_TYPE = METHOD.getGenericReturnType( );
		
		if (GENERIC_RETURN_TYPE instanceof Class)
		{
			return BUILDER.instantiate(METHOD.getReturnType( ));
		}
		else if (GENERIC_RETURN_TYPE instanceof TypeVariable)
		{
			final Type[ ] PARAMETERS = METHOD.getGenericParameterTypes( );
			
			for (int index = 0; index < PARAMETERS.length; index++)
			{ // Check arguments for parameterization of return type.
				try
				{
					if (GENERIC_RETURN_TYPE.equals(PARAMETERS[index]) && ARGUMENTS[index] != null)
					{
						LOGGER.debug
						(
							"Generic return type {} parameterized by argument type {}.",
							GENERIC_RETURN_TYPE,
							ARGUMENTS[index].getClass( ).getCanonicalName( )
						);
						
						return
						(
							BUILDER.instantiate(ARGUMENTS[index].getClass( ))
						);
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
				catch (ArrayIndexOutOfBoundsException e)
				{
					throw
					(
						new InstantiationFailedException
						(
							"Number of arguments and parameters don't match.",
							e
						)
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
}