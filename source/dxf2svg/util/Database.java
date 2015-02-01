
/****************************************************************************
**
**	FileName:	Database.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This is the Base Database class where all the figures will be
**				stored. This is then, in turn, stored on a DatabaseContainer
**				which will act as a namespace for all the stored figures.
**
**	Date:		November 13, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 13, 2003
**				0.02 - May 5, 2004 Added getTable() method. All serialized databases
**				created before this date will now be unreadable because the 
**				underlying database structure has changed.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;
import java.io.*;

/** This class is a simple database that could incorporate numerous tables, each
*	with a number of records. Records are made up of a primary key (String value)
*	and the record data itself which is stored in the form of String arrays 
*	(String[]).
*	<P>
*	Because the underlying structure is a {@link java.util.Hashtable}, adding a 
*	record that duplicates another will result in the first record being over
*	written.
*	<P>
*	A typical application is the {@link dxf2svg.FigureSheetDatabase} which stores
*	all of a publication's figures as individual tables; each of these tables
*	stores the sheet number as a key and a Vector of spotcall titles etc as the value.
*
*	@version 	0.01 - November 13, 2003
*	@author		Andrew Nisbet
*/
public class Database implements Serializable
{
	// The database is essentially a Hashtable of Hashtables that stores
	// String[]s.
	protected Hashtable db;
	
	
	/////////////////////////
	//                     //
	//     Constructor     //
	//                     //
	/////////////////////////
	/** Default constructor that initializes the database.
	*/
	public Database()
	{
		db = new Hashtable();
	}
	
	///////////////////////
	//                   //
	//     Methods       //
	//                   //
	///////////////////////
	
	// These methods are included to comply with the contract with 
	// Serializable.
	private void writeObject(ObjectOutputStream out)
		throws IOException
	{
		if (out == null)
		{
			throw new NullPointerException(
				"dxf2svg.util.Database.writeObject() error: the ObjectOutputStream is null.");
		}
		
		out.writeObject(db);
	}
	
	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException
	{
		if (in == null)
		{
			throw new NullPointerException(
				"dxf2svg.util.Database.readObject() error: the ObjectInputStream is null.");
		}
		
		db = (Hashtable)in.readObject();
	}
	
	/** Creates a new record on the argument table.
	*	@param tableName of the table within the database.
	*	@param key Key String for the new record.
	*	@param values Vector of values to be stored in record.
	*	@return true if the database has the requested table and the
	*	record is created in that table successfully.
	*	@throws NullDatabaseObjectException if the table name or key are null.
	*/
	public boolean createRecord(String tableName, String key, Vector values)
	{
		Hashtable table;
		
		if (tableName == null)
		{
			throw new NullDatabaseObjectException("table");
		}
		
		if (key == null)
		{
			throw new NullDatabaseObjectException("record");
		}
		
		if (db.containsKey(tableName))
		{
			table = (Hashtable)db.get(tableName);
			table.put(key, values);
		}
		else	// create a new table and record at same time
		{
			table = new Hashtable();
			table.put(key, values.clone());
			// Put the Hashtable on the Hashtable db.
			db.put(tableName, table);
		}
		
		return true;
	}
	
	
	/** Returns a {@link java.util.Set} of the names of stored objects.
	*/
	public Set keySet()
	{
		return db.keySet();
	}	
	
	/** This method returns the values stored in a table called tableName by
	*	reference of the key argument.
	*	@return Vector of values stored, or alternatively null if the table
	*	or record does not exist in the database.
	*	@throws NullDatabaseObjectException if the table name or key are null.
	*/
	public Vector getRecord(String tableName, String key)
	{
		if (tableName == null)
		{
			throw new NullDatabaseObjectException("table");
		}
		
		if (key == null)
		{
			throw new NullDatabaseObjectException("key");
		}
		
		Hashtable table = (Hashtable)db.get(tableName);
		
		if (table == null)
		{
			System.err.println("Database.getRecord() error: no table by name of: "+tableName);
			return null;
		}
		
		if (table.containsKey(key))
		{		
			return (Vector)table.get(key);
		}
		
		System.err.println("Database.getRecord() error: '"+tableName+
			"' has no key of: '"+key+"'.");
		
		return null;
	}
	
