package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the user management screen ({@code secondary.fxml}).
 */
public class SecondaryController {

    private static final Logger logger = Logger.getLogger(SecondaryController.class.getName());

    @FXML
    private Button logoutButton;

    @FXML
    private TableView<User> userTableView;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> passwordColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @FXML
    private Button uploadFileButton;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        try {
            // Set up the table columns
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            passwordColumn.setCellValueFactory(cellData -> new SimpleStringProperty("********"));
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

            // Fetch data from the remote database
            ObservableList<User> userList = FXCollections.observableArrayList(userService.getAllUsers());
            userTableView.setItems(userList);

            // Add action buttons to the "Actions" column
            addActionButtons();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize user management screen", e);
        }
    }

    @FXML
    private void openFileUploadScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("file_upload.fxml"));

            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found: file_upload.fxml");
            }

            Parent root = loader.load();
            Stage stage = (Stage) uploadFileButton.getScene().getWindow();
            stage.setScene(new Scene(root, 860, 580));
            stage.setTitle("File Management");
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading file_upload.fxml", e);
            showErrorAlert("Error", "Unable to load the file upload screen. Please check logs for more details.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error opening file upload screen", e);
            showErrorAlert("Unexpected Error", "An unexpected error occurred. Check logs for details.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void logoutHandler(ActionEvent event) {
        SessionManager.getInstance().clearSession();
        navigateToLoginScreen();
    }

    private void navigateToLoginScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 450));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            showErrorAlert("Navigation Error", "Unable to switch to the login screen.");
        }
    }

    private void addActionButtons() {
        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button updateButton = new Button("Update");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionButtons = new HBox(updateButton, deleteButton);

            {
                // Style and spacing for buttons
                actionButtons.setSpacing(10);
                updateButton.getStyleClass().add("btn-info");
                deleteButton.getStyleClass().add("btn-danger");

                // Action for Update button
                updateButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleUpdate(user);
                });

                // Action for Delete button
                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null); // No buttons for empty cells
                } else {
                    setGraphic(actionButtons); // Add buttons for each row
                }
            }
        });

    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleUpdate(User user) {
        User loggedInUser = SessionManager.getInstance().getLoggedInUser();

        // Check permissions
        if ("user".equals(loggedInUser.getRole()) && !loggedInUser.getUsername().equals(user.getUsername())) {
            // Regular users can only update their own account
            showErrorAlert("Permission Denied", "You can only update your own account.");
            return;
        }

        // Show options for updating username or password
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Update Options");
        choiceAlert.setHeaderText("Update User Details");
        choiceAlert.setContentText("What do you want to update?");
        ButtonType updateUsername = new ButtonType("Update Username");
        ButtonType updatePassword = new ButtonType("Update Password");
        ButtonType cancel = new ButtonType("Cancel");

        choiceAlert.getButtonTypes().setAll(updateUsername, updatePassword, cancel);

        Optional<ButtonType> result = choiceAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == updateUsername) {
                // Update Username Logic
                TextInputDialog usernameDialog = new TextInputDialog(user.getUsername());
                usernameDialog.setTitle("Update Username");
                usernameDialog.setHeaderText("Update details for: " + user.getUsername());
                usernameDialog.setContentText("Enter new username:");

                Optional<String> newUsername = usernameDialog.showAndWait();
                newUsername.ifPresent(username -> {
                    try {
                        if (userService.updateUsername(user.getUsername(), username, loggedInUser)) {
                            showAlert("Success", "Username updated successfully!");
                            refreshTable();
                        } else {
                            showAlert("Error", "Failed to update username.");
                        }
                    } catch (SecurityException e) {
                        showErrorAlert("Permission Denied", e.getMessage());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to update username", e);
                        showAlert("Error", "An error occurred while updating username.");
                    }
                });
            } else if (result.get() == updatePassword) {
                // Update Password Logic — use PasswordField to mask input
                Dialog<String> passwordDialog = new Dialog<>();
                passwordDialog.setTitle("Update Password");
                passwordDialog.setHeaderText("Update Password for: " + user.getUsername());

                ButtonType confirmButtonType = new ButtonType("OK", ButtonType.OK.getButtonData());
                passwordDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

                PasswordField passwordField = new PasswordField();
                passwordField.setPromptText("Enter new password");
                passwordDialog.getDialogPane().setContent(passwordField);

                passwordDialog.setResultConverter(dialogButton -> {
                    if (dialogButton == confirmButtonType) {
                        return passwordField.getText();
                    }
                    return null;
                });

                Optional<String> newPassword = passwordDialog.showAndWait();
                newPassword.ifPresent(password -> {
                    try {
                        if (userService.updatePassword(user.getUsername(), password, loggedInUser)) {
                            showAlert("Success", "Password updated successfully!");
                            refreshTable();
                        } else {
                            showAlert("Error", "Failed to update password.");
                        }
                    } catch (SecurityException e) {
                        showErrorAlert("Permission Denied", e.getMessage());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to update password", e);
                        showAlert("Error", "An error occurred while updating password.");
                    }
                });
            }
        }
    }

    private void handleDelete(User user) {
        User loggedInUser = SessionManager.getInstance().getLoggedInUser();

        // Allow "admin" to delete any account; "user" can only delete their own account
        if ("user".equals(loggedInUser.getRole()) && !loggedInUser.getUsername().equals(user.getUsername())) {
            showErrorAlert("Permission Denied", "You can only delete your own account.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User");
        confirmAlert.setHeaderText("Are you sure you want to delete: " + user.getUsername() + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (userService.deleteUser(user.getUsername(), loggedInUser)) {
                    showAlert("Success", "User deleted successfully!");
                    refreshTable();
                } else {
                    showAlert("Error", "Failed to delete user.");
                }
            } catch (SecurityException e) {
                showErrorAlert("Permission Denied", e.getMessage());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to delete user", e);
                showAlert("Error", "An error occurred while deleting user.");
            }
        }
    }

    private void refreshTable() {
        try {
            ObservableList<User> users = FXCollections.observableArrayList(userService.getAllUsers());
            userTableView.setItems(users);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to refresh user table", e);
            showErrorAlert("Error", "Failed to refresh user table.");
        }
    }

}
