
/****************************************************************************
**
**	FileName:	SymbolNode.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates node in the symbol table within the Svg Animation Language.
**
**	Date:		May 20, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 20, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.sally;

/**	This class represents a node within the Svg Animation Language's symbol
*	table.
*
*	@version 	0.01 - May 20, 2003
*	@author		Andrew Nisbet
*/
public class SymbolNode
{
	private SymbolNode leftNode 	= null;
	private SymbolNode rightNode 	= null;
	private Symbol symbol			= null;

	/////////////////////////////////////////
	//				Constructor
	/////////////////////////////////////////
	public SymbolNode(Symbol s)
	{	symbol = s;		}

	/////////////////////////////////////////
	//				Methods
	/////////////////////////////////////////
	/** Returns the left node (or subtree) of the current node.
	*/
	public SymbolNode getLeftNode()
	{	return leftNode;	}
	
	/** Returns the right node (or subtree) of the current node.
	*/
	public SymbolNode getRightNode()
	{	return rightNode;	}

	/** Allows the setting of the left node of the current node.
	*/
	public void setLeftNode(SymbolNode s)
	{	leftNode = s; 	}
	
	/** Allows the setting of the right node of the current node.
	*/
	public void setRightNode(SymbolNode s)
	{	rightNode = s; 	}
	
	/** Sets the symbol payload data of the node to an arbitrary data type.
	*/
	public void setSymbol(Symbol s)
	{	symbol = s;		}
	
	/**	Returns the symbol data of this node.
	*/
	public Symbol getSymbol()
	{	return symbol;	}

	/** Returns the symbol's key, or variable name.
	*/
	public String getSymbolKey()
	{	return symbol.getName();	}
}