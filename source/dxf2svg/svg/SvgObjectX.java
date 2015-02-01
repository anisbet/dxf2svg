
/****************************************************************************
**
**	FileName:	SvgObjectX.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This object could be any SvgObject you wish. If you want to
**				manually include a bounding box around a hatchpattern,
**				if you need to include <defs> beyond what will be included
**				with a hatch pattern or symbol then this is the object for
**				you. This is basically a wrapper for an SvgObject so an
**				arbitrary object of your creation can be included in
**				SvgCollections. This also allows the application to create
**				data that doesn't exist in the Dxf like <defs> for file name
**				or what-have-you. I use this object to create a bounding box
**				for a hatchpattern for DEBUGing purposes, but you could build
**				anything. You will need to pass a object type, and optional
**				attribute string and optional data string. For now the class
**				will be final for security reasons but that could change to
**				allow inheritance.
**
**	Date:		February 4, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - February 4, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Iterator;	// for animation
import dxf2svg.DxfConverter;
import dxf2svg.animation.*;
/**
*	This object could be any SvgObject you wish. If you want to
*	manually include a bounding box around a hatchpattern,
*	if you need to include &lt;defs&gt; beyond what will be included
*	with a hatch pattern or symbol then this is the object for
*	you. This is basically a wrapper for an SvgObject so an
*	arbitrary object of your creation can be included in
*	SvgCollections. This also allows the application to create
*	data that doesn't exist in the Dxf like &lt;defs&gt; for file name
*	or what-have-you. I use this object to create a bounding box
*	for a hatchpattern for DEBUGing purposes, but you could build
*	anything. You will need to pass a object type, and optional
*	attribute string and optional data string. For now the class
*	will be final for security reasons but that could change to
*	allow inheritance.
*	<P>
*	It should be noted that you must pass all attributes and {@link dxf2svg.util.Attribute}
*	objects use the {@link dxf2svg.svg.SvgElement#addAttribute} and 
*	{@link dxf2svg.svg.SvgElement#getAdditionalAttributes}
*
*	@version	0.01 - February 4, 2003
*	@author		Andrew Nisbet
*/

public final class SvgObjectX extends SvgObject
{
	//private String attributes;				// attribs for this object
	private boolean isEmptyElement = true;	// Is the object an empty element?

	// Constructor
	public SvgObjectX(DxfConverter dxfc)
	{	super(dxfc);	}


	////////////////////////// Methods ////////////////////////////
	/** Sets this objects attributes. This class is very dumb - it
	*	outputs the attribute argument verbatum.You do not need to
	*	include a leading space in the attribute string but all
	*	attributes must	contain all necessary quotes and escaped
	*	characters.
	*/
	//public void setAttributes(String attributes)
	//{	this.attributes = attributes;	}

	/** This method sets the element's content. If the IS_EMPTY is <B>true</B> then
	*	calling this method will set IS_EMPTY to false.
	*/
	public void setContentString(String content)
	{
		this.content = content;
		if (isEmptyElement == true)
			isEmptyElement = false;
	}

	/**	This method sets the element to either an empty or content
	*	containing element. If this element is empty and you supply content
	*	the IS_EMPTY flag is set to false.
	*/
	public void setEmptyElement(boolean isEmptyElement)
	{	this.isEmptyElement = isEmptyElement;	}

	/**	This method returns the object's state of emptyness (read
	*	is this element an empty element?)
	*/
	public boolean isEmptyElement()
	{	return isEmptyElement;	}

	/**	Returns the object as a String. Does not do any fancy processing
	*	the assumtion is that the developer has supplied all the data
	*	necessary to make this object do what you want.
	*/
	public String toString()
	{
		StringBuffer out = new StringBuffer();

		out.append("<" + getType());
		//if (attributes != null)
		//	out.append(" "+attributes);
		
		out.append(getAdditionalAttributes());
			
		if (vAnimationSet != null)
			isEmptyElement = false;
		if(isEmptyElement == true)
		{
			out.append("/>");
			return out.toString();
		}
		out.append(content);
		// Now add animation if any.
		if (vAnimationSet != null)
		{
			out.append(">");
			Iterator itor = vAnimationSet.iterator();
			while (itor.hasNext())
				out.append("\n\t"+itor.next());
		}	
		out.append("</"+getType()+">");

		return out.toString();
	}

	public Object clone()
	{
		SvgObjectX ox 		= (SvgObjectX)super.clone();

		//ox.attributes		= this.attributes;
		ox.isEmptyElement	= this.isEmptyElement;

		return ox;
	}
}	// SvgObjectX	