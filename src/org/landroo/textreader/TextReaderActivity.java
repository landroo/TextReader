package org.landroo.textreader;
/*

textReader
This is a simple e-book reader application.
- Supports ePub, mobi (unencrypted only), txt, html/xml and docx formats.
- With the installed dictionary, translate the selected word.
- By the built in speech synthesis, read the book.
- Support page zooming.
- You can find a word in a page.
- Support hyper links.
- Built in picture viewer
- Content page for ePub books.

v.1.3.0
- Built in picture viewer
- Content page for ePub books.
- Bug fix in word selection.
- Support file associations for epub, txt, prc files. 
- You can delete book data with long press.
- Auto paging in TexttoSpeech.

v.1.3.1
- Bug fixes
- Save a snapshot on long tap

v.1.4
- Bug fix in external links
- Other bug fixes
- Memory optimization in rendering
- Add selectable background feature
- Add off screen text to speech 

Ez egy egyszerű e-könyv olvasó program.
- Támogatja a ePub, mobi (nem titkosított), txt, html/xml és docx formátumokat.
- A telepített szótárprogram segítségével lefordítja a kiválasztott szót.
- A beállított nyelven felolvassa a könyvet a beépített beszédszintetizátor segítségével.
- Lehetőség van a szöveg tetszés szerinti nagyítására.
- Lehetőséget biztosít az oldalon való keresésre.
- Támogatja a belső hivatkozásokat.
- Beépített képnézegetővel rendelkezik.
- Az ePub könyvekhez tartalom oldalt biztosít.

v.1.3.0
- Beépített képnézegetővel rendelkezik.
- Az ePub könyvekhez tartalom oldalt boztosít. 
- Hibajavítás a kijelöléseknél.
- Hozzárendeli az epub, txt és prc állományokat.
- Hosszú lenyomással törölhetjük a könyadatokat.
- Lapozás szövegfelolvasás alatt.

v.1.3.1
- Hibajavítások
- Előkép mentés hosszú érintéssel

v.1.4
- Hibajavítás a külső link kezelésnél
- Egyéb hibák javítása
- Memóri optimalizáció a megjelenítésnél
- Választható háttér hozzáadása 
- Szövegfelolvasás kikappcsolt képernyő alatt 

*/
// TODO
// UTF-8 Encode					ok
// ePub CoverPage				ok
// next page pictures			ok
// page from effect				ok
// exit from reader				ok	
// load last images				ok
// text to speech stop			ok
// off screen text to speech 	ok
// scroll error in tts			ok
// two bitmap scroll		
// background bitmap			ok
// highlight alpha, overlap		-
// tts highlight synchrony bug	ok
// delete one book bug			ok
// page position bug			ok
// tool tip hide on tap			??
// start with blank screen		ok
// start from file manager bug	ok
// redraw after setting change	ok
// start web browser bug		ok
// book content bug				ok
// link error in htmltotxt		ok
// local file link bug			ok
// empty page memory bug		ok
// html andtag error />			ok
// found result paragraph		ok
// missing line after a tag />	ok

