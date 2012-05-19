package org.gdejohn.similitude;

import static java.lang.Class.forName;
import static java.lang.Integer.valueOf;
import static java.lang.Math.nextUp;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepHashCode;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;

/**
 * A immutable representation of a Java type.
 * 
 * @param <T> The represented type.
 * 
 * @author Griffin DeJohn
 */
public class TypeToken<T>
{
	private static final Logger LOGGER = getLogger(TypeToken.class);
	
	private static final Map<TypeVariable<?>, TypeToken<?>> NO_TYPE_ARGUMENTS =
	(
		emptyMap( )
	);
	
	private static final Map<Type, List<Object>> NO_PARAMETERIZATIONS =
	(
		emptyMap( )
	);
	
	private static final Set<TypeToken<?>> NO_INTERFACES = emptySet( );
	
	private static final Set<Field> NO_INSTANCE_FIELDS = emptySet( );
	
	private final Class<T> RAW_TYPE;
	
	private final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS;
	
	private final TypeToken<?> ENCLOSING_TYPE;
	
	private final TypeToken<?> SUPER_TYPE;
	
	private final Set<TypeToken<?>> INTERFACES;
	
	private Set<Field> instanceFields = null;
	
	private Set<Constructor<T>> constructors = null;
	
	private Integer hashCode = null;
	
	private String toString = null;
	
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
				
				@SuppressWarnings("unchecked")
				final Class<T> RAW_TYPE = (Class<T>)TYPE_TOKEN.getRawType( );
				
				this.RAW_TYPE = RAW_TYPE;
				
				this.TYPE_ARGUMENTS = TYPE_TOKEN.getAllTypeArguments( );
				
				this.ENCLOSING_TYPE = TYPE_TOKEN.getEnclosingType( );
				
				this.SUPER_TYPE = TYPE_TOKEN.getSuperType( );
				
				this.INTERFACES = TYPE_TOKEN.getInterfaces( );
				
