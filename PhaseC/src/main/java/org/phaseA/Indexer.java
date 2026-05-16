package org.phaseA;

import gr.uoc.csd.hy463.NXMLFileReader;
import mitos.stemmer.Stemmer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;


public class Indexer{

    private static Set<String> stopwordsEn;
    private static Set<String> stopwordsGr;
    private static final Map<String, Map<File, InvertedInfo>> wordIndex = new TreeMap<>();
    private static final Map<String, DocumentInfo> documentIndex = new TreeMap<>();
    private static final Map<String, Long> documentPointers = new TreeMap<>();
    private static final TreeMap<String, VocabularyEntry> vocabulary = new TreeMap<>();

    private static long pointer = 0;
    private static File outputDirectory;
    public static int fileCount = 0;
    public static int currentFileWordCount = 0;
    private static final int DOCUMENT_THRESHOLD = 10;
    private static int documentCount = 0;
    private static int partialFilesCount = 0;
    private static final Queue<File> partialPostingFiles = new LinkedList<>();
    private static final Queue<File> partialVocabularyFiles = new LinkedList<>();
    //private static Queue<PartialIndexPair> partialFilesQueue = new LinkedList<>();
    private static int mergesNum = 0;

    //testing
    //private static File collectionFolder = new File(System.getProperty("user.dir") + "\\src\\resources\\CollectionIndex");
    //private static File outFile = new File(collectionFolder, "test.txt");


    /**
     * this main function was used for testing during development
     * @param args idk
     */
    public static void main(String[] args) throws IOException{

        File folder = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\MiniCollection");

        //File folder = new File(System.getProperty("user.dir") + "\\src\\resources\\onefile");
        //File folder = new File("C:\\Users\\anton\\Desktop\\MedicalCollection");

        Stemmer.Initialize();
        loadStopwords();

        readFiles(folder);
        writePartialPosting();
        writePartialVocabulary();
        createDocumentsFile();

//        FileWriter writer = new FileWriter(outFile);
//        for(String pmcid : documentPointers.keySet()){
//            writer.write(pmcid + "\t" + documentPointers.get(pmcid) + "\n");
//        }

        //partialFilesQueue = MergeIndex.buildPartialIndexQueue(System.getProperty("user.dir") + "\\src\\resources\\CollectionIndex");

        mergePartialFiles();
        //mergePartialFiles(8);

        System.out.println("PartialFiles: " + partialPostingFiles.size());


        //FileWriter writer = new FileWriter(outFile);
        //listFiles(folder, writer);

        /*File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\resources\\CollectionIndex");
        File documentsFile = new File(collectionIndex, "DocumentsFile.txt");
        RandomAccessFile documentsRAF = new RandomAccessFile(documentsFile, "r");
        documentsRAF.seek(documentPointers.get("130966"));
        System.out.println(documentsRAF.readLine());*/

    }

    /**
     * just a setter for outputDirectory
     * @param outDir the file containing the new directory
     */
    public static void setOutputDirectory(File outDir){ outputDirectory = outDir; }

    /**
     * this function is used to stem a word using the mitos stemmer
     * @param word the word to be stemmed
     * @return the stemmed word
     */
    public static String stemWord(String word){

        return Stemmer.Stem(word);

    }

    /**
     * this function was used for testing during development
     * @param folder the folder to be read
     * @param writer the writer of the file to be written
     */
    public static void listFiles(File folder, FileWriter writer) throws IOException {

        for(File fileEntry : Objects.requireNonNull(folder.listFiles())){

            if(fileEntry.isDirectory()){
                listFiles(fileEntry, writer);
            }else{

                writer.write(fileEntry.getName() + "\n");

            }

        }

    }


    /**
     * This function is used to recursively read all files from the folder specified
     * @param folder the name of the folder to be searched
     */
    public static void readFiles(File folder) throws IOException{

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        collectionIndex.mkdir();
        File documentsFile = new File(collectionIndex, "DocumentsFile.txt");
        RandomAccessFile documentsRAF = new RandomAccessFile(documentsFile, "rw");

        for(File fileEntry : Objects.requireNonNull(folder.listFiles())){

            if(fileEntry.isDirectory()) {
                System.out.println("Directory " + fileEntry.getName());
                readFiles(fileEntry);
            }else{
                fileCount++;
                documentCount++;
                if(documentCount >= DOCUMENT_THRESHOLD){

                    processFile(fileEntry, documentsRAF);
                    documentCount = 0;
                    writePartialPosting();
                    writePartialVocabulary();
                    wordIndex.clear();
                    vocabulary.clear();


                }else
                    processFile(fileEntry, documentsRAF);
            }
        }

    }

