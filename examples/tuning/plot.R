#called="epsilon";
called="epochs";
#called="alpha";

data = read.table(paste(called,"tuning.rdata",sep="_"),col.names=c(
     "dataset","phase","subset","x","called","x","pairs","x","errors","x","errorRate","x","map"));
post = data[data$phase=="post",];
post = post[post$subset != "train",];

hue=0;
leg = c(); hues=c();
maxratio=1.0;
for (d in unique(post$dataset)) { 
    red1 = post[post$dataset==d,];
    for (s in unique(red1$subset)) {
    	red = red1$errorRate[red1$subset==s];
    	maxratio = max(maxratio, max(red / min(red)));
    }
}

par(mfrow=c(2,1));
#plot(range(post$called),range(post$errorRate),type="n",xlab=called,ylab="errorRate");
plot(range(post$called),c(1,maxratio),type="n",xlab=called,ylab="errorRate",log="y");
for (d in unique(post$dataset)) { 
  red1 = post[post$dataset==d,];
  for (s in unique(red1$subset)) { 
    red = red1[red1$subset==s,];
    red = red[order(red$called),];
    name = paste(d,s);
    fac = min(red$errorRate);
    #print(name);
    #print(dim(red));
    h = hcl(hue,60,60);
    lines(red$called,red$errorRate / fac,col=h);	
    #lines(errorRate~called,red,col=h); 
    leg = c(leg,name); 
    hues = c(hues,h);
    hue=hue+40;
  }
}
legend("topright",leg,col=hues,lw=1);


hue=0;
leg = c(); hues=c();
maxratio=1.0;
for (d in unique(post$dataset)) { 
    red1 = post[post$dataset==d,];
    for (s in unique(red1$subset)) {
    	red = red1$map[red1$subset==s];
    	maxratio = min(maxratio, min(red / max(red)));
    }
}

#plot(range(post$called),range(post$map),type="n",xlab=called,ylab="MAP");
plot(range(post$called),c(1,maxratio),type="n",xlab=called,ylab="MAP",log="y");
for (d in unique(post$dataset)) { 
  red1 = post[post$dataset==d,];
  for (s in unique(red1$subset)) { 
    red = red1[red1$subset==s,];
    red = red[order(red$called),];
    name = paste(d,s);
    fac = max(red$map);
    #print(name);
    #print(dim(red));
    h = hcl(hue,60,60);
    lines(red$called,red$map / fac,col=h);	
    #lines(map~called,red,col=h); 
    leg = c(leg,name); 
    hues = c(hues,h);
    hue=hue+40;
  }
}
legend("topright",leg,col=hues,lw=1);