				return;
			}
		}
		
		throw new RuntimeException( );
	}
	
	private TypeToken(final Class<T> RAW_TYPE, final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS, final TypeToken<?> ENCLOSING_TYPE, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		this.RAW_TYPE = RAW_TYPE;
		
		this.TYPE_ARGUMENTS = unmodifiableMap(TYPE_ARGUMENTS);
		
		this.ENCLOSING_TYPE = ENCLOSING_TYPE;
		
		final TypeToken<?> CALLER = CALLERS.get(this);
		
		if (CALLER != null)
		{
			LOGGER.debug("Self-reference encountered for type: {}", this);
			
			throw new CircularSuperTypeException(CALLER);
		}
		else
		{
			CALLERS.put(this, this);
			
			this.SUPER_TYPE =
			(
				typeOf(RAW_TYPE.getGenericSuperclass( ), this, CALLERS)
			);
			
			final Type[ ] GENERIC_INTERFACES =
			(
				RAW_TYPE.getGenericInterfaces( )
			);
			
			if (GENERIC_INTERFACES.length == 0)
			{
				this.INTERFACES = NO_INTERFACES;
			}
			else
			{
				final Set<TypeToken<?>>	INTERFACES =
				(
					new LinkedHashSet<TypeToken<?>>
					(
						GENERIC_INTERFACES.length, nextUp(1.0f)
					)
				);
				
				for (final Type INTERFACE : GENERIC_INTERFACES)
				{
					if (INTERFACES.add(typeOf(INTERFACE, this, CALLERS)))
					{
						continue;
					}
					else
					{
						throw new RuntimeException("Interface already added.");
					}
				}
				
				this.INTERFACES = unmodifiableSet(INTERFACES);
			}
		}
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS, final TypeToken<?> ENCLOSING_TYPE, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		LOGGER.debug
		(
			"Getting type of class \"{}\" with type arguments \"{}\"",
			CLASS.getSimpleName( ),
			TYPE_ARGUMENTS
		);
		
		try
		{
			return
			(
				new TypeToken<T>
				(
					CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE, CALLERS
				)
			);
		}
		catch (final CircularSuperTypeException e)
		{
			/*
			 * This exception is only thrown if CALLER is equal to what would
			 * have otherwise been returned, ensuring that the cast is safe.
			 */
			@SuppressWarnings("unchecked")
			final TypeToken<T> CALLER = (TypeToken<T>)e.getCaller( );
			
			return CALLER;
		}
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final TypeToken<?> ENCLOSING_TYPE, final Map<TypeToken<?>, TypeToken<?>> CALLERS, final IdentityHashMap<Object, TypeToken<?>> VALUES)
	{
		final TypeVariable<Class<T>>[ ] TYPE_PARAMETERS =
		(
			CLASS.getTypeParameters( )
		);
		
		if (TYPE_PARAMETERS.length == 0)
		{
			return typeOf(CLASS, NO_TYPE_ARGUMENTS, ENCLOSING_TYPE, CALLERS);
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
					typeOf
					(
						TYPE_VARIABLE,
						PARENT,
						PARAMETERIZATIONS, 
						CALLERS,
						VALUES
					)
				);
			}
			
			return typeOf(CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE, CALLERS);
		}
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final TypeToken<?> ENCLOSING_TYPE, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		return
		(
			typeOf
			(
				CLASS, PARENT, 
				PARAMETERIZATIONS,
				ENCLOSING_TYPE,
				CALLERS,
				new IdentityHashMap<Object, TypeToken<?>>( )
			)
		);
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		final TypeToken<?> ENCLOSING_TYPE;
		
		if (CLASS.isMemberClass( ) && isStatic(CLASS.getModifiers( )) == false)
		{
			ENCLOSING_TYPE =
			(
				typeOf
				(
					CLASS.getEnclosingClass( ),
					PARENT,
					PARAMETERIZATIONS,
					CALLERS
				)
			);
		}
		else
		{
			ENCLOSING_TYPE = null;
		}
		
		return
		(
			typeOf(CLASS, PARENT, PARAMETERIZATIONS, ENCLOSING_TYPE, CALLERS)
		);
	}
	
	static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		if (CLASS == null)
		{
			return null;
		}
		else
		{
			return typeOf(CLASS, PARENT, NO_PARAMETERIZATIONS, CALLERS);
		}
	}
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final TypeToken<?> PARENT)
	{
		return
		(
			typeOf
			(
				CLASS, PARENT, new LinkedHashMap<TypeToken<?>, TypeToken<?>>( )
			)
		);
	}
	
	/**
	 * Models the type represented by a given {@code Class} instance.
	 * 
	 * @param <T> The type represented by {@code CLASS}.
	 * @param CLASS The class to model.
	 * 
	 * @return A {@code TypeToken} representing {@code T}.
	 * 
	 * @see java.lang.Class
	 */
	public static <T> TypeToken<T> typeOf(final Class<T> CLASS)
	{
		return typeOf(CLASS, (TypeToken<?>)null);
	}
	
	private static <T> TypeToken<? extends T> typeOf(final T OBJECT, final IdentityHashMap<Object, TypeToken<?>> VALUES)
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
		
		final Map<Type, List<Object>> PARAMETERIZATIONS =
		(
			new LinkedHashMap<Type, List<Object>>
			(
				INSTANCE_FIELDS.size( ), nextUp(1.0f)
			)
		);
		
		final Class<?> ENCLOSING_CLASS;
		
		if (RAW_TYPE.isMemberClass( ) && !isStatic(RAW_TYPE.getModifiers( )))
		{ // Non-static member class.
			ENCLOSING_CLASS = RAW_TYPE.getEnclosingClass( );
			
			if (ENCLOSING_CLASS == null)
			{
				throw new RuntimeException("Enclosing class is null.");
			}
		}
		else
		{
			ENCLOSING_CLASS = null;
		}
		
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
				{ // Check for recursive data types?
					final Type FIELD_TYPE = FIELD.getGenericType( );
					
					final List<Object> OBJECTS =
					(
						PARAMETERIZATIONS.get(FIELD_TYPE)
					);
					
					if(OBJECTS == null)
					{
						final LinkedList<Object> LIST =
						(
							new LinkedList<Object>( )
						);
						
						LIST.add(VALUE);
						
						PARAMETERIZATIONS.put(FIELD_TYPE, LIST);
					}
					else
					{
						OBJECTS.add(VALUE);
					}
					
					if (ENCLOSING_CLASS != null && FIELD.isSynthetic( ))
					{
						if (ENCLOSING_CLASS.equals(FIELD_TYPE))
						{
							if (FIELD.getName( ).matches("\\Athis\\$\\d++\\z"))
							{ // "this$" followed by one or more digits
								continue; // skip remove() at end of loop
							}
						}
					}
				}
			}
			catch (final SecurityException e)
			{
				LOGGER.debug("Couldn't set field accessible.", e);
			}
			catch (final IllegalAccessException e)
			{ // SecurityException should always be thrown before this.
				throw new RuntimeException(e);
			}
			
			/*
			 * If OBJECT is an instance of a non-static inner class, 
			 */
			ITERATOR.remove( ); // If exception was thrown or VALUE was null.
		}
		
		final Map<TypeToken<?>, TypeToken<?>> CALLERS =
		(
			new LinkedHashMap<TypeToken<?>, TypeToken<?>>( )
		);
		
		if (ENCLOSING_CLASS == null)
		{
			return
			(
				typeOf
				(
					RAW_TYPE, null, PARAMETERIZATIONS, null, CALLERS, VALUES
				)
			);
		}
		else if (INSTANCE_FIELDS.size( ) > 1)
		{
			LOGGER.debug
			(
				"Multiple enclosing instance fields: {}", INSTANCE_FIELDS
			);
			
			throw new RuntimeException("Multiple enclosing instance fields.");
		}
		else
		{
			try
			{
				if (INSTANCE_FIELDS.isEmpty( ))
				{
					LOGGER.debug("No enclosing instance field.");
				}
				else
				{
					final TypeToken<?> ENCLOSING_TYPE =
					(
						typeOf
						(
							INSTANCE_FIELDS.iterator( ).next( ).get(OBJECT),
							VALUES
						)
					);
					
					return
					(
						typeOf
						(
							RAW_TYPE,
							null, // PARENT
							PARAMETERIZATIONS,
							ENCLOSING_TYPE,
							CALLERS,
							VALUES
						)
					);
				}
			}
			catch (final IllegalAccessException e)
			{ // Inaccessible fields shouldn't be present.
				throw new RuntimeException(e);
			}
			catch (final RuntimeException e)
			{
				LOGGER.debug("");
			}
			
			return
			(
				typeOf
				(
					RAW_TYPE,
					null, // PARENT
					PARAMETERIZATIONS,
					typeOf(ENCLOSING_CLASS, null, PARAMETERIZATIONS, CALLERS),
					CALLERS,
					VALUES
				)
			);
		}
	}
	
	/**
	 * Models the runtime type of a given object.
	 * 
	 * @param <T> The type of {@code OBJECT}.
	 * @param OBJECT An instance of the type to model.
	 * 
	 * @return A {@code TypeToken} representing {@code T}.
	 */
	public static <T> TypeToken<? extends T> typeOf(final T OBJECT)
	{
		return typeOf(OBJECT, new IdentityHashMap<Object, TypeToken<?>>( ));
	}
	
	private static TypeToken<?> typeOf(final WildcardType WILDCARD_TYPE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		final Type[ ] UPPER_BOUNDS = WILDCARD_TYPE.getUpperBounds( );
		
		if (UPPER_BOUNDS.length == 1)
		{
			return typeOf(UPPER_BOUNDS[0], PARENT, PARAMETERIZATIONS, CALLERS);
		}
		else
		{
			throw new RuntimeException("Multiply bounded wildcard type.");
		}
	}
	
	private static TypeToken<?> typeOf(final GenericArrayType GENERIC_ARRAY_TYPE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
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
			typeOf(type, PARENT, PARAMETERIZATIONS, CALLERS).getRawType( )
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
			return typeOf(forName(CLASS_NAME.toString( )), null, CALLERS);
		}
		catch (final ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static TypeToken<?> typeOf(final ParameterizedType PARAMETERIZED_TYPE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
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
							PARAMETERIZATIONS,
							CALLERS
						)
					);
				}
				
				final TypeToken<?> ENCLOSING_TYPE =
				(
					typeOf
					(
						PARAMETERIZED_TYPE.getOwnerType( ),
						PARENT,
						PARAMETERIZATIONS,
						CALLERS
					)
				);
				
				return typeOf(CLASS, TYPE_ARGUMENTS, ENCLOSING_TYPE, CALLERS);
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
	
	private static TypeToken<?> typeOf(final TypeVariable<?> TYPE_VARIABLE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS, final IdentityHashMap<Object, TypeToken<?>> VALUES)
	{
		LOGGER.debug
		(
			"Getting type of type variable.\nType variable: {}\nDeclarer: {}\nParent: {}\nParameterizations: {}\nCallers: {}",
			new Object[ ]
			{
				TYPE_VARIABLE,
				TYPE_VARIABLE.getGenericDeclaration( ),
				PARENT,
				PARAMETERIZATIONS,
				CALLERS
			}
		);
		
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
		
		final Set<TypeToken<?>> TYPE_ARGUMENTS =
		(
			new LinkedHashSet<TypeToken<?>>( )
		);
		
		final Set<Entry<Type, List<Object>>> ENTRIES =
		(
			PARAMETERIZATIONS.entrySet( )
		);
		
		for (final Entry<Type, List<Object>> ENTRY : ENTRIES)
		{
			final Type TYPE = ENTRY.getKey( );
			
			final List<TypeVariable<?>> TRACE;
			
			if (TYPE instanceof GenericArrayType)
			{
				TRACE = null;
			}
			else
			{
				try
				{
					TRACE = traceTypeVariable(TYPE, TYPE_VARIABLE);
				}
				catch (final RuntimeException e)
				{
					continue;
				}
			}
			
			for (final Object OBJECT : ENTRY.getValue( ))
			{
				if (OBJECT == null)
				{
					continue;
				}
				else if (TRACE == null)
				{ // GenericArrayType
					if (OBJECT instanceof Object[ ] == false)
					{
						throw new RuntimeException( );
					}
					
					final Object[ ] ARRAY = (Object[ ])OBJECT;
					
					final GenericArrayType GENERIC_ARRAY_TYPE =
					(
						(GenericArrayType)TYPE
					);
					
					final Type COMPONENT_TYPE =
					(
						GENERIC_ARRAY_TYPE.getGenericComponentType( )
					);
					
					final Map<Type, List<Object>> ELEMENTS =
					(
						singletonMap(COMPONENT_TYPE, asList(ARRAY))
					);
					
					try
					{
						TYPE_ARGUMENTS.add
						(
							typeOf
							(
								TYPE_VARIABLE,
								PARENT,
								ELEMENTS,
								CALLERS,
								VALUES
							)
						);
					}
					catch (final RuntimeException e)
					{
						LOGGER.debug("Type argument not found in array.", e);
						
						continue;
					}
				}
				else
				{
					TypeToken<?> typeArgument;
					
					if (VALUES.containsKey(OBJECT))
					{
						typeArgument = VALUES.get(OBJECT);
						
						LOGGER.debug("Value already encountered: {} = {}", typeArgument, OBJECT);
					}
					else
					{
						VALUES.put(OBJECT, null);
						
						typeArgument = typeOf(OBJECT, VALUES);
						
						LOGGER.debug("Values before: {}", VALUES);
						
						VALUES.put(OBJECT, typeArgument);
						
						LOGGER.debug("Values after: {}", VALUES);
						
						LOGGER.debug("Value added: {} = {}", typeArgument, OBJECT);
					}

					if (typeArgument == null)
					{
						continue;
					}
					
					for (final TypeVariable<?> TYPE_PARAMETER : TRACE)
					{
						typeArgument =
						(
							typeArgument.getTypeArgument
							(
								TYPE_PARAMETER
							)
						);
					}
					
					TYPE_ARGUMENTS.add(typeArgument);
					
					/*if (typeArgument != null)
					{
						TYPE_ARGUMENTS.add(typeArgument);
					}
					else
					{
						throw
						(
							new RuntimeException("Null type argument.")
						);
					}*/
				}
			}
		}
		
		if (TYPE_ARGUMENTS.isEmpty( ))
		{
			LOGGER.debug("Type arguments empty.");
			
			return null; /*
			
			throw
			(
				new RuntimeException("Type argument couldn't be determined.")
			); */
		}
		else
		{
			LOGGER.debug("Type arguments not empty: {}", TYPE_ARGUMENTS);
			
			final Iterator<TypeToken<?>> ITERATOR = TYPE_ARGUMENTS.iterator( );
			
			TypeToken<?> typeArgument = ITERATOR.next( );
			
			while (ITERATOR.hasNext( ))
			{
				final TypeToken<?> NEXT = ITERATOR.next( );
				
				LOGGER.debug("Next: {}", NEXT);
				
				typeArgument = typeArgument.getCommonSuperType(NEXT);
			}
			
			return typeArgument;
		}
	}
	
	private static TypeToken<?> typeOf(final TypeVariable<?> TYPE_VARIABLE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		return
		(
			typeOf
			(
				TYPE_VARIABLE,
				PARENT,
				PARAMETERIZATIONS,
				CALLERS,
				new IdentityHashMap<Object, TypeToken<?>>( )
			)
		);
	}
	
	private static TypeToken<?> typeOf(final Type TYPE, final TypeToken<?> PARENT, final Map<Type, List<Object>> PARAMETERIZATIONS, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		if (TYPE == null)
		{
			return null;
		}
		else if (TYPE instanceof Class)
		{
			return typeOf((Class<?>)TYPE, PARENT, PARAMETERIZATIONS, CALLERS);
		}
		else if (TYPE instanceof WildcardType)
		{
			return
			(
				typeOf((WildcardType)TYPE, PARENT, PARAMETERIZATIONS, CALLERS)
			);
		}
		else if (TYPE instanceof GenericArrayType)
		{
			return
			(
				typeOf
				(
					(GenericArrayType)TYPE, PARENT, PARAMETERIZATIONS, CALLERS
				)
			);
		}
		else if (TYPE instanceof ParameterizedType)
		{
			return
			(
				typeOf
				(
					(ParameterizedType)TYPE, PARENT, PARAMETERIZATIONS, CALLERS
				)
			);
		}
		else if (TYPE instanceof TypeVariable)
		{
			return
			(
				typeOf
				(
					(TypeVariable<?>)TYPE, PARENT, PARAMETERIZATIONS, CALLERS
				)
			);
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
	
	private static TypeToken<?> typeOf(final Type TYPE, final TypeToken<?> PARENT, final Map<TypeToken<?>, TypeToken<?>> CALLERS)
	{
		return typeOf(TYPE, PARENT, NO_PARAMETERIZATIONS, CALLERS);
	}
	
	static TypeToken<?> typeOf(final Type TYPE, final TypeToken<?> PARENT)
	{
		return
		(
			typeOf
			(
				TYPE, PARENT, new LinkedHashMap<TypeToken<?>, TypeToken<?>>( )
			)
		);
	}
	
	/**
	 * Models the type represented by the given {@code Type} instance.
	 * 
	 * @param TYPE The type to model.
	 * 
	 * @return A {@code TypeToken} representing {@code TYPE}.
	 * 
	 * @see java.lang.reflect.Type
	 */
	public static TypeToken<?> typeOf(final Type TYPE)
	{
		return typeOf(TYPE, (TypeToken<?>)null);
	}
	
	private static LinkedList<TypeVariable<?>> traceTypeVariable(final WildcardType TYPE, final TypeVariable<?> TYPE_VARIABLE)
	{
		final Type[ ] UPPER_BOUNDS = TYPE.getUpperBounds( );
		
		if (UPPER_BOUNDS.length == 1)
		{
			return traceTypeVariable(UPPER_BOUNDS[0], TYPE_VARIABLE);
		}
		else
		{
			throw new RuntimeException("Multiply bounded wildcard type.");
		}
	}
	
	private static LinkedList<TypeVariable<?>> traceTypeVariable(final ParameterizedType TYPE, final TypeVariable<?> TYPE_VARIABLE)
	{
		final Type RAW_TYPE = TYPE.getRawType( );
		
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
			TYPE.getActualTypeArguments( )
		);
		
		for (int index = 0; index < TYPE_PARAMETERS.length; index++)
		{
			try
			{
				final LinkedList<TypeVariable<?>> TRACE =
				(
					traceTypeVariable(TYPE_ARGUMENTS[index], TYPE_VARIABLE)
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
	
	private static LinkedList<TypeVariable<?>> traceTypeVariable(final TypeVariable<?> TYPE, final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE_VARIABLE.equals(TYPE))
		{
			return new LinkedList<TypeVariable<?>>( );
		}
		else
		{
			for (final Type BOUND : TYPE.getBounds( ))
			{
				try
				{
					return traceTypeVariable(BOUND, TYPE_VARIABLE);
				}
				catch (final RuntimeException e)
				{
					continue;
				}
			}
			
			throw new RuntimeException("Type variable not found.");
		}
	}
	
	private static LinkedList<TypeVariable<?>> traceTypeVariable(final Type TYPE, final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE instanceof Class)
		{
			throw new RuntimeException("Type variable not found.");
		}
		else if (TYPE instanceof WildcardType)
		{
			return traceTypeVariable((WildcardType)TYPE, TYPE_VARIABLE);
		}
		else if (TYPE instanceof ParameterizedType)
		{
			return traceTypeVariable((ParameterizedType)TYPE, TYPE_VARIABLE);
		}
		else if (TYPE instanceof TypeVariable)
		{
			return traceTypeVariable((TypeVariable<?>)TYPE, TYPE_VARIABLE);
		}
		else
		{
			throw new RuntimeException("Subtype of type not recognized.");
		}
	}
	
	/**
	 * Gets {@code this} type's raw type.
	 * 
	 * @return The {@code Class} instance representing {@code this} type.
	 */
	public final Class<T> getRawType( )
	{
		return RAW_TYPE;
	}
	
	/**
	 * Gets {@code this} type's enclosing type, if it exists.
	 * 
	 * @return The enclosing type, or {@code null} if {@code this} type is top-level or static.
	 */
	public final TypeToken<?> getEnclosingType( )
	{
		return ENCLOSING_TYPE;
	}
	
	/**
	 * Gets {@code this} type's super type, if it exists.
	 * 
	 * @return The super type, or {@code null} if it doesn't exist.
	 */
	public final TypeToken<?> getSuperType( )
	{
		return SUPER_TYPE;
	}
	
	/**
	 * Gets the types of {@code this} type's directly implemented interfaces.
	 * 
	 * The returned set is read-only.
	 * 
	 * @return A set of {@code this} type's directly implemented interfaces, or the empty set if no interfaces are implemented.
	 * 
	 * @see java.util.Set
	 */
	public final Set<TypeToken<?>> getInterfaces( )
	{
		return INTERFACES;
	}
	
	/**
	 * Gets the type arguments to {@code this} type, if it is generic.
	 * 
	 * The returned map is read-only.
	 * 
	 * @return The type parameters declared by {@code this} type, mapped to their respective type arguments, or the empty map if not generic.
	 * 
	 * @see java.util.Map
	 */
	public final Map<TypeVariable<?>, TypeToken<?>> getAllTypeArguments( )
	{
		return TYPE_ARGUMENTS;
	}
	
	/**
	 * Gets the type argument for a given type variable.
	 * 
	 * The given type variable may be declared directly by {@code this} type,
	 * by an in-scope enclosing type, a super type, or a directly or indirectly
	 * implemented interface.
	 * 
	 * @param TYPE_VARIABLE A type parameter declared by {@code this} type, its enclosing type, or its super types.
	 * 
	 * @return The type argument corresponding to {@code TYPE_VARIABLE}.
	 */
	public final TypeToken<?> getTypeArgument(final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE_ARGUMENTS.containsKey(TYPE_VARIABLE))
		{
			return TYPE_ARGUMENTS.get(TYPE_VARIABLE);
		}
		
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
		
		if (SUPER_TYPE != null)
		{
			try
			{
				return SUPER_TYPE.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final RuntimeException e)
			{
				
			}
		}
		
		for (final TypeToken<?> INTERFACE : INTERFACES)
		{
			try
			{
				return INTERFACE.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final RuntimeException e)
			{
				continue;
			}
		}
		
		LOGGER.debug("Type variable {} declared by {} not found.", TYPE_VARIABLE, TYPE_VARIABLE.getGenericDeclaration( ));
		
		throw new RuntimeException("Type variable not found.");
	}
	
	/**
	 * Gets the common super type of {@code this} and a given type.
	 * 
	 * @param THAT The type for which to get {@code this} type's common super type.
	 * 
	 * @return The most specific common super type of {@code this} type and {@code THAT}.
	 */
	public final TypeToken<?> getCommonSuperType(final TypeToken<?> THAT)
	{
		if (THAT == null || this.isAssignableFrom(THAT))
		{
			return this;
		}
		else if (THAT.isAssignableFrom(this))
		{
			return THAT;
		}
		else
		{
			TypeToken<?> type = this;
			
			while (true)
			{
				type = type.getSuperType( );
				
				if (type.isAssignableFrom(THAT))
				{
					return type;
				}
			}
		}
	}
	
	/**
	 * Models the return type of a given method, which may be generic.
	 * 
	 * If the return type depends on the type argument to a type parameter
	 * declared by the method, then that type parameter must be present in the
	 * method's parameter types, and an argument satisfying that type parameter
	 * must be provided. Otherwise, {@code ARGUMENTS} may be {@code null} or
	 * empty.
	 * 
	 * @param METHOD The method for which to model the return type.
	 * @param ARGUMENTS The arguments to the method.
	 * 
	 * @return The return type of {@code METHOD}, with type arguments taken into account.
	 */
	public final TypeToken<?> getReturnType(final Method METHOD, final Object... ARGUMENTS)
	{
		if (METHOD == null)
		{
			throw new IllegalArgumentException("No method specified.");
		}
		
		if (ARGUMENTS == null || ARGUMENTS.length == 0)
		{
			return
			(
				typeOf
				(
					METHOD.getGenericReturnType( ),
					this,
					new LinkedHashMap<TypeToken<?>, TypeToken<?>>( )
				)
			);
		}
		
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
				final Map<Type, List<Object>> PARAMETERIZATIONS =
				(
					new LinkedHashMap<Type, List<Object>>
					(
						ARGUMENTS.length, nextUp(1.0f)
					)
				);
				
				for (int index = 0; index < ARGUMENTS.length; index++)
				{
					if (ARGUMENTS[index] == null)
					{
						continue;
					}
					else if (!PARAMETERS[index].isInstance(ARGUMENTS[index]))
					{
						if (PARAMETERS[index].isPrimitive( ))
						{
							LOGGER.debug("Autoboxed method argument");
						}
						else
						{
							throw new RuntimeException( );
						}
					}
					
					final List<Object> OBJECTS =
					(
						PARAMETERIZATIONS.get(GENERIC_PARAMETERS[index])
					);
					
					if(OBJECTS == null)
					{
						LinkedList<Object> LIST = new LinkedList<Object>( );
						
						LIST.add(ARGUMENTS[index]);
						
						PARAMETERIZATIONS.put(GENERIC_PARAMETERS[index], LIST);
					}
					else
					{
						OBJECTS.add(ARGUMENTS[index]);
					}
				}
				
				return
				(
					typeOf
					(
						METHOD.getGenericReturnType( ),
						this,
						PARAMETERIZATIONS,
						new LinkedHashMap<TypeToken<?>, TypeToken<?>>( )
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
	
	static Set<Field> getAllInstanceFields(Class<?> type)
	{
		final Set<Field> INSTANCE_FIELDS = new LinkedHashSet<Field>( );
		
		do
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
		while (type != null);
		
		return INSTANCE_FIELDS;
	}
	
	/**
	 * Gets all instance fields declared or inherited by {@code this} type.
	 * 
	 * The returned fields may not be accessible.
	 * 
	 * @return A {@code Set} of the instance fields declared or inherited by {@code this} type.
	 * 
	 * @throws RuntimeException If a security manager denies access to any instance fields.
	 */
	public final Set<Field> getAllInstanceFields( )
	{
		if (instanceFields == null)
		{ // First time this method has been invoked on this instance.
			final Set<Field> INSTANCE_FIELDS = getAllInstanceFields(RAW_TYPE);
			
			if (INSTANCE_FIELDS.isEmpty( ))
			{
				instanceFields = NO_INSTANCE_FIELDS;
			}
			else
			{
				instanceFields = unmodifiableSet(INSTANCE_FIELDS);
			}
			
			if (instanceFields == null)
			{
				throw new RuntimeException( );
			}
		}
		
		return instanceFields;
	}
	
	/**
	 * Gets {@code this} type's accessible constructors.
	 * 
	 * Returned constructors are not necessarily public, but non-public
	 * constructors are set to accessible. If a non-public constructor can't be
	 * set to accessible, due to a restrictive security manager, then it is not
	 * included in the returned set. If there are no accessible constructors,
	 * the empty set is returned.
	 * 
	 * @return A {@code Set} of {@code this} type's accessible constructors.
	 */
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
	
	/**
	 * Checks if a given type is assignment-compatible with {@code this} type.
	 * 
	 * Assignment-compatible means that {@code this} type is the same as, or is
	 * a super type of, the given type.
	 * 
	 * @param THAT The type to be checked for assignment-compatibility.
	 * 
	 * @return {@code true} if instances of {@code THAT} can safely be assigned to variables of {@code this} type, else {@code false}.
	 * 
	 * @see java.lang.Class#isAssignableFrom(Class)
	 */
	public final boolean isAssignableFrom(final TypeToken<?> THAT)
	{
		if (THAT == null)
		{
			return false;
		}
		
		if (RAW_TYPE.isAssignableFrom(THAT.getRawType( )))
		{
			final Set<Entry<TypeVariable<?>, TypeToken<?>>> ENTRIES =
			(
				TYPE_ARGUMENTS.entrySet( )
			);
			
			for (final Entry<TypeVariable<?>, TypeToken<?>> ENTRY : ENTRIES)
			{
				final TypeToken<?> THIS_TYPE_ARGUMENT =
				(
					ENTRY.getValue( )
				);
				
				final TypeToken<?> THAT_TYPE_ARGUMENT =
				(
					THAT.getTypeArgument(ENTRY.getKey( ))
				);
				
				if (THIS_TYPE_ARGUMENT.isAssignableFrom(THAT_TYPE_ARGUMENT))
				{
					continue;
				}
				else
				{
					return false;
				}
			}
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Determines if a given object is an instance of {@code this} type.
	 * 
	 * @param OBJECT The object to check.
	 * 
	 * @return {@code true} if {@code OBJECT} is an instance of {@code this} type, else {@code false}.
	 * 
	 * @see java.lang.Class#isInstance(Object)
	 */
	public final boolean isInstance(final Object OBJECT)
	{
		return this.isAssignableFrom(typeOf(OBJECT));
	}
	
	/**
	 * Gets the hash code for {@code this} type.
	 * 
	 * This method is memoized, since this class is immutable. Repeated calls
	 * return the same value as the initial call, with no further work needed.
	 * 
	 * @return A hash code for {@code this} type.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
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
	
	/**
	 * Determines if a given object is equal to {@code this} type.
	 * 
	 * The given object is equal to {@code this} type if it is an instance of
	 * {@code TypeToken}, its raw type is equal to {@code this} type's raw
	 * type, its type arguments are equal to {@code this} type's type
	 * arguments, and either its enclosing type is equal to {@code this} type's
	 * enclosing type, or neither types have enclosing types.
	 * 
	 * @param THAT The object to compare {@code this} type against.
	 * 
	 * @return {@code true} if {@code this} type equals {@code THAT}, else {@code false}.
	 * 
	 * @see java.lang.Object#equals(Object)
	 */
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
	
	/**
	 * Gets the {@code String} representation of {@code this} type.
	 * 
	 * The {@code String} representation uses the simple names of {@code this}
	 * type's class, its enclosing type's class, and its type arguments'
	 * classes.
	 * 
	 * This method is memoized, since this class is immutable. Repeated calls
	 * return the same value as the initial call, with no further work needed.
	 * 
	 * @return The {@code String} representation of {@code this} type.
	 * 
	 * @see java.lang.Object#toString()
	 * @see java.lang.Class#getSimpleName()
	 */
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
			{ // Generic type.
				STRING_BUILDER.append('<');
				
				final Iterator<TypeToken<?>> TYPE_ARGUMENTS_ITERATOR =
				(
					TYPE_ARGUMENTS.values( ).iterator( )
				);
				
				while (true)
				{
					STRING_BUILDER.append(TYPE_ARGUMENTS_ITERATOR.next( ));
					
					if (TYPE_ARGUMENTS_ITERATOR.hasNext( ))
					{ // Only append separator if there are more parameters.
						STRING_BUILDER.append(',');
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