
/****************************************************************************
**
**	FileName:	SvgHatchPattern.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the elements required to fill the hatch.
**
**	Date:		November 28, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - December 2, 2002
**
**	TODO:		Add methods to handle pattern definitions from SvgHatch
**				class.
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Vector;
import dxf2svg.DxfConverter;		// DxfConverter

/**
*	This class encapsulates a hatch pattern. The hatch pattern is described
*	as a fill pattern in the &lt;defs&gt; section of a Svg file. The pattern
*	is referenced by a URL.
*
*	@author		Andrew Nisbet
*	@version	0.01 - December 2, 2002
*/
public final class SvgHatchPattern extends SvgCollection
{
	private static int serialNo = 0;	// Unique naming value.
	private double width;				// Width of pattern.
	private double height;				// Height of pattern.
	protected double scale = 1.0;		// Hatch Pattern Scale.
	private int numberOfLinesInPattern;	// Number of ...
	private Vector repeatPattern;		// Hold repeat of pattern lines.

	/** The only constructor for this object takes a pattern name as
	*	an argument that will later be used as an ID on the &lt;pattern&gt;
	*	element in the Svg's &lt;defs&gt; element. This pattern is then
	*	referenced as a fill by URL.
	*	@see DxfConverter#addHatchPattern
	*	@throws UndefinedHatchPatternNameException if the argument is
	*	invalid because it is empty.
	*/
	public SvgHatchPattern(DxfConverter dxfc, String name)
	{
		super(dxfc);
		setType("pattern");

		if (name.equals(""))
			throw new UndefinedHatchPatternNameException(name);
		else
			// This objects name is its ID attribute unlike most
			// other objects that use the Dxf handle as an ID.
			setObjID(name);
	}

	/** Returns the current valid serial number for applying to make
	*	a unique name for a hatch pattern. This is required because
	*	patterns can have similar, (but different) values and be called
	*	the same name. The default behaviour is if two identical hatch
	*	patterns are created, only one gets output so avoid name conflict
	*	and superfluous data in the Svg. If the two patterns are the same
	*	except for, say, the scale of the pattern, then we need a new name
	*	for the pattern as AutoCAD doesn't see anything wrong with giving
	*	two patterns with different data the same name. We can't have that
	*	in SVG. To fix the problem a new name is created that basically
	*	adds a unique serial number to the end of the name. This also has
	*	to be done for the corresponding SvgHatch object as well or the
	*	hatch path and hatch pattern won't match URLs. This method returns
	*	the 'synchronized' name of this pattern. This is used by
	*	to synchornize the pattern object's name with the hatch object's
	*	URL reference name.
	*/
	public final String getSychronizedPatternName()
	{
		String name = SvgObjID + "_" + String.valueOf(serialNo);
		setObjID(name);
		// Increment serial for next unique pattern name.
		serialNo++;

		return name;
	}

	/**	Sets the scale of the hatch pattern. Scales in autoCAD are
	*	listed in percentages and appear as a number from 0 - 1. We
	*	use this here in this class to aid in comparing patterns that
	*	have already been defined.
	*/
	public void setScale(double scale)
	{	this.scale = scale;	}


	/** This method returns the bounding box width for the entire pattern.
	*/
	public double getPatternWidth()
	{	return width;	}

	/** This method returns the bounding box height for the entire pattern.
	*/
	public double getPatternHeight()
	{	return height;	}

