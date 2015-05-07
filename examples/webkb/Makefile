include ../Makefile.in

### DATASET
PROGRAM:=webkb0.wam:webkb.graph:webkb.cfacts
SUBSETS:=wisc wash texas cornell


### TARGETS

all: results.txt

clean:
	rm -f *results.txt *.params *.cooked *.log

results.txt: $(addsuffix .results.txt,$(foreach subset,$(SUBSETS),$(addprefix pre.,$(subset)) $(addprefix post.,$(subset))))
	echo phase.subset uR mR uMRR mMRR uMAP mMAP > $@
	cat $^ >> $@

%.results.txt: %.solutions.txt
	python ${PROPPR}/scripts/answermetrics.py --data train_no_$(word 2,$(subst ., ,$*)).data --answers $< --metric recall --metric mrr --metric map |\
	grep -e "micro:" -e "macro:" |\
	awk '{print $$3}' |\
	tr "\n" " " |\
	awk '{print "$*",$$0}' > $@

%.params: train_no_%.data.grounded
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer --prover ${PROVER} --trainer ${TRAINER} --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS)

train_no_%.data.grounded: train_no_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $< --grounded $@ --prover ${PROVER} --threads ${THREADS}

pre.%.solutions.txt: train_no_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads ${THREADS}

post.%.solutions.txt: train_no_%.data %.params
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads ${THREADS} --params $(word 2,$^)

.SECONDARY:
#.PRECIOUS: train_no_%.data.cooked %.params
