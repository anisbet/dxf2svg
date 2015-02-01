
/****************************************************************************
**
**	FileName:	SvgEllipse.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgEllipse class
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - December 10, 2002 Added getElementAsPath(),
**				setMajorAxisEndPointXUU() methods.
**				1.02 - April 8, 2004 Moved conditional processing to mutate
**				object between ellipse and path at runtime from getMyUniqueData()
**				to setEndAngle(). I need to 
**				know what each opject type is before we output the file so 
**				that I can modify attributes of necessary.
**				1.03 - October 5, 2004 I have found that ellipses that are trimmed
**				in half and then mirrored have exactly the same values as the
**				original ellipse BUT the 'z' extrusion direction value is '-1'.
**				instead of +0.99999998 or '1'. If we find a negative value in 
**				group code 230 (z extrusion direction value) then draw the ellipse's
**				sweep_flag as a '1'.
**				1.04 - October 18, 2004 another crack at the incorrect ellipse
**				draw path.
**				1.05 - January 17, 2005 Another crack at an old problem of:
**				if an ellipse start and end point are identical, the ellipse
**				will not display. If you change one of the values in the x or
**				y values in the start or end point the ellipse will draw. The
**				key is to add such a small amount that it does not effect the
**				positioning of the ellipse if the transform attribute is used.
**				See compensateForAdobeSVGViewerV3() method.
**
**	TODO:		Fix the rotation of ellipse rendered as a path.
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;						// DxfConverter
import dxf2svg.util.*;

/**
*	This class encapsulates the SVG ellipse element.
*
*	@version	1.02 - April 8, 2004
*	@author		Andrew Nisbet
*/
public final class SvgEllipse extends SvgGraphicElement
{
	private double rx;						// length of x axis
	private double ry;						// length of y axis
	private double Rotation;				// angle of Rotation
	private double StartAngle;				// Start angle of ellipse arc
	private double EndAngle;				// End angle of ellipse arc.
	private int large_arc_flag;				// Flag to indicate large arc.
	private int sweep_flag;					// Clockwise or counter-clockwise arc.
	private double MinorToMajorRatio;		// ratio of minor to major axis length.
	private Point MajorAxisEndPoint;		// Major axis endpoint location
	private Point StartPoint = null;		// Start of elliptical arc.
	private Point EndPoint = null;			// End of the elliptical arc.
	private Point DXFAnchor = null;			// Path Anchor unconverted for calculations.
	///////////// 1.02 - April 8, 2004 /////////////
	private boolean isEllipse;				// Is this object an ellipse or a path.
	// These are used so calculations that controls whether this object describes itself 
	// as an ellipse or a path. We do this so it doesn't matter which order the start and end
	// angle come in from the dxf, a meaningful assessment can be made.
	//
	// We do all of this to ensure that we know what type element we are going to render because 
	// the may be a need to modify this objects attributes somehow.
	private boolean isStartAngleSet;		// This signals that we have a start angle from the dxf
	private boolean isEndAngleSet;			// This signals that we have a end angle from the dxf
	
	private static final int COUNTER_CLOCKWISE 	= 0;
	private static final int CLOCKWISE			= 1;
	private boolean isZExtrusionSet;

	/** Sets the fill of the ellipse to none and calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgEllipse(DxfConverter dxfc)
	{
		super(dxfc);
		setType("ellipse");
		///////////// 1.02 - April 8, 2004 /////////////
		isEllipse = true;
		isStartAngleSet = false;
		isEndAngleSet = false;
		// We set the fill of this object because it will default fill.
		// If we don't we get filled ellipses. I used to counter act this
		// by setting the layer to automatically add a fill:none; but
		// have opted to include only layer information that comes from
		// the Dxf. All other information that is required to correctly
		// generate an object has to be explicitly coded.
		MajorAxisEndPoint = new Point(DxfConverterRef);
		setFill("none");
		isZExtrusionSet = false;
	}

	/** Ellipses that are trimmed
	*	in half and then mirrored have exactly the same values as the
	*	original ellipse BUT the 'z' extrusion direction value is '-1'.
	*	instead of +0.99999998 or '1'. If we find a negative value in 
	*	group code 230 (z extrusion direction value), then draw the ellipse's
	*	sweep_flag as a '1'.
	*	@since 1.03 October 5, 2004
	*/
	public void setZExtrusionDirectionValue(double zExtrusion)
	{
		if (zExtrusion < 0.0)
		{
			isZExtrusionSet = true;
		}
	}

