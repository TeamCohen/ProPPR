TRAIN=train
TEST=test
VECTORS=lexical.embeddings
THEANO_ETA=10

default: proppr.settings ${VECTORS}.pkl
	proppr compile wordsimilarity.ppr
	proppr ph-train ${TRAIN}.examples ${VECTORS}.pkl model.pkl --model SimilarityRegression --numIters 5
	proppr answer ${TRAIN}.examples pre.${TRAIN}.solutions.txt
	proppr answer ${TRAIN}.examples post.${TRAIN}.solutions.txt --params ${TRAIN}.params
	proppr answer ${TEST}.examples pre.${TEST}.solutions.txt
	proppr ph-answer ${TEST}.examples ${VECTORS}.pkl model.pkl ${TRAIN}.params post.${TEST}.solutions.txt
	rm -f ${TRAIN}.results ${TEST}.results
	for d in ${TRAIN} ${TEST}; do \
	for p in pre post; do \
	proppr eval $$d.examples $$p.$$d.solutions.txt | tee -a $$d.results; \
	done; \
	done

proppr.settings:
	proppr set --programFiles wordsimilarity.wam --fixedWeights theano_p* --relaxFW 1 --unnormalized 1

${VECTORS}.pkl: ${VECTORS}
	python embeddings2theano.py ${VECTORS} $@

clean:
	rm -f *.grounded* ${TRAIN}.gradient *.pkl *.params* *.solutions.txt *.results

.SECONDARY:
