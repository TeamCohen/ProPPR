include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
PROGRAM=textcat.wam:toylabels.cfacts:toywords.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded *.solutions.txt

results.txt: pre.train.results.txt post.train.results.txt pre.test.results.txt  post.test.results.txt
	echo phase.subset uR mR uMRR mMRR > $@
	cat $^ >> $@

%.results.txt: %.solutions.txt
	python ${PROPPR}/scripts/answermetrics.py --data $(word 2,$(subst ., ,$*)).examples --answers $< --metric recall --metric mrr |\
	grep -e "micro:" -e "macro:" |\
	awk '{print $$3}' |\
	tr "\n" " " |\
	awk '{print "$*",$$0}' > $@

params.wts: ${TRAIN}.examples.grounded
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS)

%.examples.grounded: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $< --grounded $@ --prover ${PROVER} --threads ${THREADS}

pre.%.solutions.txt: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads 1

post.%.solutions.txt: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads 1 --params $(word 2,$^)

.PRECIOUS: %.examples params.wts %.solutions.txt
