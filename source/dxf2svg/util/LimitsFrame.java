
/***************************************************************************
**
**	FileName:	LimitsFrame.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Provides frame, beyond which graphic elements do not appear.
**				This frame is a rectangle that represents the limits of a 
**				DXF. Any elements whose anchor point appear outside of this
**				frame are instructed to be invisible.
**
**	Date:		October 31, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 31, 2003
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.svg.Point;

/**	This class encapsulates the rectangular frame represented by the drawing
*	limits set in the DXF file being converted. The anchor point of an SvgObject
*	could be tested to see if it lies outside of the limits frame and then its
*	attribute could be set to 'invisible', or, in the case of SvgText, the text
*	appears as &lt;desc&gt; text (and thus is invisible but still searchable).
*	<P>
*	The reason that this class exists is to allow minimal cleanup of a DWG 
*	before exporting to DXF and more importantly, to allow all objects in 
*	the DXF to be output to SVG. The reason for this is because we need 
*	to capture the figure name of the file during the conversion but we don't 
*	want to see the figure title in the SVG file.
*	
*	@version	0.01 - October 31, 2003
*	@author		Andrew Nisbet
*/
public class LimitsFrame
{
	double xMin, yMin;
	double xMax, yMax;
	
	// Stops use of default constructor.
	protected LimitsFrame()
	{	}
	
	
	/** The four arguments of this constructor a the minimum x and y and 
	*	the maximum x and y in that order. If the order gets reversed the
	*	constructor will automatically correct the min and max values.
	*/
	public LimitsFrame(double xMin, double yMin, double xMax, double yMax)
	{
		// Transpose the x values if 'max' is smaller then min.
		if (xMax < xMin)
		{
			this.xMin = xMax;
			this.xMax = xMin;
		}
		else
		{
			this.xMin = xMin;
			this.xMax = xMax;
		}
		
		// Transpose the y values if 'max' is smaller then min.
		if (yMax < yMin)
		{
			this.yMin = yMax;
			this.yMax = yMin;
		}
		else
		{
			this.yMin = yMin;
			this.yMax = yMax;
		}
	}
	
	
	/** Two Point constructor.
	*/
	public LimitsFrame(Point pMin, Point pMax)
	{
		double pMinX = pMin.getX();
		double pMinY = pMin.getY();
		double pMaxX = pMax.getX();
		double pMaxY = pMax.getY();
		
		// Transpose the x values if 'max' is smaller then min.
		if (pMaxX < pMinX)
		{
			this.xMin = pMaxX;
			this.xMax = pMinX;
		}
		else
		{
			this.xMin = pMinX;
			this.xMax = pMaxX;
		}
		
		// Transpose the y values if 'max' is smaller then min.
		if (pMaxY < pMinY)
		{
			this.yMin = pMaxY;
			this.yMax = pMinY;
		}
		else
		{
			this.yMin = pMinY;
			this.yMax = pMaxY;
		}		
		
	}
	
	
	/** Returns the absolute width of the rectangle as a double.
	*/
	public double getWidth()
	{
		return Math.abs((xMax - xMin));
	}
	

	/** Returns the absolute height of the rectangle as a double.
	*/	
	public double getHeight()
	{
		return Math.abs((yMax - yMin));
	}
	
	
	/**	This method returns true if the point described by the x and y values is inside the 
	*	limits rectangle, and false otherwise.
	*/
	public boolean contains(double x, double y)
	{
		if (x >= xMin && x <= xMax)
		{
			if (y >= yMin && y <= yMax)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/** This method returns true if the argument point is inside of the limits of the 
	*	dxf being converted and false otherwise.
	*/
	public boolean contains(Point p)
	{
		return contains(p.getX(), p.getY());
	}
	
	
	/** Returns a String representation of this object that displays the 
	*	name of the object and its dimensions.
	*/
	public String toString()
	{
		return this.getClass().getName() + "["+xMin+","+yMin+" "+xMax+","+yMax+"]";
	}
}