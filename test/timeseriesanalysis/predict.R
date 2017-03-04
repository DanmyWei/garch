pf<-1;i<-1 
pL<-length(json_predict$PredictedData)
for(i in 1:pL){
pf[i]<-json_predict$PredictedData[[i]]$pd
i<-i+1
}