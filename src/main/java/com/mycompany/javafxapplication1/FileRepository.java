package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for file record and chunk persistence.
 */
public interface FileRepository {
    void insertFileRecord(String fileName, String uploadedBy) throws SQLException;

    void insertChunkRecord(String fileName, String chunkName, String parentFile) throws SQLException;

    List<String> findChunksByParentFile(String parentFile) throws SQLException;

    void deleteChunksByParentFile(String parentFile);

    void deleteFileRecord(String fileName) throws SQLException;

    List<FileRecord> findUploadedFiles() throws SQLException;
}
