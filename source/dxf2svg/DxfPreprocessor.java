
/****************************************************************************
**
**	FileName:	DxfPreprocessor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Kicks off the conversion process and houses some static objects
**				that are required for all file conversions.
**
**	Date:		August 20, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - August 20, 2002
**				1.01 - October 6, 2004 Added Graphic link database handling functions.
**				1.02 - January 7, 2005 getCustomLayerLineWeight(String name), 
**				getCustomLayerColour(String name) and getCustomLayerFill(String name) 
**				have been 
**				made redundant by changes to StyleSheetGenerator and have been removed
**				as this is still the initial release of dxf2svg's API.
**				1.03 - January 11, 2005 Added getCustomTextStyle() and readCustomTextStyle
**				for SSG to extract config.d2s textStyles. 
**				1.04 - February 8, 2005 Added setUpdateDatabaseOnly() method and 
**				internals to make that work in method: activateProcessor().
**				1.05 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04. Also changed ndidRegex to
**				accept a 3digit termination in stead of '00[0-5]'.
**				1.06 - February 23, 2005 added gc() to see if we can stop Out of memory
**				errors.
**				1.07 - February 25, 2005 Fixed a bug where this class wouldn't find
**				a French title if it Started with a capital accented French Character.
**				Changed the figureNumSheetTitleRegex to include É, È, Á, À...
**				1.08 - March 11, 2005 Fixed NDID regex to include '$' "\\p{Digit}{3}$";
**				If you don't it successfully matches legit sentences that start with an NDID.
**				1.09 - August 28, 2005 Added new switch to swap .svgz links for .html wrappers.
**				1.10 - October 4, 2005 Added progress reporting "file " + i + " of..."
**
**	TODO:
**
*****************************************************************************/

package dxf2svg;

// Don't forget to include this class when it is time to package product.
import nisbet.andrew.util.*; // For DirLister and FileTester.
import java.io.*;
import java.net.URI;		// for validating and normalizing URI if nec.
import java.util.*;			// for storing default spar layer styles if required.
import java.util.regex.*;	// regular expression matching in convertToSvgCss()
import dxf2svg.util.*;		// Utility directory stuff like svgUtil.
import dxf2svg.sally.*;
import dxf2svg.animation.*;	// Animation objects from Sally

/**	The DxfPreprocessor instantiates and controls the multithreaded
*	conversion environment of the DxfConverter. In addition it houses
*	the user defined switches and the methods to alter their values.
*	It also creates a master list of files to process through testing
*	and collecting methods.
*
*	@version 	1.10 - October 4, 2005
*	@author		Andrew Nisbet
*/
public final class DxfPreprocessor
{
	// If the PATH variable is a directory this array is for storing the
	// files we will process
	private String[] Dxfs;
	private Vector fileList;
	// We will also need a DirLister Object.
	private DirLister DL;
	// And we need a FileFilter object to screen for DXF files.
	private DXFFilter DFilter;
	// And we will need a FileTester object.
	private FileTester FTester;
	/** The number of pens in a complete set of Pens.*/
	public final static int NUM_OF_PENS = 256;
	// The array of standard pens.
	private static Pen[] pens;
	// Stores the custom layer styles sheet entries from the config.d2s file or defined in this object.
	private static Vector vCustomLayerStyles;
	// Stores the custom text styles from the config.d2s file.
	private static Hashtable hCustomTextStyles;
	// List of names of layers and their desired ordering.
	private static Vector orderedLayerList;
	// Location of layer order preference; HEAD or TAIL. If HEAD then the supplied list
	// will appear in order at the front of the list. The reverse for TAIL; the list
	// will appear in order after all of the other layers in the drawing are output.
	private static int listPlacement = Dxf2SvgConstants.TAIL;
	// List of fonts and their mappings
	private static Hashtable hFontMap = new Hashtable();
	// Animation engine
	private static AnimationEngine ae;
	// This value indicates that we want all colours to be displayed as the coerceColour
	private static boolean isCoercedColour = false;
	private static int coerciveColour = 1;		// preset arbitrary colour (red).
	private static Hashtable collaboratorTable = null;	// List of animation targets that require
		// chaining of collaborative line segments. See AnimationEngine.java for more details
	private static Hashtable gangTable = null;	// List of layers that have a true or false
		// value that indicates whether the polyline <path>s need to be treated as ganged wires.
	private static Vector javaScriptStack = null; // JavaScript functions for header.
	private static boolean thereIsJavaScript = false; // Switch for javaScript presents.
	private static String dbPath;					// path to serialize database of converted figs.
	private static String dbXMLPath;				// path and name of XML file;
	private static final String XMLDatabaseName = "boardno-control.xml";


	// Global switches
	private static boolean	VERBOSE = false;
	private static boolean	WIPE_OUT_TEXT = false;
	private static boolean	DEBUG = false;
	private static int		MAKE_CSS = Dxf2SvgConstants.DECLARED_CSS;	// Possible values are in StyleConst.java
	private static String	DXF_FILE = "";
	private static int		PRECISION = 2;				// number of decimal places in calculations
	private static boolean	DTD_ALT = false;			// include valid DTD description and path.
	private static int		INCLUDE_JAVASCRIPT = Dxf2SvgConstants.NO_SCRIPT;	// internal external ref to JavaScript
	private static String 	JS_SRC_PATH = "";			// path to the javascript file
	private static File 	IN;							// file to process.
	private static boolean	RECURSIVE = false;			// Dxf conversion in subdirectories?
	private static String	WORKING_DIR;				// conversion directory.
	private static int		RESOLUTION = Dxf2SvgConstants.BEST_GUESS;	// preferred resolution
	private static String	FONT_DIRECTORY = null;		// system fonts directory.
	private static boolean	INCLUDE_FONT_URL = false;	// URL CSS inclusion.
	private static boolean	IS_ZIPPED = false;			// Zip the Svg file with GZIP.
	private static boolean	HTML_WRAPPERS = true;		// Wrap SVG files in HTML for printing and
														// language context switching.
	private static boolean	SYNC_DATABASE = false;		// Do we keep records of what we convert?
	private static boolean	USES_LINKS = false;			// does the dxf contain hyperlinks?
	private static boolean  IS_PARTS_LIST_FIGURE = false;// was the -MY switch used.
	private static boolean	GENERATE_NOTES = false;		// Search for and apply notes as per SvgNotes contract.
	private static boolean	UPDATE_DB_ONLY = false;		// Don't run the Svg conversion update db only.
	
	// here is the colour lookup table (based on Acad's 255 colour palette)
	// note that 0 indicates that the layer is off.
	private final static String[] colourTable = { 	"#000000",		// [0] we'll set this for simplicity
		// since some objects, notably attribs set their colour to zero
		// which sets their fill to "" and for the purposes of conversion KISS...
		"#FF0000", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF",		// [1],[2],[3],[4],[5],
		"#FF00FF", "#000000", "#808080", "#DCDCDC", "#FF0000",		// [6],[7*],[8],[9],[10],
		"#FF7F7F", "#DD0000", "#DD6E6E", "#B80000", "#B85C5C",		// [11],[12],[13],[14],[15],
		"#950000", "#954A4A", "#720000", "#723939", "#FF3F00",		// [16],[17],[18],[19],[20],
		"#FF9F7F", "#DD3700", "#DD8A6E", "#B82E00", "#B8735C",		// [21],[22],[23],[24],[25],
		"#952500", "#955D4A", "#721C00", "#724739", "#FF7F00",		// [6],[7],[8],[9],[0],
		"#FFBF7F", "#DD6E00", "#DDA56E", "#B85C00", "#B88A5C",		// [31],[32],[33],[34],[35],
		"#954A00", "#95704A", "#723900", "#725639", "#FFBF00",		// [6],[7],[8],[9],[0],
		"#FFDF7F", "#DDA500", "#DDC16E", "#B88A00", "#B8A15C",		// [41],[42],[43],[44],[45],
		"#957000", "#95834A", "#725600", "#726439", "#FFFF00",		// [6],[7],[8],[9],[0],
		"#FFFF7F", "#DDDD00", "#DDDD6E", "#B8B800", "#B8B85C",		// [51],[52],[53],[54],[55],
		"#959500", "#95954A", "#727200", "#727239", "#BFFF00",		// [6],[7],[8],[9],[0],
		"#DFFF7F", "#A5DD00", "#C1DD6E", "#8AB800", "#A1B85C",		// [61],[62],[63],[64],[65],
		"#709500", "#83954A", "#567200", "#647239", "#7FFF00",		// [6],[7],[8],[9],[0],
		"#BFFF7F", "#6EDD00", "#A5DD6E", "#5CB800", "#8AB85C",		// [71],[72],[73],[74],[75],
		"#4A9500", "#70954A", "#397200", "#567239", "#3FFF00",		// [6],[7],[8],[9],[0],
		"#9FFF7F", "#37DD00", "#8ADD6E", "#2EB800", "#73B85C",		// [81],[82],[83],[84],[85],
		"#259500", "#5D954A", "#1C7200", "#477239", "#00FF00",		// [6],[7],[8],[9],[0],
		"#7FFF7F", "#00DD00", "#6EDD6E", "#00B800", "#5CB85C",		// [91],[92],[93],[94],[95],
		"#009500", "#4A954A", "#007200", "#397239", "#00FF3F",		// [6],[7],[8],[9],[0],
		"#7FFF9F", "#00DD37", "#6EDD8A", "#00B82E", "#5CB873",		// [101],[102],[103],[104],[105],
		"#009525", "#4A955D", "#00721C", "#397247", "#00FF7F",		// [6],[7],[8],[9],[0],
		"#7FFFBF", "#00DD6E", "#6EDDA5", "#00B85C", "#5CB88A",		// [111],[112],[113],[114],[115],
		"#00954A", "#4A9570", "#007239", "#397256", "#00FFBF",		// [6],[7],[8],[9],[0],
		"#7FFFDF", "#00DDA5", "#6EDDC1", "#00B88A", "#5CB8A1",		// [121],[122],[123],[124],[125],
		"#009570", "#4A9583", "#007256", "#397264", "#00FFFF",		// [6],[7],[8],[9],[0],
		"#7FFFFF", "#00DDDD", "#6EDDDD", "#00B8B8", "#5CB8B8",		// [131],[132],[133],[134],[135],
		"#009595", "#4A9595", "#007272", "#397272", "#00BFFF",		// [6],[7],[8],[9],[0],
		"#7FDFFF", "#00A5DD", "#6EC1DD", "#008AB8", "#5CA1B8",		// [141],[142],[143],[144],[145],
		"#007095", "#4A8395", "#005672", "#396472", "#007FFF",		// [6],[7],[8],[9],[0],
		"#7FBFFF", "#006EDD", "#6EA5DD", "#005CB8", "#5C8AB8",		// [151],[152],[153],[154],[155],
		"#004A95", "#4A7095", "#003972", "#395672", "#003FFF",		// [6],[7],[8],[9],[0],
		"#7F9FFF", "#0037DD", "#6E8ADD", "#002EB8", "#5C72B8",		// [161],[162],[163],[164],[165],
		"#002595", "#4A5D95", "#001C72", "#394772", "#0000FF",		// [6],[7],[8],[9],[0],
		"#7F7FFF", "#0000DD", "#6E6EDD", "#0000B8", "#5C5CB8",		// [171],[172],[173],[174],[175],
		"#000095", "#4A4A95", "#000072", "#393972", "#3F00FF",		// [6],[7],[8],[9],[0],
		"#9F7FFF", "#3700DD", "#8A6EDD", "#2E00B8", "#7C5CB8",		// [181],[182],[183],[184],[185],
		"#250095", "#5D4A95", "#1C0072", "#473972", "#7F00FF",		// [6],[7],[8],[9],[0],
		"#BF7FFF", "#6E00DD", "#A56EDD", "#5C00B8", "#8A5CB8",		// [191],[192],[193],[194],[195],
		"#4A0095", "#704A95", "#390072", "#563972", "#BF00FF",		// [6],[7],[8],[9],[0],
		"#DF7FFF", "#A500DD", "#C16EDD", "#8A00B8", "#A15CB8",		// [201],[202],[203],[204],[205],
		"#700095", "#834A95", "#560072", "#643972", "#FF00FF",		// [6],[7],[8],[9],[0],
		"#FF7FFF", "#DD00DD", "#DD6EDD", "#B800B8", "#B85CB8",		// [211],[212],[213],[214],[215],
		"#950095", "#954A95", "#720072", "#723972", "#FF00BF",		// [6],[7],[8],[9],[0],
		"#FF7FDF", "#DD00A5", "#DD6EC1", "#B8008A", "#B85CA1",		// [221],[222],[223],[224],[225],
		"#950070", "#954A83", "#720056", "#723964", "#FF007F",		// [6],[7],[8],[9],[0],
		"#FF7FBF", "#DD006E", "#DD6EA5", "#B8005C", "#B85C8A",		// [231],[232],[233],[234],[235],
		"#95004A", "#954A70", "#720039", "#723956", "#FF003F",		// [6],[7],[8],[9],[0],
		"#FF7F9F", "#DD0037", "#DD6E8A", "#B8002E", "#B85C73",		// [241],[242],[243],[244],[245],
		"#950025", "#954A5D", "#72001C", "#723947", "#545454",		// [6],[7],[8],[9],[0],
		"#767676", "#989898", "#BABABA", "#DCDCDC", "#FFFFFF",		// [251],[252],[253],[254],[255],
	};	// end of colourTable[] 0:byblock 256:bylayer <0:layer off
	
