
/****************************************************************************
**
**	FileName:	SvgArc.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgArc class
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.5 - December 16, 2002 Added getElementAsPath() method and
**				made variable declaration modifications to suit. Made class
**				final. Added setCounterClockwiseFlag() method.
**				1.6 - June 23, 2003 Added endPoint and modified calculations
**				to produce it.
**				1.61 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				1.62 - September 15, 2005 Fix for SVG Plug-in version 3.01 and 3.02 bug.
**				1.63 - January 31, 2006 Fix for SVG Plug-in version 3.01 and 3.02 bug
**              for arcs that produce '-0.0,-0.0'.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;						// DxfConverter

/**
*	This class encapsulates the SVG arc element. This class will compensate 
*	for the SVG Plug-in version 3.01 and 3.02 bug where by arcs that have
*	0.0 values for rx and or ry will crash the browser.
*	@see        #getElementAsPath
*	@see        #calculateMyUniqueData
*	@version	1.62 - September 15, 2005
*	@author		Andrew Nisbet
*/

public final class SvgArc extends SvgDoubleEndedGraphicElement
{
	private double Radius;		// radius of arc
	private double StartAngle;	// angle for start point
	private double EndAngle;	// angle of end point
	private double c1;			// Start Point x
	private double c2;			// Start Point y
	private double rx;			// x radius
	private double ry;			// y radius
	private double x_rotation;	// angle of rotation about x-axis
	private int large_arc_flag;	// large_arc_flag.
	private int sweep_flag = 0;	// Arc sweep flag.
	private double x;			// x value of the destination point
	private double y;			// y value of the destination point
	private boolean isCounterClock = true;// counterClockwise flag default
		// is Counter Clock wise.
	private Point startPoint = null;	// Start point, for point comparisons.
		// not equiv to Anchor.
		

	/** Sets the fill of the arc to none and calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc Dxf conversion context
	*/
	public SvgArc(DxfConverter dxfc)
	{
		super(dxfc);
		setType("path");
		// We set the fill of this object because it will default fill.
		// If we don't we get filled arcs. I used to counter act this
		// by setting the layer to automatically add a fill:none; but
		// have opted to include only layer information that comes from
		// the Dxf. All other information that is required to correctly
		// generate an object has to be explicitly coded.
		setFill("none");
	}

	/** Sets the radius of the arc object.*/
	public void setRadius(double r)
	{
		Radius = r;

		Radius *= svgUtility.Units();
	}

	/** Sets the start angle, degrees above 0, of the arc.*/
	public void setStartAngle(double a)
	{
		// convert to radians as Java uses them as default for calculations.
		StartAngle = a;
	}

	/** Sets the end angle, degrees above 0, of the arc.*/
	public void setEndAngle(double a)
	{
		// convert to radians as Java uses them as default for calculations.
		EndAngle = a;
	}


	/**	Sets the CounterClockwize flag.
	*	Normal angle orientation dictates that 90 degrees is north and
	*	270 degrees is south.
	*	The CounterClockwize flag means that 90 degrees is in the
	*	south and 270 is in the north.
	*	<BR><BR>
	*	0 = counterclockwize (90 at south).
	*	1 = normal, default, counterclockwise (90 at north).
	*/
	public void setCounterClockwiseFlag(int isCounterClockwise)
	{

		if (isCounterClockwise > 0)
		{
			isCounterClock = true;	// default
		}
		else
		{
			// swap the start and end angles to control the
			// sweep and large arc flags.
			StartAngle = 360 - StartAngle;
			EndAngle = 360 - EndAngle;
			isCounterClock = false;
		}
	}

	/** Returns the description of the arc as a path in a String format.
	*/
	public String getElementAsPath(boolean fromPreviousPoint)
	{
		calculateMyUniqueData();
		StringBuffer ArcOutput = new StringBuffer();

		// This is a bug fix for arcs that are complete circles. If you
		// are too accurate the start and end point overshoot each other
		// and make the fill 'move' around erratically. To compensate
		// we will trim the decimal value of the path's 'M' point.
		if (EndAngle == StartAngle)
			c2 = svgUtility.trimDouble(c2,0);
			
			
		///////////////////////////////////////////////////////////////////
		// Fix for SVG Plug-in version 3.01 and 3.02 bug. If the rx and/or
		// ry are zero, make them so they have a very small value of 0.001
		// or else these plug-ins will crash.
		
		if(Math.abs(rx) == 0.0)
		{
			rx = 0.001;
		}
		
		if(Math.abs(ry) == 0.0)
		{
			ry = 0.001;
		}
		///////////////////////////////////////////////////////////////////
		
		
		
		if (fromPreviousPoint)
		{
			ArcOutput.append("A"+rx+","+ry+" "+x_rotation+" "+
				large_arc_flag+","+sweep_flag+" "+x+","+y);
		}
		else
		{
			// map using absolute coord system.
			ArcOutput.append("M"+c1+","+c2+"A"+rx+","+ry+" "+x_rotation+" "+
				large_arc_flag+","+sweep_flag+" "+x+","+y);
		}

		return ArcOutput.toString();
	}

