DIR=v12
JM=java -cp .:../bin/:../lib/*:../conf/ edu.cmu.ml.praprolog
PSTEM=h22
P=$(PSTEM).crules
#SRW1=l2p:0.00001:20
SRW1=l2p:0.0001:1
SRW2=l2p:0.0001:1
EPOCHS=100

#PR=dpr:0.00001
#PR=ppr:20
PR=dpr:0.0001:0.001:boost
TH=16
#PR=ppr:10

rebuild:
	(cd ..; ant)

#program files is used as marker to make sure compilation is up-to-date
compile: programFiles.arg

programFiles.arg: *.facts *.rules
	(cd ..; sh scripts/compile.sh $(DIR))

clean:
	rm -f *.*params *.*gradient *.*grounded *.*labeledTestAnswers
	rm -f *.*testMap *.*testAnswers *.g-rules
	rm -f *.fig-crules *.fig-rules

clean-expt:
	rm -f *.expt

.PRECIOUS: %.testAnswers %.gr-testAnswers %.ugr-testAnswers %.testMap %.gr-testMap \
	   %.ugr-testMap %.params %.gr-params %.g-rules %.gradient %.u-testMap %.u-testAnswers \
	   %.fig-params %.fig-rules %.fig-testAnswers

##############################################################################
# experiments with the non-iterated gradient baseline

#executing test queries before training - on the untrained 2nd-order theory

%.u-testAnswers: %-test.testData %-test.cfacts programFiles.arg
	$(JM).QueryAnswerer --programFiles $P:x.cfacts:$*-test.cfacts --threads 3 --prover $(PR) --queries $*-test.testData --output $*.u-testAnswers --unnormalized 

%.u-testMap: %-test.testData %.u-testAnswers programFiles.arg 
	python bin/orderAnswers.py $*.u-testAnswers $*-test.testData | sort -gr > $*.u-labeledTestAnswers
	python bin/avgprec.py < $*.u-labeledTestAnswers > $*.u-testMap
	tail $*.u-testMap

####################
#baseline: learn parameters for the second-order theory

%.grounded %.params: %-train.trainData %-train.cfacts
	echo now creating .grounded and .params for $*
	$(JM).Experiment --programFiles  $P:x.cfacts:$*-train.cfacts --prover $(PR) --epochs $(EPOCHS) --strict \
			 --train $*-train.trainData  --test $*-train.trainData  --srw $(SRW1) \
			 --output $*.grounded --params $*.params --threads 3
	perl bin/view-params.pl $*.params

#test the learned second-order theory parameters - step 1, the raw answers; step 2, the map

%.testAnswers: %.params %-test.testData
	$(JM).QueryAnswerer --programFiles $P:x.cfacts:$*-test.cfacts  --threads 3 --prover $(PR) --queries $*-test.testData --params $*.params --output $*.testAnswers --unnormalized

%.testMap: %-test.testData %.testAnswers 
	python bin/orderAnswers.py $*.testAnswers $*-test.testData | sort -gr > $*.labeledTestAnswers
	python bin/avgprec.py < $*.labeledTestAnswers > $*.testMap
	tail $*.testMap

####################
# baseline for full-data only

%.baseline-testAnswers: programFiles.arg %-test.testData %-test.cfacts
	java edu.cmu.ml.praprolog.QueryAnswerer --programFiles baseline.crules:x.cfacts:$*-test.cfacts --threads 3 --prover dpr:0.0001 \
	       --queries $*-test.testData --output $*.baseline-testAnswers --unnormalized

%.baseline-testMap: %-test.testData %.baseline-testAnswers 
	python bin/orderAnswers.py $*.baseline-testAnswers $*-test.testData | sort -gr > $*.baseline-labeledTestAnswers
	python bin/avgprec.py < $*.baseline-labeledTestAnswers> $*.baseline-testMap
	tail $*.baseline-testMap

#################### the non-iterated gradient

#step one - the gradient

%.gradient: %-train.trainData
	$(JM).FindGradient --programFiles $P:x.cfacts:$*-train.cfacts --prover $(PR) --train $*-train.trainData  \
			 --output $*.grounded --params $*.gradient --srw $(SRW1) --epochs 0
	swap12 $*.gradient | sort -g | head

#step 2 - create a gradient theory
%.g-rules: %.gradient
	perl bin/compileGradient.pl $*.gradient > $*.g-rules

#step 3 - compile the gradient theory
%.crules: %.g-rules
	python ../scripts/rulecompiler.py $*.g-rules $*.crules

#test the gradient-created rules without training (ugr = untrained-gradient-rules)
#again step 1, the raw answers; step 2, the map

%.ugr-testAnswers: %-test.testData %.crules
	$(JM).QueryAnswerer --programFiles $*.crules:$*-test.cfacts --threads 3 --prover $(PR) --queries $*-test.testData --output $*.ugr-testAnswers --unnormalized

%.ugr-testMap: %.ugr-testAnswers %-test.testData
	python bin/orderAnswers.py $*.ugr-testAnswers $*-test.testData | sort -gr > $*.ugr-labeledTestAnswers
	python bin/avgprec.py < $*.ugr-labeledTestAnswers > $*.ugr-testMap
	tail $*.ugr-testMap

#step 4 - retrain the gradient theory

%.gr-grounded %.gr-params: %-train.trainData %.crules %-train.cfacts
	$(JM).Experiment --programFiles uml.cfacts:$*.crules --prover $(PR) --epochs $(EPOCHS) \
			 --train $*-train.trainData  --srw $(SRW2) --test $*-train.trainData \
			 --output $*.gr-grounded --params $*.gr-params --threads 3
	cat $*.gr-params

#test the gradient theory (gr = gradient-rules)
#again step 1, the raw answers; step 2, the map

%.gr-testAnswers: %.gr-params %-test.testData %.crules
	$(JM).QueryAnswerer --programFiles $*.crules:uml.cfacts --threads 3 --prover $(PR) --queries $*-test.testData --output $*.gr-testAnswers \
	--params $*.gr-params --unnormalized 

%.gr-testMap: %.gr-testAnswers %-test.testData
	python bin/orderAnswers.py $*.gr-testAnswers $*-test.testData | sort -gr > $*.gr-labeledTestAnswers
	python bin/avgprec.py < $*.gr-labeledTestAnswers > $*.gr-testMap
	tail $*.gr-testMap

##############################################################################
# iterative gradient finding

# step 1 - iteratively create and compile the iterative gradient rules
# (fig = final iterated gradient, figs = stratified final iterated gradient)

%.fig-crules: %-train.cfacts %-train.trainData
	python bin/iterativeGradientFinder.py $*

# training the fig rules

%.fig-params %-fig.crules: %-train.trainData %.fig-crules %-train.cfacts
	cp $*.fig-crules $*-fig.crules
	$(JM).Experiment --programFiles uml.cfacts:$*-fig.crules --prover $(PR) --epochs $(EPOCHS)  \
				 --train $*-train.trainData  --srw $(SRW2) --test $*-train.trainData \
				 --output $*.gr-grounded --params $*.fig-params --threads $(TH)
	cat $*.fig-params

# testing the fig rules

%.fig-testAnswers: %.fig-params %-test.testData %-fig.crules
	$(JM).QueryAnswerer --programFiles $*-fig.crules:uml.cfacts --strict off --threads $(TH) --prover $(PR) --tester mt:$(TH) --queries $*-test.testData --params $*.fig-params --output $*.fig-testAnswers --unnormalized --weightingScheme tanh

%.fig-testMap: %-test.testData %.fig-testAnswers 
	python bin/orderAnswers.py $*.fig-testAnswers $*-test.testData | sort -gr > $*.fig-labeledTestAnswers
	python bin/avgprec.py < $*.fig-labeledTestAnswers > $*.fig-testMap
	tail $*.fig-testMap

%.ufig-testAnswers: %-test.testData %-fig.crules
	$(JM).QueryAnswerer --programFiles $*-fig.crules:x.cfacts:$*-test.cfacts --threads 3 --prover $(PR) --queries $*-test.testData --output $*.ufig-testAnswers --unnormalized

%.ufig-testMap: %-test.testData %.ufig-testAnswers 
	python bin/orderAnswers.py $*.ufig-testAnswers $*-test.testData | sort -gr > $*.ufig-labeledTestAnswers
	python bin/avgprec.py < $*.ufig-labeledTestAnswers > $*.ufig-testMap
	tail $*.ufig-testMap

##############################################################################
# aggregate experiments...

all-single-expt: father.expt mother.expt husband.expt wife.expt daughter.expt son.expt sister.expt brother.expt uncle.expt aunt.expt niece.expt nephew.expt

all-pair-expt: father+mother.expt husband+wife.expt daughter+son.expt sister+brother.expt uncle+aunt.expt niece+nephew.expt

all-pair-figs-testMap: father+mother-figs.testMap husband+wife-figs.testMap daughter+son-figs.testMap \
	sister+brother-figs.testMap uncle+aunt-figs.testMap niece+nephew-figs.testMap

single-table:
	python bin/createTable.py \
	father.expt mother.expt husband.expt wife.expt daughter.expt son.expt sister.expt brother.expt uncle.expt aunt.expt niece.expt nephew.expt

pair-table:
	python bin/createTable.py \
	father+mother.expt husband+wife.expt daughter+son.expt sister+brother.expt uncle+aunt.expt niece+nephew.expt

fig-single-column: father.fig-testMap mother.fig-testMap husband.fig-testMap wife.fig-testMap daughter.fig-testMap son.fig-testMap sister.fig-testMap brother.fig-testMap uncle.fig-testMap aunt.fig-testMap niece.fig-testMap nephew.fig-testMap
	perl bin/testMap2column.pl father.fig-testMap mother.fig-testMap husband.fig-testMap wife.fig-testMap daughter.fig-testMap son.fig-testMap sister.fig-testMap brother.fig-testMap uncle.fig-testMap aunt.fig-testMap niece.fig-testMap nephew.fig-testMap

ufig-single-column: father.ufig-testMap mother.ufig-testMap husband.ufig-testMap wife.ufig-testMap daughter.ufig-testMap son.ufig-testMap sister.ufig-testMap brother.ufig-testMap uncle.ufig-testMap aunt.ufig-testMap niece.ufig-testMap nephew.ufig-testMap
	perl bin/testMap2column.pl father.ufig-testMap mother.ufig-testMap husband.ufig-testMap wife.ufig-testMap daughter.ufig-testMap son.ufig-testMap sister.ufig-testMap brother.ufig-testMap uncle.ufig-testMap aunt.ufig-testMap niece.ufig-testMap nephew.ufig-testMap

fig-pair-column: father+mother.fig-testMap husband+wife.fig-testMap daughter+son.fig-testMap sister+brother.fig-testMap	uncle+aunt.fig-testMap niece+nephew.fig-testMap
	perl bin/testMap2column.pl father+mother.fig-testMap husband+wife.fig-testMap daughter+son.fig-testMap \
	   sister+brother.fig-testMap uncle+aunt.fig-testMap niece+nephew.fig-testMap

ufig-pair-column: father+mother.ufig-testMap husband+wife.ufig-testMap daughter+son.ufig-testMap sister+brother.ufig-testMap uncle+aunt.ufig-testMap niece+nephew.ufig-testMap
	perl bin/testMap2column.pl father+mother.ufig-testMap husband+wife.ufig-testMap daughter+son.ufig-testMap \
	sister+brother.ufig-testMap uncle+aunt.ufig-testMap niece+nephew.ufig-testMap

full-baseline-column: full-0.05.baseline-testMap full-0.10.fig-testMap full-0.20.baseline-testMap full-0.30.baseline-testMap full-0.40.baseline-testMap full-0.50.baseline-testMap
	perl bin/testMap2column.pl full-0.05.baseline-testMap full-0.10.baseline-testMap full-0.20.baseline-testMap full-0.30.baseline-testMap full-0.40.baseline-testMap full-0.50.baseline-testMap

full-fig-column: full-0.05.fig-testMap full-0.10.fig-testMap full-0.20.fig-testMap full-0.30.fig-testMap full-0.40.fig-testMap full-0.50.fig-testMap
	perl bin/testMap2column.pl full-0.05.fig-testMap full-0.10.fig-testMap full-0.20.fig-testMap full-0.30.fig-testMap full-0.40.fig-testMap full-0.50.fig-testMap

####################
#summarize a full experiment with the non-iterated gradient

%.expt: %.u-testMap %.testMap %.ugr-testMap %.gr-testMap
	echo on `date` summary with P=$P EPOCHS=$(EPOCHS) SRW1=$(SRW1) SRW2=$(SRW2)> $*.expt
	(cd ..; svn info >> $(DIR)/$*.expt)
	tail $*.u-testMap >> $*.expt
	tail $*.testMap >> $*.expt
	tail $*.ugr-testMap >> $*.expt
	tail $*.gr-testMap >> $*.expt
	cat $*.expt

####################
# save .expts in a directory

save:
	if [ -e $(PSTEM).results ]; then echo 'directory $(PSTEM).results exists'; else mkdir $(PSTEM).results; fi
	cp *.*testMap *.expt *.g-rules $(PSTEM).rules $(PSTEM).results


####################
# export data

#assumes we did 'clean' and then 'all-single-expt'

single-tar: 
	tar -vczf leave-one-relation-out.tgz `python bin/ls-single.py -train.trainData -test.testData -test.cfacts -train.cfacts`

#assumes we did 'clean' and then 'all-pair-expt'

pair-tar:
	tar -vczf leave-two-relations-out.tgz `python bin/ls-pair.py -train.trainData -test.testData -test.cfacts -train.cfacts`

