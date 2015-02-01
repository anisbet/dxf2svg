
/****************************************************************************
**
**	FileName:	DxfParser.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Read and parse either binary or ASCII DXF files.
**
**	Date:		April 19, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	2.00 - August 5, 2002
**				2.01 - August 15, 2002 Removed the DxfParserConst interface
**				to simplify things. Brought all constants into DxfParser.java
**				2.50 - August 20, 2002 Remodelled parser to populate a
**				Vector[] so this class is now completely modular.
**				2.51 - August 22, 2002 DxfParser now operates on a reference
**				to a Vector[] from the caller and checks to see if the array
**				is big enough before starting. Instance data and methods are
**				no longer static in anticipation of being run in a multithread
**				environment.
**				2.52 - September 25, 2002 Added copyright notice.
**				2.53 - November 6, 2002 Removed interface constants and placed
**				them within the class a-la-Effective Java Programming. Added
**				toString() method for same reason.
**				2.54 - November 27, 2002 Changed UndefinedGroupCodeException
**				from an Exception to an unchecked RuntimeException.
**				2.55 - December 13, 2002 Moved UndefinedGroupCodeException
**				to an internal class.
**				2.56 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04 and version and copyright date
**				update.
**				3.0 - April 21, 2005 Stream line the processor to reduce the number
**				of persistant objects to speed things up and to reduce memory.
**
**	TODO:
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;
import java.util.*;
import dxf2svg.*;
import dxf2svg.util.ProcessorManager;

/**	Reads and parses either an ASCII or Binary AutoCAD DXF file.<BR><BR>
*
*	Specifically it reads the DXF file, organizes the data into DxfElementPairs
*	and stacks them in one of seven different containers ready for use by
*	the rest of the conversion process.<BR><BR>
*
*	This parser was designed for Dxf2Svg but is flexible enough to
*	not require any additions to be useful for other
*	applications that need to parse Dxf files.
*
*	If you are wanting to reuse this class for other purposes you will need
*	DxfElementPair to make it work.
*
*	@see		DxfElementPair
*	@version	3.0 - April 14, 2005
*	@author		Andrew Nisbet
*/

public class DxfParser
{
	// Standard sections within a DXF.
	/** Parser version */
	public final static String ParserVersion = "3.0 - April 21, 2005";
	/** HEADER section */
	public final static int HEADER			= 0;
	/** CLASSES section */
	public final static int CLASSES			= 1;
	/** TABLES section */
	public final static int TABLES			= 2;
	/** BLOCKS section */
	public final static int BLOCKS			= 3;
	/** ENTITIES section */
	public final static int ENTITIES		= 4;
	/** OBJECTS section */
	public final static int OBJECTS			= 5;
	/** THUMBNAIL section (optional) */
	public final static int THUMBNAIL		= 6;
	// This file contains a list of possible sections in the DXF file.
	/** No section detected: default state */
	public final static int NONE			= -1;
	// See DxfParserConst for enum values 0 - 6.
	/** ENDSECtion */
	public final static int ENDSEC			= 7;

	// class's instance data
	private boolean VERBOSE = false;// Um... you know.
	private boolean BINARY;			// which type of file we are reading.
	private int Section = NONE;		// which part of the DXF file are we in.
	private long LinesRead = 0;		// Line number for totals read and error if any.
	private long BytesRead = 0;		// Offset for totals read and error if any.
	private File IN;				// File descriptor for Dxf
	private String FileName = new String();	// name of passed file.
	private static final int BYTE_MARKER = 2050;	// represents the recommended
	// not to exceed marker for strings in Acad2000

	private static final String Sentinal = "AutoCAD Binary DXF";
	private static final int EOF		= -999;
	
	
	
	private ProcessorManager processorManager;
	private DxfElementPair pair;







	/** This constructor was specifically designed to dove-tail into Dxf2Svg.
	*	@param DXFFileName Name of the DXF file.
	*	@param VerboseMode switch setting for outputting lots of messages
	*/
	public DxfParser(String DXFFileName, ProcessorManager pManager, boolean VerboseMode)
	{
		System.out.println("Parser: Dxf parser version " +ParserVersion);
		System.out.println("\tDeveloped for "+Dxf2SvgConstants.APPLICATION+" on Java VM 1.4.");
		System.out.println("\tCopyright (c) 2001 - 2005");
		FileName = DXFFileName;
		processorManager = pManager;
		VERBOSE = VerboseMode;
		pair = new DxfElementPair();
	}


