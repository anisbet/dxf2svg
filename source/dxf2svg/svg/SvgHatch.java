
/****************************************************************************
**
**	FileName:	SvgHatch.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates both the hatch boundery and the hatch fill.
**
**	Date:		November 28, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 28, 2002
**				0.02 - February 14, 2003 Added clone() method.
**				0.03 - November 16, 2003 Changed setSolidFill() so that fill is not
**				propagated but rather takes the fill that is the colour of the layer
**				it comes from. (What happens if the user wants a specific colour?)
**				0.04 - January 26, 2005 Added file name to spline error message in
**				setGroupCode20().
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.*;
import dxf2svg.*;		// DxfConverter

/**
*	SvgHatch encapsulates the Dxf object, HATCH. A hatch is made up of a
*	boundary path and a fill pattern. The boundary path is in turn, made up
*	of one or more sub-objects namely polyline, line, circular arc,
*	elliptical arc or spline in various combinations.
*	<BR><BR>
*	The hatch pattern fill is a collection of SvgHatchLine objects that describe
*	a pattern much like the line type descriptions in {@link dxf2svg.util.TableLineType}.
*	<BR><BR>
*	SvgHatch, upon initialization, enters a machine state and continues
*	until state change cues are received while processing the dxf data.
*	Each state changes the context of the data being collected and dictates
*	the expected data type of the object currently being processed.
*	This is done because a large number group codes are reused throughout
*	a hatch's description.
*	<BR><BR>
*	There are three different types of objects that are required to define
*	a hatch in AutoCAD:
*	<UL>
*		<LI> <B>SvgHatch</B>: an SvgGraphicElement object, whose primary task
*		is to describe the boundary of the hatch as a continuous path.
*		<LI> <B>SvgHatchPattern</B>: is an SvgCollection object, that holds all
*		the SvgHatchLine elements that are used to create the hatch pattern.
*		There is only one SvgHatchPattern for each hatch in a DXF, but the
*		SvgHatchPattern object is passed back to the DxfConverter and kept
*		for outputting in the Svg header. Other SvgHatch objects may re-use
*		the SvgHatchPattern object and SvgHatchPattern objects can determine
*		if they have already been defined.
*		<LI> <B>SvgHatchLine</B>: There may be zero or more of these objects
*		depending on whether the hatch is a solid and, in-turn, the complexity
*		of the pattern being described. A simple slanted line pattern is
*		one line, but other patterns like 'echer' contain 20 or 30. This
*		object is a decendant of SvgLine but unlike its parent object, it
*		may contain dashed lines and angle describing data.
*	</UL>
*
*
*	@author		Andrew Nisbet
*	@version	0.01 - November 28, 2002
*/
public final class SvgHatch extends SvgGraphicElement
{
	//////////////////////////////////////////////////////
	//			Hatch defining members.		 			//
	//////////////////////////////////////////////////////
	////////// Boundary and pattern objects.
	private SvgHatchPattern hatchPatt;	// this Hatch's pattern description
	private Vector[] boundaryArray;		// An array of boundaries
	private int boundaryIndex = -1;		// to iterate over the boundaryArray

	// These are the valid machine states for this object.
	private static final int ELEVATION 	= 0;
	private static final int PLINE		= 1;// part of the boundary state
	private static final int LINE		= 2;// part of the boundary state
	private static final int C_ARC		= 3;
	private static final int E_ARC		= 4;
	private static final int SPLINE		= 5;
	// Pattern state, not strictly required as group codes are all unique.
	private static final int PATTERN	= 6;
	// Seed state collect seed points.
	private static final int SEED		= 7;
	private static final String[] STATES= {
		"ELEVATION",
		"POLYLINE",
		"LINE",
		"CIRCULAR_ARC",
		"ELLIPTICAL_ARC",
		"SPLINE",
		"PATTERN",
		"SEED",
	};

