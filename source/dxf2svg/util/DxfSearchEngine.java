
/****************************************************************************
**
**	FileName:	DxfSearchEngine.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Searches an ASCII DXF file.
**
**	Date:		April 19, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1 November 5, 2003
**				1.01 January 26, 2005 Added file name to pattern error in find()
**				1.02 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:		
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;			// File operations
import java.util.*;			// For Vectors and Iterators.
import java.util.regex.*;	// Pattern and Matcher
import dxf2svg.DxfPreprocessor;// Used for logging events.

/**	Searches for patterns in an ASCII AutoCAD DXF file.
*	<P>
*	How to use the search engine:
*<OL>
*	<LI> Create a DxfSearchEngine object.
*	<LI> Pass a DXF file handle to the engine (in a loop if necessary).
*	<LI> Create pattern (regular expression) and pass it to the DxfSearchEngine's {@link #find} method.
*</OL>
*	Use the {@link #findNext} method to find additional instances of a search pattern.
*	You may also explicitly test a file for searchability with the {@link #isAsciiDxf} method, but 
*	this test is called before the find() method is run.
*	<p>
*	This class can be made to searches sensitive to certain group codes. This allows the 
*	implementer to 
*	specify the exact group code that the expected data can be found in. The default
*	is to search for patterns only in group codes of '1' - general text Strings.
*	Pass a search code of '999' to additionally search comments.
*	<P>
*	This class can search for any type of data within the DXF file, but all returned values
*	are expressed as Java Strings.
*	<P>
*	Here is a list of things that you can do to customize searches:
*<OL>
*	<LI> Limit searches to a specific layer.
*	<LI> Search all layers by passing null as a search layer.
*	<LI> Search specific sections of the DXF.
*</OL>
*
*	@version	1.1 April 21, 2004
*	@author		Andrew Nisbet
*/
public class DxfSearchEngine
{
	protected File dxfFile;				// Name of the DXF file.
	protected Vector significantCodes;	// Container of Integers that are significant codes.
	protected Pattern pattern;			// Current pattern we are searching for.
	protected int dxfLineNum;			// Current line number of the dxf file for findNext()
	protected String layer;				// Name of layer to confine search to.
	protected boolean validLayer;		// We are on a layer that is valid to search.
	
	/** This value allows you to exclude a particular section from a search. This is useful
	*	to allow you specify sections that will have data that you are interested in. This 
	*	will automatically exclude other sections from the search. Skipping a search of the
	*	header will be produce fewer false hits. For example: if you were to perform a search
	*	of the entire DXF for text strings that occur on layer '0', and you want to collect
	*	the group code 10 and 20 for each hit, you will get positive hits if HEADER is allowed 
	*	to be searched. You will pick up the autocad version number for example and several 
	*	unrelated 10 and 20 values. 
	*	
	*	The reason is that there is a group code of 8 in the header that is not related to the header
	*	but is usually set to '0'. All other entities use the group code 8 to indicate the layer
	*	they are on. This is how layer searches are identified. The HEADER section is also peppered
	*	with 10 and 20 values that to the header are just double precision values but to entities
	*	are x any y coordinates for the location of the anchor points for entities.
	*
	*	The sectionInclude variable contains all the areas you wish to search and is controllable
	*	from the {@link #addSectionToSearch(int)} and the {@link #removeSectionFromSearch(int)} methods.
	*/
	protected int sectionInclude;		// Never search any section noted here.
	private int thisSection;
	
	public static final int EOF			 = -1;
	public static final int INITIAL		 = 0;
	/** HEADER contains all the variables that describe the state of AutoCAD at the time the
	*	DXF was made. Removing this section from the search criteria will eliminate a lot of
	*	false hits for searches for stuff on layer '0'.
	*/
	public static final int HEADER		 = 1;
	public static final int CLASSES		 = 2;
	public static final int TABLES		 = 4;
	/** The Blocks section is an important section to search for visible content in a DXF. It 
	*	contains entity descriptions and grouping information for blocks.
	*/
	public static final int BLOCKS		 = 8;
	/** This is the most important section to search if you want to collect information from 
	*	the visible data in the DXF.
	*/
	public static final int ENTITIES	 = 16;
	public static final int OBJECTS		 = 32;
	/** To date I have not seen any DXF with this SECTION populated.
	*/
	public static final int THUMBNAIL	 = 64;
	
	
	
