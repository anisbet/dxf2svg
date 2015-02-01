
/****************************************************************************
**
**	FileName:	SvgNoteManager.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Manages notes for all the files in the current conversion
**				process. This object is the controller for all SvgNotes 
**				objects created during this conversion.
**
**	Date:		May 3, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.1)
**
**	Version:	0.1 - May 3, 2004
**
**	TODO:		
**

**
*****************************************************************************/

package dxf2svg;

import java.util.*;
import dxf2svg.svg.SvgNotes;	// SvgBuilder, FigureSheetDatabase.

/** This class creates and manages all of the SvgNotes that will be required 
*	during the current conversion process.
*	@author		Andrew Nisbet
*	@version	0.1 - May 3, 2004
*/
public class SvgNoteManager
{
	private Vector mySvgNotes;	// SvgNotes storage.
	
	/** The argument is a Vector of Strings that represent the names of the 
	*	files to be converted. Note: Converting two graphics with the same name is 
	*	asking for trouble. Most searches quit after the first duplicate is found
	*	leaving the second (and all others thereafter) unconverted or partially 
	*	converted.
	*/
	public SvgNoteManager(Vector dxfFiles, LibraryCatalog lib)
	{
		System.out.println("compiling figure notes...");
		mySvgNotes = new Vector();
	
		if (lib == null)
		{
			System.err.println("SvgNoteManager.constructor(): library is null.");
			System.err.println("Notes will not be applied.");
			return;
		}
		
		// Here are the names of books stored in the library.
		Set bookNames = lib.getBooks();
		if (bookNames == null)
		{
			System.err.println("SvgNoteManager.constructor(): the library does not contain any books.");
			System.err.println("Notes will not be applied.");
			return;
		}
		
		// iterate over the list of dxf files
		String name = null;
		for (int i = 0; i < dxfFiles.size(); i++)
		{
			name = DxfPreprocessor.getNormalizedFileName((String)(dxfFiles.get(i)));
		}
		
		Iterator bookNameIt = bookNames.iterator();
		String book = null;
		while (bookNameIt.hasNext())
		{
			// Extract each book in turn.
			book = (String)(bookNameIt.next());
			FigureSheetDatabase figDB = lib.retrieveFigures(book);
			Set figs = figDB.keySet();
			Iterator figsIt = figs.iterator();
			while (figsIt.hasNext())
			{
				// This will retrieve a list of sheets of this figure.
				Hashtable figure = figDB.getTable((String)(figsIt.next()));
				SvgNotes svgNote = new SvgNotes(dxfFiles, figure);
				if (svgNote.isRelevantToConversion() && svgNote.hasNotes())
				{
					mySvgNotes.add(svgNote);
				}
			}
		}
		
		//for (int i = 0; i < mySvgNotes.size(); i++)
		//{
		//	System.out.println("SvgNotes #"+i+" = "+
		//		((SvgNotes)(mySvgNotes.get(i))).getNotesAsJavaScript());
		//}
		
		// don't convert the files, just process them.
		//System.exit(0);
	}


	/** This method brokers communication between the SvgBuilder and SvgNotes objects.
	*	This method employs a call back method that directly calls the
	*	{@link dxf2svg.SvgBuilder#addJavaScript(java.lang.String)} method if there were
	*	any notes of interest for this file.
	*/
	public void getNotes(SvgBuilder svgB, String fileAndPath)
	{
		// circulate over all of the notes and see if any apply to this file and if they
		// do break the loop
		SvgNotes notes = null;
		for (int i = 0; i < mySvgNotes.size(); i++)
		{
			notes = (SvgNotes)(mySvgNotes.get(i));
			if (notes.getNotes(svgB, fileAndPath) == true)
			{
				break;
			}
		}
	}


	/** Returns a String that states the name of the class and its version number.
	*/
	public String toString()
	{
		return this.getClass().getName() + " version 0.1 - May 3, 2004";
	}
}