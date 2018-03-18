package min.senlda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created for model the document sentences
 * @author Min Shi
 *
 */
public class DocSentences
{
	/**
	 * document id
	 */
	int docNo;
	/**
	 * total number of sentences in this document
	 */
	int S;
	/**
	 * number of topics
	 */
	int K;
	/**
	 * total number of word in this description
	 */
	int V;
	/**
	 * nw[i][j]: number of instances of word i (term?) assigned to topic j.
	 */
	int[][] nw;
	Map<Integer, Integer> id2Index;
	/**
	 * nd[i][j]: number of words in sentence i assigned to topic j
	 */
	int[][] nd;
	/**
	 * z[i][j]:topic assignments for each word i in sentence j
	 */
	int z[][];
	/**
	 * sentence data
	 */
	int[][] sentences;
	/**
	 * nwsum[j]: total number of words in this document assigned to topic j.
	 */
	int nwsum[];
	/**
	 * ndsum[i]: total number of words in sentence i.
	 */
	int[] ndsum;
	
	/**
	 * 
	 * @param docNo document id
	 * @param data 
	 */
	public DocSentences(int docNo, List<int[]> data)
	{
		this.docNo = docNo;
		this.S = data.size();
		int wordsize = 0;
		id2Index = new HashMap<Integer, Integer>();
		sentences = new int[S][];
		int index = 0;
		for(int i = 0; i < S; i++)
		{
			int[] sentence = data.get(i);
			sentences[i] = new int[sentence.length];
			for(int j = 0; j < sentence.length; j++)
			{
				sentences[i][j] = sentence[j];
				if(!id2Index.containsKey(sentence[j]))
				{
					id2Index.put(sentence[j], index++);
				}
			}
			wordsize += sentence.length;
		}
		this.V = wordsize;
	}
	public void initialState(int K)
	{
		nw = new int[id2Index.size()][K];
		nd = new int[S][K];
		nwsum = new int[K];
		ndsum = new int[S];
		z = new int[S][];
		
		for(int s = 0; s < S; s++)
		{
			int N = sentences[s].length;
			z[s] = new int[N];
			for(int n = 0; n < N; n++)
			{
				int topic = (int) (Math.random() * K);
				z[s][n] = topic;
				nw[id2Index.get(sentences[s][n])][topic]++;
				nd[s][topic]++;
				nwsum[topic]++;
			}
			ndsum[s] = N;
		}
	}
}
