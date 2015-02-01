
/****************************************************************************
**
**	FileName:	Dxf2Svg.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Contains the main() function and methods for returning
**				user preferences.
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.2 - August 20, 2002
**				1.0 - February 8, 2005 Added '-update_db_only' switch to
**				allow users to just update the boardno-control.xml database
**				without re-running the graphics conversion.
**				1.1 - March 10, 2005 Added new switch -collaborate. Can
**				be called repeatedly.
**				1.2 - May 18, 2005 Removed boardno_wrapper_names switch.
**				1.21 - August 28, 2005 Added new switch to swap .svgz links for .html wrappers.
**
**	TODO:		Add GUI
**
*****************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.Vector;	// See '-ol' switch 
import dxf2svg.util.*;
import dxf2svg.sally.*;		// sally language.
import dxf2svg.svg.Point;	// For setting the fuzz value of all points.

/**
*	This is the entry point into the application.
*
*	It reads the command line arguments and sets up user preferences
*	that will be used all through the rest of the conversion process.
*	If there are no command line arguments it starts the Graphical User
*	Interface (here after refered to as 'the GUI').
*
*	There is a plethora of switches to customize various aspects of the
*	conversion use the <B>-h</B> switch to see them all.
*
*	@version	1.1 - March 10, 2005
*	@author		Andrew Nisbet
*/


public final class Dxf2Svg
{

	private boolean VERBOSE;			// Verbose messaging mode.
	private String PATH;				// Path to the file to process
	private File IN;					// Name of file to process or Dir
	private String License;				// name of the licensing text
	private boolean SeenUsageMsg;		// limits usage display to once.
	private String SparCss;				// name of spar CSS
	private double MINIMUM_VERSION;		// Minimum version to run correctly.
	// This is the default directory for all conversion in the Spar world
	private static final String defaultPath = ".";
	protected static boolean isConfigFileConversion;
	
		
	// This is the global var of the name of the config file. It is used so that
	// the application can be run with no command arguments.
	private static String CONFIG_FILE = "config.d2s";
	// 
	private SvgAnimationLanguage sal;
	private DxfPreprocessor pprocessor;




