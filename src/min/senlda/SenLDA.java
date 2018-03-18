/*
 * Created on February 22, 2018
 */
package min.senlda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import min.util.URLs;

/**
 * The Sentence Latent Dirichlet Allocation (Sen-LDA)
 * (2018).
 * 
 * @author Min Shi
 */
public class SenLDA 
{

	/**
	 * document-sentence-word Ids
	 */
	Map<Integer, List<int[]>> doc2senetnceLists;
	/**
	 * document-sentence corpus
	 */
	DocSentences[] doc2sentences;
    /**
     * document data (term lists)
     */
    int[][] documents;

    /**
     * vocabulary size
     */
    int V;

    /**
     * number of topics
     */
    int K;
    /**
     * total number of sentences
     */
    int S;

    /**
     * Dirichlet parameter (document--topic associations)
     */
    double alpha;

    /**
     * Dirichlet parameter (topic--term associations)
     */
    double beta;
    /**
     * Dirichlet parameter (sentence--topic associations)
     */
    double gama;
    /**
     * topic assignments for each word.
     */
    int z[][];

    /**
     * cwt[i][j] number of instances of word i (term?) assigned to topic j.
     */
    int[][] nw;

    /**
     * na[i][j] number of words in document i assigned to topic j.
     */
    int[][] nd;

    /**
     * nwsum[j] total number of words assigned to topic j.
     */
    int[] nwsum;

    /**
     * nasum[i] total number of words in document i.
     */
    int[] ndsum;

    /**
     * cumulative statistics of theta
     */
    double[][] thetasum;
    /**
     * cumulative statistics of delta
     */
    double[][] deltasum;
    /**
     * cumulative statistics of phi
     */
    double[][] phisum;

    /**
     * size of statistics
     */
    int numstats;

    /**
     * sampling lag (?)
     */
    private static int THIN_INTERVAL = 20;

    /**
     * burn-in period
     */
    private static int BURN_IN = 100;

    /**
     * max iterations
     */
    private int ITERATIONS = 1000;

    /**
     * sample lag (if -1 only one sample taken)
     */
    private static int SAMPLE_LAG;

    private static int dispcol = 0;

    /**
     * Initialise the Gibbs sampler with data.
     * 
     * @param V
     *            vocabulary size
     * @param data
     */
    public SenLDA(int[][] documents, int V, Map<Integer, List<int[]>> doc2senetnceLists) 
    {
        this.documents = documents;
        this.V = V;
        this.doc2senetnceLists = doc2senetnceLists;
    }
    public SenLDA()
	{
		// TODO Auto-generated constructor stub
	}

    /**
     * Initialisation: Must start with an assignment of observations to topics ?
     * Many alternatives are possible, I chose to perform random assignments
     * with equal probabilities
     * 
     * @param K
     *            number of topics
     * @return z assignment of topics to words
     */
    public void initialState(int K) 
    {
        int M = documents.length;
        // initialise count variables.
        nw = new int[V][K];
        nd = new int[M][K];
        nwsum = new int[K];
        ndsum = new int[M];
        doc2sentences = new DocSentences[M];
        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.
        z = new int[M][];
        for (int m = 0; m < M; m++) 
        {
            int N = documents[m].length;
            z[m] = new int[N];
            for (int n = 0; n < N; n++)
            {
                int topic = (int) (Math.random() * K);
                z[m][n] = topic;
                // number of instances of word i assigned to topic j
                nw[documents[m][n]][topic]++;
                // number of words in document i assigned to topic j.
                nd[m][topic]++;
                // total number of words assigned to topic j.
                nwsum[topic]++;
            }
            // total number of words in document i
            ndsum[m] = N;
            // each description corresponds to a sentence corpus
            doc2sentences[m] = new DocSentences(m, doc2senetnceLists.get(m));
            doc2sentences[m].initialState(K);
            S += doc2sentences[m].S;
        }
    }

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.
     * 
     * @param K
     *            number of topics
     * @param alpha
     *            symmetric prior parameter on document--topic associations
     * @param beta
     *            symmetric prior parameter on topic--term associations
     */
    public void gibbs(int K, double alpha, double beta, double gama, int iteration) 
    {
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;
        this.gama = gama;
        this.ITERATIONS = iteration;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            deltasum = new double[S][K];
            phisum = new double[K][V];
            numstats = 0;
        }
        // initial state of the Markov chain:
        initialState(K);

        System.out.println("Sampling " + ITERATIONS
            + " iterations with burn-in of " + BURN_IN + " (B/S="
            + THIN_INTERVAL + ").");