	/** Sets the <B>x</B> value of the end point. Here is the trick though;
	*	the end point isn't a point is Svg space, it is a relative point from
	*	the centre point of the ellipse.
	*/
	public void setMajorAxisEndPointXUU(double x)
	{	MajorAxisEndPoint.setXUU(x);	}

	/** Sets the <B>y</B> value of the end point.
	*	@see #setMajorAxisEndPointXUU
	*/
	public void setMajorAxisEndPointYUU(double y)
	{	MajorAxisEndPoint.setYUU(y);	}

	/**	Sets the CounterClockwize flag for HATCH boundary path that includes an ellipse.
	*	Normal angle orientation dictates that 90 degrees is north and
	*	270 degrees is south.
	*	The CounterClockwize flag means that 90 degrees is in the
	*	south and 270 is in the north.
	*	<BR><BR>
	*	0 = counterclockwize (90 at south).
	*	1 = normal, default, counterclockwise (90 at north).
	*	@throws SvgEllipseSweepFlagException runtime exception if sweep is
	*	not within the accepted range of either 0 or 1.
	*/
	public void setCounterClockwiseFlag(int isCounterClockwise)
	{
		switch (isCounterClockwise)
		{
			case 0:
				sweep_flag = CLOCKWISE;
				break;

			case 1:
				sweep_flag = COUNTER_CLOCKWISE;
				break;

			default:
				// the rational is that if you get an illegal value
				// this should help diagnose the error's origins.
				throw new SvgEllipseIllegalSweepFlagException(
					isCounterClockwise);
		}
	}

	/** Sets a pre-calculated value of major to minor ratio of the ellipse. */
	public void setMinorToMajorRatio(double MMR)
	{	MinorToMajorRatio = MMR;	}

	/** Sets the start parameter for drawing the arc of the ellipse from
	*	0&ordm;
	*/
	public void setStartAngle(double sp)
	// Angles in a hatch can sometimes exceed 360 degrees. I rotated an
	// ellipse in space 3 or 4 times and the result was cumulative. That
	// is to say the angle ended up 789 degrees. Anyway this will make
	// all the angles fit into a 360 degrees.
	{	
		StartAngle = sp % 360;
		///////////// 1.02 - April 8, 2004 /////////////
		isStartAngleSet = true;
		// This code is run only once for every call 
		// to either setStartAngle() or setEndAngle() regardless of 
		// which order the methods was called first.
		if (isEndAngleSet)
		{
			// 6.28 = 2 * pi. If you are too accurate this will
			// skip to the else statement and draw a partial ellipse.
			// this discribes a complete ellipse in DXF.	
			if (StartAngle == 0.0 && EndAngle >= 6.2831)
			{
				setType("ellipse");
				isEllipse = true;
			}
			else
			{
				setType("path");
				isEllipse = false;
			}  // end else
			
			///////////// 1.04 - October 18, 2004 //////////////
			if (isZExtrusionSet)
			{
				flipStartAndEndAnglesAroundXAxis();
			}
			///////////// 1.04 - October 18, 2004 //////////////
		}  // end if
	}
	
	
	
	
	/** Sets the end parameter for drawing the arc of the ellipse from
	*	0&ordm;
	*/
	public void setEndAngle(double ep)
	{	
		EndAngle = ep % 360;
		///////////// 1.02 - April 8, 2004 /////////////
		isEndAngleSet = true;
		if (isStartAngleSet)
		{
			// 6.28 = 2 * pi. If you are too accurate this will
			// skip to the else statement and draw a partial ellipse.
			// this discribes a complete ellipse in DXF.	
			if (StartAngle == 0.0 && EndAngle >= 6.2831)
			{
				setType("ellipse");
				isEllipse = true;
			}
			else
			{
				setType("path");
				isEllipse = false;
			}  // end else
			///////////// 1.04 - October 18, 2004 //////////////
			if (isZExtrusionSet)
			{
				flipStartAndEndAnglesAroundXAxis();
			}
			///////////// 1.04 - October 18, 2004 //////////////
		}  // end if
	}  // end setEndAngle()
	
	
	
