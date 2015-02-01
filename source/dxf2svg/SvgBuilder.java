
/***************************************************************************
**
**	FileName:	SvgBuilder.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	To output all the SVG objects to the final SVG file.
**
**	Date:		January 8, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.10 - August 16, 2002 Removed the MAKE_CSS ONLY to DxfConverter
**				where things can be stream lined.
**				1.11 - September 6, 2002 Added a HeaderProcessor object.
**				1.20 - October 20, 2003 Made changes to the writeHTMLWrapper()
**				that will output a french and english html wrapper as required.
**				1.30 - October 29, 2003 Added functions that can optionally 
**				make html wrapper files with the boardno values of the drawing.
**				1.40 - March 2, 2004 Updated the DTD.
**				1.5 - July 7, 2004 Added methods to apply additional attributes
**				to the root element a-la-SvgElement.addAttribute().
**				1.6 - August 13, 2004 Added lines to writeSvgObjectsToFile()
**				which allows places all the content into a single page <g> element
**				with an ID of 'Page_1'.
**				1.7 - August 30, 2004 Commented out checkFileExists() so all files
**				will be over-written by new files without checking.
**				1.71 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				1.80 - April 11, 2005 Added height and width attributes to the <svg>
**				tag and put <defs> tags around the javascript.
**				1.90 - April 14, 2005 Removed reference to the HeaderProcessor object.
**				2.00 - May 18, 2005 Made changes to accomodate the HTMLWrapperBuilder.
**				2.01 - July 29, 2005 Moved the getAttributes() method out of JavaScript
**				processing if statement. Fixes additional attributes not showing 
**				on <svg> tag if there is not JavaScript.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg;

import java.io.*;			// file handling.
import java.util.*;			// vector handling
import dxf2svg.util.*;
import dxf2svg.svg.*;
import java.util.zip.*;		// For writing compressed svg.
import java.util.regex.*;	// Search for boardnos.

/**
*	This is where all the SVG objects output their results to the final SVG
*	file.<BR><BR>
*
*	Methodology:<BR><BR>
*<OL>
*	<LI> SvgBuilder initiates the values of several switches inside its
*	constructor.
*	<LI> The rest of the process is controlled by a DxfConverter object
*	starting with the generation of the SVG header and populating
*	it with the prescribed scripts, entity declarations and style information.
*	In addition it sets up the SVG view port.
*	<LI> Determine the name of the SVG file and tests for its existence.
*	If it finds one, ask the user if (s)he wishes to over-write it.
*	<LI> Use a loop to iterate over the entire Vector of SVG elements
*	writing them to file.
*	<LI> Close the file and clean up as required.
*</OL>
*
*	@version	1.90 - April 14, 2005
*	@author		Andrew Nisbet
*/

public final class SvgBuilder
{
	// Switches
	private boolean VERBOSE;					// Verbosity
	private boolean DEBUG;						// debug mode
												// this one not implemented yet.
	private boolean DTD_ALT;					// Valid Dtd is TBD from user if false use default declaration
												// in svg header.
	private static int MAKE_CSS;				// if set we need to account for css in doc declaration
	private boolean INCLUDE_ENCODING = true;	// Character encoding set specified.
	private int INCLUDE_JAVASCRIPT;				// place script tags and safety for old browsers and
												// users with JavaScript turned off.
	private String JS_SRC_PATH;					// location of external scripts
	private static final String ENCODING = " encoding=\"iso-8859-1\"";
	private double Width;						// Width of the drawing
	private double Height;						// Height of the drawing
	private StringBuffer DTD		= new StringBuffer();	// Name of the out file
	private StringBuffer SvgHeader	= new StringBuffer();	// header for Svg
	private StringBuffer Declare	= new StringBuffer();	// Declaration info like file name in <desc> tag.
	private String FileNameOut;								// Name of the out file
	private Vector SvgEntityDeclareList	= null;				// Blocks if any.
	private Vector SvgPatternList	= null;					// Hatch Patterns if any.
	private Vector SvgEntityList	= new Vector();			// Vector for all the new element
	private DxfConverter DxfConvertRef;						// For HtmlWrapperBuilder
	private StyleSheetGenerator SSG;						// StyleSheetGenerator.
	private SvgUtil svgUtility;								// Conversion utility
	private Vector javaScript;					// Storage for additional, conditional JS.

