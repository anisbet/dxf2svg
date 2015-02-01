
/****************************************************************************
**
**	FileName:	SvgNotes.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Takes the information from the DXF file and grabs all of
**				the notes from the file and orders them and outputs to 
**				JavaScript array.
**
**	Date:		April 19, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.1)
**
**	Version:	0.1 - April 19, 2004
**				0.2 - May 3, 2004 Moved from test environment to Dxf2Svg production.
**				0.3 - August 31, 2004 Updated the Regex that identifies index numbers
**				in SvgNoteString.testNote().
**				0.4 - October 13, 2004 Added method getMaxIndexNumber() (see notes)
**				and in method getCompiledNoteStrings() I have make the noteStringArray
**				the size of the maximum index number (see notes). Removed the insertion
**				of a index value of zero set to 'null' as it is no longer required.
**				This fixes a reported bug were Notes compile incorrectly if there is 
**				only one note but its number is not '1', but '2' or anything else;
**				yesssss it does happen.
**				0.5 - Noveber 4, 2004 Replace double quotes for single quotes so they
**				are interpreted correctly by javascript in method getNotesAsJavaScript().
**				0.51 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:		
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.io.File;
import java.util.*;
import java.util.regex.*;	// for finding index numbers.
import dxf2svg.util.*;
import dxf2svg.SvgBuilder;
import dxf2svg.DxfPreprocessor;


/** This class will generate the notes for a discreet series of illustration 
*	sheets. The sheets that belong to the same family will have any notes that
*	are appropriate to those sheets included as pop-up text or tooltips when
*	the user <EM>mouseover</EM>s the note number.
*	<P>
*	<B>How It Works</B>
*	<P>
*	This is the pattern for a note that SvgNotes recognizes:
*	<BR>
*	<IMG SRC="../../images/npat.png" ALT="Notes in AutoCAD" BORDER=0>
*	<P>
*	If the generateNotes switch is used, the DXF 
*	files are scoured for text on the layers NOTESENG, NOTESFRE and NOTENUMS.
*	The text is extracted, a determination of the ordering of multi-line text
*	is made as well as the number of the note. Once individual lines of text
*	have been isolated, they are stored in order ready for outputting via 
*	{@link dxf2svg.SvgBuilder#addJavaScript(java.lang.String)} method.
*	<P>
*	This class makes heavy use of the {@link dxf2svg.FigureSheetDatabase}
*	to retrieve related sheets within a figure. To retain backward compatability
*	the {@link dxf2svg.FigureSheetDatabase} will not be modified. This is
*	to maintain readability from the serialization/de-serialization process.
*	<P>
*	If the -db switch is not used a new database will be created or any existing
*	database in the current directory will be opened read and modified as required.
*	If the -db switch is used the path database specified will be used and modified
*	as required.
*	<P>
*	It is assumed that the files in the current directory are related or that the
*	existing database contains entries for the files being currently converted.
*	<P>
*	Each file that is added to the SvgNotes object will be checked for notes. If more
*	than one file contains notes (which is not allowed in the AG specification) the
*	behaviour is to over-write any previously collected notes completely. Another 
*	behaviour modification is to stop searching each file if notes were already found on
*	one of the sheets that was added (see {@link #hasNotes()}).
*	<P>
*	It is the responsibility of this class to mediate between the current file conversion
*	and FigureSheetDatabase, to maintain all the notes for all the figures that
*	are required for any figure and all of its sheets.
*	
*	@version 	0.3 - August 31, 2004
*	@author		Andrew Nisbet
*/

public class SvgNotes
{
	private Vector engNotes;		// All English processed SvgNoteStrings.
	private Vector freNotes;		// All French SvgNoteStrings. 
	private Vector mySheets;		// Names of the sheets of this figure.
	
	// These are defined in Dxf2SvgConstants.ENGLISH and appear for reference only.
	//public final static int ENGLISH = 1;  // French language processing.
	//public final static int FRENCH  = 2;  // English language processing
	
	private String lang1ArrayName;	// Name of the array used in the javascript to store the 
		// notes for language 1 notes.
	private String lang2ArrayName;	// ditto, but for the second language.
	private String lang1LayerName;	// Name of the language layer for language 1 usually NOTESENG.
	private String lang2LayerName;	// ditto, but for the second language.
	
	private int language;			// Current language setting.
	private String javaScript;		// The JavaScript version of the notes.
	private DxfSearchEngine dxfSearch;
	private boolean isRelevantToConversion;	// True if the SvgNotes are required for the files currently
									// being converted.
	private boolean foundNotes;		// Flag that indicates that notes have been found on one of the
									// sheets of this SvgNotes Object.
									
	private static int count = 0;	// test, remove without repercushions.
	