	///////////// 1.04 - October 18, 2004 //////////////
	/** This method ONLY gets called if the ellipical arc is mirrored in a certain way.
	*	On many drawings the illustrator was drawing an ellipse cutting it and mirroring
	*	it. When this happens the two ellipses occasionally are handled by AutoCAD by
	*	making a copy of the ellipse, setting the end point of the major axis values
	*	to their opposite signs (because the end point of the major axis is a point 
	*	relative to the center point) and making group code 230 (Z extrusion), a -1
	*	(usually it is some number close to 1). To create a mirroring effect we swap
	*	the start and end angles of the ellipse and draw the ellipse in the positive
	*	direction (counter-clockwise).
	*/
	protected void flipStartAndEndAnglesAroundXAxis()
	{
		StartAngle = Math.PI + (Math.PI - StartAngle);
		EndAngle = Math.PI + (Math.PI - EndAngle);
	}
	
	/**	This method will normalize both start and end angles onto the
	*	rotated cartisian plane. Initially DXF supplies start
	*	and end angles of the ellipse as if the ellipse was not rotated.
	*	You could have an end angle of 30 degrees but the end of the arc
	*	could be at 180 degrees in unrotated space.
	*	@param rotation of ellipse in degrees.
	*	@param angle of end point, from zero degrees, in degrees.
	*	@return normalized angle trimmed to 1 decimal place precision.
	*/
	protected double normalizeAngle(double rotation, double angle)
	{
		// To normalize an angle we have to calculate use the rotation
		// of the ellipse and add it to the existing Start and End angles.
		double normAngle;
		if (Math.abs(angle) > 180)
			// we have to use this modulus operator to trim angles that
			// are larger than 360 degrees. Acad sees nothing wrong with
			// -907 degree rotation. In addition, in Python -721%360 = 359
			// but java's answer is -1.0.
			normAngle = (360 - (Math.abs(angle)) % 360) + Rotation;
		else
			normAngle = (Math.abs(angle) % 360) + Rotation;

		return svgUtility.trimDouble(normAngle, 1);
	}