	/**	Indicates last element to be added to the pattern;
	*	invokes special handling of repeat patterns if required.
	*/
	public final static int LAST_PATTERN_LINE = 1024;

	// Set the initial state.
	private int STATE 						= ELEVATION;
	private int numObjectsToCollect			= 0;	// from group code 93.

	// At any one time we can only have one of these going at a time.
	private SvgPolyLine pline 	= null;
	private SvgLine line		= null;
	private SvgArc c_arc		= null;
	private SvgEllipse e_arc	= null;
	private SvgSpline spline	= null;
	//private Point seed		= null;
	//private Vector vSeeds		= null;

	// Name of the pattern could be SOLID or ANSI31 or what have you.
	private String patternName	= null;

	// We save the x value of the control points because SvgSpline expects the 
	// an X and Y value at the same time.
	private double splineX;


	//////////////////////////////////////////////////////
	//			Hatch Constructor methods. 				//
	//////////////////////////////////////////////////////
	public SvgHatch(DxfConverter dxfc)
	{
		super(dxfc);
		setType("path");
		//vSeeds			= new Vector();
	}





	//////////////////////////////////////////////////////
	//			Hatch member methods.		 			//
	//////////////////////////////////////////////////////
	/** Sets the current operating state of this hatch object.
	*	@throws UnknownHatchStateException if an attempt to switch
	*	to an unknown state is made.
	*/
	protected void setState(int state)
	{
		if (state < ELEVATION || state > SEED)
			throw new UndefinedHatchStateException(state, STATE);
		else
			STATE = state;
	}

	/** Returns the current machine state. The machine state for
	*	this object could be one of the following states:
	*	ELEVATION the initial state of the machine. Named after
	*	the first need to change states - the elevation group pair.
	*	The next state is the BOUNDARY state which is defined by
	*	one of the following sub-states: POLYLINE, LINE, CIRCULAR_ARC,
	*	ELLIPTICAL_ARC or SPLINE. The last state is the SEED state
	*	which denotes the collection of seed points for determining the
	*	internal points of a complex boundary.
	*/
	protected int getState()
	{	return STATE;	}

	/** This method overrides the {@link SvgObject#setFill} method so
	*	that repeated calling of this method will concatinate and
	*	accumulate values manditory fill values. Unlike other SvgObjects
	*	fill can be one value or none at all. With SvgHatch there is always
	*	'no stroke' attribute, a url to a pattern fill or solid fill colour
	*	and	a fill-rule.
	*/
	public void setFill(String fill)
	{	Fill = Fill + fill;	}

	/**	Allows for the resetting of the fill attribute. This may become
	*	necessary because there is always a fill attribute for a hatch
	*	object and the fill attribute <I>usually</I> takes a URL to
	*	a hatch pattern. The exception to this case is if the fill is
	*	solid. Then the fill is just a solid with either the layer colour
	*	or a user defined colour. In that case the URL is superfluous and
	*	should be deleted and the attributes that discribe a solid should
	*	be instated.
	*/
	protected void resetFill()
	{	Fill = "";	}

	/** Returns the fill type as a String ready to be used as an
	*	attribute value if one is set returns null otherwise.
	*	Overrides {@link SvgObject#getFill} to allow proper parsing
	*	of the manitory fill attirbutes.
	*	@return String <code>fill:&#035;somenum;</code>
	*/
	protected String getFill()
	{	return Fill;	}


	/** Closes any open object and adds it to the boundary
	*	path vector.
	*/
	protected void closeObjectAddToPath()
	{
		// Close the last collected object (if there is one) and get ready
		// to open a new path since this group code always herolds a new
		// boundary path...
		switch (STATE)
		{
			case PLINE:
				if (pline != null) // ...except if this is the first boundary path.
				{
					boundaryArray[boundaryIndex].add(pline);
					pline = null;
				}
				break;

			case LINE:
				if (line != null)
				{
					boundaryArray[boundaryIndex].add(line);
					line = null;
				}
				break;

			case C_ARC:
				if (c_arc != null)
				{
					boundaryArray[boundaryIndex].add(c_arc);
					c_arc = null;
				}
				break;

			case E_ARC:
				if (e_arc != null)
				{
					boundaryArray[boundaryIndex].add(e_arc);
					e_arc = null;
				}
				break;

			case SPLINE:
				if (spline != null)
				{
					boundaryArray[boundaryIndex].add(spline);
					spline = null;
				}
				break;

			default:
				break;
		} // end switch
	}


