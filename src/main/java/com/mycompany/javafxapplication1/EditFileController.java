package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the file editing screen ({@code edit_file.fxml}).
 */
public class EditFileController {

    @FXML
    private TextField fileNameField;

    @FXML
    private TextArea fileContentArea;

    private static final Logger logger = Logger.getLogger(EditFileController.class.getName());

    private File file;

    /**
     * Initializes the controller with the given decrypted file.
     *
     * @param file The decrypted file to edit.
     */
    public void initialize(File file) {
        this.file = file;

        fileNameField.setText(file.getName());

        try {
            String content = Files.readString(file.toPath());
            fileContentArea.setText(content);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading file content", e);
            showErrorAlert("Error", "Failed to load file content.");
        }
    }

    /**
     * Saves the changes made to the file and re-encrypts it.
     */
    @FXML
    private void saveChangesHandler() {
        try {
            Files.writeString(file.toPath(), fileContentArea.getText());
            FileUtils.encryptFile(file, new File(FileUtils.UPLOADS_DIR + "/" + file.getName() + ".zip"));
            User currentUser = SessionManager.getInstance().getLoggedInUser();
            AuditLogger.log(currentUser != null ? currentUser.getUsername() : null,
                    AuditLogger.Action.FILE_EDITED, file.getName(), "Content saved");
            showInfoAlert("Success", "File changes saved.");
            closeWindow();
        } catch (IOException e) {
            showErrorAlert("Error", "Failed to save changes.");
        }
    }

    @FXML
    private void cancelHandler() {
        closeWindow();
    }

    private void closeWindow() {
        cleanupTempFolder();
        fileNameField.getScene().getWindow().hide();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cleanupTempFolder() {
        if (file == null) {
            return;
        }

        File parent = file.getParentFile();
        if (parent == null || !parent.exists()) {
            return;
        }

        File tempRoot = new File("temp").getAbsoluteFile();
        File parentAbs = parent.getAbsoluteFile();
        if (!parentAbs.toPath().startsWith(tempRoot.toPath())) {
            return;
        }

        try {
            FileUtils.deleteDirectory(parentAbs);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to clean temp directory: " + parentAbs.getAbsolutePath(), e);
        }
    }
}
