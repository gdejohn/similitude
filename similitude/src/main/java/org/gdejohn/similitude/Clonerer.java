package org.gdejohn.similitude;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("javadoc")
public class Clonerer
{
	public enum Cloners implements Cloning
	{
		CLONEABLE
		{
			@Override
			public <T> T toClone(final T ORIGINAL)
			{
				try
				{
					@SuppressWarnings("unchecked")
					final Class<? extends T> TYPE = (Class<? extends T>)ORIGINAL.getClass( );
					
					return TYPE.cast(TYPE.getDeclaredMethod("clone").invoke(ORIGINAL));
				}
				catch (final Exception CAUSE)
				{
					throw new RuntimeException("Failed.", CAUSE);
				}
			}
		},
		
		SERIALIZABLE
		{
			@Override
			public <T> T toClone(final T ORIGINAL)
			{
				try
				{
					@SuppressWarnings("unchecked")
					final Class<? extends T> TYPE =
					(
						(Class<? extends T>)ORIGINAL.getClass( )
					);
					
					final ByteArrayOutputStream OUT =
					(
						new ByteArrayOutputStream( )
					);
					
					new ObjectOutputStream(OUT).writeObject(ORIGINAL);
					
					final ObjectInputStream IN =
					(
						new ObjectInputStream
						(
							new ByteArrayInputStream(OUT.toByteArray( ))
						)
					);
					
					return TYPE.cast(IN.readObject( ));
				}
				catch (final Exception CAUSE)
				{
					throw new RuntimeException("Failed.", CAUSE);
				}
			}
		}
	}
	
	private final boolean CHECK_IMMUTABLE;
	
	private final boolean CHECK_CLONEABLE;
	
	private final boolean CHECK_SERIALIZABLE;
	
	private final Map<TypeToken2<?>, Cloning.ForType<?>> TYPE_CLONERS;
	
	private final Set<TypeToken2<?>> IMMUTABLE;
	
	private final Set<TypeToken2<? extends Cloneable>> CLONEABLE;
	
	private final Set<TypeToken2<? extends Serializable>> SERIALIZABLE;
	
	public class ReflectionCloner implements Cloning
	{
		@Override
		public <T> T toClone(final T ORIGINAL)
		{
			final TypeToken2<T> TYPE = new TypeToken2<T>(ORIGINAL.getClass( ));
			
			if (TYPE_CLONERS.containsKey(TYPE))
			{
				@SuppressWarnings("unchecked")
				final Cloning.ForType<T> TYPE_CLONER = (Cloning.ForType<T>)TYPE_CLONERS.get(TYPE);
				
				return TYPE_CLONER.toClone(ORIGINAL);
			}
			else if (IMMUTABLE.contains(TYPE))
			{
				return ORIGINAL;
			}
			else if (CHECK_IMMUTABLE && TYPE.isImmutable( ))
			{
				IMMUTABLE.add(TYPE);
				
				return ORIGINAL;
			}
			else if (CLONEABLE.contains(TYPE))
			{
				return Cloners.CLONEABLE.toClone(ORIGINAL);
			}
			else if (CHECK_CLONEABLE && TYPE instanceof Cloneable)
			{
				CLONEABLE.add(new TypeToken2<Cloneable>(TYPE));
				
				return Cloners.CLONEABLE.toClone(ORIGINAL);
			}
			else if (SERIALIZABLE.contains(TYPE))
			{
				return Cloners.SERIALIZABLE.toClone(ORIGINAL);
			}
			else if (CHECK_SERIALIZABLE && TYPE instanceof Serializable)
			{
				SERIALIZABLE.add(new TypeToken2<Serializable>(TYPE));
				
				return Cloners.SERIALIZABLE.toClone(ORIGINAL);
			}
			else
			{
				return null;
			}
		}
	}
	
	private final Cloning DEFAULT_STRATEGY;
	
