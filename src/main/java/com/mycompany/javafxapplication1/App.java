package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());

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

        AuditLogger.log(null, AuditLogger.Action.APP_STARTED, null, null);

        // Synchronise local SQLite with remote MySQL (if configured)
        try {
            new SyncService().syncAll();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Initial database sync failed", e);
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
