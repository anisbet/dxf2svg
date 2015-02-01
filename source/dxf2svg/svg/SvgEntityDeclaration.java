
/****************************************************************************
**
**	FileName:	SvgEntityDeclaration.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates all objects from a discreet DXF layer into a
**				<g>&entityRef;</g> tag.
**
**	Date:		April 04, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - April 04, 2003
**				0.02 - March 25, 2004 Added special handling for note and 
**				legend blocks.
**				0.03 - April 16, 2004 Added getAdditionalAttributes().
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;

/**
*	This class encapsulates a collection of SvgObjects that, in the case
*	of converting Dxf files, represents a block. These will become entity
*	declarations in the DTD section of the SVG.
*	<P>
*	An interesting feature of this class is that you can switch the 
*	quotes used in the declaration between single and double quotes. Why
*	would you want or need that? Well if ENTITY descriptor uses single 
*	quotes the enclosing elements must contain double quotes or a not 
*	well formed error will occur. Also, my editor has colour syntax high-
*	lighting which will still work. If the ENTITY declaration uses double
*	quotes, the contained entities require and single quotes to work and
*	syntax high-lighting is not available. Most references describe enclosed
*	entities as having double-quotes while the ENTITY declaration itself with
*	single quotes. This is the default behaviour.
*	<P>
*	Since SvgEntityDeclaration is the SVG equivalent of a DXF block, 
*	SvgEntityDeclaration can also contain attribs just like their AutoCAD
*	counterparts.
*
*	@version	0.03 - April 16, 2004
*	@author		Andrew Nisbet
*/
public class SvgEntityDeclaration extends SvgCollection
{
	/** Refers to whether the ENTITY declaration uses a single or double quote.
	*	The SvgEntityDeclaration will use the opposite.
	*/
	public final static int SINGLE_QUOTE = 0;
	public final static int DOUBLE_QUOTE = 1;
	private int quoteType = SINGLE_QUOTE;		// ENTITY will take a single quote default
	private boolean isSpecialBlock;				// Special like notes, legends or other interesting
												// things.
	public final static String NOTE = "note";	// Prefix for notes
	public final static String LEGEND = "legend";// Prefix for legends.
		
		
	//////////// Constructor ////////////////
	// default
	/**
	*	Sets a reference to this Thread's current DxfConverter Object. Strings within
	*	the collection will be double quoted by default.
	*/
	public SvgEntityDeclaration(DxfConverter dxfc)
	{
		super(dxfc);
		init(dxfc, DOUBLE_QUOTE);
	}

	/**
	*	Sets a reference to this Thread's current DxfConverter Object and switches
	*	between single and double quotes (default is double quoted strings throughout).
	*/
	public SvgEntityDeclaration(DxfConverter dxfc, int quoteType)
	{
		super(dxfc);
		init(dxfc, quoteType);
	}
	
	
	/** Unifies the initialization process 
	*/
	protected void init(DxfConverter dxfc, int quoteType)
	{
		////////////// April 8, 2004 //////////////
		setType("g");
		if (quoteType <= SINGLE_QUOTE)
			this.quoteType = SINGLE_QUOTE;
		else
			this.quoteType = DOUBLE_QUOTE;
			
		determineSpecialBlock();
	}
	
	protected void determineSpecialBlock()
	{
		
	}
	
	
	/**
	*	Returns the collection's attributes as a StringBuffer.
	*	@return StringBuffer of formatted attributes.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer OutputString = new StringBuffer();

		// if the ObjType is null or unknown and we still want to see it in
		// the SVG for debugging purposes
		if (quoteType == DOUBLE_QUOTE)
			OutputString.append(getObjID().replace('\'','\"'));
		else
			OutputString.append(getObjID());

		if (getIncludeClassAttribute() == true)
		{
			if (quoteType == DOUBLE_QUOTE)
			{
				OutputString.append(getClassAttribute().replace('\'','\"'));
			}
			else
			{
				OutputString.append(getClassAttribute());
			}
		}
		
		OutputString.append(getAdditionalAttributes());

		return OutputString;
	}
	
	/**
	*	Outputs a formatted String, collection of SvgObjects.
	*/
	public String toString()
	{
		int GroupSize = SvgElementVector.size();
		// First we check to see if we even have any data to display
		// We do this because some layers contain no data and if you
		// continue with the iteration on an empty Vector you will
		// throw a NullPointerException. It is completely legit to
		// be required to handle empty layers.
		if (GroupSize < 1)
			return "";

		// If we made it here we can continue safely
		StringBuffer OutputString = new StringBuffer();
		OutputString.append("<");
		// now the type or tag of this object.
		OutputString.append(getType());
		// This method appends a StringBuffer to a StringBuffer
		// which was only introduced in Java 1.4 but it saves
		// us a toString() conversion call.
		OutputString.append(getAttributes());
		OutputString.append(">\n");

		for (int i = 0; i < GroupSize; i++)
		{
			String outStr = (SvgElementVector.get(i)).toString();
			// We can alternatively use single quotes or double quotes for SvgEntities.
			// They must alternate though; if the declaration uses single quotes the
			// enclosed entities must have double quotes, and if the declaration is in
			// double quotes the enclosed entities must have double quotes where quotes
			// are used.
			////////////////////// remember /////////////////////
			// If the Entity declaration is single quoted we use double here
			// otherwise if the ENTITY key word uses a double quote we have 
			// to use single quotes internally for all of our entities.
			if (quoteType == DOUBLE_QUOTE)
			{
				outStr = outStr.replace('\'', '\"');
			}
				
			OutputString.append("\t"+outStr+"\n");
		}

		// closing tag.
		OutputString.append("</"+getType()+">");

		return OutputString.toString();
	}

}	// end of SvgEntityDeclaration class