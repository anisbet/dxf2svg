
/****************************************************************************
**
**	FileName:	TableStyles.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates DXF text style table objects.
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.04 - January 12, 2005 Set the members to protected to allow
**				subclass CustomTextStyle to access them.
**				1.05 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;
import java.net.URI;
import java.awt.*;
import java.util.*;						// For font maps.
import dxf2svg.*;						// DxfConverter
import dxf2svg.util.FontMapElement;	// font map element

/**
*	Encapsulates DXF Text Style Table objects. This object controls the styles of
*	text.
*
*	@version	1.00 - August 5, 2002
*	@author		Andrew Nisbet
*/
public class TableStyles
{
	// Instance data
	protected String	StyleName;			// Style name 'UVB' 'UV'
	protected String	FontFamilyName;		// For generic font name.
	protected double	FixedTextHeight;	// 0 if text is not of a fixed height
		// initialized for testing to see if set.
	protected double	dxfFixedTextHeight; // Sets the original size of the text (in inches).
		// this is used as a reference later for detecting scaling of text.
	protected double	WidthFactor;		// 'nuff said
	protected double	ObliqueAngle;		// Italicized is the immediate thought, though
		// this has also been used to put text into isometric.
	protected int		TextGenFlag;		// see notes below.
	protected String	FontName;			// Logical name like Helvetica Bold Oblique
	protected String	FontURI;			// Font URI location.
	protected String	FontFormat;			// TrueType what have you.
	protected boolean IsTrueTypeFont;		// This is a true type font?
	protected boolean GotLogicalFontName;	// were we able to get the
		// true logical name for this font?
	protected SvgUtil svgUtility;
	protected boolean DEBUG;				// DxfPreprocessor.
	protected FontMapElement fme;			// Font map element if there is one.
	

	public TableStyles( ProcessorManager pm )
	{
		svgUtility 			= pm.getSvgUtilInstance();
		init();
	}

	public TableStyles( DxfConverter dxfc )
	{
		svgUtility 			= dxfc.getSvgUtil();
		init();
	}
	
	public TableStyles( SvgUtil svgUtil )
	{
		svgUtility 			= svgUtil;
		init();
	}
	
	protected void init()
	{
		DEBUG 				= DxfPreprocessor.debugMode();
		StyleName 			= "standard";
		FontName			= "courier";	// default font style; easily visible; on most systems.
		FontFamilyName 		= new String();
		FontFormat 			= "";
		WidthFactor 		= 1.0;
		FixedTextHeight 	= 0.0;
		dxfFixedTextHeight	= 0.0;
		ObliqueAngle 		= 0.0;
		IsTrueTypeFont 		= false;
		GotLogicalFontName 	= false;
		// This will set up a default FontMapElement with the name of 
		// the default font file name for, courier, and a scale factor of one. 
		// The font file name is not critical and will never be referenced,
		// but we do need a default scale factor for SvgText and outputting 
		// this object.
		fme					= new FontMapElement("COUR.TTF", 1.0);
	}






	////////////////////////////////////////////////
	//				Methods
	////////////////////////////////////////////////
	// This method is strictly used by CustomTextStyle to create a custom text style
	// using TableStyles object.
	public SvgUtil getSvgUtil()
	{
		return svgUtility;
	}



	
	/** Returns true if dxf object's text size matches the size of this style in inches. 
	*	SvgText objects can	query this to determine if their heights match the heights 
	*	of their stated style.
	*/
	public boolean isStyleTextHeightMatch(double height)
	{
		if (dxfFixedTextHeight -height == 0.0)
		{
			return true;
		}
		
		return false;
	}
	
	
	/** Sets the name of the style of text to be used like 'Standard'.
	*/
	public void setStyleName(String name)
	{	StyleName = DxfPreprocessor.convertToSvgCss(name);	}



	/** Returns the name of the text style */
	public String getStyleName()
	{	return StyleName;	}



