
/**************************************************************************
**
** 	Date: 		January 06, 2003
** 	File Name:	SvgUnZip.java
**
** 	Java Version:
**	Java(TM) 2 Runtime Environment, Standard Edition (build 1.4.0_01-b03)
**	Java HotSpot(TM) Client VM (build 1.4.0_01-b03, mixed mode)
**
** 	Author: 	Andrew Nisbet
** 	Purpose: 	Unzip compressed svg files.
**
**	Version:	1.00 - January 06, 2003 
** 	Todo:
**
***************************************************************************/

package dxf2svg;

import java.io.*;
import java.util.zip.*;

/**
*	This class is a quick little file unzipper for svgz files.
*
*	Usage: <b>java dxf2svg.SvgUnZip (file.svgz|directory)</b>
*	where the Dxf2Svg.jar is registered in your CLASSPATH.
*	<P>
*	If more than, or less than one argument is passed the application
*	prints a usage message and exits. If the argument is a directory
*	all '*.svgz' files in all sub-directories are decompressed recursively.
*	<p>
*	Place the class file in some directory in your class path.
*
*	@author		Andrew Nisbet
*	@version	1.00 - January 06, 2003
*/
public class SvgUnZip
{
	private static double VERSION = 1.0;
	
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println();
			System.out.println("Usage: java SvgUnZip (file|directory)");
			System.out.println();
			System.out.println("SvgUnZip (v"+VERSION+") is used for unzipping SVGZ files created by");
			System.out.println("the Dxf2Svg conversion application when the '-z' switch is used to");
			System.out.println("compress the file.");
			System.out.println();
			System.out.println("Enter a file or directory as an argument on the command line");
			System.out.println("to run SvgUnZip. If a directory is entered all subdirectories will");
			System.out.println("will be processed recursively.");
			System.out.println();
			System.out.println("Exiting.");
			System.exit(1);
		}

		File file = new File(args[0]);

		try
		{
			unZipFile(file);
		}
		catch (FileNotFoundException nf)
		{
			System.err.println("SvgUnZip error: file '"+file+"' not found.");
			System.err.println(nf);
			System.exit(2);
		}
		catch (IOException io)
		{
			System.err.println("SvgUnZip error: reading '"+file+"'.");
			System.err.println(io);
			System.exit(2);
		}
	}

	/**
	*	Actually decompresses the file.
	*	@throws FileNotFoundException
	*	@throws IOException
	*/
	public static void unZipFile(File f)
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
				unZipFile(new File(f, FileNames[i]));
			}
		}
		else	// just one file.
		{
			// We have to stop the application from opening any
			// zip files it encounters because we don't need to
			// but if we do zipentry succeeds and we corrupt the
			// file by reading only the first entry and over writing
			// the original.
			String tmp = f.toString().toLowerCase();
			if (! tmp.endsWith(".svgz"))
			{	// are there other types of files too
				return;
			}

			GZIPInputStream GZIPStream = new GZIPInputStream(
				new FileInputStream(tmp) );
				
			DataInputStream ZStream = new DataInputStream(
				new BufferedInputStream(GZIPStream) );

			// get the name of the file for use later.
			String outName = f.toString();
			int posDot = outName.lastIndexOf(".");
			if (posDot >= 1) // must be one char in length min.
			{
				outName = outName.substring(0,posDot);
			}
			File fOut = new File(outName + ".svg");

			BufferedOutputStream deflated = new BufferedOutputStream(
				new FileOutputStream(fOut));

			byte[] Buff = new byte[65536];
			int len;
			while ((len = ZStream.read(Buff)) >= 0)
			{
				deflated.write(Buff,0,len);
			}
			
			ZStream.close();
			deflated.close();

			// Delete original zip file
			if (f.delete() == true)
			{
				System.err.println(outName+" unzipped.");
			}
			else
			{
				System.err.println("Unable to delete svgz file: "+outName);
			}
		} // end else
	} // end unZipFile()
} // end class