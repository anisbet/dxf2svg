
/****************************************************************************
**
**	FileName:	HeaderProcessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - August 15, 2002 Added $LTSCALE data collection.
**				1.02 - August 29, 2002 Made this a subclass of Processor.
**				1.50 - September 9, 2002 Made the class non-static in
**				preparation for Thread safty.
**				2.00 - April 16, 2005 Modified to accomodate new parser functionality
**				and elimination of DxfElementPair as fly weight.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

/**
*	This class reads the container (Vector) of variables from the DXF header
*	and populates variables that will be useful for generating the SVG.
*
*	This class could become quite big if you implemented the complete DXF
*	variable set as AutoCAD defines some 70 variables.
*
*	@version	2.00 - April 16, 2005
*	@author		Andrew Nisbet
*/

final class HeaderProcessor extends Processor
{
	private static final int S_NONE = 0;
	private static final int S_LMAX = 1;
	private static final int S_LMIN = 2;
	private static final int S_SCALE= 3;
	private static final int S_JOIN = 4;
	
	private int currentVarState;
	
	private double	LimitsMaxX; 	// X drawing Limits upper-right corner
	private double	LimitsMaxY; 	// Y drawing Limits upper-right corner
	private double	LimitsMinX;		// X drawing Limits lower-left corner
	private double	LimitsMinY;		// Y drawing Limits lower-left corner
	private double	LtScale;		// $LTSCALE Global Linetype scale.  
	

	/**
	*	Default constructor just sets the VERBOSE mode for activity logging.
	*/
	public HeaderProcessor()
	{
		super();
		currentVarState = S_NONE;
	}



	/**
	*	Processes the ElementPairs list (Vector) and populates key SVG variables.
	*
	*	All the header ElementPairs are placed within a Vector and the Vector
	*	is traversed to extract useful information to be used later during
	*	the SVG building process.  Much of the information inside the header
	*	of the DXF is current application state information and is not required.
	*/
	public void process( DxfElementPair pair )
	{

		if ( pair.getValue().equals("$LIMMAX") )
		{
			currentVarState = S_LMAX;
		}
		
		else if ( pair.getValue().equals("$LIMMIN") )
		{
			currentVarState = S_LMIN;
		}
		
		else if ( pair.getValue().equals("$LTSCALE") )
		{
			currentVarState = S_SCALE;
		}
		
		else if ( pair.getValue().equals("$JOINSTYLE") )
		{
			currentVarState = S_JOIN;
		}
		
		else if ( pair.getCode() == 10 )
		{
			if ( currentVarState == S_LMAX )
			{
				LimitsMaxX = Double.parseDouble( pair.getValue() );
			}
			else if ( currentVarState == S_LMIN )
			{
				LimitsMinX = Double.parseDouble( pair.getValue() );
			}
		}
		
		else if ( pair.getCode() == 20 )
		{
			if ( currentVarState == S_LMAX )
			{
				LimitsMaxY = Double.parseDouble( pair.getValue() );
				currentVarState = S_NONE;
			}
			else if ( currentVarState == S_LMIN )
			{
				LimitsMinY = Double.parseDouble( pair.getValue() );
				currentVarState = S_NONE;
			}
		}
		
		else if ( pair.getCode() == 40 )
		{		
			if ( currentVarState == S_SCALE )
			{
				LtScale = Double.parseDouble( pair.getValue() );
				currentVarState = S_NONE;
			}
		}

		else if ( pair.getCode() == 280 )
		{		
			if ( currentVarState == S_JOIN )
			{
				int myInt = Integer.parseInt( pair.getValue() );
				switch ( myInt )
				{
				case 1:	// round
					Pen.setLineJoinType(Pen.LINEJOIN_ROUND);
					break;
					
				case 2:	// angle
					Pen.setLineJoinType(Pen.LINEJOIN_MITER);
					break;
					
				case 3:	// flat
					Pen.setLineJoinType(Pen.LINEJOIN_BEVEL);
					break;
						
				default:
					break;
				}	// end switch
				
				currentVarState = S_NONE;
			} // end if
		} // end else if
	}
	
	
	/** Finishes the object collection.
	*/
	protected void finishObject()
	{
		// We don't need ProcessorManager to do anything for this object.
		return;
	}
	

	/** Returns the Global LTSCALE for the DXF */
	public final double getLtScale()
	{	return LtScale;	}


	/** The X value drawing limits lower-left corner in WCS.
	*
	*	The difference between <I>Limits</I> and <I>Extends</I> is
	*	extends is the point just beyond the outer most object in a
	*	drawing and the limits are set by the user to include only
	*	the image data of interest; like a view port.
	*/
	public final double getLimitsMinX()
	{	return LimitsMinX;	}

	/** The Y value drawing limits lower-left corner in WCS.*/
	public final double getLimitsMinY()
	{	return LimitsMinY;	}

	/** The X value drawing limits upper-right corner in WCS.*/
	public final double getLimitsMaxX()
	{	return LimitsMaxX;	}

	/** The Y value drawing limits upper-right corner in WCS.*/
	public final double getLimitsMaxY()
	{	return LimitsMaxY;	}


} // end of HeaderProcessor class