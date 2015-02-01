
/****************************************************************************
**
**	FileName:	SvgPolyLine.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgPolyLine Class definition
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - October 8, 2002 Added line weight handling method.
**				2.00 - December 3, 2002 Added handling of polyline segment
**				bulges.
**				2.01 - December 10, 2002 Added getElementAsPath() method.
**				2.02 - December 11, 2002 Added setVertexX() and setVertexY()
**				2.03 - December 16, 2002 Added setVertex(Point,bulge) method
**				as a requirement from SvgHatch and a handy feature.
**				2.04 - April 8, 2004 Removed conditional processing to mutate
**				object from polygon to polyline to path at runtime. I need to
**				know what each opject type is before we output the file so 
**				that I can modify attributes of necessary.
**				2.05 - October 27, 2004 Added code to check if bulge is too
**				small. If setting the precision of bulge results in a bulge
**				of zero then it should not be included because the arc is so
**				shallow it has unpredictable redering results.
**				2.06 - March 23, 2005 Added	getAllSegmentPoints() method.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.*;
import dxf2svg.DxfConverter;				// DxfConverter

/**
*	This class encapsulates the SVG polyline element.
*
*	@version	2.04 - April 8, 2004
*	@author		Andrew Nisbet
*/
public class SvgPolyLine extends SvgDoubleEndedGraphicElement
{
	protected Vector PolyLinePoints = null;		// container for point objects
	protected double Bulge;						// Bulge value saved for
		// putting on the vector next time a group code 10 is encountered.
	////////////// 2.04 //////////////
	//protected boolean objectIsPath = false;		// only set if there are
		// bulges in which case this object can't be a svg polyline or
		// polygon but a path.
	//protected StringBuffer SvgPolyLineOut = new StringBuffer();	// Output string
	protected int Closed;
	protected double LineWeight;			// Line weight of the polyline.
	private VertexPoint p = null;				// Wrapper for point with bulge.

	/** Sets the fill of the polyline to none and calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgPolyLine(DxfConverter dxfc)
	{
		super(dxfc);
		////////////// 2.04 //////////////
		//setType("polyline");
		setType("path");
		// This is correct for SvgPolyLine but not for SvgHatch. Look into.
		setFill("none");
		PolyLinePoints = new Vector();
		Bulge = 0.0;
		LineWeight = 0.01;
	}

	/** Sets the vertex's bulge value if any.
	*	The bulge is the tangent of 1/4 the including angle of the
	*	curved segment so, an arc that covers 180 deg has a bulge
	*	of 1 by the following math: tan(180 / 4). Conversely
	*	if you have the bulge you can calculate the encompassing
	*	arc with this formula: angle = 4 * atan(bulge).
	*/
	public void setBulge(double b)
	{
		//////////////// 2.05 /////////////////
		if (Math.abs(b) < 0.01)
		{
			return;
		}
		//////////////// 2.05 /////////////////
		Bulge = b;
		// This object can no longer be rendered as a SvgPolyline
		// now we are a path.
		////////////// 2.04 //////////////
		//objectIsPath = true;
	}


	/** Sets the line weight of the polyline.
	*/
	public void setLineWeight(double lweight)
	{
		// if group code 40 and/or 41 are included (start and end width)
		// then this value is added and means constant line width.
		LineWeight = lweight;
	}



	/** This method over-rides the {@link SvgObject#getFill} to test
	*	for the default polyline fill value of 'none'. If it finds
	*	'fill:none;' it means that the solid takes the layer's
	*	colour attribute. If it is anything else
	*	it means that the illustrator has changed the colour value
	*	of this object to something other from the value of the layer.
	*	@see SvgObject#getFill
	*	@see SvgObject#setFill
	*	@return String <code>fill:&#035;somenum;</code>
	*/
	protected String getFill()
	{
		StringBuffer ret = new StringBuffer();
		// We can add the stroke-width value here because internal or
		// external stylesheets don't reflect line weights of polylines
		// and because they are set by the user, they are guaranteed to
		// be different from the layer's pen weight.
		ret.append(Fill);
		if (LineWeight != 0.01 && LineWeight > 0.0)
		{
			ret.append("stroke-width:"+
				svgUtility.trimDouble(LineWeight * svgUtility.Units()) +
				";");
		}

		return ret.toString();
	}