	private final static int SVG 	= 0;		// Which type of file do we need a name for
	private final static int HTML 	= 1;		// The wrapper file name.
	private final static int SVGZ	= 2;		// Compressed svg file.

	private int entityQuoteType = SvgEntityDeclaration.SINGLE_QUOTE;	// Quoting for entities.
	// These values are used to control the default language of the SVG file when it opens.
	// To date only english and French are implemented.
	private final static int ENGLISH	= 1;
	private final static int FRENCH		= 2;

	// Storage for additional attributes.
	private Vector vAttribs;


	//******* constructor *********/
	/** Sets switches that control the 'output state'.*/
	public SvgBuilder(DxfConverter dxfc)
	{
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: SvgBuilder.");
		DxfConvertRef		= dxfc;
		VERBOSE 			= DxfPreprocessor.verboseMode();
		if (VERBOSE)
		{
			System.out.println("The SvgBuilder exists now.");
		}
		DEBUG				= DxfPreprocessor.debugMode();
		MAKE_CSS			= DxfPreprocessor.cssMode();
		DTD_ALT				= DxfPreprocessor.includeDTD();
		INCLUDE_JAVASCRIPT	= DxfPreprocessor.includeJavaScript();
		SSG 				= dxfc.getStyleSheetGenerator();// assign StyleSheetGenerator.
		svgUtility 			= dxfc.getSvgUtil();			// conversion utility
	}
	
	
	/** This method allows you to apply attributes to the root element &lt;svg&gt;.
	*/
	public void addAttribute(Attribute attrib)
	{
		if (vAttribs == null)
		{
			vAttribs = new Vector();
			vAttribs.add(attrib);
			return;
		}
		
		Attribute a = null;
		// do not store duplicate attributes.
		for (int i = 0; i < vAttribs.size(); i++)
		{
			a = (Attribute)vAttribs.get(i);
			if (attrib.equals(a))
			{
				return;
			}
		}
		vAttribs.add(attrib);
	}
	
	/**
	*	Returns additional attributes if any attributes if there are any.
	*/
	protected String getAdditionalAttributes()
	{
		// The SvgElement can contain ElementEvent objects which are the attribs like onclick.
		StringBuffer attribs = new StringBuffer();
		
		if (vAttribs != null)
		{
			Attribute attrib = null;
			for (int i = 0; i < vAttribs.size(); i++)
			{
				attrib = (Attribute)vAttribs.get(i);
				attribs.append(attrib.toString());
			}  // end for
		} // end if
		
		return attribs.toString();
	}

