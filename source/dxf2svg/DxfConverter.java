
/****************************************************************************
**
**	FileName:	DxfConverter.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Coordinates the conversion process, acting as a buffer between
**				the DxfParser and the SvgBuilder classes
**
**	Date:		January 7, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - November 6, 2002 Removed interface constants and placed
**				them within the class a-la-Effective Java Programming.
**				1.02 - March 30, 2004 Added functionality that checks each svg 
**				element for modified attributes.
**				1.5 - July 7, 2004 Moved JavaScript functioning from SvgBuilder.
**				1.51 - January 7, 2005 Added method to return current file name
**				called oddly enough, getFileName().
**				1.52 - February 14, 2005 BUG FIX Fixed a bug that images in blocks threw 
**				null pointer exceptions. 
**				Fixed by processing ObjectProc.process(DxfSections[DxfParser.OBJECTS]);
**				before blocks. Also added testing for SvgText so those objects
**				would be output on the layer last.
**				1.53 - February 24, 2005 Added method to get path of file being
**				converted.
**				1.54 - March 24, 2005 Added Search Strategy for cross-sheet wire linking.
**				1.55 - March 30, 2005 Added Search Strategy for part numbers.
**				2.0  - April 22, 2005 Radical changes to processing, removed Vector 
**				processing of DxfElementPair and replaced it with a single fly weight
**				DxfElementPair.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg;

import java.util.*;		// for Vectors and Iterators handling
import java.io.*;
import dxf2svg.svg.*;
import dxf2svg.util.wiretrace.CrossSheetWireIDs;
import dxf2svg.util.svglink.PartNumberLinks;
import dxf2svg.util.*;
import dxf2svg.animation.*;
import dxf2svg.sally.*;

/*
*	This class acts as a buffer between DxfParser which doesn't need
*	to know why we need the information and SvgBuilder which is a dumb class that needs
*	coordination.  All converters using DxfParse should be set up in a similar manner
*	to take advantage of the DXF data.  I have added this extra class to modularize the
*	whole project more for re-usability's sake.
*/

/**
*	This class actually coordinates the building of the SVG graphic.
*
*	It is an intermediary between DxfParser and SvgBuilder. It gets the
*	Vector data from the Parser and creates the rest of the objects needed
*	to finish the conversion, namely the Processors, StyleSheetGenerator
*	and SvgBuilder.
*
*	@version	1.52 - February 14, 2005
*	@author		Andrew Nisbet
*/
public final class DxfConverter
{
	/*	Instance Data for this class
	*
	*	Create objects for each of the sections ready to receive any data
	*	This section provides the skeleton of a more comprehensive DXF
	*	reader should you want to develope one.
	*/


	private static boolean VERBOSE;		// Standard for many classes
	private static int MAKE_CSS;		// we need this to see if we
	// Controls how entity grouping occurs with the svg file.
	//private int ENTITIES_ON_LAYERS_GROUPED_BY = Dxf2SvgConstants.GROUP;
		// cut the process short by just outputting the Style Sheet.


	// We only need this Vector back again for future intelligence processing.
	// the other vectors interact with objects on their own.
	private Vector vEntities;	// entities info
	private Vector vLayers;		// A vector of layer groups (and their entities).
	private Vector vBlocks;		// A Vector for all the blocked entities.
	/////////////////////// need this here. /////////////////////////
	private Vector vDxfObjects;	// A Vector returned from the DxfObjectProcessor.
	private Vector vPatterns;	// A Vector to hold hatch patterns.

	private StyleSheetGenerator SSG;
	private SvgBuilder svgBuilder;
	private SvgUtil svgUtilities;
	private AnimationEngine ae;		// For discribing animation.
	private SvgAnimationLanguage sal;// animation/automation engine reference.
	
	private String currentFileName; // This is required for logging purposes of global objects like
	// SAL. It has no way of knowing what file is being processed but it can query this object's
	// getFileName() method to provide a more meaningful message.
	private String currentFile;		// As above but the entire path of the file.
	
