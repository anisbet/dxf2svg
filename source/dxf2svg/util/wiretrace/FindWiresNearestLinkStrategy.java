
/****************************************************************************
**
**	FileName:	FindWiresNearestLinkStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SVG Search stategy that seeks wires based on the location
**				to a label indicating a hyperlink to 'Sheet 2'.
**				based on the Strategy OOP design pattern.
**
**	Date:		March 14, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 14, 2005
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import java.util.Vector;
import dxf2svg.svg.*;
import dxf2svg.util.RelativeLimitsFrame;
import java.util.regex.Pattern;
import dxf2svg.DxfConverter;

/** This algorithm takes the matched sheet(s) and searches for matching 
*	polyline wires (or gangs) nearby. This is strictly for Spar formatted
*	wiring diagrams.
*
*	@version	0.01 - March 14, 2005
*	@author		Andrew Nisbet
*/
public class FindWiresNearestLinkStrategy extends WireSearchStrategy
{
	/** The parameter results will be filled with references to the SvgObjects
	*	that match the following criteria.
	*	The insert point of the text must fit within a box that is 0.65in x 0.06in
	*	from the end of the polyline to make a match.
	*/
	public void searchMatchModify(
		DxfConverter conversionContext,
		Vector results,            
		SvgLayerGroup svgl,
		Vector labels,
		Pattern p
	)
	{
		// Here we will take each of the labels in turn and search for ALL the
		// applicable polyline wires.
		Vector tmpGroupItems     = svgl.getGroupElementsByReference();
		// Storage for WireLabelLocation Objects.
		if (results == null)
		{
			results = new Vector();
		}
		
		
		for (int i = 0; i < tmpGroupItems.size(); i++)
		{
			// Test to avoid ClassCastExceptions of elements that are not wires (like text
			// for instance).
			Object o = tmpGroupItems.get(i);
			if ( ! ( o instanceof SvgDoubleEndedGraphicElement ) )
			{
				continue;
			}
			
			
			SvgDoubleEndedGraphicElement ge = (SvgDoubleEndedGraphicElement)o;
			
			
			
			// compare the start and end point with the the insertion point of 
			// the text.
			// 66.0px width x 4.07px height. Measuring text heights and converting to pixels.
			// This is the bounding box that a text insertion point should lie in.
			RelativeLimitsFrame boundingBox = new RelativeLimitsFrame(
				66.0, 4.07, RelativeLimitsFrame.BOTTOM, ge.getStartPoint());
			
			
			
			// Test the start end of the wire
			///////////////// START POINT OF WIRE.
			if ( wireMatchesALabel( labels, boundingBox ) )
			{
				// so lets store this element as a WireLabelLocation object.
				WireLabelLocation wll = new WireLabelLocation(
					ge, WireLabelLocation.START_POINT);
				results.add(wll);
				
				continue;
			}
		
			
			// now try the end point
			///////////////// END POINT OF WIRE.
			boundingBox = new RelativeLimitsFrame(
				66.0, 4.07, RelativeLimitsFrame.BOTTOM, ge.getEndPoint());
					
				
			if (wireMatchesALabel( labels, boundingBox ) )
			{
				// so lets store this element as a WireLabelLocation object.
				WireLabelLocation wll = new WireLabelLocation(
					ge, WireLabelLocation.END_POINT);
				results.add(wll);
				
			}  // end if
			
			
		} // end for
		
		
		System.out.println("Total wire matches: " + results.size());

	}
	
	
	
	/** Tests if any of the text labels falls inside of the arg bounding box.
	*	@param labels Vector of found text labels (like SHEET 2). Elements
	*	other that {@link dxf2svg.svg.SvgText} elements are ignored.
	*	@param boundingBox the virtual test box.
	*/	
	protected boolean wireMatchesALabel( Vector labels, RelativeLimitsFrame boundingBox )
	{
		// now cycle the list of SHEETs and see if one falls inside the 
		// bounding box set at the start end of the wire polyline.
		///////////////// START POINT.
		for (int i = 0; i < labels.size(); i++)
		{
			
			// All elements on this vector must be SvgText elements
			Object o = labels.get(i);
			if ( ! ( o instanceof SvgText) )
			{
				continue;
			}
			
			SvgText text = (SvgText)o;
			
			
			if (boundingBox.contains(text.getAnchor()))
			{
				return true;
			}

		} // end for
		
		
		return false;
	}
}