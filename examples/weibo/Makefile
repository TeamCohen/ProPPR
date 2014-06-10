include ../Makefile.in

DIR=weibo_bones
SCRIPTS=scripts
JM=java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog
PSTEM=weibo1
P=$(PSTEM).crules
#SRW1=l2plocal:0.001:2
SRW1=l2p:0.001:2
WT=tanh
TRSIZE=12700
EPOCHS=10
TH=${THREADS}

PR=dpr:0.0001:0.1:adjust
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

%.crules: %.rules
	L=`pwd`; cd ${PROPPR}; ./scripts/compile.sh $$L

##### experiments

%.run: $(P) %.data train.data
	$(JM).Experiment \
			 --programFiles $(P):all.cfacts --prover $(PR) --epochs $(EPOCHS) \
			 --strict --train train.data --test $*.data --srw $(SRW1) --output train.grounded \
			 --params train.params --traceLosses --threads $(TH) --weightingScheme $(WT) --queryAnswerer qa:unnorm \
			 --solutions $*.answers --queries $*.data --notest

	${SCRIPTS}/scoring.pl $*.answers $*.data > $*.results; \
	cat $*.results; \

%.train %.params: %.data $(P)
	$(JM).Trainer \
			 --programFiles $(P):all.cfacts --prover $(PR) --epochs $(EPOCHS) \
			 --strict --train $*.data --test $*.data --srw $(SRW1) --output train.grounded \
			 --params train.params --traceLosses --threads $(TH) --weightingScheme $(WT) --queryAnswerer qa:unnorm \
			 --solutions $*.answers --queries $*.data --notest

%.query: %.data train.params
	$(JM).QueryAnswerer \
			 --programFiles $(P):all.cfacts --prover $(PR) \
			 --strict --srw $(SRW1) --output $*.answers \
			 --params train.params --threads $(TH) --weightingScheme $(WT) --queries $*.data --unnormalized; \
	${SCRIPTS}/scoring.pl $*.answers $*.data > $*.results
	cat $*.results; \