	///////////// test value //////////
	private String currentFile;
	///////////// test value //////////
	
	
	////////////////////////////////
	//      Constructor(s)        //
	////////////////////////////////
	/** Double argument constructor that takes a Vector of dxf file paths and a Hashtable
	*	where the sheet number is the 
	*	Hashtable's key and the value is a Vector that is a record that contains boardnos,
	*	titles, spotcalls and the like.
	*	@param dxfFiles List of all DXF files that the client want converted to SVG. If the 
	*	Vector of file names is null or empty then a call to {@link #isRelevantToConversion()}
	*	returns false and the constructor returns without
	*	@param figure Hashtable of all the sheet numbers versus the records of the sheet's info.
	*	@throws NullPointerException if the argument Hashtable is null.
	*	@throws ClassCastException if the Object stored as a value is not a Vector.
	*/
	public SvgNotes(Vector dxfFiles, Hashtable figure)
	{
		init();
		if (figure == null)
		{
			throw new NullPointerException(this.getClass().getName() +
				" Could not create object because the contructor received a null argument."+
				" Expected a non-null Hashtable with sheet numbers as keys and Vectors of"+
				" Strings that contain boardnos, titles, spotcalls and the like.");
		}
		 
		if (dxfFiles == null)
		{
			throw new NullPointerException(this.getClass().getName() +
				" Could not create object because the contructor received a null argument."+
				" Expected a non-null Vector of with names and paths to DXF files");
		}
		
		count++;
		
		Set keys = figure.keySet();
		Iterator keysIt = keys.iterator();
		String recordName = null;
		Vector record = null;
		String spotcall = null;
		
		while (keysIt.hasNext())
		{
			// we need each of the records to search for the spotcalls of this figure.
			// We will retain the 
			recordName = (String)(keysIt.next());

			record = (Vector)(figure.get(recordName));
			
			if (record == null)
			{
				System.err.println(this.getClass().getName() + " constructor() " +
					" ...hmmm, the record with the name "+record+" is a key to a null value." +
					" That means that the sheet has no data and is null.");
			}
			
			// Now we have the name(s) of the files that pertain to this figure
			// let's save them. The spot call or original file name of the converted file
			// is always stored at index 4 of the zero indexed Vector record.
			spotcall = (String)(record.get(4));
			
			// Place name of sheet number 1 at the front of the line. 
			// If we don't find a sheet 1 (because of 1a or something, no big deal we'll search them all).
			if (recordName.equals("1"))
			{
				mySheets.insertElementAt(spotcall, 0);
			}
			else
			{
				mySheets.add(spotcall);
			}
		}
		
		
		// Now we need to search the list of supplied dxf files because the items in the database
		// are not necessarily the ones that are going to be converted. This has implications 
		// for the conversion process. What do we do if the list of dxf files contains only some
		// of the files that are listed in the database.
		//
		//
		// Here are the rules:
		//
		// 1) The database contains all the files and only the files that are going to be 
		// converted. No problem. This would happen if you were converting a directory that
		// contains no previous database. Remember the default action of the database is to 
		// look for a previously available database and try and open it (throw an exception
		// if you can't). If there is no previously existing database then make one.
		//
		// 2) Find a previously existing database. This may contain more information than we 
		// need which in turn will create massive numbers of unused SvgNotes objects. To 
		// combat that we will take the list of dxf files and compare them to the list of 
		// sheets that we have stored. If the two don't match do one of two things.
		//
		// 	2-1) If the SvgNotes object has more sheets listed than is found in the list
		//	of dxf files this means that a partial conversion is going to take place. I am
		//	not sure this should be allowed. What would happen if the sheet with the notes 
		//	was not one of the files to be converted? Is it worth throwing an exception or
		//	just issue a message?
		//
		//	2-2) What do we do in the case where we find other sheets that are in the dxf list
		//	but not on our list of sheets. This would not be a problem because when the data-
		//	base was created it added or updated all additional sheets that are in the path 
		//	of the conversion.
		
		// Here we will remove each of our sheets from the dxf list. If we do not find one
		// of the sheets we should alert the user.
		String sheetName = null;
		String sheetNameAndPath = null;
		int missingSheetCount = 0;
		Vector missingSheetNames = null;
		Vector foundSheetNamesPaths = null;
		for (int i = 0; i < mySheets.size(); i++)
		{
			sheetName = (String)(mySheets.get(i));
			sheetNameAndPath = containsSheetNamed(dxfFiles, sheetName);
			if (sheetNameAndPath != null && sheetNameAndPath.length() > 0)
			{
				if (foundSheetNamesPaths == null)
				{
					foundSheetNamesPaths = new Vector();
				}
				// Collect all the sheet names so we can use #addSheet() method.
				//////////////////////////////////////////
				//              CAVEAT                  //
				//////////////////////////////////////////
				// Here there is a breakdown in logic   //
				// that could potentially have some     //
				// problems. Because the name searched  // 
				// here are just spot calls, and the    //
				// we are trying to do a lookup in a    //
				// table that contains complete path    //
				// info, there may be a case where there//
				// will be one spotcall and two files   //
				// that are to be converted. If that    //
				// happens this is what will occur:     //
				// The spotcall will match the first    //
				// path on the list and the second will //
				// not get searched. If that happens    //
				// you have two files in different dirs //
				// with the same name which SHOULD never//
				// happen.                              //
				//////////////////////////////////////////
				foundSheetNamesPaths.add(sheetNameAndPath);
			}
			else
			{
				if (missingSheetNames == null)
				{
					missingSheetNames = new Vector();
				}
				missingSheetNames.add(sheetName);
				missingSheetCount++;
			}
		}
		
		// To get here we have found all of our sheets in the list of dxf files to be converted
		// or alternatively we found none of the sheet. Either state is acceptable. If all were
		// found then the object is relevant and we can continue to the next step. If none were
		// found then the object is not relevant and can be disposed of. If only some sheets 
		// were found throw an exception.
		if (missingSheetCount == mySheets.size())
		{
			isRelevantToConversion = false;
			return;
		}
		else if (missingSheetCount > 0 && missingSheetCount < mySheets.size())
		{
			throw new SheetMissingFromNoteCollectionException(missingSheetNames);
		}
		
		// Now we have relevant notes we need to process the sheets one by one and collect 
		// the notes off of them. To do that we are going to need the files with their 
		// pathing information.
		// Empty the mySheets vector as it only holds the names of the files and we need to
		// put fully qualified paths on this vector. We have them in foundSheetNamesPaths so
		// we will add the paths from that vector to mySheets, but we want that vector empty
		// first or it will throw a FileNotFoundException when the DxfSearchEngine tries to
		// open it.
		mySheets.removeAllElements();
		
		// Now add the sheets and hopefully we find the notes on the first sheet (see addSheet()).
		for (int i = 0; i < foundSheetNamesPaths.size(); i++)
		{
			addSheet((String)(foundSheetNamesPaths.get(i)));
		}
	}
	
	
	/** Default constructor.
	*/
	public SvgNotes()
	{
		init();
	}
	
	
	////////////////////////////////
	//         Methods            //
	////////////////////////////////
	/** Allows multipule constructors to perform the same tasks that all SvgNotes require
	*	before performing the unique tasks required to set up the object to the specifications
	*	required by the constructor. You never need to call this method explicitly; it is done
	*	for you by the constructors.
	*/
	protected void init()
	{
		///////////// test value //////////
		currentFile = new String();
		///////////// test value //////////
		foundNotes = false;
		isRelevantToConversion = true;
		lang1ArrayName = "englishNote";
		lang2ArrayName = "frenchNote";
		//lang1LayerName = "NOTESENG"; 
		lang1LayerName = TableLayer.getNoteLayerName(Dxf2SvgConstants.ENGLISH);
		//lang2LayerName = "NOTESFRE";
		lang2LayerName = TableLayer.getNoteLayerName(Dxf2SvgConstants.FRENCH);
		engNotes = new Vector();
		freNotes = new Vector();
		mySheets = new Vector();
		dxfSearch = new DxfSearchEngine(); // Does the searching of the DXFs.
		// Default search for the String (code 1) already is set.
		dxfSearch.addSearchCode(new Integer(10));			// x location as a double
		dxfSearch.addSearchCode(new Integer(20));			// y location as a double
	}
	
	
	/** This method returns the fully qualified path of the file that matches the arg string
	*	'name' on the list 'files'. Returns null if the file name could not be found.
	*/
	private String containsSheetNamed(Vector files, String name)
	{
		
		String dxfName = null;
		for (int i = 0; i < files.size(); i++)
		{
			dxfName = DxfPreprocessor.getNormalizedFileName((String)(files.get(i)));
			if (dxfName.equalsIgnoreCase(name))
			{
				return (String)(files.get(i));
			}
		}
		
		return null;
	}
	
