package com.mycompany.javafxapplication1;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class for user registration.
 */
public class RegisterController {

    private static final Logger logger = Logger.getLogger(RegisterController.class.getName());
    private final AuthService authService = new AuthService();

    @FXML
    private CheckBox masterCheckbox;

    @FXML
    private TextField masterPasswordField;

    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;

    @FXML
    private void toggleMasterPasswordField(ActionEvent event) {
        masterPasswordField.setDisable(!masterCheckbox.isSelected());
    }

    private static final String ENV_ADMIN_REGISTRATION_SECRET = "ADMIN_REGISTRATION_SECRET";
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]{3,50}$";

    @FXML
    private void registerHandler(ActionEvent event) {
        String username = userTextField.getText();
        String password = passPasswordField.getText();
        String rePassword = rePassPasswordField.getText();
        boolean isMasterUser = masterCheckbox.isSelected();

        // Input validation
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Invalid Input", "Username and password are required.");
            return;
        }

        // Username format validation (#10)
        if (!username.matches(USERNAME_PATTERN)) {
            showAlert("Invalid Username",
                    "Username must be 3-50 characters and contain only letters, digits, '.', '_', or '-'.");
            return;
        }

        // Password strength validation (#7)
        if (password.length() < 3) {
            showAlert("Weak Password", "Password must be at least 3 characters long.");
            return;
        }

        if (!password.equals(rePassword)) {
            showAlert("Password Mismatch", "Passwords do not match.");
            return;
        }

        // Validate master password for admin registration (#1)
        String role;
        if (isMasterUser) {
            String masterInput = masterPasswordField.getText();
            String adminSecret = System.getenv(ENV_ADMIN_REGISTRATION_SECRET);
            if (adminSecret == null || adminSecret.isBlank()) {
                showAlert("Admin Registration Unavailable",
                        "Admin registration is not configured. Contact the system administrator.");
                return;
            }
            if (!adminSecret.equals(masterInput)) {
                showAlert("Invalid Master Password", "The master password is incorrect.");
                return;
            }
            role = "admin";
        } else {
            role = "user";
        }

        // Register user
        try {
            if (authService.registerUser(username, password, role)) {
                AuditLogger.log(username, AuditLogger.Action.USER_REGISTERED, username, "Role: " + role);
                showAlert("Success", "User registered successfully as a " + role + " user.");
            } else {
                AuditLogger.logFailure(username, AuditLogger.Action.USER_REGISTERED, username,
                        "Registration failed – duplicate username");
                showAlert("Registration Failed", "A user with this username already exists.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred during registration", e);
            showAlert("Error", "An error occurred during registration.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            Stage stage = (Stage) backLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 450));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to navigate to login screen", e);
        }
    }
}