	/** Returns the description of the ellipse as a path in a String format.
	*/
	public String getElementAsPath(boolean fromPreviousPoint)
	{
		// This converts the Anchor to a DXFAnchor. The difference is
		// a Path Anchor uses
		convertAnchorToDXFAnchor();

		StringBuffer out = new StringBuffer();

		// draw the rotation in reverse to what you would expect for SVG.
		double rotation = Math.atan(
			MajorAxisEndPoint.getY() / MajorAxisEndPoint.getX()
		);

		if (sweep_flag == COUNTER_CLOCKWISE)
			Rotation = Math.toDegrees(rotation);
		else
		{
			if (Math.toDegrees(rotation) == 0)
				Rotation = 0.0;
			else
				Rotation = -Math.toDegrees(rotation);
		}

		//System.out.println("Rotation = "+Rotation);

		double rx = Math.sqrt(
			Math.pow(MajorAxisEndPoint.getX(), 2) +
			Math.pow(MajorAxisEndPoint.getY(), 2)
		) * svgUtility.Units();
		double ry = rx * MinorToMajorRatio;
		double startAngle = Math.toRadians(StartAngle);
		double endAngle = Math.toRadians(EndAngle);
		large_arc_flag = 1;

		StartPoint = new Point(DxfConverterRef);
		double[] coordinate1 = new double[2];
		double[] coordinate2 = new double[2];
		if(fromPreviousPoint == false) // if we need the initial M point.
		{
			// Get the double coordinates of the start point
			// *Note* this calculation is performed on Dxf coordinates NOT
			// SVG coordinates, and the coordinates are in pixels (USR_UNITS).
			coordinate1 = calculateEndPointOfEllipse(
			DXFAnchor, rx, ry, Rotation, startAngle);
			StartPoint.setXUU(coordinate1[0]);	// x and...
			StartPoint.setYUU(coordinate1[1]);	// y doubles.
			//System.out.println("Start Point before swapping: "+StartPoint);
			svgUtility.flipCoordinateSpace(StartPoint);	// convert to SVG space
		}


		EndPoint = new Point(DxfConverterRef);
		// Get the double coordinates of the end point
		// *Note* this calculation is performed on Dxf coordinates NOT
		// SVG coordinates, and the coordinates are in pixels (USR_UNITS).
		coordinate2 = calculateEndPointOfEllipse(
			DXFAnchor, rx, ry, Rotation, endAngle);


		EndPoint.setXUU(coordinate2[0]);	// x and...
		EndPoint.setYUU(coordinate2[1]);	// y doubles.
		//System.out.println("End Point before swapping: "+EndPoint);
		svgUtility.flipCoordinateSpace(EndPoint);	// convert to SVG space




		// trim the extra digits accuracy
		double Mx = 0.0;
		double My = 0.0;
		if(fromPreviousPoint == false)
		{
			Mx = svgUtility.trimDouble(StartPoint.getX());
			// This is a bug fix for arcs that are complete circles. If you
			// are too accurate the start and end point overshoot each other
			// and make the fill 'move' around erratically. To compensate
			// we will trim the decimal value of the path's 'M' y point.
			if (StartAngle == EndAngle)
				My = svgUtility.trimDouble(StartPoint.getY(),0);
			else
				My = svgUtility.trimDouble(StartPoint.getY());
			out.append("\nM");
			out.append(Mx+","+My);
		}

		// now for the 'A' and remaining data.
		rx 			= svgUtility.trimDouble(rx);
		ry			= svgUtility.trimDouble(ry);
		Rotation 	= svgUtility.trimDouble(Rotation, 1);

		double x 	= svgUtility.trimDouble(EndPoint.getX());
		double y 	= svgUtility.trimDouble(EndPoint.getY());
		out.append("A" + rx + "," + ry);
		out.append(" " + Rotation);
		out.append(" " + large_arc_flag);
		out.append(" " + sweep_flag);
		x = compensateForAdobeSVGViewerV3(Mx,My,x,y);
		out.append(" " + x + "," + y);
		
		return out.toString();
	}
	
	
	/** This method checks if compensation is required when drawing ellipses targeted for 
	*	Adobe's SVG viewer version 3.0 build 76. This viewer has a problem in that an 
	*	ellipse that has an identical start and end point value will not draw. To compensate
	*	we test if this case is true and if so subtract a very tiny amound from the x value 
	*	of the detination point to make the ellipse render. This should be done as the
	*	last step so this tiny value doesn't have any effect on other calculations.
	*	@return Original x value of the end point. Don't substute with other x values.
	*/
	protected double compensateForAdobeSVGViewerV3(double xStart, double yStart,
		double xEnd, double yEnd)
	{
		// Here we will test to see if the end of the ellipse is identical to the 
		// the start point. If they are we will subract 0.0001 from the x value 
		// to make the ellipse render.
		if (xStart - xEnd == 0.0)
		{
			if (yStart - yEnd == 0.0)
			{
				if (DEBUG)
				{
					System.out.println("an ellipse with a start point of: "+
					xStart+", "+yStart+" needs to compensate for Adobe SVG Viewer V3.0 build 76");
				}
				return xEnd - 0.0001;
			}
		}
		return xEnd;
	}


	// I made this public and static in case that it turns out to be useful
	// thing in future.
	/** Calculates the end point of an ellipse given the following values:
	*<OL>
	*	<LI> Centre point.
	*	<LI> Length of major axis.
	*	<LI> Length of minor axis.
	*	<LI> Rotation of ellipse in radians.
	*	<LI> angle of point on ellipse in radians.
	*</OL>
	*	@return double array of Px and Py.
	*<BR><BR>
	*	<i>Special thanks to Jerry Yee for his contribution to the
	*	algorithm used in this method.</i>
	*/
	public static double[] calculateEndPointOfEllipse(
		Point centre,				// centre point
		double lenMajorAxis,		// len major axis
		double lenMinorAxis,		// len of minor axis
		double rotationOfEllipse,	// rotation in x axis
		double angle)				// angle of point on ellipse
	{
		double[] EndPoint = new double[2];


		//EndPoint[0] = centre.getX() +
		//	lenMajorAxis * Math.cos(angle) * Math.cos(rotationOfEllipse) -
		//	lenMinorAxis * Math.sin(angle) * Math.sin(rotationOfEllipse);

		//EndPoint[1] = centre.getY() +
		//	lenMinorAxis * Math.sin(angle) * Math.cos(rotationOfEllipse) +
		//	lenMajorAxis * Math.cos(angle) * Math.sin(rotationOfEllipse);
		//System.out.println("-----------------------------");
		//System.out.println("Calc: centerX: "+centre.getX());
		//System.out.println("Calc: centerY: "+centre.getY());
		//System.out.println("Calc: lenMajorAxis: "+(lenMajorAxis / 48.68));
		//System.out.println("Calc: lenMinorAxis: "+(lenMinorAxis / 48.68));
		//System.out.println("Calc: rotationOfEllipse: "+rotationOfEllipse);
		//System.out.println("Calc: angle in degrees: "+Math.toDegrees(angle));
		//System.out.println("Calc: angle in radians: "+angle);
		double cosA = Math.cos(angle);
		//System.out.println("Calc: cos(angle): "+cosA);
		double cosR = Math.cos(rotationOfEllipse);
		//System.out.println("Calc: cos(rotationOfEllipse): "+cosR);
		double sinA = Math.sin(angle);
		//System.out.println("Calc: sin(angle): "+sinA);
		double sinR = Math.sin(rotationOfEllipse);
		//System.out.println("Calc: cos(rotationOfEllipse): "+sinR);
		//System.out.println("-----------------------------");

		EndPoint[0] = centre.getX() +
			lenMajorAxis * cosA * cosR - lenMinorAxis * sinA * sinR;

		EndPoint[1] = centre.getY() +
			lenMinorAxis * sinA * cosR + lenMajorAxis * cosA * sinR;

		//System.out.println("centre.getX() +	lenMajorAxis * cosA * cosR - lenMinorAxis * sinA * sinR = "+
		//	EndPoint[0]);

		//System.out.println("centre.getY() +	lenMinorAxis * sinA * cosR + lenMajorAxis * cosA * sinR = "+
		//	EndPoint[1]);

		return EndPoint;
	}





