package org.gdejohn.similitude;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public class TypeRef<E>
{
	static final Logger LOGGER = getLogger(TypeRef.class);
	
	static final Map<TypeVariable<?>, TypeRef<?>> EMPTY_MAP =
	(
		Collections.emptyMap( )
	);
	
	private final Type TYPE;
	
	private final Class<? super E> RAW_TYPE;
	
	private final TypeRef<?> OWNER_TYPE = null;
	
	private final boolean IS_GENERIC;
	
	private final boolean IS_IMMUTABLE = false;
	
	private final String TO_STRING = "";
	
	private final Map<TypeVariable<?>, TypeRef<?>> TYPE_ARGUMENTS;
	
	@SuppressWarnings("unchecked")
	private TypeRef(final Type TYPE, final Object... PARAMETERIZATIONS)
	{
		LOGGER.debug("Type: {}", TYPE);
		
		this.TYPE = TYPE;
		
		if (TYPE instanceof Class)
		{
			RAW_TYPE = (Class<? super E>)TYPE;
			
			final TypeVariable<?>[ ] PARAMETERS =
			(
				RAW_TYPE.getTypeParameters( )
			);
			
			if (PARAMETERS == null || PARAMETERS.length == 0)
			{
				IS_GENERIC = false;
				TYPE_ARGUMENTS = EMPTY_MAP;
			}
			else
			{
				throw
				(
					new RuntimeException
					(
						"Raw type."
					)
				);
			}
		}
		else if (TYPE instanceof GenericArrayType)
		{
			throw
			(
				new RuntimeException
				(
					"Subtype of java.lang.Type not recognized."
				)
			);
		}
		else if (TYPE instanceof WildcardType)
		{
			throw
			(
				new RuntimeException
				(
					"Subtype of java.lang.Type not recognized."
				)
			);
		}
		else if (TYPE instanceof TypeVariable)
		{
			throw
			(
				new RuntimeException
				(
					"Subtype of java.lang.Type not recognized."
				)
			);
		}
		else if (TYPE instanceof ParameterizedType)
		{
			RAW_TYPE = (Class<? super E>)((ParameterizedType)TYPE).getRawType( );
			
			IS_GENERIC = true;
			
			TYPE_ARGUMENTS = mapTypeArguments(RAW_TYPE, (ParameterizedType)TYPE);
			
			throw
			(
				new RuntimeException
				(
					"Subtype of java.lang.Type not recognized."
				)
			);
		}
		else
		{
			throw
			(
				new RuntimeException
				(
					"Subtype of java.lang.Type not recognized."
				)
			);
		}
	}
	
	protected TypeRef( )
	{
		/*final Type SUPERCLASS = this.getClass( ).getGenericSuperclass( );
		
		if (SUPERCLASS instanceof ParameterizedType)
		{
			final ParameterizedType TYPE = (ParameterizedType)SUPERCLASS;
			
			if (TYPE.getRawType( ).equals(TypeRef.class))
			{
				GENERIC = TYPE.getActualTypeArguments( )[0];
				
				LOGGER.debug("Type: {}", GENERIC);
				
				return;
			}
		}*/
		
		throw
		(
			new RuntimeException
			(
				"This constructor may only be invoked by parameterizing sub-types."
			)
		);
	}
	
	public static TypeRef<?> getTypeOf(final Type TYPE, final Object... ARGUMENTS)
	{
		return new TypeRef<Object>(TYPE, ARGUMENTS);
	}
	
	public static <T> TypeRef<T> getTypeOf(final Class<T> CLASS)
	{
		return new TypeRef<T>(CLASS);
	}
	
	public static <T> TypeRef<? extends T> getTypeOf(final T INSTANCE)
	{
		@SuppressWarnings("unchecked")
		final Class<? extends T> CLASS = (Class<? extends T>)INSTANCE.getClass( );
		
		return getTypeOf(CLASS);
	}
	
	private static Map<TypeVariable<?>, TypeRef<?>> mapTypeArguments(final Class<?> RAW_TYPE, final ParameterizedType TYPE)
	{
		final Map<TypeVariable<?>, TypeRef<?>> MAP =
		(
			new LinkedHashMap<TypeVariable<?>, TypeRef<?>>( )
		);
		
		final TypeVariable<?>[ ] PARAMETERS =
		(
			RAW_TYPE.getTypeParameters( )
		);
		
		final Type[ ] ARGUMENTS =
		(
			((ParameterizedType)TYPE).getActualTypeArguments( )
		);
		
		int index = 0;
		
		while (true)
		{
			LOGGER.debug
			(
				"Argument for type parameter {}: {}",
				PARAMETERS[index],
				ARGUMENTS[index]
			);
			
			final TypeRef<?> ARGUMENT =
			(
				getTypeOf(ARGUMENTS[index])
			);
			
			MAP.put
			(
				PARAMETERS[index],
				ARGUMENT
			); // Look through constructor args? GenericArrayType? If WildcardType, use bounds?
			
			//TEMP.append(ARGUMENT);
			
			if (++index < PARAMETERS.length)
			{ // Only append separator if there are more parameters.
				//TEMP.append(",");
			}
			else
			{
				break;
			}
		}
		
		return MAP;
	}
	
	public final Type getType( )
	{
		return TYPE;
	}
	
	public final Class<? super E> getRawType( )
	{
		return RAW_TYPE;
	}
	
	public final boolean isGeneric( )
	{
		return IS_GENERIC;
	}
	
	public final boolean isArray( )
	{
		return RAW_TYPE.isArray( );
	}
	
	public final boolean isImmutable( )
	{
		return IS_IMMUTABLE;
	}
	
	public final TypeRef<?> getArgumentFor(final TypeVariable<?> TYPE_VARIABLE)
	{
		final TypeRef<?> ARGUMENT = TYPE_ARGUMENTS.get(TYPE_VARIABLE);
		
		if (ARGUMENT == null)
		{
			return OWNER_TYPE.getArgumentFor(TYPE_VARIABLE);
		}
		else
		{
			return ARGUMENT;
		}
	}
	
	public final Map<TypeVariable<?>, TypeRef<?>> getTypeArguments( )
	{
		return TYPE_ARGUMENTS;
	}
	
	@Override
	public final int hashCode( )
	{
		return 0;
	}
	
	@Override
	public final boolean equals(final Object THAT)
	{
		return false;
	}
	
	@Override
	public final String toString( )
	{
		return TO_STRING;
	}
	
	static class Foo
	{
		class Bar<E>
		{
			class Baz<T extends E>
			{
				E doThings(E arg)
				{
					return arg;
				}
			}
		}
	}
	
	interface Interface<T, U extends T>
	{
		Foo.Bar<T>.Baz<U> doStuff( );
	}
	
	static class Clazz
	{
		Clazz(Interface<CharSequence, String> arg)
		{
			arg.doStuff( );
		}
	}/*
	
	static <E> E instantiate(Builder builder, final TypeRef<E> TYPE)
	{
		return
		(
			builder.instantiate
			(
				TYPE.getRawType( ),
				builder.getTypeArguments(TYPE.getType( ), Collections.<TypeVariable<?>, Type>emptyMap( ))
			)
		);
	}
	
	public static void main(String[ ] args) throws Exception
	{
		Builder builder = new Builder( );
		builder.addDefault(String.class, "xyzzy");
		java.util.List<String> list = instantiate(builder, new TypeRef<java.util.List<String>>( ) { });
		System.out.println(list.get(0).toUpperCase( ));
		
		System.out.println(new TypeToken<Object>(TypeToken.class.getDeclaredMethod("snuts").getGenericReturnType( )));
		TypeToken<Object> conParam = new TypeToken<Object>(Clazz.class.getDeclaredConstructors( )[0].getGenericParameterTypes( )[0]);
		System.out.println(conParam);
		TypeToken<Object> returnType = new TypeToken<Object>(Interface.class.getDeclaredMethod("doStuff").getGenericReturnType( ), conParam, false);
		System.out.println(returnType);
	}
	
	@SuppressWarnings("unused")
	public static void main(String[ ] args) throws Exception
	{
		String o = "xyzzy";
		CharSequence c = new Cloner( ).toClone((CharSequence)o);
		System.out.println(java.util.Arrays.toString(new Builder( ).getTypeArguments(Baz.class.getDeclaredMethod("doStuff").getGenericReturnType( ), Collections.<TypeVariable<?>, Type>emptyMap( )).entrySet( ).toArray( )));
		System.out.println(((ParameterizedType)new Object( ){void foo(java.util.List<String> l){ }}.getClass( ).getDeclaredMethod("foo", java.util.List.class).getGenericParameterTypes( )[0]).getRawType( ));
	}
	
	Class<?> reify(final Type TYPE, final Map<TypeVariable<?>, Type> TYPE_ARGUMENTS)
	{
		if (TYPE instanceof Class)
		{
			return (Class<?>)TYPE;
		}
		else if (TYPE_ARGUMENTS.containsKey(TYPE))
		{
			return reify(TYPE_ARGUMENTS.get(TYPE), TYPE_ARGUMENTS);
		}
		else if (TYPE instanceof GenericArrayType)
		{
			return
			(
				reify
				(
					((GenericArrayType)TYPE).getGenericComponentType( ),
					TYPE_ARGUMENTS
				)
			);
		}
		
		return null;
	}
	
	@Override
	public Object _invoke(final Object PROXY, final Method METHOD, final Object[ ] ARGUMENTS)
	{
		LOGGER.debug
		(
			"Method \"{}\" invoked on proxy instance implementing: {}",
			METHOD.toGenericString( ),
			PROXY.getClass( ).getInterfaces( )
		);
		
		final Type RETURN_TYPE = METHOD.getGenericReturnType( );
		
		if (RETURN_TYPE instanceof Class)
		{
			return BUILDER.instantiate(METHOD.getReturnType( ));
		}
		else if (RETURN_TYPE instanceof GenericArrayType)
		{
			LOGGER.debug
			(
				"Generic array return type for method: {}",
				METHOD.toGenericString( )
			);
			
			Type componentType = RETURN_TYPE;
			
			int dimensions = 0;
			
			do
			{
				componentType =
				(
					((GenericArrayType)componentType).getGenericComponentType( )
				);
				
				dimensions++;
			}
			while (componentType instanceof GenericArrayType);
			
			final Class<?> ACTUAL_COMPONENT_TYPE;
			
			if (componentType instanceof ParameterizedType)
			{
				ACTUAL_COMPONENT_TYPE =
				(
					(Class<?>)((ParameterizedType)componentType).getRawType( )
				);
			}
			else if (componentType instanceof TypeVariable)
			{
				ACTUAL_COMPONENT_TYPE =
				(
					ARGUMENTS[0].getClass( )
				);
			}
			else
			{
				throw
				(
					new InstantiationFailedException
					(
						"Couldn't reify generic return type."
					)
				);
			}
			
			return
			(
				Array.newInstance
				(
					ACTUAL_COMPONENT_TYPE, new int[dimensions] // (Class<?>)componentType
				)
			);
		}
		else if (RETURN_TYPE instanceof ParameterizedType)
		{
			LOGGER.debug
			(
				"Paramaterized return type for method: {}",
				METHOD.toGenericString( )
			);
			
			return
			(
				BUILDER.instantiate
				(
					(Class<?>)((ParameterizedType)RETURN_TYPE).getRawType( ),
					BUILDER.getTypeArguments
					(
						RETURN_TYPE, TYPE_ARGUMENTS, ARGUMENTS
					)
				)
			);
		}
		else if (RETURN_TYPE instanceof TypeVariable)
		{
			LOGGER.debug
			(
				"Type variable return type for method: {}",
				METHOD.toGenericString( )
			);
			
			if (TYPE_ARGUMENTS != null && TYPE_ARGUMENTS.containsKey(RETURN_TYPE))
			{
				return
				(
					BUILDER.instantiate
					(
						(Class<?>)TYPE_ARGUMENTS.get(RETURN_TYPE),
						TYPE_ARGUMENTS
					)
				);
			}
			else
			{
				final Type[ ] PARAMETERS = METHOD.getGenericParameterTypes( );
				
				for (int index = 0; index < PARAMETERS.length; index++)
				{ // Check arguments for parameterization of return type.
					try
					{
						LOGGER.debug("Parameter: {}, Argument: {}", PARAMETERS[index], ARGUMENTS[index]); /////////////////////////////////////////////////////////////////////////////
						
						if(ARGUMENTS[index] != null)
						{
							if (RETURN_TYPE.equals(PARAMETERS[index]))
							{
								LOGGER.debug
								(
									"Generic return type {} parameterized by argument type {}.",
									RETURN_TYPE,
									ARGUMENTS[index].getClass( ).getSimpleName( )
								);
								
								return
								(
									BUILDER.instantiate(ARGUMENTS[index].getClass( ))
								);
							}
							else if(PARAMETERS[index] instanceof GenericArrayType)
							{
								if (RETURN_TYPE.equals(((GenericArrayType)PARAMETERS[index]).getGenericComponentType( )))
								{
									return BUILDER.instantiate(ARGUMENTS[index].getClass( ).getComponentType( ));
								}
							}
						}
					}
					catch (InstantiationFailedException e)
					{
						LOGGER.debug
						(
							"Instantiating argument type failed.",
							e
						);
					}
				}
				
				throw
				(
					new InstantiationFailedException
					(
						"Couldn't find parameterization of generic return type."
					)
				);
			}
		}
		else
		{
			throw
			(
				new InstantiationFailedException
				(
					"Couldn't instantiate return type."
				)
			);
		}
	}
	
	static abstract class Foo<E, F extends E, G extends F, H extends Number & Comparable<Comparable<Comparable<H>>> & java.util.List<? super G>>
	{
		abstract H bar( );
		
		abstract java.util.List<? super G>[ ] foo(java.util.List<? super Comparable<G>> arg);
		
		abstract <T> T[ ][ ] baz( );
		
		class Inner
		{
			H method( )
			{
				return null;
			}
		}
	}
	
	static class Foo<E>
	{
		class Bar<F extends E>
		{
			
		}
	}
	
	static abstract class Baz
	{
		abstract Foo<Number>.Bar<Integer> doStuff( );
	}
	
	public static void main(String[ ] args) throws Exception
	{
		System.out.println("" instanceof CharSequence);
		Type type = Handler.Foo.class.getDeclaredMethod("baz").getGenericReturnType( );
		System.out.println(((GenericArrayType) ((GenericArrayType) type).getGenericComponentType( )).getGenericComponentType( ));
		//System.out.println(java.util.Arrays.toString(((TypeVariable<?>)((TypeVariable<?>)(((TypeVariable<?>)((ParameterizedType)((TypeVariable<?>)type).getBounds( )[2]).getActualTypeArguments( )[0])).getBounds( )[0]).getBounds( )[0]).getBounds( )));
	}*/
}