
/****************************************************************************
**
**	FileName:	Pen.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsultes a spar standard layer style.
**
**	Date:		March 20, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - March 20, 2003
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.DxfPreprocessor;

// This class encapsulates the pen object from AutoCAD. When the
// DxfPreprocessor starts it sets up a set of (255, no pen zero)
// pens. These pens are the basic pens and their colours for rendering
// AutoCAD files. Each layer created will take a pen object so it
// will have a rendering capability. All pens are editable but are
// kept in a static array. If there are specific pens required they
// new ones can be created and kept by the StyelSheetGenerator for
// one time use.
/**	This class encapsulates an AutoCAD pen object. Pens, like their
*	real world counterparts, have attributes that make them useful.
*	These attributes are line width (line weight or thickness),
*	line colour (or stroke in SVG) and line type. The line type of 
*	a pen controls whether the lines created by the pen have
*	dashes in them.
*	<P>
*	Pens also have general characteristics that describe how all
*	pen lines behave when then meet at end points. Key words that discribe 
*	this behaviour are miter, round and bevel. In addition you may specify
*	how the end of the line ends; whether it is ends with a round end
*	a square end or a butt end, where the line stops dead at the end point.
*/
public final class Pen implements Cloneable
{
	private int penColour;		// The stroke colour produced by this pen.
	private double lineWeight;	// Default line weight 0.010 in.
	private TableLineType lineType;	// Line type this pen draws Default is CONTINUOUS.
	private boolean visible;	// A pen has to have some memory of its previous
		// visibility state. If it gets a signal to change visibility and then a request
		// to change colour, and that colour is a positive integer we have a problem as 
		// one request over-rules another.

	/** Types of line caps for all pens either square, butt or round. */
	public final static int LINECAP_SQUARE 	= 1;
	public final static int LINECAP_BUTT	= 2;
	public final static int LINECAP_ROUND	= 3;
	/** Types of line joins for all pens which can be either miter, round or bevel. */
	public final static int LINEJOIN_MITER 	= 101;
	public final static int LINEJOIN_ROUND	= 102;
	public final static int LINEJOIN_BEVEL	= 103;
	/** Default for both linecap and linejoin setting this will switch off
	* explicit join and cap requests and let the plugin decide. */
	public final static int DEFAULT			= 0;	
		
		
	private static int lineCap 	= DEFAULT;	// Type of line cap (square, butt, round)
	private static int lineJoin = DEFAULT;	// Type of line intersection join type.



	///////////////////////////////////////////////////////////////////////
	//					Constructors
	///////////////////////////////////////////////////////////////////////
	/**	Creates a new pen object with colour and line weight values
	*	and line type of 'CONTINUOUS'.
	*	@throws InvalidColourValueException if argument is out of range.
	*/
	public Pen(int colourNumber, double lineWeight)
	{
		if (colourNumber < -255 || colourNumber > 255)
			throw new InvalidColourValueException(colourNumber);
		this.penColour 	= colourNumber;
		this.lineWeight = Math.abs(lineWeight);
		setVisibility();
	}

	/**	Creates a new pen object with line weight and the
	*	default colour of "#000000" (black) and line type of 'CONTINUOUS'.
	*/
	public Pen(double lineWeight)
	{
		this.penColour 	= 7;				// black
		this.lineWeight = Math.abs(lineWeight);
		setVisibility();
	}

	/**	Creates a new pen object with a specific colour,
	*	default line weight of '0.01' inches
	*	and line type of 'CONTINUOUS'.
	*	@throws InvalidColourValueException if argument is out of range.
	*/
	public Pen(int colourNumber)
	{
		if (colourNumber < -255 || colourNumber > 255)
			throw new InvalidColourValueException(colourNumber);
		this.penColour 	= colourNumber;
		this.lineWeight = 0.01;
		setVisibility();
	}
	
	/**	Creates a new pen object with a specific line type, the
	*	default colour of black and a line weight of '0.01' inches.
	*/
	public Pen(TableLineType lineType)
	{
		this.penColour 	= 7;
		this.lineWeight = 0.01;
		this.lineType	= lineType;		// take the reference to the TableLineType object.
		setVisibility();
	}

	/**	Creates a pen object with default
	*	colour, line weight and line type of 'CONTINUOUS'.
	*	Default colour: "000000" - black.<br>
	*	Default line weight: 0.01 inches.
	*/
	public Pen()
	{
		this.penColour 	= 7;				// black
		this.lineWeight = 0.01;
		setVisibility();
	}




