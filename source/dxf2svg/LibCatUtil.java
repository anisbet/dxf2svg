
/**************************************************************************
**
** 	Date: 		December 11, 2003
** 	File Name:	LibCatUtil.java
**
** 	Java Version:
**	Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.0_01-b03)
**	Java HotSpot(TM) Client VM (build 1.4.0_01-b03, mixed mode)
**
** 	Author: 	Andrew Nisbet
** 	Purpose: 	Manages Dxf2Svg's database of serialized
**			 	converted figures.
**
**	Version:	1.00 - December 11, 2003 
**				1.01 - December 8,  2004 Updated documentation and help method.
**				1.02 - February 16, 2005 Changed messages to STDOUT in mergeTwoCatalogs()
** 	Todo:
**
***************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.*;

/** This class is intended as a utility to manage LibraryCatalog.ser files. The
*	class can perform 3 different types of jobs: it can output a catalog in XML,
*	It can delete specific data from the database and it can merge two catalogs
*	together.
*	<P>
*	Usage: <b>java dxf2svg.LibCatUtil (options)</b>
*	<P>
*	Convensions used: The LibraryCatalog.ser file is a serialization of a 
*	of a dxf2svg LibraryCatalog. The LibraryCatalog object stores FigureSheetDatabases
*	and each FigureSheetDatabase holds an entire book's figures and sheet listing.
*	<P>
*	The catalog is the entire collection of books - also called the 'database'.
*	The FigureSheetDatabases contain Vectors of information stored as Strings. These
*	Strings contain the figure title in French and English, the spot calls etc.
*	<P>
*	<H4><U>Merging Databases</U></H4>
*	<P>
*	This is done in a safe way that updates data in the destination
*	catalog if the data exists in the source catalog and only records that are
*	not part of the destination catalog are added from the source catalog.
*	<P>
*	The result of merging the source catalog with the destination catalog and then
*	merging the destination catalog with the original source catalog will produce
*	identical catalogs.
*	<P>
*	<H4><U>XML Output</U></H4>
*	<P>
*	Output of any catalog can be facilitated with the '-x' switch which will make
*	an XML version of the contents of the database.
*	<P>
*	<H4><U>Deleting Data</U></H4>
*	<P>
*	Occasionally it may be necessary to delete information from the catalog. This
*	can occur if the a converted DXF file has an incorrect figure number or sheet
*	number. The new figure number (or sheet number) will be added but the old number
*	will still remain, dirtying your data. To remove erroneous information use the 
*	'-d' switch. The '-d' switch can take as many as 4 additional arguments or as 
*	few as 2 additional arguments. The expected arguments are (in the case where 
*	they number 2) the actual serialized database - usually 'LibraryCatalog.ser' -
*	and the book (NDID) you wish to delete. You will be asked to confirm all delete
*	functions.
*	<P>
*	If you include three arguments, the last is assumed to be a figure number. A
*	four argument list is assumed to have a specific sheet number to delete as its 
*	final argument.
*	<P>
*	In any case if the requested delete object is not found the database remains 
*	unchanged.
*	<P>
*	<B>Typical Usage</B>
*	java dxf2svg.LibCatUtil -x LibraryCatalog.ser
*	<P>
*	java dxf2svg.LibCatUtil -d LibraryCatalog.ser "C-12-130-0G3/MF-002" "2-1-24"
*
*	@author Andrew Nisbet
*	@version 1.02 - February 16, 2005
*	@since dxf2svg version 1.0
*/
public class LibCatUtil
{
	protected static final String xmlOutputName = "boardno-control.xml";
	protected static final int DELETE_SHEET		= 1;
	protected static final int DELETE_FIGURE	= 2;
	protected static final int DELETE_BOOK		= 3;
	protected static final int MERGE_CATALOGS	= 4;

