
/****************************************************************************
**
**	FileName:	GraphicLinkDatabase.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class represents a database of graphic link targets.
**
**	Date:		September 23, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.1 - Initial development.
**
**	TODO:		
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;			// ObjectInput/OutputStreams
import java.util.regex.*;
import java.util.Hashtable;

/** This class represents a database of graphic link targets.
*	The class is responsible for serializing and deserializing the {@link java.util.Hashtable}
*	that contains the graphic link targets and responds to requests for link
*	targets from clients via the {@link #getFigureId} method. 
*	<P>
*	To create a database use the constructor with argument action of {@link #CREATE}. You
*	may now populate the database with data. When done use the {@link #closeDatabase()} method.
*	This will re-serialize the database, saving the changes you made. Once a database is closed,
*	you may not reopen it. You must create a new instance
*	of the class with the appropriate action and path argument.
*	<P>
*	Because the underlying structure of the database is a single {@link java.util.Hashtable}
*	you could - in a pinch - open the database by creating a class that can de-serialize
*	the Hashtable.
*	<P>
*	The internals of the database is actually two tables in one. Part one is a 
*	list of spotcalls keys with values of boardnos, the second part is 
*	a list of boardno keys with figureIDs. 
*	<P>
*	The database is called GraphicLinkDatabase.ser and cannot be changed.
*
*	@version 	0.1 - September 23, 2004
*	@author		Andrew Nisbet
*/
public class GraphicLinkDatabase
{
	/** Requires new database to be created; deletes any that exists. */
	public static final int CREATE 	= 1;
	/** Open an existing database if none found, application will exit with message. */
	public static final int OPEN	= 2;
	/** Enum value for testing of type of match we would like: spotcall or boardno */
	protected static final int SPOTCALL = 1;
	protected static final int BOARDNO	= 2;
	/** Name for the graphicsLinkDatabase.*/
	public static final String GRAPHIC_LINK_DATABASE_NAME = "GraphicLinkDatabase.ser";
	/** This is a regex pattern for a boardno used for error checking input requests on 
	*	{@link #setSpotcallBoardnoRecord} and {@link #setBoardnoFigureIdRecord}.
	*/
	public static final String SPOTCALL_REGEX 	= "[f|F|s|S|w|W|m|M]\\p{Alpha}{5,7}";
	public static final String BOARDNO_REGEX	= "[gG]"+ // starts with 'g' always followed by...
		"\\p{Digit}{5}"+		// five digits, ...
		"\\p{Alpha}{2}";		// and ends with two [a-zA-z] characters.";
	private Pattern pSpotCall;
	private Pattern pBoardno;
	
	private Hashtable db;	// Table of data.
	private String path;	// directory path to the database.

	//////////////////////////
	//     Constructor      //
	//////////////////////////
	public GraphicLinkDatabase(int action, String path)
	{
		pSpotCall 	= Pattern.compile(SPOTCALL_REGEX);
		pBoardno	= Pattern.compile(BOARDNO_REGEX);
		this.path 	= path;
		switch(action)
		{
			case CREATE:
				db = new Hashtable();
				break;
			
			case OPEN:
				openDatabase(path);
				break;
				
			default:
				System.out.println(this.getClass().getName() + 
					"Error: unknown action type: " + action);
				
		}	// end switch
	}
	
	///////////////////////
	//     Methods       //
	///////////////////////
	/** This method allows the class to add a new part to the table that contains 
	*	a lookup between the boardno and spot-call. This method checks the spotcall 
	*	against a name filter for a 
	*	standard spotcall [f|F|s|S|w|W]\\p{Alpha}{5,7}. There is no limitation on the
	*	contents of the argument figureId at this time.
	*/
	protected synchronized void setSpotcallBoardnoRecord(String spotCall, String boardno)
	{
		if (isWellFormed(spotCall, SPOTCALL))
		{
			if (isWellFormed(boardno, BOARDNO))
			{
				db.put(spotCall, boardno);
			}
		}
	}
	
	/** This method adds a boardno key with a figureID value to the database.
	*	This method is called by a SearchTask object, which is a component of
	*	of the MakeExternalLinksDatabase application.
	*/
	public synchronized void setBoardnoFigureIdRecord(String boardno, String figureId)
	{
		if (isWellFormed(boardno, BOARDNO))
		{
			db.put(boardno, figureId);
		}
	}
	
	/** Returns the figureID stored by this spotcall key. If the key does not exist,
	*	the returning String will be null.
	*	@throws CastClassException if an object other than a string is stored as a 
	*	value for the argument key.
	*	TODO: This record will take the a spotcall and return the ID of the figure
	*	that it comes from.
	*/
	public String getFigureId(String spotCall)
	{
		if (isWellFormed(spotCall, SPOTCALL))
		{
			String boardno = (String)(db.get(spotCall));
			if (boardno != null)
			{
				return (String)(db.get(boardno));
			}
		}
		return null;
	}
	

	
	
	/** This method closes, and serializes the database. Don't forget to run this method when you are
	*	finished with the database or your changes since you opened it will be lost. 
	*	Once a database is closed, you may not reopen it. You must create a new instance
	*	of the class with the appropriate action and path argument.
	*/
	public void closeDatabase()
	{
		ObjectOutputStream oos = null;
		
		try
		{
			oos = new ObjectOutputStream(new FileOutputStream(new File(path,GRAPHIC_LINK_DATABASE_NAME)));
			oos.writeObject(db);				
		}
		catch (Exception e)
		{
			System.err.println(e);
			e.printStackTrace(System.out);
		}	
	}
	
	
	
	
	
	/** This method is called from the constructor to open a database that can be found in 
	*	the directory designated by the argument path.
	*/
	protected void openDatabase(String path)
	{
		ObjectInputStream ios = null;
		db = null;
		
		try
		{
			ios = new ObjectInputStream(
				new FileInputStream(new File(path,GRAPHIC_LINK_DATABASE_NAME)));
			db = (Hashtable)ios.readObject();
		}
		catch (Exception e)
		{
			System.err.println(e);
			e.printStackTrace(System.out);
			System.exit(1);
		}  // end catch
	}
	
	
	/** Tests to see if the argument spotcall has valid formation.
	*/
	protected boolean isWellFormed(String testString, int type)
	{
		Matcher m = null;
		
		if (testString == null)
		{
			System.out.println("GraphicLinkDatabase.isWellFormed()'s testString is null.");
			return false;
		}
		
		switch(type)
		{
			case SPOTCALL:
				m = pSpotCall.matcher(testString);
				break;
				
			case BOARDNO:
				m = pBoardno.matcher(testString);
				break;
				
			default:
				System.err.println("GraphicLinkDatabase.isWellFormed(): "+
					"unknown type: "+type);
				return false;
		}
		if (m.find())
		{
			return true;
		}

		System.err.println("GraphicLinkDatabase.isWellFormed(): the spotcall '"+
			testString + "' is malformed.");
		
		return false;
	}
	
	/** Returns the size of the database (number of entry pairs).
	*/
	public int size()
	{
		return db.size();
	}
	
	
	/** Returns a String version of the contents of the database.
	*/
	public String toString()
	{
		return db.toString();
	}
}