
/****************************************************************************
**
**	FileName:	SvgAnimationLanguage.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class represents the implementation of the Svg Animation
**				Language. 
**
**	Date:		May 27, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.1 - May 27, 2003
**				0.2 - Oct 23, 2003 Fixed a bug in getKeys(String nameSpace)
**				that would include the names of any animation layers in the
**				<script> section of the svg if, and only if, there were no
**				:javaScript{} instructions to include in the file.
**				0.3 - April 2, 2004 Added modify Heap and functionality 
**				to effect modifications.
**				0.4 - July 23, 2004 Ignore SvgAnimator elements in modifyElement().
**				0.41 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.sally;

import java.io.*;				// File StreamTokenizer FileReader.
import java.util.*;				// for token list

import dxf2svg.SvgBuilder;
import dxf2svg.animation.*;		// For SvgAnimator.
import dxf2svg.DxfPreprocessor;	// for verbosity
import dxf2svg.util.*;			// attribute object for modify command EventAttributeModifier
import dxf2svg.svg.*;			// Svg elements (SvgElement and SvgText specifically).
import dxf2svg.svg.SvgCollection;// for recursive modifying functions.

/**	This class represents the implementation of the Svg Animation Language API.
*	This class tokenizes the config.d2s file and passes the tokens to the parser.
*	It also contains and maintains the tables of symbols returned from the parser.
*
*	@version	0.4 - July 23, 2004
*	@author		Andrew Nisbet
*	@see dxf2svg.sally.Parser for more information on SALly grammar.
*/
public class SvgAnimationLanguage
{

	/////////////////// end constants /////////////////////////
	// The stuff required to tokenize a stream.
	private StreamTokenizer st;
	private Stack tokenStack;
	private int tokenNum = 0;
	private boolean packTokens = false;
	private StringBuffer tokenBuff;
	private boolean isQuotedString = false;	// makes EOL significant or not.
	
	// This is the storage location of all the symboltables
	private Hashtable SALHeap;				// General storage of symbol tables for everything but
											// animation and layerordering.
	private AnimationHeap animHeap;
	private ModifyHeap addHeap, changeHeap, deleteHeap;	// Object Modification instructions. 
	
	private SymbolTable SAL_LO_HeadHeap;	// List of layers to appear at the top of the layering
	private SymbolTable SAL_LO_TailHeap;	// List to appear at the bottom.
	public final static int HEAP			= 1;
	public final static int ANIMATION_HEAP	= 2;
	public final static int HEAD_HEAP		= 4;
	public final static int TAIL_HEAP		= 8;
	public final static int ADD_HEAP    	= 16;
	public final static int CHANGE_HEAP		= 32;
	public final static int DELETE_HEAP		= 64;
	
	private int populatedHeaps = 0;
	private boolean hasModifiedContent = false;

	public SvgAnimationLanguage(String configFile)
	{
		// Check argument for validity.
		File IN	= new File(configFile);
		FileReader fr = null;
		try{
			fr 	= new FileReader(IN);
		} catch (FileNotFoundException e){
			System.err.println(e);
		}

		// create new Parser object
		Parser parser = new Parser(this);
		SALHeap = new Hashtable();


		// create and set up tokenizer
		st = new StreamTokenizer(fr);
		// accepts C/C++ comments
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.eolIsSignificant(true); // this will reset the token count for a line.
		st.quoteChar('\"');
		// add these word char identifiers
		st.wordChars(58,59);	// these characters, ':' ';', are word chars.
		st.wordChars(61,61);	// '='.
		st.wordChars(44,44);	// ','. comma
		st.wordChars(123,123);
		st.wordChars(125,125);	// '{' and '}'
		st.wordChars(95,95);	// '_' Some files have underscores in their names.
		st.wordChars(35,35);	// '#' for name/identifier separation in modify command.
		// case insensitivity.
		// st.lowerCaseMode(true);

		try{
			// create a new tokenStack if necessary.
			tokenStack = new Stack();
			while (st.nextToken() != st.TT_EOF)
			{
				switch(st.ttype)
				{
					case (int)'\"':		// quote char
						tokenNum++;		// the whole string is one token
						parser.parseToken(st.sval);
						break;

					case StreamTokenizer.TT_WORD:
						// we test to see if any of the punctuation are present
						// inside of the string like, 'token,token'.
						findEmbeddedTokens(parser, st.sval);
						break;

					case StreamTokenizer.TT_NUMBER:	// found a number in the stream
						tokenNum++;
						// this is one token; the line above will increment
						// token count when it gets passed to the parser.
						parser.parseToken(String.valueOf(st.nval));
						break;

					case StreamTokenizer.TT_EOL:
						tokenNum = 0;
						break;

					default:
						break;
				} // end switch
				//System.out.println("tokenNum: "+tokenNum);
			} // end while
		} catch (IOException e){
			System.err.println(e);
			System.exit(-1);
		}
		
	}	// constructor



