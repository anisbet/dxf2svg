
/****************************************************************************
**
**	FileName:	SvgGroup.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates a collection of SvgObjects into a
**				<g></g> tag.
**
**	Date:		August 30, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 30, 2002
**				1.01 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Vector;
import dxf2svg.animation.SvgAnimator;
import dxf2svg.DxfConverter;			// DxfConverter

/**
*	This class encapsulates a generic grouping of SvgObjects.
*	This class is mostly used for grouping objects into functional
*	units so things like animation can be applied to the group as
*	a whole.
*	<BR><BR>The subclass of the object {@link SvgLayerGroup} is a
*	speciallized class of this object that relates objects together
*	in groups by determining which layer they came from in the Dxf.
*
*	@version	1.00 - August 30, 2002
*	@author		Andrew Nisbet
*/
public class SvgGroup extends SvgCollection
{

	//////////// Constructors ////////////////
	// default
	/**
	*	Sets the DxfConverter object reference.
	*	@param dxfc Dxf conversion context.
	*/
	public SvgGroup(DxfConverter dxfc)
	{
		super(dxfc);
		setType("g");
	}

	/**
	*	Returns the collection's attributes as a StringBuffer.
	*	@return StringBuffer of formatted attributes.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer OutputString = new StringBuffer();

		// if the ObjType is null or unknown and we still want to see it in
		// the SVG for debugging purposes
		if ((DEBUG) && (! SvgObjID.equals("")))
			OutputString.append(" "+getObjID());

		if (getIncludeClassAttribute() == true)
			OutputString.append(getClassAttribute());

		OutputString.append(getAdditionalAttributes());

		return OutputString;
	}
	
	/** Allows the addition of animation to an SvgCollection object. or any of its subclasses.
	*/
	public void addAnimation(SvgAnimator sa)
	{
		if (sa == null)
			return;
		
		// Now we add the animation to the front of the collection (for consistancy)
		// by converting it to a string.
		SvgElementVector.add(0, sa);
	}
	
	/** Allows the addition of multipule animations to an SvgCollection object,
	*	or any of its subclasses. The array of animation objects are placed,
	*	in order, at the beginning of the group element.
	*	<P>
	*	If the array of animation objects is null, or its length is less than 1,
	*	the method returns without altering the SvgGroup (or subclass).
	*/
	public void addAnimation(Vector sas)
	{
		if (sas == null)
			return;
		
		// Now we add the animation to the front of the collection (for consistancy)
		// by converting it to a string.
		SvgElementVector.addAll(0, sas);
	}
}