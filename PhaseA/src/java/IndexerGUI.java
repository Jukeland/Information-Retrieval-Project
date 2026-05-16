import mitos.stemmer.Stemmer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class IndexerGUI extends JFrame{

    private final JTextField inputField;
    private final JTextField outputField;
    private final JTextArea logArea;

    private File inputDirectory;
    private File outputDirectory;

    /**
     * This function is used set up the indexer gui
     */
    public IndexerGUI(){

        /* set the window */
        setTitle("Indexer");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        /* set the components of the gui */
        inputField = new JTextField(System.getProperty("user.dir"));
        outputField = new JTextField(System.getProperty("user.dir"));
        inputField.setEditable(false);
        outputField.setEditable(false);
        JButton inputButton = new JButton("Select");
        JButton outputButton = new JButton("Select");
        JButton startButton = new JButton("Start Indexing");
        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        /* construct the input directory field with its button */
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        add(inputField, gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        add(inputButton, gbc);
        gbc.gridx = 2;
        add(new JLabel("Select the Document Folder"), gbc);

        /* construct the output directory field with its button */
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1;
        add(outputField, gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        add(outputButton, gbc);
        gbc.gridx = 2;
        add(new JLabel("Select the Output Folder"), gbc);

        /* construct the start button */
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1;
        add(startButton, gbc);

        /* construct the logging area */
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        add(scrollPane, gbc);

        /* add button listeners */
        inputButton.addActionListener(e -> selectFolder("input"));
        outputButton.addActionListener(e -> selectFolder("output"));
        startButton.addActionListener(e -> startIndexing());
    }


    /**
     * This function is used to select the input or output directory specified by the user
     * @param mode input or output
     */
    private void selectFolder(String mode){

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File dir = new File(System.getProperty("user.dir"));
        chooser.setCurrentDirectory(dir);

        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){

            if(mode.equals("input")){

                inputDirectory = chooser.getSelectedFile();
                inputField.setText(inputDirectory.getAbsolutePath());

            }else if(mode.equals("output")){

                outputDirectory = chooser.getSelectedFile();
                outputField.setText(outputDirectory.getAbsolutePath());

            }else
                System.err.println("selectFolder says: Wrong mode");

        }

    }

    /**
     * This function is used to start the process of indexing a directory of documents
     */
    private void startIndexing(){

        if(inputDirectory == null || outputDirectory == null){

            logArea.append("Select both input and output directories before indexing\n");
            return;

        }

        logArea.append("Starting Indexing...\n");

        try{

            Indexer.setOutputDirectory(outputDirectory);
            long start = System.nanoTime();
            logArea.append("Reading files from: " + inputDirectory + "\n");
            Stemmer.Initialize();
            Indexer.loadStopwords();
            Indexer.readFiles(inputDirectory);
            Indexer.writePartialPosting();
            Indexer.writePartialVocabulary();
            Indexer.createDocumentsFile();

            double timeObjects = (System.nanoTime() - start) / 1e9;
            logArea.append("Files to index: " + Indexer.fileCount + "\n");
            logArea.append("Creating Objects TimeTaken: " + timeObjects + "\n");

            start = System.nanoTime();
            Indexer.mergePartialFiles();
            double mergeTime = (System.nanoTime() - start) / 1e9;
            logArea.append("Merging Files TimeTaken: " + mergeTime + "\n");

        }catch (Exception ex){
            logArea.append("Error: " + ex.getMessage() + "\n");
        }

    }

    public static void main(String[] args){
        new IndexerGUI().setVisible(true);
    }
}
