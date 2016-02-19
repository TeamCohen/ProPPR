include Makefile.in

TRAIN=train
VECTORS=lexical.embeddings

default:
	@echo To run, do:
	@echo make clean
	@echo make init
	@echo make proppr
	@echo make theano
	@echo make proppr
	@echo ...
	@echo make eval

proppr.settings:
	proppr set --programFiles wordsimilarity.wam --fixedWeights theano_p* --relaxFW 1


init: proppr.settings ${TRAIN}.gradient ${VECTORS}.pkl
	python ../pronghorn.py update ${TRAIN}.gradient ${TRAIN}.params ${VECTORS}.pkl model.pkl
	echo "ETA=1.0" > Makefile.in

theano: ${TRAIN}.params
	python ../pronghorn.py update ${TRAIN}.gradient ${TRAIN}.params ${VECTORS}.pkl model.pkl

proppr: ${TRAIN}.params
	BACKUP_NUMBER=`ls train.params* | wc -l`; \
	mv ${TRAIN}.params ${TRAIN}.params.$${BACKUP_NUMBER}; \
	proppr gradient ${TRAIN}.grounded ${TRAIN}.gradient --epochs 1 --initParams ${TRAIN}.params.$${BACKUP_NUMBER} --params ${TRAIN}.params --srw ppr:eta=${ETA}
	ETA=`echo "scale=4;${ETA}*0.8"|bc`; \
	echo "ETA=$$ETA" > Makefile.in

eval: ${TRAIN}.eval

%.eval: pre.%.solutions.txt post.%.solutions.txt
	rm -f $@
	for f in $^; do\
		echo $$f >> $@;\
		proppr eval $*.examples $$f | tee -a $@;\
	done

${VECTORS}.pkl: ${VECTORS}
	python embeddings2theano.py ${VECTORS} $@

%.grounded:
	proppr ground $*.examples $@

%.gradient: %.grounded
	proppr gradient $< $@ --epochs 0

pre.%.solutions.txt:
	proppr answer $*.examples $@

post.%.solutions.txt: %.params
	proppr answer $*.examples $@ --params $<

${TEST}.params: ${TEST}.grounded ${TRAIN}.params
	cp ${TRAIN}.params $@
	python ../pronghorn.py query ${TEST}.grounded.features $@ ${VECTORS}.pkl model.pkl

clean:
	rm -f *.grounded* ${TRAIN}.gradient *.pkl *.params* *.solutions.txt

.SECONDARY:
