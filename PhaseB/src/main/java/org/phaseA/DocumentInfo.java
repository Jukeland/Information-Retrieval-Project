package org.phaseA;

public class DocumentInfo{

    private String filePath;
    private double vectorLength;
    private long recordLength;
    private double sumOfSquares = 0.0;

    public DocumentInfo(String path, double vectorLength, long recordLength){

        this.filePath = path;
        this.vectorLength = vectorLength;
        this.recordLength = recordLength;

    }

    public String getFilePath(){ return this.filePath; }

    public double getVectorLength(){ return vectorLength; }

    public void setVectorLength(double vectorLength){ this.vectorLength = vectorLength; }

    public long getRecordLength(){ return recordLength; }

    public void updateVectorLength(int tf){
        // We assume `tf` was incremented from tf-1, so update the delta: tf^2 - (tf-1)^2
        double delta = tf * tf - (tf - 1) * (tf - 1);
        sumOfSquares += delta;
    }

    public double getVectorLengthNormalized(int totalWords){
        return totalWords == 0 ? 0.0 : Math.sqrt(sumOfSquares) / totalWords;
    }

}
