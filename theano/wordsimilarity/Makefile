include Makefile.in

TRAIN=train
VECTORS=lexical.embeddings

proppr.settings:
	proppr set --programFiles wordsimilarity.wam --fixedWeights theano_p* --relaxFW 1


init: proppr.settings ${VECTORS}.pkl ${TRAIN}.gradient ${TRAIN}.grounded
	python run_pronghorn.py update ${TRAIN}.grounded ${TRAIN}.gradient ${VECTORS}.pkl model.pkl
	echo "ETA=1.0" > Makefile.in

theano: ${TRAIN}.params
	python run_pronghorn.py update ${TRAIN}.params ${TRAIN}.gradient ${VECTORS}.pkl model.pkl

proppr: ${TRAIN}.params
	BACKUP_NUMBER=`ls train.params* | wc -l`; \
	mv ${TRAIN}.params ${TRAIN}.params.$${BACKUP_NUMBER}; \
	proppr gradient ${TRAIN}.grounded ${TRAIN}.gradient --epochs 1 --initParams ${TRAIN}.params.$${BACKUP_NUMBER} --params ${TRAIN}.params --srw ppr:eta=${ETA}
	ETA=`echo "scale=4;${ETA}*0.8"|bc`; \
	echo "ETA=$$ETA" > Makefile.in

${VECTORS}.pkl: ${VECTORS}
	python embeddings2theano.py ${VECTORS} $@

%.grounded:
	proppr ground $*.examples $@

%.gradient: %.grounded
	proppr gradient $< $@ --epochs 0

reset:
	mv ${TRAIN}.grounded.bak ${TRAIN}.grounded
	rm -f model.pkl

clean:
	rm -f ${TRAIN}.grounded* ${TRAIN}.gradient *.pkl ${TRAIN}.params*
