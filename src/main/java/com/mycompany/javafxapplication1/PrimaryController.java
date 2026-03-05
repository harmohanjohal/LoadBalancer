package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the login screen ({@code primary.fxml}).
 */
public class PrimaryController {

    private static final Logger logger = Logger.getLogger(PrimaryController.class.getName());
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30_000; // 30 seconds

    private static final Map<String, int[]> failedAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lockoutUntil = new ConcurrentHashMap<>();

    private final AuthService authService = new AuthService();

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private Button registerBtn;

    @FXML
    private Button loginBtn;

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("register.fxml"));
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Register");
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load registration screen", e);
            showAlert("Navigation Error", "Failed to load registration screen.");
        }
    }

    @FXML
    private void switchToSecondary() {
        String username = userTextField.getText();
        String password = passPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Invalid Input", "Please enter both username and password.");
            return;
        }

        // Check if account is locked out
        Long lockedUntil = lockoutUntil.get(username);
        if (lockedUntil != null && System.currentTimeMillis() < lockedUntil) {
            long remainingSec = (lockedUntil - System.currentTimeMillis()) / 1000 + 1;
            showAlert("Account Locked",
                    "Too many failed attempts. Try again in " + remainingSec + " seconds.");
            return;
        }

        try {
            User authenticatedUser = authService.authenticate(username, password);
            if (authenticatedUser != null) {
                // Reset failure tracking on success
                failedAttempts.remove(username);
                lockoutUntil.remove(username);

                SessionManager.getInstance().setLoggedInUser(authenticatedUser);
                AuditLogger.log(username, AuditLogger.Action.LOGIN_SUCCESS, username, null);

                Parent root = FXMLLoader.load(getClass().getResource("secondary.fxml"));
                Stage stage = (Stage) userTextField.getScene().getWindow();
                stage.setScene(new Scene(root, 860, 540));
                stage.setTitle("User Management");
                stage.show();
            } else {
                // Track failed attempt
                int[] count = failedAttempts.computeIfAbsent(username, k -> new int[]{0});
                count[0]++;
                if (count[0] >= MAX_FAILED_ATTEMPTS) {
                    lockoutUntil.put(username, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
                    failedAttempts.remove(username);
                    AuditLogger.logFailure(username, AuditLogger.Action.LOGIN_LOCKED, username,
                            "Account locked for 30 seconds");
                    showAlert("Account Locked",
                            "Too many failed attempts. Account locked for 30 seconds.");
                } else {
                    AuditLogger.logFailure(username, AuditLogger.Action.LOGIN_FAILURE, username, null);
                    showAlert("Login Failed", "Invalid username or password.");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred during login", e);
            showAlert("Error", "An error occurred during login.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
