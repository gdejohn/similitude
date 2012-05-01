package org.gdejohn.similitude;

import static java.lang.Class.forName;
import static java.lang.Integer.valueOf;
import static java.lang.Math.nextUp;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.deepHashCode;
import static java.util.Collections.emptyMap;
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
public class TypeToken1<E>
{
	static final Logger LOGGER = getLogger(TypeToken1.class);
	
	private static final Map<TypeVariable<?>, TypeToken1<?>> NO_TYPE_ARGUMENTS =
	(
		emptyMap( )
	);
	
	private static final Map<Type, Object> NO_PARAMETERIZATIONS =
	(
		emptyMap( )
	);
	
	private final Class<E> RAW_TYPE;
	
	private final TypeToken1<?> OWNER_TYPE;
	
	private final TypeToken1<? super E> SUPER_TYPE;
	
	private final Set<TypeToken1<?>> INTERFACES;
	
	private final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS;
	
	private Set<Field> instanceFields = null;
	
	private Integer hashCode = null;
	
	private String toString = null;
	
	private TypeToken1(final Class<E> RAW_TYPE, final TypeToken1<?> OWNER_TYPE, final TypeToken1<? super E> SUPER_TYPE, final Set<TypeToken1<?>> INTERFACES, final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS)
	{
		LOGGER.debug
		(
			"Constructing type token, class: {}", RAW_TYPE.getName( )
		);
		
		this.RAW_TYPE = RAW_TYPE;
		this.OWNER_TYPE = OWNER_TYPE;
		this.SUPER_TYPE = SUPER_TYPE;
		this.INTERFACES = unmodifiableSet(INTERFACES);
		this.TYPE_ARGUMENTS = unmodifiableMap(TYPE_ARGUMENTS);
	}
	
	public static <T> TypeToken1<T> typeOf(final Class<T> RAW_TYPE, final TypeToken1<?> OWNER_TYPE, final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS)
	{
		LOGGER.debug("Getting type of class: {}", RAW_TYPE.getName( ));
		
		return new TypeToken1<T>(RAW_TYPE, OWNER_TYPE, typeOf(RAW_TYPE.getSuperclass( )), null, TYPE_ARGUMENTS);
	}
	
	public static <T> TypeToken1<T> typeOf(final Class<T> RAW_TYPE, final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS)
	{
		//final Class<?> ENCLOSING_CLASS = RAW_TYPE.getEnclosingClass( );
		
		return
		(
			typeOf
			(
				RAW_TYPE,
				null, // ENCLOSING_CLASS == null ? null : typeOf(ENCLOSING_CLASS),
				TYPE_ARGUMENTS
			)
		);
	}
	