	/**
	*	Prints out parser version and credits.
	*/
	public String toString()
	{
		return "DxfParser: version "+ParserVersion+"\n"+
			"Written by Andrew Nisbet";
	}

	/**	Reads and parses a DXF, encapsulates the data into DxfElementPairs and
	*	packs the data into the appropriate index of a Vector[] array.
	*
	*	The methology of parsing a DXF follows:<BR><BR>
	*	Read in the dxf one line at a time and for each pair of lines assign it to a
	*	new DxfElementPair and pack it onto the end of a Vector of DxfElementPairs.
	*	When you detect the end of a section assign the value of this Vector to
	*	the appropriate Vector list for the section that it pertains with the clone()
	*	method.  Clear the epArray Vector and start over for the next section.
	*	After all the sections have been collected, make a DxfConverter function.
	*	The DxfConverter function will take over and make sense of the information
	*	depending on the implementers needs.
	*
	*	@throws IOException If there is an error reading the DXF.
	*	@throws NullPointerException if the DXF file ends unexpectedly.
	*/
	public void parse() throws IOException
	{
		// make new file descriptor.
		IN = new File(FileName);

		// create Vector to act as an array of ElementPair objects for each section.
		//Vector epArray = new Vector();

		/*
		** This seems clumsy but I am going to open two streams
		** one for a potential binary file and one for a potential
		** text file then close one after we know which we are to use.
		*/
		BufferedReader DxfStrm = new BufferedReader(
			new FileReader(IN));

		DataInputStream DxfBinStrm = new DataInputStream(
			new FileInputStream(IN));




		/*
		**	We do this so we can determine and set a switch for binary
		** 	or text files.
		*/
		try
		{
			/*
			** Mark the beginning of the string because if this is not
			** a binary file then the file pointer will not point to the
			** initial code and the rest of the stream will be out of sync.
			*/
			DxfStrm.mark(24);
			String StrRead = DxfStrm.readLine();

			if (StrRead.startsWith(Sentinal))
			{
				BINARY = true;
				if (VERBOSE)
					System.out.println("Parser message: skipping sentinal.");
				DxfBinStrm.skipBytes(22);
				DxfStrm.close();
			}
			else
			{
				BINARY = false;
				DxfBinStrm.close();
				// reset the stream so we can grab the first code.
				DxfStrm.reset();
			}
		}
		catch (IOException e)
		{
			System.err.println("Parser error: file pointer reset failure while reading \""+FileName+"\"");
			System.exit(9);
		}





		try
		{
			String codeStr = new String();
			String valueStr = new String();
			int Code;
			while(true)
			{
				StringBuffer SBuff = new StringBuffer();	// to convert primative data types to strings.
				
				if (BINARY)
				{
					// start reading the binary file after the sentinal
					// the first value found will be an int.
					Code = readDxfInteger(DxfBinStrm);
					SBuff.append(Code);
					codeStr = SBuff.toString();
					//System.out.println("codeStr = "+codeStr);
					valueStr = determineValueString(Code,DxfBinStrm);
					//System.out.println("valStr = "+valueStr);
				}
				else
				{
					String StrRead = DxfStrm.readLine();
					setLineNumber();

					// get code/value pair and find out where we are
					// and if necessary update Section var to new section
					codeStr = StrRead.trim();
					valueStr = DxfStrm.readLine();
					setLineNumber();
					valueStr = valueStr.trim();
				}


				if ((codeStr.equals("0")) && (valueStr.equals("EOF")))
				{
					// end of dxf signal.
					if (VERBOSE)
						System.out.println("Parser: finished reading DXF.");
					return;
				}
				else if ((codeStr.equals("0")) && (valueStr.equals("SECTION")))
				{
					DxfElementPair myEPTest;
					if (BINARY)
						myEPTest = getNextPair(DxfBinStrm);
					else
						myEPTest = getNextPair(DxfStrm);

					setSection(myEPTest.getValue());
					// we'll do this so these values don't end up on the
					// epList Vector.
					continue;
				}
				else if ((codeStr.equals("0")) && (valueStr.equals("ENDSEC")))
				{
					Section = NONE;
					/*
					** Go get the next token pair - do not process any further because
					** these are just markers in the file stream, not useful data.
					*/
					processorManager.endSection( );
					continue;
				}
				/*
				** Handle any comments in DXF by printing out the comment string.
				** and like the other special cases above, consume the token and
				** continue parse. Note in binary DXF does not support comments
				** so this rule does not fire.
				*/
				else if (codeStr.equals("999"))
				{
					System.out.println("Parser: DXF comment \""+valueStr+"\"");
					continue;
				}



				/*
				** This is done to ignore the DxfElementPair if
				** no section has been specified which should never happen.
				*/
				if (Section == NONE)
				{
					continue;
				}
				else
				{
					pair.setCode( codeStr );
					pair.setValue( valueStr );
					processorManager.setDxfElementPair( pair );
				}

			}

		}
		/*
		** Exception is thrown if we run off the end of the file
		** but as we test for the manditory code 0, string "EOF"
		** this try block could be gotten rid of.
		*/
		catch (NullPointerException e)
		{
			if (BINARY == false)
				System.err.println("Parser error: DXF file \""+FileName+
					"\" ends unexpectedly at line: "+LinesRead+"." + e);
			else
				System.err.println("Parser error: DXF file \""+FileName+
					"\" ends unexpectedly at offset: 0x"+Long.toHexString(BytesRead)+"." + e);

		}

		if (BINARY)
			DxfBinStrm.close();
		else
			DxfStrm.close();

		if (VERBOSE == true)
		{
			System.out.println("Parser:***** start summary *****");
			if (BINARY == true)
				System.out.println("  Total file size: "+ BytesRead +" bytes.");
			else
				System.out.println("  Total file size: "+ LinesRead +" lines.");
			System.out.println("Parser:***** end summary *****");
		}

		/*
		*	Now all the Dxf data has been parsed and packed we let the graphic conversion
		*	application take over and make sense of it all.
		*/
		return;
	}










