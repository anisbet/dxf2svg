
/****************************************************************************
**
**	FileName:	FigureSheetDatabase.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class is a trivial wrapper for the dxf2svg.util.Database
**				class. It provides method names that are more in tune with
** 				the functions we require during the conversion of illustrations.
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

package dxf2svg; // Note: this objects super class is in dxf2svg.util package.

import java.util.*;
import java.io.*;
import dxf2svg.util.Dxf2SvgConstants;
import dxf2svg.util.Database;

/**
*	This class is a trivial wrapper for the dxf2svg.util.Database
*	class. It provides method names that are more in tune with
*	the functions we require during the conversion of illustrations.
*	<P>
*	Here is a view of a typical table as it is stored in the database:
*	<pre>
"1-2-3" <-- Table's name
 -----
   key     eBoardno   fBoardno   eTitle      fTitle    spotcall   total Sht
+-------++----------+----------+---------+-----------+----------+----------+
|  "1"  || g01234ea | g01235fa | "Title" | "Fig nom" | faaabcd  |   "2"    |
+-------++----------+----------+---------+-----------+----------+----------+
|  "2"  || g01236ba | g01236ba | "Title" | "Title"   | faaabce  |   "2"    |
+-------++----------+----------+---------+-----------+----------+----------+
</pre>
*
*	@version 	0.01 - November 13, 2003
*	@author		Andrew Nisbet
*/
public class FigureSheetDatabase extends Database
{
	/** This is the number of fields in a figure's record. */
	public final static int NUM_FIELDS = 6;
	/////////////////////////
	//                     //
	//     Constructor     //
	//                     //
	/////////////////////////
	/** Default constructor that initializes the database.
	*/
	public FigureSheetDatabase()
	{
		super();
	}
	
	///////////////////////
	//                   //
	//     Methods       //
	//                   //
	///////////////////////
	
	/** Creates a new record on the argument table.
	*	@param figure illustration figure number like '1-2-3a'.
	*	@param sheet The number of this sheet like 2 of 3 means this is sheet 2.
	*	@param boardnos String array of boardnos - from null to 2 usually but not 
	*	limited to the upper range at all.
	*	@return true requested table and the record was created successfully.
	*/
	public boolean addFigureSheetAndValues(
		String figure,
		String sheet,
		String[] boardnos
	){
		Vector bnosV = new Vector();
		// Add each element individually.
		for (int i = 0; i < boardnos.length; i++)
		{
			bnosV.add(boardnos[i]);
		}
		return createRecord(figure, sheet, bnosV);
	}
	
	/** This method returns the values stored in a table called tableName by
	*	reference of the key argument.
	*	@return Vector of values stored, or alternatively null if the table
	*	or record does not exist in the database.
	*/
	public Vector getFigureData(String figure, String sheet)
	{
		return getRecord(figure, sheet);
	}
	
	/** This method, when supplied with a data value, will perform a reverse lookup 
	*	of the Figure and sheet that contains that value. If multipule values are 
	*	present in the database the first value is returned.
	*	@return String[] , zero indexed with a size of two. The first value is the figure
	*	the second is the sheet number (as a string) and may be null if the illustration
	*	is a single sheet. May also be null no entry matches <EM>'value'</EM>.
	*/
	public String[] findFigureAndSheetByValue(String value)
	{
		return getTableAndRecord(value);
	}
	
	/** Deletes a sheet from a figure specified by 'figure'.
	*	@return false if the figure does not contain a sheet by the number sheetNo or
	*	if the figure does not exist in the database or either figure or sheetNo are
	*	null. Returns true otherwise.
	*/
	public boolean deleteSheet(String figure, String sheetNo)
	{
		return delete(figure, sheetNo);
	}
	
	/** Deletes an entire figure from the database if a figure by the name 'figure' exists.
	*	@return false if the figure does not exist or the argument figure is null.
	*	returns true otherwise.
	*/
	public boolean deleteFigure(String figure)
	{
		return delete(figure);
	}
	
	/** Returns true if the key was a valid pointer to data stored in the database.
	*/
	public boolean containsFigure(String figure)
	{
		return containsKey(figure);
	}
	

	/** Adds the data from the argument {@link dxf2svg.FigureSheetDatabase} to 
	*	this {@link dxf2svg.FigureSheetDatabase} in a safe way. The method for
	*	adding the information takes the following form. If a figure from the 
	*	argument {@link dxf2svg.FigureSheetDatabase} is already a member of this
	*	{@link dxf2svg.FigureSheetDatabase} then search the sheets adding or 
	*	replacing as required. If is not a member, then the figures are simply added 
	*	to this {@link dxf2svg.FigureSheetDatabase}.
	*/
	public void add(FigureSheetDatabase f)
	{
		String fFigure = new String();
		String fSheet = new String();
		Set fEntrySet = f.keySet();
		Iterator fItEntrySet = fEntrySet.iterator();
		// The Set contains the names of all the figures in this FigShtDB.
		// pull each of the names from the guest FigShtDB.
		while (fItEntrySet.hasNext())
		{
			fFigure = (String)(fItEntrySet.next());
			if (db.containsKey(fFigure))
			{
				// check the sheets one by one; to do that we need to get a list of 
				// stored values and search for each
				Hashtable fTable = (Hashtable)f.get(fFigure);
				Hashtable thisTable = (Hashtable)get(fFigure);
				// Now get the names of the sheets from the guest FigShtDB.
				Set sheetSet = fTable.keySet();
				Iterator itSheetSet = sheetSet.iterator();
				while (itSheetSet.hasNext())
				{
					// get the sheets and add them to this table.
					fSheet = (String)itSheetSet.next();
					thisTable.put(fSheet, fTable.get(fSheet));
				}
				db.put(fFigure, thisTable);
			}  // end if
			else
			{
				db.put(fFigure, ((Hashtable)f.get(fFigure)));
			}
		}  // end while
	}  // end add()
	
