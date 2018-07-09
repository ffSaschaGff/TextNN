import org.neuroph.core.NeuralNetwork;

import javax.swing.*;

public class FirstClass {
    public static volatile WebServer webServer;
    public static volatile NeuralNetwork neuralNetwork;
    public static volatile SQLConnector sqlConnector;

    public static void main(String[] args) {
        JFrame mainFrame = new MainFrame();
    }
}
