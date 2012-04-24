package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
	 * The type being proxied.
	 */
	private final TypeToken<?> TYPE;
	
	/**
	 * Initializes the associated {@code Builder} instance.
	 * 
	 * @param BUILDER The {@code Builder} instance that created {@code this}.
	 */
	Handler(final Builder BUILDER, final TypeToken<?> TYPE)
	{
		this.BUILDER = BUILDER;
		this.TYPE = TYPE;
	}
	
	/**
	 * Processes a method invocation on a proxy instance.
	 * 
	 * The result is an instance of the invoked method's declared return
	 * type, as created by {@link Builder#instantiate(TypeToken)}.
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
		return BUILDER.instantiate(TYPE.getReturnType(METHOD, ARGUMENTS));
	}
}