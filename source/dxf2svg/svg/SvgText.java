
/****************************************************************************
**
**	FileName:	SvgText.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgText class description
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - December 7, 2004 Fixed a bug where very small text that
**				used the SvgFontMetrics object which creates a font object and then
**				measures the font metrics, reports a font size of -0.0 if the specified
**				height is too small. I just replaced it with the original font size in inches
**				(and then converted to px using SvgUtil) and use that as a raw value.
**				1.5 - January 14, 2005 Removed the Wipeout variable. Added conditional
**				processing for CustomTextStyles, getAttributes() method has changed to
**				use Attribute objects and added set fill to override parent classes 
**				setFill() methods and changed the constructor so it initializes 
**				the fill value to black.
**				1.51 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Iterator;	// For animation objects.
import dxf2svg.*;			// converter and preprocessor
import dxf2svg.util.*;		// table styles etc.
import java.util.regex.*;	// for find() method.

/**
*	This class encapsulates a single line of text in SVG. All fonts used in the conversion
*	should be TrueType<SUP>&reg;</SUP> fonts. Any other
*	unknown fonts (not a truetype font) will be substituted with <code>Courier</code>.
*	You may also specify a URL to a specific font. This URL need not be
*	on this system but {@link TableStyles#setPrimaryFontFileName} does test
*	for valid TrueType fonts and includes the URL in the <code>@font-face</code>
*	Cascading Style Sheet.
*	<P>
*	Note: SvgText elements and its descendants, can mutate into &lt;desc&gt; elements if their anchor (insertion)
*	point is outside of the limits rectangle (see {@link dxf2svg.util.LimitsFrame} for more 
*	information).
*
*	@version	1.01 - December 7, 2004
*	@author		Andrew Nisbet
*/


public class SvgText extends SvgObject
{
	// these are the only things that we can get from the DXF
	protected String FontName;			// font name
	protected double FontSize;			// font size
	protected double originalFontSize;	// Original Font size from the dxf in inches.
	protected int HorzJust;				// Left centre or right.
	protected int VertJust;				// baseline, bottom, middle and top.
	protected String Style; 			// The style name of this text.
	protected double Rotation;			// Angle of rotation of text in radians.
	protected double ObliqueAngle;		// angle of text (skew).
	protected double WidthFactor = 1.0;	// Width factor of the text.
	protected TableStyles Style_Table;	// Style table associated with this text.
	protected Point SAPoint;			// Second Alignment Point Optional.
	protected int justification = 0;	// Justification from group pair 71
		// This value only used with Multi line text but here because
		// we don't want to rewrite an almost exact duplicate of getAttributes()
		// method to do this. It may turn out that SvgText can use this later
	protected boolean isInsideDrawingLimits;  // is calculated when the setY() function is called.



	// These values are enums to test for changes in SvgObjects, changes
	// that make these objects unique to the layer they came from in the DXF.
	// We use them in the toString() and getAttributes() methods.
	// These two have sister values in sub-classes but these two
	// are reserved for this class. Don't duplicate the first two
	// numbers in sub-classes.
	//*******	reserved ANY_STYLE	= 0;	*********/
	//*******	reserved COLOUR 	= 1;	*********/
	//*******	reserved FILL 		= 2;	*********/
	//*******	reserved VISIBILITY	= 4;	*********/
	public final static int TEXT_HEIGHT		= 8;
	public final static int FONT_FAMILY		= 16;
	public final static int STROKE			= 32; // Normally stroke is set to none.
		// but for it needs to be included in, alternatively, IN_LINE
		// styles or CSS styles.
		
	// This value is the tolerance allowed between the style table's font size and the
	// SvgText font object's font size. If they are within this percentage of each other
	// consider them the same size and in that case don't output font size on the SvgText
	// object (except INLINE styles), but rather allow the CSS to dictate the font size.
	public final static double TOLERANCE = 3.0;


	// We leave the value as an expression; 1) to show the values used
	// to arrive at the final number and 2) to allow trimming to arbitrary
	// number of decimal points.
	/** This is the magic number that AutoCAD uses for text leading.
	*	It is used as the basis for all calculations of distances
	*	between lines. Any deviation of this value is handled by
	*	a line leading scale factor that ranges from 0.25 - 4.00.
	*	The trouble is, AutoCAD uses strictly ascenders to measure height
	*	and any other text handling application includes the leading as
	*	part of the true height. So multiply this number by the AutoCAD
	*	reported font measure size to get the adjusted scale.
	*/
	public static final double fontScaleFactor = 5.0 / 3.0;

		

