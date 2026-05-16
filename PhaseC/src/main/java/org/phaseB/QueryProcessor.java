package org.phaseB;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.phaseA.VocabularyEntry;
import javax.swing.JTextArea;
import mitos.stemmer.Stemmer;

public class QueryProcessor {

    private static Set<String> stopwordsEn;
    private static Set<String> stopwordsGr;
    private final JTextArea outputArea;

    //private static final String VOCAB_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex\\merged-vocabulary-4.txt";
    //private static final String POSTING_FILE = System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex\\merged-posting-4.txt";

    public static String VOCAB_FILE;
    public static String POSTING_FILE;


    public QueryProcessor(){
        this.outputArea = null;
    }
    public QueryProcessor(String vocab, String posting) throws IOException {

        VOCAB_FILE = vocab;
        POSTING_FILE = posting;
        this.outputArea = null;
        loadStopwords();

    }
    public QueryProcessor(JTextArea outputArea) {
        this.outputArea = outputArea;
    }


    /**
     * this function is used to search for the query's terms on the vocabulary file<br>
     * if the term is present, retrieve the posting data using the pointer to the posting file<br>
     * compute the score and display the top 15 results
     * @param query the query to be searched
     */
    public List<Map.Entry<String, Double>> processQuery(String query) throws Exception {

        String[] terms = query.toLowerCase().split("\\s+");
        Map<String, List<PostingEntry>> termPostings = new HashMap<>();
        RandomAccessFile postingRAF = new RandomAccessFile(POSTING_FILE, "r");

        BufferedReader reader = new BufferedReader(new FileReader(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex\\DocumentsFile.txt"));
        //BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\anton\\OneDrive\\Desktop\\University\\HY-463\\Project\\hy463_project\\src\\resources\\CollectionIndex\\DocumentsFile.txt"));
        int lines = 0;
        while (reader.readLine() != null) lines++;
        reader.close();
        //System.out.println("no docs: " + lines);

        for(String term : terms){

            //System.out.println("query term before: " + term);
            term = term.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
            if(stopwordsEn.contains(term) || stopwordsGr.contains(term)) continue;
            term = Stemmer.Stem(term);
            //System.out.println("query term after: " + term);

            VocabularyEntry vocab = Utils.getVocabEntry(term, VOCAB_FILE);
            if (vocab == null) continue;

            postingRAF.seek(vocab.postingFilePointer);
            List<PostingEntry> postings = new ArrayList<>();
            for(int i = 0; i < vocab.df; i++){

                String line = postingRAF.readLine();
                String[] parts = line.split(", ", 4);
                String pmcid = parts[0];
                int tf = Integer.parseInt(parts[1]);
                double idf = (Math.log((double) lines / vocab.df) / Math.log(2));
                //System.out.println("idf: " + idf);
                //System.out.println("tf: " + tf);
                postings.add(new PostingEntry(pmcid, tf, idf));

            }
            termPostings.put(term, postings);

        }

        postingRAF.close();

        // score each document
        Map<String, Double> docScores = new HashMap<>();
        for(String term : termPostings.keySet()){
            for(PostingEntry entry : termPostings.get(term)){

                docScores.put(entry.pmcid, entry.tf*entry.idf);

            }
        }

        if(docScores.isEmpty()){

            System.out.println("No results found.");
            return null;

        }

        // sort by score
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(docScores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int maxResults = Math.min(15, sorted.size());

        // output results
        /*System.out.println("Results:");
        for (Map.Entry<String, Double> entry : sorted) {
            System.out.println("Doc: " + entry.getKey() + " | Score: " + entry.getValue());
        }*/

        List<Map.Entry<String, Double>> fifteenSorted = new ArrayList<>();

        //outputArea.append("Top " + maxResults + " Results:\n");
        //System.out.println("Top " + maxResults + " Results:\n");
        for(int i = 0; i < maxResults; i++){

            Map.Entry<String, Double> entry = sorted.get(i);
            //outputArea.append((i + 1) + ". Doc: " + entry.getKey() + " | Score: " + entry.getValue() + "\n");
            //System.out.println((i + 1) + ". Doc: " + entry.getKey() + " | Score: " + entry.getValue());
            fifteenSorted.add(i, sorted.get(i));
            //System.out.println(fi);

        }

        return sorted;

    }

    public static void loadStopwords() throws IOException{

        stopwordsEn = new HashSet<>(Files.readAllLines(new File(System.getProperty("user.dir") + "\\src\\main\\resources\\stopwordsEn.txt").toPath(), StandardCharsets.UTF_8));
        stopwordsGr = new HashSet<>(Files.readAllLines(new File(System.getProperty("user.dir") + "\\src\\main\\resources\\stopwordsGr.txt").toPath(), StandardCharsets.UTF_8));

    }

    private static class PostingEntry {
        String pmcid;
        int tf;
        double idf;

        public PostingEntry(String pmcid, int tf, double idf) {
            this.pmcid = pmcid;
            this.tf = tf;
            this.idf = idf;
        }
    }
}
