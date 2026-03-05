package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;

/**
 * SQLite-backed implementation of {@link FileRepository}.
 */
public class SQLiteFileRepository implements FileRepository {

    private final DB db;

    public SQLiteFileRepository() {
        this.db = new DB();
    }

    @Override
    public void insertFileRecord(String fileName, String uploadedBy) throws SQLException {
        db.insertFileRecord(fileName, uploadedBy);
    }

    @Override
    public void insertChunkRecord(String fileName, String chunkName, String parentFile) throws SQLException {
        db.insertChunkRecord(fileName, chunkName, parentFile);
    }

    @Override
    public List<String> findChunksByParentFile(String parentFile) throws SQLException {
        return db.getChunksForParent(parentFile);
    }

    @Override
    public void deleteChunksByParentFile(String parentFile) {
        db.deleteChunksForParent(parentFile);
    }

    @Override
    public void deleteFileRecord(String fileName) throws SQLException {
        db.deleteFileRecord(fileName);
    }

    @Override
    public List<FileRecord> findUploadedFiles() throws SQLException {
        return db.fetchUploadedFiles();
    }
}
