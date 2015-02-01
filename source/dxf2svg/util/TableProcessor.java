
/****************************************************************************
**
**	FileName:	TableProcessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:
**
**	Date:
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.10 - August 13, 2002 Now collects group code 70 (layer
**				visibility) in collectLayerTableData().
**				1.11 - August 15, 2002 Removed the TableConst interface.
**				1.12 - August 22, 2002 Changed HashMaps to Hashtables for
**				multithreading.
**				1.13 - February 20, 2003 Changed initialization of Hashmaps
**				to be done locally within collectLayerTableData() so Style
**				SheetGenerator can over-write its default tables with any
**				tables collected here. This is currently true for TableStyles.
**				2.00 - April 16, 2005 Modified to accomodate new parser functionality
**				and elimination of DxfElementPair as fly weight.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;					// For vector handling.
import dxf2svg.*;

/**
*	Like the other Processor objects, TableProcessor unpackages the Vector
*	it receives from DxfConverter and searches it for table boundaries. When
*	it finds one it identifies the table type and determines if it is
*	of any interest in our quest to build an SVG. If it is of interest then
*	it creates the required table object and populates it with the DXF data
*	then packs the Table object onto a Hashtable for reference by the
*	StyleSheetGenerator object.<BR><BR>
*	<H4>Tables of Interest</H4>
*<UL>
*	<LI> Layer.
*	<LI> Line type.
*</UL>
*
*
*	@see StyleSheetGenerator
*	@see TableLayer
*	@see TableLineType
*	@see TableStyles
*
*	@version	2.0 - April 20, 2005
*	@author		Andrew Nisbet
*/
final class TableProcessor extends Processor
{
	/*
	*	This class defines the different types of tables possible in a Dxf.
	*	Not all of them have meaning to Svg but they are included for future development
	*	or other Dxf reading projects.
	*/

	private final static int LAYER			= 3;	// required
	private final static int LTYPE			= 4;	// required
	private final static int STYLE			= 5;	// required
	