	/**	Outputs the entire hatch boundary as a path.
	*/
	public String toString()
	{
		StringBuffer hatch = new StringBuffer();
		
		///////////////////// 0.03 - November 16, 2003 ////////////////
		// This is commented out because, for some reason, if it is mentioned you cannot
		// manipulate a solid hatch fill. We inherit the attributes from the class by
		// from the layer anyway.
		// setIncludeClassAttribute(true);
		///////////////////// 0.03 - November 16, 2003 ////////////////
		/*
		*	Syntax for a hatch in Svg is:
		*	<path style="fill:url(#myFill)"
		*		d="M1.0,1.0A2.0,1.0 0 1 2.3,2.3 ..."/>.
		*/

		hatch.append("<"+getType());

		// Get all the text descriptive attributes for this text.
		hatch.append(getAttributes());
		// path description to follow
		hatch.append(" d=\"");

		// we could do some auto indenting of nested tags in here
		for (int i = 0; i < boundaryArray.length; i++)
		{
			int size = boundaryArray[i].size();
			for (int j = 0; j < size; j++)
			{
				SvgGraphicElement se = (SvgGraphicElement)boundaryArray[i].get(j);
				if (j == 0) // first element off the boundary path.
					hatch.append(se.getElementAsPath(false));
				else
					hatch.append(se.getElementAsPath(true));
			} // while
		}	// for

		hatch.append("\"/>");

		return hatch.toString();
	}






	//////////////////////////////////////////////////////
	//			Hatch attributes setting methods.		//
	//////////////////////////////////////////////////////
	/**	Sets the hatch pattern name which other SvgObjects refer to as a fill.
	*/
	public void setPatternName(String name)
	{
		resetFill();
		patternName = name;
		String fill = "fill:url(#"+patternName+");stroke:none;";
		setFill(fill);
	}

	/**	Sets whether the hatch is a pattern fill or a solid fill.
	*	If the value is true then the hatch is filled with a solid,
	*	if it is false, the appropriate hatch pattern fill is used.
	*	This value is set by group code (70).
	*/
	public void setSolidFill(boolean isSolid)
	{
		if (isSolid)
		{
			resetFill();
			// Set the fill to the colour of the layer.
			///////////////////// 0.03 - November 16, 2003 ////////////////
			//setFill("fill:" + getColour() + ";stroke:none;");
			// This change stops the propigation of colour fill to a solid fill. This 
			// is required because if an object has a fill specifically stated it cannot
			// be manipulated by JavaScript. If is not mentioned it takes the default
			// fill of the layer that it came from and further if the fill on the layer 
			// is not mentioned then, unless the fill is none, the object will be filled
			// the colour of the stroke. By setting this value the illustrator can no
			// longer animate this fill.
			// Similar functionality can be found int SvgSolid.getFill().
			// If the fill is still the default from the super class then just add stroke none
			// because we don't need the fill set (use the layer's).
			//if (Fill.equals("fill:none;"))
			//{
			//	return "stroke:none;";
			//}
			if (! (fillColourNumber == 0 || DxfPreprocessor.isColourCoercedByLayer()))
			{
				System.out.println("setting fill for hatch."+fillColourNumber);
				setFill("fill:" + DxfPreprocessor.getColour(fillColourNumber) + ";");
			}
			setFill("stroke:none;");
			///////////////////// 0.03 - November 16, 2003 ////////////////
		}
		else
		{
			/////////////////// TODO ///////////////////////////
			// We assume the pattern name is populated.
			// This is a good time to initiate a new HatchPattern
			// BUT we want to know if the hatch pattern has already
			// been defined by another object so we don't get multipule
			// definitions.
			/////////////////// TODO ///////////////////////////
			hatchPatt = new SvgHatchPattern(DxfConverterRef, patternName);
		}
	}







