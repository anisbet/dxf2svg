
/****************************************************************************
**
**	FileName:	CustomLayerStyle.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsultes a C-130 spar standard layer style.
**
**	Date:		March 20, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - March 20, 2003
**				0.02 - January 6, 2005 Fixed documentation.
**				0.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.DxfPreprocessor;

/**	This class encapsulates the concept of a AutoCAD layer object.
*	It wraps a custom style for a layer. It's used 
*	to query spar standard styles that are the same for all 
*	Drawings (i.e. all layers called 7 have a stroke of black 
*	and a line-weight of 0.007in.)
*	<P>Features: one can also specify a fill for a layer.
*
*	@version	0.1 March 20, 2003
*	@author		Andrew Nisbet
*/
public final class CustomLayerStyle implements Cloneable
{
	public final static int DEFAULT_FILL = 241;
	private String name;	// Name of the layer.
	private Pen pen;		// pen assignment for this layer.
	private int penNum = 1;	// Number of pen for future lookup of real Pen.
	private int fill;		// Fill for this layer.
	
	/**	Takes two arguments; the name of the layer and the pen assignment.
	*/
	public CustomLayerStyle(String name, Pen pen)
	{
		this.name 	= name;
		this.pen 	= pen;
		this.fill	= DEFAULT_FILL;
	}
	
	/**	Takes two arguments; the name of the layer and the pen assignment as a String.
	*/
	public CustomLayerStyle(String name, int penNum)
	{
		this.name 	= name;
		this.penNum	= penNum;	// We will look this up later
		this.fill	= DEFAULT_FILL;
	}
	
	/**	Takes the name of the layer and the pen assignment as a String and an fill value.
	*	This is a string and as such valid values are those accepted by SVG
	*	like '#EFEFFF' or 'black'.
	*/
	public CustomLayerStyle(String name, int penNum, int fill)
	{
		this.name 	= name;
		this.penNum	= penNum;	// We will look this up later
		this.fill	= fill;
	}
	
	/**	Takes the name of the layer and the pen assignment and an fill value.
	*	This is a string and as such valid values are those accepted by SVG
	*	like '#EFEFFF' or 'black'.
	*/
	public CustomLayerStyle(String name, Pen pen, int fill)
	{
		this.name 	= name;
		this.pen 	= pen;
		this.fill	= fill;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////
	//								Methods
	//////////////////////////////////////////////////////////////////////////////////
	/**	Returns name of layer.
	*/
	public final String getName()
	{	return name;	}
	
	/**	Returns the colour of this layer as a Hex string which in the case
	*	of style sheets typically will be prefixed with a '#' mark.
	*/
	public final String getColour()
	{	
		if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);
		return pen.getColour();
	}
	
	/**	Returns the colour of this layer as an integer.
	*/
	public final int getColour(int i)
	{	
		if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);
		return pen.getColour(1);
	}
	
	/**	Returns the line weight of this layer.
	*/
	public final String getLineWeight()
	{
		if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);
		return pen.getLineWeight();
	}
	
	/**	Returns the line weight in it's original form of a double.
	*	The argument can be any arbitrary double value.
	*/
	public final double getLineWeight(double d)
	{	
		if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);		
		return pen.getLineWeight(1.0);
	}
	
	/** Returns the Pen assignment of this layer.
	*/
	public final Pen getPen()
	{	
		if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);
		return pen;
	}
	
	/** Returns the fill value of this layer if any.
	*/
	public final String getFill()
	{	return DxfPreprocessor.getColour(fill);	}
	
	/** Returns the fill value of this layer if any.
	*	@param i an integer of any valid integer.
	*/
	public final int getFill(int i)
	{	return fill;	}
	
	/** Returns the formatted style string for this layer.
	*	<B>Example</B>:	<CODE>".st7{stroke:#000000;stroke-width:0.007in;}"</CODE>.
	*/
	public String toString()
	{
		// Looks like:
		// ".st7{stroke:#000000;stroke-width:0.007in;}"
		StringBuffer sb = new StringBuffer();
		//if (pen == null)
			pen = DxfPreprocessor.getPen(penNum);
		sb.append(".st"+name+"{"+pen.toString());
		// Check to see if there is a fill colour set.
		if (fill > 0 && fill <= 255)
			sb.append("fill:"+DxfPreprocessor.getColour(fill)+";");
		
		sb.append("}");
		return sb.toString();
	}
	
	// This is implemented to allow for deep copy in stead of reference
	// to be put onto the Hashtable.
	protected Object clone()
	{
		try{
			CustomLayerStyle style = (CustomLayerStyle)super.clone();
			
			style.name 	= this.name;
			style.pen	= (Pen)this.pen.clone();
			style.fill	= this.fill;
			style.penNum= this.penNum;
			
			return style;
		} catch (CloneNotSupportedException e){
			throw new InternalError();
		}
	}
}