	// Constructors
	/** The coordination of the production of SVG graphics is done within
	*	this class.
	*
	* 	This class encapsulates vectors of the 7 different parts of a DXF
	*	file and hands them off to the appropriate Processor objects.
	*/
	public DxfConverter(String path)
	{
		init(path);
	}
	
	/** Takes a reference to the SvgAnimationLanguage as an argument
	*	to allow for conditional application of attributes to specific 
	*	SvgElements (if required).
	*/
	public DxfConverter(SvgAnimationLanguage sal, String path)
	{
		this.sal = sal;
		init(path);
	}
	
	private void init(String path)
	{
		System.out.println("DxfConverter: "+path);
		currentFileName = new File(path).getName();
		currentFile = path;
		// Set switches
		VERBOSE  	= DxfPreprocessor.verboseMode();
		MAKE_CSS 	= DxfPreprocessor.cssMode();



		vDxfObjects = new Vector();
		vEntities   = new Vector();
		vBlocks     = new Vector();
		ProcessorManager processorManager = new ProcessorManager( 
			this, 
			vDxfObjects, 
			vEntities, 
			vBlocks
		);
		
		
		
		
		// Make an array of Vectors for each of the sections to
		// collect from the DXF and then instantiate the DxfParser.
		DxfParser parser = new DxfParser(path, processorManager, VERBOSE);
		
		try
		{
			parser.parse();
		}
		catch (IOException e)
		{
			System.err.println("DxfConverter error: error reading target file \""+path+"\"");
			return;
		}
		
		
		
		// This object is responsible for the output of the elements to
		// a well formatted instance of an XML document.
		svgBuilder 		= new SvgBuilder(this);
		

		if ( MAKE_CSS == Dxf2SvgConstants.CSS_ONLY )
		{
			makeStyleSheetOnly();
			return;
		}


		//////////////////////////////////////////////////
		//	6) Intelligence handling functions go here. //
		//////////////////////////////////////////////////
		// Here is where we organize the svg output.
		// If the objects are to be grouped in the Svg by <g> tags
		// then call putEntitiesOnlyLayers(). The advantage of this
		// method is the file is smaller and easier to read. It is
		// the default for files that don't require any special
		// animation.
		outputLayersByGroup();
		
		
		//////// Search for, and apply wire IDs (if necessary). ////////
		if ( currentFileName.startsWith( "w" ) || currentFileName.startsWith( "W" ) )
		{
			System.out.println( "searching wiring diagram for cross links..." );
			new CrossSheetWireIDs( this, vLayers );
			System.out.println( "...done." );
		}
		
		//////// Search for part numbers (if necessary). ////////
		if ( DxfPreprocessor.isPartsListFigure() )
		{
			if ( currentFileName.startsWith( "f" ) || currentFileName.startsWith( "F" ) )
			{
				System.out.println( "searching figure for part numbers..." );
				new PartNumberLinks( vLayers, this );
				System.out.println( "...done." );
			}
		}		
		
		
		
		if (sal != null && sal.hasModifiedAttributes())
		{
			// pass sal the object for modification (if necessary).
			sal.modifyAttributes(vBlocks);
			sal.modifyAttributes(vPatterns);
			// Modify root element if nec. We have to do this this way because 
			// it seems the simplest way for SAL to get the SvgBuilder object
			// for this conversion context.
			sal.modifyAttributes(svgBuilder, SvgAnimationLanguage.ADD_HEAP);
			sal.modifyAttributes(vLayers);
		}
		
		
		




		//	7) Now the enviornment is populated with everything you need
		// to write the SVG	so lets do that starting with the header...
		// This is done in the SvgBuilder object.
		// svgBuilder.makeSvgHeader();
		//	8) Now output the preliminary <defs> and <symbol> objects
		svgBuilder.submitSvgSymbols(vBlocks);
		svgBuilder.submitSvgPatterns(vPatterns);
		
		// 9) Now we add javascript removing this burden from the SVG Builder and 
		// allow finer grained control over which and how javascript functions are
		// incorporated into this file.
		prepAndApplyJavaScript();
		
		
		
		
		try
		{
			svgBuilder.writeSvgObjectsToFile(vLayers);
		}
		catch (IOException e)
		{
			System.err.println("DxfConverter Error: an IO error has occured while ouputting "+
				"elements from SvgBuilder."+e);
			System.err.println(e.getStackTrace());
		}

	} // end of the constructor.
	

