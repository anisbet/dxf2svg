
/****************************************************************************
**
**	FileName:	SvgSpline.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Is the base class for all Svg Objects.
**
**	Date:		October 9, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 9, 2002
**				0.02 - December 10, 2002 Added getElementAsPath() method.
**				0.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.*;					/* For vector handling. */
import dxf2svg.DxfConverter;				// DxfConverter

/**
*	This class encapsulates curves whose Dxf equivilant is
*	the spline object.
*
*	@version	0.02 - December 10, 2002
*	@author		Andrew Nisbet
*/
public class SvgSpline extends SvgGraphicElement
{
	private boolean CloseFLAG 		= false;
	private boolean PeriodicFLAG 	= false;
	private boolean RationalFLAG 	= false;
	private boolean PlanarFLAG 		= false;
	private boolean LinearFLAG 		= false;

	private Vector vCntlPoints;		// Container for the control points
	private Vector vFitPoints;		// Container for the fit points.

	/** Calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context.
	*/
	public SvgSpline(DxfConverter dxfc)
	{
		super(dxfc);
		setType("path");
		setFill("none");
		vFitPoints = new Vector();
		vCntlPoints = new Vector();
	}

	public void addFitPoint(double x, double y)
	{
		Point thisPoint = new Point(DxfConverterRef);
		thisPoint.setX(x);
		thisPoint.setY(y);
		vFitPoints.add(thisPoint);
	}

	public void addControlPoint(double x, double y)
	{
		Point thisPoint = new Point(DxfConverterRef);
		thisPoint.setX(x);
		thisPoint.setY(y);
		vCntlPoints.add(thisPoint);
	}

	/**	This removes the redundant first and last control points as they
	*	are the same as the first and last fit points in a planar and linear
	*	spline.
	*/
	protected void normalizeControlPoints()
	{
		//SAME_POINT				= -1;
		//NOT_LOCAL 				= 0;
		//VERTICALLY_ALIGNED 		= 1;
		//HORIZONTIALLY_ALIGNED 	= 2;
		//LOCAL 					= 3;


		Point aPt = (Point)vCntlPoints.firstElement();
		Point bPt = (Point)vFitPoints.firstElement();
		System.out.println("vCntlPoints.size(): "+vCntlPoints.size());

		if (aPt.testRelationship(bPt) == Point.LOCAL)
		{
			// Remove the item from the array.
			System.out.println("Ok to remove vCntlPoints[0].");
			vCntlPoints.remove(aPt);
		}

		aPt = (Point)vCntlPoints.lastElement();
		bPt = (Point)vFitPoints.lastElement();

		if (aPt.testRelationship(bPt) == Point.LOCAL)
		{
			System.out.println("Ok to remove vCntlPoints[last].");
			vCntlPoints.remove(aPt);
		}

		System.out.println("vCntlPoints.size(): "+vCntlPoints.size());

	}

	/**	Sets the flag that indicates what type of spline we are dealing with.
	*	Spline flag are bit encoded with the following meanings:<BR>
	*	1 = Closed spline.<BR>
	*	2 = Periodic spline.<BR>
	*	4 = Rational spline.<BR>
	*	8 = Planar.	Generic spline.<BR>
	*	16 = Linear (planar bit is also set).<BR>
	*	@param flag integer value of the bit settings.
	*/
	public void setSplineFlag(int flag)
	{
		if ((flag & 1) == 1)
			CloseFLAG = true;
		else if ((flag & 2) == 2)
			PeriodicFLAG = true;
		else if ((flag & 4) == 4)
			RationalFLAG = true;
		else if ((flag & 8) == 8)
			PlanarFLAG = true;
		else if ((flag & 16) == 16)	// Spline has no curve.
			LinearFLAG = true;
	}