	/** Performs the unique calculation required to describe this object as an SVG element.*/
	protected String calculateMyUniqueData()
	{

		/*
		*	Make the necessary calculations to determine start points location
		*	the values below have already been converted to SVG space.
		*/

		/*
		*	Path example:
		*	<path d="M 125,75 a100,50 0 ?,? 100,50"
		*		style="fill:none; stroke:red; stroke_width:6"/>
		*/

		/*
		*	In the translation to svg space we have to add to x since it is the
		*	same as Acad and subtract from y as it is the opposite to Acad.
		*/


		//c1 = Anchor.getX() + Math.cos(Math.toRadians(StartAngle)) * Radius;
		//c2 = Anchor.getY() - Math.sin(Math.toRadians(StartAngle)) * Radius;
		//c1 = svgUtility.trimDouble(c1);
		//c2 = svgUtility.trimDouble(c2);

		setStartPoint();

		rx = ry = svgUtility.trimDouble(Radius);

		x_rotation = 0.0D;
		large_arc_flag = 0;

		/*
		*	The large arc flag is set if the StartAngle is larger than the end angle
		*	i.e. go all the way around the origin to complete the arc - or -
		* 	if the difference of the two angles is greater than 180 degrees.  That
		*	means that the center of the arc is inside the cord of the start point and endpoint
		*	and the arc must be a large arc. If the center is outside it will be a small arc.
		*
		*	On a more fundamental level the case for arcs whose start angle is larger than
		*	their end angle also have to be taken into consideration and we do that like this:
		*/
		if (EndAngle > StartAngle)
		{
			if ((EndAngle - StartAngle) > 180)
			{
				large_arc_flag = 1;
			}
		}
		else  //(StartAngle > EndAngle)
		{
			if ((360 - Math.abs(StartAngle - EndAngle)) > 180)
			{
				large_arc_flag = 1;
			}
		}

		sweep_flag = 0;

		if (isCounterClock == false)
		{
			sweep_flag = 1;
			// now reverse the large arc flag because the calculation
			// for the large arc is identical except with the start and
			// end angles reversed, so it will be the opposite of any
			// previous calculation.
			if (large_arc_flag == 0)
				large_arc_flag = 1;
			else
				large_arc_flag = 0;
		}
		
		setEndPoint();
			
		//x = svgUtility.trimDouble(Anchor.getX() +
		//	Math.cos(Math.toRadians(EndAngle)) * Radius);
		//y = svgUtility.trimDouble(Anchor.getY() -
		//	Math.sin(Math.toRadians(EndAngle)) * Radius);

		StringBuffer ArcOutput = new StringBuffer();

		///////////////////////////////////////////////////////////////////
		// Fix for SVG Plug-in version 3.01 and 3.02 bug. If the rx and/or
		// ry are zero, make them so they have a very small value of 0.001
		// or else these plug-ins will crash.
		
		if(rx == 0.0)
		{
			rx = 0.001;
		}
		
		if(ry == 0.0)
		{
			ry = 0.001;
		}
		///////////////////////////////////////////////////////////////////
		
		// map using absolute coord system.
		ArcOutput.append(" d=\"M"+c1+","+c2+" A"+rx+","+ry+" "+x_rotation+" "+
			large_arc_flag+","+sweep_flag+" "+x+","+y+"\"");

		return ArcOutput.toString();
	}
	
	// This method can be called for two different reasons so I have separated it here
	// so they could be called individually and use the same algorithm.
	private void setEndPoint()
	{
		x = svgUtility.trimDouble(Anchor.getX() +
			Math.cos(Math.toRadians(EndAngle)) * Radius);
		y = svgUtility.trimDouble(Anchor.getY() -
			Math.sin(Math.toRadians(EndAngle)) * Radius);
			
		endPoint = new Point(DxfConverterRef);
		endPoint.setXUU(x);
		endPoint.setYUU(y);		
	}
	
	// The start point is a calculated point not the Anchor.
	private void setStartPoint()
	{
		c1 = Anchor.getX() + Math.cos(Math.toRadians(StartAngle)) * Radius;
		c2 = Anchor.getY() - Math.sin(Math.toRadians(StartAngle)) * Radius;
		c1 = svgUtility.trimDouble(c1);
		c2 = svgUtility.trimDouble(c2);
		
		startPoint = new Point(DxfConverterRef);
		startPoint.setXUU(c1);
		startPoint.setYUU(c2);
	}
	
	/** This method calculates and returns the end point of the arc.
	*	This is primarily required for animation when we compare objects'
	*	points to one-another to see if they are located at the same position
	*	and therefore animated as a group.
	*/
	public Point getEndPoint()
	{
		if (endPoint == null)
			setEndPoint();
		
		return endPoint;
	}
	
	
	/** This method returns the start point of an arc. Make no mistake, the
	*	start point is not the Anchor, but rather a calculation based on the
	*	location of an Anchor.
	*/
	public Point getStartPoint()
	{
		if (startPoint == null)
			setStartPoint();
		
		return startPoint;		
	}

	protected Object clone()
	{
		SvgArc sa 			= (SvgArc)super.clone();

		sa.Radius			= this.Radius;
		sa.StartAngle		= this.StartAngle;
		sa.EndAngle			= this.EndAngle;
		sa.c1				= this.c1;
		sa.c2				= this.c2;
		sa.rx				= this.rx;
		sa.ry				= this.ry;
		sa.x_rotation		= this.x_rotation;
		sa.large_arc_flag	= this.large_arc_flag;
		sa.sweep_flag 		= this.sweep_flag;
		sa.x				= this.x;
		sa.y				= this.y;
		sa.isCounterClock 	= this.isCounterClock;

		return sa;
	}
}