
/****************************************************************************
**
**	FileName:	SvgReference.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Maps the data from a DXF into equivelant SvgObjects
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 13, 2002
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;
import dxf2svg.util.Attribute;
import dxf2svg.animation.*;

/**
*	This abstract class encapsulates an SvgReference of which
*	there can be three types: SvgEntityReference, SvgUseXLink and SvgImage.
*
*	@version	0.01 - September 13, 2002
*	@author		Andrew Nisbet
*/
public abstract class SvgReference extends SvgElement
{
	protected String ReferenceURL;
	protected Point Anchor;

	/**
	*	Public constructor that takes a reference to the conversion
	*	context and the type of object that we are.
	*
	*	@param dxfc the conversion context
	*/
	public SvgReference(DxfConverter dxfc)
	{
		super();
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: "+
				this.getClass().getName());
		DxfConverterRef = dxfc;
		// This will turn off the class attribute in favour
		// of inheriting the class from the 'layer' or SvgCollection object.
		// It is controllable from SvgElement so we could control
		// an object's class readily.
		// Default to true for outputting entities that are grouped by
		// classes rather than by the <g> tags.
		setIncludeClassAttribute(true);
		// Set the anchor point
		Anchor = new Point(DxfConverterRef);
	}

	/** Sets the anchor's x value
	*/
	public void setX(double x)
	{	Anchor.setX(x);	}

	/** Sets the anchor's y value
	*/
	public void setY(double y)
	{	Anchor.setY(y);	}

	///** Sets the reference object type. For example, for SvgUseXLink, it would
	//*	be &lt;use&gt; and for SvgMPath it would be &lt;mpath&gt;.
	//*	@deprecated April 8, 2004 in favour of super classes getType()
	//*/
	//public final void setType(String type)
	//{	setType(type);	}

	/** This method sets the reference's URL
	*	@throws NullSvgUrlReferenceException
	*	@param url to link to.
	*/
	public void setReferenceURL(String url)
	{
		try
		{
			if (url.equals(""))
			{
				NullUrlSvgReferenceException e = new NullUrlSvgReferenceException();
				throw e;
			}
			else
			{
				ReferenceURL = url;
			}
		}
		catch (NullUrlSvgReferenceException e)
		{
			System.err.println("SvgReference error: null URL passed to SvgReference object.");
			DxfPreprocessor.logEvent("SvgReference error","null URL passed to SvgReference object.");
			return;
		}
	}

	/** This method returns the reference's URL
	*
	*	@return String preformatted URL link in the following format:
	*	<code>xlink:href=&quot;reference&quot;.
	*/
	protected Attribute getReferenceURL()
	{
		return new Attribute("xlink:href", ReferenceURL);
	}
	

	/** This method is inherited from the super class, but if envoked will produce
	*	an error message. This is because, to date, no SvgReference object supports
	*	animation. This behaviour could easily be changed in the future.
	*/
	public void addAnimation(SvgAnimator sa)
	{
		System.err.println("Warning SvgReference.addAnimation(): "+
			this.getClass().getName()+" does not support animation.");
	}

	/** This returns the String value of these types of objects.
	*/
	public String toString()
	{
		StringBuffer OutString = new StringBuffer();
		// Open tag
		OutString.append("<");
		// Add the object's type
		OutString.append(getType());
		// attach the ID if required.
		if (DEBUG)
			OutString.append(getObjID());
		// add the class attribute if required; default: don't.
		if (getIncludeClassAttribute() == true)
			OutString.append(getClassAttribute());
		// Attach the URL.
		addAttribute(getReferenceURL());
		// Add any transformation information,
		// probably the minimum is the x and y values.
		// again this is new to Java 1.4;
		// this function returns a StringBuffer.
		OutString.append(getAttributes());
		// close the tag.
		OutString.append("/>");
		// and return.
		return OutString.toString();
	}

}