	/**
	*	This class encapsultates a spline curve.
	*	This class is incomplete, it needs more research into the conversion
	*	between a mathematical b-spline and a quadradic Bezi&eacute;r curve.
	*	For now it only draws straight lines between the spline points.
	*	It remians unfinished because of the relative rarity of splines in
	*	our files  that we are converting verses the amount of time required
	*	to research and fix the algorithm.
	*/
	protected String calculateMyUniqueData(){
		StringBuffer Output = new StringBuffer();
		Point pt;

		pt = (Point)vFitPoints.firstElement();
		Output.append("d=\"M");
		Output.append(pt.toStringPolyLine());

		for (int i = 1; i < vFitPoints.size(); i++){
			Output.append(" L");
			pt = (Point)vFitPoints.get(i);
			Output.append(pt.toStringPolyLine());
		}

		if (CloseFLAG)
			Output.append(" Z");
		Output.append("\"");

		return Output.toString();
	}

	/** Returns the description of the spline as a path in a String format.
	*/
	public String getElementAsPath()
	{	return "";	}



	/*protected String calculateMyUniqueData()
	**{
		**StringBuffer Output = new StringBuffer();

		**normalizeControlPoints();
		**Output.append("d=\"M");

		**Point pt;
		**Point p0 = new Point(DxfConverterRef);
		**Point p1 = new Point(DxfConverterRef);
		**Point p2 = new Point(DxfConverterRef);

		**Point op0 = new Point(DxfConverterRef);
		**Point op1 = new Point(DxfConverterRef);
		**Point op2 = new Point(DxfConverterRef);
		**Point op3 = new Point(DxfConverterRef);

		**pt = (Point)vFitPoints.get(0);
		**p0.setXUU((int)pt.getX());
		**p0.setYUU((int)pt.getY());

		**pt = (Point)vCntlPoints.get(0);
		**p1.setXUU((int)pt.getX());
		**p1.setYUU((int)pt.getY());

		**pt = (Point)vFitPoints.get(1);
		**p2.setX((int)pt.getX());
		**p2.setY((int)pt.getY());

		**op0 = p0;

		**op1.setXUU(p0.getX() + 2 * (p1.getX() - p0.getX()) /3);
		**op1.setYUU(p0.getY() + 2 * (p1.getY() - p0.getY()) /3);

		**op2.setXUU(p1.getX() + 1 * (p2.getX() - p1.getX()) /3);
		**op2.setYUU(p1.getY() + 1 * (p2.getY() - p1.getY()) /3);

		**op3 = p2;

		**Output.append(op0.toStringPolyLine());
		**Output.append(" C");
		**Output.append(op1.toStringPolyLine());
		**Output.append(op2.toStringPolyLine());
		**Output.append(op3.toStringPolyLine());


		** // From here on is my conjecture...
		**for (int i = 2; i < vFitPoints.size(); i++)
		**{
		**	Output.append(" C");

		**	pt = (Point)vFitPoints.get(i -1);
		**	p0.setXUU((int)pt.getX());
		**	p0.setYUU((int)pt.getY());

		**	pt = (Point)vCntlPoints.get(i -1);
		**	p1.setXUU((int)pt.getX());
		**	p1.setYUU((int)pt.getY());

		**	pt = (Point)vFitPoints.get(i);
		**	p2.setX((int)pt.getX());
		**	p2.setY((int)pt.getY());

		**	op1.setXUU(p0.getX() + 2 * (p1.getX() - p0.getX()) /3);
		**	op1.setYUU(p0.getY() + 2 * (p1.getY() - p0.getY()) /3);
		**	Output.append(op1.toStringPolyLine());

		**	op2.setXUU(p1.getX() + 1 * (p2.getX() - p1.getX()) /3);
		**	op2.setYUU(p1.getY() + 1 * (p2.getY() - p1.getY()) /3);
		**	Output.append(op2.toStringPolyLine());

		**	// the next end point
		**	pt = (Point)vFitPoints.get(i);
		**	Output.append(pt.toStringPolyLine());
		**}

		**if (CloseFLAG)
		**	Output.append(" Z");
		**Output.append("\"");
		**return Output.toString();
	}	*/
}	// End of SvgSpline class.