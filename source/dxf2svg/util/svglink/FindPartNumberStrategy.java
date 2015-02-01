
/****************************************************************************
**
**	FileName:	FindPartNumberStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates a search strategy that looks for part numbers
**				in a figure.
**
**	Date:		0.01 - March 30, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 30, 2005
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util.svglink;

import java.util.Vector;
import dxf2svg.svg.*;
import dxf2svg.DxfConverter;
import dxf2svg.DxfPreprocessor;
import dxf2svg.util.Attribute;
import dxf2svg.util.EventAttributeModifier;
import dxf2svg.util.Dxf2SvgConstants;
import java.util.regex.Pattern;

/** This algorithm seeks the part numbers from the argument layer and
*	creates a SvgHyperlink object around the part number, adds a 'PartNo'
*	id to the text object and restores the new SvgHyperLink object to
*	the layer group at the position in the layer, of the original SvgText object.
*
*	@version	0.01 - March 30, 2005
*	@author		Andrew Nisbet
*/
public class FindPartNumberStrategy extends LinkSearchStrategy
{
	public void searchMatchModify(	SvgLayerGroup svgl,	Pattern p, DxfConverter dxfc  )
	{

		// We will manually collect all the elements and test them ourselves for
		// the pattern so that we can maintain the text object ordering.
		Vector vItems = svgl.getGroupElementsByReference();
		
		for (int i = 0; i < vItems.size(); i++)
		{
			Object o = vItems.get(i);
			if ( ! ( o instanceof SvgText ) )
			{
				continue;
			}
			
			SvgText text = (SvgText)o;
			
			
			/////// Note: the ID attribute is added automatically by SvgDxfHyperlink. ///////
			if (( text.getStyle().equals( "8ptbold" ) || text.getStyle().equals( "8ptb" )) && text.find( p ) )
			{
				// remove the text item and replace it with the SvgHyperlink object.
				vItems.remove( i );
				

				SvgDxfHyperlink link = new SvgDxfHyperlink( dxfc );
				
				// This could be any string as SvgDxfHyperlink applies this value automatically.
				link.setXLink("#stay");
				
				Attribute partNo = DxfPreprocessor.getPartsListAttribute();
				
				EventAttributeModifier eAttribMod = 
					new EventAttributeModifier(
						text, 
						partNo,
						EventAttributeModifier.MY_FORMAT
					);

				
				link.addAttribute( eAttribMod.getAttribute() );
				
				// now add the id attribute for the text element.
				String id = Dxf2SvgConstants.PART_NUMBER_ID + "_" + text.getString();
				text.addAttribute( new Attribute( "id", id ) );
				
				link.addElement( text );
				
				vItems.insertElementAt( link, i);
			}
		}
		
	} // end of searchMatchModify
	
}  // end class