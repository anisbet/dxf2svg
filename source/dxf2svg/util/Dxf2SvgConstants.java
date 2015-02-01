
/****************************************************************************
**
**	FileName:	Dxf2SvgConstants.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Contains all the static variables required for the conversion
**				process of DXF to SVG.
**
**	Date:		November 6, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 6, 2002
**				0.02 - November 15, 2002 Added getAdjustedFontScaleFactor() and
**				getFontIndex() methods and associated constants.
**				0.03 - March 29, 2005 Added PART_NUMBER_ID as a value to be
**				used as a id for a part number.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

/**
*	This class represents all of the constants that are required for the
*	Dxf2Svg conversion process. The class is not instantialable, all
*	variables are called via calls to static values. This class may not
*	be sub-classed.
*	@author		Andrew Nisbet
*	@version	0.02 - November 15, 2002
*/
public final class Dxf2SvgConstants
{
	private Dxf2SvgConstants()	// Stops the instantiation of this object.
	{	}

	/** Make a Cascading Style Sheet (CSS) called svg.css and exit application
	*	See command line arguments in Dxf2Svg.java documentation.
	*	These values are bit coded.
	*/
	public final static int CSS_ONLY		= 1;	// make svg.css only and exit application.
	/** Each element looks after its own attributes */
	public final static int INLINE_STYLES	= 2;	// place in each element.
	/** Declare styles at the beginning of the Svg (default). */
	public final static int DECLARED_CSS	= 4;	// place ent declare at start of svg
	/** Place attributes in external style sheet */
	public final static int EXTERNAL_CSS 	= 8;	// Make an external sheet

	// Any additional style sheet values are custom built-in style sheets.
	// These values are bit encoded and used in conjuction with the above bit codes 
	// describe what type of custom style sheet and where it should appear in relation
	// to the SVG file.
	/** Make a custom page by reading in the styling information from an external file.*/
	public final static int CUSTOM_CSS		= 16;	// Not implemented yet.
	/** Make an internal Spar style sheet that matches line weights and layer colours.*/
	public final static int SPAR_C130		= 32;
	
	// This value will have to grow as more functions are added but will cover the first
	// 4 custom styles. There are currently just two custom styles. CUSTOM_CSS and SPAR_C130
	/** This mask will, when anded with MAKE_CSS, will mask out the lower placement bits.*/
	public final static int CUSTOM_MASK		=240;
	/** This mask will mask off any custom setting bit set in MAKE_CSS.*/
	public final static int PLACEMENT_MASK	= 15;
	
	/** All external css are refered to by this name.*/
	public final static String STYLE_SHEET_NAME = "svg.css";



	/** Do not indent for external style sheet entries.*/
	public final static int NO_INDENT		= 0;
	/** Indent internal style sheet entries.*/
	public final static int INDENT			= 1;




	// the public static final identifier is not strictly required but
	// it makes documentation more readable and in some cases if it is
	// not included the values are left out of the documentation.
	/** This value needs to be updated with each release of Dxf2Svg. */
	public static final String VERSION = "0.1";
	/** The name of the application. */
	public static final String APPLICATION = "Dxf2Svg";



	// this values are used by HeaderProc.java and SvgObject.java
	public static final int UNDEFINED	= -1;



	// These values are for dimensioning the graphic to fill the
	// browser window upon opening.
	/** Auto detect current display settings. Warning: these settings
	*	may not be optimal for target's monitor.
	*/
	public static final int CURRENT_RESOLUTION 				= 0;
	/** VGA type resolution */
	public static final int SIXFORTY_X_FOUREIGHTY			= 1;
	/** Typical setting and IETM default resolution specification */
	public static final int EIGHT_HUNDRED_X_SIX_HUNDRED		= 2;
	/** 1024 x 768 resolution typically large monitors. */
	public static final int TENTWENTYFOUR_X_SEVENSIXTYEIGHT	= 3;
	/** Default if no resolution specified. */
	public static final	int BEST_GUESS = EIGHT_HUNDRED_X_SIX_HUNDRED;



	// Units of measure constants
	/** Inch measure conversion (default). */
	public static final int INCHES	= 0;
	/** Points measure conversion (72 per inch). */
	public static final int POINTS	= 1;


	/** Script placement settings */
	public final static int NO_SCRIPT	= 0; // we must be careful not to define NONE in other interfaces
						 // unless we know that they will not be used in conjuction with each other.
	/** Internal placement i.e. with in the &lt;script&gt; tags in &lt;head&gt;*/
	public final static int INTERNAL_SCRIPT	= 1; // remain consistant with naming (see below)
	/** External placement */
	public final static int EXTERNAL_SCRIPT	= 2;
	/** You can also include both internal and external JavaScript in the same file.*/
	public final static int INTERNAL_AND_EXTERNAL_SCRIPT = 3;
	
	
	/** These control where any supplied partial list should appear. If the list ordering
	*	is <B>HEAD</B> then the list contains all the layers that must be drawn before any
	*	other. <B>TAIL</B> means that all not on the list must be drawn first then the layer
	*	list in order are to be rendered last, and in the supplied order. The default is 
	*	<B>TAIL</B> for convience.<BR><BR>
	*/
	public final static int TAIL = 0;
	public final static int HEAD = 1;
	
	/** This is a list of attributes that typically can be targets for animation.
	*/
	public final static int ANIMATE_FILL 			= 1024;
	public final static int ANIMATE_STROKE 			= 1025;
	public final static int ANIMATE_STROKE_WIDTH 	= 1026;
	public final static int ANIMATE_VISIBILITY 		= 1027;
	public final static int ANIMATE_FONT_SIZE 		= 1028;
	
	/** This is a set of languages currently understood by the conversion process.
	*/
	//UNDEFINED: isn't a recognized language; already defined above.
	public final static int ENGLISH				= 1; // Is English.
	public final static int FRENCH				= 2; // Is French.
	public final static int MULTI_LINGUAL		= 3; // English & French or Bilingual
	
	/** This is the default id value for all groups of wires that are created dynamically
	*	during the conversion process.
	*/
	public final static String WIRE_RUN_ID_VALUE = "wire_run";
	
	/** This value is the id name of any text if it is a part number.
	*/
	public final static String PART_NUMBER_ID = "PartNo";
}