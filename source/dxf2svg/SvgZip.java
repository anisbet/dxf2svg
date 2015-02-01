
/**************************************************************************
**
** 	Date: 		January 06, 2003
** 	File Name:	SvgZip.java
**
** 	Java Version:
**	Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.0_01-b03)
**	Java HotSpot(TM) Client VM (build 1.4.0_01-b03, mixed mode)
**
** 	Author: 	Andrew Nisbet
** 	Purpose: 	Unzip compressed svg files.
**
**	Version:	1.00 - January 07, 2003 
** 	Todo:
**
***************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.zip.*;

/**
*	This class is a quick little SVG zipper.
*
*	Usage: <b>java dxf2svg.SvgZip (file.svgz|directory)</b>
*	where the Dxf2Svg.jar is registered in your CLASSPATH.
*	<P>
*	If the argument is a directory
*	all '*.svg' files in all sub-directories are compressed recursively.
*
*	@author		Andrew Nisbet
*	@version	1.00 - January 07, 2003
*/
public class SvgZip
{
	private static double VERSION = 1.0;
	
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println();
			System.out.println("Usage: java SvgZip (file|directory)");
			System.out.println();
			System.out.println("SvgZip (v"+VERSION+") is used for zipping SVG files created by");
			System.out.println("the Dxf2Svg conversion application when the '-z' switch is <em>not</em> used.");
			System.out.println();
			System.out.println("Enter a file or directory as an argument on the command line");
			System.out.println("to run SvgZip. If a directory is entered all subdirectories will");
			System.out.println("will be processed recursively. Only SVG files will be compressed.");
			System.out.println("The application uses GZIP which is compatable with Adobe's plugin.");
			System.out.println();
			System.out.println("Exiting.");
			System.exit(1);
		}

		File file = new File(args[0]);

		try
		{
			zipFile(file);
		}
		catch (FileNotFoundException nf)
		{
			System.err.println("SvgZip error: file '"+file+"' not found.");
			System.err.println(nf);
			System.exit(2);
		}
		catch (IOException io)
		{
			System.err.println("SvgZip error: reading '"+file+"'.");
			System.err.println(io);
			System.exit(2);
		}
	}

	/**
	*	Compresses the SVG file using GZIP algorithm.
	*	@throws FileNotFoundException
	*	@throws IOException
	*/
	public static void zipFile(File f)
		throws FileNotFoundException, IOException
	{
		if (f.isDirectory() == true)
		{
			// Puts a new line for the next file name
			System.out.println();
			// get a list of files in the directory.
			String[] FileNames = f.list();
			for (int i = 0; i < FileNames.length; i++)
			{
				zipFile(new File(f, FileNames[i]));
			}
		}
		else	// just one file.
		{
			// Get the name of the file to be compressed.
			String tmp = f.toString().toLowerCase();
			if (! tmp.endsWith(".svg"))
			{
				return;
			}

			// get the name of the file for use later.
			String outName = f.toString();
			int posDot = outName.lastIndexOf(".");
			if (posDot >= 1) // must be one char in length min.
			{
				outName = outName.substring(0,posDot);
			}
			File fOut = new File(outName + ".svgz");


			int SIZE = 65536;
			GZIPOutputStream GZIPStream = new GZIPOutputStream(
				new FileOutputStream(fOut) );
				
			DataOutputStream GZIPOut = new DataOutputStream(
				new BufferedOutputStream(GZIPStream) );
				
			File IN = new File(f.toString());
			
			DataInputStream svgStreamIn = new DataInputStream(
				new BufferedInputStream( new FileInputStream(IN) ) );
			
			byte[] Buf = new byte[SIZE];

			int bytesRead = 0;
			while (bytesRead != -1)
			{
				bytesRead = svgStreamIn.read(Buf);
				if (bytesRead < SIZE)
				{
					for (int i = 0; i < bytesRead; i++)
					{
						GZIPOut.write(Buf[i]);
					} // end for
		
				} // end if
				else
				{
					GZIPOut.write(Buf);
				}
			}
			
			svgStreamIn.close();
			GZIPOut.close();
			

			// Delete original zip file
			if (f.delete() == true)
			{
				System.err.println(outName+" zipped.");
			}
			else
			{
				System.err.println("Unable to delete svg file: "+outName);
			}
		} // end else
	} // end zipFile()
} // end class