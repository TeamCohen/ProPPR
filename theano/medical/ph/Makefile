DATASET=CL50-disease0.01
TRAIN=${DATASET}-train
#TEST=toytest
VECTORS=${TRAIN}.sentence
PROGRAM_SL=sg-ph-train
PROGRAM_INF=sg-ph
DB=${TRAIN}.rel.cfacts 


default:
	@echo Still under development. Recommended procedure:
	@echo  make import
	@echo  make two

SRC=/remote/curtis/yww/data/ForProfCohen/jointIE+SL/medical
import:
	grep "^rel" ${SRC}/${TRAIN}.cfacts > ${DB}
	grep "^sentence" ${SRC}/${TRAIN}.cfacts > ${VECTORS}.cfacts
	sort ${SRC}/${TRAIN}.trainData | \
	uniq | \
	awk 'BEGIN{FS=OFS="\t"}{if ($$1!=last && last != "") {print last cache;cache=""} cache = cache FS $$2;last=$$1}END{print last cache}'> ${TRAIN}.examples

init: proppr.settings ${VECTORS}.pkl
	cp ${PROGRAM_SL}.ppr ${PROGRAM_SL}_n00.ppr
	proppr compile ${PROGRAM_SL}_n00.ppr
	proppr ground ${TRAIN}.examples ${TRAIN}.grounded --programFiles ${PROGRAM_SL}_n00.wam:${DB} --ternaryIndex true
	proppr gradient ${TRAIN}.grounded ${TRAIN}_n00.gradient --epochs 0
	proppr sl-grad2ppr ${TRAIN}_n00.gradient ${TRAIN}-learned_n00.ppr
	cat ${PROGRAM_SL}.ppr ${TRAIN}-learned_n00.ppr > ${PROGRAM_SL}_n01.ppr

two: init ${TRAIN}-learned_n01.ppr

trash:
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

${TRAIN}-learned_n%.ppr: ${PROGRAM_SL}_n%.ppr
	proppr compile $<
	proppr ph-train ${TRAIN}.examples ${VECTORS}.pkl model.pkl ${TRAIN}-learned_n$*.params --programFiles ${PROGRAM_SL}_n$*.wam:${DB} --ternaryIndex true --numIters $*
	proppr gradient ${TRAIN}.grounded ${TRAIN}_$*.gradient --initParams ${TRAIN}
	proppr sl-grad2ppr ${TRAIN}_n$*.gradient $@
	NEXT=`echo "$*+1"|bc`; \
	cat ${PROGRAM_SL}.ppr $@ > ${PROGRAM_SL}_n0$$NEXT.ppr

proppr.settings:
	proppr set --fixedWeights theano_p*:baseline --relaxFW 1

${VECTORS}.pkl: ${VECTORS}.cfacts
	sort $< | uniq -c | awk 'BEGIN{OFS="\t"}{print $$2,$$3,$$4,$$5,$$1}' > ${VECTORS}.tf
	proppr ph-db ${VECTORS}.tf $@ 3,4 2 weighted

clean:
	rm -f *.grounded* ${TRAIN}.gradient *.pkl *.tf *.params* *.solutions.txt *.results

.SECONDARY:
