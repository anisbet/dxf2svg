
/****************************************************************************
**
**	FileName:	SvgElement.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Is the base class for all Svg Objects.
**
**	Date:		August 29, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - August 29, 2002
**				0.02 - October 3, 2002 Added check for null value in
**				getClassAttribute(). If not done text includes a class="null"
**				after resetting class with resetClass().
**				0.03 - February 12, 2003 Added clone(), equals().
**				0.04 - March 4, 2004 Added ElementEvent handling for onclick etc.
**				0.05 - April 6, 2004 Moved setType() method from SvgObject to here.
**				0.06 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;
import dxf2svg.util.*;
import dxf2svg.animation.*;

import java.util.Vector;   // for element event actions.

/**
*	SvgElement is the abstract base class for all Svg Objects. This includes
*	collections of SvgObjects, visible objects like graphic elements and
*	invisible objects such as references.
*
*	@version	0.03 - February 12, 2003
*	@author		Andrew Nisbet
*/
public abstract class SvgElement implements Cloneable
{
	protected String SvgObjID = new String();	// Usually the acad handle, but can be string name.
	private String ObjType = new String();		// Type of object involved (line, arc etc.)
	protected String Layer = new String();		// Layer name to be used in conjunction with CSS
	protected String myClass = new String();	// This will be used to control styles and intelligence.
	private boolean 	INCLUDE_CLASS_ATTRIBUTE;// allows an
												// object to set its own
												// class but can be turned
												// off by an "intelligence"
												// engine so the object's
												// class can be manipulated.
												// All SvgObjects will
												// manifest this behaviour.
	protected static boolean		VERBOSE; 	// verbose mode.
	protected static boolean		DEBUG;		// debug mode on or off.
	protected static int 			MAKE_CSS; 	// make CSS is set?

	// We let the subclass decide if and which of the following objects it needs.
	protected StyleSheetGenerator 	SSG;		// For getColour() etc.
	protected SvgUtil 				svgUtility;	// conversion routines.
	protected DxfConverter 			DxfConverterRef; // This allows us to access
												// to DxfConverter for SSG
												// (and any future objects
												// we may require).
	protected boolean suppressElement = false;	// Used for optimization by
												// suppressing the output
												// of this element if true.
	protected boolean objectIsVisible = true;	// Controls the visibility of an 
												// object. If the colour is <= 0
												// this gets set to false.
	protected Vector vAttribs = null;			// Vector of events associated with this object.
	protected String originalLayer;				// Name of the layer this object originally came 
												// from. Important if you want to coerce objects
												// to standard layer. See SvgAnimationLanguage
												// for examples.


	/** Default constructor.
	*/
	public SvgElement()
	{
		VERBOSE 			= DxfPreprocessor.verboseMode();
		DEBUG				= DxfPreprocessor.debugMode();
		MAKE_CSS 			= DxfPreprocessor.cssMode();
		INCLUDE_CLASS_ATTRIBUTE=true;
	}
	
	/** This method returns the type of SvgElement that this object could be.
	*	In the case of SvgPolyline it could be a <em>polygon</em>, a <em>polyline</em>
	*	or a <em>path</em>.
	*/
	public String getType()
	{
		return ObjType;
	}
	
	
	///////// This method moved from SvgObject as all objects should use it
	///////// to set their method types. April 6, 2004.
	/** Sets the object's <I>type</I>.
	*	This may be required for some objects that are the same base object
	*	to Dxf2Svg but very different objects to SVG. 
	*	After that we use this method to change the object's 'type' as
	*	required to render the object correctly in SVG. For a good example
	*	see {@link SvgPolyLine#calculateMyUniqueData}
	*/
	public void setType(String type)
	{
		/*
		*	We may have to change types for a class like SvgEllipse which turns out
		*	to be an elliptical arc which is described by the <path> tag rather than
		*	the <ellipse> tag.  You don't know that though until you complete some
		*	of the calculations.
		*/
		ObjType = type;
	}
	
