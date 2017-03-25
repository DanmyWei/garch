rmape<-1;i<-1 
rL<-length(json_mape$PredictedData)
for(i in 1:rL){
rmape[i]<-json_mape$PredictedData[[i]]$MAPE
i<-i+1
}