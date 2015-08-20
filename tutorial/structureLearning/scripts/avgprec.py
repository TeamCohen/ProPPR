import sys

#scan thru lines like this:
#+--------------------
#|0.00280174	0	interp(i_uncle,arthur,margaret)
#|0.00276313	1	interp(i_uncle,charles,charlotte)
#|0.00273479	1	interp(i_uncle,arthur,charlotte)
#|0.00273329	1	interp(i_uncle,arthur,colin)
#|0.0025534	0	interp(i_uncle,christopher,victoria)
#|0.00255312	0	interp(i_uncle,andrew,jennifer)
#...
#
#and find average precision

if __name__ == "__main__":

    lastScore = 2.0
    n = 0.0
    sumPrec = 0.0
    rank = 0.0
    numPosAtThisRank = 0
    print '#%s\t%s\t%s\t%s' % ('rank','prec@r','score','example')
    for line in sys.stdin:
        rank += 1.0
        (scoreStr,label,goal) = line.strip().split("\t")
        # a check
        score = float(scoreStr)        
        if score > lastScore:
            print 'lines out of order: this line has score > previous score:',line
            sys.exit(-1)
        if int(label):
            if score>0:
                numPosAtThisRank += 1.0
                prec = numPosAtThisRank/rank
                sumPrec += prec
                n += 1.0
                print '%f\t%f\t%f\t%s' % (rank,prec,score,goal)
            else:
                n += 1.0
                prec = 0.0
                print '%f\t%f\t%f\t%s' % (rank,prec,score,goal)
    if n>0:
        print '#\n#avgPrecision: ',sumPrec/n


