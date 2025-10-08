package com.finance.pfm.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

@Service
public class GoogleDriveService {

    @Value("${google.drive.folder-id}")
    private String folderId; // Optional folder in Drive

    private static Drive driveService;
    private static final String APPLICATION_NAME = "PFM Bot";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    private static final String TOKENS_DIR = System.getProperty("user.home") + "/.pfm_bot_tokens";
    private static final String CREDENTIALS_PATH = System.getenv("GOOGLE_OAUTH2_CREDENTIALS");

    public GoogleDriveService() throws Exception {
        if (CREDENTIALS_PATH == null || CREDENTIALS_PATH.isEmpty()) {
            throw new RuntimeException("Environment variable GOOGLE_OAUTH2_CREDENTIALS not set");
        }

        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(new FileInputStream(CREDENTIALS_PATH))
        );

        // Build authorization flow with token storage
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                SCOPES
        )
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIR)))
        .setAccessType("offline")
        .build();

        Credential credential;

        // Check if token exists
        if (flow.loadCredential("user") != null) {
            credential = flow.loadCredential("user");
        } else {
            // Manual code flow
            String authUrl = flow.newAuthorizationUrl().setRedirectUri("urn:ietf:wg:oauth:2.0:oob").build();
            System.out.println("Open this URL in your browser:");
            System.out.println(authUrl);
            System.out.print("Enter the authorization code: ");
            try (Scanner scanner = new Scanner(System.in)) {
                String code = scanner.nextLine().trim();

                TokenResponse tokenResponse = flow.newTokenRequest(code)
                        .setRedirectUri("urn:ietf:wg:oauth:2.0:oob")
                        .execute();

                credential = flow.createAndStoreCredential(tokenResponse, "user");
            }
        }

        // Build Drive service
        driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }

    public String uploadFile(java.io.File localFile, String timestamp) throws IOException {
        String fileName = timestamp + ".jpg";

        File fileMetadata = new File();
        fileMetadata.setName(fileName);

        if (folderId != null && !folderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }

        FileContent mediaContent = new FileContent("image/jpeg", localFile);
        driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, parents")
                .execute();

        return fileName; // return filename for confirmation message
    }

}
