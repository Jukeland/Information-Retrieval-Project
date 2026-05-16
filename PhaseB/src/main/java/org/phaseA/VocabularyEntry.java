package org.phaseA;

public class VocabularyEntry{

    public String term;
    public final int df;
    public final long postingFilePointer;

    public VocabularyEntry(int documentFrequency, long postingFilePointer) {
        this.df = documentFrequency;
        this.postingFilePointer = postingFilePointer;
    }

    public VocabularyEntry(String term, int df, long pointer){

        this.term = term;
        this.df = df;
        this.postingFilePointer = pointer;

    }

    public String getTerm(){ return term; }
    public int getDocumentFrequency() {
        return df;
    }

    public long getPostingFilePointer() {
        return postingFilePointer;
    }

}
