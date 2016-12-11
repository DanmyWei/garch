package com.njust.test;

import java.util.*;

import com.njust.helper.RServeConnection;

public class RServe_Test
{
	public static void main(String[] args)
	{
		RServeConnection test = new RServeConnection(); // 实例化对象
		test.setFolderPath("D", "R-Data", "garch"); // 指定文件路径 (盘符,文件夹,文件名前缀)
		
		// 将需要执行的R代码依次放入List
		List list = new ArrayList();
		list.add("library(TSA)");
		list.add("set.seed(1234567)");
		list.add("test.sim = garch.sim(alpha = c(0.02,0.05), beta = .9, n = 500)");
		list.add("plot(test.sim, type = 'o', ylab = expression(r[t]), xlab = 't')");
		
		test.make(list);// 执行
	}
}