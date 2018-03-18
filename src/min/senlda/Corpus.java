package min.senlda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import min.util.URLs;


/**
 * Description Collection from Web services and Mashups  
 * @author mshi2018
 *
 */
public class Corpus
{
	List<int[]> documentList;
	Map<Integer, List<int[]>> doc2senetnceLists;
	Vocabulary vocabulary; 
	Map<Integer, Integer> serviceDocLocalId2CorpudId, mashupDocLocalId2CorpudId; //mapping the doc local ids to the corpus ids
	
	public Corpus()
	{
		documentList = new ArrayList<int[]>();
		doc2senetnceLists = new HashMap<Integer, List<int[]>>();
		vocabulary = new Vocabulary();
		serviceDocLocalId2CorpudId = new HashMap<Integer, Integer>();
		mashupDocLocalId2CorpudId = new HashMap<Integer, Integer>();
	}
	
	// Load the corpus
	public void load(String corpusFile)
	{
		try
		{
			File sourceFile = new File(corpusFile);
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			if(!sourceFile.getName().equals("sreviceMashupSentenceToken.txt"))
			{
				System.out.println("load fails! The source file is not matched.");
			}
			else
			{
				String line = "", originaLine = "";
				List<String> wordList = null, singleSentence = null;;
				Map<String, Integer> wordIds = null;
				List<List<String>> sentenceList = null;
				int descriptionCount = 0, wIdCount = 0; // # of the description documents
				List<int[]> sentenceIdList = null;
				int[] sIds = null;
				String [] params = null;
				while((originaLine = br.readLine()) != null)
				{
					params = originaLine.split("=");
					if(params[0].split("\\_")[0].equals("api"))
					{
						serviceDocLocalId2CorpudId.put(Integer.parseInt(params[0].split("\\_")[1]), descriptionCount);
					}
					else
					{
						mashupDocLocalId2CorpudId.put(Integer.parseInt(params[0].split("\\_")[1]), descriptionCount);
					}
					if(params.length < 2) line = "";
					else line = params[1];
					wordList = new ArrayList<String>();
					sentenceList = new ArrayList<List<String>>();
					sentenceIdList = new ArrayList<int[]>(); //store the word ids for all sentences of a description 
					for(String sen : line.split("    "))
					{
						singleSentence = new ArrayList<String>();
						for(String word : sen.split(" "))
						{
							wordList.add(word);
							singleSentence.add(word);
						}
						sentenceList.add(singleSentence);
					}
					wordIds = addDocument(wordList);
					for(List<String> oneSentence : sentenceList)
					{
						wIdCount = 0;
						sIds = new int[oneSentence.size()];
						for(String w : oneSentence)
						{
							sIds[wIdCount++] = wordIds.get(w);
						}
						sentenceIdList.add(sIds);
					}
					addSentenceIds(descriptionCount, sentenceIdList);
					descriptionCount++;
				}
				br.close();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// Add sentence Ids of description
	public void addSentenceIds(int descId, List<int[]> sentenceIdList)
	{
		this.doc2senetnceLists.put(descId, sentenceIdList);
	}
	
	// return all documents in the corpus
	public int[][] getDocuments()
	{
		return toArray();
	}
	// add the description
    public Map<String, Integer> addDocument(List<String> document)
    {
    	Map<String, Integer> wordIds = new HashMap<String, Integer>();
        int[] doc = new int[document.size()];
        int i = 0;
        for (String word : document)
        {
            doc[i++] = vocabulary.getId(word, true);
            wordIds.put(word, doc[i-1]);
        }
        documentList.add(doc);
        return wordIds;
    }
    
    // convert documentList to array
    public int[][] toArray()
    {
    	int[][] docs = new int[this.documentList.size()][];
    	for(int i = 0; i < documentList.size(); i++)
    	{
    		docs[i] = documentList.get(i);
    	}
    	return docs;
    }
    
    public Map<Integer, List<int[]>> getDoc2senetnceLists()
	{
		return doc2senetnceLists;
	}

	// save the vocabulary, document and sentences ids
    public void saveFiles()
    {
    	try
		{
    		// save the vocabulary
			BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.vocabulary));
			bw.write(this.vocabulary.toString());
			bw.flush();
			bw.close();
			// save the document and sentence ids
			StringBuffer sb = null;
			bw = new BufferedWriter(new FileWriter(URLs.documentSentenceIds));
			for(Entry<Integer, List<int[]>> docsentence : doc2senetnceLists.entrySet())
			{
				sb = new StringBuffer();
				for(int[] sentence : docsentence.getValue())
				{
					for(int id : sentence)
					{
						sb.append(id + "  ");
					}
					sb.append("  ");
				}
				bw.write(sb.toString().trim());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    // return the vocabulary
    public Vocabulary getVocabulary()
    {
    	return vocabulary;
    }
    
    // return the vocabulary size
    public int getVocabularySize()
    {
    	return vocabulary.size();
    }
    
	public static void main(String[] args) throws Exception
	{
		Corpus corpus = new Corpus();
		corpus.load(URLs.sreviceMashupSentenceToken);
		corpus.saveFiles();
	}
	
}
