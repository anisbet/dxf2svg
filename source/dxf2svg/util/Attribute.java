
/****************************************************************************
**
**	FileName:	Attribute.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates an arbitrary attribute that can be applied to any
**				SvgElement.
**
**	Date:		March 3, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.1)
**
**	Version:	0.1 - March 3, 2004
**				1.5 - January 20, 2005 Remodled the class to allow attribute
**				values to be Attributes themselves and the ability to detect
**				output requirements in the toString() method. This will allow
**				embedded Attributes to be output as 'style="font-family:switzerland;font-size:0.945;"'
**				as an example.
**				1.6 - September 6, 2005 The equals() method no longer checks for
**				the attribute name AND value, but instead justs tests the name.
**				Added matchesValue() to replace old equals test for value equality
**				functionality if required.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

/**	The class allows you to apply an arbitrary attribute to an SvgElement.This
*	is to account new attributes that may be allowed in the future and for the
*	applying of ElementEvents to SvgElements.
*	<P>
*	If the attribute value is null or empty the attribute {@link #toString()} will
*	return an empty String.
*	<P>
*	An attributte makes available extra information regarding the element on which it appears.
*	Attributes always appear as a name-value pair. Example of an attribute is id="identification",
*	which gives the attribute id the value identification.
*	<P>
*	Note: The equals() method no longer checks for equality of
*	the Attribute name AND value, but instead justs tests the names of two Attributes..
*
*	@version	1.6 - September 6, 2005
*	@author		Andrew Nisbet
*/
public class Attribute implements Cloneable
{
	private String attributeName;
	private String attributeValue;

	protected Attribute(){}

	////////////////////// Constructors //////////////////////
	/**	Creates an attribute with the argument attribute name and value.
	*/
	public Attribute(String attrib, String attribVal)
	{
		if ((attrib == null || attrib.equals("")) ||
			(attribVal == null || attribVal.equals("")))
		{
			throw new EmptyAttributeException();
		}
		attributeName  = attrib;
		attributeValue = attribVal;
	}


	////////////////////// Methods //////////////////////
	/** Sets an attribute's value deleting all previously set values.
	*	@throws EmptyAttributeException if the attribute name or value are null or empty values.
	*/
	public void setAttributeValue(String attribVal)
	{
		if (attribVal == null || attribVal.equals(""))
		{
			throw new EmptyAttributeException();
		}
		attributeValue = attribVal;
	}

	/** Allows an additional value to be appended to a attribute value.
	*	Does not throw EmptyAttributeException but rather the empty
	*	or null value is ignored and the existing value is left untouched.
	*/
	public void addAttributeValue(String attribVal)
	{
		if (attribVal == null || attribVal.equals(""))
		{
			return;
		}
		StringBuffer attrib = new StringBuffer();
		attrib.append(attributeValue);
		attrib.append(attribVal);
		attributeValue = attrib.toString();
	}

	/** Returns the name of the attribute.
	*/
	public String getAttribute()
	{
		return attributeName;
	}

	/** Returns this attribute's value(s).
	*/
	public String getAttributeValue()
	{
		return attributeValue;
	}

	/** This is a convience method for subclasses to set attributes from constructors because
	*	the attribute and value are private.
	*/
	protected void setAttribute(String attrib)
	{
		if (attrib == null || attrib.equals(""))
		{
			throw new EmptyAttributeException();
		}
		attributeName = attrib;
	}

	/** Tests attribute to determine if it is empty.
	*	Note - January 20, 2005 EmptyAttributeException is thrown if
	*	the client tries to create an empty attribute.
	*/
	public boolean isEmptyAttributeValue()
	{
		if (attributeValue == null || attributeValue.equals(""))
		{
			return true;
		}

		return false;
	}


	/**	If the attribute value is null or empty the attribute toString() will
	*	return an empty String.
	*/
	public String toString()
	{
		return " " + attributeName + "=\"" + attributeValue + "\"";
	}
	
	
	/** This method allows the caller to test if the value of an Attribute matches
	*	this Attribute's value.
	*/
	public boolean matchesValue(Attribute a)
	{
		if (a.getAttributeValue().equals(this.getAttributeValue()))
		{
			return true;
		}
		return false;
	}


	/** Returns true if the Attribute is equal to the argument object and false 
	*	otherwise. Equals is defined as the attribute's name matching itself, not the value.
	*/
	public boolean equals(Object obj)
	{
		if (! (obj instanceof Attribute))
		{
			return false;
		}
		
		if (((Attribute)obj).getAttribute().equals(this.getAttribute()))
		{
			return true;
		}
		
		return false;
	}
	
	/** Creates a clone of the Attribute.
	*/
	public Object clone()
	{
		Attribute a = null;

		try
		{
			a = (Attribute)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			System.err.println(e + "failed to clone Attribute.");
		}

		a.attributeName   = this.attributeName;
		a.attributeValue  = this.attributeValue;

		return a;
	}

	///////////////////////// Exceptions classes
	/** This RuntimeException gets thrown if an Attribute is created or modified so that
	*	its name or value is null or empty.
	*	@since 1.5 - January 20, 2005
	*/
	protected class EmptyAttributeException extends RuntimeException
	{
		protected EmptyAttributeException()
		{
			System.err.println("Attribute: Attribute name or value is null or empty.");
		}
	}
}