	/** Sets the polyline to a closed if DXF object is a closed polyline.
	*	@param c Polyline flag (bit-coded); default is 0:
	*	1 = Closed; 128 = Plinegen (polyline code/value pair 70).
	*/
	public void setClosed(int c)
	{
		// called by finding a code/value pair of 70
		Closed = c;
	}

	/** Sets a vertice of a polyline.
	*	@param x double value to populate the Point.y value of
	*	an internal Point.
	*	@param y double. Both values are supplied by EntityProcessor.
	*/
	public void setVertex(double x, double y)
	{
		/*
		*	We couldn't use Anchor because all the functions I tried placed a reference to
		*	Anchor on the PolyLinePoints Vector. With this set up we get a new point each time
		*	we call the function. The down side is that we have to pass both values to the function.
		*	I do this in Entity Processor.
		*/
		p = new VertexPoint();	// point to be placed on vector

		p.setX(x);
		p.setY(y);

		if (Bulge != 0.0)
		{
			p.setBulge(Bulge);
			// reset the Bulge so it doesn't get propagated to the next
			// vertex.
			Bulge = 0.0;
		}

		PolyLinePoints.add(p);
		p = null;
	}

	/** sets the x value of this vertex. This method is intended to allow
	*	external implementors to add vertices on the most granular level.
	*/
	public void setVertexX(double x)
	{
		p = new VertexPoint();

		p.setX(x);
	}

	/** sets the y value of this vertex.This method is intended to allow
	*	external implementors to add vertices on the most granular level.
	*/
	public void setVertexY(double y)
	{
		if (p == null)
			throw new NullSvgPointException("SvgPolyLine.setVertexY(): "+
				"attempted to set a Y value on a null VertexPoint.");

		p.setY(y);

		if (Bulge != 0.0)
		{
			p.setBulge(Bulge);
			// reset the Bulge so it doesn't get propagated to the next
			// vertex.
			Bulge = 0.0;
		}

		PolyLinePoints.add(p);
		p = null;
	}





	/** Sets a vertice of a polyline from a pre-existing Point.
	*	This is used by SvgHatch to switch from <B>line</B> mode
	*	to <B>polyline</B> mode. This
	*	can occur in a HATCH pattern where the expected code describes
	*	a line but then we detect a bulge which means, we have to switch
	*	streams and create a polyline. It can only happen in the first
	*	point of a line because we get a bulge with every point description
	*	even if it is 0.0 (no bulge).<BR><BR>
	*
	*	As an aside, this would also allow you to convert a line into
	*	a polyline without any fuss at all. Make a SvgPolyLine object and
	*	just collect all the <B>connected</B> lines together, iterate
	*	through them and pass them to SvgPolyLine with this method.
	*/
	public void setVertex(Point linePoint, double bulge)
	{
		p = new VertexPoint(linePoint);	// point to be placed on vector
		if (Bulge != 0.0)
		{
			p.setBulge(Bulge);
			// reset the Bulge so it doesn't get propagated to the next
			// vertex.
			Bulge = 0.0;
		}
		// we do this now because typically with this method we have
		// already collected a point (group code 10 and 20) and now
		// realize that we have a polyline instead of some other SvgObject.
		// To make ammends we create a VertexPoint quickly and if a bulge
		// was assigned from a previous point collection we apply it, and
		// in any case pass on any bulge value to the next VertexPoint.
		setBulge(bulge);

		PolyLinePoints.add(p);
		p = null;
	}



	/** Returns the description of the polyline as a path in a String format.
	*	The boolean parameter has no effect on a polyline because the
	*	concatination of points is handled internally.
	*	@see SvgGraphicElement#getElementAsPath
	*/
	public String getElementAsPath(boolean fromPreviousPoint)
	{
		// We have to do this because of an assumption we made about
		// polyline arcs. Every VertexPoint carries a bulge; this is
		// a good thing, but for regular polylines the last point's
		// is superfluous. If you account for it there is trouble.
		// The reverse is true for a Polyline hatch because for some
		// inconsistant (historic?) reason, they decided that the last
		// polyline vertex could be left implied with the bulge to
		// that point included in the last VertexPoint. The problem is
		// SvgHatch collects all the VertexPoints but doesn't know about
		// the bulge until it gets a group code 42, by then it's all over
		// so here is what we do:
		// assume that there is an additional point which in fact is
		// the initial point. We add the start point to the PolyLinePoints
		// Vector via the setVertex(double, double) so the last bulge and
		// point get included.

		VertexPoint start = (VertexPoint)PolyLinePoints.firstElement();
		// add the final vertex with a bulge of zero.
		setVertex(start.getPoint(), 0.0);

		return extractPolyLineArcPoints();
	}






