package edu.cmu.ml.proppr.learn;


import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import java.util.HashSet;

/**
 * Created by kavyasrinet on 9/26/15.
 */
public class L2SqLoss extends LossFunction{
    protected double margin=0.01;
    @Override
    public int computeLossGradient(ParamVector params, PosNegRWExample example,
                                   TIntDoubleMap gradient, LossData lossdata, SRWOptions c) {
        PosNegRWExample ex = (PosNegRWExample) example;
        int nonzero=0;
        // add empirical loss gradient term
        // positive examples
        double pmax = 0;
        for(int b: ex.getNegList()){
            for(int a: ex.getPosList()){
                double delta = ex.p[b] - ex.p[a];

                int[] keys = getKeys(ex.dp[b],ex.dp[a]);
                for(int feature: keys) {
                    double db = ex.dp[b].get(feature);
                    if(db!=0.0)
                        nonzero++;
                    double da = ex.dp[a].get(feature);
                    if(da!=0.0)
                        nonzero++;
                    double del = derivLoss(delta) * (db - da);
                    gradient.adjustOrPutValue(feature, del, del);

                }
                if (log.isDebugEnabled()) log.debug("+pa=" + ex.p[a] +" pb = " + ex.p[b]);
                lossdata.add(LOSS.L2, Math.max(0, delta));
            }
        }

       return nonzero;
    }

    /**
     * The derivative of the loss associated with a difference in ranking scores of diff.
     * @param diff
     * @return
     */
    public double derivLoss(double diff) {
        return (diff+margin)<0 ? 0 : diff;
    }

    public int[] getKeys(TIntDoubleMap da, TIntDoubleMap db){
        HashSet<Integer> set = new HashSet<Integer>();
        for(int i: da.keys())
            set.add(i);
        for(int i: db.keys())
            set.add(i);
        int[] keys =  new int[set.size()];
        int i=0;
        for(int k: set)
            keys[i++] = k;
        return keys;


    }
}
