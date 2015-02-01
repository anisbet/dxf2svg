
/****************************************************************************
**
**	FileName:	StyleSheetGenerator.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Set up the SVG graphics "state" ready for outputting SvgObjects
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.50 - August 14, 2002 Added getLayerIsVisible() and getColour(int)
**				Made ColourTable private.
**				1.51 - August 22, 2002 Changed HashMaps to Hashtables for
**				multithreading.
**				1.52 - September 5, 2002 Changed setLayerInfo() method to
**				not pre-format Layer names with 'st' (because that makes
**				them class names).
**				1.53 - September 6, 2002 Added DefaultClasses and changed
**				methods: makeSparStyleSheet() and makeStyleSheet() to include
**				its information into style sheets. This allows common objects
**				like text to automatically have basic attributes included
**				in class descriptions. Made the class final.
**				1.60 - October 30, 2002 Now handles Style Tables.
**				1.70 - March 24, 2003 Moved Colour lookup table to DxfPreprocessor.
**				1.71 - January 6, 2005 SSG now updates the layer information
**				from the config.d2s file in the setLayerInfo() method istead
**				of waiting for the last minute in the conversion to check
**				each layer for changes. This has made conformToCustomStyle() 
**				redundant and so the method has been depricated and removed.
**				1.72 - January 11, 2005 Added similar functionality for TableStyles
**				so they can be updated from the config.d2s file.
**				1.73 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg;

import java.util.*;		// for Hashtable
import java.io.*;		// for external style sheet production.
import dxf2svg.*;		// DxfConverter
import dxf2svg.svg.*;
import dxf2svg.util.*;	// Tables, CustomLayerStyle CustomTextStyle.

/**
*	This class will generate internal or external style sheets for SVG
*	depending on the stylesheet option switch on the command line.
*
*	If the user wants to use style sheets then this class will:<BR><BR>
*	1) test for existing css directory<BR>
*	2) test for existing svg.css in that directory<BR>
*	3) if both test are false and MAKE_CSS is true write to file a standard set of
*		Spar styles.<BR><BR>
*
*	If the user wants to include styles in the SVG file then:<BR>
*	1) an external class will call the makeStyle() method which will place style info
*		into a string and return it to the caller to be appended to the SVG (in DTD.append()).<BR>
*	2) for special cases like linetypes that are NOT "Bylayer" use a method that will return
*		a string that contains the correct CSS over-riding info contained in a 'style' attribute.
*		like: style="stroke-dasharray:10 2".<BR><BR>
*
*	Adobe also allows the user to determine if the styles should be entity attributes or
*	declared style attributes at the start of the file.
*
*	@version	1.60 - October 30, 2002
*	@author		Andrew Nisbet
*/

public final class StyleSheetGenerator
{
	private int MAKE_CSS;					// determined by Dxf2Svg's MAKE_CSS var
	private static boolean DEBUG;			// debug switch
	private static boolean VERBOSE;			// Verbosity
	private boolean GEN_NEW_CSS;			// generate one svg.css file only
	private String StyleStr;				// Used for holding temporary Style for elements
	private String StyleSheet;				// for placement into the style section of SVG
	private Hashtable LayerTables;			// Layer table data from TableProcessor.java
	private Hashtable LineTypeTables;		// table of LineTypeObjects
	private Hashtable StyleTables;			// table of Styletable objects
	private Vector Attdef;					// Attribute definitions.


	// constructor
	/** Calls for the following states from Dxf2Svg:<BR><BR>
	*<UL>
	*	<LI> VERBOSE
	*	<LI> DEBUG
	*	<LI> MAKE_CSS
	*</UL>
	*/
	public StyleSheetGenerator( ProcessorManager pm )
	{
		VERBOSE 	= DxfPreprocessor.verboseMode();
		DEBUG		= DxfPreprocessor.debugMode();
		MAKE_CSS 	= DxfPreprocessor.cssMode();
		GEN_NEW_CSS = true;


		// Initialize the TableStyles 
		// HashMap and create one 'DEFAULT'
		// style. We do this because in some cases we have text discribed
		// in the DXF, but there is no "default" text style, or any style
		// at all defined. This occurs when a dimension is the only text
		// on a drawing with no styles defined. This will ensure that
		// there is always one style.
		StyleTables = new Hashtable();
		// now create a default style
		TableStyles defaultStyle = new TableStyles( pm.getSvgUtilInstance() );
		defaultStyle.setStyleName("default");
		defaultStyle.setFixedTextHeight(8.0);
		defaultStyle.setFontFamilyName("Courier");
		// Now there is at least one style on the hashmap.
		StyleTables.put("default",defaultStyle);

		StyleStr = new String();
		StyleSheet = new String();
		// Now let's create a repository for attribute definitions.
		Attdef = new Vector();
	}


