
/****************************************************************************
**
**	FileName:	Base64Generator.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This is a UU encoder decoder object. It has two modes: encode
**				and decode. In the encode mode it takes data and encodes it 
**				into base64 text. In the decode mode it takes UU encoded data
**				and decodes it.
**
**	Date:		September 9, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	September 9, 2004
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util;

import java.io.*;
import nisbet.andrew.util.FileTester; // For DirLister and FileTester.

/**
*	This is a UU encoder decoder object. It has two modes: encode
*	and decode. In the encode mode it takes data and encodes it 
*	into base64 text. In the decode mode it takes UU encoded data
*	and decodes it.
*
*	@version	0.01 - September 9, 2004
*	@author		Andrew Nisbet
*/
public final class Base64Generator
{
	public static final int ENCODE = 1;
	public static final int DECODE = 2;
	// Mime type declaration like: "data:image/jpg;base64," where jpg can also be png.
	private final static String MIME_PREFIX = "data:image/";
	private final static String MIME_SUFFIX = ";base64,";
	
	private final static int ENCODED_ARRAY_SIZE 	= 4;
	private final static int UNENCODED_ARRAY_SIZE 	= 3;

		//     Value Encoding  Value Encoding  Value Encoding  Value Encoding
		//         0 A            17 R            34 i            51 z
		//         1 B            18 S            35 j            52 0
		//         2 C            19 T            36 k            53 1
		//         3 D            20 U            37 l            54 2
		//         4 E            21 V            38 m            55 3
		//         5 F            22 W            39 n            56 4
		//         6 G            23 X            40 o            57 5
		//         7 H            24 Y            41 p            58 6
		//         8 I            25 Z            42 q            59 7
		//         9 J            26 a            43 r            60 8
		//        10 K            27 b            44 s            61 9
		//        11 L            28 c            45 t            62 +
		//        12 M            29 d            46 u            63 /
		//        13 N            30 e            47 v
		//        14 O            31 f            48 w         (pad) =
		//        15 P            32 g            49 x
		//        16 Q            33 h            50 y

	private final static byte[] code = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
		'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
		'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
		'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
		's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2',
		'3', '4', '5', '6', '7', '8', '9', '+', '/'
	};
	private final static byte fillChar = '=';

	/////////////////////////////
	//      Constructor        //
	/////////////////////////////
	private Base64Generator(){}
	
	public static boolean getData(int encodeDecode, String file, StringBuffer outData)
	{
		switch(encodeDecode)
		{
			case ENCODE:
				// get the mime type extension for make a base64 header.
				String mimeExt = file.substring(file.length() -3);
				// ecode the data
				return readData(file, mimeExt, outData);
				
			case DECODE:
				System.err.println("Base64Generator function: "+encodeDecode+
					" not implemented yet.");
				return false;
				
			default:
				System.err.println("Base64Generator unknown function numbered: "+encodeDecode);
				return false;
		} // end switch
	} // end getData()
	
	/** This method test to see if the argument String is in fact a file on the file system 
	*/
	protected static boolean testFile(String file)
	{
		FileTester ft = new FileTester();
		if (ft.test(true, file) == FileTester.IS_OK)
		{
			return true;
		}
		
		return false;
	}
	
	/** This is where the actual UUEncoding occurs.
	*	@throws NullPointerException if any of the arguments is null.
	*	@throws IOException if there was a problem reading the unencoded data file.
	*/
	protected static boolean readData(String file, String mimeExtension, StringBuffer outBuff)
	{
		boolean result = false;
		if (! testFile(file))
		{
			System.err.println("Base64Generator: problems with file to encode.");
			return false;
		}
		

		byte[] encodedBytes = new byte[ENCODED_ARRAY_SIZE]; 
		byte[] unEncodedBytes = new byte[UNENCODED_ARRAY_SIZE];
		
		DataInputStream input = null;
		
		try
		{
			input = new DataInputStream(new FileInputStream(new File(file)));
			int bytesRead = 0;
			while (true)
			{
				bytesRead = input.read(unEncodedBytes);
				encode(bytesRead, unEncodedBytes, encodedBytes);
				writeCodedData(encodedBytes, outBuff);
				if (bytesRead < unEncodedBytes.length)
				{
					break;
				}
			}
			input.close();
			// create the header.
			String mimeHeader = MIME_PREFIX + mimeExtension + MIME_SUFFIX;
			outBuff.insert(0, mimeHeader.toCharArray());
		}
		catch (IOException ioe)
		{
			return false;
		}
		
		return true;
	}
	
	/** Takes two arrays of bytes; one plain text, and one the destination for the coded bytes.
	*/
	protected static void encode(int bytesRead, byte[] plainText, byte[] codeText)
	{
		// Take three bytes in and put out four bytes (usually).
		switch(bytesRead)
		{
			case 1:
				codeText[0] = code[(plainText[0] & 252) >> 2];
				codeText[1] = code[(plainText[0] & 3) << 4];
				codeText[2] = fillChar;
				codeText[3] = fillChar;
				break;
				
			case 2:
				codeText[0] = code[(plainText[0] & 252) >> 2];
				codeText[1] = code[((plainText[0] & 3) << 4) | ((plainText[1] & 240) >> 4)];
				codeText[2] = code[(plainText[1] & 15) << 2];
				codeText[3] = fillChar;
				break;
				
			case 3:
				codeText[0] = code[(plainText[0] & 252) >> 2];
				codeText[1] = code[((plainText[0] & 3) << 4) | ((plainText[1] & 240) >> 4)];
				codeText[2] = code[((plainText[1] & 15) << 2) | ((plainText[2] & 192) >> 6)];
				codeText[3] = code[plainText[2] & 63];
				break;
				
			default:
				break;
		}
	}
	
	/** This method takes the four byte array and writes it byte by byte to the Output
	*	StringBuffer.
	*/
	protected static void writeCodedData(byte[] encodedByte, StringBuffer outBuff)
	{
		for (int i = 0; i < encodedByte.length; i++)
		{
			// we need to output 76 chars but they are numbered starting with '0'.
			if (outBuff.length() % 77 == 0)
			{
				outBuff.append("\n");
			}
			outBuff.append((char)encodedByte[i]);
		}
	}
}