
/****************************************************************************
**
**	FileName:	SvgAnimator.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This class encapsulates any sort of animation that is possible
**				in SVG and SMIL specifications.
**
**	Date:		May 9, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 May 9, 2003
**				1.0  February 9, 2005 Added the key word static to the inner
**				classes to comply with 'language lawyers'.
**              Was getting:
**    			[javac] H:\develop\Java\svgDev\R1.0\dxf2svg\animation\SvgAnimator.java:1328:
**				 cannot reference this before supertype constructor has been called
**			    [javac]                     super();
**				Fix description:
**				Take a look at how subclasses and inner classes work.  When you declare a
**				subclass, the subclass constructor always calls the superclass
**				constructor.  [For this discussion, we can ignore the exceptions - when
**				you explicitly call another constructor or where there is no superclass -
**				i.e. Object.]
**				So if you do not explicitly call a superclass constructor, the compiler
**				will insert this call.  So the constructor B above is equivalent to:
**
**				public B() {
**				super();   // Calls A()
**				}
**
**				Now, since B is an inner class of A, it is allowed to reference the
**				members of the enclosing instance.  The way this happens is that the
**				compiler generates a pointer to the enclosing instance and passes it as a
**				hidden argument to the constructor.  So a more or less equivalent view of
**				class B is:
**
**				public static class B extends A {
**				A ei;  // reference to enclosing instance
**				public B( A _ei ) {
**				this.ei = _ei;
**				}
**				}
**
**				Whenever you call the constructor of an inner class, you need to provide
**				the reference to the enclosing instance.  If you use an unqualified call
**				of the constructor from the enclosing class, the constructor automatically
**				provides the 'this' pointer as the enclosing instance.  If not, you would
**				need to qualify the call with an expression like: a.new B();
**
**				Of course, the superclass constructor is also there, so the constructor is
**				now:
**
**				public B( A _ei ) {
**				super();   // Calls A()
**				this.ei = _ei;
**				}
**
**				If we apply the same logic to C, we get the following:
**
**				public static class C extends B {
**				A ei;  // reference to enclosing instance
**				public C( A _ei ) {
**				super();   // Calls B()
**				this.ei = _ei;
**				}
**				}
**
**				But this code also has to provide the enclosing instance for the B()
**				constructor.  JLS 15.9.2 seems to suggest that the value provided would be
**				the hidden enclosing instance variable 'ei', so the constructor really
**				looks like this:
**
**				public C( A _ei ) {
**				super( this.ei );   // Calls B()
**				this.ei = _ei;
**				}
**
**				You can see the problem with this code: the superclass constructor is
**				always called before *any* subclass initializations are performed, and it
**				is illegal to reference any subclass members before that call - i.e. in
**				the arguments.  But in the code above, the superclass constructor argument
**				references the hidden instance member this.ei, before this.ei has been
**				initialized.
**
**				I suspect that JLS 15.9.2 was loosely interpreted before Java 1.4.2, so
**				that the argument _ei was passed to the superclass constructor instead of
**				the enclosing instance variable this.ei, and that the language lawyers
**				decided that the spec should be followed more closely.  I could not find
**				anything in the Java 1.4.2 release notes that says this, so I am just
**				guessing.
**
**				FWIW, I can see no reason that it should not be allowed to pass the _ei
**				argument.  But at the same time, it does not bother me much, since I do
**				not think that a class structure like this is a very good idea.
**
**				I am guessing that you do not really need to define classes B and C as
**				*inner* classes of A.  In the example you posted, there is no reason to do
**				this, since neither B or C contain any references to instance members of
**				A.  If that is the case, you could change B and C to non-inner nested
**				classes.  In other words, add the keyword "static" to the class
**				definitions.  If you do that, the error should go away.
**
**				And it does.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.animation;

import dxf2svg.svg.SvgMPath;

// Implementation notes: If you want to extend any of the subclasses or create a new class
// you must define the new classes methods in the super class AND in the class itself. The
// super classes definitions will probably contain a message about an object that is not
// of that type does not support this method. The method in the new class will contain
// the actual working method implementation.
/**	This class creates animation objects depending on the type of animation
*	required. Use the factory method {@link #getInstanceOf} to get an animation object.
*	Here are the different types of animate objects that are currently supported:
*	<P>
*<UL>
*	<LI> animate
*	<LI> animateMotion
*	<LI> animateColour
*	<LI> animateTransform
*	<LI> set
*</UL>
*	<P>
*	For ease of debugging the implementing application if any attribute gets an illegal value
*	an exception is thrown. This is done to stop processing rather than letting a subtle error
*	slip by as a default value for the attribute.
*	<P>
*	Animation in SVG is based on the SMIL specification.
*	@see <a href="http://www.w3.org/TR/2001/REC-smil-animation-20010904/#AnimationElements">SMIL</a>
*
*	@author		Andrew Nisbet
*	@version 	0.01 May 9, 2003
*/
public class SvgAnimator
{
	private boolean VERBOSE = true;
	// Some SvgAnimator members have mirror value of a String value and an int value.
	// This is done to facilitate error checking and inclusion status in the toString()
	// method when we are ready to output the data. Inclusion status is something like:
	// if the attribute value has been set to the default don't include it. Let's add
	// the definition for default.
	public final static byte DEFAULT			= -1;
	public final static byte NOT_DEFAULT		= -9;
	public final static String NOT_APPLICABLE	= "n/a";