	////////////////////
	//  Constructors  //
	////////////////////
	/** The constructor allows for
	*/
	public DxfSearchEngine()
	{ 
		significantCodes	= new Vector();
		// Add the default search for string content of code 1.
		significantCodes.add(new Integer(1));
		reset();
		sectionInclude = BLOCKS;		// add blocks to the default search
		sectionInclude |= ENTITIES;		// add entities to our search 
		thisSection = INITIAL;			// initial section state.
	}
	
	////////////////////
	//     Methods    //
	////////////////////
	/** Prints out the layer that searches will be bound to.
	*/
	public void displaySearchLayer()
	{
		if (layer == null)
		{
			System.out.println("All layers will be searched.");
		}
		else
		{
			System.out.println("Layer search binding: '"+layer+"'");
		}
	}
	
	/** This method displays the sections that are going to be searched.
	*/
	public void displaySearchSections()
	{
		System.out.println("Section to search:");
		
		if ((sectionInclude & HEADER) != 0)
		{
			System.out.println("HEADER");
		}
			
		if ((sectionInclude & CLASSES) != 0)
		{
			System.out.println("CLASSES");
		}
			
		if ((sectionInclude & TABLES) != 0)
		{
			System.out.println("TABLES");
		}
		
		if ((sectionInclude & BLOCKS) != 0)
		{
			System.out.println("BLOCKS");
		}
		
		if ((sectionInclude & ENTITIES) != 0)
		{
			System.out.println("ENTITIES");
		}
		
		if ((sectionInclude & OBJECTS) != 0)
		{
			System.out.println("OBJECTS");
		}
			
		if ((sectionInclude & THUMBNAIL) != 0)
		{
			System.out.println("THUMBNAIL");
		}	
		System.out.println("=====================");
	}
	
	
	/** This method allows you to remove sections from the search criteria. The default
	*	is to search the ENTITIES section and the BLOCKS section so if you called this
	*	method with the argument BLOCKS, the BLOCKS section would be dropped from the
	*	search criteria and only the ENTITIES section would remain.
	*/
	public void removeSectionFromSearch(int section)
	{
		switch (section)
		{
			case HEADER:
				if ((sectionInclude & HEADER) != 0)
					sectionInclude ^= HEADER;
				break;
				
			case CLASSES:
				if ((sectionInclude & CLASSES) != 0)
					sectionInclude ^= CLASSES;
				break;
				
			case TABLES:
				if ((sectionInclude & TABLES) != 0)
					sectionInclude ^= TABLES;
				break;
			
			case BLOCKS:
				if ((sectionInclude & BLOCKS) != 0)
					sectionInclude ^= BLOCKS;
				break;
			
			case ENTITIES:
				if ((sectionInclude & ENTITIES) != 0)
					sectionInclude ^= ENTITIES;
				break;
			
			case OBJECTS:
				if ((sectionInclude & OBJECTS) != 0)
					sectionInclude ^= OBJECTS;
				break;
				
			case THUMBNAIL:
				if ((sectionInclude & THUMBNAIL) != 0)
					sectionInclude ^= THUMBNAIL;
				break;
				
			default:
				System.err.println("DxfSearchEngine.removeSectionFromSearch(): invalid section " +
					section+" identifier will be ignored.");
				break;
		}  // end switch
	}
	
	/** This method allows you to add a specific section to the search criteria. The default
	*	is to search the ENTITIES section and the BLOCKS section.
	*/
	public void addSectionToSearch(int section)
	{
		switch (section)
		{
			case HEADER:
				sectionInclude |= HEADER;
				break;
				
			case CLASSES:
				sectionInclude |= CLASSES;
				break;
				
			case TABLES:
				sectionInclude |= TABLES;
				break;
			
			case BLOCKS:
				sectionInclude |= BLOCKS;
				break;
			
			case ENTITIES:
				sectionInclude |= ENTITIES;
				break;
			
			case OBJECTS:
				sectionInclude |= OBJECTS;
				break;
				
			case THUMBNAIL:
				sectionInclude |= THUMBNAIL;
				break;
				
			default:
				System.err.println("DxfSearchEngine.addSectionToSearch(): invalid section"+
					" identifier will be ignored.");
				break;
		}  // end switch
	}
	
	/** Allows the client to specify a DXF to search. All previous search 
	*	criteria are reset.
	*/
	public void setDxfFile(File dxfFile)
	{	
		this.dxfFile = dxfFile;
		reset();  // In case a layer search was done in the previous file.
	}
	
	
	
	/** Searches the next instance of a previously defined pattern.
	*	If no previous pattern has been defined, the return value
	*	will be null and a error message will be issued.
	*/
	public String findNext()
	{	
		return find();
	}
	
