
/****************************************************************************
**
**	FileName:	SvgAttrib.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgAttdef class description
**
**	Date:		May 1, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 1, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;

// This class currently has no purpose in the conversion process but remains for 
// possible future considerations.
/**
*	This class encapsulates and converts the DXF object ATTRIB. An attrib is used
*	to identify special values for blocks. These attributes can have four attributes
*	of their own: visibility, consistancy, required verification and a preset value.
*	<P>
*	ATTRIBs are sister objects to ATTDEFs.
*
*	@version	0.01 - May 1, 2003
*	@author		Andrew Nisbet
*/
public class SvgAttrib extends SvgText
{
	private String tag;		// name of the attribute
	
	////////////////////////////////////////////////
	//				Constructor
	////////////////////////////////////////////////
	public SvgAttrib(DxfConverter dfxc)
	{	super(dfxc);	}
	
	
	////////////////////////////////////////////////
	//				Methods
	////////////////////////////////////////////////	
	/**	This will make an attribute invisible if the argument's bit
	*	value contains '1'.
	*/
	public void setAttributeFlag(int flag)
	{
		if ((flag & 1) == 1)
		{
			// flip the colour to a negative value to ensure a hidden attribute
			// when output.
			int colour = getColour(1);
			setColour(-(Math.abs(colour)));
		}
		// ignore the rest of the flags...
	}
	
	/** Sets the tag name for the attribute. The tag is the attribute's label, and 
	*	does not have to be unique even within the same block.
	*/
	public void setTag(String tag)
	{	this.tag = tag;	}
	
	/** Creates a deep copy of this object.
	*/
	protected Object clone()
	{
		SvgAttrib a = (SvgAttrib)super.clone();

		a.tag = this.tag;

		return a;
	}
}