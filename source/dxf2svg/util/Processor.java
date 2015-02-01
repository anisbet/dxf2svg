
/****************************************************************************
**
**	FileName:	Processor.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Abstract encapsulation of Processor objects. This provides
**				the framework and standard variable names to be used to
**				make processors.
**
**	Date:		August 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.02 - September 9, 2002
**				0.03 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				1.0  - April 15, 2005 added process( DxfElementPair pair ).
**				2.0  - April 22, 2005 Reworked the Processors for flyweight implemenation
**				of DxfElementPair.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import dxf2svg.DxfPreprocessor;
import dxf2svg.DxfConverter;

/**
*	This abstract class is the super class for all processors in Dxf2Svg.
*	All the processing required by the processor needs to be implemented
*	in the protected method process which takes a Vector of ElementPairs
*	as its argument. This class creates the framework and standard variable
*	names used by all processors.
*
*	@see DxfElementPair
*	@version	2.00 - April 16, 2005
*	@author		Andrew Nisbet
*/
abstract class Processor
{
	protected static boolean VERBOSE = false;// Verbosity
	protected DxfConverter DxfConverterRef; // Reference to the conversion context.

	/** Sets <I>VERBOSE</I> mode depending on user preferences set in Dxf2Svg,
	*	instantiates various insundry variables.
	*/
	public Processor()
	{
		VERBOSE = DxfPreprocessor.verboseMode();
	}

	
	/** Processes the DxfElementPairs one-at-a-time.
	*/
	public abstract void process( DxfElementPair pair );
	
	/** Calling this method halts the processor, finalizes the current object
	*	being processed.
	*/
	public void halt()
	{
		finishObject();
	}
	
	/** Finalizes the current object being processed.
	*/
	protected abstract void finishObject();

}