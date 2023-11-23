package org.landroo.textreader;

import java.io.File;
import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;

public class TextFormat
{
	private static final String TAG = "textReader";

	private static float LEFT_MARGIN = 5;
	private static float RIGHT_MARGIN = 5;
	private static float PARGRAPH_OFFSET = 8;

	private static Bitmap loadImage = null;
	private static Bitmap txtBitmap = null;
	private int txtWidth; // width of the view
	private int txtHeight; // height of the view
	private float foundPos;
	private float stampSize; // highlight size

	private ArrayList<Integer> paragList = new ArrayList<Integer>();// paragraphs
	private ArrayList<Integer> imageList = new ArrayList<Integer>();// images
	private ArrayList<Integer> imagePage = new ArrayList<Integer>();//
	private ArrayList<Integer> linkList = new ArrayList<Integer>();// link character start positions
	private int pageNum = 0;
	private String sState = "";

	private int colorScheme = 1;
	private int backGround = 0;
	private int fontSize = 16;
	private String[] sParags; // the main text paragraphs
	private ArrayList<TextPos> imagePos = null; // image positions from the text file
	private ArrayList<TextPos> linkPos = null; // link positions from the text file
	private String sFile = ""; // the selected file with path
	private int maxHeight = 8000;
	private ArrayList<TextPos> linePos = null; // text line positions on screen
	private RectF wordRect;
	private int lineHeight;
	private ArrayList<RectF> searchPos = null; // position rectangles of found words
  
	private int nextSentence = 0;// text to speech position
	private int speechCnt = 0;
	
	private boolean isWrite = false;
	private boolean newFile = false;
	
	private Handler handler;

	public TextFormat(int width, int height, Handler handler)
	{
		paragList.add(0);
		txtWidth = width;
		maxHeight = height;
		
		linePos = new ArrayList<TextPos>();
		wordRect = new RectF();
		searchPos = new ArrayList<RectF>();
		
		this.handler = handler; 
	}

	public void setImagePos(ArrayList<TextPos> pos)
	{
		imagePos = pos;
	}

	public ArrayList<TextPos> getLinkPos()
	{
		return linkPos;
	}

	public void setLinkPos(ArrayList<TextPos> pos)
	{
		linkPos = pos;
		return;
	}

	public String getFile()
	{
		return sFile;
	}

	public void setFile(String file)
	{
		newFile = false;
		if(!sFile.equals(file) && !sFile.equals("")) newFile = true;
		sFile = file;
	}

	public void setPageText(String text)
	{
		sParags = text.split("\n");
	}

	public void setMaxHeight(int height)
	{
		maxHeight = height;
	}

	public void setColorScheme(int scheme)
	{
		colorScheme = scheme;
	}
	
	public void setBackGround(int back)
	{
		backGround = back;
	}

	public void setFontSize(int size)
	{
		fontSize = size;
	}

	public Bitmap getTextImage()
	{
		return txtBitmap;
	}

	// next page
	public boolean nextPage()
	{
		if (pageNum + 1 < paragList.size())
		{
			pageNum++;
			clearText();
			
			// hide viewed pictures
			if (imagePos != null)
			{
				for (int i = 0; i < imagePos.size(); i++)
				{
					TextPos tp = imagePos.get(i);
					if(tp.miPictPage == pageNum) tp.mbPictOver = false;
				}
			}
			
			writeText();
			
			//Log.i(TAG, "nextPage ");
			
			return true;
		}
		return false;
	}

	// previous page
	public boolean prewPage()
	{
		if (pageNum > 0)
		{
			pageNum--;
			clearText();

			// hide viewed pictures
			if (imagePos != null)
			{
				for (int i = 0; i < imagePos.size(); i++)
				{
					TextPos tp = imagePos.get(i);
					if(tp.miPictPage == pageNum) tp.mbPictOver = false;
				}
			}

			writeText();
			
			//Log.i(TAG, "prewPage ");

			return true;
		}
		return false;
	}

	public boolean currPage()
	{
		if (txtWidth > 0) writeText();
		
		//Log.i(TAG, "currPage ");

		return true;
	}

	public int getTextWidth()
	{
		return txtWidth;
	}

