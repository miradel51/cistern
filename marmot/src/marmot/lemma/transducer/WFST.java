package marmot.lemma.transducer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.javatuples.Pair;

import marmot.lemma.Instance;
import marmot.lemma.Lemmatizer;
import marmot.lemma.transducer.exceptions.NegativeContext;
import marmot.util.Numerics;

public class WFST extends Transducer {

	private static final Logger LOGGER = Logger.getLogger(PFST.class.getName());

	
	public WFST(Map<Character,Integer> alphabet, int c1, int c2, int c3, int c4) throws NegativeContext {
		super(alphabet,c1,c2,c3,c4);
	}

	
	protected void observedCounts(double[][][] gradient, int instanceId) {
		// get data instance
		Instance instance = this.trainingData.get(instanceId);
		String upper = instance.getForm();
		String lower = instance.getLemma();
				
		LOGGER.info("Starting gradient computation for pair (" + upper + "," + lower + ")...");
		//zero out the relevant positions in the log-semiring
		zeroOut(alphas,upper.length()+1, lower.length()+1);
		zeroOut(betas,upper.length()+1, lower.length()+1);
		
		//make the start position unity in the log-semiring
		alphas[0][0] = 0.0;
		betas[upper.length()][lower.length()] = 0.0;
		
		//backward
		for (int i = upper.length() ; i >= 0; --i) {
			for (int j = lower.length(); j >= 0; --j) {
				int contextId = contexts[instanceId][i][j];
				// del 
				if (i < upper.length()) {
					//betas[i][j] = Numerics.sumLogProb(betas[i][j], betas[i+1][j] + Math.log(distribution[contextId][2][0]));
				}
				// ins
				if (j < lower.length()) {
					int outputId = this.alphabet.get(lower.charAt(j));
					//betas[i][j] = Numerics.sumLogProb(betas[i][j], betas[i][j+1] + Math.log(distribution[contextId][0][outputId]));
				}
				// sub
				if (i < upper.length() && j < lower.length()) {
					int outputId = this.alphabet.get(lower.charAt(j));
					//betas[i][j] = Numerics.sumLogProb(betas[i][j], betas[i+1][j+1] + Math.log(distribution[contextId][1][outputId]));
				}
			}
		}

		// partition function
		double Z = Math.exp(betas[0][0]);

		// forward 
		for (int i = 0; i < upper.length() + 1; ++i) {
			for (int j = 0; j < lower.length() + 1 ; ++j ) {
				int contextId = contexts[instanceId][i][j]; 
				
				//alpha updates and gradient observed counts
				double thisAlpha =  Math.exp(alphas[i][j]);
				// ins 
				if (j < lower.length()) {
					int outputId = this.alphabet.get(lower.charAt(j));
					//gradient[contextId][0][outputId] += thisAlpha * distribution[contextId][0][outputId]  / Z *  Math.exp(betas[i][j+1]);
					//alphas[i][j+1] = Numerics.sumLogProb(alphas[i][j+1],alphas[i][j] + Math.log(distribution[contextId][0][outputId]));				

				}
				// sub
				if (j < lower.length() && i < upper.length()) {
					int outputId = this.alphabet.get(lower.charAt(j));
					//gradient[contextId][1][outputId] += thisAlpha * distribution[contextId][1][outputId] / Z *  Math.exp(betas[i+1][j+1]);
					//alphas[i+1][j+1] = Numerics.sumLogProb(alphas[i+1][j+1],alphas[i][j] + Math.log(distribution[contextId][1][outputId]));
				}
				
				// del
				if (i < upper.length()) {
					//gradient[contextId][2][0] += thisAlpha * distribution[contextId][2][0] / Z *  Math.exp(betas[i+1][j]);
					//alphas[i+1][j] = Numerics.sumLogProb(alphas[i+1][j],alphas[i][j] + Math.log(distribution[contextId][2][0]));
				}
			}
		}
	}
	
	@Override
	protected void gradient(double[][][] gradient) {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	protected void gradient(double[][][] gradient, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected double logLikelihood() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	protected double logLikelihood(int i ) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public Lemmatizer train(List<Instance> instances,
			List<Instance> dev_instances) {
		

		this.trainingData = instances;
		this.devData = dev_instances;
		
		Pair<int[][][],Integer> result = preextractContexts(instances,this.c1,this.c2, this.c3, this.c4);
		this.contexts = result.getValue0();
			
		// get maximum input and output strings sizes
		this.alphabet = new HashMap<Character,Integer>();
		Pair<Integer,Integer> maxes = extractAlphabet();
		
		// weights and gradients
		this.weights = new double[result.getValue1()][3][this.alphabet.size()];

		double[][][] gradientVector = new double[result.getValue1()][3][this.alphabet.size()];
		double[][][] approxGradientVector = new double[result.getValue1()][3][this.alphabet.size()];

		
		randomlyInitWeights();
		
		this.alphas = new double[maxes.getValue0()][maxes.getValue1()];
		this.betas = new double[maxes.getValue0()][maxes.getValue1()];
		
		zeroOut(alphas);
		zeroOut(betas);
	
		this.gradient(gradientVector,5);
		
		
		return new LemmatizerWFST();
		
		
	}

}