	////////////////////////////////////////////////////////////////////////////////////////
	//							Constructor
	////////////////////////////////////////////////////////////////////////////////////////
	public Dxf2Svg(String[] commandArgs)
	{
		MINIMUM_VERSION = 1.4;
		// Test if the user has got the correct version of Java.
		if (isValidJavaVersion(MINIMUM_VERSION) == false)
		{
			System.err.println("\n///////////////////////////////////////////////////////////////");
			System.err.println("//");
			System.err.println("// You are currently running Java version '"+
				System.getProperty("java.version")+"'; you need");
			System.err.println("// version "+
			MINIMUM_VERSION +
			" or higher. See documentation "+
			"overview for");
			System.err.println("// details on getting the latest version of Java. Exiting.");
			System.err.println("//");
			System.err.println("///////////////////////////////////////////////////////////////");
			System.exit(-1);
		}
		
		
		// Preset variables
		VERBOSE = false;
		License = "docs/license/license.txt";
		SeenUsageMsg = false;		
		SparCss = "spar.css";
		String[] args;			// Command line argument array.
		// if we don't get one we will try to find and populate it from a config file.
		

		
		
		// check to see if there are args passed to Dxf2Svg and if not then 
		// read the config file to find out what to do.
		if (commandArgs.length == 0)
		{
			// This will launch the gui instead.
			System.out.println("Looking for configuration file...");
			File d2sConfig = new File(CONFIG_FILE);
			if (! d2sConfig.isFile())
			{
				new Dxf2SvgGui(Dxf2SvgGui.MISSING_CONFIG_FILE, CONFIG_FILE);
				return;
			}
			
			// Create the SAL language object for future processing
			sal = new SvgAnimationLanguage(CONFIG_FILE);
			args = sal.getKeys("setup");
			// Now we can create a new PreProcessor object with the sal
			// functionality rather than command line.
			pprocessor = new DxfPreprocessor(sal);
			isConfigFileConversion = true;
		}	
		else if (commandArgs.length == 1)  // a config file of a non-default name
		{
			if (commandArgs[0].endsWith(".d2s"))
			{
				CONFIG_FILE = commandArgs[0];
				// this is a non-default-named config.d2s file.
				if (! new File(CONFIG_FILE).isFile())
				{
					new Dxf2SvgGui(Dxf2SvgGui.MISSING_CONFIG_FILE, CONFIG_FILE);
					return;
				}
				// Create the SAL language object for future processing
				sal = new SvgAnimationLanguage(CONFIG_FILE);
				args = sal.getKeys("setup");
				// Now we can create a new PreProcessor object with the sal
				// functionality rather than command line.
				isConfigFileConversion = true;
				pprocessor = new DxfPreprocessor(sal);
				
			}  
			else
			{
				args = commandArgs; 
				isConfigFileConversion = false;
				pprocessor = new DxfPreprocessor();
			}
			// else carry on...
		}  // end if
		else 	// Use the switches from the command line.
		{
			args = commandArgs;
			isConfigFileConversion = false;
			pprocessor = new DxfPreprocessor();
		}
		
		
		
		// if we made it here then we can start the version is correct and we
		// have read the contents of the config file.
		String ThisArg = new String();
		
		
		
		// Leave this as a try block so if we check i++ and we are at the end of the array...
		try
		{
			// read through the args passed
			for(int i = 0; i < args.length; i++)
			{
				ThisArg = args[i];
				if (ThisArg.equals("-v"))
				{
					//System.out.println(APPLICATION+": review mode selected.");
					VERBOSE = true;
					pprocessor.setVerboseMode(true);
				}
				else if (ThisArg.equals("-DEBUG"))
				{
					pprocessor.setDebugMode(true);
				}
				else if (ThisArg.equals("-r"))
				{
					pprocessor.setRecursiveMode(true);
				}
				///////// Screen Resolution ///////////
				else if (ThisArg.equals("-s"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<screen_size>");
					}
					// 1024 x 768
					if (tmp.startsWith("1"))
						pprocessor.setRenderSize(Dxf2SvgConstants.TENTWENTYFOUR_X_SEVENSIXTYEIGHT);
					// 640 x 480
					else if (tmp.startsWith("6")||tmp.equalsIgnoreCase("vga"))
						pprocessor.setRenderSize(Dxf2SvgConstants.SIXFORTY_X_FOUREIGHTY);
					// 800 x 600
					else if (tmp.startsWith("8"))
						pprocessor.setRenderSize(Dxf2SvgConstants.EIGHT_HUNDRED_X_SIX_HUNDRED);
					// User doesn't know so use current
					else if (tmp.startsWith("c"))
						pprocessor.setRenderSize(Dxf2SvgConstants.CURRENT_RESOLUTION);
					// User is confused just use default.
					else
						// resort to the IETM specification screen resolution (800x600)
						pprocessor.setRenderSize(Dxf2SvgConstants.BEST_GUESS);
				}
				////////////////////////////////
				else if (ThisArg.equals("-css"))
				{
					String tmp = args[++i];


					// we have to cycle through a series of command line controls related
					// to this switch. All the arguments to be process as a CSS must end
					// with a comma.
					pprocessor.setCssMode(-1); // we are going to set some value so reset it now.
					do{
						if (tmp.startsWith("external"))
							pprocessor.setCssMode(Dxf2SvgConstants.EXTERNAL_CSS);
						else if (tmp.startsWith("inline"))
							pprocessor.setCssMode(Dxf2SvgConstants.INLINE_STYLES);
						else if (tmp.startsWith("only"))
							pprocessor.setCssMode(Dxf2SvgConstants.CSS_ONLY);
						else if (tmp.startsWith("declared"))
							pprocessor.setCssMode(Dxf2SvgConstants.DECLARED_CSS);
						else if (tmp.startsWith("spar_c130"))
							pprocessor.setCssMode(Dxf2SvgConstants.SPAR_C130);
						else if (tmp.startsWith("spar_only"))
						{
							pprocessor.setCssMode(-1);	// reset the css if any set
							pprocessor.setCssMode(Dxf2SvgConstants.SPAR_C130);	// request SPAR_C130 styles
							pprocessor.setCssMode(Dxf2SvgConstants.CSS_ONLY);
							pprocessor.makeDefaultStyleSheet();

							if (VERBOSE)
								System.out.println("Dxf2Svg: Spar CSS created.");
							return;
						}	// if it is not one of these cases the arg is consumed and ignored.
						i++;
						tmp = args[i];	// if this goes off the end of the args list processing 
						//will start.
						if (tmp.startsWith("-"))
						{
							// if user entered another switch
							throw new IncompleteSwitchSettingException(ThisArg, "<css_keyword,>");
						}
					} while(tmp.endsWith(",")); // if there is a ',' at the end it means there
						// are additional values to parse.
					// if not then back up or we consume one too many arguments.
					i--;
				}
				else if (ThisArg.equals("-js"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "[<keyword>|<path>]");
					}

					if (tmp.equalsIgnoreCase("internal"))
						pprocessor.setIncludeJavaScript(Dxf2SvgConstants.INTERNAL_SCRIPT);
					else if (tmp.equalsIgnoreCase("none"))
						pprocessor.setIncludeJavaScript(Dxf2SvgConstants.NO_SCRIPT);
					else
					{
						// Assume that the next arg is a path to the javascript
						// if the user doesn't read carefully and places another
						// switch next we have to catch it and or if the string
						// provided doesn't end with '.js' also set value to INTERNAL.
						if (tmp.endsWith(".js"))
						{
							pprocessor.setIncludeJavaScript(Dxf2SvgConstants.EXTERNAL_SCRIPT);
							pprocessor.setIncludeJavaScriptSrcPath(tmp);
						}
						else  // default action
						{
							System.err.println(Dxf2SvgConstants.APPLICATION+": Warning invalid JavaScript file name.");
							System.err.println(Dxf2SvgConstants.APPLICATION+": Requires a file with a '.js' extension.");
							System.err.println(Dxf2SvgConstants.APPLICATION+": adding tags but svg requires editing.");
							pprocessor.setIncludeJavaScript(Dxf2SvgConstants.INTERNAL_SCRIPT);
						}
					}
				}
				else if (ThisArg.equals("-dtd"))
				{
					pprocessor.setIncludeDTD(true);
				}
				else if (ThisArg.equals("-h"))
				{
					help();
				}
				else if (ThisArg.equals("-z"))
				{
					pprocessor.setZip(true);
				}
				else if (ThisArg.equals("-c"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<(int)coerce_colour>");
					}
					
					try{
						pprocessor.setCoerciveColour(Integer.parseInt(tmp));
					}	catch (NumberFormatException e){
						throw new NumberFormatException("expected integer value for coercive colour.");
					}
				}
				else if (ThisArg.equals("-fuzz"))  // sets the fuzz value for point proximity testing.
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<(double)fuzz_value>");
					}
					try{
						Point.setFuzz(Double.parseDouble(tmp));
					}	catch (NumberFormatException e){
						throw new NumberFormatException("expected double value for fuzz value.");
					}
				}
				else if (ThisArg.equals("-p"))
				{
					try
					{
						int precision = Integer.parseInt(args[++i]);
						// now restrict precision to sensible values
						if (precision < 1)
						{
							System.err.println(Dxf2SvgConstants.APPLICATION+" warning: precision argument out of range, reset to 1 decimal place.");
							pprocessor.setPrecision(1);
						}
						else if (precision > 9)
						{
							System.err.println(Dxf2SvgConstants.APPLICATION+" warning: precision argument out of range, reset to 10 decimal place.");
							pprocessor.setPrecision(10);
						}
						else
						{
							pprocessor.setPrecision(precision);
						}
					}
					catch (NumberFormatException e)
					{
						System.err.print(Dxf2SvgConstants.APPLICATION+" warning: precision argument must be an integer.");
						System.err.println(" Defaulting to 2 decimal places.");
						pprocessor.setPrecision(2);
					}
				}
				else if (ThisArg.equals("-f"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<file.dxf>");
					}

					pprocessor.setFileName(tmp);
				}
				else if (ThisArg.equals("-u"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<font_URL>");
					}

					pprocessor.includeFontUrl(tmp);
				}
				else if (ThisArg.equals("-version"))
				{
					System.out.println();
					System.out.println(Dxf2SvgConstants.APPLICATION+": version " + Dxf2SvgConstants.VERSION);
					System.out.println(Dxf2SvgConstants.APPLICATION+" comes with ABSOLUTELY NO WARRANTY");
					System.out.println("This is free software and protected by the Gnu Public License.");
					System.out.println("You are welcome to use it and redistribute it under the conditions");
					System.out.println("of the GPL; use `-l' switch for details.\n");
				}
				else if (ThisArg.equals("-l"))
				{
					new ShowLicense(License);
				}
				else if (ThisArg.equals("-xh"))
				{
					pprocessor.setHTMLWrappers(false);
				}
				else if (ThisArg.equals("-line_cap"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another
						// switch lets go back an argument.
						--i;
						continue;
					}
					// butt
					if (tmp.startsWith("b"))
						Pen.setLineCapType(Pen.LINECAP_BUTT);
					// round
					else if (tmp.startsWith("r"))
						Pen.setLineCapType(Pen.LINECAP_ROUND);
					// square
					else if (tmp.startsWith("s"))
						Pen.setLineCapType(Pen.LINECAP_SQUARE);
					else if (tmp.startsWith("d"))
						Pen.setLineCapType(Pen.DEFAULT);
					// else don't do anything. If the user entered the command but the value
					// is invalid then we leave the line cap value at what ever it was.
				}
				else if (ThisArg.equals("-line_join"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another
						// switch lets go back an argument.
						--i;
						continue;
					}
					// butt
					if (tmp.startsWith("m"))
						Pen.setLineJoinType(Pen.LINEJOIN_MITER);
					// round
					else if (tmp.startsWith("r"))
						Pen.setLineJoinType(Pen.LINEJOIN_ROUND);
					// square
					else if (tmp.startsWith("b"))
						Pen.setLineJoinType(Pen.LINEJOIN_BEVEL);
					else if (tmp.startsWith("d"))
						Pen.setLineJoinType(Pen.DEFAULT);						
					// else don't do anything. If the user entered the command but the value
					// is invalid then we leave the line cap value at what ever it was.
				}
				else if (ThisArg.equals("-db"))
				{
					String tmp = args[++i];
					if (tmp.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<path>");
					}

					pprocessor.syncDatabase(tmp);
				}
				else if (ThisArg.equals("-IETM")) 
				{
					// set all the switches necessary to process graphics in current directory
					pprocessor.setRenderSize(Dxf2SvgConstants.EIGHT_HUNDRED_X_SIX_HUNDRED);
					// Use the 'penTable' settings from config.d2s.
					pprocessor.setCoerciveColour(7);
					pprocessor.setIncludeDTD(true);
					pprocessor.setFileName(defaultPath);
					pprocessor.setHTMLWrappers(true);		// or we don't get language switching
					//pprocessor.setZip(true);
					pprocessor.syncDatabase(defaultPath);
					//pprocessor.setSuppressBoilerPlateJavaScriptFlag(true);
				}
				else if (ThisArg.equals("-MY")) 
				{
					i++;
					String attrib = args[i];
					if (attrib.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<parts list attribute>");
					}
					
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<parts list attribute value>");
					}					
					pprocessor.setPartsListAttribute(attrib, value);					
				}
				else if (ThisArg.equals("-notes"))
				{
					pprocessor.setGenerateNotes(true);
				}
				else if (ThisArg.equals("-english_note_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setNoteLayerName(Dxf2SvgConstants.ENGLISH, value);						
				}
				else if (ThisArg.equals("-french_note_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setNoteLayerName(Dxf2SvgConstants.FRENCH, value);						
				}
				else if (ThisArg.equals("-english_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setLanguageLayerName(Dxf2SvgConstants.ENGLISH, value);						
				}
				else if (ThisArg.equals("-french_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setLanguageLayerName(Dxf2SvgConstants.FRENCH, value);						
				}
				else if (ThisArg.equals("-note_number_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setNoteNumberLayerName(value);						
				}
				else if (ThisArg.equals("-default_target_note_layer_name"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<layer name>");
					}
					
					TableLayer.setDefaultLanguageLayerName(value);						
				}
				else if (ThisArg.equals("-suppress_default_js"))
				{
					pprocessor.setSuppressBoilerPlateJavaScriptFlag(true);
				}
				else if (ThisArg.equals("-include"))
				{
					i++;
					String value = args[i];
					if (value.startsWith("-"))
					{
						// if user entered another switch
						throw new IncompleteSwitchSettingException(ThisArg, "<include file name>");
					}
					
					pprocessor.addIncludeFileName(value);						
				}
				else if (ThisArg.equals("-use_dxf_object_colour"))
				{
					// This allows the conversion to ignore specific colour request for specific
					// objects. The user can change an object's colour in AutoCAD by setting a 
					// colour to a value other than the layer the object is put on. You wouldn't 
					// think that that would be too common, but it is big-time in blocks.
					pprocessor.setColourCoercedByLayer(false);						
				}
				else if (ThisArg.equals("-update_db_only"))
				{
					// This allows the user to update the database without reconverting the
					// dxf files to svg. This speeds things up when you just need the information
					// from the graphics for catalogs etc. Added Feb. 8, 2005.
					pprocessor.setUpdateDatabaseOnly(true);						
				}
				else if (ThisArg.startsWith("-collaborate"))
				{
					// this switch can be called repeatedly to add more layers to the list
					// of layers requiring collaborative functionality.
					String tmp = args[++i];
					pprocessor.registerCollaboratorTarget(tmp, "true");
				}
				else if (ThisArg.equals("-swap_svg_link_for_html_link"))
				{
					pprocessor.setSwapSvgForHtml(true);
				}
				else
				{
					System.err.println(Dxf2SvgConstants.APPLICATION+": Ignoring unsupported option: \"" + ThisArg + "\"");
					if ( ! SeenUsageMsg)
					{
						System.err.println("Usage: "+Dxf2SvgConstants.APPLICATION+" [-hlvr] [-f foo/bar.Dxf] [-DEBUG]");
						System.err.println("\t[-version] [-css only|external|declared|custom|spar_c130|spar_ext]");
						System.err.println("\t[-p n] [-dtd] [-xh] [-js foo/bar.js|internal|none] -c num");
						System.err.println("\t[-s [[current]|[vga|640x480]|800x600|1024x768]]");
						System.err.println("\t[-line_cap [butt|round|square]] [-line_join [round|bevel|miter]]");
						System.err.println("\t[-boardno_wrapper_names][-db <path>][-fuzz <double>]");
						System.err.println("\t[-IETM][-MY <attribute> <attribute_value>][-notes]");
						System.err.println("\t[-english_note_layer_name <layer_name>]");
						System.err.println("\t[-french_note_layer_name <layer_name>]");
						System.err.println("\t[-english_layer_name <layer_name>]");
						System.err.println("\t[-french_layer_name <layer_name>]");
						System.err.println("\t[-note_number_layer_name <layer_name>]");
						System.err.println("\t[-default_target_note_layer_name <layer_name>]");
						System.err.println("\t[-suppress_default_js][-use_dxf_object_colour]");
						System.err.println("\t[-inlcude <fileName> ...][-update_db_only]");
						System.err.println("\t[-collaborate <layer_name>][-swap_svg_link_for_html_link]");
						SeenUsageMsg = true;
					}  // end if
				}  // end else
			}
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			throw new IncompleteSwitchSettingException();
		}

		pprocessor.activateProcessor();
	}	// end constructor
	





	////////////////////////////////////////////////////////////////////////////////////////
	//							Application main()
	////////////////////////////////////////////////////////////////////////////////////////
	/** Dxf2Svg converts AutoDesk's open Drawing Exchange Format (DXF) to Scalable Vector Graphic
	*	(SVG). The application is primarily a command line tool, so if you are not familiar with
	*	using this type of tool, read the over view documents for more information.
	*/
	public static void main(String[] args)
	{	new Dxf2Svg(args);	}	

	

	
	
	
	/** Displays a usage message and command line argument syntax. */
	protected void help()
	{
		System.out.println("\nUsage: "+Dxf2SvgConstants.APPLICATION+" [options]");
		System.out.println();
		System.out.println(Dxf2SvgConstants.APPLICATION+" parses a valid Dxf file and converts it");
		System.out.println("into a Scalable Vector Graphic (SVG).");
		System.out.println();
		System.out.println("Valid options are:");
		System.out.println("------------------");
		System.out.println("'-v' verbose mode; parses and reports on the content of the files.");
		System.out.println("'-DEBUG' provides debug messages and includes dxf handles as attribute ids in SVG.");
		System.out.println("'-h' prints this message.");
		System.out.println("'-c' coerce all colours to the following arguments value.");
		System.out.println("'-r' process DXF files in subdirectories.");
		System.out.println("'-js' include JavaScripting.");
		System.out.println("   'foo/bar.js' add SRC attrib with this path to <script>.");
		System.out.println("   'internal' add tags for user to write in JavaScript code.");
		System.out.println("   'none' do not include JavaScript (default action of "+Dxf2SvgConstants.APPLICATION+").");
		System.out.println("'-f' the next argument will be the source DXF file or directory.");
		System.out.println("'-l' show the licencing text."); // Currently GPL
		System.out.println("'-s' match rendering to display resolution.");
		System.out.println("   '6[40x480]' VGA");
		System.out.println("   '8[00x600]' Default");
		System.out.println("   '1[024x768]'");
		System.out.println("   'c[urrent]' If you don't know "+Dxf2SvgConstants.APPLICATION+" can determine");
		System.out.println("   setting for this machine. May not match target computer displays.");
		System.out.println("'-css' style sheet implementation.");
		System.out.println("   The following commands are cummulative and multipule options may be entered.");
		System.out.println("   You must enter a location (default 'declared') and then a custom type if you wish.");
		System.out.println("   Entering 'declared' and 'external' will create an external CSS and a declared CSS");
		System.out.println("   in the converted SVG. Entering 'declared' and 'element' will create both CSSs in the SVG.");
		System.out.println("   All arguments for this switch need to be separated with a comma ','.");
		System.out.println("   Be careful, some CSS combinations create redundant CSS.");
		System.out.println("   Note: All custom line weights are assumed to be in inches.");
		System.out.println("   'external' make CSS in css/svg.css if possible.");
		System.out.println("   'declared' element attributes as entity declarations; default.");
		System.out.println("   'element' as element attributes.");
		System.out.println("   'only' 	make 'svg.css' in graphic's directory and exit.");
		System.out.println("   'spar_c130' make internal CSS with spar c130 line weights, layer colours");
		System.out.println("   'spar_only' make a CSS with standard Spar pen weights bylayer and exit.");
		System.out.println("'-p' controls precision of calculation to 'n' decimal places; default: 2.");
		System.out.println("'-xh' suppresses the output of HTML wrappers for each converted SVG file.");
		System.out.println("'-u' include font URL in Cascading Style Sheets.");
		System.out.println("'-dtd' include DTD in SVG header.");
		System.out.println("'-line_cap' Explicitly set line cap type. Valid line caps are:");
		System.out.println("   'b[utt]', 's[quare]' or 'r[ound]'.");
		System.out.println("'-line_join' Explicitly set line join type. Valid line joins are:");
		System.out.println("   'm[iter]', 'r[ound]' or 'b[evel]'. The DXF file will over-ride '-line_join' if");
		System.out.println("   the DXF variable $JOINSTYLE has been set to a non-zero value.");
		System.out.println("'-boardno_wrapper_names' Names the HTML wrapper files after the illustration's boardno names.");
		System.out.println("'-db <path>' Searches DXF source files in a directory for family members (sheet 1,2,3 etc.)");
		System.out.println("   and adds appropriate data into a database. The database is used to add links to next");
		System.out.println("   graphic in HTML wrappers and to output as XML for IETM.");
		System.out.println("'-fuzz' <double> Sets the tolerance in inches for Point proximity testing. Default 0.088\"");
		System.out.println("'-IETM' Sets all switches required to convert graphics for the CC130 IETM; see documentation.");
		System.out.println("'-MY <event> <funcName>' Detects part numbers and uses them as arguments for ");
		System.out.println("   event=\"funcName('001')\"; see documentation.");
		System.out.println("'-notes' Compiles notes from all sheets of a figure and places them in the SVG output as JavaScript arrays.");		
		System.out.println("   for reference in each sheet of an illustration (like say, tooltips).");
		System.out.println("'-english_note_layer_name <layer_name>' Name of the layer for English notes. Default 'engnotes'.");		
		System.out.println("'-french_note_layer_name <layer_name>' Name of the layer for French notes. Default 'frenotes'.");		
		System.out.println("'-english_layer_name <layer_name>' Denotes the layer name for English text.");		
		System.out.println("'-french_layer_name <layer_name>' Denotes the layer name for French text.");
		System.out.println("'-note_number_layer_name <layer_name>' Layer that the note numbers can be found on.");
		System.out.println("'-default_target_note_layer_name <layer_name>' Default target layer.");
		System.out.println("'-suppress_default_js' Suppresses automatic inclusion of default javascript functions.");
		System.out.println("'-include <fileName> ... Includes fileName as a child(ren) of the <svg> element. Repeatable.");
		System.out.println("'-use_dxf_object_colour' Renders objects by either their user specified colour or layer colour.");
		System.out.println("   Default is to render all object's BYLAYER even if they have a user specified colour.");
		System.out.println("'-update_db_only' Updates the boardno-control.xml database from the graphics only;");
		System.out.println("   does not convert the graphics to SVG.");
		System.out.println("'-collaborate <layer_name>' Joints separate lines and arcs into");
		System.out.println("   a single polyline. See documentation for more details.");
		System.out.println("'-swap_svg_link_for_html_link' changes the link from an svg(z) target");
		System.out.println("   to an html target of the same name.");
		System.out.println();
	}	// end Help()


	// This checks for a valid version of Java. It does this by converting the major
	// and minor number for this machine's JRE and compares it with the minimum Java
	// version.
	private boolean isValidJavaVersion(double version)
	{
		String jVersion = System.getProperty("java.version");
		String jVerDouble;

		int pos = jVersion.lastIndexOf(".");
		if (pos < 0)
			return false;

		jVerDouble = jVersion.substring(0,pos);
		double testVal = Double.parseDouble(jVerDouble);
		if (testVal < version)
			return false;

		return true;
	}	// end isValidJavaVersion()



	/** Warns that switch denoted by <em>'arg'</em> requires an additional argument which
	*	was not supplied.
	*/
	protected class IncompleteSwitchSettingException extends RuntimeException
	{
		protected IncompleteSwitchSettingException()
		{
			if (isConfigFileConversion)
			{
				new Dxf2SvgGui(Dxf2SvgGui.INCOMPLETE_CMD_LINE_SWITCH, "<last_arg>");
			}
			else
			{
				System.err.println("Error: the last command line switch entered is incomplete.");
			}
		}
		
		protected IncompleteSwitchSettingException(String arg, String expected)
		{
			if (isConfigFileConversion)
			{
				new Dxf2SvgGui(Dxf2SvgGui.INCOMPLETE_CMD_LINE_SWITCH, arg);
			}
			else
			{
				System.err.println("Error: the '"+arg+"' switch expects an additional argument of "+
				expected + ". Exiting.");
			}
		}
	}
}