	/** This method is called by the toString() method where upon it iterates
	*	over the Vector container of vertices printing out the results with
	*	some formatting for readability in the SVG file.
	*	@return String A formatted series of vertices.
	*/
	protected String extractPolyLinePoints()
	{
		StringBuffer str = new StringBuffer();
		int numPoints = PolyLinePoints.size();
		for (int i = 0; i < numPoints; i++)
		{
			VertexPoint tmpP = (VertexPoint)PolyLinePoints.get(i);
			// place 4 points to a line like Adobe does.
			if ((i % 4) == 0 && i > 0)
				str.append("\n\t");
				// take the polyline points and use Point's
				// special formatting function
			str.append(tmpP.toString());
		}
		return str.toString();
	}


	/** This method is called by the toString() method where upon it iterates
	*	over the Vector container of vertices and bulge points, printing
	*	out the results with some formatting for readability in the SVG file.
	*	@return String A formatted series of vertices.
	*/
	protected String extractPolyLineArcPoints()
	{
		StringBuffer str = new StringBuffer();

		// Move the pen to the first point of the polyline
		VertexPoint start = (VertexPoint)PolyLinePoints.firstElement();
		str.append("M"+start.toString());
		
		// We have some unfinished business with VertexPoints collected. It seems
		// that when the Closed flag is set to one the last vertex is implied to
		// be the first point. Here, if the flag is set, we make a copy of the first
		// vertex, add a bulge if one was set earlier in the process, and add it
		// to the vector of Vertex points already collected.
		if (Closed == 1)
		{
			VertexPoint end = (VertexPoint)start.clone();
			// We do this if the end vertex has a bulge.
			if (Bulge != 0.0)
				end.setBulge(Bulge);
			PolyLinePoints.add(end);
		}
		
		int numPoints = PolyLinePoints.size();
		VertexPoint lastPoint = start;
		VertexPoint nextPoint = null;

		// Iterate over the entire list of remaining points.
		for (int i = 1; i < numPoints; i++)
		{
			nextPoint = (VertexPoint)PolyLinePoints.get(i);
			if (nextPoint.hasBulge())	// No bulge between these vertices.
			{
				str.append("A"+calculateArc(lastPoint,nextPoint));
			}
			else
			{
				str.append("L"+nextPoint.toString());
			}
			lastPoint = nextPoint;

			// place 4 points to a line like Adobe does.
			if ((i % 4) == 0)
				str.append("\n\t");
		}

		return str.toString();
	}



	/** This method performs the internal calculations for the expression
	*	of a polyline arc.
	*/
	protected String calculateArc(VertexPoint a, VertexPoint b)
	{
		double rx, ry;				// X and Y radius
		double x_rotation = 0.0;	// Angle of the arc on the x axis
		int large_arc_flag;			// Greater than, equal to 180 degrees.
		int sweep_flag;				// Which direction do we draw the arc
		double x, y;				// End point.
		double bulge = b.getBulge();// Bulge value for arc calculations.


		// The end point is simple so let's flesh that out.
		x = svgUtility.trimDouble(b.getX());
		y = svgUtility.trimDouble(b.getY());

		// The bulge is the tangent of 1/4 the including angle of the
		// curved segment so, an arc that covers 180 deg has a bulge
		// of 1 by the following math: tan (180 / 4). Conversely
		// if you have the bulge you can calculate the encompassing
		// arc with this formula: angle = 4 * atan(bulge).
		// If the bulge is negative the sweep is reversed.
		if (bulge < 0)
			sweep_flag = 1;
		else
			sweep_flag = 0;

		// If the bulge is greater than or equal to 1 it's large
		if (Math.abs(bulge) >= 1)
			large_arc_flag = 1;
		else
			large_arc_flag = 0;

		// Wow that is four of the seven pieces of data that we need.
		// Now let's calculate the x_rotation.
		// There is a potential divide by zero problem if the angle of
		// rotation ends up being 90 degrees i.e. point a is directly
		// above (or below) point b.
		if (b.getX() - a.getX() != 0.0)
		{
			x_rotation = svgUtility.trimDouble(
				Math.atan(						/* the arc tangent of ... */
					Math.abs(					/* the absolute value of...*/
						(b.getY() - a.getY())	/* the adjacent side */
						/ 						/* divided by... */
						(b.getX() - a.getX())	/* the opposite side. */
					)
				)
			);
		}

		// Now let's figure out the distance between Point a and Point b
		double distAxBx = Math.sqrt(
			Math.abs(
				Math.pow((b.getX() - a.getX()), 2.0) /* x squared */
				+
				Math.pow((b.getY() - a.getY()), 2.0) /* y squared */
			)
		);


		// Now we know that we can calculate the radi
		// The bulge is the tangent of 1/4 the including angle of the
		// curved segment so, an arc that covers 180 deg has a bulge
		// of 1 by the following math: tan(180 / 4). Conversely
		// if you have the bulge you can calculate the encompassing
		// arc with this formula: angle = 4 * atan(bulge).
		double arcAngle = 4 * Math.atan(bulge);

		rx = ry = svgUtility.trimDouble(
			(distAxBx / 2) /* this gives the mid-point of the cord */
			/
			Math.sin(arcAngle / 2)
		);


		StringBuffer sb = new StringBuffer();
		// Now a bulging polyline is an arc segment.
		// arc syntax is A rx ry x-axis-rot large-arc sweep x y
 		sb.append(/*"A"+*/rx+","+ry+" "+x_rotation+" "+
			large_arc_flag+","+sweep_flag+" "+x+","+y);

		return sb.toString();
	}