	public void setTextWidth(int width)
	{
		txtWidth = width;
	}

	public int getTextHeight()
	{
		return txtHeight;
	}

	public boolean isNextPage()
	{
		if (pageNum + 1 < paragList.size())	return true;
		return false;
	}

	public boolean isPrewPage()
	{
		if (pageNum == 0) return false;
		return true;
	}

	public int getPageNo()
	{
		return pageNum;
	}

	/**
     * 
     */
	public synchronized boolean newText(String newState)
	{
		paragList = new ArrayList<Integer>();
		imageList = new ArrayList<Integer>();
		imagePage = new ArrayList<Integer>();
		linkList = new ArrayList<Integer>();
		
		if(newFile) newState = ""; 

		if (newState.equals(""))
		{
			pageNum = 0;

			paragList.add(0);
			imageList.add(0);
			imagePage.add(0);
			linkList.add(0);
		}
		else
		{
			String[] sArr1 = newState.split("[\n]");
			if (sArr1.length > 3)
			{
				pageNum = Integer.parseInt(sArr1[0]);

				if(pageNum != 0)
				{
					String[] sArr2 = sArr1[1].split("[;]");
					for (int i = 0; i < sArr2.length; i++) paragList.add(Integer.parseInt(sArr2[i]));
	
					sArr2 = sArr1[2].split("[;]");
					for (int i = 0; i < sArr2.length; i++) imageList.add(Integer.parseInt(sArr2[i]));
	
					sArr2 = sArr1[3].split("[;]");
					for (int i = 0; i < sArr2.length; i++) linkList.add(Integer.parseInt(sArr2[i]));
					
					sArr2 = sArr1[4].split("[;]");
					for (int i = 0; i < sArr2.length; i++)
					{
						if(!sArr2[i].equals(""))
						{
							int ip = Integer.parseInt(sArr2[i]);
							if(ip < pageNum)imagePage.add(ip);
						}
					}
	
					// show pictures on current page
					if (imagePos != null && imagePos.size() > 0)
					{
						for (int i = 0; i < imagePage.size(); i++)
						{
							TextPos tp = imagePos.get(i);
							int ip = imagePage.get(i);
							tp.miPictPage = ip;
							if(ip < pageNum) tp.mbPictOver = true;
						}
					}
				}
				
				if(paragList.size() > 0 && paragList.get(pageNum) > sParags.length)
				{
					pageNum = 0;

					paragList.add(0);
					imageList.add(0);
					imagePage.add(0);
					linkList.add(0);
				}
			}
			else
			{
				pageNum = 0;

				paragList.add(0);
				imageList.add(0);
				imagePage.add(0);
				linkList.add(0);
			}
		}
		
		writeText();
		
		//Log.i(TAG, "newText ");

		return true;
	}

	public float foundPos()
	{
		return foundPos * -1;
	}

	private void clearText()
	{
		switch (colorScheme)
		{
		case 0:
			txtBitmap.eraseColor(Color.TRANSPARENT);
			break;
		case 1:
			txtBitmap.eraseColor(Color.BLACK);
			break;
		case 2:
			txtBitmap.eraseColor(Color.WHITE);
			break;
		case 3:
			txtBitmap.eraseColor(Color.GRAY);
			break;
		case 4:
			txtBitmap.eraseColor(Color.GRAY);
			break;
		case 5:
			txtBitmap.eraseColor(Color.BLACK);
			break;
		}
	}
	
