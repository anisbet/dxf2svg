
/****************************************************************************
**
**	FileName:	EntityProcessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Maps the data from a DXF into equivelant SvgObjects
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - September 17, 2002 Added ENDBLK to stop processing in
**				the special case of encountering an endblk reference in the
**				DXF. This only happens in the blocks section, never the
**				the entity section.
**				1.50 - November 28, 2003 Added hyperlinking for SvgText objects.
**				1.60 - January 23, 2004 Added POLYLINE as a processed element.
**				1.70 - October 5, 2004 Added ELLIPSE group code 230 for negative
**				values, if this occurs the sweep_flag of the ellipse is reversed.
**				1.71 - November 17, 2004 Removed conditional processing that restricts
**				setting the fill on an SvgSolid and SvgHatch objects if the coerceColourByLayer
**				switch is set to true.
**				1.72 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				2.00 - April 16, 2005 Modified to accomodate new parser functionality.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;							// For vector handling.
import java.io.*;							// for invoking SvgBuilders file handlers
import dxf2svg.*;
import dxf2svg.svg.*;
import dxf2svg.animation.*;

/**
*	The EntityProcessor maps the data from a DXF into equivelant SvgObjects.
*	<P>
*	EntityProcessor reads the Entity Vector looking for entity boundery markers.
*	When if finds one it determines what type of DXF entity it is and creates
*	the equivilant SvgObject. It then continues reading the Vector, populating
*	the SvgObject's instance data until it reads another entity boundary or
*	reaches the end of the Vector.
*	<P>
*	If the object has a hyperlink, it is also added at this time. The hyperlink
*	is like an HTML &lt;a&gt; tag but contains an xlink:href attribute.
*	<P>
*	Here is a list of Svg objects that can take a hyperlink:
*	<P>
*	<UL>
*	<LI> SvgLine		
*	<LI> SvgCircle		
*	<LI> SvgText		
*	<LI> SvgMultiLineText
*	<LI> SvgArc			
*	<LI> SvgEllipse		
*	<LI> SvgPolyline	
*	<LI> SvgPoint		
*	<LI> SvgInsert		
*	<LI> SvgSolid		
*	<LI> SvgImage		
*	<LI> SvgHatch		
*	</UL>
*
*	@version	1.71 - November 17, 2004
*	@author		Andrew Nisbet
*/

final class EntityProcessor extends Processor
{
	// all the different types of Acad objects.
	private final static int NONE				= 0;
	private final static int THREEDFACE 		= 1;
	private final static int THREEDSOLID		= 2;
	private final static int ACAD_PROXY_ENTITY	= 3;
	private final static int ARC				= 4;
	private final static int ARCALIGNEDTEXT		= 5;
	private final static int ATTDEF				= 6;
	private final static int ATTRIB				= 7;
	private final static int BODY				= 8;
	private final static int CIRCLE				= 9;
	private final static int DIMENSION			= 10;
	private final static int ELLIPSE			= 11;
	private final static int HATCH				= 12;
	private final static int IMAGE				= 13;
	private final static int INSERT				= 14;
	private final static int LEADER				= 15;
	private final static int LINE				= 16;
	private final static int LWPOLYLINE			= 17;
	private final static int MLINE				= 18;
	private final static int MTEXT				= 19;
	private final static int OLEFRAME			= 20;
	private final static int OLE2FRAME			= 21;
	private final static int POINT				= 22;
	private final static int POLYLINE			= 23;
	private final static int RAY				= 24;
	private final static int REGION				= 25;
	private final static int RTEXT				= 26;
	private final static int SEQEND				= 27;
	private final static int SHAPE				= 28;
	private final static int SOLID				= 29;
	private final static int SPLINE				= 30;
	private final static int TEXT				= 31;
	private final static int TOLERANCE			= 32;
	private final static int TRACE				= 33;
	private final static int VERTEX				= 34;
	private final static int VIEWPORT			= 35;
	private final static int WIPEOUT			= 36;
	private final static int XLINE				= 37;


	private int 				objectType;
	private boolean 			isCollecting;
	private boolean 			DEBUG;

	private Vector				svgEntities;
	private SvgElement	        svgElement;
	private SvgAttdef           svgAttdef;                // Decendant of Object, not SvgElement
	
