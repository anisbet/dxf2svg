
/****************************************************************************
**
**	FileName:	FindApplyWireIDsStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SVG Search stategy that seeks the first text from the opposite
**				end of the 'Sheet n[n]' pattern and gives the wire that the 
**				text appears over, that wire number as an ID attribute.
**				based on the Strategy OOP design pattern.
**
**	Date:		March 14, 2005
**
**	Author:		Andrew Nisbet, The Third
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - April 08, 2005
**				2.00 - August 31, 2005 Rework of the match wire id algorithm.
**              2.01 - October 4, 2005 Cleaned up un-necessary code.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import java.util.Vector;
import java.util.regex.Pattern;
import dxf2svg.DxfPreprocessor;
import dxf2svg.DxfConverter;
import dxf2svg.svg.*;
import dxf2svg.util.Attribute;
import dxf2svg.util.RelativeLimitsFrame;

/** This algorithm seeks the first text from the opposite
*	end of the 'SHEET n[n]' pattern and gives the wire that the 
*	text appears over, that wire number as an ID attribute. 
*	<P>
*	This is strictly for Spar formatted wiring diagrams.
*	<P>
*	NOTES: The algorithm works like this:
*	<ol>
*	<li>If the drawing starts with a 'w' or 'W' only proceed. (see {@link dxf2svg.DxfConverter}
*	for code location.)
*	<li>Search the 't' layer for all text that matches the 
*	{@link dxf2svg.util.wiretrace.CrossSheetWireIDs#wirePattern} regex.
*	<li>Search the 'english' layer for all references to 'SHEET [n]'.
*	<li>Find all the polylines on the 'gang' layer that are close to the previous search results.
*	These polylines have intersection points numbered from 0 to n -1.
*	<li>Take each of the found wire identification numbers with the smallest virtual reference
*	frame, and test each polyline wire to see if the 1 to n-1 intersections fall inside the frame.
*	If not test the 0th intersection. If matched, make entry in log, add the text as an ID attribute to the wire.
*	<li>If the previous test fails search all the other wires until successful match is found.
*	<li>If the text still does not match, extend the reference frame and research the wires useing
*	the previously mentioned steps.
*	<li>If that fails discard text, make entry in log, take the next wire id number and repeat 
*	until the list of wire id numbers is exhausted.
*	</ol>
*	@version	2.01 - October 4, 2005
*	@author		Andrew Nisbet
*/
public class FindApplyWireIDsStrategy extends WireSearchStrategy
{
	protected final int MAX_SEARCH = 3;  // Number of iterations before giving up.
	protected Vector unmatchedWireIds;
	protected boolean isPrimaryPass;
	
