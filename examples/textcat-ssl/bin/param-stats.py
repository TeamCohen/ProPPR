import sys

if __name__=="__main__":
    for paramFile in sys.argv[1:]:
        total = {-1:0.0,+1:0.0}
        squares = {-1:0.0,+1:0.0}
        extreme = {-1:0.0,+1:0.0}
        n = {-1:0,+1:0}
        for line in open(paramFile):
            if not line.startswith("#"):
                feat,valString = line.strip().split("\t")
                val = float(valString)
                sign = -1 if val < 0.0 else +1
                total[sign] += val
                squares[sign] += val*val
                n[sign] += 1
                if val*sign > extreme[sign]*sign: extreme[sign] = val 
        print "\t".join([""] + "count avg max ||w||^2".split() + [paramFile])
        for s in (+1,-1):
            tag = 'pos' if s>0 else 'neg'
            avg = '%.3f' % (total[s]/n[s]) if n[s] else '***'
            print "\t".join([tag, ('%d' % n[s]), avg, ('%.2f' % extreme[s]), ('%.2f' % squares[s])])

