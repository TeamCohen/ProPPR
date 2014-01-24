package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class MultithreadedTrainerTest extends RedBlueGraph {
	@Test
	public void testTrainParametersOnCookedIterator_smoke() {
		MultithreadedTrainer<String> trainer = 
				new MultithreadedTrainer<String>(new L2PosNegLossTrainedSRW<String>(), 4);
		

		TreeMap<String,Double> startVec = new TreeMap<String,Double>();
		startVec.put("r0",1.0);
		
		Map<String,Double> baseLineVec = brGraphs.get(0).rwr(startVec);
		
		Set<String> pos = bluePart(baseLineVec).keySet();
		Set<String> neg = redPart(baseLineVec).keySet();
		
		TreeMap<String,Double>uniformWeightVec = new TreeMap<String,Double>();
		String[] names = {"fromb","tob","fromr","tor"};
		for (String n : names) uniformWeightVec.put(n,1.0);
		
		Collection<PosNegRWExample<String>> examples = 
				Collections.singleton(new PosNegRWExample<String>(brGraphs.get(0), startVec, pos,neg));
		
		
		trainer.trainParametersOnCookedIterator(examples);
	}

	@Test
	public void testTrainParametersOnCookedIterator_textcattoy() {
		MultithreadedTrainer<String> trainer = new MultithreadedTrainer<String>(new L2PosNegLossTrainedSRW<String>(), 4);
		
		String cookedExamples = 
				 "predict(dh,Y)	1	10	9	16	39	w(a,pos):id(trueLoopRestart):id(trueLoop):w(a,neg):w(pricy,neg):w(pricy,pos):id(demo/textcat/toylabels.facts):w(house,neg):alphaBooster:w(doll,pos):r:w(doll,neg):w(house,pos):id(defaultRestart):id(demo/textcat/toywords.graph)	11->1:13	11->9:4	10->1:1	10->10:2	13->1:13	13->9:11	12->1:13	12->10:5	15->1:13	15->9:7	14->1:13	14->10:9	16->1:13	16->10:12	1->1:13	1->2:10	3->1:13	3->8:6	3->7:6	2->1:13,8	2->3:14	2->5:14	2->4:14	2->6:14	5->1:13	5->13:6	5->14:6	4->1:13	4->11:6	4->12:6	7->1:13	7->9:3	6->1:13	6->15:6	6->16:6	9->1:1	9->9:2	8->1:13	8->10:0"
				+"\npredict(ft,Y)	1	11	10	19	47	w(a,pos):id(trueLoopRestart):w(red,neg):id(trueLoop):w(a,neg):id(demo/textcat/toylabels.facts):w(fire,neg):w(fire,pos):w(red,pos):alphaBooster:w(truck,neg):r:w(truck,pos):id(defaultRestart):w(little,neg):id(demo/textcat/toywords.graph):w(little,pos)	11->1:1	11->11:3	10->1:1	10->10:3	13->1:13	13->11:16	12->1:13	12->10:14	15->1:13	15->11:8	14->1:13	14->10:2	17->1:13	17->11:7	16->1:13	16->10:6	19->1:13	19->11:12	18->1:13	18->10:10	1->1:13	1->2:11	3->1:13	3->8:5	3->9:5	2->1:13,9	2->3:15	2->5:15	2->4:15	2->7:15	2->6:15	5->1:13	5->15:5	5->14:5	4->1:13	4->13:5	4->12:5	7->1:13	7->18:5	7->19:5	6->1:13	6->17:5	6->16:5	9->1:13	9->11:0	8->1:13	8->10:4"
				+"\npredict(rw,Y)	1	9	8	13	31	w(a,pos):id(trueLoopRestart):w(red,neg):id(trueLoop):w(a,neg):id(demo/textcat/toylabels.facts):w(wagon,pos):w(red,pos):alphaBooster:r:w(wagon,neg):id(defaultRestart):id(demo/textcat/toywords.graph)	11->1:11	11->9:7	10->1:11	10->8:2	13->1:11	13->9:6	12->1:11	12->8:10	1->1:11	1->2:9	3->1:11	3->7:5	3->6:5	2->1:11,8	2->3:12	2->5:12	2->4:12	5->1:11	5->13:5	5->12:5	4->1:11	4->10:5	4->11:5	7->1:11	7->9:0	6->1:11	6->8:4	9->1:1	9->9:3	8->1:1	8->8:3"
				+"\npredict(sc,Y)	1	11	10	19	47	w(a,pos):id(trueLoopRestart):w(red,neg):id(trueLoop):w(a,neg):w(pricy,neg):w(pricy,pos):id(demo/textcat/toylabels.facts):w(car,pos):w(red,pos):alphaBooster:r:w(sports,pos):w(car,neg):id(defaultRestart):w(sports,neg):id(demo/textcat/toywords.graph)	11->1:1	11->11:3	10->1:1	10->10:3	13->1:14	13->11:6	12->1:14	12->10:5	15->1:14	15->11:9	14->1:14	14->10:2	17->1:14	17->11:12	16->1:14	16->10:15	19->1:14	19->11:8	18->1:14	18->10:13	1->1:14	1->2:11	3->1:14	3->8:7	3->9:7	2->1:14,10	2->3:16	2->5:16	2->4:16	2->7:16	2->6:16	5->1:14	5->15:7	5->14:7	4->1:14	4->13:7	4->12:7	7->1:14	7->18:7	7->19:7	6->1:14	6->17:7	6->16:7	9->1:14	9->11:0	8->1:14	8->10:4"
				+"\npredict(bk,Y)	1	11	10	19	47	id(trueLoopRestart):w(punk,pos):id(trueLoop):w(barbie,neg):id(demo/textcat/toylabels.facts):w(queen,neg):w(and,pos):alphaBooster:w(and,neg):r:w(queen,pos):w(ken,neg):w(ken,pos):id(defaultRestart):w(barbie,pos):w(punk,neg):id(demo/textcat/toywords.graph)	11->1:0	11->11:2	10->1:0	10->10:2	13->1:13	13->11:10	12->1:13	12->10:5	15->1:13	15->11:14	14->1:13	14->10:3	17->1:13	17->11:6	16->1:13	16->10:8	19->1:13	19->11:12	18->1:13	18->10:11	1->1:13	1->2:9	3->1:13	3->8:4	3->9:4	2->1:13,7	2->3:16	2->5:16	2->4:16	2->7:16	2->6:16	5->1:13	5->15:4	5->14:4	4->1:13	4->13:4	4->12:4	7->1:13	7->18:4	7->19:4	6->1:13	6->17:4	6->16:4	9->1:13	9->11:1	8->1:13	8->10:15"
				+"\npredict(rb,Y)	1	10	9	16	39	w(a,pos):id(trueLoopRestart):w(red,neg):id(trueLoop):w(a,neg):id(demo/textcat/toylabels.facts):w(red,pos):alphaBooster:r:w(bike,neg):id(defaultRestart):w(bike,pos):w(little,neg):id(demo/textcat/toywords.graph):w(little,pos)	11->1:10	11->9:12	10->1:1	10->10:3	13->1:10	13->9:2	12->1:10	12->10:14	15->1:10	15->9:9	14->1:10	14->10:6	16->1:10	16->10:11	1->1:10	1->2:8	3->1:10	3->8:5	3->7:5	2->1:10,7	2->3:13	2->5:13	2->4:13	2->6:13	5->1:10	5->13:5	5->14:5	4->1:10	4->11:5	4->12:5	7->1:10	7->9:4	6->1:10	6->15:5	6->16:5	9->1:1	9->9:3	8->1:10	8->10:0"
				+"\npredict(mv,Y)	1	13	14	28	71	w(minivan,neg):id(defaultRestart):id(trueLoopRestart):w(7-seater,neg):id(demo/textcat/toylabels.facts):w(transmission,pos):w(minivan,pos):w(transmission,neg):w(a,pos):w(an,neg):w(automatic,pos):w(an,pos):w(7-seater,pos):id(trueLoop):w(a,neg):w(automatic,neg):w(big,pos):w(with,pos):alphaBooster:r:w(big,neg):w(with,neg):id(demo/textcat/toywords.graph)	24->1:1	24->14:11	25->1:1	25->13:15	26->1:1	26->14:10	27->1:1	27->13:7	20->1:1	20->14:6	21->1:1	21->13:21	22->1:1	22->14:17	23->1:1	23->13:9	28->1:1	28->14:5	1->1:1	1->2:19	3->1:1	3->11:4	3->12:4	2->10:22	2->1:1,18	2->3:22	2->5:22	2->4:22	2->7:22	2->6:22	2->9:22	2->8:22	5->1:1	5->18:4	5->17:4	4->1:1	4->15:4	4->16:4	7->1:1	7->21:4	7->22:4	6->1:1	6->19:4	6->20:4	9->1:1	9->25:4	9->26:4	8->1:1	8->24:4	8->23:4	11->1:1	11->13:14	10->1:1	10->27:4	10->28:4	13->1:2	13->13:13	12->1:1	12->14:8	15->1:1	15->13:20	14->1:2	14->14:13	17->1:1	17->13:3	16->1:1	16->14:16	19->1:1	19->13:0	18->1:1	18->14:12"
				+"\npredict(hs,Y)	1	14	15	31	79	w(the,neg):id(defaultRestart):w(mortgage,pos):id(trueLoopRestart):w(suburbs,neg):w(house,pos):r:w(in,neg):w(a,pos):w(crushing,neg):w(house,neg):w(crushing,pos):w(suburbs,pos):id(trueLoop):w(a,neg):w(mortgage,neg):w(in,pos):w(big,pos):w(with,pos):alphaBooster:w(the,pos):w(big,neg):w(with,neg):id(demo/textcat/toylabels.facts):id(demo/textcat/toywords.graph)	24->1:1	24->14:4	25->1:1	25->15:12	26->1:1	26->14:22	27->1:1	27->15:18	20->1:1	20->14:7	21->1:1	21->15:16	22->1:1	22->14:0	23->1:1	23->15:20	28->1:1	28->14:9	29->1:1	29->15:11	1->1:1	1->2:6	3->1:1	3->13:23	3->12:23	2->11:24	2->10:24	2->1:1,19	2->3:24	2->5:24	2->4:24	2->7:24	2->6:24	2->9:24	2->8:24	5->1:1	5->18:23	5->19:23	4->1:1	4->17:23	4->16:23	7->1:1	7->22:23	7->23:23	6->1:1	6->20:23	6->21:23	9->1:1	9->26:23	9->27:23	8->1:1	8->24:23	8->25:23	11->1:1	11->31:23	11->30:23	10->1:1	10->28:23	10->29:23	13->1:1	13->15:8	12->1:1	12->14:14	15->1:3	15->15:13	14->1:3	14->14:13	17->1:1	17->15:17	16->1:1	16->14:21	19->1:1	19->15:5	18->1:1	18->14:10	31->1:1	31->15:2	30->1:1	30->14:15"
				+"\npredict(ji,Y)	1	11	12	22	55	w(at,pos):w(a,pos):id(trueLoopRestart):id(trueLoop):w(a,neg):w(job,neg):w(life,pos):id(demo/textcat/toylabels.facts):w(IBM,pos):alphaBooster:w(life,neg):r:w(IBM,neg):w(for,neg):id(defaultRestart):w(at,neg):w(for,pos):id(demo/textcat/toywords.graph):w(job,pos)	20->1:14	20->12:0	21->1:14	21->11:12	22->1:14	22->12:8	1->1:14	1->2:11	3->1:14	3->9:7	3->10:7	2->1:14,9	2->3:17	2->5:17	2->4:17	2->7:17	2->6:17	2->8:17	5->1:14	5->15:7	5->16:7	4->1:14	4->13:7	4->14:7	7->1:14	7->19:7	7->20:7	6->1:14	6->18:7	6->17:7	9->1:14	9->11:4	8->1:14	8->21:7	8->22:7	11->1:2	11->11:3	10->1:14	10->12:1	13->1:14	13->11:5	12->1:2	12->12:3	15->1:14	15->11:13	14->1:14	14->12:18	17->1:14	17->11:10	16->1:14	16->12:16	19->1:14	19->11:15	18->1:14	18->12:6"
				+"\npredict(tf,Y)	1	13	14	28	71	w(due,pos):w(pile,pos):w(forms,neg):w(forms,pos):w(tax,neg):id(defaultRestart):id(trueLoopRestart):id(demo/textcat/toylabels.facts):w(tax,pos):w(yesterday,pos):w(huge,neg):w(a,pos):w(yesterday,neg):w(pile,neg):w(huge,pos):w(due,neg):id(trueLoop):w(a,neg):w(of,pos):w(of,neg):alphaBooster:r:id(demo/textcat/toywords.graph)	24->1:5	24->14:3	25->1:5	25->13:15	26->1:5	26->14:0	27->1:5	27->13:12	20->1:5	20->14:18	21->1:5	21->13:4	22->1:5	22->14:8	23->1:5	23->13:2	28->1:5	28->14:9	1->1:5	1->2:21	3->1:5	3->11:7	3->12:7	2->10:22	2->1:5,20	2->3:22	2->5:22	2->4:22	2->7:22	2->6:22	2->9:22	2->8:22	5->1:5	5->18:7	5->17:7	4->1:5	4->15:7	4->16:7	7->1:5	7->21:7	7->22:7	6->1:5	6->19:7	6->20:7	9->1:5	9->25:7	9->26:7	8->1:5	8->24:7	8->23:7	11->1:5	11->13:17	10->1:5	10->27:7	10->28:7	13->1:6	13->13:16	12->1:5	12->14:11	15->1:5	15->13:10	14->1:6	14->14:16	17->1:5	17->13:13	16->1:5	16->14:14	19->1:5	19->13:19	18->1:5	18->14:1"
				+"\npredict(jm,Y)	1	13	14	28	71	w(bills,pos):w(pile,pos):w(mail,pos):w(and,pos):id(defaultRestart):alphaBooster:id(trueLoopRestart):id(demo/textcat/toylabels.facts):w(huge,neg):w(junk,neg):w(pile,neg):w(huge,pos):w(catalogs,neg):w(bills,neg):w(catalogs,pos):id(trueLoop):w(of,pos):w(of,neg):w(junk,pos):w(mail,neg):w(and,neg):r:id(demo/textcat/toywords.graph)	24->1:4	24->14:0	25->1:4	25->13:20	26->1:4	26->14:3	27->1:4	27->13:12	20->1:4	20->14:18	21->1:4	21->13:19	22->1:4	22->14:2	23->1:4	23->13:13	28->1:4	28->14:14	1->1:4	1->2:21	3->1:4	3->11:7	3->12:7	2->10:22	2->1:4,5	2->3:22	2->5:22	2->4:22	2->7:22	2->6:22	2->9:22	2->8:22	5->1:4	5->18:7	5->17:7	4->1:4	4->15:7	4->16:7	7->1:4	7->21:7	7->22:7	6->1:4	6->19:7	6->20:7	9->1:4	9->25:7	9->26:7	8->1:4	8->24:7	8->23:7	11->1:4	11->13:8	10->1:4	10->27:7	10->28:7	13->1:6	13->13:15	12->1:4	12->14:11	15->1:4	15->13:10	14->1:6	14->14:15	17->1:4	17->13:17	16->1:4	16->14:1	19->1:4	19->13:9	18->1:4	18->14:16";
		Collection<PosNegRWExample<String>> examples = 
				trainer.importCookedExamples(new ParsedFile(new StringReader(cookedExamples)), new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING));

		Map<String,Double> params = trainer.trainParametersOnCookedIterator(examples);
		nonNull(params);
	}
	
	public void testTextCatLoadInnerLoop(String u, PosNegRWExample<String> ex) {
		Map<String,Double> near = ex.getGraph().nearNative(u); 
		if (near==null) assertTrue("near:"+u+":"+ex.toString(),false);
		for (String v : near.keySet()) {
			List<Feature> phi = ex.getGraph().phi(u, v);
			assertFalse(u+":"+v,phi.isEmpty());
		}
	}
	
	public void nonNull(Map<String,Double> params) {
		for (String k : params.keySet()) {
			System.err.println(k+" "+params.get(k));
			assertFalse(k,params.get(k).isNaN());
		}
	}
	
	@Test
	public void test_queuevsrr() {
		System.out.println("\n\nWARNING: This test will occasionally give a false negative (fail in spite of correct behavior).\n"+
				"Multithreading introduces randomness that cannot be seeded. Try re-running before investigating issues.\n");
		SRW.seed(0);
		int epochs = 20;
		
		L2PosNegLossTrainedSRW<String> srwQ = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> trainerQ = new MultithreadedTrainer<String>(srwQ,4);
		Map<String,Double> paramVecQ = trainerQ.trainParametersOnCookedIterator(
				trainerQ.importCookedExamples("testcases/toy.cooked", 
						new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				epochs,
				false);//tracelosses
		
		L2PosNegLossTrainedSRW<String> srwRR = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> trainerRR = new MultithreadedRRTrainer<String>(srwRR,4);
		Map<String,Double> paramVecRR = trainerRR.trainParametersOnCookedIterator(
				trainerRR.importCookedExamples("testcases/toy.cooked", 
						new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				epochs,
				false);//tracelosses
		System.err.println("name                  rrvalue  qvalue");
		for (String f : paramVecRR.keySet()) {
			
			assertTrue(f+" not in Q map",paramVecQ.containsKey(f));
			System.err.println(String.format("%20s  %f  %f",f,paramVecRR.get(f),paramVecQ.get(f)));

			assertFalse(f+" rr nan",paramVecRR.get(f).isNaN());
			assertFalse(f+" rr inf",paramVecRR.get(f).isInfinite());
			assertFalse(f+" q nan",paramVecQ.get(f).isNaN());
			assertFalse(f+" q inf",paramVecQ.get(f).isInfinite());
			// accurate to 2% since most param values ~=1
			assertEquals(f,paramVecRR.get(f),paramVecQ.get(f),0.02);
		}
	}
}