	private int calcHeight(Paint textPaint)
	{
		float x = 0;
		float y = lineHeight + PARGRAPH_OFFSET;
		
		float minSpaceWidth = textPaint.measureText("i");
		float fMes;
		int iImgPos = imageList.get(pageNum);
		
		int iLine = 0;
		float lineWidth = 0;
		float spaceWidth;
		
		String sTxt;
		String[] sWords;
		
		for (int paragcnt = paragList.get(pageNum); paragcnt < sParags.length; paragcnt++)
		{
			sTxt = sParags[paragcnt];
			sWords = sTxt.split("[ ]");
			
			// calculate and add bitmap at position
			y = drawImage(iImgPos, x, y, textPaint, null);

			// next page
			fMes = textPaint.measureText(sTxt);
			if (y + (fMes / txtWidth) * lineHeight > maxHeight)
			{
				int i = 0;
				for (i = 0; i < paragList.size(); i++)
				{
					if (paragList.get(i) == paragcnt) break;
				}

				break;
			}

			iImgPos += sTxt.length() + 1;

			if (sWords.length > 0)
			{
				for (int wordcnt = 0; wordcnt < sWords.length; wordcnt++)
				{
					iLine = 0;
					lineWidth = 0;

					// new paragraph
					if (wordcnt == 0) lineWidth = minSpaceWidth * 2;

					// calculate line width
					while (lineWidth < txtWidth && wordcnt + iLine < sWords.length)
					{
						sTxt = sWords[wordcnt + iLine];
						fMes = textPaint.measureText(sTxt);

						if (lineWidth + fMes > txtWidth) break;

						lineWidth += fMes + minSpaceWidth;
						iLine++;
					}

					// wrong line
					if (iLine == 0)
					{
						if(lineWidth + fMes > txtWidth)
						{
							iLine = 1;
							lineWidth = txtWidth;
							spaceWidth = 0;
						}
						else continue;
					}
					else
					{
						// calculate space width
						spaceWidth = (txtWidth - LEFT_MARGIN - RIGHT_MARGIN - lineWidth) / (iLine - 1) + minSpaceWidth;
					}

					// check maximum space width
					if (spaceWidth > minSpaceWidth * 4) spaceWidth = minSpaceWidth;

					// begin line
					x = LEFT_MARGIN;
					if (wordcnt == 0) x = minSpaceWidth * 3;

					y += lineHeight;
					wordcnt += iLine - 1;
				}
			}
			else y += lineHeight;

			y += PARGRAPH_OFFSET;
		}

		// calculate and add bitmap at position
		y = drawImage(iImgPos, x, y, textPaint, null);

		y += lineHeight;
		
		// restore the original state
		if(imagePos != null)
		{
			for (TextPos tp: imagePos)
			{
				if(tp.bCalc)
				{
					tp.mbPictOver = false;
					tp.miPictPage = -1;
					tp.bCalc = false;
				}
			}
		}		
		//Log.i(TAG, "calcHeight " + y);
		
		return (int) y;
	}