	/** Searches the target DXF file for the argument search pattern. Any layer sensitive
	*	searches are reset. If you require a layer sensitive search to be repeated use
	*	{@link #findOnLayer(String,Pattern)}.
	*	@return String value of the first match found or null if the pattern was not found.
	*	use {@link #findNext} to search for additional matches of the pattern.
	*/	
	public String find(Pattern p)
	{
		// This is done so we don't get confused about if our searches are being
		// restricted to a specific layer.
		reset();
		pattern = p;
		
		return find();
	}
	
	/** This method allows the client to sensitively isolate searches to a specific layer.
	*	The behaviour is as follows: using this method sets a layer to search on subsequent
	*	calls to {@link #find} or {@link #findNext} or {@link #find(java.util.regex.Pattern)}
	*	all will confine searches to this layer as well. To remove this behaviour, call the
	*	method again with the argument set to null.
	*	@param p Pattern of text to search for.
	*	@return layer value of the first match found or null if the pattern was not found case 
	*	insensitive.
	*/
	public String findOnLayer(String layer, Pattern p)
	{
		reset();
		pattern = p;
		// Restart search from top of dxf file.
		this.layer = layer;
		validLayer = false;		// Switch off all other searches until this is true.
		
		return find();		
	}
	
	

	
	/** Resets the seach engine (but not the already entered search codes).
	*	This method clears a search sensitivity for a specific layer, reset
	*	the search to the top of the file ready for searching all layers.
	*/
	public void reset()
	{
		pattern = null;
		// Restart search from top of dxf file.
		dxfLineNum = 0;
		layer = null;
		validLayer = true;		// Switch off all other searches until this is true.
	}
	
	
	/** This method will report if a search by a layer is in progress.
	*/
	protected boolean isLayerSearch()
	{
		if (layer == null)
		{
			return false;
		}
		return true;
	}
		
	
	/** Makes a search of a specific layer for pre-applied group codes (see {@link #addSearchCode(Integer)}
	*	for setting group codes that you are interested in. The Hashtable results contains all matching
	*	group codes in order that they were entered and the values that were found.
	*	This search is recommended as it is very fast and more efficient than {@link #find()}.
	*	@param results A user supplied container of all the matching group 
	*	codes and values delimited by a ':'.
	*	@return true if search produced at least one match and false otherwise.
	*	@since 1.1
	*/
	public boolean find(Vector results)
	{
		return find(null,results);
	}
	
	
	/////////////////////////////////// New Find /////////////////////////////////
	/** Makes a search of a specific layer for pre-applied group codes (see {@link #addSearchCode(Integer)}
	*	for setting group codes that you are interested in. The Hashtable results contains all matching
	*	group codes in order that they were entered and the values that were found.
	*	This search is recommended as it is very fast and more efficient than {@link #find()}.
	*	@param layer String name of the layer to search. A null value will cause the search engine 
	*	to search all layers.
	*	@param results a container of all the matching group codes and values delimited by a ':'.
	*	@return true if search produced at least one match and false otherwise.
	*	@since 1.1
	*/
	public boolean find(String layer, Vector results)
	{
		if (! isAsciiDxf())
		{
			System.err.println("DxfSearchEngine: there is no DXF file to search or the dxf if binary.");
			return false;
		}
		
		this.layer = layer;
		// This is where we store the contents of each section for searching.
		Vector buffer = new Vector();
		boolean retVal = false;
		try
		{
			BufferedReader DxfStrm = new BufferedReader(
				new FileReader(dxfFile));
			
			while (readSection(DxfStrm, buffer) == true)
			{
				if (isEligibleForSearch(buffer))
				{
					retVal = matchGroupCodes(buffer, results);
				}
			}
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
		
		return retVal;
	}


	/** This method takes a buffer of groupcodes from the DXF, searches for all of interesting
	*	groupcodes and when one is found adds it to the second argument Hashtable.
	*	If the buffer is empty results is unchanged.
	*/
	protected boolean matchGroupCodes(Vector buff, Vector results)
	{
		if (buff == null)
		{
			return false;
		}
		
		boolean retVal = false;
		
		for (int i = 0; i < buff.size(); i++)
		{
			if (i % 2 == 0) // first, and all odd indexes in buffer are group codes. 
			{
				String testCode = (String)(buff.get(i));
				
				if (isInterestingCode(testCode))
				{
					if (results != null)
					{
						results.add(testCode+":"+buff.get(i +1));
						retVal = true;
					}
					else
					{
						// stop the hunt; there is no where to store results
						// and we found at least one.
						return true;
					}
				}
			}  // end if
		}  // end for
		
		return retVal;
	}


	/** Determines if this layer is the layer we wish to search on.
	*	@return true if the buffer contains a layer identified for a search or
	*	no search layer is identified and
	*	false if it is not the indicated layer, if the buffer is empty or null. 
	*/
	protected boolean isEligibleForSearch(Vector buff)
	{
		// Check if the buffer is empty or null; that would obviously be a problem to search.
		if (buff != null && ! buff.isEmpty())
		{
			// Do we have a SECTION that requires searching.
			if ((getCurrentSection() & sectionInclude) != 0)
			{
				if (isLayerSearch())
				{
					// Does the Vector contain a code '8' (the layer name).
					int layerCode = buff.indexOf("8");
					if (layerCode < 0)
					{
						return false;
					}
					
					// We check the layer name (group code value).
					if (((String)(buff.get(layerCode +1))).equalsIgnoreCase(layer))
					{
						return true;
					}
				}  // end if
				else
				{
					// we are not searching any perticular layer, but all of them.
					return true;
				}
			}	// is not an interesting Section.
		}  // end if
		
		// Buffer was null and or empty.
		return false;
	}


	
	/** Reads an entire section of a DXF file. A section consists of 
	*	all of the group codes which start with group code zero '0' and
	*	continue until the next group code zero.
	*	@return false if the section was an end of file section code and true if the section was read.
	*	The vector buffer will be filled with the content
	*	of the section without the initil group code and value, if the buffer is not null.
	*	The method will 
	*/
	protected boolean readSection(BufferedReader bRead, Vector buff) throws IOException
	{
		// read the group value pairs from the stream until you find the next section (or entity).
		String code = new String();
		String value = new String();
		buff.clear();
		
		while (true)
		{
			code = (bRead.readLine()).trim();
			value = (bRead.readLine()).trim();
			
			if (buff != null)
			{
				buff.add(code);
				buff.add(value);
			}
			
			if (code.equals("2"))
			{
				setCurrentSection(value);
			}
							
			if ( code.equals("0"))
			{
				if (value.equalsIgnoreCase("EOF"))
				{
					break;
				}
				
				// Just another section so we're finished here.
				return true;
			}  // end if
		}  // end while
		
		return false;
	}
	
	
	
	/** Sets the current SECTION of the DXF we are currently in.
	*/
	protected void setCurrentSection(String value)
	{	
		if (value.equalsIgnoreCase("HEADER"))
		{
			thisSection = HEADER;
			return;
		}
		else if (value.equalsIgnoreCase("CLASSES"))
		{
			thisSection = CLASSES;
			return;
		}
		else if (value.equalsIgnoreCase("TABLES"))
		{
			thisSection = TABLES;
			return;
		}
		else if (value.equalsIgnoreCase("BLOCKS"))
		{
			thisSection = BLOCKS;
			return;
		}
		else if (value.equalsIgnoreCase("ENTITIES"))
		{
			thisSection = ENTITIES;
			return;
		}
		else if (value.equalsIgnoreCase("OBJECTS"))
		{
			thisSection = OBJECTS;
			return;
		}
		else if (value.equalsIgnoreCase("THUMBNAIL"))
		{
			thisSection = THUMBNAIL;
			return;
		}
		
	}
	
	/** Returns the current section of the DXF being read as an integer.
	*/
	public int getCurrentSection()
	{
		return thisSection;
	}
	
	
	/** This method does the actual searching of the Dxf file and is never called directly.
	*/
	protected String find()
	{	
		if (! isAsciiDxf())
		{
			System.err.println("DxfSearchEngine: there is no DXF file to search or the dxf if binary.");
			return null;
		}
		
		if (pattern == null)
		{
			System.err.println("DxfSearchEngine: search pattern is null.");
			return null;
		}
		
		try
		{
			BufferedReader DxfStrm = new BufferedReader(
				new FileReader(dxfFile));
			// Local count of line numbers in the dxf file with which
			// we will compare locations to the last line number we searched
			// from
			int myDxfLineNum = 0;

			// DXF Code number.
			String code = new String();

			// Value String read from dxf file.
			String value = new String();
			
			while (! (code.equals("0") && value.equalsIgnoreCase("EOF")))
			{
				// read the code half of the group pair.
				code = DxfStrm.readLine();
				code = code.trim();
				// Advance line number.
				myDxfLineNum++;
				// read the value half of the group pair.
				value = DxfStrm.readLine();
				value = value.trim();
				myDxfLineNum++;

				// Now we check for a layer information
				// We do this here even if we are doing a repeat search 
				// so we don't miss tagging an important layer.
				if (isLayerSearch())
				{
					// Layer is group code '8'
					if (code.equals("8")) 
					{
						if (value.equalsIgnoreCase(layer))  // see if this is a sensitive layer
						{
							validLayer = true;
						}
						else
						{
							// This turns off search sensitivity if we are finished searching a layer.
							validLayer = false;	
						}
					}  // end if
				}  // end if
				
				// Skip upto dxfLineNum; used with findAgain()
				if (myDxfLineNum <= dxfLineNum)
				{
					continue;
				}
				
				dxfLineNum = myDxfLineNum;
				
				if (isInterestingCode(code) && validLayer == true)
				{
					Matcher m = pattern.matcher(value);
					if (m.find())
					{
						return value;
					}  // end if 
				}  // end if
			}  // end while
			// close the stream
			DxfStrm.close();
		}
		catch (IOException e)
		{
			System.err.println("DxfSearchEngine dxf read error: "+e+" at line: "+dxfLineNum);
		}
		catch (NullPointerException npe)
		{
			String msg = "Search for pattern '"+pattern+ " in file '"+ dxfFile.getName() +"' " +
				"' produced a null pointer exception. The DXF may be corrupt at line: "+
				String.valueOf(dxfLineNum);
			System.err.println(msg);
			DxfPreprocessor.logEvent(dxfFile.getName(), msg);
			System.exit(-1);
		}
		
		return null;
	}
	
	
	
	/** Allows significant codes to be identified. All Strings in DXF are preceeded by a code
	*	value that indicates what type of String they are. Codes like '0' represent SECTION strings
	*	Codes like 999 are comment strings.
	*	<P>
	*	The default search code is '1' and it does not need to be added for the search to be 
	*	performed. To remove a search code use the {@link #removeSearchCode} method.
	*/
	public void addSearchCode(Integer i)
	{	
		if (! significantCodes.contains(i))
		{
			significantCodes.add(i);
		}
	}
	
	
	
	/** This method removes a search code from the list of codes to search stack. If there
	*	are multipule codes of the same value only the first will be removed, so in that 
	*	case there may be a requirement to call this method until the it returns false.
	*/
	public boolean removeSearchCode(Integer i)
	{
		return significantCodes.remove(i);
	}
	
	
	
	/** Displays the codes that this object has been trained to focus on as significant.
	*/
	public void displaySearchCodes()
	{
		System.out.println("The following codes are significant:");
		for (int i = 0; i < significantCodes.size(); i++)
		{
			Integer integer = (Integer)significantCodes.get(i);
			System.out.print("values under group code "+integer.intValue());
			if (integer.intValue() == 1)
			{
				System.out.print(" (default)");
			}
			System.out.println();
		}  // end for
		System.out.println("===================================");
	}

	
	
	/** Returns true if the file is an ASCII DXF file, and false if it is
	*	anything else.
	*/
	public boolean isAsciiDxf()
	{	
		boolean isAscii = false;
		
		// If the file descriptor is not a normal file notify client.
		if (dxfFile == null)
		{
			System.err.println("DXF File is null.");
			return false;
		}
		
		if (! dxfFile.isFile())
		{
			System.err.println("DxfSearchEngine: "+dxfFile.getName()+" could not be found.");
			return false;
		}

		
		try
		{
			BufferedReader DxfStrm = new BufferedReader(
				new FileReader(dxfFile));

			String testStr = DxfStrm.readLine();
			testStr = testStr.trim();

			// Standard DXF starts with 0 and SECTION.
			if (testStr.equals("0"))
			{
				testStr = DxfStrm.readLine();
				if (testStr.equals("SECTION"))
				{
					isAscii = true;
				}
			}
			// Dxf starts with a comment. Rare but possible.
			else if (testStr.equals("999")) 
			{
				isAscii = true;
			}
			
			DxfStrm.close();
		}
		catch (IOException e)
		{
			System.err.println("DxfSearchEngine error: "+e);
		}
		
		return isAscii;
	}
	
	
	
	/** Returns true if the passed code string contains a code we are interested in searching.
	*	@throws NumberFormatException if the String does not contain a valid integer.
	*/
	protected boolean isInterestingCode(String s)
	{
		// Decode the string value.
		if (s.equals(""))
		{
			return false;
		}
		
		int code = Integer.parseInt(s);
		
		Integer intCode;
		
		// Check the other interesting values.
		for (int i = 0; i < significantCodes.size(); i++)
		{
			intCode = (Integer)significantCodes.get(i);
			if (code == intCode.intValue())
			{
				return true;
			}
		}
		
		return false;
	}
} // end class