	/** Used by the {@link dxf2svg.util.ProcessorManager} to set the SvgUtil object
	*	after its creation.
	*/
	public void setSvgUtility( SvgUtil svgUtility )
	{
		svgUtilities = svgUtility;
	}
	
	/** Used by the {@link dxf2svg.util.ProcessorManager} to set the style sheet generator
	*	after its creation.
	*/
	public void setStyleSheetGenerator( StyleSheetGenerator ssg )
	{
		SSG = ssg;
	}




	//**************** Methods *************************/

	/** This method allows any object to query the current conversion file name. This 
	*	is useful for objects like {@link dxf2svg.animation.AnimationEngine} which is 
	*	persistant to all files during the conversion but needs to flag a file by name
	*	to the logger object.
	*/
	public String getFileName()
	{
		return currentFileName;
	}
	
	/** This method allows any object to query the current conversion file path. This 
	*	is useful for objects like {@link dxf2svg.svg.DxfImageObjectReference} which 
	*	uses it to find raster images for embedding into the SVG.
	*/
	public String getFilePath()
	{
		return currentFile;
	}
	
	/** This method will apply any and all javascript to simplify operations within
	*	{@link dxf2svg.SvgBuilder} and remove hard coded javascript that may become
	*	outdated or need modification.
	*/
	private void prepAndApplyJavaScript()
	{
		//////////////////// Handle Notes ////////////////////
		// Here we signal any SvgNoteManager Object that we are ready for notes if there
		// are any.
		if (DxfPreprocessor.takeNotes() == true)
		{
			// This is a call back function that returns notes for the current file 
			// to be converted.
			DxfPreprocessor.getNotes(svgBuilder);
		}
		
		//////////////////// Handle Language (if required) ///////////////////////////
		// Here we actually enter a the functions we want. If the HTML wrapper flag is
		// set then we have to include it now and then look for a file that may contain
		// some scripts... like JS_SRC_PATH, for example.
		if (SSG.isLangSwitchRequired() == true && 
			DxfPreprocessor.suppressBoilerPlateJavaScript() == false)
		{
			svgBuilder.addJavaScript(getSwitchLanguageScripts());
			// We also now control the Attribute for <SVG> onload and these 
			// scripts use those so add it here.
			Attribute onload = new Attribute("onload","init()");
			svgBuilder.addAttribute(onload);
		}
		
		
		//////////////////// Handle User Defined JS //////////////////////////////////
		// Include any JavaScript functions that were read from the config file.
		if (DxfPreprocessor.hasJavaScript() == true)
		{
			Vector jsStack = DxfPreprocessor.getJavaScript();
			Iterator jsStackIt = jsStack.iterator();
			while(jsStackIt.hasNext())
			{
				svgBuilder.addJavaScript((String)jsStackIt.next()+"\n");
			}
		}
	}
	
