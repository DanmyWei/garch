package timeseriesanalysis;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.elasticworkflow.ResultOutput;
import org.jdom2.Element;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.RVector;
import java.util.ArrayList;
import org.eclipse.swt.widgets.Shell;
import javax.json.Json;
import javax.json.stream.JsonGenerator;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
public class TimeSeriesAnalysis_MarkoveSwitch {

	public TimeSeriesAnalysis_MarkoveSwitch() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		if (!Rengine.versionCheck()) {
		    System.err.println("** Version mismatch - Java files don't match library version.");
		    System.exit(1);
		}
	        System.out.println("Creating Rengine (with arguments)");
			// 1) we pass the arguments from the command line
			// 2) we won't use the main loop at first, we'll start it later
			//    (that's the "false" as second argument)
			// 3) the callbacks are implemented by the TextConsole class above
			Rengine re=new Rengine(null, false, new TextConsole());
	        System.out.println("Rengine created, waiting for R");
			// the engine creates R is a new thread, so we should wait until it's ready
	        if (!re.waitForR()) {
	            System.out.println("Cannot load R");
	            return;
	        }
	        REXP x;
			//x=re.eval("plot(c(2,4,5,5),type='b')");
			//REXP rexpSetFolder = re.eval("setwd('C:/Users/CZC/Documents')");
			//REXP rexpSetFolder = re.eval("setwd('C:/Users/Lenovo/Documents')");
		   // REXP rexpFolder = re.eval("getwd()");
			//re.eval("load(file='C:/数据文件/Spot价格预测/TSMarkov.RData')", false);
			System.out.println(re.eval(".libPaths()"));
			
			System.out.println(re.eval("Sys.getlocale()"));
			//re.eval(".libPaths('C:/Users/CZC/Documents/R/win-library/3.3')");
			//System.out.println(re.eval(".libPaths()"));
			x=re.eval("library(rjson)");
			x=re.eval("library(TSA)");
			//x=re.eval("library(JavaGD)");
			
			x=re.eval("library(MSwM)");
			TimeSeriesAnalysis_MarkoveSwitch analiser=new TimeSeriesAnalysis_MarkoveSwitch();
			//analiser.ParseOneFile_Example(re,"D:/workspacenew/MyworkflowsimV2/Spot价格数据/c4.4xlarge-spotprice_us-east-1c.json");
			
