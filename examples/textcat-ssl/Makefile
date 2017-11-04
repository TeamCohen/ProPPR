NUM_SEEDS=20
PPR=all
PARAMS=$(PPR).$(NUM_SEEDS)
BASELINE_PPR=textcat
BASELINE_PARAMS=textcat.$(NUM_SEEDS)

PROVER=idpr
THREADS=32
#keep things fast
DEPTH=10
EPS=5e-6
SQUASHINGFUNCTION=clipExp
#ADAGRAD_TRAIN=--trainer adagrad --srw adagrad:fix=f~n,g~n,~y
ADAGRAD_TRAIN=--trainer adagrad --srw adagrad --fixedWeights f=y
ADAGRAD_TEST=

TRAINER_SETTINGS=--squashingFunction $(SQUASHINGFUNCTION) --threads $(THREADS) --apr depth=$(DEPTH):eps=$(EPS) --epochs 40 --srw:sched=local --traceLosses 1 
GROUNDER_SETTINGS=--threads $(THREADS) --apr depth=$(DEPTH):eps=$(EPS) --duplicateCheck -1 --prover $(PROVER)
ANSWER_SETTINGS=--squashingFunction $(SQUASHINGFUNCTION) --threads $(THREADS) --apr depth=$(DEPTH):eps=$(EPS) --duplicateCheck -1 --prover $(PROVER)

#############################################################################
# spearmint parameters

SETTINGS:=$(shell find . -name "settings.in")
ifneq ($(SETTINGS),)
include settings.in
endif



default:
	echo no default target

clean:
	rm -rf *-inputs
	rm -rf *-dataset-*
	rm -rf *log
	rm -rf nohup.out
	rm -rf output/*

##############################################################################
# foo-inputs: 
#    all the --programFiles inputs other than the wam
#    a programFiles.txt 
#
# foo-$PARAMS-dataset:
#    train.examples
#    test.examples
#    whatever proppr droppings are there, based on foo-inputs
#    eval.log and final line acc.txt
#

##############################################################################
# collect the input files needed to run proppr for an experiment:
#   alltrain-examples.txt, test-examples.txt, labels.txt, corpus.graph, near.graph 
#   *-examples.txt files: lines are docid <TAB> label
#   labels.txt files: one label per line
#   corpus.graph, near.graph files: define hasWord(docid,word) and near(docid,docid), resp
#   also creates labels.cfacts, mutex.cfacts and seeds.graph

%.inputs:
	rm -rf $*-inputs
	mkdir $*-inputs
	(cd `grep $* bin/dataset-index.txt | cut -f1`; pwd; STEM=$* DEST=../$*-inputs NUM_SEEDS=$(NUM_SEEDS) make generate)
	echo building labeled.txt, unlabeled.txt - partition of alltrain-examples.txt
	perl bin/split-train.pl $*-inputs/labeled.txt $*-inputs/unlabeled.txt $(NUM_SEEDS) < $*-inputs/alltrain-examples.txt
	echo renaming test-examples.txt
	cp $*-inputs/test-examples.txt $*-inputs/test.txt
	echo building seeds.graph
	cat $*-inputs/labeled.txt | perl -nae 'print join("\t","seed",$$F[1],$$F[0]),"\n"' > $*-inputs/seeds.graph
	echo building labels.cfacts and mutex.cfacts
	cat $*-inputs/labels.txt | perl -ne 'print "isLabel\t$$_"' > $*-inputs/labels.cfacts
	perl bin/getmutex.pl $*-inputs/labels.txt > $*-inputs/mutex.cfacts
	echo building programFiles.txt
	(ls $*-inputs/*.graph $*-inputs/*.cfacts | perl -ne 'chop &&  print ":$$_"') > $*-inputs/programFiles.txt


##############################################################################
# experiments - pseudo targets

# produce the a training and test set of examples

%.dataset: %-inputs
	rm -rf $*-dataset-$(PARAMS)
	mkdir $*-dataset-$(PARAMS)
	python bin/dataset-builder.py ppr/$(PPR).ppr $*-inputs $*-dataset-$(PARAMS)
	echo building config.json
	bash bin/spearmint-config-builder.bash $*-dataset-$(PARAMS) $*
	\cp $*-dataset-$(PARAMS)/config.json ./
	\cp $*-dataset-$(PARAMS)/train.examples $*-dataset-$(PARAMS)/all_train.examples
	rm -rf $*-dataset-$(PARAMS)/spearmint-output
	mkdir $*-dataset-$(PARAMS)/spearmint-output
	echo created $*-dataset-$(PARAMS)

%.expt: %-dataset-$(PARAMS)
	echo "Make $*.expt"
	make $*.sample
	make -e $*.eval
	\cp *log $*-dataset-$(PARAMS)

%.sample: %-dataset-$(PARAMS)
	bash bin/sampleExample.bash $*-dataset-$(PARAMS) $(jumpyNear1) $(jumpyNear2) $(coFailure) $(mutexFailure1) $(mutexFailure2) $(smoothFailure1) $(smoothFailure2)
	
%.ground: %-dataset-$(PARAMS)
	proppr ground $*-dataset-$(PARAMS)/train $(GROUNDER_SETTINGS)

%.eval: %-dataset-$(PARAMS)
	echo "Make $*.eval"
	proppr compile ppr/$(PPR)
	proppr set --programFiles ppr/$(PPR).wam`cat $*-inputs/programFiles.txt` 
	proppr settings
	proppr ground $*-dataset-$(PARAMS)/train $(GROUNDER_SETTINGS)
	proppr train $*-dataset-$(PARAMS)/train $(TRAINER_SETTINGS) 
	proppr answer $*-dataset-$(PARAMS)/test --params $*-dataset-$(PARAMS)/train.params $(ANSWER_SETTINGS) 
	if [ -s  $*-dataset-$(PARAMS)/eval.log ]; then cp $*-dataset-$(PARAMS)/eval.log eval-`date +%y.%m.%d.%H.%M`.log ; fi
	proppr eval $*-dataset-$(PARAMS)/test --metric acc2 > $*-dataset-$(PARAMS)/eval.log
	\cp $*-dataset-$(PARAMS)/eval.log results.txt
	mv $*-dataset-$(PARAMS)/eval.log $*-dataset-$(PARAMS)/spearmint-output/`printf "%0*d\n" 8 ${job_id}`.results
