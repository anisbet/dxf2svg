
/****************************************************************************
**
**	FileName:	TableLayer.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates DXF layer tables
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.10 - August 13, 2002 two methods added to report if a layer
**				is locked or frozen. Used by SvgObject to control visibility
**				and ultimately in the future to control if objects get
**				written to SVG.
**				1.11 - September 6, 2002 Removed the attribute for fill of
**				none. This means that objects that, by default, take a fill
**				like SvgCircle, SvgArc and SvgEllipse now need to specifically
**				mention their fill values. This is done so things like SvgSolid
**				don't have fills that compete with the layer's fill attribute.
**				1.5 - June 23, 2004 Created setter and getter methods for statically
**				defining language layers and layers that various parts of notes can
**				be found.
**				1.5 - August 31, 2004 Made the constructor public so it has public
**				accessability rather than protected package accessability.
**				1.6 - September 15, 2004 Changed the DEFAULT_COLOUR to -254 from
**				-998 because dxfs with just an inserted image would not convert
**				because the previous default colour would throw an invalid colour
**				exception in the Pen class. The same for DEFAULT_FILL.
**				1.61 - January 7, 2005 Added methods that take a CustomeLayerStyle
**				and update this layer's current settings. This is called by 
**				the style sheet generator.
**				1.62 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.*;						// StyleSheetGenerator and DxfPreprocessor
import dxf2svg.util.CustomLayerStyle;

/**
*	This class represents an encapsulation of DXF layer tables. These tables
*	are stored in java.util.HashMaps and referenced in style sheets
*	by the StyleSheetGenerator object.
*
*	@version	1.6 - September 15, 2004
*	@author		Andrew Nisbet
*/

public final class TableLayer
{
	// Layer information required by various classes that relates to layers.
	/** These values are the names of language layers from the DXF file. lang1 is english
	*	lang2 is french the default layer name is 't'. All these values can be set dynamically
	*	from the command line with the switch '-english_note_layer_name', '-french_note_layer_name'
	*	and finally '-default_target_english_note_layer_name' and '-default_target_french_note_layer_name'.
	*	What ever the layers are called, they will be switched to the 'english' and 'french' 
	*	layers if they exist and if they don't then they are placed on the defaultNoteLayerName.
	*/
	protected static String lang1NoteLayerName = "noteseng";
	protected static String lang2NoteLayerName = "notesfre";
	protected static String noteNumbersLayerName = "notenums";
	protected static String lang1LayerName = "english";
	protected static String lang2LayerName = "french";
	protected static String defaultLangLayerName = "t";
	
	public final static int DEFAULT_COLOUR = -254;	// an impossible value for a stroke colour
	public final static int DEFAULT_FILL = -254;	// an impossible value for a fill
	// instance data
	private String LayerName;
	private String LineTypeName;
	private DxfConverter conversionContext;
	private StyleSheetGenerator SSG;		// the real reason we need the conversion context.
	private int fill;				// fill for this layer, if set.
	private Pen pen;				// This layer's pen object.




	//////////////////////////////////////////////////////////////////////
	// 							constructor
	//////////////////////////////////////////////////////////////////////
	public TableLayer(DxfConverter dxfc)
	{
		conversionContext = dxfc; // get the SSG (after its creation) for the toString() method.
		init();
	}
	
	public TableLayer(StyleSheetGenerator ssg)
	{
		SSG = ssg;
		init();
	}
	
	//////////////////////////////////////////////////////////////////////
		// 							methods
	//////////////////////////////////////////////////////////////////////
	// Proforms general object initialization.
	private void init()
	{
		fill = DEFAULT_FILL;
		// The pen number is not critical yet so I set it to a default
		// pen number value.
		// This pen currently has a colour of black and a line weight of 0.01 inches.
		pen = new Pen();		
	}
	
	
	
