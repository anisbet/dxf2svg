
/****************************************************************************
**
**	FileName:	SvgUtil.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class is used for various utility functions and
**				common conversions.
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 September 6, 2002 Made this class thread safe by making
**				a constructor to take a HeaderProcessor and making the
**				methods of the class non-static.
**				1.02 September 11, 2002 Removed 'final' qualification
**				from USR_UNITS to allow for determining screen size
**				dynamically.
**				1.03 October 31, 2002 Added Text height calculater.
**				1.04 November 27, 2002 Removed the Dxf2SvgConst interface.
**				1.05 October 31, 2003 Added LimitsFrame object as experiment
**				in view port clipping.
**				1.06 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				2.0 - April 20, 2005 Implemented new constructor to take min/max width 
**				and height sizes instead of the DxfConverter object.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;


import java.awt.Toolkit;		// for Toolkit and Dimension.
import java.awt.Dimension;		// so it doesn't collide with our Point.
import dxf2svg.*;
import dxf2svg.svg.*;			// Point.

/**
*	This class provides various utilities and conversions methods.
*	It also provides controls sizing of Svg output files by determining
*	screen size and optimum page sizing in the default view.
*
*	@version	2.0 - April 20, 2005
*	@author		Andrew Nisbet
*/
public final class SvgUtil
{
	private int Precision;				// get value from Dxf2Svg class
	
	private double limitsMinX;
	private double limitsMinY;
	private double limitsMaxX;
	private double limitsMaxY;
	private double lineTypeScale;
	
	private double classVersion = 1.05; // version number of this class
	/**
	*	This value is what the SVG specification refers to as
	*	the "users units". It is arbitrary and dependant on resolution
	*	and screen sizing.  This value is based on the measurement of the
	*	height of a AG1P frame (8.75") and I wanted it to fit in a window
	*	that is 600 pixels high.  600 / 8.75 = 68.571429.
	*
	*	We also need to account for the unusual prospect of the drawing
	*	being drawn in metric. If that is the case convert it to inches.
	*/
	private static double USR_UNITS = 68.571429;	// Pixels per inch.
	private static double POINT_UNITS = 72 / USR_UNITS; // Points per pixels.
	private static double ILLUSTRATION_HEIGHT = 8.75; // height of AG1P in inches
	private Toolkit tk;				// Toolkit for getting the screen dimensions
	private Dimension ScreenDim;	// Screen dimension.
	// Size of browser display area without tool bars
	// as a percent ~= 71% on average but I have measured it, with default settings, on all
	// of the resolutions supported and included the percentage of usable display space
	// for each.
	private double BrowserWindowHeight;
	private int Resolution;			// target screen resolution.
	private int embedImageWidth;	// Max viewport for embedded Svg in HTML
	private int embedImageHeight;	// Max viewport for embedded Svg in HTML
	private static LimitsFrame frame;// Frame of the Limits of the DXF file.