	// This is where we will get files, layerOrders, fontMaps etc. if there is a config file.
	private SvgAnimationLanguage sal;

	// This is a database of all the FigureSheetDatabases. It includes all the namespaces or books
	// that have been converted or are in the current process of being converted.
	private static LibraryCatalog library;
	// This object manages the searching synchronizing between sheets and mediates with SvgBuilder
	// to output the required notes to the correct files.
	private static SvgNoteManager svgNoteManager;
	private static Attribute partsListOnClickAttrib;	// attribute of onclick and its value.
	// This is a set of switches that are indexed to match a custom pen. If the arrays index
	// number is false it means that the pen of the same index is a default colour. If true
	// it means that the pen of the same number has a custom colour.
	// If this object is uninitiated, the value of any index is false by default.
	private static boolean[] isCustomPenColour; 
	private static boolean suppressDefaultJavaScript = false;
	private static Dxf2SvgLogger eventLogger;	// Event logger event object.
	private static StringBuffer includeFileData;
	private static boolean COERCE_COLOUR_BY_LAYER = true;
	// This is the database created by the application MakeExternalLinksDatabase that created
	// the 'GraphicLinkDatabase.ser' database. This object reads this database. Hint
	// the database is just a serialized java.util.Hashtable for simplicity speed and 
	// versioning consistancy.
	private static GraphicLinksDatabaseReader gldbReader;
	private static boolean IS_EXTERNAL_GRAPHIC_LINKS;
	private static boolean IS_SWAP_SVG_FOR_HTML;
	
	
	
	
	//************ Constructors *********/
	/** Default constructor. */
	public DxfPreprocessor()
	{
		init();
	}
	
	/** Constructor that takes an {@link SvgAnimationLanguage} object as an argument. */
	public DxfPreprocessor(SvgAnimationLanguage sal)
	{
		this.sal = sal;
		init();
		if (readFontMap() && DEBUG)
			System.out.println("There was a fontmap");
		// Get layering information from the config file.
		if (setLayerOrder() && DEBUG)
			System.out.println("There was a layerorder");
		// read the pens object
		if (readCustomPens() && DEBUG)
			System.out.println("There were custom pens");
		// read in the custom layer information.
		if (readCustomLayer() && DEBUG)
			System.out.println("There were custom layers");
		// This will pick up any JavaScript functions that are requested.
		if (readJavaScriptObjects() && DEBUG)
			System.out.println("There are JavaScript objects");
		// And now for animation objects
		if (readAnimationObjects() && DEBUG)
			System.out.println("There are animation objects");
		if (readCustomTextStyles() && DEBUG)
			System.out.println("There are Custom text style objects");
	}


	////////////////////////////////////////////////
	//				Methods
	////////////////////////////////////////////////
	// This is just a funnel to catch two different constructors mutual
	// requirements.
	private void init()
	{
		// The main job of the Preprocessor is to determine which files
		// to process and start the DxfConverter.
		FTester			= new FileTester();
		createPens();
	}
	
	
	
	
	
	/** Retrieves JavaScript code from the config file and places it within the &lt;script&gt; tags
	*	of each of the converted Svg files. JavaScript objects can come in 3 different forms:
	*<OL>
	*	<LI> Automatic language switching scripts.
	*	<LI> Scripts specified by the user with the javaScript keyword.
	*	<LI> Scripts created through the animation API (see Sally documentation).
	*</OL>
	*/
	public boolean readJavaScriptObjects()
	{
		if (sal == null)
		{
			thereIsJavaScript = false;
			return false;
		}
		
		String[] keys = new String[0];
		keys = sal.getKeys(SALConsts.L_JAVASCRIPT);
		
		if (keys == null)
		{
			thereIsJavaScript = false;
			return false;			
		}
		
		if (keys.length < 1)
		{
			thereIsJavaScript = false;
			return false;
		}
		
		// Create a new object 
		if (javaScriptStack == null)
		{
			javaScriptStack = new Vector();
		}
		
		for (int i = 0; i < keys.length; i++)
		{
			// place on Script Vector.
			javaScriptStack.add(keys[i]);
		}
		
		thereIsJavaScript = true;

		return true;
	}
	
	
	
	
	
	/** Returns a stack of JavaScript code for incorporation into Svg files.
	*	@return null if there are no functions stored. You may want to check
	*	for the presents of JavaScript with the {@link #hasJavaScript} method.
	*/
	public static Vector getJavaScript()
	{	return javaScriptStack;	}
	
	
	
	
	
	/** Reports on whether the config file contained javaScript to be placed
	*	in the Svg file during conversion.
	*/
	public static boolean hasJavaScript()
	{	return thereIsJavaScript;	}
		
	
	
	
	
	/** This method allows clients to register whether a target for animation requires
	*	collaborative processing.
	*	@param name of the animation target (usually a layer name). Null or empty name
	*	strings are not registered. Sending an entry that already exists will over-write
	*	the previous value.
	*	@param collaborates finds other line segments share start or end points and assumes
	*	that they are all part of one entity. True - searches for collaborator line segments
	*	false - apply animation to entire layer as one group.
	*/
	public static void registerCollaboratorTarget(String name, String collaborates)
	{
		if (name == null || collaborates == null)
			return;
		if (name.equals(""))
			return;
			
		if (collaboratorTable == null)
			collaboratorTable = new Hashtable();
		
		// If collaborates is anything but "true" ignoring case, the value will be false.
		collaboratorTable.put(name, new Boolean(collaborates));
	}
	
	/** Returns true if the target cited by argument <EM>name</EM> is registered as 
	*	requiring searches for collaborative line segments, false if the
	*	argument name is Null, an empty String, has not been registered
	*	or has been expressly set to false.
	*/
	public static boolean isCollaboratorTarget(String name)
	{
		if (name == null)
			return false;
		if (name.equals(""))
			return false;
		
		if (collaboratorTable == null)
			return false;
			
		Boolean value = (Boolean)collaboratorTable.get(name);
		if (value == null)
			return false;
		
		return value.booleanValue();
	}
	
	
	/** This method allows clients to register this layer as requiring gang wire treatment
	*	or not.
	*	@param name of the animation target (usually a layer name). Null or empty name
	*	strings are not registered. Sending an entry that already exists will over-write
	*	the previous value. So 'layerName' = true will over write a value already stored
	*	that says 'layerName' = false.
	*	@param isGang finds other line segments share start or end points and assumes
	*	that they are all part of one entity. True - searches for ganged polyline segments
	*	false - do nothing with &lt;path&gt; elements.
	*/
	public static void registerGangTarget(String name, String isGang)
	{
		if (name == null || isGang == null)
			return;
		if (name.equals(""))
			return;
			
		if (gangTable == null)
			gangTable = new Hashtable();
		
		// If isGang is anything but "true" ignoring case, the value will be false.
		gangTable.put(name, new Boolean(isGang));
	}
	
