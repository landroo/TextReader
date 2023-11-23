package org.landroo.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.landroo.textreader.Decompress;
import org.landroo.textreader.TextLink;
import org.landroo.textreader.TextPos;

import android.os.Environment;
import android.util.Log;

public class DocXParser implements ParserBase 
{
	private static final String TAG = "textReader";	// Log Tag

	private String sText = "";						// the main text
	private String docPath = "";					// document path
	private String docName = "";					// document name
	private String wordName = "";					// word xml name
	private String sState = "";						// paragraph numbers
	private String sOrigPath = "";					// the original path of the document
	
	private ArrayList<TextPos> imageList = new ArrayList<TextPos>();
	
	public DocXParser()
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
	 * parse docx file
	 * @param sFileName file name
	 * @return true if success
	 */
	public boolean parseDocX(String sFileName)
    {
    	sOrigPath = sFileName;
		docName = sFileName.substring(sFileName.lastIndexOf("/") + 1);
		
    	File tmpDir = new File(docPath + "/" + docName);
    	if(!tmpDir.exists()) 
    	{
            try 
            {
                if (Environment.getExternalStorageDirectory().canWrite()) 
                {
                	tmpDir.mkdirs();
                
        			Decompress d = new Decompress(sFileName, docPath + "/" + docName); 
        			d.unzip();
                }
            } 
            catch(Exception ex) 
            {
                return false;
            }
	    }

		// /_rels/.rels
		String sMetaFile = docPath + "/" + docName + "/_rels/.rels";
		String sTmpText = loadTxt(sMetaFile);
		if(sTmpText.equals(""))
		{
			return false;
		}
		
		//<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		//<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
		//	<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
		//	<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
		//	<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
		//</Relationships>
		
		// relationships/officeDocument Target
		sTmpText = sTmpText.replace("\n", " ");
		sTmpText = sTmpText.replace("\r", " ");
		sTmpText = sTmpText.replace("/>", "/>\n");
    	String[] sLine = sTmpText.split("\n");
    	String sContent = "";
    	for(int i = 0; i < sLine.length; i++)
    	{
    		sContent = sLine[i];
    		if(sContent.indexOf("relationships/officeDocument") != -1)
    		{
    			int is = sContent.indexOf("Target") + 6;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"") + 1;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"");
    			sContent = sContent.substring(0, is);
    			break;
    		}
    	}

    	wordName = docPath + "/" + docName + "/" + sContent;
        TextLink tl = new TextLink();
        sText = tl.getText(wordName, 0);
        
        // parse text
        String[] sDoc = sText.split("\r");
        sText = sDoc[0];
		String[] sPairs;
		
		// parse images
		imageList.clear();
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
    	
    	if(imageList.size() == 0)
    	{
    		return true;
    	}

		// /word/_rels/document.xml.rels
		sMetaFile = docPath + "/" + docName + "/word/_rels/document.xml.rels";
		sTmpText = loadTxt(sMetaFile);
		if(sTmpText.equals(""))
		{
			return false;
		}
		
		//<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
		//<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
		//	<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/webSettings" Target="webSettings.xml"/>
		//	<Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
		//	<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
		//	<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
		//	<Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
		//	<Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image2.png"/>
		//	<Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.png"/>
		//</Relationships>
    	
		// image Target
		sTmpText = sTmpText.replace("\n", " ");
		sTmpText = sTmpText.replace("\r", " ");
		sTmpText = sTmpText.replace("/>", "/>\n");
    	sLine = sTmpText.split("\n");
    	sContent = "";
    	String sImgPath;
    	for(int i = 0; i < sLine.length; i++)
    	{
    		sContent = sLine[i];
    		if(sContent.indexOf("relationships/image") != -1)
    		{
    			int is = sContent.indexOf("Target") + 6;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"") + 1;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"");
    			sContent = sContent.substring(0, is);
    			sImgPath = sContent;
    			
    			sContent = sLine[i];
    			is = sContent.indexOf("Id") + 2;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"") + 1;
    			sContent = sContent.substring(is);
    			is = sContent.indexOf("\"");
    			sContent = sContent.substring(0, is);

    			for(int j = 0; j < imageList.size(); j++)
    			{
    				if(imageList.get(j).msText.equals(sContent))
    				{
    					imageList.get(j).msText = sImgPath;
    					break;
    				}
    			}
    		}
    	}
		
    	return true;
    }
	
	public String getText()
	{
		return sText;
	}
	
	public ArrayList<TextPos> getImageList()
	{
		return imageList;
	}

    private String loadTxt(String sFileName)
    {
    	try 
    	{
    		BufferedReader input = new BufferedReader(new FileReader(sFileName));
    		File file = new File(sFileName);
    		long length = file.length();
    		char[] charBuff = new char[(int)length];
    		input.read(charBuff);
    		input.close();
    		
    		return new String(charBuff);
    	}
    	catch(OutOfMemoryError ex)
    	{
    		Log.i("TextLink", "Out of memory error DocX parser!");
    	}    	
    	catch(Exception ex)
    	{
    		Log.i("TextLink", "File error in DocX parser!");
    	}
    	
    	return "";
    }

	@Override
	public String getFileName() 
	{
		return wordName;
	}

	@Override
	public void savePage(int iPage) 
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

	@Override
	public String loadLast(String fileName) 
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
	public String getOrigPath() 
	{
		return sOrigPath;
	}

}
