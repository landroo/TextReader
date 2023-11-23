package org.landroo.textreader;

import android.os.Bundle;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.tapfortap.TapForTap;

public class AndroidExplorer extends Activity
{
	private static final String TAG = "AndroidExplorer";
	
	private ListView fileListView;
	private fileListAdapter fileAdapter;
	
	private List<String> filePath = null;
	private List<String> imagePath = null;
	private TextView myPath;
	private SharedPreferences settings;		// preferences
	private String lastDir;
	private String prevDir;
	
	private Gallery galery = null;
	private imageListAdapter imageAdapter;
	
	private Bitmap[] icons = new Bitmap[6];
	
	private AlertDialog.Builder deleteDialogBuilder;
	private String bookPath;

    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        TapForTap.initialize(this, "a782d62089d03a05d2e42061d6ea16d6");
        
        setContentView(R.layout.explorer);
        
        fileListView = (ListView) findViewById(R.id.file_list);
        fileListView.setOnItemClickListener(new OnItemClickListener() 
        {
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
        	{
        		String sText = filePath.get(arg2);
        		changeDir(sText);
        	}
        });
		
	    // setting
	    settings = getSharedPreferences("org.landroo.textreader_preferences", MODE_PRIVATE);
	    String lastDir = settings.getString("lastdir", "/");
	    lastDir = lastDir.substring(0, lastDir.lastIndexOf("/"));
	    if(lastDir.equals("")) lastDir = "" + Environment.getExternalStorageDirectory();
       
        myPath = (TextView)findViewById(R.id.path);
        getDir(lastDir);
        
        imageAdapter = new imageListAdapter(this);
        
        collectImages();
        
