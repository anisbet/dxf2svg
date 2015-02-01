
/****************************************************************************
**
**	FileName:	SvgHatchLine.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgHatchLine object definition. A hatch line is just a line
**				but described as a path so that dashes can be introduced to
**				forma a pattern. The hatch line also is only used to form a
**				hatch pattern so it is included in the <defs><pattern>
**				elements.
**
**	Date:		January 28, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - January 28, 2003
**				0.02 - August 13, 2004 Used Attribute object instead of hard 
**				coded values for adding the hatch pattern bounding box.
**				0.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;						// DxfConverter
import dxf2svg.util.Attribute;

/**
*	This class encapsulates a line element that is used expressly for hatch
*	patterns. To that end this class implements features not found in {@link dxf2svg.svg.SvgLine}
*	namely the introduction of dash elements a-la {@link dxf2svg.util.TableLineType}.
*	<BR><BR>
*	Patterns exist in their own space in Svg. They are referenced into
*	a drawing as a &lt;pattern&gt; element nested within a &lt;defs&gt;
*	element, by way of a URL. A pattern line is essentially a &lt;path&gt;
*	element because of its definition in DXF, but could in SVG be a circle
*	or ellipse or what have you.
*
*	@version	0.02 - August 13, 2004
*	@author		Andrew Nisbet
*/

public final class SvgHatchLine extends SvgLine // implements Cloneable
{
	private double patternLineAngle;	// angle of line pattern.
	private Point basePoint;			// Pattern base point.
	private Point offsetPoint;			// Offset of line pattern.
	private int dashes;					// Number of dashes in pattern.
	private int dashIndex = 0;			// Index to dash array.
	private double[] dashArray;			// Array of dash lengths.
	private double[] bBoxDim = new double[2];
										// Bounding box of this line.
	private double x = 0.0;			// Pattern's bounding box width.
	private double y = 0.0;		// Pattern's bounding box height.
	private SvgHatchPattern parentPattern;// Parent pattern we need to query


	// Const Quadrant location values
	public static final int UPPER_RIGHT	= 1;	// 0 <= angleQuad <= 90
	public static final int UPPER_LEFT	= 2;	// 90 < angleQuad <= 180
	public static final int LOWER_LEFT	= 3;	// 180 < angleQuad <= 270
	public static final int LOWER_RIGHT	= 4;	// 270 < angleQuad <= 360
	private int quadrant = 0;					// Which of these quads does
												// our line project into.
												// Here it is not set.

	/** Calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@see SvgLine
	*	@param dxfc Dxf conversion context.
	*/
	public SvgHatchLine(DxfConverter dxfc)
	{
		super(dxfc);
		setType("path");
		offsetPoint = new Point(DxfConverterRef);
		setFill("");
		// added March 10, 2004 so we don't inherit line weight from the layer.
		// the problem is that we get dotting effect if the line weight is too
		// large. This is a quick fix for that bug. To fix the problem we should
		// draw a line at the corner of the patterns bounding box to create a 
		// continuous line pattern. Once you have fixed the bug comment this 
		// out so that it can inherit the line type from the layer it came from.
		setIncludeClassAttribute(false);
	}



	/** Sets the angle of the line pattern from group code (53).
	*/
	public void setPatternLineAngle(double angle)
	{
		patternLineAngle = angle % 360.0;

		// Set the quadrant that the line points into.
		if (patternLineAngle >= 0 && patternLineAngle < 90)
			quadrant = UPPER_RIGHT;
		else if (patternLineAngle >= 90 && patternLineAngle < 180)
			quadrant = UPPER_LEFT;
		else if (patternLineAngle >= 180 && patternLineAngle < 270)
			quadrant = LOWER_LEFT;
		else 	// Fall through to here.
				//if (patternLineAngle >= 270 && patternLineAngle < 360)
			quadrant = LOWER_RIGHT;
		//svgUtility.trimDouble(angle,1);
	}


	/** Used to determine which quadrant this line points.
	*/
	public int getHatchLineDirection()
	{	return quadrant;	}


	/** Sets X value of the pattern's base point.
	*/
	public void setPatternBasePointX(double x, boolean convertToSvg)
	{
		if (basePoint == null)
			basePoint = new Point(DxfConverterRef);

		if (convertToSvg)
			// The pattern base point is not in SVG space.
			basePoint.setXUU(x * svgUtility.Units());
		else
			basePoint.setXUU(x);
	}



