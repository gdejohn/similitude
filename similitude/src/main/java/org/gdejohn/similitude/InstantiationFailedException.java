package org.gdejohn.similitude;

@SuppressWarnings("serial")
public class InstantiationFailedException extends RuntimeException
{
	public InstantiationFailedException( )
	{
		super( );
	}
	
	public InstantiationFailedException(String message)
	{
		super(message);
	}
	
	public InstantiationFailedException(Throwable cause)
	{
		super(cause);
	}
	
	public InstantiationFailedException(String message, Throwable cause)
	{
		super(message, cause);
	}
}