    /**
     * this function is used to create the vocabulary file which
     * contains each unique word along with its document frequency<br>
     * == was used before the partial indexing phase ==
     */
    public static void writeVocabularyFile() throws IOException {

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        File vocabularyFile = new File(collectionIndex, "VocabularyFile.txt");
        FileWriter writer = new FileWriter(vocabularyFile);

        for(Map.Entry<String, VocabularyEntry> entry : vocabulary.entrySet()) {
            writer.write(entry.getKey() + ", " +
                    entry.getValue().getDocumentFrequency() + ", " +
                    entry.getValue().getPostingFilePointer() + "\n");
        }

        writer.close();
        System.out.println("Successfully written VocabularyFile.txt");

    }

    /**
     * this function is used to create the documents file which
     * contains each unique file along with:
     * 1) its pmcid
     * 2) its complete path
     * 3) its vector's length
     */
    public static void createDocumentsFile() throws IOException {

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        if(collectionIndex.mkdir())
            System.out.println("Directory created: " + collectionIndex.getName());

        File documentsFile = new File(collectionIndex, "DocumentsFile.txt");
        if(documentsFile.createNewFile())
            System.out.println("File created: " + documentsFile.getName());

        try{

            FileWriter writer = new FileWriter(documentsFile);

            for(Map.Entry<String, DocumentInfo> entry : documentIndex.entrySet()){

                String pmcid = entry.getKey();
                DocumentInfo info = entry.getValue();
                writer.write(pmcid + ", " + info.getFilePath() + ", " + info.getVectorLength() + "\n");

            }

            writer.close();
            System.out.println("Successfully created the documents file.");

        }catch(IOException e){
            System.err.println("Error with FileWriter: " + e.getMessage());
        }

    }

    /**
     * this function is used to calculate the vector length of the file<br>
     * === initially built for calculating the vector length of each file but couldn't be used for
     * the large collection due to its heavy toll on resources ===<br>
     * TODO: find a way to calculate a file's vector length efficiently or at least approximate it
     * @param file the file for calculation
     * @return the vector length
     */
    public static double calculateVectorLength(File file){

        double sum = 0.0;

        for(Map.Entry<String, Map<File, InvertedInfo>> wordEntry : wordIndex.entrySet()){

            int totalTf = 0;

            for(Map.Entry<File, InvertedInfo> fileEntry : wordEntry.getValue().entrySet()){

                if(fileEntry.getKey().equals(file)){
                    totalTf += fileEntry.getValue().getTf();
                }
            }

            sum += totalTf * totalTf;

        }

        return Math.sqrt(sum);

    }


    /**
     * this function is used to print each different word along with<br>
     * 1) the file(s) it occurred in<br>
     * 2) the tag(s) of the file it occurred in<br>
     * 3) the number of occurrences in each tag<br>
     */
    public static void printWords() throws IOException{

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        if(collectionIndex.mkdir())
            System.out.println("Directory created: " + collectionIndex.getName());
        setOutputDirectory(collectionIndex);

        File outFile = new File(outputDirectory, "output.txt");
        if(outFile.createNewFile())
            System.out.println("File created: " + outFile.getName());

        try{

            FileWriter writer = new FileWriter(outFile);
            writer.write("Total unique words: " + wordIndex.size() + "\n");

            for(Map.Entry<String, Map<File, InvertedInfo>> wordEntry : wordIndex.entrySet()){

                String word = wordEntry.getKey();
                writer.write("Word: " + word + "\n");

                for(Map.Entry<File, InvertedInfo> fileEntry : wordEntry.getValue().entrySet()){

                    File file = fileEntry.getKey();
                    writer.write("  File: " + file.getName() + "\n");
                    writer.write("    Count: " + fileEntry.getValue().getTf() + "\n");

                }

                writer.write("\n");

            }

            writer.close();
            System.out.println("Successfully written to the output file.");

        }catch(IOException e){
            System.err.println("Error with FileWriter: " + e.getMessage());
        }

    }