	/**	This method returns the boiler plate language switching scripts if required.
	*/
	private String getSwitchLanguageScripts()
	{
		StringBuffer lang = new StringBuffer();

		lang.append("var englishLayer = svgDocument.getElementById(\"st"+
			TableLayer.getLanguageLayerName(Dxf2SvgConstants.ENGLISH)+"\");\n");
		lang.append("var frenchLayer = svgDocument.getElementById(\"st"+
			TableLayer.getLanguageLayerName(Dxf2SvgConstants.FRENCH)+"\");\n");
		lang.append("\n");
		lang.append("function init() {\n");
		lang.append("\tparent.changeLanguage = svgChangeLanguage;\n");
		lang.append("\tenglishLayer.setAttribute(\"visibility\", \"visible\");\n");
		lang.append("\tfrenchLayer.setAttribute(\"visibility\", \"hidden\");\n");
		lang.append("}\n");
		lang.append("\n");
		// Remainder of the language switching function.
		lang.append("function svgChangeLanguage(language){\n");
		lang.append("\tif (language == 0){\n");
		lang.append("\tenglishLayer.setAttribute(\"visibility\", \"visible\");\n");
		lang.append("\tfrenchLayer.setAttribute(\"visibility\", \"hidden\");\n");
		lang.append("\t} else {\n");
		lang.append("\tenglishLayer.setAttribute(\"visibility\", \"hidden\");\n");
		lang.append("\tfrenchLayer.setAttribute(\"visibility\", \"visible\");\n");
		lang.append("\t}\n");
		lang.append("}\n");

		return lang.toString();
	} // getSwitchLanguageScripts

	
	/** This method returns the SvgBuilder object.
	*
	*	@return {@link SvgBuilder}.
	*/
	public SvgBuilder getSvgBuilder()
	{	return svgBuilder;	}	
	
	
	
	
	
	/** This method allows a hatch pattern definition to be added
	*	to the list of other objects to be output in the &lt;defs&gt;
	*	section of the SVG file (along with symbols). Duplicate hatch
	*	patterns are searched for to optimize the database of patterns,
	*	and patterns that have the same name but different data are
	*	renamed to avoid name conflicts in the SVG file.
	*/
	public void addHatchPattern(SvgHatch hatch, SvgHatchPattern pattern)
	{
		if (vPatterns == null)
		{
			vPatterns = new Vector();
			//vPatterns.add(pattern);
		}

		int size = vPatterns.size();

		for (int i = 0; i < size; i++)
		{
			// If there is an entry in the patterns database that matches
			// the argument pattern...
			SvgHatchPattern testHP = (SvgHatchPattern)vPatterns.get(i);
			if (testHP.equals(pattern))
				return;		// ... don't add it to the database.

			// To get here you have to be unequal hatchpatterns.
			// but you could still have a pattern name that is the same
			// so check for similarity of names and change them if need be.
			//
			// data within the pattern is not duplicated in the database
			// so check to see if the names are the same; if so this
			// pattern has to have a unique name AND it's associated hatch
			// has to be updated or 'synchronized' to the pattern name.
			if (testHP.getObjIDUU().equals(pattern.getObjIDUU()))
			{
				String newName = pattern.getSychronizedPatternName();
				hatch.setPatternName(newName);
			}
		}

		vPatterns.add(pattern);
	}

	/** Used by SvgImage to get a reference to its specific image handle (dxf hex code handle).
	*	The handle is used by the SvgImage to locate individual images (if there are more than
	*	one) so that it can base64 encode the image into the SVG.
	*/
	public synchronized DxfImageObjectReference getImageObjectReference(String imageHandle)
	{	
		
		if (vDxfObjects == null)
		{
			System.err.println("DxfConverter: vDxfObjects is null.");
			System.exit(-1);
		}
		Iterator dxfObjIt = vDxfObjects.iterator();
		while (dxfObjIt.hasNext())
		{
			Object o = dxfObjIt.next();
			if (! (o instanceof DxfImageObjectReference))
			{
				continue;
			}
			
			String currHandle = ((DxfImageObjectReference)o).getHardReference();
			if (currHandle.equalsIgnoreCase(imageHandle))
			{
				return (DxfImageObjectReference)o;
			}	
		}
		System.err.println( "DxfConverter: Found no image with handle '"+imageHandle+"'." );
		DxfPreprocessor.logEvent("DxfConverter","Found no image with handle '"+imageHandle+"'.");
		
		return null;
	}





	/** This method returns the StyleSheetGenerator currently being used by
	*	this thread. It is mainly used SvgObjects, in their constructors, to
	*	allow them to determine the current Thread's StyleSheetGenerator.
	*
	*	@return {@link StyleSheetGenerator}.
	*/
	public StyleSheetGenerator getStyleSheetGenerator()
	{	return SSG;	}







