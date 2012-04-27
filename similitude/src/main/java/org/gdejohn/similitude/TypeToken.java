package org.gdejohn.similitude;

import static java.lang.Integer.valueOf;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeToken<T>
{
	private static final Logger LOGGER = getLogger(TypeToken.class);
	
	private final Class<T> RAW_TYPE;
	
	private Set<Field> instanceFields = null;
	
	private Integer hashCode = null;
	
	private String toString = null;
	
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
	
	private static Set<Field> getAllInstanceFields(Class<?> type)
	{
		final Set<Field> INSTANCE_FIELDS = new LinkedHashSet<Field>( );
		
		while (type != null)
		{
			for (final Field FIELD : type.getDeclaredFields( ))
			{
				if (isStatic(FIELD.getModifiers( )))
				{ // If static, ignore and skip to the next one.
					continue;
				}
				else
				{
					LOGGER.debug("Found instance field: {}", FIELD);
					
					if (INSTANCE_FIELDS.add(FIELD) == false)
					{
						throw new RuntimeException("Field already added.");
					}
				}
			}
			
			type = type.getSuperclass( );
		}
		
		return unmodifiableSet(INSTANCE_FIELDS);
	}
	
	public Set<Field> getAllInstanceFields( )
	{
		if (instanceFields == null)
		{ // First time this method has been invoked on this instance.
			instanceFields = getAllInstanceFields(RAW_TYPE);
			
			if (instanceFields == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return instanceFields;
	}
	
	@Override
	public int hashCode( )
	{
		if (hashCode == null)
		{ // First time this method has been invoked on this instance.
			hashCode = valueOf(RAW_TYPE.hashCode( ));
			
			if (hashCode == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return hashCode.intValue( );
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
		if (toString == null)
		{
			toString = RAW_TYPE.getSimpleName( );
			
			if (toString == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return toString;
	}
}