	/**
	*	This method signals the last of a series of hatch pattern Lines to
	*	be added to the SvgCollection. The integer flags an additional
	*	process to check for the requirement, and if necessary, repeat
	*	the pattern collection. This is required because
	*	most pattern tiles don't have an equal aspect ratio of width to
	*	height. If this is the case the pattern has to be repeated or gaps
	*	will form in the tile. <BR><BR> The special cases for a non repeating
	*	pattern that is a line, two lines symetrically arrange
	*	as with an 'X' or '+' a horizontal pattern of straight lines and a
	*	vertical pattern of straight lines.
	*	<BR><BR>If the argument is not {@link SvgHatch#LAST_PATTERN_LINE}
	*	this method behaves exactly like {@link SvgCollection#addElement(Object)}.
	*/
	public void addElement(SvgObject o, int last)
	{
		this.addElement(o);

		if(patternRepeatRequired(last))
		{
			// create a separate place to hold the new calculate refection
			// values.
			repeatPattern = new Vector();

			int size = SvgElementVector.size();
			for (int i = 0; i < size; i++)
			{
				calculatePatternBoundingBox();

				SvgHatchLine hLine = (SvgHatchLine)SvgElementVector.get(i);
				SvgHatchLine newhLine = (SvgHatchLine)hLine.clone();

				Point base = hLine.getBasePoint();
				Point offset = hLine.getOffsetPoint();


				switch(hLine.getHatchLineDirection())
				{
					case SvgHatchLine.UPPER_RIGHT:
						newhLine.setPatternBasePointX((base.getX() - width),false);
						break;

					case SvgHatchLine.UPPER_LEFT:
						newhLine.setPatternBasePointX((base.getX() + width),false);
						break;

					case SvgHatchLine.LOWER_LEFT:
						newhLine.setPatternBasePointX((base.getX() + width),false);

						break;

					case SvgHatchLine.LOWER_RIGHT:
						newhLine.setPatternBasePointX((base.getX() - width),false);
						break;

					default:
						System.out.println
						(
							"The quadrant that this HatchLine points into "+
							"has not be determined yet."
						);
						break;
				}	// switch

				repeatPattern.add(newhLine);
			}	// for
		} // IF
	}

	private boolean patternRepeatRequired(int check_flag)
	{
		// First value to check is to see if the user has supplied
		// the correct value to ensure the pattern repetition.
		if (check_flag != SvgHatch.LAST_PATTERN_LINE)
			return false;
		// If there is only one element on the Vector we don't need
		if (SvgElementVector.size() == 1)
			return false;

		// We have to also check for non-symmetry.

		return true;
	}

	/**
	*	Returns the pattern collection's attributes as a StringBuffer.
	*	@return StringBuffer of formatted attributes.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer OutputString = new StringBuffer();

		// This is manditory for a pattern as its name is its ID attrib.
		OutputString.append(getObjID());

		// Next apply the class attrib...
		//if (getIncludeClassAttribute() == true)
		//	OutputString.append(getClassAttribute());

		// If stroke width is set to anything thicker the line edges
		// get trimmed by the tile bounding box. The small line weight
		// also seems to scale nicely in the viewer so it is always
		// visible at any scale.
		// March 10, 2004 changed constructor on SvgHatchLine to accomodate this 
		// as a quick fix. Now neither pattern nor hatchline take a style 
		// and so the default path width (thin) prevails.
		//OutputString.append(" style=\"stroke-width:0.05\"");


		// Now we add the patterns origin point.
		OutputString.append(" x=\"0\" y=\"0\"");

		// Now calculate the width and height of the pattern tile.
		calculatePatternBoundingBox();
		OutputString.append(" width=\""+width+"\" height=\""+height+"\"");
		// The final attribute controls the tile so that they always
		// butt up to and ajoin each other. If you use the opposite
		// (patternUnits="objectBoundingBox") you get gaps and tile
		// trimming which is not the goal.
		OutputString.append(" patternUnits=\"userSpaceOnUse\"");
		// The opposite is 'objectBoundingBox' and indicates that the
		// whole pattern is dependant on a specific bounding box's
		// dimensions, so scaling will occur. This value allows
		// all bounding objects to fill with consistant fills.
		OutputString.append(" patternContentUnits=\"userSpaceOnUse\"");
		if (scale != 1.0)
			OutputString.append(" patternTransform=\"scale("+scale+")\"");

		OutputString.append(getAdditionalAttributes());

		return OutputString;
	}

	/** Calculates the bounding box necessary to contain all pattern lines.
	*/
	protected void calculatePatternBoundingBox()
	{
		if (width == 0.0 && height == 0.0)
		{
			int len = SvgElementVector.size();	// length of SvgElementVector
			double[] boxDim = new double[2];
			SvgHatchLine hLine;

			// We cycle through the entire collection and calculate the final
			// bounding box of the entire pattern.
			for (int i = 0; i < len; i++)
			{
				hLine = (SvgHatchLine)SvgElementVector.get(i);

				boxDim = hLine.getBoundingBox();

				if (boxDim[0] > width)
					width = boxDim[0];
				if (boxDim[1] > height)
					height = boxDim[1];
			}
		}
	}

