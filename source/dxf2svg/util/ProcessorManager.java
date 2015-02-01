
/****************************************************************************
**
**	FileName:	ProcessorManager.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Controls the processors and manages their output.
**
**	Date:		August 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - April 14, 2005
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.DxfConverter;
import dxf2svg.StyleSheetGenerator;
import java.util.Vector;


/** This object manages the various processors switching states as the Dxf file
*	is interpreted. It also mediates all requests for the output Svg Elements. All
*	Svg element creation takes place within this class.
*	
*	@author	 Andrew Nisbet
*	@version 1.0 - April 14, 2005
*/
public final class ProcessorManager
{
	private Processor processor;             // A generic processor object.
	private DxfConverter conversionContext;  // DxfConverterRef in all other files.
	private SvgUtil svgUtil;                 // Singleton instance of this object.
	private StyleSheetGenerator SSG;         // Singleton instance of this object.
	private Vector svgObjects;               // Destination of SvgOjbects
	private Vector svgEntities;              // Destination of SvgEntities
	private Vector svgBlocks;                // Destination of SvgBlocks
	
	
	/** Constructor */
	public ProcessorManager( 
		DxfConverter dxfc, 
		Vector svgObjects, 
		Vector svgEntities, 
		Vector svgBlocks
	)
	{
		conversionContext = dxfc;
		
		this.svgObjects 	= svgObjects;
		this.svgEntities 	= svgEntities;
		this.svgBlocks      = svgBlocks;
	}
	
	
	/** Method for passing DxfElementPairs to the processor Manager object.
	*/
	public void setDxfElementPair( DxfElementPair pair )
	{
		processor.process( pair );
	}
	
	
	/** Creates and return an instance of the SvgUtil.
	*/
	public SvgUtil getSvgUtilInstance()
	{
		return svgUtil;
	}
	
	/** Returns the instance of the style sheet generator for this conversion context.
	*/
	public StyleSheetGenerator getStyleSheetGeneratorInstance()
	{
		return SSG;
	}
	
	/** Used to signal the end of a section of a DXF. Section ends have 
	*	significance to objects like HeaderProcessor which needs to make
	*	an SvgUtil object and TableProcessor which needs to make a Style
	*	Sheet Generator object right away so it is ready for the next 
	*	section.
	*/
	public void endSection()
	{
		processor.halt();
		
		if ( processor instanceof HeaderProcessor )
		{
			double minX  = ((HeaderProcessor)processor).getLimitsMinX();
			double minY  = ((HeaderProcessor)processor).getLimitsMinY();
			double maxX  = ((HeaderProcessor)processor).getLimitsMaxX();
			double maxY  = ((HeaderProcessor)processor).getLimitsMaxY();
			double scale = ((HeaderProcessor)processor).getLtScale();
			svgUtil = new SvgUtil( minX, minY, maxX, maxY, scale );
			conversionContext.setSvgUtility( svgUtil );			
		}
		else if ( processor instanceof TableProcessor )
		{
			SSG = new StyleSheetGenerator( this );
			SSG.setLayerInfo(    ((TableProcessor)processor).getLayerInfo()    );
			SSG.setStylesInfo(   ((TableProcessor)processor).getStylesInfo()   );
			SSG.setLineTypeInfo( ((TableProcessor)processor).getLineTypeInfo() );
			conversionContext.setStyleSheetGenerator( SSG );			
		}
	}
	
	
	/** Sets the current Dxf Section being processed.
	*/
	public void setDxfSection( int section )
	{
		switch ( section )
		{
		case DxfParser.HEADER:
			processor = new HeaderProcessor();
			break;
			
		case DxfParser.CLASSES:
			break;
		 
		case DxfParser.TABLES:
			processor = new TableProcessor( conversionContext, this );
			break;
			
		case DxfParser.BLOCKS:
			processor = new BlockProcessor( conversionContext, svgBlocks );
			break;
			
		case DxfParser.ENTITIES:
			processor = new EntityProcessor( conversionContext, svgEntities );
			break;
			
		case DxfParser.OBJECTS:
			processor = new DxfObjectProcessor( conversionContext, svgObjects );
			break;
			
		default:
			System.err.println("ProcessorManager.setDxfSection(): Unrequired section."+section);
			break;
		}
	} // setDxfSection()
} // end class.