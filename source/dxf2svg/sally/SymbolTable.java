
/****************************************************************************
**
**	FileName:	SymbolTable.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the symbol table within the Svg Animation Language.
**
**	Date:		May 20, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 20, 2003
**				1.0	 - January 5, 2005 Made changes to printTable() method to
**				fix ClassCastException error.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.sally;

import java.util.Vector;			// to hold names of all the nodes.
import java.util.Iterator;			// For printing a special list.

/**	This class represents the symbol table within the Svg Animation Language's.
*	A symbol table is a binary tree that contains all the defined variables 
*	currently available in this instance of SALly.
*
*	@version 	0.01 - May 20, 2003
*	@author		Andrew Nisbet
*/
public class SymbolTable
{	
	private final static String version = "0.01 - May 20, 2003";
	private SymbolNode root 		= null;
	private SymbolNode head 		= null;
	private Symbol symbolFactory	= null;
	private Vector vNodeNames;
	private int nodeCount;
	
	// Data structure to contain unordered lists of names.
	private Vector dataList;		// Stores symbols, not symbolNodes.

	///////////////////////////////////////////////////////
	//				Constructor
	///////////////////////////////////////////////////////
	public SymbolTable()
	{	}
	
	///////////////////////////////////////////////////////
	//					Methods
	///////////////////////////////////////////////////////
	/**	Inserts a new Symbol object into the symbol table. This list does not
	*	store duplicates.
	*/
	public void insert(Symbol s)
	{
		if (s.getSymbolType() == Symbol.UNORDERED_STRING_SYMBOL) // Special list case
		{
			if (dataList == null)
			{
				dataList = new Vector();
				dataList.add(s);
				return;
			}
			// Search the list for a pre-existing symbol of this name.
			Symbol tmpS = getSymbol(s.getName());
			if (tmpS == null)	// We didn't find it.
			{
				dataList.add(s);
			}
		}
		else	// regular symbol
		{
			if (root == null)
			{
				root = new SymbolNode(s);			
			}
			else
			{
				insertNode(root, s);
			}
		}
	}
	
	// Performs the insertion of the node recursively.
	private void insertNode(SymbolNode n, Symbol s)
	{
		head = n;
		if (head == null)
		{
			head = new SymbolNode(s);
		}
		else if (head.getSymbolKey().compareTo(s.getName()) > 0)
		{
			if (head.getLeftNode() == null)
				head.setLeftNode(new SymbolNode(s));
			else
				insertNode(n.getLeftNode(), s);
		}
		else if (head.getSymbolKey().compareTo(s.getName()) < 0)
		{
			if (head.getRightNode() == null)
				head.setRightNode(new SymbolNode(s));
			else
				insertNode(n.getRightNode(), s);
		}
		else	// the two keys are equal so we over write the current symbol
		{
			head.setSymbol(s);
		}
	}
	
	/** Prints the Symbol table with dots to represent the level of the tree that the symbol
	*	was found.
	*/
	public void printTable()
	{
		if (dataList != null) // Special symbol list
		{
			Iterator it = dataList.iterator();
			while (it.hasNext())
			{
				// Changed because of ClassCastException error.
				//System.out.println((String)it.next());
				System.out.println((it.next()).toString());
			}
		}
		else	// regular symbol table.
		{
			printNode(root, 0);
		}
	}
	
	// Prints the tree nodes recursively
	private void printNode(SymbolNode sn, int level)
	{
		//System.out.println("Level: "+String.valueOf(level));
		if (sn != null)
		{
			printNode(sn.getLeftNode(), (level +1));
			for (int i = 0; i < level; i++)
				System.out.print(".");
			System.out.println(sn.getSymbol());
			printNode(sn.getRightNode(), (level +1));
		}
	}
	
	/** Retrieves the value of the desired symbol, and returns it.
	*/
	public Object getSymbolValue(String name)
	{
		Symbol s = getSymbol(name);
		try
		{
			return s.getValue();
		}
		catch (NullPointerException e)
		{
			return null;
		}
	}
	
	/**	Searches Symbol table for the argument value.
	*/
	public Symbol getSymbol(String name)
	{
		if (dataList != null)	// special list case.
		{
			Symbol s = null;
			for (int i = 0; i < dataList.size(); i++)
			{
				s = (Symbol)dataList.get(i);
				if (s.getName().equals(name))
				{
					return s;
				}
			} // end for
			return null;
		}
		else  // regular symbol table.
		{
			SymbolNode s = find(root, name);
			if (s == null)
				return null;
			return s.getSymbol();
		}
	}
	
	// Provides the recursive search functionality.
	private SymbolNode find(SymbolNode sn, String name)
	{
		if (sn == null)	// table is empty
		{
			return null;
		}
		else if (name.compareTo(sn.getSymbolKey()) < 0)
		{
			return find(sn.getLeftNode(), name);
		}
		else if (name.compareTo(sn.getSymbolKey()) == 0)	// found it
		{
			return sn;
		}
		else 	// must be greater
		{
			return find(sn.getRightNode(), name);
		}
	}


	/** Returns a vector that contains all the names of the variables currently stored
	*	in the symboltable. The names are stored as Strings.
	*/
	public Vector list()
	{
		vNodeNames = new Vector();
		
		if (dataList != null)
		{
			Symbol s = null;
			Iterator it = dataList.iterator();
			while (it.hasNext())
			{
				s = (Symbol)it.next();
				vNodeNames.add(s.getName());
			}	// end while
		}
		else
		{
			getNodeName(root);
		}
		return vNodeNames;
	}
	
	// Prints the tree nodes recursively
	private void getNodeName(SymbolNode sn)
	{
		if (sn != null)
		{
			getNodeName(sn.getLeftNode());
			vNodeNames.add(sn.getSymbolKey());
			getNodeName(sn.getRightNode());
		}
	}
	
	/** Returns the total number of nodes in the binary tree.
	*/
	public int size()
	{
		if (dataList != null)
		{
			return dataList.size();
		}
		else
		{
			nodeCount = 0;
			addNode(root);
			return nodeCount;
		}
	}
	
	private void addNode(SymbolNode sn)
	{
		if (sn != null)
		{
			addNode(sn.getLeftNode());
			nodeCount++;
			addNode(sn.getRightNode());
		}
	}
	
	/** Prints the symbol table and associated classes version information.
	*/
	public String toString()
	{
		return "SymbolTable, version "+version+".";
	}
	
} // end of SymbolTable.