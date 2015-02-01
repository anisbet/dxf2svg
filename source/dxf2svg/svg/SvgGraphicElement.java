
/****************************************************************************
**
**	FileName:	SvgGraphicElement.java
**
**	Project:	Dxf2Svg
**
**	Purpose:
**
**	Date:
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				2.00 - August 14, 2002 Rewrote toString() method, implemented
**				setFill() method, and getLineType() to encapsulate the
**				setting of both the	line type and scale in the same method.
**				2.01 - October 30, 2002 Moved isStyleSet() from SvgGraphicElement
**				to here so it could be used in sub-class SvgText and rearanged
**				the setApplingRules() to place the normal switch checks in
**				the else block of if(CSS == IN_LINE).
**				2.02 - December 10, 2002 Added getElementAsPath() method.
**				2.03 - February 12, 2003 Implemented clone() method.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.Iterator;	// for animation vector if any.
import dxf2svg.*;
import dxf2svg.util.*;

/**
*	A graphic element object is defined here to be an empty class
*	that needs to be sub-classed to be made into a useful SvgOject
*	like SvgArc or Svgline or what ever.<BR><BR>
*
*	This is a super class to all of the graphic objects that can carry
*	a line type. The philosophy is that all SvgObjects inherit
*	basic functionality from SvgObject but the object tree splits after
*	that. Many objects like SvgText and SvgWipeOut do not require
*	any line type handling but graphic elements like SvgLine do.
*	To handle this subset we make an intermediary class that includes
*	the methods to change and query the objects line type.
*
*	@version	2.03 - February 12, 2003
*	@author		Andrew Nisbet
*/

public abstract class SvgGraphicElement extends SvgObject
{
	/*
	*	This is a super class to the graphic objects that can carry
	*	a line type.  The thought is that all objects inherit
	*	basic functionality from SvgObject but the tree splits after
	*	that. Many objects like SvgText and SvgWipeOut do not require
	*	any line type handling but graphic elements line SvgLine do
	*	so to handle those we make an intermediary class that includes
	*	support all of the subclasses required. In this case it is
	*	the methods to change line type and query the line type.
	*/
	protected boolean LINETYPE_BYLAYER = true;	// All lineType() methods too.
	protected String LineType;					// only graphic elements require change of line type
	protected double LineTypeScale = 1.0;		// as the name suggests.default value 1
	// These values are enums to test for changes in SvgObjects, changes
	// that make these objects unique to the layer they came from in the DXF.
	// We use them in the toString() and getAttributes() methods.
	// These two have sister values in sub-classes but these two
	// are reserved for this class. Don't duplicate numbers in sub-classes.
	//*******	reserved ANY_STYLE	= 0;	*********/
	//*******	reserved COLOUR 	= 1;	*********/
	//*******	reserved FILL 		= 2;	*********/
	//*******	reserved VISIBILITY	= 4;	*********/
	public final static int LINETYPE 		= 8;
	public final static int LINETYPESCALE 	= 16;


	/** Calls the super class' constructor.
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgGraphicElement(DxfConverter dxfc)
	{	super(dxfc);	}

	// used for line type scale independent of layer control
	/** Sets the line type scale of the object */
	public void setLineTypeScale(double lts)
	{	LineTypeScale = lts;	}
	
	/** Returns the line type scale of this object.
	*/
	public double getLineTypeScale()
	{	return LineTypeScale;	}

	// used for line type description in dependant of layer control
	/** Sets the line type independently of the layer's line type.*/
	public void setLineType(String lt)
	{	LineType = lt.toUpperCase();	}

	// used for line type description independent of layer control
	/** Returns the line type of the object if it is different from
	*	the layer's line type or, in a special case, where the object
	*	has been changed to a CONTINUOUS line type and the object's
	*	layer is non-CONTINUOUS.
	*/
	protected String getLineType()
	{
		//******************* Warning: ******************/
		// Never call this method before setApplingRules()
		// because it tests WhichRulesDiffer which is set in
		// setApplingRules(). If you do the answer will be zero
		// which is not necessarily true.
		//******************* Warning: ******************/
		// so we have to test to see if the line type is different
		// from this layer's line type AND test to see if the ltScale
		// is different.
		String LineTypeToString = new String();

		if ((WhichRulesDiffer & LINETYPE) > 0)
		{
			// Ok, so the line type differes from the layer but
			// if it is CONTINUOUS we don't need to do anything.
			if (LineType.equals("CONTINUOUS"))
			{
				; //LineTypeToString = "stroke-dasharray:none;";
			}
			else
			{
				// If it is not then return the line type description
				// for this line type through SSG's lookup table.
				LineTypeToString = SSG.getLineType(LineType,LineTypeScale);
			}
		}
		else
		{
			LineTypeToString = SSG.getLineTypeByLayer(Layer,LineTypeScale);
		}

		return LineTypeToString;
	}