	private Clonerer
	( // parameters
		final boolean CHECK_IMMUTABLE,
		final boolean CHECK_CLONEABLE,
		final boolean CHECK_SERIALIZABLE,
		final Map<TypeToken2<?>, Cloning.ForType<?>> TYPE_CLONERS,
		final Set<TypeToken2<?>> IMMUTABLE,
		final Set<TypeToken2<? extends Cloneable>> CLONEABLE,
		final Set<TypeToken2<? extends Serializable>> SERIALIZABLE,
		final Cloning DEFAULT_STRATEGY
	)
	{ // body
		this.CHECK_IMMUTABLE = CHECK_IMMUTABLE;
		
		this.CHECK_CLONEABLE = CHECK_CLONEABLE;
		
		this.CHECK_SERIALIZABLE = CHECK_SERIALIZABLE;
		
		this.TYPE_CLONERS =
		(
			new LinkedHashMap<TypeToken2<?>, Cloning.ForType<?>>
			(
				TYPE_CLONERS
			)
		);
		
		this.IMMUTABLE =
		(
			new LinkedHashSet<TypeToken2<?>>
			(
				IMMUTABLE
			)
		);
		
		this.CLONEABLE =
		(
			new LinkedHashSet<TypeToken2<? extends Cloneable>>
			(
				CLONEABLE
			)
		);
		
		this.SERIALIZABLE =
		(
			new LinkedHashSet<TypeToken2<? extends Serializable>>
			(
				SERIALIZABLE
			)
		);
		
		this.DEFAULT_STRATEGY =
		(
			DEFAULT_STRATEGY == null ? new ReflectionCloner( ) : DEFAULT_STRATEGY
		);
	}
	
	public Clonerer( )
	{
		this
		(
			false,
			false,
			false,
			Collections.<TypeToken2<?>, Cloning.ForType<?>>emptyMap( ),
			Collections.<TypeToken2<?>>emptySet( ),
			Collections.<TypeToken2<? extends Cloneable>>emptySet( ),
			Collections.<TypeToken2<? extends Serializable>>emptySet( ),
			null
		);
	}
	
	public Clonerer checkImmutable( )
	{
		return
		(
			new Clonerer
			(
				true,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer checkCloneable( )
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				true,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer checkSerializable( )
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				true,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer checkAll( )
	{
		return
		(
			new Clonerer
			(
				true,
				true,
				true,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer withTypeCloners(final Map<TypeToken2<?>, Cloning.ForType<?>> TYPE_CLONERS)
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer withImmutableTypes(final Set<TypeToken2<?>> IMMUTABLE)
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer withCloneableTypes(final Set<TypeToken2<? extends Cloneable>> CLONEABLE)
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer withSerializableTypes(final Set<TypeToken2<? extends Serializable>> SERIALIZABLE)
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public Clonerer withDefaultStrategy(final Cloning DEFAULT_STRATEGY)
	{
		return
		(
			new Clonerer
			(
				CHECK_IMMUTABLE,
				CHECK_CLONEABLE,
				CHECK_SERIALIZABLE,
				TYPE_CLONERS,
				IMMUTABLE,
				CLONEABLE, 
				SERIALIZABLE,
				DEFAULT_STRATEGY
			)
		);
	}
	
	public boolean registerImmutable(final Class<?> TYPE)
	{
		return IMMUTABLE.add(new TypeToken2<Object>(TYPE));
	}
	
	public <T> boolean registerTypeCloner(final Class<T> TYPE, final Cloning.ForType<T> TYPE_CLONER)
	{
		return TYPE_CLONERS.put(new TypeToken2<T>(TYPE), TYPE_CLONER) == null;
	}
	
	public boolean registerCloneable(final Class<? extends Cloneable> TYPE)
	{
		return CLONEABLE.add(new TypeToken2<Cloneable>(TYPE));
	}
	
	public boolean registerSerializable(final Class<? extends Serializable> TYPE)
	{
		return SERIALIZABLE.add(new TypeToken2<Serializable>(TYPE));
	}
	
	public <T> T toClone(final T ORIGINAL)
	{
		return DEFAULT_STRATEGY.toClone(ORIGINAL);
	}
	
	public static void main(String[ ] args) throws Exception
	{
		StringBuilder original = new StringBuilder("xyzzy");
		StringBuilder clone = Cloners.SERIALIZABLE.toClone(original);

		System.out.printf("original: %s (%d)%nclone: %s (%d)%noriginal == clone: %b%n%n", original, original.capacity( ), clone, clone.capacity( ), original == clone);
		
		clone.reverse( );
		clone.trimToSize( );
		
		System.out.printf("original: %s (%d)%nclone: %s (%d)%n%n", original, original.capacity( ), clone, clone.capacity( ));
		
		java.util.ArrayList<String> list = new java.util.ArrayList<String>(java.util.Arrays.asList("foo", "bar", "baz"));
		java.util.ArrayList<String> newList = Cloners.CLONEABLE.toClone(list);

		System.out.printf("original: %s (%d)%nclone: %s (%d)%noriginal == clone: %b%n%n", list, list.size( ), newList, newList.size( ), list == newList);
		
		newList.clear( );

		System.out.printf("original: %s (%d)%nclone: %s (%d)%n%n", list, list.size( ), newList, newList.size( ));
	}
}