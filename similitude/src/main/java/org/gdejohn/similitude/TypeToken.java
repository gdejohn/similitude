package org.gdejohn.similitude;

import static java.lang.Class.forName;
import static java.lang.Integer.valueOf;
import static java.lang.Math.nextUp;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.deepHashCode;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeToken<T>
{
	private static final Logger LOGGER = getLogger(TypeToken.class);
	
	private static final Map<TypeVariable<?>, TypeToken<?>> NO_TYPE_ARGUMENTS =
	(
		emptyMap( )
	);
	
	private static final Map<Type, Object> NO_PARAMETERIZATIONS = emptyMap( );
	
	private final Class<T> RAW_TYPE;
	
	private final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS;
	
	private final TypeToken<?> ENCLOSING_TYPE;
	
	private Map<TypeVariable<?>, TypeToken<?>> allTypeArguments = null;
	
	private Set<Field> instanceFields = null;
	
	private Set<Constructor<T>> constructors = null;
	
	private Integer hashCode = null;
	
	private String toString = null;
	
	@SuppressWarnings("unchecked")
	protected TypeToken( )
	{
		final Type TYPE = this.getClass( ).getGenericSuperclass( );
		
		if (TYPE instanceof ParameterizedType)
		{
			final ParameterizedType PARAMETERIZED_TYPE =
			(
				(ParameterizedType)TYPE
			);
			
			if (TypeToken.class.equals(PARAMETERIZED_TYPE.getRawType( )))
			{
				final TypeToken<?> TYPE_TOKEN =
				(
					typeOf(PARAMETERIZED_TYPE.getActualTypeArguments( )[0])
				);
				
				this.RAW_TYPE = (Class<T>)TYPE_TOKEN.getRawType( );
				this.TYPE_ARGUMENTS = TYPE_TOKEN.getAllTypeArguments( );
				this.ENCLOSING_TYPE = TYPE_TOKEN.getEnclosingType( );
				
				return;
			}
		}
		
		throw new RuntimeException( );
	}
	
	private TypeToken(final Class<T> RAW_TYPE, final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS, final TypeToken<?> ENCLOSING_TYPE)
	{
		this.RAW_TYPE = RAW_TYPE;
		this.TYPE_ARGUMENTS = TYPE_ARGUMENTS;
		this.ENCLOSING_TYPE = ENCLOSING_TYPE;
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS, final TypeToken<?> ENCLOSING_TYPE)
	{
		LOGGER.debug
		(
			"Getting type of class \"{}\" with type arguments \"{}\"",
			CLASS.getSimpleName( ),
			TYPE_ARGUMENTS
		);
		
		return new TypeToken<T>(CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE);
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS, final TypeToken<?> ENCLOSING_TYPE)
	{
		final TypeVariable<Class<T>>[ ] TYPE_PARAMETERS =
		(
			CLASS.getTypeParameters( )
		);
		
		if (TYPE_PARAMETERS.length == 0)
		{
			return typeOf(CLASS, NO_TYPE_ARGUMENTS, ENCLOSING_TYPE);
		}
		else
		{
			final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeToken<?>>
				(
					TYPE_PARAMETERS.length, nextUp(1.0f)
				)
			);
			
			for (final TypeVariable<?> TYPE_VARIABLE : TYPE_PARAMETERS)
			{
				TYPE_ARGUMENTS.put
				(
					TYPE_VARIABLE,
					typeOf(TYPE_VARIABLE, PARENT, PARAMETERIZATIONS)
				);
			}
			
			return typeOf(CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE);
		}
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		final TypeToken<?> ENCLOSING_TYPE;
		
		if (CLASS.isMemberClass( ) && isStatic(CLASS.getModifiers( )) == false)
		{
			ENCLOSING_TYPE =
			(
				typeOf(CLASS.getEnclosingClass( ), PARENT, PARAMETERIZATIONS)
			);
		}
		else
		{
			ENCLOSING_TYPE = null;
		}
		
		return typeOf(CLASS, PARENT, PARAMETERIZATIONS, ENCLOSING_TYPE);
	}
	
	static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT)
	{
		if (CLASS == null)
		{
			return null;
		}
		else
		{
			return typeOf(CLASS, PARENT, NO_PARAMETERIZATIONS);
		}
	}
	
	public static <T> TypeToken<T> typeOf(final Class<T> CLASS)
	{
		return typeOf(CLASS, (TypeToken<?>)null);
	}
	
	public static <T> TypeToken<? extends T> typeOf(final T OBJECT)
	{
		LOGGER.debug("Getting type of object: {}", OBJECT);
		
		if (OBJECT == null)
		{
			return null;
		}
		
		@SuppressWarnings("unchecked")
		final Class<? extends T> RAW_TYPE =
		(
			(Class<? extends T>)OBJECT.getClass( )
		);
		
		final Set<Field> INSTANCE_FIELDS = getAllInstanceFields(RAW_TYPE);
		
		final Map<Type, Object> PARAMETERIZATIONS =
		(
			new LinkedHashMap<Type, Object>
			(
				INSTANCE_FIELDS.size( ), nextUp(1.0f)
			)
		);
		
		final Iterator<Field> ITERATOR = INSTANCE_FIELDS.iterator( );
		
		while (ITERATOR.hasNext( ))
		{
			final Field FIELD = ITERATOR.next( );
			
			try
			{
				if (isPublic(FIELD.getModifiers( )) == false)
				{
					FIELD.setAccessible(true);
				}
				
				final Object VALUE = FIELD.get(OBJECT);
				
				if (VALUE != null)
				{
					PARAMETERIZATIONS.put(FIELD.getGenericType( ), VALUE);
				}
			}
			catch (final SecurityException e)
			{
				ITERATOR.remove( );
				
				continue;
			}
			catch (final IllegalAccessException e)
			{ // SecurityException should always be thrown before this.
				throw new RuntimeException(e);
			}
		}
		
		if (RAW_TYPE.isMemberClass( ))
		{
			if (isStatic(RAW_TYPE.getModifiers( )) == false)
			{
				final Class<?> ENCLOSING_CLASS = RAW_TYPE.getEnclosingClass( );
				
				for (final Field FIELD : INSTANCE_FIELDS)
				{
					if (FIELD.isSynthetic( ))
					{
						if (ENCLOSING_CLASS.equals(FIELD.getType( )))
						{
							if (FIELD.getName( ).matches("\\Athis\\$\\d++\\z"))
							{
								try
								{
									final Object VALUE = FIELD.get(OBJECT);
									
									if (VALUE != null)
									{
										return
										(
											typeOf
											(
												RAW_TYPE,
												null, // PARENT
												PARAMETERIZATIONS,
												typeOf(VALUE) // ENCLOSING_TYPE
											)
										);
									}
									else
									{
										return
										(
											typeOf
											(
												RAW_TYPE,
												null,
												PARAMETERIZATIONS,
												typeOf
												(
													ENCLOSING_CLASS,
													null,
													PARAMETERIZATIONS
												)
											)
										);
									}
								}
								catch (final IllegalAccessException e)
								{ // Inaccessible fields shouldn't be present.
									throw new RuntimeException(e);
								}
							}
						}
					}
				}
				
				throw new RuntimeException("Enclosing instance not found.");
			}
		}
		
		return typeOf(RAW_TYPE, null, PARAMETERIZATIONS, null);
	}
	
	private static TypeToken<?> typeOf(final WildcardType WILDCARD_TYPE, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		final Type[ ] UPPER_BOUNDS = WILDCARD_TYPE.getUpperBounds( );
		
		if (UPPER_BOUNDS.length == 1)
		{
			return typeOf(UPPER_BOUNDS[0], PARENT, PARAMETERIZATIONS);
		}
		else
		{
			throw new RuntimeException("Multiply bounded wildcard type.");
		}
	}
	
	private static TypeToken<?> typeOf(final GenericArrayType GENERIC_ARRAY_TYPE, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		Type type = GENERIC_ARRAY_TYPE;
		
		final StringBuilder CLASS_NAME = new StringBuilder( );
		
		do
		{
			type = ((GenericArrayType)type).getGenericComponentType( );
			
			CLASS_NAME.append('[');
		}
		while (type instanceof GenericArrayType);
		
		final Class<?> COMPONENT_TYPE =
		(
			typeOf(type, PARENT, PARAMETERIZATIONS).getRawType( )
		);
		
		if (COMPONENT_TYPE.isPrimitive( ))
		{
			if(COMPONENT_TYPE.equals(byte.class))
			{
				CLASS_NAME.append('B');
			}
			else if(COMPONENT_TYPE.equals(short.class))
			{
				CLASS_NAME.append('S');
			}
			else if(COMPONENT_TYPE.equals(int.class))
			{
				CLASS_NAME.append('I');
			}
			else if(COMPONENT_TYPE.equals(long.class))
			{
				CLASS_NAME.append('J');
			}
			else if(COMPONENT_TYPE.equals(float.class))
			{
				CLASS_NAME.append('F');
			}
			else if(COMPONENT_TYPE.equals(double.class))
			{
				CLASS_NAME.append('D');
			}
			else if(COMPONENT_TYPE.equals(char.class))
			{
				CLASS_NAME.append('C');
			}
			else if(COMPONENT_TYPE.equals(boolean.class))
			{
				CLASS_NAME.append('Z');
			}
			else
			{
				throw new RuntimeException( );
			}
		}
		else
		{ // array of reference type
			CLASS_NAME.append('L');
			CLASS_NAME.append(COMPONENT_TYPE.getName( ));
			CLASS_NAME.append(';');
		}
		
		try
		{
			return typeOf(forName(CLASS_NAME.toString( )));
		}
		catch (final ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static TypeToken<?> typeOf(final ParameterizedType PARAMETERIZED_TYPE, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		final Type RAW_TYPE = PARAMETERIZED_TYPE.getRawType( );
		
		if (RAW_TYPE instanceof Class)
		{
			final Class<?> CLASS = (Class<?>)RAW_TYPE;
			
			final TypeVariable<?>[ ] TYPE_PARAMETERS =
			(
				CLASS.getTypeParameters( )
			);
			
			final Type[ ] ACTUAL_TYPE_ARGUMENTS =
			(
				PARAMETERIZED_TYPE.getActualTypeArguments( )
			);
			
			final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeToken<?>>( )
			);
			
			if (TYPE_PARAMETERS.length == ACTUAL_TYPE_ARGUMENTS.length)
			{
				for (int index = 0; index < TYPE_PARAMETERS.length; index++)
				{
					TYPE_ARGUMENTS.put
					(
						TYPE_PARAMETERS[index],
						typeOf
						(
							ACTUAL_TYPE_ARGUMENTS[index],
							PARENT,
							PARAMETERIZATIONS
						)
					);
				}
				
				final TypeToken<?> ENCLOSING_TYPE =
				(
					typeOf
					(
						PARAMETERIZED_TYPE.getOwnerType( ),
						PARENT,
						PARAMETERIZATIONS
					)
				);
				
				return typeOf(CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE);
			}
			else
			{
				throw
				(
					new RuntimeException
					(
						"Different number of type variables and arguments."
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
					"Raw type not instance of java.lang.Class"
				)
			);
		}
	}
	
	private static LinkedList<TypeVariable<?>> traceTypeVariable(final TypeVariable<?> TYPE_VARIABLE, final Type TYPE)
	{
		if (TYPE instanceof TypeVariable)
		{
			if (TYPE_VARIABLE.equals(TYPE))
			{
				return new LinkedList<TypeVariable<?>>( );
			}
			else
			{ // Check upper bounds.
				throw new UnsupportedOperationException( );
			}
		}
		else if (TYPE instanceof WildcardType)
		{ // Check upper bound.
			final WildcardType WILDCARD_TYPE =
			(
				(WildcardType)TYPE
			);
			
			final Type[ ] UPPER_BOUNDS = WILDCARD_TYPE.getUpperBounds( );
			
			if (UPPER_BOUNDS.length == 1)
			{
				return traceTypeVariable(TYPE_VARIABLE, UPPER_BOUNDS[0]);
			}
			else
			{
				throw new RuntimeException("Multiply bounded wildcard type.");
			}
		}
		else if (TYPE instanceof GenericArrayType)
		{ // Check component type.
			throw new UnsupportedOperationException( );
		}
		else if (TYPE instanceof ParameterizedType)
		{
			final ParameterizedType PARAMETERIZED_TYPE =
			(
				(ParameterizedType)TYPE
			);
			
			final Type RAW_TYPE = PARAMETERIZED_TYPE.getRawType( );
			
			final TypeVariable<?>[ ] TYPE_PARAMETERS;
			
			if (RAW_TYPE instanceof Class)
			{
				TYPE_PARAMETERS = ((Class<?>)RAW_TYPE).getTypeParameters( );
			}
			else
			{
				throw new RuntimeException( );
			}
			
			final Type[ ] TYPE_ARGUMENTS =
			(
				PARAMETERIZED_TYPE.getActualTypeArguments( )
			);
			
			for (int index = 0; index < TYPE_PARAMETERS.length; index++)
			{
				try
				{
					final LinkedList<TypeVariable<?>> TRACE =
					(
						traceTypeVariable
						(
							TYPE_VARIABLE, TYPE_ARGUMENTS[index]
						)
					);
					
					TRACE.addFirst(TYPE_PARAMETERS[index]);
					
					return TRACE;
				}
				catch (final RuntimeException e)
				{
					continue;
				}
			}
			
			throw new RuntimeException("Type variable not found.");
		}
		else if (TYPE instanceof Class)
		{
			throw new RuntimeException("Type variable not found.");
		}
		else
		{
			throw new RuntimeException("Subtype of type not recognized.");
		}
	}
	
	private static TypeToken<?> typeOf(final TypeVariable<?> TYPE_VARIABLE, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		if (PARENT != null)
		{
			try
			{
				return PARENT.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final Exception e)
			{
				LOGGER.debug("Type argument not found in parent.");
			}
		}
		
		for (final Entry<Type, Object> ENTRY : PARAMETERIZATIONS.entrySet( ))
		{
			try
			{
				final List<TypeVariable<?>> TRACE =
				(
					traceTypeVariable(TYPE_VARIABLE, ENTRY.getKey( ))
				);
				
				TypeToken<?> typeArgument = typeOf(ENTRY.getValue( ));
				
				for (final TypeVariable<?> TYPE_PARAMETER : TRACE)
				{
					typeArgument =
					(
						typeArgument.getTypeArgument(TYPE_PARAMETER)
					);
				}
				
				return typeArgument;
			}
			catch (final RuntimeException e)
			{
				continue;
			}
		}
		
		throw new RuntimeException("Type argument couldn't be determined.");
	}
	
	private static TypeToken<?> typeOf(final Type TYPE, final TypeToken<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		if (TYPE == null)
		{
			return null;
		}
		else if (TYPE instanceof Class)
		{
			return typeOf((Class<?>)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else if (TYPE instanceof WildcardType)
		{
			return typeOf((WildcardType)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else if (TYPE instanceof GenericArrayType)
		{
			return typeOf((GenericArrayType)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else if (TYPE instanceof ParameterizedType)
		{
			return typeOf((ParameterizedType)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else if (TYPE instanceof TypeVariable)
		{
			return typeOf((TypeVariable<?>)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else
		{
			LOGGER.warn
			(
				"Subtype of java.lang.reflect.Type not recognized: {}", TYPE
			);
			
			throw new RuntimeException("Subtype of type not recognized.");
		}
	}
	
	static TypeToken<?> typeOf(final Type TYPE, final TypeToken<?> PARENT)
	{
		return typeOf(TYPE, PARENT, NO_PARAMETERIZATIONS);
	}
	
	public static TypeToken<?> typeOf(final Type TYPE)
	{
		return typeOf(TYPE, (TypeToken<?>)null);
	}
	
	public final Class<T> getRawType( )
	{
		return RAW_TYPE;
	}
	
	public final TypeToken<?> getEnclosingType( )
	{
		return ENCLOSING_TYPE;
	}
	
	public final Map<TypeVariable<?>, TypeToken<?>> getAllTypeArguments( )
	{
		if (allTypeArguments == null)
		{
			allTypeArguments = unmodifiableMap(TYPE_ARGUMENTS);
			
			if (allTypeArguments == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return allTypeArguments;
	}
	
	public final TypeToken<?> getTypeArgument(final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE_ARGUMENTS.containsKey(TYPE_VARIABLE))
		{
			return TYPE_ARGUMENTS.get(TYPE_VARIABLE);
		}
		
		LOGGER.debug("Type variable not found in type arguments.");
		
		if (ENCLOSING_TYPE != null)
		{
			try
			{
				return ENCLOSING_TYPE.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final RuntimeException e)
			{
				
			}
		}
		
		LOGGER.debug("Type variable not found in enclosing type.");
		
		throw new RuntimeException("Type variable not found.");
	}
	
	public final TypeToken<?> getReturnType(final Method METHOD, final Object... ARGUMENTS)
	{
		final Class<?>[ ] PARAMETERS =
		(
			METHOD.getParameterTypes( )
		);
		
		if (PARAMETERS.length == ARGUMENTS.length)
		{
			final Type[ ] GENERIC_PARAMETERS =
			(
				METHOD.getGenericParameterTypes( )
			);
			
			if (GENERIC_PARAMETERS.length == ARGUMENTS.length)
			{
				final Map<Type, Object> PARAMETERIZATIONS =
				(
					new LinkedHashMap<Type, Object>
					(
						ARGUMENTS.length, nextUp(1.0f)
					)
				);
				
				for (int index = 0; index < ARGUMENTS.length; index++)
				{
					if (PARAMETERS[index].isInstance(ARGUMENTS[index]))
					{
						PARAMETERIZATIONS.put
						(
							GENERIC_PARAMETERS[index], ARGUMENTS[index]
						);
					}
					else if (ARGUMENTS[index] != null)
					{
						throw new RuntimeException( );
					}
				}
				
				return
				(
					typeOf
					(
						METHOD.getGenericReturnType( ), this, PARAMETERIZATIONS
					)
				);
			}
			else
			{
				throw new RuntimeException( );
			}
		}
		else
		{
			throw new RuntimeException( );
		}
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
		
		return INSTANCE_FIELDS;
	}
	
	public final Set<Field> getAllInstanceFields( )
	{
		if (instanceFields == null)
		{ // First time this method has been invoked on this instance.
			instanceFields = unmodifiableSet(getAllInstanceFields(RAW_TYPE));
			
			if (instanceFields == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return instanceFields;
	}
	
	public final Set<Constructor<T>> getAccessibleConstructors( )
	{
		if (constructors == null)
		{ // First time this method has been invoked on this instance.
			final Set<Constructor<T>> ACCESSIBLE_CONSTRUCTORS =
			(
				new LinkedHashSet<Constructor<T>>( )
			);
			
			try
			{
				try
				{
					final Constructor<?>[ ] DECLARED =
					(
						RAW_TYPE.getDeclaredConstructors( )
					);
					
					for (final Constructor<?> CONSTRUCTOR : DECLARED)
					{
						try
						{
							/*
							 * Passing the array of Class instances
							 * representing the parameter types uniquely
							 * identifying the RAW constructor to
							 * Class.getDeclaredConstructor() returns the same
							 * constructor, but with the required type
							 * information.
							 */
							final Constructor<T> PARAMETERIZED =
							(
								RAW_TYPE.getDeclaredConstructor
								(
									CONSTRUCTOR.getParameterTypes( )
								)
							);
							
							/*
							 * Constructors that wouldn't normally be
							 * accessible (e.g. private) need to be made
							 * accessible before they can be invoked.
							 */
							PARAMETERIZED.setAccessible(true);
							
							ACCESSIBLE_CONSTRUCTORS.add(PARAMETERIZED);
						}
						catch (final SecurityException e)
						{
							LOGGER.debug
							(
								"Non-public constructors not available: {}",
								RAW_TYPE.getSimpleName( )
							);
							
							continue;
						}
					}
					
					constructors = unmodifiableSet(ACCESSIBLE_CONSTRUCTORS);
				}
				catch (final SecurityException e)
				{
					LOGGER.debug
					(
						"Non-public constructors not available for type: {}",
						RAW_TYPE.getSimpleName( )
					);
					
					final Constructor<?>[ ] PUBLIC =
					(
						RAW_TYPE.getConstructors( )
					);
					
					for (final Constructor<?> CONSTRUCTOR : PUBLIC)
					{
						ACCESSIBLE_CONSTRUCTORS.add
						(
							RAW_TYPE.getConstructor
							(
								CONSTRUCTOR.getParameterTypes( )
							)
						);
					}
					
					constructors = unmodifiableSet(ACCESSIBLE_CONSTRUCTORS);
				}
			}
			catch (final SecurityException e)
			{
				LOGGER.debug
				(
					"Public constructors not available for: {}",
					RAW_TYPE.getSimpleName( )
				);
				
				constructors = emptySet( );
			}
			catch (final NoSuchMethodException e)
			{ // This should never happen.
				throw new RuntimeException(e);
			}
			
			if (constructors == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return constructors;
	}
	
	@Override
	public final int hashCode( )
	{
		if (hashCode == null)
		{ // First time this method has been invoked on this instance.
			hashCode =
			(
				valueOf
				(
					deepHashCode
					(
						new Object[ ]
						{
							RAW_TYPE,
							ENCLOSING_TYPE,
							TYPE_ARGUMENTS.values( ).toArray( )
						}
					)
				)
			);
			
			if (hashCode == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return hashCode.intValue( );
	}
	
	@Override
	public final boolean equals(final Object THAT)
	{
		if (THAT instanceof TypeToken)
		{
			final TypeToken<?> TYPE = (TypeToken<?>)THAT;
			
			if (RAW_TYPE.equals(TYPE.getRawType( )))
			{
				if (TYPE_ARGUMENTS.equals(TYPE.getAllTypeArguments( )))
				{
					if (ENCLOSING_TYPE == null)
					{
						if (TYPE.getEnclosingType( ) == null)
						{
							return true;
						}
					}
					else if (ENCLOSING_TYPE.equals(TYPE.getEnclosingType( )))
					{
						return true;
					}
				}
			}
		}

		return false;
	}
	
	@Override
	public final String toString( )
	{
		if (toString == null)
		{ // First time this method has been invoked on this instance.
			final StringBuilder STRING_BUILDER = new StringBuilder( );
			
			if (ENCLOSING_TYPE != null)
			{
				STRING_BUILDER.append(ENCLOSING_TYPE.toString( )).append('.');
			}
			
			STRING_BUILDER.append(RAW_TYPE.getSimpleName( ));
			
			if (TYPE_ARGUMENTS.isEmpty( ) == false)
			{ // Parameterized type.
				STRING_BUILDER.append('<');
				
				final Iterator<TypeToken<?>> ARGUMENTS =
				(
					TYPE_ARGUMENTS.values( ).iterator( )
				);
				
				while (true)
				{
					final TypeToken<?> ARGUMENT = ARGUMENTS.next( );
					
					if (ARGUMENT == null)
					{
						STRING_BUILDER.append('?');
					}
					else
					{
						STRING_BUILDER.append(ARGUMENT);
					}
					
					if (ARGUMENTS.hasNext( ))
					{ // Only append separator if there are more parameters.
						STRING_BUILDER.append(",");
					}
					else
					{
						break;
					}
				}
				
				STRING_BUILDER.append('>');
			}
			
			toString = STRING_BUILDER.toString( );
			
			if (toString == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return toString;
	}
}