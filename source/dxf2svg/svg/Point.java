
/****************************************************************************
**
**	FileName:	Point.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Point Class for Dxf2Svg. Point is a basic element for
**				describing the location of objects in SVG and DXF. Point
**				encapsulates all the data and conversion methods necessary
**				for both DXF and SVG space.
**
**	Date:		January 7, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 August 14, 2002 dropped most preceeding spaces in
**				toString() methods.
**				1.02 August 27, 2002 Added toTransformCoordinate() method.
**				1.03 September 6, 2002 Added SvgUtil Object locally.
**				1.04 September 10, 2002 Added DxfConverter reference so
**				SvgUtil can be gotten through the getSvgUtil() method of
**				that class.
**				1.05 October 10, 2002 Moved Point compare values from here
**				to Dxf2SvgConst interface.
**				1.06 November 19, 2002 Added a copy() method and test for
**				NullDxfConverterReferenceException.
**				1.07 November 27, 2002 Repatriated Location constants from
**				Dxf2SvgConst interface.
**				1.08 December 11, 2002 Added copy constructor.
**				1.50 December 19, 2003 Altered fuzzy value mutator methods 
**				to allow the setting of the fuzz value statically from a
**				command line switch. This necessetates the calculations of 
**				the fuzz * svgUtil.units() functions to be changed because
**				the svgUtil.units() will not be known when fuzz is set by 
**				the command line.
**				1.51 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;
import dxf2svg.util.*;

/**
*	The Point class encapsulates the smallest element used in the conversion
*	between DXF and SVG.
*
*	Each Point object has several useful methods including auto conversion
*	to SVG space and units and the ability to determine if one Point is
*	within close proximity to another.
* 	<BR><BR>
*	All Points include X, Y and Z space coordinates but the Z coord
*	is not used by SVG, but is included for future projects.<BR><BR>
*	The <code>toString()</code> method has be overwritten from its
*	base class, Java.lang.Object, but is joined by several other useful
*	<code>toString()</code> methods that allow a point to express itself
*	in a different manner depending on context. So objects such as SVGCircle
*	can call the <code>toStringCircle()</code> and get the unique
*	SVG syntax for describing a circle's centre point.
*	<BR><BR>
*	<H4>Note</H4>
*	All methods whose names end with 'UU' pass arguments unconverted to the
*	Point object.
*	Example: if you setX(9.0) this would be converted to 614.11... but if
*	the setXUU(9.0), x will = 9.0.
*	@version	1.08 December 11, 2002
*	@author		Andrew Nisbet
*/

public final class Point implements Cloneable
{
	// Point comparison values.
	/** The points are the one-in-the-same. */
	public final static int SAME_POINT				= -1;
	/** The points are not local to each other. */
	public final static int NOT_LOCAL 				= 0;
	/** The points share the same vertical alignment. */
	public final static int VERTICALLY_ALIGNED 		= 1;
	/** The points share the same horizontal line. */
	public final static int HORIZONTIALLY_ALIGNED 	= 2;
	/** The points are local to each other within fuzz distance.
	*	@see Point#getFuzz
	*/
	public final static int LOCAL 					= 3;

	private double x;
	private double y;
	private static double FuzzyValue = 0.05;
	// FuzzyValue is for measuring how close another point is to this point.
	// and therefore if the two points are related ie part of the same path or group
	// or, in the case of strings whether the strings above and below are
	// part of the same paragraph.

	private DxfConverter DxfConverterRef;
	// We need an SvgUtil object to do some calculations.
	private SvgUtil svgUtility;








	///////////////////////////////////////////////////////////////////
	//					Constructors
	///////////////////////////////////////////////////////////////////
	/** Creates Point object with DXF coordinates of '0,0' and converts
	*	it to SVG space.
	*	@param dxfc conversion context
	*/
	public Point(DxfConverter dxfc)
	{
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: Point");
		DxfConverterRef = dxfc;
		svgUtility = DxfConverterRef.getSvgUtil();

		// Translate the graphics from whatever in dxf to 0,0 reference in svg (see SvgUtil.java
		// for complete description.)
		x = 0.0 - svgUtility.deltaDxfSvgLimitsX();

		// I moved the svgUtility.Units() calculation to here for consistency between x and y calculations.
		y = ( svgUtility.convertYToSvgSpace(0.0) ) - svgUtility.deltaDxfSvgLimitsY();
	}