	/** Sets the object's layer name. Over-rides super class' {@link SvgElement#setLayer}.
	*	It does the same thing as the super class's but also sets the
	*	line type for the object to its layer's line type. This value
	*	may in-turn be over-written by the EntityProcessor if the line
	*	type has been set to some value other than the layer's line type.
	*	<BR><BR>
	*	Since the layer name is the argument String and almost anything
	*	goes as far as layer naming conventions in AutoCAD. In SVG it is
	*	a different matter. SVG uses this layer name as a 'class' for
	*	styling properties and class name cannot have spaces so we also
	*	replace all the spaces with hyphens.
	*	Do not confuse this with the class name though, this method only
	*	parses out (svg) illegal characters. A layer name could be 'Layer0'
	*	but its class name would then be 'stLayer0'.
	*	@see #setClass
	*	@param laName A layer name as a raw String from the EntityProcessor.
	*/
	public void setLayer(String laName)
	{
		super.setLayer(laName);

		// Now we explicitely set the line type to the layer's line type so
		// if LineType is null. The Entity processor will change this value
		// if it finds another LineType group code.  This will avoid a
		// nasty NullPointerException that is thrown by "equalsIgnoreCase("CONTINUOUS")"
		// when a SvgGraphicElement tries to get its linetype when it has not
		// been set, during the method getAttributes().
		if (LineType == null)
			LineType = SSG.getLineTypeNameByLayer(Layer);
	}


