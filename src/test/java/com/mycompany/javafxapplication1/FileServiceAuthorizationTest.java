package com.mycompany.javafxapplication1;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileServiceAuthorizationTest {

    private InMemoryFileRepository fileRepository;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileRepository = new InMemoryFileRepository();
        fileService = new FileService(fileRepository);
    }

    @Test
    void ownerShouldManageOwnFile() {
        User owner = new User("alice", "ignored", "user");
        assertDoesNotThrow(() -> fileService.assertCanManageFile("report.txt", owner));
    }

    @Test
    void adminShouldManageAnyFile() {
        User admin = new User("root", "ignored", "admin");
        assertDoesNotThrow(() -> fileService.assertCanManageFile("report.txt", admin));
    }

    @Test
    void nonOwnerNonAdminShouldBeDenied() {
        User otherUser = new User("bob", "ignored", "user");
        assertThrows(SecurityException.class, () -> fileService.assertCanManageFile("report.txt", otherUser));
    }

    static class InMemoryFileRepository implements FileRepository {

        private final List<FileRecord> files = new ArrayList<>();

        InMemoryFileRepository() {
            files.add(new FileRecord("report.txt", "alice", "2026-03-04T12:00:00"));
        }

        @Override
        public void insertFileRecord(String fileName, String uploadedBy) {
        }

        @Override
        public void insertChunkRecord(String fileName, String chunkName, String parentFile) {
        }

        @Override
        public List<String> findChunksByParentFile(String parentFile) {
            return List.of();
        }

        @Override
        public void deleteChunksByParentFile(String parentFile) {
        }

        @Override
        public void deleteFileRecord(String fileName) {
        }

        @Override
        public List<FileRecord> findUploadedFiles() {
            return files;
        }
    }
}