	/** Makes the SVG header in the form of an internal String to be used by
	*	{@link #writeSvgObjectsToFile}.<BR><BR>
	*
	*	Called from DxfConverter object.
	*/
	private String makeSvgHeader()
	{

		//****************************  set up DTD declaration  ***************************/
		// standalone yes individual xml doc no: a fragment imbedded in a parent xml doc
		DTD.append("<?xml version=\"1.0\"");
		if (INCLUDE_ENCODING)
			DTD.append(ENCODING);
		DTD.append(" standalone=\"no\"?>\n");
		DTD.append("<!-- Generated by Dxf2Svg v"+Dxf2SvgConstants.VERSION+" -->\n");
		DTD.append("<!-- Built to conform with W3C SVG specification v1.0   -->\n");
		DTD.append("<!-- with specific conformance; but not restricted to   -->\n");
		DTD.append("<!-- Adobe's SVG viewer version 3.0 running in          -->\n");
		DTD.append("<!-- MS Internet Explorer ver. 6.0.2600.0000            -->\n");
		// for now find out it the MAKE_CSS switch has been thrown but deal with it later.
		if (MAKE_CSS >= (Dxf2SvgConstants.CSS_ONLY + Dxf2SvgConstants.INLINE_STYLES))
		{
			if (VERBOSE)
				System.out.println("SvgBuilder: making css...");

			DTD.append("<?xml-stylesheet ");

			if ((MAKE_CSS & Dxf2SvgConstants.EXTERNAL_CSS) == Dxf2SvgConstants.EXTERNAL_CSS)
				DTD.append("href=\""+Dxf2SvgConstants.STYLE_SHEET_NAME+"\"");

			DTD.append(" type=\"text/css\"?>\n");
		}

		// Append DTD and entity declarations.
		DTD.append("<!DOCTYPE svg PUBLIC ");
		if (DTD_ALT)
		{
			// Updated the DTD March 2, 2004
			DTD.append("\"-//W3C//DTD SVG 1.0//EN\"\n");
			DTD.append("\t\"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\"[\n");
			// test this //
			//DTD.append("\"-//W3C//DTD SVG 20000303 Stylable//EN\"\n");
			//DTD.append("\t\"http://www.w3.org/TR/2000/03/WD-SVG-20000303/DTD/svg-20000303-stylable.dtd\"[\n");

		}
		else
		{
			DTD.append("\"\" \"\"[\n");
		}

		////////////////// Place ENTITY declarations here as required.
		//////////////////////
		//		Blocks		//
		//////////////////////
		if (SvgEntityDeclareList != null && ! SvgEntityDeclareList.isEmpty())
		{
			// lets make an iterator to traverse the list of blocks
			Iterator SvgEntityDeclareListItorator = SvgEntityDeclareList.iterator();
			// we could do some auto indenting of nested tags in here
			SvgEntityDeclaration thisEntity;
			while (SvgEntityDeclareListItorator.hasNext())
			{
				thisEntity = (SvgEntityDeclaration)SvgEntityDeclareListItorator.next();
				DTD.append("<!ENTITY ");			// declare new entity
				DTD.append(thisEntity.getObjIDUU());// write unformatted ref id
				// This controls the entities quote type and that trickles down
				// to use the reverse quote type within the enclosed entities.
				if (entityQuoteType == SvgEntityDeclaration.SINGLE_QUOTE)
					DTD.append(" \'\n");	// write start double quote
				else
					DTD.append(" \"\n");	// write start double quote
				DTD.append(thisEntity.toString());	// write out the collection's objects
				if (entityQuoteType == SvgEntityDeclaration.SINGLE_QUOTE)
					DTD.append(" \'>\n");	// write start double quote
				else
					DTD.append(" \">\n");	// write the closing quote and close tag
			}
			// wrote blocks
			if (VERBOSE)
				System.out.println("SvgBuilder: " + SvgEntityDeclareList.size() +
					" Entity Declarations (blocks) written.");
		}	// end if

		// Terminate the DTD declaration.
		DTD.append("]>\n");


		//******************** viewbox attribs ********************/
		// now figure out the dimensioning for the <svg> tag.
		// <svg xml:space="preserve" id="FIG_10-20" width="482" height="602" viewBox="0 0 482 602" >
		SvgHeader.append("<svg xml:space=\"preserve\"");
		// calculate width and height and normalize if dxf window is not 0,0
		double XMax = svgUtility.getLimitsMaxX() * svgUtility.Units();
		double XMin = svgUtility.getLimitsMinX() * svgUtility.Units();
		double YMax = svgUtility.getLimitsMaxY() * svgUtility.Units();
		double YMin = svgUtility.getLimitsMinY() * svgUtility.Units();

		Width = XMax - XMin ;
		Height = YMax - YMin;
		Width = svgUtility.trimDouble(Width);
		Height = svgUtility.trimDouble(Height);
		// you have to keep the viewbox with origin at 0,0
		XMax = svgUtility.trimDouble(XMax);
		YMax = svgUtility.trimDouble(YMax);
		addAttribute(new Attribute("width", String.valueOf(XMax)));
		addAttribute(new Attribute("height", String.valueOf(YMax)));
		SvgHeader.append(" viewBox=\"0 0 "+XMax+" "+YMax+"\"");
		// get any additional attributes.
		SvgHeader.append(getAdditionalAttributes());
		// This is required if you have two languages. It will initialize the required
		// functions and let the SVG file know what HTML function will interface with it.
		if (DxfPreprocessor.hasJavaScript() == true || SSG.isLangSwitchRequired() == true)
		{
			// This will switch on the inclusion of JS if it has not been done by user
			// but it will also reset the users choice if he/she has set JS to an
			// external location.
			switch (INCLUDE_JAVASCRIPT)
			{
			case Dxf2SvgConstants.NO_SCRIPT:	// now include an internal script
				INCLUDE_JAVASCRIPT = Dxf2SvgConstants.INTERNAL_SCRIPT;
				break;

			case Dxf2SvgConstants.INTERNAL_SCRIPT:	// This is the default so nothing to do.
				break;

			case Dxf2SvgConstants.EXTERNAL_SCRIPT:	// Now we need to include internal as well
				INCLUDE_JAVASCRIPT = Dxf2SvgConstants.INTERNAL_AND_EXTERNAL_SCRIPT;
				break;

			case Dxf2SvgConstants.INTERNAL_AND_EXTERNAL_SCRIPT: // both set already continue as planned.
				break;

			default:	// javascript not set at all (is that possible?)
				INCLUDE_JAVASCRIPT = Dxf2SvgConstants.INTERNAL_SCRIPT;
				break;
			}
		}
		// Added xlink specification March 3, 2004
		// other namespaces could be added here later as required.
		if (DxfPreprocessor.getUsesLinks() == true)
		{
			addAttribute(new Attribute("xmlns:xlink", "http://www.w3.org/1999/xlink"));
		}
		addAttribute(new Attribute("xmlns", "http://www.w3.org/2000/svg"));
		// close the svg tag.
		SvgHeader.append(">\n");
		// This is where the style sheets get added. (See StyleSheetGenerator for more detail.)
		SSG.makeStyleSheet(SvgHeader);

		// now include JavaScript or the tags if the user has requested them.
		if (INCLUDE_JAVASCRIPT == Dxf2SvgConstants.EXTERNAL_SCRIPT ||
			INCLUDE_JAVASCRIPT == Dxf2SvgConstants.INTERNAL_AND_EXTERNAL_SCRIPT)
			this.JS_SRC_PATH = DxfPreprocessor.includeJavaScriptSrcPath();

		if (INCLUDE_JAVASCRIPT >= Dxf2SvgConstants.INTERNAL_SCRIPT ||
			INCLUDE_JAVASCRIPT == Dxf2SvgConstants.INTERNAL_AND_EXTERNAL_SCRIPT)
		{
			SvgHeader.append("<defs>\n");
			SvgHeader.append(getJavaScript());
			SvgHeader.append("\n</defs>\n");
		}
		// else INCLUDE_JAVASCRIPT == NO_SCRIPT in which case we don't include anything.


		return DTD.toString() + SvgHeader.toString();
	}