	/**
	 * the main writer function
	 * @return
	 */
	private int writeText()
	{
		if (imageList == null || pageNum >= imageList.size()) return 0;
		
		if (pageNum == 0) clearState();

		isWrite = true;

		// text color
		Paint textPaint = setBaseColor();
		// link background
		Paint linkPaint = setLinkColor();

		lineHeight = fontSize + 2;
		stampSize = fontSize / 4;
		float minSpaceWidth = textPaint.measureText("i");

		String[] sWords;
		int iLine = 0, iLnk;
		float lineWidth = 0;
		float spaceWidth;
		float x = 0;
		float y = lineHeight + PARGRAPH_OFFSET;
		float fMes = 0;
		float m;
		String sTxt;
		int iImgPos = imageList.get(pageNum);
		int iLnkPos = linkList.get(pageNum);
		int iCharNo = iLnkPos;

		linePos.clear();
		
		int height = calcHeight(textPaint);
		if(createBitmap(height) == false) return 0;
		
		Canvas canvas = new Canvas(txtBitmap);
		
		for (int paragcnt = paragList.get(pageNum); paragcnt < sParags.length; paragcnt++)
		{
			sTxt = sParags[paragcnt];
			sWords = sTxt.split("[ ]");
			iCharNo += sTxt.length();
			
			// calculate and add bitmap at position
			y = drawImage(iImgPos, x, y, textPaint, canvas);

			// next page
			fMes = textPaint.measureText(sTxt);
			if (y + (fMes / txtWidth) * lineHeight > maxHeight)
			{
				int i = 0;
				for (i = 0; i < paragList.size(); i++)
				{
					if (paragList.get(i) == paragcnt) break;
				}
				if (i == paragList.size())
				{
					paragList.add(paragcnt);
					imageList.add(iImgPos);
					linkList.add(iLnkPos);
				}

				break;
			}

			iImgPos += sTxt.length() + 1;

			if (sWords.length > 0)
			{
				for (int wordcnt = 0; wordcnt < sWords.length; wordcnt++)
				{
					iLine = 0;
					lineWidth = 0;

					// new paragraph
					if (wordcnt == 0) lineWidth = minSpaceWidth * 2;

					// calculate line width
					while (lineWidth < txtWidth && wordcnt + iLine < sWords.length)
					{
						sTxt = sWords[wordcnt + iLine];
						fMes = textPaint.measureText(sTxt);

						// if line wider than the screen !!!
						if (lineWidth + fMes > txtWidth) break;

						lineWidth += fMes + minSpaceWidth;
						iLine++;
					}

					// wrong line
					if (iLine == 0)
					{
						if(lineWidth + fMes > txtWidth || lineWidth > txtWidth)
						{
							iLine = 1;
							lineWidth = txtWidth;
							spaceWidth = 0;
						}
						else 
						{
							Log.i(TAG, "Wrolg line: " + sTxt);
							continue;
						}
					}
					else
					{
						// calculate space width
						spaceWidth = (txtWidth - LEFT_MARGIN - RIGHT_MARGIN - lineWidth) / (iLine - 1) + minSpaceWidth;
					}

					// check maximum space width
					if (spaceWidth > minSpaceWidth * 4) spaceWidth = minSpaceWidth;

					// begin line
					x = LEFT_MARGIN;
					if (wordcnt == 0) x = minSpaceWidth * 3;

					TextPos line = new TextPos((int) x, (int) y, "", spaceWidth, false, false);

					// draw words
					for (int i = 0; i < iLine; i++)
					{
						sTxt = sWords[wordcnt + i];
						m = textPaint.measureText(sTxt);

						// new paragraph
						if (i > 0) x += spaceWidth;

						// link highlight on counter
						iLnk= checkLinks(iLnkPos, iLnkPos + sTxt.length(), sTxt);
						if (iLnk != -1)
						{
							RectF rec = new RectF(x, y - lineHeight + stampSize, x + m, y + stampSize);
							canvas.drawRoundRect(rec, 5, 5, linkPaint);
							line.linkPos = iLnk;
						}
						
						// draw text on the x, y position!!!
						canvas.drawText(sTxt, x, y, textPaint);

						line.msText += sTxt + " ";

						iLnkPos += sTxt.length() + 1;

						x += m;
					}

					y += lineHeight;
					wordcnt += iLine - 1;

					linePos.add(line);
				}
				
				// fix character position if necessary
				if(iLnkPos != iCharNo) iLnkPos = iCharNo;
			}
			else y += lineHeight;

			y += PARGRAPH_OFFSET;
			if (linePos.size() > 0) linePos.get(linePos.size() - 1).mbEnd = true;
		}

		// calculate and add bitmap at position
		y = drawImage(iImgPos, x, y, textPaint, canvas);

		y += lineHeight;

		txtHeight = (int) y;
		
		//Log.i(TAG, "txtHeight " + txtHeight + " imgHeight " + txtBitmap.getHeight());

		saveState();

		isWrite = false;

		return (int)y;
	}
	
	private boolean createBitmap(int height)
	{
		try
		{
			if(txtBitmap != null) txtBitmap.recycle();
			txtBitmap = null;
			System.gc();
			
			txtBitmap = Bitmap.createBitmap(txtWidth, height, Bitmap.Config.ARGB_4444);
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new page!");
			if(txtBitmap != null) txtBitmap.recycle();
			txtBitmap = null;
			System.gc();
			handler.sendEmptyMessage(5);
			
			return false;
		}
		catch (Exception ex)
		{
			return false;
		}
		
		return true;
	}

	private float drawImage(int iImgPos, float x, float y, Paint paint, Canvas canvas)
	{
		// calculate and add bitmap at position
		TextPos tp = checkImages(iImgPos, canvas != null);
		int top, left;
		if (tp != null && loadImage != null)
		{
			x = LEFT_MARGIN;
			if (txtWidth > loadImage.getWidth() - RIGHT_MARGIN) x = (txtWidth - loadImage.getWidth()) / 2;

			if(canvas != null) canvas.drawBitmap(loadImage, x, y - lineHeight + stampSize, paint);
			left = (int)x;
			top = (int)(y - lineHeight + stampSize);
			
			tp.miTop = top;
			tp.miLeft = left;
			tp.miRight = left + loadImage.getWidth();
			tp.miBottom = top + loadImage.getHeight();

			y += PARGRAPH_OFFSET;
			try
			{
				y += loadImage.getHeight();
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Load imgae error!");
			}

			loadImage.recycle();
			loadImage = null;
			System.gc();
		}

		return y;
	}

