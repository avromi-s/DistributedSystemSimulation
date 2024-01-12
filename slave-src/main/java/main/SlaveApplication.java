// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.enums.JobType;

import java.io.IOException;
import java.util.Arrays;

/**
 * The application class responsible for launching the program and GUI
 * */
public class SlaveApplication extends Application {
    private FXMLLoader fxmlLoader;
    private Stage stage;
    private JobType slaveType;
    private Label label;
    private ChoiceBox<JobType> slaveTypeChoiceBox;
    private Button setSlaveTypeButton;
    private final int WINDOW_WIDTH = 750;
    private final int WINDOW_HEIGHT = 530;
    private final int POPUP_WINDOW_WIDTH = 330;
    private final int POPUP_WINDOW_HEIGHT = 150;
    private final int POPUP_STAGE_PADDING = 8;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        fxmlLoader = new FXMLLoader(SlaveApplication.class.getResource("slave-view.fxml"));
        this.stage = stage;

        // Retrieve the job type that this slave is optimized for by reading the first command-line argument
        // If a command-line argument is not provided or an invalid one is provided, display a popup to get it
        try {
            slaveType = JobType.valueOf(getParameters().getUnnamed().get(0));
            launchSlaveApplication(slaveType);
        } catch (IndexOutOfBoundsException|IllegalArgumentException e) {
            // show popup and start application once slave type is provided:
            showSlaveTypePopup(stage);
            setSlaveTypeButton.setOnAction(event -> {
                try {
                    launchSlaveApplication(slaveTypeChoiceBox.getSelectionModel().getSelectedItem());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    private void launchSlaveApplication(JobType slaveType) throws IOException {
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Slave - Type " + slaveType);
        stage.setScene(scene);
        SlaveController controller = fxmlLoader.getController();
        controller.init(slaveType);
        stage.show();
    }

    private void showSlaveTypePopup(Stage stage) {
        label = new Label("Please set the job type this Slave is optimized for");
        slaveTypeChoiceBox = new ChoiceBox<>();
        ObservableList<JobType> jobTypes = FXCollections.observableArrayList();
        jobTypes.addAll(Arrays.asList(JobType.values()));
        slaveTypeChoiceBox.setItems(jobTypes);
        slaveTypeChoiceBox.getSelectionModel().selectFirst();
        setSlaveTypeButton = new Button("Set");

        VBox vBox = new VBox(label, slaveTypeChoiceBox, setSlaveTypeButton);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(POPUP_STAGE_PADDING * 1.5);
        FlowPane root = new FlowPane(POPUP_STAGE_PADDING, POPUP_STAGE_PADDING);
        root.getChildren().add(vBox);
        root.setPadding(new Insets(POPUP_STAGE_PADDING * 2));
        root.setAlignment(Pos.CENTER);

        stage.setMinHeight(POPUP_WINDOW_HEIGHT + POPUP_STAGE_PADDING * 6);
        stage.setMinWidth(POPUP_WINDOW_WIDTH + POPUP_STAGE_PADDING * 6);

        Scene scene = new Scene(root, POPUP_WINDOW_WIDTH, POPUP_WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }
}