	/**
	*	Calculates styling rules for an SvgGraphicElement.
	*	<BR>
	*	1 = Fill<BR>
	*	2 = Colour<BR>
	*	4 = Line Type Scale<BR>
	*	8 = Line Type
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
		if ((MAKE_CSS & Dxf2SvgConstants.INLINE_STYLES) == 
			Dxf2SvgConstants.INLINE_STYLES)
		{
			// The fill of an object is not automatically included
			// in styling information even if the styles are set to
			// INLINE_STYLES.
			if (! Fill.equals(""))
				WhichRulesDiffer += FILL;
			// Set the colour switch and if the colour value is not set
			// set it with the layers colour number.
			WhichRulesDiffer += COLOUR;
			if (getColour(1) == 0)
				// by calling it by method we also can determine if the 
				// object's visibility should be 'hidden'.
				setColour(SSG.getLayerColourNumber(Layer));

			// Set the LINETYPESCALE switch. The default value for this
			// value is preset to 1.0
			WhichRulesDiffer += LINETYPESCALE;
			WhichRulesDiffer += LINETYPE;
			if (LineType == null)
				LineType = SSG.getLineTypeNameByLayer(Layer);
			// This object may not be visible it depends on colour of
			// itself and colour of the layer so let's test if it is 
			// visible or not. The layer AND/OR the object can be invisible.
			if (SSG.getLayerIsVisible(Layer) == false || this.isVisible() == false)
				WhichRulesDiffer += VISIBILITY;
		}
		else // CSS not IN_LINE
		{
			if (! Fill.equals(""))
				WhichRulesDiffer += FILL;
			if ((getColour(1) > 0) && (getColour(1) <= 256))
				WhichRulesDiffer += COLOUR;
			if (LineTypeScale != svgUtility.getLtScale())
				WhichRulesDiffer += LINETYPESCALE;
			if (! LineType.equalsIgnoreCase(SSG.getLineTypeNameByLayer(Layer)))
				WhichRulesDiffer += LINETYPE;
			if (SSG.getLayerIsVisible(Layer) == true && this.isVisible() == false)
				WhichRulesDiffer += VISIBILITY;
		}
	}





	/**
	*	This method overrides the super classes method to include
	*	functionality to determine which attributes, if any, differ
	*	from the layer that they came from in the dxf file.<BR><BR>
	*
	*	There is a complex interaction of rules for expressing the
	*	correct array of attributes that any one SvgObject requires.<BR><BR>
	*
	*	Below is a list of cases and typical behaviours one can expect:
	*<OL>
	*	<LI> If the <b>-css</b> switch is set to inline, all attributes
	*	are included for each element. (Not implemented yet.)
	*	<LI> If the <b>-css</b> switch is set to declared or external,
	*	the attributes will not be included unless they differ from the
	*	class (layer) to which they belong. This can occur if a user
	*	changes an attribute of an object to something other than the
	*	default of the dxf layer. E.g. if the user changes a line to
	*	green and the layer's colour is red, then in the svg file that
	*	line will include a unique attribute 'style="stroke:green;"'
	*	to override the effects of the class' style description.
	*	<LI> The Svg file uses 'Group by layers' in which case the
	*	SvgObject will allow the enclosing group node to handle the
	*	styling issues with its class attribute, but the previous
	*	case rules apply.
	*</OL><BR><BR>
	*	@return StringBuffer
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer Output = new StringBuffer();

		setApplingRules();

		// if the ObjType is null or unknown and we still want to see it in
		// the SVG for debugging purposes
		if (DEBUG)
			Output.append(getObjID());

		// now we append the class information
		// I assume that it is very important for all objects to know
		// what class they belong to. It may be over-ridden by some
		// intellegence engine later so let's allow maximum flexibility
		// for instance later.
		if (getIncludeClassAttribute() == true)
		{
			setClass();
			Output.append(" ");
			Output.append(getClassAttribute());
		}

		// now we output any styling information depending on whether
		// 1) we need the information because the attributes are INLINE_STYLES.
		// 2) the attribs differ from the layer they are on.
		// We'll handle them one by one so it is clear when we set one
		// or not.
		// So if we have at least one style rule unique or the attribs
		// are INLINE_STYLES

		if (isStyleSet(ANY_STYLE))	// if any style has been set.
		{
			// There is a rare case when a line will end up on a layer whose
			// line type is something other than CONTINUOUS. If an object
			// on that layer, has its line type set to CONTINUOUS and
			// that is the only rule that is different for this object,
			// then we don't need a style tag because all line types in SVG
			// are continuous by default and outputting just the continuous
			// line type definition to a style tag looks like this 'style=" "'.
			// *Note* you can not do a test like equals("") on a null object
			// without throwing a NullPointerException.
			// add the style attribute.
			Output.append(" style=\"");
			if (isStyleSet(COLOUR))
			{
				// if we got here its because our colour is different from
				// the layer's (use the methods rather than instance data
				// because we don't know if the formatting takes place on
				// the set() (how we would like it) or on the get() method
				// because we can't be sure everything we need is ready
				// until we are outputting.
				Output.append("stroke:"+getColour()+";");
			}
			if (isStyleSet(FILL))
			{
				// append a fill if any calls this.getFill() not super.getFill()
				Output.append(getFill());
			}
			if ((isStyleSet(LINETYPE)) || (isStyleSet(LINETYPESCALE)))
			{
				// this handles both linetype and linetype scale uniqueness.
				Output.append(getLineType());
			}
			// if the layer is frozen or the colour is a neg number do this.
			if (isStyleSet(VISIBILITY))
			{
				Output.append("visibility:hidden;");
			}

			// close the style attribute and add a space so we don't
			// have to compensate for this in many of Point's toString()s.
			Output.append("\"");

		}
		
		// this will get rid of elements that have empty style strings.
		if (Output.length() == (" style=\""+"\"").length())
		{
			Output = new StringBuffer();
		}
		
		Output.append(getAdditionalAttributes());
		
		return Output;
	}


	// The super class method sets a fill no matter what. If it wasn't set
	// specifically with (group code 62) then it gets the colour of the
	// layer it came from. However, we over ride this method to circumvent
	// that assumption because several SvgGraphicObjects don't take any
	// fill attribute and the others take one with a value of 'none'.
	// The Objects set their fill when the constructor is called depending
	// on what they want or can set it at any point if they wished (but
	// to date none do).
	/** This method over-rides the method in the super class to allow
	*	SvgObjects, sub-classed from SvgGraphicElement, to control whether
	*	they should have a fill attribute or not.
	*	@return String The current value of the Fill variable, but without
	*	formatting. {@link SvgObject#setFill}
	*
	*/
	protected String getFill()
	{	return Fill;	}


