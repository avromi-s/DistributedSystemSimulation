// Avromi Schneierson - 1/10/2024
package main;

import javafx.application.Platform;
import javafx.scene.control.TextInputControl;

public class Logging {
    /**
     * Log the specified string to the console and append it to the GUI control specified
     * */
    public static void consoleLogAndAppendToGUILogs(String message, TextInputControl textField) {
        consoleLog(message);
        appendToGUILogs(message, textField);
    }

    public static void appendToGUILogs(String message, TextInputControl textField) {
        Platform.runLater(() -> textField.appendText(message));
    }

    public static void consoleLog(String message) {
        System.out.println(Thread.currentThread().getName() + " - " + message);
    }
}
