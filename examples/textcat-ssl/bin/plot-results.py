#convert a results.txt file produced by expt.bash
#to files usable by gnuplot

import sys
import collections
import math

import accum

if __name__=="__main__":
    def accumDict(): return collections.defaultdict(accum.Accum)
    delta = collections.defaultdict(accumDict)
    accuracy = collections.defaultdict(accumDict)
    for line in open(sys.argv[1]):
        if line.startswith("RESULT"):
            parts = line.strip().split()
            before = float(parts[2])
            after = float(parts[3])
            diff = float(parts[4])
            numUnlabeled = int(parts[-1])
            key = ".".join(parts[5:-1])
            #print 'accum',key,numUnlabeled,after,diff
            delta[key][numUnlabeled].inc(diff)
            accuracy[key][numUnlabeled].inc(after)
    for k in accuracy:
	print 'opening', k
        fp = open('plots/%s.gdata' % k,'w')
        for x in sorted(accuracy[k]):
            amu = accuracy[k][x].mean()
            ase = accuracy[k][x].sd()/math.sqrt(accuracy[k][x].count)
            dmu = delta[k][x].mean()
            dse = delta[k][x].sd()/math.sqrt(delta[k][x].count)
            fp.write('%d\t%.3f\t%.3f\t%.3f\t%.3f\n' % (x,amu,ase,dmu,dse))
        fp.close()