	///////////////////////////////////////////////////////////////////////
	//					Mutator Methods
	///////////////////////////////////////////////////////////////////////
	/** Sets the line type of the Pen's line; leaving this unset results in the
	*	pen having a default line type of CONTINUOUS.
	*	Typical line types are CONTINUOUS, DASHED, HIDDEN etc..
	*	@param lineType This object is created from a line type definition 
	*	within the blocks section of the DXF and are typically created by EntityProcessor.
	*	Passing a TableLineType allows the pen to determine the current drawing's 
	*	line type configuration for this pen. The TableLineType object describes 
	*	the dashes and dot sequences of a line type. If no line type is set for this
	*	type of Pen, the default value is assumed (CONTINUOUS) and no 'stroke-dasharray'
	*	attribute is included.
	*/
	public void setLineType(TableLineType lineType)
	{	this.lineType	= lineType;	}
	
	/**	Sets the line weight of this pen.
	*	All pen line weights are presumed to be in inches.
	*	The default pen weight is 0.01 inches.
	*/
	public void setLineWeight(double lineWeight)
	{	this.lineWeight = Math.abs(lineWeight);	}
	
	/**	Sets the colour of this pen. Normally setting a pen's colour
	*	to a negative number will make the pen invisible. This method
	*	has a sticky visibility attribute in that if the layer was 
	*	visible and we try to set it invisible by sending a negative 
	*	number our efforts will be in vain. Once a Pen is created, to
	*	change the visibility of the pen, you must explicitely call the
	*	{@link #setVisible} method. This is done to over come a short
	*	coming in AutoCAD that allows frozen objects to have positive 
	*	colour numbers, and therefore appearing in the drawing dispite
	*	their being frozen. What was happening is a pen for a layer
	*	was created, the layer turns out to be frozen so the visibility
	*	is turned off, and then the colour is switched to a positive 
	*	value; rendering the previous calculations null.
	*	The default colour of a pen is black (#000000).
	*	@throws InvalidColourValueException if argument is out of range.
	*/
	public void setColour(int colourNumber)
	{	
		if (colourNumber < -255 || colourNumber > 255)
			throw new InvalidColourValueException(colourNumber);
		this.penColour = colourNumber;
		// Since this method can't be invoked until the object is created
		// and we have no way after creation, to check to see if the current
		// state is we check the state and if the pen is invisible we retain
		// that state.
		if (visible == false)	// the object is invisible
			setVisible(false);
		else
			setVisible(true);
	}
	
	/** This method switches the visibility of a Pen on or off.
	*	If a Pen is made invisible, its colour number is negative 
	*	and if visible its colour is positive.
	*	@see #setColour for more information about setting colour values
	*	for Pens.
	*/
	public void setVisible(boolean isVisible)
	{
		// We make a layer invisible by switching its colour to a negative value.
		// We also have to take the absolute value so a negative colour value
		// doesn't get transposed to a positive (visible) value.
		if (isVisible)
			penColour = (int)Math.abs(penColour);
		else
			penColour = -(int)(Math.abs(penColour));
		
		setVisibility();
	}
	
	
	
	///////////////////////////////////////////////////////////////////////
	//					Accessor Methods
	///////////////////////////////////////////////////////////////////////
	
	/**	Returns an unformatted hex string value of colour i.e. '#00FE09'. The string
	*	representation of the colour is a Hex string made up of
	*	the three RGB values (i.e. EF09AF).
	*/
	public final String getColour()
	{	return DxfPreprocessor.getColour(penColour);	}

	/** Returns the colour of the pen as an integer value.
	*/
	public final int getColour(int i)
	{	return penColour;	}

	/**	Returns the line weight of the pen, converted to a String value.
	*/
	public final String getLineWeight()
	{	return String.valueOf(lineWeight);	}

	/**	Returns the line weight of the pen in it's original as a double.
	*	@param d the argument can be any arbitrary, double value.
	*/
	public final double getLineWeight(double d)
	{	return lineWeight;	}
	
	/** Returns the visibility of the pen.
	*/
	public final boolean isVisible()
	{	
		if (penColour <= 0)
			return false;
		return true;
	}

	/**	Returns a string version of the Pen object in the following
	*	convienent form:
	*	<P>
	*	<CODE>"stroke:#000000;stroke-width:0.007in;visibility: ... "</CODE>.
	*/
	public String toString()
	{
		StringBuffer outBuff = new StringBuffer();
		
		// Looks like:
		// "stroke:#000000;stroke-width:0.007in;"
		outBuff.append("stroke:"+DxfPreprocessor.getColour(penColour)+";");
		outBuff.append("stroke-width:"+String.valueOf(lineWeight)+"in;");
		
		// Add the line type if it isn't continuous
		if (lineType != null)	// Output table if not null...
			if (! lineType.getLineTypeName().equals("CONTINUOUS")) // ...and the line type is not continous
				outBuff.append(lineType.toString());
				
		// Now add the visibility
		if (this.isVisible() == false)
			outBuff.append("visibility:hidden;");
			
		// add the join type and line end type if set.
		if (lineCap != DEFAULT)
		{
			outBuff.append("stroke-linecap:");
			outBuff.append(getLineCapType());
			outBuff.append(";");
		}
		if (lineJoin != DEFAULT)
		{
			outBuff.append("stroke-linejoin:");
			outBuff.append(getLineJoinType());
			outBuff.append(";");
		}
		
		return outBuff.toString();
	}

