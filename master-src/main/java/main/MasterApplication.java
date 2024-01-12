// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MasterApplication extends Application {
    private final int WINDOW_WIDTH = 750;
    private final int WINDOW_HEIGHT = 530;
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MasterApplication.class.getResource("master-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Master");
        stage.setScene(scene);
        stage.show();
    }
}