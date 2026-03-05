package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            AppConfig.validateOrThrow();
        } catch (IllegalStateException e) {
            showStartupErrorAndExit("Configuration Error", e.getMessage());
            return;
        }

        try {
            MigrationService migrationService = new MigrationService();
            migrationService.migrate();
        } catch (Exception e) {
            showStartupErrorAndExit("Database Migration Error", "Failed to run database migrations.\n" + e.getMessage());
            return;
        }

        // Load login page
        FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 600, 450);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    private void showStartupErrorAndExit(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Application startup blocked");
        alert.setContentText(message);
        alert.showAndWait();
        javafx.application.Platform.exit();
    }

    public static void main(String[] args) {
        launch();
    }

}
