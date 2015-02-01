
/****************************************************************************
**
**	FileName:	EventAttributeModifier.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This object extracts text content from an SvgText element and 
**				provides methods for retrieving that content with specific
**				formatting for the caller.
**
**	Date:		May 17, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 17, 2004
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				0.03 - April 11, 2005 Changed the function that pads a parts list
**				index number with preceeding '0's so that it will work with 
**				a part index number like 12B. It used to throw a NumberFormatException.
**				0.04 - April 12, 2005 Simplified the implied keyword to 
**				so it will pass the entire attribute regardless of content
**				and replace the implied keyword with the content of the text object
**				modified appropriately.
**				0.05 - August 2, 2005 Reinstated the stringAsIntPadChar() method 
**				to handle the special case of part index numbers that end with
**				a character like '024A'.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.util.Vector;
import java.util.regex.*;
import dxf2svg.DxfPreprocessor;
import dxf2svg.svg.SvgText;


/**	This object extracts text content from an SvgText element and modifies the argument
*	list of any javascript function call found as the value of an {@link Attribute}.
*	<P>
*	Often it is necessary to add an event attribute to an XML object. This occurs for 
*	all objects that are animated or react to user events. Each of these events reacts
*	by calling javascript code that performs some action based on the type of event that
*	has occured. If the user mouses over a text element and you want a tool tip to appear
*	when that happens, you use a mouseover event attribute like this: <CODE>onmouseover="someFunction()"</CODE>.
*	In this case the value of the attribute is a call to a javascript function. These
*	function calls can pass arguments to the function, and some of these arguments are not
*	known until the SVG file is converted. Consider the case for a Parts list illustration.
*	Each part number is a hyperlink that takes an additional attribute of <EM>onclick</EM>
*	and the value of that attribute is a function call called showPart(). showPart() 
*	takes an argument - the number of the part. In addition the part number is formatted
*	to be padded to 3 digits by adding preceeding zeros. So a part number like 23 will 
*	look like: '023' and a number like 7 looks like: '007' while a number like 138 is not 
*	modified. These numbers come from the SvgText element but are formatted and added to
*	the SvgHyperLink object.
*	<P>
*	To add flexibility to this kind of functionality, the author has included a new keyword:
*	'<EM>implied</EM>'. Implied means that the content of the SvgText object should be 
*	extracted, optionally formatted and included and a member of the argument list as 
*	a replacement for the word 'implied'. The user could use the '<CODE>-MY</CODE>' switch like this:
*	<CODE>... -MY onClick showPart(evt, implied, another_arg)...</CODE> and the resultant 
*	code snippet that looks like this:
*	<P>
<PRE>
&lt;a xlink:href="#stay" onClick="showPart(evt,014,another_arg)"&gt;
&lt;text class="ststandard" style="font-size:34.17;fill:#00FF00;" x="372.86" y="328.81"&gt;14&lt;/text&gt;
&lt;/a&gt;
</PRE>
*	The implied keyword is replaced verbatim - if you need a string argument you must quote
*	the keyword like this 'implied'. 
*
*	@version	0.05 - August 2, 2005
*	@author		Andrew Nisbet
*/
public class EventAttributeModifier
{
	private Attribute attrib;
	
	/** This value will pass the SvgText's content through without any formatting.*/
	public static final int UNFORMATTED = 0;
	/** This value will format the SvgText's content to the MY javascript function call
	*	specification which is an integer that is padded with initial zeros to the max
	*	size of three digits. Example: 1 = 001, 23 = 023.
	*/
	public static final int MY_FORMAT   = 1;


	/** After this object is created the {@link #getAttribute()} can be called to get the attribute.
	*	The returned attribute may or may not have been modified based on whether the keyword
	*	'implied' was used.
	*	@param svgText element to extract the content from.
	*	@param a the attribute you wish to modify. 
	*/	
	public EventAttributeModifier(SvgText svgText, Attribute a)
	{
		attrib = a;
		// This will parse out the arguments of the attribute value and determine if
		// the keyword 'implied' is present. If it is then search for content otherwise
		// there is basically no need to modify the attribute.
		modifyAttribute(svgText, UNFORMATTED);
	}


	
	/** After this object is created the {@link #getAttribute()} can be called to get the attribute.
	*	The returned attribute may or may not have been modified based on whether the keyword
	*	'implied' was used.
	*	@param svgText element to extract the content from.
	*	@param a the attribute you wish to modify. 
	*	@param type is the type of formatting you would like on the implied argument default
	*	is {@link #UNFORMATTED}. Currently other supported types are:
	*<UL>
	*	<LI> {@link #MY_FORMAT}
	*</UL>
	*/
	public EventAttributeModifier(SvgText svgText, Attribute a, int type)
	{
		attrib = a;
		// This will parse out the arguments of the attribute value and determine if
		// the keyword 'implied' is present. If it is then search for content otherwise
		// there is basically no need to modify the attribute.
		modifyAttribute(svgText, type);
	}
	

	
	/** Modifies the Attribute's value if necessary. It is deemed necessary if the keyword
	*	'implied' is found in the argument list of the Attribute. The formatting of the
	*	content is controlled by the 'type' parameter. The type is an integer whose values
	*	can be {@link #UNFORMATTED} or {@link #MY_FORMAT}. If the MY format is choosen the
	*	content of the SvgText element is padded with initial zeros to the max
	*	size of three digits. Example: 1 = 001, 23 = 023. 
	*/
	protected void modifyAttribute(SvgText svgText, int type)
	{
		
	
		String s = attrib.getAttributeValue();
		int impliedPos = s.indexOf("implied");
		
		if ( impliedPos >= 0 )
		{
			String funcName = s.substring( 0, impliedPos );
			String arguments = s.substring( (impliedPos + "implied".length()) );
			StringBuffer sb = new StringBuffer();
			String svgTextContent = null;
			switch (type)
			{
				case MY_FORMAT:
					svgTextContent = stringAsIntPadChar( svgText.getString() );
					break;
					
				default:
					svgTextContent = svgText.getString();
					break;
			}  // end switch
			
			sb.append( funcName + svgTextContent + arguments );
			
			// Now modify the attribute.
			attrib.setAttributeValue(sb.toString());
		}  // end if
	}  // end modifyAttribute
	
	
	
	
	/** This method performs the same function as ManipulateString.intToStringPadChar()
	*	but for string text, but it also handles part index numbers that end with
	*	a character like '024A'.
	*	@return A string pre-padded with '0' upto 3 digits wide. If the arg
	*	is not a 1 or 2 digit number with optional uppercase character, then the string
	*	is returned untouched. The rule is: if the string starts with a single digit 
	*	and ends with an optional char then prefix 2 '0's like this '00'. if the string
	*	starts with two digits and ends with an optional char then prefix 1 '0's like this '0'.
	*	and return anything else.
	*/
	protected String stringAsIntPadChar( String testStr )
	{
		// test if the arg testStr ends with a char
		if ( testStr.matches( "^\\p{Digit}{1}[A-Z]?$") )
		{
			return "00" + testStr;
		}
		else if ( testStr.matches( "^\\p{Digit}{2}[A-Z]?$") )
		{
			return "0" + testStr;
		}
		else
		{
			return testStr;
		}
	}
	
	
	/** Returns the attribute.
	*/
	public Attribute getAttribute()
	{
		return attrib;
	}
}