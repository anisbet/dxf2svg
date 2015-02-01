
/****************************************************************************
**
**	FileName:	SvgMultiLineText.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgMultiLineText captures, encapsulates and converts multi-
**				line text into SVG.
**
**	Date:		November 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 13, 2002
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:		Test.
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Vector;	// Multiple Strings of text.
import dxf2svg.DxfConverter;
import dxf2svg.util.LimitsFrame;

/**
*	This class encapsulate a Svg multi-line text object. All fonts used in the conversion
*	should be TrueType<SUP>&reg;</SUP> fonts. All non-truetype fonts will be
*	substuted with <code>Txt.ttf</code> as of release 0.01 of Dxf2Svg.
*	A URL to a specific font can also be specified. This URL need not be
*	on this system and {@link dxf2svg.util.TableStyles#setPrimaryFontFileName} does not test
*	for valid TrueType fonts and includes the URL in the <code>@font-face</code>
*	Cascading Style Sheet regardless.
*	<P>
*	Note: SvgText elements can mutate into &lt;desc&gt; elements if their anchor point (insertion
*	point) is outside of the limits rectangle (see {@link dxf2svg.util.LimitsFrame} for more 
*	information).
*
*	@version	0.01 - November 13, 2002
*	@author		Andrew Nisbet
*/

public final class SvgMultiLineText extends SvgText
{
	private Vector textVector;
	private double maxLineWidth;		// Width of reference rectangle
	private boolean groupCodeThree = false;// if a group code of three
		// is encountered then content is the last line to be output.
		// for this to occur the group code 1 will contain more than
		// 250 (why not 256?) characters.
	private double lineSpaceFactor = 1.3; // Line spacing factor percentage.
		// The default is 3 on 5 which is 60 percent of the line height.
	private Point[] anchors;		// array of anchors for each line.
		// these are filled out by the TextBounderyFormatter.
	private double rawSize;			// size passed from EntityProcessor
		// group code 40


	/////////////////////////////////////////////////
	//				Constructor
	/////////////////////////////////////////////////
	/** Calls the super class' constructor.
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgMultiLineText(DxfConverter dxfc)
	{
		super(dxfc);
	}


	/////////////////////////////////////////////////
	//				Methods
	/////////////////////////////////////////////////
	public void setJustification(int just)
	{	justification = just;	}

	// populate font size instance data This method overrides the
	// super classes method to allow for the saving of the original
	// raw font size for offsetting the mtext box from the normal insertion
	// point to the upper left hand corner.
	/** Sets the font size of the text object.
	*	@param size in AutoCADs current default unit of measure as a double.
	*/
	public void setFontSize(double size)
	{
		// This will give us fonts height converted to Svg space units (px).
		rawSize = size * svgUtility.Units();
		// Now divide that value by the Pixels-per-point value to convert
		// the value into points.
		FontSize = rawSize * svgUtility.pointsPerPixel();
	}

	/**
	*	Adds a string to the group of string(s) that make up a multi-line
	*	text box.
	*/
	public void addAnotherString(String line)
	{
		if (textVector == null)
			textVector = new Vector();

		textVector.add(line);
	}	// addAnotherString

	/**	This sets a switch to let the object know that there is
	*	more than 250 characters to be displayed and the initial
	*	data string needs to be output last as per the Dxf specification.
	*/
	public void setGroupCodeThree(boolean areThereGroupThreeCodes)
	{	groupCodeThree = areThereGroupThreeCodes;	}

	/**
	*	Sets the width defining rectangle that constrains mtext.
	*	The value is converted to UserUnits.
	*/
	public void setMaxLineWidth(double width)
	{
		// Causes a OutOfMemory Exception if the value does not
		// get set so do that now to an arbitrary value. It will
		// get over-written by setWidth() later, but for dimensions
		// blocks sometimes get a value of 0.0.
		if (width <= 0.0) // which can happen with dimension blocks
			maxLineWidth = 3.0 * svgUtility.Units(); // arbitrarily decide 3".
		else
			maxLineWidth = width * svgUtility.Units();
	}