	/** Sets Y value of the pattern's base point.
	*/
	public void setPatternBasePointY(double y, boolean convertToSvg)
	{
		if (convertToSvg)
			basePoint.setYUU(y * svgUtility.Units());
		else
			basePoint.setYUU(y);
	}



	/** Sets X value of the pattern's line offset.
	*/
	public void setPatternLineOffsetX(double x, boolean convertToSvg)
	{
		if (offsetPoint == null)
			offsetPoint = new Point(DxfConverterRef);

		if (convertToSvg)
			offsetPoint.setXUU(x * svgUtility.Units());
		else
			offsetPoint.setXUU(x);
	}

	/** Sets Y value of the pattern's line offset.
	*/
	public void setPatternLineOffsetY(double y, boolean convertToSvg)
	{
		if(convertToSvg)
			offsetPoint.setYUU(y * svgUtility.Units());
		else
			offsetPoint.setYUU(y);
	}


	/** Returns the offset of this perticular pattern line.
	*/
	public Point getOffsetPoint()
	{	return offsetPoint;		}

	/** Return the basepoint of a SvgHatchLine.
	*/
	public Point getBasePoint()
	{	return basePoint;		}



	/** Sets the number of dashes in a line that makes up the pattern.
	*/
	public void setNumberDashs(int dashes)
	{
		// Check to see that the argument is reasonable.
		if (dashes < 1)
			this.dashes = 1;	// One continuous line (at least).
		else
			this.dashes = dashes;

		// Now initialize the dashArray[] to the total that we need.
		if (dashArray == null)
			dashArray = new double[dashes];
	}



	/** Populates an array of dash lengths which will form a dotted pattern.
	*	This method could be called repeatedly; once for each dash or space.
	*/
	public void setDashLength(double len)
	{
		if (dashIndex >= dashes)
			throw new DashArrayIndexOutOfRangeException();

		dashArray[dashIndex] = len;
		dashIndex++;
	}


	/** This method sets the bounding box for this object to the maximum
	*	for the pattern. The SvgHatchPattern object polls all of the
	*	lines in its pattern for bounding boxes and sets the size to
	*	the maximum for the entire pattern.
	*	@throws NullParentPatternException if the parent pattern is null.
	*/
	public void setParentPattern(SvgHatchPattern pattern)
	{
		if (pattern == null)
			throw new NullParentPatternException();

		parentPattern = pattern;
	}



