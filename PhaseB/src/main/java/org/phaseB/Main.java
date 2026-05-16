package org.phaseB;

/**
 * main function used for testing, not viable anymore
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please provide a query string as argument.");
            return;
        }

        String query = String.join(" ", args);
        System.out.println("query: " + query);
        //QueryProcessor processor = new QueryProcessor();
        //processor.processQuery(query);
    }
}