	// constructor
	/** Calls the super class' constructor.
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgText(DxfConverter dxfc)
	{
		super(dxfc);
		Style	= "standard";
		isInsideDrawingLimits = true;
	}
	
	

	/**	This method allows the EntityProcessor to create a second
	*	alignment point which will occur if the group codes 72
	*	horizontal justification values, and 73 vertical justification
	*	values, are populated.
	*	@param x value of point
	*	@param y value of point
	*/
	protected void createSecondAlignmentPoint(double x, double y)
	{
		SAPoint = new Point(DxfConverterRef,x,y);
	}

	/** Sets the horizontal justification; options being <b>left</b>,<b>center</b>
	*	and <b>right</b>. There is a complicated relationship between this method
	*	and {link: #setVerticalJustify}. If group code 73, which dictates vertical
	*	justification, is set to '0' or missing then this value takes on the
	*	role of describing, in a more general fashion, whether the text is
	*	<b>left</b>, <b>center</b>, <b>right</b>, <b>aligned</b>, <b>middle</b>
	*	or <b>fit</b>.
	*	@param hjust horizontal justification value.
	*/
	public void setHorizontalJustification(int hjust)
	{
		HorzJust = hjust;
	}

	/** Sets the vertical justification; options being <b>baseline</b>,<b>bottom</b>
	*	<b>middle</b> and <b>top</b>. There is a complicated relationship between this method
	*	and {link: #setHorizontalJustify}. If group code 72, which dictates horizontal
	*	justification.
	*	@param vjust integer horizontal justification value.
	*/
	public void setVerticalJustification(int vjust)
	{
		VertJust = vjust;
	}


	/**
	*	This method sets the skew of the text and is not related to italic.
	*/
	public void setObliqueAngle(double angle)
	{	ObliqueAngle = -svgUtility.trimDouble(angle);	}



	/** Sets the font description. Dxf2Svg uses this as the name for a
	*	styling class.
	*	@param style name of style
	*/
	public void setStyle(String style)
	{	Style = DxfPreprocessor.convertToSvgCss(style);	}


	/** Returns the name of the style converted to CSS format.
	*/
	public String getStyle()
	{
		return Style;
	}


	// populate font size instance data
	/** Sets the font size of the text object.
	*	@param size in AutoCADs current default unit of measure as a double.
	*/
	public void setFontSize(double size)
	{
		// Save the original value for testing with style later.
		originalFontSize = size;
		// This will give us fonts height converted to Svg space units (px).
		// The font compensation factor is used to match AutoCAD's absolute
		// text height value to a svg size, which is slightly smaller cause it
		// measures ascenders not by absolute height.
		// 
		// This factor may not be known at this time so the default is 1.0
		// to make the total effect on the calculation to nothing. It happens
		// like that if setStyle() doesn't get called before setFontSize();
		FontSize = size * svgUtility.Units() * svgUtility.pointsPerPixel();
		// Now divide that value by the Pixels-per-point value to convert
		// the value into points.
		
		// At this point we are just collecting information from the
		// EntityProcessor and cannot be guaranteed that the FontName
		// and FontSize will be populated. This has implications later
		// when we test to see if the <code>font-size</code> attribute
		// is compared with the font's style table to see if the attribute
		// should be included in the CSS or in the element.
	}

	/** Returns the size of font currently set. */
	public double getFontSize()
	{	return FontSize;	}

	// required by wipeout to determine size of wipeout.
	/** Returns the text string portion of a SvgText object to caller.*/
	public String getString()
	{	return content;	}
	


	/** This method allows for the searching of the content of a SvgText element
	*	and through inheritance an SvgMultilineText element.
	*	<P>
	*	The argument pattern is tested against the unformatted content of this
	*	object. Unformatted means before any characters are replaced with 
	*	any character entities.
	*	<P>
	*	Also note that this method finds only the first instance of the pattern
	*	in the content string. Use the {@link #getString} method to capture the 
	*	content and do further comparisons if required.
	*	@return true if the pattern matched this SvgText object's content,
	*	false otherwise.
	*/
	public boolean find(Pattern p)
	{
		Matcher m = p.matcher(content);
		if (m.find())
		{
			return true;	// Found it!
		}
		
		return false;
	}