	/*************** reader methods for the different streams ***************/
	// Character stream.
	protected DxfElementPair getNextPair(BufferedReader Strm) throws IOException//, NullPointerException
	{
		String tmpCode = Strm.readLine();
		String tmpValue = Strm.readLine();
		setLineNumbers(2);
		DxfElementPair EP = new DxfElementPair(tmpCode,tmpValue);
		return EP;
	}

	// Binary stream.
	protected DxfElementPair getNextPair(DataInputStream Strm) throws IOException//, NullPointerException
	{
		int Code = readDxfInteger(Strm);
		String tmpValue = determineValueString(Code,Strm);
		DxfElementPair EP = new DxfElementPair(Code,tmpValue);
		return EP;
	}




	/**	This method takes a code read from the stream and returns a string
	*	representation of the data collected in the next series of bytes
	*	as determined by the code collected.
	*	@return String version of the value read from the stream.
	*	@throws UndefinedDxfGroupCodeException if the parser finds an unknown group code.
	*	This means the parser would not know what type of data to expect next.
	*/
	protected String determineValueString(int Code, DataInputStream Strm) throws IOException
	{

		String ValStr = new String();
		StringBuffer SBuff = new StringBuffer();
		double DoubVal;
		int IntVal;

		if ((Code >= 0)&&(Code <= 9))// we have a section
		{
			ValStr = readDxfString(Strm);
		}
		else if ((Code >= 10)&&(Code <= 59))
		{
			// read a double value IEEE standard byte swapped.
			DoubVal = readDxfDouble(Strm);
			SBuff.append(DoubVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 60)&&(Code <= 79))
		{
			// read a integer 16 bit
			IntVal = readDxfInteger(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 90)&&(Code <= 99))
		{
			// read a 32 bit integer.
			IntVal = readDxfIntegerWide(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 100)&&(Code <= 105))
		{
			ValStr = readDxfString(Strm);
		}
		else if ((Code >= 110)&&(Code <= 149))
		{
			// read a double value IEEE standard byte swapped.
			DoubVal = readDxfDouble(Strm);
			SBuff.append(DoubVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 170)&&(Code <= 179))
		{
			// read a integer 16 bit
			// there is no mention of this range in the spec
			// but with reverse engineering we discover it is
			// 16 bits wide.
			IntVal = readDxfInteger(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 210)&&(Code <= 240))
		{
			// read a double value IEEE standard byte swapped.
			DoubVal = readDxfDouble(Strm);
			SBuff.append(DoubVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 280)&&(Code <= 289))
		{
			// read a integer 8 bit
			IntVal = readDxfByte(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 290)&&(Code <= 299))
		{
			// read a integer for boolean
			IntVal = readDxfBoolean(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if (((Code >= 300)&&(Code <= 369))||
			((Code >= 390)&&(Code <= 399)))
		{
			// read string (these represent hex numbers as strings).
			ValStr = readDxfString(Strm);
		}
		else if ((Code >= 370)&&(Code <= 389))
		{
			// read a integer 8 bit
			IntVal = readDxfByte(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 400)&&(Code <= 409))
		{
			// read a integer 16 bit
			IntVal = readDxfInteger(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 410)&&(Code <= 419))
		{
			// read string (these represent hex numbers as strings).
			ValStr = readDxfString(Strm);
		}
		else if ((Code >= 1000)&&(Code <= 1009))
		{
			// read string (these represent hex numbers as strings).
			ValStr = readDxfString(Strm);
		}
		else if ((Code >= 1010)&&(Code <= 1059))
		{
			// read a float swapped haven't seen one yet.
			DoubVal = readDxfDouble(Strm);
			SBuff.append(DoubVal);
			ValStr = SBuff.toString();
		}
		else if ((Code >= 1060)&&(Code <= 1070))
		{
			// read a integer 16 bit
			IntVal = readDxfInteger(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else if (Code == 1071)
		{
			// read a 32 bit integer.
			IntVal = readDxfIntegerWide(Strm);
			SBuff.append(IntVal);
			ValStr = SBuff.toString();
		}
		else
		{
			throw new UndefinedDxfGroupCodeException(Code,BytesRead);
		} // end if
		return ValStr;
	} // end method














	//***************** private methods ******************/
	protected void setSection(String val)
	{
		if (val.equalsIgnoreCase("HEADER"))
			Section = HEADER;
		else if (val.equalsIgnoreCase("CLASSES"))
			Section = CLASSES;
		else if (val.equalsIgnoreCase("TABLES"))
			Section = TABLES;
		else if (val.equalsIgnoreCase("BLOCKS"))
			Section = BLOCKS;
		else if (val.equalsIgnoreCase("ENTITIES"))
			Section = ENTITIES;
		else if (val.equalsIgnoreCase("OBJECTS"))
			Section = OBJECTS;
		else if (val.equalsIgnoreCase("THUMBNAILIMAGE"))
			Section = THUMBNAIL;
		else
			System.err.println("Parser: Warning! DXF section not handled; perhap new section discriptor.");
		
		processorManager.setDxfSection( Section );
		// System.out.println(Section);
		return;
	}




	/***************** private data primitive extraction methods ***********/
	/***************** for Binary streams **********************/
	protected int readDxfByte(DataInputStream DS)
	{
		int IntResult = 0;
		try
		{
			IntResult = (int)DS.readShort();
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading boolean."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}

		BytesRead += 2;

		return IntResult;
	}




	protected int readDxfBoolean(DataInputStream DS)
	{
		/*
		** AutoCad actually uses a byte of data to represent a boolean so
		** read a byte of data.
		*/
		int IntResult = 0;
		try
		{
			IntResult = (int)DS.readByte();
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading boolean."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}

		BytesRead++;

		return IntResult;
	}


	protected double readDxfDouble(DataInputStream DS)
	{
		/*
		*	This was the biggest stumbling block to the successful reading of
		*	a binary DXF. The documentation is of little help as the emphasis
		*	is naturally focused on the majority of DXF files which happen to
		*	be ASCII.
		*
		*	When reading a double from a binary DXF we know that we
		*	have to read the next eight bytes and place them in a double. Easy
		*	right, wrong! As it turns out AutoDesk chose (wisely) to express
		*	double as a IEEE double-precision value with little-endian byte
		*	ordering. Little-endian byte ordering means placing the less sign-
		*	ificant byte first and the most significant byte last - the reverse
		*	of the way humans naturally interpret numbers.
		*
		*	This forces all DXF applications that write DXF to
		*	use the 'Intel' byte ordering and eliminate the file conversion
		*	problems faced by TIFF readers which must figure out which byte
		*	ordering to use on the fly from the information in the first two
		*	bytes.
		*
		*	Here is how got it to work (a real programmer would probably laugh):
		*	1) read eight bytes from file and store in a Java long (64 bits).
		*	2) mask the high 32 bytes and low 32 bytes because Java's bit wise
		*	operators like '&' don't work on anything larger than int (32 bits)
		*	and store them in two new long called Ll and Lh.
		*	3) shift the high order 32 bits to the low order using the >>>
		*	to avoid the sign bit propagating. This is due to the fact that
		*	unlike C and C++ Java does it's best to simplify things by making
		*	all basic number data types signed. This creates a problem for us
		*	because all of our bytes must be unsigned to calculate correctly.
		*	4) shift the low order 32 bits (Ll) up 32 bits and then back down
		*	to get rid of any sign bit propagation (see step 3).
		*	5) mask each byte individually and swap their position with their
		*	partner. i.e. if you read in byte 0,1,2,4,5,6,7 swap bytes so they
		*	look like 1,0,3,2,5,4,7,6 and your done right - think again!
		*	6) take the bytes and add them in pairs and shift them so they end
		*	up in the lowest order position of their respective longs called
		*	at this point Word[x].
		*	7) now that there is a correctly swapped pair of bytes in each word
		*	we move them so they actually appear in the reverse order they were
		*	read from the file in. That is bytes 7,6 come first and 0,1 pair last
		*	8) now you have a long we can use, use the Double method:
		*	longBitsToDouble() to convert that value to a double.
		*
		*	Now you are done!
		*	Here is the sequence again in slow motion:
		*	1) read bytes: 0,1,2,3,4,5,6,7
		*	2) Lh = 0,1,2,3,-,-,-,-  Lh = (int)(-,-,-,-,4,5,6,7) = (4,4,4,4,4,5,6,7)
		*	3) Lh = -,-,-,-,0,1,2,3
		*	4) Ll = 4,5,6,7,-,-,-,-		Ll = -,-,-,-,4,5,6,7
		*	5) Blong[7] = 7, Blong[6] = 6, ... Blong[0] = 0 and then
		*	swap the bytes Blong[7] <<= 8, Blong[6] >>= 8
		*	6) Word[3] = Blong[7] + Blong[6]  Word[2] = Blong[5] + Blong[4] ...
		*	7) Word[3] << 48, Word[2] << 32, Word[1] << 16, Word[3] << 0
		*	8) double = Double.longBitsToDouble((Word[0]+Word[1]+Word[2]+Word[3]));
		*/

		double D;
		long[] Blong = new long[8];
		long[] Word = new long[4];
		long L = 0;
		long Ll,Lh;


		try
		{
			L = DS.readLong();
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading double."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}
		//System.out.println("L = \t\t"+Long.toBinaryString(L));

		Lh = (L >>> 32);
		// if you don't cast it as int it keeps all the original bits
		Ll = (int)(L & 0xffffffff);
		// do this to remove make it an unsigned long.
		Ll <<= 32;
		Ll >>>= 32;

		// now mask the two longs to get the individual bytes
		Blong[7] = Ll & 0xff;
		Blong[6] = Ll & 0xff00;
		Blong[5] = Ll & 0xff0000;
		Blong[4] = Ll & 0xff000000;
		Blong[3] = Lh & 0xff;
		Blong[2] = Lh & 0xff00;
		Blong[1] = Lh & 0xff0000;
		Blong[0] = Lh & 0xff000000;

		// swap them with their partners i.e. byte swap them so we can
		// load a java 'long' with little endian byte pairs.
		Blong[7] <<= 8;
		Blong[6] >>= 8;
		Blong[5] <<= 8;
		Blong[4] >>= 8;
		Blong[3] <<= 8;
		Blong[2] >>= 8;
		Blong[1] <<= 8;
		Blong[0] >>= 8;

		// next we added the pairs together again into a new long
		Word[3] = Blong[7] + Blong[6];
		Word[2] = Blong[5] + Blong[4];
		Word[1] = Blong[3] + Blong[2];
		Word[0] = Blong[1] + Blong[0];

		// these two are shifted because they represent high order pairs
		// of the lower 32 bits of the java 'long'.
		Word[2] >>= 16;
		Word[0] >>= 16;

		// This is THE key. The double that comes from a binary Dxf is written
		// in reverse because we have to move low order byte pair to the high
		// order and the high order byte pair to the lowest order.
		Word[3] <<= 48;
		Word[2] <<= 32;
		Word[1] <<= 16;
		// we don't have to do anything to Word[0] it is ok where it is.
		//Word[0];


		D = Double.longBitsToDouble((Word[0]+Word[1]+Word[2]+Word[3]));
		//D = 99;

		BytesRead += 8;

		// in a complete Dxf file we should never need this but we are testing
		// with partial Dxf to test theories.
		return D;
	}









	protected int readDxfInteger(DataInputStream DS)
	{
		/*
		** read an integer from the stream. Here we mean a Java short (16 bits)
		*/
		int IntResult = 0;
		int S = 0,Sl = 0,Sh = 0;


		/*
		** Read two bytes from the stream, swap them and return the value
		** for further analysis.
		*/

		try
		{
			/* Read two bytes from the stream. */
			S = DS.readShort();
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading Integer."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}

		/* Mask the top byte and shift without propagating the sign bit */
		Sl = (S & 0xff00) >>> 8;
		Sh = (S & 0xff) << 8;
		IntResult = (int)(Sh + Sl);


		BytesRead += 2;
		return IntResult;
	}

	protected int readDxfIntegerWide(DataInputStream DS)
	{
		/*
		** This method reads a 32 bit integer.
		*/
		int IntResult = 0;
		int S = 0,Sl1 = 0,Sh1 = 0,Sl0 = 0,Sh0 = 0;

		/*
		** Read two bytes from the stream, swap them and return the value
		** for further analysis.
		*/

		/* Read two bytes from the stream. */
		try
		{
			S = DS.readInt();
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading Integer Wide."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}
		/* Mask the top byte and shift without propagating the sign bit */
		Sl0 = (S & 0xff00) >> 8;
		Sh0 = (S & 0xff) << 8;
		Sl1 = (S & 0xff000000) >>> 8;
		Sh1 = (S & 0xff0000) << 8;
		/* now swap the words around... this really works!!!*/
		IntResult = ((Sh0 + Sl0)<<16) + ((Sh1 + Sl1) >>> 16);

		BytesRead += 4;

		return IntResult;
	}


	protected String readDxfString(DataInputStream DS)
	{
		String sout = new String();
		String tmp = new String();
		char[] ca = new char[BYTE_MARKER];
		byte b;

		try
		{
			for (int i = 0; i < BYTE_MARKER; i++)
			{
				if ((ca[i] = (char)DS.readByte()) == '\0')
					break;
			}
		}
		catch (IOException e)
		{
			System.err.println("Parser Bin error: reading string."+e);
			System.err.println("\tat offset 0x"+Long.toHexString(BytesRead));
			System.exit(1);
		}

		tmp = (sout.copyValueOf(ca)).trim();
		BytesRead += tmp.length();

		return tmp;
	}
	//**************** end of Binary data primative methods ****************/








	//*********** Character stream Exception line numbering **********/
	protected void setLineNumbers(int num)	// used to advance line numbers
	{
		LinesRead += num;
		return;
	}

	protected void setLineNumber()  // used to advance line numbers
	{
		LinesRead++;
		return;
	}


	protected int getSection()   // sets current section
	{
		return Section;
	}


	/**
	*	This exception is thrown if a binary DXF file contains an unknown group code.
	*	<BR><BR>
	*	It is never thrown by ASCII DXF parse because each piece of information
	*	appears on a unique line. If we read a line we don't need or understand,
	*	we ignore it. If we read a binary DXF, by contrast, we don't have the luxury.
	*	We must be able to read all codes to know what size of data to expect
	*	in the next value.  If we don't know, we may read too many or too few bytes and
	*	end up reading incorrect data into incorrect data types.
	*
	*	The exception reports the offset of the offending data and a note about
	*	contacting the author to amend and test the DxfParser module.
	*
	*	@version	1.01 - November 27, 2002
	*	@author		Andrew Nisbet
	*/
	protected class UndefinedDxfGroupCodeException extends RuntimeException
	{
		protected UndefinedDxfGroupCodeException(String s)
		{
			super(s);
		}

		/** Message about exception.
		*	@param code unknown code read.
		*	@param bytes byte offset from start of DXF file.
		*/
		protected UndefinedDxfGroupCodeException(int code,long bytes)
		{
			System.err.println("Parser Exception at offset 0x"+Long.toHexString(bytes)+":");
			System.err.println("Code \"" + code + "\" found but not defined.");
			System.err.println("The Binary DXF parser has come across a code that has not been");
			System.err.println("defined. "+Dxf2SvgConstants.APPLICATION+" will now exit because the parser cannot");
			System.err.println("read this data type. This may be due to the introduction of a new");
			System.err.println("group code by AutoDesk(R), file corruption or some other error.");
			System.err.println("Try opening the file in AutoCAD to prove its integrity.");
			System.err.println("Contact the author for assistance.");
		}
	}


} // EOF