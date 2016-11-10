package com.njust.test;

import com.njust.helper.RServeConnection;

public class RServe_Test
{

	public static void main(String[] args)
	{
		RServeConnection test = new RServeConnection();
		test.setFolderPath("D", "R-Data", "garch");
		String str[] = new String[4];
		str[0] = "library(TSA)";
		str[1] = "set.seed(1234567)";
		str[2] = "test.sim = garch.sim(alpha = c(0.02,0.05), beta = .9, n = 500)";
		str[3] = "plot(test.sim, type = 'o', ylab = expression(r[t]), xlab = 't')";
		test.make(str);
		str[1] = "set.seed(5345365)";
		test.make(str);
	}
}