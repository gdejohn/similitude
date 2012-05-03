package org.gdejohn.similitude;

@SuppressWarnings({"javadoc", "serial"})
public class CircularSuperTypeException extends RuntimeException
{
	private final TypeToken<?> CALLER;
	
	public CircularSuperTypeException(final TypeToken<?> CALLER)
	{
		this.CALLER = CALLER;
	}
	
	public TypeToken<?> getCaller( )
	{
		return CALLER;
	}
}