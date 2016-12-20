f<-1;i<-1 
L<-length(json_data$SpotPriceHistory)
for(i in 1:L){
f[L+1-i]<-json_data$SpotPriceHistory[[i]]$SpotPrice
i<-i+1
}
