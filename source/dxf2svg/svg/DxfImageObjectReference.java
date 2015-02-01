
/****************************************************************************
**
**	FileName:	DxfImageObjectReference.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Hold URI information about images that may be
**				imbedded in the DXF file.
**
**	Date:		October 3, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 3, 2002
**				0.02 - February 13, 2003 Implemented cloneable interface. -RECINDED-
**				0.03 - February 24, 2004 Made the constructor take a conversion 
**				context to fix a bug where raster images of files in subdirs
**				of the conversion don't get embedded because this object is looking
**				in the current directory (where dxf2svg started) and not the working
**				directory (where the dxf files are located).
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.io.File;
import dxf2svg.DxfConverter;

/**	This class Hold URI information about images that may be
*	imbedded in the DXF file.
*/
public class DxfImageObjectReference
{
	private String ReferenceURL;
	private String ReferenceHandle;	// A hex string hard reference to
									// imagedef object.
	private DxfConverter DxfConverterRef; // reference to the conversion context.

	// Constructor
	/** Constructor takes a reference to the conversion context to fix a bug
	*	that images with no path which are therefore searched for in the current
	*	directory, wouldn't be found because the current directory is the place
	*	where the dxf2svg conversion is started, not the working directory where
	*	the dxf files are. The result is that if you don't start the conversion 
	*	in the working directory, dxf2svg cannot find any raster images to embed.
	*	@since 0.03 - February 24, 2004
	*/	
	public DxfImageObjectReference(DxfConverter dxfc)
	{	
		DxfConverterRef = dxfc;
	}

	/** Sets the image's URL.
	*/
	public void setUrl(String url)
	{
		File test = new File(url);
		// can we find the file to start off with.
		if (test.isFile())
		{
			ReferenceURL = url;
			return;
		}		
		// since 0.03 - February 24, 2004
		// Here we will get the dxf's file name and search in its directory
		// for the raster image.
		File fWhere = new File(DxfConverterRef.getFilePath());
		String where = fWhere.getParent();
		// Image check
		test = new File(where, url);
		// can we find the file to start off with.
		if (test.isFile())
		{
			ReferenceURL = test.getPath();
			return;
		}

		// OK, no joy there, let's see if there is an images directory.
		String testName = test.getName();
		if (new File("images",testName).isFile())
		{
			ReferenceURL = "images/"+testName;
			return;
		}
		
		// OK, no joy there, let's assume the file is in the current dir
		ReferenceURL = testName;
	}

	/** Sets the hard reference hex string value.
	*/
	public void setHardReference(String href)
	{	ReferenceHandle = href;	}

	/** Returns a String containing the image's URL.
	*/
	public String getUrl()
	{	return ReferenceURL;	}

	/**	Returns the image's hard reference.
	*/
	public String getHardReference()
	{	return ReferenceHandle;	}

	/** Returns information about the reference noteably, the reference
	*	and the URL.
	*/
	public String toString()
	{
		return this.getClass() + ": [ " + ReferenceHandle +
			": " + ReferenceURL + " ]";
	}

}	// end of class