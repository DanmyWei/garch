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
	private static double t_predict;
	private static double t_real;

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

	public double getPredict()
	{
		return t_predict;
	}

	public double getReal()
	{
		return t_real;
	}

	public void setPredict(double t)
	{
		t_predict = t;
	}

	public void setReal(double t)
	{
		t_real = t;
	}

	public void start() throws RserveException
	{
		if (c == null || !c.isConnected())
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
		if (c == null || !c.isConnected())
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
			int base = 100;// 拟合起点
			int learnstep = 360;// 拟合长度
			int prestep = 360;

			c.eval("base<-" + String.valueOf(base));
			c.eval("learnstep<-" + String.valueOf(learnstep));
			c.eval("flag<-base+learnstep");
			c.eval("prestep<-" + String.valueOf(prestep));
			c.eval("temp<-f[base:(base+learnstep)]");// 原始数据
			c.eval("d<-diff(log(temp))*100");// 差分
			c.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");// 观测值

			c.eval("m1=garch(x=d,order=c(" + p + "," + q + "))");// 拟合garch模型
			// c.eval("g1 = garchFit(formula=~garch(" + p + "," + q +
			// "),data=temp,trace=F,cond.dist='std')");
			c.eval("jpeg('" + filePath + "')");
			System.out.println("图像渲染成功 : " + filePath);
			c.eval("plot(residuals(m1),type='l',ylab = 'Standard residual')");// 异方差模型标准残差分布
			c.eval("abline(h=0)");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict(int p, int q)
	{
		filePath = folderPath + "Predict-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			t_predict = c.eval("m1$coef[1]/(1-m1$coef[2]-m1$coef[3])")
					.asDouble();// 预测平均方差
			t_real = c.eval("sd(diff(log(prepart))*100)^2").asDouble();// 实际方差
			t_predict = (double)(Math.round(t_predict*100)/100.0);
			t_real = (double)(Math.round(t_real*100)/100.0);
			System.out.println("t_predict=" + t_predict + ",t_real=" + t_real);
			c.eval("jpeg('" + filePath + "')");
			// c.eval("plot(predict(g1,n.ahead=prestep)$standardDeviation,type='b',ylab = 'standardDeviation')");
			// c.eval("abline(h=coef(predict(g1,n.ahead=prestep)$meanError))");
			c.eval("plot((fitted(m1)[,1])^2,type='l',ylab='Conditional Variance',xlab='t')");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_next(int p, int q)
	{
		filePath = folderPath + "Predict-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("flag<-flag+30");
			c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值
			t_real = c.eval("sd(diff(log(prepart))*100)^2").asDouble();// 实际方差
			c.eval("jpeg('" + filePath + "')");
			// c.eval("plot(predict(g1,n.ahead=prestep)$standardDeviation,type='b',ylab = 'standardDeviation')");
			// c.eval("abline(h=coef(predict(g1,n.ahead=prestep)$meanError))");
			c.eval("plot(" + t_real + ")");
			c.eval("abline(h=" + t_predict + ")");
			c.eval("dev.off()");
			t_real = (double)(Math.round(t_real*100)/100.0);
			System.out.println("t_predict=" + t_predict + ",t_real=" + t_real);
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void acf(int p, int q)
	{
		filePath = folderPath + "ACF-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("acf(d)");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void pacf(int p, int q)
	{
		filePath = folderPath + "PACF-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("pacf(d)");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void abs_acf(int p, int q)
	{
		filePath = folderPath + "ABS_ACF-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("acf(abs(d))");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void abs_pacf(int p, int q)
	{
		filePath = folderPath + "ABS_PACF-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("pacf(abs(d))");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void qq(int p, int q)
	{
		filePath = folderPath + "QQ-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("qqnorm(residuals(m1))");
			c.eval("qqline(residuals(m1))");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void res(int p, int q)
	{
		filePath = folderPath + "Standard-residual-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
	}
}