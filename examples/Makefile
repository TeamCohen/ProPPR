include Makefile.in

### DATASETS
FAST:=textcattoy top-1000-near-google webkb
SLOW:=cora weibo
ALL:=$(FAST) $(SLOW)

### TARGETS

default: $(addsuffix .results,$(FAST))

all: $(addsuffix .results.$(ALL))

clean: fast.clean

fast.clean: $(addsuffix .clean,$(FAST))

fast.clean-post: $(addsuffix .clean-post,$(FAST))

all.clean: $(addsuffix .clean,$(ALL))

%.results:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS)

%.clean:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean

%.clean-post:
	cd $*; CP=${CP} $(MAKE) $(MFLAGS) clean-post