import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.sql.SQLException;

public class FXMainFrame extends Application {

    private ObservableList<TokenRow> tokenData = FXCollections.observableArrayList();
    private static volatile FXMainFrame currentFrame;
    private volatile TextField sourceField;

    static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(FXMainFrame.class.getResource("view/MainFrameLayout.fxml"));
        //Parent root = FXMLLoader.load(FirstClass.class.getResource("/view/MainFrameLayout.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Админка нейросети");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.setOnCloseRequest(new MainFrameEventHandler());
        FXMainFrame.currentFrame = this;
        sourceField = (TextField) primaryStage.getScene().lookup("SampleField");

        refreshToken();
        MainFrameController controller = loader.getController();
        controller.setMainApp(this);

        primaryStage.show();

    }

    public ObservableList<TokenRow> getTokenData() {
        return tokenData;
    }

    public void refreshToken() {
        try {
            tokenData.clear();
            String[] tokens = FirstClass.getTokensArray();
            for (String token: tokens) {
                tokenData.add(new TokenRow(token));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class MainFrameEventHandler implements javafx.event.EventHandler {

        @Override
        public void handle(Event event) {
            if (event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
                FirstClass.stopAll();
            }
        }
    }

    public static String getSourceText() {
        if (currentFrame.sourceField != null) {
            return currentFrame.sourceField.getText();
        } else {
            return "";
        }
    }

}
