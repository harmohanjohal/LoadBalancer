package com.mycompany.javafxapplication1;

/**
 * Immutable data carrier representing a file record stored in the database.
 * <p>
 * JavaBean-style getters are provided alongside the canonical record accessors
 * so that JavaFX {@code PropertyValueFactory} can locate the properties by name.
 */
public record FileRecord(String fileName, String uploadedBy, String uploadDate) {

    public String getFileName() {
        return fileName;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploadDate() {
        return uploadDate;
    }
}