	// Constructor
	/**
	*	The constructor calculates the target User Units (the default
	*	measure unit in Svg) by querying the current screen resolution
	*	(default) or, alternatively, by making the appropriate calculations
	*	based on the <code><b>-s</b></code> switch value. The end result
	*	will size an 8.75 inch illustration to fit within a default Internet
	*	Explorer window with a high degree of accuracy.
	*	@param minX Minimum x value of the drawing's limits.
	*	@param minY Minimum y value of the drawing's limits.
	*	@param maxX Minimum x value of the drawing's limits.
	*	@param maxY Minimum y value of the drawing's limits.
	*	@param scale Line type scale.
	*/
	public SvgUtil(double minX, double minY, double maxX, double maxY, double scale)
	{
		limitsMinX = minX;
		limitsMinY = minY;
		limitsMaxX = maxX;
		limitsMaxY = maxY;
		
		lineTypeScale = scale;
		
		// Now we calculate the size of the svg graphic dynamically.
		// by first calculating the User Units to be used in further
		// calculations.
		Resolution = DxfPreprocessor.getRenderSize();

		// now calculate average percentage of usable display area for
		// each of the resolution settings (excluding menu bars etc.
		// in the browser).


		switch (Resolution){
			case Dxf2SvgConstants.CURRENT_RESOLUTION:
				tk = Toolkit.getDefaultToolkit();
				ScreenDim = tk.getScreenSize();
				BrowserWindowHeight = 0.71;
				USR_UNITS = (ScreenDim.getHeight() * BrowserWindowHeight) / ILLUSTRATION_HEIGHT;
				POINT_UNITS = 72.0 / USR_UNITS;
				embedImageHeight = (int)(ScreenDim.getHeight() * BrowserWindowHeight);
				break;

			case Dxf2SvgConstants.SIXFORTY_X_FOUREIGHTY:
				BrowserWindowHeight = 0.68;
				USR_UNITS = ((double)480 * BrowserWindowHeight) / ILLUSTRATION_HEIGHT;
				POINT_UNITS = 72.0 / USR_UNITS;
				embedImageHeight = (int)(480 * BrowserWindowHeight);
				break;

			case Dxf2SvgConstants.EIGHT_HUNDRED_X_SIX_HUNDRED:
				BrowserWindowHeight = 0.71;
				USR_UNITS = ((double)600 * BrowserWindowHeight) / ILLUSTRATION_HEIGHT;
				POINT_UNITS = 72.0 / USR_UNITS;
				embedImageHeight = (int)(600 * BrowserWindowHeight);
				break;

			case Dxf2SvgConstants.TENTWENTYFOUR_X_SEVENSIXTYEIGHT:
				BrowserWindowHeight = 0.76;
				USR_UNITS = ((double)768 * BrowserWindowHeight) / ILLUSTRATION_HEIGHT;
				POINT_UNITS = 72.0 / USR_UNITS;
				embedImageHeight = (int)(768 * BrowserWindowHeight);
				break;

			default:
				// Should never get here.
				BrowserWindowHeight = 0.71;
				USR_UNITS = 68.571429;
				POINT_UNITS = 72.0 / USR_UNITS;
				embedImageHeight = (int)(600 * BrowserWindowHeight);
		}
		
		// The 1.65 an estimate of aspect ratio for the screen width to height
		embedImageWidth = (int)((double)embedImageHeight * 1.8);
		
		// Here we will attend to the creation of the Dxf frame limits.
		// This will allow querying SvgText elements to determine if they
		// are in fact visible in the svg conversion. Elements outside of 
		// the limits area will continue to be drawn but SvgText elements
		// will be rendered in the Svg as <desc> tags. This will facilitate
		// searches for page numbers NDID numbers and illustration titles.
		// 
		// In the future this will also allow for the setting of the next and
		// last illustration chain information.
		// We will also convert the frame to Svg space for so points can be
		// compared in a more meaningful way.
		double pMinX = convertXToSvgSpace(limitsMinX) - deltaDxfSvgLimitsX();
		double pMinY = convertYToSvgSpace(limitsMinY) - deltaDxfSvgLimitsY();
		double pMaxX = convertXToSvgSpace(limitsMaxX) - deltaDxfSvgLimitsX();
		double pMaxY = convertYToSvgSpace(limitsMaxY) - deltaDxfSvgLimitsY();		
		frame = new LimitsFrame(pMinX, pMinY, pMaxX, pMaxY);
		
		
		if (DxfPreprocessor.verboseMode() == true)
		{
			System.out.println("USR_UNITS: "+USR_UNITS);
			System.out.println("POINT_UNITS: "+POINT_UNITS);
			System.out.println("Embedded Image height: "+embedImageHeight);
			System.out.println("Embedded Image width: "+embedImageWidth);
		}
	}
	
	/**	This method tests if the argument point is within the frame designated by the DXF's 
	*	limits.
	*	@return true if the point is within or on the frame and false if outside the limits frame.
	*/
	public boolean isWithinLimitsFrame(double x, double y)
	{	return frame.contains(x,y);	}
	
