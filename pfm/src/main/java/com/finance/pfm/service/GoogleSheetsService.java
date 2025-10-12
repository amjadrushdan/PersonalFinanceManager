package com.finance.pfm.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class GoogleSheetsService {

        @Value("${google.sheets.credentials}")
        private String credentialsPath;

        @Value("${google.sheets.spreadsheet-id}")
        private String spreadsheetId;

        private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        private Sheets getSheetsService() throws Exception {
                InputStream in = new FileInputStream(System.getenv("GOOGLE_SERVICE_ACCOUNT_FILE"));

                GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                                .createScoped(List.of(SheetsScopes.SPREADSHEETS));

                return new Sheets.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                JSON_FACTORY,
                                new HttpCredentialsAdapter(credentials)).setApplicationName("PFM Bot").build();
        }

        public void addExpense(String date, String item, double amount, String merchant, String category)
                        throws Exception {
                Sheets sheets = getSheetsService();

                // Default values if null or empty
                if (merchant == null)
                        merchant = "";
                if (category == null || category.isEmpty())
                        category = "Other";

                ValueRange appendBody = new ValueRange().setValues(List.of(
                                List.of(date, item, amount, merchant, category)));

                sheets.spreadsheets().values()
                                .append(spreadsheetId, "Sheet1!A:E", appendBody)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
        }

}
