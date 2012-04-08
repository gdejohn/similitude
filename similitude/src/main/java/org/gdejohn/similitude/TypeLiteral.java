package org.gdejohn.similitude;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public final class TypeLiteral<E>
{
	static final Logger LOGGER = getLogger(TypeLiteral.class);
	
	private static final Map<TypeVariable<?>, TypeLiteral<?>> EMPTY_MAP =
	(
		emptyMap( )
	);
	
	private static final Set<Field> EMPTY_SET = emptySet( );
	
	private final Class<E> RAW_TYPE;
	
	private final Map<TypeVariable<?>, TypeLiteral<?>> TYPE_ARGUMENTS;
	
	/*
	 * The unchecked casts to Class<E> in this constructor can only happen when
	 * the corresponding type argument is guaranteed to be java.lang.Object,
	 * preventing unexpected ClassCastExceptions later on.
	 */
	@SuppressWarnings("unchecked")
	private TypeLiteral(final Class<E> RAW_TYPE, final Type GENERIC_TYPE, final TypeLiteral<?> PARENT, final Object... ARGUMENTS)
	{
		final ParameterizedType PARAMETERIZED_TYPE;
		
		if (RAW_TYPE == null)
		{
			if (GENERIC_TYPE == null)
			{
				throw
				(
					new RuntimeException
					(
						"RAW_TYPE and GENERIC_TYPE can't both be null."
					)
				);
			}
			else if (GENERIC_TYPE instanceof Class)
			{
				this.RAW_TYPE = (Class<E>)GENERIC_TYPE;
				
				PARAMETERIZED_TYPE = null;
			}
			else if (GENERIC_TYPE instanceof TypeVariable)
			{
				final TypeLiteral<?> RESOLVED_TYPE =
				(
					resolve((TypeVariable<?>)GENERIC_TYPE, PARENT, ARGUMENTS)
				);
				
				this.RAW_TYPE = (Class<E>)RESOLVED_TYPE.getRawType( );
				
				this.TYPE_ARGUMENTS = RESOLVED_TYPE.getAllTypeArguments( );
				
				return;
			}
			else if (GENERIC_TYPE instanceof ParameterizedType)
			{
				PARAMETERIZED_TYPE = (ParameterizedType)GENERIC_TYPE;
				
				final Type TYPE = PARAMETERIZED_TYPE.getRawType( );
				
				if (TYPE instanceof Class)
				{
					this.RAW_TYPE = (Class<E>)TYPE;
				}
				else
				{
					throw new RuntimeException("Problem?");
				}
			}
			else
			{
				throw
				(
					new RuntimeException
					(
						"Implementation of java.lang.reflect.Type not recognized."
					)
				);
			}
		}
		else if (GENERIC_TYPE == null)
		{
			this.RAW_TYPE = RAW_TYPE;
			
			PARAMETERIZED_TYPE = null;
		}
		else
		{
			throw
			(
				new RuntimeException
				(
					"Either RAW_TYPE or GENERIC_TYPE must be null."
				)
			);
		}
		
		final TypeVariable<?>[ ] TYPE_PARAMETERS =
		(
			this.RAW_TYPE.getTypeParameters( )
		);
		
		if (TYPE_PARAMETERS == null || TYPE_PARAMETERS.length == 0)
		{
			TYPE_ARGUMENTS = EMPTY_MAP;
		}
		else
		{
			TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeLiteral<?>>( )
			);
			
			if (PARAMETERIZED_TYPE == null)
			{
				if (ARGUMENTS == null || ARGUMENTS.length != 1)
				{
					throw new RuntimeException("Problem?");
				}
				else
				{
					for (int index = 0; index < TYPE_PARAMETERS.length; index++)
					{
						TYPE_ARGUMENTS.put
						(
							TYPE_PARAMETERS[index],
							resolve
							(
								TYPE_PARAMETERS[index],
								this.RAW_TYPE,
								null,
								ARGUMENTS[0]
							)
						);
					}
				}
			}
			else
			{
				final Type[ ] ACTUAL_TYPE_ARGUMENTS =
				(
					PARAMETERIZED_TYPE.getActualTypeArguments( )
				);
				
				for (int index = 0; index < TYPE_PARAMETERS.length; index++)
				{
					TYPE_ARGUMENTS.put
					(
						TYPE_PARAMETERS[index],
						getTypeOf
						(
							ACTUAL_TYPE_ARGUMENTS[index],
							PARENT,
							ARGUMENTS
						)
					);
				}
			}
		}
	}
	
	private static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final Type GENERIC_TYPE, final TypeLiteral<?> PARENT, final Object... ARGUMENTS)
	{
		return new TypeLiteral<T>(RAW_TYPE, GENERIC_TYPE, PARENT, ARGUMENTS);
	}
	
	/*
	
	@SuppressWarnings("unused")
	private static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final Type GENERIC_TYPE, final TypeLiteral<?> PARENT)
	{
		return getTypeOf(RAW_TYPE, GENERIC_TYPE, PARENT, new Object[0]);
	}
	
	private static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final Type GENERIC_TYPE, final Object... ARGUMENTS)
	{
		return getTypeOf(RAW_TYPE, GENERIC_TYPE, null, ARGUMENTS);
	}
	
	@SuppressWarnings("unused")
	private static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final Type GENERIC_TYPE)
	{
		return getTypeOf(RAW_TYPE, GENERIC_TYPE, new Object[0]);
	}
	
	*/
	
	static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final TypeLiteral<?> PARENT, final Object... ARGUMENTS)
	{
		return getTypeOf(RAW_TYPE, null, PARENT, ARGUMENTS);
	}
	
	static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final TypeLiteral<?> PARENT)
	{
		return getTypeOf(RAW_TYPE, PARENT, new Object[0]);
	}
	
	static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE, final Object... ARGUMENTS)
	{
		return getTypeOf(RAW_TYPE, (TypeLiteral<?>)null, ARGUMENTS);
	}
	
	public static <T> TypeLiteral<T> getTypeOf(final Class<T> RAW_TYPE)
	{
		return getTypeOf(RAW_TYPE, new Object[0]);
	}
	
	static TypeLiteral<?> getTypeOf(final Type GENERIC_TYPE, final TypeLiteral<?> PARENT, final Object... ARGUMENTS)
	{
		return getTypeOf(null, GENERIC_TYPE, PARENT, ARGUMENTS);
	}
	
	static TypeLiteral<?> getTypeOf(final Type GENERIC_TYPE, final TypeLiteral<?> PARENT)
	{
		return getTypeOf(GENERIC_TYPE, PARENT, new Object[0]);
	}
	
	static TypeLiteral<?> getTypeOf(final Type GENERIC_TYPE, final Object... ARGUMENTS)
	{
		return getTypeOf(GENERIC_TYPE, null, ARGUMENTS);
	}
	
	public static TypeLiteral<?> getTypeOf(final Type GENERIC_TYPE)
	{
		return getTypeOf(GENERIC_TYPE, new Object[0]);
	}
	
	public static <T> TypeLiteral<? extends T> getTypeOf(final T INSTANCE)
	{
		@SuppressWarnings("unchecked")
		final Class<? extends T> RAW_TYPE =
		(
			(Class<? extends T>)INSTANCE.getClass( )
		);
		
		return getTypeOf(RAW_TYPE, INSTANCE);
	}

	private static Set<Field> getInstanceFields(final Class<?> CLASS)
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
			
			for (final Field FIELD : getInstanceFields(CLASS.getSuperclass( )))
			{ // Recursively get all inherited fields.
				if (INSTANCE_FIELDS.add(FIELD) == false)
				{
					throw new RuntimeException("Field already added.");
				}
			}
			
			return unmodifiableSet(INSTANCE_FIELDS);
		}
	}
	
	public static void main(String[ ] args)
	{
		class Class<E>
		{
			@SuppressWarnings("unused")
			E field;
			
			Class(E arg)
			{
				field = arg;
			}
		}
		
		System.out.println(resolve(Class.class.getTypeParameters( )[0], Class.class, null, new Class<String>("")).getRawType( ));
		System.out.println(resolve(Class.class.getTypeParameters( )[0], Object.class, Class.class.getDeclaredFields( )[0].getGenericType( ), "").getRawType( ));
		System.out.println(getTypeOf(new Class<String>("")).getTypeArgument(Class.class.getTypeParameters( )[0]).getRawType( ));
	}
	
	private static TypeLiteral<?> resolve(final TypeVariable<?> TYPE_VARIABLE, final Class<?> CLASS, final Type TYPE, final Object ARGUMENT)
	{
		if (CLASS.isInstance(ARGUMENT))
		{
			if (TYPE_VARIABLE.equals(TYPE))
			{
				LOGGER.debug
				(
					"Type variable {} equals type: {}", TYPE_VARIABLE, TYPE
				);
				
				return getTypeOf(ARGUMENT);
			}
			else if (TYPE instanceof ParameterizedType)
			{
				final Type[ ] TYPE_ARGUMENTS =
				(
					((ParameterizedType)TYPE).getActualTypeArguments( )
				);
				
				final TypeVariable<?>[ ] TYPE_PARAMETERS =
				(
					CLASS.getTypeParameters( )
				);
				
				for (int index = 0; index < TYPE_ARGUMENTS.length; index++)
				{
					if (TYPE_VARIABLE.equals(TYPE_ARGUMENTS[index]))
					{
						return
						(
							resolve
							(
								TYPE_PARAMETERS[index],
								CLASS,
								TYPE_ARGUMENTS[index],
								ARGUMENT
							)
						);
					}
				}
			}
			else
			{
				for (final Field FIELD : getInstanceFields(CLASS))
				{
					try
					{
						final Object VALUE = FIELD.get(ARGUMENT);
						
						if (VALUE == null)
						{
							continue;
						}
						else
						{
							return
							(
								resolve
								(
									TYPE_VARIABLE,
									FIELD.getType( ),
									FIELD.getGenericType( ),
									VALUE
								)
							);
						}
					}
					catch (final IllegalAccessException e)
					{
						continue;
					}
					catch (final RuntimeException e)
					{
						continue;
					}
				}
			}
		}
		
		LOGGER.debug
		(
			"Resolving failed.\nType variable: {}\nClass: {}\nType: {}\nArgument: {}",
			new Object[ ] {TYPE_VARIABLE, CLASS, TYPE, ARGUMENT}
		);
		
		throw new RuntimeException("Problem?");
	}
	
	private static TypeLiteral<?> resolve(final TypeVariable<?> TYPE_VARIABLE, final TypeLiteral<?> PARENT, final Object... ARGUMENTS)
	{
		LOGGER.debug
		(
			"Attempting to resolve.\nType variable: {}\nPARENT: {}\nArguments: {}",
			new Object[ ] {TYPE_VARIABLE, PARENT, ARGUMENTS}
		);
		
		if (PARENT != null)
		{
			final TypeLiteral<?> RESOLVED_TYPE =
			(
				PARENT.getTypeArgument(TYPE_VARIABLE)
			);
			
			if (RESOLVED_TYPE != null)
			{
				return RESOLVED_TYPE;
			}
		}
		
		final GenericDeclaration DECLARATION =
		(
			TYPE_VARIABLE.getGenericDeclaration( )
		);

		if (DECLARATION instanceof Method)
		{
			final Method METHOD = (Method)DECLARATION;
			
			final Class<?>[ ] PARAMETERS_TYPES = METHOD.getParameterTypes( );
			
			final Type[ ] GENERIC_PARAMETERS_TYPES =
			(
				METHOD.getGenericParameterTypes( )
			);
			
			if (PARAMETERS_TYPES.length != ARGUMENTS.length)
			{
				throw new RuntimeException("Problem?");
			}
			else if (GENERIC_PARAMETERS_TYPES.length != ARGUMENTS.length)
			{
				throw new RuntimeException("Problem?");
			}
			else
			{
				for (int index = 0; index < PARAMETERS_TYPES.length; index++)
				{
					try
					{
						return
						(
							resolve
							(
								TYPE_VARIABLE,
								PARAMETERS_TYPES[index],
								GENERIC_PARAMETERS_TYPES[index],
								ARGUMENTS[index]
							)
						);
					}
					catch (final RuntimeException e)
					{
						continue;
					}
				}
			}
		}
		
		throw new RuntimeException("Problem?");
	}
	
	public Class<E> getRawType( )
	{
		return RAW_TYPE;
	}
	
	public TypeLiteral<? super E> getSuperType( )
	{
		return null;//getTypeOf(RAW_TYPE.getSuperclass( ));
	}
	
	public TypeLiteral<?> getOwnerType( )
	{
		return null;
	}
	
	public Map<TypeVariable<?>, TypeLiteral<?>> getAllTypeArguments( )
	{
		return
		(
			new LinkedHashMap<TypeVariable<?>, TypeLiteral<?>>(TYPE_ARGUMENTS)
		);
	}
	
	public TypeLiteral<?> getTypeArgument(final TypeVariable<?> TYPE_VARIABLE)
	{
		return TYPE_ARGUMENTS.get(TYPE_VARIABLE);
	}
	
	public TypeLiteral<?> getReturnType(final Method METHOD, final Object... ARGUMENTS)
	{
		/*
		 * 
		 */
		
		return null;
	}
	
	public boolean isAssignableFrom(final TypeLiteral<?> TYPE)
	{
		// check type arguments of generic superclass and implemented interfaces
		
		if (getRawType( ).isAssignableFrom(TYPE.getRawType( )))
		{
			for (final Map.Entry<TypeVariable<?>, TypeLiteral<?>> ENTRY : getAllTypeArguments( ).entrySet( ))
			{
				if (ENTRY.getValue( ).isAssignableFrom(TYPE.getTypeArgument(ENTRY.getKey( ))))
				{
					break; // continue;
				}
				else
				{
					// return false;
				}
			}
		}
		else
		{
			// return false;
		}
		
		throw new UnsupportedOperationException( ); // return true;
	}
	
	public boolean isInstance(final Object INSTANCE)
	{
		return isAssignableFrom(getTypeOf(INSTANCE));
	}
	
	public boolean isImmutable( )
	{
		throw new UnsupportedOperationException( );
	}
	
	@Override
	public int hashCode( )
	{
		return super.hashCode( );
	}
	
	@Override
	public boolean equals(final Object THAT)
	{
		if (THAT instanceof TypeLiteral)
		{
			final TypeLiteral<?> TYPE_LITERAL = (TypeLiteral<?>)THAT;
			
			return
			(
				getRawType( ).equals(TYPE_LITERAL.getRawType( )) &&
				// getOwnerType( ).equals(TYPE_LITERAL.getOwnerType( )) &&
				getAllTypeArguments( ).equals(TYPE_LITERAL.getAllTypeArguments( ))
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
		return super.toString( );
	}
	
	static void resolve2(final TypeVariable<?> TYPE_VARIABLE, final Type TYPE, final Object INSTANCE)
	{
		;
	}
	
	static void searchArguments(final TypeVariable<?> TYPE_VARIABLE, final Object... ARGUMENTS)
	{
		final GenericDeclaration GENERIC_DECLARATION =
		(
			TYPE_VARIABLE.getGenericDeclaration( )
		);
		
		if (GENERIC_DECLARATION instanceof Method)
		{
			final Type[ ] GENERIC_PARAMETER_TYPES =
			(
				((Method)GENERIC_DECLARATION).getGenericParameterTypes( )
			);
			
			if (GENERIC_PARAMETER_TYPES.length == ARGUMENTS.length)
			{
				for (int index = 0; index < ARGUMENTS.length; index++)
				{
					resolve2
					(
						TYPE_VARIABLE,
						GENERIC_PARAMETER_TYPES[index],
						ARGUMENTS[index]
					);
				}
			}
			else
			{
				throw new RuntimeException("Wrong number of arguments.");
			}
		}
		else
		{
			throw
			(
				new RuntimeException
				(
					"Type variable wasn't declared by a method."
				)
			);
		}
	}
	
	static void searchFields(final TypeVariable<?> TYPE_VARIABLE, final Class<?> CLASS, final Object INSTANCE)
	{
		if (CLASS.equals(TYPE_VARIABLE.getGenericDeclaration( )))
		{
			for (final Field FIELD : getInstanceFields(CLASS))
			{
				try
				{
					resolve2
					(
						TYPE_VARIABLE,
						FIELD.getGenericType( ),
						FIELD.get(INSTANCE)
					);
				}
				catch (IllegalArgumentException e)
				{
					throw
					(
						new RuntimeException
						(
							"INSTANCE isn't an instance of CLASS."
						)
					);
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			throw new RuntimeException("TYPE_VARIABLE not declared by CLASS.");
		}
	}
}