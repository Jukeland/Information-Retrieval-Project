package org.phaseB;

import gr.uoc.csd.hy463.Topic;
import gr.uoc.csd.hy463.TopicsReader;
import java.util.ArrayList;

public class TopicQueryEvaluator {

    public static void main(String[] args) throws Exception {

        String topicsPath = "C:\\Users\\anton\\OneDrive\\Desktop\\University\\HY-463\\Project\\2025-3-EkfonisiPublic\\2_Resources_Corpus\\topics.xml";
        String vocab_file = "C:\\Users\\anton\\OneDrive\\Desktop\\University\\HY-463\\Project\\hy463_project\\src\\resources\\CollectionIndex\\merged-vocabulary-9.txt";
        String posting_file = "C:\\Users\\anton\\OneDrive\\Desktop\\University\\HY-463\\Project\\hy463_project\\src\\resources\\CollectionIndex\\merged-posting-9.txt";
        System.out.println("topics_path: " + topicsPath);
        boolean useSummary = args.length > 0 && args[0].equalsIgnoreCase("--use-summary");

        ArrayList<Topic> topics = TopicsReader.readTopics(topicsPath);
        QueryProcessor processor = new QueryProcessor(vocab_file, posting_file);
        QueryProcessor.loadStopwords();

        for(Topic topic : topics){

            System.out.println("Topic " + topic.getNumber() + ": (" + topic.getType() + ")");
            System.out.println("----------------------------------------------------------");

            if(useSummary)
                processor.processQuery(topic.getSummary());
            else
                processor.processQuery(topic.getDescription());

            System.out.println("==========================================================\n");

        }
    }
}
