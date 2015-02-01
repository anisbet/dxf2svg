
/****************************************************************************
**
**	FileName:	FontMapElement.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the Font name and scaling information to compensate
**				for discrepancies between AutoCAD's font measuring system and 
**				the true type measuring system.
**
**	Date:		August 20, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 5, 2002
**				0.02 - December 7, 2004
**				1.01 - January 12, 2005 This class now implements the clonable
**				interface.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

/** This object is created to manage fonts internally to Dxf2Svg. It is used by the
*	to store data retrieved from the configuration file.(which should be placed in the 
*	conversion directory).
*	<P>
*	This class can also be used to control font hinting information to control discrepancies
*	between font sizes due to different font measuring systems between AutoCAD and SVG.
*
*	@version 	0.01 - May 5, 2002
*	@author		Andrew Nisbet
*/
public class FontMapElement implements Cloneable
{
	String font;	// font file's logical name or any other value you store.
	double scale;	// scale percent required to display like AutoCAD.
	
	
	///////////////////////////////////////////////////////
	//					Constructor
	///////////////////////////////////////////////////////
	/**	Creates a FontMapElement object with font and scale 
	*	arguments.
	*/
	public FontMapElement(String font, double scaleHint)
	{
		this.font = font;
		this.scale = scaleHint;
	}
	
	/**	Creates a FontMapElement object given a font name.
	*/	
	public FontMapElement(String font)
	{
		this.font = font;
		this.scale = 1.0;
	}


	///////////////////////////////////////////////////////
	//					Methods
	///////////////////////////////////////////////////////		
	/**	Returns the font scale hinting. This is requied because AutoCAD measures fonts
	*	by their absolute heights, while TrueType fonts are not measure that way at all.
	*	The end result is that most fonts appear too small compared to their counterparts
	*	in AutoCAD.
	*/
	public double getCompensationScaleFactor()
	{	
		return scale;
	}
	
	/** Sets the font scale factor.
	*/
	public void setCompensationScaleFactor(double s)
	{	scale = s;	}
	
	/** Returns the logical font file name for the font in question.
	*/
	public String getFont()
	{	return font;	}
	
	public String toString()
	{
		return this.getClass().getName() + "{"+font+","+String.valueOf(scale)+"}";
	}
	
	/** Tests this FontMapElement object with the argument for equality.
	*/
	public boolean equals(Object o)
	{
		if (! (o instanceof FontMapElement))	// not even the same class
			return false;
		if (this == o)		// they refer to the same memory location 
			return true;
			
		FontMapElement tmp = (FontMapElement)o;
		if (this.font.equals(tmp.font) && this.scale == tmp.scale)
			return true;
		else
			return false;
	}
	
	/** Clones this FontMapElement.
	*	@throws CastClassException if an object stored on the attributes vector is not an {@link dxf2svg.util.Attribute}
	*/
	protected Object clone()
	{
		FontMapElement fme;
		
		try
		{
			fme = (FontMapElement)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new InternalError("clone of FontMapElement failed.");
		}

		// FontMapElement' data
		fme.font 	= this.font;
		fme.scale 	= this.scale;
		
		return fme;
	}
}