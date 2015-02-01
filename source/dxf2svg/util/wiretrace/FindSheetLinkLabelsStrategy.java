
/****************************************************************************
**
**	FileName:	FindSheetLinkLabelsStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SVG Search stategy that seeks 'Sheet n[n]' type patterns.
**				based on the Strategy OOP design pattern.
**
**	Date:		March 14, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 14, 2005
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import java.util.Vector;
import dxf2svg.svg.SvgLayerGroup;
import dxf2svg.DxfConverter;
import java.util.regex.Pattern;

/** This algorithm searches for sheet(s) matches like: 'SHEET n[n]'. This is
*	strictly for Spar formatted wiring diagrams.
*
*	@version	0.01 - March 14, 2005
*	@author		Andrew Nisbet
*/
public class FindSheetLinkLabelsStrategy extends WireSearchStrategy
{
	public void searchMatchModify(
		DxfConverter conversionContext,
		Vector results,
		SvgLayerGroup svgl,
		Vector components,
		Pattern p
	)
	{
		// This method only returns SvgText elements as per the contract with SvgCollection.
		svgl.searchForElements(results, p);
	}
}