	//////////////////////////////////////////////////////
	//			Boundary Path setting methods. 			//
	//////////////////////////////////////////////////////
	/** Sets group code 10 which, depending on the machine state, could
	*	be the x value of a point, the x value of a center point, the
	*	x value of a seed point or the x value of an elevation.
	*/
	public void setGroupCode10(double val)
	{
		switch (STATE)
		{
			case PLINE: // pline edge
				if (pline == null)
					pline = new SvgPolyLine(DxfConverterRef);
				pline.setVertexX(val);
				break;

			case LINE: // line arc
				if (line == null)
					line = new SvgLine(DxfConverterRef);
				line.setX(val);
				break;

			case C_ARC:	// circular arc
				if (c_arc == null)
					c_arc = new SvgArc(DxfConverterRef);
				c_arc.setX(val);
				break;

			case E_ARC: // elliptical arc
				if (e_arc == null)
					e_arc = new SvgEllipse(DxfConverterRef);
				e_arc.setX(val);
				break;

			case SPLINE: // spline edge
				if (spline == null)
					spline = new SvgSpline(DxfConverterRef);
				splineX = val;
				break;

			case SEED: // seed point
				//if (seed == null)
				//	seed = new Point(DxfConverterRef);
				//seed.setX(val);
				break;

			default:	// This can never happen.
				break;
		}
	}

	/** Sets group code 20 which, depending on the machine state, could
	*	be the y value of a point, the y value of a center point, the
	*	y value of a seed point or the y value of an elevation.
	*/
	public void setGroupCode20(double val)
	{
		switch (STATE)
		{
			case PLINE: // pline edge
				pline.setVertexY(val);
				break;

			case LINE: // line arc
				line.setY(val);
				break;

			case C_ARC:	// circular arc
				c_arc.setY(val);
				break;

			case E_ARC: // elliptical arc
				e_arc.setY(val);
				break;

			case SPLINE: // spline edge
				DxfPreprocessor.logEvent("SvgHatch.setGroupCode20(): "+DxfConverterRef.getFileName(), "spline object found.");
				spline.addControlPoint(splineX, val);
				break;

			case SEED: // seed point
				//seed.setY(val);
				//vSeeds.add(seed);
				//seed = null;
				break;

			default:	// This can never happen.
				break;
		}
	}

	/** Sets group code 11 which, depending on the machine state, could
	*	be the x value of an end point of a line or the x value of the
	*	end point of the major axis of an ellipse edge.
	*/
	public void setGroupCode11(double val)
	{
		switch (STATE)
		{
			case LINE: // line arc
				line.setEndPointX(val);
				break;

			case E_ARC: // elliptical arc
				e_arc.setMajorAxisEndPointXUU(val);
				break;

			default:
				break;
		}	// switch
	}

	/** Sets group code 21 which, depending on the machine state, could
	*	be the y value of an end point of a line or the y value of the
	*	end point of the major axis of an ellipse edge.
	*/
	public void setGroupCode21(double val)
	{
		switch (STATE)
		{
			case LINE: // line arc
				line.setEndPointY(val);
				break;

			case E_ARC: // elliptical arc
				e_arc.setMajorAxisEndPointYUU(val);
				break;

			default:
				break;
		}	// switch
	}

