
/****************************************************************************
**
**	FileName:	AnimationEngine.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This is a singleton class pattern object.
**				Controls the animation process. It looks for target elements
**				that match the targets described in the config.d2s file, 
**				spawns and populates the correct animation object and applies
**				it to the target element(s).
**
**	Date:		June 13, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - June 13, 2003
**				0.02 - July 23, 2004 Added wire run attribute value to dynamically created wire 
**				run groups.
**				0.03 - September 1, 2004 Added functionality to convert collaborated elements
**				into complete paths.
**				0.04 - November 16, 2004 Added check for empty SVG layer group in
**				method applyAnimation() to avoid NullPointerExceptions on empty layers.
**				0.05 - January 7, 2005 Added a call to getFileName() from applyAnimation()
**				method for logging purposes.
**				0.5  - March 3, 2005 applyAnimation() no longer places ganged wires <path>s
**				in their own group tag <g>. This is not necessary and matches the
**				way that wires are handled. It more importantly, makes javascript
**				DOM manipulation of the document tree work in a consistant manner with
**				wires.
**				0.6 - March 10, 2005 Moved the collaborate functionality to the 
**				DxfConverter object.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.animation;

import java.util.*;
import dxf2svg.sally.*;
import dxf2svg.svg.*;			// for SvgLayerGroup
import dxf2svg.*;
import dxf2svg.util.Dxf2SvgConstants;

/**	This class controls the animation process. It looks for target elements
*	that match the targets described in the config.d2s file, 
*	spawns and populates the correct animation object and applies
*	it to the target element(s).
*
*	@version 	0.6 - March 10, 2005
*	@author		Andrew Nisbet
*/
public final class AnimationEngine
{
	private static AnimationEngine aeInstance = null;
	private static SvgAnimationLanguage sal = null;
	private boolean hasAnimation;
	private String[] keys;
	
	protected AnimationEngine(SvgAnimationLanguage s)
	{	
		this.sal = s;
		
		// Now register the content of the animation.
		// SET					= 1;
		// ANIMATE				= 2;
		// ANIMATE_COLOUR		= 4;
		// ANIMATE_MOTION		= 8;
		// ANIMATE_TRANSFORM	= 16;
		if (sal.hasContent(SvgAnimationLanguage.ANIMATION_HEAP))
		{
			hasAnimation = true;
			keys = sal.getKeys(SvgAnimationLanguage.ANIMATION_HEAP);
		}
	}
	
	/** Returns a singleton instance of AnimationEngine.
	*/
	public static AnimationEngine getInstance(SvgAnimationLanguage sal)
	{
		if (aeInstance == null)
		{
			aeInstance = new AnimationEngine(sal);
		}
			
		return aeInstance;
	}

	
	
	
	