	// These are the portals from TableProcessor.
	/** This method takes the layer information collected from TableProcessor and 
	*	makes a new list of the names of the layers for other steps in the conversion.
	*	This method should only be called once.
	*	<P>
	*	This method also checks to see if the default language layer has been found
	*	because if a DXF has notes but does not have a default language layer i.e. there
	*	is a NOTENUM layer but not 't' layer then this method will search for the 
	*	default target layer (no matter what it was set to with '-default_target_note_layer_name')
	*	so when text has to be moved, there is a layer to move it to, otherwise you get a 
	*	NullPointerException as text objects test for line type with {@link #getLineTypeNameByLayer(java.lang.String)}.
	*/
	public void setLayerInfo(Hashtable layers)
	{
		LayerTables = layers;
		// Here we have to check to see if target layers exist for Notes and Note Numbers.
		// This can occur if there is a NOTENUM layer but no 'default' note layer target.
		// Later, method getLineTypeNameByLayer(String) will throw a NullPointerException
		// 'cause the layer doesn't exist.
		//
		// To fix this obscure situation we are going to check to see if the 
		// -default_target_note_layer_name exists and if it doesn't make one with
		// default values.
		//
		// First let's test to see if the default language layer exists and if it doesn't
		// make one.
		String defaultLayerName = TableLayer.getDefaultLanguageLayerName();
		if (! LayerTables.containsKey(defaultLayerName))
		{
			TableLayer TLayer = new TableLayer( this );
			// Name of the layer.
			TLayer.setName(defaultLayerName);
			// LineTypeName
			TLayer.setLineTypeName("CONTINUOUS");
			// Layer colour, which will be the TableLayer.DEFAULT_COLOUR if there is no
			// custom style listed for this layer.
			// TLayer.setColour(DxfPreprocessor.getCustomLayerColour(defaultLayerName));
			// Layer locked or frozen etc.
			// WHAT IS THE VISIBLE LAYER VALUE NUMBER? ZERO IS VISIBLE.
			TLayer.setLayerVisible(0);
			LayerTables.put(defaultLayerName, TLayer);
		}
		// Now we will synchronize the contents of the CustomLayerStyle objects set in the 
		// config.d2s file (if any).
		// Here we will get the list of layer names and query DxfPreprocessor for a CustomLayerStyle
		// to match. If there is one by the same name then get it and update the layer info.
		Vector vLayerNames = new Vector();
		getLayerNames(vLayerNames);
		for (int i = 0; i < vLayerNames.size(); i++)
		{
			String layerName = new String();
			layerName = (String)(vLayerNames.get(i));
			CustomLayerStyle customlayerStyle = DxfPreprocessor.getCustomLayerStyle(layerName);
			if (customlayerStyle == null)
			{
				// couldn't find a custom layer reference for this layer name.
				continue;
			}
			// Get the current layer from the dxf...
			TableLayer layer = (TableLayer)LayerTables.get(layerName);
			// update the layer from the config.d2s settings.
			layer.updateLayer(customlayerStyle);
		}
	}

	/**
	*	Returns the names of all the layers in the original DXF in the
	*	form of a Vector.
	*	@see #setLayerInfo
	*	@throws NullPointerException if the names are requested before the
	*	layer information is parsed from the DXF.
	*/
	public void getLayerNames(Vector nameList)
	{
		//System.out.println("LayerTables"+LayerTables);
		Set LayerKeys = LayerTables.keySet();
		Iterator KeyIterator = LayerKeys.iterator();
		String name;
		while (KeyIterator.hasNext())
		{
			name = (String)KeyIterator.next();
			nameList.add(name);
		}
		if (DEBUG)
			for (int i = 0; i < nameList.size(); i++)
				System.out.println("Layer: "+(String)nameList.get(i));
	}




