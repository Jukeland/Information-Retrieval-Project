package org.phaseB;

import org.phaseA.VocabularyEntry;
import java.io.*;

public class Utils {

    public static VocabularyEntry getVocabEntry(String targetTerm, String vocabFilePath) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(vocabFilePath));
        String line;

        while((line = reader.readLine()) != null){

            String[] parts = line.split(", ", 3);
            String term = parts[0].trim();
            if(term.equals(targetTerm)){

                int df = Integer.parseInt(parts[1].trim());
                long pointer = Long.parseLong(parts[2].trim());
                reader.close();
                return new VocabularyEntry(term, df, pointer);

            }

        }

        reader.close();
        return null;

    }

}
