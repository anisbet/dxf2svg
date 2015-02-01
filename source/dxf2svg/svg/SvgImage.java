
/****************************************************************************
**
**	FileName:	SvgImage.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates an IMAGE object from Dxf and translates it into
**				an xml link reference.
**
**	Date:		October 3, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 3, 2002
**				0.02 - September 17, 2004 changed setReferenceHandle() method.
**				The method used to request the entire list of images from 
**				DxfConverter and process them each in turn. Now the object passes
**				the image's unique handle to DxfConverter which searches the
**				listed images for the correct image and returns it. This not
** 				only simplifies the code but reduces redundant image processing,
**				allows for multipule images within the DXF (of different types too),
**				and allows for and fixes a bug where if there was more than
**				one image the last image would be processed by each of the SvgImage
**				objects.
**				0.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				0.04 - August 23, 2005 Added error reporting for AutoCAD rotated
**				PNG files which, because of a bug, don't report the correct 
**				pixel width and height, which calculates to '0.0' and then the
**				image will not be displayed.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.svg;

import java.util.*;
import dxf2svg.DxfConverter;
import dxf2svg.DxfPreprocessor;
import dxf2svg.util.*;
/**
*	This class encapsulates images that is imbedded in a DXF.
*	The following are valid image formats for DXF:
*<UL>
*	<LI> JPG
*	<LI> PNG
*</UL>
*	<P>
*	If the image can be found from the URL at conversion time, it is embedded
*	automatically. If it cannot be found, the URL is inserted instead because
*	it is assumed that the image was not available at the time of conversion
*	and will be added later. It may also be an image that changes frequently.
*
*	@version	0.04 - August 23, 2005
*	@author		Andrew Nisbet
*/
public final class SvgImage extends SvgReference
{
	private double ImageWidth;		// Width of the image.
	private double ImageHeight;		// Height of the image.
	private double pixelWidth;		// Size of a pixel horizontally.
	private double pixelHeight;		// Size of a pixel vertically
	private String HardReference;	// Hex string handle to IMAGEDEF
	private DxfImageObjectReference DIORef;
	private SvgUtil	svgUtility;		// conversion routines.


	/**
	*	Set the conversion context and type of object.
	*	@param dxfc Dxf conversion context.
	*/
	public SvgImage(DxfConverter dxfc)
	{
		super(dxfc);
		setType("image");
		svgUtility = DxfConverterRef.getSvgUtil();
	}

	/**
	*	Sets the image's width.
	*	@param width as a double.
	*/
	public void setImageWidth(double width)
	{	ImageWidth = width;	}

	/**
	*	Sets the image's height.
	*	@param height as a double.
	*/
	public void setImageHeight(double height)
	{	ImageHeight = height;	}

	/**
	*	This method allow the setting of a pixel's width. This is
	*	critical for Autocad because DXF is a vector file format.
	*	@param width pixel width as a double.
	*/
	public void setPixelWidth(double width)
	{	pixelWidth	= width;	}

	/**
	*	This method allow the setting of a pixel's height. This is
	*	critical for Autocad because DXF is a vector file format.
	*	@param height pixel height as a double.
	*/
	public void setPixelHeight(double height)
	{	pixelHeight = height;}

	/**
	*	This method is called by the EntityProcessor and provides the dxf
	*	handle of the URL for this image. This is in turn, looked up in
	*	a vector of Dxf objects collected from the Dxf's OBJECT section.
	*	@param imageHandle reference collected from the entities section of
	*	the DXF.
	*/
	public void setReferenceHandle(String imageHandle)
	{
		HardReference = imageHandle;
	}
	
	

	/** Returns the placement description for a raster image.
	*	@return StringBuffer of &lt;image&gt; attributes.
	*/
	protected StringBuffer getAttributes()
	{
		double Width, Height;
		StringBuffer OutString = new StringBuffer();

		Width = svgUtility.trimDouble((ImageWidth * pixelWidth) * svgUtility.Units());
		Height = svgUtility.trimDouble((ImageHeight * pixelHeight) * svgUtility.Units());
		if ( Width == 0.0 || Height == 0.0 )
		{
			if ( DIORef != null )
			{
				System.out.println("SvgImage '"+DIORef.getUrl()+"' image width and/or height are 0.0 and will");
				DxfPreprocessor.logEvent("SvgImage","'"+DIORef.getUrl()+"' image width and/or height are 0.0");
			}
			else
			{
				System.out.println("SvgImage image width and/or height are 0.0 and will");
				DxfPreprocessor.logEvent("SvgImage","image width and/or height are 0.0");
			}
			System.out.println("not display. This can occur if the image was rotated within ");
			System.out.println("AutoCAD. To correct this problem detach the image, rotate ");
			System.out.println("the image in something other than AutoCAD and re-attach the image.");
		}
		// The insertion point of an image in Dxf is the bottom left
		// and the insertion point in Svg is the top left so adjust
		// for that now by subtracting the height of the image from
		// the Svg 'y' coord.
		Anchor.setYUU(Anchor.getY() - Height);
		OutString.append(" "+Anchor.toStringText());
		addAttribute(new Attribute("width",String.valueOf(Width)));
		addAttribute(new Attribute("height",String.valueOf(Height)));
		OutString.append(getAdditionalAttributes());

		return OutString;
	}
	
	/** This method returns the reference's URL in a normal reference object. However SvgImages
	*	need to also be able to embed their images as Base64 encoded data. This method makes
	*	that descision based on whether it can find the image; if it can it automatically
	*	embeds it, if it can't find the data then it will just use the URL like all other 
	*	classes derived from the SvgReference class.
	*
	*	@return String preformatted URL link in the following format:
	*	<code>xlink:href=&quot;reference&quot; or the image as base64 encoded data.
	*/
	protected Attribute getReferenceURL()
	{
		// We over ride this method from the base class to allow this class to either
		// embed the image if it can be found where the url states and if not just include
		// the url and assume that the image will be added to the url directory later.
		// The advantage is, if you embed you have less file management but if you reference
		// you can change the image with another and the svg file will automatically be 
		// graphically up-to-date. The further disadvantage is if the image is not exactly 
		// the same size as the original then it will scale and it will scale non-uniformly.
		// make a UUEncoder Object
		
		// We need to get ReferenceURL from DxfConverter's list of object references.
		DIORef = DxfConverterRef.getImageObjectReference( HardReference );
		// This prevents a NullPointerException because if the image handle number 
		// can't be found, the dxf if malformed and DxfConverter will return null.
		// NullPointerExceptions, in my experience, are the hardest to trace.
		if ( DIORef != null )
		{
			setReferenceURL( DIORef.getUrl() );
		}
		String url = DIORef.getUrl();
		StringBuffer encodedBuff = new StringBuffer();
		if (! Base64Generator.getData( Base64Generator.ENCODE, url, encodedBuff ) )
		{
			System.out.println("SvgImage Unable to find image file to embed '"+url+"'.");
			DxfPreprocessor.logEvent("SvgImage","Unable to find image file to embed '"+url+"'.");	
			return new Attribute("xlink:href", ReferenceURL);
		}
		else
		{
			return new Attribute("xlink:href",encodedBuff.toString());		
		}
	}
}	// end of SvgImage class.