	/** This method allows you to apply attributes to this object.
	*	This method checks if the arg Attribute {@link dxf2svg.util.Attribute#equals}
	*	any other arg for this element and if it does it removes the old attribute
	*	and adds the new argument.
	*	@param attrib Attribute to be added to this element.
	*/
	public void addAttribute(Attribute attrib)
	{
		if (vAttribs == null)
		{
			vAttribs = new Vector();
			vAttribs.add(attrib);
			return;
		}
		
		Attribute a = null;
		// do not store duplicate attributes, but rather replace attributes
		// that have duplicate names.
		for (int i = 0; i < vAttribs.size(); i++)
		{
			a = (Attribute)vAttribs.get(i);
			if (attrib.equals(a))
			{
				vAttribs.remove(i);
			}
		}
		vAttribs.add(attrib);
	}

	/**
	*	Returns additional attributes if any attributes specified.
	*/
	protected StringBuffer getAdditionalAttributes()
	{
		// The SvgElement can contain ElementEvent objects which are the attribs like onclick.
		StringBuffer attribs = new StringBuffer();
		
		if (vAttribs != null)
		{
			Attribute attrib = null;
			for (int i = 0; i < vAttribs.size(); i++)
			{
				attrib = (Attribute)vAttribs.get(i);
				attribs.append(attrib.toString());
			}  // end for
		} // end if
		
		return attribs;
	}

	/** This method allows the inclusion of additional attributes that may be defined 
	*	by the user and are outside the scope of the conversion process. All subclasses
	*	can also use this method unless they specifically overwrite this method and do
	*	not get the extra attributes. {@link dxf2svg.svg.SvgEntityDeclaration} is one
	*	such class.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer outBuff = new StringBuffer();
		
		outBuff.append(getAdditionalAttributes());
		
		return outBuff;
	}

	/*
	*	To further simplify the repeated editing of toString() member functions in subclasses
	*	I will add this function that can be called from subclasses to report the objects name
	*	in the correct format and then just edit it here, once if needed.  In the long run
	*	I want to display the id=objName if CSS mode is on OR DEBUG mode is set. That way
	*	in either case I can find the errant object in the dxf if necessary and turn it off
	*	to save more space and download time.
	*/
	/** This method will format the ID attribute if the object name has
	*	been set. If the objects name is set to <code>null</code> the
	*	object's ID is omitted and the object is said to be <em>anonymous</em>.
	*	Groups are made up of anonymous objects that should have IDs.
	*	@return String ' id="myID"'
	*/
	protected String getObjID()
	{
		if (SvgObjID != null)
		{
			return " id=\"" + SvgObjID + "\"";
		}
		return "";
	}

	/**	This method will return the unformatted name of this object.
	*	@return String 'myID'
	*/
	public String getObjIDUU()
	{	return SvgObjID;	}

	/** Sets the object's ID attribute with, in most cases, the Dxf
	*	object's handle, but may also be set to the name of a pattern
	*	or the 'wire_run' if this SvgElement is a group that represents
	*	a wire run.
	*	@see SvgHatchPattern
	*	@see dxf2svg.animation.AnimationEngine#applyAnimation
	*/
	public void setObjID(String name)
	{	SvgObjID = name;	}


