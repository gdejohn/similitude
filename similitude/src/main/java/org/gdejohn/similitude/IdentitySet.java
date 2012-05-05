package org.gdejohn.similitude;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

class IdentitySet extends AbstractSet<Object>
{
	private final IdentityHashMap<Object, Void> BACKING_MAP;
	
	IdentitySet(final IdentityHashMap<Object, Void> BACKING_MAP)
	{
		this.BACKING_MAP = BACKING_MAP;
	}
	
	IdentitySet( )
	{
		this(new IdentityHashMap<Object, Void>( ));
	}
	
	@Override
	public boolean add(final Object OBJECT)
	{
		if (BACKING_MAP.containsKey(OBJECT))
		{
			return false;
		}
		else
		{
			BACKING_MAP.put(OBJECT, null);
			
			return true;
		}
	}
	
	@Override
	public Iterator<Object> iterator( )
	{
		return BACKING_MAP.keySet( ).iterator( );
	}
	
	@Override
	public int size( )
	{
		return BACKING_MAP.size( );
	}
	
	public static void main(String[ ] args)
	{
		IdentitySet set = new IdentitySet( );
		Object o = "xyzzy";
		System.out.println(set.size( ));
		System.out.println(set.isEmpty( ));
		System.out.println(set);
		System.out.println(set.add(o));
		System.out.println(set.size( ));
		System.out.println(set.isEmpty( ));
		System.out.println(set.add(o));
		System.out.println(set.size( ));
		System.out.println(set.isEmpty( ));
		System.out.println(set);
	}
}