        for (int i = 0; i < ITERATIONS; i++) 
        {
            // for all z_i
            for (int m = 0; m < z.length; m++) 
            {
            	DocSentences docsentence = doc2sentences[m]; // get all sentences of the current training description
            	int n = 0;
            	for(int s = 0; s < docsentence.S; s++)
            	{
            		for(int wIndex = 0; wIndex < docsentence.z[s].length; wIndex++)
            		{
            			// (z_i = z[m][n])
                        // sample from p(z_i=j│d_i,s_i,z_(¬i) )∝J(d_i,s_i,α,β,γ)
                        int topic = sampleFullConditional(m, n, s, wIndex, docsentence);
                        z[m][n] = topic;
                        docsentence.z[s][wIndex] = topic;
            			n++;
            		}
            	}
            }
            if ((i < BURN_IN) && (i % THIN_INTERVAL == 0)) 
            {
                System.out.print("B");
                dispcol++;
            }
            // display progress
            if ((i > BURN_IN) && (i % THIN_INTERVAL == 0)) 
            {
                System.out.print("S");
                dispcol++;
            }
            // get statistics after burn-in
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) 
            {
                updateParams();
                System.out.print("|");
                if (i % THIN_INTERVAL != 0)
                    dispcol++;
            }
            if (dispcol >= 100) 
            {
                System.out.println();
                dispcol = 0;
            }
        }
    }

    /**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha)
     * 
     * @param m
     *            document
     * @param n
     *            word in document
     * @param s
     *            sentence
     * @param wIndex
     *            word in sentence
     */
    private int sampleFullConditional(int m, int n, int s, int wIndex, DocSentences docsentence) 
    {
        // remove z_i from the count variables of current description
        int topic = z[m][n];
        nw[documents[m][n]][topic]--;
        nd[m][topic]--;
        nwsum[topic]--;
        ndsum[m]--;
        
        // remove z_i from the count variables of current sentence
        int stopic = docsentence.z[s][wIndex];
        docsentence.nw[docsentence.id2Index.get(docsentence.sentences[s][wIndex])][stopic]--;
        docsentence.nd[s][stopic]--;
        docsentence.nwsum[stopic]--;
        docsentence.ndsum[s]--;
        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) 
        {
//            p[k] = (nw[documents[m][n]][k] + beta) / (nwsum[k] + V * beta)
//                * (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
        	
        	//p(d_i=j│MSU,W,d_(¬i),α,β)
            double pdk = (nw[documents[m][n]][k] + beta) / (nwsum[k] + V * beta)
                * (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
            //p(s_i=j│W,S^((u) ),s_(¬i),β,γ)
            double psk = (docsentence.nw[docsentence.id2Index.get(docsentence.sentences[s][wIndex])][stopic] + beta) / ( docsentence.nwsum[stopic] + docsentence.V * beta) 
            		* (docsentence.nd[s][stopic] + gama) / (docsentence.ndsum[s] + K * gama);
//            if(psk<0)
//            {
//            	System.out.println(stopic);
//            	System.out.println(docsentence.id2Index.get(docsentence.sentences[s][wIndex]));
//            	System.out.println(docsentence.nw[docsentence.id2Index.get(docsentence.sentences[s][wIndex])][stopic]);
//            	System.out.println(psk);
//            }
            p[k] = pdk * psk;
            	
        }
        // cumulate multinomial parameters
        for (int k = 1; k < p.length; k++) 
        {
            p[k] += p[k - 1];
        }
        // scaled sample because of unnormalised p[]
        double u = Math.random() * p[K - 1];
        for (topic = 0; topic < p.length; topic++)
        {
            if (u < p[topic])
                break;
        }

        // add newly estimated z_i to count variables in sentence
        docsentence.nw[docsentence.id2Index.get(docsentence.sentences[s][wIndex])][topic]++;
        docsentence.nd[s][topic]++;
        docsentence.nwsum[topic]++;
        docsentence.ndsum[s]++;
        
        // add newly estimated z_i to count variables in description
//        System.out.println("topic="+topic + " " + p[K - 1]);
        nw[documents[m][n]][topic]++;
        nd[m][topic]++;
        nwsum[topic]++;
        ndsum[m]++;

        return topic;
    }


    // obtain the Top-K similar terms of a given term
    public List<String> getTopKNeighbors(String givenTerm, int topK) throws Exception
    {
    	Map<String, Integer> wordMap = new HashMap<String, Integer>();
    	Map<Integer, String> wordMapstr = new HashMap<Integer, String>();
    	Map<Integer, double[]> wordVector = new HashMap<Integer, double[]>();
    	BufferedReader br = new BufferedReader(new FileReader(URLs.vocabulary));
    	String line = "";
    	String[] params = null;
    	while((line = br.readLine()) != null)
    	{
    		params = line.split("\\=");
    		if(params.length < 2)
    		{
    			params = new String[]{params[0], " "};
    		}
    		wordMap.put(params[1], Integer.parseInt(params[0]));
    		wordMapstr.put(Integer.parseInt(params[0]), params[1]);
    	}
    	br.close();
    	br = new BufferedReader(new FileReader(URLs.modelSavePath+"\\service_phi.txt"));
    	int linecount = 0;
    	String[] probs = null;
    	double[] probsToD = null;
    	while((line = br.readLine()) != null)
    	{
    		probs = line.split(" ");
    		probsToD = new double[probs.length];
    		for(int i = 0; i < probs.length; i++)
    		{
    			probsToD[i] = Double.parseDouble(probs[i]);
    		}
    		wordVector.put(linecount, probsToD);
    		++linecount;
    	}
    	br.close();
    	Map<String, Double> neighbors = new HashMap<String, Double>();
    	int termNo = wordMap.get(givenTerm);
    	String[] TP = topicProb(wordVector.get(termNo));
    	System.out.println(TP[0] + " " + TP[1]);
    	for(Entry<Integer, double[]> wws : wordVector.entrySet())
    	{
    		if(TP[0].equals(topicProb(wws.getValue())[0]))
    		{
    			neighbors.put(wordMapstr.get(wws.getKey()), Math.abs(Double.parseDouble(TP[1]) - Double.parseDouble(topicProb(wws.getValue())[1])));
    		}
    	}
    	neighbors = sortedByIntValue(neighbors);
    	List<String> nearstTerms = new ArrayList<String>();
    	for(Entry<String, Double> entry : neighbors.entrySet())
    	{
    		if((topK--) <= 0) break;
    		nearstTerms.add(entry.getKey() + " = " + entry.getValue());
    	}
    	return nearstTerms;
    }
	private Map<String, Double> sortedByIntValue(final Map<String, Double> map)
	{
		Comparator<String> valueComparator = new Comparator<String>() 
		{
			public int compare(String k1, String k2)
			{
				return map.get(k2) - map.get(k1) > 0 ? -1 : 1;
			}
		};
		Map<String, Double> newMap = new TreeMap<String, Double>(valueComparator);
		newMap.putAll(map);
		return newMap;
	}
	private String[] topicProb(double[] probs)
	{
		String[] results = new String[2];
		double temp = probs[0];
		int pos = 0;
		for(int i = 0; i < probs.length; i++)
		{
			if(temp < probs[i])
			{
				pos = i;
				temp = probs[i];
			}
		}
		results[0] = "" + pos;
		results[1] = "" + temp;
		return results;
	}
    // Save the model
    public void saveModel(String modelName, String savePath) throws Exception
    {
    	String thetaFile = savePath + "\\" + modelName + "_theta.txt";
    	String phiFile = savePath + "\\" + modelName + "_phi.txt";
    	String deltaFile = savePath + "\\" + modelName + "_delta.txt";
    	BufferedWriter bw = new BufferedWriter(new FileWriter(thetaFile));
    	double[][] result = getTheta();
    	StringBuffer sb = new StringBuffer();
    	// Save the theta file
    	for(int i = 0; i < result.length; i++)
    	{
    		for(int j = 0; j < this.K; j++)
    		{
    			sb.append(result[i][j] + " ");
    		}
    		sb.append("\n");
    	}
    	bw.write(sb.toString());
    	bw.flush();
    	bw.close();
    	// Save the phi file
    	bw = new BufferedWriter(new FileWriter(phiFile));
    	result = getPhi();
    	sb = new StringBuffer();
    	for(int i = 0; i < this.V; i++)
    	{
    		for(int j = 0; j < result.length; j++)
    		{
    			sb.append(result[j][i] + " ");
    		}
    		sb.append("\n");
    	}
    	bw.write(sb.toString());
    	bw.flush();
    	bw.close();
    	// save the delta file
    	bw = new BufferedWriter(new FileWriter(deltaFile));
    	List<double[][]> docSentenceTopics = getDelta();
    	sb = new StringBuffer();
    	for(double[][] docSentenceTopic : docSentenceTopics)
    	{
    		for(double[] sentenceTopic : docSentenceTopic)
    		{
    			for(double topic : sentenceTopic)
    			{
    				sb.append(topic + " ");
    			}
    			sb.append("\n");
    		}
    		sb.append("\n\n");
    	}
    	bw.write(sb.toString());
    	bw.flush();
    	bw.close();
    }
    
    /**
     * Retrieve estimated document--topic associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.
     * 
     * @return theta multinomial mixture of document topics (M x K)
     */
    public double[][] getTheta() 
    {
        double[][] theta = new double[documents.length][K];

        if (SAMPLE_LAG > 0) 
        {
            for (int m = 0; m < documents.length; m++)
            {
                for (int k = 0; k < K; k++) 
                {
                    theta[m][k] = thetasum[m][k] / numstats;
                }
            }

        } 
        else 
        {
            for (int m = 0; m < documents.length; m++) 
            {
            	DocSentences docsentence = doc2sentences[m]; // get all sentences of the current training description
            	
                for (int k = 0; k < K; k++) 
                {
                	double probs = 0;
                	for(int s = 0; s < docsentence.S; s++)
                	{
                		probs += (docsentence.nd[s][k] + gama) / (docsentence.ndsum[s] + K * gama);
                	}
                	// p(d_i=j│W,S,α,γ)
                    theta[m][k] = (nd[m][k] + alpha) / (ndsum[m] + K * alpha) * probs;
                }
            }
        }

        return theta;
    }

    /**
     * Retrieve estimated topic--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.
     * 
     * @return phi multinomial mixture of topic words (K x V)
     */
    public double[][] getPhi() 
    {
        double[][] phi = new double[K][V];
        if (SAMPLE_LAG > 0) 
        {
            for (int k = 0; k < K; k++) 
            {
                for (int w = 0; w < V; w++) 
                {
                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } 
        else 
        {
            for (int k = 0; k < K; k++) 
            {
                for (int w = 0; w < V; w++) 
                {
                    phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }
    public List<double[][]> getDelta()
    {
    	List<double[][]> docSentenceTopics = new ArrayList<double[][]>();
    	if (SAMPLE_LAG > 0) 
        {
    		int senCount = 0;
    		for (int m = 0; m < documents.length; m++) 
    		{
    			DocSentences docsentence = doc2sentences[m]; // get all sentences of the current training description
    			double[][] delta = new double[docsentence.S][K];
    			for(int s = 0; s < docsentence.S; s++)
    			{
    				for (int k = 0; k < K; k++) 
    				{
    					delta[s][k] = deltasum[senCount][k] / numstats; 
    				}
    				senCount++;
    			}
    			docSentenceTopics.add(delta);
    		}
        }
    	else
    	{
    		for (int m = 0; m < documents.length; m++) 
    		{
    			DocSentences docsentence = doc2sentences[m]; // get all sentences of the current training description
    			double[][] delta = new double[docsentence.S][K];
    			for(int s = 0; s < docsentence.S; s++)
    			{
    				for (int k = 0; k < K; k++) 
    				{
    					delta[s][k] = (docsentence.nd[s][k] + gama) / (docsentence.ndsum[s] + K*gama); 
    				}
    			}
    			docSentenceTopics.add(delta);
    		}
    	}
    	return docSentenceTopics;
    }
    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() 
    {
        for (int m = 0; m < documents.length; m++) 
        {
            for (int k = 0; k < K; k++) 
            {
                thetasum[m][k] += (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
            }
        }
        for (int k = 0; k < K; k++) 
        {
            for (int w = 0; w < V; w++) 
            {
                phisum[k][w] += (nw[w][k] + beta) / (nwsum[k] + V * beta);
            }
        }
        int senCount = 0;
        for (int m = 0; m < documents.length; m++) 
        {
    		DocSentences docsentence = doc2sentences[m]; // get all sentences of the current training description
        	for(int s = 0; s < docsentence.S; s++)
        	{
        		for (int k = 0; k < K; k++) 
        		{
        			deltasum[senCount][k] += (docsentence.nwsum[k] + gama) / (docsentence.ndsum[s] + K*gama); 
        		}
        		senCount++;
        	}
        }
        numstats++;
    }

    /**
     * Print table of multinomial data
     * 
     * @param data
     *            vector of evidence
     * @param fmax
     *            max frequency in display
     * @return the scaled histogram bin values
     */
    public static void hist(double[] data, int fmax) 
    {

        double[] hist = new double[data.length];
        // scale maximum
        double hmax = 0;
        for (int i = 0; i < data.length; i++) 
        {
            hmax = Math.max(data[i], hmax);
        }
        double shrink = fmax / hmax;
        for (int i = 0; i < data.length; i++) 
        {
            hist[i] = shrink * data[i];
        }

        NumberFormat nf = new DecimalFormat("00");
        String scale = "";
        for (int i = 1; i < fmax / 10 + 1; i++) 
        {
            scale += "    .    " + i % 10;
        }

        System.out.println("x" + nf.format(hmax / fmax) + "\t0" + scale);
        for (int i = 0; i < hist.length; i++) 
        {
            System.out.print(i + "\t|");
            for (int j = 0; j < Math.round(hist[i]); j++) 
            {
                if ((j + 1) % 10 == 0)
                    System.out.print("]");
                else
                    System.out.print("|");
            }
            System.out.println();
        }
    }

    /**
     * Configure the gibbs sampler
     * 
     * @param iterations
     *            number of total iterations
     * @param burnIn
     *            number of burn-in iterations
     * @param thinInterval
     *            update statistics interval
     * @param sampleLag
     *            sample interval (-1 for just one sample at the end)
     */
    public void configure(int iterations, int burnIn, int thinInterval,
        int sampleLag) 
    {
        ITERATIONS = iterations;
        BURN_IN = burnIn;
        THIN_INTERVAL = thinInterval;
        SAMPLE_LAG = sampleLag;
    }

    /**
     * Driver with example data.
     * 
     * @param args
     */
    public static void main(String[] args) 
    {

        // words in documents
        int[][] documents = { {1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 6},
            {2, 2, 4, 2, 4, 2, 2, 2, 2, 4, 2, 2},
            {1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 0},
            {5, 6, 6, 2, 3, 3, 6, 5, 6, 2, 2, 6, 5, 6, 6, 6, 0},
            {2, 2, 4, 4, 4, 4, 1, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 0},
            {5, 4, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2}};
        // vocabulary
        int V = 7;
        int M = documents.length;
        // # topics
        int K = 2;
        // good values alpha = 2, beta = .5
        double alpha = 2;
        double beta = .5;
        double gama = .5;

        System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");

        SenLDA lda = new SenLDA(documents, V, null);
        lda.configure(10000, 2000, 100, 10);
        lda.gibbs(K, alpha, beta, gama, 1000);

        double[][] theta = lda.getTheta();
        double[][] phi = lda.getPhi();

        System.out.println();
        System.out.println();
        System.out.println("Document--Topic Associations, Theta[d][k] (alpha="
            + alpha + ")");
        System.out.print("d\\k\t");
        for (int m = 0; m < theta[0].length; m++) 
        {
            System.out.print("   " + m % 10 + "    ");
        }
        System.out.println();
        for (int m = 0; m < theta.length; m++) 
        {
            System.out.print(m + "\t");
            for (int k = 0; k < theta[m].length; k++) 
            {
                // System.out.print(theta[m][k] + " ");
                System.out.print(shadeDouble(theta[m][k], 1) + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("Topic--Term Associations, Phi[k][w] (beta=" + beta
            + ")");

        System.out.print("k\\w\t");
        for (int w = 0; w < phi[0].length; w++) 
        {
            System.out.print("   " + w % 10 + "    ");
        }
        System.out.println();
        for (int k = 0; k < phi.length; k++) 
        {
            System.out.print(k + "\t");
            for (int w = 0; w < phi[k].length; w++) 
            {
                // System.out.print(phi[k][w] + " ");
                System.out.print(shadeDouble(phi[k][w], 1) + " ");
            }
            System.out.println();
        }
    }

    static String[] shades = {"     ", ".    ", ":    ", ":.   ", "::   ",
        "::.  ", ":::  ", ":::. ", ":::: ", "::::.", ":::::"};

    static NumberFormat lnf = new DecimalFormat("00E0");

    /**
     * create a string representation whose gray value appears as an indicator
     * of magnitude, cf. Hinton diagrams in statistics.
     * 
     * @param d
     *            value
     * @param max
     *            maximum value
     * @return
     */
    public static String shadeDouble(double d, double max) 
    {
        int a = (int) Math.floor(d * 10 / max + 0.5);
        if (a > 10 || a < 0) 
        {
            String x = lnf.format(d);
            a = 5 - x.length();
            for (int i = 0; i < a; i++) 
            {
                x += " ";
            }
            return "<" + x + ">";
        }
        return "[" + shades[a] + "]";
    }
}