	/** Returns the rectangular frame that outlines the frame that describes the Dxf's limits.
	*/
	public LimitsFrame getLimits()
	{	
		return frame;
	}
	
	
	/**	Returns the maximum height of the viewport that will allow the drawing to viewed
	*	and the <B>English / French</B> button to appear properly when the browser is maximized.
	*	This value is used to specify the height <CODE>attribute</CODE> of the &lt;EMBED&gt; tag in HTML.
	*/
	public final int getMaximumHTMLViewportHeight()
	{
		return embedImageHeight;
	}
	
	/**	Returns the maximum width of the viewport that will allow the drawing to viewed
	*	and the <B>English / French</B> button to appear properly when the browser is maximized.
	*	This value is used to specify the height <CODE>attribute</CODE> of the &lt;EMBED&gt; tag in HTML.
	*/
	public final int getMaximumHTMLViewportWidth()
	{
		return embedImageWidth;
	}

	/** Trims double values to user specified number of digit precision.
	*
	*	@param DNum number you wish to trim.
	*	@return double Trimmed value as double.
	*/
	public double trimDouble(double DNum)
	{
		double Result;
		String DblStr = new String();
		Precision = DxfPreprocessor.getPrecision();


		// catch any extremely small numbers like -4.27E-23
		if (Math.abs(DNum) < Math.pow(10, -(Precision + 1)))
			return 0.0;

		DblStr = Double.toString(DNum);
		// catch anything that is trying to trim Infinity or NaN.
		if (DblStr.endsWith("Infinity") || DblStr.equalsIgnoreCase("NaN"))
			return DNum;

		int index = DblStr.indexOf('.');

		try
		{
			DblStr = DblStr.substring(0,(index + Precision + 1));
			Result = Double.parseDouble(DblStr);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			// user asked for more dec places than there are so return
			// what ever we can.
			return DNum;
		}
		
		return Result;
	}

	/** Used to trim numbers to an arbitrary precision despite what was
	*	set at runtime with the <code>-p</code> switch.
	*
	*	Used for increasing precision of angles for trigonometric calculations.
	*
	*	@param DNum Number to trim as a double
	*	@param P	as an integer of the number of digits precision.
	*	@return double vale trimmed to <code>P</code> digits.
	*/
	public double trimDouble(double DNum, int P)
	{
		double Result;
		String DblStr = new String();
		int AnglePrecision = P;

		// catch any extremely small numbers like -4.27E-23
		if (Math.abs(DNum) < Math.pow(10, -(AnglePrecision + 1)))
			return 0.0;

		DblStr = Double.toString(DNum);
		// catch anything that is trying to trim Infinity or NaN.
		if (DblStr.endsWith("Infinity") || DblStr.equalsIgnoreCase("NaN"))
			return DNum;
		int index = DblStr.indexOf('.');

		try
		{
			DblStr = DblStr.substring(0,(index + AnglePrecision + 1));
			Result = Double.parseDouble(DblStr);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			// user asked for more dec places than there are so return
			// what ever we can.
			return DNum;
		}
		return Result;

	}


	/** Returns the value of USR_UNITS (pixels per inch).
	*	interface.
	*	@return double USR_UNITS
	*/
	public final double Units()
	{
		/*
		*	This method returns specific screen resolution values depending on what
		*	resolution the browser is set to.
		*/

		return USR_UNITS;
	}

	/** Returns the points per pixel for the user defined screen resolution.
	*/
	public final double pointsPerPixel()
	{	return POINT_UNITS;	}

	/** This method converts an <B>x</B> value to Svg space. Essentially
	*	this method makes the convertion the <B>x</B> value to pixels as
	*	there is no other transformation for the <B>x</B> coordinate from
	*	DXF space.
	*/
	public double convertXToSvgSpace(double x)
	{	return x * USR_UNITS;	}