	/** Compares this object to the argument object and returns a boolean
	*	value that indicates whether the two objects are equal. Here
	*	'equals' means that this object and the argument hatch pattern
	*	both have equal data values.
	*/
	public boolean equals(Object o)
	{
		// This is part of the contract of this method that a null
		// object returns false.
		if (o == null){
			if (DEBUG)
				System.out.println("The pattern is null.");
			return false;
		}

		if (this == o){
			if (DEBUG)
				System.out.println("The two test patterns are one-and-the-same.");
			return true;
		}

		if (o instanceof SvgHatchPattern)
			;
		else{
			if (DEBUG)
				System.out.println("The pattern not an instanceof SvgHatchPattern.");
			return false;
		}

		try{
			SvgHatchPattern testHP = (SvgHatchPattern)o;

			if(! this.SvgObjID.equals(testHP.SvgObjID)){
				if (DEBUG)
					System.out.println("The pattern names are different.'"+
						this.SvgObjID +"':'"+testHP.SvgObjID+"'");
				return false;
			}
			if (this.width != testHP.width
				|| this.height != testHP.height){
				if (DEBUG)
					System.out.println("The pattern heights are different.");
				return false;
			}
			if (this.scale != testHP.scale){
				if (DEBUG)
					System.out.println("The pattern's scales are different.");
				return false;
			}
			if (this.numberOfLinesInPattern !=
				testHP.numberOfLinesInPattern){
				if (DEBUG)
					System.out.println("The number of lines in the pattern are different.");
				return false;
			}
		} catch (ClassCastException e){
			if (DEBUG)
				System.out.println("Unable to cast the argument as a SvgHatchPattern.");
			return false;
		}

		return true;
	}

	/**	Overrides ancestor toString() method.
	*/
	public String toString()
	{
		// This method uses the collections to string method
		// and adds the repeatition vector of pattern lines as well.
		StringBuffer outBuff = new StringBuffer();

		outBuff.append(super.toString());

		if (repeatPattern != null)
		{
			// Find the </pattern> end tag in the outBuff
			// and insert this before it.
			String end = "</pattern>";
			StringBuffer newOut = new StringBuffer();
			int pos = outBuff.indexOf(end);

			if (pos < 0)
				System.err.println("SvgHatchPattern: Panic, could not "+
				"find pattern close tag in output buffer (</pattern>).");
			else
				newOut.append(outBuff.substring(0, pos));

			int size = repeatPattern.size();

			for (int i = 0; i < size; i++)
				newOut.append("\t"+repeatPattern.get(i)+"\n");

			newOut.append(end);
			outBuff = new StringBuffer(newOut.toString());
		} // if

		return outBuff.toString();
	}




	/* ******************************************************
	**					Internal classes 				   **
	********************************************************/



	/**	This Runtime Exception gets thrown if the hatch pattern name
	*	is invalid. Such invalidity includes the hatch pattern name
	*	being an empty string, but may also be thrown if the name
	*	contains illegal values.
	*/
	protected class UndefinedHatchPatternNameException
	extends RuntimeException
	{
		protected UndefinedHatchPatternNameException(String name)
		{
			System.err.println("The hatch pattern name is invalid: '"+
				name +"'.");
		}
	}



	/** This class gets thrown if there are two defined hatch patterns
	*	that contain dissimilar data. This could occur if the pattern
	*	<B>ANSI31</B> is called twice but in the second reference some
	*	aspect of the pattern changes, like scale for instance.
	*	It is on the TODO list.
	*/
	protected class HatchPatternNameSpaceConflictException
	extends RuntimeException
	{
		protected HatchPatternNameSpaceConflictException(String name)
		{
			System.err.println(
				"I have encountered a duplicate hatch pattern: '"+name+
				"' that contains dissimilar data to the hatch pattern "+
				" of the same name that is already defined. There is no "+
				" way to clearify which hatch pattern to call."
				);
		}
	}
}
