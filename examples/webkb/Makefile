include ../Makefile.in

### DATASET
NAME=webkb
PROGRAM:=${NAME}0.crules:${NAME}.graph:${NAME}.cfacts
SUBSETS:=wisc wash texas cornell

### TARGETS

all: results.txt

clean:
	rm -f *results.txt *.params *.cooked *.log

results.txt: $(addsuffix .results.txt,$(addprefix pre.,$(SUBSETS)) $(addprefix post.,$(SUBSETS)))
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*","${NAME}",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: test_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --tester ${TESTER} | tee pre.$*.Tester.log

post.%.Tester.log: test_%.data %.params
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params $*.params --tester ${TESTER} --srw $(SRW) | tee post.$*.Tester.log

%.params: train_no_%.data.cooked
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS) --trainer ${TRAINER} 

train_no_%.data.cooked: train_no_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.ExampleCooker --programFiles ${PROGRAM} --data $< --output $@ --prover ${PROVER} --threads ${THREADS}


pre.%.solutions.txt: test_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.MultithreadedQueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER} --threads ${THREADS}

post.%.solutions.txt: test_%.data %.params
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.MultithreadedQueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER} --threads ${THREADS} --params $(word 2,$^)

# edu.cmu.ml.praprolog.Trainer --prover dpr:1e-5:0.01 --train train.examples.cooked --params params.wts --threads 16 --srw l2plocal:mu=0.001:eta=1.0:alpha=0.01 --epochs 20 --trainer trove.mrr


.PRECIOUS: train_no_%.data.cooked %.params
