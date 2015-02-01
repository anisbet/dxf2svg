
/****************************************************************************
**
**	FileName:	SvgCollection.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates collections of svg objects.
**
**	Date:		September 3, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - September 3, 2002
**				1.01 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				1.10 - February 14, 2005 Added overloaded addElement(index, object)
**				to ensure that text is entered onto the layer last. This fixes a but
**				where a png on an english layer is written after the text of the layer
**				thus obscuring the text.
**				1.11 - March 1, 2005 getGroupElements() method now returns a clone
**				of the vector of SvgObjects.
**				1.12 - March 16, 2005 Added getGroupElementsByReference() method for
**				searching and modifying elements in-situ. See DxfConverter.init().
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import java.util.*;
import dxf2svg.*;		// DxfConverter & NullDxfConverter
import dxf2svg.svg.SvgText;	
import dxf2svg.animation.*;
import java.util.regex.*;

/**
*	This class is the base class for all collections of SvgObjects.
*	These come in three types: <B>&lt;pattern&gt;</B>, <B>&lt;g&gt;</B>
*	and <B>&lt;symbol&gt;</B> but you are not limited to those three.
*	Collections may contain a mixture of SvgReferences and SvgObjects.
*
*	@see SvgSymbol
*	@see SvgGroup
*	@see SvgLayerGroup
*
*	@version	1.11 - March 1, 2005 
*	@author		Andrew Nisbet
*/
public abstract class SvgCollection extends SvgElement
{
	////////////////////////////////// Instance data
	protected Vector SvgElementVector;		// We use this to add elements individually
		// and later convert it to an array during output.




	//////////// Constructors ////////////////
	// default
	/**
	*	Sets the super classes reference to the current Thread's
	*	DxfConverter object and initializes internals.
	*/
	public SvgCollection(DxfConverter dxfc)
	{
		super();
		if (dxfc == null)
			throw new NullDxfConverterReferenceException(
				"Svg object instantiation attempt: "+this.getClass());
		DxfConverterRef			= dxfc;
		SSG 					= DxfConverterRef.getStyleSheetGenerator();
		setIncludeClassAttribute(true);
		SvgElementVector 		= new Vector();
	}




	////////////// methods ///////////////////

	/**
	*	This allows a calling object to add an array of
	*	SvgReferences to the collection. The ordering of the objects in the
	*	array will be preserved.
	*	@param srs
	*/
	public void addElements(Object[] srs)
	{
		for (int i = 0; i < srs.length; i++)
			SvgElementVector.add(srs[i]);
	}

	// use this if you already have preprocessed the elements into
	// a specific order on a Vector
	/**
	*	This allows a calling object to add a Vector of
	*	additional SvgObjects to the collection.
	*	@param sos vector of numerous other svg objects.
	*/
	public void addElements(Vector sos)
	{
		// put in a try block so if the user sends an empty Vector
		// we don't crash, we just ignore.
		try{
			//for (int i = 0; i < sos.size(); i++)
			//	SvgElementVector.add(sos.get(i));
				
			SvgElementVector.addAll(sos);
		}
		catch (NullPointerException npe)
		{
			System.err.println("SvgCollection error: a null vector of "+
				"SvgElements was passed to SvgCollection.");
		}
	}

	// You can use this to post-process a layer as a collection of elements
	// after collection. This method will not output the group tag.
	/**
	*	Returns a Vector containing all of the SvgObjects currently
	*	residing within the collection.
	*	@return Vector clone of the internal collection of SvgObjects.
	*/
	public Vector getGroupElements()
	{
		return (Vector)(SvgElementVector.clone());
	}
	
	
	/**
	*	Returns a reference to the Vector containing all of the SvgObjects currently
	*	residing within the collection.
	*	@return Vector reference to the internal collection of SvgObjects.
	*/
	public Vector getGroupElementsByReference()
	{
		return SvgElementVector;
	}
	
	/** This method will return the number of elements in the collection, much
	*	like the size() method of a java lang collection.
	*/
	public int size()
	{
		return SvgElementVector.size();
	}

