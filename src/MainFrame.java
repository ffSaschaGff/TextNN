import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class MainFrame extends JFrame {

    private WebServer webServer;
    private SQLConnector sqlConnector;
    private NeuralNetwork neuralNetwork;
    private Thread learningThread;

    //гуи
    private JButton learnButton, stopLearnButton, saveButton, loadButton, clearSQLButton, loadLearningSetButton;
    private JPanel nnPanel, saveLoadPanel, sqlPanel;
    private JLabel learnInfo;



    MainFrame() {
        this.setSize(400,300);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        createGUI();
        createConnector();
        createNN();
        createWebServer();

        this.setVisible(true);
    }

    private void createNN() {
        int[] lauers = new int[3];
        try {
            ResultSet resultSet = sqlConnector.getResult("select count(ID) as ID from "+SQLConnector.TABLE_DICTONARY);
            if (resultSet.next()) {
                lauers[0] = resultSet.getInt("ID");
            }
            lauers[2] = SQLConnector.COUNT_OF_CLASES;
            lauers[1] = lauers[0]/3;
            neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, lauers);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void createGUI() {
        this.getRootPane().setLayout(new BoxLayout(this.getRootPane(), BoxLayout.Y_AXIS));

        sqlPanel = new JPanel();
        sqlPanel.setLayout(new BoxLayout(sqlPanel, BoxLayout.X_AXIS));

        loadLearningSetButton = new JButton("Загрузить в БД");
        loadLearningSetButton.addActionListener(new MainFrameActionLisner());
        clearSQLButton = new JButton("Очистить БД");
        clearSQLButton.addActionListener(new MainFrameActionLisner());
        sqlPanel.add(loadLearningSetButton);
        sqlPanel.add(clearSQLButton);
        this.getRootPane().add(sqlPanel);

        nnPanel = new JPanel();
        nnPanel.setLayout(new BoxLayout(nnPanel, BoxLayout.X_AXIS));

        learnButton = new JButton("Обучить");
        learnButton.addActionListener(new MainFrameActionLisner());
        stopLearnButton = new JButton("Остановить");
        stopLearnButton.addActionListener(new MainFrameActionLisner());
        learnInfo = new JLabel("");

        nnPanel.add(learnButton);
        nnPanel.add(stopLearnButton);
        nnPanel.add(learnInfo);
        this.getRootPane().add(nnPanel);

        saveLoadPanel = new JPanel();
        saveLoadPanel.setLayout(new BoxLayout(saveLoadPanel, BoxLayout.X_AXIS));
        saveButton = new JButton("Сохранить");
        loadButton = new JButton("Загрузить");
        loadButton.addActionListener(new MainFrameActionLisner());
        saveButton.addActionListener(new MainFrameActionLisner());
        saveLoadPanel.add(saveButton);
        saveLoadPanel.add(loadButton);
        this.getRootPane().add(saveLoadPanel);

        this.getRootPane().setAlignmentY(JFrame.TOP_ALIGNMENT);
        nnPanel.setAlignmentX(JFrame.LEFT_ALIGNMENT);
        nnPanel.setAlignmentY(JFrame.TOP_ALIGNMENT);
        saveLoadPanel.setAlignmentX(JFrame.LEFT_ALIGNMENT);
        saveLoadPanel.setAlignmentY(JFrame.TOP_ALIGNMENT);
        sqlPanel.setAlignmentX(JFrame.LEFT_ALIGNMENT);
        sqlPanel.setAlignmentY(JFrame.TOP_ALIGNMENT);
    }

    private void createWebServer() {
        try {
            webServer = new WebServer(sqlConnector, neuralNetwork);
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createConnector() {
        try {
            sqlConnector = new SQLConnector();
            sqlConnector.initSQL();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void teachNN() {
        DataSet dataSet = new DataSet(neuralNetwork.getInputsCount(), neuralNetwork.getOutputsCount());
        try {
            ResultSet outputSet = sqlConnector.getResult("select ID, CLASS_ID from " + SQLConnector.TABLE_SOURCES);
            while (outputSet.next()) {
                double[] input = new double[neuralNetwork.getInputsCount()];
                double[] output = new double[neuralNetwork.getOutputsCount()];
                output[outputSet.getInt("CLASS_ID")-1] = 1;
                ResultSet inputSet = sqlConnector.getResult("select * from " + SQLConnector.TABLE_SOURCES_IN_UNIGRAM + " where TEXT_ID = " + outputSet.getInt("ID"));
                while (inputSet.next()) {
                    input[inputSet.getInt("UNIGRAMM_ID")-1] = 1;
                }
                dataSet.addRow(input, output);
            }

            MomentumBackpropagation learningRule = new MomentumBackpropagation();
            learningRule.setLearningRate(0.2);
            learningRule.setMomentum(0.3);

            learningRule.setMaxError(0.01);
            learningRule.setNeuralNetwork(neuralNetwork);

            learningRule.setTrainingSet(dataSet);
            learningRule.setMaxIterations(10_000_000);

            learningRule.learn(dataSet);
            neuralNetwork.learn(dataSet);
            webServer.setNeuralNetwork(neuralNetwork);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void readAndLoadToSQL(File file) {
        ArrayList<String> sql = new ArrayList<>();
        try {

            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_16);


            for (int i = 0; i < lines.size(); i++) {
                String[] line = lines.get(i).split(";");
                if (line.length == 2) {
                    sql.add("INSERT INTO "+SQLConnector.TABLE_SOURCES+" VALUES (null,"+line[1]+",'"+line[0]+"')");
                }

            }
            sqlConnector.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MainFrameActionLisner implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            if (source == learnButton) {
                learningThread = new Thread(() -> {
                    webServer.setNnInCalculation(true);
                    DictonaryBilder dictonaryBilder = new DictonaryBilder(sqlConnector);
                    try {
                        learnInfo.setText("rebild DB");
                        dictonaryBilder.rebildDictonary();
                        createNN();
                        learnInfo.setText("teach NN");
                        teachNN();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    } finally {
                        learnInfo.setText("learning stopped");
                        webServer.setNnInCalculation(false);
                    }
                });
                learningThread.start();
            } else if (source == stopLearnButton) {
                neuralNetwork.stopLearning();
                learnInfo.setText("learning stopped");
            } else if (source == saveButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    neuralNetwork.save(chooser.getSelectedFile().getAbsolutePath());
                }
            } else if (source == loadButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    try {
                        neuralNetwork = NeuralNetwork.load(new FileInputStream(chooser.getSelectedFile()));
                        webServer.setNeuralNetwork(neuralNetwork);
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            } else if (source == clearSQLButton) {
                String[] sql = {"delete from "+SQLConnector.TABLE_DICTONARY,
                                "delete from "+SQLConnector.TABLE_SOURCES,
                                "delete from "+SQLConnector.TABLE_SOURCES_IN_UNIGRAM};//,
                                //"delete from "+SQLConnector.TABLE_CLASES};
                try {
                    sqlConnector.execute(sql);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            } else if (source == loadLearningSetButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    readAndLoadToSQL(chooser.getSelectedFile());
                }
            }
        }
    }
}
