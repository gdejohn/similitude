package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeToken<T>
{
	private static final Logger LOGGER = getLogger(TypeToken.class);
	
	private final Class<T> RAW_TYPE;
	
	private TypeToken(final Class<T> RAW_TYPE)
	{
		LOGGER.debug("Constructing type token.\nRaw type: {}", RAW_TYPE);
		
		this.RAW_TYPE = RAW_TYPE;
	}
	
	public static <T> TypeToken<T> typeOf(final Class<T> RAW_TYPE)
	{
		LOGGER.debug("Getting type of class: {}", RAW_TYPE.getSimpleName( ));
		
		return new TypeToken<T>(RAW_TYPE);
	}
	
	public Class<T> getRawType( )
	{
		return RAW_TYPE;
	}
	
	@Override
	public String toString( )
	{
		return RAW_TYPE.getSimpleName( );
	}
}