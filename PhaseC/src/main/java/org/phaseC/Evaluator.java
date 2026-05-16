package org.phaseC;

import org.phaseB.QueryProcessor;
import gr.uoc.csd.hy463.Topic;
import gr.uoc.csd.hy463.TopicsReader;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Evaluator {

    private static final String TOPICS_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\topics.xml";
    private static final String QRELS_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\qrels.txt";

    /**
     *  make sure you have the posting and vocabulary file in the CollectionIndex directory located in the resources directory,
     *  and they are named as such: "posting_file.txt" and "vocabulary.txt" respectively
     *              <br><br>
     *  also make sure you run the .jar file from inside the hy463_project_v2 directory and not from anywhere else as
     *  it uses a relative path to the files
     */
    private static final String POSTING_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex\\posting_file.txt";
    private static final String VOCAB_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex\\vocabulary.txt";

    private static final int CUTOFF_K = 100;

    /**
     * the main function where you can choose what to run by commenting the thing you don't want to run
     */
    public static void main(String[] args) throws Exception {

        List<Topic> topics = TopicsReader.readTopics(TOPICS_FILE);
        System.out.println("\n" + System.getProperty("user.dir") + "\n");

        /* comment this to only run the creation of eval_results.txt functionality */
        createResultsFile(topics);

        File tsv = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\results.txt");
        ArrayList<String[]> res = tsvReader(tsv);
        List<String> retrievedDocs = new ArrayList<>();

        for(String[] line : res){
            retrievedDocs.add(line[2]);
        }

        /* comment this to only run the creation of results.txt functionality */
        createEvalResultsFile(topics, retrievedDocs);

    }

    /**
     * a simple tab separated values file parser
     * @param tsvFile the file to be parsed
     * @return the parsed data of the tsv file
     */
    public static ArrayList<String[]> tsvReader(File tsvFile) throws IOException {

        ArrayList<String[]> data = new ArrayList<>();
        BufferedReader TSVReader = new BufferedReader(new FileReader(tsvFile));

        String line;
        while((line = TSVReader.readLine()) != null){

            String[] lineItems = line.split("\t");
            /* only add the line if it has all 4 components */
            if(lineItems.length == 4)
                data.add(lineItems);

        }

        return data;

    }

    /**
     * this function is used for creating the results.txt file
     * @param topics the topics as they are parsed from the topics.xml file
     * @throws Exception
     */
    public static void createResultsFile(List<Topic> topics) throws Exception {

        File resultsFile = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\results.txt");
        resultsFile.createNewFile();
        FileWriter resultsWriter = new FileWriter(resultsFile);

        for(Topic topic : topics){

            int topicId = topic.getNumber();
            /* change this to .getDescription() to query the whole description instead */
            String queryText = topic.getSummary();

            QueryProcessor processor = new QueryProcessor(VOCAB_FILE, POSTING_FILE);
            List<Map.Entry<String, Double>> retrievedDocsScores = processor.processQuery(queryText);

            for (Map.Entry<String, Double> entry : retrievedDocsScores) {

                //System.out.println(topicId + ". Doc: " + entry.getKey() + " | Score: " + entry.getValue());
                resultsWriter.write(topicId + "\t0\t" + entry.getKey() + "\t" + entry.getValue() + "\n");

            }

        }

        resultsWriter.close();

    }

    /**
     * this function is used for creating the eval_results.txt file
     * @param topics the topics as they are parsed from the topics.xml file
     * @param retrievedDocs the pmcids of the retrieved documents
     * @throws IOException
     */
    public static void createEvalResultsFile(List<Topic> topics, List<String> retrievedDocs) throws IOException {

        File evalResultsFile = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\eval_results.txt");
        evalResultsFile.createNewFile();
        FileWriter evalResultsWriter = new FileWriter(evalResultsFile, false);
        Map<Integer, Set<String>> relevantDocs = loadRelevant(QRELS_FILE);
        Map<Integer, Set<String>> notRelevantDocs = loadNotRelevant(QRELS_FILE);

        for(Topic topic : topics){

            int topicId = topic.getNumber();
            Set<String> relevant = relevantDocs.getOrDefault(topicId, new HashSet<>());
            Set<String> notRelevant = notRelevantDocs.getOrDefault(topicId, new HashSet<>());

            double bprefValue = bpref(retrievedDocs, relevant, notRelevant);
            double avepValue = averagePrecisionPrime(retrievedDocs, relevant);
            double ndcgValue = ndcgPrime(retrievedDocs, relevant, CUTOFF_K);

            evalResultsWriter.write(topicId + "\t" + bprefValue + "\t" + avepValue + "\t" + ndcgValue + "\n");

            System.out.println("--- Topic " + topicId + " ---");
            System.out.println("bpref: " + bprefValue);
            System.out.println("AveP': " + avepValue);
            System.out.println("nDCG': " + ndcgValue);
            System.out.println();

        }

        evalResultsWriter.close();

    }

    /**
     * this function is used to load the relevant documents of each topic from the qrels.txt file
     * @param path the path to the qrels.txt file
     * @return a map mapping the topic id to the set of relevant documents
     * @throws IOException
     */
    private static Map<Integer, Set<String>> loadRelevant(String path) throws IOException {

        Map<Integer, Set<String>> qrels = new HashMap<>();
        for(String line : Files.readAllLines(Paths.get(path))){

            String[] parts = line.split("\\s+");
            int topicId = Integer.parseInt(parts[0]);
            String docId = parts[2];
            int relevance = Integer.parseInt(parts[3]);
            if(relevance > 0)
                qrels.computeIfAbsent(topicId, k -> new HashSet<>()).add(docId);

        }

        /*for(Map.Entry<Integer, Set<String>> entry : qrels.entrySet()){

            System.out.println("Topic: " + entry.getKey());
            for(String doc : entry.getValue()){
                System.out.println("\t" + doc);
            }

        }*/

        return qrels;

    }

    /**
     * this function is used to load the irrelevant documents of each topic from the qrels.txt file
     * @param path the path to the qrels.txt file
     * @return a map mapping the topic id to the set of irrelevant documents
     * @throws IOException
     */
    private static Map<Integer, Set<String>> loadNotRelevant(String path) throws IOException {

        Map<Integer, Set<String>> qrels = new HashMap<>();
        for(String line : Files.readAllLines(Paths.get(path))){

            String[] parts = line.split("\\s+");
            int topicId = Integer.parseInt(parts[0]);
            String docId = parts[2];
            int relevance = Integer.parseInt(parts[3]);
            if(relevance == 0)
                qrels.computeIfAbsent(topicId, k -> new HashSet<>()).add(docId);

        }

        return qrels;

    }

    /**
     * this function is used to compute the bpref evaluation metric<br>
     * bpref = 1/R * Sum(1 - (n ranked higher than r) / Min(R, N))
     * @param retrieved the list of documents retrieved
     * @param relevant the set of relevant documents
     * @param nonRelevant the set of non-relevant documents
     * @return the bpref score as calculated by the formula above
     */
    public static double bpref(List<String> retrieved, Set<String> relevant, Set<String> nonRelevant){

        if(relevant.isEmpty()) return 0.0;
        double score = 0.0;

        for(String rel : relevant){

            int nonRelAbove = 0;
            for(String doc : retrieved){
                if(doc.equals(rel)) break;
                if(nonRelevant.contains(doc)) nonRelAbove++;
            }
            score += 1.0 - ((double) nonRelAbove / Math.min(relevant.size(), nonRelevant.size()));

        }

        return score / relevant.size();

    }

    /**
     * this function is used to compute the AveP' evaluation metric<br>
     * AveP' = 1/R * Sum_k=1^R((hits / k) * rel(k))<br>
     * so we actually only count the document when it is judged
     * @param retrieved the list of the retrieved documents
     * @param relevant the set of the relevant documents
     * @return the AveP' score as calculated by the formula above
     */
    public static double averagePrecisionPrime(List<String> retrieved, Set<String> relevant){

        if(relevant.isEmpty()) return 0.0;
        double sum = 0.0;
        int hits = 0;

        for(int i = 0; i < retrieved.size(); i++){

            String doc = retrieved.get(i);
            if(relevant.contains(doc)){
                hits++;
                sum += (double) hits / (i + 1);
            }

        }

        return sum / relevant.size();

    }

    /**
     * this function is used to compute the ndcg' evaluation metric<br>
     * ndcg' =
     * @param retrieved the list of the retrieved documents
     * @param relevant the set of the relevant documents
     * @param k cutoff threshold
     * @return
     */
    private static double ndcgPrime(List<String> retrieved, Set<String> relevant, int k){

        double dcg = 0.0;
        for(int i = 0; i < Math.min(k, retrieved.size()); i++){

            if(relevant.contains(retrieved.get(i))){
                dcg += 1 / (Math.log(i + 2) / Math.log(2)); // log2(i+2)
            }

        }

        // ideal dcg
        double idcg = 0.0;
        int idealCount = Math.min(relevant.size(), k);
        for(int i = 0; i < idealCount; i++){
            idcg += 1 / (Math.log(i + 2) / Math.log(2));
        }

        if(idcg == 0.0)
            return 0.0;
        else
            return dcg / idcg;

    }

}