	public static void main(String[] args)
	{
		try
		{
			if (args[0].equals("-x"))
			{
				// the next arg is the catalog to export as XML.
				makeXmlCatalog(args[1]);
			}
			else if (args[0].equals("-m"))
			{
				// the next arg is source catalog, the one after is the destination catalog.
				mergeTwoCatalogs(args[1], args[2]);
			}
			else if (args[0].equals("-d"))
			{
				if (args.length == 5)
				{
					deleteEntry(args[1],args[2],args[3],args[4]);
					return;
				}
				if (args.length == 4)
				{
					deleteEntry(args[1],args[2],args[3]);
					return;
				}
				if (args.length == 3)
				{
					deleteEntry(args[1],args[2]);
					return;
				}
				else
				{
					// the user has entered too few or too many arguments.
					System.err.println("'" + args[0] + "' requires at least two, and " +
						"no more than four other arguments.");
					usage();
				}
			}
			else
			{
				// the user has entered an invalid or unimplemented argument.
				System.err.println("Invalid or unimplemented argument: '" + args[0] + "'.");
				usage();
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.err.println("Error: expected other arguments.");
			usage();
		}

	}  // end main()
	
	//////////////////////
	//    Methods       //
	//////////////////////

	/** Deletes an entry from a namespace within the specified catalog.
	*	@param book Name of book in library.
	*	@param catalog Name of the library to search.
	*/
	protected static void deleteEntry(String catalog, String book)
	{
		LibraryCatalog lib = readCatalog(catalog);
		
		if (actionConfirmed(DELETE_BOOK))
		{
			if (lib.deleteBook(book))
			{
				System.out.println("'"+book+"' deleted from catalog.");
				serializeCatalog(catalog, lib);
			}
			else
			{
				System.out.println("No book by the name of '"+book+"' could be found. Nothing done.");
			}
			return;
		}
		
		System.out.println("action cancelled.");
	}
	
	/** Deletes an entry from a namespace within the specified catalog.
	*	@param figure Name of record to remove.
	*	@param book Name of book in library.
	*	@param catalog Name of the library to search.
	*/
	protected static void deleteEntry(String catalog, String book, String figure)
	{
		LibraryCatalog lib = readCatalog(catalog);
		
		if (actionConfirmed(DELETE_FIGURE))
		{
			FigureSheetDatabase figDB = (FigureSheetDatabase)lib.retrieveFigures(book);
			if (figDB == null)
			{
				System.out.println("Figure not found: '"+book+"' figure: '"+figure+"'.");
				return;
			}
			else
			{
				if (figDB.deleteFigure(figure))
				{
					System.out.println("'"+figure+"' deleted from "+book+".");
					serializeCatalog(catalog, lib);
				}
				else
				{
					System.out.println("no such figure in book.");
				}
			}
			return;
		}
		
		System.out.println("action cancelled.");
	}
	
	/** Deletes a sheet from a figure within a book in a specified catalog.
	*	@param sheetNo Number of the sheet you wish to remove from the figure.
	*	@param figure Name of record to remove.
	*	@param book Name of book in library.
	*	@param catalog Name of the library to search.
	*/
	protected static void deleteEntry(String catalog, String book, String figure, String sheetNo)
	{
		LibraryCatalog lib = readCatalog(catalog);
		
		if (actionConfirmed(DELETE_SHEET))
		{
			FigureSheetDatabase figDB = (FigureSheetDatabase)lib.retrieveFigures(book);
			if (figDB == null)
			{
				System.out.println("Figure not found: '"+book+"' figure: '"+figure+"'.");
				return;
			}
			else
			{
				if (figDB.containsFigure(figure))
				{
					if (figDB.deleteSheet(figure, sheetNo))
					{
						System.out.println("sheet '"+sheetNo+"', of figure: '"+figure+"' deleted from "+book+".");
						serializeCatalog(catalog, lib);
					}
					else
					{
						System.out.println("no such sheet: '"+sheetNo+"' in figure: '"+figure+"' of book: '"+book+"'.");
					}
				}
				else
				{
					System.out.println("no such figure: '"+figure+"' of book: '"+book+"'.");
				}
			}
			return;
		}
		
		System.out.println("action cancelled.");
	}
	
	/**	Makes the user confirm dangerous actions like deleting books or figures from the catalog.
	*	@return boolean <code>true</code> if user is sure; <code>false</code> if no sure.
	*	@throws IOException if an error occurs reading the users answer from STDIN.
	*/
	protected static boolean actionConfirmed(int action)
	{
		String Answer = new String();

		try
		// this try will check on correct keyboard input answer from user.
		{
			// read in data from keyboard
			switch (action)
			{
				case DELETE_SHEET:
					System.out.print("Are you sure you want to delete this sheet from this figure?");
					break;
					
				case DELETE_FIGURE:
					System.out.print("Are you sure you want to delete entire figure from this book?");
					break;
					
				case DELETE_BOOK:
					System.out.print("Are you sure you want to delete entire book from the catalog?");
					break;
					
				case MERGE_CATALOGS:
					System.out.print("Are you sure you want to merge these catalogs?");
					break;
					
				default:
					System.err.println("Invalid action type in actionConfirmed(). Exiting.");
					System.exit(-1);
			}
			System.out.print(" <y/n>[n]");
			// Now read in response
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
	
	/** Merges two discreet LibraryCatalogs into one. This is done in a safe way so that douplicates
	*	in the destination catalog are updated and new unique entries are added.
	*/
	protected static void mergeTwoCatalogs(String catSrc, String catDest)
	{
		if (actionConfirmed(MERGE_CATALOGS)){
			LibraryCatalog libSrc = readCatalog(catSrc);
			LibraryCatalog libDest = readCatalog(catDest);
			FigureSheetDatabase figDB;

			// IMPLEMENT this method in LibraryCatalog.
			Set bookSet = libSrc.getBooks();
			Iterator itBook = bookSet.iterator();
			String book;
			while (itBook.hasNext())
			{
				book = (String)itBook.next();
				figDB = libSrc.retrieveFigures(book);
				libDest.update(book, figDB);
			}  // end while

			System.out.println("'" + catSrc + "' has been merged with '" + catDest + "'.");
			System.out.println("To update the boardno-control.xml file run LibCatUtil again");
			System.out.println("with the '-x' switch.");

			serializeCatalog(catDest, libDest);
			
			System.out.println("done.");
		}  // end if
	}  // end mergeTwoCatalogs()
	

	/** Outputs the entire catalog into an XML file called 'boardno-control.xml' if the -x
	*	switch is selected.
	*/
	protected static void makeXmlCatalog(String catalog)
	{
		LibraryCatalog lib = readCatalog(catalog);

		lib.outputXML(xmlOutputName);

		System.out.println("output of '" + xmlOutputName + "' complete.");
	}  // makeXmlCatalog()

	/**	Diplays usage message and exits with a status of -1.
	*/
	protected static void usage()
	{
		System.err.println("Usage: java LibCatUtil [-x catalog.ser][-m <foo/sourceCatalog.ser> <bar/destCatalog.ser>]]"+
			"[-d [<catalog.ser> <namespace> [<figure> [<sheet>]]]]");
		System.err.println();
		System.err.println("-x  Export the catalog's contents as XML.");
		System.err.println("-m  Merge src to dest catalog.");
		System.err.println("-d  deletes a <sheet> from <figure> from <book> from <catalog>.");
		System.err.println("    If two args follow the '-d' it is assumed that the second arg is a book");
		System.err.println("    and it will be searched for and deleted from the catalog. A message will");
		System.err.println("    be issued if the book could not be found in the catalog.");
		System.err.println("    If three arguments are found, the third is assumed to be a figure and ");
		System.err.println("    the named figure, and all sheets will be deleted.");
		System.err.println("    If a forth argument is found it is assumed to be a sheet number and");
		System.err.println("    if found within the specified figure within the specified book it,");
		System.err.println("    and it alone, will be deleted.");
		System.err.println();
		System.err.println("    The minimum arguments in addition to '-d' is two (catalog and book)");
		System.err.println("    The maximum is four: catalog, book, figure and sheetno.");
		System.exit(-1);
	}  // usage()

	/** Opens a library catalog and reads in the contents into the argument lib.
	*	@throws IOException
	*	@throws FileNotFoundException
	*/
	protected static LibraryCatalog readCatalog(String catalog)
	{
		ObjectInputStream ios = null;
		LibraryCatalog lib = null;
		
		try
		{
			ios = new ObjectInputStream(new FileInputStream(catalog));
			lib = (LibraryCatalog)ios.readObject();
		}
		catch (Exception e)
		{
			System.err.println(e);
			e.printStackTrace(System.out);
		}  // end catch
		finally
		{
			if (ios != null)
			{
				try
				{
					ios.close();
				}
				catch (IOException ioe)
				{
					System.err.println(ioe);
					ioe.printStackTrace(System.out);
				}
			}  // end if
		}  // end finally.
		
		return lib;
	}  // end readCatalog

	/** Writes the argument database to a {@link java.io.FileOutputStream}.
	*/
	protected static void serializeCatalog(String name, LibraryCatalog lib)
	{
		ObjectOutputStream oos = null;
		
		try
		{
			oos = new ObjectOutputStream(new FileOutputStream(name));
			oos.writeObject(lib);				
		}
		catch (Exception e)
		{
			System.err.println(e);
			e.printStackTrace(System.out);
		}
		finally
		{
			if (oos != null)
			{
				try
				{
					oos.close();
				}
				catch (IOException ioe)
				{
					System.err.println(ioe);
					ioe.printStackTrace(System.out);
				}
			}  // end if
		}  // end finally.
	}
}  // end class