	/** Sets group code 42 which, depending on the machine state, could
	*	be the bulge flag on a polyline path or 'weights' on a spline.
	*/
	public void setGroupCode42(double val)
	{
		switch (STATE)
		{
			case PLINE: // Set polyline arc value 42
				pline.setBulge(val);
				break;

			case LINE: // if this is a line and we get a bulge we have
				// to convert the line to a polyline and change states.
				// Since, if there is a bulge it appears after group
				// code 20 we can be sure that there is only one point
				// set within the line so far. The Anchor.
				// Get a copy of the insertion point for the line.
				// create a new polyline object
				pline = new SvgPolyLine(DxfConverterRef);
				// Add the vertices and bulge.
				pline.setVertex(line.getAnchor(),val);
				// set the state.
				setState(PLINE);
				// delete the line so we don't confuse the next line object
				// if any.
				line = null;
				break;

			default:
				break;
		}	// switch
	}

	/** Sets the number of loops (boundary paths) in the pattern.
	*	Set by group code (91).
	*/
	public void setNumberOfPaths(int num)
	{	boundaryArray = new Vector[num];	}


	/** Sets group code 92 which, depending on the machine state, could
	*	mean the boundary path type flag, but it always signals the start
	*	of a new boundary path.
	*	@see #setHatchStyle
	*/
	public void setGroupCode92(int val)
	{
		closeObjectAddToPath();
		// up the index cause we are signaled that we are getting another path.
		boundaryIndex++;
		boundaryArray[boundaryIndex] = new Vector();
	}


	/** Sets the type of edge about to be described. Edge types can be of
	*	the following values:<BR>
	*<OL start=0>
	*	<LI> = polyline
	*	<LI> = line
	*	<LI> = circular arc
	*	<LI> = elliptical arc
	*	<LI> = spline
	*</OL>
	*	@throws UndefinedHatchEdgeException if the edge value is invalid,
	*	unknown or undefined. This could occur with splines as the spline
	*	conversion algorithm is still not developed.
	*/
	public void setEdgeType(int edge)
	{
		// set by group code 72
		// If the edge is set then we need to pack whatever edge it was
		// onto the bondary path and set that object to null and then
		// make the new type.
		closeObjectAddToPath();

		switch (edge)
		{
			case 0:	// polyline
				setState(PLINE);
				pline = new SvgPolyLine(DxfConverterRef);
				break;

			case 1: // Line edge
				setState(LINE);
				line = new SvgLine(DxfConverterRef);
				break;

			case 2: // circular arc
				setState(C_ARC);
				c_arc = new SvgArc(DxfConverterRef);
				break;

			case 3:	// elliptical arc
				setState(E_ARC);
				e_arc = new SvgEllipse(DxfConverterRef);
				break;

			case 4: // spline edge
				//throw new UndefinedHatchEdgeException(edge);
				setState(SPLINE);
				break;

			default:
				throw new UndefinedHatchEdgeException(edge);

		}	// switch

	}	// setEdgeType

	/** This value sets the hatch style or <code><B>fill-rule</B></code>
	*	controlled by group code (75).
	*	Valid values are:<BR>
	*<OL start=0>
	*	<LI> = Odd parity <code><B>fill-rule:evenodd</B></code>;
	*	<LI> = outermost area <code><B>fill-rule:nonezero</B></code>;
	*	<LI> = Ignore style, hatch all <code><B>fill-rule:nonzero</B></code>;
	*</OL>
	*/
	public void setHatchStyle(int style)
	{
		// This value also indicates that we have collected the last
		// object and need to pack it on the last boundary path.
		///////////// 75 and 93 indicate close of path states. ////
		closeObjectAddToPath();

		setState(PATTERN);

		switch (style)
		{
			case 0:
				setFill("fill-rule:evenodd;");
				break;

			case 1:
				setFill("fill-rule:nonzero;");
				break;

			case 2:
				setFill("fill-rule:nonzero;");
				break;

			default:
				setFill("fill-rule:evenodd;");
				break;
		}
	}