        galery = (Gallery) findViewById(R.id.gallery);
        galery.setAdapter(imageAdapter);
        galery.setOnItemClickListener(new OnItemClickListener()
        {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
			{
				String fileName = imagePath.get(arg2);
				if(fileName.length() > 0)
				{
					fileName = fileName.substring(0, fileName.length() - 4);
			        Bundle bundle = new Bundle();
			       	bundle.putString("file", fileName);
			       	
					Intent intent = new Intent(AndroidExplorer.this, TextReaderActivity.class);
					intent.putExtras(bundle);
					startActivityForResult(intent, 1);
					
					AndroidExplorer.this.finish();
				}
			}
        });
        galery.setOnItemLongClickListener(new OnItemLongClickListener()
        {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				bookPath = imagePath.get(arg2);
				String sMessage = getResources().getString(R.string.deletebook);
				String yes = getResources().getString(R.string.yes);
				String no = getResources().getString(R.string.no);
				if(bookPath.length() > 0)
				{
					deleteDialogBuilder = new AlertDialog.Builder(AndroidExplorer.this);
					deleteDialogBuilder.setMessage(sMessage);
					deleteDialogBuilder.setCancelable(true);
					deleteDialogBuilder.setPositiveButton(yes, new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog,	int id) 
						{
							
							cleanUp(bookPath);
							collectImages();
							dialog.dismiss();
						}
					});
					deleteDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog,	int id) 
						{
							dialog.cancel();
						}
					});
					deleteDialogBuilder.create().show();
				}
				return false;
			}
		});
        
        icons[0] = BitmapFactory.decodeResource(getResources(), R.drawable.document_text);
        icons[1] = BitmapFactory.decodeResource(getResources(), R.drawable.document_html);
        icons[2] = BitmapFactory.decodeResource(getResources(), R.drawable.document_epub);
        icons[3] = BitmapFactory.decodeResource(getResources(), R.drawable.document_mobi);
        icons[4] = BitmapFactory.decodeResource(getResources(), R.drawable.document_docx);
        icons[5] = BitmapFactory.decodeResource(getResources(), R.drawable.document_plain);
        
        System.runFinalizersOnExit(true);
    }
    
    @Override
    public void onBackPressed() 
    {
    	String sText = "";
    	if(filePath != null && filePath.size() > 0)
		{
    		sText = filePath.get(0);
        	if(!sText.subSequence(0, 6).equals("  ../\t"))
        	{
        		android.os.Process.killProcess(android.os.Process.myPid());
        		finish();
        	}
        	else changeDir(sText);
		}
    	else
    	{
    		android.os.Process.killProcess(android.os.Process.myPid());
    		AndroidExplorer.this.finish();
    	}
    }
    
    @Override
	protected void onResume()
	{
		super.onResume();

		collectImages();
	}
    
    @Override
    protected void onStop()
    {
    	super.onStop();
    	SharedPreferences settings = getSharedPreferences("org.landroo.textreader_preferences", MODE_PRIVATE);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("lastdir", lastDir);
    	editor.commit();
    }

    private void getDir(String dirPath)
    {
		prevDir = dirPath;
    	myPath.setText(dirPath);
    	filePath = new ArrayList<String>();
    	
    	File f = new File(dirPath);
    	File[] files = f.listFiles();
    	if(files == null)
    	{
    		dirPath = "/";
    		f = new File(dirPath);
    		files = f.listFiles();
    	}
    	
    	if(!dirPath.equals("/"))
    	{
    		filePath.add("  /\t  /\t/");
    		filePath.add("  ../\t  ../\t" + f.getParent());
    	}

    	File file;
    	for(int i = 0; i < files.length; i++)
    	{
    		file = files[i];
    		if(file.isDirectory()) filePath.add(" " + file.getName().toLowerCase() + "\t" + " " + file.getName() + "\t" + file.getPath());
    		else filePath.add(file.getName().toLowerCase() + "\t" + file.getName() + "\t" + file.getPath());
    	}
    	Collections.sort(filePath);

    	fileAdapter = new fileListAdapter(this, (ArrayList<String>)filePath);
		fileListView.setAdapter(fileAdapter);
	}
    
    private void changeDir(String sText) 
    {
		String[] sPath = sText.split("\t");
		sText = sPath[2];

    	File file = new File(sText);
    	
    	if(file.isDirectory())
    	{
    		if(file.canRead()) getDir(sText);
    		else
    		{
    			new AlertDialog.Builder(this)
    			.setIcon(R.drawable.private_folder)
    			.setTitle("[" + file.getName() + "] folder can't be read!")
    			.setPositiveButton("OK", new DialogInterface.OnClickListener() 
    			{
    				@Override
    				public void onClick(DialogInterface dialog, int which) 
    				{
    				}
    			}).show();
    		}
    	}
    	else
    	{
    		lastDir = file.getPath();
	        Bundle bundle = new Bundle();
	       	bundle.putString("file", lastDir);
	       	
			Intent intent = new Intent(AndroidExplorer.this, TextReaderActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
			
			AndroidExplorer.this.finish();
    	}
    }
   
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if(requestCode == 1 && resultCode == 1) 
        {
        	android.os.Process.killProcess(android.os.Process.myPid());
        	finish();
        }
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * 
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle item selection
		switch (item.getItemId()) 
		{
		case R.id.menu_setup:
			Intent SettingsIntent = new Intent(this, SettingsScreen.class);
			startActivity(SettingsIntent);
			getDir(prevDir);
			collectImages();
			return true;
			
		case R.id.menu_refresh:
			getDir(prevDir);
			collectImages();
			return true;
			
		case R.id.menu_exit:
			this.setResult(1);
	    	int pid = android.os.Process.myPid(); 
	    	android.os.Process.killProcess(pid);
			this.finish();
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public class imageListAdapter extends BaseAdapter 
	{
		private Context context;
		private FileInputStream in;
		private BufferedInputStream buf;
		private byte[] bMapArray;
        
        public imageListAdapter(Context c) 
        {
        	context = c;
        }

        public int getCount() 
        {
            return imagePath.size();
        }

        public Object getItem(int position) 
        {
            return position;
        }

        public long getItemId(int position) 
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) 
        {
            if(imagePath == null) return null;
            
            ImageView iv = new ImageView(context);
            
            try 
            {
            	in = new FileInputStream(imagePath.get(position));
            	buf = new BufferedInputStream(in, 1024);
                bMapArray = new byte[buf.available()];
                buf.read(bMapArray);
                Bitmap bMap = BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);

                iv.setImageBitmap(bMap);
                
                if(in != null) in.close();
                if(buf != null) buf.close();
                bMapArray = null;
                System.gc();
            } 
    		catch(OutOfMemoryError e)
    		{
    			Log.e(TAG, "Out of memory error in imageListAdapter!");
        		System.gc();
    		}
            catch(Exception ex) 
            {
                Log.e(TAG, ex.toString());
            }            
            
            iv.setAdjustViewBounds(true);
            iv.setLayoutParams(new Gallery.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            
            return iv;
        }
    }
	
	private void collectImages()
	{
		imagePath = new ArrayList<String>();
		String docPath = Environment.getExternalStorageDirectory() + "/TextReader";
		chekDirs(docPath);
		imageAdapter.notifyDataSetChanged();
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
    		if(file.isDirectory()) 	chekDirs(filePath);
    		else if(filePath.length() > 4 && filePath.substring(filePath.length() - 4).equals(".thu")) imagePath.add(filePath);
    	}
	}
    
    private class fileListAdapter extends ArrayAdapter<String> 
	{
    	private ArrayList<String> values;
    	private TextView textView;
    	private View rowView;
    	private LayoutInflater inflater;
    	private ImageView imageView;

    	public fileListAdapter(Context context, ArrayList<String> values) 
    	{
    		super(context, R.layout.row, values);
    		this.values = values;
    		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
        
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) 
    	{
    		try
    		{
    			rowView = convertView;
    			if(rowView == null) rowView = inflater.inflate(R.layout.row, parent, false);
    		
    			textView = (TextView) rowView.findViewById(R.id.label);
    			imageView = (ImageView) rowView.findViewById(R.id.icon);

    			String sExt = "";
    			String sText = values.get(position);
    			String[] sPath = sText.split("\t");
    			sText = sPath[1];
	    		if(sText.indexOf(" ") == 0) 
	    		{
	    			imageView.setImageResource(R.drawable.normal_folder);
					sText = sText.substring(1);
	    		}
	    		else
	    		{
	    			if(sText.lastIndexOf(".") != -1) sExt = sText.substring(sText.lastIndexOf("."));
	    			sExt = sExt.toLowerCase();
	    			if(sExt.equals(".txt")) imageView.setImageBitmap(icons[0]);
	    			else if(sExt.equals(".htm") || sExt.equals(".html") || sExt.equals(".xhtml") || sExt.equals(".xml")) imageView.setImageBitmap(icons[1]);
	    			else if(sExt.equals(".epub")) imageView.setImageBitmap(icons[2]);
	    			else if(sExt.equals(".prc") || sExt.equals(".mobi")) imageView.setImageBitmap(icons[3]);
	    			else if(sExt.equals(".docx")) imageView.setImageBitmap(icons[4]);
	    			else imageView.setImageBitmap(icons[5]);
	    		}
	    		textView.setText(sText);

	        } 
			catch(OutOfMemoryError e)
			{
				Log.e(TAG, "Out of memory error in fileListAdapter!");
	    		System.gc();
			}
    		
    		return rowView;
    	}

	}
    
    private void cleanUp(String sFile)
	{
    	String sPath = sFile.substring(0, sFile.lastIndexOf("/"));
    	String docPath = Environment.getExternalStorageDirectory() + "/TextReader";

    	if(!sPath.equals(docPath))
    	{
			deleteDirs(sPath);
			
	    	File f = new File(sPath);
			f.delete();
			
	    	f = new File(sFile);
			f.delete();
    	}
	}
	
    private boolean deleteDirs(String dirRoot)
    {
    	File f = new File(dirRoot);
    	File[] files = f.listFiles();
    	if(files == null) return false;
    	
    	File file;
    	String filePath;
    	for(int i = 0; i < files.length; i++)
    	{
    		file = files[i];
    		filePath = file.getPath();
    		if(file.isDirectory()) deleteDirs(filePath);
    		
   			file.delete();
    	}
    	
    	return true;
	}

}
