package org.gdejohn.similitude;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeToken<E>
{
	static final Logger LOGGER = getLogger(TypeToken.class);
	
	private static final Set<Field> EMPTY_SET = emptySet( );
	
	private final Class<E> RAW_TYPE;
	
	private final Set<Field> INSTANCE_FIELDS;
	
	private static Set<Field> findInstanceFields(final Class<?> CLASS)
	{
		if (CLASS == null)
		{ // Base case, top of the hierarchy has been reached.
			return EMPTY_SET;
		}
		else
		{
			final Set<Field> INSTANCE_FIELDS = new LinkedHashSet<Field>( );
			
			for (final Field FIELD : CLASS.getDeclaredFields( ))
			{
				if (isStatic(FIELD.getModifiers( )))
				{ // If static, ignore and skip to the next one.
					LOGGER.debug
					(
						"Skipping static field: {}", FIELD
					);
					
					continue;
				}
				else
				{
					LOGGER.debug
					(
						"Found instance field: {}", FIELD
					);
					
					if (INSTANCE_FIELDS.add(FIELD) == false)
					{
						throw new RuntimeException("Field already added.");
					}
				}
			}
			
			for (final Field FIELD : findInstanceFields(CLASS.getSuperclass( )))
			{ // Recursively get all inherited fields.
				if (INSTANCE_FIELDS.add(FIELD) == false)
				{
					throw new RuntimeException("Field already added.");
				}
			}
			
			return INSTANCE_FIELDS;
		}
	}
	
	private TypeToken(final Class<E> RAW_TYPE)
	{
		LOGGER.debug
		(
			"Constructing type token, class: {}", RAW_TYPE.getName( )
		);
		
		INSTANCE_FIELDS = unmodifiableSet(findInstanceFields(RAW_TYPE));
		this.RAW_TYPE = RAW_TYPE;
	}
	
	public static <T> TypeToken<T> getTypeOf(final Class<T> RAW_TYPE)
	{
		LOGGER.debug("Getting type of class: {}", RAW_TYPE.getName( ));
		
		return new TypeToken<T>(RAW_TYPE);
	}
	
	public static <T> TypeToken<? extends T> getTypeOf(final T INSTANCE)
	{
		LOGGER.debug("Getting type of object: {}", INSTANCE);
		
		@SuppressWarnings("unchecked")
		final Class<? extends T> RAW_TYPE =
		(
			(Class<? extends T>)INSTANCE.getClass( )
		);
		
		return getTypeOf(RAW_TYPE);
	}
	
	public Class<E> getRawType( )
	{
		return RAW_TYPE;
	}
	
	public Set<Field> getAllInstanceFields( )
	{
		return INSTANCE_FIELDS;
	}
}