	/**	Allows the submission of hatch patterns for output to the Svg file.
	*/
	public void submitSvgPatterns(Vector hatchPatterns)
	{	SvgPatternList = hatchPatterns;	}

	/**	Allows the submission of blocks to SvgBuilder for output
	*	to the Svg file. Here we are talking about DXF blocks which translates 
	*	into entities in the SVG.
	*/
	public void submitSvgSymbols(Vector blocks)
	{	SvgEntityDeclareList = blocks;	}






	/** Iterates over the Vector that is supplied as an argument and writes
	*	its contents to file.
	*	@throws IOException if the SVG file can not be written to.
	*/
	public void writeSvgObjectsToFile(Vector SOut) throws IOException
	{

		// DO NOT FORGET TO CLOSE THE BUFFEREDWRITER OR THE FUNCTION FAILS TO
		// WRITE TO FILE.
		boolean isZipped = DxfPreprocessor.isZipped();
		String zipFileNameOut = new String();
		SvgEntityList = SOut;
		File OUT;							// File descriptor for out file
		String htmlTargetName;				// Name to appear in <embed> tags.
		String htmlTargetNameRoot;			// Name of target file without extension.
		
		
		// Now we will group the entire set of layers as a page within the svg
		// This is a kludge to accomodate scripting from the menu.svg developed
		// for Spar. This element is not handled by modifyAttributes(). If you wish
		// that, move it to DxfConverter and add it before the call to modifyAttributes()
		// To do that, make a new SvgGroup and add all of the vLayers elements then somehow
		// add that SvgGroup (since it is the first child element of <svg>) to vLayer.
		SOut.add(0,"<g id=\"Page_1\">");
		SOut.add("</g>  <!-- id=\"Page_1\" -->");
			
			
		// We need this file for the initial SVG. Later if required, we 
		// zip this file so we need both the svg name and the compressed file
		// name. If the file is not to be compressed then we just need the name
		// of the svg file for writing out the the <embed> tags in the html wrapper(s).
		FileNameOut = makeFileNameOut(SVG);
		if (isZipped)
		{
			zipFileNameOut = makeFileNameOut(SVGZ);
			htmlTargetName = new String(zipFileNameOut);
		}
		else // if not zipped we need the name of the file target for <embed>.
		{
			htmlTargetName = new String(FileNameOut);
		}
		
		// Let's now get just the name of the target without the path information
		// for the title of the html and the src for the <embed> tag in the html.
		File TMP_HTML_TARGET_NAME = new File(htmlTargetName);
		htmlTargetName = TMP_HTML_TARGET_NAME.getName();
		TMP_HTML_TARGET_NAME = null;
		
		OUT	= new File(FileNameOut);



		// check if the file exists and if it does ask user if he wants to overwrite
		if (checkFileExists(OUT) == true)
		{
			if (overWriteFile() == true)
			{
				// this clobbers the previous contents.
				OUT.createNewFile();
			}
			else
			{
				System.out.println("Operation aborted.");
				return;
			}
		}


		BufferedWriter BWriter = new BufferedWriter(
			new FileWriter(OUT) );

		BWriter.write(makeSvgHeader());


		// Now output the preliminaries like <symbols> and <defs>
		////////////////////////
		//		Patterns	  //
		////////////////////////
		if (SvgPatternList != null && ! SvgPatternList.isEmpty())
		{
			// lets make an iterator to traverse the list of blocks
			Iterator SvgPatternListItorator = SvgPatternList.iterator();
			BWriter.write("<defs>");
			BWriter.newLine();
			// we could do some auto indenting of nested tags in here
			while (SvgPatternListItorator.hasNext())
			{
				BWriter.write(SvgPatternListItorator.next().toString());
				BWriter.newLine();
			} // wrote patterns
			BWriter.write("</defs>");
			BWriter.newLine();
			if (VERBOSE)
				System.out.println("SvgBuilder: " + SvgPatternList.size() +
					" hatch pattern object(s).");
		}	// end if




		//////////////////////
		//		Entities	//
		//////////////////////
		// lets make an iterator to traverse the list
		Iterator SvgEntityListItorator = SvgEntityList.iterator();
		// we could do some auto indenting of nested tags in here
		while (SvgEntityListItorator.hasNext())
		{
			BWriter.write(SvgEntityListItorator.next().toString());
			BWriter.newLine();
		}
		if (VERBOSE)
			System.out.println("SvgBuilder: " + SvgEntityList.size() +
				" other Svg elements written.");
		// added August 12, 2004 //
		// This places any extra file content into the SVG. See DxfPreprocessor for details.
		if (DxfPreprocessor.isInclude())
		{
			SvgObjectX dxf2svgNameSpaceElement = new SvgObjectX(DxfConvertRef);
			// Here is the namespace for the menu items.
			// <dxf2svg:init width="340.79" height="426.0" wiring="true"/>

			Attribute attribHeight = new Attribute("height",String.valueOf(Height));
			dxf2svgNameSpaceElement.addAttribute(attribHeight);
			Attribute attribWidth = new Attribute("width",String.valueOf(Width));
			dxf2svgNameSpaceElement.addAttribute(attribWidth);
			Attribute attribWiring = new Attribute(
				"activatePalette",String.valueOf(isWiring()));
			dxf2svgNameSpaceElement.addAttribute(attribWiring);
			dxf2svgNameSpaceElement.setType("dxf2svg:init");
			BWriter.write(dxf2svgNameSpaceElement.toString());
			BWriter.write(DxfPreprocessor.getIncludeFileData());
			BWriter.newLine();
		}
		BWriter.write("</svg>");
		BWriter.close();
		
		//////////////////////
		//  HTML Wrappers   //
		//////////////////////
		// Now all of the header calculations etc. are done we can carry on with
		// making the HTML wrappers if required.
		if (DxfPreprocessor.useHTMLWrappers())
		{
			//writeHTMLWrapper(htmlTargetName, Dxf2SvgConstants.ENGLISH);
			//writeHTMLWrapper(htmlTargetName, Dxf2SvgConstants.FRENCH);
			HtmlWrapperBuilder wb = new HtmlWrapperBuilder(DxfConvertRef, this);
			wb.writeHtmlWrapper();
		}
		
		// Here we will compress the file we just output if the user requested it.
		if (isZipped)
		{
			int SIZE = 100;
			GZIPOutputStream GZIPStream = new GZIPOutputStream(
				new FileOutputStream(new File(zipFileNameOut)) );
				
			DataOutputStream GZIPOut = new DataOutputStream(
				new BufferedOutputStream(GZIPStream) );
				
			File IN = new File(FileNameOut);
			
			DataInputStream  svgStreamIn = new DataInputStream(
				new BufferedInputStream( new FileInputStream(IN) ) );



			
			byte[] Buf = new byte[SIZE];

			int bytesRead = 0;
			while (bytesRead != -1)
			{
				bytesRead = svgStreamIn.read(Buf);
				if (bytesRead < SIZE)
				{
					for (int i = 0; i < bytesRead; i++)
					{
						GZIPOut.write(Buf[i]);
					} // end for
		
				} // end if
				else
				{
					GZIPOut.write(Buf);
				}
			}
			
			svgStreamIn.close();

			GZIPOut.close();
			
			// Remove the Original svg file leaving only the compressed file.
			IN.delete();
		}
	}
	
	
	/** This method detects if the graphic is a wiring diagram.
	*/
	protected boolean isWiring()
	{
		// Basically we know if this drawing is a wiring diagram if it contains layer
		// that have names equivalent to names that have been registered as collaborators
		// or gangs. To do this we have to find out the names of the collaborators or gangs.
		// Then we have to search the drawing for layers with those names.
		// get all the layer names
		Vector layerNames = new Vector();
		SSG.getLayerNames(layerNames);
 		String layerName;
		for (int i = 0; i < layerNames.size(); i++)
		{
			layerName = (String)(layerNames.get(i));
			if (DxfPreprocessor.isCollaboratorTarget(layerName) ||
				DxfPreprocessor.isGangingTarget(layerName))
			{
				return true; // stops looking after it finds a match for gang or wire.
			}
		}
		
		return false;
	}
	
	


	