	/**
	*	Converts DXF 'y' values into SVG equivalents.
	*
	*	This is required because DXF space numbers its 'y' coordinates
	*	starting with 0 at the bottom of the image and SVG enumerates its
	*	'y' values starting with 0 from the top of the page. X values
	*	are handled the same way in both file formats.
	*	@param yVal value you want to flip on axis into SVG space.
	*	@return double Converted value.
	*/
	public double convertYToSvgSpace(double yVal)
	{

		/*
		*	Dxf space has it's y-axis origin at the bottom of the page and
		*	Svg's y-axis origin is at the top-left. In addition Svg space
		*	is relative based on pixels and is called UserUnits by the
		*	specification.
		*
		*	The following formula will convert the y-axis values from DXF space
		*	to SVG space.
		*
		*	ss = SVG space
		*	as = Acad space
		*
		*	(ss)y = ( (as)y(max) - (as)y )
		*/

		return (limitsMaxY - yVal) * USR_UNITS;
	}
	


	/** This method will flip a point from one space to another. This is
	*	required for classes that perform calculations in DXF space and
	*	then need to flip into SVG space. It should be noted that there
	*	is an assumtion that both x and y values of the argument point
	*	are in USR_UNITS.
	*	
	*	@see SvgEllipse#getElementAsPath
	*/
	public void flipCoordinateSpace(Point p)
	{
		//double x = p.getX();
		double y = p.getY();

		//p.setXUU(x);
		p.setYUU(limitsMaxY * USR_UNITS - y);
	}

	/** Converts SVG y value of a Point back to the equivelant DXF y value.
	*	This is essentially the reverse of {@link #convertYToSvgSpace} with
	*	the exception that the returned value remains in USR_UNITS (pixels)
	*	not the original Inches or MM.<BR><RB>
	*	<i>There is no convertXToDxfSpace() method because there is no X
	*	transformation from DXF space to SVG space.</i>
	*/
	public double convertYToDxfSpace(double yVal)
	{
		/*
		*	Dxf space has it's y-axis origin at the bottom of the page and
		*	Svg's y-axis origin is at the top-left. In addition Svg space
		*	is relative based on pixels and is called UserUnits by the
		*	specification.
		*
		*	The following formula will convert the y-axis values from SVG space
		*	to DXF space.
		*
		*	ss = SVG space
		*	as = Acad space
		*
		*	(as)y = ((as)y(max) * USER_UNITS) - (as)y
		*/

		return limitsMaxY * USR_UNITS - yVal;
	}



	/**
	*	Returns the delta between the DXF origin which could be arbitrary
	*	and the SVG origin which is always 0,0.
	*	@return double The delta between the X values in these files.
	*/
	public double deltaDxfSvgLimitsX()
	{
		/*
		*	There are a great deal of illustrations in our library that have their lower limits
		*	set to something other than 0,0. If we process these illustrations there will be
		*	drawn in Svg off the screen - not good in a browser.
		*
		*	This function corrects that by allowing all points to auto-translate to 0,0
		*	in svg space.
		*
		*	This one handles the X value.
		*/
		// don't forget to convert to Units.
		return limitsMinX * USR_UNITS;
	}

	/**
	*	Returns the delta between the DXF origin which could be arbitrary
	*	and the SVG origin which is always 0,0.
	*	@return double The delta between the Y values in these files.
	*/
	public double deltaDxfSvgLimitsY()
	{
		/*
		*	There are a great deal of illustrations in our library that have their lower limits
		*	set to something other than 0,0. If we process these illustrations there will be
		*	drawn in Svg off the screen - not good in a browser.
		*
		*	This function corrects that by allowing all points to auto-translate to 0,0
		*	in svg space.
		*
		*	This one handles the Y value.
		*/
		return limitsMinY * USR_UNITS;
	}