	// This is implemented to allow for deep copy in stead of reference
	// to be put onto the Hashtable.
	public Object clone()
	{
		try{
			Pen pen = (Pen)super.clone();

			pen.penColour	= this.penColour;
			pen.lineWeight	= this.lineWeight;
			pen.lineType	= this.lineType;	// Just copy the reference 
				// because TableLineType is not, itself, cloneable. No 
				// deep copy required.

			return pen;
		} catch (CloneNotSupportedException e){
			throw new InternalError();
		}
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////
	//					Private Methods
	///////////////////////////////////////////////////////////////////////
	// Used to set the current visibility state of this Pen.
	private void setVisibility()
	{	
		if (penColour < 1)
			visible = false;
		else
			visible = true;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////
	//					Static Methods
	///////////////////////////////////////////////////////////////////////
	/** Sets the join type for pens when two lines meet at a corner.
	*	Valid line joins are:
	*<UL>
	*	<LI> {@link #LINEJOIN_MITER}
	*	<LI> {@link #LINEJOIN_ROUND}
	*	<LI> {@link #LINEJOIN_BEVEL}
	*	<LI> {@link #DEFAULT} uses the viewer's default value for line joins.
	*</UL>
	*	<P>All pens for all drawings inherit this value.
	*/
	public static void setLineJoinType(int joinType)
	{	
		switch (joinType)
		{
			case LINEJOIN_MITER:
			case LINEJOIN_ROUND:
			case LINEJOIN_BEVEL:
				lineJoin = joinType;
				break;
				
			case DEFAULT:
				lineJoin = DEFAULT;
				break;
				
			default:
				// don't touch anything.
		}	// end switch
	}


	/** Sets the line cap for pens.
	*	Valid line cap types are:
	*<UL>
	*	<LI> {@link #LINECAP_SQUARE}
	*	<LI> {@link #LINECAP_BUTT}
	*	<LI> {@link #LINECAP_ROUND}
	*	<LI> {@link #DEFAULT} uses the viewer's default value for line caps.
	*</UL>
	*	<P>All pens in all drawings will inherit this value.
	*/	
	public static void setLineCapType(int capType)
	{	
		switch (capType)
		{
			case LINECAP_SQUARE: 	// fall through
			case LINECAP_BUTT:		// fall through
			case LINECAP_ROUND:
				lineCap = capType;
				break;

			case DEFAULT:
				lineCap = DEFAULT;
				break;
				
			default:
				// don't touch anything.
		}	// end switch		
	}
	
	/** Returns a string value of the current line cap type.
	*	Valid line cap types are:
	*<UL>
	*	<LI> {@link #LINECAP_BUTT} default.
	*	<LI> {@link #LINECAP_SQUARE}
	*	<LI> {@link #LINECAP_ROUND}
	*</UL>
	*/
	public static String getLineCapType()
	{	
		switch (lineCap)
		{
			case LINECAP_SQUARE:
				return "square";
			case LINECAP_ROUND:
				return "round";
			default:
				return "butt";
		}
	}
	
	/**	Returns a string value of the current line join type.
	*	Valid line joins are:
	*<UL>
	*	<LI> {@link #LINEJOIN_MITER} default
	*	<LI> {@link #LINEJOIN_ROUND}
	*	<LI> {@link #LINEJOIN_BEVEL}
	*</UL>
	*/
	public static String getLineJoinType()
	{	
		switch (lineJoin)
		{
			case LINEJOIN_ROUND:
				return "round";
			case LINEJOIN_BEVEL:
				return "bevel";
			default:
				return "miter";		
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////
	//					Enclosed Class(es)
	///////////////////////////////////////////////////////////////////////	
	/**	This exception gets thrown if an argument colour value is out of 
	*	range. Out of range values are those that are less than -255 or 
	*	greater than (+)255. This range of values may change in the next release
	*	of the DXF specification (2004).
	*/
	protected class InvalidColourValueException extends RuntimeException
	{
		protected InvalidColourValueException(int colourNumber)
		{
			System.err.println("The colour value of: '"+colourNumber+
			"' is out of range. The valid range for colours is -255 <= number <= 255"+
			" as of DXF specification release: 2002.");
		}
	}
}