	/**
	*	This method sets the rotation angle of the text.
	*	@param rotation of text
	*/
	public void setRotation(double rotation)
	{	Rotation = -rotation;	}

	/**
	*	This method sets the width scaling factor of text. It uses the
	*	width factor value directly from the Dxf file.
	*	@param width the width factor of the text
	*/
	public void setWidthFactor(double width)
	{	WidthFactor = width;	}

	/**
	*	Calculates styling rules for an SvgText. Styling rules are a
	*	series of bits that encode what sort of styling an object takes.
	*	In the case of SvgText if the any values have been set on an
	*	entity explicitly it generally means that this is a special case
	*	which contradicts the styling rules set up in the TableStyles
	*	for this text's style.
	*
	*	1 = Fill<BR>
	*	2 = Colour<BR>
	*	4 = Text Height<BR>
	*	8 = Font Family<BR>
	*
	*	All other attributes are included in the text's attributes.
	*	Attributes like oblique angle and width factor do not seem
	*	to work through cascading style sheets so they must remain
	*	in-line.
	*/
	protected void setApplingRules()
	{
		// Let's calculate which of the styling rules differ from
		// those of the layer we belong to. We need to know that
		// to explicitely add unique styling attributes to objects.
		// First reset the value of WhichRulesDiffer so we
		// don't get accumulative results from succesive calls.
		WhichRulesDiffer = 0;

		// Now if the user has selected INLINE_STYLES then do this:
		// Determine if the style is unique to this element and if it
		// is not then populate the field with data from the layer
		// and set all the styling switches to on.
		if ((MAKE_CSS & Dxf2SvgConstants.INLINE_STYLES) == Dxf2SvgConstants.INLINE_STYLES)
		{
			// The fill of an object is not automatically included
			// in styling information even if the styles are set to
			// INLINE_STYLES
			if (Fill.equals(""))
			{
				setFill(SSG.getLayerFill(Layer));
			}
			WhichRulesDiffer += FILL;
			WhichRulesDiffer += TEXT_HEIGHT;
			WhichRulesDiffer += FONT_FAMILY;
			WhichRulesDiffer += STROKE;
			if (SSG.getLayerIsVisible(Layer) == false || this.isVisible() == false)
			{
				WhichRulesDiffer += VISIBILITY;
			}
		}
		else // CSS not IN_LINE
		{
			// If the fill has been set to something other than the layer's
			// colour, set this switch.
			if (DxfPreprocessor.isColourCoercedByLayer() == false && 
			   (Style_Table instanceof CustomTextStyle) == false)
			{
				if (! Fill.equals(""))
				{
					WhichRulesDiffer += FILL;
				}
			}
			// Test for text height.
			if (Style_Table.isStyleTextHeightMatch(originalFontSize) == false)
			{
				WhichRulesDiffer += TEXT_HEIGHT;
			}
			// There is no test for FONT_FAMILY because you can't change
			// a text string's font family independantly of its style. It
			// is a test switch value to determine if it is to be written
			// to the CSS or to be written IN_LINE only. The thinking is
			// to get attributes to populate automatically if IN_LINE
			// CSS is selected it to manually switch all the different style
			// switches manually to on.
			if (SSG.getLayerIsVisible(Layer) == true && this.isVisible() == false)
			{
				WhichRulesDiffer += VISIBILITY;
			}
		}
	}

	/** Sets the fill of the text object.
	*/
	public void setFill(int AcadColorNumber)
	{
		// If colour of '0' is passed it means the object is hidden or
		// on a locked layer.
		if (AcadColorNumber <= 0)
		{
			setObjectVisible(false);
		}
		
		Fill = DxfPreprocessor.getColour(AcadColorNumber);
		fillColourNumber = AcadColorNumber;
	}

