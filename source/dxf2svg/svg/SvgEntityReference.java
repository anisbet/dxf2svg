
/****************************************************************************
**
**	FileName:	SvgEntityReference.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates an INSERT object from Dxf and translates it into
**				a <g> that contains refers to an Entity declaration.
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - April 04, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;	// converter and preprocessor

/**
*	This class encapsulates Dxf INSERT object. Any block reference will
*	be translated into a &lt;g&gt; referencing an entity declaration.
*
*	@version	0.01 - September 13, 2002
*	@author		Andrew Nisbet
*/
public class SvgEntityReference extends SvgReference
{
	// These are set to 1.0 because if they don't get set
	// Svg refuses to scale by 0.0.
	private double xScale 		= 1.0;	// scale on x axis
	private double yScale 		= 1.0;	// scale on y axis
	protected double Rotation 	= 0.0;	// rotation of symbol
	private Point virtualDxfPoint;		// Virtual point of DXF origin of anchor.

	private String entityReferenceName;
	/**
	*	Set the conversion context and type of object.
	*/
	public SvgEntityReference(DxfConverter dxfc)
	{
		super(dxfc);
		setType("g");
		svgUtility = DxfConverterRef.getSvgUtil();
		virtualDxfPoint = new Point(DxfConverterRef);
	}

	/** Sets the entity reference name.
	*/
	public void setEntityReferenceName(String name)
	{	entityReferenceName = DxfPreprocessor.convertToSvgCss(name,true);	}


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
	{	xScale = xscale;	}

	/**
	*	Set the scale factor of the y axis.
	*/
	public void setScaleY(double yscale)
	{	yScale = yscale;	}

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
		// Output buffer
		StringBuffer transformBuff	= new StringBuffer();
		// The various transformation buffers.
		StringBuffer rotateBuff		= new StringBuffer();
		StringBuffer translateBuff	= new StringBuffer();
		StringBuffer scaleBuff 		= new StringBuffer();

		
		// now let's calculate our transformation information if any.
		////////////////// Rotate the object ////////////
		// you may want to play with this if it doesn't work
		// Transformation a followed by tranformation b is not the
		// same as transformation b followed by transformation a.
		// Page 65 SVG Essentials, J. David Eisenberg, O'Reilly Press.
		if (Rotation != 0.0)
		{
			rotateBuff.append("rotate(");
			rotateBuff.append(Rotation + " ");
			// Blocks are rotated around their insertion point.
			rotateBuff.append(Anchor.toTransformCoordinate());
			rotateBuff.append(") ");
		}
		
		