	/** Performs the unique calculation required to describe this
	*	object as an SVG element.
	*/
	protected String calculateMyUniqueData()
	{
		// These buffers will contain there respective data that is
		// calculated over the duration of this method. Some of the
		// data for attributes is not available until the end of the
		// calculation (read tranlation) and has to be appended out
		// of order. All the buffers will be combined at the end.
		StringBuffer hatchLineOutput 	= new StringBuffer();



		////////////////////////// test ///////////////////////////////////
		//System.out.println("##Angle of line pattern: "+patternLineAngle);
		//System.out.println("##Number of dashes in pattern: "+dashes);
		//System.out.println("##Base point: "+basePoint);
		//System.out.println("##Offset point: "+offsetPoint);

		//System.out.println("==================== next object ===================");
		//if (dashArray == null)
		//	System.out.println("dashArray[] = null");
		//for (int i = 0; i < dashArray.length; i++)
		//	System.out.println("dash["+i+"] = "+dashArray[i]);
		////////////////////////// test ///////////////////////////////////





		// Now do we need to account add dashes?
		if (dashes > 1)
		{
			hatchLineOutput.append
			(
				" stroke-dasharray=\"" + calculateDashArray() + "\""
			);
		}



		// We have to account for possibly two things; a rotation
		// and a translation.
		//
		// Svg rotates the Svg space of an object not the object itself,
		// so if you rotate an object you have to translate it to make
		// it appear at its origin.
		//
		//
		// But where to locate the origin of the pattern line to begin with?
		// If we start at 0,0 any positive rotation will draw an object
		// outside of the tile.
		//
		// To correctly place the line we move it to one of the four corners
		// of the tile depending on the angle we wish to render.
		//
		//	Origin for:
		//  270º to 360º       180º to 270º
		//          0,0  -----  1,0
		//              |     |
		//              |     |
		//              |     |
		//          0,1  -----  1,1
		//     0º to 90º       90º to 180º
		//
		// If the angle is 0 < theta < 90, we have to translate the origin
		// in the positive y value.
		//
		// Calculate the transformation.
		// To do that we need to know the width and height of the bounding
		// box.

		if (parentPattern == null)
			System.out.println("parentPattern is null.");

		if (basePoint == null)
			throw new NullSvgPointException("SvgHatchLine, '"+
				getObjID()+"' basePoint is null.");


		switch (quadrant)
		{
			case UPPER_RIGHT:
				x = 0.0 + basePoint.getX();
				y = parentPattern.getPatternHeight() + basePoint.getY();
				break;

			case UPPER_LEFT:
				x = parentPattern.getPatternWidth() + basePoint.getX();
				y = parentPattern.getPatternHeight() + basePoint.getY();
				break;

			case LOWER_LEFT:
				x = parentPattern.getPatternWidth() + basePoint.getX();
				y = 0.0 + basePoint.getX();
				break;

			case LOWER_RIGHT:
				x = 0.0 + basePoint.getX();
				y = 0.0 + basePoint.getX();
				break;

			default:
				throw new UnsetLinePatternDataException("quadrant");
		}	// end switch


		if (patternLineAngle != 0.0)
		{
			hatchLineOutput.append(" transform=\"");
			hatchLineOutput.append
			(
				"translate("+ svgUtility.trimDouble(x) +
				","+ svgUtility.trimDouble(y) +")"
			);
			hatchLineOutput.append
				(", rotate("+(-patternLineAngle)+")");
			hatchLineOutput.append("\"");
		}

		hatchLineOutput.append(" d=\"M 0,0 h 100\"");

		if (DEBUG)
		{
			// This is a bit of a bodge. Basically we are inserting an
			// element into another element's toString() method so
			// to get this to work we prematurely close the other element
			// and make this boundingbox. There will be an extra closing
			// angle '/>' at the end of the line but the viewer doesn't
			// seem to mind, and compared to the amount of time to work
			// out another statagy... well, just do this.
			hatchLineOutput.append("/>\n\t");
			hatchLineOutput.append(addHatchPatternBoundingBox());
		}

		return hatchLineOutput.toString();
	}


	/** Returns the <B>width</B> and <B>height</B> (in that order)
	*	of a box big enough (or	small enough) to render this perticular
	*	pattern line.
	*	@throws UnsetLinePatternDataException if the quadrant the line
	*	extends into has not been set. It is supposed to be set in
	*	{@link #calculateMyUniqueData}
	*/
	public double[] getBoundingBox()
	{
		// Have we already done this calcualtion? If so then return
		// the calculated amounts. This function gets called from
		// SvgHatchPattern as well. Saves time but can be deleted.
		if (bBoxDim[0] != 0.0 || bBoxDim[1] != 0.0)
			return bBoxDim;


		double x = basePoint.getX() + offsetPoint.getX();
		double y = basePoint.getY() + offsetPoint.getY();
		double angle = Math.toRadians(patternLineAngle);


		// The width of the box is determined by calculating the lengths
		// of the sides of the triangle that extends from the origin (P1)
		// to the offset point (P2), along the hatch line to the x axis,
		// and back to the origin. the width is the section along the x
		// axis. The formula is:
		//		w = root((offsetX sq)+(offsetY sq)) / sine(pattern_angle)
		double lineWidth = Math.abs
		(
			(
				Math.sqrt
				(
					( (x * x) + (y * y) )
				)
			) / Math.sin(angle)
		);

		// The height of the box is defined by this calculation.
		// Width actually describes the hypotenuse of the triangle that
		// extends from the 0,0 origin to offset point, which forms
		// a right angle to the pattern's line and then down
		// to the horizontal x axis (where y = 0) and back to the origin.
		// The base or hypotenuse is the horzontal x axis line.
		//
		// Now we have to find the height of the pattern.
		// We can create another right angle triangle up 90º to
		// where the pattern line touches the y axis.
		// Since we know the width or base of this triangle, and we
		// know the angle of the pattern line we can calculate the height
		// using the following formula:
		//		tan(theta) = Opposite / Adjacent
		// 		tan(patternLineAngle) = Opposite / width
		//		tan(patternLineAngle) * width = Opposite

		double lineHeight = Math.abs
		(
			lineWidth *
			Math.tan(
				Math.toRadians(patternLineAngle)
			)
		);

		bBoxDim[0]= svgUtility.trimDouble(lineWidth);
		bBoxDim[1]= svgUtility.trimDouble(lineHeight);

		return bBoxDim;
	}

