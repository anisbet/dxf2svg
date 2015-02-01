
/****************************************************************************
**
**	FileName:	SvgDxfHyperlink.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This object is analogous to HTML's 'a' element. It contains
**				hyperlinking information which is activate by clicking the
**				collection of elements contained in this object.
**
**	Date:		November 27, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - November 27, 2003
**				0.02 - April 8, 2004 Changed ObjType = to setType()
**				0.03 - September 17, 2004 converted to use Attribute object from 
**				hard coded values.
**				0.04 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**				0.05 - March 29, 2005 Changed assignEvent() to assignPartsListEvent()
**				so that it also puts in an PartNo as the ID on each text element
**				that matches the item number in the illustration.
**				0.06 - April 11, 2005 We don't use this anymore because the -MY switch
**				now links all "part numbers"
**				as described by the regex in class PartNumberLinks.java. Part numbers no 
**				longer need to be explicitly hperlinked by the illustrator.
**				0.07 - August 26, 2005 spotcalls will be intercepted, if the
**				link target matches a spotcall that ends with <CODE>.svgz</CODE>
**				that link will be re-interpreted to change that to the SVGz's 
**				parent wrapper file. E.g. if the link target is FAAAAGN.svgz this
**				method - if the IETM switch is used - will take the link target 
**				and direct convert it to the SVG's parent HTML wrapper.
**				0.08 - September 2, 2005 Add check to make sure the hyperlink contains
**				a spotcall before swapping the '.svgz' extension with '.html'.
**				This will stop all links to svg graphics from changing; it 
**				restricts the changes to spotcall links only. If the user enters
**				a valid URL to another site, those links will be honoured.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;
import dxf2svg.DxfPreprocessor;   // Switch to test if we swap svgz ext. for html.
import dxf2svg.util.*;
import java.util.regex.*;	      // regular expression matching for content search.


/**
*	This object is analogous to HTML's 'a' element. It contains
*	hyperlinking information which is activate by clicking the
*	collection of elements contained in this object. Typically
*	when you request an object in DXF to have a hyperlink, the
*	hyperlink is placed on each of the group's elements. This
*	means that each SvgDxfHyperlink object will contain just
*	one element. This does not have to be so though; if you wish
*	you could group objects through searches during the final 
*	assembly stage of SVG generation (see
*	{@link dxf2svg.SvgBuilder#searchSvgContent} for more details).
*	<P>
*	This element carries the same functionality contract of {@link dxf2svg.svg.SvgGroup}
*	as far as containing other Svg elements (including animation).
*	<p>
*	As of version 0.07 - August 26, 2005, spotcalls will be intercepted, if the
*	link target matches a spotcall that ends with <CODE>.svgz</CODE>
*	that link will be re-interpreted to change that to the SVGz's 
*	parent wrapper file. E.g. if the link target is FAAAAGN.svgz this
*	method - if the IETM switch is used - will take the link target 
*	and direct convert it to the SVG's parent HTML wrapper. This will only
*	occur if the pattern {@link #SPOT_CALL} can be found in the URL. This 
*	will automatically change 
*
*	@see SvgSymbol
*	@see SvgGroup
*	@see SvgLayerGroup
*	@see SvgCollection
*
*	@version	0.08 - September 2, 2005
*	@author		Andrew Nisbet
*/
public class SvgDxfHyperlink extends SvgGroup
{
	// Sample: <a xlink:href="notes.html" target="main">
	private String xLink;			// Location of the link destination.
	private String frameTarget;		// Name of the frame to open the link in.
	private String linkTitle;		// Sets the link title (alt) rollover messaging feature.
	private boolean opensNewWindow; // Indicate whether the link is to open a new window.
	public final static String SPOT_CALL = "(F|f|S|s|W|w)\\p{Alpha}{6}\\x2e((s|S)(v|V)(G|g)(Z|z)?)";
	
	///////////////////////////////
	//                           //
	//        Constructor        //
	//                           //
	///////////////////////////////
	
	/**
	*	Creates an SvgDxfHyperlink object.
	*	@param dxfc Reference to the conversion context for this file.
	*/
	public SvgDxfHyperlink(DxfConverter dxfc)
	{
		super(dxfc);
		////////////// April 8, 2004 //////////////
		setType("a");
		// controls if the link target opens in a new window or not.
		opensNewWindow = false;
	}
	
