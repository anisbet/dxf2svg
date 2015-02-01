
/****************************************************************************
**
**	FileName:	SvgElementAggregate.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class takes SvgDoubleEndedElements and combines their pathing
**				attributes into one continuous path description. This is what 
**				Design Patterns calles an Adaptor.
**
**	Date:		September 1, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 1, 2004
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;
import dxf2svg.animation.SvgAnimator;
import java.util.Vector;
import java.util.Iterator;
import dxf2svg.util.Attribute;

/**	This class takes SvgDoubleEndedElements and combines their pathing
*	attributes into one continuous path description. This is required 
*	to accomodate animation that must be applied to a group of element
*	but whose javascript event target would be individual pieces. Consider
*	a wire run in a schematic that needs to turn red when the user clicks
*	the wire. With animation the group will react together onmouseover,
*	however when you need to click the object to change the colour the 
*	event is fired for the element that was directly under the mouse when the 
*	the click event took place and not the group of elements that is formed by the &lt;g&gt; tag.
*	<P>
*	Doing this also makes the elements simpler and takes up less space.
*	<P>
*	At the current time the only client of this class is {@link dxf2svg.animation.AnimationEngine}
*	exclusively. It is also required only for item for wire runs.
*	<P>
*	How do we apply specific 
*
*	@version 	0.01 - September 1, 2004
*	@author		Andrew Nisbet
*/
public class SvgDoubleEndedElementAggregate extends SvgGraphicElement
{
	// Keep track of if this is the first element to get layer, id, colour etc.
	private boolean isFirstElement;
	private StringBuffer pathPoints;	// for the storage of path aggregates.
	
	public SvgDoubleEndedElementAggregate(DxfConverter dxfc)
	{	
		super(dxfc);
		setType("path");
		isFirstElement = true;
		pathPoints = new StringBuffer();
	}
	