	/** This method controls the rendering of the text.
	*	@param TGF A value of 2 will render the text in reverse,
	*	4 means the text will be upside down.
	*/
	public void setTextGenFlag(int TGF)
	{
		// if this value is 2 then the text is reversed.
		// if this value is 4 then the text is upside down.
		// if this value is 6 then the text is upside down and backwards.
		TextGenFlag = TGF;
	}

	/** Returns the current TextGenFlag setting.
	*	@see #setTextGenFlag
	*/
	public int getTextGenFlag()
	{	return TextGenFlag;	}

	/** Sets the size of the text height.*/
	public void setFixedTextHeight(double fth)
	{
		dxfFixedTextHeight = fth;
		FixedTextHeight = fth * svgUtility.Units() * svgUtility.pointsPerPixel();
	}
	
	/**	Returns the required font compensation value. This is scale factor
	*	required to make an SVG font appear at the absolute size of the 
	*	AutoCAD font sizes.
	*/
	public double getCompensationScaleFactor()
	{	return fme.getCompensationScaleFactor();	}


	/** Sets the kerning between letters(?) */
	public void setWidthFactor(double WF)
	{	WidthFactor = WF;	}

	/** Returns the current width factor.*/
	public double getWidthFactor()
	{	return WidthFactor;	}


	/** Returns the text height of the current font. This method
	*	is called by external clients.
	*	@see dxf2svg.svg.SvgFontMetrics#getAscent
	*/
	public double getTextHeight()
	{
		// If we don't do this we end up sending 0.0 to the
		// SvgFontMetrics object and it, in turn throws an exception
		// To get around that if the we just return 0.0 thereby not
		// altering the fixed text height if it has not been set, and
		// if it has been explicitly set to 0.0.
		//if (FixedTextHeight <= 0.0)
		return FixedTextHeight;
	}

	/** Sets the oblique angle. Sometimes used to simulate italics.
	* Some illustrators use it to create text in isometric.
	*/
	public void setObliqueAngle(double oa)
	{	ObliqueAngle = -oa;	} // Svg's rotation is opposite to AutoCAD's

	/** Returns the Oblique angle setting for the text.*/
	public double getObliqueAngle()
	{	return ObliqueAngle;	}

	/**
	*	This method sets the font family name from the group code 1000.
	*	This method will also check to see if {@linkplain #setPrimaryFontFileName}
	*	succeeded and if it did then keep the font name it found and if
	*	it failed then substitute the font family name.
	*/
	public void setFontFamilyName(String name)
	{
		FontFamilyName = name;
		// did we manage to open and read the .ttf font file.
		if (GotLogicalFontName == false)
			FontName = FontFamilyName;
	}

