import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.neuroph.core.NeuralNetwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainFrameController {
    public void LoadBD_OnAction(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        File ret = chooser.showOpenDialog(null);
        if (ret != null) {
            FirstClass.readAndLoadToSQL(ret, true);
        }
    }

    public void SaveBD_OnAction(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        File ret = chooser.showSaveDialog(null);
        if (ret != null) {
            FirstClass.saveSQLdata(ret);
        }
    }

    public void ClearBD_OnAction(ActionEvent actionEvent) {
        FirstClass.clearDB();
    }

    public void LoadLearningSet_OnAction(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        File ret = chooser.showOpenDialog(null);
        if (ret != null) {
            FirstClass.readAndLoadToSQL(ret, false);
        }
    }

    public void LoadNN_OnAction(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        File ret = chooser.showOpenDialog(null);
        if (ret != null) {
            try {
                FirstClass.setNeuralNetwork(NeuralNetwork.load(new FileInputStream(ret)));
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void SaveNN_OnAction(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        File ret = chooser.showSaveDialog(null);
        if (ret != null) {
            FirstClass.getNeuralNetwork().save(ret.getAbsolutePath());
        }
    }

    public void TeachNN_OnAction(ActionEvent actionEvent) {
        FirstClass.startLearninig();
    }

    public void StopTeaching_OnAction(ActionEvent actionEvent) {
        FirstClass.stopLearning();
    }

    public void GetNewToken_OnAction(ActionEvent actionEvent) {
    }

    public void GetClass_OnAction(ActionEvent actionEvent) {
        String response = FirstClass.getClass(FXMainFrame.getSourceText());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Результат");
        alert.setContentText(response);
        alert.show();
    }
}