	/** Allows passing of <I>line type</I> information */
	public void setLineTypeInfo(Hashtable linets)
	{
		LineTypeTables = linets;
		if (DEBUG)
		{
			Set LineTypeKeys = LineTypeTables.keySet();
			Iterator KeyIterator = LineTypeKeys.iterator();
			while (KeyIterator.hasNext())
				System.out.println("Line Types: "+KeyIterator.next());
		}
	}






	///////////////// Style Tables methods //////////////////////
	/** Allows passing of <I>styles</I> information from table processor.
	*	This method also updates the styles based on the content of the
	*	<CODE>textStyle</CODE> values stored in the config.d2s if any.
	*/
	public void setStylesInfo(Hashtable styles)
	{
		// We initialized one object onto the StyleTables Hashtable
		// now it will be overwritten if any other style
		// collected in the TableProcessor.
		StyleTables	= styles;
		if (DEBUG)
		{
			System.out.println("SSG.StyleTables: "+StyleTables.toString());
		}
		
		// Delete styles without names. Yes they do occur.
		StyleTables.remove("");	
		
		// now update the styles with the styles from the config.d2s if 
		// there is one.
		Set StyleKeys = StyleTables.keySet();
		Iterator KeyIterator = StyleKeys.iterator();
		while (KeyIterator.hasNext())
		{
			String name        = (String)(KeyIterator.next());
			String styleString = DxfPreprocessor.getCustomTextStyle(name);
			if (styleString == null)
			{
				continue;
			}
			
			// Create a new custom style object and place it on the 
			// hashtable of styles. This will replace the existing style
			// in the hashtable.
			// Get the dxf style and clone it then add the new information.
			
			TableStyles dxfTableStyle = (TableStyles)(StyleTables.get(name));
			CustomTextStyle cts = new CustomTextStyle(dxfTableStyle);
			cts.setCustomStyle(styleString);
			StyleTables.put(name, cts);
		}
	} // end setStylesInfo()
	
	
	
	
	
	
	/**
	*	Returns a 'TableStyles' object requested by name. With
	*	SvgGraphicElements the objects query the StyleSheetGenerator
	*	for TableLayers information for comparison. SvgText objects
	*	just request a copy of their TableStyles table and query
	*	the data they need. This in the long run is cheaper because
	*	you just query the HashMap once for the table and then all
	*	the rest of the queries are calls to local objects. In the final
	*	analysis layer information is brokered by StyleSheetGenerator
	*	and Styles are handled by the text objects themselves. This
	*	may change in the future.
	*	@param style_name name of the style
	*	@return TableStyles
	*	@throws UndefinedTableException if the style table is null.
	*/
	public TableStyles getStyle(String style_name)
	{
		if (StyleTables == null)
			throw new UndefinedTableException(
				"TableStyles (text style table)");

		TableStyles STable = null;

		if (StyleTables.containsKey(style_name))
			STable = (TableStyles)StyleTables.get(style_name);
		else 	// if the search produced nothing return standard
		{
			System.err.println("StyleSheetGenerator error: no such style '"+
				style_name+"'.");
			if (StyleTables.containsKey("standard"))
				STable = (TableStyles)StyleTables.get("standard");
			else // if that failed to there is our good ole
				 // stand-by.
			{
				System.err.println("StyleSheetGenerator error: standard not set "+
					" returning 'default'.");
				if (StyleTables.containsKey("default"))
				{
					STable = (TableStyles)StyleTables.get("default");
					if (STable == null)
					{
						System.err.println("StyleSheetGenerator error: can't find 'default' either.");
						System.exit(9);
					}
				}
			}
		}

		return STable;
	}
	
	
	
	
	/** This method allows DXF attribute definition objects to be passed for reference
	*	by SvgAttrib objects.
	*	<P>
	*	This method will not add this SvgAttdef if another with the same values already
	*	exists. see {@link SvgAttdef#equals} for what equals means.
	*/
	public void addAttdef(SvgAttdef sa)
	{
		// check for duplicates
		int len = Attdef.size();
		boolean found = false;
		for (int i = 0; i < len; i++)
		{
			if (sa.equals(Attdef.get(i)))
			{
				found = true;
				break;
			}
		}
		
		if (found == false)
		{
			Attdef.add(sa);
			System.out.println("Added ATTDEF: "+sa);
		}
	}