import org.landroo.enghunbig.ISmartDictInterfaceEngBig;
import org.landroo.enghunmini.ISmartDictInterfaceEngMini;
import org.landroo.esphunmini.ISmartDictInterfaceEspMini;
import org.landroo.frahunbig.ISmartDictInterfaceFraBig;
import org.landroo.gerhunmini.ISmartDictInterfaceGerMini;
import org.landroo.parsers.DocXParser;
import org.landroo.parsers.EPubParser;
import org.landroo.parsers.HtmlParser;
import org.landroo.parsers.MobiParser;
import org.landroo.parsers.TextParser;
import org.landroo.textservice.TextService;
import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TextReaderActivity extends Activity implements UIInterface, Runnable, ScrollViewListener
{
	// constans
	private static final boolean debug = false;
	private static final String TAG = "textReader";
	private static final int HUD_POSY = 64;
	private static final int HUD_TIMERMAX = 1000;

	// document type 0 No, 1 Html, 2, Txt, 3 ePub, 4 mobi, 5 DocX
	private static final int NO = 0;
	private static final int HTML = 1;
	private static final int TEXT = 2;
	private static final int EPUB = 3;
	private static final int MOBI = 4;
	private static final int DOCX = 5;

	// english dictionary service
	private ISmartDictInterfaceEngBig enghunbig_service;
	private myEngHunBigServiceConnection enghunbig_connection;
	private boolean mIsEngHunBigBound = false;

	// english mini dictionary service
	private ISmartDictInterfaceEngMini enghunmini_service;
	private myEngHunMiniServiceConnection enghunmini_connection;
	private boolean mIsEngHunMiniBound = false;

	// spanish mini dictionary service
	private ISmartDictInterfaceEspMini esphunmini_service;
	private myEspHunMiniServiceConnection esphunmini_connection;
	private boolean mIsEspHunMiniBound = false;

	// french big dictionary service
	private ISmartDictInterfaceFraBig frahunbig_service;
	private myFraHunBigServiceConnection frahunbig_connection;
	private boolean mIsFraHunBigBound = false;

	// german mini dictionary service
	private ISmartDictInterfaceGerMini gerhunmini_service;
	private myGerHunMiniServiceConnection gerhunmini_connection;
	private boolean mIsGerHunMiniBound = false;
	
	// text service connection
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private boolean mIsTextServiceBound = false;
	private Messenger mTextService = null;

	// preferences
	private SharedPreferences settings; // preferences
	private int colorScheme = 1; // 1 black/white, 2 white/black, 3 gray/black
	private int backGround = 0; // 0 Wall, 1-4 paper, 5-8 stone
	private int fontSize = 16; // font size 16, 20, 24, 32
	private int isZoomable = -1; // display mode
	private int pageSize = 8000; // page size 2000, 4000, 8000
	private boolean useDict = true; // use dict
	private int dictLang = 1; // 1 english
	private boolean inForegin = true; // seach in foreign language
	private int inSayLang = 1; // speech language

	// activity
	private ImageView txtImage = null; // the main image
	private ImageView prevImage = null; // previous image
	private ImageView forwImage = null; // next image
	private ImageView speakImage = null; // speech image
	private ImageView findImage = null; // find image
	private ScrollChange scrollView = null; //
	private AlertDialog.Builder searchDialogBuilder; //
	private AlertDialog alertOnTap; //
	private EditText comment; //
	private Thread thread; //
	private boolean findWord = false; // true while finding a word
	private int nextPage = 0; //
	private int grpButton = 0; //
	private String wordList = ""; //
	private Paint paint = new Paint();
	private RectF rec = new RectF(0, 0, 0, 0);
	private int pageFrom = 1; // 1 page form right, 2 page from left 

	// text
	private static TextFormat textFormat = null; // textformat class
	private EPubParser epub = new EPubParser(); // epub class
	private HtmlParser html = new HtmlParser(); // html class
	private TextParser text = new TextParser(); // text class
	private MobiParser mobi = new MobiParser(); // mobi class
	private DocXParser docx = new DocXParser(); // docx class
	private String sFile = ""; // file name
	private int iDocType = 0; // document type 0 No, 1 Html, 2, Txt, 3 ePub, 4
								// mobi, 5 DocX
	private String thumbName = ""; // thumbnail name

	// UI
	private UI ui = null; // touch handler
	
	private TextView textView; // the text view
	private BitmapDrawable textDrawable; // text bitmap drawable
	private BitmapDrawable backDrawable; // backgrond bitmap drawable
	
	private int mX = 0; // touch x
	private int mY = 0; // touch y
	private int sX = 0; // touch start x
	private int sY = 0; // touch start y

	private float xPos = 0; // text bitmap x position
	private float yPos = 0; // text bitmap y position
	
	private Timer swipeTimer = null;
	private float swipeDistX = 0;
	private float swipeDistY = 0;
	private float swipeVelocity = 0;
	private float swipeSpeed = 0;
	private float backSpeedX = 0;
	private float backSpeedY = 0;
	private float offMarginX = 0;
	private float offMarginY = 0;
	
	private int displayWidth = 0; // display width
	private int displayHeight = 0; // display height

	public float pictureWidth = 0; // text bitmap width
	public float pictureHeight = 0; // text bitmap height
	public float origWidth = 0; // original picture width
	public float origHeight = 0; // original picture height
	
	private float zoomSize = 0; //
	
	private float thumbX = 0; // thump x position
	private float thumbY = 0; // thumb y positions
//	private double sizeDiff; // zoom multiplier

	private Bitmap prev1; // back button normal
	private Bitmap prev2; // back button selected
	private Bitmap find1; // find button normal
	private Bitmap find2; // find button selected
	private Bitmap forw1; // next button normal
	private Bitmap forw2; // next button selected
	private Bitmap thumb1; // scroll bar thumb button normal
	private Bitmap thumb2; // scroll bar thumb button selected
	private Bitmap sound1; // sound button normal
	private Bitmap sound2; // sound button selected
	private Bitmap wait; // wait circle

	private int prevCnt = 0; // previous selected
	private int forwCnt = 0; // forward button selected
	private int findCnt = 0; // find button selected
	private int thumbCnt = 0; // thumb button selected
	private int hudAlphaCnt = HUD_TIMERMAX; // hide gui down counter

	private RectF wordRect = null; // selected word highlight rectangle
	private ArrayList<RectF> searchList = new ArrayList<RectF>(); // found word highlight rectangles
	private String searchText = ""; // word to search
	private String sMess = ""; // message to show

	private boolean isSay = false; // now speaking
	private RectF lineRect = null; // line highlight
	private boolean enableSay = true; // text to speech enabled

	private String sLast = ""; // last paragraph info

	private Rect waitRect = new Rect(0, 0, 64, 64); // process bitmap size
	private int waitCnt = 0; //
	private int waitIcon = 0; //
	private Bitmap waitBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_4444); // empty bitmap frame

	private Toast toastDict;
	private CountDownTimer toastTimer;
	
	private int contentPage = -1;
	
	private boolean hourGlass = true;
	
	private boolean bFirst = true;

	/**
	 * english dictionary service connection class
	 * 
	 * @author rkovacs
	 * 
	 */
	class myEngHunBigServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder boundService)
		{
			enghunbig_service = ISmartDictInterfaceEngBig.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name)
		{
			enghunbig_service = null;
		}
	}

	/**
	 * english mini dictionary service connection class
	 * 
	 * @author rkovacs
	 * 
	 */
	class myEngHunMiniServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder boundService)
		{
			enghunmini_service = ISmartDictInterfaceEngMini.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name)
		{
			enghunmini_service = null;
		}
	}

	/**
	 * spanish mini dictionary service connection class
	 * 
	 * @author rkovacs
	 * 
	 */
	class myEspHunMiniServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder boundService)
		{
			esphunmini_service = ISmartDictInterfaceEspMini.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name)
		{
			esphunmini_service = null;
		}
	}

	/**
	 * french big dictionary service connection class
	 * 
	 * @author rkovacs
	 * 
	 */
	class myFraHunBigServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder boundService)
		{
			frahunbig_service = ISmartDictInterfaceFraBig.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name)
		{
			frahunbig_service = null;
		}
	}

	/**
	 * german mini dictionary service connection class
	 * 
	 * @author rkovacs
	 * 
	 */
	class myGerHunMiniServiceConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName name, IBinder boundService)
		{
			gerhunmini_service = ISmartDictInterfaceGerMini.Stub.asInterface((IBinder) boundService);
		}

		public void onServiceDisconnected(ComponentName name)
		{
			gerhunmini_service = null;
		}
	}
	
    private ServiceConnection mTextServiceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            mTextService = new Messenger(service);
            //Log.i(TAG, "TextService Attached.");
            try
            {
                Message msg = Message.obtain(null, TextService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mTextService.send(msg);
                
                setTTS();
            }
            catch (RemoteException e)
            {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mTextService = null;
            //Log.i(TAG, "TextService Disconnected.");
        }
    };
	
    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case TextService.MSG_SET_INT_VALUE:
                	int iVal = msg.arg1;
                    //Log.i(TAG, "Int Message: " + iVal);
                	switch(iVal)
                	{
                	case 1:
                		showReadText();
                		break;
                	case 2:
                		nextPage();
                		break;
                	case 3:
        				sMess = getResources().getString(R.string.saylangerror);
        				Toast.makeText(TextReaderActivity.this, sMess, Toast.LENGTH_LONG).show();
        				enableSay = false;
        				break;
                	}
                    break;
                case TextService.MSG_SET_STRING_VALUE:
                    String strVal = msg.getData().getString("strVal");
                    Log.i(TAG, "Str Message: " + strVal);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

	/**
	 * create event
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// setting
		applySettings(true);

		bindDictionaries();
		
        if (!TextService.isRunning())
        {
            bindService(new Intent(this, TextService.class), mTextServiceConnection, Context.BIND_AUTO_CREATE);
            mIsTextServiceBound = true;
            //Log.i(TAG, "TextService Binding.");
            
            isSay = TextService.isSpeech();
        }
        
		// ui
		ui = new UI(this);
		textView = new TextView(this);
		if (isZoomable == 1) setContentView(textView);

		else
		{
			setContentView(R.layout.main);

			scrollView = (ScrollChange) findViewById(R.id.imgScroller);
			scrollView.setScrollViewListener(this);

			// dsiplay image
			txtImage = (ImageView) findViewById(R.id.imgText);
			txtImage.setClickable(true);
			txtImage.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					grpButton = 0;
					return ui.tapEvent(event);
				}
			});
			// back image
			prevImage = (ImageView) findViewById(R.id.prew);
			prevImage.setClickable(true);
			prevImage.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					grpButton = 1;
					return ui.tapEvent(event);
				}
			});
			// next image
			forwImage = (ImageView) findViewById(R.id.forw);
			forwImage.setClickable(true);
			forwImage.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					grpButton = 2;
					return ui.tapEvent(event);
				}
			});
			// speech image
			speakImage = (ImageView) findViewById(R.id.speak);
			speakImage.setClickable(true);
			speakImage.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					grpButton = 3;
					return ui.tapEvent(event);
				}
			});
			// find image
			findImage = (ImageView) findViewById(R.id.find);
			findImage.setClickable(true);
			findImage.setOnTouchListener(new View.OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					grpButton = 4;
					return ui.tapEvent(event);
				}
			});
		}
		swipeTimer = new Timer();
		swipeTimer.scheduleAtFixedRate(new SwipeTask(), 0, 10);

		prev1 = BitmapFactory.decodeResource(getResources(), R.drawable.back1);
		prev2 = BitmapFactory.decodeResource(getResources(), R.drawable.back2);
		forw1 = BitmapFactory.decodeResource(getResources(), R.drawable.forw1);
		forw2 = BitmapFactory.decodeResource(getResources(), R.drawable.forw2);
		find1 = BitmapFactory.decodeResource(getResources(), R.drawable.search1);
		find2 = BitmapFactory.decodeResource(getResources(), R.drawable.search2);
		thumb1 = BitmapFactory.decodeResource(getResources(), R.drawable.thumb1);
		thumb2 = BitmapFactory.decodeResource(getResources(), R.drawable.thumb2);
		sound1 = BitmapFactory.decodeResource(getResources(), R.drawable.sound1);
		sound2 = BitmapFactory.decodeResource(getResources(), R.drawable.sound2);
		wait = BitmapFactory.decodeResource(getResources(), R.drawable.wait);

		Bitmap bitmap = textFormat.getBackImage(displayWidth, displayHeight, getResources());
		if(bitmap != null)
		{
			backDrawable = new BitmapDrawable(bitmap);
			backDrawable.setBounds(0, 0, displayWidth, displayHeight);
		}

		// Get filename
		sFile = null;
		Intent intent = getIntent();
		Uri data = intent.getData();
		if(data != null)
		{
			sFile = data.getEncodedPath();
			try
			{
				sFile = URLDecoder.decode(sFile, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
			}
		}
		Bundle extras = intent.getExtras();
		if (sFile == null && extras != null) sFile = extras.getString("file");

		System.runFinalizersOnExit(true);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		toastDict = Toast.makeText(TextReaderActivity.this, "", Toast.LENGTH_LONG);
	}

	/**
	 * scroll change event handler
	 */
	public void onScrollChanged(ScrollChange scrollView, int x, int y, int oldx, int oldy)
	{
		if (y > pictureHeight - displayHeight) scrollView.scrollTo(0, (int)pictureHeight - displayHeight);
	}

	/**
	 * load the selected file
	 * 
	 * @param sFileName
	 * @return
	 */
	private boolean loadFile(String sFileName)
	{
		if(sFileName == null) return false;
		
		boolean bOK = false;
		int i = sFileName.lastIndexOf(".");
		if (i == -1) return false;

		String sExt = sFileName.substring(i);
		sExt = sExt.toLowerCase();
		String sFile = sFileName;

		// 1 htm html xhtml xml
		if (sExt.equals(".htm") || sExt.equals(".html") || sExt.equals(".xhtml") || sExt.equals(".xml"))
		{
			if (iDocType == NO)
			{
				sLast = html.loadLast(sFileName);
				if (sLast.equals("")) bOK = html.parseHtml(sFileName);
				else
				{
					sFile = html.getOrigPath();
					bOK = html.parseHtml(sFile);
				}
			}
			else bOK = html.parseHtml(sFileName);

			if (bOK)
			{
				textFormat.setTextWidth(displayWidth);
				textFormat.setFile(sFile);
				textFormat.setPageText(html.getText());
				textFormat.setImagePos(html.getImageList());
				textFormat.setLinkPos(html.getLinkList());
				
				if (!textFormat.newText(sLast))
				{
					android.os.Process.killProcess(android.os.Process.myPid());
					// this.finish();
					System.exit(0);
				}

				if (iDocType == NO) thumbName = html.getImageName();

				html.setState(textFormat.getState());
			}
			else handler.sendEmptyMessage(3);
			if (iDocType == NO) iDocType = HTML;
		}
		else if (sExt.equals(".txt")) // txt
		{
			iDocType = TEXT;
			sLast = text.loadLast(sFileName);
			textFormat.setIsWrite(true);
			if (sLast.equals("")) bOK = text.parseTxt(sFileName);
			else
			{
				sFile = text.getOrigPath();
				bOK = text.parseTxt(sFile);
			}
			textFormat.setIsWrite(false);
			if (bOK)
			{
				textFormat.setTextWidth(displayWidth);
				textFormat.setFile(sFile);
				textFormat.setPageText(text.getText());
				
				if (!textFormat.newText(sLast))
				{
					android.os.Process.killProcess(android.os.Process.myPid());
					// this.finish();
					System.exit(0);
				}

				thumbName = text.getImageName();

				text.setState(textFormat.getState());
			}
			else handler.sendEmptyMessage(3);
		}
		else if (sExt.equals(".epub")) // ePub
		{
			iDocType = EPUB;
			sLast = epub.loadLast(sFileName);
			textFormat.setIsWrite(true);
			if (sLast.equals("")) bOK = epub.parseEpub(sFileName);
			else
			{
				sFile = epub.getOrigPath();
				bOK = epub.parseEpub(sFile);
			}
			textFormat.setIsWrite(false);
			if (bOK)
			{
				loadFile(epub.getChapter(epub.currChapter()));

				thumbName = epub.getImageName();

				epub.setState(textFormat.getState());
			}
			else handler.sendEmptyMessage(3);
		}
		else if (sExt.equals(".prc") || sExt.equals(".mobi")) // prc mobi
		{
			iDocType = MOBI;
			sLast = mobi.loadLast(sFileName);
			textFormat.setIsWrite(true);
			if (sLast.equals("")) bOK = mobi.parseMobi(sFileName);
			else
			{
				sFile = mobi.getOrigPath();
				bOK = mobi.parseMobi(sFile);
			}
			textFormat.setIsWrite(false);
			if (bOK)
			{
				loadFile(mobi.getFileName());

				thumbName = mobi.getImageName();

				mobi.setState(textFormat.getState());
			}
			else handler.sendEmptyMessage(3);
		}
		else if (sExt.equals(".docx")) // docx
		{
			iDocType = DOCX;
			sLast = docx.loadLast(sFileName);
			textFormat.setIsWrite(true);
			if (sLast.equals("")) bOK = docx.parseDocX(sFileName);
			else
			{
				sFile = docx.getOrigPath();
				bOK = docx.parseDocX(sFile);
			}
			textFormat.setIsWrite(false);
			if (bOK)
			{
				textFormat.setTextWidth(displayWidth);
				textFormat.setFile(docx.getFileName());
				textFormat.setPageText(docx.getText());
				textFormat.setImagePos(docx.getImageList());
				
				if (!textFormat.newText(sLast))
				{
					android.os.Process.killProcess(android.os.Process.myPid());
					// this.finish();
					System.exit(0);
				}

				thumbName = docx.getImageName();

				docx.setState(textFormat.getState());

				//handler.sendEmptyMessage(0);
			}
			else handler.sendEmptyMessage(3);
		}
		else handler.sendEmptyMessage(3);

		return bOK;
	}

	/**
	 * handle the back button
	 */
	@Override
	public void onBackPressed()
	{
		//android.os.Process.killProcess(android.os.Process.myPid());
		this.finish();
		// System.exit(0);
	}

	/**
	 * resume event handler
	 */
	@Override
	protected void onResume()
	{
		super.onResume();

		boolean bRedraw = applySettings(false);
		
		String sName = textFormat.getFile();
		if (bFirst || bRedraw)
		{
			if(bFirst) epub.setChapter(sName);
			loadPage();
			
			bFirst = false;
		}
	}

	/**
	 * stop event handler
	 */
	@Override
	protected void onStop()
	{
		super.onStop();
		
		if(toastTimer != null)
		{
			toastTimer.cancel();
			toastDict.cancel();
		}
	}

	/**
	 * finish event handler
	 */
	@Override
	public void finish()
	{
		android.os.Process.killProcess(android.os.Process.myPid());
		super.finish();
	}

	/**
	 * destroy event handler
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		if (mIsEngHunBigBound) unbindService(enghunbig_connection);
		if (mIsEngHunMiniBound) unbindService(enghunmini_connection);
		if (mIsEspHunMiniBound) unbindService(esphunmini_connection);
		if (mIsFraHunBigBound) unbindService(frahunbig_connection);
		if (mIsGerHunMiniBound) unbindService(gerhunmini_connection);
		
		doUnbindTextService();
		
		if(toastTimer != null)
		{
			toastTimer.cancel();
			toastDict.cancel();
		}
	}

	/**
	 * pause event hander
	 */
	@Override
	public synchronized void onPause()
	{
		super.onPause();

		saveState();
	}

	/**
	 * start event handler
	 */
	@Override
	public void onStart()
	{
		super.onStart();

//		colorScheme = Integer.parseInt(settings.getString("colorScheme", "10"));
//		backGround = Integer.parseInt(settings.getString("backGround", "10"));
	}

	/**
	 * short touch event handler
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (isZoomable == 1) return ui.tapEvent(event);

		return false;
	}

	/**
	 * finger touch handler
	 */
	@Override
	public void onDown(float x, float y)
	{
		/*if (isSay)
		{
			sendMessageToService(101, "", "");// TTS stop
			isSay = false;
		}*/
		
		if (isZoomable == 1)
		{
			sX = (int) x;
			sY = (int) y;

			swipeVelocity = 0;

			checkButtons(x, y);

			textView.postInvalidate();
		}
	}

	/**
	 * finger detach handler
	 */
	@Override
	public void onUp(float x, float y)
	{
		if (isZoomable == 1)
		{
			checkOff();
			
			prevCnt = 0;
			forwCnt = 0;
			findCnt = 0;
			thumbCnt = 0;

			textView.postInvalidate();
		}
	}

	/**
	 * next page
	 */
	private void nextPage()
	{
		clearState();

		if (textFormat.isNextPage())
		{
			nextPage = 1;
			thread = new Thread(this);
			thread.start();
		}
		else
		{
			if (epub.isNextChapter())
			{
				sFile = epub.nextChapter();
				loadPage();
			}
		}
	}

	/**
	 * previous page
	 */
	private void prevPage()
	{
		clearState();

		if (textFormat.isPrewPage())
		{
			nextPage = 2;
			thread = new Thread(this);
			thread.start();
		}
		else
		{
			if (epub.isPrewChapter())
			{
				sFile = epub.prewChapter();
				loadPage();
			}
		}
	}
	
	private void lastPage()
	{
		sFile = epub.getChapter(contentPage);
		loadPage();
	}
	
	private void contPage()
	{
		if(iDocType == EPUB)
		{
			nextPage = 5;
			thread = new Thread(this);
			thread.start();
		}
	}

	/**
	 * finger touch event handler
	 */
	@Override
	public void onTap(float x, float y)
	{
		//float bx = (x - xPos) * (origWidth / pictureWidth);
		//float by = (y - yPos) * (origHeight / pictureHeight);
		float posX = x;
		float posY = y;
		boolean bTranslate = false;
		
		if(toastTimer != null)
		{
			toastTimer.cancel();
			toastDict.cancel();
			toastDict.setText("");
			toastDict.show();
		}
	
		if (isZoomable == 1)
		{
			int i = checkButtons(x, y);
			switch (i)
			{
			case 1:
				pageFrom = 2;
				if(contentPage != -1) lastPage();
				else prevPage();
				break;
			case 2:
				pageFrom = 1;
				if(contentPage != -1) lastPage();
				else nextPage();
				break;
			case 3:
				findText();
				break;
			case 4:
				float pos = x - 128;
				yPos = pos * ((float) pictureHeight - displayHeight) / (float) (displayWidth - 170) * -1;
				thumbX = calcThumb();
				textView.postInvalidate();
				break;
			case 5:
				if (enableSay)
				{
					isSay = !isSay;
					if(isSay) sendMessageToService(100, "", "");// TTS start
					else sendMessageToService(101, "", "");// TTS stop
					//Log.i(TAG, "TTS: " + isSay);
				}
				break;
			default:
				posX = (float) (x - xPos) * ((float) origWidth / pictureWidth);
				posY = (float) (y - yPos) * ((float) origHeight / pictureHeight);
				bTranslate = true;
			}
		}
		else
		{
			// simple mod
			switch (grpButton)
			{
			case 1:
				txtImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hourglass));
				if(contentPage != -1) lastPage();
				else prevPage();
				break;
			case 2:
				txtImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hourglass));
				if(contentPage != -1) lastPage();
				else nextPage();
				break;
			case 3:
				if (enableSay)
				{
					isSay = !isSay;
					if(isSay) sendMessageToService(100, "", "");// TTS start
					else sendMessageToService(101, "", "");// TTS stop
				}
				break;
			case 4:
				findText();
				break;
			default:
				bTranslate = true;
			}
			grpButton = 0;
		}
		
		if (bTranslate && useDict && findWord == false) new TranslateTask().execute((int) posX, (int) posY);
	}

	/**
	 * finger hold down event

	 */
	@Override
	public void onHold(float x, float y)
	{
		// Thumbnail saved
		boolean bOK = saveThumb(true);
		if(bOK) sMess = getResources().getString(R.string.thumbsave);
		else sMess = "Error in save snapshot!";
		handler.sendEmptyMessage(4);
	}

	/**
	 * finger movement handler
	 */
	@Override
	public void onMove(float x, float y)
	{
		if (isZoomable == 1)
		{
			// scrollbar
			if (x > 128 && x < displayWidth - 128 && y > displayHeight - 80 && y < displayHeight)
			{
				float pos = x - 128;
				yPos = pos * ((float) pictureHeight - displayHeight) / (float) (displayWidth - 256) * -1;
			}
			else
			{
				mX = (int) x;
				mY = (int) y;
	
				float dx = mX - sX;
				float dy = mY - sY;
	
				if ((pictureWidth >= displayWidth) && (xPos + dx < displayWidth - (pictureWidth + offMarginX) || xPos + dx > offMarginX)) dx = 0;
				if ((pictureHeight >= displayHeight) && (yPos + dy < displayHeight - (pictureHeight + offMarginY) || yPos + dy > offMarginY)) dy = 0;
				if ((pictureWidth < displayWidth) && (xPos + dx > displayWidth - pictureWidth || xPos + dx < 0)) dx = 0;
				if ((pictureHeight < displayHeight) && (yPos + dy > displayHeight - pictureHeight || yPos + dy < 0)) dy = 0;
	
				xPos += dx;
				yPos += dy;
	
				sX = (int) mX;
				sY = (int) mY;
			}
			
			thumbX = calcThumb();

			hudAlphaCnt = HUD_TIMERMAX;

			textView.postInvalidate();
		}
	}

	/**
	 * fling event handler
	 */
	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		if (isZoomable == 1)
		{
			if ((direction == 1 || direction == 2) && pictureWidth <= displayWidth + 1)
			{
				pageFrom = direction;
				if (direction == 1) nextPage();
				else prevPage();

				return;
			}
			
			swipeDistX = x2 - x1;
			swipeDistY = y2 - y1;
			swipeSpeed = 1;
			swipeVelocity = velocity;
			if (swipeVelocity > 200) swipeVelocity = 200;

			textView.postInvalidate();
		}
	}

	/**
	 * double tap zoom to original
	 */
	@Override
	public void onDoubleTap(float x, float y)
	{
		if (isZoomable == 1 && pictureWidth > displayWidth)
		{
			swipeVelocity = 0;
			
			backSpeedX = 0;
			backSpeedY = 0;

			pictureWidth = origWidth;
			pictureHeight = origHeight;

			xPos = (displayWidth - pictureWidth) / 2;
			yPos = (displayHeight - pictureHeight) / 2;

			textDrawable.setBounds(0, 0, (int) pictureWidth, (int) pictureHeight);
			
			textView.postInvalidate();
		}
	}

	/**
	 * two finger zoom handler
	 */
	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		if (isZoomable == 1)
		{
			int dist = (int) distance * 30;
			switch (mode)
			{
			case 1:
				zoomSize = dist;
				break;
			case 2:
				int diff = (int) (dist - zoomSize);
				float sizeNew = FloatMath.sqrt(pictureWidth * pictureWidth + pictureHeight * pictureHeight);
				float sizeDiff = 100 / (sizeNew / (sizeNew + diff));
				float newSizeX = pictureWidth * sizeDiff / 100;
				float newSizeY = pictureHeight * sizeDiff / 100;

				// zoom between min and max value
				if (newSizeX > origWidth && newSizeX < origWidth * 10)
				{
					textDrawable.setBounds(0, 0, (int)newSizeX, (int)newSizeY);

					zoomSize = dist;

					float diffX = newSizeX - pictureWidth;
					float diffY = newSizeY - pictureHeight;
					float xPer = 100 / (pictureWidth / (Math.abs(xPos) + mX)) / 100;
					float yPer = 100 / (pictureHeight / (Math.abs(yPos) + mY)) / 100;

					xPos -= diffX * xPer;
					yPos -= diffY * yPer;

					pictureWidth = newSizeX;
					pictureHeight = newSizeY;

					if (pictureWidth > displayWidth || pictureHeight > displayHeight)
					{
						if (xPos > 0) xPos = 0;
						if (yPos > 0) yPos = 0;

						if (xPos + pictureWidth < displayWidth) xPos = displayWidth - pictureWidth;
						if (yPos + pictureHeight < displayHeight) yPos = displayHeight - pictureHeight;
					}
					else
					{
						if (xPos <= 0) xPos = 0;
						if (yPos <= 0) yPos = 0;

						if (xPos + pictureWidth > displayWidth) xPos = displayWidth - pictureWidth;
						if (yPos + pictureHeight > displayHeight) yPos = displayHeight - pictureHeight;
					}

					// Log.i(TAG, "" + xPos + " " + yPos);
				}
				break;
			case 3:
				zoomSize = 0;
				break;
			}
			
			textView.postInvalidate();
		}
	}

	/**
	 * two finger rotation handler
	 */
	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
	}

	/**
	 * menu creation handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * menu selection handler
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.menu_content:
			contPage();
			return true;
		
		case R.id.menu_setup:
			Intent SettingsIntent = new Intent(this, SettingsScreen.class);
			startActivity(SettingsIntent);
			return true;

		case R.id.menu_refresh:
			nextPage = 3;
			thread = new Thread(this);
			thread.start();
			return true;

		case R.id.menu_exit:
			this.setResult(1);
			int pid = android.os.Process.myPid();
			android.os.Process.killProcess(pid);
			// this.finish();
			System.exit(0);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * display a new page
	 */
	private void loadPage()
	{
		nextPage = 0;

		thread = new Thread(this);
		thread.start();
		contentPage = -1;
	}

	/**
	 * thread run
	 */
	// TODO main thread
	@Override
	public void run()
	{
		hourGlass = true;
		clearState();

		switch (nextPage)
		{
		case 0: // load file
			loadFile(sFile);
			handler.sendEmptyMessage(0);
			break;
		case 1: // next page
			textFormat.nextPage();
			handler.sendEmptyMessage(0);
			saveState();
			break;
		case 2: // previous page
			textFormat.prewPage();
			handler.sendEmptyMessage(0);
			saveState();
			break;
		case 3: // refesh
			textFormat.currPage();
			handler.sendEmptyMessage(0);
			break;
		case 4: // search
			int found = textFormat.findWord(searchText);
			searchList = textFormat.getSearchList();
			if (found == 0) sMess = getResources().getString(R.string.notfound);
			else
			{
				// TODO
				yPos = textFormat.foundPos();
				if (yPos > 0) yPos = 0;
				if (yPos < displayHeight - pictureHeight) yPos = displayHeight - pictureHeight;
				
				//Log.i(TAG, "yPos: " + yPos + " " +(pictureHeight - displayHeight));
				
				textView.postInvalidate();
				
				sMess = found + " " + getResources().getString(R.string.found);
			}
			if (isZoomable == 0) textFormat.drawFound();
			handler.sendEmptyMessage(4);
			break;
		case 5: // content
			contentPage = epub.currChapter();
			textFormat.setPageText(epub.getCotentText());
			textFormat.setImagePos(null);
			textFormat.setLinkPos(epub.getCotentLinks());
			textFormat.newText("");
			handler.sendEmptyMessage(0);
			
			sendMessageToService(0, "tts", textFormat.getText());// TTS set text
			break;
		}
		
//		Log.i(TAG, "nextPage: " + nextPage);
	}

	/**
	 * Message handler
	 */
	// TODO message handler
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			// new page loaded
			switch(msg.what)
			{
			case 0:
				hudAlphaCnt = HUD_TIMERMAX;
				xPos = 0;
				yPos = 0;
				if (textFormat.getTextImage() != null)
				{
					if(pageFrom == 1) xPos = displayWidth / 2 - 1;
					else xPos = -displayWidth / 2 + 1;

					if (isZoomable == 1)
					{
						textDrawable = new BitmapDrawable(textFormat.getTextImage());
						textDrawable.setBounds(0, 0, textFormat.getTextImage().getWidth(), textFormat.getTextImage().getHeight());

						textView.postInvalidate();
					}
					else
					{
						scrollView.scrollTo(0, 0);
						txtImage.setImageBitmap(textFormat.getTextImage());
						hideButtons();
					}
					pictureWidth = textFormat.getTextWidth();
					pictureHeight = textFormat.getTextHeight();
					
					origWidth = pictureWidth;
					origHeight = pictureHeight;
					
					//Log.i(TAG, "pictureHeight " + pictureHeight + " " + textFormat.getTextImage().getHeight());
					
					sendMessageToService(0, "tts", textFormat.getText());// TTS set text
					
					saveThumb(false);
					
					checkOff();
					
					if(isSay) sendMessageToService(100, "", "");// TTS start
				}
				
				break;
			case 1:// show translate list
				if (!wordList.equals(""))
				{
					int max = 9000;
					if(wordList.length() < 100) max = 5000;
					toastDict.setText(wordList); 
					toastTimer = new CountDownTimer(max, 1000)
					{
						public void onTick(long millisUntilFinished)
						{
							toastDict.show();
						}

						public void onFinish()
						{
							toastDict.show();
						}

					}.start();
				}
				break;
			case 2:// internal link selected
				if (isZoomable == 0) txtImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hourglass));
				loadPage();
				break;
			case 3:// show file error
				sMess = getResources().getString(R.string.fileerror);
				Toast.makeText(TextReaderActivity.this, sMess + " (" + sFile + ")", Toast.LENGTH_LONG).show();
				break;
			case 4:// show message
				Toast.makeText(TextReaderActivity.this, sMess, Toast.LENGTH_LONG).show();
				if (isZoomable == 0)
				{
					txtImage.setImageBitmap(textFormat.getTextImage());
					scrollView.scrollTo((int) textFormat.foundPos(), 0);
				}
				break;
			case 5:// out of memory
				sMess = getResources().getString(R.string.memoryerror);
				Toast.makeText(TextReaderActivity.this, sMess, Toast.LENGTH_LONG).show();
				break;
			}
			hourGlass = false;
		}
	};

	private boolean saveThumb(boolean bForce)
	{
		boolean bOK = false;
		File tmpDir = new File(thumbName);
		if(bForce == false) bForce = !tmpDir.exists(); 
		if (bForce)
		{
			try
			{
				float sX = displayWidth > pictureWidth ? pictureWidth : displayWidth;
				float sY = displayHeight > pictureHeight ? pictureHeight : displayHeight;
				Bitmap bmp = Bitmap.createBitmap(textFormat.getTextImage(), 0, 0, (int)sX, (int)sY);
				FileOutputStream out = new FileOutputStream(thumbName);
				bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
				bOK = true;
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Error thumbnail: " + ex.getMessage());
			}
		}
		
		return bOK;
	}

	/**
	 * show/hide buttons
	 */
	private void hideButtons()
	{
		if (epub.isPrewChapter() || textFormat.isPrewPage()) prevImage.setVisibility(View.VISIBLE);
		else prevImage.setVisibility(View.INVISIBLE);

		if (epub.isNextChapter() || textFormat.isNextPage()) forwImage.setVisibility(View.VISIBLE);
		else forwImage.setVisibility(View.INVISIBLE);
	}

	/**
	 * Timer task to calculate position
	 * 
	 * @author rkovacs
	 * 
	 */
	class SwipeTask extends TimerTask
	{
		public void run()
		{
			boolean redraw = false;
			if (swipeVelocity > 0)
			{
				float dist = FloatMath.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX);
				float x = xPos - (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				float y = yPos - (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				if ((pictureWidth >= displayWidth) && (x < displayWidth - (pictureWidth + offMarginX) || x > offMarginX)
						|| ((pictureWidth < displayWidth) && (x > displayWidth - pictureWidth || x < 0)))
				{
					swipeDistX *= -1;
					swipeSpeed = swipeVelocity;
					//swipeSpeed += .5;
				}

				if ((pictureHeight >= displayHeight) && (y < displayHeight - (pictureHeight + offMarginY) || y > offMarginY)
						|| ((pictureHeight < displayHeight) && (y > displayHeight - pictureHeight || y < 0)))
				{
					swipeDistY *= -1;
					swipeSpeed = swipeVelocity;
					//swipeSpeed += .5;
				}

				xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				swipeVelocity -= swipeSpeed;
				swipeSpeed += .0001;
				
				thumbX = calcThumb();

				redraw = true;
				
				if(swipeVelocity <= 0) checkOff();
			}
			
			if(backSpeedX != 0)
			{
				if((backSpeedX < 0 && xPos <= 0.1f) || (backSpeedX > 0 && xPos + 0.1f >= displayWidth - pictureWidth)) backSpeedX = 0;
				else if(backSpeedX < 0) xPos -= xPos / 20;
				else xPos += (displayWidth - (pictureWidth + xPos)) / 20;

				redraw = true;
			}
			
			if(backSpeedY != 0)
			{
				if((backSpeedY < 0 && yPos <= 0.1f) || (backSpeedY > 0 && yPos + 0.1f >= displayHeight - pictureHeight)) backSpeedY = 0;
				else if(backSpeedY < 0) yPos -= yPos / 20;
				else yPos += (displayHeight - (pictureHeight + yPos)) / 20;
				
				redraw = true;
			}
			
			if (hudAlphaCnt > 64)
			{
				hudAlphaCnt--;
				redraw = true;
			}

			if (textFormat.isWrite())
			{
				waitCnt++;
				if (waitCnt > 10)
				{
					waitRect.top = waitIcon * 64;
					waitRect.bottom = waitIcon * 64 + 64;

					Rect desc = new Rect(0, 0, 64, 64);

					paint.setAlpha(255);

					Canvas canvas = new Canvas(waitBmp);
					canvas.drawBitmap(wait, waitRect, desc, paint);

					waitIcon++;
					if (waitIcon >= 12) waitIcon = 0;

					waitCnt = 0;
				}
			}
			
			if(redraw) textView.postInvalidate();
		}
	}

	/**
	 * calculate the position of the thumb
	 * 
	 * @return
	 */
	private float calcThumb()
	{
		float W = (float) displayWidth - 256 - (float) thumb1.getWidth();
		float H = (float) pictureHeight - (float) displayHeight;
		float O = 128;
		return W / H * -yPos + O;
	}

	/**
	 * implementation of the view draw method
	 * 
	 * @author rkovacs
	 * 
	 */
	// TODO main view
	private class TextView extends View
	{
		private float barX;
		
		public TextView(Context context)
		{
			super(context);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			try
			{
				// draw the main text bitmap
				if (backDrawable != null)
				{
					backDrawable.draw(canvas);
				}
				
				if (hourGlass)
				{
					//paint.setAlpha(255);
					canvas.drawBitmap(waitBmp, (displayWidth - 64) / 2, (displayHeight - 64) / 2, paint);
				}
				else
				{
					if (textDrawable != null)
					{
						canvas.translate(xPos, yPos);
						textDrawable.draw(canvas);
						//canvas.translate(-xPos, -yPos);
						canvas.restore();
					}
				}
	
				paint.setColor(0xFFAAAAAA);
				if (hudAlphaCnt > 255) paint.setAlpha(255);
				else paint.setAlpha(hudAlphaCnt);
	
				if (!textFormat.isWrite())
				{
					// back icon
					if (epub.isPrewChapter() || textFormat.isPrewPage())
					{
						if (prevCnt > 0) canvas.drawBitmap(prev2, 0, displayHeight - HUD_POSY, paint);
						else canvas.drawBitmap(prev1, 0, displayHeight - HUD_POSY, paint);
					}
	
					// next icon
					if (epub.isNextChapter() || textFormat.isNextPage())
					{
						if (forwCnt > 0) canvas.drawBitmap(forw2, 64, displayHeight - HUD_POSY, paint);
						else canvas.drawBitmap(forw1, 64, displayHeight - HUD_POSY, paint);
					}
	
					// search icon
					if (findCnt > 0) canvas.drawBitmap(find2, displayWidth - 64, displayHeight - HUD_POSY, paint);
					else canvas.drawBitmap(find1, displayWidth - 64, displayHeight - HUD_POSY, paint);
	
					// sound icon
					if (enableSay)
					{
						if (isSay) canvas.drawBitmap(sound2, displayWidth - 128, displayHeight - HUD_POSY, paint);
						else canvas.drawBitmap(sound1, displayWidth - 128, displayHeight - HUD_POSY, paint);
					}
	
					// scroll bar
					//if (pictureHeight - marginY * 2 > displayHeight)
					if (pictureHeight > displayHeight)
					{
						// draw bar
						rec.left = 128;
						rec.top = displayHeight - 40;
						rec.right = displayWidth - 128;
						rec.bottom = displayHeight - 24;
						canvas.drawRoundRect(rec, 5, 5, paint);
						
						// draw thumb
						barX = thumbX;
						if(barX < rec.left) barX = rec.left;
						if(barX > rec.right - thumb1.getWidth()) barX = rec.right - thumb1.getWidth();
						if (thumbCnt > 0) canvas.drawBitmap(thumb2, barX, thumbY, paint);
						else canvas.drawBitmap(thumb1, barX, thumbY, paint);
					}
				}
				// highlights selected word
				if (wordRect != null)
				{
					paint.setColor(0x70FF0000);
					canvas.drawRoundRect(calcRect(wordRect), 5, 5, paint);
				}
	
				// highlights found words
				if (searchList.size() > 0)
				{
					paint.setColor(0x7000FF00);
					RectF rect;
					for (int i = 0; i < searchList.size(); i++)
					{
						rect = calcRect(searchList.get(i));
						if (rect.top >= 0 && rect.top <= displayHeight) canvas.drawRoundRect(rect, 5, 5, paint);
					}
				}
	
				// highlights said line
				if (lineRect != null)
				{
					paint.setColor(0x700000FF);
					canvas.drawRoundRect(calcRect(lineRect), 5, 5, paint);
				}
	
				// debug
				if (debug)
				{
					paint.setColor(0xFFFFFFFF);
					canvas.drawText("textXY: " + xPos + " " + yPos, 0, 20, paint);
					canvas.drawText("pictureWidthY: " + pictureWidth + " " + pictureHeight, 0, 40, paint);
					canvas.drawText("w/h: " + textFormat.getTextImage().getWidth() + " " + textFormat.getTextImage().getHeight(),
							0, 60, paint);
					canvas.drawText("origHeight: " + origHeight, 0, 80, paint);
					canvas.drawText("screenXY: " + displayWidth + " " + displayHeight, 0, 100, paint);
					canvas.drawText("isWrite: " + textFormat.isWrite(), 0, 120, paint);
				}
			}
			catch (Exception ex)
			{
				return;
			}
		}
	}

	/**
	 * Calculate the highlight rectangle position and size
	 * 
	 * @param orig
	 * @return
	 */
	private RectF calcRect(RectF orig)
	{
		RectF rect = new RectF();
		if (pictureWidth > displayWidth)
		{
			float mulX = (float) pictureWidth / (float) origWidth;
			float mulY = (float) pictureHeight / (float) origHeight;

			rect.top = orig.top * mulY + yPos;
			rect.bottom = orig.bottom * mulY + yPos;
			rect.left = orig.left * mulX + xPos;
			rect.right = orig.right * mulX + xPos;
		}
		else
		{
			rect.top = orig.top + yPos;
			rect.bottom = orig.bottom + yPos;
			rect.left = orig.left + xPos;
			rect.right = orig.right + xPos;
		}

		return rect;
	}

	/**
	 * Check the user hit a button
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private int checkButtons(float x, float y)
	{
		hudAlphaCnt = HUD_TIMERMAX;
		int iconSize = prev1.getHeight();

		if (!textFormat.isWrite())
		{
			// left arrow
			if (x > 0 && x < iconSize && y > displayHeight - HUD_POSY && y < displayHeight - HUD_POSY + iconSize)
			{
				prevCnt = 1;
				return 1;
			}
			// rigt arrow
			if (x > 66 && x < 64 + iconSize && y > displayHeight - HUD_POSY && y < displayHeight - HUD_POSY + iconSize)
			{
				forwCnt = 1;
				return 2;
			}
			// search
			if (x > displayWidth - 64 && x < displayWidth - 64 + iconSize && y > displayHeight - HUD_POSY && y < displayHeight - HUD_POSY + iconSize)
			{
				findCnt = 1;
				return 3;
			}
			// scrollbar
			if (x > 128 && x < displayWidth - 128 && y > displayHeight - 64 && y < displayHeight)
			{
				thumbCnt = 1;
				return 4;
			}
			// text to speech
			if (x > displayWidth - 128 && x < displayWidth - 128 + iconSize && y > displayHeight - HUD_POSY
					&& y < displayHeight - HUD_POSY + iconSize)
			{
				return 5;
			}
		}
		return 0;
	}

	/**
	 * Text search dialog
	 */
	private void findText()
	{
		String sMessage = getResources().getString(R.string.finword);
		String sPButt = getResources().getString(R.string.findbutt);
		String sCancel = getResources().getString(R.string.cancel);

		searchDialogBuilder = new AlertDialog.Builder(TextReaderActivity.this);
		searchDialogBuilder.setMessage(sMessage);
		searchDialogBuilder.setCancelable(true);
		searchDialogBuilder.setPositiveButton(sPButt, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dialog.dismiss();

				searchText = comment.getText().toString();

				if (searchText.length() > 1)
				{
					nextPage = 4;
					thread = new Thread(TextReaderActivity.this);
					thread.start();
				}
			}
		});
		searchDialogBuilder.setNegativeButton(sCancel, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dialog.cancel();
			}
		});
		comment = new EditText(TextReaderActivity.this);
		comment.setHint("Keresés");
		comment.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				comment.setText("");
			}
		});
		comment.setOnKeyListener(new View.OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					switch (keyCode)
					{
					case KeyEvent.KEYCODE_DPAD_CENTER:
					case KeyEvent.KEYCODE_ENTER:
						InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						in.hideSoftInputFromWindow(comment.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						alertOnTap.dismiss();

						searchText = comment.getText().toString();

						if (searchText.length() > 1)
						{
							nextPage = 4;
							thread = new Thread(TextReaderActivity.this);
							thread.start();
						}
						return true;
					}
				}
				return false;
			}
		});
		searchDialogBuilder.setView(comment);

		alertOnTap = searchDialogBuilder.create();
		alertOnTap.show();
	}

	/**
	 * Store settings
	 * 
	 * @param first
	 * @return
	 */
	private boolean applySettings(boolean first)
	{
		boolean bRedraw = false;
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
		
		thumbX = 128;
		thumbY = displayHeight - 64;

		settings = getSharedPreferences("org.landroo.textreader_preferences", MODE_PRIVATE);
		int it = Integer.parseInt(settings.getString("colorScheme", "1"));
		if(it == 0 && it != colorScheme) bRedraw = true;
		colorScheme = it;
		it = Integer.parseInt(settings.getString("backGround", "0"));
		if(it != backGround) bRedraw = true;
		backGround = it; 
		it = Integer.parseInt(settings.getString("fontSize", "16"));
		if(it != fontSize) bRedraw = true;
		fontSize = it;
		it = Integer.parseInt(settings.getString("pageSize", "8000"));
		if(it != pageSize) bRedraw = true;
		pageSize = it;
		
		if(bFirst)
		{
			pictureHeight = pageSize;
			pictureWidth = displayWidth;
	
			origWidth = pictureWidth;
			origHeight = pictureHeight;
		}
		
		useDict = settings.getBoolean("enable_dictionary", true);
		dictLang = Integer.parseInt(settings.getString("dictLang", "1"));
		inForegin = settings.getBoolean("inForegin", true);
		inSayLang = Integer.parseInt(settings.getString("sayLang", "1"));
		boolean bZoom = settings.getBoolean("zoom_mode", false);
		int iZoom = bZoom ? 0 : 1;
		if (isZoomable == -1) isZoomable = iZoom;
		else if (isZoomable != iZoom)
		{
			android.os.Process.killProcess(android.os.Process.myPid());
			this.finish(); // restart needed
		}
		if (textFormat == null)	textFormat = new TextFormat(displayWidth, pageSize, handler);
		textFormat.setColorScheme(colorScheme);
		textFormat.setBackGround(backGround);
		textFormat.setFontSize(fontSize);
		textFormat.setMaxHeight(pageSize);
		textFormat.setTextWidth(displayWidth);
		
		// the x margin is fifty percent
		offMarginX = displayWidth / 2;
		//offMarginY = (displayWidth / 10) * (displayHeight / displayWidth);
		offMarginY = displayHeight / 10;

		bindDictionaries();

		if (first == false)
		{
			saveState();
			
			if(mTextService != null) setTTS();
		}
		
		Bitmap bitmap = textFormat.getBackImage(displayWidth, displayHeight, getResources());
		if(bitmap != null)
		{
			backDrawable = new BitmapDrawable(bitmap);
			backDrawable.setBounds(0, 0, displayWidth, displayHeight);
		}
		
		return bRedraw;
	}

	/**
	 * Call the external dictionary service
	 * 
	 * @author rkovacs
	 * 
	 */
	private class TranslateTask extends AsyncTask<Integer, Integer, Long>
	{
		private int mRes = -1;

		protected Long doInBackground(Integer... posXY)
		{
			int posX = posXY[0];
			int posY = posXY[1];
			String sWord = textFormat.getWordAtPos(posX, posY);
			if (sWord.equals("")) return (long) 0;
			String sPairs[] = sWord.split("\t");
			String sText;
			
			if(sWord.toLowerCase().indexOf(".jpeg") != -1
			|| sWord.toLowerCase().indexOf(".jpg") != -1
			|| sWord.toLowerCase().indexOf(".bmp") != -1
			|| sWord.toLowerCase().indexOf(".gif") != -1
			|| sWord.toLowerCase().indexOf(".png") != -1)// if image selected start imageViewer
			{
		        Bundle bundle = new Bundle();
		       	bundle.putString("file", sWord);
		       	
		       	//Log.i(TAG, sText);
		       	
				Intent intent = new Intent(TextReaderActivity.this, PictureViewer.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
				
				return (long) 0;
			}

			sWord = sPairs[0];
			wordRect = textFormat.getWordRect();
			if (sPairs.length == 2)
			{
				sText = sPairs[1];
				// if external link selected start browser
				if (sText.toLowerCase().indexOf("http") != -1
				|| sText.toLowerCase().indexOf(".com") != -1
				|| sText.toLowerCase().indexOf(".org") != -1) 
				{
					//Log.i(TAG, sText);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(sText));
					startActivity(intent);
				}
				else // if internal link selected go to chapter
				{
					String sName = textFormat.getFile();
					sFile = sName.substring(0, sName.lastIndexOf("/")) + "/" + sText;
					epub.setChapter(sFile);
					mRes = 2;
				}
				
				wordList = sText;
			}
			else // if word selected find it in the dictionary
			{
				findWord = true;
				sWord = sWord.replaceAll("[.]", " ");
				sWord = sWord.replaceAll("[?]", " ");
				sWord = sWord.replaceAll("[!]", " ");
				sWord = sWord.replaceAll("[,]", " ");
				sWord = sWord.replaceAll("[)]", " ");
				sWord = sWord.replaceAll("[(]", " ");
				sWord = sWord.replaceAll("[:]", " ");
				sWord = sWord.replaceAll("[“]", " ");
				sWord = sWord.replaceAll("[”]", " ");
				sWord = sWord.replaceAll("[;]", " ");
				sWord = sWord.trim();

				wordList = sWord;
				// handler.sendEmptyMessage(1);
				try
				{
					int iLang = 1;
					if (!inForegin) iLang = 2;
					if (sWord.length() > 1)
					{
						String sRes[] = new String[1];
						switch (dictLang)
						{
						case 1:
							sRes = enghunbig_service.fillWordList(sWord, iLang, 1);
							break;
						case 2:
							sRes = enghunmini_service.fillWordList(sWord, iLang, 1);
							break;
						case 3:
							sRes = esphunmini_service.fillWordList(sWord, iLang, 1);
							break;
						case 4:
							sRes = frahunbig_service.fillWordList(sWord, iLang, 1);
							break;
						case 5:
							sRes = gerhunmini_service.fillWordList(sWord, iLang, 1);
							break;
						}
						if (sRes[0].length() > 9 && sRes[0].equals("No data:-1"))
						{
							String sNotFound = getResources().getString(R.string.notinitialized);
							wordList = sNotFound;
						}
						else if ((sRes[0].length() > 9 && sRes[0].substring(0, 9).equals("No result")) || (sRes.length > 0 && sRes[0].equals("")))
						{
							String sNotFound = getResources().getString(R.string.notfound);
							wordList += "\n" + sNotFound;
						}
						else
						{
							wordList += "\n";
							int cnt = 0;
							String[] sArr = new String[sRes.length];
							for (int i = 0; i < sRes.length; i++)
							{
								sPairs = sRes[i].split("\t");
								sArr[i] = sPairs[1];
							}
							// Arrays.sort(sArr);
							for (int i = 0; i < sArr.length; i++)
							{
								if (cnt >= 80) break;
								if(wordList.indexOf(sArr[i]) == -1 && sArr[i].indexOf(" ") == -1)
								{
									if(cnt > 0) wordList += ", ";
									wordList += sArr[i];
									cnt++;
								}
							}
							
							if(cnt == 0)
							{
								for (int i = 0; i < sArr.length; i++)
								{
									if (cnt >= 80) break;
									if(wordList.indexOf(sArr[i]) == -1)
									{
										if(cnt > 0) wordList += ", ";
										wordList += sArr[i];
										cnt++;
									}
								}
							}
						}
						mRes = 1;
					}
				}
				catch (Exception ex)
				{
					Log.e(TAG, "Error in service");
				}
			}

			findWord = false;

			return (long) 0;
		}

		protected void onPostExecute(Long result)
		{
			handler.sendEmptyMessage(mRes);
		}
	}

	/**
	 * start text to speech
	 */
	private void showReadText()
	{
		if (isSay)
		{
			// get the size of the block reading
			lineRect = textFormat.getTextLine();
			if(!(-yPos < lineRect.bottom && -yPos + displayHeight > lineRect.bottom)) yPos = displayHeight - lineRect.bottom;
			if(yPos > 0) yPos = 0;
			
			textView.postInvalidate();
		}
	}

	/**
	 * clear state
	 */
	private void clearState()
	{
		thumbX = 128;
		prevCnt = 0;
		forwCnt = 0;
		findCnt = 0;
		thumbCnt = 0;
		wordRect = null;
		searchList.clear();

		if(mTextService != null) sendMessageToService(102, "", "");// TTS reset

		textFormat.resetSpeech();
		lineRect = null;
	}

	/**
	 * save last page number
	 */
	private void saveState()
	{
		switch (iDocType)
		{
		case HTML: // html
			html.setState(textFormat.getState());
			html.savePage(textFormat.getPageNo());
			break;
		case TEXT: // text
			text.setState(textFormat.getState());
			text.savePage(textFormat.getPageNo());
			break;
		case EPUB: // ePub
			epub.setState(textFormat.getState());
			epub.savePage(textFormat.getPageNo());
			break;
		case MOBI: // mobi
			mobi.setState(textFormat.getState());
			mobi.savePage(textFormat.getPageNo());
			break;
		case DOCX: // docx
			docx.setState(textFormat.getState());
			docx.savePage(textFormat.getPageNo());
			break;
		}
	}

	private void bindDictionaries()
	{
		try
		{
			// english dictionary
			if (dictLang == 1)
			{
				enghunbig_connection = new myEngHunBigServiceConnection();
				Intent intent = new Intent();
				intent.setClassName("org.landroo.enghunbig", "org.landroo.enghunbig.SmartDictServiceEngBig");
				if (bindService(intent, enghunbig_connection, Context.BIND_AUTO_CREATE)) mIsEngHunBigBound = true;
			}
			// english mini dictionary
			if (dictLang == 2)
			{
				enghunmini_connection = new myEngHunMiniServiceConnection();
				Intent intent = new Intent();
				intent.setClassName("org.landroo.enghunmini", "org.landroo.enghunmini.SmartDictServiceEngMini");
				if (bindService(intent, enghunmini_connection, Context.BIND_AUTO_CREATE)) mIsEngHunMiniBound = true;
			}
			// spanish mini dictionary
			if (dictLang == 3)
			{
				esphunmini_connection = new myEspHunMiniServiceConnection();
				Intent intent = new Intent();
				intent.setClassName("org.landroo.esphunmini", "org.landroo.esphunmini.SmartDictServiceEspMini");
				if (bindService(intent, esphunmini_connection, Context.BIND_AUTO_CREATE)) mIsEspHunMiniBound = true;
			}
			// french big dictionary
			if (dictLang == 4)
			{
				frahunbig_connection = new myFraHunBigServiceConnection();
				Intent intent = new Intent();
				intent.setClassName("org.landroo.frahunbig", "org.landroo.frahunbig.SmartDictServiceFraBig");
				if (bindService(intent, frahunbig_connection, Context.BIND_AUTO_CREATE)) mIsFraHunBigBound = true;
			}
			// german mini dictionary
			if (dictLang == 5)
			{
				gerhunmini_connection = new myGerHunMiniServiceConnection();
				Intent intent = new Intent();
				intent.setClassName("org.landroo.gerhunmini", "org.landroo.gerhunmini.SmartDictServiceGerMini");
				if (bindService(intent, gerhunmini_connection, Context.BIND_AUTO_CREATE)) mIsGerHunMiniBound = true;
			}
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new page!");
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}
	}
	
	private void checkOff()
	{
		if(pictureWidth >= displayWidth)
		{
			if(xPos > 0 && xPos <= offMarginX) backSpeedX = -1;
			else if(xPos < pictureWidth - offMarginX && xPos <= pictureWidth) backSpeedX = 1;
		}
		if(pictureHeight >= displayHeight)
		{
			if(yPos > 0 && yPos <= offMarginY) backSpeedY = -1;
			else if(yPos < pictureHeight - offMarginY && yPos <= pictureHeight) backSpeedY = 1;
		}
	}

	@Override
	public void onFingerChange()
	{
	}
	
    void doUnbindTextService()
    {
        try
	        {
	        if (mIsTextServiceBound)
	        {
	            // If we have received the service, and hence registered with it, then now is the time to unregister.
	            if (mTextService != null)
	            {
	                try
	                {
	                    Message msg = Message.obtain(null, TextService.MSG_UNREGISTER_CLIENT);
	                    msg.replyTo = mMessenger;
	                    mTextService.send(msg);
	                }
	                catch (RemoteException e)
	                {
	                    // There is nothing special we need to do if the service has crashed.
	                }
	            }
	            // Detach our existing connection.
	            unbindService(mTextServiceConnection);
	            mIsTextServiceBound = false;
	            //Log.i(TAG, "TextService Unbinding.");
	        }
	    }
        catch(Exception ex)
        {
        	Log.e(TAG, "Error int TextService Unbinding. " + ex);
        }
    }

    
    private void sendMessageToService(int intValue, String sName, String strValue)
    {
        if (mIsTextServiceBound)
        {
            if (mTextService != null)
            {
                try
                {
                    Message msg;
                    if(sName.equals(""))
                    {
                    	msg = Message.obtain(null, TextService.MSG_SET_INT_VALUE, intValue, 0);
                    	msg.replyTo = mMessenger;
                    	mTextService.send(msg);
                    }
                    else
                    {
                    	//Send data as a String
                    	Bundle bundle = new Bundle();
                    	bundle.putString(sName, strValue);
                    	msg = Message.obtain(null, TextService.MSG_SET_STRING_VALUE);
                    	msg.setData(bundle);
                    	mTextService.send(msg);
                    }
                }
                catch (RemoteException ex)
                {
                	Log.e(TAG, "Error in send message: " + ex);
                }
            }
            else
            {
            	Log.e(TAG, "TextService is NULL!");
            }
        }
    }
    
    private void setTTS()
    {
		// Text to speech
		enableSay = true;
		switch (inSayLang)
		{
		case 0:
			enableSay = false;
			break;
		case 1:
			sendMessageToService(110, "", "");// TTS default
			break;
		case 2:
			sendMessageToService(111, "", "");// TTS Englis
			break;
		case 3:
			sendMessageToService(112, "", "");// TTS french
			break;
		case 4:
			sendMessageToService(113, "", "");// TTS german
			break;
		}   	
    }
}