	/**
	*	Sets the attributes for a text element.
	*	@see SvgGraphicElement#getAttributes
	*	@return StringBuffer This value will then be used by the {link: #toString}
	*	method. The concatination of a StringBuffer onto another StringBuffer,
	*	as we do in {link: #toString}, was not allowed until Java version 1.4.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer Attribs = new StringBuffer();	// actual attribute string.
		StringBuffer styleAttribs = new StringBuffer(); // Styling info only.
		StringBuffer Transform = new StringBuffer();// tranformation substring
		StringBuffer TmpTrans = new StringBuffer(); // Calculation string for Transform.

		if (Style_Table == null)
		{
			Style_Table = SSG.getStyle(Style);
		}

		// Let's set the font size to it's correct size so that we
		// can compare it with the Style table.
		FontName = Style_Table.getFontName();

		//////////////////// Font-metrics here /////////////////////
		SvgFontMetrics sfm = new SvgFontMetrics(FontName, getFontSize()); // Metrics for this font.
	
		///////////// 0.02 December 7, 2004 
		// If the font metrics are too small to measure read -0.0 returned from sfm.getAscent()
		// then fudge it with the actual value from ACAD (in inches) and convert to pxs and
		// use the raw value for calculations.
		double sfmFontSize = sfm.getAscent();
		if (Math.abs(sfmFontSize) != 0.0)
		{
			FontSize = sfmFontSize;
		}
		///////////// 0.02 December 7, 2004

		// Set the applying rules of differences between the text's
		// unique values and those that differ from the CSS.
		setApplingRules();

		// Debug shows text element's handle.
		if ((DEBUG) && (! SvgObjID.equals("")))
		{
			addAttribute(new Attribute("id",SvgObjID));
		}

		// now we append the class information
		// I assume that it is very important for all objects to know
		// what class they belong to. It may be over-ridden by some
		// intellegence engine later so let's allow maximum flexibility
		// for instance later.
		if (getIncludeClassAttribute() == true)
		{
			// Text we handle differently to layers. We rely more heavily
			// on styling information from the style table because layers
			// don't know anything about fonts. Let's make the class
			// the name of the text style. If it wasn't for colour we
			// could dispense with the layer name and staight substitute
			// class values.
			//setClass(Style_Table.getStyleName());
			//Attribs.append(getClassAttribute());
			// Isn't that the same as...
			addAttribute(new Attribute("class","st"+Style_Table.getStyleName()));
		}

		if (isStyleSet(FONT_FAMILY))
		{
			styleAttribs.append("font-family:"+FontName+";");
			//addAttribute(new Attribute("font-family",FontName));
		}

		if (isStyleSet(TEXT_HEIGHT))
		{
			double fs = svgUtility.trimDouble(FontSize * Style_Table.getCompensationScaleFactor());				
			//addAttribute(new Attribute("font-size", String.valueOf(fs)));
			styleAttribs.append("font-size:"+String.valueOf(fs)+";");
		}

		if (isStyleSet(VISIBILITY))
		{
			//addAttribute(new Attribute("visibility","hidden"));
			styleAttribs.append("visibility:hidden;");
		}

		// All text elements must take a fill, unlike other SvgGraphic
		// Elements. This may change if we want to produce outline text.
		//styleAttribs.append(getFill());
		if (isStyleSet(FILL))
		{
			//addAttribute(new Attribute("fill", Fill));
			styleAttribs.append("fill:"+Fill+";");
		}
		
		if (styleAttribs.length() > 0)
		{
			addAttribute(new Attribute("style", styleAttribs.toString()));
		}

		// Used exclusively (to date) by the sub-class SvgMultiLineText.
		// This is set if EntityProcessor passes a group code 71 - a
		// desciptor that tells the mtext box's attach point, which just
		// results in justification of the text.
		//Attribs.append(getJustify());
		getJustify();

		// If there are any transformations that need to be done
		// do them now.
		getTransformationIfNecessary(Attribs);
		Attribs.append(getAdditionalAttributes());
			
		return Attribs;
	} // end of getAttributes() method.
	


	/**	Calculates and returns a string discribing the justification of
	*	text within an MTEXT object which is has the same result as having
	*	set the 'attachment point' in on an MTEXT box.
	*/
	protected void getJustify()
	{
		switch(justification)
		{
			case 0:	// Top left default
				break;

			case 1:	// Top left default
				break;

			case 2: // Top center
				addAttribute(new Attribute("text-anchor","middle"));
				break;

			case 3: // Top right
				addAttribute(new Attribute("text-anchor","end"));
				break;

			case 4:	// Middle left
				break;

			case 5:	// Middle center
				addAttribute(new Attribute("text-anchor","middle"));
				break;

			case 6:	// Middle right
				addAttribute(new Attribute("text-anchor","end"));
				break;

			case 7:	// Bottom left
				break;

			case 8:	// Bottom center
				addAttribute(new Attribute("text-anchor","middle"));
				break;

			case 9:	// Bottom right
				addAttribute(new Attribute("text-anchor","end"));
				break;

			default:
				System.err.println("SvgText->SvgMultiLineText: unimplemented option.");
				System.err.println("MTEXT box insertion point (justification). "+justification);
				// don't do anything except fall through the switch.

		}	// end switch
	}