	/** Creates Point object with DXF argument coordinates and converts	them to SVG space.
	*
	*	@param dxfc conversion context
	*	@param xNum x value from DXF space
	*	@param yNum y value from DXF space
	*/
	public Point(DxfConverter dxfc, double xNum, double yNum)
	{
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: Point");
		DxfConverterRef = dxfc;
		svgUtility = DxfConverterRef.getSvgUtil();
		x = svgUtility.convertXToSvgSpace(xNum) - svgUtility.deltaDxfSvgLimitsX();
		y = svgUtility.convertYToSvgSpace(yNum) - svgUtility.deltaDxfSvgLimitsY();
	}

	/** Creates Point object with DXF argument coordinates, converts to SVG space and allows specification of fuzz value.
	*
	*	Fuzz value is a tolerance beyond which two points are no longer
	*	considered to be at the same location. If the fuzz value is set
	*	too low no points will detect proximity to each other, to big
	*	and too many points will be included in a local set.
	*
	*	@param dxfc conversion context
	*	@param xNum x value from DXF space
	*	@param yNum y value from DXF space
	*	@param Fuzz value
	*/
	public Point(DxfConverter dxfc, double xNum, double yNum, double Fuzz)
	{
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: Point");
		DxfConverterRef = dxfc;
		svgUtility = DxfConverterRef.getSvgUtil();
		x = svgUtility.convertXToSvgSpace(xNum) - svgUtility.deltaDxfSvgLimitsX();
		y = svgUtility.convertYToSvgSpace(yNum) - svgUtility.deltaDxfSvgLimitsY();
		FuzzyValue = Fuzz * svgUtility.Units();
	}




	///////////////////////////////////////////////////////////////////
	//					Methods
	///////////////////////////////////////////////////////////////////


	/** The caller sends the target	point and this point copies in its data.
	*	This is used in SvgMultiLineText to copy points into
	*	an array. If a null Point is passed a NullPointerException is thrown.
	*	If an already existing point is passed its current state is
	*	replaced with the copy of this point.
	*	@see SvgMultiLineText
	*	@param target A reference to a point into which you want to copy
	*	this point.
	*	@throws unchecked NullSvgPointException if the copy target Point is null.
	*/
	public final void copyInto(Point target)
	{
		if (target == null)
			throw new NullSvgPointException("Attempted to copy a Point "+
			"into a null Point reference.");
		// Don't reconvert the x, y and fuzz values.
		target.setXUU(this.getX());
		target.setYUU(this.getY());
		target.setFuzz(this.getFuzzRaw());
	}


	////****************** experimental ********************/
	// Used by SvgSpline Only.
	public void setX(int xNum)
	{
		x = xNum;
	}
	// Used by SvgSpline Only.
	public void setY(int yNum)
	{
		y = yNum;
	}
	////****************** experimental ********************/

	/** Sets X value of a point converting from DXF to SVG space
	*
	*	@param xNum x value from DXF space
	*/
	public void setX(double xNum)
	{
		x = svgUtility.convertXToSvgSpace(xNum) - svgUtility.deltaDxfSvgLimitsX();
	}




	/** Sets Y value of a point converting from DXF to SVG space
	*
	*	@param yNum y value from DXF space
	*/
	public void setY(double yNum)
	{
		y = svgUtility.convertYToSvgSpace(yNum) - svgUtility.deltaDxfSvgLimitsY();
	}

	/** Accepts but ignores Z value of a point; issues error message to stdout.
	*
	*	@param zNum z value from DXF space
	*/
	public void setZ(double zNum)
	{
		System.err.println("Point Error: objects ignore 'z' dimensions.");
	}

