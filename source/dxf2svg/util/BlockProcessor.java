
/****************************************************************************
**
**	FileName:	BlockProcessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Maps the data from a DXF into equivelant SvgObjects and places
**				them into a ``symbol'' collection
**
**	Date:		September 12, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 12, 2002
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				2.00 - April 16, 2005 Modified to accomodate new parser functionality
**				and elimination of DxfElementPair as fly weight.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;			// For vector handling.
import dxf2svg.*;
import dxf2svg.svg.*;

/**
*	The BlockProcessor reads an Entity Pair List and breaks the Dxf blocks
*	into SvgSymbols.
*
*	@version	2.00 - April 16, 2005
*	@author		Andrew Nisbet
*/
final class BlockProcessor extends Processor
{
	
	protected static final int BLOCK 	= 1;
	protected static final int ENDBLK 	= 2;
	protected static final int NONE		= 0;
	private boolean isCollecting;
	private boolean isCollectingBlockEntities;
	private boolean DEBUG;
	private SvgEntityDeclaration svgEntityDeclaration;
	private Vector vBlocks;             // reference to results vector.
	private Vector vThisBlocksEntities;	// group of entities returned by EntityProc
	private EntityProcessor entityProcessor; // Used to process the block's entities.
	
	
	
	
	
	public BlockProcessor( DxfConverter dxfc, Vector vBlocks )
	{
		super();
		DxfConverterRef           = dxfc;
		vThisBlocksEntities       = new Vector();
		this.vBlocks              = vBlocks;
		isCollecting              = false;
		isCollectingBlockEntities = false;
		DEBUG                     = false;
	}
	
	
	
	/** Makes sense of the current DxfElementPair calfed off of the DXF file.
	*/
	public void process( DxfElementPair pair )
	{	
		
		// if we are not collecting yet and we find a start of object marker
		// test it to see if we are already collecting an object and if it is 
		// the type of object we want.
		if ( pair.getCode() == 0 )
		{
			if ( pair.getValue().equals( "BLOCK" ) )
			{
				if (DEBUG){ System.out.println( "^^^^BEGIN block" ); }
				svgEntityDeclaration = new SvgEntityDeclaration( DxfConverterRef );
				vThisBlocksEntities = new Vector();
				// create the entity processor.
				entityProcessor = new EntityProcessor( DxfConverterRef,  vThisBlocksEntities );
				isCollecting = true;
			} 
			else if ( pair.getValue().equals( "ENDBLK" ) )
			{
				finishObject();
			}
			else
			{	
				isCollectingBlockEntities = true;
			}
		}  // end if
		
		// We are in the midst of populating either the blocks entities or the 
		// block itself.
		if ( isCollectingBlockEntities || isCollecting )
		{
			if (DEBUG){ System.out.println( "...collecting..." + pair.toString() ); }
			collectEntity( svgEntityDeclaration, pair );
		}
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
			entityProcessor.halt();
			if ( vThisBlocksEntities.size() > 0 )
			{
				svgEntityDeclaration.addElements( (Vector)vThisBlocksEntities.clone() );
				vBlocks.add( svgEntityDeclaration );
			}
			isCollectingBlockEntities = false;
			isCollecting = false;
			if (DEBUG){ System.out.println("END^^^^."); }
		}
	}
	
	

	//******************************** SvgEntityDeclaration *******************************/
	/** 
	*	@param myObject the current object SvgEntityDeclaration.
	*	@param pair DxfElementPair code value pair.
	*/
	protected void collectEntity( SvgEntityDeclaration myObject, DxfElementPair pair )
	{

		if ( isCollectingBlockEntities )
		{
			entityProcessor.process( pair );
			return;
		}


		// This code is executed first before the above code.
		switch ( pair.getCode() )
		{
		case 2:
			// This is the blocks name or in Svg parlance the Entity's ID.
			// *NOTE* we don't collect the Handle of this object as an ID.
			String id = DxfPreprocessor.convertToSvgCss( pair.getValue(), true );
			myObject.setObjID( id );
			// needed for name space resolution with attribute definitions ATTDEFs.
			entityProcessor.setBlockName( id );
			break;

		case 4:
			// optional block description. We can put some <desc> tags in
			// a string and add it to the vector.
			if (! pair.getValue().equals("")) // there is something in the value
			{
				String sb = new String();
				sb = "<desc>"+pair.getValue()+"</desc>";
				myObject.addElement(sb);
			}
			break;

		case 8:
			//	Set the layer of this block/symbol.
			myObject.setLayer(pair.getValue());
			break;

		case 70:
			// Block-type flags (bit coded values, may be combined):
			// 1 = This is an anonymous block generated by hatching, associative dimensioning, other internal operations, or an application.
			// 2 = This block has non-constant attribute definitions (this bit is not set if the block has any attribute definitions that are constant, or has no attribute definitions at all).
			// 4 = This block is an external reference (xref).
			// 8 = This block is an xref overlay.
			// 16 = This block is externally dependent.
			// 32 = This is a resolved external reference, or dependent of an external reference (ignored on input).
			// 64 = This definition is a referenced external reference (ignored
			// on input).
 			break;

		default:
			break;

		} // end switch
	} // SvgEntityDeclaration
} // End of BlockProcessor class.