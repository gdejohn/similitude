package org.gdejohn.similitude;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeToken<T> implements Type
{
	static final Logger LOGGER = getLogger(TypeToken.class);
	
	private final TypeToken<?> PARENT;
	
	private final Class<?> RAW_TYPE;
	
	private final TypeToken<?> OWNER_TYPE;
	
	private final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS;
	
	/**
	 * Known immutable types.
	 */
	final Set<Class<?>> IMMUTABLE;
	
	private final String TO_STRING;
	
	private final boolean IS_GENERIC_ARRAY;
	
	private final int DIMENSIONS;
	
	private final boolean DETERMINE_IMMUTABLE;

	public TypeToken(final Type TYPE, final TypeToken<?> PARENT, final Set<Class<?>> IMMUTABLE, final boolean DETERMINE_IMMUTABLE, final Object... ARGUMENTS)
	{
		this.PARENT = PARENT;
		
		this.IMMUTABLE = IMMUTABLE;
		
		this.DETERMINE_IMMUTABLE = DETERMINE_IMMUTABLE;
		
		if (TYPE instanceof GenericArrayType)
		{
			IS_GENERIC_ARRAY = true;
			
			Type current = TYPE;
			
			int dimensions = 0;
			
			final StringBuilder TEMP = new StringBuilder( );
			
			do
			{
				current =
				(
					((GenericArrayType)current).getGenericComponentType( )
				);
				
				dimensions++;
				
				TEMP.append("[]");
			}
			while (current instanceof GenericArrayType);
			
			DIMENSIONS = dimensions;
			
			final TypeToken<Object> COMPONENT_TYPE =
			(
				new TypeToken<Object>
				(
					current, PARENT, IMMUTABLE, DETERMINE_IMMUTABLE, ARGUMENTS
				)
			);
			
			RAW_TYPE = COMPONENT_TYPE.RAW_TYPE;
			
			TYPE_ARGUMENTS = COMPONENT_TYPE.TYPE_ARGUMENTS;
			
			OWNER_TYPE = COMPONENT_TYPE.OWNER_TYPE;
			
			TO_STRING = TEMP.insert(0, COMPONENT_TYPE).toString( );
		}
		else
		{
			IS_GENERIC_ARRAY = false;
			
			DIMENSIONS = 0;
			
			if (TYPE instanceof Class)
			{
				RAW_TYPE = (Class<?>)TYPE;
				
				TYPE_ARGUMENTS = Collections.emptyMap( );
				
				OWNER_TYPE = null;
				
				TO_STRING = RAW_TYPE.getSimpleName( );
			}
			else if (TYPE instanceof WildcardType)
			{
				final TypeToken<?> UPPER_BOUND =
				(
					new TypeToken<Object>
					(
						((WildcardType)TYPE).getUpperBounds( )[0],
						PARENT,
						IMMUTABLE,
						DETERMINE_IMMUTABLE,
						ARGUMENTS
					)
				);
				
				RAW_TYPE = UPPER_BOUND.RAW_TYPE;
				
				TYPE_ARGUMENTS = UPPER_BOUND.TYPE_ARGUMENTS;
				
				OWNER_TYPE = UPPER_BOUND.OWNER_TYPE;
				
				TO_STRING = UPPER_BOUND.TO_STRING;
			}
			else if (TYPE instanceof TypeVariable)
			{
				final TypeToken<?> RESOLVED_TYPE =
				(
					resolve((TypeVariable<?>)TYPE, ARGUMENTS)
				);
				
				RAW_TYPE = RESOLVED_TYPE.RAW_TYPE;
				
				TYPE_ARGUMENTS = RESOLVED_TYPE.TYPE_ARGUMENTS;
				
				OWNER_TYPE = RESOLVED_TYPE.OWNER_TYPE;
				
				TO_STRING = RESOLVED_TYPE.TO_STRING;
			}
			else if (TYPE instanceof ParameterizedType)
			{
				final StringBuilder TEMP =
				(
					new StringBuilder( )
				);
				
				final Type OWNER =
				(
					((ParameterizedType)TYPE).getOwnerType( )
				);
				
				if (OWNER instanceof ParameterizedType)
				{
					OWNER_TYPE =
					(
						new TypeToken<Object>
						(
							OWNER,
							PARENT,
							IMMUTABLE,
							DETERMINE_IMMUTABLE,
							ARGUMENTS
						)
					);
					
					TEMP.append(OWNER_TYPE).append('.');
				}
				else
				{
					OWNER_TYPE = null;
				}
				
				RAW_TYPE = (Class<?>)((ParameterizedType)TYPE).getRawType( );
				
				TEMP.append(RAW_TYPE.getSimpleName( ));
				
				TYPE_ARGUMENTS =
				(
					new LinkedHashMap<TypeVariable<?>, TypeToken<?>>( )
				);
				
				final TypeVariable<?>[ ] PARAMETERS =
				(
					RAW_TYPE.getTypeParameters( )
				);
				
				final Type[ ] TYPE_ARGUMENTS =
				(
					((ParameterizedType)TYPE).getActualTypeArguments( )
				);
				
				TEMP.append('<');
				
				int index = 0;
				
				while (true)
				{
					LOGGER.debug
					(
						"Argument for type parameter {}: {}",
						PARAMETERS[index],
						TYPE_ARGUMENTS[index]
					);
					
					final TypeToken<?> ARGUMENT =
					(
						new TypeToken<Object>
						(
							TYPE_ARGUMENTS[index],
							this,
							IMMUTABLE,
							DETERMINE_IMMUTABLE,
							ARGUMENTS
						)
					);
					
					this.TYPE_ARGUMENTS.put
					(
						PARAMETERS[index],
						ARGUMENT
					); // Look through constructor args? GenericArrayType? If WildcardType, use bounds?
					
					TEMP.append(ARGUMENT);
					
					if (++index < PARAMETERS.length)
					{ // Only append separator if there are more parameters.
						TEMP.append(",");
					}
					else
					{
						break;
					}
				}
				
				TO_STRING = TEMP.append('>').toString( );
			}
			else
			{
				throw
				(
					new RuntimeException
					(
						"Subtype of java.lang.reflect.Type not recognized."
					)
				);
			}
		}
	}
	
	public TypeToken(final Type TYPE, final TypeToken<?> PARENT, final boolean DETERMINE_IMMUTABLE)
	{
		this(TYPE, PARENT, Cloner.BASIC_TYPES.keySet( ), DETERMINE_IMMUTABLE, new Object[0]);
	}
	
	public TypeToken(final Type TYPE, final boolean DETERMINE_IMMUTABLE, final Object... ARGUMENTS)
	{
		this(TYPE, null, Cloner.BASIC_TYPES.keySet( ), DETERMINE_IMMUTABLE, ARGUMENTS);
	}
	
	public TypeToken(final Type TYPE, final boolean DETERMINE_IMMUTABLE)
	{
		this(TYPE, DETERMINE_IMMUTABLE, new Object[0]);
	}
	
	public TypeToken(final Type TYPE)
	{
		this(TYPE, false);
	}
	
	/**
	 * Determines the parameterized type of a given object.
	 * 
	 * @param PARAMETERIZED_TYPE_INSTANCE An instance of a parameterized type.
	 */
	public TypeToken(final Object PARAMETERIZED_TYPE_INSTANCE)
	{
		this(PARAMETERIZED_TYPE_INSTANCE.getClass( ), false, PARAMETERIZED_TYPE_INSTANCE);
		
		throw new UnsupportedOperationException( );
	}
	
	private TypeToken<?> getArgument(final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE_ARGUMENTS == null)
		{
			return null;
		}
		else if (TYPE_ARGUMENTS.containsKey(TYPE_VARIABLE))
		{
			return TYPE_ARGUMENTS.get(TYPE_VARIABLE);
		}
		else if (PARENT != null)
		{
			return PARENT.getArgument(TYPE_VARIABLE);
		}
		else
		{
			return null;
		}
	}
	
	public final TypeToken<?> resolve(final TypeVariable<?> TYPE_VARIABLE, final Object... ARGUMENTS)
	{
		if (getArgument(TYPE_VARIABLE) != null)
		{
			return getArgument(TYPE_VARIABLE);
		}
		else
		{
			final GenericDeclaration DECLARER =
			(
				((TypeVariable<?>)TYPE_VARIABLE).getGenericDeclaration( )
			);
			
			if (DECLARER instanceof Method)
			{
				final Type[ ] PARAMETERS =
				(
					((Method)DECLARER).getGenericParameterTypes( )
				);
				
				for (int index = 0; index < PARAMETERS.length; index++)
				{ // Check arguments for parameterization of return type.
					try
					{
						LOGGER.debug
						(
							"Parameter: {}, Argument: {}",
							PARAMETERS[index],
							ARGUMENTS[index]
						); /////////////////////////////////////////////////////////////////////////////
						
						return resolve(TYPE_VARIABLE, PARAMETERS[index], ARGUMENTS[index]);
					}
					catch (InstantiationFailedException e)
					{
						LOGGER.debug
						(
							"Instantiating argument type failed.",
							e
						);
						
						continue;
					}
				}
				
				throw
				( // Only reached if the above for loop finishes normally.
					new InstantiationFailedException
					(
						"Couldn't resolve return type."
					)
				);
			}
			else
			{
				throw
				(
					new InstantiationFailedException
					(
						"Type variable %s declared by: %s",
						TYPE_VARIABLE,
						DECLARER
					)
				);
			}
		}
	}
	
	private TypeToken<?> resolve(final TypeVariable<?> TYPE_VARIABLE, final Type TYPE, final Object ARGUMENT)
	{
		if (ARGUMENT == null)
		{
			throw new RuntimeException("Argument is null");
		}
		else if (TYPE instanceof Class)
		{ // If field's declared type is Object, can't resolve type variable.
			throw new RuntimeException("Type is instance of java.lang.Class (not generic).");
		}
		else if (TYPE instanceof WildcardType)
		{
			throw new RuntimeException("Resolving wildcard type not handled.");
		}
		else if (TYPE instanceof GenericArrayType)
		{
			throw new RuntimeException("Resolving generic array type not handled.");
		}
		else if (TYPE instanceof ParameterizedType)
		{
			final Type[ ] TYPE_ARGUMENTS = ((ParameterizedType)TYPE).getActualTypeArguments( );
			
			final TypeVariable<?>[ ] TYPE_PARAMETERS = ((Class<?>)((ParameterizedType)TYPE).getRawType( )).getTypeParameters( );
			
			for (int index = 0; index < TYPE_ARGUMENTS.length; index++)
			{
				if (TYPE_ARGUMENTS[0].equals(TYPE_VARIABLE));
				{
					for (final Field FIELD : new TypeToken<Object>(ARGUMENT.getClass( )).getInstanceFields( ))
					{
						try
						{
							FIELD.setAccessible(true);
							
							return
							(
								resolve
								(
									TYPE_PARAMETERS[0],
									FIELD.getGenericType( ),
									FIELD.get(ARGUMENT)
								)
							);
						}
						catch (final SecurityException e)
						{
							continue;
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
			
			throw new RuntimeException("Type argument not found.");
		}
		else if (TYPE instanceof TypeVariable)
		{
			LOGGER.debug("Type variable: {}", TYPE);
			
			if (TYPE.equals(TYPE_VARIABLE))
			{
				return new TypeToken<Object>(ARGUMENT.getClass( ), PARENT, DETERMINE_IMMUTABLE);
			}
			else
			{
				throw
				(
					new RuntimeException
					(
						"Resolving type variable not handled."
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
					"Subtype of type not recognized: " +
					TYPE.getClass( ).getCanonicalName( )
				)
			);
		}
	}
	
	public final boolean isGenericArrayType( )
	{
		return IS_GENERIC_ARRAY;
	}
	
	public final int getDimensions( )
	{
		return DIMENSIONS;
	}
	
	@SuppressWarnings("unchecked")
	public final Class<? super T> getRawType( )
	{
		return (Class<? super T>)RAW_TYPE;
	}
	
	/**
	 * Gets all of the instance fields declared or inherited by a given type.
	 * 
	 * Static fields are ignored. If the given type is {@code null} or doesn't
	 * declare or inherit any instance fields, then the empty set is returned.
	 * 
	 * @param TYPE The type for which to get all instance fields.
	 * 
	 * @return All of the instance fields declared or inherited by {@code TYPE}.
	 * 
	 * @throws SecurityException If a security manager disallows access to the declared members of {@code TYPE} or any of its supertypes.
	 * 
	 * @see Class#getDeclaredFields()
	 */
	Set<Field> getInstanceFields( )
	{
		if (RAW_TYPE == null)
		{ // Base case, top of the hierarchy has been reached.
			return Collections.emptySet( );
		}
		else
		{
			final Set<Field> INSTANCE_FIELDS = new LinkedHashSet<Field>( );
			
			for (final Field FIELD : RAW_TYPE.getDeclaredFields( ))
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
				{ // Clone field in ORIGINAL, set result in CLONE.
					LOGGER.debug
					(
						"Found instance field: {}", FIELD
					);
					
					INSTANCE_FIELDS.add(FIELD);
				}
			}
			
			INSTANCE_FIELDS.addAll
			( // Recursively get all inherited fields.
				new TypeToken<Object>(RAW_TYPE.getSuperclass( )).getInstanceFields( )
			);
			
			return Collections.unmodifiableSet(INSTANCE_FIELDS);
		}
	}
	
	/**
	 * Recursively determines if the given type is statically immutable.
	 * 
	 * A class type is considered statically immutable if all of its declared
	 * and inherited fields are final, the declared types of those fields are
	 * themselves statically immutable, and either the type itself is declared
	 * final, or its {@link java.lang.Object#equals(Object)} and {@link
	 * java.lang.Object#hashCode()} methods are final. Otherwise, immutability
	 * can't be guaranteed, since non-final fields can be reassigned, and
	 * instances of extending classes, which can override non-final definitions
	 * of {@code equals()} and {@code hashCode()} to depend on mutable fields,
	 * can be substituted for that type.
	 * 
	 * @param TYPE The type to check for static immutability.
	 * 
	 * @return If {@code TYPE} definitely is immutable, {@code true}, else {@code false}.
	 * 
	 * @throws SecurityException If any calls to {@link #getInstanceFields(Class)} fail.
	 */
	boolean isImmutable( )
	{
		if (IMMUTABLE.contains(getRawType( )))
		{
			LOGGER.debug("Known immutable type: {}", this);
			
			return true;
		}
		else if (getRawType( ).isPrimitive( ))
		{
			LOGGER.debug("Primitive type.");
			
			return true;
		}
		else if (isGenericArrayType( ) || getRawType( ).isArray( ))
		{
			LOGGER.debug("Arrays aren't immutable.");
			
			return false;
		}
		else if (DETERMINE_IMMUTABLE)
		{ // Reflectively evaluate static immutability.
			if (isFinal(getRawType( ).getModifiers( )))
			{
				LOGGER.debug("Unknown type: {}", this);
				
				for (final Field FIELD : getInstanceFields( ))
				{
					final int MODIFIERS = FIELD.getModifiers( );
					
					if (isFinal(MODIFIERS))
					{
						if (isImmutable(new TypeToken<Object>(FIELD.getType( ))))
						{
							continue;
						}
						else
						{
							LOGGER.debug("Field type is not immutable.");
							
							return false;
						}
					}
					else if (isPrivate(MODIFIERS))
					{ // Can't guarantee immutability.
						LOGGER.debug("Private, non-final field.");
						
						return false;
					}
					else
					{ // Definitely not immutable.
						LOGGER.debug("Non-private, non-final field.");
						
						return false;
					}
				}
				
				LOGGER.debug("All field types are statically immutable.");
				
				return true;
			}
			else
			{ // TYPE isn't final. Subtypes might not be immutable.
				LOGGER.debug("Non-final type.");
				
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Recursively determines if the given object is dynamically immutable.
	 * 
	 * The given object is considered dynamically immutable if all of the
	 * instance fields declared and inherited by that object's type are final
	 * and the values of those fields for that object are also dynamically
	 * immutable. Such an object can't be changed after it's constructed.
	 * 
	 * @param TYPE The type to check for dynamic immutability.
	 * 
	 * @return If {@code TYPE} definitely is immutable, {@code true}, else {@code false}.
	 * 
	 * @throws SecurityException If any calls to {@link #getInstanceFields(Class)} fail.
	 */
	boolean isImmutable(final Object INSTANCE)
	{
		final TypeToken<?> CLASS = new TypeToken<Object>(INSTANCE.getClass( ));
		
		if (IMMUTABLE.contains(CLASS.getRawType( )))
		{
			LOGGER.debug("Known immutable type: {}", CLASS);
			
			return true;
		}
		else if (CLASS.getRawType( ).isArray( ))
		{
			LOGGER.debug("Arrays aren't immutable.");
			
			return false;
		}
		else if (DETERMINE_IMMUTABLE)
		{
			LOGGER.debug("Unknown class: {}", CLASS);
			
			for (final Field FIELD : getInstanceFields( ))
			{
				final int MODIFIERS = FIELD.getModifiers( );
				
				if (isFinal(MODIFIERS))
				{
					FIELD.setAccessible(true);
					
					final Object VALUE;
					
					try
					{
						VALUE = FIELD.get(INSTANCE);
					}
					catch (final IllegalAccessException e)
					{
						throw new RuntimeException("");
					}
					
					if (VALUE == null || isImmutable(VALUE))
					{
						continue;
					}
					else
					{
						LOGGER.debug("Field value is not immutable.");
						
						return false;
					}
				}
				else if (isPrivate(MODIFIERS))
				{ // Can't guarantee immutability.
					LOGGER.debug("Private, non-final field.");
					
					return false;
				}
				else
				{ // Definitely not immutable.
					LOGGER.debug("Non-private, non-final field.");
					
					return false;
				}
			}
			
			LOGGER.debug("All field values are dynamically immutable.");
			
			if (isImmutable(CLASS))
			{
				IMMUTABLE.add(CLASS.getRawType( ));
			}
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public final String toString( )
	{
		return TO_STRING;
	}
	
	static java.util.List<?>[ ][ ] snuts( )
	{
		return null;
	}
	
	@SuppressWarnings("unchecked")
	static <E> E doSnuts(Class<? super E> clazz)
	{
		Map<TypeVariable<?>, Type> map = new LinkedHashMap<TypeVariable<?>, Type>( );
		map.put(java.util.List.class.getTypeParameters( )[0], String.class);
		Builder builder = new Builder( );
		builder.addDefault(String.class, "xyzzy");
		return builder.instantiate((Class<E>)clazz, map);
	}
	
	public static void main(String[ ] args) throws Exception
	{
		java.util.List<String> list = TypeToken.<java.util.List<String>>doSnuts(java.util.List.class);
		System.out.println(list.get(-1));
	}
}