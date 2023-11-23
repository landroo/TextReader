package org.landroo.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.landroo.textreader.Decompress;
import org.landroo.textreader.TextPos;

import android.os.Environment;
import android.util.Log;

public class EPubParser implements ParserBase
{
	private static final String TAG = "textReader";

	private int charpterNo = 0;
	private ArrayList<Chapters> fileList;
	private String docPath = "";
	private String docName = "";
	private String sState = "";
	private String sOrigPath = ""; // the original path of the document
	
	public class Chapters
	{
		public String filePath;
		public String fileTitle;
		public String fileName;
		
		public Chapters(String path, String title, String name)
		{
			filePath = path;
			fileTitle = title;
			fileName = name;
		}
	}

	public EPubParser()
	{
		fileList = new ArrayList<Chapters>();
		
		docPath = Environment.getExternalStorageDirectory() + "/TextReader";
		File tmpDir = new File(docPath);
		if (!tmpDir.exists())
		{
			try
			{
				if (Environment.getExternalStorageDirectory().canWrite()) tmpDir.mkdirs();
			}
			catch (Exception ex)
			{
				Log.i(TAG, "Cannot create directory: " + docPath);
			}
		}
	}

	public int chapterNo()
	{
		return fileList.size();
	}

	public int currChapter()
	{
		return charpterNo;
	}

	public String nextChapter()
	{
		String sRet = "";
		if (charpterNo + 1 < fileList.size())
		{
			charpterNo++;
			sRet = fileList.get(charpterNo).filePath;
			savePage(0);
		}

		return sRet;
	}

	public String prewChapter()
	{
		String sRet = "";
		if (charpterNo - 1 > -1)
		{
			charpterNo--;
			if(charpterNo >= fileList.size()) sRet = fileList.get(0).filePath;
			else sRet = fileList.get(charpterNo).filePath;
			savePage(0);
		}

		return sRet;
	}

	public String getChapter(int iPage)
	{
		String sRet = "";
		if (iPage > -1 && iPage < fileList.size())
		{
			sRet = fileList.get(iPage).filePath;
		}

		return sRet;
	}

	public boolean isNextChapter()
	{
		if (charpterNo + 1 < fileList.size()) return true;

		return false;
	}

	public boolean isPrewChapter()
	{
		if (charpterNo - 1 > -1) return true;

		return false;
	}

	public boolean setChapter(String sPage)
	{
		for (int i = 0; i < fileList.size(); i++)
		{
			if (sPage.equals(fileList.get(i).filePath))
			{
				charpterNo = i;
				savePage(0);
				return true;
			}
		}

		return false;
	}
	
	public boolean parseEpub(String sFileName)
	{
		sOrigPath = sFileName;
		docName = sFileName.substring(sFileName.lastIndexOf("/") + 1);
		File tmpDir = new File(docPath + "/" + docName);
		if (!tmpDir.exists())
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
			catch (Exception ex)
			{
				return false;
			}
		}

		// /META-INF/container.xml
		String sMetaFile = docPath + "/" + docName + "/META-INF/container.xml";
		String sText = loadTxt(sMetaFile);
		if (sText.equals("")) return false;

		// <rootfile full-path="Ops/content.opf"
		// media-type="application/oebps-package+xml" />
		// <rootfile xmlns="urn:oasis:names:tc:opendocument:xmlns:container"
		// full-path="OEBPS/html/0132711907.opf"
		// media-type="application/oebps-package+xml"/>
		sText = sText.replace("\n", " ");
		sText = sText.replace("\r", " ");
		sText = sText.replace("/>", "/>\n");
		int is = sText.indexOf("rootfile ") + 9;
		String sContent = sText.substring(is);
		is = sContent.indexOf("full-path") + 9;
		sContent = sContent.substring(is);
		is = sContent.indexOf("\"") + 1;
		sContent = sContent.substring(is);
		is = sContent.indexOf("\"");
		sContent = sContent.substring(0, is);

		String sIndexFile = docPath + "/" + docName + "/" + sContent;
		sText = loadTxt(sIndexFile);
		if (sText.equals("")) return false;

		String sBookPath = sIndexFile.substring(0, sIndexFile.lastIndexOf("/"));
		parseContent(sText, sBookPath);

		if (fileList.size() > 0) return true;

