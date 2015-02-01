
/****************************************************************************
**
**	FileName:	HtmlWrapperBuilder.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Performs the complex activity of generating the HTML wrapper
**				files. The complexity comes about from forward and back buttons,
**				conditional language inclusion, and other JavaScript functionality.
**
**	Date:		November 14, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 14, 2003
**				0.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				1.0  - May 12, 2005 Added the wire list array so illustrators 
**				can trace the wires across sheets. 
**				1.1  - May 17, 2005 Changed everything so we could test x-sheet
**				wire runs.
**
**	TODO:		
**
*****************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.*;			// Vectors etc.
import java.util.regex.*;	// for patterns.
import dxf2svg.util.*;		

/**
*	This class performs the complex activity of generating the HTML wrapper
*	files. The complexity comes about from forward and back buttons,
*	conditional language inclusion, print and other JavaScript functionality.
*
*	@version 	1.1  - May 17, 2005
*	@author		Andrew Nisbet
*/
public class HtmlWrapperBuilder
{
	private final static double version			= 1.1;
		
	private final static int SVG 				= 1; // Which type of file do we need a name for
	private final static int SVGZ				= 2; // Compressed svg file.
	
	private LibraryCatalog library;				 	 // namespace container for books.
	private FigureSheetDatabase figDB;			 	 // Database of fig families.
	private String book;							 // Name space of search.
	private String fileName;						 // Converted file name without dxf or path.
	private String filePath;                         // Converted file's fully qualified path.
	
	// Referenced Conversion objects
	private SvgUtil svgUtility;						 // Needed for getMaximumHTMLViewportHeight()
	private StyleSheetGenerator SSG;				 // Style Sheet generator.
	private SvgBuilder svgBuilder;					 // Parent object, in case we need to SvgElement
													 // Information.
													 
	// Pattern required for searching the SVG for specific information if required.
	private Pattern ndidPattern;					 // For determining the namespace for searches.
	
	// Figure number.
	private String myFigureNumber;
	// Sheet number if required.
	private String mySheetNumber;
	// Figure title for the <title>tag</title>.
	private String englishTitle;
	private String frenchTitle;
	private String totalSheets;
	private boolean compileFigureTitles;
	private boolean libraryIsAvailable;
	/** Name of the default control panel frame. */
	public final static String buttonFrame = "controlPanel.html";
	
	
	/////////////////////////
	//                     //
	//     Constructor     //
	//                     //
	/////////////////////////
	/** Constructor requires the name of the file currently being converted. This
	*	is a fully qualified path and file name, but tests will be performed to 
	*	determine the root name of the file being converted.
	*/
	public HtmlWrapperBuilder(
		DxfConverter dxfc, 		// Conversion context.
		SvgBuilder parent		// Parent object. Used if we fail to find the information
								// in the database, because -group_families is not on, so 
								// we will need to go back to SvgBuilder to query the 
								// SvgElements for their content to get the boardno etc.
	)
	{
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
			"Svg object instantiation attempt: HtmlWrapperBuilder.");
			