	/**
	*	Replaces characters with their equivalent character	entities.
	*
	*	@param s the string input to replace characters in
	*	@return String with values replaced.
	*/
	public static String replaceCharacterEntities(String s)
	{
		String DataStr = s;							// the arg string
		StringBuffer sbuff = new StringBuffer(s);	// a temp resource
		int pos = 0;								// position of hit in string
		final String marker = "@=!=@";				// Marker can be made of any no-entity chars.

		// now we need a list of what to replace
		final int[] searchChars = {
			37,		// '%'
			38,		// '&'
			35,		// '#'
			/* added December 16, 2003 - Found some of these in wiring diagrams. */
			60,		// <
			62,		// >
			/* added January 26, 2004. */
			150,	// '--'
			151,	// '---'
			192,	// À
			193,	// Á
			194,	// Â
			199,	// Ç
			200,	// È
			201,	// É
			202,	// Ê
			224,	// à
			225,	// á
			226,	// â
			231,	// ç
			232,	// è
			233,	// é
			234		// ê
		};

		final String[] replaceChars = {
			marker + "037;",	// '%'
			marker + "038;",	// '&'
			marker + "035;",	// '#'
			marker + "060;",	// '<'
			marker + "062;",	// '>'
			marker + "150;",	// '--'
			marker + "151;",	// '---'
			marker + "192;",	// À
			marker + "193;",	// Á
			marker + "194;",	// Â
			marker + "199;",	// Ç
			marker + "200;",	// È
			marker + "201;",	// É
			marker + "202;",	// Ê
			marker + "224;",	// à
			marker + "225;",	// á
			marker + "226;",	// â
			marker + "231;",	// ç
			marker + "232;",	// è
			marker + "233;",	// é
			marker + "234;"		// ê
		};




		// Pass 1
		// replace with the suffix because if we search again on '&'
		// we go into an endless loop. This solution would probably make a real
		// programmer gag but it works for all cases except when the text includes
		// a "marker" which will hopefully be improbable.
		for (int i=0; i<searchChars.length; i++)
		{
			while ((pos = DataStr.indexOf((char)searchChars[i])) >= 0)
			{
				sbuff.replace(pos,(pos + 1),replaceChars[i]);
				DataStr = sbuff.toString();
			}
		}

		// Pass 2 replace marker
		while ((pos = DataStr.indexOf(marker)) >= 0)
		{
			sbuff.replace(pos,(pos + marker.length()),"&#");
			DataStr = sbuff.toString();
		}

		return DataStr;
	}


	/** This function simply reports whether a supplied test value is within range
	*	of (+-) the supplied fuzz value.
	*	@param test Value to be tested for tolerances.
	*	@param norm The value to be measured against.
	*	@param fuzz The acceptable tolerance as a percentage. 3.5 is 3 1/2 percent.
	*	@return true if test is within tolerances; false otherwise.
	*/
	public static boolean isWithinTolerance(double test, double norm, double fuzz)
	{
		double range = norm * (fuzz / 100.0);
		
		if (test <= (norm + range) && test >= (norm - range))
			return true;
		
		return false;
	}
	
	
	/** Returns the line type scale state from the DXF being converted.
	*/
	public double getLtScale()
	{
		return lineTypeScale;
	}

	/** Returns the maximum X value of the limits frame.
	*/
	public double getLimitsMaxX()
	{
		return limitsMaxX;
	}

	/** Returns the minimum X value of the limits frame.
	*/	
	public double getLimitsMinX()
	{
		return limitsMinX;
	}

	/** Returns the maximum Y value of the limits frame.
	*/	
	public double getLimitsMaxY()
	{
		return limitsMaxY;
	}

	/** Returns the minimum Y value of the limits frame.
	*/	
	public double getLimitsMinY()
	{
		return limitsMinY;
	}

	/**
	*	Reports the SvgUtil version and current User Units value.
	*/
	public String toString()
	{
		return "SvgUtil version "+classVersion+", November 27, 2002\n"+
			"Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.0_01-b03)\n"+
			"Java HotSpot(TM) Client VM (build 1.4.0_01-b03, mixed mode)"+
			"current User Units: "+String.valueOf(Units())+
			"target screen dimensions: "+ScreenDim.toString();
	}
}