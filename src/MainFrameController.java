import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import org.neuroph.core.NeuralNetwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainFrameController {
    @FXML
    private TableView<TokenRow> tokenTable;
    @FXML
    private TableColumn<TokenRow, String> tokenColumn;

    @FXML
    private Label tokenNameLabel;


    // Ссылка на главное приложение.
    private FXMainFrame mainApp;

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
        FirstClass.getNewToken();
        mainApp.refreshToken();
    }

    public void GetClass_OnAction(ActionEvent actionEvent) {
        String response = FirstClass.getClass(FXMainFrame.getSourceText());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Результат");
        alert.setContentText(response);
        alert.show();
    }

    public void RefreshTokenTable_OnAction(ActionEvent actionEvent) {
        mainApp.refreshToken();
    }


    /**
     * Инициализация класса-контроллера. Этот метод вызывается автоматически
     * после того, как fxml-файл будет загружен.
     */
    @FXML
    private void initialize() {
        // Инициализация таблицы адресатов с двумя столбцами.
        tokenColumn.setCellValueFactory(cellData -> cellData.getValue().getTokenRow());
        tokenColumn.setCellFactory(TextFieldTableCell.<TokenRow>forTableColumn());
    }

    /**
     * Вызывается главным приложением, которое даёт на себя ссылку.
     *
     * @param mainApp
     */
    public void setMainApp(FXMainFrame mainApp) {
        this.mainApp = mainApp;

        // Добавление в таблицу данных из наблюдаемого списка
        tokenTable.setItems(mainApp.getTokenData());
    }
}
