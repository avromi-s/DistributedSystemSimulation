module main.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens main to javafx.fxml;
    exports main;
//    opens main.tasks to javafx.fxml;
//    exports main.tasks;
    opens main.enums to javafx.fxml;
    exports main.enums;
    opens main.classes to javafx.fxml;
    exports main.classes;
    opens PacketCommunication.enums to javafx.fxml;
    exports PacketCommunication.enums;
    opens PacketCommunication to javafx.fxml;
    exports PacketCommunication;
}