	/**
	 * Draw a bitmap into the page
	 * @param iPos
	 * @return
	 */
	private TextPos checkImages(int iPos, boolean act)
	{
		if (imagePos != null)
		{
			for (TextPos tp: imagePos)
			{
				if (tp != null && tp.miYPos <= iPos && tp.mbPictOver == false)
				{
					// set picture sate to viewed
					tp.mbPictOver = true;
					tp.miPictPage = pageNum;
					if(act) imagePage.add(pageNum);
					else tp.bCalc= true; 
					
					String sImagePath = sFile.substring(0, sFile.lastIndexOf("/")) + "/" + tp.msText;
					File imgFile = new File(sImagePath);
					if (imgFile.exists())
					{
						try
						{
							loadImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
							if (loadImage != null && txtWidth < loadImage.getWidth())
							{
								int origWidth = loadImage.getWidth();
								int origHeight = loadImage.getHeight();
								float newheight = (float) txtWidth / (float) origWidth * (float) origHeight;
								float scaleWidth = ((float) txtWidth) / origWidth;
								float scaleHeight = newheight / origHeight;
								Matrix matrix = new Matrix();
								matrix.postScale(scaleWidth, scaleHeight);
								loadImage = Bitmap.createBitmap(loadImage, 0, 0, origWidth, origHeight, matrix, false);
							}
						}
						catch (OutOfMemoryError e)
						{
							Log.e(TAG, "Out of memory error in new page!");
							if(txtBitmap != null) txtBitmap.recycle();
							txtBitmap = null;
							System.gc();
							
							handler.sendEmptyMessage(5);
						}
						catch (Exception ex)
						{
							Log.e(TAG, "Load image error!");
						}
					}
					return tp;
				}
			}
		}
		
		return null;
	}

	/**
	 * 
	 * @param iPos
	 * @return
	 */
	private int checkLinks(int iBeg, int iEnd, String sTxt)
	{
		int iRet = -1;
		
		if (linkPos != null)
		{
			try
			{
				for (TextPos lp: linkPos)
				{
					if (iBeg <= lp.miYPos && iEnd > lp.miYPos)
					{
						iRet = lp.miYPos;
						//Log.i(TAG, sTxt + " " + lp.miYPos + " " + iBeg + " " + iEnd);
						break;
					}
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "linkPos Error (" + iBeg + ", " + iEnd + ")");
			}
		}
		
		return iRet;
	}

	private Paint setBaseColor()
	{
		Paint textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setStrokeWidth(1);
		textPaint.setTextSize(fontSize);
		
		if(colorScheme == 0)
		{
			switch (backGround)
			{
			case 0:
				textPaint.setColor(Color.WHITE);
				break;
			case 1:
				textPaint.setColor(Color.BLACK);
				break;
			case 2:
				textPaint.setColor(Color.BLACK);
				break;
			case 3:
				textPaint.setColor(Color.BLACK);
				break;
			case 4:
				textPaint.setColor(Color.BLACK);
				break;
			case 5:
				textPaint.setColor(Color.WHITE);
				break;
			case 6:
				textPaint.setColor(Color.BLACK);
				break;
			case 7:
				textPaint.setColor(Color.BLACK);
				break;
			case 8:
				textPaint.setColor(Color.BLACK);
				break;
			}			
		}
		else
		{
			switch (colorScheme)
			{
			case 0:
				textPaint.setColor(Color.WHITE);
				break;
			case 1:
				textPaint.setColor(Color.WHITE);
				break;
			case 2:
				textPaint.setColor(Color.BLACK);
				break;
			case 3:
				textPaint.setColor(Color.BLACK);
				break;
			case 4:
				textPaint.setColor(Color.WHITE);
				break;
			case 5:
				textPaint.setColor(Color.GRAY);
				break;
			}
		}		

		return textPaint;
	}