	/** Sets group code 40 which, depending on the machine state, could
	*	be the length of the minor axis as a percentage or ratio of the major
	*	axis for an ellipse arc, radius of a
	*	circular arc or knot values for a spline.
	*/
	public void setGroupCode40(double val)
	{
		switch(STATE)
		{
			case C_ARC:
				c_arc.setRadius(val);
				break;

			case E_ARC:
				e_arc.setMinorToMajorRatio(val);
				break;
			
			case SPLINE:
				// set the splines knot value.
				break;
				
			default:
				break;
		}
	}

	/** Sets the start angle of, depending on the state, an elliptical
	*	or circular arc group code (50).
	*/
	public void setStartAngle(double val)
	{
		switch(STATE)
		{
			case C_ARC:
				// for some stupid reason (probably historic) the
				// hatch arc uses accumulated arc angles so some
				// legitimate hatch angles are 908 degrees where
				// the any arc drawn now flips over to zero from
				// 360 degrees.
				double angle = val % 360;
				c_arc.setStartAngle(angle);
				break;

			case E_ARC:
				e_arc.setStartAngle(val);
				break;

			default:
				break;
		}	// switch
	}

	/** Sets the end angle of, depending on the state, an elliptical
	*	or circular arc group code (51).
	*/
	public void setEndAngle(double val)
	{
		switch(STATE)
		{
			case C_ARC:
				double angle = val % 360;
				c_arc.setEndAngle(angle);
				break;

			case E_ARC:
				e_arc.setEndAngle(val);
				break;

			default:
				break;
		}	// switch
	}


	/** Sets group code 73 which, depending on the machine state, could
	*	be the isClosedFlag if we are talking polylines, the
	*	counterClockWise flag of a circular or elliptical arc
	*	or a 'rational'	value if we are talking splines.
	*/
	public void setGroupCode73(int val)
	{
		switch(STATE)
		{
			case C_ARC:	// not used.
				c_arc.setCounterClockwiseFlag(val);
				break;

			case E_ARC:
				e_arc.setCounterClockwiseFlag(val);
				break;

			case SPLINE:
				// Rational
				break;
				
			default:
				break;
		}	// switch
	}

	/** Sets the degree within the context of a spline.
	*/
	//public void setDegree(double val)
	//{		}

	/** Sets the periodic value within the context of a spline.
	*/
	//public void setPeriodic(double val)
	//{		}

	/** Sets the number of knots within a spline.
	*/
	//public void setNumberOfKnots(int num)
	//{		}

	/** Sets the number of control points within a spline.
	*/
	//public void setNumberOfControlPoints(int num)
	//{		}




	/** Sets the number of seed points. This refers to the point(s)
	*	that lie inside the hatch that define the internal bounding
	*	edge of a hatch.
	*/
	public void setNumberOfSeedPoints(int seeds)
	{
		// This also indicates that we have finished collecting the data
		// for the hatch pattern so pass it back to the DxfConverter.
		if (hatchPatt != null)
		{
			// Add the array of pattern lines to the collection.
			//hatchPatt.addElements(patternLinesArray);
			// Pass the collection to DxfConverter for independant
			// output into the Svg header <defs>.
			if (currentHatchLine != null)
			{
				hatchPatt.addElement(currentHatchLine, LAST_PATTERN_LINE);
			}
			DxfConverterRef.addHatchPattern(this, hatchPatt);
		}

		// change state to collect seed points from pattern collection
		// to seed state.
		setState(SEED);
		// set up the vector to hold the seed values.
		//vSeeds = new Vector();
	}



	//////////////////////////////////////////////////////
	//			Pattern Data setting methods. 			//
	//////////////////////////////////////////////////////

	// Patterns are described by lines with dashes in them. SvgHatch
	// uses a SvgCollection (SvgHatchPattern) to contain these lines.
	// All of these methods are called from EntityProcessor.
	//
	// The hatch pattern object is initiallized as soon as we know that
	// we are not dealing with a solid hatch. With that done we need now
	// only create an array of hatch line objects and that discribe the
	// pattern and pass them to the SvgHatchPatternCollection.
	//
	// We know there are no more hatch pattern lines when we switch states
	// to SEED state.

