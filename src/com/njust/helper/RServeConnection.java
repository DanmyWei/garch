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
	private static String realDiffVar;
	private static String MAPEPATH;
	private static String MAPEPICPATH;
	private static RConnection c = null;
	private static String inputJsonName;
	private static String predictPath;
	private static double t_predict;
	private static double t_long;
	
	private static int base = 100;// 拟合起点
	private static int n = 0;
	
	private static int learnstep = 100;// 拟合长度
	private static int prestep = 5;// 取样间隔
	private static int lenth_mape = 5;// MAPE间隔
	private static double aic;
	private static ArrayList predict_list;// 向前预测序列
	private static ArrayList real_list;
	private static ArrayList real2_list;
	private static ArrayList real_var_list;

	public RServeConnection()
	{
		index = 1;
		folderPath = "D://R-Data/" + inputJsonName + "-"; // 默认写入D盘R-Data文件夹
	}
	
	public RServeConnection(int b,int nb)
	{
		base = b;
		n = nb;
		index = 1;
		folderPath = "D://R-Data/" + inputJsonName + "-"; // 默认写入D盘R-Data文件夹
	}
	
	public RServeConnection(int nb)
	{
		n = nb;
		if(n==0)
			base = 100;
		else if(n==1)
			base = 140;
		else if(n==2)
			base = 170;
		else if(n==3)
			base = 200;
		else if(n==4)
			base = 230;
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
		list.add("library(forecast)");
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
		real_var_list = new ArrayList();
		index = 0;
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
			c.eval("g1 = garchFit(formula=~garch(" + p + "," + q
					+ "),data=d,trace=F,cond.dist='std')");
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
					t_real = c.eval("var(diff(log(presteppart))*100)")
							.asDouble();// 方差
					if (t_real != t_real)
					{
						t_real = 0;
						System.out.println("数据越界,超出观测范围!");
					}
					real2_list.add(new Double(t_real));
					c.eval("flag2<-flag2+learnstep");
					// c.eval("flag2<-flag2+1");
				}
				real_json.SavetoJson(real2_list, realDiffSD, "sd");

				c.eval("json_predict<-fromJSON(paste(readLines('" + predictPath
						+ "'), collapse=''))");
				c.eval("json_real<-fromJSON(paste(readLines('" + realDiffSD
						+ "'), collapse=''))");
				c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real2.R')");
				c.eval("source('D:/workspace/garch/test/timeseriesanalysis/predict.R')");
				c.eval("h2<-max(max(as.double(pf)),max(as.double(rf2)))");
				c.eval("low<-min(min(as.double(pf)),min(as.double(rf2)))");
				c.eval("jpeg('" + filePath + "')");
				c.eval("plot(pf,type='o',col='red',ylim=c(low*0.5,h2*1.1))");
				c.eval("par(new=TRUE)");
				c.eval("plot(rf2,type='o',col='green',axes = FALSE,ylim=c(low*0.5,h2*1.1))");
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
		switch (index)
		{
		case 0:
			predict_real_start(p, q);
			index = 2;
			break;
		case 1:
			predict_real_run(p, q);
			index++;
			break;
		case 2:
			predict_real_log(p, q);
			index++;
			break;
		case 3:
			predict_real_var(p, q);
			index = 1;
			break;
		default:
			System.out.println("数据非法,index=" + index);
		}
	}

	public void predict_real_run(int p, int q)
	{
		filePath = folderPath + "real/realDiff-(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rf,type='l',col='green',ylab='Conditional Variance',ylim=c(0,h),xlab='t')");// DEBUG,纵轴无穷大导致
			c.eval("par(new=TRUE)");
			c.eval("plot(rfv,type='l',col='blue',ylab='Conditional Variance',ylim=c(0,h),axes = FALSE,xlab='t')");
			c.eval("par(new=TRUE)");
			c.eval("plot(pm1,col='red',type='l',ylab='Conditional Variance',ylim=c(0,h),xlab='t',main=MAPE)");// 拟合值
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_real_var(int p, int q)
	{
		filePath = folderPath + "real/realDiff-(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rfv,type='l',col='blue',ylab='Conditional Variance',ylim=c(0,h),axes = FALSE,xlab='t')");
			c.eval("par(new=TRUE)");
			c.eval("plot(pm1,col='red',type='l',ylab='Conditional Variance',ylim=c(0,h),xlab='t',main=MAPE_2)");// 拟合值
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_real_log(int p, int q)
	{
		filePath = folderPath + "real/realDiff-(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		try
		{
			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rf,type='l',col='green',ylab='Conditional Variance',ylim=c(0,h),xlab='t')");// DEBUG,纵轴无穷大导致
			c.eval("par(new=TRUE)");
			c.eval("plot(pm1,col='red',type='l',ylab='Conditional Variance',ylim=c(0,h),xlab='t',main=MAPE_1)");// 拟合值
			c.eval("dev.off()");
		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void predict_real_start(int p, int q)
	{
		filePath = folderPath + "real/realDiff-(" + p + "," + q + ")-"
				+ inputJsonName + ".jpg";
		realDiffSD = folderPath + "real/realDiff-" + inputJsonName + ".json";
		realDiffVar = folderPath + "real/realDiffVar-" + inputJsonName
				+ ".json";
		outputFilePath = folderPath + "output/Predict-garch(" + p + "," + q
				+ ")-" + inputJsonName + ".json";
		MAPEPATH = folderPath + "MAPE/MAPE-" + inputJsonName + ".json";
		MAPEPICPATH = folderPath + "MAPE/MAPE-" + inputJsonName + ".jpg";
		real_list.clear();
		real_var_list.clear();
		try
		{

			// 原来的逻辑,按取样区间计算平均方差 (蓝色)
			int max = learnstep / prestep;
			double var_real;
			// c.eval("flag<-base+learnstep");
			c.eval("flag<-base");
			for (int i = 0; i < max; i++)
			{
				c.eval("prepart<-f[(flag+1):(flag+prestep)]");// 观测值
				var_real = c.eval("var(diff(log(prepart))*100)").asDouble();// 实际方差
				real_var_list.add(new Double(var_real));
				c.eval("flag<-flag+prestep");
			}

			JsonFileHelper real_var_json = new JsonFileHelper();
			if (real_var_json.SavetoJson(real_var_list, realDiffVar, "sd"))
				System.out.println("平均方差写入成功 : " + realDiffVar);
			else
				System.out.println("平均方差写入失败 : " + realDiffVar);

			// 取收益率绝对值作为瞬时方差近似值(绿色)
			c.eval("ftemp<-f[(base+learnstep):(base+learnstep+learnstep+1)]");// 原始数据
			c.eval("fd<-diff(log(temp)*100)");// 差分
			double tr;
			double[] d_real_list = c.eval("log(temp)*100").asDoubles();// 差分

			for (int i = 0; i < d_real_list.length; i++)
				if (d_real_list[i] >= Double.MAX_VALUE
						|| d_real_list[i] <= -Double.MAX_VALUE)
					d_real_list[i] = 0;
				else if (d_real_list[i] != d_real_list[i])
					d_real_list[i] = 0;

			for (int i = 0; i < learnstep; i++)
			{
				tr = d_real_list[i + 1] - d_real_list[i];
				tr = tr > 0 ? tr : -tr;
				real_list.add(new Double(tr));
			}

			JsonFileHelper real_json = new JsonFileHelper();
			if (real_json.SavetoJson(real_list, realDiffSD, "sd"))
				System.out.println("观测数据写入成功 : " + realDiffSD);
			else
				System.out.println("观测数据写入失败 : " + realDiffSD);
			c.eval("json_real<-fromJSON(paste(readLines('" + realDiffSD
					+ "'), collapse=''))");
			c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real.R')");

			c.eval("json_var_real<-fromJSON(paste(readLines('" + realDiffVar
					+ "'), collapse=''))");
			c.eval("source('D:/workspace/garch/test/timeseriesanalysis/real_var.R')");

			// c.eval("fm1<-(fitted(m1)[,1])^2");
			// c.eval("fm1<-(predict(m1,d)[,1])^2");
			c.eval("fm1<-(predict(m1)[,1])^2");
			c.eval("fm1[1]<-fm1[2]");
			c.eval("rf<-as.double(rf)");
			c.eval("rfv<-as.double(rfv)");
			c.eval("h<-max(max(rf),max(fm1))");

			// 纵轴为无穷大时,直接设置为1
			if (c.eval("h").asDouble() >= Double.MAX_VALUE
					|| c.eval("h").asDouble() <= -Double.MAX_VALUE
					|| c.eval("h").asDouble() != c.eval("h").asDouble())
				c.eval("h<-1");

			ArrayList predict_list = new ArrayList();
			double[] data = c.eval("fm1").asDoubles();
			for (int i = 0; i < data.length; i++)
				predict_list.add(new Double(data[i]));
			JsonFileHelper predict_json = new JsonFileHelper();
			if (predict_json.SavetoJson(predict_list, outputFilePath, "sd"))
				System.out.println("预测数据写入成功 : " + outputFilePath);
			else
				System.out.println("预测数据写入失败 : " + outputFilePath);

			// c.eval("pm1<-fm1 - min(fm1) + min(rf)");
			// c.eval("pm1<-fm1");
			c.eval("pm1<-fm1 - min(fm1) + min(rf)");
			// c.eval("fm1<-fm1 - min(fm1) + min(rf)");

			c.eval("MAPE_1<-accuracy(rf + min(fm1) - min(rf),fm1)[5]")
					.asDouble();
			c.eval("MAPE_2<-accuracy(rfv + min(fm1) - min(rf),fm1)[5]")
					.asDouble();
			double MAPE_1 = c.eval("MAPE_1").asDouble();
			double MAPE_2 = c.eval("MAPE_2").asDouble();

			c.eval("MAPE<-min(as.double(MAPE_1),as.double(MAPE_2))");

			c.eval("rf<-rf[length(rf):1]");
			c.eval("rfv<-rfv[length(rfv):1]");
			c.eval("pm1<-pm1[length(pm1):1]");

			c.eval("jpeg('" + filePath + "')");
			c.eval("plot(rf,type='l',col='green',ylab='Conditional Variance',ylim=c(0,h),xlab='t')");// DEBUG,纵轴无穷大导致
			c.eval("par(new=TRUE)");
			c.eval("plot(rfv,type='l',col='blue',ylab='Conditional Variance',ylim=c(0,h),axes = FALSE,xlab='t')");
			c.eval("par(new=TRUE)");
			c.eval("plot(pm1,col='red',type='l',ylab='Conditional Variance',ylim=c(0,h),xlab='t',main=MAPE)");// 拟合值
			c.eval("dev.off()");

			System.out.println("MAPE_1=" + MAPE_1);
			System.out.println("MAPE_2=" + MAPE_2);

			ArrayList mape_list = new ArrayList();
			int mape_length = learnstep / lenth_mape;
			c.eval("mape_length<-" + mape_length);
			for (int i = 0; i < mape_length; i++)
			{
				double mape = c.eval(
						"accuracy((rf+ min(fm1) - min(rf))["
								+ (i * lenth_mape + 1) + ":"
								+ (i * lenth_mape + lenth_mape) + "],fm1["
								+ (i * lenth_mape + 1) + ":"
								+ (i * lenth_mape + lenth_mape) + "])[5]")
						.asDouble();
				mape_list.add(new Double(mape));
			}
			JsonFileHelper mape_json = new JsonFileHelper();
			if (mape_json.SavetoJson(mape_list, MAPEPATH, "MAPE"))
				System.out.println("MAPE数据写入成功 : " + MAPEPATH);
			else
				System.out.println("MAPE数据写入失败 : " + MAPEPATH);
			c.eval("json_mape<-fromJSON(paste(readLines('" + MAPEPATH
					+ "'), collapse=''))");
			c.eval("source('D:/workspace/garch/test/timeseriesanalysis/mape.R')");
			c.eval("rmape<-as.double(rmape)");

			c.eval("jpeg('" + MAPEPICPATH + "')");
			c.eval("plot(sort(rmape),type='b',col='green',ylim=c(0,max(150,max(rmape)*1.1)),ylab='MAPE (%)',xlab='prestep')");
			c.eval("abline(h=100)");
			c.eval("dev.off()");

			// 大数据处理
			c.eval("result<-data.frame(Alname='GARCH',vmtype='" + inputJsonName
					+ "',time="+n+",prestep=c(1:mape_length),MAPE=sort(rmape))");
			c.eval("write.csv(result,'D:/R-Data/result/result-" + inputJsonName +n+".csv')");

		} catch (Exception exception)
		{
			System.out.println(exception.toString());
			exception.printStackTrace();
		}
	}

	public void mape_pic()
	{
		filePath = MAPEPICPATH;
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