	private Paint setLinkColor()
	{
		Paint linkPaint = new Paint();
		linkPaint.setAntiAlias(true);

		switch (colorScheme)
		{
		case 0:
			linkPaint.setColor(0xFFFF9933);
			break;
		case 1:
			linkPaint.setColor(0xFFFF9933);
			break;
		case 2:
			linkPaint.setColor(0xFFFF9933);
			break;
		case 3:
			linkPaint.setColor(0xFFFF9933);
			break;
		case 4:
			linkPaint.setColor(0xFFFF9933);
			break;
		case 5:
			linkPaint.setColor(0xFFFF9933);
			break;
		}

		return linkPaint;
	}

	/**
	 * Select the closest word of the coordinate
	 * @param posx
	 * @param posy
	 * @return a word or the picture path
	 */
	public String getWordAtPos(float posx, float posy)
	{
		String sRes = "";
		float x = 0;
		float m = 0;
		String[] sArr;
		Paint textPaint = setBaseColor();
		String word;
		
		// an image selected
		if(imagePos != null)
		{
			for (TextPos ip: imagePos)
			{
				if(posx > ip.miLeft && posx < ip.miRight && posy > ip.miTop &&posy < ip.miBottom)
				{
					sRes = sFile.substring(0, sFile.lastIndexOf("/")) + "/" + ip.msText;
					return sRes;
				}
			}
		}
		// a word selected
		if (linkList != null) 
		{
			// through line position list
			for (TextPos line: linePos)
			{
				// y position bigger than tap y position
				if (line.miYPos >= posy)
				{
					// split the line to words
					sArr = line.msText.split("[ ]");
					m = textPaint.measureText("i");
					x = line.miXPos - m / 2;
					
					// through the word list
					for (int i = 0; i < sArr.length; i++)
					{
						word = sArr[i];
						m = textPaint.measureText(word);
	
						wordRect.top = line.miYPos - lineHeight + stampSize;
						wordRect.left = x;
						wordRect.right = x + m + stampSize;
						wordRect.bottom = line.miYPos + stampSize;
	
						x += m;
	
						// select the word in line
						if (x >= posx)
						{
							sRes = word;
							
							// check the word is a link
							if (linkPos != null)
							{
								// through the link position list
								for(TextPos link: linkPos)
								{
									Log.i(TAG, "" + line.linkPos + " " + link.miYPos + " " + link.msText);
									if (line.linkPos == link.miYPos)
									{
										sRes += "\t" + link.msText;
										Log.i(TAG, sRes);
										break;
									}
								}
							}
							break;
						}
	
						x += line.mfSize;
					}
					break;
				}
			}
		}
		
		return sRes;
	}

	public RectF getWordRect()
	{
		return wordRect;
	}

	public int findWord(String sWord)
	{
		String[] sArr;
		float x = 0;
		float y = 0;
		float m = 0;
		Paint textPaint = setBaseColor();
		int foundCnt = 0;
		foundPos = 0;
		searchPos.clear();
		for (int i = 0; i < linePos.size(); i++)
		{
			m = textPaint.measureText("i");
			x = linePos.get(i).miXPos - m / 2;
			y = linePos.get(i).miYPos;
			
			sArr = linePos.get(i).msText.split("[ ]");
			for (int j = 0; j < sArr.length; j++)
			{
				m = textPaint.measureText(sArr[j]);
				if (sArr[j].toLowerCase().indexOf(sWord) != -1)
				{
					if (foundPos == 0) foundPos = y - lineHeight;
					RectF rect = new RectF();
					rect.top = y - lineHeight + stampSize;
					rect.left = x;
					rect.right = x + m + stampSize;
					rect.bottom = y + stampSize;
					searchPos.add(rect);

					foundCnt++;
				}
				x += m;
				x += linePos.get(i).mfSize;
			}
		}

		return foundCnt;
	}

	public ArrayList<RectF> getSearchList()
	{
		return searchPos;
	}

	public void drawFound()
	{
		if (searchPos.size() > 0)
		{
			Canvas canvas = null;
			canvas = new Canvas(txtBitmap);

			Paint paint = new Paint();
			paint.setColor(0x7000FF00);
			for (int i = 0; i < searchPos.size(); i++) canvas.drawRoundRect(searchPos.get(i), 5, 5, paint);
		}
	}