	/**
	*	This method calculates the necessary transformation data for
	*	a string. Typically text needs transformation if the text generation
	*	flag indicates that the text should be backwards and or upside down.
	*	Text is also transformed if width factor and or oblique angle are
	*	required.
	*	@param attribString string.
	*/
	protected void getTransformationIfNecessary(StringBuffer attribString)
	{
		boolean TRANSFORM = false;	// default requirement for a transformation.
		StringBuffer Transform = new StringBuffer();// tranformation substring
		StringBuffer TmpTrans = new StringBuffer(); // Calculation string for Transform.
		// Now we handle the transformation sub string buffer and
		// if later append it to the attribs string buffer. We do
		// this so we can add more attributes to the attrib string
		// if we have to be we don't know until we finish the
		// transformation calculations.
		int Gen = Style_Table.getTextGenFlag();
		if ((Rotation != 0.0) || (Gen >= 4))
		{
			// This switch will allow the transform=" string
			// and all tranform data to be appended to the
			// attribute string.
			TRANSFORM = true;
			TmpTrans.append(" rotate(");
			if (Gen >= 4) // This and the '((Gen & 2) == 2)' will handle
			// both the case of 4, mirror y, and 6, mirror x and y.
			{
				TmpTrans.append(svgUtility.trimDouble((180.0 +Rotation))+", ");
			}
			else // the text is just rotated.
			{
				// rotated text.
				TmpTrans.append(svgUtility.trimDouble(Rotation)+", ");
			}
			// Append all the trailing coordinates.
			TmpTrans.append(svgUtility.trimDouble(Anchor.getX())+", ");
			TmpTrans.append(svgUtility.trimDouble(Anchor.getY())+")");
		}

		// Text generation (lr-tb | rl-tb | tb-rl | lr | rl | tb | inherit)
		if ((Gen > 0) && (Gen < 6))	// Text is mirrored in y (backwards).
		{
			TRANSFORM = true;
			// Scaling moves the svg space by not the svgelement. The exact
			// amount is
			double TransX = -(svgUtility.trimDouble(Anchor.getX() * 2));
			TmpTrans.append(" scale(-1,1) translate("+TransX+",0)");
		}

		if (ObliqueAngle == 0.0) // see if the style table has one.
			// but only if one hasn't been set explicitly yet.
			ObliqueAngle = Style_Table.getObliqueAngle();

		// Oblique text handled here.
		if (ObliqueAngle != 0.0)
		{
			TRANSFORM = true;
			// Now the entire plane is skewed so to move it back we have to
			// do some math.
			// X has moved the equivalent of tan(angle)*Ydist.
			// We only do this calculation for a SvgText object. SvgMulti
			// Line objects do the same calculation but for each <tspan>
			// element and NOT the Anchor point. We can't test an object
			// to see if it is an instanceof SvgText because both SvgText
			// and SvgMultiLineText return true. We can't use a negation
			// test because it is an illegal statement. So we have to test on
			// a SvgMultiLineText and do our work in the 'else' statement.
			if (! (this instanceof SvgMultiLineText))
				calculatePointXSkew(Anchor);

			TmpTrans.append(" skewX("+ObliqueAngle+")");
		}

		// This is not necessary for regular text but MultiLine text
		// requires
		if (this instanceof SvgMultiLineText)
		{
			if (WidthFactor == 1.0) // default check Style
				WidthFactor = Style_Table.getWidthFactor();
		}

		// handle width factor here.
		// Width factor is always passed as an attribute unless it is
		// the default 1.0, in which case group code 41 does not appear.
		if (WidthFactor != 1.0)
		{
			TRANSFORM = true;
			double CurrX = Anchor.getX();
			double DeltaX = CurrX / WidthFactor;
			Anchor.setXUU(DeltaX);
			WidthFactor = svgUtility.trimDouble(WidthFactor);
			TmpTrans.append(" scale("+WidthFactor+", 1)");
		}

		if (TRANSFORM == true)
		{
			Transform.append(" transform=\""+TmpTrans+"\"");
			attribString.append(Transform);
		}
	}