	// output the entire polyline.
	/*
	*	<polyline fill="none" stroke="blue" stroke-width="10"
	*            points="50,375
	*                    150,375 150,325 250,325 250,375
	*                    350,375 350,250 450,250 450,375
	*                    550,375 550,175 650,175 650,375
	*                    750,375 750,100 850,100 850,375
	*                    950,375 950,25 1050,25 1050,375
	*                    1150,375" />
	*/
	/** Performs the unique calculation required to describe this object as an SVG element.
	*	Here is an example of an object type changing depending on data. If
	*	the polyline is closed it is refered to as a <em>polygon</em> in
	*	SVG but if it's open it's a <em>polyline</em>. The same object as far as
	*	Dxf2Svg but not at all the same for SVG.
	*/
	protected String calculateMyUniqueData()
	{
		StringBuffer SvgPolyLineOut = new StringBuffer();
		////////////// 2.04 //////////////
		//if (objectIsPath == false)
		//{
		//	if (Closed > 0)
		//	{
		//		this.setType("polygon");
		//	}
		//	else
		//	{
		//		this.setType("polyline");
		//	}

		//	SvgPolyLineOut.append(" points=\"");
		//	SvgPolyLineOut.append(extractPolyLinePoints());
		//	SvgPolyLineOut.append("\"");
		//}
		//else	// there is a bulge in one of the vertices.
		//{
			//this.setType("path");
			SvgPolyLineOut.append(" d=\"");
			SvgPolyLineOut.append(extractPolyLineArcPoints());
			SvgPolyLineOut.append("\"");
		//}

		return SvgPolyLineOut.toString();
	}


	protected Object clone()
	{
		SvgPolyLine pl = (SvgPolyLine)super.clone();

		for (int i = 0; i < PolyLinePoints.size(); i++)
		{
			VertexPoint vxp = (VertexPoint)this.PolyLinePoints.get(i);
			VertexPoint tmp = (VertexPoint)vxp.clone();
			pl.PolyLinePoints.add(tmp);
		}

		pl.Bulge 			= this.Bulge;
		////////////// 2.04 //////////////
		//pl.objectIsPath 	= this.objectIsPath;
		pl.Closed 			= this.Closed;
		pl.LineWeight 		= this.LineWeight;
		pl.p				= (VertexPoint)this.p.clone();

		return pl;
	}

	/** Returns the end point of the polyline. Used in animation to
	*	determine if two points share the same space.
	*/
	public Point getEndPoint()
	{
		VertexPoint vp = (VertexPoint)PolyLinePoints.lastElement();
		if (vp != null)
			return vp.getPoint();
		return null;
	}

