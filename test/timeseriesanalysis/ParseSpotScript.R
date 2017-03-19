fray<-1;i<-1 
time<-1;
L<-length(json_data$SpotPriceHistory)
for(i in 1:L)
{
	fray[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice
	time[L+1-i]<-json_data$SpotPriceHistory[[i]]$Timestamp
}

f<-1;i<-1;
k<-1;
count<-0;
for(i in 1:L)
{
     count<-count+1;
     if(count==60)
     {
		f[k]<-max(fray[(i-29):i]);
		k<-k+1;
        count<-0;
     } 
}