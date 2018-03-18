package min.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Test;
/**
 * description expansion at sentence level in two ways: similar services and composed Mashups
 * @author Min Shi
 *
 */
public class DescExpansion
{
	int An = 2; // total number of sentences extended from similar services
	int Mn = 2; // total number of sentences extended from composed Mashups
	double miu = 0.5; // variable to balance the sentence similarity and the description similarity
	Map<Integer, String[]> serviceSentences;
	Map<Integer, String[]> mashupSentences;
	Map<Integer, List<double[]>> delta_service, delta_mashup;
	Map<Integer, double[]> theta_service, theta_mashup;
	
	Map<Integer, List<Integer>> compositionNets; // the composition network between services and mashups
	
	@Test
	public void statistic()
	{
		
	}
	
//	@Test
	// expand from similar services
	public void expandFromSimilarService() throws Exception
	{
		loadFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.expansionfromservice));
		int currId = -1;
		List<double[]> senTopics = null;
		double[] descTopics = null;
		StringBuffer sb = null;
		String[] expansions;
		for(Entry<Integer, String[]> sentry : serviceSentences.entrySet())
		{
			System.out.println(sentry.getKey());
			sb = new StringBuffer();
			for(String origin : sentry.getValue())
			{
				sb.append(origin + "    ");
			}
			currId = sentry.getKey();
			descTopics = theta_service.get(currId);// topic distribution of current description
			senTopics = delta_service.get(currId); // topic distributions of all its sentences
			for(int i = 0; i < sentry.getValue().length; i++)
			{
				expansions = expandfrom(currId, descTopics, senTopics.get(i), "fromservice");
//				System.out.println("Target: " + sentry.getValue()[i] + "  " + senTopics.get(i).length + " " + expansions.length);
//				for(String sen : expansions) System.out.println(sen);
				for(String expansion : expansions)
				{
					sb.append(expansion + "    ");
				}
			}
			bw.write(sb.toString().trim());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	@Test
	// expand from composed Mashups
	public void expandFromComposedMashup() throws Exception
	{
		loadFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.expansionfrommashup));
		int currId = -1;
		List<double[]> senTopics = null;
		double[] descTopics = null;
		StringBuffer sb = null;
		for(Entry<Integer, String[]> sentry : serviceSentences.entrySet())
		{
			System.out.println(sentry.getKey());
			sb = new StringBuffer();
			for(String origin : sentry.getValue())
			{
				sb.append(origin + "    ");
			}
			currId = sentry.getKey();
			descTopics = theta_service.get(currId);// topic distribution of current description
			senTopics = delta_service.get(currId); // topic distributions of all its sentences
			for(int i = 0; i < sentry.getValue().length; i++)
			{
				String[] expansions = expandfrom(currId, descTopics, senTopics.get(i), "frommashup");
//				System.out.println("Target: " + sentry.getValue()[i]);
//				for(String sen : expansions) System.out.println(sen);
				for(String expansion : expansions)
				{
					sb.append(expansion + "    ");
				}
			}
			bw.write(sb.toString().trim());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	@Test
	// expand from both service and composed Mashups
	public void expandFromServiceandComposedMashup() throws Exception
	{
		loadFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(URLs.expansionfromserviceandmashup));
		int currId = -1;
		List<double[]> senTopics = null;
		double[] descTopics = null;
		StringBuffer sb = null;
		for(Entry<Integer, String[]> sentry : serviceSentences.entrySet())
		{
			System.out.println(sentry.getKey());
			sb = new StringBuffer();
			for(String origin : sentry.getValue())
			{
				sb.append(origin + "    ");
			}
			currId = sentry.getKey();
			descTopics = theta_service.get(currId);// topic distribution of current description
			senTopics = delta_service.get(currId); // topic distributions of all its sentences
			for(int i = 0; i < sentry.getValue().length; i++)
			{
				String[] expansions = expandfrom(currId, descTopics, senTopics.get(i), "fromserviceandmashup");
//				System.out.println("Target: " + sentry.getValue()[i]);
//				for(String sen : expansions) System.out.println(sen);
				for(String expansion : expansions)
				{
					sb.append(expansion + "    ");
				}
			}
			bw.write(sb.toString().trim());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	/**
	 * 
	 * @param targetId id of the expanded description
	 * @param descTopics the topic distribution of this description
	 * @param senTopics topic distribution of the current sentence of this description
	 * @param flag expand from where (service, mashup or both)
	 * @return
	 */
	private String[] expandfrom(int targetId, double[] descTopics, double[] senTopics, String flag)
	{
		Map<Integer, String[]> composedMashups = new HashMap<Integer, String[]>();
		List<Integer> composedMashupIds = compositionNets.get(targetId);
		String[] results = null;
		if(flag.equals("fromservice"))
		{
			results = expand(targetId, descTopics, senTopics, An, serviceSentences, theta_service, delta_service, "service");
		}
		else if(flag.equals("frommashup"))
		{
			if(composedMashupIds != null) 
			{
				for(int mId : composedMashupIds) composedMashups.put(mId, mashupSentences.get(mId));
				results = expand(targetId, descTopics, senTopics, Mn, composedMashups, theta_mashup, delta_mashup, "mashup");
			}
			else
			{
				results = new String[]{};
			}
		}
		else if(flag.equals("fromserviceandmashup"))// expand from both services and mashups
		{
			String[] result_service = expand(targetId, descTopics, senTopics, An, serviceSentences, theta_service, delta_service, "service");
			String[] result_mashup = null;
			
			if(composedMashupIds != null) 
			{
				for(int mId : composedMashupIds) composedMashups.put(mId, mashupSentences.get(mId));
				result_mashup = expand(targetId, descTopics, senTopics, Mn, composedMashups, theta_mashup, delta_mashup, "mashup");
			}
			else
			{
				result_mashup = new String[]{};
			}
			
			results = new String[result_service.length + result_mashup.length];
			int index = 0;
			for(String sen : result_service) results[index++] = sen;
			for(String sen : result_mashup) results[index++] = sen;
		}
		return results;
	}
	private String[] expand(int targetId, double[] descTopics, double[] senTopics, int topN, Map<Integer, String[]> source, Map<Integer, double[]> theta, Map<Integer, List<double[]>> delta, String flag)
	{
		
		int currId = -1;
		double descSimilarity = 0, senSimilarity = 0, globalSimilarity;
		double[] currDescTopics = null;
		List<double[]> currSenTopics = null;
		Map<String, Double> similarSentences = new TreeMap<String, Double>();
		for(Entry<Integer, String[]> sentry : source.entrySet())
		{
			currId = sentry.getKey();
			currDescTopics = theta.get(currId);
			currSenTopics = delta.get(currId);
			if(targetId != currId || flag.equals("mashup"))
			{
				descSimilarity = SimilarityMeasure.JSDivergence(descTopics, currDescTopics); // similarity at description level
				
				for(int i = 0; i < sentry.getValue().length; i++)
				{
					double[] currSenTopic = currSenTopics.get(i);
					String currSenText= sentry.getValue()[i];
					
					senSimilarity = SimilarityMeasure.JSDivergence(senTopics, currSenTopic); // similarity at sentence level
					globalSimilarity = miu * senSimilarity + (1 - miu) * descSimilarity;
					similarSentences.put(currSenText, globalSimilarity);
				}
				
			}
		}
		similarSentences = sortedByValue(similarSentences);
//		System.out.println(similarSentences.values());
		int n = 0;
		String[] results = new String[topN];
		if(similarSentences.size() < topN) results = new String[similarSentences.size()];
		for(String sen : similarSentences.keySet())
		{
			results[n++] = sen;
			if(n >= topN) break;
		}
		return results;
	}
	
	// sort by value
	private Map<String, Double> sortedByValue(final Map<String, Double> map)
	{
		Comparator<String> valueComparator = new Comparator<String>() 
		{
			public int compare(String k1, String k2)
			{
				return map.get(k2) - map.get(k1) >= 0 ? -1 : 1;
			}
		};
		Map<String, Double> newMap = new TreeMap<String, Double>(valueComparator);
		newMap.putAll(map);
		return newMap;
	}
//	@Test
	// api_11=navvi navvi creat    public document account    navvi indoor map indoor navig
	public void loadFile() throws Exception
	{
		serviceSentences = new HashMap<Integer, String[]>();
		mashupSentences = new HashMap<Integer, String[]>();
		delta_service = new HashMap<Integer, List<double[]>>();
		delta_mashup = new HashMap<Integer, List<double[]>>();
		theta_service = new HashMap<Integer, double[]>();
		theta_mashup = new HashMap<Integer, double[]>();
		compositionNets = new HashMap<Integer, List<Integer>>();
		
		// load the service and Mashup sentences
		BufferedReader br = new BufferedReader(new FileReader(URLs.sreviceMashupSentenceToken));
		BufferedReader brTopic = new BufferedReader(new FileReader(URLs.modelSavePath+"\\service_delta.txt"));
		BufferedReader brTheta = new BufferedReader(new FileReader(URLs.modelSavePath+"\\service_theta.txt"));
		String originaLine = "", line = "";
		String[] params = null;
		int id = -1, thetaSCount = 0, thetaMCount = 0;
		while((originaLine = br.readLine()) != null)
		{
			params = originaLine.split("=");
			id = Integer.parseInt(params[0].split("\\_")[1]);
			if(params.length < 2) line = "";
			else line = params[1];
			String[] sens = line.split("    ");
			List<double[]> block = new ArrayList<double[]>();
			for(int i = 0; i < sens.length; i++)
			{
				line = brTopic.readLine();
				block.add(converttoArray(line));
			}
			if(params[0].split("\\_")[0].equals("api"))
			{
				serviceSentences.put(id, sens);
				delta_service.put(id, block);
				theta_service.put(thetaSCount++, converttoArray(brTheta.readLine()));
			}
			else
			{
				mashupSentences.put(id, sens);
				delta_mashup.put(id, block);
				theta_mashup.put(thetaMCount++, converttoArray(brTheta.readLine()));
			}
		}
		brTopic.close();
		br.close();
		brTheta.close();
		
		// load the composition networks
		br = new BufferedReader(new FileReader(URLs.apisHostMashups));
		while((line = br.readLine()) != null)
		{
			int serviceId = Integer.parseInt(line.split("    ")[0]);
			params = line.split("    ")[1].split(" ");
			List<Integer> midlist = new ArrayList<Integer>();
			for(String mashupId : params) midlist.add(Integer.parseInt(mashupId));
			compositionNets.put(serviceId, midlist);
		}
	}
	
	private double[] converttoArray(String dist)
	{
		String[] temp = dist.split(" ");
		double[] probs = new double[temp.length];
		int index = 0;
		for(String prob : temp)
		{
			probs[index++] = Double.parseDouble(prob);
		}
		return probs;
	}
}
