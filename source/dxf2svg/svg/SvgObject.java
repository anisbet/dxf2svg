
/****************************************************************************
**
**	FileName:	SvgObject.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Is the base class for all Svg Objects.
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				2.00 - August 14, 2002 Re-wrote most of this class to
**				encapsulate setting of various attributes like 'fill'
**				and values like 'color' so output Strings are pre-processed
**				before they are called by the toString() method. Also added
**				are methods to control which classes SvgObjects belong to.
**				This allows an object to be included in another class or
**				have its class changed completely. The new methods are
**				setClass(String), setClass(), listClass() (not getClass)
**				and resetClass().
**				2.01 - October 10, 2002 Added abstract methods to return
**				end points to an intelligence engine.
**				2.01 - October 30, 2002 Moved isStyleSet() from SvgGraphicElement
**				to here so it could be used in sub-class SvgText.
**				2.02 - December 12, 2002 Removed String arg constructor 'type'.
**				2.03 - February 4, 2003 Changed Anchor.toString() to Anchor.toStringText()
**				in toString() method. Renamed setString() to setContent().
**				2.04 - February 12, 2003 Implemented clone() method.
**				2.10 - May 13, 2003 Added facilities to allow for animation of individual elements.
**				2.11 - April 14, 2004 Added modified setLayer(String laName) method.
**				2.12 - November 17, 2004 Rationalized some redundant code in getFill().
**				2.13 - January 6, 2005 Made modification to getFill() to call SSG.getLayerFill()
**				in stead of SSG.getLayerColour() which doesn't allow unique fill settings
**				that are permissable in the config.d2s.
**				2.14 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;			// DxfConverter and NullDxfConverter
import java.util.*;			// for storing Animation objects.
import dxf2svg.util.Attribute;// for cloning the super classes attribute Vector.
import dxf2svg.animation.*;	// animation stuff
/**
*	This class encapsulates the basic SVG object.<BR><BR>
*	While SvgObject is the base class for all other Svg objects it
*	can also be instantiated as an object of 'unknown' type. This
*	allows new or undefined Acad objects to be written to the SVG
*	file providing a sort of debug feature. The object appears as
*	a text string of the name of the object at the location it would
*	have appeared in the DXF, but only if DEBUG is set. If it is not
*	set you get a warning on the command line about an un handled object.
*
*	@version	2.10 - May 13, 2003
*	@author		Andrew Nisbet
*/

