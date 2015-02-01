
/****************************************************************************
**
**	FileName:	GraphicLinksDatabaseReader.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Reads existing GraphicLinksDatabase and closes with an
**				error message if one cannot be found.
**
**	Date:		0.01 - October 5, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 5, 2004
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;		// For Hashtable handling.
import dxf2svg.*;		// DxfPreprocessor

/**	This class is called by the DxfPreprocessor and opens, reads the Graphic Links Database
*	then retains the database for query until the application stops. Since the
*	database is not modified - only referenced during conversion, the database is not 
*	reserialized. This means that numerous application could read the database at the 
*	same time.
*
*	@version	0.01 - October 5, 2004
*	@author		Andrew Nisbet
*/

public class GraphicLinksDatabaseReader
{
	private GraphicLinkDatabase gldb;
	
	/** The location argument is the directory of the graphic link database WITHOUT
	*	the name of the database. The name of the database is standardized to 
	*	GraphicLinkDatabase.ser and cannot be changed.
	*/
	public GraphicLinksDatabaseReader(String location)
	{
		gldb = new GraphicLinkDatabase(GraphicLinkDatabase.OPEN, location);
	}
	
	/** Returns the graphic linking database to the caller.
	*	@return null if the database could not be found by the path passed as a 
	*	constructor argument or the figureid of the spotcall is not in the database.
	*/
	public String getFigureIdForSpotCall(String spotCall)
	{
		return gldb.getFigureId(spotCall);
	}
	
	/** Returns a String representation of this object, which in this case is the 
	*	contents of the database. Null will be returned if the database could not 
	*	be found.
	*/
	public String toString()
	{
		return gldb.toString();
	}
}