package com.njust.test;

import java.util.*;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.njust.helper.TimeSeriesAnalysis;


public class RServe_Test
{
	public static void main(String[] args)
	{
		String datafile = "D:/workspace/garch/example/c4.2xlarge-spotprice_us-east-1b.json";
		try
		{
			RConnection re = new RConnection();
			TimeSeriesAnalysis test = new TimeSeriesAnalysis();
			test.ParseOneFile_Example(re,datafile);
		} catch (RserveException e)
		{
			e.printStackTrace();
		}
	}
}