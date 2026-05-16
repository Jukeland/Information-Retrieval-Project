package org.phaseB;

import mitos.stemmer.Stemmer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class QueryGUI extends JFrame {

    private final JTextField vocabField;
    private final JTextField postingField;
    private final JTextField queryField;
    private final JTextArea resultArea;

    private File vocabFile;
    private File postingFile;

    public QueryGUI(){

        /* set the window */
        setTitle("Query Processor");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        /* set the components of the gui */
        vocabField = new JTextField(System.getProperty("user.dir"));
        postingField = new JTextField(System.getProperty("user.dir"));
        vocabField.setEditable(false);
        postingField.setEditable(false);
        JButton vocabButton = new JButton("Select");
        JButton postingButton = new JButton("Select");
        JButton searchButton = new JButton("Start Search");
        queryField = new JTextField();
        resultArea = new JTextArea(15, 50);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        /* construct the vocabulary file field with its button */
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        add(vocabField, gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        add(vocabButton, gbc);
        gbc.gridx = 2;
        add(new JLabel("Select the Vocabulary File"), gbc);

        /* construct the posting file field with its button */
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1;
        add(postingField, gbc);
        gbc.gridx = 1;
        add(postingButton, gbc);
        gbc.gridx = 2;
        add(new JLabel("Select the Posting File"), gbc);

        /* construct the query area with the search button */
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(queryField, gbc);
        gbc.gridx = 2; gbc.gridwidth = 1;
        add(searchButton, gbc);

        /* construct the logging area */
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        add(scrollPane, gbc);

        /* add button listeners */
        vocabButton.addActionListener(e -> selectFile("vocab"));
        postingButton.addActionListener(e -> selectFile("posting"));
        searchButton.addActionListener(e -> startQuery());

    }

    /**
     * this function is used to select the vocabulary and posting files from the directory they're into
     * @param mode vocab or posting, depends on the button pressed
     */
    private void selectFile(String mode){

        JFileChooser chooser = new JFileChooser();
        File dir = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\CollectionIndex");
        chooser.setCurrentDirectory(dir);

        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            File selected = chooser.getSelectedFile();

            if(mode.equals("vocab")){

                vocabFile = selected;
                vocabField.setText(selected.getAbsolutePath());
                QueryProcessor.VOCAB_FILE = selected.getAbsolutePath();

            }else if (mode.equals("posting")){

                postingFile = selected;
                postingField.setText(selected.getAbsolutePath());
                QueryProcessor.POSTING_FILE = selected.getAbsolutePath();

            }

        }

    }

    /**
     * this function is used to start the search for the query provided
     */
    private void startQuery(){

        if(vocabFile == null || postingFile == null || queryField.getText().isEmpty()){

            resultArea.append("Please select both files and enter a query first.\n");
            return;

        }

        try{

            Stemmer.Initialize();
            QueryProcessor.loadStopwords();

            resultArea.append("Searching for: " + queryField.getText() + "\n");
            QueryProcessor processor = new QueryProcessor(resultArea);
            processor.processQuery(queryField.getText());

        }catch (Exception ex){
            resultArea.append("Error: " + ex.getMessage() + "\n");
        }

    }

    public static void main(String[] args) {
        new QueryGUI().setVisible(true);
    }

}
