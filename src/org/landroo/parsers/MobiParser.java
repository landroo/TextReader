package org.landroo.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.landroo.textreader.TextLink;

import android.os.Environment;
import android.util.Log;

public class MobiParser implements ParserBase
{
	private static final String TAG = "textReader";	// Log Tag
	
	private String docPath = "";					// document path
	private String docName = "";					// document name
	private String sState = "";						// paragraph numbers
	private String sOrigPath = "";					// the original path of the document
	
	public MobiParser()
	{
		docPath = Environment.getExternalStorageDirectory() + "/TextReader";
    	File tmpDir = new File(docPath);
    	if(!tmpDir.exists()) 
    	{
            try 
            {
                if (Environment.getExternalStorageDirectory().canWrite()) 
                {
                	tmpDir.mkdirs();
                }
            } 
            catch(Exception ex) 
            {
            }
        }
	}
	
	/**
	 * extract mobi file
	 * @param sFileName document file name
	 * @return
	 */
	public boolean parseMobi(String sFileName)
    {
    	sOrigPath = sFileName;
		docName = sFileName.substring(sFileName.lastIndexOf("/") + 1);

    	File tmpDir = new File(docPath + "/" + docName);
    	if(!tmpDir.exists()) 
    	{
            try 
            {
                if(Environment.getExternalStorageDirectory().canWrite()) 
                {
                	tmpDir.mkdirs();
                
                    TextLink tl = new TextLink();
                    String sText = tl.getPrcText(sFileName, docPath + "/" + docName);
                    if(sText.length() > 0 && !sText.substring(sText.length() - 1).equals("0"))
                    {
                    	return false;
                    }
                }
            } 
            catch(Exception ex) 
            {
                return false;
            }
	    }
    	
    	return true;
    }
	
	/**
	 * return the extracted documnet name
	 * @return
	 */
	@Override
	public String getFileName()
	{
		return docPath + "/" + docName + "/index.html"; 
	}
	
	/**
	 * save page number
	 * @param iPage
	 */
	@Override
	public synchronized void savePage(int iPage)
	{
    	if(docName.equals(""))
    	{
    		return;
    	}
    	String sFile = docPath + "/" + docName + "/" + docName + ".dat";
    	String text = null;
    	try
    	{
    		FileWriter fstream = new FileWriter(sFile);
    		BufferedWriter out = new BufferedWriter(fstream);

    		text = iPage + "\n";
			out.write(text);
			
    		text = sState;
			out.write(text);
			
    		text = sOrigPath;
			out.write(text);
			
    		out.close();
    	}
    	catch (Exception e)
    	{
    		Log.i(TAG, "Error in savePage");
    	}
	}
	
	/**
	 * load page number
	 * @return
	 */
	@Override
	public synchronized String loadLast(String fileName)
	{
		docName = fileName.substring(fileName.lastIndexOf("/") + 1);
    	String sFile = docPath + "/" + docName + "/" + docName + ".dat";
    	String text = null;
    	String sRet = "";
    	try
    	{
    		BufferedReader reader = new BufferedReader(new FileReader(sFile));
    		while((text = reader.readLine()) != null) 
    		{
    			sRet += text + "\n";

    			sOrigPath = text;
    		}
    		reader.close();
    	}
    	catch (Exception e)
    	{
    		Log.i(TAG, "Error in loadPage");
    	}
    	
    	return sRet;
	}
	
	/**
	 * return the thumbnail name
	 * @return
	 */
	@Override
	public String getImageName()
	{
		return docPath + "/" + docName + "/" + docName + ".thu";
	}

	/**
	 * set the paragraph numbers
	 * @param state
	 */
	@Override
	public void setState(String state)
	{
		sState = state;
	}
	
	@Override
	public String getOrigPath()
	{
		return sOrigPath;
	}
}
