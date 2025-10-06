package com.finance.pfm.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class GoogleSheetsService {

    @Value("${google.sheets.credentials}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private Sheets getSheetsService() throws Exception {
        InputStream in = getClass().getResourceAsStream("/credentials/pfm-service-account.json");

        if (in == null) {
            throw new IllegalStateException("Cannot find Google credentials file: " + credentialsPath);
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("PFM Bot").build();
    }

    // public void addExpense(String description, double amount) throws Exception {
    //     Sheets sheets = getSheetsService();

    //     ValueRange appendBody = new ValueRange().setValues(List.of(
    //             List.of(LocalDate.now().toString(), description, amount)
    //     ));

    //     sheets.spreadsheets().values()
    //             .append(spreadsheetId, "Sheet1!A:C", appendBody)
    //             .setValueInputOption("USER_ENTERED")
    //             .execute();
    // }
    @Autowired
    private CategoryService categoryService;

    public void addExpense(String description, double amount) throws Exception {
        Sheets sheets = getSheetsService();
        // String category = categoryService.classifyExpense(description);
        String category = "Other"; // AI classification disabled


        ValueRange appendBody = new ValueRange().setValues(List.of(
                List.of(LocalDate.now().toString(), description, amount, "", category)
        ));

        sheets.spreadsheets().values()
                .append(spreadsheetId, "Sheet1!A:E", appendBody)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }
}