		return false;
	}

	/**
	 * get file path data from xml
	 */
	private void parseContent(String sText, String sPath)
	{
		sText = sText.replace("\n", " ");
		sText = sText.replace("\r", " ");
		sText = sText.replace("/>", "/>\n");
		String[] sLines = sText.split("\n");
		int is;
		String sLine;
		ArrayList<String> items = new ArrayList<String>();

		// <itemref idref="coverpage"/>
		for (int i = 0; i < sLines.length; i++)
		{
			sLine = sLines[i];
			is = sLine.indexOf("<itemref ");
			if (is > -1)
			{
				sLine = sLine.substring(is + 9);
				is = sLine.indexOf("\"") + 1;
				sLine = sLine.substring(is);
				is = sLine.indexOf("\"");
				sLine = sLine.substring(0, is);
				items.add(sLine);
			}
		}

		// <item id="Chapter01" href="Chapter01.html"
		// media-type="application/xhtml+xml"/>
		// <item href="toc.ncx" id="ncx" media-type="application/x-dtbncx+xml"/>
		String sProp;
		for (int i = 0; i < sLines.length; i++)
		{
			sLine = sLines[i];
			is = sLine.indexOf("<item ");
			if (is > -1)
			{
				sProp = sLine.substring(is + 5);
				is = sProp.indexOf(" id");
				sProp = sProp.substring(is + 3);
				is = sProp.indexOf("\"") + 1;
				sProp = sProp.substring(is);
				is = sProp.indexOf("\"");
				sProp = sProp.substring(0, is);

				for (int j = 0; j < items.size(); j++)
				{
					if (sProp.equals(items.get(j)))
					{
						sLine = sLines[i];
						is = sLine.indexOf("href") + 4;
						sLine = sLine.substring(is);
						is = sLine.indexOf("\"") + 1;
						sLine = sLine.substring(is);
						is = sLine.indexOf("\"");
						sLine = sLine.substring(0, is);
						fileList.add(new Chapters(sPath + "/" + sLine, sProp, sLine));
						break;
					}
				}
			}
		}
		
		setPageTitles();
		
		return;
	}

	/**
	 * load a text feile into memory
	 * 
	 * @param sFileName
	 * @return
	 */
	private String loadTxt(String sFileName)
	{
		try
		{
			BufferedReader input = new BufferedReader(new FileReader(sFileName));
			File file = new File(sFileName);
			long length = file.length();
			char[] charBuff = new char[(int) length];
			input.read(charBuff);
			input.close();

			return new String(charBuff);
		}
		catch (OutOfMemoryError ex)
		{
			Log.i("TextLink", "Out of memory error in ePub parser!");
		}
		catch (Exception ex)
		{
			Log.i("TextLink", "File error in ePub parser!");
		}

		return "";
	}

	/**
	 * save state
	 */
	@Override
	public synchronized void savePage(int iPage)
	{
		if (docName.equals("")) return;

		String sFile = docPath + "/" + docName + "/" + docName + ".dat";
		String text = null;
		try
		{
			FileWriter fstream = new FileWriter(sFile);
			BufferedWriter out = new BufferedWriter(fstream);

			text = charpterNo + "\n";
			out.write(text);

			text = iPage + "\n";
			out.write(text);

			// TODO
			if(iPage == 0) sState = "";
//			Log.i(TAG, "savePage " + iPage + "\n" + sState);
			
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
		
		return;
	}

	/**
	 * load state
	 */
	@Override
	public synchronized String loadLast(String fileName)
	{
		docName = fileName.substring(fileName.lastIndexOf("/") + 1);
		String sFile = docPath + "/" + docName + "/" + docName + ".dat";
		String text = null;
		String sRet = "";
		int iCnt = 0;
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(sFile));
			while ((text = reader.readLine()) != null)
			{
				switch (iCnt++)
				{
				case 0:
					charpterNo = Integer.parseInt(text);
					break;
				default:
					sRet += text + "\n";
				}
				sOrigPath = text;
			}
			reader.close();
		}
		catch (Exception e)
		{
			Log.i(TAG, "Error in loadLast");
		}

		return sRet;
	}

	@Override
	public String getImageName()
	{
		return docPath + "/" + docName + "/" + docName + ".thu";
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

	@Override
	public String getFileName()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getCotentText()
	{
		String sTxt = "";
		for(Chapters ch: this.fileList)	sTxt += ch.fileTitle + "\n";
		
		return sTxt;
	}
	
	public ArrayList<TextPos> getCotentLinks()
	{
		ArrayList<TextPos> aRet = new ArrayList<TextPos>();
		
		int pos = 0;
		for(Chapters ch: this.fileList)
		{
			TextPos tp = new TextPos(0, pos, ch.fileName, 0, false, false);
			aRet.add(tp);
			
			pos += ch.fileTitle.length() + 1;
		}
		
		return aRet;
	}
	
	private void setPageTitles()
	{
		int is, ie;
		String sTitle;
		for(Chapters ch: this.fileList)
		{
			String sText = loadTxt(ch.filePath);
			is = sText.indexOf("<h1");
			ie = sText.indexOf("</h1>");	
			if(is > 0 && ie > 0)
			{
				is = ie;
				while(!sText.substring(is, is + 1).equals(">")) is--;
				sTitle = sText.substring(is + 1, ie);
				if(!sTitle.equals("")) ch.fileTitle = sTitle.replace(" ", "·");
			}
			else
			{
				is = sText.indexOf("<h2");
				ie = sText.indexOf("</h2>");
				if(is > 0 && ie > 0)
				{
					is = ie;
					while(!sText.substring(is, is + 1).equals(">")) is--;
					sTitle = sText.substring(is + 1, ie);
					if(!sTitle.equals("")) ch.fileTitle = sTitle.replace(" ", "·");
				}
				else
				{
					is = sText.indexOf("<title>");
					ie = sText.indexOf("</title>");
					if(is > 0)
					{
						sTitle = sText.substring(is + 7, ie); 
						if(!sTitle.equals("")) ch.fileTitle = sTitle.replace(" ", "·");
					}
				}
			}
		}
		return;
	}

}