	/**
	*	Returns the start point of a double ended graphic object, which 
	*	is usually the Anchor. It is a trivial implementation that duplicates 
	*	another method but this method will help keep the abstraction clear
	*	to the author's intent.
	*	@return Point The anchor point or point of origin for the object.
	*/
	public Point getStartPoint()
	{			
		VertexPoint vp = (VertexPoint)PolyLinePoints.firstElement();
		if (vp != null)
			return vp.getPoint();
		return null;
	}
	
	
	/** This method returns the end points of all the segments in this object.
	*	For {@link SvgPolyLine} the entire set of intersection points is 
	*	returned (as references). 
	*	The method does not alter the contents of the argument vector, but
	*	simply adds the points to the end. Start point is added first, end
	*	point second.
	*	@param points A vector for storing the points. If the vector is 
	*	null the method returns; it is upto the caller to manage this.
	*/
	public void getAllSegmentPoints(Vector points)
	{
		if ( points == null )
		{
			return;
		}
		
		for (int i = 0; i < PolyLinePoints.size(); i++)
		{
			VertexPoint vp = (VertexPoint)PolyLinePoints.get(i);
			if (vp != null)
			{
				points.add( vp.getPoint() );
			}
		}  // end for
	}	// end getAllSegmentPoints()
	
	
	
	
	//////////////////////////////////////////////////////////////////
	//						 VertexPoint class						//
	//////////////////////////////////////////////////////////////////
	/** This is a wrapper class for Point that allows the inclusion of
	*	a bulge value which is required to perform routine calculations
	*	for polylines. You may not instantiate a VertexPoint yourself
	*	they are created when you pass a point into a polyline or
	*	its sub-classes.
	*/
	protected final class VertexPoint implements Cloneable
	{
		Point point = new Point(DxfConverterRef);
		private boolean bulge_flag = false;
		private double bulge = 0.0;


		// default, no argument, constructor.
		protected VertexPoint()
		{	}

		// constructor.
		protected VertexPoint(Point p)
		{	p.copyInto(point);	}


		/** Returns the VertexPoint's X value.
		*/
		protected final double getX()
		{	return point.getX();	}

		/** Returns the VertexPoint's Y value.
		*/
		protected final double getY()
		{	return point.getY();	}

		/** Sets the VertexPoint's X value. Note this is
		*	a wrapper for {@link Point#setX} method which
		*	performs all necessary conversion calculations.
		*/
		protected void setX(double x)
		{	point.setX(x);	}

		/** Sets the VertexPoint's Y value. Note this is
		*	a wrapper for {@link Point#setY} method which
		*	performs all necessary conversion calculations.
		*/
		protected void setY(double y)
		{	point.setY(y);	}

		/** Sets the vertex's bulge value if any.
		*	The bulge is the tangent of 1/4 the including angle of the
		*	curved segment so, an arc that covers 180 deg has a bulge
		*	of 1 by the following math: tan(180 / 4). Conversely
		*	if you have the bulge you can calculate the encompassing
		*	arc with this formula: angle = 4 * atan(bulge).
		*/
		protected void setBulge(double b)
		{
			bulge = b;
			bulge_flag = true;
		}

		/** returns the current bulge value for the current vertex.
		*/
		protected final double getBulge()
		{	return bulge;	}

		/** Remove bulge, the reverse of {@link #setBulge}. This unsets
		*	the bulge flag.
		*/
		protected void removeBulge()
		{
			bulge = 0.0;
			bulge_flag = false;
		}

		/** Reports if the bulge flag is set. If true there is a bulge
		*	if false bulge is 0.0.
		*/
		protected boolean hasBulge()
		{	return bulge_flag;	}

		/** Allows the passing of a Point object argument instead
		*	of using the setX() and setY() methods. This method
		*	copies the argument Point's data. It does not use
		*	its reference.
		*/
		protected void setPoint(Point p)
		{	p.copyInto(point);	}

		/** Returns a reference to the VertexPoint's Point.
		*/
		protected Point getPoint()
		{	return point;	}

		/** Makes a copy of the VertexPoint's Point into
		*	the argument Point.
		*/
		protected void getPoint(Point p)
		{
			if (p == null)
				throw new NullSvgPointException("SvgPolyLine.VertexPoint.getPoint(): "+
				"argument point is null. Can't copy into a null object.");
			else
				point.copyInto(p);
		}

		/** Outputs the String value of the VertexPoint. This method
		*	is a trivial wrapper that uses Point's {@link Point#toStringPolyLine}
		*	method.
		*/
		public String toString()
		{
			return point.toStringPolyLine();
		}

		protected Object clone()
		{
			try{
				VertexPoint v 	= (VertexPoint)super.clone();

				v.point 		= (Point)this.point.clone();
				v.bulge_flag 	= this.bulge_flag;
				v.bulge			= this.bulge;

				return v;
			} catch (CloneNotSupportedException e){
				throw new InternalError();
			}
		}
	}	// end of VertexPoint



} // EOF SvgPolyLine