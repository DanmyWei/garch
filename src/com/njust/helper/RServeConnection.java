package com.njust.helper;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RServeConnection
{
	private static int index;
	private static String folderPath;
	private static String filePath;
	private static RConnection c;

	public RServeConnection()
	{
		index = 1;
		folderPath = "D://R-Data/test-"; // 默认写入D盘R-Data文件夹
	}

	public void setFolderPath(String diskName, String folderName,
			String fileName)
	{
		folderPath = diskName + "://" + folderName + "/" + fileName + "-";
	}

	public String getFilePath()
	{
		return filePath;
	}

	public void start() throws RserveException
	{
		c = new RConnection();
	}

	public void end() throws RserveException
	{
		if(c.isConnected())
			c.close();
	}

	//读取json数据和加载库
	public void read(String jsonPath) throws RserveException
	{
		List list = new ArrayList();
		list.add("library(TSA)");
		list.add("library(rjson)");
		list.add("json_data<-fromJSON(paste(readLines('" + jsonPath
				+ "'), collapse=''))");
		list.add("source('D:/workspace/garch/test/timeseriesanalysis/ParseSpotScript.R')");

		for (int i = 0; i < list.size(); i++)
			c.eval(list.get(i).toString()); // 依次执行R命令
	}

	public void build()
	{
		filePath = folderPath + index++ + ".jpg";
		
		try
		{
			REXP length_f = c.eval("length(f)");
			int totallength = length_f.asInteger();// 向量长度

			int base = 100;
			int learnstep = 360;
			int arimalearnstep = 60;
			int prestep = 30;

			c.eval("base<-" + String.valueOf(base));
			c.eval("learnstep<-" + String.valueOf(learnstep));
			c.eval("prestep<-" + String.valueOf(prestep));
			c.eval("temp<-f[base:(base+learnstep)]");
			c.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");

			c.eval("garchmod=garch(x=temp,order=c(1,1))");
			c.eval("jpeg('" + filePath + "')");
			System.out.println(filePath);
			c.eval("plot(residuals(garchmod),type='h',ylab = 'Standard residual')");
			c.eval("dev.off()");
			// c.eval("");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}
}