	private boolean             isHyperLinked;            // Does this object carry a hyperlink?
	private boolean             isVertexPolyLine;         // is this object made up of vertexes.
	private boolean             ignoreLocationGroupCodes; // Ignore initial 10, 20 and 30 group codes.
	private SvgDxfHyperlink     hLink;			          // Hyperlink ref if required.
	private double              tmpDoubleX;               // Used to store the x value of 
		// of points for some objects. See MultiLineText as an example.
	private String              blockName; // Name of the block who's entities we are processing
		// not used for standard entity processing, only for attribute definitions ATTDEFs.

	
	

	
	/** Sets <I>VERBOSE</I> mode depending on user preferences set in Dxf2Svg.*/
	public EntityProcessor( DxfConverter dxfc, Vector svgEntities )
	{
		super();
		isCollecting     = false;
		isHyperLinked    = false;
		isVertexPolyLine = false;         // is this object made up of vertexes.
		ignoreLocationGroupCodes = false; // Ignore initial 10, 20 and 30 group codes of vertexes.
		DEBUG            = false;
		DxfConverterRef  = dxfc;
		this.svgEntities = svgEntities;
		tmpDoubleX       = 0.0;
	}
	
	
	// This method is used to identify the block that these entities are being collected
	// from. It is necessary because we need some sort of name space for attdefs so
	// different blocks can contain the same tag names without a clash when attribs go
	// looking for their specific definition by tag name. It is only used in conjuction
	// with blockprocessor and then only with ATTDEFs.
	/** This method is used to set the current block name if the entity processor
	*	has been invoked to parse entities within a block.
	*/
	public void setBlockName(String name)
	{	
		blockName = name;
	}
	
