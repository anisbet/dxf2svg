
/****************************************************************************
**
**	FileName:	TableLineType.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates a line type table object.
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - August 15, 2002 Removed setScale(), added it to the
**				constructor.
**				1.02 - September 5, 2002 Added line to toString()s and
**				toAttributeString() that stops the output of a trailing
**				space in a dash-array attribute value.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;

/**
*	This object, in many ways, is subserviant to the TableLayer object.
*	It basically handles the line patterns found on a particular layer in DXF.
*
*	@version	1.02 - September 5, 2002
*	@author		Andrew Nisbet
*/


public final class TableLineType
{
	// Instance Data
	private String LineTypeName;				// Name of the line type: Continuous etc.
	private String Description;					// descriptive text about the line type: solid line
	private int	EleNum;							// number of line elements; if 0 is continuous
	//private double PatternLength;				// length of the pattern
	private Vector Dashs = new Vector();		// length of space between elements
	private int EIndex = 0;						// iterates over the array of line elements
	private double Scale = 1.0;					// Scale of LineType. Preset for multiplication
	private SvgUtil svgUtility;					// converter utility
	

	// Constructor
	/**
	*	@param pm the {@link ProcessorManager}.
	*/
	public TableLineType( ProcessorManager pm )
	{
		svgUtility = pm.getSvgUtilInstance();
		Scale = svgUtility.getLtScale();
	}

	/** Sets the line type name.*/
	public void setLineTypeName(String name)
	// so there is no confusion over Continuous or CONTINUOUS
	{	LineTypeName = name.toUpperCase();	}

	/** Returns the line type name, usually for referencing as a key to a
	*	hashmap lookup table.
	*/
	public String getLineTypeName()
	{	return LineTypeName;}

	/** Allows a description to be attached to the line type.*/
	public void setDescription(String desc)
	{	Description = desc.toUpperCase();	}

	/** Returns line type description.*/
	public String getDescription()
	{	return Description;	}

	/** Sets the number of pattern elements in a line type,
	*	0 for a continuous line.
	*/
	public void setEleNum(int elenum)
	{	EleNum = elenum;	}

	/** Returns the number of elements in a line's pattern.*/
	public int getEleNum()
	{	return EleNum;	}

	/** Returns a Vector of pattern array.*/
	public Vector getElementArray()
	{	return Dashs;		}

	/** Returns the scale of the line's pattern.*/
	public double getScale()
	{	return Scale;	}

	/** This method packs the text values of the converted pattern into
	*	a Vector for referencing by the toString() method.
	*	@see #toString
	*/
	public void setDashDotSpaceLength(double LEle) // called by code 49
	{
		double  LineElement = LEle;			// a tmp container for manipulating and conversion
		String	tmpStr = new String();

		// some of these values are negative
		// convert them to Svg space size.
		LineElement = Math.abs(LineElement) * svgUtility.Units() * Scale;
		// if we still have a length of 0 (true for dxf dots) then add something to make it show up
		if (LineElement == 0.0)
			LineElement = 1.0;

		// add it to the array
		tmpStr = String.valueOf(LineElement);
		Dashs.add(tmpStr);
	}




	// return a meaningful description of the line type based on the calculations
	// of the above data

	/** Returns an SVG representation of a <code>stroke-dasharray</code>.
	*	to be used in style sheets.
	*/
	public String toString()
	{	return calculateDashArray(Scale, false);	}
	
	/**
	*	This changes the scale of a linetype temporarily. Used when an element has a unique
	*	scale.
	*	@param scale Scale of the pattern.
	*/
	public String toString(double scale)
	{	return calculateDashArray(scale, false);	}

	/** This next method is used when you have the unique case of an object whose lineType
	*	differs from it's layer's lineType. I noticed this with an arc that took all layer
	*	information except line type so when toString() was called it output the wrong string
	*	and the svg was drawn incorrectly (or not at all.)
	*	This will add it when it is the only attribute on an element.
	*/
	public String toAttributeString(double scale)
	{	return calculateDashArray(scale, true);	}

	// Since all toString() methods do the same thing but need different formatting
	// I altered the basic (and private) method so it is the only method that does 
	// any of the linetype calculations and returns them to wrapper methods.
	private String calculateDashArray(double scale, boolean isAttribute)
	{
		// return a meaningful description of the line type based on the calculations
		// of the above data
		StringBuffer LineTypeOutput = new StringBuffer();
		if (EleNum > 0) // ie this is more than just a continuous line
		{
			// looks like 'stroke-dasharray:5 2.5'
			if (isAttribute)
				LineTypeOutput.append(" stroke-dasharray=\"");
			else
				LineTypeOutput.append("stroke-dasharray:");
			for (int i=0;i<Dashs.size();i++)
			{
				String tmp = (String)Dashs.get(i);
				double dash = Double.parseDouble(tmp) * scale;
				dash = svgUtility.trimDouble(dash);
				// Now don't put on an extra space if this is the last
				// element in the dash array description. Makes it more
				// pretty.
				if (i <= (Dashs.size() - 1))
					LineTypeOutput.append(dash+" ");
			}
			if (isAttribute)
				LineTypeOutput.append("\" ");
			else
				LineTypeOutput.append(";");
		} // end if. That is to say that Svg's default linetype is continuous
		// so we don't need to add it. All other line types are not continuous.

		return LineTypeOutput.toString();
	}



}	// end of class TableLineType