	/** Sets X value of a point converting to USR_UNITS but does not
	*	convert to Svg space coordinates. Used in transformations
	*	in <code>SvgEntityReference</code> {@link SvgEntityReference#toString}
	*
	*	@param xNum x value from DXF space
	*/
	public void setTransformationX(double xNum)
	{
		x = xNum * svgUtility.Units();
	}


	/** Sets Y value of a point converting to USR_UNITS but does not
	*	convert to Svg space coordinates. Used in transformations
	*	in <code>SvgEntityReference</code> {@link SvgEntityReference#toString}
	*
	*	@param yNum y value from DXF space
	*/
	public void setTransformationY(double yNum)
	{
		// We have to account for same objects moving in the same way
		// as they do in DXF. I.e. if I have a block that moves from
		// 0,0 dxf to 1,1 dxf that is a right and up transformation.
		// We have to do the same transformation in Svg - right and up.
		// so we compensate by moving in a negative direction in Svg space.
		y = -(yNum * svgUtility.Units());
	}


	/** Sets the fuzz value for all Points. Default is '0.05'. Because
	*	points are stored as doubles values and have an emense depth of
	*	precision we have to use a fuzz value as a tolerance distance between
	*	two points. If the distance is smaller than the fuzz value then
	*	the two points are considered to be on top of each other; anything
	*	larger and the points are not local to each other.
	*
	*	@param fuzz fuzz value like '0.05' or what-have-you.
	*/
	public static void setFuzz(double fuzz)
	{
		FuzzyValue = fuzz;
	}

	/** Returns the fuzz value for all Points. Fuzz is a legitimate term
	*	used by AutoCAD for tolerance distances within which two points
	*	can be considered to be located on the same spot.
	*
	*	@return fuzz fuzz value like '0.05'
	*/
	public double getFuzz()
	{
		return FuzzyValue * svgUtility.Units();
	}



	// some classes notably SvgWipeOut modify anchor points of objects
	// and need to setX()
	// but without converting units
	// so we pass an SVG converted coordinate.

	/** Sets the X value of a Point without any conversions.
	*
	*	UU suffix refers to User Units.
	*	@param xNum x value
	*/
	public void setXUU(double xNum)
	{
		x = xNum;
	}

	/** Sets the Y value of a Point without any conversions.
	*
	*	UU suffix refers to User Units.
	*	@param yNum y value
	*/
	public void setYUU(double yNum)
	{
		y = yNum;
	}


	/** Sets the Z value of a Point without any conversions.
	*
	*	UU suffix refers to User Units.
	*	@param zNum z value
	*/
	public void setZUU(double zNum)
	{
		System.err.println("Point Error: objects ignore 'z' dimensions.");
	}

	/** Returns the raw fuzz value, that is, the fuzz value before being converted to SVG Units.
	*
	*	@return Units of fuzziness as a double.
	*/
	public double getFuzzRaw()
	{
		return FuzzyValue;
	}

	/** Returns this Points x value */
	public double getX()
	{
		return(x);
	}

	/** Returns this Points y value */
	public double getY()
	{
		return(y);
	}

	/**	Used to determine the relationship of two points spacially.
	*	<BR><BR>
	*	This method returns an integer that specifies how two points relate to
	*	each other. This will be used to group objects
	*	that share the same axis or are of similar types and in the same area.
	*	You will also be able to tell if any coordinate is part of a continuous path
	*	for if the end point of a line is directly over another endpoint of another line
	*	then they are part of the same path.
	*
	*	@param Pt Another Point object.
	*	@return integer value of results of the calculations.
	*	@see #SAME_POINT
	*	@see #NOT_LOCAL
	*	@see #VERTICALLY_ALIGNED
	*	@see #HORIZONTIALLY_ALIGNED
	*/
	public int testRelationship(Point Pt)
	{
		// If the references are pointing to the same object...
		if (this == Pt)
			return SAME_POINT;

		int result = NOT_LOCAL; // or 0

		double DeltaX = Math.abs(this.x - Pt.x);
		double DeltaY = Math.abs(this.y - Pt.y);

		if (DeltaX <= getFuzz())
			result += VERTICALLY_ALIGNED;

		if (DeltaY <= getFuzz())
			result += HORIZONTIALLY_ALIGNED;

		return result;
	}
	
	
	/** This method will boolean test for locality of two points. Locality
	*	is defined as two points on the same spot within FUZZ level of proximity.
	*/
	public boolean isSamePlace(Point p)
	{
		if (testRelationship(p) == LOCAL)
			return true;
		return false;
	}




