package min.run;

import java.util.List;

import min.senlda.Corpus;
import min.senlda.SenLDA;
import min.util.URLs;
import org.junit.Test;

public class SenLDATest
{
	@Test
	// Train the Sen-LDA model
	public void TestMain() throws Exception
	{
		int T = 20; // The number of latent topics
		double alpha = 0.1; // The prior hyperparameter of documentToTopic 
		double beta = 0.5; // The prior hyperparameter of topicToTerm
		double gama = 0.1;
		int iters = 20000; // The iteration time
		// 1. Load corpus from disk
//		Corpus corpus = new Corpus();
//		corpus.load(URLs.sreviceMashupSentenceToken);
//        // 2. Create a Sen-LDA sampler
//        SenLDA senlda = new SenLDA(corpus.getDocuments(), corpus.getVocabularySize(), corpus.getDoc2senetnceLists());
//        // 3. Training
//        senlda.gibbs(T, alpha, beta, gama, iters);
//        // 4. Save model
//        String modelName = "service";
//        senlda.saveModel(modelName, URLs.modelSavePath);
        
        // 5. Calculate the top-k similar terms for a given term
		SenLDA senlda = new SenLDA();
		String word = "video";
		List<String> topWords = senlda.getTopKNeighbors(word, 500);
		System.out.println("The source word is: " + word + "\n");
		for(String s : topWords)
		{
			System.out.println(s);
		}
	}
}