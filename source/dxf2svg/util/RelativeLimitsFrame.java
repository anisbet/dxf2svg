
/***************************************************************************
**
**	FileName:	RelativeLimitsFrame.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Provides a relative box for testing if an object's anchor
**				point falls within it.
**
**	Date:		March 17, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_05-b05)
**
**	Version:	0.01 - March 17, 2005
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.svg.Point;

/**	This class represents a rectangular frame by which and object can 
*	query if another object falls within its bounderies. All calculations
*	are based on SVG space (0,0 at top left).
*	
*	@version	0.01 - March 17, 2005
*	@author		Andrew Nisbet
*/
public class RelativeLimitsFrame
{
	double width;
	double height;
	int position;   // The position of the box relative to the reference point (Anchor).
	Point anchor;
	
	/** Position of frame relative to a reference point (*). These positions can also
	*	be added together like this: TOP + LEFT = 
<pre>
+----------+
|          |
+----------*
</pre>	
<pre>
+----------+
|     *    |
+----------+
</pre>
*/
	public final static int CENTER   = 0;
	/** Frame LEFT of reference point (*).
<pre>
+----------+
|          *
+----------+
</pre>
	*/
	public final static int LEFT     = 1;
	/** Frame RIGHT of reference point (*).
<pre>
 +----------+
\*          |
 +----------+
</pre>
	*/
	public final static int RIGHT    = 2;
	/** Frame TOP of reference point (*).
<pre>
+----*-----+
|          |
+----------+
</pre>
	*/
	public final static int TOP      = 4;
	/** Frame BOTTOM of reference point (*).
<pre>
+----------+
|          |
+----*-----+
</pre>
	*/
	public final static int BOTTOM   = 8;
	
	// Stops use of default constructor.
	protected RelativeLimitsFrame()
	{	}
	
	
	/**
	*	@param width Width of the box.
	*	@param height Height of the box.
	*	@param position Position of the box relative to the argument point.
	*/
	public RelativeLimitsFrame(double width, double height, int position, Point anchor)
	{
		this.width    = width;
		this.height   = height;
		setBoxPosition(position);
		this.anchor   = anchor;
	}
	
	public RelativeLimitsFrame(double width, double height, Point anchor)
	{
		this.width    = width;
		this.height   = height;
		this.position = CENTER;
		this.anchor   = anchor;		
	}
	
	
	/** Returns the absolute width of the rectangle as a double.
	*/
	public double getWidth()
	{
		return width;
	}
	
	
	/** Allows the increasing or decreasing of the width of the apature
	*	of the RelativeLimitsFrame.
	*/
	public void setWidth(double newWidth)
	{
		this.width = newWidth;
	}
	

	/** Returns the absolute height of the rectangle as a double.
	*/	
	public double getHeight()
	{
		return height;
	}
	
	
	/** Allows the RelativeLimitsFrame's apature to be increased or decreased.
	*/
	public void setHeight(double newHeight)
	{
		this.height = newHeight;
	}
	
	
	/** Sets position so you can move the box around and also tests for valid
	*	box positions.
	*/
	public void setBoxPosition(int position)
	{
		switch (position)
		{
		case CENTER:
			this.position = CENTER;
			break;
		
		case LEFT:
			this.position = LEFT;
			break;
			
		case RIGHT:
			this.position = RIGHT;
			break;
			
		case TOP:
			this.position = TOP;
			break;
			
		case BOTTOM:
			this.position = BOTTOM;
			break;
		
		case TOP + LEFT:
			this.position = TOP + LEFT;
			break;
		
		case TOP + RIGHT:
			this.position = TOP + RIGHT;
			break;
			
		case BOTTOM + LEFT:
			this.position = BOTTOM + LEFT;
			break;
			
		case BOTTOM + RIGHT:
			this.position = BOTTOM + RIGHT;
			break;
			
		default:
			System.err.println("Invalid box position value: "+position+
				" defaulting to CENTER.");
			this.position = CENTER;

		}	// end switch	
	}
	
	/** Returns Position as a string.
	*/
	public String getPosition()
	{
		switch (position)
		{
		case CENTER:
			return "centered";
		
		case LEFT:
			return "left";
			
		case RIGHT:
			return "right";
			
		case TOP:
			return "top";
			
		case BOTTOM:
			return "bottom";
		
		case TOP + LEFT:
			return "top-left";
		
		case TOP + RIGHT:
			return "top-right";
			
		case BOTTOM + LEFT:
			return "bottom-left";
			
		case BOTTOM + RIGHT:
			return "bottom-right";
			
		default:
			return "unknown"; // should never get here.
		}
	}
	
