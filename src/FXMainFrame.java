import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class FXMainFrame extends Application {

    private static volatile FXMainFrame currentFrame;
    private volatile TextField sourceField;

    static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(FirstClass.class.getResource("/view/MainFrameLayout.fxml"));
        primaryStage.setTitle("Админка нейросети");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.setOnCloseRequest(new MainFrameEventHandler());
        FXMainFrame.currentFrame = this;
        sourceField = (TextField) primaryStage.getScene().lookup("SampleField");

        primaryStage.show();

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
