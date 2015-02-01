
/****************************************************************************
**
**	FileName:	SvgFontMetrics.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the font metrics object required to convert
**				fonts sized in inches to points in Svg space.
**
**	Date:		November 21, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 21, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;		// AffineTransform and FontRenderContext.

/**	This class represents the encapsulation of the metrics of a
*	text styles font and line metrics. There are several useful
*	methods that can be called to get information about this style's
*	font. The default measurement values returned from this class
*	are doubles to be consistant with expected returns and unlike
*	java.awt.FontMetrics.<BR><BR>
*
*	The implementation of this class within Dxf2Svg stops the conversion
*	if the font required for the conversion does not exist on the system.
*
*	@see UnResolveableSvgFontMetricsException
*	@version	1.00 - November 21, 2002
*	@author		Andrew Nisbet
*/
public class SvgFontMetrics
{
	private Font font;
	private String fontName;
	private double textHeight;
	private AffineTransform af;
	private FontRenderContext frc;
	private LineMetrics lm;
	private Rectangle2D r2d = new Rectangle2D.Double();

	// actual metric valiues
	private double height;	// distance from baseline to next baseline.
	private double ascent;	// height of text above baseline
	private double decent;	// distance decenders decend below baseline
	private double leading;	// distance from baseline to top of next line

	/**
	*	Create a new SvgFontMetrics object with a FontName and TextHeight
	*	as reference.
	*	@throws UnResolveableSvgFontMetricsException
	*/
	public SvgFontMetrics(String FontName, double TextHeight)
		throws UnResolveableSvgFontMetricsException
	{
		if (FontName == null || TextHeight == 0.0)
			throw new UnResolveableSvgFontMetricsException();

		textHeight = TextHeight;
		int size = (int)textHeight;
			// accuracy.
		font = new Font(FontName, Font.PLAIN, size);

		af = font.getTransform();
		frc = new FontRenderContext(af, true, true);
		String test = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
		lm = font.getLineMetrics(test, frc);

		height = 		(double)lm.getHeight();//Math.ceil((double)tmpHigh);
		ascent = 		(double)lm.getAscent();
		decent = 		(double)lm.getDescent();
		leading = 		(double)lm.getLeading();
	}


	public final double getWidth(String str)
	{
		r2d = font.getStringBounds(str,frc);
		return r2d.getWidth();
	}

	/** Returns the height of the text. The definition of height is
	*	the distance from the bottom of a line of text to the bottom
	*	of the next line of text.
	*/
	public final double getLineSpacing()
	{	return height;	}

	/**	Returns the ascent or distance from the baseline to the top of
	*	the tallest character.
	*/
	public final double getAscent()
	{	return ascent;	}

	/**	Returns the distance from the baseline to the bottom of the
	*	lowest decender. Decender characters are lowercase 'p' 'j'
	*	'g' etc..
	*/
	public final double getDecent()
	{	return decent;	}

	/**	Returns the leading of a line of text. The leading is the
	*	recommended interline spacing for the font (named for the
	*	strips of lead that were placed between rows of movable
	*	type).
	*/
	public final double getLeading()
	{	return leading;	}

	/** Returns a String representation of this class.
	*/
	public final String toString()
	{
		return this.getClass()+": '"+fontName+"':"+textHeight+
			"\nHeight: "	+Double.toString(height)+
			"\nAscent: "	+Double.toString(ascent)+
			"\nDescent: "	+Double.toString(decent)+
			"\nLeading: "	+Double.toString(leading);
	}



	/**
	*	This checked exception can occur if the SvgFontMetrics object is
	*	instantiated before the style table has its font name or
	*	font size set.
	*/
	public class UnResolveableSvgFontMetricsException extends RuntimeException
	{
		public UnResolveableSvgFontMetricsException()
		{
			super("Either the font name or font size are not set.\nFontName: "+
				fontName+"\nFixedTextHeight: "+textHeight);
		}
	}
} 	// SvgFontMetrics class