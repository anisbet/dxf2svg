
/****************************************************************************
**
**	FileName:	NullUrlSvgReferenceException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Throws a exception if an SvgReference object gets passed
**				if an empty or null URL.
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 13, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

/**
*	Gets thrown if an SvgReference object gets passed an empty or null URL.
*
*	@author		Andrew Nisbet
*	@version	0.01 - September 13, 2002
*/
public class NullUrlSvgReferenceException extends Exception
{
	public NullUrlSvgReferenceException()
	{
		System.err.println("NullUrlSvgReferenceException: the URL passed to SvgReference is empty.");
		printStackTrace();
	}

}