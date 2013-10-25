PROPPR=/home/krivard/workspace/ProPPR-git

SOURCES=knows_person_person.cfacts eats_person_food.cfacts

# TYPES definition:
# drop .cfacts from SOURCES
# foreach elt:
## change _ to space
## get the first word
## drop the first word+_
# change _ to space
# sort and drop duplicates
# add .i to each elt
TYPES=$(addsuffix .i,$(sort $(subst _, ,$(foreach f,$(subst .cfacts,,$(SOURCES)),$(subst $(firstword $(subst _, ,$(f)))_,,$(f))))))
SPARSEN=$(SOURCES:.cfacts=.rce)
ROWOFFSET=$(SOURCES:.cfacts=.rowOffset)
COLINDEX=$(SOURCES:.cfacts=.colIndex)

all: $(SPARSEN) $(ROWOFFSET) $(COLINDEX) $(TYPES) sparseIndex.txt

clean:
	rm -f *.i *.index *.rowOffset *.colIndex *.rce

sparseIndex.txt:
	for k in $(subst .cfacts,,$(SOURCES)) ; \
	do echo "$$k" >> sparseIndex.txt ; \
	done

%.i: %.i1 %.i2
	cat $^ | sort | uniq > $@
# double-colon rule: execute none, any, any set, or all rules, independently, by dependency
%.i1:: *_%_*.cfacts
	cut -f 2 $^ | sort | uniq >> $@
%.i1:: 
	touch $@
%.i2:: *_*_%.cfacts
	cut -f 3 $^ | sort | uniq >> $@
%.i2:: 
	touch $@

%.index: %.cfacts $(TYPES) bin/cfacts2sparse_index.pl
	bin/cfacts2sparse_index.pl $<
	sort $@ -o $@

%.rowOffset %.colIndex: %.index
	bin/index2sparse.pl $<

%.rce: %.rowOffset %.colIndex
#rows
	wc -l $*.rowOffset | awk '{print $$1}' >$@ 
#columns
	wc -l $(word 3,$(subst _, ,$(subst .rowOffset,,$<))).i | awk '{print $$1}' >>$@
#entries
	wc -l $*.colIndex | awk '{print $$1}' >>$@