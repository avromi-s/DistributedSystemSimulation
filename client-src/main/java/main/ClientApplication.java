// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * The Application class responsible for launching the program and GUI
 * */
public class ClientApplication extends Application {
    private final int WINDOW_WIDTH = 750;
    private final int WINDOW_HEIGHT = 530;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Client");
        stage.setScene(scene);
        stage.show();
    }
}
