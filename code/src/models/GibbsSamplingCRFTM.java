package models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import utility.FuncUtils;

public class GibbsSamplingCRFTM
{
	public ArrayList<List<String>> allneighbor =new ArrayList<List<String>>(); //Word correlation edges
	
	public double alpha; // Hyper-parameter alpha
	public double beta; // Hyper-parameter beta
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int topWords; // Number of most probable words for each topic

	public double alphaSum; // alpha * numTopics
	public double betaSum; // beta * vocabularySize

	public List<List<Integer>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for words
													// in the corpus
	public int numDocuments; // Number of documents in the corpus
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
														// given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
														// given an ID
	public int vocabularySize; // The number of word types in the corpus

	// numDocuments * numTopics matrix
	// Given a document: number of its words assigned to each topic
	public int[][] docTopicCount;
	// Number of words in every document
	public int[] sumDocTopicCount;
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;

	// Double array used to sample a topic
	public double[] multiPros;

	// Path to the directory containing the corpus
	public String folderPath;
	// Path to the topic modeling corpus
	public String corpusPath;
	// Path to the file of word correlations
	public String pathToNeighbor;

	public String expName = "CRFTMmodel";
	public String orgExpName = "CRFTMmodel";
	public String tAssignsFilePath = "";
	public int savestep = 0;

