package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.geometry.Pos;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Controller for the file upload and management screen.
 */
public class FileUploadController {

    private final FileService fileService = new FileService();
    private static final Logger logger = Logger.getLogger(FileUploadController.class.getName());
    private static final String UPLOADS_DIR = FileUtils.UPLOADS_DIR;

    @FXML
    private Button backBtn;
    @FXML
    private Button downloadBtn;
    @FXML
    private TableView<FileRow> fileTableView;
    @FXML
    private TableColumn<FileRow, String> fileNameColumn;
    @FXML
    private TableColumn<FileRow, String> uploadDateColumn;
    @FXML
    private TableColumn<FileRow, String> uploadedByColumn;
    @FXML
    private TableColumn<FileRow, Void> actionsColumn;
    @FXML
    private Button uploadButton;

    private final ObservableList<FileRow> fileRows = FXCollections.observableArrayList();
    private final User loggedInUser = SessionManager.getInstance().getLoggedInUser();

    @FXML
    private void initialize() {
        fileNameColumn.setCellValueFactory(data -> data.getValue().fileNameProperty());
        uploadDateColumn.setCellValueFactory(data -> data.getValue().uploadDateProperty());
        uploadedByColumn.setCellValueFactory(data -> data.getValue().uploadedByProperty());
        addActionsColumn();
        loadUploadedFiles();
    }

    private void addActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.getStyleClass().add("btn-primary");
                deleteButton.getStyleClass().add("btn-danger");

                editButton.setOnAction(event -> {
                    FileRow data = getTableView().getItems().get(getIndex());
                    handleEditAction(data);
                });