	/*
	*	This is the basic toString() method to convert (default behaviour)
	*	The other two methods are for the case of an object like a line that
	*	has two points and the outputs are designated by x1,x2,y1 and y2.
	*	Depending on which type of Point we are, return the correct notation.
	*/

	/** Outputs default String representation of x and y coordinates
	*
	*	@return Point Default point information.
	*/
	public String toString()
	{

		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return getClass().getName()+"[x=" + x + ",y=" + y + "]";
	}

	/** Outputs default String representation of x and y coordinates.
	*	Used for SvgText objects.
	*	@return &lt;space&gt;x=&quot;xValue&quot; y=&quot;yValue&quot;
	*/
	public String toStringText()
	{

		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return "x=\"" + x + "\" y=\"" + y + "\"";
	}
	
	
	/** Outputs default String representation of x and y coordinates in DXF space.
	*	@return &lt;space&gt;x=&quot;DxfXValue&quot; y=&quot;DxfYValue&quot;
	*/
	public String asDxfPoint()
	{
		double xRef = svgUtility.trimDouble(x / svgUtility.Units());
		double yRef = svgUtility.trimDouble(svgUtility.getLimitsMaxY() - y / svgUtility.Units());
		// all toString methods should return the x and y values as Strings and not rely
		// on implicit casting of doubles.
		return "[" + String.valueOf(xRef) + ", " + String.valueOf(yRef) + "]";
	}


	/** Outputs the first of a pair of coordinates.
	*
	*	@return &lt;space&gt;x1=&quot;xValue&quot; y1=&quot;yValue&quot;
	*	@see #toStringSecondCoord()
	*/
	public String toStringFirstCoord()
	{

		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return "x1=\"" + x + "\" y1=\"" + y + "\"";
	}

	/** Outputs the second of a pair of coordinates.
	*
	*	@return &lt;space&gt;x2=&quot;xValue&quot; y2=&quot;yValue&quot;
	*	@see #toStringFirstCoord()
	*/
	public String toStringSecondCoord()
	{

		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return " x2=\"" + x + "\" y2=\"" + y + "\"";
	}

	/** Outputs a pair of coordinates for the centre of a SVGCircle.
	*
	*	@return &lt;space&gt;cx=&quot;xValue&quot; cy=&quot;yValue&quot;
	*/
	public String toStringCircle()
	{

		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return "cx=\"" + x + "\" cy=\"" + y + "\"";
	}

	/** Outputs a single pair of comma separated coordinates like those used in translations
	*	<code>points=&quot;x,y x,y x,y...&quot;</code>
	*/
	public String toStringPolyLine()
	{
		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return " " + x + "," + y;
	}

	/** Outputs a single pair of comma separated coordinates like those used in transformations.
	*
	*	@return xValue&quot;,&quot;yValue
	*/
	public String toTransformCoordinate()
	{
		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return x + "," + y;
	}


	/** Outputs a single pair of unformatted coordinates. When you need something like this <code>translate(x y)</code>
	*
	*	@return xValue&lt;space&gt;yValue
	*/
	public String toStringRaw()
	{
		x = svgUtility.trimDouble(x);
		y = svgUtility.trimDouble(y);
		return x + " " + y;
	}
	
	/** Creates a clone of the object.
	*/
	public Object clone()
	{
		try{
			Point p 			= (Point)super.clone();
			p.x					= this.x;
			p.y					= this.y;
			p.DxfConverterRef 	= this.DxfConverterRef;
			
			return p;
		} catch (CloneNotSupportedException e){
			throw new InternalError();
		}
	}	// clone

}	// end of Point class