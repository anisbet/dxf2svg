
/****************************************************************************
**
**	FileName:	Parser.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Parses tokens from the config file and applies the rules it
**				finds there.
**
**	Date:		May 27, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.1 - May 27, 2003
**				0.3 - April 2, 2004 Added modifying keywords :add :delete :change
**				and content.
**				1.0 - January 5, 2005 Filled in curly braces for else statements 
**				to improve clearity.
**				1.1 - January 11, 2005 Added new keyword of textStyle as a 
**				two-item list.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.sally;

import dxf2svg.util.Pen;
import dxf2svg.util.CustomLayerStyle;
import dxf2svg.util.FontMapElement;


/////////////////////////////////////////////////////////////////////////////
//					Special Development Instructions
/////////////////////////////////////////////////////////////////////////////
// If you wish to create a new list type:
// 1) make sure the list type doesn't already exist.
// 2) make the appropriate entries for the list in SALConsts.java
// 3) decide if the list type needs to be a 1, 2 or 3 entry list.
// 4) make allowances for the list type in parseToken() method under case statement
// of LABEL.
// 5) make an entry for it in the setListType() method.
// 6) Create a symbol table for your data to be stored on in method "parseToken() 
//    case SALConsts.S_LABEL:".
// 7) Test the hell out of it.
// 8) Make necessary changes to the config.d2s syntax highlighting file.


/**	Parser parses the configuration file and applies the rules that it finds there.
*	The following is a set of rules that the parser knows about.
*<UL>
*	<LI> Layer ordering list.
*	<LI> Files to process.
*	<LI> Animation instructions.
*	<LI> Font map list.
*</UL>
*	<P>
*	<PRE>
*	The SAL Grammar:
*	================
*
*	Label:
*		':' list_type file_list
*		|	layer_order_list
*		|	fontmap_list
*		|	animation_instructions
*		|	variable_list
*		|	pen_table
*		|	layer_style
*		|	textStyle
*		;
*
*	list_type:	STRING
*		;
*
*	variable_list:	instruction_list
*		;
*
*	file_list:
*		|	'{' STRING ';' [STRING ...] '}'
*		;
*
*	layer_order_list: "tail"
*		|	"head" file_list
*		;
*
*	pen_table: three_item_list
*		;
*
*	layer_style: three_item_list
*		;
*	// The last string, after the comma is a string version of a double value of the
*	// scale of font. If no scaling is required leave it blank and the default value
*	// of 1.0 will be used.
*	three_item_list:
*		|	'{' STRING '=' STRING [',' STRING] ';' [repeated ...] '}'
*		;
*
*	animation_instructions:	animation_type animation_target instruction_list [repeated ...]
*		;
*
*	animation_target:STRING
*		;
*
*	animation_type:	STRING //"set" "animate" "animateColour" "animateMotion" "animateTransform"
*		;
*
*	instruction_list:	'{' STRING '=' STRING ';' [repeat ...] '}'
*		;
*	</PRE>
*	@version	0.1 - May 27, 2003
*	@author		Andrew Nisbet
*/
public class Parser
{
	/////////////////////////////////////////////////////////////////
	//				 		Instance data
	/////////////////////////////////////////////////////////////////
	private int state;					// Current parser state.
	protected SvgAnimationLanguage sal;	// parent object, tokenizer.
	private int[] stack;				// stack of past states
	private int listType;				// type of list we are parsing
	private boolean ignoreTilNextLabel;	// will ignore all instructions until next label
	private int numItemsPerEntry;		// Valid number of list items in an entry
	// The state's symbol table objects.
	private String currTableName;
	private SymbolTable currSymbolTable;
	private Symbol symbolFactory;
	// Entries for the list.
	private String leftValue;			// Final left hand value; used in all list types.
	private String rightValue;			// Final right hand value
	private String thirdValue;			// Final third value. 
	private FontMapElement fontMap;
	private int threeValueListType;		// Type of list either fontmap or pen table or customlayer.
	// These values are packed onto the symbol table
	private int lOrderType;				// either head or tail
	private int animationType;			// This is set or animate or animationMotion etc.
	private String[] modTarget;
	
	

	public Parser(SvgAnimationLanguage sal)
	{
		this.sal 	= sal;
		stack = new int[4];
		// at the beginning of the parse the state is S_INIT
		push(SALConsts.S_INIT);
		symbolFactory = new Symbol();
		thirdValue  = "1.0";
		threeValueListType = SALConsts.S_INIT;
	}