                deleteButton.setOnAction(event -> {
                    FileRow data = getTableView().getItems().get(getIndex());
                    handleDeleteAction(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10, editButton, deleteButton);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }

    @FXML
    private void backBtnHandler(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("secondary.fxml"));
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 860, 540));
            stage.setTitle("User Management");
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error navigating back", e);
        }
    }

    @FXML
    private void uploadFileHandler() {
        if (loggedInUser == null) {
            showErrorAlert("Not Logged In", "You must be logged in to upload files.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File");
            File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());

            if (selectedFile != null) {
                String fileName = selectedFile.getName();
                new File(UPLOADS_DIR).mkdirs();
                String encryptedFileName = UPLOADS_DIR + "/" + fileName + ".zip";
                File encryptedFile = new File(encryptedFileName);

                // Encrypt the file
                FileUtils.encryptFile(selectedFile, encryptedFile);

                // Split the encrypted file into chunks
                List<File> chunks = FileUtils.splitFile(encryptedFile);

                // Insert file record into the database
                String uploader = loggedInUser.getUsername();
                List<String> chunkNames = new ArrayList<>();
                for (File chunk : chunks) {
                    chunkNames.add(chunk.getName());
                }
                fileService.registerUploadedFile(fileName, uploader, chunkNames);

                // Refresh file list
                loadUploadedFiles();
                showInfoAlert("Success", "File uploaded and encrypted successfully.");
            } else {
                showErrorAlert("No File Selected", "Please select a file to upload.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "File upload failed", e);
            showErrorAlert("Error", "Failed to upload the file.");
        }
    }

    private void handleEditAction(FileRow fileRow) {
        try {
            fileService.assertCanManageFile(fileRow.getFileName(), loggedInUser);
            File encryptedFile = new File(UPLOADS_DIR + "/" + fileRow.getFileName() + ".zip");
            File tempFolder = new File("temp/" + fileRow.getFileName());
            FileUtils.decryptFile(encryptedFile, tempFolder);
            File decryptedFile = new File(tempFolder, fileRow.getFileName());
            openEditFileScreen(decryptedFile);
        } catch (SecurityException e) {
            showErrorAlert("Access Denied", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error decompressing file for editing", e);
            showErrorAlert("Error", "Failed to decompress the file for editing.");
        }
    }

    private void openEditFileScreen(File decryptedFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("edit_file.fxml"));
            Parent root = loader.load();

            EditFileController controller = loader.getController();
            controller.initialize(decryptedFile);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit File - " + decryptedFile.getName());
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening edit file screen", e);
            showErrorAlert("Error", "Could not open the edit file screen.");
        }
    }

    private void handleDeleteAction(FileRow fileRow) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete Confirmation");
        confirmationAlert.setHeaderText("Are you sure?");
        confirmationAlert.setContentText("This will permanently delete the file: " + fileRow.getFileName());

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                fileService.assertCanManageFile(fileRow.getFileName(), loggedInUser);
                // Fetch and delete chunks from the file system
                List<String> chunks = fileService.getChunkNames(fileRow.getFileName());
                for (String chunkName : chunks) {
                    File chunkFile = new File(UPLOADS_DIR + "/" + chunkName);
                    if (chunkFile.exists()) {
                        chunkFile.delete();
                    }
                }

                // Delete chunk and parent file records from the database
                fileService.deleteFileAndChunks(fileRow.getFileName());

                // Delete the parent compressed file
                File zipFile = new File(UPLOADS_DIR + "/" + fileRow.getFileName() + ".zip");
                if (zipFile.exists()) {
                    zipFile.delete();
                }

                loadUploadedFiles(); // Refresh file list
                showInfoAlert("Success", "File deleted successfully.");
            } catch (SecurityException e) {
                showErrorAlert("Access Denied", e.getMessage());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error deleting file", e);
                showErrorAlert("Error", "Failed to delete the file.");
            }
        }
    }

    private void loadUploadedFiles() {
        fileRows.clear();
        try {
            List<FileRecord> uploadedFiles = fileService.getUploadedFiles();
            for (FileRecord uploadedFile : uploadedFiles) {
                fileRows.add(new FileRow(uploadedFile.getFileName(), uploadedFile.getUploadedBy(), uploadedFile.getUploadDate()));
            }
            fileTableView.setItems(fileRows);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load uploaded files", e);
            showErrorAlert("Error", "Could not load uploaded files.");
        }
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

    public static class FileRow {

        private final StringProperty fileName;
        private final StringProperty uploader;
        private final StringProperty uploadDate;

        public FileRow(String fileName, String uploader, String uploadDate) {
            this.fileName = new SimpleStringProperty(fileName);
            this.uploader = new SimpleStringProperty(uploader);
            this.uploadDate = new SimpleStringProperty(uploadDate);
        }

        public String getFileName() {
            return fileName.get();
        }

        public void setFileName(String fileName) {
            this.fileName.set(fileName);
        }

        public StringProperty fileNameProperty() {
            return fileName;
        }

        public String getUploader() {
            return uploader.get();
        }

        public void setUploader(String uploader) {
            this.uploader.set(uploader);
        }

        public StringProperty uploadedByProperty() {
            return uploader;
        }

        public StringProperty uploadDateProperty() {
            return uploadDate;
        }
    }

    @FXML
    private void downloadFileHandler() {
        FileRow selectedFile = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showErrorAlert("No File Selected", "Please select a file to download.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File As");
        fileChooser.setInitialFileName(selectedFile.getFileName());
        File destinationFile = fileChooser.showSaveDialog(uploadButton.getScene().getWindow());

        if (destinationFile != null) {
            File mergedFile = null;
            File tempOutputDir = null;
            try {
                fileService.assertCanManageFile(selectedFile.getFileName(), loggedInUser);
                // Fetch chunks from the database
                List<String> chunkNames = fileService.getChunkNames(selectedFile.getFileName());
                List<File> chunkFiles = new ArrayList<>();
                for (String chunkName : chunkNames) {
                    chunkFiles.add(new File(UPLOADS_DIR + "/" + chunkName));
                }

                // Merge chunks and decrypt the file
                mergedFile = new File("temp/" + selectedFile.getFileName() + ".zip");
                FileUtils.mergeChunks(mergedFile, chunkFiles);

                tempOutputDir = new File("temp/decrypted_" + selectedFile.getFileName());
                FileUtils.decryptFile(mergedFile, tempOutputDir);

                File decryptedFile = new File(tempOutputDir, selectedFile.getFileName());
                Files.copy(decryptedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                showInfoAlert("Success", "File downloaded and decrypted successfully.");
            } catch (SecurityException e) {
                showErrorAlert("Access Denied", e.getMessage());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "File download failed", e);
                showErrorAlert("Error", "Failed to download the file.");
            } finally {
                safeDeleteFile(mergedFile);
                safeDeleteDirectory(tempOutputDir);
            }
        }
    }

    private void safeDeleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.delete()) {
            logger.log(Level.WARNING, "Failed to delete temporary file: {0}", file.getAbsolutePath());
        }
    }

    private void safeDeleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete temporary directory: {0}", directory.getAbsolutePath());
        }
    }

}
