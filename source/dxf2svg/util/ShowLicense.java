
/****************************************************************************
**
**	FileName:	ShowLicense.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Displays any licencing and copyright information
**
**	Date:		August 9, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**
**	TODO:
**
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;

/**
*	This class is used to show the licencing information on the command line
*	through the command line.
*
*	@see dxf2svg.Dxf2Svg
*
*	@version	1.00 - August 9, 2002
*	@author		Andrew Nisbet
*/
public final class ShowLicense
{
	private File LICENSE;
	private final String Message = "You should have received a license and terms of use with this appliction\n"+
		Dxf2SvgConstants.APPLICATION+" version "+Dxf2SvgConstants.VERSION+", Copyright (C) 2001 - 2005.";

	/** Displays the contents of the terms of use statement.
	*	@param license File name or fully qualified path if the licence is
	*	not in the same directory as the application (not recommended).
	*/
	public ShowLicense(String license)
	{
		LICENSE = new File(license);

		if ((LICENSE.isFile())== true)
		{
			readFile();
		}
		else
		{
			System.err.println("ShowLicense Error: could not find '"+license+"'.");
			System.err.println(Message);
		}

	} // end constructor



	private void readFile()
	{
		String StrRead = new String();
		try
		{
			BufferedReader BReader = new BufferedReader(
				new FileReader(LICENSE) );

			for (int j = 0; ;j++)
			{
				StrRead = BReader.readLine();
				if (StrRead.equals(null) == true)
					break;
				if ((j > 1) && ((j % 20) == 0))
					pause();
				System.out.println(StrRead);
			}

			BReader.close();
		}
		catch (NullPointerException e)
		{
			System.out.println("END OF LICENSE");
		}
		catch (IOException e)
		{
			System.err.println(Message);
		}

	}



	private void pause()
	{
		System.out.println("\t<hit return to continue>");
		try
		{
			System.in.read();
		}
		catch (IOException e)
		{
			System.err.println("ShowLicense Error: reading user's input from stdin.");
		}
	}

}	// end of ShowLicense