package org.gdejohn.similitude;

/**
 * Instantiate arbitrary classes.
 * 
 * @author Griffin DeJohn
 */
public class Builder
{
	/**
	 * Creates an instance of the given class.
	 * 
	 * @param CLASS The class to instantiate.
	 * 
	 * @return An instance of {@code CLASS}.
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T> T instantiate(final Class<T> CLASS)
	{
		try
		{
			return CLASS.newInstance( );
		}
		catch (InstantiationException e)
		{
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
}