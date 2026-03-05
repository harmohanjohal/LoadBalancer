package com.mycompany.javafxapplication1;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * Utility class for file operations: splitting, merging, compressing,
 * encrypting and decrypting files.
 */
public class FileUtils {

    public static final long CHUNK_SIZE = 1024 * 1024; // 1 MB
    static final String UPLOADS_DIR = "uploads";

    private FileUtils() {
        // Prevent instantiation of utility class
    }

    public static List<File> splitFile(File inputFile) throws IOException {
        List<File> chunkFiles = new ArrayList<>();
        byte[] buffer = new byte[(int) CHUNK_SIZE];
        ensureSafeFileName(inputFile.getName());

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            int bytesRead;
            int partNumber = 0;

            while ((bytesRead = fis.read(buffer)) > 0) {
                File chunk = new File(UPLOADS_DIR + "/" + inputFile.getName() + ".part" + partNumber);
                try (FileOutputStream fos = new FileOutputStream(chunk)) {
                    fos.write(buffer, 0, bytesRead);
                }
                chunkFiles.add(chunk);
                partNumber++;
            }
        }
        return chunkFiles;
    }

    public static void mergeChunks(File destinationFile, List<File> chunkFiles) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            for (File chunk : chunkFiles) {
                try (FileInputStream fis = new FileInputStream(chunk)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    public static void compressChunks(String fileName, List<File> chunks) throws IOException {
        ensureSafeFileName(fileName);
        File zipFile = new File(UPLOADS_DIR + "/" + fileName + ".zip");
        zipFile.getParentFile().mkdirs(); // Ensure directory exists

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File chunk : chunks) {
                try (FileInputStream fis = new FileInputStream(chunk)) {
                    ZipEntry zipEntry = new ZipEntry(chunk.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    public static List<File> decompressChunks(String zipFilePath, File outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            zipFile.extractAll(outputDir.getAbsolutePath());
        } catch (ZipException e) {
            throw new IOException("Failed to decompress chunks: " + e.getMessage(), e);
        }
        File[] files = outputDir.listFiles();
        return files != null ? List.of(files) : new ArrayList<>();
    }

    /**
     * Encrypts a file using AES encryption via Zip4j. The password is read
     * from the {@code FILE_ENCRYPTION_PASSWORD} environment variable.
     *
     * @param inputFile     The file to encrypt.
     * @param outputZipFile The destination for the encrypted zip file.
     * @throws IOException if encryption fails or the input file does not exist.
     */
    public static void encryptFile(File inputFile, File outputZipFile) throws IOException {
        if (!inputFile.exists()) {
            throw new IOException("Input file does not exist.");
        }
        ensureSafeFileName(inputFile.getName());

        try {
            try (ZipFile zipFile = new ZipFile(outputZipFile, getEncryptionPassword())) {
                ZipParameters zipParameters = new ZipParameters();
                zipParameters.setEncryptFiles(true);
                zipParameters.setEncryptionMethod(EncryptionMethod.AES);
                zipFile.addFile(inputFile, zipParameters);
            }
        } catch (ZipException e) {
            throw new IOException("Failed to encrypt file: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an AES-encrypted zip file via Zip4j. The password is read
     * from the {@code FILE_ENCRYPTION_PASSWORD} environment variable.
     *
     * @param zipFile   The encrypted zip file to decrypt.
     * @param outputDir The folder where the decrypted content will be placed.
     * @throws IOException if decryption fails or the zip file does not exist.
     */
    public static void decryptFile(File zipFile, File outputDir) throws IOException {
        if (!zipFile.exists()) {
            throw new IOException("Encrypted zip file does not exist.");
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (ZipFile zip = new ZipFile(zipFile, getEncryptionPassword())) {
            zip.extractAll(outputDir.getAbsolutePath());
        } catch (ZipException e) {
            throw new IOException("Failed to decrypt file: " + e.getMessage(), e);
        }
    }

    private static char[] getEncryptionPassword() throws IOException {
        String password = System.getenv(AppConfig.ENV_FILE_ENCRYPTION_PASSWORD);
        if (password == null || password.isBlank()) {
            throw new IOException("Missing encryption password. Set environment variable '"
                    + AppConfig.ENV_FILE_ENCRYPTION_PASSWORD + "'.");
        }
        return password.toCharArray();
    }

    private static void ensureSafeFileName(String fileName) throws IOException {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9._-]+")) {
            throw new IOException("Invalid file name: " + fileName);
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }

        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory.getAbsolutePath());
        }
    }
}