	private SvgHatchLine currentHatchLine = null;	// Reference to an element on array.





	/** Sets the angle of the line pattern from group code (53).
	*/
	public void setPatternLineAngle(double angle)
	{
		// This seems to be the initial point to collect data
		// concerning a line pattern so initialize the SvgHatchLine Object
		// and set the temp hatch line to it as a reference for the other
		// methods.
		if (currentHatchLine != null)
		{
			hatchPatt.addElement(currentHatchLine);
		}

		currentHatchLine = new SvgHatchLine(DxfConverterRef);
		// Because SvgHatchLine is a decendant of SvgGraphicElement
		// we have some obligations to it in regard to describing the
		// layer it belongs to.
		currentHatchLine.setLayer(this.Layer);
		currentHatchLine.setParentPattern(hatchPatt);
		currentHatchLine.setPatternLineAngle(angle);
	}



	/** Sets X value of the patterns base point.
	*/
	public void setPatternBaseX(double x)
	{	currentHatchLine.setPatternBasePointX(x, true);	}



	/** Sets Y value of the patterns base point.
	*/
	public void setPatternBaseY(double y)
	{	currentHatchLine.setPatternBasePointY(y, true);	}



	/** Sets X value of the patterns offset point.
	*/
	public void setPatternLineOffsetX(double offsetx)
	{	currentHatchLine.setPatternLineOffsetX(offsetx, true);	}



	/** Sets Y value of the patterns offset point.
	*/
	public void setPatternLineOffsetY(double offsety)
	{	currentHatchLine.setPatternLineOffsetY(offsety, true);	}



	/** This method sets the number of dashes in a pattern line.
	*/
	public void setNumberOfDashLengthItems(int dashes)
	{	currentHatchLine.setNumberDashs(dashes);	}



	/** Sets the length of the dashe(s). There may be multipule entries.
	*/
	public void setDashLength(double len)
	{	currentHatchLine.setDashLength(len);	}



	/** Sets the scale of the hatch pattern.
	*/
	public void setHatchPatternScale(double scale)
	{	hatchPatt.setScale(scale);	}

	/** Sets the hatch pattern type with one of the following values<BR>
	*<OL start=0>
	*	<LI> = User-defined.
	*	<LI> = Predefined.
	*	<LI> = Custom.
	*</OL>
	*/
	//public void setHatchPatternType(int pattype)
	//{		}


	/** It is unclear what this value refers to.
	*/
	//public void setPixelSize(double size)
	//{		}


	/** SvgHatch elements cannot be cloned because collections cloning
	*	is unsupported.
	*	@throws CloneNotSupportedException
	*/
	protected Object clone()
	{
		try{
			throw new CloneNotSupportedException();
		} catch (Exception e){	// impossible to get here.
			throw new InternalError();
		}
		// no return required because it is an UnreachableStatement.
	}







	//////////////////////////////////////////////////////
	//			Internal Exception classes. 			//
	//////////////////////////////////////////////////////


	/** Gets thrown if we encounter a undefined hatch edge type.
	*/
	protected class UndefinedHatchEdgeException extends RuntimeException
	{
		protected UndefinedHatchEdgeException(int i)
		{
			System.err.println("The hatch edge type: "+i+" has not been defined by "+
			"AutoDesk as of AutoCAD version 2002i or is not rendered by Dxf2Svg.");
		}
	}

	/** This Runtime exception is thrown if the SvgHatch object is coerced
	*	into an unknown state. This could occur if a DXF file has been
	*	manually manipulated or created by a poorly behaving DXF generator.
	*/
	protected class UndefinedHatchStateException extends RuntimeException
	{
		protected UndefinedHatchStateException(int state, int lastState)
		{
			System.err.println("SvgHatch: attempt to enter an undefined state: '"+
				state+"'. Last valid state was: "+
				STATES[lastState]+"("+lastState+").");
		}
	}
}	// end SvgHatch.