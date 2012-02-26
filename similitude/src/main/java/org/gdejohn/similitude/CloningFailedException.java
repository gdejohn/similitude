package org.gdejohn.similitude;

@SuppressWarnings("serial")
public class CloningFailedException extends RuntimeException
{
	public CloningFailedException( )
	{
		super( );
	}
	
	public CloningFailedException(final String FORMAT, final Object... ARGUMENTS)
	{
		super(String.format(FORMAT, ARGUMENTS));
	}
	
	public CloningFailedException(final Throwable CAUSE)
	{
		super(CAUSE);
	}
	
	public CloningFailedException(final String FORMAT, final Throwable CAUSE, final Object... ARGUMENTS)
	{
		super(String.format(FORMAT, ARGUMENTS), CAUSE);
	}
}