	/** Checks the DXF conversion directory for the presents of an
	*	existing SVG file. A file with the same name as the originating DXF
	*/
	private boolean checkFileExists(File CHECK)
	{
		// checks if any file handle passed actually represents a physical file.
		//if (CHECK.isFile() == false)
		//{
			return false;
		//}

		//return true;
	}



	// This method is protected so it will be added to documentation even though
	// the class if final. You could include a -private flag in makedoc but 
	// there are a bunch of private methods that are not required.
	/** Determines the SVG's file name by trimming the DXF's extension
	*	and applying one of three possible extensions.&quot;.svg&quot;.
	*	<P>
	*	The SVG file will always end up with the same path as the original
	*	DXF.
	*	@param extension enumerated value defined by the following values:
	*	<UL>
	*	<LI> SVG = 0
	*	<LI> HTML = 1
	*	<LI> SVGZ = 2
	*	</UL>
	*	<P>
	*	@return the original file name with the new extension unless the value
	*	passed as a parameter is out of range. In that case the original file
	*	name is returned with an &quot;.xxx&quot; extension.
	*/
	protected String makeFileNameOut(int extension)
	{
		String FileName = DxfPreprocessor.getFileName();
		int len = FileName.length();
		// We collect the root name sans extension.
		String fOut = FileName.substring(0,(len - 4));
			
		switch (extension)
		{
			case SVG:
				fOut = fOut.concat(".svg");
				break;
			
			case HTML:
				fOut = fOut.concat(".html");
				break;

			case SVGZ:
				fOut = fOut.concat(".svgz");
				break;
				
			default:
				System.err.println("SvgBuilder.makeFileNameOut() error: illegal extension value argument.");
				System.err.println("Expected SVG, HTML, or SVGZ but got:" + extension);
				System.err.println("Don't know what to do; look for a file with a '.xxx' extension");
				System.err.println("in comversion directory.");
				fOut = fOut.concat(".xxx");
		} // end switch

		return fOut;
	}
	

	
	

