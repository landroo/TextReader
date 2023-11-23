package org.landroo.parsers;

public interface ParserBase 
{
	public String getFileName();
	public void savePage(int iPage);
	public String loadLast(String fileName);
	public String getImageName();
	public void setState(String state);
	public String getOrigPath();
}