	/**	Outputs this object as XML.
	*	@see #outputXML for complete description.
	*	@throws FileNotFoundException
	*	@throws IOException
	*/
	public void outputXML(BufferedWriter bw)
	{
		String figNo;
		String totalSheets;
		String sheetNo;
		Vector sheet = new Vector();
		String valIndex;				// Stored values of the sheet vector
		String indentFig = "\t\t";
		String indentSheet = "\t\t\t";
		
		try
		{
			Set entrySet = db.keySet();
			Iterator itEntrySet = entrySet.iterator();
			
			while (itEntrySet.hasNext())
			{
				// This extracts the name of each of the 'databases' (Hashtables) stored.
				figNo = (String)itEntrySet.next();
				
				// Now we can start to assemble a Strings necessary to output the XML.
				StringBuffer figureBuff = new StringBuffer();
				// This is where we will put our sheets.
				StringBuffer sheetBuff = new StringBuffer();
				// This is the buffer for the titles
				StringBuffer titleBuff = new StringBuffer();
				
				// This is all we have just now; we will collect the total sheets when
				// we extract the sheets.
				figureBuff.append(indentFig+"<figure figNo=\""+figNo+"\"");
				
				// retrieve each of the databases.
				Hashtable table = (Hashtable)db.get(figNo);
				if (table == null || table.size() == 0)
				{
					figureBuff.append("></figure>\n");
					continue;
				}
				
				boolean isFirstSheet = true;
				// iterate over the keys of the database; these keys represent the sheet numbers
				// or records.
				Set recSet = table.keySet();
				Iterator itRecSet = recSet.iterator();
				while (itRecSet.hasNext())
				{
					// This is the sheet number of the current figure.
					sheetNo = (String)itRecSet.next();
					sheet = (Vector)table.get(sheetNo);
					sheetBuff.append(indentSheet+"<sheet sheetNo=\""+sheetNo+"\"");
					
					// Let's get the vector of values for this sheet and output them as XML
					for (int i = 0; i < sheet.size(); i++)
					{
						valIndex = (String)sheet.get(i);
						
						switch(i)
						{
							case 0:  // english boardno or bilingual boardno
								if (DxfPreprocessor.getBoardnoLanguage(valIndex) == 
									Dxf2SvgConstants.ENGLISH)
								{
									sheetBuff.append(" engBoardno=\""+valIndex+"\"");
								}
								else if (DxfPreprocessor.getBoardnoLanguage(valIndex) == 
									Dxf2SvgConstants.MULTI_LINGUAL)
								{
									sheetBuff.append(" bilBoardno=\""+valIndex+"\"");
								}
								break;
								
							case 1:  // french boardno or bilingual boardno
								if (DxfPreprocessor.getBoardnoLanguage(valIndex) 
									== Dxf2SvgConstants.FRENCH)
								{
									sheetBuff.append(" freBoardno=\""+valIndex+"\"");
								}
								break;
								
							case 2:  // english title 
								if (valIndex != null && valIndex.length() > 0 && isFirstSheet)
								{
									//titleBuff.append(indentSheet+"<title language=\"english\">"+
									titleBuff.append(indentSheet+"<title>"+
										valIndex+"</title>\n");
								}
								break;
								
							case 3:  // french title
								//if (valIndex != null && valIndex.length() > 0 && isFirstSheet)
								//{
								//	if (valIndex != null && valIndex.length() > 0)
								//	{
								//		titleBuff.append(indentSheet+"<title language=\"french\">"+
								//		convertCharEntities(valIndex)+"</title>\n");
								//	}
								//}
								break;
								
							case 4:  // spot call *** Impl. note: remember to get extension ***
								sheetBuff.append(">"+valIndex+"</sheet>\n");
								break;
								
							case 5:  // Total sheets
								// each entry has a total sheets we only need one (I know 
								// poorly normalized table!), anyway we just need the first one.
								if (isFirstSheet)
								{
									figureBuff.append(" totalSheets=\""+valIndex+"\">\n");
									isFirstSheet = false;
								}
								break;
								
							default:  // off the end of the currently known stored values 
								break;
						}
					}  // end for
				}  // end while
				
				figureBuff.append(titleBuff);
				figureBuff.append(sheetBuff);
				figureBuff.append(indentFig+"</figure>\n");
				bw.write(figureBuff.toString());
			}  // end while
		}
		catch (IOException e)
		{
			System.err.println("FigureSheetDatabase.outputXML() error: Unable to output as XML "+
				"because of an Input/Output exception occured."+e);
			e.printStackTrace(System.out);
		}
	}  // end outputXML()
	
	/** Converts any ascii characters greater than 126 to its character entity value.
	*/
	protected String convertCharEntities(String s)
	{
		StringBuffer sb = new StringBuffer();
		byte[] chArr = s.getBytes();
		for (int i = 0; i < chArr.length; i++)
		{
			if ((int)chArr[i] > 126)  // 126 = '~'
			{
				sb.append("&#"+(int)chArr[i]+";");
			}
			else
			{
				sb.append((char)chArr[i]);
			}
		}
		return sb.toString();
	}
}