	/** This method searches the content of the Svg document prior to output. The pattern
	*	to search for is denoted by the argument Pattern p (see {@link java.util.regex.Pattern}
	*	for more information.)
	*	<P>
	*	The matchingContent Vector is where matching content strings are placed for futher
	*	processing. This Vector may be null if match counts are required but no content.
	*	<P>
	*	The return value is the number of matches found. 
	*/
	public int searchSvgContent(Vector matchingContent, Pattern p)
	{
		
		Vector[] vArray = new Vector[2];	// Array of the vectors that contain SVG elements.
		vArray[0] = SvgEntityDeclareList;
		vArray[1] = SvgEntityList;
									
		int numFound = 0;					// Number of matching patterns

		for (int j = 0; j < vArray.length; j++)
		{
			// If this array vector element is null skip to next one.
			if (vArray[j] != null)
			{
				Object s;
				// In this loop we are looking for SvgText elements that are not
				// part of any group, or, SvgText elements whose parent element 
				// is <svg>.
				for (int i = 0; i < vArray[j].size(); i++)
				{
					s = (Object)vArray[j].get(i);
					if (s instanceof SvgText)
					{
						SvgText sText = (SvgText)vArray[j].get(i);
						if (sText.find(p) == true)
						{
							// add the content match to the return vector if the
							// Vector is not null. This could happen if the user
							// is only interested in the number of matches -not
							// the actual content itself.
							if (matchingContent != null)
							{
								matchingContent.add(sText.getString());
							}
							numFound++;
						}
					}
					// This is by far more common. Almost all elements in the SVG are contained
					// in SvgCollections. Let's analyse them one by one for SvgText elements.
					else if (s instanceof SvgCollection) 
					{
						SvgCollection sColl = (SvgCollection)vArray[j].get(i);
						// Even if the vector is null it will still return the number of matches.
						// just no content.
						numFound += sColl.searchContent(matchingContent, p);
					} // end else if
				} // end for
			} // end if
		} // end for
		
		return numFound;
	}