	// Pass back any LineTypeTable as requested by name
	/** Retrieves a line type table by name and uses its <code>toString()</CODE>
	*	method to return table contents.
	*	@throws UndefinedTableException if the line type table is null.
	*/
	public String getLineType(String name)
	{
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		TableLineType TLType = (TableLineType)LineTypeTables.get(name);
		if (TLType == null)
		{
			System.out.println("StyleSheetGenerator getLineType(): couldn't find a line type table with the name: '"+
				name+"'.");
		}
		// now get all the info from the table to fill in the dasharray

		return TLType.toString();
	}

	/** Retrieves any LineTypeTable as requested by name and set scale
	*	@param name of line type required.
	*	@param scale of line pattern.
	*	@return LineTypeTable converted to String.
	*	@throws UndefinedTableException if the line type table is null.
	*/
	public String getLineType(String name, double scale)
	{
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		TableLineType TLType = (TableLineType)LineTypeTables.get(name);
		if (TLType == null)
		{
			System.out.println("StyleSheetGenerator getLineType(String,String): couldn't find a line type table with the name: '"+
				name+"' and scale of " + scale);
			return "";
		}
		// now get all the info from the table to fill in the dasharray

		return TLType.toString(scale);
	}

	/** Pass back any LineTypeTable as requested by name, any scale and by prefered
	*	output style.<BR><BR>
	*
	*	This method is used when you have the unique case of an object whose lineType
	*	differs from it's layer's lineType. You request the special option of
	*	<code>as_attribute</code> and you will get the LineTypeTable as an
	*	attribute value.
	*
	*	@param name of the line type required
	*	@param scale of the line's pattern.
	*	@param style controls if the toString() method of the LineTypeTable
	*	outputs its data in &quot;as_attribute&quot; format.
	*	@throws UndefinedTableException if the line type table is null.
	*/
	public String getLineType(String name, double scale, String style)
	{
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		TableLineType TLType = (TableLineType)LineTypeTables.get(name);
		// now get all the info from the table to fill in the dasharray

		if (style.equals("as_attribute"))
			return TLType.toAttributeString(scale);

		//System.out.println("line type name = '"+name+"'");
		//System.out.println("scale = "+TLType.getScale());
		//System.out.println("scale = "+TLType.getDescription());

		return TLType.toString(scale);
	}

	/**
	*	This method is called by an SvgGraphicElement as soon as its
	*	layer name has been established.
	*	@param LayerName name of the object's layer.
	*	@return line type name by layer i.e. CONTINUOUS.
	*	@throws UndefinedTableException if the layer table is null.
	*/
	public String getLineTypeNameByLayer(String LayerName)
	{
		if (LayerTables == null)
		{
			throw new UndefinedTableException(
				"TableLayer (layer name table) "+LayerName+" ");
		}

		TableLayer TL = (TableLayer)LayerTables.get(LayerName);
		if (TL == null)
		{
			System.out.println("TableLayer "+LayerName+" is null.");
		}
		return TL.getLineTypeName();
	}

	/** Given the name of a layer this method does a lookup and returns
	*	its (Acad) colour number.
	*	@see #getLayerColourNumber
	*	@param LayerName
	*	@return String Web safe colour equivilant of Acad Colour as 6 hex digits
	*	prefixed with a '&#035;'.
	*	@throws UndefinedTableException if the layer table is null.
	*/
	public String getLayerColour(String LayerName)
	{
		// I have to write in some compensation for CustomLayerStyles.
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");

		// Colour return type from LayerTable
		int ColorNum;

		TableLayer TL = (TableLayer)LayerTables.get(LayerName);
		if (TL == null)
		{
			System.out.println("TableLayer "+LayerName+" is null.");
		}
		ColorNum = TL.getColour();

		return DxfPreprocessor.getColour(ColorNum);
	}

