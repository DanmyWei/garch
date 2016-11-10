package com.njust.helper;

import org.rosuda.REngine.Rserve.RConnection;

public class RServeConnection
{
	private static int index;
	private static String folderPath;
	private static String filePath;

	public RServeConnection()
	{
		index = 1;
		folderPath = "D://R-Data/test-"; // 默认写入D盘R-Data文件夹
	}

	public static void setFolderPath(String diskName, String folderName,
			String fileName)
	{
		folderPath = diskName + "://" + folderName + "/" + fileName + "-";
	}
	
	public static String getFilePath()
	{
		return filePath;
	}

	public static void make(String cmdLine[])
	{
		filePath = folderPath + index++ + ".jpg";
		
		try
		{
			RConnection c = new RConnection(); // 打开RServe连接
			c.eval("jpeg('" + filePath + "')"); // 图片路径
			for (int i = 0; i < cmdLine.length; i++)
				c.eval(cmdLine[i]); // 依次执行R命令
			c.eval("dev.off()"); // 结束此次渲染
			c.close();
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}
}