package org.landroo.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;


import android.os.Environment;
import android.util.Log;

public class TextParser implements ParserBase 
{
	private static final String TAG = "textReader";
	
	private String sText = "";					// the main text
	private String docPath = "";				// document path
	private String docName = "";				// document name
	private String sState = "";					// paragraph numbers
	private String sOrigPath = "";				// the original path of the document
	
	public TextParser()
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
	 * load text file
	 * @param sFileName
	 * @return
	 */
    public boolean parseTxt(String sFileName)
    {
    	sOrigPath = sFileName;
		docName = sFileName.substring(sFileName.lastIndexOf("/") + 1);
   	
		byte[] data;
		try 
		{
			File file = new File(sFileName);
			long length = file.length();
			
			data = new byte[(int)length];
			InputStream in = new FileInputStream(file);
			in.read(data);
			in.close();
			
			if(data[0] == -1 && data[1] == -2)
			{
				sText = new String(data, "UTF-16");
			}
			else
			{
				sText = new String(data, "UTF-8");
			}
			sText = sText.replaceAll("�", "");
			sText = sText.replaceAll("�\r\n", "");
			sText = sText.replaceAll("�", "");
		}
		catch(OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in text parser!");
			data = null;
			sText = null;
    		System.gc();
			return false;
		}
    	catch(Exception ex)
    	{
    		return false;
    	}
		
    	return true;
    }
    
    /**
     * retunr the text
     * @return
     */
    public String getText()
    {
    	return sText;
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
    	String sFile = docPath + "/" + docName + ".dat";
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
    	String sFile = docPath + "/" + docName + ".dat";
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
	 * return thumbnail name
	 * @return
	 */
	@Override
	public String getImageName()
	{
		return docPath + "/" + docName + ".thu";
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
	
	/**
	 * return the original file path
	 * @return
	 */
	@Override
	public String getOrigPath()
	{
		return sOrigPath;
	}

	@Override
	public String getFileName() 
	{
		// TODO Auto-generated method stub
		return "";
	}
}
