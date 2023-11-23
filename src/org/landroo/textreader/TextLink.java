package org.landroo.textreader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;

import android.util.Log;

public class TextLink 
{
	// Java_org_landroo_textreader_TextLink_htmToTxt
	static 
    {
        System.loadLibrary("textlink");
    }
    private native byte[] htmToTxt(String sPath, int iMode);	// htm to text jni function
    private native byte[] prcToTxt(String sInFile, String sOutFolder);	// prc to text jni function

    /**
     * Get pure text from a html
     * @param sPath the path of the html file
     * @param iMode
     * @return pure text
     */
    public String getText(String sPath, int iMode)
    {
        String tmpFile = sPath + ".txt";
        
		File file = new File(tmpFile);
    	if(!file.exists())
		{
        	try
        	{
        		byte[] resBytes = htmToTxt(sPath, iMode);
        		
        		FileOutputStream fos = new FileOutputStream(tmpFile);
        		fos.write(resBytes);
        		fos.close();
        	}
        	catch(OutOfMemoryError ex)
        	{
        		Log.i("TextLink", "Out of memory error in textlink!");
        	}
        	catch(Exception ex)
        	{
        		Log.i("TextLink", ex.getMessage());
        	}
		}
    	
   		return loadTxt(tmpFile);
    }
    
    public String loadUTF(String sPath)
    {
    	byte[] resBytes = null;
        String tmpFile = sPath + ".txt";
        
		File file = new File(tmpFile);
    	if(file.exists())
		{
        	try
        	{
        		resBytes = htmToTxt(tmpFile, 10);
        	}
        	catch(OutOfMemoryError ex)
        	{
        		Log.i("TextLink", "Out of memory error in textlink!");
        	}
        	catch(Exception ex)
        	{
        		Log.i("TextLink", ex.getMessage());
        	}
		}
    	
    	String sRes = "Encoding error!";
    	
    	try
		{
			sRes = new String(resBytes, "windows-1250");
		}
		catch (Exception e)
		{
			sRes = new String(resBytes);
		}
    	
   		return sRes; 
    }
   
    /**
     * extract mobi file
     * @param sInFile mobi file
     * @param sOutFile output path
     * @return 
     */
    public String getPrcText(String sInFile, String sOutFile)
    {
        String text = null;
    	try
    	{
    		byte[] resBytes = prcToTxt(sInFile, sOutFile);
    		text = new String(resBytes);
    	}
    	catch(Exception ex)
    	{
    		Log.i("TextLink", ex.getMessage());
    	}
    	
    	return text;
    }
    
    /**
     * load a text file
     * @param sFileName text file name
     * @return the content of the text file
     */
    private String loadTxt(String sFileName)
    {
    	try 
    	{
    		BufferedReader input = new BufferedReader(new FileReader(sFileName));
    		File file = new File(sFileName);
    		long length = file.length();
    		char[] charBuff = new char[(int)length];
    		input.read((char[])charBuff);
    		input.close();
    		
    		return new String(charBuff);
    		/*
    		File file = new File(sFileName);
    		byte [] fileData = new byte[(int)file.length()];
    		DataInputStream dis = new DataInputStream(new FileInputStream(file));
    		dis.readFully(fileData);
    		dis.close();
    		
    		return new String(fileData, "UTF-8");//"UTF-8" "WINDOWS-1250" "ISO-8859-2"
    		*/
    	}
    	catch(OutOfMemoryError ex)
    	{
    		Log.i("TextLink", "Out of memory error in textlink!");
    	}    	
    	catch(Exception ex)
    	{
    		Log.i("TextLink", "File error in textlink!");
    	}
    	
    	return "";
    }
}
