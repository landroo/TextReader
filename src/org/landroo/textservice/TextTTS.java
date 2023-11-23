package org.landroo.textservice;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

public class TextTTS implements TextToSpeech.OnInitListener
{
	private static final String TAG = "TextTTS";

	private TextToSpeech mTTS;
	private Locale mLocale;
	private Handler mHandler;
	private boolean mInit = false;
	private boolean stopped = false;
	private Context mContext;

	public TextTTS(Context context, Locale locale, Handler handler)
	{
		mLocale = locale;
		mHandler = handler;
		mContext = context;
		if (mTTS != null)
		{
			mTTS.stop();
			mTTS.shutdown();
			mTTS = null;
		}
		mTTS = new TextToSpeech(context, this);
	}

	@Override
	public void onInit(int status)
	{
		if (status == TextToSpeech.SUCCESS)
		{
			int result = mTTS.setLanguage(mLocale);
			//if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
			if (result == TextToSpeech.LANG_NOT_SUPPORTED)
			{
				mHandler.sendEmptyMessage(101);
				try
				{
					Intent intent = new Intent();
					intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					mContext.startActivity(intent);
				}
				catch(Exception ex)
				{
					Log.i(TAG, "" + ex);
				}
			}
			else
			{
				mInit = true;
				mTTS.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener()
				{
					@Override
					public void onUtteranceCompleted(String utteranceId)
					{
						if (mHandler != null) mHandler.sendEmptyMessage(100);
					}
				});
			}
		}
		else
		{
			Log.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	public void say(String sSentence)
	{
		if (mInit == true && stopped == false)
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, sSentence);
			int res = mTTS.speak(sSentence, TextToSpeech.QUEUE_FLUSH, params);
			if (res == -1) Log.i(TAG, "TextToSpeech speak Error!");
		}
	}

	public void finalize()
	{
		if (mTTS != null)
		{
			mTTS.stop();
			mTTS.shutdown();
			mTTS = null;
		}
	}

	public void stop()
	{
		if (mTTS != null) 
		{
			stopped = true;
			mTTS.stop();
			//Log.i(TAG, "TTS Stopped!");
		}
	}
	
	public void start()
	{
		stopped = false;
	}
}