	/** This method returns a Hashtable table with the name of the argument 'tableName'.
	*	The Hashtable includes entries listed by sheet number as a key and Vector of records as 
	*	the value where boardnos, spotcalls, titles and the like are stored.
	*/
	public Hashtable getTable(String tableName)
	{
		if (tableName == null)
		{
			System.err.println("The name of the table to be searched for is null.");
			return null;
		}
		
		Hashtable table = (Hashtable)db.get(tableName);
		
		if (table == null)
		{
			System.err.println("Database.getRecord() error: no table by name of: "+tableName);
			return null;
		}
		
		return table;		
	}
	
	
	/** Deletes a record (named key) from the table 'tableName'.
	*/
	public boolean delete(String tableName, String key)
	{
		if (tableName == null || key == null)
		{
			return false;
		}
		
		Hashtable table = (Hashtable)db.get(tableName);
		
		if (table == null)
		{
			return false;
		}
		
		if (table.containsKey(key))
		{		
			table.remove(key);
			return true;
		}
		return false;
	}
	
	
	/** Deletes an entire table (tableName).
	*/
	public boolean delete(String tableName)
	{
		if (tableName == null)
		{
			return false;
		}
		
		if (db.containsKey(tableName))
		{		
			db.remove(tableName);
			return true;
		}
		return false;
	}
	
	/** Returns the object by the key of name.
	*/
	protected Object get(Object name)
	{
		return db.get(name);
	}
	
	/** This method returns an array of strings in which the first value is the name of
	*	the table and the second is the name of the key for any supplied value. Both could 
	*	be null if the key could not be found in any table.
	*	@return String[] where element 0 is the name of the table, and element 1 is the 
	*	name of the primary key record name that contains the argument 'value'.
	*	@throws NullDatabaseObjectException if the value argument is a null String.
	*/
	public String[] getTableAndRecord(String value)
	{
		if (value == null)
		{
			throw new NullDatabaseObjectException("value");
		}
		
		Set tableNameSet = db.keySet();			// All the keys from the database.
		String tableName;						// Current table being searched.
		String recordKey;						// Current record key being searched.
		Vector values;							// Stored field values for this record.
		String[] tableRecord = new String[2]; 	// One for table, one for record name
		Iterator setIt = tableNameSet.iterator();
		while (setIt.hasNext())
		{
			tableName = (String)setIt.next();
			Hashtable table = (Hashtable)db.get(tableName);
			// Get the tables keys
			Set records = table.keySet();
			Iterator recIt = records.iterator();
			// Search each record.
			while (recIt.hasNext())
			{
				recordKey = (String)recIt.next();
				// Now search the fields
				values = (Vector)table.get(recordKey);

				if (values == null)
				{
					break;
				}
				for (int i = 0; i < values.size(); i++)
				{
					String valuesStr = (String)values.get(i);
					// if one of the store values is null test the next one
					if (valuesStr == null)
					{
						continue;
					}
					if (valuesStr.equals(value))
					{
						tableRecord[0] = tableName;
						tableRecord[1] = recordKey;
						return tableRecord;
					}
				}  // end for
			}  // end while
		}  // end while
		
		return null;
	}
	
	/** This method returns true if the requested key was found in the database
	*	false otherwise.
	*/
	public boolean containsKey(String key)
	{
		if (key == null)
		{
			System.err.println("Database.containsKey() error: key is null.");
		}
		
		return db.containsKey(key);
	}

	///////////////////////
	//                   //
	//    Exceptions     //
	//                   //
	///////////////////////
	/** Gets thrown typically, if searches or entry creations are attempted with null objects.
	*/
	protected class NullDatabaseObjectException extends RuntimeException
	{
		protected NullDatabaseObjectException(String s)
		{
			System.err.println("Access to a database "+s+" was attempted using a name that is null.");
		}
	}
	
}  // end FigureDatabase