	/** This method will convert Anchor to Path Anchor if you need to
	*	coerce an ellipse element into a path element.
	*/
	private void convertAnchorToDXFAnchor()
	{
		if (Anchor == null)
		{
			System.err.println("SvgEllipse error: attempt to convert from "+
				"Anchor to Path Anchor but Anchor is null.");
			return;
		}

		if (DXFAnchor == null)
		{
			DXFAnchor = new Point(DxfConverterRef);
		}

		double y = Anchor.getY();

		DXFAnchor.setXUU(Anchor.getX());	// no conversion required.
		DXFAnchor.setYUU(svgUtility.convertYToDxfSpace(y)); // convert but leave
	}






	/** Performs the unique calculation required to describe this object as an SVG element.*/
	protected String calculateMyUniqueData()
	{
		/*
		*	The output of the Ellipse looks like this in Svg:
		*
		*	<ellipse cx="300" cy="200" rx="250" ry="100" fill="red"  />
		*
		*	You will also be required to Rotate the ellipse using the transform attrib like so:
		*
		*	<ellipse transform="translate(900 200) Rotation(-30)" rx="250" ry="100"
		*		fill="none" stroke="blue" stroke-width="20"  />
		*/

		StringBuffer OutputEllipse = new StringBuffer();
		// get the x value of the Major axis endpoint.
		double x = MajorAxisEndPoint.getX() ;
		// get the y value of the Major axis endpoint.
		double y = MajorAxisEndPoint.getY() ;
		double Opposite = y;
		double Adjacent = x;

		// we find the hypotenuse and multiply by svgUnits.
		double Hypotenuse = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0));
		rx = Hypotenuse * svgUtility.Units();
		// calculate the ratio of minor to major axis.
		ry = (rx * MinorToMajorRatio);
		rx = svgUtility.trimDouble(rx);
		ry = svgUtility.trimDouble(ry);



		// Calculates rotation based on the location of the end point of the major axis.
		if ((x >= 0) && (y >= 0))			// first quad
		{
			// make the rotation angle negative because Svg angles are the reverse of Acad's
			Rotation = -Math.toDegrees(Math.atan(Opposite/Adjacent));
		}
		else if ((x < 0) && (y >= 0))	// second quad
		{
			// here we have a the end of the major axis in the top left quadrant so
			// the inverse tan will give -45 instead of 135
			Rotation = -(180 - Math.abs( Math.toDegrees( Math.atan( Opposite/Adjacent))));
		}
		else if ((x < 0) && (y < 0))	// third quad
		{
			// here we have a the end of the major axis in the top left quadrant so
			// the inverse tan will give 45 instead of 225
			Rotation = -(180 + Math.abs( Math.toDegrees( Math.atan( Opposite/Adjacent))));
		}
		else // ((x > 0) && (y < 0))	// forth quad
		{
			// here we have a the end of the major axis in the top left quadrant so
			// the inverse tan will give -45 instead of 315
			Rotation = -(360 - Math.abs( Math.toDegrees( Math.atan( Opposite/Adjacent))));
		}





		///////////// 1.02 - April 8, 2004 /////////////
		if (isEllipse)
		{
			// clip the extra angle accuracy to one decimal place.
			Rotation = svgUtility.trimDouble(Rotation,1);
			// Svg draws ellipse in the opposite direction to Acad so we have to make the angle negative.
			OutputEllipse.append("transform=\"translate("+Anchor.toStringRaw()+") rotate("+Rotation+")\"");
			OutputEllipse.append(" rx=\""+rx+"\" ry=\""+ry+"\"");
			return OutputEllipse.toString();
		}
		
		
		////////// from here on is for elliptical arcs (parts of ellipses). ///////////
		// this is not a complete ellipse i.e. it is an elliptical arc
		// or the user requires the ellipse to be described as a path.
		
		// If however we do not have a complete ellipse we need to use a different
		// rendering object and model - the <path> element with the modifier 'a'.
		// OutputEllipse.append("<text x=\""+Anchor.getX()+"\" y=\""+Anchor.getY()+"\">ellipse here</text>");

		/*
		*  <path d="M600,350 a25,100 -30 0,1 50,-25
		*          fill="none" stroke="red" stroke-width="5"  />
		*/
		double mx, endx;
		double my, endy;
		
		///////////// 1.02 - April 8, 2004 /////////////
		// This is now set in setEndAngle()
		//this.setType("path");

		/*
		*	Ok, here things get a little more complex in fact the most complex so far.
		*	We are now going to draw an elliptical arc and rotate it.
		*
		*	Here is what needs to be done:
		*
		*	1) we calculate the elliptical arc based on absolute coordinates in SVG space.
		*	This calculation is straight forward - you calculate the start of the ellipical
		*	arc by the information already collected from the DXF and move to that point with
		*	the 'M' operator in the path statement. (See below.)
		*
		*	2) you already know the major (x) axis and minor (y) axis lengths so add them
		*	after the 'A' operator (for elliptical 'A'rc).
		*
		*	3) In any other elliptical arc you might add the rotation but in SVG this means
		*	rotating the x axis and we don't want that because we are drawing an arc based
		*	on absolute path description. So how do we rotate the arc into position?
		*	Read on...
		*
		*	4) Next we have to determine what the large-arc and sweep-flag values will be.
		*
		*	5) Now calculate the end point using a similar formula to the start point.
		*
		*	6) There is only one thing left to do - handle the special case of a rotated
		*	elliptical arc.  The rotation is handled in the 'transform' operator but you
		*	don't get off that easy! When you transform an element in SVG you actually
		*	move the entire SVG space for that element around the 0,0 coordinate. In this
		*	case we rotate the arc but in doing so all of Svg space for that element is
		*	rotated from the 0,0 coordinate. This means that you have to add an additional
		*	control to the transform operator to translate it from it's new position in
		*	space back to it's original position.  You do this by calculating the delta
		*	between the new Anchor point and the original and use the negative values to
		*	move the arc back to the correct location.  This calculation adds a lot of
		*	overhead but I can't think of another way to do it just now and time is
		*	a-wasting...
		*/


		/*
		*	The calculation has three steps:
		*/

		// We will need the following for this for the Calculation...
		double H;				// length of line from object to 0,0
		double xT;				// x coordinate of translation
		double yT;				// y coordinate of translation
		double a;				// original angle of object to 0,0 x,y coordinates
		double rT;				// total angle of a + rotation or Rotation translation.

		//	1) Solve for the hypotenuse of angle from object origin to 0,0
		H = Math.sqrt(Math.pow(Anchor.getX(),2) + Math.pow(Anchor.getY(),2));

		//	2) Solve original angle of object to line y = 0
		a = Math.toDegrees( Math.atan( Anchor.getY() / Anchor.getX() ) );

		//	3) Find the total of the original angle plus rotation angle
		rT = a + Rotation;

		//	4) Calculate the translation x,y values denoted by xT and yT.
		xT = Anchor.getX() - H * Math.cos( Math.toRadians( rT ) );
		yT = Anchor.getY() - H * Math.sin( Math.toRadians( rT ) );

		//  5) Calculate the large-arc-flag
		/*
		*	AutoCAD has very little info on this subject but they do broach it this way:
		*	Start parameter (this value is 0.0 for a full ellipse)
		*	End parameter (this value is 2pi for a full ellipse)
		*	From this we understand that we are dealing with radians and that unlike
		*	the SvgArc we do not have a problem situation where the end parameter could
		*	be smaller than the start. So from that we can say the following:
		*
		*	The O'Reilly book SVG Essentials (Page 79) says "The large-arc-flag, which is zero
		*	if the arc's measure [delta start end points] is less that 180 degrees, or 1 if 
		*	the arc's measure is greater than or equal to 180 degrees."
		*
		*	Since sometimes the delta of start and end point is a negative value, take the
		*	absolute value. This condition is quite rare.
		*/
		double deltaEndStart = Math.abs(Math.toDegrees(EndAngle) - Math.toDegrees(StartAngle));
		if (deltaEndStart >= 180)
			large_arc_flag = 1;
		else
			large_arc_flag = 0;
		
		
		//////////////////// This added in version 1.03 //////////////////
		if (isZExtrusionSet)
		{
			sweep_flag = 1;
		}
		else
		{
			sweep_flag = 0;
		}
		//////////////////// This added in version 1.03 //////////////////

		Rotation = svgUtility.trimDouble(Rotation);
		xT = svgUtility.trimDouble(xT);
		yT = svgUtility.trimDouble(yT);
		OutputEllipse.append("transform=\"translate("+xT+" "+yT+") rotate("+Rotation+")\"\n\t");
		// so where does this ellipical arc start? At the
		OutputEllipse.append(" d=\"M"); //+Anchor.toStringPolyLine()		// starting point
		// the x value of the first point is made with the following formula
		mx = svgUtility.trimDouble(Anchor.getX() + rx * Math.cos(StartAngle));
		// the y value of the first point is made with the following formula
		my = svgUtility.trimDouble(Anchor.getY() - ry * Math.sin(StartAngle));
		OutputEllipse.append(mx + "," + my);
		// now it's time to add the length of the radius x and radius y
		OutputEllipse.append(" A" + rx + "," + ry);
		// now the rotation this is not used because we draw using absolute values.
		// we could use 'Rotation' if we had used relative drawing instructions.
		OutputEllipse.append(" 0");
		// here is where you will put in the large-arc sweep calculations (see SvgArc.java)
		// note the sweep flag may have to be zero all the time to account for acad's
		// drawing ellipses in a ccw direction.
		OutputEllipse.append(" "+large_arc_flag+","+sweep_flag+" ");
		// now the end point calculations.
		endx = svgUtility.trimDouble(Anchor.getX() + rx * Math.cos(EndAngle));
		endy = svgUtility.trimDouble(Anchor.getY() - ry * Math.sin(EndAngle));
		// compensate for SVG viewer version 3.0 build 76 bug.
		endx = compensateForAdobeSVGViewerV3(mx,my,endx,endy);
		OutputEllipse.append(" " + endx + "," + endy);
		OutputEllipse.append("\""); // close 'path=d' quote
		
		return OutputEllipse.toString();
	}

	protected Object clone()
	{
		SvgEllipse se 		= (SvgEllipse)super.clone();

		se.rx				= this.rx;
		se.ry				= this.ry;
		se.Rotation			= this.Rotation;
		se.StartAngle		= this.StartAngle;
		se.EndAngle			= this.EndAngle;
		se.large_arc_flag	= this.large_arc_flag;
		se.sweep_flag		= this.sweep_flag;
		se.MinorToMajorRatio= this.MinorToMajorRatio;
		se.MajorAxisEndPoint= (Point)this.MajorAxisEndPoint.clone();
		se.StartPoint		= (Point)this.StartPoint.clone();
		se.EndPoint			= (Point)this.EndPoint.clone();
		se.DXFAnchor 		= (Point)this.DXFAnchor.clone();
		se.isEllipse		= this.isEllipse;
		se.isStartAngleSet	= this.isStartAngleSet;
		se.isEndAngleSet	= this.isEndAngleSet;

		return se;
	}

	/* ***************************************************
	*****************  Internal class  *******************
	*****************************************************/

	/** Is thrown if an illegal sweep flag value is received.
	*	Legal values are either <B>0</B> or <B>1</B>.
	*/
	protected class SvgEllipseIllegalSweepFlagException
	extends RuntimeException
	{
		SvgEllipseIllegalSweepFlagException(int i)
		{
			System.err.println("Illegal sweep flag value of ->"+
				i+" received.");
		}
	}
}