	/** Sets the primary font file name (group code 3 of style table).
	*	This method also attempts to set the logical font file name based
	*	on the true font name as listed within the actual font file itself.
	*	It does that by attempting to find the argument file and reading
	*	that file with {@linkplain java.awt.Font#createFont} method. and then
	*	calling the {@linkplain java.awt.Font#getFontName} method that returns
	*	the logical font file name. <B>For example:</B><BR><BR>
	*	If the argument font file is <code>'switzb.ttf'</code> this method will attempt
	*	to find the file using either the default font directory or the
	*	directory passed as a URL from {@linkplain dxf2svg.DxfPreprocessor}. If the font
	*	can't be found on the system (this could happen if this is not
	*	the computer that the original drawings were created on), then
	*	the font family name is substituted from group code 1000. This
	*	could be an unsatisfactory situation because <code>'switzb.ttf'</code>
	*	is actually <b>Switzerland Bold</b> but group code 1000 reports
	*	that this font is just <b>Switzerland</b>. The bold is a switch in the
	*	Style dialog box in AutoCAD and will not get set in the DXF output
	*	and therefore does not migrate to the Svg file.
	*	<BR><BR>
	*	Also note that a font directory can be entered on the command line
	*	with correct system slash orientation. This method is also responsible
	*	for converting the any '\' into a '/' as per true URL standard.
	*
	*	@param FFN Font File Name.
	*/
	public void setPrimaryFontFileName(String FFN)
	{
		// This method sets the font file name. It does that by reading
		// the group code '3' from the Styletable in the TABLES section
		// of the dxf. The value of this group code is always just the
		// font file name like 'switzn.ttf'. In addition the table also
		// makes use of a group code of '1000' to identify the font family.
		// No where else is there any mention of the logical font name like
		// 'Switzerland Bold'. To get that we do some reverse engineering.
		// If we know the file name of the font we can create a java.awt.Font
		// with that information and query the getFontName() method or
		// getPSName() method, but there is a catch: we have to supply
		// an InputStream, and to get that we need a path. Now there are
		// several ways to get that information:
		//
		// 1) We can rely on the default font directory on this machine,
		// which may not match even other Windows machines.
		// 2) You could use the the AutoCAD font directory, if the user
		// selected the default install directory for installation.
		// 3) If the user has selected a URL for the fonts then your worrys
		// are over because you can just parse the path and file name
		// together and expect the file to exist in that directory.
		//
		// Set the default font location directory.
		String FileLocation;
		String os = System.getProperty("os.name");
		if (os.equalsIgnoreCase("Windows 98"))
			FileLocation = "C:/WINDOWS/FONTS";
		else if (os.equalsIgnoreCase("Windows NT"))
			FileLocation = "c:/winnt/fonts";
		else if (os.equalsIgnoreCase("Windows 2000"))
			FileLocation = "c:/winnt/fonts";
		else
			FileLocation = "c:/winnt/fonts";
		// There is no c:\winnt\fonts directory (NT and WIN2000) and
		// the user has not specified a url so issue a warning.
		// This is not a show stopper if the files are to be put on
		// another machine or server somewhere.
		if ( ! (new File(FileLocation).isDirectory()) &&
			DxfPreprocessor.includeUrl() == false)
			System.err.println("TableStyles warning: font path unknown."+
				"\nUnable to define logical font name, substituting with font family.");

		// Standard style defaults to a font file name of 'txt' which is
		// of course not legal. I did find a font on my system by the name
		// of Txt so I will convert that to the corresponding font file name.

		// Substitute switzerland for uv or uvb. This is common on old Spar
		// graphics and by using a fontmap.d2s file in the conversion directory
		// you can specify what fonts are to be substituted.
		// If a font is not found in the font map, it falls through to the
		// rest of this method anyway.
		FontMapElement tmpFME = DxfPreprocessor.lookupFileNameInFontMap(FFN);
		if (tmpFME == null)
		{
			GotLogicalFontName = false;
			fme = new FontMapElement(FFN);
		}
		else
		{
			GotLogicalFontName = true;
			fme = tmpFME;
		}
			

		// Now lets test the actual file to see if we can gleen a font name.
		String f = fme.getFont().toUpperCase();

		if (f.endsWith(".TTF") || f.endsWith(".FON")) // Although fonts
		// that end with '.fon' technically are system fonts and therefore
		// low quality bitmap fonts, the Svg viewer complains if the font
		// does not take a FontFormat string of truetype. This may be a bug
		// but it works. We could also include .otf or OpenType fonts but
		// I don't have any to test.
		{
			IsTrueTypeFont = true;
			FontFormat = " format(\"truetype\");";
		}


		// Now we have to format the string so that is correctly displays
		// forward slashes for the URL string.
		if (DxfPreprocessor.includeUrl() == true)
		{
			FontURI = DxfPreprocessor.getFontUrl();
			FontURI = FontURI.replace('\\','/');
		}

		// Now see if you can get the font's logical name.
		// Check to see if supplied URI is a directory.
		////////////// test for exception /////////////////
		if (DxfPreprocessor.includeUrl() == true)
		{
			FileLocation = DxfPreprocessor.getFontUrl();
		}

		// Java 1.4.1 only supports the createFont() with
		// an argument of TRUETYPE_FONT.
		if (IsTrueTypeFont)
		{
			if(DEBUG)
			{
				System.out.println("FileLocation: "+FontName);
				System.out.println("FileLocation: "+FileLocation);
				System.out.println("FontFileName: "+fme.getFont());
				System.out.println("FontScaleHint: "+fme.getCompensationScaleFactor());
				System.out.println("-------------");
			}
			GotLogicalFontName = setFontNameFromFile(
				FileLocation, fme.getFont());
		}

	}