	/** This method returns the SvgUtil currently being used by
	*	this thread.
	*
	*	@return {@link SvgUtil} This object encapsulates all the Dxf
	*	conversion utilities.
	*/
	public SvgUtil getSvgUtil()
	{	return svgUtilities;	}











	// This method returns the layer list with any ordering if required.
	// It takes the requested layer ordering and checks for such values in the
	// master list. If any of the requested layers are not present in the master
	// list of layers that layer is ignored and the search continues with the next value
	// on the list.
	private Vector getLayerList()
	{
		// We first need the total list of valid layer names for this DXF.
		Vector masterList = new Vector();
		SSG.getLayerNames(masterList);

		Vector orderedLayerList = DxfPreprocessor.getLayerOrder();	// list of layers, in order.
		// If there is no specification for layer ordering we will return
		// the list from SSG untouched.
		if (orderedLayerList == null)
			return masterList;

		int listPlacement = DxfPreprocessor.getLayerOrderPlacement();


		Vector vSortedLayers = new Vector(); 	// list of all valid layer names
		String layerNameOrdered;				// name from the ordered list
		String layerNameMaster;					// name from the master list
		// to get here the list must have atleast one layer name.
		// Now we must check to see if the list contains any members of the
		// requested layer ordering list and put them in the appropriate order.
		if (listPlacement == Dxf2SvgConstants.HEAD)
		{
			for (int i = 0; i < orderedLayerList.size(); i++)
			{
				layerNameOrdered = (String)orderedLayerList.get(i);
				// ... looking for it's match in the master layers list of existing layers.
				for (int j = 0; j < masterList.size(); j++)
				{
					layerNameMaster = (String)masterList.get(j);
					if (layerNameOrdered.equals(layerNameMaster))
					{
						// Take the exact name from the master list.
						vSortedLayers.add(layerNameMaster);
						// remove the name from the master list
						masterList.removeElementAt(j);
						break;
					}	// end if
				}	// end for
			}	// end for

			// Now you have to paste the remaining values from the master list.
			vSortedLayers.addAll(masterList);

		}
		else	// if(listPlacement == TAIL)
		{
			// remove all elements in the masterlist that match the ordered list.
			for (int i = 0; i < orderedLayerList.size(); i++)
			{
				layerNameOrdered = (String)orderedLayerList.get(i);
				for (int j = 0; j < masterList.size(); j++)
				{
					layerNameMaster = (String)masterList.get(j);
					if (layerNameOrdered.equals(layerNameMaster))
					{
						// remove it from the master list
						masterList.removeElementAt(j);
						// and add it to the return vector.
						vSortedLayers.add(layerNameMaster);
						break;
					}
				}
			}
			// now we have a pruned masterlist and the return vector contains all
			// the layer names that were found in the master list.
			// All we have to do is to past the masterlist onto the front of the
			// final list.
			vSortedLayers.addAll(0,masterList);
		}

		//if (DEBUG)
		//{
		//	for (int i = 0; i < vSortedLayers.size(); i++)
		//		System.out.println(">>>Sorted list:"+i+" = "+(String)vSortedLayers.get(i));
		//}

		return vSortedLayers;
	}


