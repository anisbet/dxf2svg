
/****************************************************************************
**
**	FileName:	Symbol.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the variable data of a symbol in Svg Animation Language.
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

/**	This class encapsulates a symbol (read variable) in the SALly language.
*	It can hold one of the following forms of primative data types:
*<P><UL>
*	<LI> integer - 4 bytes
*	<LI> byte - 1 byte
*	<LI> double - floating point number.
*	<LI> String - Java String object.
*</UL>
*	<P>Symbol uses a factory method to create these objects so it is easily expanded
*	to include a wider range and/or more complex data types.
*
*	@version 	0.01 - May 20, 2003
*	@author		Andrew Nisbet
*/
public class Symbol	// This class encapsulates the data within a node within a symboltable
{

	private String name = null;
	private int type;
	public static final int UNORDERED_STRING_SYMBOL	= 1;
	public static final int INTEGER_SYMBOL			= 2;
	public static final int DOUBLE_SYMBOL			= 3;
	public static final int BYTE_SYMBOL				= 4;
	public static final int STRING_SYMBOL			= 5;
	public static final int OBJECT_SYMBOL			= 6;
	
	/**	Returns a new instance of an UnorderedStringList based symbol.
	*/
	public Symbol getInstanceOf(String name)
	{	return new UnorderedStringList(name);	}

	/**	Returns a new instance of an integer based symbol.
	*/
	public Symbol getInstanceOf(String name, int value)
	{	return new IntegerSymbol(name, value);	}

	/**	Returns a new instance of an double symbol.
	*/	
	public Symbol getInstanceOf(String name, double value)
	{	return new DoubleSymbol(name, value);	}

	/**	Returns a new instance of an byte based symbol.
	*/	
	public Symbol getInstanceOf(String name, byte value)
	{	return new ByteSymbol(name, value);	}

	/**	Returns a new instance of an String symbol.
	*/
	public Symbol getInstanceOf(String name, String value)
	{	return new StringSymbol(name, value);	}
	
	/**	Returns a new instance of an Object based symbol.
	*/	
	public Symbol getInstanceOf(String name, Object o)
	{	return new ObjectSymbol(name, o);	}
				
	protected String getName()
	{	return name;	}
	
	public Object getValue()
	{	return new Object();	}
	
	protected void setType(int t)
	{	type = t;	}
	
	//////// April 6, changed this method's name from getType() to avoid conflict with SvgElement's
	//////// getType()
	/** Returns the type of symbol this is. 
	*/
	public int getSymbolType()
	{	return type;	}

	protected void setValue(int v){ }
	
	protected void setValue(byte v){ }
	
	protected void setValue(double v){	}
	
	protected void setValue(String v){	}
	
	protected void setValue(Object v){	}
	
	protected void setName(String n)
	{	name = n;	}
	
	
	/////////////////////////////////////////////////////////
	//					Internal Classes
	/////////////////////////////////////////////////////////
	/**	This class encapsulates an integer as a Symbol Object
	*	suitable for placing in the symbol table.
	*/
	protected final class IntegerSymbol extends Symbol
	{
		private int value;
		
		protected IntegerSymbol(String name, int value)
		{	
			setType(INTEGER_SYMBOL);
			setName(name);
			this.value = value;
		}
		
		protected void setValue(int v)
		{	value = v;	}	
		
		public Object getValue()
		{	return new Integer(value);	}
		
		public String toString()
		{
			return "{" + getName() + " = " + String.valueOf(value) + "}";
		}
	}

	/**	This class encapsulates a byte value (not Byte) as a Symbol Object
	*	suitable for placing in the symbol table.
	*/	
	protected final class ByteSymbol extends Symbol
	{
		private byte value;
		
		protected ByteSymbol(String name, byte value)
		{
			setType(BYTE_SYMBOL);
			setName(name);
			this.value = value;
		}
		
		protected void setValue(byte v)
		{	value = v;	}	
		
		public Object getValue()
		{	return new Byte(value);	}	
		
		public String toString()
		{
			return "{" + getName() + " = " + String.valueOf(value) + "}";
		}
	}

	/**	This class encapsulates a double value (not Double) as a Symbol Object
	*	suitable for placing in the symbol table.
	*/	
	protected final class DoubleSymbol extends Symbol
	{
		private double value;
		
		protected DoubleSymbol(String name, double value)
		{
			setType(DOUBLE_SYMBOL);
			setName(name);
			this.value = value;
		}

		protected void setValue(double v)
		{	value = v;	}	
		
		public Object getValue()
		{	return new Double(value);	}
		
		public String toString()
		{
			return "{" + getName() + " = " + String.valueOf(value) + "}";
		}
	}

	/**	This class encapsulates a String as a Symbol Object
	*	suitable for placing in the symbol table.
	*/	
	protected final class StringSymbol extends Symbol
	{
		private String value = null;
		
		protected StringSymbol(String name, String value)
		{	
			setType(STRING_SYMBOL);
			setName(name);
			this.value = value;
		}
		
		protected void setValue(String v)
		{	value = v;	}	
				
		public Object getValue()
		{	return value;	}
		
		public String toString()
		{
			return "{" + getName() + " = " + value + "}";
		}
	}
	
	/**	This class encapsulates a single String word as a list Symbol Object
	*	suitable for placing in the symbol table.
	*/	
	protected final class UnorderedStringList extends Symbol
	{		
		protected UnorderedStringList(String name)
		{	
			setType(UNORDERED_STRING_SYMBOL);
			setName(name);
		}	
		
		protected void setValue(String v)
		{	System.err.println("UnorderedStringList cannot store values.");	}	
		
		public Object getValue()
		{	
			System.err.println("UnorderedStringList does not contain values.");
			return null;
		}
		
		public String toString()
		{
			return "{" + getName() + "}";
		}
	}

	/**	This class encapsulates an Object as a Symbol Object
	*	suitable for placing in the symbol table.
	*/	
	protected final class ObjectSymbol extends Symbol
	{
		private Object o = null;
		
		protected ObjectSymbol(String name, Object o)
		{	
			setType(OBJECT_SYMBOL);
			setName(name);
			this.o = o;
		}

		protected void setValue(Object o)
		{	this.o = o;	}	
		
		// Note: there is a security problem with this for the caller is able
		// to alter the object with impunity.
		public Object getValue()
		{	return o;	}
		
		public String toString()
		{
			return "{" + getName() + " = " + o.toString() + "}";
		}
	}
	
	/**	This RuntimeException is thrown if the data type requested has not been defined in this
	*	class yet.
	*/
	protected class UnSupportedSymbolTypeException extends RuntimeException
	{
		protected UnSupportedSymbolTypeException(int type, String name)
		{
			System.err.println("Symbol error: variable '"+name+"' was declared to an unsupported "+
				"data type of '"+type+"'.");
		}
	}
}