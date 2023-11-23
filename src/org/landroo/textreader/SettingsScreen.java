package org.landroo.textreader;

import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsScreen extends PreferenceActivity 
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		Preference button = (Preference)findPreference("cleanButton");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() 
		{
			@Override
			public boolean onPreferenceClick(Preference arg0) 
			{ 
				cleanUp();
				SettingsScreen.this.finish();
			    return true;
			}
		});
	}
	
	private void cleanUp()
	{
		String docPath = Environment.getExternalStorageDirectory() + "/TextReader";
		chekDirs(docPath);
	}
	
    private void chekDirs(String dirRoot)
    {
    	File f = new File(dirRoot);
    	File[] files = f.listFiles();
    	if(files == null) return;
    	
    	File file;
    	String filePath;
    	for(int i = 0; i < files.length; i++)
    	{
    		file = files[i];
    		filePath = file.getPath();
    		if(file.isDirectory()) chekDirs(filePath);
    		
   			file.delete();
    	}
	}
}