	/** The constructor manages the entire process of finding and matching wire id numbers
	*	to the collection of wires, from start to finish.
	*	@param conversionContext Parent object for important references
	*	@param results Where we put the results
	*	@param svgl All the elements from the 'T' layer
	*	@param wireCollection Collected wires as WireLabelLocation objects
	*	@param p The pattern for wire id numbers
	*/
	public void searchMatchModify(
		DxfConverter conversionContext,  // Parent object for important references.
		Vector results,                  // Where we put the results.
		SvgLayerGroup svgl,              // All the elements from the 'T' layer.
		Vector wireCollection,           // Collected wires as WireLabelLocation objects.
		Pattern p                        // The pattern for wire id numbers. 
	)
	{
		unmatchedWireIds = new Vector();
		isPrimaryPass = true;
		
		//test that the wireCollection vector is populated.
		if (wireCollection == null)
		{
			System.err.println("FindApplyWireIDsStrategy: no matching wires to search.");
			return;
		}
		
		// A place to store our wire id number found in this drawing.
		Vector wireIDHits = new Vector();		
		//search for all the unique wire number ids.
		if (isThereWireIdNumbersInDxf( wireIDHits, svgl, p ) == false)
		{
			System.out.println("found no wire id numbers on layer.");
			return;
		}
		else
		{
			if (DEBUG)
			{
				System.out.println( wireIDHits.size() + " potential wire ids found in file.");
			}
		}
		
		process(conversionContext, wireIDHits, wireCollection);
		isPrimaryPass = false;
		process(conversionContext, unmatchedWireIds, wireCollection);
	}
	
	
	protected void process(DxfConverter conversionContext, Vector wireIDHits, Vector wireCollection)
	{
		int successHitCount = 0;
		
		// Here we will conduct MAX_SEARCH passes, trying in each case to find the
		// the minimum box size that will contain a wire's intersection. We will
		// search all text elements with the minimum reference frame starting with
		// the second intersection, then the first intersection. Then all will
		// be searched again with the next size bigger reference frame based on the
		// value of testPass, until MAX_SEARCH searches are completed.
		for (int testPass = 1; testPass <= MAX_SEARCH; testPass++ )
		{
			// We will use this for the second search pass of all INITIAL intersections.
			Vector leftOverWireIds = new Vector();
			
			// Iterate over all the wireIds to match them against each of the 
			// wires on the component vector.
			for ( int i = 0; i < wireIDHits.size(); i++ )
			{
				Object o = wireIDHits.get(i);
				// Filter any objects that may not be SvgText Objects.
				// Not required.
				if ( ! ( o instanceof SvgText) )
				{
					continue;
				}
				
				SvgText svgTextWireId = (SvgText)o;
	
				// is text on wire ( text to test, collection of wires, false = don't search first
				// intersection of wire polyline ).
				if ( isTextOnWire( svgTextWireId, wireCollection, false, testPass ) == true )
				{
					successHitCount++;
					if ( DEBUG )
					{
						svgTextWireId.addAttribute( new Attribute( "fill", "blue"));
					}
					
				}
				else  // isTextOnWire() returned false
				{
					leftOverWireIds.add( svgTextWireId );
				}
			}  // end for (wireID)
		
		
			// Here we take all the wire ids that didn't match any wire and try them all
			// again; this time by trying to match them to the first intersection of all wires.
			for ( int i = 0; i < leftOverWireIds.size(); i++ )
			{
				SvgText svgTextWireId = (SvgText)leftOverWireIds.get(i);	
	
				if ( isTextOnWire( svgTextWireId, wireCollection, true, testPass ) )
				{
					successHitCount++;
					if ( DEBUG )
					{
						svgTextWireId.addAttribute( new Attribute( "fill", "blue"));
					}
				}
				else
				{
					unmatchedWireIds.add( svgTextWireId );
				}
			} // end for 
		} // end for
		
		
		if (isPrimaryPass)
		{			
			if ( DEBUG )
			{
				for ( int i = 0; i < unmatchedWireIds.size(); i++ )
				{
					DxfPreprocessor.logEvent("", ((SvgText)unmatchedWireIds.get(i)).getString() );
				}
			}
		
		
			// This prints the entire list of wire (label locations), their start and end points
			// and their id if any.
			double percent = ((double)successHitCount / (double)wireCollection.size()) * 100;
			if ( DEBUG )
			{
				System.out.println( "wire to id matches: " + percent + "%" );
			}
		
	
			// Output all the wires, locations and wireids.
			DxfPreprocessor.logEvent(conversionContext.getFileName(), wireCollection.toString() );
			DxfPreprocessor.logEvent("=============== results", "wire to id matches: " +
				percent + "% ===============\n" );
		}
	} // end of searchMatchModify
	
	
	
	
	