	/** Returns true if the target cited by argument <EM>name</EM> is registered as 
	*	requiring searches for polyline segments (&lt;path&gt;) which will be interpreted
	*	as a wire gang. If this is the case any animation will be applied to this object
	*	individually of any other gangs.
	*	<P>
	*	The function will return false if the
	*	argument name is Null, an empty String, has not been registered
	*	or has been expressly set to false.
	*/
	public static boolean isGangingTarget(String name)
	{
		if (name == null || name.equals(""))
		{
			return false;
		}
		
		if (gangTable == null)
			return false;
			
		Boolean value = (Boolean)gangTable.get(name);
		if (value == null)
			return false;
		
		return value.booleanValue();		
	}
	
	
	/** This method is used to retrieve a Pen reference for inclusion into a CustomLayerStyle
	*	@throws ArrayIndexOutOfBoundsException if the argument pen number is out of range.
	*	@throws NullPointerException if the Pens array has not been initialized.
	*/
	public static Pen getPen(int penNum)
	{
		if (penNum < 0 || penNum > NUM_OF_PENS)
			throw new ArrayIndexOutOfBoundsException(
				"DxfPreprocessor.getPen(): pen number out of range.");
		try
		{	
			return pens[penNum];
		} catch (NullPointerException e){
			throw new NullPointerException(
				"DxfPreprocessor: getPen(), the Pens[] has not be initialized yet."
			);
		}
	}
	
	
	// This method is required so we can setup the pens before even the constructor
	// runs. This is because SALly has to be able to get a pen definition before 
	// this object is constructed.
	/** This method sets up all of the pens to their default condition. NOTE: this
	*	will also reset any custom pen settings if they have already been set.
	*/
	private static void createPens()
	{
		if (pens != null && DEBUG)
			System.out.println("DxfPreprocessor warning: Overwriting Pen Table.");
		pens = new Pen[NUM_OF_PENS];
		// This is a collection of boolean values that are false if the pen of the 
		// same index is a default colour and true if it is a custom colour.
		isCustomPenColour = new boolean[NUM_OF_PENS];
		pens[0] = null;
		// make a complete set of standard pens.
		// There is no pen '0' so start at one.
		for (int i = 1; i < NUM_OF_PENS; i++)
		{
			pens[i] = new Pen(i);
		}	// end for
	}
	
	/** This method will read the necessary animation objects.
	*/
	private boolean readAnimationObjects()
	{
		if (sal == null)
			return false;
		
		ae = AnimationEngine.getInstance(sal);
		return true;
	}
	
	
	
	/** Allows any conversion context to retrieve an animation engine reference.
	*/
	public static AnimationEngine getAnimationEngine()
	{	return ae;	}
	
	/** Loads the font map data from Sally.
	*	@return true if the table loaded successfully, false otherwise.
	*/
	private boolean readFontMap()	
	{
		String fontMap = SALConsts.L_FONTMAP;
		FontMapElement fme = null;
		String[] keys = new String[0];
		keys = sal.getKeys(fontMap);
		
		if (keys == null || keys.length < 1)
			return false;
			
		
			
		String name;
		for (int i = 0; i < keys.length; i++)
		{
			// lookup the FontMapElement
			fme = (FontMapElement)sal.getValue
			(
				SvgAnimationLanguage.HEAP,
				fontMap,
				keys[i]
			);
			
			// put the file name to lowercase to match the search.
			name = keys[i].toLowerCase();
			// place on font map.
			hFontMap.put(name, fme);
		}
		
		return true;
	}	// end readFontMap()
	
	
	/** Collects and sets any custom layers from Svg Animation Language.
	*/
	private boolean readCustomLayer()
	{
		String[] keys = new String[0];
		keys 		  = sal.getKeys(SALConsts.L_CUSTOM_LAYERS);
		
		// return false if we didn't find any keys because we need to set the CSS mode
		if (keys == null || keys.length < 1)
			return false;
		
		setCssMode(Dxf2SvgConstants.CUSTOM_CSS);
		CustomLayerStyle cls 	= null;
		vCustomLayerStyles		= new Vector();
		for (int i = 0; i < keys.length; i++)
		{
			// lookup the CustomLayerStyle
			cls = (CustomLayerStyle)sal.getValue
			(
				SvgAnimationLanguage.HEAP,
				SALConsts.L_CUSTOM_LAYERS,
				keys[i]
			);
			// put the file name to lowercase to match the search.
			vCustomLayerStyles.add(cls);
		}
		
		//System.out.println("Custom Layers: "+vCustomLayerStyles.toString());
		
		return true;
	}
	


	/** Collects and sets any custom text styles from Svg Animation Language.
	*/
	private boolean readCustomTextStyles()
	{
		String[] keys = new String[0];
		keys 		  = sal.getKeys(SALConsts.L_CUSTOM_TEXTSTYLE);
		
		// return false if we didn't find any keys because we need to set the CSS mode
		if (keys == null || keys.length < 1)
			return false;
		
		setCssMode(Dxf2SvgConstants.CUSTOM_CSS);
		hCustomTextStyles		= new Hashtable();
		for (int i = 0; i < keys.length; i++)
		{
			// CustomLayerStyles are three item lists that are created by the parser
			// and stored as objects in the symbol table. CustomTextStyle is just a set
			// of Strings from which we will create new CustomTextStyle objects from 
			// here.
			//
			// The CustomTextStyles could not be created here or in the parser because
			// they're derived from TableStyle objects which need a conversion context
			// (DxfConverter reference) for their constructor.
			String symb = (String)sal.getValue
			(
				SvgAnimationLanguage.HEAP,
				SALConsts.L_CUSTOM_TEXTSTYLE,
				keys[i]
			);
			
			hCustomTextStyles.put(keys[i], symb);
		}
		
		//System.out.println("Custom text styles: "+hCustomTextStyles.toString());
		
		return true;
	}
	
	
	/** Collects and sets any custom layers from Svg Animation Language.
	*	In other words, this method takes the symbol table of custom pens
	*	from SALly and modifies the pen table to include the new data.
	*/
	private boolean readCustomPens()
	{
		String customPen = SALConsts.L_PENS;
		Pen pen = null;
		String[] keys = new String[0];
		keys = sal.getKeys(customPen);
		if (keys == null || keys.length < 1)
		{
			// This is done in the init() function.
			// Commented out January 5, 2005.
			//createPens();	// no pens in config so let's make some.
			return false;
		}
		
		int index = 0;
		for (int i = 0; i < keys.length; i++)
		{
			// lookup the CustomLayerStyle
			pen = (Pen)sal.getValue
			(
				SvgAnimationLanguage.HEAP,
				customPen,
				keys[i]
			);
			
			// Now we have the pen check which number it is.
			// To do that we convert the key that the pen was
			// stored with into an integer and use that as the
			// the index to the Pens[].
			index = Integer.parseInt(keys[i]);
			
			// Now change the Pens[] value
			pens[index] = pen;
			// The effect of this is even if a custom pen is created which uses
			// the default colour of this pen, it will still register as a custom
			// colour and thus will not be coerced to a user defined default colour
			// by the user swith '-c'.
			isCustomPenColour[index] = true;
		}

		return true;
	}



	/** Performs a lookup for font names within the font map.
	*	@return name Name of the replacement font name (if any).
	*/
	public static FontMapElement lookupFileNameInFontMap(String fontFileName)
	{
		// This method changes the argument name if it finds a match in
		// the fontmap otherwize it leaves the argument unchanged and 
		// returns a null object.
		String name = fontFileName.toLowerCase();
		FontMapElement FMETmp = (FontMapElement)hFontMap.get(name);
		//System.out.println("Font map contains"+hFontMap);

		return FMETmp;
	}	// end lookupFontName()



	/** Returns a list of layers that are to be put in order. The layers on this
	*	list may not even exist in a perticular drawing; if that is the case the
	*	entry is ignored.
	*	@see #setLayerOrder
	*/
	public static Vector getLayerOrder()
	{	return orderedLayerList;	}

	/**	The method returns an integer representation of where the list of prefered layer
	*	ordering will appear. If {@link Dxf2SvgConstants#HEAD} any layers in the drawing
	*	that match the list will be output first and therefore drawn first. If {@link Dxf2SvgConstants#TAIL}
	*	the list will appear at the end of the SVG file and therefore will be drawn last.
	*	The default is TAIL.
	*/
	public static int getLayerOrderPlacement()
	{	return listPlacement; 	}




	/**	This method places a set of layers in a particular order as read from the config file.
	*	This may be necessary if you require some layers to be drawn last or first depending.
	*	A good example of this is an anotation arrow on an illustration. Often the arrow will
	*	have a white space around it to make it easier to follow to the item being called out.
	*	The white 'mask' is drawn first and then the line of the arrow. To do this we must make
	*	sure that the mask layer is drawn first followed by the line layer.
	*
	*	<P>The list ordering is controlled by two integers, either {@link Dxf2SvgConstants#TAIL} or
	*	{@link Dxf2SvgConstants#HEAD}. <B>HEAD</B> indicates that the listed layers must appear first,
	*	in order. <B>TAIL</B> indicates the listed layers must appear drawn last.The default
	*	is <B>TAIL</B> for convience and invalid integers are interpreted to be <B>TAIL</B>.
	*
	*	<P>The layers are output according to list order until the list is exhausted and then
	*	the layers are placed in the order that the DXF file specifies.
	*
	*	<P>A request to order layers by tail will win out over any request for head layer
	*	ordering - no matter where the list is placed in the config file.
	*/
	public boolean setLayerOrder()
	{
		if (sal == null)
		{
			orderedLayerList = null;	// we do this incase someone has previously
				// set a layer list but then reset it to have a zero length list.
				// This will reduce error checking in getLayerList().
			return false;
		}
		String[] keys = new String[0];		// keys from the symbol table.
		// Now let's see if SAL has some layering information for us.
		if (sal.hasContent(sal.TAIL_HEAP))
		{
			listPlacement = Dxf2SvgConstants.TAIL;
			keys = sal.getKeys(sal.TAIL_HEAP);
		}
		else if (sal.hasContent(sal.HEAD_HEAP))
		{
			listPlacement = Dxf2SvgConstants.HEAD;
			keys = sal.getKeys(sal.HEAD_HEAP);
		} 
		else
		{
			if (VERBOSE)
				System.out.println("DxfPreprocessor message: not layer order information.");
			return false;
		}
		if (keys == null || keys.length < 1)
			return false;
			
		orderedLayerList 	= new Vector();		// Stores the actual names in order.
		for (int i = 0; i < keys.length; i++)
		{
			orderedLayerList.add(keys[i]);
		}	// end for
		
		// Display for testing.
		//for (int i = 0; i < orderedLayerList.size(); i++)
		//	System.out.println("orderedLayerList["+i+"] = "+(String)orderedLayerList.get(i));
		return true;
	}




