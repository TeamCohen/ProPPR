
'tmp' dataset
version         holdout   baseline   .ufig testMap   .fig testMap    version details
1.0-Notes.txt	90%-10%   0.80	     x.xx   	     0.86
1.0-trunk	90%-10%   0.90	     0.91	     0.93	     5fa898c
1.0-delivered	90%-10%   0.90	     x.xx	     0.60	     /remote/curtis/yww/projects/joint_IE_inference/Praprolog_working/deepLogic June 2015
2.0		90%-10%   0.84	     0.71	     0.75	     5cfd4f0

== ProPPR 1.0 results from YWW ==

python bin/prepareJointData.py tmp-train tmp-test 0.9 0.1

...creates a dataset with 90% of examples used for both train and
background and 10% of background examples discarded.

              baseline    .fig testMap
90%    10%    0.80    	  0.86
80%    20%    0.63    	  0.77

Baseline results were computed like this:

  813  python bin/genBaselinePredictor.py 
  814  python bin/genBaselinePredictor.py  > baseline.rules
  815  compile
  816  make compile
  817  java edu.cmu.ml.praprolog.QueryAnswerer --programFiles baseline.crules:x.cfacts:tmp-test.cfacts  --prover dpr:0.0001 --queries tmp-test.testData --output tmp.baseline-testAnswers --unnormalized

.fig testMap results were computed like this:

  810  python bin/prepareJointData.py tmp-train tmp-test 0.9 0.1
  811  wc tmp-t*
  812  make tmp.fig-testMap

results were evaluated like this:

  818  python bin/orderAnswers.py tmp.baseline-testAnswers tmp-test.testData | sort -gr | python bin/avgprec.py 

