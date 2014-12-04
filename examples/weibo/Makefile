include ../Makefile.in

DIR=weibo_bones
SCRIPTS=scripts
JM=java ${JOPTS} -Dconfig.properties=weibo.properties -cp ${CP} edu.cmu.ml.proppr
PSTEM=weibo1
P=$(PSTEM).wam
#APR=0.0001:0.1
#L2PLOCAL=l2plocal:0.001:2
#L2P=l2p:0.001:2
#SRW1=$(L2P)
#ifeq ($(SRW),l2plocal)
#SRW1=$(L2PLOCAL)
#else
#SRW1=$(L2P)
#endif
#WT=tanh
TRSIZE=12700
#EPOCHS=10
TH=${THREADS}

#PR=dpr:0.0001:0.1:adjust
#PR=ppr:10

##############################################################################

default: dev.run

clean:
	rm -f train.data dev.data test.data all.data train.results dev.results test.results train.answers dev.answers test.answers

clean-post: clean

##### generating data files

all.data: all all.allowed
	${SCRIPTS}/conll2proppr_weibo.pl all all.dict all.allowed
train.data: all.data
	head -$(TRSIZE) all.data > train.data
dev.data: all.data
	head -15418 all.data | tail -2718 > dev.data
test.data: all.data
	tail -2613 all.data > test.data

##### compiling the logic program

%.wam: %.ppr
	python ${PROPPR}/src/scripts/compiler.py serialize $< > $@
#	L=`pwd`; cd ${PROPPR}; python /src/scripts/compile.py $$L

##### experiments

%.run: $(P) %.answers %.data
	python ${PROPPR}/scripts/answermetrics.py --data $*.data --answers $*.answers --metric mrr --metric recall | grep -e "metric" -e "micro" -e "macro" > $*.results
	cat $*.results
#	$(JM).Experiment \
#	--programFiles $(P):all.cfacts --apr $(APR) --prover $(PR) --epochs $(EPOCHS) \
#	--strict --train train.data --test $*.data --srw $(SRW) --grounded train.grounded \
#	--params train.params --traceLosses --threads $(TH) --weightingScheme $(WT) --queryAnswerer qa:unnorm \
#			 --solutions $*.answers --queries $*.data --notest --trainer ${TRAINER} --tester ${TESTER}
#	${SCRIPTS}/scoring.pl $*.answers $*.data > $*.results; \
#	cat $*.results; \

%.grounded: $(P) %.data
	$(JM).Grounder \
	--queries $*.data --grounded $@ --threads $(TH)

%.params: %.grounded
	$(JM).Trainer \
	--train $< --params $@ --srw $(SRW) --threads $(TH) --traceLosses --epochs $(EPOCHS) 

%.answers: %.data train.params
	$(JM).QueryAnswerer \
	--queries $< --params train.params --solutions $@ --threads $(TH) --unnormalized

#%.train %.params: %.data $(P)
#	$(JM).Trainer \
#			 --programFiles $(P):all.cfacts --prover $(PR) --epochs $(EPOCHS) \
#			 --strict --train $*.data --test $*.data --srw $(SRW) --output train.grounded \
#			 --params train.params --traceLosses --threads $(TH) --weightingScheme $(WT) --queryAnswerer qa:unnorm \
#			 --solutions $*.answers --queries $*.data --notest --trainer ${TRAINER} --tester ${TESTER}

#%.query: %.data train.params
#	$(JM).QueryAnswerer \
#			 --programFiles $(P):all.cfacts --prover $(PR) \
#			 --strict --srw $(SRW) --output $*.answers \
#			 --params train.params --threads $(TH) --weightingScheme $(WT) --queries $*.data --unnormalized; \
#	${SCRIPTS}/scoring.pl $*.answers $*.data > $*.results
#	cat $*.results; \

