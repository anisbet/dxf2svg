
/****************************************************************************
**
**	FileName:	SvgLayerGroup.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates Dxf layer objects into a <g></g> tag.
**
**	Date:		August 30, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 30, 2002
**				1.02 - March 18, 2003 Added group ids to match layer names
**				to facilitate switching from French to English.
**				1.03 - March 14, 2005 Added getLayerName() method.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;			// DxfConverter
import dxf2svg.animation.SvgAnimator;	// animation.


/**
*	This class encapusulates all the SvgObjects that share a commonality
*	of having come from the same layer in a DXF.
*
*	@version	1.03 - March 14, 2005
*	@author		Andrew Nisbet
*/
public final class SvgLayerGroup extends SvgGroup
{

	//////////// Constructors ////////////////
	// default
	/**
	*	Sets the DxfConverter object and applys a layer name.
	*	@param dxfc dxf conversion context
	*	@param layerName Layer name.
	*/
	public SvgLayerGroup(DxfConverter dxfc, String layerName)
	{
		super(dxfc);
		// We need to set the layer name for group objects to
		// retrieve line types by layer and colour by layer for
		// inline styling. Besides <g>s are layers so they should
		// be obligated to report their layer name.
		setLayer(layerName);
		// Set the object ID of a layer group so we can change the attributes
		// on all the objects on the layer at once i.e. switching from English
		// to french.
		setObjID("st"+Layer);
	}
	
	/** Returns the layer's name.
	*	@since 1.03 - March 14, 2005
	*/
	public String getLayerName()
	{
		return Layer;
	}
	
	/**
	*	Creates a SvgLayerGroup without a layer attribute or class replaces SvgLayerSubGroup.
	*	@param dxfc conversion context
	*/
	public SvgLayerGroup(DxfConverter dxfc)
	{
		super(dxfc);
		setIncludeClassAttribute(false);
	}	
	
	/**
	*	Returns the collection's attributes as a StringBuffer.
	*	@return StringBuffer of formatted attributes.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer OutputString = new StringBuffer();

		// This is manitory for layer groups but not required for nested groups like 
		// those found in animation groups.
		if (getIncludeClassAttribute() == true)
		{
			OutputString.append(getObjID());
			OutputString.append(getClassAttribute());
		}
		OutputString.append(getAdditionalAttributes());
		
		return OutputString;
	}
	
}