		// Now get the conversion contextual relavent objects.
		svgUtility	= dxfc.getSvgUtil();			// conversion utility
		SSG 		= dxfc.getStyleSheetGenerator();// assign StyleSheetGenerator.
		library		= DxfPreprocessor.getLibraryCatalog(); // Database books.
		svgBuilder  = parent;	// So we can call the searchSvgContent() if required.
		// We need to do this if there is no database. This will occur if the -group_families
		// switch is not used. In that case we need to be able to pass these patterns to 
		// SvgBuilder and get it to search for content itself.
		if (library == null)
		{
			libraryIsAvailable = false;
		}
		else
		{
			ndidPattern	= Pattern.compile(DxfPreprocessor.ndidRegex);
			Vector ndidVector = new Vector();
			
			svgBuilder.searchSvgContent(ndidVector, ndidPattern);
			
			if (ndidVector.size() == 0)
			{
				// we didn't find an NDID so we can't lookup the namespace therefore no library.
				libraryIsAvailable = false;
			}
			else
			{
				// The name of the book will be the first element in the vector.
				book = (String)ndidVector.get(0);
				figDB = library.retrieveFigures(book);
				libraryIsAvailable = true;
			}
		}
		

		
		// This is the name of the dxf file being converted without any path or extension.
		filePath = dxfc.getFilePath();
		filePath = new File(filePath).getParent();
		fileName = dxfc.getFileName();
		fileName = DxfPreprocessor.getNormalizedFileName(fileName);
		myFigureNumber = new String();
		mySheetNumber = new String();
		englishTitle = new String();
		frenchTitle = new String();
		totalSheets = new String();
		if (libraryIsAvailable)
		{
			compileFigureTitles = true;
			if (isFigureDataInDatabase() == true)
			{
				// Create frameset and navigator
				createFrameSet();
			}	
		}  
		else  // libraryIsAvailable == false
		{
			compileFigureTitles = false;
		}
	}   // end of constructor.
	
	
	
	
	
	
	
	
	/** Creates a frameset for each figure. Caveat because this method is run
	*	each time a SVG graphic is output, it checks to see if the frameset 
	*	exists and if it does, it does it returns. If you want changes to
	*	be reflected in the frameset delete the frameset before converting
	*	the DXFs.
	*/
	protected void createFrameSet()
	{
		String frameSetName = "fig_" + myFigureNumber + ".html";
		File frameSet = new File(filePath, frameSetName);
		if ( frameSet.exists() )
		{
			return;
		} 
		
		// Make new Frame set for this figure.
		StringBuffer frameSetBuff = new StringBuffer();
		frameSetBuff.append("");
		frameSetBuff.append("<html><head><title>");
		frameSetBuff.append("Figure " + myFigureNumber + " " + englishTitle);
		frameSetBuff.append("</title><script language=\"JavaScript\">\n");
		frameSetBuff.append("  var highlightedWire = new Array();\n");
		frameSetBuff.append("  function printSvg(){\n");
		frameSetBuff.append("  	frames['svg'].window.print();\n");
		frameSetBuff.append("  }\n");
		frameSetBuff.append("  function changeSvgLanguage(lang){\n");
		frameSetBuff.append("  	frames['svg'].changeLanguage(lang);\n");
		frameSetBuff.append("  }\n");
		frameSetBuff.append("  function closeSvg(){\n");
		frameSetBuff.append("  	close();\n");
		frameSetBuff.append("  }\n</script></head><frameset rows=\"*,22,32\">");
		frameSetBuff.append("<frame name=\"svg\" src=\"");
		// add the html wrapper for the first sheet in the figure like: waaaogk.html
		frameSetBuff.append(getFirstSheetName());
		frameSetBuff.append("\" frameborder=\"0\"");
		frameSetBuff.append("marginheight=\"0\" marginwidth=\"0\" scrolling=\"auto\">");
		frameSetBuff.append("<frame name=\"nav\" src=\"");
		// add the html navigator for this figure like: nav_fig_2-10.html
		frameSetBuff.append(createNavigator());
		frameSetBuff.append("\" frameborder=\"0\" marginheight=\"0\" marginwidth=\"0\" scrolling=\"auto\">");
		frameSetBuff.append("<frame name=\"buttons\" src=\"controlPanel.html\" frameborder=\"0\"");
		frameSetBuff.append("marginheight=\"0\" marginwidth=\"0\" scrolling=\"auto\" noresize>");
		frameSetBuff.append("</frameset></html>");

		try
		{
			BufferedWriter bWriter = new BufferedWriter(
				new FileWriter(frameSet) );
			bWriter.write(frameSetBuff.toString());
			bWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("Error creating wrapper file frameset html file: " + e);
		}
		
		// creates the control panel which is a default for all framesets in this directory.
		createControlPanelHtml();
	}
	
	
	
	
	
	/** Returns the spotcall name of the first sheet of the figure. This is used to 
	*	embed the correct first sheet of a figure when the frameset for this figure opens.
	*	If the first sheet of a figure is a table, the application will continue searching
	*	for the first sheet that has a spotcall. If it fails to find one a message to that
	*	effect is emitted and a NullPointerException will be thrown.
	*	@throws NullPointerException if no sheets in the figure have spotcalls which is
	*	not possible when the application is run normally, however is someone has 
	*/
	protected String getFirstSheetName()
	{
		// Check for existance of library, if it isn't there, there is no way to find 
		// the first file.
		if (libraryIsAvailable == false)
		{
			return "";
		}
		// Get a record.
		//   key     index 0     index 1   index 2   index 3     index 4    index 5
		//   key     eBoardno   fBoardno   eTitle    fTitle	     spotcall   total Sht
		//+-------++----------+----------+---------+-----------+----------+----------+
		//|  "1"  || g01234ea | g01235fa | "Title" | "Fig nom" | faaabcd  |   "2"    |
		//+-------++----------+----------+---------+-----------+----------+----------+
		Vector record;
		
		record = figDB.getFigureData(myFigureNumber, String.valueOf(1));
			
		if (record == null)
		{
			String placeHolderFileName = "SGML_Figure" + myFigureNumber + ".html";
		
			File placeHolderFile = new File(filePath, placeHolderFileName);
		
			// create navigator window.
			StringBuffer placeHolderBuff = new StringBuffer();
		
			placeHolderBuff.append("<html><head><title>SGML Place holder</title></head><body><div align=\"center\">");
			placeHolderBuff.append("\n<h2>The data for this sheet in the IETM is generated by the SGML.</h2>");
			placeHolderBuff.append("\n</div></body></html>");
		
			try
			{
				BufferedWriter bWriter = new BufferedWriter(
					new FileWriter(placeHolderFile) );
				bWriter.write(placeHolderBuff.toString());
				bWriter.close();
			}
			catch (IOException e)
			{
				System.err.println("Error creating place holder for first sheet of figure for frameset.: " + e);
			}
			
			return placeHolderFileName;
		}

		return (String)record.get(4) + ".html";
	}
	
	
	
	
	// Creates the navigator pane for the frameset and returns the name of the navigator.
	protected String createNavigator()
	{
		String navName = "nav_fig_" + myFigureNumber + ".html";
		
		File navigatorName = new File(filePath, navName);
		if (navigatorName.exists())
		{
			return navName;
		}
		
		// create navigator window.
		StringBuffer navigatorBuff = new StringBuffer();
		
		navigatorBuff.append("<html><head></head><body><div align=\"center\">");
		navigatorBuff.append("\n" + getSheetAnchors());
		navigatorBuff.append("\n</div></body></html>");
		
		try
		{
			BufferedWriter bWriter = new BufferedWriter(
				new FileWriter(navigatorName) );
			bWriter.write(navigatorBuff.toString());
			bWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("Error creating navigator for frameset.: " + e);
		}		
		
		return navName;
	}






	/** This method will output a list of anchors that are links to all the sheets
	*	of this figure without regard to language. Used when creating a frameset.
	*/
	protected String getSheetAnchors()
	{
		// Check for existance of library, if it isn't there, there is no way to find 
		// the last file.
		if (libraryIsAvailable == false)
		{
			return "";
		}
		
		// find the total number of sheets expected.
		int totalNum = 1;
		try
		{
			totalNum = Integer.parseInt(totalSheets);
		}
		catch (NumberFormatException e)
		{
			return "";
		}
		
		if (totalNum <= 1)
		{
			return "";
		}
		
		
		StringBuffer sb = new StringBuffer();
		
		Vector record = new Vector();
		
		sb.append("Sheets ");
		
		// This will hold the HREF attrib value
		String anchorTarget = new String();
		
		// Now figure out each anchor.
		for (int i = 1; i <= totalNum; i++)
		{
			// Get the ith record
			record = figDB.getFigureData(myFigureNumber, String.valueOf(i));
			if (record == null)
			{
				continue;
			}
			
			// get the spotcall
			anchorTarget = (String)record.get(4);
			
			// this forces the collected anchor name to be used.
			anchorTarget = anchorTarget + ".html";
			
			sb.append("<a href=\""+anchorTarget+"\" target=\"svg\">");
			sb.append(String.valueOf(i));
			sb.append("</a>");
			// now some formatting of pipes to separate entries
			if (i < totalNum)
			{
				sb.append("&nbsp;|&nbsp;");
			}
		}  // end for
		
		return sb.toString();
	} // end getSheetAnchors()
	


	
	
	/** This method will create the template html required for every directory where
	*	svgs are converted. It creates the controlPanel.html file specifically. Only
	*	one of these is created per directory and referenced in all index_*.html 
	*	frameset.
	*/
	protected void createControlPanelHtml()
	{
		File controlPanel = new File(filePath, buttonFrame);
		if ( controlPanel.exists() )
		{
			return;
		}
		
		StringBuffer controlPanelHtml = new StringBuffer();
		controlPanelHtml.append("<html><head><title>Control Panel</title></head><body><div align=\"center\">");
		controlPanelHtml.append("<table cellpadding=2><tr>");
		controlPanelHtml.append("<td><button onclick=\"parent.printSvg()\">Print / Imprimer</button></td>");
		// conditionally add language switching buttons.
		if (SSG.isLangSwitchRequired() == true)
		{
			controlPanelHtml.append("<td><button onclick=\"parent.changeSvgLanguage(0)\">English</button></td>");
			controlPanelHtml.append("<td><button onclick=\"parent.changeSvgLanguage(1)\">Français</button></td>");
		}
		controlPanelHtml.append("<td><button onclick=\"parent.closeSvg()\">Close / Fermé</button></td>");
		controlPanelHtml.append("</tr></table></div></body></html>");
		
		try
		{
			BufferedWriter bWriter = new BufferedWriter(
				new FileWriter(controlPanel) );
			bWriter.write(controlPanelHtml.toString());
			bWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("Error creating control panel wrapper file: " + e);
		}
	}
	
	
	
	
	
	

	
	//////////////////////////////
	//                          //
	//     Public Methods       //
	//                          //
	//////////////////////////////


	
	/** Outputs version information and credits. To output the HTML use {@link #writeHtmlWrapper}
	*/
	public String toString()
	{
		return this.getClass().getName()+" version "+version+" (C) 2003.";
	}
	
	
	/** Outputs the formatted HTML wrapper to file.
	*/
	public void writeHtmlWrapper()
	{
		StringBuffer outBuff = new StringBuffer();
		
		// First append the preliminaries of any html page
		outBuff.append("<html>\n<head>\n<title>\n");
		outBuff.append(getTitle());
		outBuff.append("</title>");
		outBuff.append("</head>");
		outBuff.append("<body>");
		outBuff.append("<div align=\"center\">");
		outBuff.append("<embed width=");
		outBuff.append(String.valueOf(svgUtility.getMaximumHTMLViewportWidth()));
		outBuff.append(" height=");
		outBuff.append(String.valueOf(svgUtility.getMaximumHTMLViewportHeight()));
		outBuff.append(" src=\"");
		outBuff.append(getEmbedSrcTarget());
		outBuff.append("\" type=\"image/svg+xml\"></embed>");
		outBuff.append("</div>");	
		outBuff.append("</body>");
		outBuff.append("</html>");
		
		
		File htmlFile = new File(filePath, fileName + ".html");
		try
		{
			BufferedWriter bWriter = new BufferedWriter(
				new FileWriter(htmlFile) );
			bWriter.write(outBuff.toString());
			bWriter.close();
		}
		catch (IOException e)
		{
			System.err.println("Error creating wrapper html: " + e);
		}
	}	
	
	
	
	
	
	///////////////////////////////
	//                           //
	//     protected Methods     //
	//                           //
	///////////////////////////////	
	/** This returns the title of the illustration for the &lt;title&gt; tag.
	*/
	protected String getTitle()
	{
		// If the illustrator removed the title prior to conversion:
		if (englishTitle == null || englishTitle.equals(""))
		{
			// Use the converted figure name as a title.
			return getEmbedSrcTarget();
		}
		
		int numSheets;
		
		try
		{
			numSheets = Integer.parseInt(totalSheets);
		}
		catch (NumberFormatException e)
		{
			numSheets = 1;
		}
		
		return "Figure " + myFigureNumber + " sheet " + mySheetNumber+" " + englishTitle;
	}
	
	
	
	
	/** Creates a the correctly named file for a target, as in SRC='target'.
	*/
	protected String getEmbedSrcTarget()
	{
		String name = fileName;
		
		if (DxfPreprocessor.isZipped())
		{
			name = name.concat(".svgz");
		}
		else
		{
			name = name.concat(".svg");
		}

		return name;
	}


	
	
	/** This method populates the fields from data stored in the FigureSheetDatabase.
	*	@throws BoardNumberNotFoundException if boardnos are to be used for wrapper names but could
	*	not be found in the conversion file.
	*	@throws FigureSheetDatabaseRecordSizeMismatch if the database lookup retreives a 
	*	a record that has either too many or too few fields when compared to 
	*	{@link FigureSheetDatabase#NUM_FIELDS}.
	*/	
	protected boolean isFigureDataInDatabase()
	{
		// Now we will populate everything we need to know about this illustration and 
		// any wrapper file dependant information. Things we need to know are:
		// 1) boardnos for this illustration.
		// 2) Titles for french and english.
		// 2) wrappers for the next sheet if any (both languages).
		// 3) wrappers for previous sheet if any (both languages).
		// 4) figure title for the <title>tag</title>.
		
		//   key     index 0     index 1   index 2   index 3     index 4    index 5
		//   key     eBoardno   fBoardno   eTitle    fTitle	     spotcall   total Sht
		//+-------++----------+----------+---------+-----------+----------+----------+
		//|  "1"  || g01234ea | g01235fa | "Title" | "Fig nom" | faaabcd  |   "2"    |
		//+-------++----------+----------+---------+-----------+----------+----------+
		String[] figSheet 	= new String[2];
		figSheet 			= figDB.findFigureAndSheetByValue(fileName);
		
		// It may happen that there isn't a field that matches our file name.
		// This can happen if there are two files being converted with the same figure and
		// sheet number. The second will over-write the first record.
		if (figSheet == null)
		{
			// get this data from the SVG file instead.
			// Since we have to get this from the SVG we will use the figure title
			// as it is as a title for the html wrapper.
			compileFigureTitles = false;
			//getDataDBIsNull();
			return false;
		}
		myFigureNumber 		= figSheet[0];
		mySheetNumber  		= figSheet[1];

		Vector record 		= new Vector();
		record 				= figDB.getFigureData(myFigureNumber, mySheetNumber);
		
		// We hope the client of this object is using correctly sized fields 
		// or we will get a more mysterious ArrayIndexOutOfBoundsException
		// by referencing an object at an index that does not exist in the Vector.
		if (record.size() != FigureSheetDatabase.NUM_FIELDS)
		{
			throw new FigureSheetDatabaseRecordSizeMismatch(
				myFigureNumber,	mySheetNumber, record.size());
		}
		
		englishTitle		= (String)record.get(2);
		frenchTitle			= (String)record.get(3);
		String spotCall    	= (String)record.get(4); // not used here.
		totalSheets			= (String)record.get(5);
		
		return true;
	}  // getDataDBIsNotNull()
	

	
	
	/////////////////////////////////
	//                             //
	//      Exception Classes      //
	//                             //
	/////////////////////////////////
	
	
	/** This exception gets thrown if there is a surplus or deficit of fields in the current
	*	record.
	*/
	public class FigureSheetDatabaseRecordSizeMismatch extends RuntimeException
	{
		public FigureSheetDatabaseRecordSizeMismatch(String fig, String sheet, int size)
		{
			System.err.println("expected a record with: "+FigureSheetDatabase.NUM_FIELDS+
				" fields, but got a record with "+size+" fields in figure "+fig+", sheet "+
				sheet+".");
		}
	} // end FigureSheetDatabaseRecordSizeMismatch class
} // end class HtmlWrapperBuilder