	/** Pauses and allows user to overwrite an existing file or not.
	*	@return boolean <code>true</code>=overwrite existing file; <code>false</code>=abort.
	*	@throws IOException if an error occurs reading the users answer
	*	from STDIN.
	*/
	private boolean overWriteFile()
	{
		String Answer = new String();

		try
		// this try will check on correct keyboard input answer from user.
		{
			// read in data from keyboard
			System.out.println("Overwrite existing file \n\"" + FileNameOut + "\"? <y/n>[n]");
			BufferedReader stdin = new BufferedReader(
				new InputStreamReader(System.in) ) ;

			Answer = stdin.readLine();

			if (!Answer.equalsIgnoreCase("y"))
				return false;
			else
				return true;

		}
		catch (IOException e)
		{
			System.err.println("Error reading stdin: " + e);
			return false;
		}
	}

	
	/** This method allows extra code specific to the current conversion file to 
	*	be added on-the-fly. This could occur if conditional processing detects 
	*	values that need to be added to the &lt;script&gt; tag during this file's
	*	conversion. This occurs in functions like note searches. Some files have
	*	notes that require JavaScript, others don't.
	*	@param javascript JavaScript as a String.
	*/
	public void addJavaScript(String javascript)
	{
		if (javaScript == null)
		{
			javaScript = new Vector();
		}
		
		javaScript.add(javascript);
	}
	
	
	public boolean isJavaScript()
	{
		if (javaScript == null || javaScript.isEmpty())
		{
			return false;
		}
		
		return true;
	}
	

	/** Returns a String containing the JavaScript syntax required based on
	*	the INCLUDE_JAVASCRIPT environment variable set in Dxf2Svg.
	*	@return String Formatted JavaScript declaration.
	*/
	private String getJavaScript()
	{
		/*
		*	Handle JavaScript requirements here and include basic tag structure
		*	depending on what the user requests.
		*/

		StringBuffer jsStr = new StringBuffer();

		if (INCLUDE_JAVASCRIPT == Dxf2SvgConstants.EXTERNAL_SCRIPT ||
			INCLUDE_JAVASCRIPT == Dxf2SvgConstants.INTERNAL_AND_EXTERNAL_SCRIPT)
		{
			jsStr.append("<script content-type=\"text/ecmascript\" language=\"JavaScript\"");
			jsStr.append(" xlink:href=\""+JS_SRC_PATH+"\"></script>\n");
		}

		if (isJavaScript())
		{
			jsStr.append("<script content-type=\"text/ecmascript\" language=\"JavaScript\"");
			jsStr.append(">\n");
			jsStr.append("<![CDATA[\n");
			Iterator jsStackIt = javaScript.iterator();
			while(jsStackIt.hasNext())
			{
				jsStr.append((String)jsStackIt.next());
			}
			jsStr.append("//]]>\n</script>\n");				
		}

		return jsStr.toString();
	}

}