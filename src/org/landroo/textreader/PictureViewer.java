package org.landroo.textreader;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

public class PictureViewer extends Activity implements UIInterface
{
	private static final String TAG = "SampleIdent";
	private static final int SWIPE_INTERVAL = 10;
	
	private static Bitmap bitmap = null; // the paper
	private BitmapDrawable bitmapDrawable;

	private UI ui = null;

	private int displayWidth = 0; // display width
	private int displayHeight = 0; // display height

	private PictureView pictureView; // the view

	private int sX = 0;
	private int sY = 0;
	private int mX = 0;
	private int mY = 0;

	private float xPos = 0;
	private float yPos = 0;

	public float pictureWidth;
	public float pictureHeight;
	public float origWidth;
	public float origHeight;

	private Timer swipeTimer = null;
	private float swipeDistX = 0;
	private float swipeDistY = 0;
	private float swipeVelocity = 0;
	private float swipeSpeed = 0;
	private float backSpeedX = 0;
	private float backSpeedY = 0;
	private float offMarginX = 0;
	private float offMarginY = 0;

	private float zoomSize = 0;
	
	private Paint paint;
	
	private class PictureView extends View
	{
		public PictureView(Context context)
		{
			super(context);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			try
			{
				if (bitmapDrawable != null)
				{
					canvas.translate(xPos, yPos);
					bitmapDrawable.draw(canvas);
					canvas.translate(-xPos, -yPos);
					//canvas.restore();
				}
			}
			catch (Exception ex)
			{
				Log.i(TAG, ex.getMessage());
			}
		}
	}	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		pictureView = new PictureView(this);
		setContentView(pictureView);

		ui = new UI(this);
		
		String sFile = "";
		Bundle extras = getIntent().getExtras();
		if (extras != null) sFile = extras.getString("file");

		initApp(sFile);

		paint = new Paint();
		paint.setTextSize(48);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);

		swipeTimer = new Timer();
		swipeTimer.scheduleAtFixedRate(new SwipeTask(), 0, SWIPE_INTERVAL);
	}
	
	private void initApp(String sFile)
	{
		try
		{
			File imgFile = new File(sFile);
			if (imgFile.exists())
			{
				bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
	
				pictureWidth = bitmap.getWidth();
				pictureHeight = bitmap.getHeight();
			
				origWidth = pictureWidth;
				origHeight = pictureHeight;
			
				bitmapDrawable = new BitmapDrawable(bitmap);
				bitmapDrawable.setBounds(0, 0, (int) pictureWidth, (int) pictureHeight);
				
				// the margin is ten percent
				offMarginX = displayWidth / 10;
				offMarginY = (displayWidth / 10) * (displayHeight / displayWidth);
			
				xPos = (displayWidth - pictureWidth) / 2;
				yPos = (displayHeight - pictureHeight) / 2;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public synchronized void onResume()
	{
		super.onResume();
	}

	@Override
	public synchronized void onPause()
	{
		super.onPause();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return ui.tapEvent(event);
	}
	
	@Override
	public void onDown(float x, float y)
	{
		sX = (int) x;
		sY = (int) y;

		swipeVelocity = 0;

		pictureView.postInvalidate();
	}

	@Override
	public void onUp(float x, float y)
	{
		checkOff();
		
		pictureView.postInvalidate();
	}

	@Override
	public void onTap(float x, float y)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHold(float x, float y)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMove(float x, float y)
	{
		mX = (int) x;
		mY = (int) y;

		float dx = mX - sX;
		float dy = mY - sY;

		// picture bigger than the display
		if (pictureWidth >= displayWidth)
		{
			if(xPos + dx < displayWidth - (pictureWidth + offMarginX) || xPos + dx > offMarginX) dx = 0;
			if(yPos + dy < displayHeight - (pictureHeight + offMarginY) || yPos + dy > offMarginY) dy = 0;
		}
		else
		{
			if(xPos + dx > displayWidth - pictureWidth || xPos + dx < 0) dx = 0;
			if(yPos + dy > displayHeight - pictureHeight || yPos + dy < 0) dy = 0;
		}

		xPos += dx;
		yPos += dy;

		sX = (int) mX;
		sY = (int) mY;

		pictureView.postInvalidate();

		return;
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		swipeDistX = x2 - x1;
		swipeDistY = y2 - y1;
		swipeSpeed = 1;
		swipeVelocity = velocity;

		pictureView.postInvalidate();

		return;
	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		swipeVelocity = 0;
		
		backSpeedX = 0;
		backSpeedY = 0;
	
		pictureWidth = origWidth;
		pictureHeight = origHeight;

		xPos = (displayWidth - pictureWidth) / 2;
		yPos = (displayHeight - pictureHeight) / 2;

		bitmapDrawable.setBounds(0, 0, (int) pictureWidth, (int) pictureHeight);

		pictureView.postInvalidate();		
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		int dist = (int) distance * 10;
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
			if (newSizeX > origWidth / 2 && newSizeX < origWidth * 10)
			{
				bitmapDrawable.setBounds(0, 0, (int)newSizeX, (int)newSizeY);

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

		pictureView.postInvalidate();

		return;
	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
	}

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub
		
	}
	
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

				if ((pictureWidth > displayWidth) && (x < displayWidth - pictureWidth || x > 0)
						|| ((pictureWidth <= displayWidth) && (x > displayWidth - pictureWidth || x < 0)))
				{
					swipeDistX *= -1;
					swipeSpeed += .1;
				}

				if ((pictureHeight > displayHeight) && (y < displayHeight - pictureHeight || y > 0)
						|| ((pictureHeight <= displayHeight) && (y > displayHeight - pictureHeight || y < 0)))
				{
					swipeDistY *= -1;
					swipeSpeed += .1;
				}

				xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				swipeVelocity -= swipeSpeed;
				swipeSpeed += .0001;

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
			
			if(redraw) pictureView.postInvalidate();
			
			return;
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
}