	/**
	*	This method calculates the compensating anchor
	*	position of skewed text.
	*	@param point the insertion point of text to be skewed.
	*/
	protected void calculatePointXSkew(Point point)
	{
		// Now the entire plane is skewed so to move it back we have to
		// do some math.
		// X has moved the equivalent of tan(angle)*Ydist.
		//double CurrX = a.getX();
		//double deltaX = Math.tan(Math.toRadians(ObliqueAngle)) *
		//	a.getY();
		//a.setXUU(CurrX - deltaX);
		double x = point.getX();
		// our calculation requires absolute angle
		double angle = Math.abs(ObliqueAngle);
		x += Math.tan(Math.toRadians(angle)) * point.getY();
		point.setXUU(x);
	}

	//////////////// April 8, 2004 /////////////////
	// Over rides SvgObject's setY() to allow for text objects to change 
	// to <desc> if they are outside of the limites frame of the DXF.
	/** Sets the Anchor's y value.
	*/
	public void setY(double y)
	{	
		Anchor.setY(y);
		
		LimitsFrame limits = svgUtility.getLimits();
		if (! limits.contains(Anchor))
		{
			setType("desc");
			isInsideDrawingLimits = false;
		}
	}  // end setY()
	
	

	/** Writes the text element to an Svg element.
	*	If the text string's anchor is outside of limits rectangle of the 
	*	DXF file this method will publish its content into &lt;desc&gt; tags
	*	for inclusion as meta-data in the Svg file. If the text is output
	*	as meta-data, special characters are <B>not</B> converted to their character
	*	entities.
	*/
	public String toString()
	{
		StringBuffer TextOutput = new StringBuffer();
		// find the ampersand and convert it to '&amp;' so the viewer wont choke
		String OutData = svgUtility.replaceCharacterEntities(content);
		if (isInsideDrawingLimits)
		{
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
	
			TextOutput.append("<"+getType());

			// Get all the text descriptive attributes for this text.
			TextOutput.append(getAttributes());
	
			// add location
			TextOutput.append(" "+Anchor.toStringText());
			TextOutput.append(">");


			TextOutput.append(OutData);
			// Now add animation if any.
			if (vAnimationSet != null)
			{
				Iterator itor = vAnimationSet.iterator();
				while (itor.hasNext())
					TextOutput.append("\n\t"+itor.next());
			}		
			TextOutput.append("</"+getType()+">");
		} 
		else // Anchor is outside of the limits frame
		{
			////////////// April 8, 2004 //////////////
			// Move this so we know affor hand whether
			// this is rendered text or <desc>.
			setType("desc");
			TextOutput.append("<"+getType()+">");
			TextOutput.append(OutData);
			TextOutput.append("</"+getType()+">");
		}
		return TextOutput.toString();
	}

	/** Creates a deep copy of this object.
	*/
	protected Object clone()
	{
		SvgText t = (SvgText)super.clone();

		t.FontName 			= this.FontName;
		t.FontSize			= this.FontSize;
		t.originalFontSize  = this.originalFontSize;
		t.HorzJust			= this.HorzJust;
		t.VertJust			= this.VertJust;
		t.Style 			= this.Style;
		t.Rotation			= this.Rotation;
		t.ObliqueAngle		= this.ObliqueAngle;
		t.WidthFactor		= this.WidthFactor;
		t.Style_Table		= this.Style_Table;
		t.SAPoint			= (Point)this.SAPoint.clone();
		t.justification 	= this.justification;
							
		return t;
	}
}