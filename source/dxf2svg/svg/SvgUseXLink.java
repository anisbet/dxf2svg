
/****************************************************************************
**
**	FileName:	SvgUseXLink.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates an INSERT object from Dxf and translates it into
**				a <use> xml link reference.
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 13, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;	// DxfConverter
/**
*	This class encapsulates a <use> entity (tag). This object is included for convience
*	but it's original r&eacute;ason-d'&ecirc;tre was to allow the inclusion of symbols
*	within the converted SVG files. This object provides a location in the SVG and its
*	counterpart (SvgSymbol) is the called in reference. If this sounds a lot like ENTITY
*	references, you are correct and in-fact, these two classes have been superceded by
*	{@link SvgEntityReference} and {@link SvgEntityDeclaration} which preform the same 
*	function but without the viewport drawing problems of SvgSymbol.
*
*	<P>One of the major problems of SvgSymbol, as it is implemented in the Adobe SVG
*	viewer, is that if the insertion point of a symbol is not on the lower left corner
*	of the block (in DXF parlance), any entities that appear to the left and below the
*	insert point are not drawn. This problem is overcome by using ENTITY references
*	instead.
*
*	<P>These classes are retained for any future development that may require
*	them but are not used by any conversion function to date.
*
*	@version	0.01 - September 13, 2002
*	@author		Andrew Nisbet
*	@deprecated	This class and {@link SvgSymbol} can be implemented if required but their
*	original purpose has been superceded by {@link SvgEntityReference} and {@link SvgEntityDeclaration}
*/
public class SvgUseXLink extends SvgReference
{
	// These are set to 1.0 because if they don't get set
	// Svg refuses to scale by 0.0.
	private double XScale 		= 1.0;	// scale on x axis
	private double YScale 		= 1.0;	// scale on y axis
	protected double Rotation 	= 0.0;	// rotation of symbol
	private Point virtualDxfPoint;		// Virtual point of DXF origin of anchor.

	/**
	*	Set the conversion context and type of object.
	*/
	public SvgUseXLink(DxfConverter dxfc)
	{
		super(dxfc);
		setType("use");
		svgUtility = DxfConverterRef.getSvgUtil();
		virtualDxfPoint = new Point(DxfConverterRef);
	}

	/** Sets the anchor's x value
	*/
	public void setX(double x)
	{	
		virtualDxfPoint.setTransformationX(x);
		Anchor.setX(x);
	}

	/** Sets the anchor's y value
	*/
	public void setY(double y)
	{	
		virtualDxfPoint.setTransformationY(y);
		Anchor.setY(y);
	}
	
	/**
	*	Set the scale factor of the x axis.
	*/
	public void setScaleX(double xscale)
	{	XScale = xscale;	}

	/**
	*	Set the scale factor of the y axis.
	*/
	public void setScaleY(double yscale)
	{	YScale = yscale;	}

	/**
	*	Set and convert rotation from AutoCAD's radians to degrees
	*	in the reverse direction for SVG.
	*/
	public void setRotation(double rotation)
	{
		// AutoCAD's angle of rotation is opposite to SVG so
		// we have to convert it.
		// AutoCAD's angles are in radians and turn in the opposite
		// direction to SVG's.

		Rotation = svgUtility.trimDouble(-rotation % 360.0);
	}

	/** returns the transformation calculations for a reference.
	*	@return StringBuffer Formatted values for transformation.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer TransformString = new StringBuffer();

		
		//////////////////// Move the object ////////////
		TransformString.append(" transform=\"");		
		// now let's calculate our transformation information if any.
		////////////////// Rotate the object ////////////
		// you may want to play with this if it doesn't work
		// Transformation a followed by tranformation b is not the
		// same as transformation b followed by transformation a.
		// Page 65 SVG Essentials, J. David Eisenberg, O'Reilly Press.
		if (Rotation != 0.0)
		{
			TransformString.append("rotate(-");
			TransformString.append(Rotation + " ");
			// Blocks are rotated around their insertion point.
			TransformString.append(Anchor.toTransformCoordinate());
			TransformString.append(") ");
		}

		TransformString.append("translate(");
		TransformString.append(virtualDxfPoint.toTransformCoordinate());
		TransformString.append(")");
		
		///////////////// Scale the object //////////////
		if ((XScale != 1.0) || (YScale != 1.0))
		{
			TransformString.append(" scale("+XScale);
			if (XScale != YScale)
				TransformString.append(","+YScale);
			TransformString.append(")");
		}

		// closing quote on the transformation string.
		TransformString.append("\"");
		TransformString.append(getAdditionalAttributes());
		
		return TransformString;
	}

}