	/**
	*	Sets rotation angle of text by means of an Acad vector defined by
	*	x and y values gleened from group codes '11' and '21' respectively.
	*	This may over write any current <code>Rotation</code> value
	*	already stored by group code '50', but the DXF spefication says
	*	that the last value set wins.
	*/
	public void setXAxisDirectionVector(double x, double y)
	{
		double rot = y / x;
		rot = Math.toDegrees(Math.atan(rot));
		setRotation(rot);
	}

	/**
	*	This method sets the line spacing or leading factor as a percentage
	*	of the default '3-on-5' line spacing to be applied.
	*	The valid range of values is from 0.25 to 4.00.
	*/
	public void setLineSpacingFactor(double factor)
	{	lineSpaceFactor = (factor * (5.0 / 3.0));	}


	/** Writes the text element to an Svg element. */
	public String toString()
	{
		StringBuffer TextOutput = new StringBuffer();
		
		LimitsFrame limits = svgUtility.getLimits();
		if (limits.contains(Anchor))
		{
			// grab out style table reference.
			Style_Table = SSG.getStyle(Style);
			StringBuffer tmpSb = new StringBuffer(); // used for formatting.
			// first let's turn on our setIncludeClassAttribute() because
			// grouping by layers turns it off and we need to explicitly
			// mention that we are a text object or we will get an unnecessary
			// stroke attribute inherited from the layer's class.
			setIncludeClassAttribute(true);
		
			/*
			*	Syntax for a text in Svg is:
			*	<text id="LTop" class="someclass" x="0" y="18"
			*	fontfamily="helvetica" fontsize="6.766" >the text</text>.
			*/

			// Now create a new TextBounderyFormatter object to format the
			// text to fit the mtext box.
			TextBounderyFormatter tbf = new TextBounderyFormatter();
	
			TextOutput.append("<"+getType());
			// Get all the text descriptive attributes for this text.
			TextOutput.append(getAttributes());
			// Now MTEXT is drawn in a box and its insertion point is the top
			// left corner of the box. To get the location of the text we have
			// to offset the y value of the Anchor by LineHeight in a positive
			// Y direction.
			tbf.offsetAnchor(Anchor);
			//TextOutput.append(" "+Anchor.toStringText());
			TextOutput.append(">");



			// Append all the data chunks (250 chars) onto one StringBuffer
			// for output.
			if (groupCodeThree == false)
			{
				tmpSb.append(content);
			}
			else	// Output the data in content as the remainder of chars
			// left over, over-and-above 250.
			{
				// Paste all of the Strings together on the vector
				for (int i = 0; i < textVector.size(); i++)
				{
					tmpSb.append((String)textVector.get(i));
				}
				// append the leftover text collected from group code 1.
				tmpSb.append(content);
			}


			// This array is each of the split strings as one element.
			// Note the Anchor points are automatically calculated during
			// this function call.
			String[] eachLine = tbf.splitStringBounderies(tmpSb);

			// i is set to 1 to because index 0 of anchors[] is the
			// Anchor point.
			for (int i = 0; i < eachLine.length; i++)
			{
				// Note: don't add any formatting in a text element
				// because it causes justification problems.
				TextOutput.append("<tspan "+anchors[i].toStringText()+
					">"+eachLine[i]+"</tspan>");
			}

			// Print out the object for testing perps.
			if (DEBUG)
				System.out.println(tbf);

			// Close the tag.
			TextOutput.append("</"+getType()+">");
		}
		else // The text appears out side of the limits
		{
			TextOutput.append("<desc>");
			// Append all the data chunks (250 chars) onto one StringBuffer
			// for output.
			if (groupCodeThree == false)
			{
				TextOutput.append(content);
			}
			else	// Output the data in content as the remainder of chars
			// left over, over-and-above 250.
			{
				// Paste all of the Strings together on the vector
				for (int i = 0; i < textVector.size(); i++)
				{
					TextOutput.append((String)textVector.get(i));
				}
				// append the leftover text collected from group code 1.
				TextOutput.append(content);
			}
			TextOutput.append("</desc>");			
		}

		return TextOutput.toString();
	}


