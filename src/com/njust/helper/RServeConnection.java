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
	private static String outputFilePath;
	private static String realDiffSD;
	private static RConnection c = null;
	private static String inputJsonName;
	private static double t_predict;
	private static double t_real;
	private static int base = 100;// 拟合起点
	private static int learnstep = 3600;// 拟合长度
	private static int prestep = 10;// 每30步计算一次方差
	private static double aic;

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
	
	public void setAIC(double t)
	{
		aic = t;
	}

	public double getAIC()
	{
		return (double) (Math.round(aic * 100) / 100.0);
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
			c.eval("base<-" + String.valueOf(base));
			c.eval("learnstep<-" + String.valueOf(learnstep));
			c.eval("flag<-base+learnstep");
			c.eval("prestep<-" + String.valueOf(prestep));
			c.eval("temp<-f[base:(base+learnstep)]");// 原始数据
			c.eval("d<-diff(log(temp))*100");// 差分
			c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值

			c.eval("m1=garch(x=d,order=c(" + p + "," + q + "))");// 拟合garch模型
			// c.eval("g1 = garchFit(formula=~garch(" + p + "," + q +
			// "),data=temp,trace=F,cond.dist='std')");
			c.eval("jpeg('" + filePath + "')");
			System.out.println("图像渲染成功 : " + filePath);
			c.eval("plot(residuals(m1),type='l',ylab = 'Standard residual')");// 异方差模型标准残差分布
			c.eval("abline(h=0)");
			c.eval("dev.off()");
			setAIC(c.eval("AIC(m1)").asDouble());
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
		outputFilePath = folderPath + "output/Predict-garch(" + p + "," + q
				+ ")-" + inputJsonName + ".json";
		try
		{
			// t_predict = c.eval("m1$coef[1]/(1-m1$coef[2]-m1$coef[3])")
			// .asDouble();// 预测平均方差
			// t_real = c.eval("sd(diff(log(prepart))*100)^2").asDouble();//
			// 实际方差
			// t_predict = (double) (Math.round(t_predict * 100) / 100.0);
			// t_real = (double) (Math.round(t_real * 100) / 100.0);
			// System.out.println("t_predict=" + t_predict + ",t_real=" +
			// t_real);
			c.eval("jpeg('" + filePath + "')");
			// c.eval("plot(predict(g1,n.ahead=prestep)$standardDeviation,type='b',ylab = 'standardDeviation')");
			// c.eval("abline(h=coef(predict(g1,n.ahead=prestep)$meanError))");
			c.eval("plot((fitted(m1)[,1])^2,type='l',ylab='Conditional Variance',xlab='t')");
			c.eval("dev.off()");
			ArrayList predict_list = new ArrayList();
			double[] data = c.eval("(fitted(m1)[,1])^2").asDoubles();
			for (int i = 0; i < data.length; i++)
				predict_list.add(new Double(data[i]));
			JsonFileHelper predict_json = new JsonFileHelper();
			if (predict_json.SavetoJson(predict_list, outputFilePath))
				System.out.println("预测数据写入成功 : " + outputFilePath);
			else
				System.out.println("预测数据写入失败 : " + outputFilePath);
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_real(int p, int q)
	{
		filePath = folderPath + "real/realDiff-" + inputJsonName + ".jpg";
		realDiffSD = folderPath + "real/realDiff-" + inputJsonName + ".json";
		int max = learnstep / prestep;
		ArrayList real_list = new ArrayList();
		try
		{
			c.eval("flag<-base+learnstep");
			for (int i = 0; i < max; i++)
			{
				c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值
				t_real = c.eval("sd(diff(log(prepart))*100)^2").asDouble();// 实际方差
				real_list.add(new Double(t_real));
				c.eval("flag<-flag+prestep");
			}
			JsonFileHelper real_json = new JsonFileHelper();
			if (real_json.SavetoJson(real_list, realDiffSD))
				System.out.println("预测数据写入成功 : " + realDiffSD);
			else
				System.out.println("预测数据写入失败 : " + realDiffSD);
			c.eval("json_real<-fromJSON(paste(readLines('" + realDiffSD
					+ "'), collapse=''))");
			c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real.R')");
			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rf,type='l',ylab='Conditional Variance',xlab='t/10')");
			c.eval("dev.off()");
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
			c.eval("acf(residuals(m1)^2,na.action=na.omit)");
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
			c.eval("pacf(residuals(m1)^2,na.action=na.omit)");
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
			c.eval("acf(abs(residuals(m1)),na.action=na.omit)");
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void gbox(int p, int q)
	{
		filePath = folderPath + "gBox-garch(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("gBox(m1,method='squared')");
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