NAME=kinship
SL=sl.ppr
INTERP=interp.ppr
EPOCHS=100

mu:=1e-4
g_eta:=4.0
epsilon:=.0099
alpha:=0.01

GSRW=l2p:mu=$(mu):eta=$(g_eta)
TSRW=l2p:mu=$(mu):eta=1.0
SQUASH=clipExp
ITERS=10

default: 
	$(MAKE) isg
	$(MAKE) ${NAME}.results

#include ${ROOT}/../common.in

setup:
	proppr set --apr eps=$(epsilon):alph=$(alpha) --traceLosses 1 --prover idpr --squashingFunction $(SQUASH) --unnormalized 1 --trainer streaming

clean:
	rm -f *.grounded *.grounded.features
	rm -f *_delta_*.ppr *_delta_*.wam
	rm -f *_alone_*.ppr *_alone_*.wam *alone_*.wts
	rm -f *_sl_*.ppr *_sl_*.wam
	rm -f *.gradient

# make pre.X.solutions.txt and post.X.solutions.txt for every rulefile created by isg
${NAME}.results: $(foreach it,$(wildcard ${NAME}_delta_*.ppr),$(addsuffix $(subst .ppr,.solutions.txt,$(subst _delta,-test.alone,$(it))),pre. post.))
	for f in $^;\
	do \
	echo $$f >> $@;\
	EXAMPLES=`echo $$f|awk 'BEGIN{FS="."}{print $$2}'`;\
	proppr eval $$EXAMPLES.examples $$f $(METRICS) | tee -a $@;\
	done

# make e.g. pre.uml_1-test.SL_01.solutions.txt
# from e.g.     uml_1-test.examples
#               uml_1_SL_01.wam
pre.${NAME}-test.%.solutions.txt: ${NAME}-test.examples ${NAME}_%.wam ${NAME}-test.cfacts
	proppr set --programFiles $(word 3,$^):$(word 2,$^)
	proppr answer $< $@

# make e.g. post.uml_1-test.SL_01.solutions.txt
# from e.g.      uml_1-test.examples
#                uml_1_SL_01.wam
#               params.SL_01.wts
post.${NAME}-test.%.solutions.txt: ${NAME}-test.examples ${NAME}_%.wam params.%.wts ${NAME}-test.cfacts
	proppr set --programFiles $(word 4,$^):$(word 2,$^)
	proppr answer $< $@ --params $(word 3,$^)

# make e.g. pre.uml_1-train.SL_01.solutions.txt
# from e.g.     uml_1-train.examples
#               uml_1_SL_01.wam
pre.${NAME}-train.%.solutions.txt: ${NAME}-train.examples ${NAME}_%.wam ${NAME}-train.cfacts
	proppr set --programFiles $(word 3,$^):$(word 2,$^)
	proppr answer $< $@

# make e.g. post.uml_1-train.SL_01.solutions.txt
# from e.g.      uml_1-train.examples
#                uml_1_SL_01.wam
#               params.SL_01.wts
post.${NAME}-train.%.solutions.txt: ${NAME}-train.examples ${NAME}_%.wam params.%.wts ${NAME}-train.cfacts
	proppr set --programFiles $(word 4,$^):$(word 2,$^)
	proppr answer $< $@ --params $(word 3,$^)

# make e.g.      params.alone_01.wts
# from e.g. uml_1-train.alone_01.examples.grounded
params.%.wts: ${NAME}-train.%.examples.grounded
	proppr train $< $@ --epochs $(EPOCHS) --srw ${TSRW}
	sed -i '/train.cfacts/ {p;s/train/test/}' $@
# ^^^ copy the train.cfacts param value to test.cfacts

# make e.g. uml_1-train.SL_01.examples.grounded
# from e.g. uml_1-train.examples
#           uml_1_SL_01.wam
${NAME}-train.%.examples.grounded: ${NAME}-train.examples ${NAME}_%.wam 
	proppr set --programFiles ${NAME}-train.cfacts:$(word 2,$^)
	$(JOPTS) proppr ground $< $@

## Additional targets for building an executable program from gradient-generated rules:

# uml_1_delta_01.ppr + sl.ppr = uml_1_sl_01.ppr
${NAME}_sl_%.ppr: ${NAME}_delta_%.ppr ${SL}
	cat $^ > $@

# uml_1_delta_01.ppr + interp.ppr = uml_1_alone_01.ppr
${NAME}_alone_%.ppr: ${NAME}_delta_%.ppr ${INTERP}
	cat $^ > $@

# Actually run the iterated structural gradient procedure, and supporting target:
isg:
	python scripts/iterativeGradientFinder.py ${NAME} ${ITERS}

# make e.g.           uml_1_01.gradient
# from e.g. uml_1-train.SL_01.examples.grounded
# first training for e.g.   01 epochs
${NAME}_%.gradient: ${NAME}-train.sl_%.examples.grounded
	proppr gradient $< --epochs $* --srw ${GSRW} --gradient $@

%.wam: %.ppr
	proppr compile $<

.SECONDARY:
