
/****************************************************************************
**
**	FileName:	SvgAttdef.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgAttdef class description
**
**	Date:		April 29, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - April 29, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfPreprocessor;

// This class currently has no purpose in the conversion process but remains for 
// possible future considerations.
/**
*	This immutable class encapsulates a DXF attribute definition, known as an ATTDEF, into
*	an SVG format.
*	<P>
*	An attdef is a string value that is usually associated with a 
*	block. When you define, or redefine a block you can include any number of attributes. 
*	When the block is next referenced into the drawing, if the attribute definition stipulates,
*	you will be prompted for the attribute's value which will then become part of the block.
*	For each attribute, there must be a attribute definition.
*	<P>
*	If an attdef is not associated with any block it is refered to as a stand-alone
*	attribute definition. When the drawing is next referenced into another drawing
*	in AutoCAD, AutoCAD will prompt you to fill in the attributes in a modal dialog
*	box or on the command line. This class encapsulates the result of that process.
*	<P>
*	Attribute definitions really only contain two critical pieces of information. The
*	tag (or variable) name, and the visibility of the attribute. The tag is not really 
*	a variable because two or more tags with the same name can exist in one block. This
*	should be a rare event, but it contravenes the SVG namespace, so please make sure 
*	there are no duplicate tag names within any block.
*	<P>
*	Of course AutoCAD makes
*	more extensive use of this object, but its job has very little to do with SVG. ATTDEF,
*	in AutoCAD, is remarkably similar to TableStyle while its counterpart, ATTRIB is very
*	similar to an SvgText object.
*
*	@version	0.01 - April 29, 2003
*	@author		Andrew Nisbet
*/
public class SvgAttdef 
{
	//////////////////////////////////////////////////
	//				Instance Data
	//////////////////////////////////////////////////
	private String tag;				// Name of this attribute definition's tag.
	private boolean isVisible;		// Visibility flag.
	
	//////////////////////////////////////////////////
	//				Constructors
	//////////////////////////////////////////////////	
	public SvgAttdef( )
	{
		this.tag 		= "";
		this.isVisible 	= true;
	}
	
	
	public SvgAttdef(String tag, boolean isVisible)
	{	
		this.tag 		= DxfPreprocessor.convertToSvgCss(tag);
		this.isVisible 	= isVisible;
	}

	//////////////////////////////////////////////////
	//				Methods
	//////////////////////////////////////////////////
	
	/** Sets the tag name of the object.
	*/
	public void setTag(String tagName)
	{
		this.tag 		= DxfPreprocessor.convertToSvgCss( tagName );
	}
	
	/** Sets the visibility of the attribute definition.
	*/
	public void setAttributeVisibility( boolean isVisible )
	{
		this.isVisible 	= isVisible;
	}
	
	/** Returns the tag name of the attribute definition.
	*/
	public String getTag()
	{	return tag;		}

	/**	Returns the visibility of this definition.
	*/
	public boolean getAttributeVisibility()
	{	return isVisible;	}
	
	
	/**	This method compares two attribute definition and determines whether they
	*	are the same. This is necessary because, unlike text styles,
	*	attribute definitions can be repeated several times within the same block.
	*	@return true if the attribute definition matches the argument definition,
	*	false otherwise.
	*/
	public boolean equals(Object o)
	{
		// Compare the basics
		// ... is the arg null?
		if (this == o)
			return true;
		// ... is the arg and this object one-and-the-same?
		if (!(o instanceof SvgAttdef))
			return false;
		
		// Now for the details
		SvgAttdef argDef = (SvgAttdef)o;
		return (this.tag.equals(argDef.tag) &&
			this.isVisible == argDef.isVisible);
	}
	
	/**	Returns a String representation of this object.
	*/
	public String toString()
	{
		return "Tag: "+tag+"\nAttribute is visible: "+String.valueOf(isVisible);
	}
}