	/**	This method is used to supply the xlink href link target. This is 
	*	analogous to HTML 'a' element's 'HREF' attribute.
	*	<P>
	*	As of 0.07 - August 26, 2005 spotcalls will be intercepted, if the
	*	link target matches a spotcall that ends with <CODE>.svgz</CODE>
	*	that link will be re-interpreted to change that to the SVGz's 
	*	parent wrapper file. E.g. if the link target is FAAAAGN.svgz this
	*	method - if the IETM switch is used - will take the link target 
	*	and direct convert it to the SVG's parent HTML wrapper.
	*	<P>
	*	Note: Only URIs that contain valid spot calls will be altered. All
	*	others will pass through this process un-altered. This is done
	*	so an illustrator can add a link to a IETM fragment or any other file
	*	and that link will be honoured.
	*
	*	@param uri Unformatted link target. You do not need to include 
	*	formatting like 'xlink:href=' attributes etc.. You do need to include
	*	any internal link targets i.e. 'MyDrawing.svg#viewA'. Also the path
	*	information should use the Un*x style of path separator that is the
	*	defacto web standard (NO Windows BACKSLASHES). Be careful when you 
	*	type link addresses when inserting a hyperlink in AutoCAD.
	*	@see dxf2svg.DxfPreprocessor#setSwapSvgForHtml
	*	@see #SPOT_CALL
	*	@see dxf2svg.DxfPreprocessor#swapSvgzForHtml
	*/
	public void setXLink(String uri)
	{
		String result = uri;
		// Check if there is a spot call in the url. If there is change it to
		// 'html'. This will allow non-spotcall related URLs to pass through
		// unchanged. This would be required if the user wanted to link to 
		// a web address or a IETM fragment.
		Pattern pattern = Pattern.compile( SPOT_CALL );	
		Matcher m = pattern.matcher( uri );
		if (DxfPreprocessor.swapSvgzForHtml() && m.find())
		{
			result = uri.replaceFirst("\\.((s|S)(v|V)(G|g)(Z|z)?)","\\.html");
			// Do this so the names of links match names in frameset
			// and so the 'javascript.js' function highlightsheet()
			// will work.
			result = result.toLowerCase();
		}
		this.xLink = result;
	}
	
	/** The title attribute is used to describe the meaning of a link or
	*	resource in a human-readable fashion, along the same lines as the
	*	role or arcrole attribute. A value is optional; if a value is
	*	supplied, it should contain a string that describes the resource.
	*	The use of this information is highly dependent on the type of
	*	processing being done. It may be used, for example, to make titles
	*	available to applications used by visually impaired users, or to
	*	create a table of links, or to present help text that appears when
	*	a user lets a mouse pointer hover over a starting resource.
	*	<P>
	*	<B>Note:</B> This functionality is not supported by the Adobe SVG 
	*	plugin as far as is currently tested.
	*/
	public void setLinkTitle(String title)
	{
		linkTitle = title;
	}
	
	/** Indicates whether, upon activation of the link, traversing to the ending 
	*	resource should load it in a new window, frame, pane, or other relevant
	*	presentation context or load it in the same window, frame, pane, or other
	*	relevant presentation context in which the starting resource was loaded. 
	*/
	public void openLinkInNewWindow(boolean opensNewWindow)
	{
		this.opensNewWindow = opensNewWindow;
	}
	
	/** This attribute has applicability when there are multiple possible targets
	*	for the ending resource, such as when the parent document is a multi-frame
	*	HTML or XHTML document. This attribute specifies the name of the target
	*	location (e.g., an HTML or XHTML frame) into which a document is to be
	*	opened when the link is activated. 
	*/
	public void setFrameTarget(String frameName)
	{
		frameTarget = frameName;
	}
	
	/**	Returns attributes required to make a 'a' link work.
	*	@throws NullXLinkException if the link destination is null or an empty String.
	*/
	protected StringBuffer getAttributes()
	{
		if (xLink != null)
		{
			if (xLink.length() == 0)
			{
				throw new NullXLinkException();
			}
		}
		else
		{
			throw new NullXLinkException();
		}
		
		
		
		StringBuffer attribs = new StringBuffer();
		
		// get the all important xlink reference
		addAttribute(new Attribute("xlink:href", xLink));
		//attribs.append(" xlink:href=\"" + xLink + "\"");
		
		
		
		if (frameTarget != null)
		{
			if (frameTarget.length() > 0)
			{
				addAttribute(new Attribute("target", frameTarget));
				//attribs.append(" target=\"" + frameTarget + "\"");
			}  // end if
		}  // end if
		
		
		
		if (opensNewWindow)
		{
			addAttribute(new Attribute("xlink:show", "new"));
			//attribs.append(" xlink:show=\"new\"");
		}  // else default is to replace and is not included as an optimization.
		
		
		
		if (linkTitle != null)
		{
			if (linkTitle.length() > 0)
			{
				addAttribute(new Attribute("xlink:title", linkTitle));
				//attribs.append(" xlink:title=\"" + linkTitle + "\"");
			}  // end if
		}  // end if
		
		attribs.append(getAdditionalAttributes());
		
		return attribs;
	}
	

	
	///////////////////////////////
	//                           //
	//    Exception class(es)    //
	//                           //
	///////////////////////////////
	/** This exception gets thrown if, at the time of rendering this object, the xlink
	*	has not been set.
	*/
	protected class NullXLinkException extends RuntimeException
	{
		protected NullXLinkException()
		{
			System.err.println("SvgDxfHyperlink reports that a link object is required but "+
				"no target was supplied to link to.");
		}
	}  // end exception
}