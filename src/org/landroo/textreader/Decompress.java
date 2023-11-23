package org.landroo.textreader;

import android.util.Log; 

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.util.zip.ZipEntry; 
import java.util.zip.ZipInputStream; 
 
public class Decompress 
{ 
	private static final String TAG = "textReader";
	
	private String zipFile; 
	private String location; 
 
	public Decompress(String sFile, String sLocation) 
	{ 
		zipFile = sFile; 
		location = sLocation; 
 
		checkDir(""); 
	} 
 
	public void unzip() 
	{ 
		try
		{
			FileInputStream in = new FileInputStream(zipFile);
			BufferedInputStream fin = new BufferedInputStream(in);
			ZipInputStream zin = new ZipInputStream(fin); 
			ZipEntry ze = null;
			byte b[] = new byte[65536];
			int i;
			while((ze = zin.getNextEntry()) != null) 
			{ 
				Log.v("Decompress", "Unzipping " + ze.getName()); 
				if(ze.isDirectory()) 
				{ 
					checkDir(ze.getName());
					continue;
				}
				checkPath(ze.getName());
				try
				{
					FileOutputStream fout = new FileOutputStream(location + "/" + ze.getName()); 
					BufferedOutputStream out = new BufferedOutputStream(fout);
					while((i = zin.read(b, 0, 65536)) > 0) 
					{ 
						out.write(b, 0, i); 
					} 
 
					zin.closeEntry(); 
					out.close();
				} 
				catch(Exception e) 
				{ 
					Log.e(TAG, "unzip out", e); 
				} 
			} 
			zin.close(); 
		} 
		catch(Exception e) 
		{ 
			Log.e(TAG, "unzip in", e); 
		} 
 	} 
 
	private void checkDir(String dir) 
	{ 
		File f = new File(location + "/" + dir); 
 
		if(!f.isDirectory()) 
		{ 
			f.mkdirs(); 
		} 
	} 
	
	private void checkPath(String sFile)
	{
		if(sFile.indexOf("/") > -1)
		{
			String[] aPath = sFile.split("/");
			String sPath = location;
			for(int i = 0; i < aPath.length - 1; i++)
			{
				if(!aPath[i].equals(""))
				{
					sPath += "/" + aPath[i];
					File f = new File(sPath);
					if(!f.isDirectory()) 
					{ 
						f.mkdirs(); 
					}
				}
			}
		}
	}
} 