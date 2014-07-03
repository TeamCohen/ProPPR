include Makefile.in

### DATASETS
FAST:=textcattoy top-1000-near-google webkb
SLOW:=cora weibo
ALL:=$(FAST) $(SLOW)

### TARGETS

default: $(addsuffix .results,$(FAST))

all: $(addsuffix .results,$(ALL))

clean: clean.fast

clean.fast: $(addsuffix .clean,$(FAST))

clean.all: $(addsuffix .clean,$(ALL))

%.results:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS)

%.clean:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean

%.clean-post:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean-post