package org.landroo.textreader;

public class TextPos 
{
	public int miXPos = 0;
	public int miYPos = 0;

	public int miTop = 0;
	public int miLeft = 0;
	public int miRight = 0;
	public int miBottom = 0;

	public String msText = "";//
	public float mfSize = 0;//
	public boolean mbEnd = true;//
	public boolean mbPictOver = false;// the picture already displayed
	public int miPictPage = -1;// picture page
	public boolean bCalc = false;// for calc
	public int linkPos = -1;// position in link list
	
	public TextPos()
	{
	}
	
	public TextPos(int xpos, int ypos, String name, float size, boolean end, boolean over)
	{
		miXPos = xpos;
		miYPos = ypos;
		msText = name;
		mfSize = size;
		mbEnd = end;
		mbPictOver = over;
	}
}