	/**	This method formats the argument string to match valid SVG CSS string formatting.
	*	Valid CSS strings cannot contain underscores and convensions with Dxf2Svg state
	*	that all CSS names be in lowercase. Other could be added to accomodate future
	*	developments or restrictions for CSS.
	*	@param src String remains untouched.
	*	@return -1 if src is null, 0 if conversion successfull and 1 if dest is null.
	*/
	public static String convertToSvgCss(String src)
	{
		return convertToSvgCss(src, false);
	}

	/**	This method formats the argument string to match valid SVG CSS string formatting.
	*	Valid CSS strings cannot contain underscores and convensions with Dxf2Svg state
	*	that all CSS names be in lowercase. Other could be added to accomodate future
	*	developments or restrictions for CSS.
	*	@param src String remains untouched.
	*	@param isEntity If this is an entity then any initial digit will be preceeded by
	*	an under-score. If this is not an entity it is a CSS property or declaration and will
	*	be preceeded by a 'st'.
	*	@return -1 if src is null, 0 if conversion successfull and 1 if dest is null.
	*/	
	public static String convertToSvgCss(String src, boolean isEntity)
	{
		if (src == null)
		{
			System.err.println("DxfPreprocessor.convertToSvgCss(): Error src string is null.");
			return null;
		}
		// This is simple but there are a few classes that require consistancy
		// in naming conventions.
		String tmp = src.toLowerCase();
		// if nothing else happens return the string in lower case.
		String dest = tmp;

		Pattern p = Pattern.compile("\\p{Punct}|\\p{Blank}");
		Matcher m = p.matcher(tmp);
		if (m.find())
		{
			dest = m.replaceAll("-");
			dest = trimInitialHyphen(dest);
		}
		
		if (isEntity)
		{
			// Entities and classes cannot have a number as the initial character in their 
			// name. Here we will replace the initial character with an under-score.
			char c = dest.charAt(0);
			if (c >= '0' && c <= '9')
			{
				dest = "_" + dest;
			}
		}

		return dest;
	}


	// This looks for an initial hyphen which is illegal but may appear if the
	// arg string started with spaces or other illegal characters; those would
	// have been converted into hyphens. We then go through and trim them off
	// one-by-one by making recursive calls. Used by convertToSvgCss() exclusively.
	private static String trimInitialHyphen(String in)
	{
		String str;
		if (in.startsWith("-"))
		{
			str = in.substring(1);
			str = trimInitialHyphen(str);
		}
		else
		{
			str = in;
		}

		return str;
	}