	//////////////////////// Static Methods //////////////////////////////
	/** Allows a client to set note layer names. The default is {@link #lang1NoteLayerName}
	*	for English and {@link #lang2NoteLayerName} for French.
	*	see {@link #setDefaultLanguageLayerName(java.lang.String)} for information about setting the default layer
	*	name. This is required in the case that the graphic is a bilingual illustration i.e.
	*	it contains two languages that are both visible all the time. If this is the case 
	*	it is not possible to move the objects to the 'english' or 'french' layer
	*	because they do not exist. In this case the instruction will be to move items on these
	*	layers to {@link #setDefaultLanguageLayerName}.
	*	@see #setDefaultLanguageLayerName(java.lang.String)
	*	@see #setLanguageLayerName(int, String)
	*/
	public static void setNoteLayerName(int language, String name)
	{
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				lang1NoteLayerName = name;
				break;
				
			case Dxf2SvgConstants.FRENCH:
				lang2NoteLayerName = name;
				break;
				
			default:
				System.err.println("TableLayer: setNoteLayerName()," + 
					language + " is an unknown language argument. The defaults will be applied.");
		}
	}
	
	/** Returns the name of the layer that notes can be found on predicated on the 
	*	argument language type.
	*/
	public static String getNoteLayerName(int language)
	{
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				return DxfPreprocessor.convertToSvgCss(lang1NoteLayerName);
				
			case Dxf2SvgConstants.FRENCH:
				return DxfPreprocessor.convertToSvgCss(lang2NoteLayerName);
				
			default:
				System.err.println("TableLayer: setNoteLayerName()," + 
					language + " is an unknown language argument. The defaults will be applied.");
		}
		
		return null;
	}
	
	
	/** Returns the name of a language layer predicated on the argument language type.
	*/
	public static String getLanguageLayerName(int language)
	{
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				return DxfPreprocessor.convertToSvgCss(lang1LayerName);
				
			case Dxf2SvgConstants.FRENCH:
				return DxfPreprocessor.convertToSvgCss(lang2LayerName);
				
			default:
				System.err.println("TableLayer: setNoteLayerName()," + 
					language + " is an unknown language argument. The defaults will be applied.");
		}
		
		return null;
	}
	
	/** Returns the name of the default langugage layer name. The default is layer 't' on 
	*	Spar illustrations. The return name is normalized to CSS standards (odd characters 
	*	are translated to standard CSS naming convensions.)
	*/
	public static String getDefaultLanguageLayerName()
	{
		return DxfPreprocessor.convertToSvgCss(defaultLangLayerName);
	}

	/** Allows the client to specify the name of the layer that the notes should be moved to. In 
	*	the case where notes appear on the default 'noteseng'; once the notes have been processed
	*	the text should be moved to the 'english' layer so they can participate with standard
	*	language processing (switching of languages etc.)
	*	@see #setDefaultLanguageLayerName(java.lang.String)
	*	@see #setNoteLayerName(int, java.lang.String)
	*/
	public static void setLanguageLayerName(int language, String name)
	{
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				lang1LayerName = name;
				break;
				
			case Dxf2SvgConstants.FRENCH:
				lang2LayerName = name;
				break;
				
			default:
				System.err.println("TableLayer: setLanguageLayerName()," + 
					language + " is an unknown language argument. The defaults will be applied.");
		}
	}
	
	
	/** Sets the default target note layer name; that is if the language target layer name 
	*	doesn't exist then we cover by setting the value to the {@link #defaultLangLayerName}.
	*	@see #setLanguageLayerName(int, java.lang.String)
	*	@see #setNoteLayerName(int, java.lang.String)
	*/
	public static void setDefaultLanguageLayerName(String name)
	{
		defaultLangLayerName = name;
	}

	/** This method allows the user to specify the name of the layer that note numbers can be 
	*	found on. The default is 'notenums'. There is no method that changes the target layer
	*	name of the note numbers as it is assumed that the index numbers are not translated and
	*	therefore get placed on the default text layer specified with 
	*	{@link #setDefaultLanguageLayerName(java.lang.String)}.
	*/
	public static void setNoteNumberLayerName(String name)
	{
		noteNumbersLayerName = name;
	}
	
	/** Returns the name of the layer that the note numbers appears on 
	*	(default {@link #noteNumbersLayerName}).
	*/
	public static String getNoteNumberLayerName()
	{
		return DxfPreprocessor.convertToSvgCss(noteNumbersLayerName);
	}
	
	
	//////////////////////////// end of static methods ///////////////////////////
	
	
	
	
	/** Sets the name of the layer, also removes spaces and for consistance
	*	places all characters into lower case in preparation to be used as
	*	class names.
	*/
	public void setName(String name)
	// so there is no confusion over Continuous or CONTINUOUS
	{
		// we do this because multiple class names are separated
		// by white space and autocad supports spaces in layer names.
		LayerName = DxfPreprocessor.convertToSvgCss(name);
	}

	/** Returns name of layer. Used to fetch an TableLayer object from
	*	a hashmap using the name as a key.*/
	public String getName()
	{	return LayerName;	}

	/** Sets the line type for this layer. */
	public void setLineTypeName(String ltname)
	{	LineTypeName = ltname.toUpperCase();	}

	/** Returns the line type for this layer. */
	public String getLineTypeName()
	{	return LineTypeName;	}

	/** Sets the layer's colour.
	*	@param colour Acad colour number as an integer.
	*/
	public void setColour(int colour)
	{	
		// Note: any neg colour value indicates that the layer is hidden
		// (either off or frozen). Once the LayerVisible is switched off
		// there is no external way to turn it back on. This is because
		// we have to account for customs layer styles. If the layer from
		// the DXF is hidden then LayerVisible gets set to false. Then,
		// along comes a a custom style and sets the colour to black, or
		// what have you. If that occured and the layer reset the LayerVisible
		// flag then the layer would appear if custom styles were applied and
		// disappear if default styles were applied.
		//
		// This is all done in the Pen.java object.
		pen.setColour(colour);
	}

	/** Sets the locked or frozen switch for this layer.
	*	<UL>
	*	<LI>1 = Layer is frozen; otherwise layer is thawed.
	*	<LI>2 = Layer is frozen by default in new viewports.
	*	<LI>4 = Layer is locked.
	*	</UL>
	*/
	public void setLayerVisible(int test)
	{
		// Table Processor will grab a group code pair called
		// '70' this is a bit field test for this layer's status.
		// Some of the bits we will ignore for now.
		//1 = Layer is frozen; otherwise layer is thawed.
		//2 = Layer is frozen by default in new viewports.
		//4 = Layer is locked.

		if ((test & 1) == 1 || (test & 2) == 2)
			pen.setVisible(false);
	}

	/** Reports whether this layer is locked or frozen */
	public boolean getLayerVisible()
	{	return pen.isVisible();	}

	/** Returns this layer's colour as an Acad colour number.
	*	@return Colour as an integer. This is the colour number
	*	directly from the DXF. The colour of a layer can be over-
	*	ridden with the '-c' switch. This will coerce the colour
	*	to a predefined value.
	*/
	public int getColour()
	{	return pen.getColour(1);	}	// The one is necessary to signal int return value

	/**	Sets the line weight for the layer. This is primarily information 
	*	gleened from {@link CustomLayerStyle}.
	*/
	public void setLineWeight(double w)
	{	pen.setLineWeight(w);	}
	
	/**	Used to set the fill of a layer.
	*	@param fill The integer colour value of the fill. Refer to an
	*	AutoCAD colour chart for valid values.
	*/
	public void setFill(int fill)
	{	
		if (DxfPreprocessor.isColourCoercedByLayer())
		{
			// This sets the fill colour to the colour of the layer, not to the user specified colour.
			this.fill = pen.getColour(1);
		}
		else
		{
			this.fill = fill;
		}
	}
	
	
	
	/** Returns the integer value of the fill for this layer if set. If it is not set the 
	*	current layer colour is returned. 
	*/
	public int getFill()
	{
		if (fill == 0)
		{
			return getColour();
		}
		
		return fill;
	}
	
	
	/** This method takes a custom layer style passed from the config.d2s file via
	*	the StyleSheetGenerator and updates the layers settings based on those 
	*	custom settings.
	*/
	public void updateLayer(CustomLayerStyle customLayerStyle)
	{
		fill 	= customLayerStyle.getFill(999); // use the integer value of the fill.
		pen		= customLayerStyle.getPen();
	}
	

	/** Returns the sumation of the layer's data (line type, colour)
	*	in a String format for incorporation into stylesheets.
	*/
	public String toString()
	{
		//System.out.println("LayerTable::"+LayerName+" colour set to "+pen.getColour());
		// Use this method to output to a Cascading Style Sheet
		StringBuffer LayerOutput = new StringBuffer();
		// here we need a mechanism in TableProcessor that will
		// do a look up in a keyed Map for TableLineType so we can
		// include that info as well.  That can be done by querying
		// Dxf2Svg for placement of the CSS and then calling a method
		// to do the search.
		if ( SSG == null )
		{
			// Sometimes a TableLayer can be constructed with a SSG to begin with.
			SSG = conversionContext.getStyleSheetGenerator();
		}
		pen.setLineType(SSG.getLayerLineTypeTable(LineTypeName));
		
		LayerOutput.append(".st"+LayerName+"{");
		
		// now attach the Pen definitions which will take care of any 
		// line weight, colour, and visibility.
		LayerOutput.append(pen.toString());
		
		if (fill > -256 && fill <= 256)
			LayerOutput.append("fill:"+DxfPreprocessor.getColour(fill)+";");
			
		LayerOutput.append("}\n");
		return LayerOutput.toString();
	}
}