	// Classes of animation that are possible in SVG.
	public final static byte SET				= 1;
	public final static byte ANIMATE			= 2;
	public final static byte ANIMATE_COLOUR		= 4;
	public final static byte ANIMATE_MOTION		= 8;
	public final static byte ANIMATE_TRANSFORM	= 16;
	// Feel free to add others, as required.

	/** AnimateTransform Type definition values. */
	public final static byte TRANSLATE			= 20;
	public final static byte SCALE				= 21;
	public final static byte ROTATE				= 22;
	public final static byte SKEW_X				= 23;
	public final static byte SKEW_Y				= 24;

	/** Attribute attribute types */
	public final static byte CSS				= 70;
	public final static byte XML				= 71;
	public final static byte AUTO				= DEFAULT;

	/** Acceptable values for calcMode */
	public final static byte DISCRETE 			= 80;
	public final static byte LINEAR				= DEFAULT;
	public final static byte PACED				= 82;
	public final static byte SPLINE				= 83;

	/** Acceptable values for Fill attribute values */
	public final static byte REMOVE				= DEFAULT;
	public final static byte FREEZE				= 91;
	private byte fill;
	private String fillStr;

	/** Acceptable values for Restart attribute values */
	public final static byte ALWAYS				= DEFAULT;
	public final static byte NEVER				= 101;
	public final static byte WHEN_NOT_ACTIVE	= 102;
	private byte restart;
	private String restartStr;

	/** Acceptable values for Additive attribute value */
	public final static byte REPLACE			= DEFAULT;
	public final static byte SUM				= 111;

	/** Acceptable values for Accumulate attribute values */
	public final static byte NONE				= DEFAULT;
	//public final static byte SUM				= 111; // already defined in 'additive'.


	/** Acceptable values for Repeatcount attribute values */
	public final static byte NUMERIC			= 120;	// Expect a numeric value.
	public final static byte INDEFINITE			= DEFAULT;
	private byte repeat;
	// These values represent one and the same value so use one OR the other.
	private double repeatCount;
	private String repeatCountStr; 					// This value is immutable.

	/** Acceptable values for rotation attribute values */
	public final static byte ROTATE_AUTO		= 121;
	public final static byte ROTATE_AUTO_REVERSE= 122;
	public final static byte ROTATE_ANGLE		= DEFAULT;
	// Other values required by all animation objects

	private String toStr;
	private String beginStr;
	private String durStr;
	private String endStr;
	private String repeatDurStr;
	// ... and their equivelant switches.
	private byte to;
	private byte begin;
	private byte dur;
	private byte end;
	private byte repeatDur;

	// This value controls whether the object is an empty element.
	protected boolean isEmptyElement;
	// This value controls whether to output as attributes or as discreet elements; true
	// would output as attributes of another element.
	protected boolean outputAsAttributes;
	// This value is the name of the element.
	protected String elementName;		// There are no mutators for this value.
	protected boolean takesAnimationTargetAttribute;
	// content specific variables.
	private String content;
	public static final byte ADD 	= -20;
	// public static final byte SET	= -21; // already defined
	public static final byte RESET	= -22;

	////////////////////////////////////////////////
	//				Constructor
	////////////////////////////////////////////////
	public SvgAnimator()
	{
		// These values are relevant to all animation objects.
		fill 				= DEFAULT;
		fillStr 			= null;
		restart				= DEFAULT;
		restartStr			= null;
		repeat				= DEFAULT;
		repeatCount			= 0.0;			// Number of times the animation is repeated (double).
		to					= DEFAULT;
		toStr				= null;			// To value; sister attribute of 'from'.
		begin				= DEFAULT;
		beginStr			= null;			// Where or when the animation begins
		dur					= DEFAULT;		// dur switch is set off
		durStr		 		= null;			// How long does the animaiton last.
		end					= DEFAULT;		// Don't include on output.
		endStr				= null;			// Where or when the animation ends
		repeatDur			= DEFAULT;		// repeatDur's on off switch
		repeatDurStr		= null;			// Duration of repeatition
		// Physical element representation.
		isEmptyElement		= true;			// All elements are, currently, empty.
		outputAsAttributes	= false;		// Will output as discreet elements.
		elementName			= null;			// The name of the element
		content				= null;			// Content String.
	}


	////////////////////////////////////////////////
	//				Factory Method
	////////////////////////////////////////////////
	// Objects that instantiation with a specific attribute name should be
	// included here
	/**	This factory method returns the appropriate object, depending on the
	*	the requested type of animation.
	*/
	public SvgAnimator getInstanceOf(int type)
	{
		switch (type)
		{
			case SET: 			 	return new SvgSet();
			case ANIMATE:		 	return new SvgAnimate();
			case ANIMATE_COLOUR: 	return new SvgAnimateColour();
			case ANIMATE_MOTION: 	return new SvgAnimateMotion();
			case ANIMATE_TRANSFORM:	return new SvgAnimateTransform();
			default:			 	throw new UndefinedAnimationClassException(type);
		}	// end switch
	}



	////////////////////////////////////////////////
	//		General Methods for all sub-classes
	////////////////////////////////////////////////
	//// Here we declare all the methods of subclasses so, if the client tries to invoke
	// a method on a sub-class that does not support it a message is issued. Otherwize
	// you get a message that the subclass does not have the method you require.
	/////////////////////// methods unique to a subclass or subclasses ////////////////////



