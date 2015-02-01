
/****************************************************************************
**
**	FileName:	SALConsts.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Contains all the pertinant constants for Parser.java and 
**				SALly language.
**
**	Date:		May 27, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.1 - May 27, 2003
**				1.01 - January 11, 2005 Added new Keyword and static String of 
**				"textStyle" and S_L_CUSTOM_TEXT_STYLE.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.sally;

/**	This class contains the static variables used primarily by {@link dxf2svg.sally.Parser}.
*	@version	0.1 - May 27, 2003
*	@author		Andrew Nisbet
*/
public class SALConsts
{
	private SALConsts(){ }
	// all states are prefixed with a 'S_'.
	public final static int S_INIT				= 0;
	public final static int S_EMPTY_LABEL		= 1;
	public final static int S_FULL_COLON		= 11;
	public final static int S_EQUALS			= 12;
	public final static int S_SEMI_COLON		= 13;
	public final static int S_COMMA				= 14;
	public final static int S_OPEN_CBRACE		= 15;
	public final static int S_CLOSE_CBRACE		= 16;
	public final static int S_A_KEYWORD			= 20;
	public final static int S_A_SET				= 21;
	public final static int S_A_ANIMATE			= 22;
	public final static int S_A_COLOUR			= 23;
	public final static int S_A_MOTION			= 24;
	public final static int S_A_TRANSFORM		= 25;
	public final static int S_A_TARGET			= 26;
	public final static int S_LABEL				= 30;
	public final static int S_L_FILES			= 31;
	public final static int S_L_FONTMAP			= 32;
	public final static int S_L_ANIMATION		= 33;
	public final static int S_L_LAYER_ORDER		= 34;
	public final static int S_L_VARIABLES		= 35;
	public final static int S_L_GENERAL			= 36;	// used to idenfify either files fontmap or vars lists to SALly
	public final static int S_L_PENS			= 37;
	public final static int S_L_CUSTOM_LAYERS	= 38;
	public final static int S_L_JAVASCRIPT		= 39;	// New section for JavaScript functions in <script> tags.
	public final static int S_GENERAL_STRING	= 40;
	public final static int S_RIGHT_HAND_VALUE	= 41;
	public final static int S_LEFT_HAND_VALUE	= 42;
	public final static int S_THIRD_ITEM_VALUE	= 43;
	public final static int S_LO_KEYWORD		= 50;
	public final static int S_LO_HEAD			= 51;
	public final static int S_LO_TAIL			= 52;
	public final static int S_ONE_ITEM_UNSORTED	= 60;	// acts like vector; not sorted alphabetically
	public final static int S_ONE_ITEM_ENTRY	= 61;
	public final static int S_TWO_ITEM_ENTRY	= 62;
	public final static int S_THREE_ITEM_ENTRY	= 63;
	public final static int S_L_MODIFY          = 64;	// Used for general modify state.
	//public final static int S_MOD_KEYWORD       = 65;
	public final static int S_MOD_ADD           = 66;
	public final static int S_MOD_DELETE        = 67;
	public final static int S_MOD_CHANGE        = 68;
	public final static int S_MOD_TARGET		= 69;
	public final static int S_MOD_TARGET_ID		= 70;	// Optional 'Modify' target ID value.
	public final static int S_MOD_TARGET_CLASS	= 71;	// Optional 'Modify' target class value.
	public final static int S_MOD_TARGET_ID_VALUE	= 72;	// ID of the element we are looking for.
	public final static int S_MOD_TARGET_CLASS_VALUE= 73;	// Name of the layer or class element can be found on.
	public final static int S_L_CUSTOM_TEXT_STYLE	= 74;	// New list type of textStyle.
	// Key punctuation
	public final static String SEMI_COLON		= ";";
	public final static String FULL_COLON		= ":";
	public final static String COMMA			= ",";
	public final static String OPEN_CBRACE		= "{";
	public final static String CLOSE_CBRACE		= "}";
	public final static String EQUALS			= "=";
	// Key word definitions
	public final static String L_FILES 			= "files";
	public final static String L_FONTMAP 		= "fontMap";
	public final static String L_ANIMATION		= "animation";
	public final static String L_LAYER_ORDER	= "layerOrder";
	public final static String L_VARIABLES		= "setup";
	public final static String L_PENS			= "penTable";
	public final static String L_CUSTOM_LAYERS	= "layerStyle";
	public final static String L_CUSTOM_TEXTSTYLE	= "textStyle";
	public final static String L_JAVASCRIPT		= "javaScript";
	public final static String K_TAIL			= "tail";
	public final static String K_HEAD			= "head";
	//public final static String L_MODIFY			= "modify";
	public final static String L_ADD			= "add";
	public final static String L_CHANGE			= "change";
	public final static String L_DELETE			= "delete";
	public final static String L_WITH_ID		= "withid";     // id of element.
	public final static String L_ON_LAYER		= "onlayer";	// class or layer of element.
	// Animation Key words
	public final static String A_SET			= "set";
	public final static String A_ANIMATE		= "animate";
	public final static String A_COLOUR			= "animateColour";
	public final static String A_MOTION			= "animateMotion";
	public final static String A_TRANSFORM		= "animateTransform";
	// number of strings in the name of a mod target.
	public final static int MOD_TARGET_LEN 		= 3;
	/** This flag means that the SVG Element does not match any criteria for additional attribs */
	public final static int NO_MATCH			= 0;
	/** This flag means that the element matches a specific an object's layer only. */
	public final static int MATCH_LAYER_ONLY	= 1;
	/** This flag indicates that the element matches a specific element type. */
	public final static int MATCH				= 2;
}	// end of SALConsts