	// This takes the file directory and name and finds out what the
	// logical font name is. Logical font name is like 'Switzerland Bold'
	// AutoCAD only reports Font family name (group code 1000), so to
	// focus on the actual name requires opening the .ttf file if possible.
	private boolean setFontNameFromFile(String dir, String file)
	{
		Font font;
		try{
			BufferedInputStream BIS = new BufferedInputStream(
				new FileInputStream(new File(dir,file)));
			font = Font.createFont(Font.TRUETYPE_FONT, BIS);
			FontName = font.getFontName();
			BIS.close();
		}
		catch (FileNotFoundException e)
		{
			if (DEBUG)
				System.err.println("Can't find the specified font file.\n"+
					e);
			return false;
		}
		catch (FontFormatException ffe)
		{
			if (DEBUG)
				System.err.println(ffe);
			return false;
		}
		//
		catch (IOException ioe)
		{
			if (DEBUG)
				System.err.println("Error while reading font file.\n"+ioe);
			return false;
		}

		return true;
	}

	/** Returns the font's URI. */
	public String getPrimaryFontURI()
	{	return FontURI;	}

	/** Returns the font file name. */
	public String getPrimaryFontFileName()
	{	return fme.getFont();	}

	/** Returns the name of the font face, not font family.
	*/
	public String getFontName()
	{	return FontName;	}

	/** This will Output the current collected data for debug purposes.
	*/
	public String toDebugString()
	{
		StringBuffer StyleOutput = new StringBuffer();
		// It makes little sense to output the style as a string because
		// the text object itself will determine how to handle the information
		// as required so for here we will output our settings.
		StyleOutput.append("***********\n");
		StyleOutput.append("style name: "+StyleName+"\n");
		StyleOutput.append("font file name: "+fme.getFont()+"\n");
		StyleOutput.append("font name: "+FontName+"\n");
		// this next line may throw an exception if the SvgFontMetrics object
		// can not be instantiated.
		StyleOutput.append("text height: "+getTextHeight()+"\n");
		StyleOutput.append("width factor: "+WidthFactor+"\n");
		StyleOutput.append("oblique angle: "+ObliqueAngle+"\n");
		StyleOutput.append("font format: "+FontFormat+"\n");
		StyleOutput.append("orientation (textgen) flag: "+TextGenFlag+"\n");
		StyleOutput.append("***********\n");
		return StyleOutput.toString();
	}

	/** Used by StyleSheetGenerator to output font-face descriptions
	*	from the Dxf style tables, to the CSS.
	*/
	public String toUrlString()
	{
		StringBuffer StyleOutput = new StringBuffer();

		if (DxfPreprocessor.includeUrl() == true)
		{
			//@font-face{font-family:'symap';src:url('c:/winnt/fonts/symap___.ttf')}
			StyleOutput.append("@font-face{font-family:\"");
			StyleOutput.append(FontName);
			StyleOutput.append("\";src:url(\"");
			StyleOutput.append(getPrimaryFontURI());
			StyleOutput.append("\")"+FontFormat+"}\n");
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

			StyleOutput.append("font-family:\""+FontName+"\";");
			StyleOutput.append("stroke:none;");
			if (FixedTextHeight != 0)
			{
				StyleOutput.append("font-size:"+
					svgUtility.trimDouble(getTextHeight() * getCompensationScaleFactor())+
					";");
			}

		StyleOutput.append("}\n");


		return StyleOutput.toString();
	}	// TableStyles class
	
}	// end TableStyles class.