	/**
	*	This method will group elements, that appear on the same dxf layer,
	*	into an &lt;g&gt; element. This &lt;g&gt; element will then take
	*	all common styling information on behalf of its children.
	*	<BR><BR>
	*	This method is not used if any elements are to be grouped by
	*	functional boundaries. That is if two lines light up when clicked,
	*	then they belong to the same functional group.
	*/
	private void outputLayersByGroup()
	{
		vLayers = new Vector();
		Vector layerNames = getLayerList();
		String thisLayerName;
		String objectLayerName;
		// Make an AnimationEngine 
		ae = DxfPreprocessor.getAnimationEngine();

		for (int i = 0; i < layerNames.size(); i++)
		{
			// Get the layer name for later reference.
			thisLayerName = (String)layerNames.get(i);
			// now we make SvgGroups based on these and populate the groups
			SvgLayerGroup myLayer = new SvgLayerGroup(this, thisLayerName);
			// set the SvgGroup's class.
			myLayer.setClass(thisLayerName);

			// cycle through the list of entities and
			// grab the ones whose layers match.
			for (int j = 0; j < vEntities.size(); j++)
			{
				SvgElement tmpSvgObj = (SvgElement)vEntities.get(j);
				if (tmpSvgObj.isSuppressed())
				{
					continue;
				}
				
				objectLayerName = tmpSvgObj.getLayer();
				// Our group has taken the name of the ith String on the
				// LayerNames. Compare the two to see if they are the
				// same, if they are then the add the entity to the
				// group and remove it from the vEntities Vector.
				if (objectLayerName.equalsIgnoreCase(thisLayerName))
				{
					// put the entity in the group
					
					tmpSvgObj.setIncludeClassAttribute(false);
					// This is to fix a bug where a png is 
					// over-writing text on the same layer
					// cause it just happens to be written
					// that way in the dxf. This controls 
					// so text is placed on the end of the 
					// layer and all others placed at the front
					// of the layer. The by-product of this 
					// is that all other objects will be output
					// on their layer in the reverse order 
					// of how they are on the layer in the
					// dxf. This doesn't matter unless you
					// get a png infront of your text.
					if (tmpSvgObj instanceof SvgText)
					{
						myLayer.addElement(tmpSvgObj);
					}
					else
					{
						myLayer.addElement(0, tmpSvgObj);
					}
				}

			}  // end for j = 0
			
			// In the old days we used to rely on AnimationEngine to arrange
			// for collaborating wire segments into one big polyline like this:
			//:animation set "wire"
			//{
			//    collaborate = "true"; 
			//     ...
			//}
			// We are going to do that here for a couple of reasons:
			// 1) We want to be able to herald collaborators from the command line
			// instead of just in an animation declaration.
			// 2) It makes more sense that this kind of functionality is controlled
			// by this object.
			// 
			boolean COLLABORATE = DxfPreprocessor.isCollaboratorTarget(thisLayerName);
			if (COLLABORATE)
			{
				collaborate(myLayer);
			}
			
			// Now we can add animation related to this layer, if it is just
			// layer specific, if it goes into matching objects on layers then
			// more animation could be applied once we start cycling through
			// the elements.
			////////////////////////////////////////////////////////////////
			// Think about adding animation as a separate step as we ran  //
			// into class cast exceptions with SvgAnimation objects when  //
			// processing all the SvgObjects while modifying attributes.  //
			// This is because they are derived from Object not SvgElement//
			////////////////////////////////////////////////////////////////
			if (ae != null)
			{
				ae.applyAnimation(myLayer, this); // pass conversion context aswell
			}

			// Put the group onto the layers Vector for output by
			// SvgBuilder
			// Pack the symbol and all of its SvgObjects onto the return Vector.
			// Unless, of course, there are no objects on that layer.
			if (! myLayer.isEmpty())
			{
				vLayers.add(myLayer);
			}
		}  // end for i = 0
	} // end of putEntitiesOnlyLayers()









