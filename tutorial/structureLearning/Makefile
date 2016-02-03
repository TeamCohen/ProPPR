default: k_spouse.src k_spouse.sg.eval

# single-iteration structure learning
%.sg.eval:
	proppr sl-train1 $*-train.examples $*-train.cfacts
	proppr sl-answer $*-train.examples $*-train.cfacts $*-train-learned1.ppr
	proppr eval $*-train | tee $@

# iterated structure learning
%.isg.eval:
	proppr sl-train $*-train.examples $*-train.cfacts $*-train-isg.ppr --numIters 5
	proppr sl-answer $*-train.examples $*-train.cfacts $*-train-isg.ppr
	proppr eval $*-train | tee $@

.SECONDARY:

clean:
	rm -f *.wam *.grounded* *.gradient k_*.ppr sg-interp_*.ppr *.solutions.txt *.eval

k_spouse.src:
	grep -v -e "husband" -e "wife" kinship-train.cfacts > k_spouse-train.cfacts
	grep -e "husband" -e "wife" kinship-train.examples > k_spouse-train.examples