	/**
	*	Creates a clone of this object.
	*/
	public Object clone()
	{
		SvgMultiLineText mt	= (SvgMultiLineText)super.clone();

		// mt.textVector 		= (Vector)this.textVector.clone(); //Vector
		// Since that doesn't work we have to do an explicit clone.
		for (int i = 0; i < textVector.size(); i++)
		{
			// Since String is an immutable object we can
			// let the string = the string from the vector
			// without it becoming just a reference.
			String tmpStr = new String((String)textVector.get(i));
			mt.textVector.add(tmpStr);
		}

		mt.maxLineWidth		= this.maxLineWidth;
		mt.groupCodeThree 	= this.groupCodeThree;
		mt.lineSpaceFactor 	= this.lineSpaceFactor;
		mt.rawSize			= this.rawSize;
		// clone the array of points
		int size = anchors.length;
		mt.anchors = new Point[size];
		for (int i = 0; i < size; i++)
		{
			mt.anchors[i] 	= (Point)this.anchors[i].clone();
		}

		return mt;
	}



///////////////////////////////////////////////////////////////////////////
	////////////////// sub class /////////////////////
	/**
	*	This class splits strings breaking lines on word boundaries and
	*	formats the multiline text to fit within the bounding box.
	*/
	protected class TextBounderyFormatter
	{
		private double avgCharWidth = 0.0;	// to get a close guess as to
			// how many characters to put on a line.
		private double StringWidth = 0.0;
		private int charsPerLine = 0;
		private int numberOfLines;			// referenced in splitStringBounderies()
			// and toString()
		private double offset = rawSize;	// Used by offset() only and needs
			// to global so the value is retained for repeat calculations
			// after the first line has done all the hard work.
		private SvgFontMetrics sfm;			// Object to accurately measure text sizes.




		// Constructor
		protected TextBounderyFormatter()
		{
			//	Set the font name as there may have been no reason to
			//	set it before now.
			FontName = Style_Table.getFontName();
			
			sfm = new SvgFontMetrics(FontName,FontSize);
		}






		/**
		*	This method handles the mechanics of offsetting the text by
		*	appropriate distances for tspan elements depending on line
		*	number. This is required because Dxf does not specify where
		*	each line appears, only the first. Line numbers start at 1
		*	not 0. This method is intended to calculate the offset of
		*	the Anchor. This new point location will be applied during
		*	the calculation of the placements of the &lt;tspan&gt; elements.
		*	@throws NullSvgPointException if the argument anchor Point is null.
		*/
		protected void offsetAnchor(Point p)
		{
			if (p == null)
				throw new NullSvgPointException("Tried to calculate the offset of a multiline\n"+
				"text string, but the argument Anchor pointer is null.");

			// Now MTEXT is drawn in a box and its insertion point is the top
			// left corner of the box. To get the location of the text we have
			// to offset the y value of the Anchor by LineHeight in a positive
			// direction.
			// This is also necessary so we don't change Anchor again if
			// this method is called repeatedly.
			if (DEBUG)
				System.out.println("MTEXT box offset from insertion point: "+offset);

			double y = p.getY();
			y += offset;
			p.setYUU(y);		// Sset the Anchors y value.
		}





		/**	Calculates all the point anchors for all the &lt;tspan&gt;
		*	elements and places them on an array for output.
		*	@param p The Anchor point of the first line of text.
		*	@param numLines the number of lines of text in the text box.
		*/
		private void calculateLineOffsets(Point p, int numLines)
		{

			if (p == null)
				throw new NullSvgPointException("Tried to calculate the offset of a multiline"+
				"text string, but the argument Anchor pointer is null.");

			// Calculate the box height so we can move the offset of
			// the anchor to account for MTEXT box insertion point change.
			if (justification >= 4) // some box offsetting is required.
			{
				double tmpY = p.getY();
				double boxHeight = rawSize * numLines * lineSpaceFactor;
				// middle offset by half box height
				if (justification < 7)
					boxHeight /= 2;
				tmpY = tmpY - boxHeight;
				p.setYUU(tmpY);
			}

			anchors = new Point[numLines];

			for (int thisLineNum = 0; thisLineNum < numLines; thisLineNum++)
			{
				Point point = new Point(DxfConverterRef);
				p.copyInto(point);

				double y = point.getY();
				y += offset * thisLineNum * lineSpaceFactor;
				if (DEBUG)
					System.out.println("MTEXT line offset point.y = "+y);
				point.setYUU(y);

				// This where you make changes to the x value if, for
				// instance, the style of this text is skewed.
				if (ObliqueAngle != 0.0)
				{
					calculatePointXSkew(point);
				}

				anchors[thisLineNum] = point;
			}

		}