	private int checkEnd(String sLine)
	{
		int iRes = -1;
		iRes = sLine.indexOf(".");
		if (iRes != -1)	return iRes;
		
		iRes = sLine.indexOf("?");
		if (iRes != -1) return iRes;
		
		iRes = sLine.indexOf("!");
		if (iRes != -1) return iRes;
		
		return iRes;
	}

	/**
	 * Get a line for speech.
	 * 
	 * @return String a complete line.
	 */
	public RectF getTextLine()
	{
		String sTmp = "";

		int iPos = 0;
		int iNo = 1;
		float y = 0;
		TextPos textPos;

		while (speechCnt < linePos.size())
		{
			textPos = linePos.get(speechCnt);
			sTmp = textPos.msText.substring(nextSentence);
			
			iPos = checkEnd(sTmp);
			if (iPos != -1)
			{
				iPos++;
				y = linePos.get(speechCnt).miYPos;

				// line end with line end
				if (iPos == sTmp.length() - 1 || iPos == sTmp.length() - 2)
				{
					speechCnt++;
					nextSentence = 0;
				}
				else nextSentence += iPos;

				break;
			}
			else
			{
				nextSentence = 0;
			}
			
			if (textPos.mbEnd)
			{
				nextSentence = 0;
				y = linePos.get(speechCnt).miYPos;
				speechCnt++;
				break;
			}			

			speechCnt++;
			iNo++;
		}

		RectF speechRect = new RectF();
		speechRect.top = y - iNo * lineHeight + stampSize;
		speechRect.left = 0;
		speechRect.right = txtWidth;
		speechRect.bottom = y + stampSize;
		
		//Log.i(TAG, "speechCnt: "+ speechCnt);

		return speechRect;
	}

	public void resetSpeech()
	{
		speechCnt = 0;
		nextSentence = 0;
	}

	/**
	 * Draw a bitmap in middle.
	 * 
	 * @param bitmap bitmap
	 * @param w width
	 * @param h height
	 */
	public void drawBitmap(Bitmap bitmap, int w, int h)
	{
		Canvas canvas = null;
		if (txtBitmap != null)
		{
			canvas = new Canvas(txtBitmap);

			Paint textPaint = setBaseColor();
			float x = (w - bitmap.getWidth()) / 2;
			float y = (h - bitmap.getHeight()) / 2;

			canvas.drawBitmap(bitmap, x, y, textPaint);
		}
	}

	/**
	 * 
	 * @param sPos
	 * @return
	 */
	public boolean setTextPos(String sPos)
	{
		if (!sPos.equals(""))
		{
			paragList = new ArrayList<Integer>();
			imageList = new ArrayList<Integer>();
			imagePage = new ArrayList<Integer>();
			linkList = new ArrayList<Integer>();

			String[] lines = sPos.split("[\n]");
			String[] pos;
			for (int i = 0; i < lines.length; i++)
			{
				pos = lines[i].split("[ ]");
				for (int j = 1; j < pos.length - 1; j++)
				{
					switch (i)
					{
					case 0:
						paragList.add(Integer.parseInt(pos[j]));
						pageNum = pos.length - 3;
						break;
					case 1:
						imageList.add(Integer.parseInt(pos[j]));
						break;
					case 2:
						linkList.add(Integer.parseInt(pos[j]));
						break;
					case 3:
						imagePage.add(Integer.parseInt(pos[j]));
						break;
					}
				}
			}
			return false;
		}

		return true;
	}

	/**
	 * create position string (paragList, imageList, linkList)
	 */
	private void saveState()
	{
		sState = "";
		for (int i : paragList)	sState += i + ";";
		sState += "\n";
		for (int i : imageList)	sState += i + ";";
		sState += "\n";
		for (int i : linkList) sState += i + ";";
		sState += "\n";
		for (int i : imagePage) sState += i + ";";
		sState += "\n";
	}

	/**
	 * return position string
	 * 
	 * @return
	 */
	public String getState()
	{
		return sState;
	}

	/**
	 * get is write state
	 * 
	 * @return
	 */
	public boolean isWrite()
	{
		return isWrite;
	}

