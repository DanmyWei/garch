package com.njust.helper;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RServeConnection
{
	private static int index;
	private static String folderPath;
	private static String filePath;
	private static RConnection c = null;
	private static String inputJsonName;

	public RServeConnection()
	{
		index = 1;
		folderPath = "D://R-Data/" + inputJsonName + "-"; // 默认写入D盘R-Data文件夹
	}

	public void setFolderPath(String diskName, String folderName)
	{
		folderPath = diskName + "://" + folderName + "/";
	}

	public String getFilePath()
	{
		return filePath;
	}

	public void start() throws RserveException
	{
		if(c == null || !c.isConnected())
			c = new RConnection();
	}

	public void end() throws RserveException
	{
		if (c.isConnected())
			c.close();
	}

	// 读取数据和加载库
	public void read(String jsonPath, String jsonName) throws RserveException
	{
		if(c == null || !c.isConnected())
			c = new RConnection();
		inputJsonName = jsonName;
		List list = new ArrayList();
		list.add("library(TSA)");
		list.add("library(rjson)");
		list.add("library(fGarch)");
		list.add("json_data<-fromJSON(paste(readLines('" + jsonPath
				+ "'), collapse=''))");
		list.add("source('D:/workspace/garch/test/timeseriesanalysis/ParseSpotScript.R')");

		for (int i = 0; i < list.size(); i++)
			c.eval(list.get(i).toString()); // 依次执行R命令
	}

	public void build(int p, int q)
	{
		filePath = folderPath + "Standard-residual-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";

		try
		{
			int base = 100;//拟合起点
			int learnstep = 360;//拟合长度
			int prestep = 30;//每半分钟比较一次

			c.eval("base<-" + String.valueOf(base));
			c.eval("learnstep<-" + String.valueOf(learnstep));
			c.eval("prestep<-" + String.valueOf(prestep));
			c.eval("temp<-f[base:(base+learnstep)]");//原始数据
			c.eval("d<-diff(log(temp))*100");//差分
			c.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");//观测值

			c.eval("m1=garch(x=d,order=c(" + p + "," + q + "))");//拟合garch模型
//			c.eval("g1 = garchFit(formula=~garch(" + p + "," + q + "),data=temp,trace=F,cond.dist='std')");
			c.eval("jpeg('" + filePath + "')");
			System.out.println("图像渲染成功 : "+filePath);
			c.eval("plot(residuals(m1),type='l',ylab = 'Standard residual')");//异方差模型标准残差分布
			c.eval("abline(h=0)");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}
	
	public void predict(int p,int q)
	{
		filePath = folderPath + "Predict-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			REXP length_f = c.eval("length(f)");
			int totallength = length_f.asInteger();// 向量长度
			c.eval("jpeg('" + filePath + "')");
//			c.eval("plot(predict(g1,n.ahead=prestep)$standardDeviation,type='b',ylab = 'standardDeviation')");
//			c.eval("abline(h=coef(predict(g1,n.ahead=prestep)$meanError))");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}
}