	/////////////////////////////////////////////////////////////////
	//				 		Methods
	/////////////////////////////////////////////////////////////////
	/**	Parses all the tokens from the tokenizer (SvgAnimationLanguage)
	*	object.
	*/
	public void parseToken(String token)
	{
		// Here we set the context for the current statement.
		//System.out.println(">>>Token retrieved: '"+token+"'");
		
		if (token.equals(SALConsts.SEMI_COLON))
		{	state = SALConsts.S_SEMI_COLON;		}
		else if(token.equals(SALConsts.FULL_COLON))
		{
			state = SALConsts.S_FULL_COLON;
			ignoreTilNextLabel = false;
		}
		else if(token.equals(SALConsts.OPEN_CBRACE))
		{	state = SALConsts.S_OPEN_CBRACE;	}
		else if(token.equals(SALConsts.CLOSE_CBRACE))
		{	state = SALConsts.S_CLOSE_CBRACE;	}
		else if(token.equals(SALConsts.EQUALS))
		{	state = SALConsts.S_EQUALS;			}
		else if(token.equals(SALConsts.COMMA))
		{	state = SALConsts.S_COMMA;			}
		else if(token.equals(SALConsts.L_FILES))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_FILES);
		}
		else if(token.equals(SALConsts.L_JAVASCRIPT))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_JAVASCRIPT);
		}
		else if(token.equals(SALConsts.L_FONTMAP))
		{
			state = SALConsts.S_LABEL;
			threeValueListType = SALConsts.S_L_FONTMAP;
			setListType(SALConsts.S_L_FONTMAP);
		}
		else if(token.equals(SALConsts.L_ANIMATION))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_ANIMATION);
		}
		else if(token.equals(SALConsts.L_LAYER_ORDER))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_LAYER_ORDER);
		}
		else if(token.equals(SALConsts.L_VARIABLES))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_VARIABLES);
		}
		else if(token.equals(SALConsts.L_PENS))
		{
			state = SALConsts.S_LABEL;
			threeValueListType = SALConsts.S_L_PENS;
			setListType(SALConsts.S_L_PENS);
		}
		else if(token.equals(SALConsts.L_CUSTOM_LAYERS))
		{
			state = SALConsts.S_LABEL;
			threeValueListType = SALConsts.S_L_CUSTOM_LAYERS;
			setListType(SALConsts.S_L_CUSTOM_LAYERS);
		}
		else if(token.equals(SALConsts.L_CUSTOM_TEXTSTYLE))
		{
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_L_CUSTOM_TEXT_STYLE);
		}
		else if(token.equals(SALConsts.A_SET))
		{	state = SALConsts.S_A_KEYWORD;		}
		else if(token.equals(SALConsts.A_ANIMATE))
		{	state = SALConsts.S_A_KEYWORD;		}
		else if(token.equals(SALConsts.A_COLOUR))
		{	state = SALConsts.S_A_KEYWORD;		}
		else if(token.equals(SALConsts.A_MOTION))
		{	state = SALConsts.S_A_KEYWORD;		}
		else if(token.equals(SALConsts.A_TRANSFORM))
		{	state = SALConsts.S_A_KEYWORD;		}
		else if(token.equals(SALConsts.L_ADD))
		{
			// :add (label state).
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_MOD_ADD);
			modTarget = new String[SALConsts.MOD_TARGET_LEN];
		}
		else if(token.equals(SALConsts.L_CHANGE))
		{
			// :change (label state).
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_MOD_CHANGE);
			modTarget = new String[SALConsts.MOD_TARGET_LEN];
		}
		else if(token.equals(SALConsts.L_DELETE))
		{
			// :delete (label state).
			state = SALConsts.S_LABEL;
			setListType(SALConsts.S_MOD_DELETE);
			modTarget = new String[SALConsts.MOD_TARGET_LEN];
		}
		else if (token.equals(SALConsts.L_WITH_ID))
		{
			state = SALConsts.S_MOD_TARGET_ID;
		}
		else if (token.equals(SALConsts.L_ON_LAYER))
		{
			state = SALConsts.S_MOD_TARGET_CLASS;
		}
		else
		{
			// We set the state to general string. Now if this value doesn't get set to
			// something else it will cause a general error because this application
			// should know what all strings it encounters are for.
			state = SALConsts.S_GENERAL_STRING;
			// now qualify the type of general string based on it's context, if possible.
			// If the previous state was a full colon then this is a label but not a
			// predefined label so we set an empty label and turn on a switch to
			// skip the rest of the content of the label.
			if (stack[0] == SALConsts.S_FULL_COLON)
			{
				// user was attempting to create a label but either spelled it wrong
				// or we haven't implemented that functionality yet.
				System.out.println("Parser warning: Ignoring unknown label '"+token+"'.");
				state = SALConsts.S_EMPTY_LABEL;
			}
			// is the last state an animation keyword if so this general string is a animation target
			// syntax:	label		type 	target	list
			//			:animation 	set 	solid	{ ... = ... ; }
			else if (stack[0] == SALConsts.S_A_KEYWORD)
			{
				//animation target is set here.
				state = SALConsts.S_A_TARGET;
			}
			else if (stack[0] == SALConsts.S_EQUALS)
			{
				// This is a right hand value
				state = SALConsts.S_RIGHT_HAND_VALUE;
			}
			else if (stack[0] == SALConsts.S_COMMA)
			{
				// This is a right hand value
				state = SALConsts.S_THIRD_ITEM_VALUE;
			}
			else if (stack[0] == SALConsts.S_OPEN_CBRACE ||
					 stack[0] == SALConsts.S_SEMI_COLON)
			{
				// This is a left hand value
				state = SALConsts.S_LEFT_HAND_VALUE;
			}
			// is the modify state which is the most complex parsing of all.
			// The modify key word takes between one and two arguments and a list
			// which can be either a two or three item list depending on the context
			// In the case below, if the optional id value was ommitted, all of the
			// text elements would have an onclick event added, a change to any stroke
			// value (changed to green) and a stroke-width attribute removed if one is present.
			// Some of these functions will not be implemented immediately.
			//
			// syntax:  : S_L_modify  S_MOD_TARGET  S_MOD_TARGET_ID  S_MOD_TARGET_ID_VALUE  S_MOD_TARGET_CLASS  S_MOD_TARGET_CLASS_VALUE{
			//				"left" = "right"; }
			//          ===========================================
			//			:add  text withid "id_value_optional" onlayer "english" {
			//				'onclick' = 'clickFunc(args)';
			//			}
			else if (stack[0] == SALConsts.S_MOD_ADD ||
					 stack[0] == SALConsts.S_MOD_CHANGE ||
					 stack[0] == SALConsts.S_MOD_DELETE)
			{
				//modification target is set here.
				state = SALConsts.S_MOD_TARGET;
			}
			else if (stack[0] == SALConsts.S_MOD_TARGET_ID)
			{
				state = SALConsts.S_MOD_TARGET_ID_VALUE;
			}
			else if (stack[0] == SALConsts.S_MOD_TARGET_CLASS)
			{
				state = SALConsts.S_MOD_TARGET_CLASS_VALUE;
			}	
			// This should be the last test as it is currently the most general rule.
			else if (stack[0] == SALConsts.S_LABEL)
			{
				// then this could be the key word tail or head or modify which we will need.
				if (token.equals(SALConsts.K_TAIL) || token.equals(SALConsts.K_HEAD))
				{
					state = SALConsts.S_LO_KEYWORD;
				}
				else 
				{
					if (listType == SALConsts.S_L_LAYER_ORDER)
						throw new SyntaxError("expected 'tail' or 'head' but got '"+token+"'");
					else
						throw new SyntaxError("illegal string value");
				}
			}
		}
		// with this we can skip until all the enclosing text within an unknown label
		// type until we see the start of another label.
		if (ignoreTilNextLabel == true)
			return;
		push(state);
		int lastState = stack[1];
		int prevState = stack[2];
		//System.out.println("lets peek at the stack: ");
		//printStack();




		///////////////////////////////////////////////////////////////////////////////////
		// This switch will determine if there are any errors depending on the context of
		// the token and the context it was found in.
		switch(state)
		{
			case SALConsts.S_THIRD_ITEM_VALUE:
				if (numItemsPerEntry != SALConsts.S_THREE_ITEM_ENTRY)
					throw new SyntaxError("unexpected value not allowed in this type of list");
				// This is where the fontMap list gets it's scale value.
				//System.out.println("passing off optional third value of: '"+token+"'.");
				thirdValue = token;
				break;

			case SALConsts.S_SEMI_COLON:
				if (isPunctuationMark(lastState))
					throw new SyntaxError("illegal punctuation");
				// here we catch too few entries for the list type.
				if ((prevState == SALConsts.S_SEMI_COLON ||
					prevState == SALConsts.S_OPEN_CBRACE) &&
					numItemsPerEntry >= SALConsts.S_TWO_ITEM_ENTRY)
					throw new SyntaxError("not enough entries for list type");
				// line terminator, but not an error, just an empty statement.
				// others will check to see if they are in error in relation
				// to this context though.
				if (numItemsPerEntry == SALConsts.S_ONE_ITEM_UNSORTED)
				{
					if (currSymbolTable == null)
						throw new InternalError();
					currSymbolTable.insert(symbolFactory.getInstanceOf(leftValue));
				}
				else if (numItemsPerEntry == SALConsts.S_ONE_ITEM_ENTRY)
				{
					if (currSymbolTable == null)
						throw new InternalError();
					currSymbolTable.insert(symbolFactory.getInstanceOf(leftValue, "<none>"));
				}
				else if (numItemsPerEntry == SALConsts.S_TWO_ITEM_ENTRY)
				{
					if (currSymbolTable == null)
						throw new InternalError();
					currSymbolTable.insert(symbolFactory.getInstanceOf(leftValue, rightValue));					
				}
				else if (numItemsPerEntry == SALConsts.S_THREE_ITEM_ENTRY)// three item list.
				{
					if (currSymbolTable == null)
						throw new InternalError();
					// The only three item list to date is the fontmap list.
					// Now we also have a pen table.
					
					double d = Double.parseDouble(thirdValue);
					if (threeValueListType == SALConsts.S_L_FONTMAP)
					{
						fontMap = new FontMapElement(rightValue, d);
						currSymbolTable.insert(symbolFactory.getInstanceOf(leftValue, fontMap));
					}
					else if (threeValueListType == SALConsts.S_L_PENS)
					{
						// convert the right value to a int
						int penColour = 0;
						try
						{
							double tmpD = Double.parseDouble(rightValue);
							penColour = (int)tmpD;
						}
						catch (NumberFormatException e)
						{
							throw new NumberFormatException(
								"Parser error: invalid pen colour number. Expected a colour number.");
						}
						Pen pen = null;
						// see if the third value, line weight, has been set.
						if (thirdValue.equals("1.0"))
						{
							pen = new Pen(penColour);
						}
						else
						{
							pen = new Pen(penColour, d);
						}
						// lets convert the leftValue (which is a number and will by default
						// be a double) to an integer.
						double dName;
						
						try
						{
							dName = Double.parseDouble(leftValue);
							
						}
						catch (NumberFormatException e)
						{
							throw new NumberFormatException(
								"Parser error: invalid pen number. Expected a numeric value.");
						}
						int iName = (int)dName;
						// Now add the pen to the SymbolTable.
						currSymbolTable.insert(symbolFactory.getInstanceOf(
								String.valueOf(iName), pen
							)
						);
					}
					else if (threeValueListType == SALConsts.S_L_CUSTOM_LAYERS)
					{
						int fill = 0;
						// convert the right value to an int
						int penNumber = 0;
						try
						{ 
							double tmpD = Double.parseDouble(rightValue);
							penNumber = (int)tmpD;
						}
						catch (NumberFormatException e)
						{
							throw new NumberFormatException(
								"Parser error: customLayerStyle, invalid pen number.");
						}
						CustomLayerStyle cls = null;
						if (thirdValue.equals("1.0"))	// Historical default value for third values.
						{
							cls = new CustomLayerStyle(leftValue,penNumber);
						}
						else
						{						
							fill = (int)d;
							//System.out.print(" ***>"+leftValue);
							//System.out.print("***>"+penNumber);
							//System.out.println("***>"+fill);
							cls = new CustomLayerStyle(leftValue,penNumber,fill);
						}
						currSymbolTable.insert(symbolFactory.getInstanceOf(leftValue, cls));						
					}
				}
				// reset the value of the third element to 1.0 so we can test to see if 
				// got set the next time around. We do this because the third item on the
				// is optional on the only three item list object: FontMap (and penTable).
				thirdValue = "1.0";
				break;

			case SALConsts.S_RIGHT_HAND_VALUE:
				if (numItemsPerEntry < SALConsts.S_TWO_ITEM_ENTRY)
				{
					throw new SyntaxError("unexpected token received about or");
				}
				//System.out.println("passing off right hand value of: '"+token+"'.");
				rightValue = token;
				break;

			case SALConsts.S_LEFT_HAND_VALUE:
				//System.out.println("passing off left hand value of: '"+token+"'.");
				leftValue = token;
				break;

			case SALConsts.S_EQUALS:
				if (isPunctuationMark(lastState))
					throw new SyntaxError("illegal punctuation");
				if (lastState != SALConsts.S_LEFT_HAND_VALUE)
					throw new SyntaxError("'=' not allowed or there may be an unknown character about or");
				break;

			case SALConsts.S_COMMA:
				if (isPunctuationMark(lastState))
					throw new SyntaxError("illegal punctuation");
				if (numItemsPerEntry < SALConsts.S_THREE_ITEM_ENTRY)
					throw new SyntaxError("',' not allowed");
				break;

			case SALConsts.S_FULL_COLON:
				if (lastState != SALConsts.S_CLOSE_CBRACE &&
					lastState != SALConsts.S_INIT)
					throw new SyntaxError("illegal punctuation");
				switch(lastState)
				{
					// if we got a '}' then see if the state before was a ';'
					// this would have signalled the end of a previous set of instructions.
					case SALConsts.S_CLOSE_CBRACE:
						if (prevState != SALConsts.S_SEMI_COLON)
							throw new SyntaxError("LABEL requested, but previous list not complete");
					case SALConsts.S_INIT:
						break;
					default:
						throw new SyntaxError("malformed LABEL instruction");
				}
				break;

			////////////////////////////// Symbol Table Creation ////////////////////////////
			case SALConsts.S_LABEL:
				//System.out.println(">>> our last state was: "+lastState);
				switch(lastState)
				{

					case SALConsts.S_FULL_COLON:
						if (token.equals(SALConsts.L_FILES))
						{
							//System.out.println("Creating a file list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_FILES;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_FONTMAP))
						{
							//System.out.println("Creating a font map list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_FONTMAP;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_ANIMATION))
						{
							//System.out.println("Creating an animation list object.");
							// For animation we don't use the 'animation' keyword
							// because there may be many animation tables created.
							// We set the title of the table to the target name.
							// In that way if the user redefines a target, the old 
							// symbol table will be over-written.
							// The down side is we don't know the target name until 
							// a little later in the process (hopefully within the 
							// next two tokens).
							//
							// We will reset this value later when we get a general string.
							currTableName = SALConsts.L_ANIMATION;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_LAYER_ORDER))
						{
							//System.out.println("Creating an layer ordered list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_LAYER_ORDER;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_VARIABLES))
						{
							//System.out.println("Creating an switch setup list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_VARIABLES;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_JAVASCRIPT))
						{
							//System.out.println("Creating a JavaScript list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_JAVASCRIPT;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_PENS))
						{
							//System.out.println("Creating an switch setup list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_PENS;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_CUSTOM_LAYERS))
						{
							//System.out.println("Creating an switch setup list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_CUSTOM_LAYERS;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_CUSTOM_TEXTSTYLE))
						{
							System.out.println("Creating custom text style list object.");
							// create a new symbol table for all the variables we find for 
							// this list.
							currTableName = SALConsts.L_CUSTOM_TEXTSTYLE;
		 					currSymbolTable = new SymbolTable();
						}
						else if (token.equals(SALConsts.L_ADD))
						{
							// Unlike any other label modify can take either one or two 
							// additional strings that can be any string value whatever.
							// Because the parser will find the next token to be a general
							// string we need to alert it to the fact that this is a significant
							// piece of string that will not match any tests. To do this 
							// we change the state of the parser to a S_L_MODIFY state and 
							// push that value so when time comes to test the value, it 
							// set the currTableName to this value PLUS any addition argument
							// pasted together with a '.' like this: 'text.layer1'.
							// We will reset this value later when we get a general string.
							currTableName = SALConsts.L_ADD;
							state = SALConsts.S_MOD_ADD;
							push(state);
		 					currSymbolTable = new SymbolTable();							
						}
						else if (token.equals(SALConsts.L_CHANGE))
						{
							// Unlike any other label modify can take either one or two 
							// additional strings that can be any string value whatever.
							// Because the parser will find the next token to be a general
							// string we need to alert it to the fact that this is a significant
							// piece of string that will not match any tests. To do this 
							// we change the state of the parser to a S_L_MODIFY state and 
							// push that value so when time comes to test the value, it 
							// set the currTableName to this value PLUS any addition argument
							// pasted together with a '.' like this: 'text.layer1'.
							// We will reset this value later when we get a general string.
							currTableName = SALConsts.L_CHANGE;
							state = SALConsts.S_MOD_CHANGE;
							push(state);
		 					currSymbolTable = new SymbolTable();							
						}
						else if (token.equals(SALConsts.L_DELETE))
						{
							// Unlike any other label modify can take either one or two 
							// additional strings that can be any string value whatever.
							// Because the parser will find the next token to be a general
							// string we need to alert it to the fact that this is a significant
							// piece of string that will not match any tests. To do this 
							// we change the state of the parser to a S_L_MODIFY state and 
							// push that value so when time comes to test the value, it 
							// set the currTableName to this value PLUS any addition argument
							// pasted together with a '.' like this: 'text.layer1'.
							// We will reset this value later when we get a general string.
							currTableName = SALConsts.L_DELETE;
							state = SALConsts.S_MOD_DELETE;
							push(state);
		 					currSymbolTable = new SymbolTable();							
						}
						break;	// end case

					default:
						throw new SyntaxError("invalid label location");
				}
				break;

			// Ignore unknown labels.
			case SALConsts.S_EMPTY_LABEL:
				pop();
				pop();
				// Signal to parser to ignore until next label is found.
				ignoreTilNextLabel = true;
				break;

			// what to do when you open a list.
			// see if it is appropriate ie was the last state a label or an animation or 
			// modification target.
			case SALConsts.S_OPEN_CBRACE:
				if (isPunctuationMark(lastState))
					throw new SyntaxError("illegal punctuation");
				if (lastState != SALConsts.S_LABEL &&
					lastState != SALConsts.S_A_TARGET &&
					lastState != SALConsts.S_MOD_TARGET &&
					lastState != SALConsts.S_MOD_TARGET_ID_VALUE &&
					lastState != SALConsts.S_MOD_TARGET_CLASS_VALUE &&
					lastState != SALConsts.S_LO_KEYWORD
					)
				{
					// if the last state wasn't an animation target...
					throw new SyntaxError("invalid start of list");
				}
				break;

			case SALConsts.S_CLOSE_CBRACE:
				if (lastState != SALConsts.S_SEMI_COLON)
				{
					// the list was not finished correctly.
					throw new SyntaxError("missing ';' near or");
				}
				if (listType == SALConsts.S_L_LAYER_ORDER)
				{
					sal.addSymbolTable(lOrderType, currTableName, currSymbolTable);
				}
				else if (listType == SALConsts.S_L_ANIMATION)
				{
					sal.addSymbolTable(animationType, currTableName, currSymbolTable);
				}
				else if (listType == SALConsts.S_MOD_ADD)
				{
					sal.addSymbolTable(SALConsts.S_MOD_ADD, currTableName, currSymbolTable);
					//System.out.println("adding an add symbol table to SAL.");
				}
				else if (listType == SALConsts.S_MOD_CHANGE)
				{
					sal.addSymbolTable(SALConsts.S_MOD_CHANGE, currTableName, currSymbolTable);
					System.out.println("adding a change table symbol table to SAL.");
				}
				else if (listType == SALConsts.S_MOD_DELETE)
				{
					sal.addSymbolTable(SALConsts.S_MOD_DELETE, currTableName, currSymbolTable);
					System.out.println("adding a delete symbol table to SAL.");
				}
				else
				{
					// This value just indicates that the passed list is not a 
					// animation or layerorder list.
					sal.addSymbolTable(SALConsts.S_L_GENERAL, currTableName, currSymbolTable);
				}
				// reset the list type
				setListType(SALConsts.S_INIT);
				break;

			// the current state is animation target name general string.
			case SALConsts.S_A_TARGET:
				if (lastState != SALConsts.S_A_KEYWORD)
				{
					throw new SyntaxError("missing animation type");
				}
				// Now we can set the name of the symbol table for all of the 
				// variables related to this particular animation request.
				currTableName = token;
				//System.out.println("setting the animation target to: '"+token+"'");
				break;

			case SALConsts.S_A_KEYWORD:
				if (token.equals(SALConsts.A_SET))
				{
					//System.out.println("creating SET animation object.");
					animationType = SALConsts.S_A_SET;
				}
				else if (token.equals(SALConsts.A_ANIMATE))
				{
					//System.out.println("creating ANIMATE animation object.");
					animationType = SALConsts.S_A_ANIMATE;
				}
				else if (token.equals(SALConsts.A_COLOUR))
				{
					//System.out.println("creating COLOUR animation object.");
					animationType = SALConsts.S_A_COLOUR;
				}
				else if (token.equals(SALConsts.A_MOTION))
				{
					//System.out.println("creating MOTION animation object.");
					animationType = SALConsts.S_A_MOTION;
				}
				else if (token.equals(SALConsts.A_TRANSFORM))
				{
					//System.out.println("creating TRANSFORM animation object.");
					animationType = SALConsts.S_A_TRANSFORM;
				}
				break;
				
			///////////////////// Modify keyword conditional testing /////////////////
			case SALConsts.S_MOD_TARGET:
				// Unlike any other label modify can take either one or two 
				// additional strings that can be any string value whatever.
				// Because the parser will find the next token to be a general
				// string we need to alert it to the fact that this is a significant
				// piece of string that will not match any tests. To do this 
				// we change the state of the parser to a S_L_MODIFY state and 
				// push that value so when time comes to test the value, it 
				// set the currTableName to this value PLUS any addition argument
				// pasted together with a '.' like this: 'text.layer1'.
				// We reset this value now incase there is no addition option.				
				// Now we can set the name of the symbol table for all of the 
				// variables related to this particular modification request.
				modTarget[0] = token;
				currTableName = modTarget[0] + SALConsts.FULL_COLON +
								modTarget[1] + SALConsts.FULL_COLON +
								modTarget[2];
				//System.out.println("setting the modification target to: '"+currTableName+"'");
				break;

			case SALConsts.S_MOD_TARGET_ID:		// "withid"
				if (lastState != SALConsts.S_MOD_ADD	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_DELETE	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_CHANGE	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_TARGET_CLASS_VALUE /* can follow the class value.*/
					&& lastState != SALConsts.S_MOD_TARGET)	/* can follow the target element.*/
				{
					
					throw new SyntaxError("key word "+SALConsts.L_WITH_ID+" not allowed here");
				}
				
				break;
				
			case SALConsts.S_MOD_TARGET_ID_VALUE:
				// Item index 1 is the id value, 2 is the class.
				modTarget[1] = token;
				currTableName = modTarget[0] + SALConsts.FULL_COLON +
								modTarget[1] + SALConsts.FULL_COLON +
								modTarget[2];
								
				//System.out.println("setting the modification target to: '"+currTableName+"'");
				break;
				
			case SALConsts.S_MOD_TARGET_CLASS:
				if (lastState != SALConsts.S_MOD_ADD	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_DELETE	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_CHANGE	/* if the target element is missing.*/
					&& lastState != SALConsts.S_MOD_TARGET_ID_VALUE /* can follow the class value.*/
					&& lastState != SALConsts.S_MOD_TARGET)	/* can follow the target element.*/
				{
					
					throw new SyntaxError("key word "+SALConsts.L_ON_LAYER+" not allowed here");
				}
		
				break;
				
				
			case SALConsts.S_MOD_TARGET_CLASS_VALUE:
				// Item index 2 is the class.
				modTarget[2] = token;
				// we do this each time because there can be as few as 1 modifiers and as many
				// as 3.
				currTableName = modTarget[0] + SALConsts.FULL_COLON +
								modTarget[1] + SALConsts.FULL_COLON +
								modTarget[2];
								
				//System.out.println("setting the modification target to: '"+currTableName+"'");
				break;			

			case SALConsts.S_LO_KEYWORD:
				if (token.equals(SALConsts.K_TAIL))
				{
					//System.out.println("applying layer ordering: TAIL.");
					lOrderType = SALConsts.S_LO_TAIL;
				}
				if (token.equals(SALConsts.K_HEAD))
				{
					//System.out.println("applying layer ordering: HEAD.");
					lOrderType = SALConsts.S_LO_HEAD;
				}
				break;
				
			default:
				System.out.println("no rule for: '"+token+"'. Must be a general String.");
				//throw new SyntaxError("'"+token+"' caused an error");
		}	// end switch
	}	// end of parseToken()


	private boolean isPunctuationMark(int someState)
	{
		switch (someState)
		{
			case SALConsts.S_FULL_COLON:
			case SALConsts.S_EQUALS:
			case SALConsts.S_SEMI_COLON:
			case SALConsts.S_COMMA:
			case SALConsts.S_OPEN_CBRACE:
			case SALConsts.S_CLOSE_CBRACE:
				return true;
			default:
				return false;
		}	// end switch
	}

	// Sets the listType and the valid number of items allowed in an entry.
	private void setListType(int type)
	{
		switch(type)
		{
			case SALConsts.S_INIT:
				break;

			case SALConsts.S_L_FILES:
				numItemsPerEntry = SALConsts.S_ONE_ITEM_ENTRY;
				break;

			case SALConsts.S_L_LAYER_ORDER:
				numItemsPerEntry = SALConsts.S_ONE_ITEM_UNSORTED;
				break;

			case SALConsts.S_L_FONTMAP:
				numItemsPerEntry = SALConsts.S_THREE_ITEM_ENTRY;
				break;
				
			case SALConsts.S_L_PENS:
				numItemsPerEntry = SALConsts.S_THREE_ITEM_ENTRY;
				break;

			case SALConsts.S_L_ANIMATION:
				numItemsPerEntry = SALConsts.S_TWO_ITEM_ENTRY;
				break;
				
			case SALConsts.S_L_CUSTOM_LAYERS:
				numItemsPerEntry = SALConsts.S_THREE_ITEM_ENTRY;
				break;

			case SALConsts.S_L_VARIABLES:
				numItemsPerEntry = SALConsts.S_ONE_ITEM_UNSORTED;
				break;
				
			case SALConsts.S_L_JAVASCRIPT:
				numItemsPerEntry = SALConsts.S_ONE_ITEM_UNSORTED;
				break;
				
			case SALConsts.S_MOD_ADD:
				numItemsPerEntry = SALConsts.S_TWO_ITEM_ENTRY;
				break;
				
			case SALConsts.S_MOD_CHANGE:
				numItemsPerEntry = SALConsts.S_TWO_ITEM_ENTRY;
				break;
				
			case SALConsts.S_MOD_DELETE:
				numItemsPerEntry = SALConsts.S_ONE_ITEM_UNSORTED;
				break;
				
			case SALConsts.S_L_CUSTOM_TEXT_STYLE:
				numItemsPerEntry = SALConsts.S_TWO_ITEM_ENTRY;
				break;
				
			default:
				throw new SyntaxError("'"+type+"' is an undefined list type");

		}	// end switch
		listType = type;
	}


	// Pushes a new value onto the stack
	private void push(int state)
	{
		for (int i = (stack.length -1); i >= 0; i--)
		{
			if (i > 0)
				stack[i] = stack[i -1];
			else
				stack[0] = state;
		}
	}

	// Pops a new value off of the stack
	private int pop()
	{
		int retVal = stack[0];
		for (int i = 0; i < stack.length; i++)
		{
			if (i < (stack.length -1))
				stack[i] = stack[i +1];
			else
				stack[i] = 0;
		}

		return retVal;
	}

	// returns the top value on the stack but does not alter the stack
	private int peek()
	{
		return stack[0];
	}

	// Shows the contents of the stack.
	private void printStack()
	{
		System.out.println("Stack contains:");
		for (int i = 0; i < stack.length; i++)
		{
			System.out.println("stack["+i+"] = "+stack[i]);
		}
	}

	//////////////////////////////////////////////////////////////////
	//					Internal Classes
	//////////////////////////////////////////////////////////////////

	/////////////////// Exceptions ///////////////////////////////////

	protected class SyntaxError extends RuntimeException
	{
		public SyntaxError(String msg)
		{
			System.err.println("Syntax Error: "+msg+" on line "+sal.getLineNumber()+
			" at word "+sal.getTokenNumber()+".");
		}
	}
}	// end parser class