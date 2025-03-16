package com.example.unifyu2.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.example.unifyu2.models.Event;
import com.example.unifyu2.models.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelExporter {
    private static final String[] HEADERS = {"Name", "Email", "Phone", "Registration Date"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public static void exportParticipants(Context context, Event event, List<User> participants) {
        String fileName = "participants_" + event.getEventId() + ".csv";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File outputFile = new File(downloadsDir, fileName);

        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write headers
            writer.write(String.join(",", HEADERS) + "\n");

            // Get phone numbers map
            Map<String, String> phoneNumbers = event.getRegisteredUsers();

            // Write participant data
            for (User participant : participants) {
                String phone = phoneNumbers != null ? phoneNumbers.get(participant.getId()) : "";
                if (phone == null) phone = "";
                
                String[] rowData = {
                    escapeSpecialCharacters(participant.getUsername()),
                    escapeSpecialCharacters(participant.getEmail()),
                    escapeSpecialCharacters(phone),
                    DATE_FORMAT.format(new Date())
                };
                writer.write(String.join(",", rowData) + "\n");
            }

            Toast.makeText(context, 
                "Participant list exported to Downloads/" + fileName, 
                Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, 
                "Failed to export participant list: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\"", "\"\"");
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("\n")) {
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }
} 