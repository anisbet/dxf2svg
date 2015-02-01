
/****************************************************************************
**
**	FileName:	DxfElementPair.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Container for pairs of group codes and values extracted
**				from DXF.
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.10 August 16, 2002 Removed methods for reporting total
**				number of these objects were made.
**				1.11 November 6, 2002 Made the class final. Removed SN serial
**				number.
**				1.12 April 14, 2005 Removed ID altogether.
**
**	TODO:		See ToDo List.xls.
**
*****************************************************************************/

package dxf2svg.util;

/**
*	DxfElementPair is a wrapper class for the primitive data types
*	extracted from a Dxf file.
*
*	A DXF file is a list of &quot;group codes&quot;, each followed
*	by a new line and its corresponding value. This class encapsulates
*	this pair of datum into one object.<BR><BR>
*
*	@version	1.12 April 14, 2005
*	@author		Andrew Nisbet
*/
public final class DxfElementPair
{
	private int code;				// integer code pair
	private String sValue;			// String value from value pair

	/** Takes a pair of Strings representing (in order)
	<OL>
		<LI> The group code.
		<LI> The value.
	</OL>
	*/
	public DxfElementPair(String cStr, String vStr)
	{
		cStr = cStr.trim();
		this.code = Integer.parseInt(cStr);
		this.sValue = vStr;					// we'll use this for now until we figure out cStr type
		// Set the id number to the current count of DxfElementPair objects.
	}

	/** The default constructor.
	*
	*	Sets the value of the group code to 0 and the value to NULL.
	*/
	public DxfElementPair()
	{
		this.code = 0;
		this.sValue = null;
	}

	/** Takes a group code as an integer and value as String.*/
	public DxfElementPair(int cInt,String vStr)
	{
		this.code = cInt;
		this.sValue = vStr;
	}
	/* end of constructors */

	/** Sets the code of the element pair.
	*	@throws NumberFormatException if the code is not an integer value.
	*/
	public void setCode(String code)
	{
		this.code = Integer.parseInt( code );
	}
	
	
	
	/** Sets the value of the entity pair.*/
	public void setValue(String value)
	{
		this.sValue = value;
	}

	// Most useful for returning the value of the group code/value pair
	/** Returns <I>this</I> DxfElementPair's <B>value</B> as a String. */
	public String getValue()
	{
		return sValue;
	}
	/*
		Returning this value will tell you what type of data is stored in
		following value of the group code/value pair.
		Note: all values in any DxfElementPair object are stored as strings.
		and all codes are integers for simplicity.  I will update this
		object to convert and return correct values as per DXF SvgUtil.
	*/
	/** Returns <I>this</I> DxfElementPair's <B>code</B> as an integer. */
	public int getCode()
	{
		return code;
	}

	/** Returns <I>this</I> DxfElementPair. */
	public DxfElementPair getElementPair()
	{
		return this;
	}

	/** Used to print out this object's data for reporting. */
	public String toString()
	{
		StringBuffer rStr = new StringBuffer();
		rStr.append(getClass().getName());
		rStr.append(": ");
		rStr.append(String.valueOf(code) +", "+ sValue);
		return rStr.toString();				// convert back from string buffer to string
	}


} // end DxfElementPair