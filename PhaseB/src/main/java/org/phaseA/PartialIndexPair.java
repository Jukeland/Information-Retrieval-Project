package org.phaseA;

import java.io.File;

public class PartialIndexPair {

    public File vocabFile;
    public File postingFile;

    public PartialIndexPair(File vocabFile, File postingFile){

        this.vocabFile = vocabFile;
        this.postingFile = postingFile;

    }
}
