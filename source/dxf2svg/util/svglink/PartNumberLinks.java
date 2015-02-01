
/****************************************************************************
**
**	FileName:	PartNumberLinks.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SVG Search class. Searches SVG's using different stategies 
**				based on the Strategy OOP design pattern.
**
**	Date:		March 30, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 30, 2005
**				1.00 - April 25, 2005 Changed the Part number Id to match 
**				index numbers with trailing characters and preceeded by
**				prefix characters. In this way we catch all the panel access
**				panel IDs as well.
**				1.50 - August 11, 2005 Modified the part number regex to accomodate
**              a wider variety of access panel numbers.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util.svglink;

import java.util.Vector;
import dxf2svg.svg.SvgLayerGroup;
import dxf2svg.DxfConverter;
import java.util.regex.Pattern;

/** This class provides context for an SVG search. In this case this class is 
*	completely customized to search for the ID of a wire if it ends in a 
*	hyperlink and place the name of the wire as the ID of the wire.
*
*	@version	0.01 - March 14, 2005
*	@author		Andrew Nisbet
*/
public class PartNumberLinks
{
	/** The regex for determining part number and access panel numbers.
	*	For more details on how part numbers see {@link dxf2svg.util.EventAttributeModifier#stringAsIntPadChar},
	*	but this regex effectively catches access panel numbers
	*	and part numbers but does not interfer with the handling of 
	*	{@link dxf2svg.util.EventAttributeModifier#stringAsIntPadChar}'s part number padding scheme.
	*	@see dxf2svg.util.EventAttributeModifier
	*/
	public final static String PART_INDEX_NO = "^([A-Z][A-Z]?)?\\p{Digit}{1,3}[A-Z]?$";
	
	/** Pass the type of stategy required for the type of search you need or create
	*	a new one as required.
	*/
	public PartNumberLinks(Vector vLayers, DxfConverter dxfc)
	{
		if (vLayers == null)
		{
			System.err.println("PartNumberLinks.init(): no layers to search.");
			return;
		}
		
		
		
		///////////// pass 1
		// Search for link labels first.
		LinkSearchStrategy searchStrategy;
		// Here is the pattern for the text that contains the Part Number.
		Pattern p = Pattern.compile( PART_INDEX_NO );
		
		
		
		// We will keep a reference to the text layer for wireid number searches.
		SvgLayerGroup textLayer = null;
		
		
		
		for (int i = 0; i < vLayers.size(); i++)
		{
			String layerName = new String();
			SvgLayerGroup layer = (SvgLayerGroup)vLayers.get(i);
			layerName = layer.getLayerName();
			
			// Keep a reference to the text layer cause it's the one with the wireid numbers.
			// This is because wireid numbers are never translated.
			if ( layerName.equals("t") )
			{
				
				textLayer = layer;
				searchStrategy = new FindPartNumberStrategy();
				searchStrategy.searchMatchModify( layer, p, dxfc );

			}  // end if
		}  // end for
		
		
		
		
		// If no t layer not wire ids.
		if ( textLayer == null )
		{
			return;
		}
		
	} // end constructor
}  // end class