	/** Given an Acad colour number, this method looks up
	*	its Web safe colour equivilant. <BR><BR>
	*
	*	If the colour number is invalid StyleSheetGenerator issues a warning to
	*	stderr and substitutes 'fushia' (No. 220) instead.<BR><BR>
	*
	*	Another feature of this method is that if AutoCAD has made the object
	*	invisible (either frozen or locked) the colour is still output correctly
	*	but the object's stroke visibility attribute should be set to hidden
	*	within the object if necessary.
	*
	*	Also note that all colours, with the exception of white (255) can be 
	*	coerced to be represented in any ohter colour with the command switch
	*	'-c'. See {@link #setCoerciveColour} and '-h' for more information.
	*
	*	@param c Colour number as integer.
	*	@return String Web safe colour equivilant of Acad Colour as 6 hex digits
	*	prefixed with a '&#035;'.
	*/
	public final static String getColour(int c)
	{
		String test;
		try
		{
			// Here we will coerce all colours but white and any custom colours that
			// were set when the pens[] array was modified by SALly in readCustomPens().
			// I have added an additional test for isColourCoercedByLayer() to the end
			// of this, already complex test, because text objects in blocks, with a layer
			// of 1 and colour by layer were testing the isCustomPenColour[Math.abs(c)]
			// which would be true, not false, because the layer was defined in the config.d2s.
			if (isCoercedColour == true && 
				(c != 255 && isCustomPenColour[Math.abs(c)] == false) )
			{
				test = colourTable[Math.abs(coerciveColour)];
			}
			else
			{
				test = colourTable[Math.abs(c)];
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.err.println("StyleSheetGenerator warning: illegal colour number: "+c+
			" (possibly out of range), using fushia (220) instead.");
			test = colourTable[220];
			isCoercedColour = false;
		}

		return test;
	}


	

	/**	Returns a custom layer that corresponds with the argument name.
	*	@return null if there are no custom layers defined or the
	*	argument layer is not defined in the custom layer list.
	*/
	public final static CustomLayerStyle getCustomLayerStyle(String name)
	{
		if (vCustomLayerStyles == null)
		{
			if (VERBOSE)
				System.err.println("DxfPreprocessor: No custom styles defined.");
			return null;
		}
		int numLayers = vCustomLayerStyles.size();
		for (int i = 0; i < numLayers; i++)
		{
			CustomLayerStyle style;
			style = (CustomLayerStyle)vCustomLayerStyles.get(i);
			
			if (style.getName().equals(name))
			{
				return style;
			}
		}

		if (DEBUG)
			System.err.println("DxfPreprocessor: requested style not found."+name);
		return null;
	}


	/** Returns all of the custome text styles gleened from the config.d2s.
	*	@return null if the number of custom styles defined by textStyle is
	*	0 or the named style is not on the hashtable.
	*/
	public final static String getCustomTextStyle(String name)
	{
		if (hCustomTextStyles == null)
		{
			if (VERBOSE)
				System.err.println("DxfPreprocessor: No custom styles defined.");
			return null;
		}
		
		return (String)(hCustomTextStyles.get(name));
	}




	/** Creates a master list of files to process and controls the rest of
	*	the multithreaded conversion process.
	*/
	public void activateProcessor()
	{
		// Report a bit about what we are about to do.
		if (VERBOSE == true)
		{
			System.out.println(Dxf2SvgConstants.APPLICATION+" verbose: " + VERBOSE );
			System.out.println("	debug: " + DEBUG );
			System.out.println("	wipeout: " + WIPE_OUT_TEXT );
			System.out.println("	cascading style sheet: " + MAKE_CSS );
			System.out.println("	precision: \"" + PRECISION + "\"");
			System.out.println("	DTD description included: \"" + DTD_ALT + "\"");
			switch (RESOLUTION){
				case Dxf2SvgConstants.CURRENT_RESOLUTION:
					System.out.println("	target screen resolution: \"current\"");
					break;

				case Dxf2SvgConstants.SIXFORTY_X_FOUREIGHTY:
					System.out.println("	target screen resolution: \"VGA (640x480)\"");
					break;

				case Dxf2SvgConstants.EIGHT_HUNDRED_X_SIX_HUNDRED:
					System.out.println("	target screen resolution: \"800x600\"");
					break;

				case Dxf2SvgConstants.TENTWENTYFOUR_X_SEVENSIXTYEIGHT:
					System.out.println("	target screen resolution: \"1024x768\"");
					break;

				default:
					// Should never get here.
					System.out.println("	target screen resolution: \""+
						Dxf2SvgConstants.BEST_GUESS+"\"");
			}

		}

		// Collect all the files from SAL and put onto a list
		// Steps to preprocessing DXF file(s).
		fileList = new Vector();
		if (sal != null)
		{
			Dxfs = sal.getKeys(SALConsts.L_FILES);
			// Now check to see if there was a ":files" list
			if(Dxfs != null)
			{
				for (int i = 0; i < Dxfs.length; i++)
				{
					fileList.add(Dxfs[i]);
				}
			}
		}
		// 1) Determine if we are dealing with a group of files.
		// We do that by testing to see if the DXF_FILE variable is a file or
		// directory. If a directory get a listing of the files in the directory
		// that are valid DXFs and create DxfParser Object(s) as threads and
		// process them.
		// Now it happens that if there was a list in the config file there will
		// not be any file here to test.
		if (! DXF_FILE.equals(""))
		{
			if ((FTester.test(false, DXF_FILE) & 16) > 0) // if this is a directory
				// and do it quietly (false).
			{
				DL = new DirLister(DXF_FILE);
				// The default behavior is work recursively but I don't want
				// to assume that in this environment so we set it to false
				// and let the user set it to true with the '-r' switch if
				// they wish.
				DL.setRecursion(false);
				DFilter = new DXFFilter();
				DL.setFileFilter(DFilter);
				// Add handler for recursion.
				if (RECURSIVE)
					DL.setRecursion(true);
	
				Dxfs = DL.getList();
	
				// Do some more processing as required.
				System.out.println("Collecting files:");
				for (int i = 0; i < Dxfs.length; i++)
				{
					// Further test the files for various attributes
					// that may inhibit successful conversion.
					if (! testFile(Dxfs[i]))
						continue;
					fileList.add(Dxfs[i]);
				}
			}
			else // 2) If DXF_FILE is a file
			{
				if (! testFile(getFileName()))
					return;
				fileList.add(getFileName());
			}	
		}
		
		if (fileList.size() < 1)
		{
			System.out.println("DxfPreprocessor: nothing to do.");
			return;
		}
		else
		{
			System.out.println("DxfPreprocessor: processing: "+fileList.size()+" files");
		}
		
		

		// Create a database of illustration families by presearching for 
		// relatives.
		if (isDatabaseDriven()  || takeNotes())
		{
			extractFigureData(fileList);
			if (takeNotes())
			{
				if (svgNoteManager == null)
				{
					svgNoteManager = new SvgNoteManager(fileList, library);
				}
			}
		}
		
		// Don't process the files if the user just needed the database updated.
		if (UPDATE_DB_ONLY == false)
		{
			Iterator it = fileList.iterator();
			String name;
			int i = 1;
			while (it.hasNext())
			{
				name = (String)it.next();
				// do this to set working dir and check pathing.
				setFileName(name);
				// report progress.
				System.out.println("========\nfile " + i + " of " + fileList.size());
				i++;
				if (sal != null)
				{
					new DxfConverter(sal, getFileName());
				}
				else
				{
					new DxfConverter(getFileName());
				}
				//	-- or --
				// This could get hairy if we have to over-write a file
				//	Thread convert = new DxfConverter(name);
				System.gc();
			}	// end while
		} else { // endif
			System.out.println("...finished updating boardno-control.xml database.");
		}
	} // end of activateProcessor()










	/** Returns the {@link SvgNoteManager} which manages the collection, compilation and output
	*	of notes into SVG graphics.
	*/
	public static void getNotes(SvgBuilder svgB)
	{
		if (svgB == null || svgNoteManager == null)
		{
			System.err.println("DxfPreprocessor.getNotes(): error either SvgBuilder "+
				"or SvgNoteManager is null");
			return;
		}
		
		svgNoteManager.getNotes(svgB, getFileName());
	}
	











	/** This will return the Figure Sheet Database if the '-group_families' has been set
	*	If this switch has not been used this method will return null.
	*/
	public static LibraryCatalog getLibraryCatalog()
	{
		return library;
	}

	/** This method takes a String name which is thought to be a file name with fully qualified 
	*	path and normalizes it. Normalization means that the returned name will be lower case
	*	and will not include the file's extension or path. Example: if the argument is 
	*	<B>c:/tmp/Faaagqc.DWG</B> the normalized version will be <B>faaagqc</B>
	*/
	public static String getNormalizedFileName(String name)
	{
		// This algorithm is used in HtmlWrapperBuilder.java as well.
		String spotCall = new File(name).getName();
		// Strip off the extension.
		int posDot = spotCall.lastIndexOf(".");
		if (posDot >= 1) // must be one char in length min.
		{
			spotCall = spotCall.substring(0,posDot);
		}
		// Everything should be in lower case to minimize false negatives on lookup in Database.
		spotCall = spotCall.toLowerCase();
		
		return spotCall;
	}
	
	
	
	///////////////// constant regex expressions for all /////////////
	public final static String figureNumSheetTitleRegex = 
		"^Figure"+				// Literal word 'Figure' followed by...
		"\\p{Blank}{1,}"+		// at least one or more spaces or tabs followed by...
		"\\p{Digit}{1,3}"+		// between 1 and 3 digits (part) followed by...
		"(\\p{Alpha}{1})?"+		// possibly a letter like 1a as well...
		"(-"+					// a hyphen followed by...
		"\\p{Digit}{1,3}"+		// between 1 and 3 digits (section or figure) followed by...
		"(\\p{Alpha}{1})?"+		// possibly a letter like 1-2-9a matches 1-2a as well...
		"(-\\p{Digit}{1,3}"+	// perhaps a dash and another 1 to 3 digits (figure no. if 
								// previous number was a section no.) followed by...
		"(\\p{Alpha}{1})?)?)?"+	// possibly a letter like 1-2-9a matches 1-2a as well...
		"\\p{Blank}{1,}"+		// at least one or more spaces or tabs followed by...
		"(\\p{Alnum}{1,}|\\(|[\\xC0-\\xD6])+"	// É, È, Á, À...
								// at least one char (UC/LC) or digit or open
		;						// parenthesis or French acented character one or more times.
								// 'Figure 1-2 (Sheet...' or 'Figure 9-12-7 TCAS...'	
								
	public final static String figureNumSheetTitleFoldOutRegex = 
		"\\p{Blank}{1,}Figure"+	// spaces followed by the literal word 'Figure' followed by...
		"\\p{Blank}{1,}"+		// at least one or more spaces or tabs followed by...
		"\\p{Digit}{1,3}"+		// between 1 and 3 digits (part) followed by...
		"(\\p{Alpha}{1})?"+		// possibly a letter like 1a as well...
		"(-"+					// a hyphen followed by...
		"\\p{Digit}{1,3}"+		// between 1 and 3 digits (section or figure) followed by...
		"(\\p{Alpha}{1})?"+		// possibly a letter like 1-2-9a matches 1-2a as well...
		"(-\\p{Digit}{1,3}"+	// perhaps a dash and another 1 to 3 digits (figure no. if 
								// previous number was a section no.) followed by...
		"(\\p{Alpha}{1})?)?)?"+	// possibly a letter like 1-2-9a matches 1-2a as well...
		"(\\p{Blank}{1,}"+		// at least one or more spaces or tabs followed by...
		"\\(Sheet"+				// an open and the english word Sheet
		"\\p{Blank}{1,}"+		// one or more blanks
		"\\p{Digit}{1,3}"+		// one to three digits
		"\\p{Blank}{1,}of"+		// one or more blanks and a literal 'of'
		"\\p{Blank}{1,}"+		// one or more blanks
		"\\p{Digit}{1,3}\\))?$"	// one to three digits, a close parenthesis that 
		;						// can occur once or not at all at the end of a line.

	public final static String boardnoRegex = 
		"^[gG]"+ 				// starts with 'g' always followed by...
		"\\p{Digit}{5}"+		// five digits, ...
		"\\p{Alpha}{2}$";		// and ends with two [a-zA-z] characters.
								// This pattern must appear on it's own and
								// not part of another string.
								
	// C-12-130-000/MN-001 or C-13-M21-000/MS-000 or C-34-100-000/MS-002
	public final static String ndidRegex = 
		"^C" + 					// Starts with 'C'
		"-" +					// then a '-'
		"\\p{Digit}{2}" + 		// then 2 digits
		"-" +					// then a hyphen
		"[1M]" +				// then either a one or an 'M'
		"\\p{Digit}{2}" +		// then two digits
		"-" + 					// then another literal hyphen
		"0[A-Z0][0-9]" + 		// followed by 0X0 or 0X2 // added April 2, 2004
		"/" +					// a forward slash
		"\\p{Upper}{2}" +		// followed by two alpha chars
		"-" +					// then a hyphen
		//"00[0-24]$";			// then ends with two zeros and any number from 0 to 2 or 4.
		"\\p{Digit}{3}$";		// This has changed to account for mods and leaflets.
								
								
	/** Creates	a database of illustrations and their relatives. This method presearches
	*	the DXF source files for figure titles and sheet numbers - like figure 1-2 sheet 1 of 2 
	*	and figure 1-2 sheet 2 of 2.
	*/
	protected void extractFigureData(Vector dxfFiles)
	{
		// Now we have a list of Dxf files we can now do searches for illustration 
		// families here; this will pre-search the dxfs for Figure numbers and try
		// and determine the sheet numbers. 
		//
		// Let's make a search engine and do a pre-search for families of illustrations.
		DxfSearchEngine dxfSearch = new DxfSearchEngine();
		FigureSheetDatabase figDB;
		// Create patterns
		Pattern figurePattern = Pattern.compile(figureNumSheetTitleRegex);
		Pattern figureFOPattern = Pattern.compile(figureNumSheetTitleFoldOutRegex);
		Pattern boardnoPattern = Pattern.compile(boardnoRegex, Pattern.CASE_INSENSITIVE);
		Pattern ndidPattern = Pattern.compile(ndidRegex);
		String name = null;
		String figure = new String();		// Where we keep the raw figure string.
		String figNumber = new String();
		String sheetNumber = new String();	// Also used as a key in the database.
		String engBoardno = new String();
		String freBoardno = new String();
		String spotCall = new String();
		String ndid = new String();
		String[] boardnos = new String[2];
		String[] figSheetPair = new String[2]; // return value from figure title parse.
		String[] record;
		// Create a language var and initialize to -1. Important when parsing figure title strings.
		int language = Dxf2SvgConstants.UNDEFINED;
		
		Iterator it = fileList.iterator();
		while (it.hasNext())
		{	
			// Get a file name
			name = (String)it.next();
			
			record = new String[FigureSheetDatabase.NUM_FIELDS];
			
			// This algorithm is used in HtmlWrapperBuilder.java as well.
			spotCall = getNormalizedFileName(name);
			
			// Now get the search engine ready to search the dxf file.
			dxfSearch.setDxfFile(new File(name));
			
			// Find the NDID 
			ndid = dxfSearch.find(ndidPattern);
			// If not, stop conversion so Illustrator can fix file.
			logEvent(name, "\n NDID: '"+ndid+"'.");
			if (ndid == null || ndid.length() == 0)
			{
				throw new NDIDNotFoundException(name);
			}
			
			// now got the namespace or book for this file; let's look it up in the library.
			if (library.containsNamespace(ndid))
			{
				figDB = library.retrieveFigures(ndid);
			}
			else
			{
				// there wasn't one so make a new entry.
				figDB = new FigureSheetDatabase();
			}
			
			// Next find the boardnos but,
			// English and or French, which is which? It depends on the DXF
			boardnos[0] = dxfSearch.find(boardnoPattern);
			boardnos[1] = dxfSearch.findNext();
			
			// boardnos[0] should always be populated but boardnos[1] may not be so test and infer.
			if (DEBUG)
			{
				System.out.println("name :"+name+" ###"+boardnos[0]);
			}
			language = getBoardnoLanguage(boardnos[0]);
		
			// Now sort out which boardno is which cause we can't tell from the 
			// order we scouped them out of the file.
			if (language == Dxf2SvgConstants.ENGLISH)
			{
				engBoardno = boardnos[0];
				freBoardno = boardnos[1];
			}
			else if (language == Dxf2SvgConstants.FRENCH)
			{
				engBoardno = boardnos[1];
				freBoardno = boardnos[0];
			}
			else if (language == Dxf2SvgConstants.MULTI_LINGUAL)
			{
				engBoardno = freBoardno = boardnos[0];
			}
			else 
			{
				System.err.println("DxfPreprocessor warning: boardnos, and therefore language values "+
				"missing '"+name+"'.");
				logEvent(name,"Missing Boardno value(s).");
			}
			
			// Next search and parse out the french title we do it first as we assume
			// the English will have the most up-to-date information and will over-write
			// any entries in the record array with more upto date information.
			figure = dxfSearch.findOnLayer("t", figurePattern);
			if (figure == null || figure.length() < 1)
			{
				// Not there? search the french layer
				figure = dxfSearch.findOnLayer(
					TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH), figurePattern);
				if (figure == null || figure.length() < 1)
				{
					if (VERBOSE)
					{
						System.out.println("DxfPreprocessor: illustration contains no french title.");
					}
					if (language == Dxf2SvgConstants.FRENCH)
					{
						throw new FigureTitleNotFoundException(
							spotCall,
							TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH));
					}
				}
				else
				{
					figSheetPair = parseFigureString(figure, Dxf2SvgConstants.FRENCH, record);
				}
			}
			else
			{
				figSheetPair = parseFigureString(figure, Dxf2SvgConstants.FRENCH, record);
			}
			
			// execute search for title on english layer.
			figure = dxfSearch.findOnLayer("t", figurePattern);
			if (figure == null || figure.length() < 1)
			{
				// Not there? search the english layer
				String langEng = TableLayer.getLanguageLayerName(Dxf2SvgConstants.ENGLISH);
				String langDef = TableLayer.getDefaultLanguageLayerName();
				figure = dxfSearch.findOnLayer(langEng, figurePattern);
				if (figure == null || figure.length() < 1) 
				{
					// Not there? maybe it's a fold out.
					figure = dxfSearch.findOnLayer(langDef, figureFOPattern);
					if (figure == null || figure.length() < 1) 
					{
						figure = dxfSearch.findOnLayer(langEng, figureFOPattern);
						if (figure == null || figure.length() < 1) 
						{
							throw new FigureTitleNotFoundException(spotCall,langEng);
						}
					}
					// to get here we managed to collect the FO version of the figure title
					// so let's put it in the proper order so we can parse it.
					int pos = figure.lastIndexOf("Figure ");
					StringBuffer tmp = new StringBuffer();
					// This will move the word Figure and the figure number to the front
					// of the string.
					tmp.append(figure.substring(pos));
					// Now trim off the Figure portion so we can do further string manipulations.
					figure = figure.substring(0, pos);
					// Now we have to move the (Sheet x of x) to the front as well 
					// be careful though, sometimes you also get a '(Aircraft' string!
					int sheetPos = figure.lastIndexOf("(Sheet ");
					if (sheetPos > -1) // found it
					{
						tmp.append(" "+figure.substring(sheetPos));
						figure = figure.substring(0,sheetPos);
					}
					else 
					{
						// better append more space between the fig num and title.
						tmp.append("   ");
					}
					tmp.append(figure.trim());

					figure = tmp.toString();
				}  // end if
			}  // end if
			// now we need to parse out the figure number sheet number number of sheets
			// and create a record of this illustration's data in the FigureSheetDatabase
			figSheetPair = parseFigureString(figure, Dxf2SvgConstants.ENGLISH, record);

			// By now the record[] array is populated.
			figNumber = figSheetPair[0];
			sheetNumber = figSheetPair[1];
			//   key     eBoardno   fBoardno   eTitle    fTitle	     spotcall   total Sht
			//+-------++----------+----------+---------+-----------+----------+----------+
			//|  "1"  || g01234ea | g01235fa | "Title" | "Fig nom" | faaabcd  |   "2"    |
			//+-------++----------+----------+---------+-----------+----------+----------+
			record[0] = engBoardno;
			record[1] = freBoardno;
			//record[2]; done
			//record[3]; done
			record[4] = spotCall;
			//record[5]; done
			
			if(DEBUG)
			{
				System.out.println("Figure: "+figNumber);
				System.out.println("Figure key: "+sheetNumber);
				for (int i = 0; i < record.length; i++)
				{
					System.out.println(record[i]);		
				}
				System.out.println("============");
			}
			// Store the results for query by HTMLWrapperBuilder when it is time to output the 
			// svg and wrapper files.
			figDB.addFigureSheetAndValues(figNumber, sheetNumber, record);
			library.update(ndid, figDB);
		} // end while
		