	/** Processes each pair from the DXF file.
	*/
	public void process( DxfElementPair pair )
	{
		// We are in the midst of populating a SvgObject.
		if ( isCollecting )
		{
			if (DEBUG){ System.out.println( "...collecting+++"); }
			switch ( objectType )
			{
			case LINE:
				collectEntity( (SvgLine)svgElement, pair );
				break;
				
			case TEXT:
				collectEntity( (SvgText)svgElement, pair );
				break;
				
			case CIRCLE:
				collectEntity( (SvgCircle)svgElement, pair );
				break;

			case ARC:
				collectEntity( (SvgArc)svgElement, pair );
				break;
				
			case ELLIPSE:
				collectEntity( (SvgEllipse)svgElement, pair );
				break;	
				
			case LWPOLYLINE:
				collectEntity( (SvgPolyLine)svgElement, pair );
				break;
				
			case POLYLINE:
				collectEntity( (SvgPolyLine)svgElement, pair );
				break;

			case POINT:
				collectEntity( (SvgPoint)svgElement, pair );
				break;
				
			case INSERT:
				collectEntity( (SvgEntityReference)svgElement, pair );
				break;

			case IMAGE:
				collectEntity( (SvgImage)svgElement, pair );
				break;
				
			case SOLID:
				collectEntity( (SvgSolid)svgElement, pair );
				break;
				
			case SPLINE:
				collectEntity( (SvgSpline)svgElement, pair );
				break;
				
			case MTEXT:
				collectEntity( (SvgMultiLineText)svgElement, pair );
				break;
				
			case HATCH:
				collectEntity( (SvgHatch)svgElement, pair );
				break;
			
			case DIMENSION:
				collectEntity( (SvgDimension)svgElement, pair );
				break;
				
			case ATTRIB:
				collectEntity( (SvgAttrib)svgElement, pair );
				break;

			case ATTDEF:
				collectEntity( svgAttdef, pair );
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
			if ( pair.getValue().equals( "LINE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgLine" ); }
				objectType = LINE;
				svgElement = new SvgLine( DxfConverterRef );
				isCollecting = true;
			}
			else if ( pair.getValue().equals( "TEXT" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgText" ); }
				objectType = TEXT;
				svgElement = new SvgText( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "CIRCLE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgCircle" ); }
				objectType = CIRCLE;
				svgElement = new SvgCircle( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "ARC" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgArc" ); }
				objectType = ARC;
				svgElement = new SvgArc( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "ELLIPSE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgEllipse" ); }
				objectType = ELLIPSE;
				svgElement = new SvgEllipse( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "LWPOLYLINE" ) && isCollecting == false )
			{
				//if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgPolyLine" ); }
				objectType = LWPOLYLINE;
				svgElement = new SvgPolyLine( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "POLYLINE" ) && isCollecting == false )
			{
				//if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgPolyLine" ); }
				objectType = POLYLINE;
				svgElement = new SvgPolyLine( DxfConverterRef );
				isCollecting = true;				
			}
			// here we have a slight diversion from other objects. If the 
			// next element is a VERTEX then keep processing. Vertexes have
			// similar information to polylines where the VERTEX is a desrete
			// object onto itself.
			else if ( pair.getValue().equals("VERTEX") )
			{
			 	// go through and gather all the same codes but from
				// vertexes instead of polylines. Nice that they match polyline's.
				// Identify as vertexed polyline so we can interpret code 70.
				isVertexPolyLine = true;
			}
			else if ( pair.getValue().equals( "POINT" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgPoint" ); }
				objectType = POINT;
				svgElement = new SvgPoint( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "INSERT" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgEntityReference" ); }
				objectType = INSERT;
				svgElement = new SvgEntityReference( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "IMAGE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgImage" ); }
				objectType = IMAGE;
				svgElement = new SvgImage( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "SOLID" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgSolid" ); }
				objectType = SOLID;
				svgElement = new SvgSolid( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "SPLINE" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgSpline" ); }
				objectType = SPLINE;
				svgElement = new SvgSpline( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "MTEXT" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgMultiLineText" ); }
				objectType = MTEXT;
				svgElement = new SvgMultiLineText( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "HATCH" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgHatch" ); }
				objectType = HATCH;
				svgElement = new SvgHatch( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "DIMENSION" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgDimension" ); }
				objectType = DIMENSION;
				svgElement = new SvgDimension( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "ATTRIB" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgAttrib" ); }
				objectType = ATTRIB;
				svgElement = new SvgAttrib( DxfConverterRef );
				isCollecting = true;				
			}
			else if ( pair.getValue().equals( "ATTDEF" ) && isCollecting == false )
			{
				if ( DEBUG ){ System.out.print( "^^^^BEGIN SvgAttribDef" ); }
				objectType = ATTDEF;
				svgAttdef = new SvgAttdef( );
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
			} // end else
		} // end pair == 0
	} // end process( pair )
	
	
	
	
	/** Finishes the object collection.
	*/
	protected void finishObject()
	{
		
		// Special case of Attribute Definition
		if ( objectType == ATTDEF )
		{
			if ( svgAttdef != null )
			{
				//StyleSheetGenerator SSG = DxfConverterRef.getStyleSheetGenerator();
				//SSG.addAttdef( svgAttdef );
				if ( DEBUG ){ System.out.println(">>>attdef: '"+svgAttdef.toString()+"'"); }
			}
		}
		else
		{
			if ( isHyperLinked )
			{
				hLink.setLayer( svgElement.getLayer() );
				hLink.addElement( svgElement );
				svgEntities.add( hLink );
				isHyperLinked = false;
			}
			else
			{
				if ( svgElement != null )
				{
					svgEntities.add( svgElement );
				}
			}
		}
		
		
		isCollecting = false;
		isVertexPolyLine = false;         // is this object made up of vertexes.
		ignoreLocationGroupCodes = false; // Ignore initial 10, 20 and 30 group codes.
		if ( DEBUG ){ System.out.println( "END^^^^." ); }		
	}

	
	
	
	
	/////////////////////////////// special case /////////////////////////////////////
	//********************** Attribute Definition (Attdef) *************************/
	/**
	*	This method collects data on behalf of a SvgAttdef.
	*/
	private void collectEntity( SvgAttdef myObject, DxfElementPair pair )
	{
		/*
		*	Proceed through the list until you come to another zero and tell StyleSheetGenerator
		*	that you have finished with that table
		*/
		int tmpInt;					// temp integer for determining visibility flag.
		switch ( pair.getCode() )
		{
		case 2: // Tag or var name of the definition
			myObject.setTag( blockName + "." + pair.getValue() );
			break;

		case 70: // Is the attribute visible.
			tmpInt = Integer.parseInt( pair.getValue() );
			// we only check to see if one is set 'cause if it is
			// then we need to flip visibility. There may be other
			// uninteresting flags aswell.
			if ((tmpInt & 1) == 1)
				myObject.setAttributeVisibility( false );
			else
				myObject.setAttributeVisibility( true );
			break;

		default:
			break;

		} // end switch
	} // end of member method collectStylesTableData()
	/////////////////////////////// special case (end) /////////////////////



// *********************** PolyMorphic object handlers here ********/
/******************************** SvgAttrib ******************************************/

	protected void collectEntity( SvgAttrib myObject, DxfElementPair pair )
	{

		double tmpDouble;		
		int tmpInt;				// tmp integer parsing variable


		switch ( pair.getCode() )
		{
		case 1:
			// Sets the string content of this object.
			myObject.setContent(pair.getValue());
			break;

		case 2:
			// Sets the tag name for the attribute.
			myObject.setTag(pair.getValue());
			break;

		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 7:
			//	Style name.
			myObject.setStyle(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		//case 11:
			//	set this object's Second alignment Point
			//tmpDoubleX = Double.parseDouble(pair.getValue());
			//break;

		//case 21:
			//	set this object's Second alignment Point
			//tmpY = Double.parseDouble(pair.getValue());
			//myObject.createSecondAlignmentPoint(tmpDoubleX,tmpY);
			//break;

		case 40:	// font size
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setFontSize(tmpDouble);
			break;

		case 41:	// Width factor
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setWidthFactor(tmpDouble);
			break;

		case 50:	// rotation angle
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRotation(tmpDouble);
			break;

		case 51:	// Oblique angle
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setObliqueAngle(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setFill(tmpInt);
			}
			break;

		case 70:
			// This sets the attribute flag which is a bit
			// value that primarily, for our purposes, controls
			// visibility.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setAttributeFlag(tmpInt);
			break;

		case 72:
			//	The justification of the text horizontally.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setHorizontalJustification(tmpInt);
			break;

		default:
			break;

		} // end switch
	}





// **************************** SvgDimension ***********************/
	protected void collectEntity( SvgDimension myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		/* Call myObject's member functions depending on what type of data is found in the entity entry
		* and populate the object's instance data.  You will have to test for almost all conditions
		* that can occur for Dxf entity entries but it will ignore unrecognized data and is easily
		* adapted to changes in Dxf specification.
		*/

		switch ( pair.getCode() )
		{
		case 2:
			// The block name will become the Insert's URL.
			// We will assume that this block is stored in the
			// local file and so needs a '#'. This could change
			// for an XREF which is contained in an external file
			// and looks like 'externFile#reference'.
			myObject.setReferenceURL("#"+pair.getValue());
			break;

		case 5:
			//	ID.
			myObject.setObjID(pair.getValue());
			break;

		case 8:
			//	Layer.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 53:	// rotation.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRotation(tmpDouble);
			break;

		default:
			break;

		} // end switch

	}
// ******************************** SvgHatch ************************/
	protected void collectEntity( SvgHatch myObject, DxfElementPair pair )
	{
		double tmpDouble;		        // temp for parsing double values
		double tmpDoubleX = 0.0;	    // a temp double for x.
		double tmpDoubleY = 0.0;	    // a temp double for y.
		int tmpInt;				        // a temp integer value for linetype detection
		
		switch ( pair.getCode() )
		{
		case 2:	// name of hatch which will match <defs> pattern </defs>
			myObject.setPatternName(pair.getValue());
			break;

		case 5:
			//	Name, Dxf Handle.
			myObject.setObjID(pair.getValue());
			break;

		case 8:
			//	Layer name.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			// group code 10
			tmpDoubleX = Double.parseDouble(pair.getValue());
			myObject.setGroupCode10(tmpDoubleX);
			break;

		case 20:
			// group code 20
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.setGroupCode20(tmpDoubleY);
			break;

		case 11:
			// group code 11
			tmpDoubleX = Double.parseDouble(pair.getValue());
			myObject.setGroupCode11(tmpDoubleX);
			break;

		case 21:
			// group code 21
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.setGroupCode21(tmpDoubleY);
			break;

		case 40:	// Circular arc radius or length of minor axis
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setGroupCode40(tmpDouble);
			break;

		case 41:	// Scale of hatch pattern.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setHatchPatternScale(tmpDouble);
			break;

		case 42:	// bulge in line edge or weights in spline
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setGroupCode42(tmpDouble);
			break;

		case 43:	// pattern line base point's X value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPatternBaseX(tmpDouble);
			break;

		case 44:	// pattern line base point's Y value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPatternBaseY(tmpDouble);
			break;

		case 45:	// pattern line offset point's X value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPatternLineOffsetX(tmpDouble);
			break;

		case 46:	// pattern line offset point's Y value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPatternLineOffsetY(tmpDouble);
			break;

		case 49:	// dash length (multipule entries).
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setDashLength(tmpDouble);
			break;

		case 50:	// start angle for Circ and ellip arcs
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setStartAngle(tmpDouble);
			break;

		case 51:	// end angle
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setEndAngle(tmpDouble);
			break;

		case 53:	// pattern line angle.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPatternLineAngle(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setFill(tmpInt);
			break;

		case 70: // solid or pattern fill
			tmpInt = Integer.parseInt(pair.getValue());
			if (tmpInt > 0)
				myObject.setSolidFill(true);
			else
				myObject.setSolidFill(false);
			break;

		case 72: // Sets edge type
				// 0 = polyline, 1 = line, 2 = circular arc, 3 = elliptical arc
				// 4 = spline.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setEdgeType(tmpInt);
			break;

		case 73:	// Sets the 'isClosedFlag'.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setGroupCode73(tmpInt);
			break;

		case 75: // Sets hatch style
				// 0 = odd parity, 1 = outermost, 2 = hatch entire area
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setHatchStyle(tmpInt);
			break;

		case 79: // Number of dash length items
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setNumberOfDashLengthItems(tmpInt);
			break;

		case 91:	// number of loops or paths in this hatch
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setNumberOfPaths(tmpInt);
			break;

		case 92: // Sets number of edges or vertices (if Polyline)
			// also trigers collection of a new boundary path.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setGroupCode92(tmpInt);
			break;

		//case 93: // Sets number of edges or vertices (if Polyline)
		//	// also trigers collection of a new boundary path.
		//	tmpInt = Integer.parseInt(pair.getValue());
		//	myObject.setGroupCode93(tmpInt);
		//	break;

		case 98: // Sets number of seed points
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setNumberOfSeedPoints(tmpInt);
			break;
			
		case 1001:  // Potential URL String in next pair.
			if ( pair.getValue().equalsIgnoreCase( "PE_URL" ) )
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink( DxfConverterRef );
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				// set xmlns:xlink switch use this abstaction so we can
				// reuse SvgDxfHyperlink outside of the context of Dxf2Svg.
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}	// end SvgHatch


/******************************** SvgMultiLineText ******************************************/

	protected void collectEntity( SvgMultiLineText myObject, DxfElementPair pair )
	{

		double tmpDouble;		// a temp double for same purpose.
		double tmpY = 0.0;		// More Temp doubles. Used grp 11, 21.
		int tmpInt;				// pair integer parsing variable


		switch ( pair.getCode() )
		{
		case 1:
			// Text string. In MTEXT this is either the first and only
			// string or it is the last string. This depends on whether
			// there are any text strings defined with group code '3'.
			// All text fields will have 250 characters or less. The
			// string that contains less than 250 will be code '1'.
			myObject.setContent(pair.getValue());
			break;

		case 3:
			// This means that group code 1 couldn't hold all the
			// data that has to be output, so the first 250 characters
			// will be put on a Vector. Any number of characters left
			// over (less than 250) will be put in the initial string.
			myObject.setGroupCodeThree(true);
			myObject.addAnotherString(pair.getValue());
			break;

		case 5:
			//	Object ID.
			myObject.setObjID(pair.getValue());
			break;

		case 7:
			//	Style name.
			myObject.setStyle(pair.getValue());
			break;

		case 8:
			//	Layer.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 11:
			//	set this object's rotation angle vector x
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 21:
			//	set this object's rotation angle vector y
			tmpY = Double.parseDouble(pair.getValue());
			myObject.setXAxisDirectionVector(tmpDoubleX,tmpY);
			break;

		case 40:	// font size
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setFontSize(tmpDouble);
			break;

		case 41:	// Reference rectangle width
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setMaxLineWidth(tmpDouble);
			break;

		case 42:	// Horizontal width factor of the characters.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setWidthFactor(tmpDouble);
			break;

		case 44:	// Line spacing factor. Percentage of default.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineSpacingFactor(tmpDouble);
			break;

		case 50:	// rotation angle
			// may over write any value collected by group code
			// 11 and 21.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRotation(tmpDouble);
			break;

		//case 51:	// Oblique angle (not possible in mtext)
		//	tmpDouble = Double.parseDouble(pair.getValue());
		//	myObject.setObliqueAngle(tmpDouble);
		//	break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setFill(tmpInt);
			}
			break;

		case 71:
			// In reality this is fixable but so rare that it doesn't
			// warrant the time it would take to fix right now.
			//System.err.println("EntityProcessor Warning: unsupportable"+
			//	" justification request, MTEXT attachment point grp"+
			//	" code 71. Ignoring.");
			//	Justification as integer.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setJustification(tmpInt);
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;

		default:
			break;

		} // end switch
	}

// ******************************** SvgSpline ************************/
	protected void collectEntity( SvgSpline myObject, DxfElementPair pair )
	{

		double tmpDouble;		// temp for parsing double values
		double tmpDoubleY = 0.0;// a temp double for y.
		int tmpInt;				// a temp integer value for linetype detection

		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			// This represents the x value of control points
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 20:
			// This represents the y value of control points
			// now we pass both to the method that in turn will convert them into a Point
			// and pack it onto the SvgSpline's Control points Vector.
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.addControlPoint(tmpDoubleX,tmpDoubleY);
			break;

		case 11:
			// This represents the x value of fit points
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 21:
			// This represents the y value of fit points
			// now we pass both to the method that in turn will convert them into a Point
			// and pack it onto the SvgSpline's Control points Vector.
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.addFitPoint(tmpDoubleX,tmpDoubleY);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;

		case 70:
			//	Spline flag (bit encoded).
			//	1 = Closed spline.
			//	2 = Periodic spline.
			//	4 = Rational spline.
			//	8 = Planar.	Generic spline.
			//	16 = Linear (planar bit is also set).
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setSplineFlag(tmpInt);
			break;

		default:
			break;

		} // end switch
	}	// end SvgSpline

// ******************************** SvgSolid ************************/
	protected void collectEntity( SvgSolid myObject, DxfElementPair pair )
	{

		double tmpDouble;		        // temp for parsing double values
		double tmpDoubleY = 0.0;        // a temp double for y.
		int tmpInt;				        // a temp integer value for linetype detection

		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

			//**************** Corner 1 ***************/
		case 10:
			// These two cases are different for this function as we are dealing with
			// repeated use of the SvgPolyLine's inherited Anchor point which we
			// pack onto a Vector over and over for as many points as there are.

			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDoubleY = Double.parseDouble(pair.getValue());
			// now we pass both to the method that in turn will convert them into a Point
			// and pack it onto the PolyLinePoints Vector.
			myObject.setVertex(tmpDoubleX,tmpDoubleY);
			break;

		//**************** Corner 2 ***************/
		case 11:	// repeat for corner two
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 21:
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.setVertex(tmpDoubleX,tmpDoubleY);
			break;

		//**************** Corner 3 ***************/
		case 12:	// repeat for corner three
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 22:
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.setVertex(tmpDoubleX,tmpDoubleY);
			break;

		//**************** Corner 4 ***************/
		case 13:	// repeat for corner four
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 23:
			tmpDoubleY = Double.parseDouble(pair.getValue());
			myObject.setVertex(tmpDoubleX,tmpDoubleY);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setFill(tmpInt);
			break;

		case 70:
			//	Is the object closed.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setClosed(tmpInt);
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}	// end SvgSolid




	// ******************************** SvgImage **************************/

	protected void collectEntity( SvgImage myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection
		/* Call myObject's member functions depending on what type of data is found in the entity entry
		* and populate the object's instance data.  You will have to test for almost all conditions
		* that can occur for Dxf entity entries but it will ignore unrecognized data and is easily
		* adapted to changes in Dxf specification.
		*/
		switch ( pair.getCode() )
		{
		case 5:
			//	ID.
			myObject.setObjID(pair.getValue());
			break;

		case 8:
			//	Layer.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 11:
			//	Size of pixel horizontally (U-Vector).
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPixelWidth(tmpDouble);
			break;

		case 22:
			//	Size of pixel vertically (U-Vector).
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setPixelHeight(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's Y value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 13:	// Image width. It is a double in Dxf but
					// I convert it to int in SvgImage class
					// because it makes no sense to have a double
					// representation of 'number of pixels'.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setImageWidth(tmpDouble);
			break;

		case 23:	// Image height
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setImageHeight(tmpDouble);
			break;

		case 340:
			// This represents the ImageRef to the... er image.
			// To get a meaningful value we have to collect this
			// value and query the Hashtable created by ObjectProcessor.
			// The value returned by this process is the URI.
			myObject.setReferenceHandle( pair.getValue() );
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}
// ******************************** SvgEntityReference (Insert) *********************************/
	protected void collectEntity( SvgEntityReference myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection
		/* Call myObject's member functions depending on what type of data is found in the entity entry
		* and populate the object's instance data.  You will have to test for almost all conditions
		* that can occur for Dxf entity entries but it will ignore unrecognized data and is easily
		* adapted to changes in Dxf specification.
		*/
		switch ( pair.getCode() )
		{
		case 2:
			// Entity reference name (the name of the block.)
			myObject.setEntityReferenceName(pair.getValue());
			break;

		case 5:
			//	ID.
			myObject.setObjID(pair.getValue());
			break;

		case 8:
			//	Layer.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 41:	// scale along x axis.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setScaleX(tmpDouble);
			break;

		case 42:	// scale along y axis.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setScaleY(tmpDouble);
			break;

		case 50:	// rotation.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRotation(tmpDouble);
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}
	
	
	
	
	
	
// ******************************** SvgLine ******************************************/

	protected void collectEntity( SvgLine myObject, DxfElementPair pair )
	// we need to pass the object type to the function for it
	// to know which functions to call. All other objects inherit
	// from the SvgObject class so I wonder if we pass an SvgObject
	// or a basic Object. hmmmmmm...
	{
		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection
		/* Call myObject's member functions depending on what type of data is found in the entity entry
		* and populate the object's instance data.  You will have to test for almost all conditions
		* that can occur for Dxf entity entries but it will ignore unrecognized data and is easily
		* adapted to changes in Dxf specification.
		*/
		switch ( pair.getCode() )
		{
		case 2:
			// This is case only occures in the subclass BlockProcessor when
			// we come across a block name. The block name will eventually
			// become added to the symbol's class.
			break;

		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers so pass the string.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 11:
			// set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setEndPointX(tmpDouble);
			break;

		case 21:
			// and again
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setEndPointY(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	} // SvgLine



/******************************** SvgText ******************************************/

	protected void collectEntity( SvgText myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// pair integer parsing variable

		switch ( pair.getCode() )
		{
		case 1:
			//	.
			myObject.setContent(pair.getValue());
			break;

		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 7:
			//	Style name.
			myObject.setStyle(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		//case 11:
			//	set this object's Second alignment Point
			//tmpDoubleX = Double.parseDouble(pair.getValue());
			//break;

		//case 21:
			//	set this object's Second alignment Point
			//tmpY = Double.parseDouble(pair.getValue());
			//myObject.createSecondAlignmentPoint(tmpDoubleX,tmpY);
			//break;

		case 40:	// font size
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setFontSize(tmpDouble);
			break;

		case 41:	// Width factor
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setWidthFactor(tmpDouble);
			break;

		case 50:	// rotation angle
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRotation(tmpDouble);
			break;

		case 51:	// Oblique angle
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setObliqueAngle(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setFill(tmpInt);
			}
			break;

		case 72:
			//	The justification of the text horizontally.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setHorizontalJustification(tmpInt);
			break;

		case 73:
			//	The justification of the text horizontally.
			tmpInt = Integer.parseInt(pair.getValue());
			myObject.setVerticalJustification(tmpInt);
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
				//hLink.openLinkInNewWindow(false); // default behaviour
				//hLink.setFrameTarget("someFrame");
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				// set xmlns:xlink switch use this abstaction so we can
				// reuse SvgDxfHyperlink outside of the context of Dxf2Svg.
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
		
		default:
			break;

		} // end switch
	} // SvgText




// ******************************** SvgObject (Default) **************/
	//	This provides a tighter control on the types of information collected
	//	for each type of SvgObject. It also speeds up the process by short
	//	cutting the number of cases that have to be tested.
	/**
	*	This method populates the SvgObjects with their data from the
	*	DxfElementPair List. There is one over-loaded method for each SvgObject type.
	*	@param myObject svg target object 
	*	@param pair DxfElementPair.
	*/
	protected void collectEntity( SvgObject myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		switch ( pair.getCode() )
		{
		case 100:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setContent(pair.getValue());
			break;

		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		default:
			break;

		} // end switch
	} // SvgObject



// ******************************** SvgCircle **************************/
	protected void collectEntity( SvgCircle myObject, DxfElementPair pair )
	{

		double tmpDouble;		        // a temp double for same purpose.
		int tmpInt;				        // a temp integer value for linetype detection
		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 40:
			//	set this object's Radius to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRadius(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
				//hLink.openLinkInNewWindow(false); // default behaviour
				//hLink.setFrameTarget("someFrame");
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				// set xmlns:xlink switch use this abstaction so we can
				// reuse SvgDxfHyperlink outside of the context of Dxf2Svg.
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch	
	}// SvgCircle


// ******************************** SvgArc ******************************************/

	protected void collectEntity( SvgArc myObject, DxfElementPair pair )
	{

		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection

		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			//	cast as a double.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 40: 	// set radius.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setRadius(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 50:	//start angle

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setStartAngle(tmpDouble);
			break;

		case 51:	// End angle

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setEndAngle(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
				//hLink.openLinkInNewWindow(false); // default behaviour
				//hLink.setFrameTarget("someFrame");
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				// set xmlns:xlink switch use this abstaction so we can
				// reuse SvgDxfHyperlink outside of the context of Dxf2Svg.
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}// SvgArc


// ******************************** SvgEllipse ******************************************/


	protected void collectEntity( SvgEllipse myObject, DxfElementPair pair )
	{
		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection

		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 11:

			tmpDouble = Double.parseDouble(pair.getValue());
			// these  two have to be handled since the point is relative to the center point
			// not absolute as assumed here. Convertion done in SvgEllipse().
			myObject.setMajorAxisEndPointXUU(tmpDouble);
			break;

		case 21:

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setMajorAxisEndPointYUU(tmpDouble);
			break;

		case 40:

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setMinorToMajorRatio(tmpDouble);
			break;

		case 41:

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setStartAngle(tmpDouble);
			break;

		case 42:

			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setEndAngle(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		// Added October 5, 2004.
		case 230:	// extrusion direction 'z' value.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setZExtrusionDirectionValue(tmpDouble);
			break;
		// Added October 5, 2004.
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}// SvgEllipse

// ******************************** SvgPolyLine ************************/
	protected void collectEntity( SvgPolyLine myObject, DxfElementPair pair )
	{

		double tmpDouble;		    // temp for parsing double values
		double tmpDoubleY = 0.0;	// a temp double for y.
		int tmpInt;				    // a temp integer value for linetype detection
		
		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 6:
			//	Line types handled here.
			myObject.setLineType(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;


		case 10:
			// These two cases are different for this function as we are dealing with
			// repeated use of the SvgPolyLine's inherited Anchor point which we
			// pack onto a Vector over and over for as many points as there are.

			//	set this object's Anchor Point to it's value
			//	cast as a double.
			if (ignoreLocationGroupCodes == true)
			{
				break;
			}
			tmpDoubleX = Double.parseDouble(pair.getValue());
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			//	cast as a double.
			if (ignoreLocationGroupCodes == true)
			{
				ignoreLocationGroupCodes = false; // all but the first set of location group
				// codes are significant so all initial location group codes from on are to
				// be read.
				break;
			}
			tmpDoubleY = Double.parseDouble(pair.getValue());
			// now we pass both to the method that in turn will convert them into a Point
			// and pack it onto the PolyLinePoints Vector.
			myObject.setVertex(tmpDoubleX,tmpDoubleY);
			break;

		case 42:	// set bulge in polyline segment.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setBulge(tmpDouble);
			break;

		case 43:	//	set line weight.
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineWeight(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		case 66:	// "Entities follow" flag (fixed)
			// since a 10, 20 and 30 group code follow, and they are just 0 ignore them
			// or else you get lines from the 0,0 DXF space to the initial vertex.
			ignoreLocationGroupCodes = true;
			break;

		case 70:
			//	Layers are all sorts of names and not just numbers.
			tmpInt = Integer.parseInt(pair.getValue());
			if(isVertexPolyLine)
			{
				// what happens now?
			}
			else
			{
				myObject.setClosed(tmpInt);
			}
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch
	}


// ******************************** SvgPoint ******************************************/

	protected void collectEntity( SvgPoint myObject, DxfElementPair pair )
	{

		double tmpDouble;		// a temp double for same purpose.
		int tmpInt;				// a temp integer value for linetype detection
		switch ( pair.getCode() )
		{
		case 5:
			//	Names are Dxf Handles and are strings of hex numbers.
			myObject.setObjID(pair.getValue());
			break;

		case 8:
			//	Layers are all sorts of names and not just numbers.
			myObject.setLayer(pair.getValue());
			break;

		case 10:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setX(tmpDouble);
			break;

		case 20:
			//	set this object's Anchor Point to it's value
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setY(tmpDouble);
			break;

		case 48:	//	set LineTypeScale
			tmpDouble = Double.parseDouble(pair.getValue());
			myObject.setLineTypeScale(tmpDouble);
			break;

		case 62:
			//	This is the colour value as integer.
			if (DxfPreprocessor.isColourCoercedByLayer() == false)
			{
				tmpInt = Integer.parseInt(pair.getValue());
				myObject.setColour(tmpInt);
			}
			break;
			
		case 1001:  // Potential URL String in next pair.
			if (pair.getValue().equalsIgnoreCase("PE_URL"))
			{
				isHyperLinked = true;
				hLink = new SvgDxfHyperlink(DxfConverterRef);
			}
			break;
		
		case 1000:  // This represents the actual hyperlink data. The value 1000
					// is repeated twice in a DXF so this will get run twice.
			if (isHyperLinked)
			{
				hLink.setXLink(pair.getValue());
				DxfPreprocessor.setUsesLinks(true);
			}
			break;
			
		default:
			break;

		} // end switch

	} // end SvgPoint
	
} // EOF EntityProcessor