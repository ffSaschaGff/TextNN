import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FirstClass {
    private static volatile WebServer webServer;
    private static volatile NeuralNetwork neuralNetwork;
    static volatile SQLConnector sqlConnector;
    private static volatile Thread learningThread;

    public static void main(String[] args) {
        //JFrame mainFrame = new MainFrame();
        try {
            FirstClass.sqlConnector = new SQLConnector();
            FirstClass.sqlConnector.initSQL();
            FirstClass.webServer = new WebServer();
            FirstClass.webServer.start();
            createNN();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FXMainFrame.run(args);
    }

    private static void createNN() {
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

    static void stopAll() {
        FirstClass.webServer.stop();
        FirstClass.sqlConnector.stop();
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

    public static synchronized void startLearninig() {
        FirstClass.learningThread = new Thread(() -> {
        FirstClass.setNnInCalculation(true);
        DictonaryBilder dictonaryBilder = new DictonaryBilder(FirstClass.sqlConnector);
        try {
            dictonaryBilder.rebildDictonary();
            createNN();
            teachNN();
        } catch (SQLException e1) {
            e1.printStackTrace();
        } finally {
            FirstClass.setNnInCalculation(false);
        }
    });
    FirstClass.learningThread.start();
    }

    static void stopLearning() {
        neuralNetwork.stopLearning();
    }

    static void clearDB() {
        String[] sql = {"delete from "+SQLConnector.TABLE_DICTONARY,
                "delete from "+SQLConnector.TABLE_SOURCES,
                "delete from "+SQLConnector.TABLE_SOURCES_IN_UNIGRAM};//,
        //"delete from "+SQLConnector.TABLE_CLASES};
        try {
            FirstClass.sqlConnector.execute(sql);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }

    static void readAndLoadToSQL(File file, boolean raw) {
        if (raw) {
            String[] clearSql = {"delete from "+SQLConnector.TABLE_CLASES,
                    "delete from "+SQLConnector.TABLE_SOURCES,
                    "delete from "+SQLConnector.TABLE_SOURCES_IN_UNIGRAM,
                    "delete from "+SQLConnector.TABLE_DICTONARY};
            try {
                FirstClass.sqlConnector.execute(clearSql);
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
        }
        ArrayList<String> sql = new ArrayList<>();
        try {

            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_16);


            for (String line1 : lines) {
                if (raw) {
                    if (!line1.equals(""))
                        sql.add(line1);
                } else {

                    String[] line = line1.split(";");
                    if (line.length == 2) {
                        sql.add("INSERT INTO " + SQLConnector.TABLE_SOURCES + " VALUES (null," + line[1] + ",'" + line[0] + "')");
                    }
                }
            }
            FirstClass.sqlConnector.execute(sql);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    static void saveSQLdata(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try {

            ResultSet resultSet = FirstClass.sqlConnector.getResult("select * from "+SQLConnector.TABLE_CLASES);
            while (resultSet.next()) {
                stringBuilder.append("insert into ").append(SQLConnector.TABLE_CLASES).append(" values(")
                        .append(resultSet.getString("ID")).append(",")
                        .append("'").append(resultSet.getString("NAME")).append("'").append(")").append(System.lineSeparator());
            }
            resultSet = FirstClass.sqlConnector.getResult("select * from "+SQLConnector.TABLE_DICTONARY);
            while (resultSet.next()) {
                stringBuilder.append("insert into ").append(SQLConnector.TABLE_DICTONARY).append(" values(")
                        .append(resultSet.getString("ID")).append(",")
                        .append("'").append(resultSet.getString("VALUE")).append("'").append(")").append(System.lineSeparator());
            }
            resultSet = FirstClass.sqlConnector.getResult("select * from "+SQLConnector.TABLE_SOURCES_IN_UNIGRAM);
            while (resultSet.next()) {
                stringBuilder.append("insert into ").append(SQLConnector.TABLE_SOURCES_IN_UNIGRAM).append(" values(")
                        .append(resultSet.getString("TEXT_ID")).append(",")
                        .append(resultSet.getString("UNIGRAMM_ID")).append(")").append(System.lineSeparator());
            }
            resultSet = FirstClass.sqlConnector.getResult("select * from "+SQLConnector.TABLE_SOURCES);
            while (resultSet.next()) {
                stringBuilder.append("insert into ").append(SQLConnector.TABLE_SOURCES).append(" values(")
                        .append(resultSet.getString("ID")).append(",")
                        .append(resultSet.getString("CLASS_ID")).append(",")
                        .append("'").append(resultSet.getString("SAMPLE")).append("'").append(")").append(System.lineSeparator());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String data = stringBuilder.toString();
        try {
            Files.write(Paths.get(file.getAbsolutePath()),data.getBytes(StandardCharsets.UTF_16),StandardOpenOption.CREATE);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    static String getClass(String source) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            Unigramm unigramm = new Unigramm();
            Set<String> wordsSet = unigramm.getNGram(source);
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
            return response.toString();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return e1.getMessage();
        }
    }

    public static String[] getTokensArray() throws SQLException {
        ResultSet resultSet = sqlConnector.getResult("select * from "+SQLConnector.TABLE_TOKENS);
        ArrayList<String> tokensList = new ArrayList<String>();
        while (resultSet.next()) {
            tokensList.add(resultSet.getString("TOKEN"));
        }
        String[] result = new String[tokensList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = tokensList.get(i);
        }
        return result;
    }

    public static void getNewToken() {
        String token = SecureTokenGenerator.nextToken();
        try {
            FirstClass.sqlConnector.execute("insert into "+SQLConnector.TABLE_TOKENS+" values('"+token+"')");
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
