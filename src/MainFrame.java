import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class MainFrame extends JFrame {

    public static final String ICON_STR = "/images/icon32x32.png";
    public static final String APPLICATION_NAME = "TNN";

    private Thread learningThread;

    //гуи
    private JButton loadSqlButton, saveSqlButton, learnButton, stopLearnButton, saveButton, loadButton, clearSQLButton, loadLearningSetButton, getClassButton;
    private JPanel nnPanel, saveLoadPanel, sqlPanel, getClassPanel, secondSqlPanel;
    private JLabel learnInfo;
    private JTextField sampelField;



    MainFrame() {
        this.setSize(400,180);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        createGUI();
        createConnector();
        createNN();

        this.setVisible(true);
        this.addWindowListener(new MainFrameWindowsLisner());
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

        loadLearningSetButton = new JButton("Загрузить образцы");
        loadLearningSetButton.addActionListener(new MainFrameActionLisner());
        clearSQLButton = new JButton("Очистить БД");
        clearSQLButton.addActionListener(new MainFrameActionLisner());
        sqlPanel.add(loadLearningSetButton);
        sqlPanel.add(clearSQLButton);
        this.getRootPane().add(sqlPanel);

        secondSqlPanel = new JPanel();
        secondSqlPanel.setLayout(new BoxLayout(secondSqlPanel, BoxLayout.X_AXIS));
        saveSqlButton = new JButton("Сохранить БД");
        loadSqlButton = new JButton("Загрузить БД");
        saveSqlButton.addActionListener(new MainFrameActionLisner());
        loadSqlButton.addActionListener(new MainFrameActionLisner());
        secondSqlPanel.add(saveSqlButton);
        secondSqlPanel.add(loadSqlButton);
        this.getRootPane().add(secondSqlPanel);

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
        saveButton = new JButton("Сохранить НС");
        loadButton = new JButton("Загрузить НС");
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
        secondSqlPanel.setAlignmentX(JFrame.LEFT_ALIGNMENT);
        secondSqlPanel.setAlignmentY(JFrame.TOP_ALIGNMENT);
        getClassPanel.setAlignmentX(JFrame.LEFT_ALIGNMENT);
        getClassPanel.setAlignmentY(JFrame.TOP_ALIGNMENT);

        this.pack();
        this.setTitle(APPLICATION_NAME);

        setTrayIcon();

        this.setResizable(false);
    }

    private void setMainFrameVisible(boolean visible) {
        this.setVisible(visible);
    }

    private void setTrayIcon() {
        if(! SystemTray.isSupported() ) {
            return;
        }

        PopupMenu trayMenu = new PopupMenu();
        MenuItem item = new MenuItem("Развернуть");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setMainFrameVisible(true);
            }
        });
        trayMenu.add(item);

        item = new MenuItem("Закрыть");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        trayMenu.add(item);

        URL imageURL = FirstClass.class.getResource(ICON_STR);

        Image icon = Toolkit.getDefaultToolkit().getImage(imageURL);
        TrayIcon trayIcon = new TrayIcon(icon, APPLICATION_NAME, trayMenu);
        trayIcon.setImageAutoSize(true);

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        trayIcon.displayMessage(APPLICATION_NAME, "Application started!",
                TrayIcon.MessageType.INFO);
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

    private void readAndLoadToSQL(File file, boolean raw) {
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

    private String saveSQLdata() {
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
        return stringBuilder.toString();
    }

    class MainFrameWindowsLisner implements WindowListener {

        @Override
        public void windowOpened(WindowEvent e) {

        }

        @Override
        public void windowClosing(WindowEvent e) {
            setMainFrameVisible(false);
        }

        @Override
        public void windowClosed(WindowEvent e) {

        }

        @Override
        public void windowIconified(WindowEvent e) {

        }

        @Override
        public void windowDeiconified(WindowEvent e) {

        }

        @Override
        public void windowActivated(WindowEvent e) {

        }

        @Override
        public void windowDeactivated(WindowEvent e) {

        }
    }

    class MainFrameActionLisner implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            if (source == learnButton) {
                learningThread = new Thread(() -> {
                    FirstClass.setNnInCalculation(true);
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
                        FirstClass.setNnInCalculation(false);
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
                    readAndLoadToSQL(chooser.getSelectedFile(), false);
                }
            } else if (source == saveSqlButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showSaveDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String data = saveSQLdata();
                    try {
                        Files.write(Paths.get(chooser.getSelectedFile().getAbsolutePath()),data.getBytes(StandardCharsets.UTF_16),StandardOpenOption.CREATE);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            } else if (source == loadSqlButton) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    readAndLoadToSQL(chooser.getSelectedFile(), true);
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
