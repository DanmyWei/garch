package com.njust.helper;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

public class TimeSeriesAnalysis
{
	public TimeSeriesAnalysis()
	{

	}

	public static void ParseOneFile_Example(RConnection re, String datafile)
	{
		try
		{
			re.eval("library(rjson)");
			re.eval("library(MSwM)");
			re.eval("json_data<-fromJSON(paste(readLines('" + datafile
					+ "'), collapse=''))");
			re.eval("source('D:/workspace/garch/test/timeseriesanalysis/ParseSpotScript.R')");

			REXP length_f = re.eval("length(f)");
			int totallength = length_f.asInteger();
			int base = 100;
			int learnstep = 360;
			int arimalearnstep = 60;
			int prestep = 30;
			ArrayList arima_errorlist = new ArrayList();
			ArrayList markov_errorlist = new ArrayList();
			ArrayList arma_errorlist = new ArrayList();
			re.eval("mod.mswm=NULL");
			re.eval("mod=NULL");
			re.eval("residual=NULL");
			re.eval("ar1ma10=NULL");
			re.eval("a=NULL");
			re.eval("predicted=NULL");
			re.eval("curprob=NULL");
			re.eval("ar1maorigi=NULL");
			re.eval("b=NULL");

			re.eval("base<-" + String.valueOf(base));
			re.eval("learnstep<-" + String.valueOf(learnstep));
			re.eval("prestep<-" + String.valueOf(prestep));
			re.eval("temp<-f[base:(base+learnstep)]");
			re.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");
			// re.eval("plot(temp,type='b')");
			re.eval("mod=lm(temp~1)");
			int k = 5;
			for (; k > 1; k--)
			{
				re.eval("k<-" + String.valueOf(k));
				System.out.println("k=" + k);
				REXP x = re.eval("mod.mswm=msmFit(mod,k,p=0,sw=c(T,T),control=list(parallel=F))");
				System.out.println(x.asString());
				REXP mswm = re.eval("mod.mswm");
				if (!mswm.isNull())
				{
					System.out.println("Model established");
					break;
				} else
				{
					System.out.println("Test state failed number=" + k);
				}

			}
			if (k <= 1)
			{
				//re.eval("plot(temp,type='b')");
				System.out.println("无法建模");
				return;
			}
			re.eval("residual=msmResid(mod.mswm)");
			re.eval("ar1ma10=arima(residual,order=c(2,1,0))");
			re.eval("jpeg('d://Residual_predict.jpg')");
			re.eval("a=plot(ar1ma10,n.ahead=prestep,type='b',xlab='time',ylab='price')");
			re.eval("dev.off()");
			Process process0 = Runtime.getRuntime().exec(
					"open d://Residual_predict.jpg");
			re.eval("predicted=" + String.valueOf(prestep));
			System.out.println(re.eval("summary(mod.mswm)"));
			System.out.println(re.eval("mod.mswm@Coef"));

			re.eval("curprob=mod.mswm@Fit@filtProb[dim(mod.mswm@Fit@filtProb)[1],]");
			for (int p = 1; p <= prestep; p++)
			{
				re.eval("curprob=curprob%*%mod.mswm@transMat");
				REXP prob = re.eval("curprob");
				System.out.println(prob);
				double[] probarray = prob.asDoubles();
				int curstate = 0;
				double maxprob = 0;
				for (int i = 1; i <= probarray.length; i++)
				{
					double curprob = probarray[i - 1];
					if (curprob > maxprob)
					{
						maxprob = curprob;
						curstate = i;
					}
				}
				System.out.println("State:" + curstate);
				re.eval("curstate=" + String.valueOf(curstate));
				re.eval("p=" + String.valueOf(p));
				re.eval("predicted[p]=mod.mswm@Coef["
						+ String.valueOf(curstate) + ",1]+a$pred[p]");

			}

			System.out.println(re.eval("predicted"));
			System.out.println(re.eval("prepart"));
			re.eval("jpeg('d://Markove.jpg')");

			re.eval("plot(c(temp,prepart),type='b')");

			re.eval("par(new=TRUE)");
			re.eval("points((learnstep+2):(learnstep+1+prestep),predicted,col='red',type='b')");
			re.eval("dev.off()");
			Process process = Runtime.getRuntime().exec("open d://Markove.jpg");
			re.eval("eacf(temp)");
			re.eval("ar1maorigi=arima(temp,order=c(2,1,0))");
			re.eval("jpeg('d://ARIMA.jpg')");
			re.eval("b=plot(ar1maorigi,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
			re.eval("plot(c(temp,prepart),type='b')");

			re.eval("points((learnstep+2):(learnstep+1+prestep),b$pred,col='red',type='b')");
			re.eval("dev.off()");
			Process process2 = Runtime.getRuntime().exec("open d://ARIMA.jpg");

			re.eval("armamodel=arima(temp,order=c(2,0,0))");
			re.eval("jpeg('d://arma.jpg')");
			re.eval("armaplot=plot(armamodel,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
			re.eval("plot(c(temp,prepart),type='b')");

			re.eval("points((learnstep+2):(learnstep+1+prestep),armaplot$pred,col='red',type='b')");
			re.eval("dev.off()");
			Process process3 = Runtime.getRuntime().exec("open d://arma.jpg");

			REXP actualprepart = re.eval("prepart");
			double[] actuaarray = actualprepart.asDoubles();

			REXP arimaprepart = re.eval("b$pred");
			double[] arimaprepartarray = arimaprepart.asDoubles();

			REXP armaprepart = re.eval("armaplot$pred");
			double[] armaprepartarray = armaprepart.asDoubles();

			REXP markoveprepart = re.eval("predicted");
			double[] markovprepartarray = markoveprepart.asDoubles();

			if (actuaarray.length == arimaprepartarray.length
					&& arimaprepartarray.length == markovprepartarray.length)
			{
				double arimatotaldeviation = 0, arima_averageerror = 0;
				for (int i = 0; i < actuaarray.length; i++)
				{
					arimatotaldeviation = arimatotaldeviation
							+ (Math.abs(arimaprepartarray[i] - actuaarray[i]))
							/ actuaarray[i];
				}
				arima_averageerror = arimatotaldeviation / actuaarray.length;
				System.out.println("arima_averageerror=" + arima_averageerror);
				double markovtotaldeviation = 0, markove_averageerror = 0;
				for (int i = 0; i < actuaarray.length; i++)
				{
					markovtotaldeviation = markovtotaldeviation
							+ (Math.abs(markovprepartarray[i] - actuaarray[i]))
							/ actuaarray[i];
				}
				markove_averageerror = markovtotaldeviation / actuaarray.length;
				System.out.println("markove_averageerror="
						+ markove_averageerror);

				double armatotaldeviation = 0, arma_averageerror = 0;
				for (int i = 0; i < actuaarray.length; i++)
				{
					armatotaldeviation = armatotaldeviation
							+ (Math.abs(armaprepartarray[i] - actuaarray[i]))
							/ actuaarray[i];
				}
				arma_averageerror = armatotaldeviation / actuaarray.length;

				System.out.println("arma_averageerror=" + arma_averageerror);
				arima_errorlist.add(new Double(arima_averageerror));
				markov_errorlist.add(new Double(markove_averageerror));
				arma_errorlist.add(new Double(arma_averageerror));
			} else
			{
				System.out
						.println("The length of predicted results of algorithms are different");
				return;
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public static void ParseOneFile(RConnection re, String datafile,
			String datadir, String picdir)
	{
		File inputfile = new File(datafile);
		String outputfilename = picdir + "/"
				+ inputfile.getName().replace(".json", "_PredictError")
				+ ".json";
		File outputfile = new File(outputfilename);
		if (outputfile.exists())
		{
			System.out.println("跳过：" + outputfile);
			return;
		}
		String strourputdir = picdir + "/"
				+ inputfile.getName().replace(".json", "");
		File outputdir = new File(strourputdir);

		if (!outputdir.exists())
		{

			try
			{
				outputdir.mkdirs();
			} catch (Exception e)
			{
				e.printStackTrace();
			}

		}
		try
		{
			datafile = datafile.replace("\\", "/");
			re.eval("library(rjson)");
			re.eval("library(MSwM)");
			re.eval("library(TSA)");
			re.eval("json_data<-fromJSON(paste(readLines('" + datafile
					+ "'), collapse=''))");
			re.eval("source('" + datadir + "/ParseSpotScript.R')");

			REXP length_f = re.eval("length(f)");
			int totallength = length_f.asInteger();
			int base = 100;
			int learnstep = 360;
			int arimalearnstep = 60;
			int prestep = 30;
			ArrayList arima_errorlist = new ArrayList();
			ArrayList markov_errorlist = new ArrayList();
			ArrayList arma_errorlist = new ArrayList();
			for (; base < totallength - learnstep - prestep; base = base
					+ learnstep)
			{

				re.eval("mod.mswm=NULL");
				re.eval("mod=NULL");
				re.eval("residual=NULL");
				re.eval("ar1ma10=NULL");
				re.eval("a=NULL");
				re.eval("predicted=NULL");
				re.eval("curprob=NULL");
				re.eval("ar1maorigi=NULL");
				re.eval("b=NULL");

				re.eval("base<-" + String.valueOf(base));
				re.eval("learnstep<-" + String.valueOf(learnstep));
				re.eval("prestep<-" + String.valueOf(prestep));
				re.eval("temp<-f[base:(base+learnstep)]");
				re.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");
				// re.eval("plot(temp,type='b')");
				re.eval("mod=lm(temp~1)");
				int k = 5;
				for (; k > 1; k--)
				{
					re.eval("k<-" + String.valueOf(k));
					re.eval("mod.mswm=msmFit(mod,k,p=0,sw=c(T,T),control=list(parallel=F))");
					REXP mswm = re.eval("mod.mswm");
					if (!mswm.isNull())
					{
						System.out.println("Model established");
						break;
					} else
					{
						System.out.println("Test state failed number=" + k);
					}

				}
				if (k <= 1)
				{
					// re.eval("plot(temp,type='b')");
					System.out.println("无法建模,base=" + base);
					continue;
				}
				re.eval("residual=msmResid(mod.mswm)");
				re.eval("ar1ma10=arima(residual,order=c(2,1,0))");
				re.eval("a=plot(ar1ma10,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");

				re.eval("predicted=" + String.valueOf(prestep));
				System.out.println(re.eval("summary(mod.mswm)"));
				System.out.println(re.eval("mod.mswm@Coef"));
				re.eval("curprob=mod.mswm@Fit@filtProb[dim(mod.mswm@Fit@filtProb)[1],]");
				for (int p = 1; p <= prestep; p++)
				{
					re.eval("curprob=curprob%*%mod.mswm@transMat");
					REXP prob = re.eval("curprob");
					System.out.println(prob);
					double[] probarray = prob.asDoubles();
					int curstate = 0;
					double maxprob = 0;
					for (int i = 1; i <= probarray.length; i++)
					{
						double curprob = probarray[i - 1];
						if (curprob > maxprob)
						{
							maxprob = curprob;
							curstate = i;
						}
					}
					System.out.println("State:" + curstate);
					re.eval("curstate=" + String.valueOf(curstate));
					re.eval("p=" + String.valueOf(p));
					re.eval("predicted[p]=mod.mswm@Coef["
							+ String.valueOf(curstate) + ",1]+a$pred[p]");

				}

				System.out.println(re.eval("predicted"));
				System.out.println(re.eval("prepart"));
				re.eval("jpeg('" + strourputdir + "//Markove_"
						+ String.valueOf(base) + ".jpg')");

				re.eval("plot(c(temp,prepart),type='b')");

				re.eval("par(new=TRUE)");
				re.eval("points((learnstep+2):(learnstep+1+prestep),predicted,col='red',type='b')");
				re.eval("dev.off()");

				re.eval("ar1maorigi=arima(temp,order=c(2,1,0))");
				re.eval("jpeg('" + strourputdir + "//ARIMA_"
						+ String.valueOf(base) + ".jpg')");
				re.eval("b=plot(ar1maorigi,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
				re.eval("plot(c(temp,prepart),type='b')");

				re.eval("points((learnstep+2):(learnstep+1+prestep),b$pred,col='red',type='b')");
				re.eval("dev.off()");

				re.eval("armamodel=arima(temp,order=c(2,0,0))");
				re.eval("jpeg('" + strourputdir + "//ARMA_"
						+ String.valueOf(base) + ".jpg')");
				re.eval("armaplot=plot(armamodel,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
				re.eval("plot(c(temp,prepart),type='b')");

				re.eval("points((learnstep+2):(learnstep+1+prestep),armaplot$pred,col='red',type='b')");
				re.eval("dev.off()");

				REXP actualprepart = re.eval("prepart");
				double[] actuaarray = actualprepart.asDoubles();

				REXP arimaprepart = re.eval("b$pred");
				double[] arimaprepartarray = arimaprepart.asDoubles();

				REXP armaprepart = re.eval("armaplot$pred");
				double[] armaprepartarray = armaprepart.asDoubles();

				REXP markoveprepart = re.eval("predicted");
				double[] markovprepartarray = markoveprepart.asDoubles();

				if (actuaarray.length == arimaprepartarray.length
						&& arimaprepartarray.length == markovprepartarray.length
						&& markovprepartarray.length == armaprepartarray.length)
				{
					double arimatotaldeviation = 0, arima_averageerror = 0;
					for (int i = 0; i < actuaarray.length; i++)
					{
						arimatotaldeviation = arimatotaldeviation
								+ (Math.abs(arimaprepartarray[i]
										- actuaarray[i])) / actuaarray[i];
					}
					arima_averageerror = arimatotaldeviation
							/ actuaarray.length;
					System.out.println("arima_averageerror="
							+ arima_averageerror);

					double markovtotaldeviation = 0, markove_averageerror = 0;
					for (int i = 0; i < actuaarray.length; i++)
					{
						markovtotaldeviation = markovtotaldeviation
								+ (Math.abs(markovprepartarray[i]
										- actuaarray[i])) / actuaarray[i];
					}
					markove_averageerror = markovtotaldeviation
							/ actuaarray.length;
					System.out.println("markove_averageerror="
							+ markove_averageerror);

					double armatotaldeviation = 0, arma_averageerror = 0;
					for (int i = 0; i < actuaarray.length; i++)
					{
						armatotaldeviation = armatotaldeviation
								+ (Math.abs(armaprepartarray[i] - actuaarray[i]))
								/ actuaarray[i];
					}
					arma_averageerror = armatotaldeviation / actuaarray.length;

					System.out
							.println("arma_averageerror=" + arma_averageerror);
					arima_errorlist.add(new Double(arima_averageerror));
					markov_errorlist.add(new Double(markove_averageerror));
					arma_errorlist.add(new Double(arma_averageerror));
				} else
				{
					System.out
							.println("The length of predicted results of algorithms are different");
					return;
				}
			}
			SavetoJson(markov_errorlist, arima_errorlist, arma_errorlist,
					outputfilename);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static boolean SavetoJson(ArrayList errorlist1,
			ArrayList errorlist2, ArrayList errorlist3, String outputfilename)
	{
		File outputfile = new File(outputfilename);
		if (!outputfile.exists())
		{
			try
			{
				outputfile.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		} else
		{

			try
			{
				outputfile.delete();
				outputfile.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		try
		{
			JsonGenerator generator = Json
					.createGenerator(new FileOutputStream(outputfile));
			generator.writeStartObject();

			generator.writeStartArray("PredictedError");

			for (int i = 0; i < errorlist1.size(); i++)
			{
				generator.writeStartObject();
				generator
						.write("MarkovCost", String.valueOf(errorlist1.get(i)));
				generator.write("ARIMACost", String.valueOf(errorlist2.get(i)));
				generator.write("ARMACost", String.valueOf(errorlist3.get(i)));
				generator.writeEnd();
			}
			generator.writeEnd();
			generator.writeEnd();
			generator.close();
			return true;
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}

	}
}