		/**
		*	This method splits the StringBuffer argument into lines. It
		*	does this by taking two different queues from the StringBuffer
		*	content: <BR><BR>
		*	<ul>
		*	<li>Break the line at the sentinal characters '\P'.
		*	<li>Break the line when the content exceeds the line width.
		*	</ul>
		*/
		protected String[] splitStringBounderies(StringBuffer sBuff)
		{
			//System.out.println(sBuff.toString());
			// Prepare all the calculations for future use.
			StringWidth = sfm.getWidth(sBuff.toString());

			avgCharWidth = StringWidth / (double)(sBuff.length());
			// Add a scale factor here to allow for shrinkage.
			avgCharWidth *= WidthFactor;
			// Check to see if maxLineWidth is still 0.0 or this will
			double charsPerLineTmp = maxLineWidth / avgCharWidth;
			//System.out.println("double chars per line best guess: "+charsPerLineTmp);
			// if the number of chars that will fit is more than
			// .65 then put it on the same line. We do this because
			// there is some discrepancy over our calculations that
			// causes the number of characters to be just a bit shorter
			// than autocads calculation. So where AutoCAD puts 19 characters
			// on a line our calculation put only 18 but charsPerLineTmp is 18.87
			if ((charsPerLineTmp % 1) >= 0.7)
				charsPerLine = (int)charsPerLineTmp + 1;
			else
				charsPerLine = (int)charsPerLineTmp;

			// AutoCAD places '\P' as a line break marker if a hard return
			// is requested in text before the text wraps at the maxLineWidth.
			// We are going to replace those with new lines and <tspan> elements.
			//
			// This is a three step process
			// 1) Find all the '\P' hard returns, take note of location and delete
			// 2) Make a second pass over the string to see if any of the
			// hard returns are more than a line width distance from each
			// other and if they are insert another line break
			// (BreakPointLocation) at the position of the last space.
			// 3) Now that is done we know how many lines of text will be
			// produced and we can now start packing the String[] Out with
			// the substrings extracted using the BreakPointLocations as
			// references.
			//
			////////////////////// Part 1 ////////////////////////////////
			// This vector holds integer objects called BreakPointLocations
			// which just hold an integer value of the index of a point
			// to break the string.
			Vector BreakPoints = new Vector();
			//System.out.println("before: "+sBuff.toString());

			// Add a bounder marker at the beginning of a line
			// or long lines with no '\P' breaks don't get processed
			// in the next for loop.
			BreakPoints.add(new BreakPointLocation(0));
			// Index or position of '\P' in buffer.
			int pos;
			do{
				pos = sBuff.indexOf("\\P");
				if (pos >= 0){
					BreakPoints.add(new BreakPointLocation(pos));
					sBuff = sBuff.delete(pos,pos +2);
					//System.out.println("Found one at: "+pos + sBuff.toString());
				}
			} while (pos > -1);
			// Add the end of the line as a break or long lines with no
			// breaks won't get processed in the next for loop.
			BreakPoints.add(new BreakPointLocation(sBuff.length()));


			////////////////////// Part 2 ////////////////////////////////
			// analyse the distances between break points to see if other
			// breaks for line length are required.
			// If the distance between two breaks is greater than the
			// maximum number of characters allowed on a line then insert
			// another break at the lastIndexOf(<space>).
			for (int i = 0; i < BreakPoints.size(); i++)
			{
				// get the first break.
				BreakPointLocation current = (BreakPointLocation)BreakPoints.get(i);
				BreakPointLocation next;
				// get the second break.
				try{
					next = (BreakPointLocation)BreakPoints.get(i + 1);
				}
				catch (ArrayIndexOutOfBoundsException ne)	// we went off the end of the vector
				{	break;	}

				//System.out.println("next.getValue() = "+next.getValue()+ "\n>current.getValue() = "+current.getValue());
				if ((next.getValue() - current.getValue()) > charsPerLine)	// add another break
				{
					// To get here the two markers must exceed the predetermined
					// allowable width of the text.
					// Now make a substring of the section of text we are interested in
					// We do this because, when searching for a space we don't
					// want to go looking back into the last line of text.
					String tmpStr = sBuff.substring(
						current.getValue(),
						next.getValue()
					);
					// There is a case when we only have markers at the beginning
					// and end of a line and the line is double or more the allowable
					// window size. If that is the case redefine the substring
					// to be a sub-substring of charsPerLine in length.
					if (tmpStr.length() > charsPerLine)
					{
						tmpStr = tmpStr.substring(0,charsPerLine);
					}
					StringBuffer tmpSb = new StringBuffer(tmpStr);

					// Now we test to see if there are any spaces between the two
					// markers and if so break the line there and delete the 'next'
					// marker as its value will no longer be valid, BUT ONLY IF IT IS
					// NOT A HARD RETURN.
					// (This method was only available in Java 1.4.)
					// Uugh! reusing this variable!
					pos = tmpSb.lastIndexOf(" " /*space char*/);
					if (pos > -1) // a space was found...
					{
						// and add a new marker here
						BreakPoints.add(
							(i +1),
							new BreakPointLocation(
								(current.getValue() +1) + pos)
						); // we add
						// the '1' here so the breaking space doesn't
						// end up at the front of the next line, but
						// rather at the end of the current line
						// where it can't be seen but where it still
						// remains part of the content. Other wise
						// the space becomes an indent.
					}
					else // and add a new marker here
					{
						BreakPoints.add((i +1), new BreakPointLocation(
							current.getValue() + charsPerLine));
					}
				}

				//System.out.println("Line break at: "+current.toString());
			}


			////////////////////// Part 3 ////////////////////////////////
			numberOfLines = BreakPoints.size() -1;
			//System.out.println("Total number of lines: "+numberOfLines);
			String[] Out = new String[numberOfLines];
			for (int i = 0; i < BreakPoints.size(); i++)
			{
				BreakPointLocation current = (BreakPointLocation)BreakPoints.get(i);
				BreakPointLocation next;
				// get the second break.
				try{
					next = (BreakPointLocation)BreakPoints.get(i + 1);
				}
				catch (ArrayIndexOutOfBoundsException ne)	// we went off the end of the vector
				{	break;	}

				Out[i] = sBuff.substring(current.getValue(),next.getValue());


				if (Out[i].endsWith(" "/*space char*/))
				{
					// trim the last character.
					Out[i] = Out[i].substring(0, Out[i].length() -1);
				}
				//System.out.println("This line is: '"+Out[i]+"'");
			}

			// Calculate line offsets from the anchor based on the number
			// of lines of text that we need anchors for.
			calculateLineOffsets(Anchor, numberOfLines);

			return Out;
		}

		public String toString()
		{
			return "\nmaxLineWidth: "+Double.toString(maxLineWidth)+
				"\nStringWidth: "+Double.toString(StringWidth)+
				"\navgCharWidth: "+Double.toString(avgCharWidth)+
				"\nnumber of lines: "+Integer.toString(numberOfLines)+
				"\ncharsPerLineBestGuess: "+Integer.toString(charsPerLine);
		}
	} // TextBounderyFormatter


	/**
	*	This class represents an Integer that can be added to a Vector. The
	*	reason is that Vectors only accept objects as arguments to the add()
	*	method. Objects of this type are used to determine line breaks while
	*	reflowing text.
	*/
	public final class BreakPointLocation
	{
		private int myInt;

		// Constructor
		/** Constructor takes the value to be stored as argument.
		*/
		public BreakPointLocation(int i)
		{	myInt = i;		}

		/** Returns the stored integer value. */
		public int getValue()
		{	return myInt;	}

		/** Returns the stored integer value as a string.*/
		public String toString()
		{	return String.valueOf(myInt); }

	}	// end class BreakPointLocation.

}	// SvgMultiLineText EOF