rfv<-1;i<-1 
rLv<-length(json_var_real$PredictedData)
for(i in 1:rLv){
rfv[i]<-json_var_real$PredictedData[[i]]$sd
i<-i+1
}