	// This method will draw the bounding box around this line pattern;
	// for debugging purposes.
	private String addHatchPatternBoundingBox()
	{
		SvgObjectX boundingBox = new SvgObjectX(DxfConverterRef);
		Attribute attribClass = new Attribute("class","st0");
		boundingBox.addAttribute(attribClass);
		Attribute attribStrokeWidth = new Attribute("stroke-width","0.001");
		boundingBox.addAttribute(attribStrokeWidth);
		Attribute  attribFill = new Attribute("fill","none");
		boundingBox.addAttribute(attribFill);
		StringBuffer d = new StringBuffer();
		d.append("M 0,0 h ");
		d.append(String.valueOf(bBoxDim[0]));
		d.append(" v ");
		d.append(String.valueOf(bBoxDim[1]));
		d.append(" h -");
		d.append(String.valueOf(bBoxDim[0]));
		d.append(" v -");
		d.append(String.valueOf(bBoxDim[1]));
		Attribute attribD = new Attribute("d",d.toString());
		boundingBox.addAttribute(attribD);
		boundingBox.setType("path");

		return boundingBox.toString();
	}



	/** Calculates the dash array for describing dashed and dotted lines.
	*/
	protected String calculateDashArray()
	{
		StringBuffer dashArrayBuf = new StringBuffer();

		for (int i = 0; i < dashArray.length; i++)
		{
			// convert it to User Units. The abs is because autocad
			// describes white space with a negative distance.
			double dash = Math.abs(dashArray[i]) * svgUtility.Units();
			dash = svgUtility.trimDouble(dash);
			if (dash == 0.0)
				dash = 0.1;
			dashArrayBuf.append(" "+dash);
		}

		return dashArrayBuf.toString();
	}

	/** Creates a deep copy of this object.
	*/
	protected Object clone()
	{
		SvgHatchLine shl 		= (SvgHatchLine)super.clone();

		shl.patternLineAngle	= this.patternLineAngle;
		shl.basePoint			= (Point)this.basePoint.clone();
		shl.offsetPoint			= (Point)this.offsetPoint.clone();
		shl.dashes				= this.dashes;
		shl.dashIndex 			= this.dashIndex;

		int size = dashArray.length;
		shl.dashArray = new double[size];
		for (int i = 0; i < size; i++)
			shl.dashArray[i] 	= this.dashArray[i];

		shl.bBoxDim[0]		 	= this.bBoxDim[0];
		shl.bBoxDim[1]		 	= this.bBoxDim[1];
		shl.x 					= this.x;
		shl.y 					= this.y;
		// Make a reference to the parent pattern.
		shl.parentPattern		= this.parentPattern;

		////////////////// test //////////////////////
		if (this.parentPattern == null)
			System.out.println("The original has no reference to the parent pattern.");
		else if (shl.parentPattern == null)
			System.out.println("The clone has no reference to the parent pattern.");
		////////////////// test //////////////////////

		return shl;
	}


	/* ******************************************************
	**					Internal classes 				   **
	********************************************************/

	/**	This Runtime Exception gets thrown if there are more dashes
	*	than declared by group code (79) of the hatch pattern.
	*/
	protected class DashArrayIndexOutOfRangeException
	extends RuntimeException
	{
		protected DashArrayIndexOutOfRangeException()
		{
			System.err.println("There are more dashes than declared by group code(79).");
		}
	}

	/** Gets thrown if data critical to the calculation of a hatch pattern
	*	line has not been set.
	*/
	public class UnsetLinePatternDataException
	extends RuntimeException
	{
		protected UnsetLinePatternDataException(String data)
		{
			System.err.println("Key data to the calculation of the "+
			"hatch pattern line into has not been defined."+
			" The missing data is '"+data+"'");
		}
	}

	/** This gets throw if an attempt to set the object's parent pattern
	*	is null.
	*/
	protected class NullParentPatternException
	extends RuntimeException
	{
		protected NullParentPatternException()
		{
			System.err.println("The parent pattern of this hatch line "+
			"pattern is null.");
		}
	}
}