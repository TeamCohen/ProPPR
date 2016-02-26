TRAIN=toytrain
TEST=toytest
VECTORS=toycorpus


default: proppr.settings ${VECTORS}.pkl
	proppr compile textcat.ppr 
	proppr ph-train ${TRAIN}.examples ${VECTORS}.pkl model.pkl --numIters 5
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
	proppr set --programFiles textcat.wam:toylabels.cfacts --fixedWeights theano_p* --relaxFW 1

${VECTORS}.pkl: ${VECTORS}.graph
	proppr ph-db $^ $@

clean:
	rm -f *.grounded* ${TRAIN}.gradient *.pkl *.params* *.solutions.txt *.results

.SECONDARY:
