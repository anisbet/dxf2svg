
/****************************************************************************
**
**	FileName:	CrossSheetWireIDs.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SVG Search class. Searches SVG's using different stategies 
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
**              0.02 - October 4, 2005 Added 'KT' (engine) to the wireid regex.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import java.util.Vector;
import dxf2svg.svg.SvgLayerGroup;
import java.util.regex.Pattern;
import dxf2svg.DxfConverter;

/** This class provides context for an SVG search. In this case this class is 
*	completely customized to search for the ID of a wire if it ends in a 
*	hyperlink and place the name of the wire as the ID of the wire.
*
*	@version	0.02 - October 4, 2005
*	@author		Andrew Nisbet
*/
public class CrossSheetWireIDs
{
	/** Circuit function letters (manditory).
	*/
	public final static String circuitFunctionLetters = 
	"([A-X&&[^IFRSTUNO]]|CA|F[A-G&&[^BF]]|KT|R[A-Z&&[^BCEFIJKO-TY]]|"+
	"S[ADSWX]|T[DFGHMNPRSWY]|U[ABCE]|W[FX])";    

	
	/** The complete regex for a wire id number pattern.
	*/
	public final static String wirePattern = 		
	"^[0-9]?"+                          // Unit number - single optional digit
	circuitFunctionLetters+             // Circuit function letters manditory
	"\\p{Digit}{1,4}"+                  // Wire number - one to three digits manditory.
	"\\p{Upper}{1,2}([0-9][0-9]?)?"+    // Wire segment letters - one or two letters manditory.
										// Wire size number - one to two digits optional
	"(\\p{Upper}{1,2}([0-9][0-9]?)?)?"+	// Same as above but the whole thing is optional (sigh).
	"[A-Z]?"+                           // Ground phase letter - one optional letter.
	"(-ALML|-CHROM)?"+                  // Wire type either chrome or aluminum.
	"(\\(\\p{Upper}{3}\\))?$";          // Hyphen and upper case wire colour name always 3 letter.
	
	
	
	
	/** Pass the type of stategy required for the type of search you need or create
	*	a new one as required.
	*/
	public CrossSheetWireIDs(DxfConverter conversionContext, Vector vLayers)
	{
		if (vLayers == null)
		{
			System.err.println("CrossSheetWireIDs.init(): no layers to search.");
			return;
		}
		
		
		
		///////////// pass 1
		// Search for link labels first.
		WireSearchStrategy searchStrategy;
		// Here is the pattern for the text that contains the link to another sheet.
		Pattern p = Pattern.compile("^SHEET \\p{Digit}{1,2}$");
		Vector vLinkLabels = new Vector();
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
			}
			
			// Start searching for labels
			if ( layerName.equals("english") )
			{
				searchStrategy = new FindSheetLinkLabelsStrategy();
				searchStrategy.searchMatchModify(conversionContext, vLinkLabels, layer, null, p);
				if (vLinkLabels.size() < 1)
				{
					System.out.println("no cross sheet links in this drawing.");
					return;
				} // end if
			}  // end if
		}  // end for
		
		// If no t layer not wire ids.
		if ( textLayer == null )
		{
			System.out.println("no text layer ('t'); therefore no wire id numbers.");
			return;
		}
		
		
		///////////// pass 2
		// Here we search for polylines that have an endpoint close to the Anchor of the 
		// text.
		Vector vWires = new Vector();
		for (int i = 0; i < vLayers.size(); i++)
		{	
			String layerName = new String();
			SvgLayerGroup layer = (SvgLayerGroup)vLayers.get(i);
			layerName = layer.getLayerName();
			//System.out.println("layerName = '"+layerName+"'.");
			if (layerName.equals("gang")) // this may not work with wire because
			// a wire is a SvgDoubleEndedElementAggregate which to date does not
			// have methods for returning its end points.
			{
				searchStrategy = new FindWiresNearestLinkStrategy();
				searchStrategy.searchMatchModify(conversionContext, vWires, layer, vLinkLabels, null);
				if (vWires.size() < 1)
				{
					//System.out.println("no wires located near sheet link.");
					return;
				} // end if
				break;
			}  // end if			
		}
		
		
		
		///////////// pass 3
		// Here we will take each of the wires that end in a SHEET n[n] label, and
		// start at the end opposite from the label and search each segment of the
		// wire (polyline) for a wire number on layer 't'.
		Pattern pattern = Pattern.compile( wirePattern );
		
		// get each of the WireLabelLocation objects stored here.
		searchStrategy = new FindApplyWireIDsStrategy();
		// null unrequired vector nothing in it.
		// textLayer = 't' layer where we are going to search for matching wire ID numbers.
		// Vector of wires that terminated in a 'SHEET n[n]' pattern.
		// Pattern that contains the the wire ID number pattern for the CC130. Used to 
		// varify that the text matches a wire's location is in fact a wire ID.
		searchStrategy.searchMatchModify(conversionContext,  null, textLayer, vWires, pattern );
		
		
	} // end constructor
}  // end class