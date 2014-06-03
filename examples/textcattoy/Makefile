ifeq ($(strip $(CP)),)
PROPPR=/home/krivard/git/rinkitink/ProPPR
CP:=.:${PROPPR}/bin:${PROPPR}/conf/:${PROPPR}/lib/*

endif
ifeq ($(strip $(SCRIPTS)),)
SCRIPTS:=/home/krivard/ProPPR-examples/scripts
endif

JOPTS=-Xmx500M

### PARAMETERS ###
ifeq ($(strip $(PROVER)),)
PROVER=dpr:1e-5
endif
ifeq ($(strip $(THREADS)),)
THREADS=1
endif

### DATASET
TRAIN=train
TEST=test
PROGRAM=textcat.crules:toylabels.cfacts:toywords.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.cooked

results.txt: pre.train.results.txt pre.test.results.txt post.train.results.txt post.test.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} | tee pre.$*.Tester.log

post.%.Tester.log: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts | tee post.$*.Tester.log

params.wts: ${TRAIN}.examples.cooked
#	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --train $< --params $@ --threads ${THREADS}
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --train $< --params $@ --threads ${THREADS} --srw l2plocal --epochs 100

%.examples.cooked: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.ExampleCooker --programFiles ${PROGRAM} --data $< --output $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

#%.solutions.txt: %.queries
#	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.QueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER}

.PRECIOUS: %.examples params.wts %.solutions.txt