    /**
     * this function is used to:<br>
     * 1) read all relevant tags of the specified nxml file<br>
     * 2) remove all punctuation<br>
     * 3) produce all different words<br>
     * @param file the name of the nxml file
     */
    public static void processFile(File file, RandomAccessFile documentsRAF) throws IOException{

        currentFileWordCount = 0;

        /* read all relevant tags of the specified filename */
        NXMLFileReader xmlFile = new NXMLFileReader(file);
        String pmcid = xmlFile.getPMCID();
        String title = xmlFile.getTitle();
        String abstr = xmlFile.getAbstr();
        String body = xmlFile.getBody();
        String journal = xmlFile.getJournal();
        String publisher = xmlFile.getPublisher();
        ArrayList<String> authors = xmlFile.getAuthors();
        HashSet<String> categories =xmlFile.getCategories();

        /* convert to lower case and remove punctuation for every tag */
        title = title.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
        abstr = abstr.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
        body = body.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
        journal = journal.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
        publisher = publisher.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", "");
        authors.replaceAll(s -> s.replaceAll("\\p{Punct}", ""));

        /* produce all different words and their tags */
        produceWords(file, "pmcid", pmcid);
        produceWords(file, "title", title);
        produceWords(file, "abstr", abstr);
        produceWords(file, "body", body);
        produceWords(file, "journal", journal);
        produceWords(file, "publisher", publisher);
        for(String author: authors)
            produceWords(file, "authors", author);
        for(String category: categories){
            produceWords(file, "categories", category);
        }

        //double vectorLength = calculateVectorLength(file);
        double vectorLength = 0;
        String docLine = pmcid + ", " + file.getPath() + ", " + (vectorLength / currentFileWordCount) + "\n";
        double vecLen = vectorLength / currentFileWordCount;
        long recLen = pmcid.length() + file.getPath().length() + Double.toString(vecLen).length() + 5;
        documentIndex.computeIfAbsent(pmcid, k -> new DocumentInfo(file.getPath(), vecLen, recLen));
        documentPointers.put(pmcid, pointer);
        pointer += recLen;

    }

    /**
     * this function is used to tokenize the tag parameter in order to produce each different word<br>
     * Stopwords are ignored
     * @param file the file that contains the tag
     * @param tagName the name of the tag to be tokenized
     * @param tagBody the contents of the tag to be tokenized
     */
    public static void produceWords(File file, String tagName, String tagBody){

        String delimiter = "\t\n\r\f ";

        StringTokenizer tokenizer = new StringTokenizer(tagBody, delimiter);
        while(tokenizer.hasMoreTokens()){

            String currentToken = tokenizer.nextToken();

            if(!isValidWord(currentToken)) continue;
            if(stopwordsEn.contains(currentToken) || stopwordsGr.contains(currentToken)) continue;

            currentFileWordCount++;
            currentToken = stemWord(currentToken);

            wordIndex.computeIfAbsent(currentToken, k -> new HashMap<>())   // word -> file map
                    .computeIfAbsent(file, f -> new InvertedInfo())  // file -> tag
                    .addPosition(currentFileWordCount);             // file -> frequency

            wordIndex.get(currentToken).get(file).incrementTf();

        }

    }

    /**
     * this function extracts the pmcid of a file through the name of it
     * @param file the file we want the pmcid for
     * @return the pmcid of the file
     */
    public static String extractPMCID(File file){ return file.getName().split("\\.")[0]; }