	/** Sets the <CODE>additive</CODE> attribute. Valid values are:
	*<UL>
	*	<LI> REPLACE (default)
	*	<LI> SUM
	*</UL>
	*/
	public void setAdditive(String additiveValue)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setAdditive(). Ignoring argument value.");

	}


	/** Sets the type of attribute you would like to animate. Valid values are:
	*<UL>
	*	<LI> CSS
	*	<LI> XML
	*	<LI> auto	(default)
	*</UL>
	*/
	public void setAttributeType(String type)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setAttributeType(). Ignoring argument value.");
	}


	/** Sets the <CODE>from</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setFrom(String from)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setFrom(). Ignoring argument value.");
	}


	/** Sets the <CODE>by</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setBy(String by)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setBy(). Ignoring argument value.");
	}


	/** Sets the <CODE>values</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setValues(String v)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setValues(). Ignoring argument value.");
	}


	/** Sets the <CODE>keyTimes</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setKeyTimes(String keyTimes)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setKeyTime(). Ignoring argument value.");
	}

	/** Sets the <CODE>keySplines</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setKeySplines(String keySplines)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setKeySplines(). Ignoring argument value.");
	}


	/** Sets the <CODE>calcMode</CODE> attribute. Valid values are:
	*<UL>
	*	<LI> DISCRETE = 110;
	*	<LI> LINEAR = DEFAULT;
	*	<LI> PACED = 112;
	*	<LI> SPLINE	= 113;
	*</UL>
	*/
	public void setCalcMode(String calc)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setCalcMode(). Ignoring argument value.");
	}


	/** Sets the <CODE>path</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setPath(String path)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setPath(). Ignoring argument value.");
	}

	/** Sets the <CODE>path</CODE> attribute's value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setOrigin(String origin)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setOrigin(). Ignoring argument value.");
	}


	/** Sets the rotation of target animation object in relation to the path it
	*	is being animated along. Used in AnimateMotion object only.
	*/
	public void setRotate(String rotate)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setRotate(). Ignoring argument value.");
	}


	/** Sets an mpath sibling element on a AnimateMotion object.
	*/
	public void setMPath(SvgMPath mpath)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setMPath(). Ignoring argument value.");
	}



	/** Sets the type of transform required of the AnimateTransform object.
	*	Valid types of transformations are:
	*<UL>
	*	<LI> TRANSLATE
	*	<LI> SCALE
	*	<LI> ROTATE
	*	<LI> SKEW_X
	*	<LI> SKEW_Y
	*</UL>
	*/
	public void setTransformType(String type)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setTransformType(). Ignoring argument value.");
	}


	/** Returns the name of the attribute that is the target of the animation. Example:
	*	if you wish the fill of an object to change colour on a click, then the object
	*	will return 'fill' as its target attribute.
	*	@return Attribute Target attribute of the animation object if any, see
	*	{@link SvgAnimateTransform#getAnimationTargetAttribute}.
	*/
	public String getAnimationTargetAttribute()
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not animate attributes.");
		return NOT_APPLICABLE;
	}











	/////////////////////// methods common to all subclasses ///////////////////////
	/** Sets another attribute's value temperarily, for the course of the animation.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setTo(String to)
	{
		if (to == null){
			throw new IllegalAttributeValueException(to, "to");
		}
		if (to.equals("")){
			this.to = DEFAULT;
			return;
		}
		this.to = NOT_DEFAULT;
		this.toStr = to;
	}


	/** Sets the <CODE>restart</CODE> attribute. Valid values are:
	*<UL>
	*	<LI> ALWAYS (default)
	*	<LI> NEVER
	*	<LI> WHEN_NOT_ACTIVE
	*</UL>
	*	@throws IllegalAttributeValueException if the argument is not defined
	*	in the DTD.
	*/
	public void setRestart(String value)
	{
		if (value == null){
			throw new IllegalAttributeValueException(value, "restart");
		}
		if (value.equals("")){
			this.restart = DEFAULT;
			return;
		}

		if (value.equalsIgnoreCase("always"))
		{
			restart = ALWAYS;
			restartStr = "always";
		}
		else if (value.equalsIgnoreCase("never"))
		{
			restart = NEVER;
			restartStr = "never";
		}
		else if (value.equalsIgnoreCase("whenNotActive"))
		{
			restart = WHEN_NOT_ACTIVE;
			restartStr = "whenNotActive";
		}
		else
		{
			throw new IllegalAttributeValueException(value, "restart");
		}
	}


	/** Sets the <CODE>fill</CODE> attribute. Valid values are:
	*<UL>
	*	<LI> REMOVE (default)
	*	<LI> FREEZE
	*</UL>
	*	@throws IllegalAttributeValueException if the argument is not defined
	*	in the DTD.
	*/
	public void setFill(String value)
	{
		if (value == null){
			throw new IllegalAttributeValueException(value, "fill");
		}
		if (value.equals("")){
			this.fill = DEFAULT;
			return;
		}

		if (value.equalsIgnoreCase("remove"))
		{
			fill = REMOVE;
			fillStr = "remove";
		}
		else if (value.equalsIgnoreCase("freeze"))
		{
			fill = FREEZE;
			fillStr = "freeze";
		}
		else
		{
			throw new IllegalAttributeValueException(value, "fill");
		}
	}

	/** Sets the animations begin value. Valid values could be times or CSS attributes
	*	or what-have-you.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setBegin(String begin)
	{
		if (begin == null){
			throw new IllegalAttributeValueException(begin, "begin");
		}
		if (begin.equals("")){
			this.begin = DEFAULT;
			return;
		}

		if (begin == null)
		{
			throw new IllegalAttributeValueException(begin, "begin");
		}
		if (begin.equals(""))
		{
			System.err.println("Warning: an attempt to set "+this.getClass().getName()+"'s "+
				"attribute 'begin' to an empty value was made, and ignored.");
			this.begin = DEFAULT;
			return;
		}
		this.begin		= NOT_DEFAULT;
		this.beginStr 	= begin;
	}


	/** Sets the animations duration value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setDur(String dur)
	{
		if (dur == null)
		{
			throw new IllegalAttributeValueException(dur, "dur");
		}
		if (dur.equals(""))
		{
			System.err.println("Warning: an attempt to set "+this.getClass().getName()+"'s "+
				"attribute 'begin' to an empty value was made, and ignored.");
			this.dur = DEFAULT;
			return;
		}
		this.dur	= NOT_DEFAULT;
		this.durStr = dur;
	}



	/** Sets the animations end value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setEnd(String end)
	{
		if (end == null)
		{
			throw new IllegalAttributeValueException(end, "end");
		}
		if (end.equals(""))
		{
			System.err.println("Warning: an attempt to set "+this.getClass().getName()+"'s "+
				"attribute 'begin' to an empty value was made, and ignored.");
			this.end = DEFAULT;
			return;
		}
		this.end	= NOT_DEFAULT;
		this.endStr = end;
	}



	/** This method sets the repeat duration of the animation.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setRepeatDur(String repeatDur)
	{
		if (repeatDur == null)
		{
			throw new IllegalAttributeValueException(repeatDur, "repeatDur");
		}
		if (repeatDur.equals(""))
		{
			System.err.println("Warning: an attempt to set "+this.getClass().getName()+"'s "+
				"attribute 'begin' to an empty value was made, and ignored.");
			this.repeatDur = DEFAULT;
			return;
		}
		this.repeatDur		= NOT_DEFAULT;
		this.repeatDurStr	= repeatDur;
	}




	// Partial repeat cycles are allowed so we use a double.
	/** This method sets the <CODE>repeatCount</CODE> attribute of an animation object
	*	causing it to loop for a repeatCount number of times.
	*	@param repNum The number of times the animation is to loop. If the argument is
	*	less than zero the animation will loop indefinitely.
	*/
	public void setRepeatCount(double repNum)
	{
		if (repNum >= 0.0)			// must be greater than zero
		{
			this.repeatCount = repNum;
			repeat = NUMERIC;
		}
		else	// on a negative number, revert back to INDEFINITE (default) and set count to zero.
		{
			this.repeatCount = 0.0;
			repeat = INDEFINITE;
		}
	}

	// Partial repeat cycles are allowed so we use a double.
	/** This method sets the <CODE>repeatCount</CODE> attribute of an animation object
	*	causing it to loop for a repeatCount number of times.
	*	@param repStr The number of times the animation is to loop. If the argument is
	*	less than zero the animation will loop indefinitely.
	*	@throws NumberFormatException if the argument does not parse to a double value.
	*/
	public void setRepeatCount(String repStr)
	{
		if (repStr == null){
			throw new IllegalAttributeValueException(repStr, "repeatCount");
		}
		if (repStr.equals("")){
			this.repeat = DEFAULT;
			this.repeatCount = 0.0;
			return;
		}

		double repNum = Double.parseDouble(repStr);

		if (repNum >= 0.0)			// must be greater than zero
		{
			this.repeatCount = repNum;
			repeat = NUMERIC;
		}
		else	// on a negative number, revert back to INDEFINITE (default) and set count to zero.
		{
			this.repeatCount = 0.0;
			repeat = INDEFINITE;
		}
	}

	/** Sets the animation's target attribute value.
	*	@throws IllegalAttributeValueException if the argument string is null. If the
	*	argument String is empty '""' then the attribute is set to its default state
	*	and will not be output.
	*/
	public void setAttributeToAnimate(String attrib)
	{
		if (VERBOSE)
			System.err.println(this.getClass().getName()+
				" does not support the method setAttributeToAnimate(). Ignoring argument value.");
	}






	// Implementation notes: the content is appended directly to the output stringbuffer
	// so there is no fuss with pasting empty strings if not necessary.
	// To date there are no animation elements implemented that require this method.
	/** This method returns the content of a animation object, if any. If there is
	*	no content then an StringBuffer is returned untouched.
	*/
	public void getContent(StringBuffer buff)
	{	buff.append(content);	}

	/** Allows elements to set or add content.
	*	@param instruction What to do with the argument. Valid values are:
	*<UL>
	*	<LI> ADD adds the argument to exisiting content.
	*	<LI> SET replaces any previous content with argument string.
	*	<LI> RESET deletes any content; ingores argument string.
	*</UL>
	*/
	public void addContent(String c, byte instruction)
	{
		if (c == null)
		{
			System.err.println("Attempted to set object's content to a null value.");
			return;
		}
		StringBuffer cBuff = null;
		switch (instruction)
		{
			case ADD:
				if (content != null)
					cBuff = new StringBuffer(content);
				else
					cBuff = new StringBuffer();
				cBuff.append(c);
				content = cBuff.toString();
				break;
			case SET:
				cBuff = new StringBuffer();
				cBuff.append(c);
				content = cBuff.toString();
				break;
			case RESET:
				content = null;
				break;
			default:	System.err.println("Error addContent(): illegal instruction: '"+
				instruction+"' received.");
		}	// end switch
	}


	/** Outputs the element as an attribute String instead of a discreet XML element.
	*	This method will output all relavent attributes with the exception of those
	*	that have been set to SVG's default values.
	*/
	public void getAttributes(StringBuffer buff)
	{
		// Test for all this classes attrib values.
		// These values are relevant to all animation objects.
		if (to != DEFAULT)
			buff.append(" to=\""+toStr+"\"");
		if (begin != DEFAULT)
			buff.append(" begin=\""+beginStr+"\"");
		if (dur	!= DEFAULT)
			buff.append(" dur=\""+durStr+"\"");
		if (end	!= DEFAULT)
			buff.append(" end=\""+endStr+"\"");
		if (repeatDur != DEFAULT)
			buff.append(" repeatDur=\""+repeatDurStr+"\"");
		if (restart != DEFAULT)
			buff.append(" restart=\""+restartStr+"\"");
		if (repeat != DEFAULT)
			buff.append(" repeatCount=\""+String.valueOf(repeatCount)+"\"");
		if (fill != DEFAULT)
			buff.append(" fill=\""+fillStr+"\"");
	}


	/** Outputs the animation element tag(s) and content, if any.
	*/
	public String toString()
	{
		// All classes support this method.
		StringBuffer outBuff = new StringBuffer();

		outBuff.append("<"+elementName);		// Open the tag
		/////////////////// attributes ///////////
		getAttributes(outBuff);
		//////////////////////////////////////////
		if (this.isEmptyElement)				// close tag
		{
			outBuff.append("/>");
		}
		else
		{
			outBuff.append(">\n\t");
			///////////////////// content ////////
			getContent(outBuff);				// will output content if there is any or not.
			//////////////////////////////////////
			outBuff.append("\n</"+elementName+">");
		}

		return outBuff.toString();
	}