	/** This method coordinates the grouping of line and arc segments into continuous 
	*	polylines for application of animation or JavaScript on a single element.
	* 
	*	@since 2.0 March 10, 2005.
	*/
	private void collaborate(SvgLayerGroup svgl)
	{
		// As stated before collaborators are SvgDoubleEndedGraphicElements that
		// share the same start and or end points. We will go through the list 
		// and save collaborators onto a list. To find the collaborator take a 
		// target start Point and try and find a matching point in the SvgLayer
		// group. The algorithm goes something like this.
		// 
		// 0) Get the collection.
		Vector svgLayerStack = svgl.getGroupElements();
		// We also create a place to store all of the potential matching elements
		Vector doubleEndedEleGroup = null;
		// 1) Sort the elements of the layer into a group of double ended objects for
		// further processing.
		for (int i = 0; i < svgLayerStack.size(); i++)
		{
			//////////////// change 0.03 starts here //////////////////
			
			Object testEle = svgLayerStack.get(i);
			if (! (testEle instanceof SvgDoubleEndedGraphicElement))
			{
				continue;
			}
			
			if (doubleEndedEleGroup == null)
			{
				doubleEndedEleGroup = new Vector();
			}
			
			doubleEndedEleGroup.add(testEle);
			//////////////// change 0.03 ends here //////////////////
		}
		
		// Now we can process the group of double ended elements (if there are any).
		if (doubleEndedEleGroup == null)
		{	return;	}
		
		// To get here we have to have a group of double ended graphic elements.
		// Now we are going to sequence through the list and determine if any
		// share the same start and end points. If they do they are collaborators.
		//
		// Take the first element on the list (it may be arbitrary). If no collaborators
		// are detected the item is removed from the list and left on SvgLayerGroup
		// list untouched. 
		//
		// If any collaborators are detected, place them on a temporary stack for
		// further analysis. After the entire list of elements is exhausted, place the
		// test element into a new SvgCollection element, and remove it from the original
		// SvgLayerGroup. 
		//
		// Go to the stack of collaborators and pop an element. Search the entire
		// list for additional collaborators and if none are found, place the test
		// element on the SvgCollection element created by the last step and remove
		// it from the SvgLayerGroup. If other collaborators are found, they can be
		// added to the stack.
		//
		// Continue until the stack is empty, then get the next element off the list
		// and repeat above steps.
		//
		// After the original list is empty, any remaining stack members can be flushed
		// to the last opened SvgCollection object.
		//
		/////////////////////////////////////////////////////////////////////////////////
		//
		// We have to remember the index of the current test element so
		// we can make choices about whether to include it in 
		// Sequence over all of the double ended elements.
		//Iterator it1 = doubleEndedEleGroup.iterator();
		//while (it1.hasNext())
		// Instead of a Vector let's use an array and then we can delete
		// objects (set to null) when ever we like.
		SvgDoubleEndedGraphicElement[] doubleEndedArray = 
			new SvgDoubleEndedGraphicElement[doubleEndedEleGroup.size()];
		
		// Place all the elements of the double ended vector onto an array
		// This will allow us to delete members once we have identified their
		// correct group more easily.
		doubleEndedEleGroup.toArray(doubleEndedArray);
		
		for (int j = 0; j < doubleEndedArray.length; j++)
		{
			SvgDoubleEndedGraphicElement s1 = doubleEndedArray[j];
			// we have to test to see if the element at this index is null
			// or not
			if (s1 == null)
			{
				continue;
			}
			
			// This stops this object from showing up in the animation group twice.
			// With out this the last element in an animation group was a repeat of the second
			// element. If is tested and either stays on the original group of elements or
			// gets placed on the animation stack. Either way dropping it now fixes the double
			// animation element bug.
			doubleEndedArray[j] = null;
			
			Vector thisSubGroupsEle = new Vector();
			thisSubGroupsEle = 
				findAndChainCollaborators(s1, doubleEndedArray);

			////////////////////// chain collaborators ///////////////////////////
			// to chain collaborators you must find the indices of each connected
			// segment AND all the connectors of the children elements.
			// To do that traverse each member of the stack and match those elements
			// with other elements on the SvgLayerGroup list.
			// So now let's get each member of the stack and find there collaborators
			// and place them on the stack as well.
			// But don't do anything if the stack is null.
			if (! thisSubGroupsEle.isEmpty())
			{
				// Create a new SvgDoubleEndedElementAggregate
				SvgDoubleEndedElementAggregate aggregate = null;
				if (thisSubGroupsEle.size() > 0)
				{
					aggregate = new SvgDoubleEndedElementAggregate(this);
				}
				//SvgLayerGroup newAnimGroup = new SvgLayerGroup(dxfc);
				for (int i = 0; i < thisSubGroupsEle.size(); i++)
				{
					////////////////// 0.03 - September 1, 2004 //////////////////
					SvgDoubleEndedGraphicElement sde = (SvgDoubleEndedGraphicElement)
						thisSubGroupsEle.get(i);
					// add the element aggregate.
					aggregate.add(sde);
					// remove it from the original group or they will be doubled.
					svgl.remove(sde);	
				}					
				// Now add the animation for the SvgLayerSubGroup
				//aggregate.addAnimation(vAnim);
				aggregate.setObjID(Dxf2SvgConstants.WIRE_RUN_ID_VALUE);
				
				svgl.addElement(aggregate);
				////////////////// 0.03 - September 1, 2004 //////////////////;					
			}
			// go around again for the remainder of the elements on the double ended
			// element array.
			if (DxfPreprocessor.verboseMode())
			{
				System.out.println("==========================");
			}
		}	// end for
	}


