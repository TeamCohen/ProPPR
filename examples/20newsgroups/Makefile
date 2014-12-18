include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
SEED=seed_doc_label.1per.cfacts
PROGRAM=20news.wam:20news.sparse:labels.cfacts:$(SEED)
PROGRAM_DEPS=20news.wam 20news.sparse/sparseIndex.txt $(SEED)

### TARGETS
include ../common.in

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded *.solutions.txt

results.txt: pre.train.results.txt post.train.results.txt pre.test.results.txt  post.test.results.txt
	echo phase subset uR mR uMRR mMRR > $@
	cat $^ >> $@

20news.sparse/sparseIndex.txt:
	@echo *** Building sparse graph:
	cd 20news.sparse; $(MAKE) $(MFLAGS) inverse; $(MAKE) $(MFLAGS)

%.cfacts: %.cfacts.gz
	zcat $< > $@

.PRECIOUS: %.examples params.wts %.solutions.txt
