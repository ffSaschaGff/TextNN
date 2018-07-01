import org.h2.util.IOUtils;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    WebServer webServer;
    SQLConnector sqlConnector;
    NeuralNetwork neuralNetwork;

    //гуи
    JButton recalsDictonaryButton, learnButton, saveButton, loadButton;
    JPanel nnPanel, saveLoadPanel;



    public MainFrame() {
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
            lauers[1] = (lauers[0]+lauers[2])/2;
            neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, lauers);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void createGUI() {
        this.getRootPane().setLayout(new BoxLayout(this.getRootPane(), BoxLayout.Y_AXIS));

        nnPanel = new JPanel();
        nnPanel.setLayout(new BoxLayout(nnPanel, BoxLayout.X_AXIS));
        recalsDictonaryButton = new JButton("Пересчитать");
        recalsDictonaryButton.addActionListener(new MainFrameActionLisner());
        learnButton = new JButton("Обучить");
        learnButton.addActionListener(new MainFrameActionLisner());
        nnPanel.add(recalsDictonaryButton);
        nnPanel.add(learnButton);
        this.getRootPane().add(nnPanel);

        saveLoadPanel = new JPanel();
        saveLoadPanel.setLayout(new BoxLayout(saveLoadPanel, BoxLayout.X_AXIS));
        saveButton = new JButton("Сохранить");
        loadButton = new JButton("Загрузить");
        saveLoadPanel.add(saveButton);
        saveLoadPanel.add(loadButton);
        this.getRootPane().add(saveLoadPanel);
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
                ResultSet inputSet = sqlConnector.getResult("select * from " + SQLConnector.TABLE_SOURCES_IN_UNIGRAM + " where TEXT_ID = " + outputSet.getInt("CLASS_ID"));
                while (inputSet.next()) {
                    input[inputSet.getInt("UNIGRAMM_ID")-1] = 1;
                }
                dataSet.addRow(input, output);
            }
            neuralNetwork.learn(dataSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    class MainFrameActionLisner implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            if (source == recalsDictonaryButton) {
                DictonaryBilder dictonaryBilder = new DictonaryBilder(sqlConnector);
                try {
                    dictonaryBilder.rebildDictonary();
                    createNN();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            } else if (source == learnButton) {
                teachNN();
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
                        neuralNetwork = NeuralNetwork.load((InputStream) new FileInputStream(chooser.getSelectedFile()));
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
