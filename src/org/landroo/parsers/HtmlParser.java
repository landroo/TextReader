package org.landroo.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.landroo.textreader.TextLink;
import org.landroo.textreader.TextPos;

import android.os.Environment;
import android.util.Log;

public class HtmlParser implements ParserBase 
{
	private static final String TAG = "textReader";
	
	private String sText = "";					// the main text
	private String docPath = "";
	private String docName = "";
	//private String originalPath = "";
	private String sState = "";
	private String sOrigPath = "";				// the original path of the document
	
	private ArrayList<TextPos> imageList = new ArrayList<TextPos>();
	private ArrayList<TextPos> linkList = new ArrayList<TextPos>();
	
	public HtmlParser()
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
	
	public String getText()
	{
		return sText;
	}
	
	public ArrayList<TextPos> getImageList()
	{
		return imageList;
	}
	
	public ArrayList<TextPos> getLinkList()
	{
		return linkList;
	}
	
	public String getOrigPath()
	{
		return sOrigPath;
	}
	
	public boolean parseHtml(String sFileName)
    {
		sOrigPath = sFileName;
		docName = sFileName.substring(sFileName.lastIndexOf("/") + 1);

    	// parse Html
		imageList.clear();
		linkList.clear();
        TextLink tl = new TextLink();
        sText = tl.getText(sFileName, 0);
        if(sText.length() > 9 && sText.substring(0, 9).equals("No result"))
        {
        	sText = "Error(1)";
        	return false;
        }
        
        if(sText.indexOf("Ã") != -1) sText = tl.loadUTF(sFileName);
        
        // parse text
        String[] sDoc = sText.split("\r");
        sText = sDoc[0];
		String[] sPairs;
		
		// parse images
    	if(sDoc.length > 1)
    	{
    		String[] sImages = sDoc[1].split("\n");
            if(sImages[0].length() > 0)
            {
		    	for(int i = 0; i < sImages.length; i++)
		    	{
		    		sPairs = sImages[i].split("\t");
		    		if(sPairs.length == 2)
		    		{
		    			int pos = Integer.parseInt(sPairs[0]);
		    			String name = sPairs[1];
		    			if(!name.equals(""))
		    			{
		    				TextPos tp = new TextPos(0, pos, name, 0, false, false);
		    				imageList.add(tp);
		    			}
		    		}
		    	}
	    	}
    	}
    	
    	// parse links
    	sFileName = sFileName.substring(sFileName.lastIndexOf("/") + 1);
    	if(sDoc.length > 2)
    	{
    		String[] sLinks = sDoc[2].split("\n");
    		if(sLinks[0].length() > 0)
    		{
		    	for(int i = 0; i < sLinks.length; i++)
		    	{
		    		sPairs = sLinks[i].split("\t");
		    		if(sPairs.length == 2)
		    		{
		    			if(!sPairs[1].equals(sFileName))
		    			{
			    			int pos = Integer.parseInt(sPairs[0]);
			    			String name = sPairs[1];
			    			if(!name.equals(""))
			    			{
			    				TextPos tp = new TextPos(0, pos, name, 0, false, false);
			    				linkList.add(tp);
			    			}
		    			}
		    		}
		    	}
    		}
    	}
    	
    	if(sText.equals("\n"))
    	{
    		sText = " ";
    	}
    	
    	return true;
    }
	
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
			
    		text = sOrigPath + "\n";
			out.write(text);
    		
			out.close();
    	}
    	catch (Exception e)
    	{
    		Log.i(TAG, "Error in savePage");
    	}
	}
	
	/**
	 * load last page
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
	
	@Override
	public String getImageName()
	{
		return docPath + "/" + docName + ".thu";
	}
	
	@Override
	public void setState(String state)
	{
		sState = state;
	}

	@Override
	public String getFileName() 
	{
		// TODO Auto-generated method stub
		return "";
	}
}