	public static <T> TypeToken1<T> typeOf(final Class<T> RAW_TYPE, final TypeToken1<?> PARENT)
	{
		LOGGER.debug("Getting type of class: {}", RAW_TYPE.getName( ));
		
		final TypeVariable<Class<T>>[ ] TYPE_PARAMETERS =
		(
			RAW_TYPE.getTypeParameters( )
		);
		
		if (TYPE_PARAMETERS.length == 0)
		{
			return typeOf(RAW_TYPE, NO_TYPE_ARGUMENTS);
		}
		else if (PARENT == null)
		{
			throw new RuntimeException("Missing type arguments.");
		}
		else
		{
			final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>
				(
					TYPE_PARAMETERS.length, nextUp(1.0f)
				)
			);
			
			for (final TypeVariable<?> TYPE_VARIABLE : TYPE_PARAMETERS)
			{
				TYPE_ARGUMENTS.put
				(
					TYPE_VARIABLE,
					PARENT.getTypeArgument(TYPE_VARIABLE)
				);
			}
			
			return typeOf(RAW_TYPE, TYPE_ARGUMENTS);
		}
	}
	
	public static <T> TypeToken1<T> typeOf(final Class<T> RAW_TYPE)
	{
		return typeOf(RAW_TYPE, (TypeToken1<?>)null);
	}
	
	public static TypeToken1<?> typeOf(final GenericArrayType TYPE, final TypeToken1<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		Type current = TYPE;
		
		final StringBuilder CLASS_NAME = new StringBuilder( );
		
		do
		{
			current =
			(
				((GenericArrayType)current).getGenericComponentType( )
			);
			
			CLASS_NAME.append('[');
		}
		while (current instanceof GenericArrayType);
		
		CLASS_NAME.append('L');
		
		CLASS_NAME.append(typeOf(current, PARENT, PARAMETERIZATIONS).getRawType( ).getName( ));
		
		CLASS_NAME.append(';');
		
		try
		{
			return typeOf(forName(CLASS_NAME.toString( )));
		}
		catch (final ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static TypeToken1<?> typeOf(final ParameterizedType PARAMETERIZED_TYPE, final TypeToken1<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		final Type RAW_TYPE = PARAMETERIZED_TYPE.getRawType( );
		
		final Class<?> CLASS;
		
		if (RAW_TYPE instanceof Class)
		{
			CLASS = (Class<?>)RAW_TYPE;
			
			final TypeVariable<?>[ ] TYPE_VARIABLES =
			(
				CLASS.getTypeParameters( )
			);
			
			final Type[ ] ACTUAL_TYPE_ARGUMENTS =
			(
				PARAMETERIZED_TYPE.getActualTypeArguments( )
			);
			
			final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>( )
			);
			
			if (TYPE_VARIABLES.length == ACTUAL_TYPE_ARGUMENTS.length)
			{
				for (int index = 0; index < TYPE_VARIABLES.length; index++)
				{
					TYPE_ARGUMENTS.put
					(
						TYPE_VARIABLES[index],
						typeOf(ACTUAL_TYPE_ARGUMENTS[index], PARENT, PARAMETERIZATIONS)
					);
				}
				
				final Type OWNER_TYPE = PARAMETERIZED_TYPE.getOwnerType( );
				
				return
				(
					typeOf
					(
						CLASS,
						OWNER_TYPE == null ? null : typeOf(OWNER_TYPE, PARENT, PARAMETERIZATIONS),
						TYPE_ARGUMENTS
					)
				);
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
			throw new RuntimeException("wat");
		}
	}
	
	static LinkedList<TypeVariable<?>> traceTypeVariable(final TypeVariable<?> TYPE_VARIABLE, final Type TYPE)
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
		{ // Check upper bounds.
			throw new UnsupportedOperationException( );
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
					final LinkedList<TypeVariable<?>> REST =
					(
						traceTypeVariable
						(
							TYPE_VARIABLE, TYPE_ARGUMENTS[index]
						)
					);
					
					REST.addFirst(TYPE_PARAMETERS[index]);
					
					return REST;
				}
				catch (final RuntimeException e)
				{
					LOGGER.debug("Exception.", e);
					
					LOGGER.debug
					(
						"Type variable not found: {}, {}, {}, {}",
						new Object[ ]
						{
							TYPE_VARIABLE,
							TYPE,
							TYPE_PARAMETERS[index],
							TYPE_ARGUMENTS[index]
						}
					);
					
					continue;
				}
			}
		}
		
		throw new RuntimeException("I dunno, LOL.");
	}
	
	public static TypeToken1<?> typeOf(final TypeVariable<?> TYPE, final TypeToken1<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		try
		{
			return PARENT.getTypeArgument(TYPE);
		}
		catch (final Exception e)
		{
			LOGGER.debug("Type argument not found in parent.");
		}
		
		for (final Entry<Type, Object> ENTRY : PARAMETERIZATIONS.entrySet( ))
		{
			try
			{
				final List<TypeVariable<?>> TYPE_VARIABLES =
				(
					traceTypeVariable(TYPE, ENTRY.getKey( ))
				);
				
				TypeToken1<?> typeArgument = typeOf(ENTRY.getValue( ));
				
				for (final TypeVariable<?> TYPE_VARIABLE : TYPE_VARIABLES)
				{
					typeArgument =
					(
						typeArgument.getTypeArgument(TYPE_VARIABLE)
					);
				}
				
				return typeArgument;
			}
			catch (final RuntimeException e)
			{
				continue;
			}
		}
		
		return null; // throw new RuntimeException( );
	}
	
	public static <T> TypeToken1<?> typeOf(final WildcardType TYPE, final TypeToken1<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		final Type[ ] UPPER_BOUNDS = TYPE.getUpperBounds( );
		
		if (UPPER_BOUNDS.length == 1)
		{
			return typeOf(UPPER_BOUNDS[0], PARENT, PARAMETERIZATIONS);
		}
		else
		{
			throw new UnsupportedOperationException( );
		}
	}
	
	public static TypeToken1<?> typeOf(final Type TYPE, final TypeToken1<?> PARENT, final Map<Type, Object> PARAMETERIZATIONS)
	{
		if (TYPE instanceof Class)
		{
			return typeOf((Class<?>)TYPE, PARENT);
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
		else if (TYPE instanceof WildcardType)
		{
			return typeOf((WildcardType)TYPE, PARENT, PARAMETERIZATIONS);
		}
		else
		{
			throw new RuntimeException("Subtype of type not recognized.");
		}
	}
	
	public static TypeToken1<?> typeOf(final Type TYPE, final TypeToken1<?> PARENT)
	{
		return typeOf(TYPE, PARENT, NO_PARAMETERIZATIONS);
	}
	
	public static <T> TypeToken1<? extends T> typeOf(final T INSTANCE)
	{
		LOGGER.debug("Getting type of object: {}", INSTANCE);
		
		@SuppressWarnings("unchecked")
		final Class<? extends T> RAW_TYPE =
		(
			(Class<? extends T>)INSTANCE.getClass( )
		);
		
		final TypeVariable<?>[ ] TYPE_PARAMETERS =
		(
			RAW_TYPE.getTypeParameters( )
		);
		
		final Set<Field> INSTANCE_FIELDS = getAllInstanceFields(RAW_TYPE);
		
		final Class<?> ENCLOSING_CLASS = RAW_TYPE.getEnclosingClass( );
		
		final TypeToken1<?> OWNER_TYPE;
		
		if (ENCLOSING_CLASS == null) // || isStatic(RAW_TYPE.getModifiers( )))
		{
			OWNER_TYPE = null;
		}
		else
		{
			final Iterator<Field> FIELD_ITERATOR = INSTANCE_FIELDS.iterator( );
			
			while (true)
			{
				if (FIELD_ITERATOR.hasNext( ))
				{
					final Field FIELD = FIELD_ITERATOR.next( );
					
					if (FIELD.isSynthetic( ))
					{
						final Class<?> FIELD_TYPE = FIELD.getType( );
						
						if (ENCLOSING_CLASS.equals(FIELD_TYPE))
						{
							if (FIELD.getName( ).matches("^this\\$\\d++$"))
							{
								final Object VALUE;
								
								try
								{
									VALUE = FIELD.get(INSTANCE);
								}
								catch (final IllegalAccessException e)
								{
									continue;
								}
								
								if (VALUE != null)
								{
									OWNER_TYPE = typeOf(VALUE);
								}
								else
								{
									OWNER_TYPE = typeOf(FIELD_TYPE);
								}
								
								break;
							}
						}
					}
				}
				else
				{
					throw new RuntimeException( );
				}
			}
		}
		
		final Map<TypeVariable<?>, TypeToken1<?>> TYPE_ARGUMENTS;
		
		if (TYPE_PARAMETERS.length == 0)
		{
			TYPE_ARGUMENTS = NO_TYPE_ARGUMENTS;
		}
		else
		{
			final Map<Type, Object> PARAMETERIZATIONS =
			(
				new LinkedHashMap<Type, Object>
				(
					INSTANCE_FIELDS.size( ), nextUp(1.0f)
				)
			);
			
			for (final Field FIELD : INSTANCE_FIELDS)
			{
				final Object VALUE;
				
				try
				{
					VALUE = FIELD.get(INSTANCE);
				}
				catch (final IllegalAccessException e)
				{
					continue;
				}
				
				if (VALUE != null)
				{
					PARAMETERIZATIONS.put(FIELD.getGenericType( ), VALUE);
				}
			}
			
			TYPE_ARGUMENTS =
			(
				new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>
				(
					TYPE_PARAMETERS.length, nextUp(1.0f)
				)
			);
			
			for (final TypeVariable<?> TYPE_VARIABLE : TYPE_PARAMETERS)
			{
				TYPE_ARGUMENTS.put
				(
					TYPE_VARIABLE,
					typeOf(TYPE_VARIABLE, null, PARAMETERIZATIONS)
				);
			}
		}
		
		return typeOf(RAW_TYPE, OWNER_TYPE, TYPE_ARGUMENTS);
	}
	
	public Class<E> getRawType( )
	{
		return RAW_TYPE;
	}
	
	public TypeToken1<?> getOwnerType( )
	{
		return OWNER_TYPE;
	}
	
	public TypeToken1<? super E> getSuperType( )
	{
		return SUPER_TYPE;
	}
	
	public Set<TypeToken1<?>> getInterfaces( )
	{
		return INTERFACES;
	}
	
	public Map<TypeVariable<?>, TypeToken1<?>> getAllTypeArguments( )
	{
		return TYPE_ARGUMENTS;
	}
	
	static Set<Field> getAllInstanceFields(Class<?> type)
	{
		final Set<Field> INSTANCE_FIELDS = new LinkedHashSet<Field>( );
		
		while (type != null)
		{
			for (final Field FIELD : type.getDeclaredFields( ))
			{
				if (isStatic(FIELD.getModifiers( )))
				{ // If static, ignore and skip to the next one.
					LOGGER.debug("Skipping static field: {}", FIELD);
					
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
		}
		
		return instanceFields;
	}
	
	/**
	 * Gets accessible constructors for the given class.
	 * 
	 * The result depends on whether there's a SecurityManager present and how
	 * restrictive it is. All declared constructors are tried first. Failing
	 * that, public constructors are tried next. Failing that, an exception is
	 * thrown.
	 * 
	 * @param CLASS The class for which to get the constructors.
	 * 
	 * @return Either all of the declared constructors for {@code CLASS}, or the public constructors.
	 * 
	 * @throws InstantiationFailedException If neither the declared nor public constructors are accessible.
	 */
	public Set<Constructor<E>> getAccessibleConstructors( )
	{
		final Set<Constructor<E>> CONSTRUCTORS =
		(
			new LinkedHashSet<Constructor<E>>( )
		);
		
		try
		{
			try
			{
				for (final Constructor<?> RAW : RAW_TYPE.getDeclaredConstructors( ))
				{
					try
					{
						/*
						 * The constructor to be invoked must be parameterized by
						 * type T to return a value of type T. Passing the array of
						 * Class instances representing the parameter types
						 * uniquely identifying the current constructor to
						 * Class.getDeclaredConstructor( ) returns the same
						 * constructor, but with the required type information.
						 */
						final Constructor<E> PARAMETERIZED =
						(
							RAW_TYPE.getDeclaredConstructor
							(
								RAW.getParameterTypes( )
							)
						);
						
						/*
						 * Constructors that wouldn't normally be accessible (e.g.
						 * private) need to be made accessible before they can be
						 * invoked.
						 */
						PARAMETERIZED.setAccessible(true);
						
						CONSTRUCTORS.add(PARAMETERIZED);
					}
					catch (final SecurityException e)
					{
						LOGGER.debug
						(
							"Non-public constructors not available for type: {}",
							RAW_TYPE.getSimpleName( )
						);
						
						continue;
					}
				}
			}
			catch (final SecurityException e)
			{
				LOGGER.debug
				(
					"Non-public constructors not available for type: {}",
					RAW_TYPE.getSimpleName( )
				);
				
				for (final Constructor<?> RAW : RAW_TYPE.getConstructors( ))
				{
					CONSTRUCTORS.add
					(
						RAW_TYPE.getDeclaredConstructor
						(
							RAW.getParameterTypes( )
						)
					);
				}
			}
			
			return CONSTRUCTORS;
		}
		catch (final SecurityException e)
		{
			LOGGER.debug
			(
				"Public constructors not available for: {}",
				RAW_TYPE.getSimpleName( )
			);
			
			throw
			(
				new InstantiationFailedException
				(
					e,
					"No available constructors for: {}",
					RAW_TYPE.getSimpleName( )
				)
			);
		}
		catch (final NoSuchMethodException e)
		{
			LOGGER.error
			(
				"Constructor not found.",
				e
			);
			
			throw new RuntimeException(e);
		}
	}
	
	public TypeToken1<?> getTypeArgument(final TypeVariable<?> TYPE_VARIABLE)
	{
		if (TYPE_ARGUMENTS.containsKey(TYPE_VARIABLE))
		{
			return TYPE_ARGUMENTS.get(TYPE_VARIABLE);
		}
		else
		{
			try
			{
				return OWNER_TYPE.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final RuntimeException e)
			{
				LOGGER.debug
				(
					"Type variable {} not found in owner type.", TYPE_VARIABLE
				);
			}
			
			try
			{
				return SUPER_TYPE.getTypeArgument(TYPE_VARIABLE);
			}
			catch (final RuntimeException e)
			{
				LOGGER.debug
				(
					"Type variable {} not found in super class.", TYPE_VARIABLE
				);
			}
			
			for (final TypeToken1<?> INTERFACE : TYPE_ARGUMENTS.values( ))
			{
				try
				{
					return INTERFACE.getTypeArgument(TYPE_VARIABLE);
				}
				catch (final RuntimeException e)
				{
					LOGGER.debug
					(
						"Type variable {} not found in interfaces.",
						TYPE_VARIABLE
					);
					
					continue;
				}
			}
			
			throw new RuntimeException("Type variable not found.");
		}
	}
	
	public TypeToken1<?> getReturnType(final Method METHOD, final Object... ARGUMENTS)
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
				final Map<Type, Object> INSTANCES =
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
						INSTANCES.put
						(
							GENERIC_PARAMETERS[index], ARGUMENTS[index]
						);
					}
					else if (ARGUMENTS[index] != null)
					{
						throw new RuntimeException( );
					}
				}
				
				return typeOf(METHOD.getGenericReturnType( ), this, INSTANCES);
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
	
	public boolean isAssignableFrom(final TypeToken1<?> THAT)
	{
		if (RAW_TYPE.isAssignableFrom(THAT.getRawType( )))
		{
			final Map<TypeVariable<?>, TypeToken1<?>> THOSE_TYPE_ARGUMENTS =
			(
				THAT.getAllTypeArguments( )
			);
			
			if (TYPE_ARGUMENTS.size( ) == THOSE_TYPE_ARGUMENTS.size( ))
			{
				for (final Entry<TypeVariable<?>, TypeToken1<?>> ENTRY : TYPE_ARGUMENTS.entrySet( ))
				{
					if (ENTRY.getValue( ).isAssignableFrom(THOSE_TYPE_ARGUMENTS.get(ENTRY.getKey( ))))
					{
						continue;
					}
					else
					{
						return false;
					}
				}
				
				if (OWNER_TYPE.isAssignableFrom(THAT.getOwnerType( )))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isInstance(final Object INSTANCE)
	{
		return isAssignableFrom(typeOf(INSTANCE));
	}
	
	@Override
	public int hashCode( )
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
							RAW_TYPE, OWNER_TYPE, TYPE_ARGUMENTS
						}
					)
				)
			);
		}
		
		return hashCode.intValue( );
	}
	
	@Override
	public boolean equals(final Object THAT)
	{
		if (THAT instanceof TypeToken1)
		{
			final TypeToken1<?> THAT_TYPE_TOKEN = (TypeToken1<?>)THAT;

			return
			(
				RAW_TYPE.equals(THAT_TYPE_TOKEN.getRawType( )) &&
				// OWNER_TYPE.equals(THAT_TYPE_TOKEN.getOwnerType( )) &&
				TYPE_ARGUMENTS.equals(THAT_TYPE_TOKEN.getAllTypeArguments( ))
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
		{ // First time this method has been invoked on this instance.
			final StringBuilder STRING_BUILDER = new StringBuilder( );
			
			if (OWNER_TYPE != null)
			{
				STRING_BUILDER.append(OWNER_TYPE.toString( )).append('.');
			}
			
			STRING_BUILDER.append(RAW_TYPE.getSimpleName( ));
			
			if (TYPE_ARGUMENTS.isEmpty( ) == false)
			{ // Parameterized type.
				STRING_BUILDER.append('<');
				
				final Iterator<TypeToken1<?>> ARGUMENTS =
				(
					TYPE_ARGUMENTS.values( ).iterator( )
				);
				
				while (true)
				{
					final TypeToken1<?> ARGUMENT = ARGUMENTS.next( );
					
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
			
			if (toString == null) // || toString.isEmpty( ))
			{
				throw new RuntimeException( );
			}
		}
		
		return toString;
	}
	
	static Map<LinkedHashMap<List<Number>, Set<String>>, java.util.Collection<java.io.File>> foo( )
	{
		return null;
	}
	
	static class Foo<E>
	{
		Foo(Map<?, ?>[][] arg)
		{
			
		}
		
		class Bar<F>
		{
			class Baz<G>
			{
				
			}
		}
	}
	
	public static class Test
	{
		public Test(Foo<Number>.Bar<Integer>.Baz<String> arg)
		{
			
		}
	}
	
	static TypeToken1<?> typeOf(Object o, Object b)
	{
		Map<TypeVariable<?>, TypeToken1<?>> thirdMap = new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>( );
		thirdMap.put(Third.class.getTypeParameters( )[0], typeOf(String.class));
		TypeToken1<?> third = typeOf(Third.class, thirdMap);
		Map<TypeVariable<?>, TypeToken1<?>> secondMap = new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>( );
		secondMap.put(Second.class.getTypeParameters( )[0], third);
		TypeToken1<?> second = typeOf(Second.class, secondMap);
		Map<TypeVariable<?>, TypeToken1<?>> firstMap = new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>( );
		firstMap.put(First.class.getTypeParameters( )[0], second);
		TypeToken1<?> first = typeOf(First.class, firstMap);
		return first;
	}
	
	static class First<F>
	{
		F field;
		
		First(F arg)
		{
			field = arg;
		}
	}
	
	static class Second<S>
	{
		S field;
		
		Second(S arg)
		{
			field = arg;
		}
	}
	
	static class Third<T>
	{
		T field;
		
		Third(T arg)
		{
			field = arg;
		}
	}
	
	static <B> B bar(First<Second<Third<B>>> arg)
	{
		return null;
	}
	
	public static void main(String[ ] args) throws Exception
	{
		//System.out.println(typeOf(new First<Second<Third<Integer>>>(new Second<Third<Integer>>(new Third<Integer>(0)))));
		//System.out.println(typeOf(String.class).getReturnType(TypeToken1.class.getDeclaredMethod("bar", First.class), new First<Second<Third<Integer>>>( )));
		//Map<TypeVariable<?>, TypeToken1<?>> map = new LinkedHashMap<TypeVariable<?>, TypeToken1<?>>( );
		//map.put(Foo.class.getTypeParameters( )[0], typeOf(Map.class));
		//System.out.println(typeOf(Foo.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0], null)); // typeOf(Foo.class, map)));
		//System.out.println(java.util.Arrays.asList("foo", "bar", "baz").subList(1, 3) instanceof java.util.RandomAccess);
		//System.out.println(getTypeOf(TypeToken1.class.getDeclaredMethod("foo").getGenericReturnType( ), null));
		//System.out.println(getTypeOf(Test.class.getConstructors( )[0].getGenericParameterTypes( )[0], null));
	}
	
	/*
	
	private static <T> TypeToken<T> typeOf(final Class<T> CLASS, final Map<TypeVariable<?>, TypeToken<?>> TYPE_ARGUMENTS)
	{
		LOGGER.debug
		(
			"Getting type of class \"{}\" with type arguments \"{}\"",
			CLASS.getSimpleName( ),
			TYPE_ARGUMENTS
		);
		
		return typeOf(CLASS, TYPE_ARGUMENTS, null);
	}
	
	*/
}