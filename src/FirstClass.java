import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.learning.MomentumBackpropagation;

import javax.swing.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FirstClass {
    private static volatile WebServer webServer;
    private static volatile NeuralNetwork neuralNetwork;
    static volatile SQLConnector sqlConnector;

    public static void main(String[] args) {
        JFrame mainFrame = new MainFrame();
        try {
            FirstClass.webServer = new WebServer();
            FirstClass.webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void setNnInCalculation(boolean NNinCalc) {
        webServer.setNnInCalculation(NNinCalc);
    }

    static NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }

    static void setNeuralNetwork(NeuralNetwork setNeuralNetwork) {
        neuralNetwork = setNeuralNetwork;
    }

    static synchronized void teachNN() {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void stopLearning() {
        neuralNetwork.stopLearning();
    }
}
