
/****************************************************************************
**
**	FileName:	NDIDNotFoundException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Exception if a file was searched for the file's NDID but none was found.
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

/** This exception gets thrown if the search for an NDID produces no results. This will
*	occur if the illustration being converted contains no NDID. Typically if the
*	illustration should have an <B>N</B>ational <B>D</B>efence <B>I</B>dentification 
*	<B>D</B>ocumentation (NDID) number somewhere outside the limits frame. This 
*	will still make it through the conversion to the final SVG, but as a &lt;desc&gt; tag.
*
*	The NDID is critical for conversions that require wrapper files that link families 
*	of illustrations or require the output of the converted files XML database 
*	(see {@link dxf2svg.FigureSheetDatabase} for more details).
*
*	@version 0.01 - November 14, 2003
*	@author Andrew Nisbet
*/
public class NDIDNotFoundException extends RuntimeException
{
	public NDIDNotFoundException(String fileName)
	{
		System.err.println("The National Defence Identification Documentation (NDID) number is " +
		"required for this type of conversion, but it could not be found in file: '"+fileName+"'."+
		"\nThis can occur if the NDID is removed from the source DXF file. The NDID is used as "+
		"a namespace for lookups of certain groups of figures within a specific book.");
	}
}