	/**
	*	This method is for testing whether there are elements in the collection.
	*	This prevents empty collections from being output to the final SVG file.
	*	{@link dxf2svg.util.BlockProcessor} takes advantage of this behaviour if it
	*	doesn't find any meaningful dxf group pairs in a block's entities.
	*	@return Boolean <b>True</b>, the collection is empty; <b>false</b>,
	*	the collection contains 1 or more elements.
	*/
	public  boolean isEmpty()
	{	return SvgElementVector.isEmpty();	}



	/** Allows the addition of animation to an SvgCollection object. or any of its subclasses.
	*/
	public void addAnimation(SvgAnimator sa)
	{
		System.err.println("Warning SvgCollection.addAnimation(): "+
			this.getClass().getName()+" does not support animation.");
	}
	
	
	/**	This method removes the argument SvgElement from the collection.
	*	@return true if successful.
	*	@return false if the element is not part of the collection, and the
	*	collection remains unchanged.
	*/
	public boolean remove(Object o)
	{	return SvgElementVector.remove(o);	}
	
	/**
	*	This allows a calling object to add an Object of anytype to
	*	the collection.
	*	@param o Any Java object can be added.
	*/
	public void addElement(Object o)
	{	SvgElementVector.add(o);	}
	
	// Here we fix a very rare bug where an image was output above
	// the text on the same layer. If we have such a condition make
	// sure the text is output last in all layers.
	/**
	*	This allows a calling object to add an Object of anytype to
	*	the collection in any order position. This gives the caller flexibility
	*	to place object 
	*	@param index desired location in the collection.
	*	@param o Any Java object to be added to the collection.
	*	@since 1.10
	*/
	public void addElement(int index, Object o)
	{	
		if (index < 0)
		{
			SvgElementVector.add(0, o);
		}
		else if (index >= SvgElementVector.size())
		{
			SvgElementVector.add(o);
		}
		else
		{
			SvgElementVector.add(index, o);
		}	
	}	
	
	/**	Returns the element of the vector at index <em>i</em>.
	*	@throws IndexOutOfBoundsException if the index is out of range.
	*	@return Object at index i.
	*/
	public Object get(int i)
	{	return SvgElementVector.get(i);	}
	
	
	/**	This method will perform searches within the textual content of 
	*	the Svg file.
	*	<P>
	*	This method works by searching for content within a collection with the help
	*	of {@link java.util.regex.Pattern}. This pattern is a regular expression as 
	*	described in Pattern's documentation.
	*	<P>
	*	Searches are performed recursively on SvgText objects, that is if the collection
	*	contains collections, they too will be searched.
	*	<P>
	*	Returns the number of patterns matched.
	*	<P>
	*	The Vector v is where you will find your matches if any. If there was no content
	*	that matched the argument pattern the Vector will remain unchanged. If a match
	*	was found, any content the Vector may have held will be replaced with matched content.
	*	If the Vector is null then it is assumed that the user wants to just test for
	*	the presents of content described by the Pattern and no matches are returned.
	*	<P>
	*	The Pattern argument is the compiled regular expression that you use as 
	*	the basis of your search. 
	*/
	public int searchContent(Vector v, Pattern p)
	{
		int numCount = 0;
		
		for (int i = 0; i < SvgElementVector.size(); i++)
		{
			Object testO = (Object)SvgElementVector.get(i);
			if (testO instanceof SvgText)
			{
				SvgText sText =  (SvgText)SvgElementVector.get(i);
				if (sText.find(p) == true)
				{
					if (v != null)
					{
						v.add(sText.getString());
					}
					numCount++;
				}  // end if			
			} 
			else if (testO instanceof SvgCollection)
			{
				Integer count = searchContent(v,p,testO,new Integer(numCount),false);
				numCount += count.intValue();
			} // else if
		} // end for.
		
		return numCount;
	}
	
	
	