			 DirectoryDialog dialog=new DirectoryDialog(new Shell(), SWT.NONE);
			 dialog.setMessage("选择输入数据文件夹");
			 String dir=dialog.open();
			if(dir==null)
				return;
			dir=dir.replace("\\", "/");
			File files = new File(dir);
			FileFilter filefilter = new FileFilter() {

				public boolean accept(File file) {
					//if the file extension is .txt return true, else false
					if (file.getName().endsWith(".json")) {
						return true;
					}
					return false;
				}
			};
			File[] filelist = files.listFiles(filefilter);
			for (File cfile : filelist){
				if(!cfile.getName().contains("PredictError"))
				{
					
					try {
						analiser.ParseOneFile(re,cfile.getAbsolutePath(),dir,dir+"/output");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			//analiser.ParseOneFile(re,"D:/workspacenew/MyworkflowsimV2/Spot价格数据/t1.micro-spotprice-us-east-1c.json","D:/workspacenew/MyworkflowsimV2/Spot价格数据/output");
	}
	void ParseOneFile_Example(Rengine re,String datafile)
	{
		
		
	
		// TODO Auto-generated method stub
			        try {
						
						re.eval("json_data<-fromJSON(paste(readLines('"+datafile+"'), collapse=''))",false);
						re.eval("source('D:/workspacenew/MyworkflowsimV2/src/timeseriesanalysis/ParseSpotScript.R')");
						
						REXP length_f=re.eval("length(f)");
						int totallength=length_f.asInt();
						int base=100;
						int learnstep=360;
						int arimalearnstep=60;
						int prestep=30;
						ArrayList<Double> arima_errorlist=new ArrayList<Double>();
						ArrayList<Double> markov_errorlist=new ArrayList<Double>();
						ArrayList<Double> arma_errorlist=new ArrayList<Double>();
						re.eval("mod.mswm=NULL");
						re.eval("mod=NULL");
						re.eval("residual=NULL");
						re.eval("ar1ma10=NULL");
						re.eval("a=NULL");
						re.eval("predicted=NULL");
						re.eval("curprob=NULL");
						re.eval("ar1maorigi=NULL");
						re.eval("b=NULL");
						
						
							re.eval("base<-"+String.valueOf(base));
							re.eval("learnstep<-"+String.valueOf(learnstep));
							re.eval("prestep<-"+String.valueOf(prestep));
							re.eval("temp<-f[base:(base+learnstep)]");
							re.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");
							//re.eval("plot(temp,type='b')");
							re.eval("mod=lm(temp~1)");
							int k=5;
							for(;k>1;k--)
							{
								re.eval("k<-"+String.valueOf(k));
								re.eval("mod.mswm=msmFit(mod,k,p=0,sw=c(T,T),control=list(parallel=F))");
								REXP mswm=re.eval("mod.mswm");
								if(mswm.rtype!=REXP.XT_NULL)
								{
									System.out.println("Model established");
									break;
								}
								else
								{
									System.out.println("Test state failed number="+k);
								}
								
							}
							if(k<=1)
							{
								re.eval("plot(temp,type='b')");
								System.out.println("无法建模");
								return;
							}
							re.eval("residual=msmResid(mod.mswm)");
							//re.eval("plot(residual,type='l')");
							//re.eval("ar1ma10=arima(residual["+String.valueOf(learnstep+1-arimalearnstep)+":"+String.valueOf(learnstep+1)+"],order=c(2,1,2))");
							re.eval("ar1ma10=arima(residual,order=c(2,1,0))");
							re.eval("jpeg('d://Residual_predict.jpg')");
							re.eval("a=plot(ar1ma10,n.ahead=prestep,type='b',xlab='time',ylab='price')");
							re.eval("dev.off()");
							Process process0 = Runtime.getRuntime().exec ("open d://Residual_predict.jpg");
							//double[] predicted=new double[prestep];
							re.eval("predicted="+String.valueOf(prestep));
							System.out.println(re.eval("summary(mod.mswm)"));
							System.out.println(re.eval("mod.mswm@Coef"));
						
							re.eval("curprob=mod.mswm@Fit@filtProb[dim(mod.mswm@Fit@filtProb)[1],]");
							for(int p=1;p<=prestep;p++)
							{
								re.eval("curprob=curprob%*%mod.mswm@transMat");
								REXP prob=re.eval("curprob");
								System.out.println(prob);
								double[] probarray= prob.asDoubleArray();
								int curstate=0;
								double maxprob=0;
								for(int i=1;i<=probarray.length;i++)
								{
									double curprob=probarray[i-1];
									if(curprob>maxprob)
									{
										maxprob=curprob;
										curstate=i;
									}
								}
								System.out.println("State:"+curstate);
								re.eval("curstate="+String.valueOf(curstate));
								re.eval("p="+String.valueOf(p));
								re.eval("predicted[p]=mod.mswm@Coef["+String.valueOf(curstate)+",1]+a$pred[p]");
								
							}
							
						
							System.out.println(re.eval("predicted"));
							System.out.println(re.eval("prepart"));
							re.eval("jpeg('d://Markove.jpg')");
	
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("par(new=TRUE)");
							re.eval("points((learnstep+2):(learnstep+1+prestep),predicted,col='red',type='b')");
							re.eval("dev.off()");
							Process process = Runtime.getRuntime().exec ("open d://Markove.jpg");
							re.eval("eacf(temp)");
							re.eval("ar1maorigi=arima(temp,order=c(2,1,0))");
							re.eval("jpeg('d://ARIMA.jpg')");
							re.eval("b=plot(ar1maorigi,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("points((learnstep+2):(learnstep+1+prestep),b$pred,col='red',type='b')");
							re.eval("dev.off()");
							Process process2 = Runtime.getRuntime().exec ("open d://ARIMA.jpg");
							
							re.eval("armamodel=arima(temp,order=c(2,0,0))");
							re.eval("jpeg('d://arma.jpg')");
							re.eval("armaplot=plot(armamodel,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("points((learnstep+2):(learnstep+1+prestep),armaplot$pred,col='red',type='b')");
							re.eval("dev.off()");
							Process process3 = Runtime.getRuntime().exec ("open d://arma.jpg");
							
							REXP actualprepart=re.eval("prepart");
							double[] actuaarray= actualprepart.asDoubleArray();
							
							REXP arimaprepart=re.eval("b$pred");
							double[] arimaprepartarray= arimaprepart.asDoubleArray();
							
							REXP armaprepart=re.eval("armaplot$pred");
							double[] armaprepartarray= armaprepart.asDoubleArray();
							
							REXP markoveprepart=re.eval("predicted");
							double[] markovprepartarray= markoveprepart.asDoubleArray();
							
							if(actuaarray.length==arimaprepartarray.length
									&&arimaprepartarray.length==markovprepartarray.length)
							{
								double arimatotaldeviation=0,arima_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									arimatotaldeviation=arimatotaldeviation+(Math.abs(arimaprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								arima_averageerror=arimatotaldeviation/actuaarray.length;
								System.out.println("arima_averageerror="+arima_averageerror);
								double markovtotaldeviation=0,markove_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									markovtotaldeviation=markovtotaldeviation+(Math.abs(markovprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								markove_averageerror=markovtotaldeviation/actuaarray.length;
								System.out.println("markove_averageerror="+markove_averageerror);
								
								double armatotaldeviation=0,arma_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									armatotaldeviation=armatotaldeviation+(Math.abs(armaprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								arma_averageerror=armatotaldeviation/actuaarray.length;
								
								System.out.println("arma_averageerror="+arma_averageerror);
								arima_errorlist.add(arima_averageerror);
								markov_errorlist.add(markove_averageerror);
								arma_errorlist.add(arma_averageerror);
							}
							else
							{
								System.out.println("The length of predicted results of algorithms are different");
								return ;
							}
								
							//re.eval("points(prepart,col='red')");
	
							/*re.eval("json_data<-fromJSON(paste(readLines('C:/数据文件/Spot价格预测/Spot价格数据/m4.large-spotprice-us-east-1c.json'), collapse=''))",false);
						//	System.out.println(x=re.eval("json_data"));
							re.eval("L<-length(json_data$SpotPriceHistory)");
							//re.eval("f<-1;i<-1;L<-length(json_data$SpotPriceHistory);for(i in 1:L){f[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice;i<-i+1;};plot(f,type='b')");
							
							REXP length=re.eval("L");
						
							re.eval("f<-1");
							re.eval("i<-1");
							for(int i=1;i<=length.asInt();i++)
							{
								re.eval("f[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice");
								re.eval("i<-i+1");
							}
							//re.eval("f");
	
							re.eval("plot(f,type='b')");*/
							
							//System.out.println(x=re.eval("json_data"));
						
						
					
					
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			      
	}
	void ParseOneFile(Rengine re,String datafile,String datadir,String picdir)
	{
		File inputfile=new File(datafile);
		String outputfilename=picdir+"/"+inputfile.getName().replace(".json", "_PredictError")+".json";
		File outputfile=new File(outputfilename);
		if(outputfile.exists())
		{
			System.out.println("跳过："+outputfile);
			return ;
		}
		String strourputdir=picdir+"/"+inputfile.getName().replace(".json", "");
		File outputdir=new File(strourputdir);
		
		if(!outputdir.exists())
		{
			
				try {
					outputdir.mkdirs();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			
		}
		// TODO Auto-generated method stub
			        try {
			        	datafile=datafile.replace("\\", "/");
						re.eval("json_data<-fromJSON(paste(readLines('"+datafile+"'), collapse=''))",false);
						re.eval("source('"+datadir+"/ParseSpotScript.R')");
						
						REXP length_f=re.eval("length(f)");
						int totallength=length_f.asInt();
						int base=100;
						int learnstep=360;
						int arimalearnstep=60;
						int prestep=30;
						ArrayList<Double> arima_errorlist=new ArrayList<Double>();
						ArrayList<Double> markov_errorlist=new ArrayList<Double>();
						ArrayList<Double> arma_errorlist=new ArrayList<Double>();
						for(;base<totallength-learnstep-prestep;base=base+learnstep)
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
							
							
							re.eval("base<-"+String.valueOf(base));
							re.eval("learnstep<-"+String.valueOf(learnstep));
							re.eval("prestep<-"+String.valueOf(prestep));
							re.eval("temp<-f[base:(base+learnstep)]");
							re.eval("prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]");
							//re.eval("plot(temp,type='b')");
							re.eval("mod=lm(temp~1)");
							int k=5;
							for(;k>1;k--)
							{
								re.eval("k<-"+String.valueOf(k));
								re.eval("mod.mswm=msmFit(mod,k,p=0,sw=c(T,T),control=list(parallel=F))");
								REXP mswm=re.eval("mod.mswm");
								
								if(mswm.rtype!=REXP.XT_NULL)
								{
									System.out.println("Model established");
									break;
								}
								else
								{
									System.out.println("Test state failed number="+k);
								}
								
							}
							if(k<=1)
							{
								//re.eval("plot(temp,type='b')");
								System.out.println("无法建模,base="+base);
								continue;
							}
							re.eval("residual=msmResid(mod.mswm)");
							//re.eval("plot(residual,type='l')");
							re.eval("ar1ma10=arima(residual,order=c(2,1,0))");
							re.eval("a=plot(ar1ma10,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
							//double[] predicted=new double[prestep];
							re.eval("predicted="+String.valueOf(prestep));
							System.out.println(re.eval("summary(mod.mswm)"));
							System.out.println(re.eval("mod.mswm@Coef"));
							re.eval("curprob=mod.mswm@Fit@filtProb[dim(mod.mswm@Fit@filtProb)[1],]");
							for(int p=1;p<=prestep;p++)
							{
								re.eval("curprob=curprob%*%mod.mswm@transMat");
								REXP prob=re.eval("curprob");
								System.out.println(prob);
								double[] probarray= prob.asDoubleArray();
								int curstate=0;
								double maxprob=0;
								for(int i=1;i<=probarray.length;i++)
								{
									double curprob=probarray[i-1];
									if(curprob>maxprob)
									{
										maxprob=curprob;
										curstate=i;
									}
								}
								System.out.println("State:"+curstate);
								re.eval("curstate="+String.valueOf(curstate));
								re.eval("p="+String.valueOf(p));
								re.eval("predicted[p]=mod.mswm@Coef["+String.valueOf(curstate)+",1]+a$pred[p]");
								
							}
							
						
							System.out.println(re.eval("predicted"));
							System.out.println(re.eval("prepart"));
							re.eval("jpeg('"+strourputdir+"//Markove_"+String.valueOf(base)+".jpg')");
	
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("par(new=TRUE)");
							re.eval("points((learnstep+2):(learnstep+1+prestep),predicted,col='red',type='b')");
							re.eval("dev.off()");
							//Process process = Runtime.getRuntime().exec ("open d://Markove.jpg");
							
							re.eval("ar1maorigi=arima(temp,order=c(2,1,0))");
							re.eval("jpeg('"+strourputdir+"//ARIMA_"+String.valueOf(base)+".jpg')");
							re.eval("b=plot(ar1maorigi,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("points((learnstep+2):(learnstep+1+prestep),b$pred,col='red',type='b')");
							re.eval("dev.off()");
						//	Process process2 = Runtime.getRuntime().exec ("open d://ARIMA.jpg");
							
							
							re.eval("armamodel=arima(temp,order=c(2,0,0))");
							re.eval("jpeg('"+strourputdir+"//ARMA_"+String.valueOf(base)+".jpg')");
							re.eval("armaplot=plot(armamodel,n.ahead=prestep,type='b',xlab='time',ylab='price',Plot=FALSE)");
							re.eval("plot(c(temp,prepart),type='b')");
							
							re.eval("points((learnstep+2):(learnstep+1+prestep),armaplot$pred,col='red',type='b')");
							re.eval("dev.off()");
							
							
							REXP actualprepart=re.eval("prepart");
							double[] actuaarray= actualprepart.asDoubleArray();
							
							REXP arimaprepart=re.eval("b$pred");
							double[] arimaprepartarray= arimaprepart.asDoubleArray();
							
							REXP armaprepart=re.eval("armaplot$pred");
							double[] armaprepartarray= armaprepart.asDoubleArray();
							
							REXP markoveprepart=re.eval("predicted");
							double[] markovprepartarray= markoveprepart.asDoubleArray();
							
							if(actuaarray.length==arimaprepartarray.length
									&&arimaprepartarray.length==markovprepartarray.length
											&&markovprepartarray.length==armaprepartarray.length)
							{
								double arimatotaldeviation=0,arima_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									arimatotaldeviation=arimatotaldeviation+(Math.abs(arimaprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								arima_averageerror=arimatotaldeviation/actuaarray.length;
								System.out.println("arima_averageerror="+arima_averageerror);
								
								double markovtotaldeviation=0,markove_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									markovtotaldeviation=markovtotaldeviation+(Math.abs(markovprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								markove_averageerror=markovtotaldeviation/actuaarray.length;
								System.out.println("markove_averageerror="+markove_averageerror);
								
								double armatotaldeviation=0,arma_averageerror=0;
								for(int i=0;i<actuaarray.length;i++)
								{
									armatotaldeviation=armatotaldeviation+(Math.abs(armaprepartarray[i]-actuaarray[i]))/actuaarray[i];
								}
								arma_averageerror=armatotaldeviation/actuaarray.length;
								
								System.out.println("arma_averageerror="+arma_averageerror);
								arima_errorlist.add(arima_averageerror);
								markov_errorlist.add(markove_averageerror);
								arma_errorlist.add(arma_averageerror);
							}
							else
							{
								System.out.println("The length of predicted results of algorithms are different");
								return ;
							}
								
							//re.eval("points(prepart,col='red')");
	
							/*re.eval("json_data<-fromJSON(paste(readLines('C:/数据文件/Spot价格预测/Spot价格数据/m4.large-spotprice-us-east-1c.json'), collapse=''))",false);
						//	System.out.println(x=re.eval("json_data"));
							re.eval("L<-length(json_data$SpotPriceHistory)");
							//re.eval("f<-1;i<-1;L<-length(json_data$SpotPriceHistory);for(i in 1:L){f[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice;i<-i+1;};plot(f,type='b')");
							
							REXP length=re.eval("L");
						
							re.eval("f<-1");
							re.eval("i<-1");
							for(int i=1;i<=length.asInt();i++)
							{
								re.eval("f[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice");
								re.eval("i<-i+1");
							}
							//re.eval("f");
	
							re.eval("plot(f,type='b')");*/
						
							//System.out.println(x=re.eval("json_data"));
						}
						
					
						
						SavetoJson(markov_errorlist, arima_errorlist,arma_errorlist, outputfilename);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			      
	}
	boolean SavetoJson(ArrayList<Double> errorlist1,ArrayList<Double> errorlist2,ArrayList<Double> errorlist3,String outputfilename)
	{
		File outputfile=new File(outputfilename);
		if(!outputfile.exists())
		{
			try {
				outputfile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			
			try {
				outputfile.delete();
				outputfile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			JsonGenerator generator=Json.createGenerator(new FileOutputStream(outputfile));
			generator.writeStartObject(); 
			
			generator.writeStartArray("PredictedError");
			
			for(int i=0;i<errorlist1.size();i++)
			{
				generator.writeStartObject(); 
				generator.write("MarkovCost", String.valueOf(errorlist1.get(i)));
				generator.write("ARIMACost",String.valueOf(errorlist2.get(i)));
				generator.write("ARMACost",String.valueOf(errorlist3.get(i)));
				generator.writeEnd();
			}
			
			
			generator.writeEnd();
			generator.writeEnd();
			generator.close();
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}

}