    /**
     * this function writes the partial posting files
     */
    public static void writePartialPosting() throws IOException {

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        if(collectionIndex.mkdir())
            System.out.println("Directory created: " + collectionIndex.getName());

        File postingFile = new File(collectionIndex, "posting-" + partialFilesCount++ + ".txt");
        RandomAccessFile raf = new RandomAccessFile(postingFile, "rw");
        raf.seek(postingFile.length()); // append mode

        for(Map.Entry<String, Map<File, InvertedInfo>> wordEntry : wordIndex.entrySet()){

            String word = wordEntry.getKey();
            Map<File, InvertedInfo> postings = wordEntry.getValue();

            // Save the pointer to the beginning of this word’s block in the posting file
            long wordPointer = raf.getFilePointer();

            for(Map.Entry<File, InvertedInfo> fileEntry : postings.entrySet()){

                File file = fileEntry.getKey();
                InvertedInfo info = fileEntry.getValue();
                String pmcid = extractPMCID(file);
                long docPointer = documentPointers.getOrDefault(pmcid, -1L);

                if(docPointer == -1L){
                    System.err.println("No document pointer found for: " + pmcid);
                    continue;
                }

                // Format: pmcid, tf, [pos1,pos2,...], docPointer
                StringBuilder line = new StringBuilder();
                line.append(pmcid).append(", ");
                line.append(info.getTf()).append(", ");

                StringBuilder positionsStr = new StringBuilder("[");
                List<Integer> positions = info.getPositions();
                for(int i = 0; i < positions.size(); i++){

                    positionsStr.append(positions.get(i));
                    if(i != positions.size() - 1){
                        positionsStr.append(",");
                    }

                }
                positionsStr.append("]");

                line.append(positionsStr).append(", ");
                line.append(docPointer).append("\n");

                raf.write(line.toString().getBytes(StandardCharsets.UTF_8));

            }

            // Store vocabulary entry: word, df, pointer
            vocabulary.put(word, new VocabularyEntry(postings.size(), wordPointer));

        }

        raf.close();

        partialPostingFiles.add(postingFile);

    }


    /**
     * this function writes the different partial vocabulary files
     */
    public static void writePartialVocabulary() throws IOException {

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        if(collectionIndex.mkdir())
            System.out.println("Directory created: " + collectionIndex.getName());

        int partialFileCount = partialFilesCount - 1;
        File vocabularyFile = new File(collectionIndex, "vocabulary-" + partialFileCount + ".txt");
        RandomAccessFile raf = new RandomAccessFile(vocabularyFile, "rw");
        raf.seek(vocabularyFile.length());

        for(Map.Entry<String, VocabularyEntry> entry : vocabulary.entrySet()){

            String word = entry.getKey();
            VocabularyEntry vocabEntry = entry.getValue();

            String line = word + ", " +
                    vocabEntry.getDocumentFrequency() + ", " +
                    vocabEntry.getPostingFilePointer() + "\n";

            raf.write(line.getBytes(StandardCharsets.UTF_8));

        }

        partialVocabularyFiles.add(vocabularyFile);

    }


    /**
     * this function writes all the current term's posting data into the new merged posting file
     * @param inputRaf the input RandomAccessFile
     * @param pointer the pointer to be sought in the input file
     * @param outputRaf the output RandomAccessFile
     * @param df the current term's document frequency (defines the block)
     */
    public static void copyPostingBlock(RandomAccessFile inputRaf, long pointer, RandomAccessFile outputRaf, int df) throws IOException {

        inputRaf.seek(pointer);
        for(int i = 0; i < df; i++){

            String postingLine = inputRaf.readLine();
            outputRaf.writeBytes(postingLine + "\n");

        }

    }


