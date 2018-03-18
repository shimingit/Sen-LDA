package min.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;


/**
 * This class is used for pre-process the Web service and Mashup datasets
 * @author Min Shi
 *
 */
public class WebServicePreProcess
{
	private BufferedReader logbr = null;
	private CsvReader creader = null;
	
	
//	@Test
	// combine the service and mashup for model training
	public void combineServiceMashup() throws Exception
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.sreviceMashupSentenceToken));
		
		BufferedReader br = new BufferedReader(new FileReader(URLs.apisFileSentenceToken));
		String line = "";
		int count = 0;
		while((line = br.readLine()) != null)
		{
			bw.write("api_" + (count++) + "=" + line);
			bw.newLine();
		}
		bw.flush();
		br.close();
		br = new BufferedReader(new FileReader(URLs.mashupsFileSentenceToken));
		count = 0;
		while((line = br.readLine()) != null)
		{
			bw.write("mashup_" + (count++) + "=" + line);
			bw.newLine();
		}
		bw.flush();
		bw.close();
		br.close();
	}
	
	@Test
	// Remove the stopwords,tokenization, and convert to single files  
	public void process() throws Exception
	{
		String lineRead = "";
		String tags = "";
		String name = "";
		String categories = "";
		List<String> wordList = new ArrayList<String>();
		
		// read the stopwords
		BufferedReader brStopword = new BufferedReader(new FileReader(URLs.stopwordFile));
		StringBuffer stopwordSb = new StringBuffer();
		while((lineRead = brStopword.readLine()) != null)
		{
			stopwordSb.append(lineRead + " ");
		}
		List<String> stopwords = tokenization(stopwordSb.toString().trim());
		brStopword.close();
		//read the Web services
		List<String> apiList = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(URLs.apisFileCombine));
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.apisFileToken));
		BufferedWriter bwSentence = new BufferedWriter(new FileWriter(URLs.apisFileSentenceToken));
		BufferedWriter bwTag = new BufferedWriter(new FileWriter(URLs.apisFileTagToken));
		BufferedWriter bwTagNameCategory = new BufferedWriter(new FileWriter(URLs.apisFileTagNameCategoryToken));
		BufferedWriter bwFileNum = new BufferedWriter(new FileWriter(URLs.apisFileNum));
		creader = new CsvReader(br, ',');
		creader.skipLine();
		StringBuffer sb = null;
		String[] sentences = null;
		int totalAPItags = 0, totalAPiterms = 0;
		Map<String, String> api2category = new HashMap<String, String>();
		while(creader.readRecord())
		{
			tags = creader.get(1).trim(); //get the tags
			name = creader.get(0).trim().toLowerCase();
			if(name.trim().equals("")) continue;
			categories = creader.get(3).trim();
			api2category.put(name, categories);
			lineRead = creader.get(2).trim(); //get the whole description
			sentences = lineRead.split("\\. "); //split the whole description into sentences 
			if(!apiList.contains(name)) apiList.add(name.trim());
			bwFileNum.write(creader.getCurrentRecord() + "    " + name.trim());
			bwFileNum.newLine();
			//process the whole description
			sb = new StringBuffer();
			for(String word : tokenization(lineRead))
			{
				if(!stopwords.contains(word) && !isDigit(word)) sb.append(word + " ");
			}
			bw.write(sb.toString().trim());
			bw.newLine();
			//process the individual sentence
			sb = new StringBuffer();
			for(String sentence : sentences)
			{
//				totalAPiterms += sentence.split(" ").length;
				for(String word : tokenization(sentence))
				{
					if(!stopwords.contains(word) && !isDigit(word)) 
					{
						totalAPiterms++;
						sb.append(word + " ");
					}
				}
				if(sb.toString().trim().length() != 0)sb.append("   ");
			}
			
			bwSentence.write(sb.toString().trim());
			bwSentence.newLine();
			//process the tags
			sb = new StringBuffer();
			wordList.clear();
			for(String tag : tokenization(tags))
			{
				totalAPItags ++;
				if(!stopwords.contains(tag) && !isDigit(tag)) 
				{
					sb.append(tag + " ");
					wordList.add(tag);
				}
			}
			bwTag.write(sb.toString().trim());
			bwTag.newLine();
			//process the name and categories
			for(String ne : tokenization(name))
			{
				if(!stopwords.contains(ne) && !isDigit(ne) && !wordList.contains(ne)) 
				{
					sb.append(ne + " ");
					wordList.add(ne);
				}
			}
			for(String category : tokenization(categories))
			{
				if(!stopwords.contains(category) && !isDigit(category) && !wordList.contains(category)) 
				{
					sb.append(category + " ");
					wordList.add(category);
				}
			}
			bwTagNameCategory.write(sb.toString().trim());
			bwTagNameCategory.newLine();
		}
		bwFileNum.close();
		bwTagNameCategory.close();
		bwTag.close();
		bwSentence.close();
		bw.close();
		br.close();
		//read the mashup
		List<String> mashupList = new ArrayList<String>();
		Map<Integer, List<Integer>> apiHostMashups = new TreeMap<Integer, List<Integer>>();
		String[] memberAPIs = null;
		int curMashupNo, curAPINo;
		br = new BufferedReader(new FileReader(URLs.mashupsFileCombine));
		bw = new BufferedWriter(new FileWriter(URLs.mashupsFileToken));
		bwSentence = new BufferedWriter(new FileWriter(URLs.mashupsFileSentenceToken));
		bwTag = new BufferedWriter(new FileWriter(URLs.mashupsFileTagToken));
		bwTagNameCategory = new BufferedWriter(new FileWriter(URLs.mashupsFileTagNameCategoryToken));
		bwFileNum = new BufferedWriter(new FileWriter(URLs.mashupsFileNum));
		creader = new CsvReader(br, ',');
		creader.skipLine();
		int  totallinks = 0;
		int totalMashuptags = 0, totalMashupterms = 0;
		int validMCount = 0, validSCount = 0, diffCCount = 0;
		List<String> clist = null;
		double accumulativeDiffC = 0;
		while(creader.readRecord())
		{
			tags = creader.get(1).trim(); //get the tags
			name = creader.get(0).trim().toLowerCase();
			if(name.trim().equals("")) continue;
			memberAPIs = creader.get(4).split(",");
			totallinks += memberAPIs.length;
			categories = creader.get(3).trim();
			lineRead = creader.get(2).trim(); //get the whole description
			totalMashupterms += lineRead.split(" ").length;
			sentences = lineRead.split("\\. "); //split the description into sentences 
			if(!mashupList.contains(name.trim())) mashupList.add(name.trim());
			bwFileNum.write(creader.getCurrentRecord() + "    " + name.trim());
			bwFileNum.newLine();
			if(memberAPIs != null) //if the current mashup has at least one member API
			{
				validMCount++;
				curMashupNo = mashupList.indexOf(name.trim());
				validSCount = memberAPIs.length; // number of composed services
				clist = new ArrayList<String>();
				diffCCount = 0;
				for(String api : memberAPIs)
				{
					curAPINo = apiList.indexOf(api.toLowerCase().trim());
					if(curAPINo == -1) 
					{
//						System.out.println(curMashupNo + " " + name.trim() + "  " + api.toLowerCase());
						validSCount--;
						continue;
					}
					if(!apiHostMashups.containsKey(curAPINo)) 
					{
						ArrayList<Integer> list = new ArrayList<Integer>();
						list.add(curMashupNo);
						apiHostMashups.put(curAPINo, list);
					}
					else
					{
						apiHostMashups.get(curAPINo).add(curMashupNo);
					}
					if(!clist.contains(api2category.get(api.toLowerCase().trim())))
					{
						clist.add(api2category.get(api.toLowerCase().trim()));
						diffCCount++;
					}
				}
				if(diffCCount <= 0)
				{
					validMCount--;
				}
				else
				{
					accumulativeDiffC += (double)diffCCount / (double)validSCount;
				}
			}
			//process the whole description
			sb = new StringBuffer();
			for(String word : tokenization(lineRead))
			{
				if(!stopwords.contains(word) && !isDigit(word)) sb.append(word + " ");
			}
			bw.write(sb.toString().trim());
			bw.newLine();
			//process the individual sentence
			sb = new StringBuffer();
			for(String sentence : sentences)
			{
				for(String word : tokenization(sentence))
				{
					if(!stopwords.contains(word) && !isDigit(word)) sb.append(word + " ");
				}
				if(sb.toString().trim().length() != 0)sb.append("   ");
			}
			bwSentence.write(sb.toString().trim());
			bwSentence.newLine();
			//process the tags
			sb = new StringBuffer();
			wordList.clear();
			for(String tag : tokenization(tags))
			{
				totalMashuptags ++;
				if(!stopwords.contains(tag) && !isDigit(tag)) 
				{
					sb.append(tag + " ");
					wordList.add(tag);
				}
			}
			bwTag.write(sb.toString().trim());
			bwTag.newLine();
			//process the name and categories
			for(String ne : tokenization(name))
			{
				if(!stopwords.contains(ne) && !isDigit(ne) && !wordList.contains(ne)) 
				{
					sb.append(ne + " ");
					wordList.add(ne);
				}
			}
			for(String category : tokenization(categories))
			{
				if(!stopwords.contains(category) && !isDigit(category) && !wordList.contains(category))
				{
					sb.append(category + " ");
					wordList.add(category);
				}
			}
			bwTagNameCategory.write(sb.toString().trim());
			bwTagNameCategory.newLine();
		}
		accumulativeDiffC = accumulativeDiffC / validMCount;
		System.out.println("the probability of belonging to different functional domains: " + accumulativeDiffC);
		System.out.println("totallinks="+totallinks);
		System.out.println("totalAPItags="+totalAPItags);
		System.out.println("totalAPiterms="+totalAPiterms);
		System.out.println("totalMashuptags="+totalMashuptags);
		System.out.println("totalMashupterms="+totalMashupterms);
		bwFileNum.close();
		bwTagNameCategory.close();
		bwTag.close();
		bwSentence.close();
		bw.close();
		br.close();
		//process the composition relationships
		BufferedWriter bwComp = new BufferedWriter(new FileWriter(URLs.apisHostMashups));
		sb = new StringBuffer();
		for(Entry<Integer, List<Integer>> compEntry : apiHostMashups.entrySet())
		{
			sb.append(compEntry.getKey() + "   ");
			for(int mNo : compEntry.getValue())
			{
				sb.append(" " + mNo);
			}
			sb.append("\n");
		}
		bwComp.write(sb.toString().trim());
		bwComp.flush();
		bwComp.close();
	}
	
	private boolean isDigit(String word)
	{
		for(int i=word.length();--i>=0;)
		{  
            int chr=word.charAt(i);  
            if(chr<48 || chr>57) 
            {  
              if(chr != 46)return false;  
            }  
         }  
         return true;  
	}
	
	// Text tokenization, given a text, return a tokenized list
	public List<String> tokenization(String text)
	{
		text = text.replaceAll("'", "").replaceAll("\\.", " ").replaceAll("\\:", " ").replaceAll("\\_", " ");
		
		StandardAnalyzer standarAnalyzer = new StandardAnalyzer(Version.LUCENE_35);
		TokenStream stream = new PorterStemFilter(standarAnalyzer.tokenStream("description", new StringReader(text)));
		List<String> tokenList = new ArrayList<String>();
		try
		{
			CharTermAttribute ta = null;
			String token = "";
			while (stream.incrementToken())
			{
				ta = stream.getAttribute(CharTermAttribute.class);
				token = ta.toString();
				tokenList.add(token.trim());
			}
			stream.close();
			standarAnalyzer.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return tokenList;
	}
	
//	@Test
	// Count the words for obtaining some sights in stop word dictionary creation
	public void wordStatistic() throws Exception
	{
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		String desc = "";
		logbr = new BufferedReader(new FileReader(URLs.apisFile2016));
		creader = new CsvReader(logbr, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			desc = creader.get(3);
			String[] words = desc.split(" ");
			for(String w : words)
			{
				if(!wordCount.containsKey(w))
				{
					wordCount.put(w, 1);
				}
				else
				{
					wordCount.put(w, wordCount.get(w) + 1);
				}
			}
		}
		logbr.close();
//		creader.close();
		
		logbr = new BufferedReader(new FileReader(URLs.mashupsFile2016));
		creader = new CsvReader(logbr, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			desc = creader.get(4);
			String[] words = desc.split(" ");
			for(String w : words)
			{
				if(!wordCount.containsKey(w))
				{
					wordCount.put(w, 1);
				}
				else
				{
					wordCount.put(w, wordCount.get(w) + 1);
				}
			}
		}
		logbr.close();
		creader.close();
		
		wordCount = sortedByIntValue(wordCount);
		
		for(Entry<String, Integer> wc : wordCount.entrySet())
		{
			System.out.println(wc.getKey() + " " + wc.getValue());
		}
	}
	
//	@Test
	// combine the multiple service datasets (2012, 2013, 2016)
	public void combineDatasets() throws Exception
	{
		List<String> allAPIs = new ArrayList<String>();
		List<String> allMashups = new ArrayList<String>();
		// Process the Web service
		// First, read the service dataset 2016
		BufferedReader br = new BufferedReader(new FileReader(URLs.apisFile2016));
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.apisFileCombine));
		CsvWriter csvwriter = new CsvWriter(bw, ',');
		csvwriter.writeRecord(new String[]{"APIName", "Tags", "Description", "Category"});
		creader = new CsvReader(br, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			if(!allAPIs.contains(creader.get(0).trim()))
			{
				allAPIs.add(creader.get(0).trim());
				csvwriter.writeRecord(new String[]{creader.get(0), creader.get(2).replace("###", ","), creader.get(3), creader.get(5)});
			}
		}
		creader.close();
		// Second, read the service dataset 2012
		br = new BufferedReader(new FileReader(URLs.apisFile2012));
		creader = new CsvReader(br, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			if(!allAPIs.contains(creader.get(0).replace("API", "").trim()))
			{
				allAPIs.add(creader.get(0).replace("API", "").trim());
				csvwriter.writeRecord(new String[]{creader.get(0).replace("API", "").trim(), creader.get(1).trim().replaceAll(" ", ","), creader.get(5), creader.get(2)});
			}
		}
		creader.close();
		// Third, read the service dataset 2013
		br = new BufferedReader(new FileReader(URLs.apisFile2013));
		creader = new CsvReader(br, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			if(!allAPIs.contains(creader.get(0).trim()))
			{
				allAPIs.add(creader.get(0).trim());
				csvwriter.writeRecord(new String[]{creader.get(0), creader.get(3).replaceAll(",", "  ").trim().replaceAll("  ", ","), creader.get(7), creader.get(2)});
			}
		}
		creader.close();
		csvwriter.close();
		
		
		//process the mashup
		//First, read the service dataset 2016
		br = new BufferedReader(new FileReader(URLs.mashupsFile2016));
		bw = new BufferedWriter(new FileWriter(URLs.mashupsFileCombine));
		csvwriter = new CsvWriter(bw, ',');
		csvwriter.writeRecord(new String[]{"MashupName", "Tags", "Description", "Category", "MemberAPIs"});
		creader = new CsvReader(br, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			if(!allMashups.contains(creader.get(0).trim()))
			{
				allMashups.add(creader.get(0).trim());
				csvwriter.writeRecord(new String[]{creader.get(0), creader.get(3).replace("###", ","), creader.get(4), creader.get(6), creader.get(2).replace(" @@@ ",",")});
			}
		}
		creader.close();
		//Second, read the service dataset 2012
		br = new BufferedReader(new FileReader(URLs.mashupsFile2012));
		creader = new CsvReader(br, ',');
		creader.skipLine();
		while(creader.readRecord())
		{
			if(!allMashups.contains(creader.get(1).trim()))
			{
				if(creader.get(4).trim().equals(""))continue;
				allMashups.add(creader.get(1).trim());
				csvwriter.writeRecord(new String[]{creader.get(1), creader.get(2), creader.get(4), "", creader.get(3)});
			}
		}
		creader.close();
		csvwriter.close();
		//Third, read the service dataset 2013
//		br = new BufferedReader(new FileReader(URLs.apisFile2013));
//		creader = new CsvReader(br, ',');
//		creader.skipLine();
//		while(creader.readRecord())
//		{
//			if(!allMashups.contains(creader.get(0).trim()))
//			{
//				allMashups.add(creader.get(0).trim());
//				csvwriter.writeRecord(new String[]{creader.get(0), creader.get(3), creader.get(7), "", creader.get(3)});
//			}
//		}
	}
	
	/**
	 * Sort the map by Integer values
	 * @param map
	 * @return
	 */
	private Map<String, Integer> sortedByIntValue(final Map<String, Integer> map)
	{
		Comparator<String> valueComparator = new Comparator<String>() 
		{
			public int compare(String k1, String k2)
			{
				return map.get(k2) - map.get(k1) > 0 ? 1 : -1;
			}
		};
		Map<String, Integer> newMap = new TreeMap<String, Integer>(valueComparator);
		newMap.putAll(map);
		return newMap;
	}
	
}
