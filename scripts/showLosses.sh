#!/bin/bash

awk 'BEGIN{FS=OFS="\t"}/#/{k=k+1; if (query!="") {tot = tot + loss; print query,loss} query = $2; loss=0} /^[1-9]/ { dl = 0; if ($4=="+") { dl = -log($2); } else {dl = -log(1-$2);} loss = loss + dl; print $4,dl,loss} END {print "total loss:",(tot+loss); print "avg loss:",(tot+loss)/(2*k)}' $1