	/**
	 * set is write state
	 * 
	 * @param isWrite
	 */
	public void setIsWrite(boolean isWrite)
	{
		this.isWrite = isWrite;
	}

	public void clearState()
	{
		sState = "";
		
		paragList = new ArrayList<Integer>();
		imageList = new ArrayList<Integer>();
		imagePage = new ArrayList<Integer>();
		linkList = new ArrayList<Integer>();

		pageNum = 0;

		paragList.add(0);
		imageList.add(0);
		linkList.add(0);
	}
	
	public String getText()
	{
		StringBuffer sBuff = new StringBuffer();
		for(TextPos textPos: linePos)
		{
			sBuff.append(textPos.msText);
			if (textPos.mbEnd) sBuff.append("\n");
		}
		
		return sBuff.toString();
	}
	
	public Bitmap getBackImage(int w, int h, Resources res)
	{
		Bitmap bitmap = null;
		if(colorScheme == 0)
		{
			setBaseColor();
			switch(backGround)
			{
			case 0:
				bitmap = getBackGround(w, h, res);
				break;
			case 1:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.paper1);
				break;
			case 2:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.paper2);
				break;
			case 3:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.paper3);
				break;
			case 4:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.paper4);
				break;
			case 5:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.stone1);
				break;
			case 6:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.stone2);
				break;
			case 7:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.stone3);
				break;
			case 8:
				bitmap = BitmapFactory.decodeResource(res, R.drawable.stone4);
				break;
			}
		}
		
		return bitmap;
	}
	
	public Bitmap getBackGround(int w, int h, Resources res)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		//bitmap.eraseColor(0xFFFF0000);
		Canvas canvas = new Canvas(bitmap);
		RectF rect = new RectF();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);

		int color1 = 0xFF222222;
		int color2 = 0xFF882222;

		int[] colors = new int[2];
		colors[0] = color1;
		colors[1] = color2;

		float bw = w / 10;
		float bh = h / 40;
		float gap = w / 200;

		LinearGradient grad;

		for (int i = 0; i < 11; i++)
		{
			for (int j = 0; j < 40; j++)
			{
				if (random(0, 1, 1) == 1)
				{
					colors[0] = color1;
					colors[1] = color2 + random(0, 3, 1) * 0x1100;
				}
				else
				{
					colors[1] = color1;
					colors[0] = color2 + random(0, 3, 1) * 0x1100;
				}

				if (j % 2 == 0)
				{
					grad = new LinearGradient(i * bw, j * bh, i * bw + bw, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap, j * bh + gap, i * bw + bw - gap, j * bh + bh - gap);
				}
				else
				{
					grad = new LinearGradient(i * bw - bw / 2, j * bh, i * bw + bw - bw / 2, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap - bw / 2, j * bh + gap, i * bw + bw - gap - bw / 2, j * bh + bh - gap);
				}
				paint.setShader(grad);
				canvas.drawRect(rect, paint);
			}
		}
		
		paint.setShader(null);
		paint.setAlpha(64);
		
		float width = w / 2;
		float height = h / 2;
		
		float x, y, r, scaleWidth, scaleHeight;
		Bitmap img;

		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.amoba), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.piper), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x + width, y + height, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.jewel), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x + width, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10) / 2;
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.colorizer), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x, y + height, paint);

		return bitmap;
	}
	
	private Bitmap rotImage(Bitmap img, float rot)
	{
		int origWidth = img.getWidth();
		int origHeight = img.getHeight();
		Matrix matrix = new Matrix();
		matrix.setRotate(rot, img.getWidth() / 2, img.getHeight() / 2);
		Bitmap outImage = Bitmap.createBitmap(img, 0, 0, origWidth, origHeight, matrix, false);
		
		return outImage;
	}

	// generate a random integer
	public int random(int nMinimum, int nMaximum, int nRoundToInterval)
	{
		if (nMinimum > nMaximum)
		{
			int nTemp = nMinimum;
			nMinimum = nMaximum;
			nMaximum = nTemp;
		}

		int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
		double nRandomNumber = Math.random() * nDeltaRange;

		nRandomNumber += nMinimum;

		int nRet = (int) (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);

		return nRet;
	}
}
