
/****************************************************************************
**
**	FileName:	DxfObjectProcessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Places important URI information about images that may be
**				imbedded in the DXF file.
**
**	Date:		October 3, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 3, 2002
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				0.03 - February 24, 2005 Added the conversion context to the 
**				DxfImageObjectReference in accordance with new constructor
**				requirements.
**				2.00 - April 16, 2005 Modified to accomodate new parser functionality
**				and elimination of DxfElementPair as fly weight.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;			// For Vector handling.
import dxf2svg.svg.DxfImageObjectReference;
import dxf2svg.*;			// DxfConverter

/**
*	The ObjectProcessor	reads an Entity Pairs looking for important
*	informtion concerning images that may be embedded.
*	To date this is the only job this processor does, but that could
*	change in the future. It comes into being
*	because the designers of Dxf decided to place the URI information about
*	imbedded images in the object section of the Dxf file.
*
*	@version	2.00 - April 16, 2005
*	@author		Andrew Nisbet
*/
final class DxfObjectProcessor extends Processor
{
	private DxfImageObjectReference DIORef;
	private boolean isCollecting;
	private Vector collectedObjects;
	private boolean DEBUG;

	
	/** 
	*/
	public DxfObjectProcessor( DxfConverter dxfc, Vector collectedObjects )
	{
		super();
		DxfConverterRef       = dxfc;
		this.collectedObjects = collectedObjects;
		DEBUG                 = false;
		isCollecting          = false;
	}

	
	
	
	/** Process the pair, one-at-a-time.
	*/
	public void process( DxfElementPair pair )
	{
		// We are in the midst of populating a SvgObject.
		if ( isCollecting )
		{
			if (DEBUG){ System.out.println( "...collecting..." ); }
			collectEntity( DIORef, pair );
		}
			
		
		// if we are not collecting yet and we find a start of object marker
		// test it to see if we are already collecting an object and if it is 
		// the type of object we want.
		if ( pair.getCode() == 0 )
		{
			if ( pair.getValue().equals( "IMAGEDEF" ) && isCollecting == false )
			{
				if (DEBUG){ System.out.println( "^^^^BEGIN" ); }

				DIORef = new DxfImageObjectReference( DxfConverterRef );
				
				isCollecting = true;
			} 
			else 
			{
				if ( isCollecting == true )
				{
					finishObject();
					// Re-throw the last pair in the case that the next object is 
					// another required object.
					process( pair );
				}
			}
		}  // end if
	}  // end process(pair)


	/** Finishes the object collection.
	*/
	protected void finishObject()
	{
		// These are the only objects being collected from this section. If the object
		// is the last one, ProcessorManager must finalize it so we know the object 
		// will be added to the collection. These things are also rare. We only collect
		// one or at most a couple from a file so usually the objects are finished through
		// the normal routine of processing. We don't want a finished object to be repacked
		// twice once when the object is closed and once when ProcessorManager is finished
		// with this processor.
		if ( isCollecting == true )
		{
			collectedObjects.add( DIORef );
			isCollecting = false;
			if (DEBUG){ System.out.println("END^^^^."); }
		}
	}

	//******************************** ImageRefs ******************************************/
	protected void collectEntity( DxfImageObjectReference obj, DxfElementPair pair )
	{
		
		switch ( pair.getCode() )
		{
		case 1:
			obj.setUrl( pair.getValue() );
			break;

		case 5:		// Handle of object
			obj.setHardReference( pair.getValue() );
			break;

		default:
			break;

		} // end switch
	} // end collectEntity()

} // End of BlockProcessor class.