	/** This method will apply animation to a layer if any has been
	*	written in the config.d2s script file. If the layer has no 
	*	animation the Collection is left unchanged.
	*	@param svgl the group to which the animation will be applied.
	*	@param dxfc reference for creating animation sub-groups if required.
	*/
	public synchronized void applyAnimation(SvgLayerGroup svgl, DxfConverter dxfc)
	{
		// If you don't check for a null or empty layer group unpurged dxf files
		// will throw a NullPointerException in the animation engine.
		if (svgl == null || svgl.isEmpty()){
			// I have been getting false hits and I don't have time to diagnose 
			// so I have commented this warning out.
			//DxfPreprocessor.logEvent("SvgAnimationEngine.applyAnimation() - "+
			//	dxfc.getFileName(),"layer '"+svgl.getLayer()+
			//	"' is empty; the drawing should be purged.");
			return;
		}
		
		// We need the name of the layer to see if it is a target for
		// animation.
		String lookFor = svgl.getAbsoluteClass();
		if (DxfPreprocessor.verboseMode() == true)
		{
			System.out.println("looking for animation for: "+lookFor);
		}
		// Now we have to query the Sally object for animation for this layer.
		Vector vAnim = new Vector();
		vAnim = sal.getAnimation(lookFor);
		
		if (vAnim == null)
		{
			return;
		}

		if (vAnim.size() < 1)
		{
			if(DxfPreprocessor.debugMode() == true)
			{
				System.out.println("There are no animation objects for "+lookFor);
			}
			return;
		} // end if
		else
		{
			if (DxfPreprocessor.verboseMode() == true)
			{
				System.out.println("There are "+vAnim.size()+" animation objects.");
			}
		}
		
		// Before we get to the checking for collaborators, we are going to check
		// for gang keyword which will indicate that <path> elements are in fact
		// ganged wires and the animation mentioned here must apply to all the paths
		// individually. To do that we need to make sub groups and place the ganged 
		// wires into it. We identify a gang as any <path> element on this layer.
		// All other elements could be collaborators or unrelated elements.
		//
		// This adds animation to individual elements of a layer marked as a 'gang'ed layer.
		boolean GANG = DxfPreprocessor.isGangingTarget(lookFor);
		if (GANG)
		{
			// Let's get the entire layer group of elements and see if we can find some <path>
			// or <polyline> elements.
			Vector svgLayerStack = svgl.getGroupElements();
			Vector removeElements = null;
			for (int i = 0; i < svgLayerStack.size(); i++)
			{
				SvgDoubleEndedGraphicElement testEle = null;
				// 2) Determine if it is a SvgPolyLine. 
				//******************************
				//**  Note: this will work on **
				//**  <polyline>s and <path>s **
				//******************************
				try
				{
					testEle = (SvgDoubleEndedGraphicElement)svgLayerStack.get(i);
				}
				catch(ClassCastException e)
				{
					// The object on svgLayerStack was not an SvgPolyLine
					continue;
				} 
				
				
				// We don't want solids or polygons. They can't be ganged wires.
				if (testEle.getType().equals("path") || testEle.getType().equals("polyline"))
				{
					// remove it from the original group or they will be doubled.
					if (removeElements == null)
					{
						removeElements = new Vector();
					}
					removeElements.add(testEle);
					svgl.remove(testEle);					
				}  // end if
			}  // end For
			
			// Now iterate over the removed elements and add their animation.
			for (int j = 0; j < removeElements.size(); j++)
			{
				
				SvgDoubleEndedGraphicElement o = (SvgDoubleEndedGraphicElement)(removeElements.get(j));

				for (int k = 0; k < vAnim.size(); k++)
				{
					o.addAnimation((SvgAnimator)(vAnim.get(k)));
				}
				o.setObjID(Dxf2SvgConstants.WIRE_RUN_ID_VALUE);
				svgl.addElement(o);
			}  // end for
			
		} // end if GANG
		
		
		
		
		
		// Is the object represented by the name 'lookfor' a target for searching for
		// line segments that share the same start or end points?
		boolean COLLABORATE = DxfPreprocessor.isCollaboratorTarget(lookFor);
		
		// This adds animation to individual elements of a layer marked for 'collaborate-tion'.
		if (COLLABORATE == true)
		{
			Vector svgLayerStack = svgl.getGroupElements();
			for (int i = 0; i < svgLayerStack.size(); i++)
			{
				SvgElement svgElement = (SvgElement)(svgLayerStack.get(i));
				
				for (int k = 0; k < vAnim.size(); k++)
				{
					svgElement.addAnimation((SvgAnimator)(vAnim.get(k)));
				}
				
				svgElement.setObjID(Dxf2SvgConstants.WIRE_RUN_ID_VALUE);				
				
			}	// end for
		}	
		else	// end if /*(COLLABORATE == false)*/
		{
			// Ok now that's settled, let's unpack the vector of animation objects and add the 
			// contents to this layerGroup.
			// We don't want to apply animation to a group if there is a gang; each sub-group
			// already has this.
			if (GANG == false)
			{
				svgl.addAnimation(vAnim);
			}
		}	// end if
	} ///////////////////////// end applyAnimation /////////////////////////////////////////
	
	
	
	
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
	
	
	
	// This method takes a stack reference that contains at least one collaborator,
	// an empty vector for the storing of chained elements and the array of 
	// double ended elements that need to be searched. One of the members of stack
	// is removed and compared to the remainder of the items on the double ended 
	// element array. If they share a start or end point it is also a collaborator,
	// and gets put on the stack, and it is also put on the Vector 'eles'. 
	// The process is repeated until the stack is empty.
	private void chainCollaborators(Stack s,
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
}	// end class