	//	This method is meant as another way to get calculation data out
	//	of an SvgGraphicElement object in the form of a path so that
	//	SvgHatch doesn't have to duplicate that effort.
	/** This method returns this perticular SvgGraphicElement's as path
	*	data. It is called on special SvgObjects that form hatch boundaries
	*	but of course is not restricted to that. One could implement this
	*	method on all sub classes and render everything as paths instead of
	*	Svg elements. It will also come in handy if an element disappears
	*	or stops working in a viewer. In that case output the object as
	*	a path only.<BR><BR>
	*	The boolean parameter controls if this object should be output as a
	*	discreet path or the implementor want to concat the results to
	*	a previous point. On some objects like polylines this value has
	*	no effect but with a line object it has the following meaning:
	*	<BR>
	*	If FALSE the returned value will be a complete desciption of the
	*	object. True would assume there was a previous point and just
	*	output the lineTo coordinate.<BR><BR>
	*	<B>Example</B><BR>
	*	A line with coordinates (1,2) to (3,2) would return the following:<BR>
	*	FALSE '<code><B>M1,2L3,2</B></code>'.<BR>
	*	TRUE '<code><B>L3,2</B></code>'.<BR><BR>
	*
	*	Not all sub classes implement this feature. At the present, those
	*	that can be expressed as HATCHes do implement this method. Calling
	*	the unimplemented method returns an empty string.
	*/
	public String getElementAsPath(boolean fromPreviousPoint)
	{	return "";	}

	/*
	*	This method overrides the super class method which just is a default for
	*	displaying a dxf object that we encounter that Dxf2Svg doesn't recognize
	*	because it hasn't been defined and described for translation into SVG.
	*/
	/** Outputs the SvgObject and its data to Svg elements using the appropriate
	*	style sheet conventions set in Dxf2Svg.
	*	@return String Formatted Svg element.
	*/
	public String toString()
	{
		StringBuffer ObjOutput = new StringBuffer();	// complete Svg element description
		String ObjData = new String();					// Svg element's unique data


		/*
		*	Sometimes Dxf calls an object something which could be described as two different
		*	Svg objects.  The type of Svg object that should be used may not be known until
		*	the object's calculations are complete, or it may change in mid stream depending
		*	on the results of the calculations so we do Svg object's unique data calculation
		*	first.  By then the proper name of the Svg tag will be known and then we can
		*	paste it in place.  Objects that this pertains to: SvgPolyline(polygon, polyline)
		*	and SvgEllipse(ellipse, path).
		*
		*	I have opted for this approach to keep all similar data like Style Sheet info
		*	together in the super class and leave only the unique data and methods to the
		*	sub classes. As far as maintenance goes it may be more confusing for a novice
		*	to this project so I apologize, but it makes keeping the different svg object's
		*	Output appearance and CSS properties easier to manage.
		*/

		// This is a protected method found in all elements that are in the SvgGraphicElements
		// inheritance chain with the exception of SvgGraphicElement as I have no data.

		// If you were to instantiate an SvgGraphicElement as an object
		// and ran this next line it would call the method from the super
		// class. All sub-classes have their own method redefined and so
		// would call those methods instead.
		ObjData = calculateMyUniqueData();

		// now we know what we are dealing with and our object type is known let's put it
		// together

		// Assemble the objects information in a logical and controlled
		// way.
		// The first thing to be output is the opening of the tag
		ObjOutput.append("<");
		// now the type or tag of this object.
		ObjOutput.append(getType());
		// apply the necessary attributes. (Requires Java 1.4 to append
		// a StringBuffer to a StringBuffer.)
		ObjOutput.append(getAttributes());
		// append the geometry data for this object.
		ObjOutput.append(" "+ObjData);
		
		// Now add animation if any.
		if (vAnimationSet != null)
		{
			ObjOutput.append(">");
			Iterator itor = vAnimationSet.iterator();
			while (itor.hasNext())
				ObjOutput.append("\n\t"+itor.next());
			ObjOutput.append("\n</"+getType()+">");
		}
		else	// there are no animation objects...
		{
			// close the element's tag.
			ObjOutput.append("/>");
		}

		return ObjOutput.toString();

	} // end toString()


	protected Object clone()
	{
		SvgGraphicElement sgEle = (SvgGraphicElement)super.clone();

		sgEle.LINETYPE_BYLAYER 	= this.LINETYPE_BYLAYER;
		sgEle.LineType			= this.LineType;
		sgEle.LineTypeScale 	= this.LineTypeScale;

		return sgEle;
	}
}