    /**
     * this function is used for the merging process of partial index files.
     * it merges a pair of partial files before deleting them<br>
     * then it adds the new merged partial file into the queue.
     */
    public static void mergePartialFiles() throws IOException {

        int length = (int) (partialVocabularyFiles.size() / 2);
        System.out.println("length: " + length);

        File collectionIndex = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");

        for(int i = 0; i < length; i++){

            File vocab1 = partialVocabularyFiles.remove();
            File vocab2 = partialVocabularyFiles.remove();
            File posting1 = partialPostingFiles.remove();
            File posting2 = partialPostingFiles.remove();

            BufferedReader reader1 = new BufferedReader(new FileReader(vocab1));
            BufferedReader reader2 = new BufferedReader(new FileReader(vocab2));
            RandomAccessFile raf1 = new RandomAccessFile(posting1, "r");
            RandomAccessFile raf2 = new RandomAccessFile(posting2, "r");

            File mergedVocab = new File(collectionIndex, "merged-vocabulary-" + mergesNum + ".txt");
            File mergedPosting = new File(collectionIndex, "merged-posting-" + mergesNum++ + ".txt");
            RandomAccessFile rafMergedVocab = new RandomAccessFile(mergedVocab, "rw");
            RandomAccessFile rafMergedPosting = new RandomAccessFile(mergedPosting, "rw");

            String currentLine1 = reader1.readLine();
            String currentLine2 = reader2.readLine();

            while(currentLine1 != null && currentLine2 != null){

                String[] tokens1 = currentLine1.split(", ");
                String[] tokens2 = currentLine2.split(", ");

                String word1 = tokens1[0];
                String word2 = tokens2[0];
                int df1 = Integer.parseInt(tokens1[1]);
                int df2 = Integer.parseInt(tokens2[1]);
                long pointer1 = Long.parseLong(tokens1[2]);
                long pointer2 = Long.parseLong(tokens2[2]);

                long mergedPointer = rafMergedPosting.getFilePointer();

                if(word1.compareTo(word2) < 0){     // if w_i < w_j

                    copyPostingBlock(raf1, pointer1, rafMergedPosting, df1);
                    rafMergedVocab.writeBytes(word1 + ", " + df1 + ", " + mergedPointer + "\n");
                    currentLine1 = reader1.readLine();

                }else if(word1.compareTo(word2) > 0) {    // if w_j < w_i

                    copyPostingBlock(raf2, pointer2, rafMergedPosting, df2);
                    rafMergedVocab.writeBytes(word2 + ", " + df2 + ", " + mergedPointer + "\n");
                    currentLine2 = reader2.readLine();

                }else{      // if w_i == w_j

                    // keep the word in memory until another word is discovered
                    copyPostingBlock(raf1, pointer1, rafMergedPosting, df1);
                    copyPostingBlock(raf2, pointer2, rafMergedPosting, df2);
                    int newDf = df1 + df2;
                    rafMergedVocab.writeBytes(word1 + ", " + newDf + ", " + mergedPointer + "\n");
                    currentLine1 = reader1.readLine();
                    currentLine2 = reader2.readLine();

                }

            }

            // handle leftover lines
            while(currentLine1 != null){

                String[] tokens1 = currentLine1.split(", ");
                String word1 = tokens1[0];
                int df1 = Integer.parseInt(tokens1[1]);
                long pointer1 = rafMergedPosting.getFilePointer();

                long mergedPointer = rafMergedPosting.getFilePointer();
                copyPostingBlock(raf1, pointer1, rafMergedPosting, df1);
                rafMergedVocab.writeBytes(word1 + ", " + df1 + ", " + mergedPointer + "\n");
                currentLine1 = reader1.readLine();

            }

            while(currentLine2 != null){

                String[] tokens2 = currentLine2.split(", ");
                String word2 = tokens2[0];
                int df2 = Integer.parseInt(tokens2[1]);
                long pointer2 = rafMergedPosting.getFilePointer();

                long pointer1 = rafMergedPosting.getFilePointer();
                copyPostingBlock(raf2, pointer2, rafMergedPosting, df2);
                rafMergedVocab.writeBytes(word2 + ", " + df2 + ", " + pointer2 + "\n");
                currentLine2 = reader2.readLine();

            }

            // after writing the new merged files, close and delete the partial files used for merging
            // add the new merged file to the queue

            reader1.close();
            reader2.close();
            raf1.close();
            raf2.close();
            rafMergedVocab.close();
            rafMergedPosting.close();

            vocab1.delete();
            vocab2.delete();
            posting1.delete();
            posting2.delete();

            partialVocabularyFiles.add(mergedVocab);
            partialPostingFiles.add(mergedPosting);

        }

        if(partialPostingFiles.size() != 1)
            mergePartialFiles();

    }



    /**
     * this function is used to test if a word is valid or garbage
     * @param word the word to be tested
     * @return true if valid, false if garbage
     */
    private static boolean isValidWord(String word){

        /* only numbers */
        if(word.matches("\\d+")) return false;

        /* x followed by a series of numbers, thought of them as garbage */
        if(word.matches("^x\\d+$")) return false;

        /* alternating between numbers and letter, also thought of them as garbage */
        if(word.matches(".*\\d+[a-zA-Z]+\\d+.*")) return false;

        return true;
    }

    /**
     * this function is used to load the english and greek stopwords into their class fields
     */
    public static void loadStopwords() throws IOException{

        stopwordsEn = new HashSet<>(Files.readAllLines(new File(System.getProperty("user.dir") + "\\src\\main\\resources\\stopwordsEn.txt").toPath(), StandardCharsets.UTF_8));
        stopwordsGr = new HashSet<>(Files.readAllLines(new File(System.getProperty("user.dir") + "\\src\\main\\resources\\stopwordsGr.txt").toPath(), StandardCharsets.UTF_8));

    }

}