	// This detects character tokens from within other tokens and splits them
	// at the token position and passes them off to trimToken to extract
	// potential multipul character tokens. It looks for tokens like 'token,token'
	// and splits them at the ',' and passes them on.
	private void findEmbeddedTokens(Parser p, String s)
	{
		// we test to see if any of the punctuation are present
		// inside of the string like, 'token,token'.
		int pos;
		if ((pos = s.indexOf(':')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else if ((pos = s.indexOf(';')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else if ((pos = s.indexOf('=')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else if ((pos = s.indexOf(',')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else if ((pos = s.indexOf('{')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else if ((pos = s.indexOf('}')) > 0 && pos < s.length())
		{
			trimToken(p, s.substring(0,pos));
			findEmbeddedTokens(p, s.substring(pos));
		}
		else
		{
			trimToken(p,s);
		}
	}


	// This method trims any and all terminating characters from the end of the
	// argument string and then either casts them off to the parser, or calls
	// the method recursively until all the tokens are consumed. It is for dealing
	// with tokens that are not delimited by white space.
	private void trimToken(Parser p, String s)
	{
		String token = null;	// The token word.
		String punct = null;	// The punctuation character
		if (s.length() > 1 &&
			(
				s.endsWith(";") ||
				s.endsWith(":") ||
				s.endsWith("=") ||
				s.endsWith(",") ||
				s.endsWith("{") ||
				s.endsWith("}")
			)
		)
		{
			token = s.substring(0, (s.length() -1));
			punct = s.substring(s.length() -1);
			tokenStack.push(punct);
			// send it round again to get the next punc character
			findEmbeddedTokens(p, token);
		}
		else if (s.length() > 1 &&
			(
				s.startsWith(";") ||
				s.startsWith(":") ||
				s.startsWith("=") ||
				s.startsWith(",") ||
				s.startsWith("{") ||
				s.startsWith("}")
			)
		)
		{
			// the first char is the punctuation
			punct = s.substring(0,1);
			token = s.substring(1);
			tokenNum++;
			p.parseToken(punct);
			findEmbeddedTokens(p, token);
		}
		else // that was the last terminating token
		{
			tokenNum++;
			p.parseToken(s);

			while (! tokenStack.isEmpty())
			{
				tokenNum++;
				p.parseToken((String)tokenStack.pop());
			}
		}
	}	// end trimToken()



	/** Returns the current line number from the tokenizer.
	*/
	public int getLineNumber()
	{	return st.lineno();		}



	/** Returns the current token number on this line.
	*/
	public int getTokenNumber()
	{	return tokenNum;		}
	
	
	
	
	/** Many of the labelled lists store their information on separate heaps. This allows for 
	*	variable scope, that is, two different lists can have the same variable name without 
	*	a conflict or name collision. This 'space' is called a name space. For instance animation 
	*	objects have their own heaps that are named after the types of animation they perform. 
	*	The reference for the heap object is the animations target name like 'wires' or 'solid'.
	*	@param type Integer enumerated value of the type of table expected.
	*	@param tableName String name of the table; the animation or modify target name.
	*	@param st the variables and values stored that relate to that target.
	*/
	public void addSymbolTable(int type, String tableName, SymbolTable st)
	{
		// Note that any table that has the same name will over-write the stored table.
		if (tableName == null || tableName.equals(""))
		{
			System.err.println("SALly Error: parser passed a table that has no name. Ignoring.");
			return;
		}
		if (st == null)
		{
			System.err.println("SALly Error: parser passed a table that is null. Ignoring.");
			return;
		}
		
		switch (type)
		{
			case SALConsts.S_L_GENERAL:
				if (SALHeap == null)
					populatedHeaps += HEAP;
				//System.out.println("-->"+tableName);
				//st.printTable();
				SALHeap.put(tableName, st);
				break;
				
			case SALConsts.S_A_SET:
				if (animHeap == null)
					animHeap = new AnimationHeap();
				animHeap.addObjectToHeap(SvgAnimator.SET, tableName, st);
				break;
				
			case SALConsts.S_A_ANIMATE:
				if (animHeap == null)
					animHeap = new AnimationHeap();
				animHeap.addObjectToHeap(SvgAnimator.ANIMATE, tableName, st);
				break;
				
			case SALConsts.S_A_COLOUR:
				if (animHeap == null)
					animHeap = new AnimationHeap();
				animHeap.addObjectToHeap(SvgAnimator.ANIMATE_COLOUR, tableName, st);
				break;
				
			case SALConsts.S_A_MOTION:
				if (animHeap == null)
					animHeap = new AnimationHeap();
				animHeap.addObjectToHeap(SvgAnimator.ANIMATE_MOTION, tableName, st);
				break;
				
			case SALConsts.S_A_TRANSFORM:
				if (animHeap == null)
					animHeap = new AnimationHeap();
				animHeap.addObjectToHeap(SvgAnimator.ANIMATE_TRANSFORM, tableName, st);
				break;
				
			case SALConsts.S_MOD_ADD:
				if (addHeap == null)
				{
					addHeap = new ModifyHeap();
				}
				
				addHeap.addObjectToHeap(SALConsts.S_MOD_ADD, tableName, st);				
				break;
				
			case SALConsts.S_MOD_DELETE:
				if (deleteHeap == null)
				{
					deleteHeap = new ModifyHeap();
				}
				deleteHeap.addObjectToHeap(SALConsts.S_MOD_DELETE, tableName, st);
				break;

			case SALConsts.S_MOD_CHANGE:
				if (changeHeap == null)
				{
					changeHeap = new ModifyHeap();
				}
				changeHeap.addObjectToHeap(SALConsts.S_MOD_CHANGE, tableName, st);
				break;
				
			case SALConsts.S_LO_TAIL:
				if (SAL_LO_TailHeap == null)
					populatedHeaps += TAIL_HEAP;
				SAL_LO_TailHeap = st;
				break;
				
			case SALConsts.S_LO_HEAD:
				if (SAL_LO_HeadHeap == null)
					populatedHeaps += HEAD_HEAP;
				SAL_LO_HeadHeap = st;
				break;	
				
			default:
				System.err.println("SALly addSymbolTable(): namespace has no definition. '"+type+"'.");
				
		}	// end switch
	}




	/**	Search the heaps for a matching namespace and report on the size of its 
	*	SymbolTable. There are five pre-defined heaps which may or may not 
	*	contain any variables. The heaps, in turn, could contain any number of 
	*	symbol tables. These symbol tables represent namespaces.
	*/
	public int size(String nameSpace)
	{
		// Search the heaps for a matching name and report on the size of the 
		// SymbolTable.
		if (SALHeap != null)
		{
			Set keys = SALHeap.keySet();
			Iterator i = keys.iterator();
			String name = new String();
			while(i.hasNext())
			{
				name = (String)i.next();
				if (name.equals(nameSpace))
				{
					SymbolTable st = (SymbolTable)SALHeap.get(nameSpace);
					return st.size();
				}	// end if
			} // end while
		}
		
		if (animHeap != null)
		{
			return animHeap.size();
		}
		
		if (addHeap != null)
		{
			return addHeap.size();
		}

		if (changeHeap != null)
		{
			return changeHeap.size();
		}
		
		if (deleteHeap != null)
		{
			return deleteHeap.size();
		}
		
		if (SAL_LO_HeadHeap != null)
			return SAL_LO_HeadHeap.size();
			
		if (SAL_LO_TailHeap != null)
			return SAL_LO_TailHeap.size();
		
		return 0;
	}


	/**	Search the heaps for a matching namespace and returns the keys of its 
	*	SymbolTable. There are five pre-defined heaps which may or may not 
	*	contain any variables. The heaps, in turn, could contain any number of 
	*	symbol tables. These symbol tables represent namespaces.
	*/
	public String[] getKeys(String nameSpace)
	{
		Vector vl = null;
		String[] retStrs = null;
		// Search the heaps for a matching name and report on the size of the 
		// SymbolTable.
		if (SALHeap != null)
		{
			Set keys = SALHeap.keySet();
			Iterator i = keys.iterator();
			String name = new String();
			while(i.hasNext())
			{
				name = (String)i.next();
				if (name.equals(nameSpace))
				{
					SymbolTable st = (SymbolTable)SALHeap.get(nameSpace);
					vl = st.list();
					retStrs = new String[vl.size()];
					vl.toArray(retStrs);
				}	// end if
			} // end while
			////// version 0.2 //////////
			// Moved this next statement to here from the end of the above if statement
			// to avoid falling through to the animation heap if statement if 
			// name.equals(nameSpace) returns false.
			return retStrs;
		}
		
		
		if (animHeap != null)
		{
			Set keys = animHeap.keySet();
			retStrs = new String[animHeap.size()];
			keys.toArray(retStrs);
			return retStrs;
		}
		
		if (addHeap != null)
		{
			Set keys = addHeap.keySet();
			retStrs = new String[addHeap.size()];
			keys.toArray(retStrs);
			return retStrs;
		}

		if (changeHeap != null)
		{
			Set keys = changeHeap.keySet();
			retStrs = new String[changeHeap.size()];
			keys.toArray(retStrs);
			return retStrs;
		}
		
		if (deleteHeap != null)
		{
			Set keys = deleteHeap.keySet();
			retStrs = new String[deleteHeap.size()];
			keys.toArray(retStrs);
			return retStrs;
		}

		if (SAL_LO_HeadHeap != null)
		{
			if (nameSpace.equals(SALConsts.K_HEAD))
			{
				vl = SAL_LO_HeadHeap.list();
				retStrs = new String[vl.size()];
				vl.toArray(retStrs);
				return retStrs;			
			}
		}
			
		if (SAL_LO_TailHeap != null)
		{
			if (nameSpace.equals(SALConsts.K_TAIL))
			{
				vl = SAL_LO_TailHeap.list();
				retStrs = new String[vl.size()];
				vl.toArray(retStrs);
				return retStrs;
			}
		}
		
		return new String[0];
	}	// end getKeys(String)


	
	/** Returns a complete set of variable names from a specified namespace.
	*	@return keys, may be null if there are no variables stored or the 
	*	variables from an unknown namespace are requested.
	*/
	public String[] getKeys(int heapID)
	{
		String[] keys	= null;
		Set keySet 		= null;
		Vector vl		= null;
		
		switch(heapID)
		{
			case HEAP:
				if (SALHeap != null)
				{
					keys = new String[SALHeap.size()];
					keySet = SALHeap.keySet();
					keySet.toArray(keys);
				}
				break;
				
			case ANIMATION_HEAP:
				if (animHeap != null)
				{
					keySet = animHeap.keySet();
					keys = new String[keySet.size()];
					keySet.toArray(keys);
				}
				break;

			case ADD_HEAP:
				if (addHeap != null)
				{
					keySet = addHeap.keySet();
					keys = new String[keySet.size()];
					keySet.toArray(keys);
				}
				break;
				
			case CHANGE_HEAP:
				if (changeHeap != null)
				{
					keySet = changeHeap.keySet();
					keys = new String[keySet.size()];
					keySet.toArray(keys);
				}
				break;
				
			case DELETE_HEAP:
				if (deleteHeap != null)
				{
					keySet = deleteHeap.keySet();
					keys = new String[keySet.size()];
					keySet.toArray(keys);
				}
				break;
				
			case HEAD_HEAP:
				if (SAL_LO_HeadHeap != null)
				{
					keys = new String[SAL_LO_HeadHeap.size()];
					vl = SAL_LO_HeadHeap.list();
					vl.toArray(keys);
				}
				break;
				
			case TAIL_HEAP:
				if (SAL_LO_TailHeap != null)
				{
					keys = new String[SAL_LO_TailHeap.size()];
					vl = SAL_LO_TailHeap.list();
					vl.toArray(keys);
				}
				break;
								
			default:
				System.err.println("SvgAnimationLanguage.getSymbolTableKeys(): non-existant namespace.");
				break;
		}
		return keys;
	}	// end getKeys()
	
	
	
	/** This method will return an object form of the stored value associated with the 
	*	String name. If you query a single item list you will get &lt;none&gt; since that
	*	is the default initial value for all stored variables.
	*	@param heapID Type of heap you wish to search.
	*	@param listName the name of the list within the namespace. In the case of animation,
	*	there may be many stored lists that all animate motion in the graphic. This value
	*	would the name of the animation target.
	*	@param listItem of the variable that you require the associated data to.
	*/
	public Object getValue(int heapID, String listName, String listItem)
	{
		// Search all the heaps looking for our value; start with SALHeap.
		switch(heapID)
		{
			case HEAP:
				if (SALHeap != null)
				{
					SymbolTable st = (SymbolTable)SALHeap.get(listName);
					Symbol s = (Symbol)st.getSymbol(listItem);
					return s.getValue();
				}
				break;
				
			case ANIMATION_HEAP:
				if (animHeap != null)
				{
					return animHeap.getValue(listItem);
				}
				break;
				
			case ADD_HEAP:
				if (addHeap != null)
				{
					return addHeap.getValue(listItem);
				}
				break;
				
			case CHANGE_HEAP:
				if (changeHeap != null)
				{
					return changeHeap.getValue(listItem);
				}
				break;
				
			case DELETE_HEAP:
				if (deleteHeap != null)
				{
					return deleteHeap.getValue(listItem);
				}
				break;
							
			case HEAD_HEAP:
				return "<none>";
				
			case TAIL_HEAP:
				return "<none>";
								
			default:
				System.err.println("SvgAnimationLanguage.getValue(): non-existant namespace.");
				break;
		}		
		return null;	// This will be sent back if no heap has a value of that type.
	}
	
	
	
	
	/** Reports on the whether the heap identified by the argument heap value has content or not.
	*	@return true if the heap has content, false otherwise.
	*/
	public boolean hasContent(int heapID)
	{
		switch(heapID)
		{
			case HEAP:
				 return SALHeap != null;
				
			case ANIMATION_HEAP:
				 return animHeap != null;
				 
			case ADD_HEAP:
				 return addHeap != null;
				 
			case CHANGE_HEAP:
				 return changeHeap != null;
				 
			case DELETE_HEAP:
				 return deleteHeap != null;
							
			case HEAD_HEAP:
				 return SAL_LO_HeadHeap != null;
				
			case TAIL_HEAP:
				 return SAL_LO_TailHeap != null;
								
			default:
				System.err.println("SvgAnimationLanguage.hasContent(): non-existant namespace.");
				break;
		}
		return false;
	}
	
	/**	This method does a lookup for an animation target, and if found, returns it cast as a Vector.
	*	Valid target names are covered in the <A HREF="../../overview/sally.html">Sally</A> documentation 
	*	If the target can not be found, null is returned.
	*	@throws ClassCastException if the value stored on the heap is not a vector.
	*/
	public Vector getAnimation(String targetName)
	{
		if (animHeap != null)
			return (Vector)animHeap.getValue(targetName);
		
		return null;
	}

	/** Returns true if there are elements that are to have their attributes modified by a modify
	*	key word (add, change and or delete) in the config.d2s file.
	*/
	public boolean hasModifiedAttributes()
	{
		return hasModifiedContent;
	}

	/** Takes an Object and tests whether it is:
	*<OL>
	*	<LI> An SvgElement at all; it could also be an SvgAnimation object which is not related.
	*	<LI> An SvgElement that has a modification that uses the keyword 'implied'.
	*</OL>
	*	and then applies any modifier attributes. The SvgElement is 
	*	checked to see if there are any matching rules for this object and if there is, apply
	*	the attribute, if not leave the Object unchanged.
	*/
	public void modifyAttributes(Vector vSvges)
	{
		// Check to see if this collection actually is used.
		if (vSvges == null)
		{
			//System.out.println("Returning because vSvges is null.");
			return;
		}
		// Here we will iterate over all of the modifiers and determine if there are modifiers on 
		// any of the heaps and if there are we need apply them to the vector of elements and 
		// further sub elements of SvgCollections.
		Object o;
		Iterator it = vSvges.iterator();
		Object svgUnknownObject = null;
		// Here we will cycle through the list of SVGElements looking for any matches.
		// if any of these elements are SvgCollections then we need to search them as well;
		// That will be an inner loop.
		while (it.hasNext())
		{
			svgUnknownObject = it.next();
			modifyElement(svgUnknownObject, ADD_HEAP);
			modifyElement(svgUnknownObject, CHANGE_HEAP);
			modifyElement(svgUnknownObject, DELETE_HEAP);
		} 
	}
	
	/** This method modifies SvgElements with the attributes from the heap identified by heapType.
	*	SvgCollections can call this method recursively. You may not modify the attributes of an
	*	animation object without either making animation objects sub classes of SvgElement or 
	*	throwing in some code to handle the special requirements of SvgAnimation objects.
	*/
	protected void modifyElement(Object o, int heapType)
	{
		if (! hasModifiedAttributes(heapType))
		{
			return;
		}
		
		// This functionality doesn't work with SvgAnimation elements because you 
		// can use config.d2s with a greater degree of accuracy and flexibility.
		// Just ignore SvgAnimation elements.
		SvgElement svge = (SvgElement)o;
		
		// Depending on the heap type we will do the following
		switch (heapType)
		{
			case ADD_HEAP:
				// To get here we know we have to apply an 'add' action and we know
				// that we have a single SvgElement. We need to get all of the elements
				// off of the addHeap and apply them to this element.
				if (addHeap == null)
				{
					break;
				}
				Set heapKeys = addHeap.keySet();
				String[] keys = new String[addHeap.size()];
				heapKeys.toArray(keys);
				if (keys.length < 1)
				{
					System.out.println(" ADD Heap is empty*****");
					return;
				}
				

				// Iterate over the addHeap which lists the target names vs. a Hashtable
				// of modifying attributes.
				for (int i = 0; i < heapKeys.size(); i++)
				{
					// Test if this is a candidate for additional attributes.
					int attribMatchType = isCandidate(svge, keys[i]);
					
					// Get all the keys from the internal table and iterate 
					// over them all during which you should extract 
					Hashtable htAttribs = (Hashtable)addHeap.getValue(keys[i]);				
					if (svge instanceof SvgCollection)
					{
						if (svge instanceof SvgLayerGroup &&
						    attribMatchType == SALConsts.MATCH_LAYER_ONLY)
						{
							addAttribs(svge, htAttribs);
							return;
						}

						Vector vEles = ((SvgCollection)svge).getGroupElements();
						Iterator it = vEles.iterator();
						while (it.hasNext())
						{
							try
							{
								modifyElement((SvgElement)(it.next()), heapType);
							}
							catch (ClassCastException ce)
							{
								continue;
							}
						}
					}
					
					// This will be done specifically by a SvgLayerGroup (above).
					if (attribMatchType == SALConsts.MATCH)
					{
						addAttribs(svge, htAttribs);
					}
				} // end for 
				
				break;
			
			default:
				return;	// an unknown heap type will do nothing. 
				// This is redundant backup to hasModifiedAttributes() functionality.
		}  // end switch
	}
	
	///////////////////////////// 
	//   ROOT Element OnLy     //
	/////////////////////////////
	/** This method will apply any attributes that are destined for the root element.
	*	This is required because the &lt;SVG&gt; element is not like a normal collection
	*	as it would be if we used a real DOM. It is synchronized in the case that multipule 
	*	threads of a conversion can run.
	*/
	synchronized public void modifyAttributes(SvgBuilder svgb, int heapType)
	{
		if (! hasModifiedAttributes(heapType))
		{
			return;
		}

		// Depending on the heap type we will do the following
		switch (heapType)
		{
			case ADD_HEAP:
				// To get here we know we have to apply an 'add' action and we know
				// that we have a single SvgElement. We need to get all of the elements
				// off of the addHeap and apply them to this element.
				if (addHeap == null)
				{
					break;
				}
				Set heapKeys = addHeap.keySet();
				String[] keys = new String[addHeap.size()];
				heapKeys.toArray(keys);
				
				if (keys.length < 1)
				{
					System.out.println(" ADD Heap is empty*****");
					return;
				}
				

				// Iterate over the addHeap which lists the target names vs. a Hashtable
				// of modifying attributes.
				for (int i = 0; i < keys.length; i++)
				{
					// now break apart the attribute target string and match it if possible.
					String[] name = new String[3];
					name = keys[i].split(SALConsts.FULL_COLON);
					// Get all the keys from the internal table and iterate 
					// over them all during which you should extract 
					Hashtable htAttribs = (Hashtable)addHeap.getValue(keys[i]);					
					// We only id the root element not by ID or layer.
					if (! name[0].equals("null"))
					{
						if ("root".equalsIgnoreCase(name[0]) ||
							"svg".equalsIgnoreCase(name[0]))
						{
							addAttribs(svgb,htAttribs);
						}
						// does not support '*' option.
					}
				} // end for 
				
				break;
			
			default:
				return;	// an unknown heap type will do nothing. 
				// This is redundant backup to hasModifiedAttributes() functionality.
		}  // end switch		
	}
	///////////////////////////// 
	//   ROOT Element OnLy     //
	/////////////////////////////	
	
	
	// This object holds a value that idicates whether this SVG element is
	// a potential match. If a value of the attribute target string is null
	// it acts like a regex '*'. If the attribute string looks like this:
	// +--------------------+-------------+----------------+
	// |  Svg element Type  :  SvgObj ID  :  SvgObj layer  |
	// +--------------------+-------------+----------------+
	// |       line         :    null     :       "7"      |
	// +--------------------+-------------+----------------+
	// means that any <line> on layer '7' will match successfully.
	// +--------------------+-------------+----------------+
	// |  Svg element Type  :  SvgObj ID  :  SvgObj layer  |
	// +--------------------+-------------+----------------+
	// |       null         :    null     :   "english"    |
	// +--------------------+-------------+----------------+
	// In this case any object on the english layer will match. Comparing
	// the values of potential matches versus the values of actual matches
	// we find that if they are equal the match is successful. Any inequality
	// is treated as an unsuccessful match.	
	/** Tests the SvgElement argument to see if it matches the name specified in the 
	*	colon delimited arg string which looks like this:
	*	<P>
	<PRE>
+--------------------+-------------+----------------+
|     0              :      1      :        2       |
+--------------------+-------------+----------------+
|  Svg element Type  :  SvgObj ID  :  SvgObj layer  |
+--------------------+-------------+----------------+
	</PRE> 
	*	@param svge element to test.
	*	@param s String of attribute match targets (as above.)
	*<OL>
	*	<LI> No match = 0
	*	<LI> Matches the layer only = 1
	*	<LI> Match = 2
	*</OL>
	*/
	private int isCandidate(SvgElement svge, String s)
	{
		// We will use this to store the element name, the class or layer
		// and the id (if present).
		String[] name = new String[3];

		name = s.split(SALConsts.FULL_COLON);
		//System.out.println("Here is a comparison:");
		//System.out.println("attrib target string: "+s);
		//System.out.println("SVGE type: '"+svge.getType()+"' ");
		//System.out.print("' svge's getObjIDUU() = "+svge.getObjIDUU());
		//System.out.println(" getAbsoluteClass() = '"+svge.getAbsoluteClass()+"'");
		//System.out.println("Going to get the original layer: '"+svge.getOriginalLayer()+"'");
		// Keep track of the number of null matches cause 2 and a layer match means switch
		// position 1/2 or layer only attribute. If the null count + the match count = 3
		// then the match is successful.
		int matchCount 				= 0;
		int nullCount 				= 0;
		boolean isLayerOnly			= false;
		
		// If name[0] is the element type. If it is null it means any element,
		// it wasn't set so go onto the other tests.
		if (! name[0].equals("null"))
		{
			if (svge.getType().equalsIgnoreCase(name[0])) // these are case insensitive
			{
				matchCount++;
			}
			else if (name[0].equals("*")) // match all elements including layers and children elements.
			{
				matchCount++;
			}
		}

		else
		{
			nullCount++;
		}
		
		// Now check the id ...
		if (! name[1].equals("null"))
		{
			if (svge.getObjIDUU().equals(name[1]))  // These are case sensitive.
			{
				matchCount++;
			}		
			else if (name[1].equals("*")) // match all ids.
			{
				matchCount++;
			}
		}
		else
		{
			nullCount++;
		}
		
		// Now check the class. Here we will have to search through the class
		// because the class attribute may have multipule values stored in the
		// one attribute.
		if (! name[2].equals("null"))
		{
			if (svge.getOriginalLayer() != null && 
				svge.getOriginalLayer().equalsIgnoreCase(name[2])) // These are case sensitive too.
			{
				// Now test if any other attribute flag is set and if not the layer is 
				// the specific.
				if (nullCount == 2)
				{
					isLayerOnly = true;
				}
				else
				{
					matchCount++;
				}
			}
			else if (name[2].equals("*")) // match all ids - won't match layers. Layers don't have handles.
			{
				if (nullCount == 2)
				{
					isLayerOnly = true;
				}
				else
				{
					matchCount++;
				}
			}
		}
		else
		{
			nullCount++;
		}
		
		
		
		if (isLayerOnly)
		{
			return SALConsts.MATCH_LAYER_ONLY;
		}
		
		if ((matchCount + nullCount) == 3)
		{
			return SALConsts.MATCH;
		}
		
		return SALConsts.NO_MATCH;
	}
	
	
	/** This method add attributes for the root element only.
	*/
	private void addAttribs(SvgBuilder svgb, Hashtable attribs)
	{
		Set attribSet = attribs.keySet();
		
		Iterator attribIt = attribSet.iterator();
		
		while(attribIt.hasNext())
		{
			// Get the keys one-by-one and then the values then make an attribute
			// to pass to the successful modification element.
			String key = (String)(attribIt.next());
			String val = (String)(attribs.get(key));

			Attribute attrib = new Attribute(key,val);
			// If it is not an SvgText element then the attribute just gets added.
			//System.out.println("###"+attrib.toString());
			svgb.addAttribute(attrib);
		}  // end while				
	}
	
	
	/** This method iterates over the list of possible attribute matches, trying
	*	to find the attribute(s) that matches these elements.
	*/
	private void addAttribs(SvgElement svge, Hashtable attribs)
	{
		Set attribSet = attribs.keySet();
		
		Iterator attribIt = attribSet.iterator();
		
		while(attribIt.hasNext())
		{
			// Get the keys one-by-one and then the values then make an attribute
			// to pass to the successful modification element.
			String key = (String)(attribIt.next());
			String val = (String)(attribs.get(key));

			Attribute attrib = new Attribute(key,val);
			if (svge instanceof SvgText)
			{			
				//////////////////////////////////////////
				//   This is where we replace the key   //
				//  word 'implied' with svge's content  //
				//              if possible.            //
				//////////////////////////////////////////
				// Create a new Attribute to pass to the EventAttributeModifier
				// if this object is an SvgText object, it may require processing 
				// to grab the text content. If it is not then...
				EventAttributeModifier eAttribMod = 
					new EventAttributeModifier((SvgText)svge, attrib);
					
				attrib = eAttribMod.getAttribute();
			}
			// If it is not an SvgText element then the attribute just gets added.
			//System.out.println("###"+attrib.toString());
			svge.addAttribute(attrib);
		}  // end while		
	}
	
		
	
	
	
	
	
	
	
	/** This method returns true if the heap represented by arg heap has 
	*	content that 
	*	key word (add, change and or delete) in the config.d2s file.
	*/
	protected boolean hasModifiedAttributes(int whichHeap)
	{
		switch(whichHeap)
		{
			case ADD_HEAP:
				if (addHeap != null)
				{
					if (addHeap.size() > 0)
					{
						return true;
					}
				}
				break;
				
			case CHANGE_HEAP:
				if (changeHeap != null)
				{
					if(changeHeap.size() > 0)
					{
						return true;
					}
				}
				break;
				
			case DELETE_HEAP:
				if (deleteHeap != null)
				{
					if (deleteHeap.size() > 0)
					{
						return true;
					}
				}
				break;
				
			default:
				break;
		}
		return false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//							Internal Classes
	//////////////////////////////////////////////////////////////////////////////////////////////
	/** This class stores additional attributes and their values or content that are intended for 
	*	inclusion on a specific element. The syntax for the list definition is one of the following:
	*	<P>
	*	<pre>
	+---------------+----------+------------+
	|    keyword    |  target  | target ID  |
	+---------------+-----+----+------------+
	|     :add      |  "text"  | "text_ID"  |
	+---------------+----------+------------+
	</pre>
	*/
	public class ModifyHeap extends Heap
	{
	
		// In this class the internal heap will store three other heaps inside it. The three
		// internal heaps correspond to the three types of modifiers: add, change and delete.
		// We have one from the parent class called heap, now let's make two more.
		
		// Constructor
		protected ModifyHeap()
		{	}
		
		/** Adds a symbol table to the modify heap. Modify heap is based on a Hashtable so
		*	entering a key value that already exists will overwrite any previously existing
		*	value with the same name.
		*	@param type not used.
		*	@param tableName key name to store symboltable against
		*	@param st table of variables and their values gleened from the config.d2s.
		*/
		protected void addObjectToHeap(int type, String tableName, SymbolTable st)
		{
			// the symbol table is a list of attributes and values that need to 
			// be added to an element called tableName. We will unpack the symbol
			// table to a new hashtable and place it as a value for the key tableName
			// on 'heap'.
			if (tableName == null || tableName.equals(""))
			{
				System.err.println("Warning: ModifyHeap was told to add a symbol table to the heap "+
					"but the key tableName was null. Returning without fufilling the operation.");
				return;
			}
			
			if (st == null)
			{
				System.err.println("Warning: ModifyHeap was told to add a symbol table to the heap "+
					"but the symbol table was null. Returning without fufilling the operation.");
				return;				
			}
			Hashtable ht = new Hashtable();
			unpackSymbolTable(st, ht);
			heap.put(tableName, ht);
			hasModifiedContent = true;
		}
			
	} // end ModifyHeap class
	
	
	
	
	
	/** This is the class that you must subclass to create a new heap. 
	*	AnimationHeap and ModifyHeap both used this class as their template.
	*/
	public abstract class Heap
	{
		// This is the place where all of the animation objects will be stored by name.
		// The name is the target. The value stored is Vector of animation objects.
		protected Hashtable heap;
		
		// Constructor
		protected Heap()
		{
			heap = new Hashtable();
		}
		
		/** Adds a symbol table to the heap.
		*/
		protected abstract void addObjectToHeap(int type, String tableName, SymbolTable st);
		
		/** Returns the size of the animation heap
		*/
		protected int size()
		{
			return heap.size();
		}
		
		/** This method will return the list of keys to items currently stored on the heap.
		*/
		protected Set keySet()
		{
			return heap.keySet();
		}
		
		/** This method will unpack a SymbolTable into the argument Hashtable. By passing
		*	the Hashtable as an argument we can allow for unpacking of symbol tables to 
		*	any heap or multipule heaps.
		*	@param sTable to be unpacked
		*	@param hTable Destination to place the data from the symboltable.
		*/
		protected boolean unpackSymbolTable(SymbolTable sTable, Hashtable hTable)
		{
			if (sTable == null || hTable == null)
			{
				return false;
			}
			// Get the list of variables on the symbol table.
			Vector vl = new Vector();
			vl = sTable.list();
			// make an iterator to iterate over the list of variables.
			Iterator it = vl.iterator();
			String name = null;
			Object value = null;
			
			// now go over the vector and collect the variables values
			while (it.hasNext())
			{	
				name = (String)it.next();
				// Query the symbol table for the variable value.
				/////////////////////////////////////////////////////////////////////
				// We have to fix this for single item lists. They don't have values.
				/////////////////////////////////////////////////////////////////////
				value = sTable.getSymbolValue(name);
				// If this is a is a single item list fill the value with something.
				if (value == null)
				{
					value = new Boolean("true");
				}
				// anything stored in the table will be placed unaltered onto
				// the hashtable heap it also will over-write any object that
				// was previously stored there if the name key already exists.
				hTable.put(name, value);
			} // end while
			
			if (DxfPreprocessor.debugMode() == true)
			{
				System.out.println("The animation heap contains: >>"+hTable+"<<");
			}
			
			return true;
		}
		
		/** This method will return the requested value. It should be noted
		*	that the return value is an Object but all stored values are Vectors
		*	of SvgAnimator or modifier objects.
		*/
		protected Object getValue(String key)
		{
			return heap.get(key);
		}
		
		public String toString()
		{
			return this.getClass().getName()+":"+heap.toString();
		}
	}  // end of Heap class.
	
	
	
	
	/** This class stores animation objects. Symbol tables, gleened from the config.d2s file, are
	*	unpacked and SvgAnimation objects are created and stored in a Vectors that are, in turn,
	*	stored in a searchable Hashtable. This allows multipule rules for one target. 
	*	That is to say, if you have a target like 'wires' then
	*	you could apply a rule that set it's opacity attrib to some value and another rule to set
	*	it's opacity rule to something else. Multipule rules for one target is the motivation for
	*	this object.
	*/
	public class AnimationHeap extends Heap
	{
		private SvgAnimator animationFactory;
		
		protected AnimationHeap()
		{
			animationFactory = new SvgAnimator();
		}
		
		//This method does two things; one, it de-frocks the style table and makes the
		// appropriate SvgAnimator object, and two, it adds the animation object to the
		// appropriate vector if one exists and if not make one.
		/** Creates an SvgAnimator object from the argument symbol table and places it
		*	on the heap.
		*	@throws NumberFormatException if an 'accumulate' or 'additive' value exceeds a byte
		*	value.
		*	@param type Integer enumerated value of the animation type of table expected.
		*	@param tableName String name of the table; the animation or modify target name.
		*	@param st the variables and values stored that relate to that target.
		*/
		protected void addObjectToHeap(int type, String tableName, SymbolTable st)
		{
			// Get the list of variables on the symbol table.
			Vector vl = new Vector();
			vl = st.list();
			// make an iterator to iterate over the list of variables.
			Iterator it = vl.iterator();
			String name = null;
			String value = null;
			// create animation object.
			SvgAnimator animation = animationFactory.getInstanceOf(type);
			
			// now go over the vector and collect the variables values
			while (it.hasNext())
			{
				name = (String)it.next();
				// Query the symbol table for the variable value.
				value = (String)st.getSymbolValue(name);
				
				// Now we have to determine which keyword it is so we can create an 
				// animation object calling the correct methods.
				// Here are all the common methods...
				if (name.equalsIgnoreCase("attributeName"))
					animation.setAttributeToAnimate(value);
				// This doesn't get passed to the SvgAnimator because there could
				// be multipule instances of animation objects that all work on 
				// the same collaborator switch.
				else if (name.equalsIgnoreCase("collaborate"))
					DxfPreprocessor.registerCollaboratorTarget(tableName, value);
				else if (name.equalsIgnoreCase("gang"))
					DxfPreprocessor.registerGangTarget(tableName, value);
				else if (name.equalsIgnoreCase("attributeType"))
					animation.setAttributeType(value);				
				else if (name.equalsIgnoreCase("to"))
					animation.setTo(value);
				else if (name.equalsIgnoreCase("begin"))
					animation.setBegin(value);
				else if (name.equalsIgnoreCase("attributeType"))
					animation.setAttributeType(value);
				else if (name.equalsIgnoreCase("by"))
					animation.setBy(value);
				else if (name.equalsIgnoreCase("calcMode"))
					animation.setCalcMode(value);
				else if (name.equalsIgnoreCase("dur"))
					animation.setDur(value);
				else if (name.equalsIgnoreCase("end"))
					animation.setEnd(value);
				else if (name.equalsIgnoreCase("fill"))
					animation.setFill(value);
				else if (name.equalsIgnoreCase("from"))
					animation.setFrom(value);
				else if (name.equalsIgnoreCase("keySplines"))
					animation.setKeySplines(value);
				else if (name.equalsIgnoreCase("keyTimes"))
					animation.setKeyTimes(value);
				else if (name.equalsIgnoreCase("repeatCount"))
					animation.setRepeatCount(value);
				else if (name.equalsIgnoreCase("repeatDur"))
					animation.setRepeatDur(value);
				else if (name.equalsIgnoreCase("restart"))
					animation.setRestart(value);
				else if (name.equalsIgnoreCase("rotate"))
					animation.setRotate(value);
				else if (name.equalsIgnoreCase("to"))
					animation.setTo(value);
				else if (name.equalsIgnoreCase("values"))
					animation.setValues(value);
				else if (name.equalsIgnoreCase("path"))
					animation.setPath(value);
				else if (name.equalsIgnoreCase("origin"))
					animation.setOrigin(value);
				else if (name.equalsIgnoreCase("accumulate"))
					animation.setAttributeType(value);
				else if (name.equalsIgnoreCase("additive"))
					animation.setAdditive(value);
				else if (name.equalsIgnoreCase("transformType"))
					animation.setTransformType(value);
				else
					System.err.println("AnimationHeap: ignoring unknown request, "+name);
					
			}	// end while
			
			
			// If there is an animation target already stored on the hashtable we will
			// add this animation object to the vector of other animation objects for
			// this target.
			if (heap.containsKey(tableName))
			{
				Vector record = (Vector)heap.get(tableName);
				record.add(animation);
			}
			else	// does not contain key so make a new entry in the hashtable to accomodate
					// the one or more animations.
			{
				Vector record = new Vector();
				record.add(animation);
				heap.put(tableName, record);
			}
			
			if (DxfPreprocessor.debugMode() == true)
			{
				System.out.println("The animation heap contains: >>"+heap+"<<");
			}
		} // end addObjectToHeap()
		
	}	// end internal class AnimationHeap
	
}	// end of SvgAnimationLanguage