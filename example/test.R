#加载库
library(TSA)
library(rjson)

#数据载入
json_data<-fromJSON(paste(readLines('D:/R-Data/SpotData/c4.2xlarge-spotprice_us-east-1d.json'), collapse=''))
#预处理
source("D:/workspace/garch/test/timeseriesanalysis/ParseSpotScript.R")
base<-100
learnstep<-360
prestep<-30
temp<-f[base:(base+learnstep)]
prepart<-f[(base+learnstep+1):(base+learnstep+prestep)]

#fGarch库测试
#library(fGarch)
#predicted=100
#garchmod=garch(x=temp,order=c(1,1))
#plot(residuals(garchmod),type='l',ylab = 'Standard residual')
#g1 = garchFit(formula=~garch(1,1),data=difflog,trace=F,cond.dist="std")
#plot(predict(g1,n.ahead=100)$meanError,type='b')
#plot(predict(g2,n.ahead=100)$standardDeviation,type='b')

#差分建模
d<-diff(log(temp))*100
m1=garch(d,order=c(1,1))
summary(m1)

#图形分析
#plot(residuals(m1),type='h')
#qqnorm(residuals(m1))
#qqline(residuals(m1))
plot((fitted(m1)[,1])^2,col='blue',type='l',ylab='Conditional Variance',ylim=c(0,5000),xlab='t')

#长期方差预测
long = m1$coef[1]/(1-m1$coef[2]-m1$coef[3])
long

#条件方差向前一步预测
p = m1$coef[1] + m1$coef[2] * d[learnstep]^2 + m1$coef[3] * sd(d)^2
p
#条件方差向前多步预测
p = m1$coef[1] + m1$coef[2] * p + m1$coef[3] * p
p