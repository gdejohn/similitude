package org.gdejohn.similitude;

@SuppressWarnings("serial")
public class CloningFailedException extends RuntimeException
{
	public CloningFailedException( )
	{
		super( );
	}
	
	public CloningFailedException(String message)
	{
		super(message);
	}
	
	public CloningFailedException(Throwable cause)
	{
		super(cause);
	}
	
	public CloningFailedException(String message, Throwable cause)
	{
		super(message, cause);
	}
}