	public GibbsSamplingCRFTM(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, "CRFTMmodel");
	}

	public GibbsSamplingCRFTM(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, "", 0);
	}

	public GibbsSamplingCRFTM(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToTAfile)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, pathToTAfile, 0);
	}

	public GibbsSamplingCRFTM(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToCorrelation, int inSaveStep)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, pathToCorrelation, "", inSaveStep);
	}

	public GibbsSamplingCRFTM(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToCorrelation ,String pathToTAfile, int inSaveStep)
		throws Exception
	{

		alpha = inAlpha;
		beta = inBeta;
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		topWords = inTopWords;
		savestep = inSaveStep;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		folderPath = pathToCorpus.substring(
			0,
			Math.max(pathToCorpus.lastIndexOf("/"),
				pathToCorpus.lastIndexOf("\\")) + 1);
		System.out.println("Reading topic modeling corpus: " + pathToCorpus);

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		corpus = new ArrayList<List<Integer>>();
		numDocuments = 0;
		numWordsInCorpus = 0;

		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToCorpus));
			for (String doc; (doc = br.readLine()) != null;) {

				if (doc.trim().length() == 0)
					continue;

				String[] words = doc.trim().split("\\s+");
				List<Integer> document = new ArrayList<Integer>();

				for (String word : words) {
					if (word2IdVocabulary.containsKey(word)) {
						document.add(word2IdVocabulary.get(word));
					}
					else {
						indexWord += 1;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord, word);
						document.add(indexWord);
					}
				}

				numDocuments++;
				numWordsInCorpus += document.size();
				corpus.add(document);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		docTopicCount = new int[numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[numDocuments];
		sumTopicWordCount = new int[numTopics];

		 pathToNeighbor = pathToCorrelation;
		BufferedReader nbr= null;
		try {
			nbr= new BufferedReader(new FileReader(pathToNeighbor));
			for (String doc; (doc = nbr.readLine()) != null;) {
				if (doc.trim().length() == 0)
					continue;
				String[] words = doc.trim().split("\\s+");	
				List<String> neighbor = new ArrayList<String>();
				for (String word : words) {
					neighbor.add(word);
				}
				allneighbor.add(neighbor);
			}	
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
		




		multiPros = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

		alphaSum = numTopics * alpha;
		betaSum = vocabularySize * beta;

		System.out.println("Corpus size: " + numDocuments + " docs, "
			+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + topWords);

		tAssignsFilePath = pathToTAfile;
		if (tAssignsFilePath.length() > 0)
			initialize(tAssignsFilePath);
		else
			initialize();	
	}
	
	public double[] countNeiPro(String s, int docnum)
	{
		String[] neis = s.split("\\|");	
		double[] neiiPros = new double[numTopics];
		for(String nei : neis)
		{
			neiiPros[topicAssignments.get(docnum).get(Integer.parseInt(nei))]++;
		}
		double asum = 0;
		for(int i = 0; i < numTopics; i++)
		{
			asum += neiiPros[i];
		}
		for(int i = 0; i < numTopics; i++)
		{
			neiiPros[i] /= asum;
		}
		return(neiiPros);
	}

	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize()
		throws IOException
	{
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();

		for (int i = 0; i < numDocuments; i++) {
			List<Integer> topics = new ArrayList<Integer>();
			int docSize = corpus.get(i).size();
			for (int j = 0; j < docSize; j++) {
				int topic = FuncUtils.nextDiscrete(multiPros); // Sample a topic
				// Increase counts
				docTopicCount[i][topic] += 1;
				topicWordCount[topic][corpus.get(i).get(j)] += 1;
				sumDocTopicCount[i] += 1;
				sumTopicWordCount[topic] += 1;

				topics.add(topic);
			}
			topicAssignments.add(topics);
		}
	}

	/**
	 * Initialize topic assignments from a given file
	 */
	public void initialize(String pathToTopicAssignmentFile)
	{

	}

	public void inference()
		throws IOException
	{
		writeParameters();
		writeDictionary();

		System.out.println("Running Gibbs sampling inference: ");

		for (int iter = 1; iter <= numIterations; iter++) {

			System.out.println("\tSampling iteration: " + (iter));
			// System.out.println("\t\tPerplexity: " + computePerplexity());

			sampleInSingleIteration();

			if ((savestep > 0) && (iter % savestep == 0)
				&& (iter < numIterations)) {
				System.out.println("\t\tSaving the output from the " + iter
					+ "^{th} sample");
				expName = orgExpName + "-" + iter;
				write();
			}
		}
		expName = orgExpName;

		System.out.println("Writing output from the last sample ...");
		write();

		System.out.println("Sampling completed!");

	}

	public void sampleInSingleIteration()
	{
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int topic = topicAssignments.get(dIndex).get(wIndex);
				int word = corpus.get(dIndex).get(wIndex);

				// Decrease counts
				docTopicCount[dIndex][topic] -= 1;
				// docTopicSum[dIndex] -= 1;
				topicWordCount[topic][word] -= 1;
				sumTopicWordCount[topic] -= 1;

				String  neis = allneighbor.get(dIndex).get(wIndex);
		
				if(neis.equals("0"))
				{
					// Sample a topic
					for (int tIndex = 0; tIndex < numTopics; tIndex++) {
						multiPros[tIndex] = (docTopicCount[dIndex][tIndex] + alpha)
							* ((topicWordCount[tIndex][word] + beta) / (sumTopicWordCount[tIndex] + betaSum));
						// multiPros[tIndex] = ((docTopicCount[dIndex][tIndex] +
						// alpha) /
						// (docTopicSum[dIndex] + alphaSum))
						// * ((topicWordCount[tIndex][word] + beta) /
						// (topicWordSum[tIndex] + betaSum));
					}
				}
				else
				{
					double[] neiPro = countNeiPro(neis,dIndex );
					for (int tIndex = 0; tIndex < numTopics; tIndex++)
					{
						multiPros[tIndex] = (docTopicCount[dIndex][tIndex] + alpha)
							* ((topicWordCount[tIndex][word] + beta) / (sumTopicWordCount[tIndex] + betaSum)) * Math.exp( neiPro[tIndex]);
					}
				}
			

				topic = FuncUtils.nextDiscrete(multiPros);

				// Increase counts
				docTopicCount[dIndex][topic] += 1;
				// docTopicSum[dIndex] += 1;
				topicWordCount[topic][word] += 1;
				sumTopicWordCount[topic] += 1;

				// Update topic assignments
				topicAssignments.get(dIndex).set(wIndex, topic);
			}
		}
	}

	public void writeParameters()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".paras"));
		writer.write("-model" + "\t" + "CRFTM");
		writer.write("\n-corpus" + "\t" + corpusPath);
		writer.write("\n-ntopics" + "\t" + numTopics);
		writer.write("\n-alpha" + "\t" + alpha);
		writer.write("\n-beta" + "\t" + beta);
		writer.write("\n-niters" + "\t" + numIterations);
		writer.write("\n-twords" + "\t" + topWords);
		writer.write("\n-name" + "\t" + expName);
		if (tAssignsFilePath.length() > 0)
			writer.write("\n-initFile" + "\t" + tAssignsFilePath);
		if (savestep > 0)
			writer.write("\n-sstep" + "\t" + savestep);

		writer.close();
	}

	public void writeDictionary()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".vocabulary"));
		for (int id = 0; id < vocabularySize; id++)
			writer.write(id2WordVocabulary.get(id) + " " + id + "\n");
		writer.close();
	}

	public void writeIDbasedCorpus()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".IDcorpus"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(corpus.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicAssignments()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topicAssignments"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(topicAssignments.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopTopicalWords()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topWords"));

		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			writer.write("Topic" + new Integer(tIndex) + ":");

			Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				wordCount.put(wIndex, topicWordCount[tIndex][wIndex]);
			}
			wordCount = FuncUtils.sortByValueDescending(wordCount);

			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < topWords) {
					double pro = (topicWordCount[tIndex][index] + beta)
						/ (sumTopicWordCount[tIndex] + betaSum);
					pro = Math.round(pro * 1000000.0) / 1000000.0;
					writer.write(" " + id2WordVocabulary.get(index) + "(" + pro
						+ ")");
					count += 1;
				}
				else {
					writer.write("\n\n");
					break;
				}
			}
		}
		writer.close();
	}

	public void writeTopicWordPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".phi"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (topicWordCount[i][j] + beta)
					/ (sumTopicWordCount[i] + betaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicWordCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".WTcount"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				writer.write(topicWordCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();

	}

	public void writeDocTopicPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".theta"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				double pro = (docTopicCount[i][j] + alpha)
					/ (sumDocTopicCount[i] + alphaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeDocTopicCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".DTcount"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				writer.write(docTopicCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void write()
		throws IOException
	{
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
	}

	public static void main(String args[])
		throws Exception
	{
		double beta = 0.01;
		int num_iter = 10;
		int topic_words = 20;
		String corpus_path = "psdsample.txt" ;
		String correlation_path = "0301_psdsample_corrlations.txt";
		for(int k = 40; k <=40; k+=20)
		{
			double alpha = k / 50.0;
			for(int round = 1; round <= 1; round++)
			{
				GibbsSamplingCRFTM CRFTM = new GibbsSamplingCRFTM(corpus_path, k,  alpha,
					beta, num_iter, topic_words, round + "_" + k + "_sample", correlation_path);
				CRFTM.inference();
			}
		}
	}
}
