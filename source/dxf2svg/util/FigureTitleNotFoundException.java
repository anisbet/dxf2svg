
/****************************************************************************
**
**	FileName:	FigureTitleNotFoundException.java
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

/** This exception gets thrown if the search for Figure titles produces no results. This will
*	occur if the illustration being converted contains no titles.
*
*	@version 0.01 - November 14, 2003
*	@author Andrew Nisbet
*/
public class FigureTitleNotFoundException extends RuntimeException
{
	private static String figDesc = 
		"Figure titles can take the following forms ^(Figure 1 | Figure 1a | Figure 1-1 | "+
		"Figure 1-2-3 | Figure 1-2-3a | Figure 1-2-3 (Sheet 1 of 2). On AG1Ps this occurs at "+
		"the beginning of the title string and on english foldouts, at the end of the title string. "+
		"To find out why the search failed check for the following: \nMake sure the titles exist.\n"+
		"Make sure the sheet numbers don't have characters (no '1a of 2').";
	public FigureTitleNotFoundException(String fileName)
	{
		System.err.println("couldn't find illustration titles. I have searched layer 'english', 'french' and 'T'.");
		System.err.println(figDesc);
		System.err.println("Unable to create DB entry for figure:" + fileName + "'.");
	}
	
	public FigureTitleNotFoundException(String fileName, String language)
	{
		System.err.println(language+" title is required but found none in file: '" + fileName + "'.");
		System.err.println(figDesc);
	}
}