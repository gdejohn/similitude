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
	
	public static <T> TypeToken<T> typeOf(final Class<T> CLASS)
	{
		LOGGER.debug("Getting type of class: {}", CLASS.getSimpleName( ));
		
		return new TypeToken<T>(CLASS);
	}
	
	public static <T> TypeToken<? extends T> typeOf(final T OBJECT)
	{
		LOGGER.debug("Getting type of object: {}", OBJECT);
		
		@SuppressWarnings("unchecked")
		final Class<? extends T> RAW_TYPE =
		(
			(Class<? extends T>)OBJECT.getClass( )
		);
		
		return typeOf(RAW_TYPE);
	}
	
	public Class<T> getRawType( )
	{
		return RAW_TYPE;
	}
	
	@Override
	public boolean equals(final Object THAT)
	{
		if (THAT instanceof TypeToken)
		{
			final TypeToken<?> THAT_TYPE_TOKEN = (TypeToken<?>)THAT;

			return
			(
				RAW_TYPE.equals(THAT_TYPE_TOKEN.getRawType( ))
			);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public String toString( )
	{
		return RAW_TYPE.getSimpleName( );
	}
}