	/** Sets the object's layer name.
	*
	*	Since the layer name is the argument String and almost anything
	*	goes as far as layer naming conventions in AutoCAD. In SVG it is
	*	a different matter. SVG uses this layer name as a 'class' for
	*	styling properties and class name cannot have spaces so we also
	*	replace all the spaces with hyphens.
	*	Do not confuse this with the class name though, this method only
	*	parses out (svg) illegal characters. A layer name could be 'Layer0'
	*	but its class name would then be 'stLayer0'.
	*	<P>
	*	This method will also check to see if it is 
	*	necessary to change a layer for a piece of text or lines of a note triangle.
	*	This will occur if the '-notes' switch is selected. This is called layer normalization.
	*	Objects that are subclasses of the object and are on layer
	*	'NOTESENG' will get converted to 
	*	'ENGLISH' layer so there is less JavaScript required to coordinate language
	*	switching in the SVG and to maintain consistency in the output. This all came 
	*	into being to accommodate being able to find notes in a DXF. It was determined that
	*	finding such content was not possible (reliably) if the text was distinguished from
	*	other text by being on a different layer
	*	@see #setClass
	*	@param laName A layer name as a raw String from the EntityProcessor.
	*/
	public void setLayer(String laName)
	{
		// Save the original name in case any other object wants to know (for special 
		// processing of modification of attributes. We do this because multiple 
		// class names are separated by white space and autocad supports spaces in layer names.
		String newName = originalLayer = DxfPreprocessor.convertToSvgCss(laName);
		// Here we will check to see if the layer arg is one of the ones to look out for.
		// CAVEAT: if there is no french on a drawing this will still place the text onto
		// the english layer even if there is no layer in the drawing. The end result is 
		// that text that should be destined for layer 't' will end up on layer 'english'.
		// I am not sure what the result will be but I guess that this text will get put
		// down on the bottom of the SVG. The other disadvantage that can be cleaned up
		// later is providing a list of layers dynamically rather than hard coded. The
		// SvgNotes object allows you to change the layer that notes appear on. We should
		// be checking to see if these layers exist before we set them. This will also 
		// have an unknown effect on the relationship of SvgHatch, SvgHatchLine and SvgHatchPattern.
		//
		// June 21, 2004 I am now sure what will happen when we put something on a layer that 
		// doesn't exist; the query getLineTypeNameByLayer() will throw a null pointer exception
		// This is becuase the conversion process needs to know the style for a particular layer
		// and the layer doesn't exist. Hmmm. Well I will make a query to see if a layer exists
		// anyway and if the returning value is null then we can make a descision here about 
		// what to do vis-a-vis what layer to go to. Again it would be great to specify the 
		// layer for notes dynamically and be able to designate an alternate. For Spar conversion
		// we will hard code it for now.
		if (DxfPreprocessor.takeNotes() == true)
		{
			if (SSG == null)
			{
				SSG = DxfConverterRef.getStyleSheetGenerator();
			}
			
			String noteLayerNameEng = TableLayer.getNoteLayerName(Dxf2SvgConstants.ENGLISH);
			String englishLayer = TableLayer.getLanguageLayerName(Dxf2SvgConstants.ENGLISH);
			String noteLayerNameFre = TableLayer.getNoteLayerName(Dxf2SvgConstants.FRENCH);
			String frenchLayer = TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH);
			// we can use equals instead of equalsIgnoreCase because convertToSvgCss() changes
			// to lower case.
			if (newName.equalsIgnoreCase(noteLayerNameEng))
			{
				if (SSG.hasLayer(englishLayer))
				{
					newName = englishLayer;
				}
				else
				{
					newName = TableLayer.getDefaultLanguageLayerName();
				}
			} 
			else if (newName.equalsIgnoreCase(noteLayerNameFre))
			{
				if (SSG.hasLayer(frenchLayer))
				{
					newName = frenchLayer;
				}
				else
				{
					newName = TableLayer.getDefaultLanguageLayerName();
				}
			}
			else if (newName.equalsIgnoreCase(TableLayer.getNoteNumberLayerName()))
			{
				newName = TableLayer.getDefaultLanguageLayerName();
			}
		}

