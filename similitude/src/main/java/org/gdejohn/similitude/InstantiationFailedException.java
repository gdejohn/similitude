package org.gdejohn.similitude;

@SuppressWarnings({"serial", "javadoc"})
public class InstantiationFailedException extends RuntimeException
{
	public InstantiationFailedException( )
	{
		super( );
	}
	
	public InstantiationFailedException(final String FORMAT, final Object... ARGUMENTS)
	{
		super(String.format(FORMAT, ARGUMENTS));
	}
	
	public InstantiationFailedException(final Throwable CAUSE)
	{
		super(CAUSE);
	}
	
	public InstantiationFailedException(final Throwable CAUSE, final String FORMAT, final Object... ARGUMENTS)
	{
		super(String.format(FORMAT, ARGUMENTS), CAUSE);
	}
}