	/** Returns true if this object was deemed to be relevant to the current conversion 
	*	and false otherwise. Its relevancy is determined by checking the list of dxf files
	*	to be converted (passed to the constructor). The SvgNotes object knows (from the 
	*	database (also passed to the constructor)) what all of its sheets are. If this 
	*	object contains a figure (and therefore all the sheets) that match the names of 
	*	dxf files that are going to be converted, then it is deemed relevant and true is 
	*	returned. If false is returned this object can be discarded and will have no
	*	effect on the conversion.
	*	<P>
	*	Calling this method on an SvgNotes Object that was created with the default 
	*	no argument constructor will always return true.
	*/
	public boolean isRelevantToConversion()
	{
		return isRelevantToConversion;
	}
	
	


	/** Allows the client to change the name of the layer to search for notes. The user could
	*	change the name of the layer that French notes will appear on by calling this method
	*	and specifying the language type and the layer name to search on. The default layer
	*	names for searching are: "NOTESENG" and "NOTESFRE".
	*	@param language language expected on the layer.
	*	@param layerName name of the layer that the notes will appear on.
	*/
	public void changeNoteLayerName(int language, String layerName)
	{
		if (layerName == null)
		{
			System.err.println("SvgNotes.changeNoteLayerName() Error name of the layer to search for notes is null.");
			return;
		}
		
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				lang1LayerName = layerName;
				break;
				
			case Dxf2SvgConstants.FRENCH:
				lang2LayerName = layerName;
				break;
				
			default:
				System.err.println("SvgNotes.changeNoteLayerName() Error unsupported language. Value: "+language);
		}	
	}

	
	
	/** Adds the a figure family sheet member to this group of notes. All the sheets of a given 
	*	figure are added to an SvgNotes object regardless of if they have notes or not. Each time
	*	one is added it is searched for notes. Notes should only be found on the last sheet of
	*	a given illustration but this method searches all sheets and if additional notes are 
	*	found they are added to the entire collection of notes. These in turn will appear on
	*	each family member when the user mouses over a specific note number.
	*/
	public void addSheet(String file)
	{
		/////////// test value ////////
		currentFile = file;
		/////////// test value ////////
		
		mySheets.add(file);
		
		if (foundNotes == false)
		{
			
			dxfSearch.setDxfFile(new File(file));
			if (dxfSearch.find(lang1LayerName, engNotes) == true)   // Search layer NOTESENG for text
			{
				language |= Dxf2SvgConstants.ENGLISH;
				orderNotes(Dxf2SvgConstants.ENGLISH);
				// This stops repeated searches for notes once one set is found.
				foundNotes = true;
			}
		
			if (dxfSearch.find(lang2LayerName, freNotes) == true)   // Search layer NOTESFRE for all text and x,y coords.
			{
				language |= Dxf2SvgConstants.FRENCH;
				orderNotes(Dxf2SvgConstants.FRENCH);
				foundNotes = true;
			}
		
			// preprocess the javascript ready for when the SvgBuilder needs it.
			javaScript = getNotesAsJavaScript();
			//System.out.println("Well, here it is...");
			//System.out.println(javaScript);
		}
	}
	
 
	
	
	/** This call back method passes the name of the file being currently converted which acts
	*	like a signal to SvgNotes to call the {@link dxf2svg.SvgBuilder#addJavaScript(java.lang.String)} method
	*	if required. If this series of sheets has no notes or the argument file name is not a 
	*	member sheet of this figure then nothing happens and nothing is returned.
	*/
	public boolean getNotes(SvgBuilder svgb, String fileName)
	{
		// test if there were notes then look and see if the argument file name is a sheet
		// of the figure that these notes apply to.
		if (this.hasNotes() && isMemberSheet(fileName))
		{
			svgb.addJavaScript(javaScript);
			return true;
		}
		
		return false;
	}
	
	
	/** Allows external clients to change the name of the javascript arrays that are output.
	*	Default: {@link Dxf2SvgConstants#ENGLISH} = englishNote; {@link Dxf2SvgConstants#FRENCH} = frenchNote.
	*/
	public boolean changeJavaScriptNoteArrayName(int language, String altName)
	{
		if (altName == null || altName.length() < 1)
		{
			return false;	
		}
		
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				lang1ArrayName = altName;
				break;
				
			case Dxf2SvgConstants.FRENCH:
				lang2ArrayName = altName;
				break;
				
			default:
				System.err.println("SvgNotes.changeJavaScriptNoteArrayName() Error unsupported language. Value: "+language);
				return false;
		}
		// Rerun the getNotesAsJavaScript() to update the new names.
		javaScript = getNotesAsJavaScript();
		
		return true;
	}
	
	
	/** Returns true if notes were found on one of the sheets that this object represents.
	*	and false otherwise. It does this by checking a bitset value that contains the languages
	*	found in the file. If the bitset value is not '0' then notes were found.
	*/
	public boolean hasNotes()
	{
		if (language != 0)
		{
			return true;
		}
		
		return false;
	}
	
	
	/** Returns true if the file name argument is a member of this collection of sheets that these
	*	notes apply to. The arg fileName is compared to a fully qualified path.
	*	@return true if the file name argument is one of the files to which these notes apply.
	*/
	public boolean isMemberSheet(String fileName)
	{
		if (fileName == null || fileName.length() < 1)
		{
			return false;
		}
		// We have to get a reference to the conversion 
		String name = new String();
		for (int i = 0; i < mySheets.size(); i++)
		{
			name = (String)(mySheets.get(i));
			if (name.equals(fileName))
			{
				return true;
			}
		}
		
		return false;
	}
	

	
	/** Formats any notes into a form acceptable for use in the SVG's JavaScript functions.
	*	The format is a declared array with each element in the array representing a different
	*	note string where the index in the array matches the number of the note stored there.
	*	<P>
	*	This method also replaces double quotes with single quotes (if required).
	*/
	public String getNotesAsJavaScript()
	{
		StringBuffer sb = new StringBuffer();
		int j;

		if ((language & Dxf2SvgConstants.ENGLISH) > 0)
		{
			// Here are the preliminaries where we create the JavaScript array for each of the languages.
			// var note = new Array;
			// note[0] = null;
			// note[1] = 'SOME NOTE TEXT FOR DEMO PURPOSES.';
			// note[2] = 'AIRCRAFT 130334 THROUGH 130335.';
			// etc. ...
			
			sb.append("var "+lang1ArrayName+" = new Array;\n");
			sb.append(lang1ArrayName+"[0]=null;\n");
			for (int i = 1; i < engNotes.size(); i++)
			{
				sb.append(lang1ArrayName+"[" + i + "]=\"" + (engNotes.get(i)).toString().replace('\"','\'')
					+ "\";\n");
			} // end for.
			sb.append("\n");
		}
		
		if ((language & Dxf2SvgConstants.FRENCH) > 0)
		{
			sb.append("var "+lang2ArrayName+" = new Array;\n");
			sb.append(lang2ArrayName+"[0]=null;\n");
			for (int i = 1; i < freNotes.size(); i++)
			{
				sb.append(lang2ArrayName+"[" + i + "]=\"" + (freNotes.get(i)).toString().replace('\"','\'')
					+ "\";\n");
			} // end for.
			sb.append("\n");			
		}
		
		return sb.toString();
	}

	
	
	/** Takes a Vector of raw Strings that are the notes from the DXF and groups and orders them.
	*/
	protected void orderNotes(int language)
	{
		Vector noteStrings = new Vector();
		// In order to order the notes we need to make these into SvgNoteStrings. These
		// are strings that know their AutoCAD location and how to compare themselves.
		makeNoteStrings(language, noteStrings);
		
		sortNoteStrings(language, noteStrings);
	}
	
	
	
	/** This method sorts the individual note strings into their logical groupings.
	*	There are three different groups. Each SvgNoteString must belong to
	*	one of these groups:
	*	<P>
	*	<OL>
	*		<LI> Note index number. This value does not appear in the final output in the SVG file
	*		but is used as an index to the array of note strings in the SVG's JavaScript popup note
	*		function.
	*		<LI> The String is the initial note string. This is the first string of the note and
	*		is located on the same 'y' coordinate of the index number. All other related strings
	*		are appended to this string.
	*		<LI> The String is a continuation of a single line that was broken into other lines 
	*		of text. If a line is so long that it doesn't fit the bounderies given it is broken
	*		down into shorter strings of text. But in AutoCAD we don't use paragraph text so the
	*		strings have no knowledge of the fact that they are one of several strings that make
	*		up one note. These strings all share the same 'x' coordinate value but adhere to a 
	*		relatively small range of distances that determine if they are related. If two strings's
	*		'x' value is less than 0.1" (for standard note text), it can be considered different
	*		lines of the same note. If they are greater we it indicates that the note text is 
	*		either a string that belongs to a note 3 notes away or if it is within a certain distance
	*		say between 0.2" and 0.25" then you have leading between the last line of one note
	*		and the first line of another note.
	*	</OL>
	*/
	protected void sortNoteStrings(int language, Vector noteStrings)
	{
		// First job, find the number SvgNoteStrings
		Vector indexNumbers = new Vector();
		
		getIndexNumbers(noteStrings, indexNumbers);
		
		// iterate over the Vector of numbers and collect the initial strings and all other 
		// related note strings.
		getCompiledNoteStrings(noteStrings, indexNumbers);
		// Now populate the correct Vector according to language
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				engNotes = new Vector(noteStrings);
				break;
			
			case Dxf2SvgConstants.FRENCH:
				freNotes = new Vector(noteStrings);
				break;
				
			default:
				System.err.println("SvgNotes.sortNoteStrings() Error unsupported language. Value: "+language);
				return;			
		}
	}
	
	
	
	
	/** Searches for the initial String of a note based on the note number.
	*/
	protected void getCompiledNoteStrings(Vector noteStrings, Vector indexNumbers)
	{
		SvgNoteString index = null;
		String note = new String();
		
		// We need to know the maximum index number. The goal is to create an array of strings whose index
		// number is their note number. Example note '1' will be noteArray[1]. Well it turns out
		// that it is valid to have only one note whose number is '2'. We need to know that so
		// we can make all the extra null strings so our note numbers and indexes match.
		int maxIndex = getMaxIndexNumber(indexNumbers);
		String[] noteStringArray = new String[maxIndex +1];
		// We do this because there are cases where the first note on an illustration 
		// is numbered '2'. Yessss, this makes NO sense what-ever, but it is valid.
		
		try
		{
			for (int i = 0; i < indexNumbers.size(); i++)
			{
				index = (SvgNoteString)(indexNumbers.get(i));
				note = findNote(index, noteStrings);
				
				noteStringArray[index.getAsIndexNumber()] = note;
			}
			
			// Empty the original container and refill it with the list of notes in order.
			noteStrings.removeAllElements();
			
			for (int i = 0; i < noteStringArray.length; i++)
			{
				if (noteStringArray[i] != null)
				{
					noteStrings.add(noteStringArray[i]);
				}
				else
				{
					noteStrings.add("null");
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{

			System.out.println("current file = " + currentFile);
			System.out.println("This file may contain notes that are incorrectly positioned in");
			System.out.println("relation to one-another. For instance this Exception appears if");
			System.out.println("the index number of a note is too near or too far from the note");
			System.out.println("that references it in the DXF file.");
			System.out.println("Another reason this can occur is if the first note is not numbered '1'.");
			DxfPreprocessor.logEvent(currentFile, "note index number location may not match note string location and is perhaps missing or index number does not start with 1.");
		}
	}
	
	/** Returns the maximum index number. The goal is to create an array of strings whose index
	*	number is their note number. Example note '1' will be noteArray[1]. Well it turns out
	*	that it is valid to have only one note whose number is '2'. We need to know that so
	*	we can make all the extra null strings so our note numbers and indexes match.
	*	@throws ClassCastException if an object in the vector indexes is not a {@link SvgNoteString}.
	*/
	private int getMaxIndexNumber(Vector indexes)
	{
		Iterator it = indexes.iterator();
		int maxValue = 0;
		while (it.hasNext())
		{
			int value = ((SvgNoteString)(it.next())).getAsIndexNumber();
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		return maxValue;
	}
	
	
	/** This method finds all related Strings that are notes and returns them as a single
	*	String.
	*/
	private String findNote(SvgNoteString index, Vector v)
	{
		StringBuffer outBuff = new StringBuffer();
		SvgNoteString testNote = null;
		int intIndex = index.getAsIndexNumber();
		for (int i = 0; i < v.size(); i++)
		{
			testNote = (SvgNoteString)(v.get(i));
			// Let's find the initial string; it will be the one that
			// lines up horizontally. There will be only one.
			// If they are text and number then just paste the content and go on.
			if (testNote.isText() && testNote.isNumber())
			{
				// If the index number and note are one-and-the-same then just paste it.
				outBuff.append(testNote.getContent());
				break;
			}
			
			if (index.isHorizontallyAligned(testNote))
			{
				outBuff.append(testNote.getContent());
				break;
			}
		}
		
		// Now we go and find the other contents and append them
		findNextNoteString(testNote, v, outBuff);
		
		// All notes have extra space at end of each line. Remove the unnecessary space from the
		// end of the whole line. If you don't convert to string first the string will not trim.
		// This has to do with trim() returning a copy of the string if it has leading or trailing
		// white space.
		String ret = outBuff.toString().trim();
		return ret;
	}
	
	
	
	
	/** This method finds the next note string. It does this by comparing the current string to 
	*	all of the other strings and selects the next note based on the next nearest string with
	*	the smaller y value. This method is called recursively.
	*	@return true if there was more related text found and false otherwise.
	*	@param SvgNoteString the current SvgNoteString that is used as a comparitor.
	*	@param v other SvgNoteStrings to search.
	*	@param outBuff where all the content is collected.
	*/
	private void findNextNoteString(SvgNoteString s, Vector v, StringBuffer outBuff)
	{
		SvgNoteString nextNote = null;
		//StringBuffer outBuff = new StringBuffer();
		for (int i = 0; i < v.size(); i++)
		{
			nextNote = (SvgNoteString)(v.get(i));
			
			// Let's find the initial string; it will be the one that
			// lines up horizontally. There will be only one.
			if (s.isVerticallyAligned(nextNote))
			{
				outBuff.append(nextNote.getContent());
				// This is required because the AutoCAD Strings are not terminated with a space
				// that is required or the next string butts up to the first with no space.
				findNextNoteString(nextNote, v, outBuff);
				return;
			}  // end if
		}  // end for
	}  // end method.
	
	
	
	
	
	/** Returns a Vector of all the index number SvgNoteStrings.
	*	@param src Unsorted Vector of all SvgNoteStrings.
	*	@param dest Vector of index numbers in the form of SvgNoteStrings.
	*	@throws ClassCastException if the source Vector contains anything other
	*	than an SvgNoteString or a SvgNoteString sub-class.
	*/
	protected void getIndexNumbers(Vector src, Vector dest)
	{
		if (src == null || dest == null)
		{
			return;
		}
		
		for (int i = 0; i < src.size(); i++)
		{
			SvgNoteString sns = (SvgNoteString)(src.get(i));
			if (sns.isNumber())
			{
				dest.add(sns);
			}
		}
	}
	
	
	
	
	/** Takes the delimited data from the DXF file and converts them into {@link SvgNoteString}s.
	*	The data is in the form of a vector that is populated by the {@link DxfSearchEngine} and 
	*	looks like this:
	*	<P>
	*	10:2.1333334<BR>
	*	20:1.2322423<BR>
	*	1:Some Text String.<BR>
	*	<P>
	*/
	protected void makeNoteStrings(int language, Vector notes)
	{
		// To find the initial note Strings we need to find out how many notes there are
		// so we can iterate over the entire collection in a orderly fashion
		
		Iterator noteIt = null;
		switch (language)
		{
			case Dxf2SvgConstants.ENGLISH:
				noteIt = engNotes.iterator();
				break;
				
			case Dxf2SvgConstants.FRENCH:
				noteIt = freNotes.iterator();
				break;
				
			default:
				System.err.println("SvgNotes.getNumberOfNotes() Error unsupported language. Value: "+language);
				return;
		}
		
		String thisCodeValuePair = new String();		// New string object for each note object.
		String[] codeVal = new String[2];
		codeVal[0] = new String();
		codeVal[1] = new String();
		
		String note = new String();
		double noteX = 0.0;
		double noteY = 0.0;
		
		boolean foundX = false;
		boolean foundY = false;
		boolean foundNote = false;
		
		while (noteIt.hasNext())
		{
			thisCodeValuePair = (String)(noteIt.next());
			// We want the data in thisCodeValuePair placed into two strings codeVal[]
			if (parseEntry(thisCodeValuePair, codeVal))
			{
				// Search for content on code 1 not code 10 or 20.
				if (codeVal[0].compareTo("1") == 0)
				{
					note = codeVal[1];
					foundNote = true;
				}  // end if
				// Search for content on code 10 not code 1 or 20.
				else if (codeVal[0].compareTo("10") == 0)
				{
					noteX = Double.parseDouble(codeVal[1]);
					foundX = true;
				}  // end if
				// Search for content on code 1 not code 10 or 20 which would also match pValue.
				else if (codeVal[0].compareTo("20") == 0)
				{
					noteY = Double.parseDouble(codeVal[1]);
					foundY = true;
				}  // end if
				
				if (foundX && foundY && foundNote)
				{
					notes.add(new SvgNoteString(note, noteX, noteY));
					foundX = foundY = foundNote = false;
				}
			}
		}  // end while
	}
	
	
	
	
	
	/** Parses the ':' delimited line from a raw note string Vector entry and passes them
	*	back as an array of Strings. 
	*	@param entries an array of Strings Where String[0] = the group code that was found and
	*	String[1]
	*	contains the value of the group code. If the entry value is null false is returned
	*	and the contents of entries is untouched.
	*	@param entries where entries[0] will contaion the group code, entries[1] will contain
	*	the value of the group code and true will be returned. If the entry String is null 
	*	these values are left untouched.
	*	If the entry does not contain a delimiter entries[0] will contain a copy of the parameter
	*	entry and false will be returned. If entries is null this method will have only the
	*	effect of returning true if the task is accomplishable and false otherwise as outlined
	*	in the text above.
	*	@return true if the String could be broken down into code value pairs and false if 
	*	it cannot find a delimiter or the param entry is null.
	*/
	private boolean parseEntry(String entry, String[] entries)
	{
		if (entry == null)
		{
			return false;
		}
		// Here we are going to search for the first delimiter because the colon may
		// have been used in the content of the String value and we don't want to 
		// split at each occurance just in case.
		int pos = entry.indexOf(":");
		if (pos < 0) // couldn't find a delimiter
		{
			if (entries[0] != null)
			{
				entries[0] = entry;
			}
			return false;
		}
		
		if (entries[0] != null && entries[1] != null)
		{
			entries[0] = entry.substring(0, pos);
			entries[1] = entry.substring(pos +1);
		}
		
		return true;
	}
	
	/** Returns this object as a String that specifies its class and its note contents.
	*/
	public String toString()
	{
		return this.getClass().getName();
	}
	
	

	////////////////////////////////
	//     Internal Class(es)     //
	////////////////////////////////	
	
	/****************************************************************************
	**
	**	FileName:	SvgNoteString.java
	**
	**	Project:	Dxf2Svg
	**
	**	Purpose:	Encapsulates a note String and its position in AutoCAD space.
	**
	**	Date:		April 19, 2004
	**
	**	Author:		Andrew Nisbet
	**
	**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
	**				(build 1.4.1)
	**
	**	Version:	0.1 - April 19, 2004
	**				0.2 - June 21, 2004 Modified toString() to just return the content
	**				not the location. This is in response to a process in the parent
	**				class that is placing NoteString objects on a content Vector instead
	**				of just a String.
	**				0.3 - August 31, 2004 Updated the Regex that identifies index numbers
	**				in testNote()
	**
	**	TODO:		
	**
	
	**
	*****************************************************************************/
	
	/**	This class represents a note string and its x, y location in AutoCAD space.
	*	Note strings always return with an additional trailing space character which
	*	the implementer should {@link java.lang.String#trim()} if the SvgNoteString
	*	is the last.
	*	<P>
	*	SvgNoteStrings are capable of making complex comparisons between themselves 
	*	to determine their location in relation to each other. They do this so they
	*	can determine if they represent the next or previous line of text. Notes 
	*	in illustrations are discreet strings that know nothing of their content. 
	*	The SVG conversion needs to append these Strings together, in order, so that
	*	they can be made into pop-up boxes (commonly known as tooltips).
	*
	*	@version	0.3 - August 31, 2004
	*	@author		Andrew Nisbet
	*/
	protected class SvgNoteString implements Comparable
	{
		private String note;// Content of the note string.
		private double x;	// This is the x position in DXF space.
		private double y;	// This is the y position.
		
		private double fuzz;// tolerance testing value in inches.
		
		private boolean isNumeral;
		private int myIndexNumber; // only used if this SvgNoteSstring represents an note index number.
		private boolean isText;
		
		public final static int SAME_NOTE      = 0;
		public final static int VERT_ALIGNED   = 1;
		public final static int HORZ_ALIGNED   = 2;
		public final static int ABOVE_MIN      = 4;
		public final static int BELOW_MAX      = 8;
		// Following this logic, if two text strings are vertically aligned they would return 
		// 1. If they a vertically aligned and above a min the return value would be 5. If they
		// are vertically aligned, above the min and below the max the method would return 13.
				///////////////////////////////////////////////////
				//       Hard wired min max leading values       //
				//  for a line of AutoCAD note text (in inches)  //
				///////////////////////////////////////////////////
		public final static double MIN_DIST   = 0.05; // Minimum line leading on a standard note strings.
		public final static double MAX_DIST   = 0.15; // Maximum line leading on a standard note strings.
		public final static double MAX_OFFSET = 0.3;  // Maximum distance to insert of a note string that
			// contains both text and number.
		public final static double MIN_OFFSET = 0.2;  // Minimum distance to insert of a note string that
			// contains both text and number.

		////////////////////////////////
		//        Constructor         //
		////////////////////////////////			
		protected SvgNoteString(String note, double x, double y)
		{
			this.note = note;
			this.x = x;
			this.y = y;
			
			fuzz = 0.01;
			
			// Test to see if this is a numeral. It is defined as:
			// A String that starts with at least one and possibly two digits followed
			// by a '.'. It could also be a text string.
			testNote();
		}
		
		
		////////////////////////////////
		//         Methods            //
		////////////////////////////////
		/** This method test the SvgNoteString to determine if it is an index number
		*	or a text string or, on rare occasions, both.
		*/
		private void testNote()
		{
			String[] potNotes = note.split("\\p{Space}{1,}");
			if (potNotes.length > 1)
			{
				// To get here we have more than one string that is separated by space
				// and whether the initial part of the 'note' String passed the number
				// match, the remainder of the strings must be text.
				isText = true;
			}
			
			// Original
			//Pattern pNum = Pattern.compile("^\\p{Digit}{1,2}\\.?");
			// An index number is defined as:
			//   at least 1 and not more than 2 digits
			//   followed by potentially a period 
			//   and the match should be possessive; meaning that once the regex finds 
			//   the match it should stop looking for more matches.
			Pattern pNum = Pattern.compile("^\\p{Digit}{1,2}[\\.]?+$");
			Matcher mNum = pNum.matcher(note);
			
			if (mNum.find())
			{
				isNumeral = true;
			}
			else
			{
				isNumeral = false;
			}
			
			
			if (isNumber() && isText())
			{
				// Now we can populate the note String content. The first element in the array is
				// a number so the second must be some string of some sort so use that to find
				// its starting index in the String 'note'.
				int pos = note.indexOf(potNotes[1]);
				String number = note.substring(0, pos);
				note = note.substring(pos);
				// now parse the number we found into an integer.
				myIndexNumber = parseInt(number);
			}
			else
			{
				// Set the index number of this Object. If this is not an index number
				// it will be automatically set to -1.
				myIndexNumber = parseInt(note);
			}
		}
		
		
		/** Sets the tolerance for proximity testing of objects in DXF.
		*	@param fuzz value proximity tolerance (default 0.01 inches).
		*/
		public void setFuzz(double fuzz)
		{
			this.fuzz = fuzz;
		}
		
		/** Returns a double value that represents a threshold over which the two 
		*	testing values are not considered to be equal.
		*/
		public double getFuzz()
		{
			return fuzz;
		}
		
		/** When this object was created it tested itself to see if it was an 
		*	index number; this method returns true if this is an index number
		*	and false otherwise
		*/
		protected boolean isNumber()
		{
			return isNumeral;
		}
		
		/** When this object was created it tested itself to see if it was an 
		*	index number; whether the object tested true or not this object is
		*	also tested to see if it contains text. The two are not mutually 
		*	exclusive as some illustrators do not make separate index numbers 
		*	and text. Some combine the two into one line.
		*	@return true if this object contains non-numerical text and false otherwise.
		*/
		protected boolean isText()
		{
			return isText;
		}
		
		/** Returns this object as an index number. The stored string value is 
		*	converted to an interger and returned. If the stored String value 
		*	cannot be converted to an integer then the value '-1' is returned.
		*/
		protected int getAsIndexNumber()
		{
			return myIndexNumber;
		}
		
		/** This returns the value of this string as a integer.
		*	@return the string normalized into an integer value and -1 if the string
		*	could not be converted to a number. 
		*/
		private int parseInt(String n)
		{
			if (n == null || n.length() < 1 || isNumber() == false)
			{
				return -1;
			}
			
			char[] num = new char[3]; // max size of index is 2 digits and a potential negitive sign
				// although note indexes technically cannot have -index values.
			// To pass the isNumber test the first character must be a number
			num[0] = n.charAt(0);

			// we have to test how long the string is before we go off assuming it has 
			// a char at index 1 and 2.
			if (n.length() >= 2)
			{
				// Now test the second character 
				if (((int)(n.charAt(1)) >= 48) && 
					((int)(n.charAt(1)) <= 57))
				{
					num[1] = n.charAt(1);
				}
			}

			if (n.length() >= 3)
			{			
				// Now test the third character 
				if (((int)(n.charAt(2)) >= 48) && 
					((int)(n.charAt(2)) <= 57))
				{
					num[1] = n.charAt(2);
				}	
			}
			
			String numStr = String.valueOf(num).trim();
			
			int result = 0;
			
			try
			{
				result = Integer.parseInt(numStr);
			}
			catch (NumberFormatException e)
			{
				result = -1;
			}
			catch (NullPointerException npe)
			{
				result = -1;
			}

			return result;
		}
		
		

		
		
		
		/** Tests the relationship of this note string to the argument SvgNoteString.
		*	@return true if the two SvgNoteStrings are vertically aligned
		*	@see #isAligned(double, double, double, double)
		*	@see #isAboveMinimum(double, double)
		*	@see #isBelowMaximum(double, double)
		*/
		protected boolean isVerticallyAligned(SvgNoteString ns)
		{
			if (testRelationship(ns) == (VERT_ALIGNED + ABOVE_MIN + BELOW_MAX))
			{
				return true;
			}
			
			return false;
		}
		
		
		/** This is a convience method for determining if two SvgNoteStrings are horizontally
		*	aligned. Within a descrepancy of a maximum of 0.2 inches ; any more and 
		*	the illustrator is being awfully sloppy.
		*/
		protected boolean isHorizontallyAligned(SvgNoteString s)
		{
			if ((this.testRelationship(s) & HORZ_ALIGNED) > 0)
			{
				return true;
			}
			
			return false;
		}
		
		
		/** Performs the computation of the bitwise location calculations that describe the 
		*	geographical relationship between this object and the argument SvgNoteString.
		*/
		private int testRelationship(SvgNoteString ns)
		{
			// Test to see if the two string references are referring to the same 
			// object. If they are they will register a false positive.
			if (this == ns)
			{
				return SAME_NOTE;
			}
			
			int proximity = 0;
			
			// We will first test if the text collected is an index number and note string together.
			if (this.isText() && this.isNumber())
			{
				if (isAligned(this.x, ns.x, this.y, ns.y))
				{
					proximity |= VERT_ALIGNED;
				}
			}
			else
			{
				if (isAligned(this.x, ns.x))
				{
					proximity |= VERT_ALIGNED;
				}
			}

			if (isAligned(this.y, ns.y))
			{
				proximity |= HORZ_ALIGNED;
			}
			
			// the test here will determine if the x and y values are less than or greater
			// than the specified max values. If it is over it fails.
			if (isBelowMaximum(this.y, ns.y))
			{
				proximity |= BELOW_MAX;		
			}
				
			if (isAboveMinimum(this.y, ns.y))		
			{
				proximity |= ABOVE_MIN;
			}			

			return proximity;
		}

		/** Determines if the two points are above the max bounding tolerance value minTolerance.
		*	This method will select the next lower string not the one above. This must be so because
		*	if you have the insert coordinate for three strings, and we are testing the middle string
		*	we want to select the lower of these choices. Without this safeguard both the string 
		*	above and below would match.
		*	@return true if the difference between the two values is greater than minimum tolerance
		*	and false otherwise.
		*/
		protected boolean isAboveMinimum(double val1, double val2)
		{
			if (val1 >= val2)
			{
				double deltaV1V2 = Math.abs(val1 - val2);
			
				if (deltaV1V2 >= MIN_DIST)
				{
					return true;
				}
			}
			
			return false;
		}



		/** Determines if the two points are under the max bounding tolerance value maxTolerance.
		*	@return true if the difference between the two values is within tolerance and false
		*	otherwise.
		*/
		protected boolean isBelowMaximum(double val1, double val2)
		{
			if (val1 >= val2)
			{
				double deltaV1V2 = Math.abs(val1 - val2);
			
				if (deltaV1V2 <= MAX_DIST)
				{
					return true;
				}
			}
			
			return false;
		}
		
		/** Takes the delta between two points and determines if they are within tolerance for
		*	being the same value.
		*	@return true if the difference between the two values is within tolerance and false
		*	otherwise.
		*/
		private boolean isAligned(double val1, double val2)
		{
			double deltaV1V2 = Math.abs(val1 - val2);
			
			if (deltaV1V2 <= getFuzz())
			{
				return true;
			}
			
			return false;
		}
		
		
		/** Determines if two values are aligned if an offset value is taken into consideration.
		*	The method works like this: If the argument values represents 'x' values of a coordinate
		*	and x1 = 1.0, x2 = 1.5 and we wanted to see if they lined up, clearly not. But if we
		*	took into consideration the offset value they might. Now more than one text string will
		*	have the x values line up so I have added another test to determine if the y values are
		*	within the minimum and maximum distances allowed for leading. If both these tests
		*	pass then true is returned and false otherwize.
		*	@return true if the difference between the two values is within tolerance and the second 
		*	set of values also fall between {@link #MIN_DIST} and {@link #MAX_DIST} false otherwise.
		*/
		protected boolean isAligned(double x1, double x2, double y1, double y2)
		{
			double dx = x2 - x1;
			// Test and see if the x values are off by the correct amount
			if (dx >= MIN_OFFSET && dx <= MAX_OFFSET)
			{
				// If that was true then compare the ys and make sure they are within leading dist.
				if (isBelowMaximum(y1, y2) && isAboveMinimum(y1, y2))
				{
					return true;
				}
			}
			
			return false;
		}
		
		
		/**	Returns vertical alignment but does so by considering which of the arg values is smaller.
		*	This is done to discount lines that could test positive for proximity and verticle 
		*	alignment but is the preceeding line. Here we will test to see if the line is the next line
		*	or a smaller y value (in DXF space).
		*/
		
		/** Returns the String content of the SvgNoteString.
		*/
		protected String getContent()
		{
			// Add extra space because AutoCAD strings don't have and we need it when we append 
			// consecutive strings together.
			return note + " ";
		}
		
		/** Returns the note String content.
		*/
		public String toString()
		{
			return note;
		}
		
		/** Compares this SvgNoteString against object o by comparing the values of the 
		*	String contents only. Their position is space is irrelivant to the comparison 
		*	algorithm.
		*	
		*	@throws CastClassException like {@link java.lang.Comparable}.
		*/
		public int compareTo(Object o)
		{
			return this.note.compareTo(((SvgNoteString)o).note);
		}
		
		/** Compares this SvgNoteString to object o and returns true if they are equal and
		*	false if they are not. The contract for the determination of equality is as follows:
		*	if o refers to the same object as this object it returns true. If two different objects
		*	have the same note (using {@link java.lang.String#equals(Object)} method) and their x and y
		*	values are the same then the objects are equal.
		*/
		public boolean equals(Object o)
		{
			if (! (o instanceof SvgNoteString))
			{
				return false;
			}
			
			if (this == o)
			{
				return true;
			}
			
			if (! note.equals(((SvgNoteString)o).note))
			{
				return false;
			}
				
			if (this.x != ((SvgNoteString)o).x)
			{
				return false;
			}
			
			if (this.y != ((SvgNoteString)o).y)
			{	
				return false;
			}
			
			return true;
		}
	}
	

	
	/****************************************************************************
	**
	**	FileName:	SheetMissingFromNoteCollectionException.java
	**
	**	Project:	Dxf2Svg
	**
	**	Purpose:	An exception that gets thrown if a sheet is missing from the
	** 				list of files to be converted. This is worthy of an exception
	**				because the sheet that is missing may be the sheet with the 
	**				notes itself.
	**
	**	Date:		May 6, 2004
	**
	**	Author:		Andrew Nisbet
	**
	**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
	**				(build 1.4.1)
	**
	**	Version:	0.1 - May 6, 2004
	**
	**	TODO:		
	**
	
	**
	*****************************************************************************/
	
	/**	This exception gets thrown if the list of files to be converted is missing
	*	entries for files that are listed as being sheets for the current figure
	*	in the database. So the database is consulted and the names of all the 
	*
	*	@version	0.1 - May 6, 2004
	*	@author		Andrew Nisbet
	*/
	protected class SheetMissingFromNoteCollectionException extends RuntimeException
	{
		protected SheetMissingFromNoteCollectionException(Vector fileNames)
		{
			System.err.println("One (or more) sheets that is/are listed for this figure is/are missing.");
			System.err.println("The missing sheets are: ");
			System.err.println(fileNames.toString());
			System.err.println("It is possible that the missing sheet is critical for the following reasons:\n"+
			"1) It may contain the figure's notes.\n"+
			"2) The missing file may make reference to notes that will not appear if converted.\n"+
			"3) The missing file will contain the wrong notes (because it may already exist"+
			" as an SVG and is not being converted at this time).");
			System.err.println("It is critical that all sheets of a figure get converted at the"+
			" same time to ensure that all the notes on all the sheets are synchronized and correct.");
		}
	}
}