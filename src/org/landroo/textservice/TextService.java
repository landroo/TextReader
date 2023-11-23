package org.landroo.textservice;

import java.util.ArrayList;
import java.util.Locale;


import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TextService extends Service
{
	private static final String TAG = "textService";
	
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_INT_VALUE = 3;
    public static final int MSG_SET_STRING_VALUE = 4;
    public static final int MSG_SET_TTS_ERROR = 5;

	private ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	private static boolean isRunning = false;
	private static boolean isSpeech = false;
	
	private TextTTS tts = null; // text to speech
	private int ttsStart;
	private int ttsEnd;
	private int ttsLast;
	private String sParags;
	
	/**
	 * Message handler
	 */
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			// new page loaded
			switch(msg.what)
			{
			case 100:
				if(ttsEnd < sParags.length()) readText();
				break;
			case 101:
				sendMessageToUI(3, "");// tts error
				break;
			}
		}
	};
	
    class IncomingHandler extends Handler
    { // Handler of incoming messages from clients.

        @Override
        public void handleMessage(Message msg)
        {
        	try
        	{
	            switch (msg.what)
	            {
	                case MSG_REGISTER_CLIENT:
	                    mClients.add(msg.replyTo);
	                    break;
	                case MSG_UNREGISTER_CLIENT:
	                    mClients.remove(msg.replyTo);
	                    break;
	                case MSG_SET_INT_VALUE:
	                	switch(msg.arg1)
	                	{
	                	case 100:// start speech
	                		if(ttsEnd < sParags.length())
	                		{
	                			tts.start();
	                			if(ttsEnd > 0)
	                			{
	                				ttsEnd = ttsStart;
	                        		ttsStart = ttsLast;
	                			}
	                			readText();
	                		}
	                		break;
	                	case 101:// stop speech
	                		tts.stop();
	                		isSpeech = false;
	                		break;
	                	case 102:// reset speech
	                		tts.stop();
	                		isSpeech = false;
	                		ttsStart = 0;
	                		ttsEnd = 0;
	                		ttsLast = 0;
	                		break;
	                	case 110:// set speech engine to default
	                		tts = new TextTTS(TextService.this, Locale.getDefault(), handler);
	                		break;
	                	case 111:
	                		tts = new TextTTS(TextService.this, Locale.ENGLISH, handler);
	                		break;
	                	case 112:
	                		tts = new TextTTS(TextService.this, Locale.FRENCH, handler);
	                		break;
	                	case 113:
	                		tts = new TextTTS(TextService.this, Locale.GERMAN, handler);
	                		break;
	                	}
	                    break;
	                case MSG_SET_STRING_VALUE:
	                	String sText = msg.getData().getString("tts");
	                	if(sText != null)
	                	{
	                		ttsStart = 0;
	                		ttsEnd = 0;
	                		ttsLast = 0;
	                		sParags = sText;
	                	}
	                    break;                    
	                default:
	                    super.handleMessage(msg);
	            }
        	}
        	catch(Exception ex)
        	{
        		Log.i(TAG, "" + ex);
        	}
        	
        }
    }
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return mMessenger.getBinder();
	}

    @Override
    public void onCreate()
    {
        super.onCreate();
        //Log.i(TAG, "Service Started.");

        isRunning = true;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //Log.i(TAG, "Received start id " + startId + ": " + intent);
        
        return START_STICKY; // run until explicitly stopped.
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        //Log.i(TAG, "Service Stopped.");
		if (tts != null) tts.finalize();
        isRunning = false;
    }
	
    private void sendMessageToUI(int intValue, String strValue)
    {
        for (int i = mClients.size() - 1; i >= 0; i--)
        {
            try
            {
                // Send data as an Integer
            	if(strValue.equals(""))
            	{
            		mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, intValue, 0));
            	}
            	else
            	{
            		//Send data as a String
            		Bundle b = new Bundle();
            		b.putString("strValue", strValue);
            		Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
            		msg.setData(b);
            		mClients.get(i).send(msg);
            	}
            }
            catch (RemoteException e)
            {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    
    public static boolean isRunning()
    {
        return isRunning;
    }
    
	private int checkEnd(int iSt)
	{
		int iRes = Integer.MAX_VALUE;
		int i;
		
		i = sParags.indexOf("http://", iSt);
		if (i != -1) iSt = sParags.indexOf(" ", i);

		i = sParags.indexOf("www.", iSt);
		if (i != -1) iSt = sParags.indexOf(" ", i);
		
		i = sParags.indexOf("\n", iSt);
		if (i != -1 && i < iRes) iRes = i;

		i = sParags.indexOf(".", iSt);
		if (i != -1 && i < iRes) iRes = i;
		
		i = sParags.indexOf("?", iSt);
		if (i != -1 && i < iRes) iRes = i;
		
		i = sParags.indexOf("!", iSt);
		if (i != -1 && i < iRes) iRes = i;
		
		return iRes;
	}
    
    private void readText()
    {
    	String sLine = "";
    	
    	ttsLast = ttsStart;
    	ttsStart = ttsEnd;
    	ttsEnd = checkEnd(ttsStart) + 1;
    	if(ttsEnd != -1)
    	{
    		sLine = sParags.substring(ttsStart, ttsEnd);
    		sLine = sLine.trim();
    	}
    	
		if (!sLine.equals(""))
		{
			isSpeech = true;
			
			tts.say(sLine);

			sendMessageToUI(1, "");// show next line
		}
		else if(ttsEnd < sParags.length())
		{
			readText();
		}
		else if(ttsEnd >= sParags.length())
		{
			 sendMessageToUI(2, "");// next page
		}
		else
		{
			Log.i(TAG, "TTS say nothing!");
		}
    }
    
    public static boolean isSpeech()
    {
        return isSpeech;
    }
}
