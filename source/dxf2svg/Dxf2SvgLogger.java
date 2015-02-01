
/****************************************************************************
**
**	FileName:	Dxf2SvgLogger.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Is used as a object that can make notes on problems in conversion
**				process.
**
**	Date:		June 18, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.1)
**
**	Version:	0.1 - June 18, 2004
**
**	TODO:		
**

**
*****************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.Vector;
import java.util.Iterator;

/** This class can be invoked to output errors or any information you wish. It is 
*	meant as a way to capture errors into a file and not have to do all the maintenance
*	of file accounting.
*	@author		Andrew Nisbet
*	@version	1.0 - June 18, 2004
*/
public class Dxf2SvgLogger
{
	private BufferedWriter bOut;
	protected final static String DEFAULT_PATH = "c:/temp/dxf2svg_log.txt";
	protected File FOUT;
	private int numberOfEvents;
	private boolean isNewInstance; 	// determines if the file gets overwritten or appended to.
	
	public Dxf2SvgLogger(String preferedPath)
	{
		init(preferedPath);
	}
	
	public Dxf2SvgLogger()
	{
		init(DEFAULT_PATH);
	}
	
	protected void init(String path)
	{
		isNewInstance = true;
		FOUT = new File(path);
		numberOfEvents = 0;
	}
	
	/** logs a single event and file that it occured in.
	*/
	public void logEvent(String fileName, String eventString)
	{
		try
		{
			if (isNewInstance)
			{
				// create a new instance that will over-write any existing log.
				bOut = new BufferedWriter(new FileWriter(FOUT));
				isNewInstance = false;
			}
			else
			{
				// append the data to an existing log file.
				bOut = new BufferedWriter(new FileWriter(FOUT, true));
			}
			StringBuffer sb = new StringBuffer();
			
			sb.append(fileName);
			sb.append(" : ");
			sb.append(eventString);
			
			bOut.write(sb.toString());
			bOut.newLine();
			bOut.close();
			numberOfEvents++;
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
	}
	
	/** Returns the log file name.
	*/
	public String getLogFileName()
	{
		if (FOUT != null)
		{
			return FOUT.getName();
		}
		return null;
	}
	
	/** Has this logger logged any events.
	*/
	public boolean hasEvents()
	{
		if (numberOfEvents != 0)
		{
			return true;
		}
		return false;
	}
	
	/** Returns the number of events that were logged.
	*/
	public int getNumberOfEvents()
	{
		return numberOfEvents;
	}
	
	/** Logs a single event for multipule files. This may be required for events 
	*	that occur that are relivant to a number of files.
	*/
	public void setEvent(Vector fileNames, String eventString)
	{
		try
		{
			bOut = new BufferedWriter(new FileWriter(FOUT, true));
			Iterator it = fileNames.iterator();
			bOut.write(eventString + " event occured in the following files:");
			bOut.newLine();
			numberOfEvents++;
			while (it.hasNext())
			{
				StringBuffer sb = new StringBuffer();
				sb.append((String)(it.next()));
				bOut.write(sb.toString());
				bOut.close();
			}
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
	}
	
	/** Returns a String representation of this class.
	*/
	public String toString()
	{
		return this.getClass().getName();
	}
}