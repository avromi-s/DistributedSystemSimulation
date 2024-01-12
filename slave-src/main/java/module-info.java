module main.slave {
    requires javafx.controls;
    requires javafx.fxml;

    opens main to javafx.fxml;
    exports main;
    opens main.classes to javafx.fxml;
    exports main.classes;

    opens main.enums;
    exports main.enums to javafx.fxml;

    exports PacketCommunication.enums;
    opens PacketCommunication.enums to javafx.fxml;
    exports PacketCommunication;
    opens PacketCommunication to javafx.fxml;


}