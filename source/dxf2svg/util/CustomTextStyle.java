
/****************************************************************************
**
**	FileName:	CustomTextStyle.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates custom text style table objects from the config.d2s.
**
**	Date:		1.00 - January 11, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - January 11, 2005
**				1.01 - February 10, 2005 Updated documentation.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.*;

/**
*	Encapsulates a custom text style as read from the config.d2s 
*	Style Table objects. This object controls the styles of text and 
*	the size that they render at. This allows you to specify exactly
*	the font style and size of the text you wish.
*	<P>
*	Here is an example: if an illustration had a style defined as 6pte
*	with a font of uv.shx and a size of 0.0556 inches, you could redefine
*	all of those parameters with this class. You could write to the config.d2s
*	file something like &quot;6pte&quot; = &quot;font-family:'Switzerland';font-size:6.9;stroke:none;&quot;.
*	<P>
*	The end result is that any style defined in the DXF will be userped 
*	and replaced by the style defined in the config file. If a piece of 
*	text had a style of 6pte, as an example, it would render using the
*	style named by its class. If, however the text size differs from the 
*	named style, the text size is calculated using a ratio of its height
*	verses the height specified from the config file. If the config file
*	does not specify a size, it is calculated by the SvgText element during
*	its serialization to the SVG file.
*
*	@version	1.00 - January 11, 2005
*	@author		Andrew Nisbet
*/
public class CustomTextStyle extends TableStyles
{
	protected String customStyle;
	protected double parsedTextSize;
	
	/** The constructor acts like a copy constructor in C++. The point 
	*	here is to make a copy of the parent TableStyles data into 
	*	the CustomTextStyle object and then add the additional data. This
	*	allows the CustomTextStyle object to complete the calculations of 
	*	rotation etc. while still appearing like a TableStyles object 
	*	while down casting.
	*/
	public CustomTextStyle(TableStyles tstyle)
	{
		super( tstyle.getSvgUtil() );

		// TableStyles' data copied to this object.
		this.StyleName 			= tstyle.StyleName;
		this.FontFamilyName		= tstyle.FontFamilyName;
		this.FixedTextHeight	= tstyle.FixedTextHeight;
		this.dxfFixedTextHeight = tstyle.dxfFixedTextHeight;
		this.WidthFactor		= tstyle.WidthFactor;
		this.ObliqueAngle 		= tstyle.ObliqueAngle;
		this.TextGenFlag		= tstyle.TextGenFlag;
		this.FontName			= tstyle.FontName ;
		this.FontURI 			= tstyle.FontURI;
		this.FontFormat 		= tstyle.FontFormat;
		this.IsTrueTypeFont 	= tstyle.IsTrueTypeFont;
		this.GotLogicalFontName = tstyle.GotLogicalFontName;
		this.svgUtility 		= tstyle.svgUtility; // just copy the reference.
		this.DEBUG				= tstyle.DEBUG;		
		this.fme				= (FontMapElement)tstyle.fme.clone();
		
		parsedTextSize = 0.0;
	}
	
	/** Sets the size of the text height to the one specified in the CustomTextStyle String.
	*	If the String contains no size information the original value collected from the DXF
	*	is used. It is assumed that the font sizes provided are in SVG default text size of
	*	pixels. If a suffix follows the font-size value and it is the suffix 'in' the size 
	*	will be converted to pixels. All other suffixes will be ignored.
	*	@throws NumberFormatException if the value collected from the string couldn't be 
	*	converted to a valid double.
	*/
	protected void setCustomFixedTextHeight()
	{
		String result = parseCSSString("font-size:");
		if (! result.equals(""))
		{
			double tmpDouble;
			try
			{
				tmpDouble = Double.parseDouble(result);
			}
			catch (NumberFormatException e)
			{
				System.err.println("CustomTextStyle.setCustomFixedTextHeight(): "+
					"couldn't convert found font-size into a valid double.");
				return;
			}
			
			parsedTextSize = tmpDouble;
		}
		
		// otherwise just leave it the default 0.0.
	}
	
	/** Returns true if dxf object's text size matches the size of this style in inches. 
	*	SvgText objects can	query this to determine if their heights match the heights 
	*	of their stated style. Here we also query if the style from the config mentions
	*	a font-size attribute. If it doesn't the SvgText object must include it in its
	*	style.
	*/
	public boolean isStyleTextHeightMatch(double height)
	{
		// If we didn't successfully capture a font-size then signal the caller
		// that they need to include an attribute for font-size with a false match.
		if (parsedTextSize == 0.0)
		{
			return false;
		}
		
		if (dxfFixedTextHeight -height == 0.0)
		{
			return true;
		}
		
		return false;
	}
	
