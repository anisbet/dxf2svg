
/****************************************************************************
**
**	FileName:	FigureListLibraryNotFoundException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	his exception gets thrown if the figure list database could not be found.
**
**	Date:		0.01 - December 5, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 14, 2003
**
**	TODO:		
**
**
**
*****************************************************************************/

package dxf2svg.util;

/** This exception gets thrown if the figure list database could not be found.
*	The figure list library is a list of figures that have been converted for 
*	all books. It is used to output XML for IETM and for linking illustration
*	families in HTML wrappers.
*
*	@version 0.01 - December 5, 2003
*	@author Andrew Nisbet
*/
public class FigureListLibraryNotFoundException extends RuntimeException
{
	public FigureListLibraryNotFoundException(String path)
	{
		System.err.println("command to sync database includes an invalid path '" + 
			path + "'. There is either no database or a database could not be created here.");
	}
}