		///////////////// Scale the object //////////////
		if ((xScale != 1.0) || (yScale != 1.0))
		{
			scaleBuff.append(" scale("+xScale);
			if (xScale != yScale)
				scaleBuff.append(","+yScale);
			scaleBuff.append(")");
			
			// Now we have to compensate for any scaling in the y axis 
			// because Dxf scales objects from the lower left corner of
			// the drawing and SVG scales from the top left.
			//
			// This requires a little explanation as it took some time 
			// to work out exactly what is going on between DXF and SVG.
			//
			// Blocks in AutoCAD have their own space. When you choose 
			// an insertion point for a block you are telling the block
			// that it's new origin is the insert point, not the drawing's
			// origin.
			//
			// Dxf2Svg reads the blocks section of the DXF and interprets the
			// entities within a block literally and converts them into 
			// XML entities with the insert point as their origin.
			// 
			// Now DXF's drawing origin is in the bottom lefthand corner of the page.
			// SVG's origin is the top left hand corner. That means that all blocks 
			// are drawn in reference to the lower left corner. We have already converted
			// the blocks entities to assume that the origin is SVG style (top left)
			// so when we try to specify an insert point in SVG, we have a problem. The
			// references are from different corners of the page.
			//
			// To compensate blocks have two points; an Anchor like all other graphic 
			// objects and a Virtual DXF Coordinate (VDC). The VDC is basically
			// the x, and y values from the DXF file, converted to USR_UNITS.
			// That's all. It is not converted to SVG space. 
			// 
			// First you 
			// must imagine that you are overlaying SVG space (0,0 top left) on top
			// of DXF space (0,0 bottom left). To get objects to appear in the same
			// location as they did in the DXF you must move that insertion point
			// up above the SVG y = 0 line (so y becomes negative in SVG).
			// This requires knowing the height of 
			// the drawing which SvgUtil.java can provide. Once you know the height
			// you find out how subtract the anchor's y coordinate from the height
			// thus in effect you are flipping between the two drawing spaces.
			//
			// Inserting an object is easy now; just use the VDC as the anchor and 
			// reference the block within a &lt;g&gt; tag. If you want to scale
			// that object, things get a little more complicated. The y
			// value is in the negative 'y' range of the SVG space. Any scaling of
			// that object will be in reference to that point.
			//
			// So to get the object to appear in it's original position we must move
			// it further up into negative y SVG space by the distance it would 
			// have grown during transformation. I.e. if the object grew by 25% or
			// 1.25 in AutoCAD, and the object is 1 inch in height then the growth
			// through scaling is 1 * (1.25 -1) or 1 * 0.25 or 0.25 inches.
			//
			// Now we need to now the distance of the VDC from the SVG object's
			// origin. That should be the height of the drawing, but to be sure 
			// add the absolute value of the distance from SVG y = 0 to the object's
			// origin in negative y SVG space, and add it to the distance from 
			// SVG y = 0 to the VDC y coordinate. Now if you multiply that distance
			// by the above 0.25, and subtract that from the SVG origin point
			// in negative SVG y space you will have compensated for the shift of 
			// due to scaling.
			//
			// We don't need to do anything with the x value because it is 
			// just the VDC x value, which has a one-to-one relationship to
			// SVG space except that it is not in SVG space, but in DXF space.
			//
			// I'm sorry this would have benifited from a picture so perhaps you
			// should browse through the paper documentation as there is a diagram
			// that I used to formulate the algorithm.
			
			//double VDCx = virtualDxfPoint.getX();
			double VDCy = virtualDxfPoint.getY();
			//double Ax	= Anchor.getX();
			double Ay	= Anchor.getY();
			
			////////////////////////////////
			//VDCx = VDCx;
			VDCy = VDCy - ((Ay + Math.abs(VDCy))*(yScale -1));
			////////////////////////////////

			// We do not need to set the x value for this point. I don't know why.
			//virtualDxfPoint.setXUU(VDCx);
			virtualDxfPoint.setYUU(VDCy);
		}
		
		// We do this last because we might have adjusted the virtualDxfPoint's y 
		// value, but the transformation is best done after the rotation and it seems
		// before the scale. 
		translateBuff.append("translate(");
		translateBuff.append(virtualDxfPoint.toTransformCoordinate());
		translateBuff.append(")");
		
		
		// Put all three parts together now.
		transformBuff.append(" transform=\"");
		// Can only do this with Java 1.4
		// The transformation ordering can be changed here.
		transformBuff.append(rotateBuff);
		transformBuff.append(translateBuff);
		transformBuff.append(scaleBuff);	// this may be an empty buffer.
		// closing quote on the transformation string.
		transformBuff.append("\"");
		transformBuff.append(getAdditionalAttributes());
		
		return transformBuff;
	}




	/**	Returns a String version of this object.
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
		// Add any transformation information,
		// probably the minimum is the x and y values.
		// again this is new to Java 1.4;
		// this function returns a StringBuffer.
		OutString.append(getAttributes());
		// close the tag.
		OutString.append(">");
		OutString.append("&"+entityReferenceName+";");
		// close tag
		OutString.append("</");
		OutString.append(getType());
		OutString.append(">");

		return OutString.toString();
	}
}