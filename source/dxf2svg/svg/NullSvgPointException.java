
/****************************************************************************
**
**	FileName:	NullSvgPointException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Throws a specific exception if you try to copy a Point
**				into a null Point reference.
**
**	Date:		November 19, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 19, 2002
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

/**	This unchecked exception signals that the client application attempted to copy
*	a <B>Point</B> object into a null reference.
*
*	@version	0.01 - November 19, 2002
*	@author		Andrew Nisbet
*/
public class NullSvgPointException extends RuntimeException
{
	private static final String msg = "attempted an operation on null Point.";

	/** Outputs exception message.*/
	public NullSvgPointException()
	{
		super(msg);
	}

	/** Outputs basic message and allows for the class implementer to supply
	*	more information if desired.
	*/
	public NullSvgPointException(String str)
	{
		super(msg+"\n"+str);
	}

	/** Outputs basic message and the Point where the exception was thrown
	*	to be output. <B>Do not pass the copy target</b>; that would just be
	*	dumb.
	*	@param p You could pass any point you wish but the copier Point
	*	(the Point that is doing the copying) is the intension.
	*/
	public NullSvgPointException(Point p)
	{
		super(msg+"\nSource Point: "+p.toString()+"\nTarget Point: null");
	}

	/** Outputs basic message and the Point where the exception was thrown
	*	to be output. <B>Do not pass the copy target</b>; that would just be
	*	dumb.
	*	@param p You could pass any point you wish but the copier Point
	*	(the Point that is doing the copying) is the intension.
	*	@param str Alternate message you wish to include along with the
	*	Point's toString() data.
	*/
	public NullSvgPointException(Point p, String str)
	{
		super(msg+"\n"+str+"\nSource Point: "+
			p.toString()+"\nTarget Point: null");
	}
}