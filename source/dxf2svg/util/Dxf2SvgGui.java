
/****************************************************************************
**
**	FileName:	Dxf2SvgGui.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	GUI interface to Dxf2Svg.
**
**	Date:		March 28, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - March 28, 2002
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import javax.swing.*;
import dxf2svg.*;

/**	This class handles displayable message boxes that contain context sesitive
*	messages.
*
*	@version 	0.01 - March 28, 2002
*	@author		Andrew Nisbet
*/
public final class Dxf2SvgGui
{
	public final static int MISSING_CONFIG_FILE	= 0;
	public final static int INCOMPLETE_CMD_LINE_SWITCH = 1;
	
	private String[] msgs = {
		"Couldn't find the config file: ",
		"The application could not run because of the incomplete argument: ",
	};
	
	public Dxf2SvgGui(int msgNum, String message)
	{
		if (msgNum < 0 || msgNum > msgs.length)
		{
			System.err.println("Dxf2SvgGui: requested message is out of range.");
			System.exit(-1);
		}
		
		switch (msgNum)
		{
			case MISSING_CONFIG_FILE:
				JOptionPane.showMessageDialog(new JFrame(),
					(msgs[msgNum] + message),
					"Dxf2Svg",
					JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
				
			default:
				break;
		}

	}

}