	/** Given the name of a layer this method does a lookup and returns
	*	its (Acad) colour number.
	*	@see #getLayerColour
	*	@param LayerName
	*	@return integer colour number for this layer.
	*	@throws UndefinedTableException if the layer table is null.
	*/
	public int getLayerColourNumber(String LayerName)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");

		// Colour return type from LayerTable
		int ColorNum;

		TableLayer TL = (TableLayer)LayerTables.get(LayerName);
		ColorNum = TL.getColour();

		return ColorNum;
	}
	
	
	/** This method will return the fill for a layer. This is the colour of the layer, but
	*	with Dxf2Svg you are not restricted to that colour. You can specify a specific fill
	*	that is different from the stroke of an object. This is not possible in AutoCAD but
	*	it is possible through the config.d2s file. See Sally documentation for details under
	*	the topic of layerStyle. 
	*	<P>
	*	One condition where this would be helpful is if there were elements on a layer that 
	*	has a hydraulic line on it. any of the entities like lines or text could be rendered
	*	in black through the use of the config.d2s's layerStyle command and the fill of solids
	*	like the fill of the hydraulic line would appear in the correct colour for the layer say
	*	AutoCAD colour CY.
	*/
	public int getLayerFill(String layerName)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table: '"+layerName+"')");

		// Colour fill return type from LayerTable
		int ColorNum;

		TableLayer TL = (TableLayer)LayerTables.get(layerName);
		ColorNum = TL.getFill();

		return ColorNum;
	}





	/** This looks up a layers visibility based on whether it was frozen,
	*	locked (invisible or false) or neither (visible or true).
	*	@return boolean <code>true</code> if layer visible or <code>false</code> if invisible
	*	@throws UndefinedTableException if the layer table is null.
	*/
	public boolean getLayerIsVisible(String LayerName)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");

		// This method exists so SvgObjects can query their visibility by
		// layer settings if attributes are produced inline.
		TableLayer TL = (TableLayer)LayerTables.get(LayerName);
		return TL.getLayerVisible();
	}


	/** This method returns true if the drawing has the argument layer, false other wise.
	*	The look up is case insensitive and does not require the 'st' prefix.
	*/
	public boolean hasLayer(String layerName)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");

		
		if (LayerTables.containsKey(layerName))
		{
			return true;
		}

		return false;
	}








	// this will return a linetype depending on what layer you want
	/** Returns a line type table for a layer by name.
	*	@param lineTypeName of the line type you would like.
	*	@return TableLineType reference.
	*	@throws UndefinedTableException if the layer table
	*	or the line type table is null.
	*/
	public TableLineType getLayerLineTypeTable(String lineTypeName)
	{
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		// Find the linetype in the LineType tables
		TableLineType TLT = (TableLineType)LineTypeTables.get(lineTypeName);
		if (TLT == null)
			System.err.println("StyleSheetGenerator.getLayerLineTypeTable(): Warning "+
			"unable to find a line type table with the name: '"+lineTypeName+"'.");

		return TLT;
	}




	// this will return a linetype depending on what layer you want
	/** Returns a line type for a layer by name.
	*	@param Layer name of the layer you want to look up the line type for.
	*	@return TableLineType data converted to String.
	*	@throws UndefinedTableException if the layer table
	*	or the line type table is null.
	*/
	public String getLineTypeByLayer(String Layer)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		String LineTypeStr = new String();
		// find the linetype name by the layer it's on.
		TableLayer TL = (TableLayer)LayerTables.get(Layer);
		LineTypeStr = TL.getLineTypeName();
		// now find the linetype in the LineType tables
		TableLineType TLT = (TableLineType)LineTypeTables.get(LineTypeStr);

		return TLT.toString();
	}





	// this will return a linetype depending on what layer you want
	/** Returns a line type for a layer by name.
	*	@param Layer name of the layer you want to look up the line type for.
	*	@param scale Scale of the line's pattern.
	*	@return TableLineType data converted to String.
	*	@throws UndefinedTableException if the layer table
	*	or the line type table is null.
	*/
	public String getLineTypeByLayer(String Layer, double scale)
	{
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");
		if (LineTypeTables == null)
			throw new UndefinedTableException(
				"TableLineType (line type style table)");

		String LineTypeStr = new String();
		// find the linetype name by the layer it's on.
		TableLayer TL = (TableLayer)LayerTables.get(Layer);
		LineTypeStr = TL.getLineTypeName();
		// now find the linetype in the LineType linetypetables
		TableLineType TLT = (TableLineType)LineTypeTables.get(LineTypeStr);

		return TLT.toString(scale);
	}




	/**
	*	Places StyleSheets in Svg.<BR><BR>
	*	Called by SvgBuilder() constructor.
	*	@throws UndefinedTableException if the style table
	*	or the layer table is null.
	*/
	public void makeStyleSheet(StringBuffer svgHeader)
	{
		if (StyleTables == null)
			throw new UndefinedTableException(
				"TableStyles (text style table)");
		if (LayerTables == null)
			throw new UndefinedTableException(
				"TableLayer (layer name table)");


		if ((MAKE_CSS & Dxf2SvgConstants.CSS_ONLY) == Dxf2SvgConstants.CSS_ONLY)
		{
			if (DEBUG)
				System.out.println("@@@ CSS_ONLY @@@");
				makeExternalStyleSheet();
		}
		if ((MAKE_CSS & Dxf2SvgConstants.INLINE_STYLES) == Dxf2SvgConstants.INLINE_STYLES)
		{
			if (DEBUG)
				System.out.println("@@@ INLINE_STYLES @@@");
				// We don't do anything like that here; that is done in SvgElements.
		}
		if ((MAKE_CSS & Dxf2SvgConstants.DECLARED_CSS) == Dxf2SvgConstants.DECLARED_CSS)
		{
			if (DEBUG)
				System.out.println("@@@ DECLARED_CSS @@@");
			svgHeader.append("\n");
			svgHeader.append("<defs>\n");
			svgHeader.append("\t<style type=\"text/css\"><![CDATA[\n");
			makeStyleSheet(svgHeader, Dxf2SvgConstants.INDENT);
			svgHeader.append("\t]]></style>\n");
			svgHeader.append("</defs>\n");
		}
		if ((MAKE_CSS & Dxf2SvgConstants.EXTERNAL_CSS) == Dxf2SvgConstants.EXTERNAL_CSS)
		{
			if (DEBUG)
				System.out.println("@@@ EXTERNAL_CSS @@@");
			svgHeader.append("\n");
			makeExternalStyleSheet();
		}

		return;
	}





	// Called by the public makeStyleSheet() (above) and it is this method that
	// actually puts the style sheet together. The other method wraps the style
	// sheet as necessary, with the appropriate prefixes and suffixes.
	private void makeStyleSheet(StringBuffer buffer, int indent)
	{
		// the meat of the process goes here we need to extract all layer info
		// and text styles from the tables.

		Set LayerKeys = LayerTables.keySet();
		Iterator LayerKeyIterator = LayerKeys.iterator();
		TableLayer tmpTL;
		while (LayerKeyIterator.hasNext())
		{
			tmpTL = (TableLayer)LayerTables.get(LayerKeyIterator.next());
			
			// Make changes to the current layer to reflect the custom styles.
			if (MAKE_CSS >= Dxf2SvgConstants.CUSTOM_CSS)
				;//conformToCustomStyle(tmpTL);	// They are conformed in the setLayerInfo() method.
			if (indent == Dxf2SvgConstants.INDENT)
				buffer.append("\t\t" + tmpTL.toString());
			else
				buffer.append(tmpTL.toString());
		}


		////// Output the font URLs if required.
		if (DxfPreprocessor.includeUrl() == true)
		{
			// Now make sure that the styles that get output have unique names.
			// We cannot have two styles in a style sheet that have the same
			Vector NormalizedFontList = new Vector(); // New Vector of normalized values
			Set keys = StyleTables.keySet();
			Iterator keyIterator = keys.iterator();
			while (keyIterator.hasNext())
			{
				TableStyles tmpTStyle = (TableStyles)StyleTables.get(keyIterator.next());
				// Before we get to the normalization process we should
				// weed out a style that has no defined font. The HashMap
				// StyleTables can never be null because there is always one
				// default font defined called Standard. The problem is if
				// Standard has no font-face description it defaults to txt.shx
				// which is a AutoCAD-87 unifont and the font file name is txt
				// which is an illegal name and throws an exception.
				//
				// What we are going to do is check if the Standard font has
				// a font style attached. If it does you can export it
				// to the CSS and if it doesn't then let the TablesStyles
				// define it as courier.
				//if (tmpTStyle.getStyleName().equalsIgnoreCase("Standard"))
				//	if (tmpTStyle.getPrimaryFontFileName().equalsIgnoreCase("txt"))
				//		continue;

				String tmpFontName = tmpTStyle.getFontName();
				boolean FontAlreadyIncluded = false;
				// If you don't find the style in the list of font names
				// then add it. We do this to normalize or weed out duplicate
				// font name declarations in the CSS. The main problem is
				// that two or more styles may and often do refer to the same
				// font-face. This will result in multiple font-face entries
				// for the same font.
				for (int i = 0; i < NormalizedFontList.size(); i++)
				{
					if (((String)NormalizedFontList.get(i)).equals(tmpFontName))
					{
						FontAlreadyIncluded = true;
						break;
					}
				}
				if (FontAlreadyIncluded == false)
				{
					NormalizedFontList.add(tmpFontName);
					if (indent == Dxf2SvgConstants.INDENT)
						buffer.append("\t\t" + tmpTStyle.toUrlString());
					else
						buffer.append(tmpTStyle.toUrlString());
				}
			}
		}	// else we don't want the URLs.

		// Now output the actual styles. In this case we will need them
		// all.
		Set StyleKeys = StyleTables.keySet();
		Iterator StyleKeyIterator = StyleKeys.iterator();
		while (StyleKeyIterator.hasNext())
		{
			TableStyles tmpTStyle = (TableStyles)StyleTables.get(StyleKeyIterator.next());
			if (indent == Dxf2SvgConstants.INDENT)
				buffer.append("\t\t" + tmpTStyle.toString());
			else
				buffer.append(tmpTStyle.toString());
		}

		return;
	}	// end makeStyleSheet





	// Makes the external style sheet by using the internal style sheet as an example.
	public void makeExternalStyleSheet()
	{
		String path = DxfPreprocessor.getFileName();
		File PATH = new File(path);
		path = PATH.getParent();
		File OUT = new File(path, Dxf2SvgConstants.STYLE_SHEET_NAME);

		StringBuffer buffOut = new StringBuffer();
		makeStyleSheet(buffOut, Dxf2SvgConstants.NO_INDENT);
		try
		{
			BufferedWriter BWriter = new BufferedWriter(
				new FileWriter(OUT));

			BWriter.write(buffOut.toString());

			BWriter.write("/*** Automatically generated by "+Dxf2SvgConstants.APPLICATION+" ***/");
			BWriter.newLine();
			BWriter.write("/* EOF */");

			BWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("StyleSheetGenerator.makeExternalStyleSheet(): "+
				"An error occured writing to an external style sheet. "+e);
		}
	}

	
	

	/** This method allows objects to query if the <CODE>'onload='</CODE> declaration
	*	and <CODE>init()</CODE> functions are required. This method is currently for
	*	the convience of the SvgBuilder.
	*/
	public boolean isLangSwitchRequired()
	{
		if (DxfPreprocessor.useHTMLWrappers() == true)
		{
			if (hasLayer(TableLayer.getLanguageLayerName(Dxf2SvgConstants.ENGLISH)) && 
				hasLayer(TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH)))
			{
				return true;
			}
			// You could add more tests if some animation requires an init() function
			///////////// add it here. ///////////
		}
		return false;
	}


	/**	Thrown if a method attempts to access a null DXF table, Hashtable .
	*/
	protected class UndefinedTableException extends NullPointerException
	{
		protected UndefinedTableException(String tableType)
		{
			super();
			System.err.println
			(
				"StyleSheetGenerator error. "+
				"An attempt to reference a table of type: "+tableType+
				" was attempted but the table is null. This could "+
				"be because there was no data collected by the "+
				"TableProcessor, perhaps the reference has been set to "+
				" null or the table's creator has not initialize it yet."
			);
		}
	}
} // end StyleSheetGenerator