	/** This method takes a SvgDoubleEndedElement and takes the display attributes of the
	*	first element submitted and copies them as its own. It is assumed that all the 
	*	elements that are to be combined into this aggregation have the same display attributes
	*	because making elements with different attributes into a aggregate wouldn't make sense.
	*	<P>
	*	Next the path data is taken and assembled. together into one big &lt;path&gt; element.
	*/
	public void add(SvgDoubleEndedGraphicElement sd)
	{
		// This clones the display attriubtes of the first element because it is 
		// assumed that all the elements have the same attributes (or would making 
		// an aggregate of these objects make sense?). The only difference is that
		// the id of the element contains an 'ag' prefix to indicate that the path
		// is an aggregate. This is done for trouble shooting purposes.
		if (isFirstElement)
		{
			// I do this because if there are any specific styles associated with this
			// object's class they will over-ride any settings we want to make in the
			// JavaScript so no class. This object can get its styling from the enclosing
			// SvgGroup.
			setIncludeClassAttribute(false);
			this.setObjID("ag" + sd.getObjIDUU());
			this.setClass(sd.getAbsoluteClass());
			this.setLayer(sd.getLayer());
			this.setLineType(sd.getLineType());
			this.setLineTypeScale(sd.getLineTypeScale());
			// Test this because because if colour = zero it gets interpreted as 
			// invisible in dxf2svg.util.Pen.
			if (sd.getColour(0) != 0)
			{
				this.setColour(sd.getColour(0));
			}
			isFirstElement = false;
		}
		// next we get the elements as paths and put them together into 
		// this object's 'd' attribute's value. The false indicates that the 
		// the path is not a continuation from the last point but rather a new
		// line segment.
		pathPoints.append(sd.getElementAsPath(false));
	}
	
	
	/**
	*	This method overrides the super classes method to include
	*	functionality to determine which attributes, if any, differ
	*	from the layer that they came from in the dxf file.
	*	<P>
	*	There is a complex interaction of rules for expressing the
	*	correct array of attributes that any one SvgObject requires.
	*	<P>
	*	@return StringBuffer
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer Output = new StringBuffer();

		setApplingRules();

		// if the ObjType is null or unknown and we still want to see it in
		// the SVG for debugging purposes
		if (DEBUG)
		{
			Output.append(getObjID());
		}

		// now we output any styling information depending on whether
		// 1) we need the information because the attributes are INLINE_STYLES.
		// 2) the attribs differ from the layer they are on.
		// We'll handle them one by one so it is clear when we set one
		// or not.
		// So if we have at least one style rule unique or the attribs
		// are INLINE_STYLES
		
		this.addAttribute(new Attribute("fill","none"));
		
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

			if (isStyleSet(COLOUR))
			{
				// if we got here its because our colour is different from
				// the layer's (use the methods rather than instance data
				// because we don't know if the formatting takes place on
				// the set() (how we would like it) or on the get() method
				// because we can't be sure everything we need is ready
				// until we are outputting.
				if (this.getColour(0) != 0)
				{
					this.addAttribute(new Attribute("stroke",getColour()));
				}
			}

			if ((isStyleSet(LINETYPE)) || (isStyleSet(LINETYPESCALE)))
			{
				// this handles both linetype and linetype scale uniqueness.
				// We have to use this until we can fix TableLineType to return Attributes.
				//	Output.append(getLineType());
				//System.out.println(SSG.getLineType(LineType, LineTypeScale, "as_attribute"));
			}
			
			// if the layer is frozen or the colour is a neg number do this.
			if (isStyleSet(VISIBILITY))
			{
				this.addAttribute(new Attribute("visibility","hidden"));
			}
		}
		
		Output.append(getAdditionalAttributes());
		
		return Output;
	}
	
	
	
	/** This method returns the String version of this object, but the format is different 
	*	from the other SvgElements in that the attributes must be output individually
	*	instead of together in the 'style' attribute. The reason for this is because
	*	the Adobe plugin's JavaScript engine will not allow style string attributes to be
	*	altered. Only descrete attributes can be altered.
	*/
	public String toString()
	{
		StringBuffer ObjectOutput = new StringBuffer();
		
		ObjectOutput.append("<"+getType());
		ObjectOutput.append(getAttributes());
		ObjectOutput.append(calculateMyUniqueData());	
		// Now add animation if any.
		if (vAnimationSet != null)
		{
			ObjectOutput.append(">");
			Iterator itor = vAnimationSet.iterator();
			while (itor.hasNext())
			{
				ObjectOutput.append("\n\t"+itor.next());
			}
			ObjectOutput.append("</"+getType()+">");
		}
		else
		{
			ObjectOutput.append("/>");
		}
		
		return ObjectOutput.toString();
	}
	
	
	/** Allows the addition of multipule animations to an this class,
	*	<P>
	*	If the array of animation objects is null, or its length is less than 1,
	*	the method returns without altering the the class.
	*	@throws ClassCastException if the Vector arg contains anything other than
	*	SvgAnimator objects.
	*/
	public void addAnimation(Vector sas)
	{
		// In this respect this class behaves like a collection and also a SvgObject.
		// The AnimationEngine passes a Vector of animation objects to this class
		// and they are incorporated just like you would with a SvgGroup, but because
		// we need the rendering information we also inherit from SvgGraphicElement.
		if (sas == null)
			return;
		
		// Now we add the animation to the front of the collection (for consistancy)
		// by converting it to a string.
		for (int i = 0; i < sas.size(); i++)
		{
			SvgAnimator sa = (SvgAnimator)sas.get(i);
			this.addAnimation(sa);
		}
	}
	

	/** Performs the unique calculation required to describe this object as an SVG element.
	*	@return String the aggregate of all the pathing of all the submitted elements or
	*	if there were none, an empty String.
	*/
	protected String calculateMyUniqueData()
	{	
		return " d=\"" + pathPoints.toString() + "\"";
	}	
	
	
	protected Object clone()
	{
		SvgDoubleEndedElementAggregate sgEle = 
			(SvgDoubleEndedElementAggregate)super.clone();

		sgEle.isFirstElement 	= this.isFirstElement;
		sgEle.pathPoints		= new StringBuffer();
		sgEle.pathPoints.append(this.pathPoints.toString());

		return sgEle;
	}
}