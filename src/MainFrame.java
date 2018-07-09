import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;
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
import java.util.Set;

class MainFrame extends JFrame {

    private Thread learningThread;

    //гуи
    private JButton learnButton, stopLearnButton, saveButton, loadButton, clearSQLButton, loadLearningSetButton, getClassButton;
    private JPanel nnPanel, saveLoadPanel, sqlPanel, getClassPanel;
    private JLabel learnInfo;
    private JTextField sampelField;



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
            ResultSet resultSet = FirstClass.sqlConnector.getResult("select count(ID) as ID from "+SQLConnector.TABLE_DICTONARY);
            if (resultSet.next()) {
                lauers[0] = resultSet.getInt("ID");
            }
            lauers[2] = SQLConnector.COUNT_OF_CLASES;
            lauers[1] = lauers[0]/3;
            FirstClass.setNeuralNetwork(new MultiLayerPerceptron(TransferFunctionType.SIGMOID, lauers));
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

        getClassPanel = new JPanel();
        getClassPanel.setLayout(new BoxLayout(getClassPanel, BoxLayout.X_AXIS));
        sampelField = new JTextField("Введите для распознования");
        getClassPanel.add(sampelField);
        getClassButton = new JButton("Получить класс");
        getClassButton.addActionListener(new MainFrameActionLisner());
        getClassPanel.add(getClassButton);
        this.getRootPane().add(getClassPanel);

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
            FirstClass.webServer = new WebServer();
            FirstClass.webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createConnector() {
        try {
            FirstClass.sqlConnector = new SQLConnector();
            FirstClass.sqlConnector.initSQL();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void teachNN() {
        FirstClass.teachNN();
    }

    private void readAndLoadToSQL(File file) {
        ArrayList<String> sql = new ArrayList<>();
        try {

            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_16);


            for (String line1 : lines) {
                String[] line = line1.split(";");
                if (line.length == 2) {
                    sql.add("INSERT INTO " + SQLConnector.TABLE_SOURCES + " VALUES (null," + line[1] + ",'" + line[0] + "')");
                }

            }
            FirstClass.sqlConnector.execute(sql);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    class MainFrameActionLisner implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            if (source == learnButton) {
                learningThread = new Thread(() -> {
                    FirstClass.webServer.setNnInCalculation(true);
                    DictonaryBilder dictonaryBilder = new DictonaryBilder(FirstClass.sqlConnector);
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
                        FirstClass.webServer.setNnInCalculation(false);
                    }
                });
                learningThread.start();
            } else if (source == stopLearnButton) {
                FirstClass.stopLearning();
                learnInfo.setText("learning stopped");
            } else if (source == saveButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    FirstClass.getNeuralNetwork().save(chooser.getSelectedFile().getAbsolutePath());
                }
            } else if (source == loadButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    try {
                        FirstClass.setNeuralNetwork(NeuralNetwork.load(new FileInputStream(chooser.getSelectedFile())));
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
                    FirstClass.sqlConnector.execute(sql);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            } else if (source == loadLearningSetButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    readAndLoadToSQL(chooser.getSelectedFile());
                }
            } else if (source == getClassButton) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    Unigramm unigramm = new Unigramm();
                    Set<String> wordsSet = unigramm.getNGram((String) sampelField.getText());
                    stringBuilder.append("select ID from " + SQLConnector.TABLE_DICTONARY + " where VALUE in (");
                    boolean isFirst = true;
                    for (String word : wordsSet) {
                        if (isFirst) {
                            isFirst = !isFirst;
                        } else {
                            stringBuilder.append(",");
                        }
                        stringBuilder.append("'" + word + "'");
                    }
                    stringBuilder.append(")");
                    ResultSet resultSet = null;
                    resultSet = FirstClass.sqlConnector.getResult(stringBuilder.toString());

                    NeuralNetwork neuralNetwork = FirstClass.getNeuralNetwork();
                    double[] inputNeuro = new double[neuralNetwork.getInputsCount()];
                    while (resultSet.next()) {
                        inputNeuro[resultSet.getInt("ID") - 1] = 1;
                    }
                    neuralNetwork.setInput(inputNeuro);
                    neuralNetwork.calculate();
                    double[] outputNeuro = neuralNetwork.getOutput();
                    StringBuilder response = new StringBuilder();
                    for (int i = 0; i < outputNeuro.length; i++) {
                        response.append(String.valueOf(i + 1)).append(":").append(String.valueOf(outputNeuro[i]));
                        if (i != outputNeuro.length - 1) {
                            response.append(System.lineSeparator());
                        }
                    }
                    JOptionPane.showMessageDialog(null, response.toString());
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