	/** This method takes the test wire id number and tries to match it to any
	*	wire but by starting on the second intersection of the wire polyline.
	*	<P>
	*	This method is also where the id is added to the wire.
	*	@param svgTextWireId The test wire id
	*	@param wireCollection The collection of wires to test the wire id against
	*	@param searchFirstIntersection flag to initiate search on first intersection
	*	of the wire polyline or to start on the second and work back.
	*	@param testPass The pass number for setting the relative frame size of the 
	*	text element.
	*/
	protected boolean isTextOnWire
	( 
		SvgText svgTextWireId, 
		Vector wireCollection,
		boolean searchFirstIntersection,
		int testPass
	)
	{
		// Iterate over all the objects on the wireCollection vector extracting all the 
		// WireLabelLocation objects; from there grab all the wires.
		for (int i = 0; i < wireCollection.size(); i++)
		{
			// filter out none WireLabelLocation objects.
			Object o = wireCollection.get(i);
			if ( ! ( o instanceof WireLabelLocation ) )
			{
				continue;
			}
			
			WireLabelLocation wireLocation = (WireLabelLocation)o;
			
			// This line stops the app from taking a potential wire ID and testing 
			// against a wire that already has an ID. If I remove this, every piece
			// of wireid text is tested against every wire.
			if (isPrimaryPass && wireLocation.isIdFlagSet())
			{
				continue;
			}
			
			if (isWireMatch(wireLocation, svgTextWireId, searchFirstIntersection, testPass) )
			{
				// add the wire id to the wire.
				setWireId(svgTextWireId, wireLocation);
				return true;
			}
		}  // end for
		
		// To get here we failed to match the argument text to the wire using this 
		// algorithm.
		return false; 
	}
	
	
	
	/** This method applies the wire id. It also checks to see if the wire already has
	*	a wire id and if it does resolves which is the correct ID.
	*/
	protected void setWireId(SvgText svgTextWireId, WireLabelLocation wireLocation)
	{
		if (wireLocation.isIdFlagSet())
		{
			double incumbentInsertX = wireLocation.getId().getAnchor().getX();
			double upstartInsertX   = svgTextWireId.getAnchor().getX();
			if (incumbentInsertX <= upstartInsertX)
			{
				return;
			}
		}
		
		// add the wire id to the wire.
		wireLocation.setId( svgTextWireId, DEBUG );
	}
	
	
	
	
	
	
	/** This method takes the text and creates a virtual reference frame around the 
	*	insersion point, it then measures whether any of the intersections of the 
	*	argument wire falls within this reference frame.
	*	<P>
	*	Here is an explaination of intersections:
<pre>   intersection 1</pre> 
<pre>                o-------- Q55A16 --------</pre>
<pre>                 \</pre>
<pre>                  \o----- Q55C18 --------</pre>
<pre>      intersection 2</pre>
	*
	*	Q55A16 will match both wires if it appears on the label list before 
	*	the label Q55C18. To get around this we will test the wires from second
	*	intersection first. There will be a successful match for all the secondary
	*	wires first, then the primary wire will not match anything because its 
	*	label is on the first segment. Next we test the first segment and the
	*	primary wire will pass.
	*	@param wireLocation A {@link dxf2svg.svg.SvgDoubleEndedGraphicElement} (polyline) that
	*	represents a wire.
	*	@param svgTextWireId {@link dxf2svg.svg.SvgText} element.
	*	@param searchFirstIntersection True if the search starts at intersection 0 of
	*	a polyline and false if all other intersections to be search.
	*	@param testPass The pass number of the search from 1 to {@link #MAX_SEARCH}.
	*	@return true if an intersection falls within the SvgText object's insertion
	*	reference frame and false otherwise.
	*/
	protected boolean isWireMatch( 
		WireLabelLocation wireLocation, 
		SvgText svgTextWireId, 
		boolean searchFirstIntersection,
		int testPass
	)
	{
		SvgDoubleEndedGraphicElement wire = wireLocation.getWire();
		// We need somewhere to store the segment intersections
		Vector intersectionPoints = new Vector();
		wire.getAllSegmentPoints( intersectionPoints );
		// If the SHEET [n] label was found at one end, then the wire id
		// should be located at the other end. If the label was found at
		// the 
		if (wireLocation.getWireLabelLocation() == WireLabelLocation.START_POINT)
		{
			reverseWireIntersectionOrder(intersectionPoints);
		}

		Point pt = null;		
		if ( searchFirstIntersection )
		{
			// Test for the primary wire intersection. This is where most wires  
			// will succeed in finding their label. We have to be careful because
			// the first element and last element are interchangeable. It is unclear
			// which end of the wire we have, so we will check which is the first element
			pt = (Point)intersectionPoints.firstElement();
			
			// see if the point is in the bounding box.
			if ( isWireIntersectionInBoundingBox( svgTextWireId, pt, testPass ) )
			{
				return true;
			}
		}
		else // searchFirstIntersection == false
		{
			// Start search on intersection 1 not intersection 0.
			for (int i = 1; i < intersectionPoints.size(); i++)
			{
				pt = (Point)intersectionPoints.get(i);

				// see if the point is in the bounding box.
				if ( isWireIntersectionInBoundingBox( svgTextWireId, pt, testPass ) )
				{
					return true;
				}
			} // end for		
		} // end else

		return false;
	} // isWireMatch
	
	
	
	
	
