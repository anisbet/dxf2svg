
/****************************************************************************
**
**	FileName:	DXFFilter.java
**
**	Purpose:	This is extends the abstract class FileFilter and defines
**				its accept() and getDescription() methods.
**
**	Date:		April 25, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java Runtime V1.3.1
**
**	Version:	1.00 - April 25, 2002
**				1.01 - June 13, 2002 adapted to handle PDF files.
**				1.02 - August 21, 2002 adapted to handle DXF files.
**				1.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;	// for file information

// This FileFilter is specified explicetly because there is a FileFilter
// in java.io that conflicts.
/**
*	Filters for DXF files; <em>*.dxf</em>.
*
*	@author		Andrew Nisbet
*	@version	1.02
*/
public class DXFFilter extends javax.swing.filechooser.FileFilter
{

	/** Returns whether a file has passes through the filter.
	*
	*	@param file the file to be tested for acceptance.
	*	@return boolean <code><b>true</b></code> if the file matches the
	*	filtering requirements and <code><b>false</b></code> if not.
	*/
	public boolean accept(File file)
	{
		String name = file.getName();
		final String extT = ".dxf";
		int startPos = name.length() -4;
		String nameExt = new String();
		try
		{
			nameExt = name.substring(startPos);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			;
		}

		if (file.isDirectory())
			return true;
		else if (nameExt.equalsIgnoreCase(extT))
			return true;

		return false;
	}

	/**
	*	Supplies a discription to the all acceptable files to a drop down
	*	combo box.
	*	@return &quot;DXF Documents (*.dxf)&quot;
	*/
	public String getDescription()
	{
		return "DXF Documents (*.dxf)";
	}
} // end of class