	/** 
	*	@since 2.0 March 10, 2005.
	*/
	private Vector findAndChainCollaborators
	(
		SvgDoubleEndedGraphicElement s1,		// First member that initiates search
		SvgDoubleEndedGraphicElement[] arr		// Array of other double ended elements from layer
	)
	{
		Stack stack = new Stack();
		// Now all lines on the layer have take animation - not just the ones that have 
		// collaborators. We are going to push the source search object onto the stack 
		// and then search the array of other elements. If we don't find any other objects
		// with the same start and end point, this object has no collaborators and thus 
		// takes the animation target onto itself.
		stack.push(s1);
		// Proceed over the rest of the array looking for other objects that match
		// start and end points. 

		for (int i = 0; i < arr.length; i++)
		{
			SvgDoubleEndedGraphicElement s2 = arr[i];
			// we have to test to see if the element at this index is null
			// or not
			if (s2 == null)
			{
				continue;
			}
			// now skip the inevitable comparison of one object to itself.
			if (s1 == s2)
			{
				continue;
			}
			
			if (s1.shareStartOrEndPoint(s2) == true)
			{
				stack.push(s2);
				arr[i] = null;
			}	// end if
		}	// end for
		
		Vector completeSetOfElements = new Vector();
		// Now after the preliminary search, the index object s1 has no collaborators
		// there should be just one item on the stack. Let's skip the search for more
		// collaborators and jsut add the animation to this object.
		if (stack.size() == 1)
		{
			completeSetOfElements.add(stack.pop());
		}
		else
		{
			chainCollaborators(stack, completeSetOfElements, arr);
		}
		
		return completeSetOfElements;
	}	// end findAndChainCollaborators()
	
	
	
	/** This method takes a stack reference that contains at least one collaborator,
	*	an empty vector for the storing of chained elements and the array of 
	*	double ended elements that need to be searched. One of the members of stack
	*	is removed and compared to the remainder of the items on the double ended 
	*	element array. If they share a start or end point it is also a collaborator,
	*	and gets put on the stack, and it is also put on the Vector 'eles'. 
	*	The process is repeated until the stack is empty.
	* 
	*	@since 2.0 March 10, 2005.
	*/
	private void chainCollaborators(
		Stack s,
		Vector eles,
		SvgDoubleEndedGraphicElement[] array)
	{
		while (! s.empty())
		{
			SvgDoubleEndedGraphicElement s1 = (SvgDoubleEndedGraphicElement)s.pop();
			// s1 is a collaborator so add it to the complete set.
			eles.add(s1);
			for (int i = 0; i < array.length; i++)
			{
				SvgDoubleEndedGraphicElement s2 = array[i];
				// we have to test to see if the element at this index is null
				// or not
				if (s2 == null)
				{
					continue;
				}
				// now skip the inevitable comparison of one object to itself.
				if (s1 == s2)
				{
					continue;
				}

				if (s1.shareStartOrEndPoint(s2) == true)
				{
					s.push(s2);
					// remove the found collaborator from the search array.
					array[i] = null;
				}	// end if
			}	// end for			
		}	// end while
	}	// end chainCollaborators()




	/**	This method is called when the user requires a Cascading
	*	Style Sheet is the only output.
	*/
	private void makeStyleSheetOnly()
	{	SSG.makeExternalStyleSheet();	}


}	// end of DxfConverter