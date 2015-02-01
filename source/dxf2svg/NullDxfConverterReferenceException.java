
/****************************************************************************
**
**	FileName:	NullDxfConverterReferenceException.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This unchecked Exception signals the attempt to instantiate
**				an svg object with a null DxfConverter reference.
**
**	Date:		November 19, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 19, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg;

/**	This unchecked exception signals that the client application attempted to
*	pass a null <B>DxfConverter</B> object reference. This occurs if the client
*	attempts to instantiate any SvgElement with a null <B>DxfConverter</B>
*	object.
*
*	@version	0.01 - November 19, 2002
*	@author		Andrew Nisbet
*/
public class NullDxfConverterReferenceException extends RuntimeException
{
	private static final String msg = "attempted to instantiate an object"+
		" with a null DxfConverter reference.";

	/** Outputs exception message.*/
	public NullDxfConverterReferenceException()
	{
		super(msg);
	}

	/** Outputs basic message and allows for the class implementer to supply
	*	more information if desired.
	*/
	public NullDxfConverterReferenceException(String str)
	{
		super(msg+"\n"+str);
	}

}