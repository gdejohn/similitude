package org.gdejohn.similitude;

@SuppressWarnings("javadoc")
public interface Cloning
{
	<T> T toClone(T original);
	
	public interface ForType<E>
	{
		E toClone(E original);
	}
}