	/** Returns the parsed text height if one could be determined but if 
	*	it couldn't it returns the fixed text height.
	*/
	public double getTextHeight()
	{
		if (parsedTextSize != 0.0)
		{
			return parsedTextSize; // px size specified in config.d2s.
		}
		
		return FixedTextHeight; // converted px from inches in the dxf.
	}
	
	
	/** Searches the CSS String for the named value and returns it or an empty string if 
	*	the search string could not be found.
	*	<P>
	*	Limitations: currently, if the function will fail if the searchString does not 
	*	contain the key phrase &quot;&quot;, if it is mis-spelled, if the numeric value
	*	is not terminated with a semi-colon or there is a unit specifier used like &quot;in&quot;
	*	&quot;px&quot; or &quot;mm&quot;. The assumed size of font to date is assumed to 
	*	be the SVG default of pixels (px). This is done to reduce the overhead of including
	*	extensive calculations to convert from one unit base to another.
	*	The accurate size conversion of text from Dxf to 
	*	Svg has to be by trial-and-error. Here are some standard styles, sizes in inches and
	*	pixel sizes:
	*	<P>
	*	<TABLE BORDER=1 WIDTH="40%">
	*	<TR VALIGN="middle">
	*		<TH>Style</TH>
	*		<TH>Size (in)</TH>
	*		<TH>Size (px)</TH>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 6PTE </TD>
	*		<TD> 0.0556 </TD>
	*		<TD> 3.55 </TD>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 6PTBOLD </TD>
	*		<TD> 0.0556 </TD>
	*		<TD> 3.55 </TD>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 8PTE </TD>
	*		<TD> 0.0764 </TD>
	*		<TD> 5.15 </TD>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 8PTBOLD </TD>
	*		<TD> 0.0764 </TD>
	*		<TD> 5.15 </TD>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 10PTE </TD>
	*		<TD> 0.0972 </TD>
	*		<TD> 6.55 </TD>
	*	</TR>
	*	<TR VALIGN=middle">
	*		<TD> 12PTE </TD>
	*		<TD> 0.1166 </TD>
	*		<TD> 7.83 </TD>
	*	</TR>
	*	</TABLE>
	*/
	protected String parseCSSString(String searchString)
	{
		int pos = customStyle.indexOf(searchString);
		if (pos < 0)
		{
			return "";
		}
		
		int posEnd = customStyle.indexOf(";", pos);
		if (posEnd < 0)
		{
			return "";
		}
		
		return customStyle.substring((pos + searchString.length()), posEnd);
	}

	
	/** Allows the setting of a custom style.
	*/
	public void setCustomStyle(String style)
	{
		customStyle = style;
		setCustomFixedTextHeight();
	}
	
	/** Returns the custom style string. The custom style is a string that
	*	of what ever you want to see in the final CSS of the SVG.
	*/
	public String getCustomStyle()
	{
		return customStyle;
	}
	
	
	/** This will Output the current collected data for debug purposes.
	*/
	public String toDebugString()
	{
		StringBuffer StyleOutput = new StringBuffer();
		// It makes little sense to output the style as a string because
		// the text object itself will determine how to handle the information
		// as required so for here we will output our settings.
		StyleOutput.append("***********\n");
		StyleOutput.append("style name: "+getStyleName()+"\n");
		StyleOutput.append("font file name: "+customStyle+"\n");
		StyleOutput.append("***********\n");
		return StyleOutput.toString();
	}
	
	// getter methods that have to be over-written to return empty Strings.
	/** Used by StyleSheetGenerator to output font-face descriptions
	*	from the Dxf style tables as URLs, to the CSS. The client must have set 
	*	everything inside the curly braces through the config.d2s file.
	*	@return String with format<B>@font-face{&lt;config.d2s right-hand side&gt;}\n</B>.
	*	An example would be:
	*	<P>
	*	<CODE>@font-face{font-family:'symap';src:url('c:/winnt/fonts/symap___.ttf')}</CODE>
	*/
	public String toUrlString()
	{
		StringBuffer StyleOutput = new StringBuffer();

		if (DxfPreprocessor.includeUrl() == true)
		{
			//@font-face{font-family:'symap';src:url('c:/winnt/fonts/symap___.ttf')}
			StyleOutput.append("@font-face{");
			StyleOutput.append(customStyle);
			StyleOutput.append("}\n");
		}

		return StyleOutput.toString();
	}


	/** Used by StyleSheetGenerator to output all font descriptions
	*	from the style tables.
	*/
	public String toString()
	{
		StringBuffer StyleOutput = new StringBuffer();

		//.swissb{font-family:'symap';font-size:"6pt";}
		StyleOutput.append(".st"+getStyleName());
		StyleOutput.append("{");
		StyleOutput.append(customStyle);
		StyleOutput.append("}\n");

		return StyleOutput.toString();
	}	// TableStyles class
}