	/** @return position of the bounding box.
	*	@param i any integer will do.
	*/
	public int getPostion(int i)
	{
		return position;
	}
	
	
	/** Allows the client to change anchor points without creating a new instance
	*	of this object.
	*/
	public void setAnchor(Point anchor)
	{
		this.anchor = anchor;
	}
	
	
	/**	This method returns true if the point described by the x and y values is on
	*	or inside the limits rectangle, and false otherwise.
	*/
	public boolean contains(double x, double y)
	{
		double xA = anchor.getX();
		double yA = anchor.getY();
		
		switch (position)
		{
			
		case CENTER:
			// the reference point is in the middle of the box and we search all around it.
			// handle the x first
			if ( x >= (xA - (width / 2.0)) && x <= (xA + (width / 2.0)) )
			{
				if (y >= (yA - (height / 2.0)) && y <= (yA + (height / 2.0)))
				{
					return true;
				}
			}			
			return false;
		
		case LEFT:
			// the reference point is to the middle-right of the bounding box.
			if ( x >= (xA - width) && x <= xA )
			{
				if ( y >= (yA - (height / 2.0)) && y <= (yA + (height / 2.0)))
				{
					return true;
				}
			}			
			return false;
			
		case RIGHT:
			// the reference point is to the middle-left of the bounding box.
			if (  x >= xA && x <= (xA + width) )
			{
				if ( y >= (yA - (height / 2.0)) && y <= (yA + (height / 2.0)))
				{
					return true;
				}
			}			
			return false;
			
		case TOP:
			// the reference point is below and middle of the bounding box.
			if ( x >= (xA - (width / 2.0)) && x <= (xA + (width / 2.0)) )
			{
				if ( y >= (yA - height) && y <= yA )
				{
					return true;
				}
			}			
			return false;
			
		case BOTTOM:
			// the reference point is above and middle of the bounding box.
			if ( x >= (xA - (width / 2.0)) && x <= (xA + (width / 2.0)) )
			{
				if ( y >= yA && y <= (yA + height) )
				{
					return true;
				}
			}			
			return false;
		
		case TOP + LEFT:
			// the reference point is below right of the box
			if ( x >= (xA - width) && x <= xA )
			{
				if (y >= (yA - height) && y <= yA)
				{
					return true;
				}
			}			
			return false;
		
		case TOP + RIGHT:
			// the reference point is below left of the box
			if ( x >= xA && x <= (xA + width) )
			{
				if (y >= (yA - height) && y <= yA)
				{
					return true;
				}
			}			
			return false;
			
		case BOTTOM + LEFT:
			// the reference point is to the above right of the box
			if ( x >= (xA - width) && x <= xA )
			{
				if ( y >= yA && y <= (yA + height) )
				{
					return true;
				}
			}			
			return false;
			
		case BOTTOM + RIGHT:
			// the reference point is above left of the box
			if ( x >= xA && x <= (xA + width) )
			{
				if (y >= yA && y <= (yA + height))
				{
					return true;
				}
			}			
			return false;		
		}
		// No default case.
		return false; 
	}
	
	
	
	
	
	
	
	/** Shows the bounding box area in relation to the anchor point.
	*/
	public String showBoundingBox()
	{
		double xA = anchor.getX();
		double yA = anchor.getY();
		double x1 = 0.0;
		double x2 = 0.0;
		double y1 = 0.0;
		double y2 = 0.0;
		
		switch (position)
		{
			
		case CENTER:
			// the reference point is in the middle of the box and we search all around it.
			// handle the x first
			x1 = xA - (width / 2.0);
			x2 = xA + (width / 2.0);
			y1 = yA - (height / 2.0);
			y2 = yA + (height / 2.0);
			break;
		
		case LEFT:
			// the reference point is to the middle-right of the bounding box.
			x1 = xA - width;
			x2 = xA;
			y1 = yA - (height / 2.0);
			y2 = yA + (height / 2.0);
			break;
			
		case RIGHT:
			// the reference point is to the middle-left of the bounding box.
			x1 = xA;
			x2 = xA + width;
			y1 = yA - (height / 2.0);
			y2 = yA + (height / 2.0);
			break;
			
		case TOP:
			// the reference point is below and middle of the bounding box.
			x1 = xA - (width / 2.0);
			x2 = xA + (width / 2.0);
			y1 = yA - height;
			y2 = yA;
			break;
			
		case BOTTOM:
			// the reference point is above and middle of the bounding box.
			x1 = xA - (width / 2.0);
			x2 = xA + (width / 2.0);
			y1 = yA;
			y2 = yA + height;
			break;
		
		case TOP + LEFT:
			// the reference point is below right of the box
			x1 = xA - width;
			x2 = xA;
			y1 = yA - height;
			y2 = yA;
			break;
		
		case TOP + RIGHT:
			// the reference point is below left of the box
			x1 = xA;
			x2 = xA + width;
			y1 = yA - height;
			y2 = yA;
			break;
			
		case BOTTOM + LEFT:
			// the reference point is to the above right of the box
			x1 = xA - width;
			x2 = xA;
			y1 = yA;
			y2 = yA + height;
			break;
			
		case BOTTOM + RIGHT:
			// the reference point is above left of the box
			x1 = xA;
			x2 = xA + width;
			y1 = yA;
			y2 = yA + height;
			break;		
		}
		// No default case.
		return "x min: "+String.valueOf(x1)+", x max: "+String.valueOf(x2)+"\ny min: "+
			String.valueOf(y1)+", y max: "+String.valueOf(y2);
	}
	
	
	
	
	
	
	
	
	
	/** This method returns true if the argument point is inside of the limits of the 
	*	dxf being converted and false otherwise.
	*/
	public boolean contains(Point p)
	{
		return contains(p.getX(), p.getY());
	}
	
	
	/** Returns a String representation of this object that displays the 
	*	name of the object and its width and height.
	*/
	public String toString()
	{
		return this.getClass().getName() + "["+width+","+height+": "+getPosition()+"]";
	}
}