default: k_spouse.src k_spouse.eval

%.gradient:
	proppr compile sl.ppr
	proppr set --programFiles sl.wam:$*.cfacts
	proppr ground $*.examples
	proppr gradient $*.grounded

%.ppr:%-train.gradient
	python structureLearning.py $^ > $@

%.eval:%.ppr
	proppr compile $^
	proppr set --programFiles $*.wam:$*-train.cfacts
	proppr answer $*-train.examples
	proppr eval $*-train | tee $@

.SECONDARY:

clean:
	rm -f *.wam *.grounded* *.gradient k_*.ppr *.solutions.txt *.eval

k_spouse.src:
	grep -v -e "husband" -e "wife" kinship-train.cfacts > k_spouse-train.cfacts
	grep -e "husband" -e "wife" kinship-train.examples > k_spouse-train.examples
