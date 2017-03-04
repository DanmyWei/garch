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
	private static String predictPath;
	private static double t_predict;
	private static double t_long;
	private static int base = 100;// 拟合起点
	private static int learnstep = 360;// 拟合长度
	private static int prestep = 10;// 取样间隔
	private static double aic;
	private static ArrayList predict_list;// 向前预测序列
	private static ArrayList real_list;
	private static ArrayList real2_list;

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
		// return (double) (Math.round(t_predict * 100) / 100.0);
		return t_predict;
	}

	public double getLong()
	{
		// return (double) (Math.round(t_long * 100) / 100.0);
		return t_long;
	}

	public void setPredict(double t)
	{
		t_predict = t;
	}

	public void setLong(double t)
	{
		t_long = t;
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
		{
			c = new RConnection();
			System.out.println("RServer连接成功");
		}
	}

	public void end() throws RserveException
	{
		if (c.isConnected())
		{
			c.close();
			System.out.println("已关闭RServer连接");
		}
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
		predict_list = new ArrayList();
		real_list = new ArrayList();
		real2_list = new ArrayList();
		try
		{
			c.eval("base<-" + String.valueOf(base));
			c.eval("learnstep<-" + String.valueOf(learnstep));
			c.eval("flag<-base+learnstep");
			c.eval("prestep<-" + String.valueOf(prestep));
			c.eval("temp<-f[base:(base+learnstep)]");// 原始数据
			c.eval("d<-diff(log(temp))*100");// 差分
			c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值
			c.eval("presteppart<-f[(flag+1):(flag+learnstep)]");// 步长级观测值

			c.eval("m1=garch(x=d,order=c(" + p + "," + q + "))");// 拟合garch模型
			// c.eval("g1 = garchFit(formula=~garch(" + p + "," + q +
			// "),data=temp,trace=F,cond.dist='std')");
			c.eval("jpeg('" + filePath + "')");
			System.out.println("图像渲染成功 : " + filePath);
			c.eval("plot(residuals(m1),type='l',ylab = 'Standard residual')");// 异方差模型标准残差分布
			c.eval("abline(h=0)");
			c.eval("dev.off()");
			setAIC(c.eval("AIC(m1)").asDouble());
			setLong(c.eval("m1$coef[1]/(1-m1$coef[2]-m1$coef[3])").asDouble());
			System.out.println("拟合长期方差值=" + getLong());
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict(int p, int q, int k)
	{
		double value;
		filePath = folderPath + "predict/predict-" + inputJsonName + ".jpg";
		predictPath = folderPath + "predict/predict-" + inputJsonName + ".json";
		realDiffSD = folderPath + "real/realPredict-" + inputJsonName + ".json";
		try
		{
			if (k <= 1)
			{
				predict_list.clear();
				c.eval("p = m1$coef[1] + m1$coef[2] * d[learnstep]^2 + m1$coef[3] * sd(d)^2");
				value = c.eval("p").asDouble();
				predict_list.add(new Double(value));
				setPredict(value);
			} else
			{
				c.eval("p = m1$coef[1] + m1$coef[2] * p + m1$coef[3] * p");
				value = c.eval("p").asDouble();
				predict_list.add(new Double(value));
				setPredict(value);
			}
			JsonFileHelper predict_json = new JsonFileHelper();
			JsonFileHelper real_json = new JsonFileHelper();
			double t_real;
			real2_list.clear();
			if (predict_json.SavetoJson(predict_list, predictPath, "pd"))
			{
				System.out.println(k + "步预测数据写入成功 : " + predictPath);

				c.eval("flag2<-base+learnstep");
				for (int i = 0; i < k; i++)
				{
					c.eval("presteppart<-f[(flag2+1):(flag2+learnstep)]");// 步长级观测值
					t_real = c.eval("sd(diff(log(presteppart))*100)^2")
							.asDouble();// 方差
					real2_list.add(new Double(t_real));
					c.eval("flag2<-flag2+learnstep");
				}
				real_json.SavetoJson(real2_list, realDiffSD, "sd");

				c.eval("json_predict<-fromJSON(paste(readLines('" + predictPath
						+ "'), collapse=''))");
				c.eval("json_real<-fromJSON(paste(readLines('" + realDiffSD
						+ "'), collapse=''))");
				c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real2.R')");
				c.eval("source('D:/workspace/garch/test/timeseriesanalysis/predict.R')");
				c.eval("h<-max(max(as.double(pf)),max(as.double(rf2)))");
				c.eval("jpeg('" + filePath + "')");
				c.eval("plot(pf,type='o',col='red',ylim=c(0,h))");
				c.eval("par(new=TRUE)");
				c.eval("plot(rf2,type='o',col='green',axes = FALSE,ylim=c(0,h))");
				c.eval("dev.off()");
			} else
				System.out.println(k + "步预测数据写入失败 : " + predictPath);

		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_real(int p, int q)
	{
		filePath = folderPath + "real/realDiff-" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		realDiffSD = folderPath + "real/realDiff-" + p + "," + q + ")-"
				+ inputJsonName + ".json";
		outputFilePath = folderPath + "output/Predict-garch(" + p + "," + q
				+ ")-" + inputJsonName + ".json";
		real_list.clear();
		try
		{
			// // 原来的逻辑,按取样区间计算方差
			// int max = learnstep / prestep;
			// c.eval("flag<-base+learnstep");
			// for (int i = 0; i < max; i++)
			// {
			// c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值
			// t_real = c.eval("sd(diff(log(prepart))*100)^2").asDouble();//
			// 实际方差
			// real_list.add(new Double(t_real));
			// c.eval("flag<-flag+prestep");
			// }

			// 取收益率平方作为方差
			c.eval("ftemp<-f[(base+learnstep):(base+learnstep+learnstep)]");// 原始数据
			c.eval("fd<-diff(log(ftemp))*100");// 差分
			double[] d_real_list = c.eval("fd^2").asDoubles();// 差分
			for (int i = 0; i < d_real_list.length; i++)
				real_list.add(new Double(d_real_list[i]));
			JsonFileHelper real_json = new JsonFileHelper();
			if (real_json.SavetoJson(real_list, realDiffSD, "sd"))
				System.out.println("观测数据写入成功 : " + realDiffSD);
			else
				System.out.println("观测数据写入失败 : " + realDiffSD);
			c.eval("json_real<-fromJSON(paste(readLines('" + realDiffSD
					+ "'), collapse=''))");
			c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real.R')");
			c.eval("fm1<-(fitted(m1)[,1])^2");
			c.eval("h<-max(max(as.double(rf)),max(fm1[2:learnstep]))");
			

			ArrayList predict_list = new ArrayList();
			double[] data = c.eval("(fitted(m1)[,1])^2").asDoubles();
			for (int i = 0; i < data.length; i++)
				predict_list.add(new Double(data[i]));
			JsonFileHelper predict_json = new JsonFileHelper();
			if (predict_json.SavetoJson(predict_list, outputFilePath, "sd"))
				System.out.println("预测数据写入成功 : " + outputFilePath);
			else
				System.out.println("预测数据写入失败 : " + outputFilePath);
			
			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rf,type='l',col='green',ylab='Conditional Variance',ylim=c(0,h),xlab='t')");//DEBUG
			c.eval("par(new=TRUE)");
			c.eval("plot(fm1,col='red',type='l',ylab='Conditional Variance',ylim=c(0,h),axes = FALSE,xlab='t')");// 拟合值
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