	/** This method measures whether a vertual bounding box created in relation to
	*	the SvgText insertion point, encloses the argument point pt. If it fails, 
	*	it makes {@link #MAX_SEARCH} other attempts, each time broadening the width of
	*	the bounding box in an attempt to rigorously test if the point will eventually
	*	match. This is done because the text can appear at almost any distance from
	*	the nearest intersection of a wire.
	*
	*	@param text The text reference whose insertion point is used to create the 
	*	bounding box for testing
	*	@param pt A point that may or may not fall within the bounding box.
	*	@param testPass Determines the size of the reference frame to attempt to capture a
	*	wire's intersection. There are {@link #MAX_SEARCH} number of passes,
	*	each will extend the reference frame a little farther trying to match a polyline's
	*	intersection.
	*	@return true if the point is within the bounding box and false otherwise.
	*/
	protected boolean isWireIntersectionInBoundingBox( SvgText text, Point pt, int testPass)
	{
		// 57.0px width x 3.12px height. Measuring text heights and converting
		// to pixels. This is the bounding box that the end of the wire will lie inside 
		// of. The height will grow for taller text, but this is good for 6pte, the size
		// of our wire id numbers.
		double width  = 40.0;
		//double width  = 57.0;
		double height = 3.12;
		RelativeLimitsFrame boundingBox = null;
		
		boundingBox = new RelativeLimitsFrame(
			( width * (double)testPass ),      // width
			height,                           // height
			(RelativeLimitsFrame.TOP + RelativeLimitsFrame.LEFT), // location
			text.getAnchor()                  // relative position.
		);		

		if ( boundingBox.contains( pt ) )
		{
			if ( DEBUG )
			{
				System.out.println( "bounding box of " + text.getString() + 
					" matched a wire on try #"+testPass+".");
			}
		
			return true;
		}  // end if
		return false;
	} // end isWireIntersectionInBoundingBox()
	
	
	

	
	
	/** This method looks through the text elements and finds matches to the wire
	*	id number regex pattern.
	*	@param nums the vector of results
	*	@param layer the text layer to search
	*	@param wireIDPattern the pattern for wire id numbers.
	*/
	private boolean isThereWireIdNumbersInDxf( 
		Vector nums,
		SvgLayerGroup layer,
		Pattern wireIDPattern
	)
	{
		if (layer == null)
		{
			System.out.println( "the layer is null." );
			return false;
		}
		
		boolean result = false;
		// get reference to the collections elements
		Vector textLayerElements = layer.getGroupElementsByReference();
		
		for (int i = 0; i < textLayerElements.size(); i++)
		{
			Object obj = textLayerElements.get(i);
			if ( ! ( obj instanceof SvgText) )
			{
				continue;
			} // end if
				
			SvgText text = (SvgText)obj;
			
			if ( text.find( wireIDPattern ) )
			{
				result = true;
				nums.add( text );
			} // end if
		} // end for
		
		return result;
	}
	
	

	
	
	/** Reverses the elements on the argument Vector.
	*	@param v Vector of elements to be reversed.
	*/
	private void reverseWireIntersectionOrder( Vector v )
	{
		if (v == null)
		{
			return;
		}
		
		
		Vector vTmp = new Vector();
		
		
		for (int i = (v.size() -1); i >= 0; i--)
		{
			Object o = v.get(i);
			vTmp.add(o);
		}
		
		
		v.clear();
		v.addAll(vTmp);		
	}
}  // end class