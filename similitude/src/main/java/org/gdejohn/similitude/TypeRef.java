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
	}*/
}