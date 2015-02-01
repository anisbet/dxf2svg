
/****************************************************************************
**
**	FileName:	LibraryCatalog.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class is a wrapper class to implement namespaces for 
**				FigureSheetDatabases (see {@link dxf2svg.util.DatabaseContainer}
**				for more details.
**
**	Date:		August 20, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - August 20, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.*;
import dxf2svg.util.*;

/** This class is a wrapper class to implement namespaces for 
*	FigureSheetDatabases (see {@link dxf2svg.util.DatabaseContainer}
*	for more details. This is important because the {@link FigureSheetDatabase}
*	just maintains a table for each figure number and an entry for every sheet.
*	We need to be able to store these values by book as well so we can store multipule
*	instances of Figure 1-1, one for each book. Further when we output the database
*	to XML we need to be able to populate the &lt;Book&gt; tag with its appropriate
*	book attribute value.
*
*	@version 	0.01 - December 4, 2003
*	@author		Andrew Nisbet
*/
public class LibraryCatalog extends DatabaseContainer
{
	
	public LibraryCatalog()
	{ 	
		super();	// technically don't need to do this
	}
	
	/** Retrieves the database of figures for a specific book.
	*/
	public FigureSheetDatabase retrieveFigures(String book)
	{
		return (FigureSheetDatabase)get(book);
	}
	
	/** Pushes the current database of figures into a book's catalog.
	*	New values are stored and pre-existing values are over-written.
	*/
	public void update(String book, FigureSheetDatabase figDB)
	{
		// Find out if there is a namespace called this in the database
		// if there isn't; too easy just push this one on
		if (dbContainer.containsKey(book))
		{
			// Retrieve the figures from this database container.
			FigureSheetDatabase myFigDB = retrieveFigures(book);
			// add the figDB to it in a safe way.
			myFigDB.add(figDB);
			put(book, myFigDB);
		}
		else
		{
			put(book, figDB);
		}
	}
	
	/** This method returns a {@link java.util.Set} which contains all
	*	the names of the books currently stored in the Library as Strings.
	*/
	public Set getBooks()
	{
		return keySet();
	}
	
	/** Outputs the contents of the entire library catalog in XML.
	*	The DTD of the output is as follows:
	*	<P>
<pre>
<!DOCTYPE graphiclist [
<!ELEMENT graphiclist (book+) >
<!ELEMENT book (figure+) >
<!ATTLIST book 
	ndid	 		CDATA		#REQUIRED >
	
<!ELEMENT figure (sheet+, title*) >
<!ATTLIST figure 
	figNo	 		CDATA		#REQUIRED 
	totalSheets		NMTOKEN 	#REQUIRED >
	
<!ELEMENT sheet (#PCDATA) >	
<!ATTLIST sheet
	sheetNo	 		NMTOKEN		#REQUIRED
	engBoardno 		CDATA		#IMPLIED 
	freBoardno 		CDATA		#IMPLIED
	bilBoardno 		CDATA		#IMPLIED >
	
<!ELEMENT title (#PCDATA) >
<!ATTLIST title
	language		CDATA		#IMPLIED >
]>
</pre>
	*	<P>
	*	If you do not want to have the DTD output to file use the 
	*	{@link #outputXML(String, boolean)} method and set includeDTD 
	*	to false.
	*/
	public void outputXML(String fileName)
	{
		outputXML(fileName, false);
	}
	
	/**
	*	@see #outputXML for complete description.
	*	@throws FileNotFoundException
	*	@throws IOException
	*/
	public void outputXML(String fileName, boolean includeDTD)
	{
		FigureSheetDatabase figDB;
		String ndid;
		try
		{
			BufferedWriter bw = new BufferedWriter(
				new FileWriter(fileName));
				
			// This will ensure that XSLT transform scripts run by Saxon will
			// correctly transform the character set if a French title accidentally
			// makes it into the xml. It also gets correctly passed to HTML transformations.
			bw.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\" ?>\n");
			if (includeDTD)
			{
				bw.write("<!DOCTYPE graphiclist [\n");
				bw.write("<!ELEMENT graphiclist (book+) >\n");
				bw.write("<!ELEMENT book (figure+) >\n");
				bw.write("<!ATTLIST book \n");
				bw.write("	ndid	 		CDATA		#REQUIRED >\n");
				bw.write("\n");
				bw.write("<!ELEMENT figure (sheet+, title*) >\n");
				bw.write("<!ATTLIST figure \r\n");
				bw.write("	figNo	 		CDATA		#REQUIRED\n");
				bw.write("	totalSheets		NMTOKEN 	#REQUIRED >\n");
				bw.write("\n");	
				bw.write("<!ELEMENT sheet (#PCDATA) >\r\n");	
				bw.write("<!ATTLIST sheet\r\n");
				bw.write("	sheetNo	 		NMTOKEN		#REQUIRED\n");
				bw.write("	engBoardno 		CDATA		#IMPLIED\n"); 
				bw.write("	freBoardno 		CDATA		#IMPLIED\n");
				bw.write("	bilBoardno 		CDATA		#IMPLIED >\n");
				bw.write("\n");	
				bw.write("<!ELEMENT title (#PCDATA) >\n");
				bw.write("<!ATTLIST title\n");
				bw.write("	language		CDATA		#IMPLIED >\n");
				bw.write("]>\n");
			} // end if
			bw.write("<graphiclist>\n");
			
			// retrieve all of the namespaces.
			Set figSet = dbContainer.keySet();
			Iterator itFigSet = figSet.iterator();
			while (itFigSet.hasNext())
			{
				ndid = (String)itFigSet.next();
				figDB = (FigureSheetDatabase)dbContainer.get(ndid);
				bw.write("	<book ndid=\""+ndid+"\">\n");
				
				// Now the figure sheet database can output its own xml
				figDB.outputXML(bw);
				
				bw.write("	</book>\n");
			}
			
			bw.write("</graphiclist>");
			
			bw.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("LibraryCatalog.outputXML() error: Unable to output as XML "+
				"because the file '"+fileName+"' could not be found."+e);
			e.printStackTrace(System.out);
		}
		catch (IOException e)
		{
			System.err.println("LibraryCatalog.outputXML() error: Unable to output as XML "+
				"because of an Input/Output exception occured."+e);
			e.printStackTrace(System.out);
		}
	}  // end outputXml()
	
	
	/**	Deletes a book from the catalog.
	*	@return false if the book could not be found or the book name is null and 
	*	true otherwise.
	*/
	public boolean deleteBook(String ndid)
	{
		return deleteNamespace(ndid);
	}
}