////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////

	////////////////////////////////////////////////
	//				Sub-classes
	////////////////////////////////////////////////


	/**	This class is an encapsulation of the animation element &lt;set&gt;, not to be confused
	*	with SvgCollection or java.util.Set. This class is primarily responsible for
	*	setting a single attribute within an svg element. If you are animating an element
	*	via its attributes, use this object and call {@link #getAttributes} method.
	*/
	protected static class SvgSet extends SvgAnimator
	{
		private byte animatedAttribute;
		private String animatedAttributeStr;// Attribute name value like 'stroke-width'.
		private byte attributeType;			// Default value. This is required in all subclasses.
		private String attributeTypeStr;	// String value of attribute

		//////////////// Constructor /////////////////
		protected SvgSet()
		{
			super();
			animatedAttribute		= DEFAULT;
			animatedAttributeStr	= null;
			attributeType 			= DEFAULT;
			attributeTypeStr		= null;
			this.elementName 		= "set";
			takesAnimationTargetAttribute
									= true;			// Target attribute for animation required
		}

		//////////////// Methods /////////////////////
		/**	Sets the attribute you wish to animated.
		*	@throws IllegalAttributeValueException if the attribute argument is null.
		*	Setting the attribute to an empty string will cause the object to revert
		*	to the default value. In this case, a null string.
		*/
		public void setAttributeToAnimate(String attrib)
		{
			if (attrib == null){
				throw new IllegalAttributeValueException(attrib, "attributeName");
			}
			if (attrib.equals("")){
				this.animatedAttribute = DEFAULT;
				return;
			}
			this.animatedAttribute = NOT_DEFAULT;
			this.animatedAttributeStr = attrib;
		}


		/** Sets the type of attribute you would like to animate. Valid values are:
		*<UL>
		*	<LI> CSS
		*	<LI> XML
		*	<LI> auto	(default)
		*</UL>
		*/
		public void setAttributeType(String type)
		{
			if (type == null){
				throw new IllegalAttributeValueException(type, "attributeType");
			}
			if (type.equals("")){
				attributeType = DEFAULT;
				return;
			}

			if (type.equalsIgnoreCase("CSS"))
			{
				attributeType = CSS;
				attributeTypeStr = "CSS";
			}
			else if (type.equalsIgnoreCase("XML"))
			{
				attributeType = XML;
				attributeTypeStr = "XML";
			}
			else if (type.equalsIgnoreCase("auto"))
			{
				attributeType = AUTO;
				attributeTypeStr = "auto";
			}
			else
			{
				throw new IllegalAttributeValueException(type, "attributeType");
			}
		}


		/** Returns the name of the attribute that is the target of the animation. Example:
		*	if you wish the fill of an object to change colour on a click, then the object
		*	will return 'fill' as its target attribute.
		*	@return Attribute Target attribute of the animation object if any, see
		*	{@link SvgAnimator#getAnimationTargetAttribute}.
		*/
		public String getAnimationTargetAttribute()
		{
			return animatedAttributeStr;
		}

		/** Outputs the element as an attribute String instead of a discreet XML element.
		*	This method will output all relavent attributes with the exception of those
		*	that have been set to SVG's default values.
		*	@throws UndefinedAnimationTargetAttribute if the name of the attribute to animate
		*/
		public void getAttributes(StringBuffer buff)
		{
			if (takesAnimationTargetAttribute == true)
			{
				if(animatedAttribute == DEFAULT)	// This value has not been set.
					throw new UndefinedAnimationTargetAttribute();
				else
					buff.append(" attributeName=\""+animatedAttributeStr+"\"");
			}


			super.getAttributes(buff);

			// handle 'to' attribute if necessary
			if (attributeType != DEFAULT)
				buff.append(" attributeType=\""+attributeTypeStr+"\"");
		}
	}	// end of SvgSet.

























	/**	This class represents and encapsulates the &lt;animate&gt; element.
	*	Animate is an empty element that can do general animation within
	*	an object.
	*/
	protected static class SvgAnimate extends SvgSet
	{
		private byte calcMode;		// This is the default for all sub-classes.
		private String calcModeStr;	// CalcMode string.
		private byte additive;
		private byte accumulate;
		private byte from;
		private String fromStr;
		private byte by;
		private String byStr;
		private byte values;
		private String valuesStr;
		private byte keyTimes;
		private String keyTimesStr;
		private byte keySplines;
		private String keySplinesStr;

		//////////////// Constructor /////////////////
		protected SvgAnimate()
		{
			//super();
			this.elementName	= "animate";
			accumulate			= DEFAULT;
			calcMode			= DEFAULT;
			calcModeStr			= null;
			additive			= DEFAULT;
			from				= DEFAULT;
			fromStr				= null;
			by					= DEFAULT;
			byStr				= null;
			values				= DEFAULT;
			valuesStr			= null;
			keyTimes			= DEFAULT;
			keyTimesStr			= null;
			keySplines			= DEFAULT;
			keySplinesStr		= null;
		}

		//////////////// Methods ////////////////////

		/** Sets the <CODE>from</CODE> attribute's value.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setFrom(String from)
		{
			if (from == null){
				throw new IllegalAttributeValueException(from, "from");
			}
			if (from.equals("")){
				this.from = DEFAULT;
				return;
			}
			this.from = NOT_DEFAULT;
			this.fromStr = from;
		}


		/** Sets the <CODE>by</CODE> attribute's value.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setBy(String by)
		{
			if (by == null){
				throw new IllegalAttributeValueException(by, "by");
			}
			if (by.equals("")){
				this.by = DEFAULT;
				return;
			}
			this.by = NOT_DEFAULT;
			this.byStr = by;
		}



		/** Sets the <CODE>values</CODE> attribute's value.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setValues(String v)
		{
			if (v == null){
				throw new IllegalAttributeValueException(v, "values");
			}
			if (v.equals("")){
				this.values = DEFAULT;
				return;
			}
			this.values = NOT_DEFAULT;
			this.valuesStr = v;
		}


		/** Sets the <CODE>keyTimes</CODE> attribute's value.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setKeyTimes(String keyTimes)
		{
			if (keyTimes == null){
				throw new IllegalAttributeValueException(keyTimes, "keyTimes");
			}
			if (keyTimes.equals("")){
				this.keyTimes = DEFAULT;
				return;
			}
			this.keyTimes = NOT_DEFAULT;
			this.keyTimesStr = keyTimes;
		}


		/** Sets the <CODE>keySplines</CODE> attribute's value.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setKeySplines(String keySplines)
		{
			if (keySplines == null){
				throw new IllegalAttributeValueException(keySplines, "keySplines");
			}
			if (keySplines.equals("")){
				this.keySplines = DEFAULT;
				return;
			}
			this.keySplines = NOT_DEFAULT;
			this.keySplinesStr = keySplines;
		}



		/** Sets the <CODE>calcMode</CODE> attribute. Valid values are:
		*<UL>
		*	<LI> DISCRETE = 110;
		*	<LI> LINEAR = DEFAULT;
		*	<LI> PACED = 112;
		*	<LI> SPLINE	= 113;
		*</UL>
		*/
		public void setCalcMode(String calc)
		{
			if (calc == null){
				throw new IllegalAttributeValueException(calc, "calcMode");
			}
			if (calc.equals("")){
				this.calcMode = DEFAULT;
				return;
			}

			if(calc.equalsIgnoreCase("discrete"))
			{
				calcMode = DISCRETE;
				calcModeStr = "discrete";
			}
			else if(calc.equalsIgnoreCase("paced"))
			{
				calcMode = PACED;
				calcModeStr = "paced";
			}
			else if(calc.equalsIgnoreCase("spline"))
			{
				calcMode = SPLINE;
				calcModeStr = "spline";
			}
			else if(calc.equalsIgnoreCase("linear"))
			{
				calcMode = LINEAR;
				calcModeStr = "linear";
			}
			else
			{
				throw new IllegalAttributeValueException(calc, "calcMode");
			}

		}



		/** Sets the <CODE>additive</CODE> attribute. Valid values are:
		*<UL>
		*	<LI> REPLACE (default)
		*	<LI> SUM
		*</UL>
		*/
		public void setAdditive(String additiveValue)
		{
			if (additiveValue == null){
				throw new IllegalAttributeValueException(additiveValue, "additiveValue");
			}
			if (additiveValue.equals("")){
				this.additive = DEFAULT;
				return;
			}


			if (additiveValue.equalsIgnoreCase("sum"))
				additive = SUM;
			else if (additiveValue.equalsIgnoreCase("replace"))
				additive = REPLACE;
			else
				throw new IllegalAttributeValueException(additiveValue,"additive");

		}

		// This method chains up through the super classes collecting their entities.
		/** Outputs the element as an attribute String instead of a discreet XML element.
		*	This method will output all relavent attributes with the exception of those
		*	that have been set to SVG's default values.
		*/
		public void getAttributes(StringBuffer buff)
		{
			super.getAttributes(buff);

			if (from != DEFAULT)
				buff.append(" from=\""+fromStr+"\"");
			if (by != DEFAULT)
				buff.append(" by=\""+byStr+"\"");
			if (additive != DEFAULT)
				buff.append(" additive=\"sum\"");
			if (accumulate != DEFAULT)
				buff.append(" accumulate=\"sum\"");
			if (calcMode != DEFAULT)
				buff.append(" calcMode=\""+calcModeStr+"\"");
			if (values != DEFAULT)
				buff.append(" values=\""+valuesStr+"\"");
			if (keyTimes != DEFAULT)
				buff.append(" keyTimes=\""+keyTimesStr+"\"");
			if (keySplines != DEFAULT)
				buff.append(" keySplines=\""+keySplinesStr+"\"");
		}
	}	// end of SvgAnimate class






	/** This class specifies a colour transfromation over time.
	*/
	protected static class SvgAnimateColour extends SvgAnimate
	{
		//////////////// Constructor /////////////////
		protected SvgAnimateColour()
		{
			super();
			this.elementName	= "animateColor";
		}

		// This method chains up through the super classes collecting their entities.
		/** Outputs the element as an attribute String instead of a discreet XML element.
		*	This method will output all relavent attributes with the exception of those
		*	that have been set to SVG's default values.
		*/
		public void getAttributes(StringBuffer buff)
		{
			super.getAttributes(buff);
		}
	}






	/** The animate transform element animates a transformation attribute on a target element,
	*	thereby allowing animations to control translation, scaling, rotation and/or skewing.
	*/
	protected static class SvgAnimateTransform extends SvgAnimate
	{
		private byte type;
		private String typeStr;

		//////////////// Constructor /////////////////
		protected SvgAnimateTransform()
		{
			super();
			type				= DEFAULT;
			typeStr				= null;
			this.elementName 	= "animateTransform";
		}


		/** Sets the type of transform required over time.
		*<UL>
		*	<LI> TRANSLATE
		*	<LI> SCALE
		*	<LI> ROTATE
		*	<LI> SKEW_X
		*	<LI> SKEW_Y
		*</UL>
		*/
		public void setTransformType(String type)
		{
			if (type == null){
				throw new IllegalAttributeValueException(type, "transformType");
			}
			if (type.equals("")){
				this.type = DEFAULT;
				return;
			}

			if (type.equalsIgnoreCase("translate"))
			{
				typeStr = "translate";
				this.type = TRANSLATE;
			}
			else if (type.equalsIgnoreCase("scale"))
			{
				typeStr = "scale";
				this.type = SCALE;
			}
			else if (type.equalsIgnoreCase("rotate"))
			{
				typeStr = "rotate";
				this.type = ROTATE;
			}
			else if (type.equalsIgnoreCase("skewX"))
			{
				typeStr = "skewX";
				this.type = SKEW_X;
			}
			else if (type.equalsIgnoreCase("skewY"))
			{
				typeStr = "skewY";
				this.type = SKEW_Y;
			}
			else
			{
				throw new IllegalAttributeValueException(type, "type");
			}
		}

		// This method chains up through the super classes collecting their entities.
		/** Outputs the element as an attribute String instead of a discreet XML element.
		*	This method will output all relavent attributes with the exception of those
		*	that have been set to SVG's default values.
		*/
		public void getAttributes(StringBuffer buff)
		{
			super.getAttributes(buff);
			if (type != DEFAULT)
				buff.append(" type=\""+typeStr+"\"");
		}
	}











	/**	This class will animate an object with motion attributes. It will also animate
	*	an object along an internal path {@link #setPath} or along a path reference mpath
	*	{@link #setMPath}
	*/
	protected final class SvgAnimateMotion extends SvgAnimate
	{
		private byte path;
		private String pathStr;	// Path attribute for animating along a path.
		private byte origin;
		private String originStr;
		private byte rotate;
		private String rotateStr;
		private byte mpath;

		//////////////// Constructor /////////////////
		protected SvgAnimateMotion()
		{
			super();
			// so we don't get caught when the superclasse's
			// checks to see if the target attribute has been set.
			// because we don't take one.
			takesAnimationTargetAttribute
								= false;			// Target attribute for animation required
			path 				= DEFAULT;
			pathStr 			= null;
			origin				= DEFAULT;
			originStr			= null;
			rotate				= DEFAULT;
			rotateStr			= null;
			mpath				= DEFAULT;
			this.elementName	= "animateMotion";
		}

		/** Sets the <CODE>path</CODE> attribute's value. The path is the motion path of
		*	the animaiton expressed in the same manner as the 'd=' attribute on the path
		*	element. All SvgElements have methods to express themselves as paths, some
		*	are not implemented because it just didn't make any sense to.
		*	Here is a list to date:
		*<UL>
		*	<LI> SvgArc
		*	<LI> SvgPolyLine
		*	<LI> SvgEllipse
		*	<LI> SvgHatch
		*	<LI> SvgLine
		*	<LI> SvgSpline
		*</UL>
		*	<P>
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*	@see dxf2svg.svg.SvgGraphicElement#getElementAsPath
		*/
		public void setPath(String path)
		{
			if (path == null){
				throw new IllegalAttributeValueException(path, "path");
			}
			if (path.equals("")){
				this.path = DEFAULT;
				return;
			}
			this.path = NOT_DEFAULT;
			this.pathStr = path;
		}

		/**	Gives the AnimateMotion object a sub element mpath object.
		*	@param mpath a <i>reference</i> to a path to animate along.
		*/
		public void setMPath(SvgMPath mpath)
		{
			if (mpath == null){
				throw new IllegalAttributeValueException("mpath", "mpath");
			}
			this.mpath = NOT_DEFAULT;
			addContent(mpath.toString(), SET);
			// This is no longer an empty element.
			isEmptyElement = false;
		}


		public void setAttributeToAnimate(String attrib)
		{
			if (VERBOSE)
				System.err.println(this.getClass().getName()+
					" does not support the method setAttributeToAnimate(). Ignoring argument value.");
		}


		public void setAttributeType(String type)
		{
			if (VERBOSE)
				System.err.println(this.getClass().getName()+
					" does not support the method setAttributeType(). Ignoring argument value.");
		}

		/** Sets the <CODE>origin</CODE> attribute's value. The origin is a point from
		*	where you would like the animaiton to begin.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setOrigin(String origin)
		{
			if (origin == null){
				throw new IllegalAttributeValueException(origin, "origin");
			}
			if (origin.equals("")){
				this.origin = DEFAULT;
				return;
			}
			this.origin = NOT_DEFAULT;
			this.originStr = origin;
		}


		/** Sets the rotation of target animation object in relation to the path it
		*	is being animated along. Used in AnimateMotion object only.
		*	@throws IllegalAttributeValueException if the argument string is null. If the
		*	argument String is empty '""' then the attribute is set to its default state
		*	and will not be output.
		*/
		public void setRotate(String rotate)
		{
			if (rotate == null){
				throw new IllegalAttributeValueException(rotate, "rotate");
			}
			if (rotate.equals("")){
				this.rotate = DEFAULT;
				return;
			}
			this.rotate = NOT_DEFAULT;
			this.rotateStr = rotate;
		}

		/** Because AnimateMotion does not animate any host element's attribute,
		*	the return value is irrelevant and a message may be issued if the
		*	VERBOSE flag is set.
		*	@return {@link #NOT_APPLICABLE} because the object does not support
		*	this functionality.
		*/
		public String getAnimationTargetAttribute()
		{
			if (VERBOSE)
				System.err.println(this.getClass().getName()+
					" does not animate attributes.");
			return NOT_APPLICABLE;
		}

		// This method chains up through the super classes collecting their entities.
		/** Outputs the element as an attribute String instead of a discreet XML element.
		*	This method will output all relavent attributes with the exception of those
		*	that have been set to SVG's default values.
		*/
		public void getAttributes(StringBuffer buff)
		{
			super.getAttributes(buff);
			if (origin != DEFAULT)
				buff.append(" origin=\""+originStr+"\"");
			if (path != DEFAULT)
				buff.append(" path=\""+pathStr+"\"");
			if (rotate != DEFAULT)
				buff.append(" rotate=\""+rotateStr+"\"");
		}
	}





	////////////////////////////////////////////////
	//				Exceptions
	////////////////////////////////////////////////
	/** This exception gets thrown if the animation object requested hasn't
	*	been defined yet, or is not part of the SVG w3.org specification at
	*	the this time.
	*/
	protected class UndefinedAnimationClassException extends RuntimeException
	{
		protected UndefinedAnimationClassException(int type)
		{
			System.err.println("'"+type+"' is an undefined animation type for this version "+
				"of this application. See documentation for help.");
		}

		protected UndefinedAnimationClassException()
		{
			System.err.println("Illegal value recieved as argument: attribute type. "+
				"This type of animation object does not operate on an attribute.");
		}
	}

	/** This Runtime exception gets thrown if the value passed to an attribute is not defined
	*	in the DTD.
	*/
	protected class IllegalAttributeValueException extends RuntimeException
	{

		protected IllegalAttributeValueException(String attribValue, String attrib)
		{
			System.err.println("'"+attribValue+"' is an undefined value for attribute: '"+
				attrib+"'. See documentation for help.");
		}

		protected IllegalAttributeValueException(double attribValue, String attrib)
		{
			System.err.println("'"+attribValue+"' is an out-of-range value for attribute: '"+
				attrib+"'. See documentation for help.");
		}
	}

	/** This exception is thrown if the target attribute of the animation is required but
	*	is not specified.
	*/
	protected class UndefinedAnimationTargetAttribute extends RuntimeException
	{
		protected UndefinedAnimationTargetAttribute()
		{
			System.err.println("The DTD requires an attribute name as a target to animate, and "+
				"the attribute's name has not been set.");
		}
	}
}