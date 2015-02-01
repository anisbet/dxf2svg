
/****************************************************************************
**
**	FileName:	DatabaseContainer.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class is a container class for dxf2svg.util.Database.java
**				classes. It allows for namespaces to be allowed when storing data
**				by storing individual Databases in compartments. This creates a
**				scope within the Container that allows for multipule instances 
**				or an database stored. (See {@link dxf2svg.IETMDatabaseContainer} 
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
**
*****************************************************************************/

package dxf2svg.util;

import java.util.*;
import java.io.*;

/** This class is a container class for dxf2svg.util.Database.java
*	classes. It allows for namespaces to be allowed when storing data
*	by storing individual Databases in compartments. This creates a
*	scope within the Container that allows for multipule instances 
*	or an database stored. (See {@link dxf2svg.LibraryCatalog}
*	for more details.)
*
*	@version 	0.01 - December 4, 2003
*	@author		Andrew Nisbet
*/
public class DatabaseContainer implements Serializable
{
	protected Hashtable dbContainer;
	
	/////////////////////////////
	//                         //
	//       Constructor       //
	//                         //
	/////////////////////////////
	public DatabaseContainer()
	{
		// Ensure there is only one container.
		if (dbContainer != null)
		{
			return;
		}
		
		dbContainer = new Hashtable();
	}
	
	
	/////////////////////////////
	//                         //
	//       Mehtods           //
	//                         //
	/////////////////////////////
	
	/** Deletes an entire database from the database container
	*	@return false if the named database is null or could not be found and true
	*	otherwise.
	*/
	public boolean deleteNamespace(String nameSpace)
	{
		if (nameSpace == null)
		{
			return false;
		}
		
		if (dbContainer.containsKey(nameSpace))
		{		
			dbContainer.remove(nameSpace);
			return true;
		}
		return false;		
	}
	
	/** Returns a list of all the namespaces stored in the database as a {@link java.util.Set}.
	*/
	public Set keySet()
	{
		return dbContainer.keySet();
	}
	
	/** Returns the value to which the specified key is mapped in this database container.
	*/
	public Object get(Object key)
	{
		return dbContainer.get(key);
	}
	
	/** Stores an object in the container to be referenced by the key <em>key</em>.
	*/
	public void put(Object key, Object value)
	{
		if (key == null)
		{
			System.err.println("DatabaseContainer.put() error: key is null and will not be stored.");
			return;
		}
		
		if (value == null)
		{
			System.err.println("DatabaseContainer.put() error: value is null and will not be stored.");
			return;
		}
		
		dbContainer.put(key, value);
	}
	
	/** This method returns true if the requested namespace was found in the container
	*	false otherwise.
	*/
	public boolean containsNamespace(String nameSpace)
	{
		if (nameSpace == null)
		{
			System.err.println("DatabaseContainer.containsNamespace() error: unknown namespace: null.");
		}
		
		return dbContainer.containsKey(nameSpace);
	}
	
	/** This method is included to comply with the contract of 
	*	the Serializable interface (though not explicitly mentioned in the interface).
	*	@see java.io.Serializable
	*/
	private void writeObject(ObjectOutputStream out)
		throws IOException
	{
		if (out == null)
		{
			throw new NullPointerException(
				"dxf2svg.util.DatabaseContainer.writeObject() error: the ObjectOutputStream is null.");
		}
		
		out.writeObject(dbContainer);
	}

	/** This method is included to comply with the contract of 
	*	the Serializable interface (though not explicitly mentioned in the interface).
	*	@see java.io.Serializable
	*/	
	private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException
	{
		if (in == null)
		{
			throw new NullPointerException(
				"dxf2svg.util.DatabaseContainer.readObject() error: the ObjectInputStream is null.");
		}
		
		dbContainer = (Hashtable)in.readObject();
	}	
}  // end class