	private Hashtable     hLineTypes;	// for all the line types found
	private Hashtable     hLayers;		// for all the layers in the drawing.
	private Hashtable     hStyles;		// for font information
	private boolean       isCollecting;
	private TableLineType lineType;
	private TableLayer    layer;
	private TableStyles   style;
	private int           objectType;
	private boolean       DEBUG;
	private ProcessorManager processorManager;
	
	
	public TableProcessor( DxfConverter dxfc, ProcessorManager pm )
	{
		super();
		processorManager= pm;
		DxfConverterRef = dxfc;
		DEBUG           = false;
		isCollecting    = false;
		
		hLineTypes      = new Hashtable();
		hLayers         = new Hashtable();
		hStyles         = new Hashtable();
	}

	
	public void process( DxfElementPair pair )
	{
		// We are in the midst of populating a SvgObject.
		if ( isCollecting )
		{
			if (DEBUG){ System.out.println( "...collecting..." ); }
			switch ( objectType )
			{
			case LAYER:
				collectLayerTableData( pair );
				break;
				
			case LTYPE:
				collectLineTypeTableData( pair );
				break;
				
			case STYLE:
				collectStylesTableData( pair );
				break;
				
			default:
				break;
			}
		}
		

		
		// if we are not collecting yet and we find a start of object marker
		// test it to see if we are already collecting an object and if it is 
		// the type of object we want.
		if ( pair.getCode() == 0 )
		{
			if ( pair.getValue().equals( "LTYPE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN line type" ); }
				objectType = LTYPE;
				lineType = new TableLineType( processorManager );
				
				isCollecting = true;
				
				
			}
			else if ( pair.getValue().equals( "LAYER" ) && isCollecting == false )
			{
				
				
				if ( DEBUG ){ System.out.print( "^^^^BEGIN layer" ); }
				objectType = LAYER;
				layer = new TableLayer( DxfConverterRef );
				isCollecting = true;
				
				
			} 
			else if ( pair.getValue().equals( "STYLE" ) && isCollecting == false )
			{
				
				
				if ( DEBUG ){ System.out.print( "^^^^BEGIN style" ); }
				objectType = STYLE;
				style = new TableStyles( processorManager );
				isCollecting = true;
				
				
			} 
			else 
			{
				if ( isCollecting == true )
				{
					switch ( objectType )
					{
					case LAYER:
						hLayers.put( layer.getName(), layer );
						break;
						
					case LTYPE:
						hLineTypes.put( lineType.getLineTypeName(), lineType );
						break;
						
					case STYLE:
						hStyles.put( style.getStyleName(), style );
						break;
						
					default:
						break;
					}
					isCollecting = false;
					if (DEBUG){ System.out.println("END^^^^."); }
					// Re-throw the last pair in the case that the next object is 
					// another required object.
					process( pair );
				}  // end if
				
				
			}  // end else
		}  // end if
	}
	
	
	/** Finishes the object collection.
	*/
	protected void finishObject()
	{
		// Not required because '0','ENDTAB' finalizes each object before 
		// the processor ends.
		return;
	}


    //********************** Layer tables *************************/
	/** This method distills the layers in a Dxf and
	*	converts them into useful objects ready for the remainder of
	*	the conversion process.
	*/
	private void collectLayerTableData( DxfElementPair pair )
	{
		int tmpInt = 0;
		
		switch ( pair.getCode() )
		{
		case 2: // Layer name
			layer.setName( pair.getValue() );
			break;

		case 6: // LineTypeName
			layer.setLineTypeName( pair.getValue() );
			break;

		case 62: // Layer colour
			tmpInt = Integer.parseInt( pair.getValue() );
			layer.setColour( tmpInt );
			break;

		case 70: // Layer locked or frozen etc.
			tmpInt = Integer.parseInt( pair.getValue() );
			layer.setLayerVisible( tmpInt );
			break;

		default:
			break;

		} // end switch
	} // end of member method collectLayerTableData()





    //********************** LineTypes tables *************************/
	/**
	*	Collects data concerned with line type tables. All of the different
	*	line types used in a drawing are stored in tables.
	*/
	private void collectLineTypeTableData( DxfElementPair pair )
	{
		int tmpInt       = 0;
		double tmpDouble = 0.0;
				
		switch ( pair.getCode() )
		{
		case 2: // LineType name like: CONTINUOUS
			lineType.setLineTypeName( pair.getValue() );
			break;

		case 3: // LineType Description like: solid line
			lineType.setDescription( pair.getValue() );
			break;

		case 49: // Set dash dot space length; this takes a double
			tmpDouble = Double.parseDouble( pair.getValue() );
			lineType.setDashDotSpaceLength( tmpDouble );
			break;

		case 73: // Set the number of dash elements in the line
			tmpInt = Integer.parseInt( pair.getValue() );
			lineType.setEleNum( tmpInt );
			break;
			
		default:
			break;

		} // end switch
	} // end of member method collectLineTypeTableData()


    //********************** Styles tables *************************/
	/**
	*	Collects data concerned with style tables. All of the different
	*	text styles used in a drawing are stored in tables. This method
	*	distills these tables down into objects for the rest of the
	*	conversion process.
	*/
	private void collectStylesTableData( DxfElementPair pair )
	{
		int tmpInt       = 0;
		double tmpDouble = 0.0;
		
		switch ( pair.getCode() )
		{
		case 2: // Style name
			style.setStyleName( pair.getValue() );
			break;

		case 3:	// Primary font name.
			style.setPrimaryFontFileName( pair.getValue() );
			break;

		case 40: // Fixed text height
			tmpDouble = Double.parseDouble( pair.getValue() );
			style.setFixedTextHeight( tmpDouble );
			break;

		case 41: // Width factor
			tmpDouble = Double.parseDouble( pair.getValue() );
			style.setWidthFactor( tmpDouble );
			break;

		case 50: // Oblique angle
			tmpDouble = Double.parseDouble( pair.getValue() );
			style.setObliqueAngle( tmpDouble );
			break;

		case 71: // set text generation flag.
			tmpInt = Integer.parseInt( pair.getValue() );
			style.setTextGenFlag( tmpInt );
			break;

		case 1000:
			//	FontFamily name
			style.setFontFamilyName( pair.getValue());
			break;

		default:
			break;

		} // end switch

	} // end of member method collectStylesTableData()
	
	
	/** Returns all the line type tables.
	*/
	public Hashtable getLineTypeInfo()
	{
		return hLineTypes;
	}
	
	/** Returns all the layer tables.
	*/
	public Hashtable getLayerInfo()
	{
		return hLayers;
	}
	
	/** Returns all the style tables.
	*/
	public Hashtable getStylesInfo()
	{
		return hStyles;
	}

} // end of TableProcessor.java
