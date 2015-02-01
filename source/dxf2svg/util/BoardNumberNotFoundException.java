
/****************************************************************************
**
**	FileName:	BoardNumberNotFoundException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Exception if a file was searched for boardnos but none were found.
**
**	Date:		November 14, 2003
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

/** This exception gets thrown if the search for boardnos produces no results. This will
*	occur if the illustration being converted contains no boardnos.
*
*	@version 0.01 - November 14, 2003
*	@author Andrew Nisbet
*/
public class BoardNumberNotFoundException extends RuntimeException
{
	public BoardNumberNotFoundException(String fileName)
	{
		System.err.println("A search of file '" + fileName + "' was produced no strings matching "+
			"a 'boardno' string (i.e. [gG]Digit{5}[EFBefb][A-Z] ).");
	}
}