public class SvgObject extends SvgElement
/*
**	We could have made this an interface and derived all the other
**	dxf objects from it as if it were a super class but I have decided
**	that there is merit to a toString() method for an UNDEFINED svg
**	object.  For one thing it will place text in situ on a svg for
**	any undefined objects allowing you to debug elements that have
**	no method of handling any where else.
**	One disadvantage of this approach is that you have to redefine
**	each and every method of the interface within each of the
**	derived classes or else you must declare the sub-object abstract.
**	and then no more deriving from that sub-class.
*/
{
	protected Point Anchor;					// insertion point or first point of object.
	protected boolean COLOUR_BYLAYER = true;// all colour methods know if they were set by special rules
	protected boolean OBJECT_TAKES_FILL = true;// If SvgObject can take a fill (text) then is it bylayer?
	protected String content = new String();// this holds any string data from the dxf
	protected String Fill = new String();	// Fill colour if requested. For hatches and text
	protected String Colour = new String();	// any object can have a colour different to the layer that it is on.
	protected int ColourNumber = 0;			// remember an argument colour number for processing by getColour()
	protected int fillColourNumber = 0;		// Added to facilitate application of unique colours to fills.
	protected int WhichRulesDiffer = 0;		// How an object's attibs differ from layer's attribs
	protected Vector vAnimationSet;


	// These values are enums to test for changes in SvgObjects.
	// We use them in the toString() and getAttributes() methods.
	// These have sister values in sub-classes but these two
	// are reserved for the super class. If you need more values for
	// testing, check SvgObjects sub-class tree nodes (either SvgText or
	// SvgGraphicElement) for other pre-defined values so you don't
	// duplicate numbers in sub-classes.
	public final static int ANY_STYLE	= 0;
	public final static int COLOUR 		= 1;
	public final static int FILL 		= 2;
	public final static int VISIBILITY	= 4;
	// reserved by SvgGraphicElement
	//*****	 reserved LINETYPE 		= 8; ******/
	//*****	 reserved LINETYPESCALE = 16; ******/


	// constructor will populate object type
	/** Queries Dxf2Svg for implementation switch settings.
	*	@see DxfPreprocessor#verboseMode
	*	@see DxfPreprocessor#debugMode
	*	@see DxfPreprocessor#cssMode
	*	@param dxfc dxf conversion context
	*/
	public SvgObject(DxfConverter dxfc)
	{
		super();
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: "+
				this.getClass().getName());
		DxfConverterRef		= dxfc;
		SSG 				= DxfConverterRef.getStyleSheetGenerator();
		svgUtility			= DxfConverterRef.getSvgUtil();
		
		// determine object type.
		setType("text");
		// This will turn off the class attribute in favour
		// of inheriting the class from the 'layer' or SvgCollection object.
		// It is controllable from SvgElement so we could control
		// an object's class readily.
		// Default to true for outputting entities that are grouped by
		// classes rather than by the <g> tags.
		setIncludeClassAttribute(true);
		// Set the anchor point
		Anchor = new Point(DxfConverterRef);
	}



	/**
	*	This method, in SvgObject, formats basic attributes necessary
	*	to display a basic message in the Svg file using default styling.
	*	@return StringBuffer Includes object's name and basic styling
	*	to make the text to appear in red courir 12pt.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer Output = new StringBuffer();
		// no object in svg can have the same id so if it isn't defined
		// don't include it.
		if (SvgObjID != null)
		{
			Output.append(" id=\""+SvgObjID+"\"");
		}

		Output.append(" style=\"font-family:courier;font-size:10;stroke:none;fill:red;\"");
		Output.append(getAdditionalAttributes());
		
		return Output;
	}


	// all objects can do the following
	/** Sets the 'anchor' point for an object.
	*	<BR><BR>
	*	This is the 10 and 20 group codes on an AutoCAD object.
	*/
	public void setInsertionPoint(Point ipt)
	{	Anchor = ipt;	}

	/** Sets the Anchor's x value. The anchor equates to the insertion
	*	point or initial handle on most objects.
	*/
	public void setX(double x)
	{	Anchor.setX(x);	}

	/** Sets the Anchor's y value.
	*/
	public void setY(double y)
	{	Anchor.setY(y);	}





	/** Tells an SvgObject that its colour has been set to by a specific
	*	AutoCAD colour. A valid AutoCAD colour's range is +/-[0-255].
	*	@see #getColour
	*/
	public void setColour(int colour)
	{
		////////////// There are implications for this line if Custom CSS
		////////////// are involved.
		// Here we will only set this if the colour is set to a different value
		// because there are methods that just invert the colour (switch it to
		// a negative number) to change the object's visibility. If that occurs
		// the colour is still by layer but a negative number.
		if (Math.abs(ColourNumber) != Math.abs(colour))
		{
			// If we need to coerce all elements to BYLAYER set this true.
			// See DxfPreprocessor.isColourCoercedByLayer()
			if (DxfPreprocessor.isColourCoercedByLayer() == true)
			{
				COLOUR_BYLAYER = true;
			}
			else
			{
				COLOUR_BYLAYER = false;
			}
		}
		
		// Store the colour number even if we don't need it. 
		// we will check the validity of this number in toString()
		ColourNumber = colour;
	
		if (colour <= 0)
			setObjectVisible(false);
	}


	/** Returns an object's preformatted colour attribute.
	*/
	protected String getColour()
	{
		if (COLOUR_BYLAYER == true)
			Colour = SSG.getLayerColour(Layer);
		else
			Colour = DxfPreprocessor.getColour(ColourNumber);
				
		return Colour;
	}
	
	/** Returns an object's colour attribute as an integer. 
	*	The integer argument can be any arbitrary integer value.
	*/
	protected int getColour(int i)
	{	return ColourNumber;	}




	// this is used for specialty cases like generic text objects and hatches,
	// but is usually none.
	/** Set an object's fill as a 6 digit hex String preceeded by a '&#035;'.
	*	Called by sub-class objects that can take a fill in SVG but don't in
	*	DXF (circle, path, ellipse). If the argument String in empty then
	*	this method assumes that the object does not take a fill attribute
	*	and resets the value of Fill to null.
	*	@param fill String value of fill (a colour like <b>blue</b> or <b>#0066ff</b>).
	*/
	protected void setFill(String fill)
	{
		//System.out.print("@@set my fill to: '"+fill+"'");
		//System.out.println(" Object type: '"+this.getClass().getName()+"'");
		if (fill.equals(""))
			Fill = "";
		else
			Fill = "fill:" + fill + ";";
	}



	// now we also have to determine our fill if necessary.  Many objects don't
	// take a fill.
	/** Translates a integer argument into a hex String preceeded by a &#035;
	*	and sets an object's fill to that colour value. If the argument value
	*	is zero (0) the colour is not set;
	*	@param AcadColourNumber Colour as integer.
	*/
	public void setFill(int AcadColourNumber)
	{
		// If colour of '0' is passed it means the object is hidden or
		// on a locked layer.
		if (AcadColourNumber <= 0)
			setObjectVisible(false);
		
		Fill = "fill:" + DxfPreprocessor.getColour(AcadColourNumber) + ";";
		fillColourNumber = AcadColourNumber;
	}

	/** Returns the fill type as a String ready to be used as an
	*	attribute value if one is set returns null otherwise.
	*	@return String <code>fill:&#035;somenum;</code>
	*/
	protected String getFill()
	{
		// if the colour is not set it takes the colour of the layer.
		if (DxfPreprocessor.isColourCoercedByLayer())
		{
			Fill = "fill:" + SSG.getLayerColour(Layer) + ";";
		} 
		else if (Fill.equals(""))
		{
			Fill = "fill:" + SSG.getLayerFill(Layer) + ";";
		}
			
		return Fill;
	}

	// Generic SvgObjects need it to produce a default messages
	/** Sets any String value that this object may have.
	*	Notably this is used for &lt;text&gt;string&lt;/text&gt;.
	*	Has no effect on SvgGraphicElements.
	*/
	public void setContent(String s)
	{	content = s;	}



	/**
	*	Other methods need to be able to query an objects anchor point
	*	notably SvgWipeOut.
	*	@return Point The anchor point or point of origin for the object.
	*/
	public Point getAnchor()
	{	return Anchor;	}




	/** Performs the unique calculation required to describe this object as an SVG element.
	*	@return String In the case of SvgObject it returns an
	*	empty String.
	*/
	protected String calculateMyUniqueData()
	{	return "";	}



	/**
	*	Tests to see if 1) any styling rules apply and if a specific
	*	styling value is passed whether that style effects this object.
	*
	*	@param style_setting value of any particular applying style.
	*	Passing a 0 will test if any bits, at all, are set.
	*	@return boolean <B>true</B> if the query style is set and
	*	<B>false</B> if not.
	*/
	protected boolean isStyleSet(int style_setting)
	{
		// This allows the user to make a general query about whether
		// any bits are set by assuming if the user passes ANY_STYLE (0)
		// she wants to know if at least one switch is set.
		if (style_setting == ANY_STYLE)
			if (WhichRulesDiffer > 0)
				return true;
			else
				return false;

		if ((WhichRulesDiffer & style_setting) == style_setting)
			return true;

		return false;
	}
	
	
	/** Allows the addition of animation to an SvgObject or any of its subclasses.
	*/
	public void addAnimation(SvgAnimator sa)
	{
		// If the argument is null issue a message
		if (sa == null)
		{
			if (VERBOSE)
				System.err.println(this.getClass().getName()+".addAnimation() received a "+
					"null animation object.");
			return;
		}
		// Create the vector container for this any any other animation objects that
		// may come along.
		if (vAnimationSet == null)
			vAnimationSet = new Vector();
			
		// add the animation object.
		vAnimationSet.add(sa);
	}


	/**
	*	While SvgObject is the base class for all other Svg objects it
	*	can also be instantiated as an object of 'unknown' type. This
	*	allows new or undefined Acad objects to be written to the SVG
	*	file providing a sort of debug feature. The object appears as
	*	a text string of the name of the object at the location it would
	*	have appeared in the DXF, but only id DEBUG is set. If it is not
	*	set you get a warning on the command line about an unhandled object.
	*
	*	Method is over-written in the immediate sub-class {@link SvgGraphicElement#toString}
	*/
	public String toString()
	{
		/*	This is the very minimum of information to be included in the SVG
		*	This function should always be redefined in inheriting classes.
		*
		*	The design goal here is to catch any generic undefined objects that
		*	may have been created by EntityProcessor. If Dxf2Svg cannot determine
		*	what type of object it is it will create this generic object that will
		* 	appear in the SVG with the object's name.  It's a form of error tracking
		*	in the conversion from DXF to SVG. Who knows it DXF2SVG will be able to
		*	recognize new features of future versions of DXF but it would be nice
		*	to see what's missing!
		*
		*/

		/*
		*	I am rethinking this class. I think it would be more efficient, in light of
		*	so many classes requiring many of the same calculations and the time it
		*	takes to change them all, to make a method that each class handles it's own
		*	unique requirements and the toString() method of this Super class would handle
		*	the common calculations.  This would also make Style Sheet info easier to
		*	include or exclude depending on the users choice.
		*/

		StringBuffer ObjectOutput = new StringBuffer();
		/*
		*	If the debug switch is set SvgObject will display, in red courier text,
		*	the name of the AcDb object type. Otherwise the object will not appear at all.
		*	which I felt would be disconcerting and confusing.
		*/

		if (DEBUG == true)
		{
			// set fill to red to make it stand out more.
			setFill(1);
			ObjectOutput.append("<"+getType());
			ObjectOutput.append(getAttributes());
			ObjectOutput.append(" "+Anchor.toStringText());
			ObjectOutput.append(">");
			ObjectOutput.append(SvgObjID+":"+content);
			// Now add animation if any.
			if (vAnimationSet != null)
			{
				Iterator itor = vAnimationSet.iterator();
				while (itor.hasNext())
					ObjectOutput.append("\n\t"+itor.next());
			}		
			ObjectOutput.append("</"+getType()+">");
		}

		return ObjectOutput.toString();
	}

	/** Clones this SvgObject.
	*	@throws CastClassException if an object stored on the attributes vector is not an {@link dxf2svg.util.Attribute}
	*/
	protected Object clone()
	{
		SvgObject sobj = 	(SvgObject)super.clone();// (this.DxfConverterRef);

		// SvgElement's data
		sobj.SvgObjID 			= this.SvgObjID;
		sobj.setType(this.getType());
		sobj.Layer				= this.Layer;
		sobj.myClass			= this.myClass;
		sobj.setIncludeClassAttribute(this.getIncludeClassAttribute());
		sobj.VERBOSE			= this.VERBOSE;
		sobj.DEBUG				= this.DEBUG;
		sobj.MAKE_CSS			= this.MAKE_CSS;
		sobj.suppressElement	= this.suppressElement;
		sobj.objectIsVisible	= this.objectIsVisible;
		sobj.SSG				= this.SSG;
		sobj.svgUtility			= this.svgUtility;
		sobj.DxfConverterRef	= this.DxfConverterRef;
		// make a deep copy of any additional attributes.
		// This should never happen since attributes are added just before output.
		if (vAttribs != null)
		{
			Iterator vAttribsIt = this.vAttribs.iterator();
			while (vAttribsIt.hasNext())
			{
				Attribute att = (Attribute)(vAttribsIt.next());
				sobj.vAttribs.add(att);
			}
		}
		sobj.originalLayer		= this.originalLayer;

		// SvgObject's data
		sobj.Anchor 			= (Point)this.Anchor.clone();
		sobj.COLOUR_BYLAYER 	= this.COLOUR_BYLAYER;
		sobj.OBJECT_TAKES_FILL 	= this.OBJECT_TAKES_FILL;
		sobj.content 			= this.content;
		sobj.Fill 				= this.Fill;
		sobj.Colour 			= this.Colour;
		sobj.ColourNumber 		= this.ColourNumber;
		sobj.WhichRulesDiffer 	= this.WhichRulesDiffer;

		return sobj;
	}
}