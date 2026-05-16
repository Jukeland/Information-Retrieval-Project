public class VocabularyEntry{

    private final int df;
    private final long postingFilePointer;

    public VocabularyEntry(int documentFrequency, long postingFilePointer) {
        this.df = documentFrequency;
        this.postingFilePointer = postingFilePointer;
    }

    public int getDocumentFrequency() {
        return df;
    }

    public long getPostingFilePointer() {
        return postingFilePointer;
    }

}