		// We can now publish the data as XML.
		library.outputXML(dbXMLPath);
		reserializeLibrary();
	}  // end extractFigureData()
	
	/**	Reserializes the library for future look ups.
	*/
	protected void reserializeLibrary()
	{
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(dbPath));
			oos.writeObject(library);			
		}
		catch (Exception e)
		{
			System.err.println("DxfPreprocessor.reserializeLibrary() error: unable to write "+
				"out serialized database. "+e);
		} 
	}
	
	
	/** This method returns an array of strings that contain the figure's number
	*	the sheet number and total number of sheets and the title of the illustration.
	*	This method is highly specialized and only works for titles with the following
	*	format: 'Figure x(-x(L)?(-x(L))) (\(Sheet x of x\))? Figure Title'.
	*	<P>
	*	<B><EM>Bug:</EM></B> It is not possible, at this time to determine which title
	*	string contains french and which is english on a bilingual drawing with both
	*	titles on the same layer 'T'. The upshot is that the french version of a bilingual
	*	illustration will have the English title in the &lt;title&gt; tag.
	*	@return String The figure number as a String.
	*	@throws {@link java.lang.ArrayIndexOutOfBoundsException} if values[].length is 
	*	less than {@link FigureSheetDatabase#NUM_FIELDS}.
	*	
	*/
	protected String[] parseFigureString(String sentence, int language, String[] values)
	{
		StringTokenizer st = new StringTokenizer(sentence);
		String totalSheets = "1";	// Ditto
		StringBuffer title = new StringBuffer();
		String[] figureSheetNum = new String[2];
		String figureNum = "";
		String sheetNum = "1";		// Default value if we find none.
		String thisToken = new String();
		boolean isMultiSheet = false; // Has multipule sheets.  
		// Look for a multisheet pattern.
		Pattern sheetPattern = Pattern.compile("\\((Sheet|feuille)");	
		Matcher m = sheetPattern.matcher(sentence);
		if (m.find())
		{
			isMultiSheet = true;
		}
		
		// Count off the tokens 
		int tokensSoFar = 0;
		
		while (st.hasMoreTokens())
		{
			thisToken = st.nextToken();
			tokensSoFar++;
			
			switch (tokensSoFar)
			{
				case 1:  // 'Figure'
					break;
					
				case 2:  // figure number
					figureNum = thisToken;
					break;
					
				case 3: // This would be '(Sheet' or '(feuille'.
					if (! isMultiSheet)
					{
						title.append(" " + thisToken); 
					}
					break;
					
				case 4:	// '4B' or '1'.
					if (isMultiSheet)
					{
						sheetNum = thisToken;	
					}
					else
					{
						title.append(" " + thisToken);
					}
					break;
				
				case 5:  // this would be 'of' or 'de' or another word from the title
					if (! isMultiSheet)
					{
						title.append(" " + thisToken);
					}
					break;
					
				case 6:  // This is the last number but with a parenthesis like '4)'.
					if (isMultiSheet)
					{
						int posParen = thisToken.indexOf(")");
						if (posParen > 0)  // We found a ')' .
						{
							thisToken = thisToken.substring(0, posParen);
						}
						totalSheets = thisToken;
					}
					else
					{
						title.append(" " + thisToken);
					}
					break;
					
					
				default:  // to get here we have to have seen all the tokens we are interested
						  // in and so the rest are words of the title.
					title.append(" " + thisToken);
					break;
					
			}  // switch
		}  // while 
		
		//   key     eBoardno   fBoardno   eTitle    fTitle	     spotcall   total Sht
		//+-------++----------+----------+---------+-----------+----------+----------+
		//|  "1"  || g01234ea | g01235fa | "Title" | "Fig nom" | faaabcd  |   "2"    |
		//+-------++----------+----------+---------+-----------+----------+----------+
		//values[0] = ;  // eboardno (populated elsewhere).
		//values[1] = ;  // fboardno (populated elsewhere).
		if (language == Dxf2SvgConstants.ENGLISH)
		{
			values[2] = title.toString(); 
		}
		
		if (language == Dxf2SvgConstants.FRENCH)
		{
			values[3] = title.toString();
		}
		//values[4] =  ;  //spot call (populated elsewhere).
		values[5] = totalSheets;
		
		figureSheetNum[0] = figureNum;
		figureSheetNum[1] = sheetNum;
		
		return figureSheetNum;
	}


	// Made this static so we could use it to check for language field in DxfPreprocessor
	// without instantiating the entire object and or duplicating code.
	/**	This method tests the language character for English, French or Bilingual and
	*	returns the integer value of the language.
	*	<UL>
	* 	<LI>Dxf2SvgConstants.UNDEFINED = -1
	* 	<LI>Dxf2SvgConstants.ENGLISH = 1
	* 	<LI>Dxf2SvgConstants.FRENCH = 2
	*	<LI>Dxf2SvgConstants.MULTI_LINGUAL = 3
	*	</UL>
	*/
	public static int getBoardnoLanguage(String src)
	{
		// Check everything cause this is a static method.
		
		if (src == null)
		{
			return Dxf2SvgConstants.UNDEFINED;
		}
		
		if (src.length() < 6)
		{
			return Dxf2SvgConstants.UNDEFINED;
		}

		char c = src.charAt(6);
		
		if (c == 'e' || c == 'E')
		{
			return Dxf2SvgConstants.ENGLISH;
		}
		else if (c == 'f' || c == 'F')
		{
			return Dxf2SvgConstants.FRENCH;
		}
		else if (c == 'b' || c == 'B')
		{
			return Dxf2SvgConstants.MULTI_LINGUAL;
		}
		
		return Dxf2SvgConstants.UNDEFINED;
	}





	/** Tests if a file for the listed tests.
	*	<BR><BR>
	*	Currently it tests a file to see if it:
	*	<UL>
	*	<LI>Is empty.
	*	<LI>Does not exist.
	*	<LI>Is not readable.
	*	<LI>Is not writable.
	*	<LI>Is a directory.
	*	<LI>Is NULL.
	*	<LI>Is a hidden file.
	*	</UL>
	*	@see FileTester
	*	@param file Name of the file to test as a string.
	*/
	protected boolean testFile(String file)
	{
		int result = FTester.test(false, file);
		if (result > 0)
		{
			System.err.println("DxfPreprocessor error(s): "+file+" ");
			FTester.reportAttributes(result);
			return false;
		}

		return true;
	}





	/////// switch setting methods ///////////
	/** Returns current <I>VERBOSE</I> switch setting. */
	public static boolean verboseMode()
	{	return VERBOSE;	}
	/** Sets current <I>VERBOSE</I> switch setting. */
	public static void setVerboseMode(boolean verbose)
	{	VERBOSE = verbose;	}
	/** Returns current <I>DEBUG</I> switch setting. */
	public static boolean debugMode()
	{	return DEBUG;	}
	/** Sets current <I>DEBUG</I> switch setting. */
	public static void setDebugMode(boolean debug)
	{	DEBUG = debug;	}
	// /** Returns current <I>WIPE_OUT_TEXT</I> switch setting. */
	//public static boolean WipeOutMode()
	//{	return WIPE_OUT_TEXT;	}
	// /** Sets current <I>WIPE_OUT_TEXT</I> switch setting. */
	//public static void setWipeOutMode(boolean wipeout)
	//{	WIPE_OUT_TEXT = wipeout;	}
	/** Returns current <I>MAKE_CSS</I> mode setting. */
	public static int cssMode()
	{	return MAKE_CSS;	}
	/** Sets current <I>MAKE_CSS</I> mode setting.
	*	This method has special properties. If you pass successive integers
	*	to the method they have an accumualtive effect. This is because the
	*	fields are bit fields. This doesn't cause a problem because we test
	*	for specific bit values at specific times, if there are any values
	*	added that are not defined, they are ignored.
	*	<P> Passing a -1 to the method will reset the bit values to the default
	*	value of {@link Dxf2SvgConstants#DECLARED_CSS}.
	*/
	public static void setCssMode(int cssmode)
	{
		if (cssmode == -1)	// reset requested.
		{
			MAKE_CSS = 0;
			return;
		}

		// Has this switch already been set? If it has just issue a message and carry
		if ((MAKE_CSS & cssmode) == cssmode)
		{
			System.out.println("DxfPreprocessor: Ignoring duplicate CSS switch."+cssmode);
			return;
		}

		MAKE_CSS += cssmode;
		


		// Actually change the static pen table
		// for all drawings in this conversion and create some default
		// layers to watch for from the StyleSheetGenerator.

		if ((MAKE_CSS & Dxf2SvgConstants.SPAR_C130) == Dxf2SvgConstants.SPAR_C130)
		{
			// Make a new Vector to store the layers.
			vCustomLayerStyles		= new Vector();

			// makeup the default spar css.
			//".st0{stroke:#000000;stroke-width:0.015in;}",
			// change the pen style
			pens[1] = new Pen(0.015);
			vCustomLayerStyles.add(new CustomLayerStyle("3",pens[1]));

			pens[2] = new Pen(0.013);
			vCustomLayerStyles.add(new CustomLayerStyle("1",pens[2]));

			pens[3] = new Pen(0.007);
			vCustomLayerStyles.add(new CustomLayerStyle("t",pens[3],7));

			pens[4] = new Pen(0.020);
			vCustomLayerStyles.add(new CustomLayerStyle("6",pens[4]));

			///////////// note we don't use pen 5 so leave it. //////////////

			pens[6] = new Pen(0.003);
			vCustomLayerStyles.add(new CustomLayerStyle("10",pens[6]));

			pens[7] = new Pen(0.007);
			vCustomLayerStyles.add(new CustomLayerStyle("7",pens[7]));

			pens[8] = new Pen(255, 0.003);
			// The fill for this object is white.
			vCustomLayerStyles.add(new CustomLayerStyle("halo",pens[8],255));

			pens[9] = new Pen(0.008);
			vCustomLayerStyles.add(new CustomLayerStyle("arline",pens[9]));

			pens[10] = new Pen(0.007);
			vCustomLayerStyles.add(new CustomLayerStyle("dim",pens[10]));

			pens[11] = new Pen(255, 0.015);
			vCustomLayerStyles.add(new CustomLayerStyle("hred",pens[11],255));

			pens[12] = new Pen(255, 0.007);
			vCustomLayerStyles.add(new CustomLayerStyle("hwhite",pens[12],255));

			pens[13] = new Pen(255, 0.013);
			vCustomLayerStyles.add(new CustomLayerStyle("hyellow",pens[13],255));

			pens[14] = new Pen(0.027);
			vCustomLayerStyles.add(new CustomLayerStyle("border",pens[14]));

			pens[15] = new Pen();
			vCustomLayerStyles.add(new CustomLayerStyle(
				TableLayer.getLanguageLayerName(Dxf2SvgConstants.ENGLISH),pens[15],7));

			pens[16] = new Pen();
			vCustomLayerStyles.add(new CustomLayerStyle(
				TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH),pens[16],7));

			// These are pretty old conventions; Layer '4' became layer 2
			// a yellow, like layer 1, but with a dashed linetype.
			pens[17] = new Pen(0.013);
			vCustomLayerStyles.add(new CustomLayerStyle("2",pens[17]));

			pens[18] = new Pen(0.013);
			vCustomLayerStyles.add(new CustomLayerStyle("4",pens[18]));
			
			// Fill in the rest of the pens with default narrow widths.
			// they will all have black ink.
			for (int i = 19; i < NUM_OF_PENS; i++)
			{
				pens[i] = new Pen(0.001);
			}
		}	// end if
	}  // end of setCssMode()
	
	
	

	/** Creates a 'Spar' style sheet that dictates standard line weights
	*	used by illustrators at Spar Technical Publications department.
	*
	*	This method is called from Dxf2Svg while parsing command line options.
	*
	*	If this method is invoked and the <b>-f</b> option has been set
	*	Dxf2Svg will check to see if <b>-f</b>'s argument refers to a file
	*	or directory. If it is a file then the CSS is placed in the parent
	*	directory of the file; if it is a directory the file is place in
	*	that directory. If the <b>-f</b> is not used the CSS is created in
	*	the current directory.
	*/
	public void makeDefaultStyleSheet()
	{
		File SPAR = new File(Dxf2SvgConstants.STYLE_SHEET_NAME);
		try
		{
			BufferedWriter BWriter = new BufferedWriter(
				new FileWriter(SPAR));

			BWriter.write(getCustomStyleSheet(Dxf2SvgConstants.NO_INDENT));

			BWriter.write("/*** Automatically generated by "+Dxf2SvgConstants.APPLICATION+" ***/");
			BWriter.newLine();
			BWriter.write("/* EOF */");

			BWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("DxfPreprocessor.makeDefaultStyleSheet(): "+
				"Unable to output default style sheet. "+e);
		}
	}

	/** Returns the entire Spar style sheet as a string.
	*/
	private final String getCustomStyleSheet(int indent)
	{
		if (vCustomLayerStyles == null)
		{
			System.err.println("DxfPreprocessor error: No custom styles defined.");
			return "";
		}
		int numLayers = vCustomLayerStyles.size();
		StringBuffer sbStyle = new StringBuffer();
		CustomLayerStyle style;
		for (int i = 0; i < numLayers; i++)
		{
			style = (CustomLayerStyle)vCustomLayerStyles.get(i);
			if (indent == Dxf2SvgConstants.INDENT)
				sbStyle.append("\t\t");
			sbStyle.append(style.toString());
			sbStyle.append("\n");
		}
		return sbStyle.toString();
	}



	/** Returns current <I>DXF_FILE</I> setting. */
	public static String getFileName()
	{	return DXF_FILE;	}
	
	
	
	/** Sets current <I>DXF_FILE</I> setting.
	*	The Argument is converted into a canonical (pedantic) path name
	*	to distill out references to '.', '..' and relative paths.
	*/
	public static void setFileName(String path)
	{
		// Here we translate the argument to a conanical (pedantic)
		// path so that we can overcome goofy problems introduced
		// by users who use '.', '..' and relative names in argument
		// paths
		try
		{
			DXF_FILE = new File(path).getCanonicalPath();
		}
		catch (IOException e)
		{
			System.err.println("DxfPreprocessor error: ");
			System.err.println(e);
			return;
		}
		if ((new File(DXF_FILE).isDirectory()) == true)
			WORKING_DIR = DXF_FILE;
		else
			WORKING_DIR = new File(DXF_FILE).getParent();
	}
	
	/** Returns the conversion directory name as a String. */
	public static String getConversionDirectory()
	{	return WORKING_DIR;	}
	/** Explicitly sets the conversion directory.
	*
	*	Called when conversions are run recursively. */
	public void setConversionDirectory(String dir)
	{
		if ((new File(dir).isDirectory()))
			WORKING_DIR = dir;
	}
	/** Returns current <I>PRECISION</I> setting.
	*
	*	All calculations are carried out with
	*	full precision but the user can indicate how many digits
	*	after the decimal to include in the final SVG output.
	*	It generally reduces the size of an SVG and most applications
	*	can't take advantage of the maximum precision any way.
	*	@see SvgUtil#trimDouble
	*/
	public static int getPrecision()
	{	return PRECISION;	}
	/** Returns current <I>PRECISION</I> setting.
	*
	*	All calculations are carried out with
	*	full precision but the user can indicate how many digits
	*	after the decimal to include in the final SVG output.
	*	It generally reduces the size of an SVG and most applications
	*	can't take advantage of the maximum precision any way.
	*	@see SvgUtil#trimDouble
	*	@param precision Number of decimal places or scale of output number.
	*/
	public static void setPrecision(int precision)
	{	PRECISION = precision;	}
	/** Returns current <I>DTD_ALT</I> switch setting. */
	public static boolean includeDTD()
	{	return DTD_ALT;	}
	/** Sets current <I>DTD_ALT</I> switch setting. */
	public static void setIncludeDTD(boolean includedtd)
	{	DTD_ALT = includedtd;	}
	/** Returns current <I>INCLUDE_JAVASCRIPT</I> location setting. */
	public static int includeJavaScript()
	{	return INCLUDE_JAVASCRIPT;	}
	/** Sets current <I>INCLUDE_JAVASCRIPT</I> location setting. */
	public static void setIncludeJavaScript(int js_location)
	{	INCLUDE_JAVASCRIPT = js_location;	}
	/** Returns current <I>JS_SRC_PATH</I> setting. */
	public static String includeJavaScriptSrcPath()
	{	return JS_SRC_PATH;	}
	/** Sets current <I>JS_SRC_PATH</I> setting. */
	public static void setIncludeJavaScriptSrcPath(String js_src_path)
	{	JS_SRC_PATH = js_src_path;	}
	/** Sets a switch that allows searching of subdirectories for
	*	valid DXF files to convert.
	*
	*	Only used if the user's '-f' argument is a directory.
	*/
	public static void setRecursiveMode(boolean recursion)
	{	RECURSIVE = recursion;	}
	/** Returns the current <I>RECURSIVE</I> setting. */
	public static boolean getRecuriveMode()
	{	return RECURSIVE;	}
	/** Sets the size of the rendering to make the Svg file fill
	*	the browser window upon opening. */
	public static void setRenderSize(int resolution)
	{	RESOLUTION = resolution;	}
	/**	Returns the user requested SVG rendering monitor resolution.*/
	public static int getRenderSize()
	{	return RESOLUTION;	}
	/** Set the font file search location. This could be a web site or
	*	a network location or a directory on the local machine.
	*	No checking of valid pathing is done.
	*/
	public static void includeFontUrl(String uriString)
	{
		if (uriString == null)	// can this happen?
			return;
		FONT_DIRECTORY = uriString;
		INCLUDE_FONT_URL = true;
	}

	/**	Returns a boolean value of whether a URL discription should be
	*	output to the Cascading Style Sheet description.
	*/
	public static boolean includeUrl()
	{	return INCLUDE_FONT_URL;	}

	/** Returns a URL of the font locations.
	*
	*/
	public static String getFontUrl()
	{	return FONT_DIRECTORY;	}

	/**	Sets a switch to output HTML wrappers for the SVG files. The
	*	HTML wrappers allow for better printing and add a button that
	*	will switch languages between French and English if both are
	*	present.
	*/
	public static void setHTMLWrappers(boolean wrappersRequired)
	{	HTML_WRAPPERS = wrappersRequired;	}

	/**	Sets a switch to output HTML wrappers for the SVG files. The
	*	HTML wrappers allow for better printing and add a button that
	*	will switch languages between French and English if both are
	*	present.
	*/
	public static final boolean useHTMLWrappers()
	{	return HTML_WRAPPERS;	}
	
	/** Forces all colours to display as the argument colour.
	*/
	public static final void setCoerciveColour(int c)
	{
		isCoercedColour = true;
		coerciveColour 	= c;
	}
	
	/** Sets the compression control switch to either on or off.
	*/
	public static void setZip(boolean z)
	{	IS_ZIPPED = z;	}
	
	/** Returns the switch setting that controls whether the SVG file will be compressed.
	*	@return true if the file is to be compressed, false otherwise.
	*/
	public static boolean isZipped()
	{	return IS_ZIPPED;	}
	
	

	/** Returns true if searches for family members should be made and false otherwise.
	*	False will have no effect on any output files. If {@link #useHTMLWrappers} is 
	*	false this switch will have no effect.
	*/
	public static boolean isDatabaseDriven()
	{		
		return SYNC_DATABASE;
	}
	
	
	/** This is the name of the name of the library of converted illustrations, as serialized
	*	java objects.
	*/
	public final static String serializedDB = "LibraryCatalog.ser";
	
	
	/** Switch to allow a database in the directory of <em>path</em> to be updated.
	*	Using this switch will either use or create a database that will then be used
	*	to make links between drawings in the HTML wrappers. This database can also 
	*	be output (as XML) to create a boardno-control.xml file for IETM conversions.
	*	<P>
	*	The argument path should be a directory where a file called <em>LibraryCatalog.ser</em>
	*	can be found. If the directory does not contain such a file, one will be created and
	*	the data from this conversion added to it.
	*	@throws IOException if the path is invalid.
	*	@throws ClassCastException if the stored data is not a LibraryCatalog or DatabaseContainer.
	*/
	public static void syncDatabase(String path)
	{	
		// save the path for extractFigureData()
		if (path.endsWith(File.separator))
		{
			dbPath = path + serializedDB;
			dbXMLPath = path + XMLDatabaseName;
		}
		else
		{
			dbPath = path + File.separatorChar + serializedDB;
			dbXMLPath = path + File.separatorChar + XMLDatabaseName;
		}
		
		if (new File(dbPath).isFile())
		{
			// open file
			try
			{
				FileInputStream fin = new FileInputStream(dbPath);
				ObjectInputStream ois = new ObjectInputStream(fin);
				library = (LibraryCatalog)ois.readObject();
			}
			catch (Exception e)
			{
				System.err.println("DxfPreprocessor.syncDatabase() error: unable to read "+
					"in serialized database."+e);
			}
			
		}
		else  // not a file 
		{
			// check if it is a valid directory and if not throw an exception
			if (! new File(path).isDirectory())
			{
				throw new FigureListLibraryNotFoundException(path);
			}
			// to get here it is a directory and there is no serializedDB here so
			// create a new one.
			library = new LibraryCatalog();
		}
		
		SYNC_DATABASE = true;
	}  // end syncDatabase()
	
	/** Returns the flag if the dxf contained a hyper link. Used to determine if the 
	*	xlink namespace needs to be included as an entity in the &lt;svg&gt; tag.
	*/
	public static boolean getUsesLinks()
	{
		return USES_LINKS;
	}
	
	/** This method is used by internal processes to allow the inclusion of xlink namespace
	*	if there happens to be a hyperlink in the dxf file being converted.
	*/
	public static void setUsesLinks(boolean b)
	{
		USES_LINKS = b;
	}
	
	/** Returns a flag of whether this is a parts list figure or not.
	*/
	public static boolean isPartsListFigure()
	{
		return IS_PARTS_LIST_FIGURE;
	}
	
	/** Returns the user defined attribute that will be used on an anchor of a part index
	*	number in a parts list figure. Typically it is 'onclick'.
	*/
	public static Attribute getPartsListAttribute()
	{
		return (Attribute)partsListOnClickAttrib.clone();
	}
	
	
	/** This method returns the attribute value associated with the parts list attribute.
	*	Typically, in the parts list, this would be an event like onclick. <B>NOTE: you 
	*	do NOT need the closing parenthesis like a normal function</B>. Example: where
	*	normally you would use onclick="someFunc('001')", you need only supply 'someFunc'.
	*	All other calculations and parenthesis are added by {@link dxf2svg.svg.SvgDxfHyperlink}.
	*/
	public void setPartsListAttribute(String a, String v)
	{
		partsListOnClickAttrib = new Attribute(a,v);
		IS_PARTS_LIST_FIGURE = true;
	}
	
	/** Returns true if drawings are to be searched for notes and false otherwise.
	*/
	public static boolean takeNotes()
	{
		return GENERATE_NOTES;
	}
	
	/** Sets the flag to instruct the conversion to search for and incorporate notes (as 
	*	pop-ups currently) in the converted SVG graphics.
	*/
	public void setGenerateNotes(boolean b)
	{
		GENERATE_NOTES = b;
	}
	
	/** Sets the flag that will either suppress the inclusion of the default JavaScript 
	*	scripts (for language selection etc.) or the default; include the scripts.
	*/
	public static void setSuppressBoilerPlateJavaScriptFlag(boolean b)
	{
		suppressDefaultJavaScript = b;
	}
	
	/** Returns the current setting for whether the default javascript functions will
	*	be included (default) or not.
	*/
	public static boolean suppressBoilerPlateJavaScript()
	{
		return suppressDefaultJavaScript;
	}
	
	/** Logs an event to the default event file which can be found in 'c:/temp/dxf2svg_log.txt'.
	*	@param fileName name of the file where the event occured (but not limited to that function).
	*	@param eventString what happened; a brief description of the problem. Output each time
	*	the event occurs.
	*/
	public static void logEvent(String fileName, String eventString)
	{
		if (eventLogger == null)
		{
			eventLogger = new Dxf2SvgLogger();

			System.out.println("Generating log file: "+
				eventLogger.getLogFileName());
		}
		
		eventLogger.logEvent(fileName, eventString);
	}
	
	/** Adds the data from the file specified by the param file inside of the &lt;svg&gt; tag. Use this 
	*	if you wish to include a sub-SVG or javascript or what ever. Note the content of the
	*	file must be formatted as children of the SVG tag or the graphic may not display
	*	correctly.
	*/
	public void addIncludeFileName(String file)
	{
		if (includeFileData == null)
		{
			includeFileData = new StringBuffer();
		}
		
		if (testFile(file) == true)
		{
			BufferedReader bread;
			try
			{
				File FIN = new File(file);
				bread = new BufferedReader(new FileReader(FIN));
				String data;
				while ((data = bread.readLine()) != null) 
				{
					includeFileData.append(data+"\n");
				}
				bread.close();
			}
			catch(IOException e)
			{
				throw new IncludeFileIOException("IO error reading include file: '"+file+"'.");
			}
			
			return;
		}

		throw new IncludeFileIOException("include file failure.");
	}
	
	
	/** Returns a big String that contains all of the data that was contained in the 
	*	include file(s).
	*/
	public static String getIncludeFileData()
	{
		return includeFileData.toString();
	}
	
	/** Returns true if there is include data to be added to the SVGs, false otherwise.
	*/
	public static boolean isInclude()
	{
		if (includeFileData != null)
		{
			return true;
		}
		
		return false;
	}
		
	/** This class gets thrown if the include file could not be found or read for some reason.
	*	see {@link #testFile(String)}.
	*/
	protected class IncludeFileIOException extends RuntimeException
	{
		protected IncludeFileIOException(String reason)
		{
			System.err.println(reason);
		}
	}
	
	/** Sets any object's colour to bylayer regardless of what the illustrator requested.
	*	Often blocks will be on a correct layer and the elements in the block are set 
	*	on the correct layer but their colour is set to something specific. The conversion,
	*	by default assumes that the illustrator required the element to appear as a specific
	*	colour if he set an elements colour to a specific value. This was a mistake; they
	*	seem to change an object's colour to what ever for no reason at all.
	*/
	public static void setColourCoercedByLayer(boolean b)
	{
		COERCE_COLOUR_BY_LAYER = b;
	}
	
	/** Returns true if the user requires all elements to be converted to represented in the
	*	colour of the layer they come from - even if the illustrator asked it to be a specific
	*	colour.
	*/
	public static boolean isColourCoercedByLayer()
	{
		return COERCE_COLOUR_BY_LAYER;
	}
	
	/** Use this method to read and reference graphic links. This is done to link svg graphics
	*	to other figures in other books. This graphic link database is a large database that 
	*	contains all the figureids of all the figures of all the Spar CC130 CFTOs. To create
	*	the database reference the documents for MakeExternalLinksDatabase application.
	*
	*/
	public static void setUseGraphicLinkDatabase(boolean b)
	{
		IS_EXTERNAL_GRAPHIC_LINKS = b;
	}
	
	/** Returns true if the user requests the use of the external graphic links database.
	*	false otherwise.
	*/
	public static boolean usesGraphicLinkDatabase()
	{
		return IS_EXTERNAL_GRAPHIC_LINKS;
	}
	
	/**	This method allows the user to update the boardno-control database <EM>without</EM>
	*	converting the graphics to SVG. This is done if the catalog pertinent information
	*	has changed in the dxfs but those changes don't effect the graphic information.
	*	(It speeds things up).
	*/
	public static void setUpdateDatabaseOnly(boolean update)
	{
		UPDATE_DB_ONLY = update;
	}
	
	/** This method dynamically switches any link that contain <CODE>.svg(z)</CODE> to
	*	<CODE>.html</CODE>. This is a bandaid solution to fix the fact that all links
	*	were erroneously entered to link to an svg rather than the svg's wrapper html file.
	*	Live and learn.
	*	@see #setSwapSvgForHtml
	*	@see dxf2svg.svg.SvgDxfHyperlink
	*	@see dxf2svg.svg.SvgDxfHyperlink#setXLink
	*/
	public void setSwapSvgForHtml(boolean swap)
	{
		IS_SWAP_SVG_FOR_HTML = swap;
	}
	
	/** Returns true if the user wants to swap links that have <CODE>.svg(z)</CODE> reference
	*	to html and false otherwise.
	*/
	public static boolean swapSvgzForHtml()
	{
		return IS_SWAP_SVG_FOR_HTML;
	}	
			
}	// end of DxfPreprocessor class