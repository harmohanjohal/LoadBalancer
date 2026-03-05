package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for file management operations, delegating persistence
 * to a {@link FileRepository} implementation.
 */
public class FileService {

    private final FileRepository fileRepository;

    public FileService() {
        this(new SQLiteFileRepository());
    }

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void registerUploadedFile(String fileName, String uploadedBy, List<String> chunkNames) throws SQLException {
        fileRepository.insertFileRecord(fileName, uploadedBy);
        for (String chunkName : chunkNames) {
            fileRepository.insertChunkRecord(fileName, chunkName, fileName);
        }
    }

    public List<FileRecord> getUploadedFiles() throws SQLException {
        return fileRepository.findUploadedFiles();
    }

    public List<String> getChunkNames(String parentFile) throws SQLException {
        return fileRepository.findChunksByParentFile(parentFile);
    }

    public void deleteFileAndChunks(String fileName) throws SQLException {
        fileRepository.deleteChunksByParentFile(fileName);
        fileRepository.deleteFileRecord(fileName);
    }

    public void assertCanManageFile(String fileName, User actor) throws SQLException {
        if (actor == null) {
            throw new SecurityException("You must be logged in to manage files.");
        }

        Optional<FileRecord> fileRecord = fileRepository.findUploadedFiles().stream()
                .filter(record -> record.getFileName().equals(fileName))
                .findFirst();

        if (fileRecord.isEmpty()) {
            throw new SecurityException("File record not found.");
        }

        boolean isAdmin = "admin".equalsIgnoreCase(actor.getRole());
        boolean isOwner = actor.getUsername().equals(fileRecord.get().getUploadedBy());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("You are not authorized to manage this file.");
        }
    }
}
