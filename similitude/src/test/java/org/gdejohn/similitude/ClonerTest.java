package org.gdejohn.similitude;

import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class ClonerTest
{
	@Test
	public void testNull( )
	{
		assertNull(new Cloner( ).toClone(null));
	}
}