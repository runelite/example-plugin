package com.RuneLingual;

import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TranscriptsDatabaseManager {
    public String FILE_PATH = new String();
    public TranscriptManager transcript = new TranscriptManager();

    public void saveTranscript() throws Exception {
        String filePath = FILE_PATH;
        File file = new File(filePath);

        if (file == null) {
            throw new Exception("Invalid file");
        }

        try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directories.");
                    }
                }
                if (!file.createNewFile()) {
                    throw new IOException("Failed to create new file.");
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.setIndent("    ");
            gson.toJson(transcript, TranscriptManager.class, writer);
            System.out.println("File created or accessed successfully");
        } catch (IOException e) {
            throw new Exception("Could not update transcripts on master: " + e.getMessage());
        }
    }

    public void loadTranscripts() throws Exception {
        String filePath = FILE_PATH;
        File file = new File(filePath);

        if (file.exists()) {
            if (file.length() > 0) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }

                    String json = jsonBuilder.toString();
                    if (json.trim().isEmpty()) {
                        System.out.println("Empty JSON file");
                        transcript = new TranscriptManager();
                    } else {
                        // Manually check if JSON is an object
                        if (json.startsWith("{") && json.endsWith("}")) {
                            Gson gson = new Gson();
                            transcript = gson.fromJson(json, TranscriptManager.class);
                            System.out.println("Transcript was loaded successfully.");
                            System.out.println("Aqui: " + transcript);
                        } else {
                            System.out.println("Invalid JSON format. Expected an object.");
                            transcript = new TranscriptManager();
                        }
                    }
                } catch (IOException e) {
                    throw new Exception("Could not load translation files: " + e.getMessage());
                }
            } else {
                System.out.println("Empty file");
                transcript = new TranscriptManager();
            }
        } else {
            throw new Exception("Given file does not exist!");
        }
    }

    public void setFile(String filePath){this.FILE_PATH = filePath;}
}