	/**	This method will perform searches within the textual content of 
	*	the Svg file and return references to all the elements found.
	*	<P>
	*	This method works by searching for content within a collection with the help
	*	of {@link java.util.regex.Pattern}. This pattern is a regular expression as 
	*	described in Pattern's documentation.
	*	<P>
	*	Searches are performed recursively on SvgText objects, that is if the collection
	*	contains collections, they too will be searched.
	*	<P>
	*	Returns the number of patterns matched.
	*	<P>
	*	The Vector v is where you will find your matches if any. If there was no content
	*	that matched the argument pattern the Vector will remain unchanged. If a match
	*	was found, any content the Vector may have held will be replaced with matched content.
	*	If the Vector is null then it is assumed that the user wants to just test for
	*	the presents of content described by the Pattern and no matches are returned.
	*	<P>
	*	The Pattern argument is the compiled regular expression that you use as 
	*	the basis of your search. 
	*/
	public int searchForElements(Vector v, Pattern p)
	{
		int numCount = 0;
		
		for (int i = 0; i < SvgElementVector.size(); i++)
		{
			Object testO = (Object)SvgElementVector.get(i);
			if (testO instanceof SvgText)
			{
				SvgText sText =  (SvgText)SvgElementVector.get(i);
				if (sText.find(p) == true)
				{
					if (v != null)
					{
						v.add(sText);
					}
					numCount++;
				}  // end if			
			} 
			else if (testO instanceof SvgCollection)
			{
				Integer count = searchContent(v,p,testO,new Integer(numCount),true);
				numCount += count.intValue();
			} // else if
		} // end for.
		
		return numCount;
	}
	
	
	
	
	// We pass an Integer object to retain persistance of the number of matches found
	// between two different methods without using a global scoped variable that has
	// to be reset and managed.
	/** Recursive search method. This method is not meant to be called by client objects,
	*	but acts a the recursive version of the {@link #searchContent} method.
	*/
	protected Integer searchContent(
		Vector v,
		Pattern p,
		Object o,
		Integer prevMatchCount,
		boolean isElementSearch)
	{
		int numCount = prevMatchCount.intValue();
		
		// we assume o is a SvgCollection because the parent caller has already done
		// type checking.
		SvgCollection svgC = (SvgCollection)o;
		for (int i = 0; i < svgC.size(); i++)
		{
			Object testO = (Object)svgC.get(i);
			if (testO instanceof SvgText)
			{
				SvgText sText =  (SvgText)svgC.get(i);
				if (sText.find(p) == true)
				{
					if (v != null)
					{
						if (isElementSearch)
						{
							v.add(sText);
						}
						else
						{
							v.add(sText.getString());
						}
					}
					numCount++;
				}  // end if			
			} 
			else if (testO instanceof SvgCollection)
			{
				Integer count = searchContent(v,p,testO, new Integer(numCount), isElementSearch);
				numCount += count.intValue();
			} // else if
		} // end for.
		
		return new Integer(numCount);
	}
	
	

	/**
	*	Outputs a formatted String, collection of SvgObjects.
	*/
	public String toString()
	{
		int GroupSize = SvgElementVector.size();
		// First we check to see if we even have any data to display
		// We do this because some layers contain no data and if you
		// continue with the iteration on an empty Vector you will
		// throw a NullPointerException. It is completely legit to
		// be required to handle empty layers.
		if (GroupSize < 1)
			return "";

		// If we made it here we can continue safely
		StringBuffer OutputString = new StringBuffer();
		OutputString.append("<");
		// now the type or tag of this object.
		OutputString.append(getType());
		// This method appends a StringBuffer to a StringBuffer
		// which was only introduced in Java 1.4 but it saves
		// us a toString() conversion call.
		OutputString.append(getAttributes());
		OutputString.append(">\n");

		for (int i = 0; i < GroupSize; i++)
		{
			OutputString.append("\t"+(SvgElementVector.get(i)).toString()+"\n");
		}

		// closing tag.
		OutputString.append("</"+getType()+">");

		return OutputString.toString();
	}
}