		Layer = newName;
		// any object or collection that sets a layer name automatically
		// gets a class too - even if they don't use it.
		setClass();
	}
	

	
	/**
	*	Returns the name of the layer that this object came from in the
	*	DXF or whatever Layer instance variable is set to.
	*	Do not confuse this with the class of an object. If this object's
	*	layer name is 'foo' its class name is 'stfoo'.
	*	@return String Name of layer (with svg illegal chars removed).
	*/
	public String getLayer()
	{
		return Layer; // if set.
	}
	
	
	/**
	*	Returns the name of the original layer that this object came from.
	*	Do not confuse this with the real layer. The real layer refers to
	*	the group that this object belongs to.
	*	Some objects are placed on special layers so that there content 
	*	can be identified, but after that occurs they should be moved to 
	*	a standard layer so that they can participate in animations or 
	*	other javascript events.
	*	@return String Name of original layer; may be null.
	*/
	public String getOriginalLayer()
	{
		return originalLayer; // if set, may be null.
	}



	/** Setting this switch controls the inclusion of a class attribute.
	*	It allows other processes to change an objects class.
	*	@param yesno <code>true</code> include class
	*	class and <code>false</code> don't.
	*	@see #setClass
	*/
	public void setIncludeClassAttribute(boolean yesno)
	{	INCLUDE_CLASS_ATTRIBUTE = yesno;	}

	/** Returns the switch setting that will allow or stop an SvgObject from
	*	including its class attribute.
	*	class and <code>false</code> don't..
	*	@see #setClass
	*/
	public boolean getIncludeClassAttribute()
	{	return INCLUDE_CLASS_ATTRIBUTE;	}



	/** Sets the class value for an SvgObject. The default is the Layer name
	*	that the object belonged to in the DXF. If the class has already
	*	been set no action is carried out.
	*/
	public void setClass()
	{
		// if it has already been set don't over-write
		// use reset class to change this
		if (myClass.equals(""))
			myClass = Layer;
	}

	/** Allows the setting of an SvgObjects class.
	*	Use this method	to set the class attribute.
	*	To remove a class from an object use resetClass();
	*	@param ClassStr name of class
	*	without the 'st' prefix.
	*/
	public void setClass(String ClassStr)
	{
		myClass = ClassStr;
	}

	/** Allows the setting of an SvgObjects class.
	*	Use this method	to set the class attribute.
	*	To remove a class from an object use resetClass();
	*/
	public String getClassAttribute()
	{
		if (myClass == null)
			return "";
		return " class=\"st"+myClass+"\"";
	}


	/** Returns the class name of an object without the 'st' prefix.
	*	The returned String contains just the class name and not the
	*	whole formatted attribute String.
	*	@see #setClass
	*	@see #getClassAttribute
	*	@return string name of class
	*/
	public String getAbsoluteClass()
	{
		if (myClass.equals(""))
			// empty string rather than a pointer to a null(?) object.
			return "";

		return myClass;
	}


	/**
	*	Allows the suppressing of elements during optimization of Svg file.
	*	This can occur if the objects are on a hidden layer or an image
	*	has a '0' value for its height or width.
	*/
	public void setSuppressElement(boolean tf)
	{	suppressElement = tf;	}

	/**
	*	Returns the current setting for outputting of this object.
	*	@see #setSuppressElement
	*	@return boolean <b>true</b> the element does not get expressed
	*	<b>false</b> (default) the element gets output to the Svg.
	*/
	public boolean isSuppressed()
	{	return suppressElement;	}

	/**	Sets the objects visibility. If the file is optimized this will 
	*	supress the output of this element or alternatively it will change
	*	the object's visibility attribute to 'hidden'.
	*/
	public void setObjectVisible(boolean tf)
	{	objectIsVisible = tf;	}
	
	/**
	*	Returns the current setting for outputting of this object.
	*	@see #setSuppressElement
	*	@return boolean <b>true</b> the element does not get expressed
	*	<b>false</b> (default) the element gets output to the Svg.
	*/
	public boolean isVisible()
	{	return objectIsVisible;	}
	
	
	/**	Allows an animation to be added to an object.
	*	@see SvgCollection#addAnimation
	*	@see SvgObject#addAnimation
	*	@see SvgReference#addAnimation
	*/
	public abstract void addAnimation(SvgAnimator sa);
	
	
	// We don't copy all the instance data here because this is an abstract class.
	// That task is left to the concrete classes.
	protected Object clone()
	{
		try{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}
} // SvgElement