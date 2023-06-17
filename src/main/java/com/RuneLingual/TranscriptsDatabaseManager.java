package com.RuneLingual;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TranscriptsDatabaseManager {
    public String FILE_PATH = new String();
    public LingualTranscript transcript = new LingualTranscript();

    public void saveTranscript() throws Exception
    {

        /* This should only be called to update the master database

        @param name: the label of the object that should be saved
        @param text: the contents of the object that should be saved

        Only useful for master database.
        Given contents will be added as new entries.*/
        String filePath = FILE_PATH;
        File file = new File(filePath);
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file)))
        {
            if (!file.exists())
            {
                if (!file.getParentFile().exists())
                {
                    if (!file.getParentFile().mkdirs())
                    {
                        throw new IOException("Failed to create parent directories.");
                    }
                }
                if (!file.createNewFile())
                {
                    throw new IOException("Failed to create new file.");
                }
            }
            System.out.println("File created or accessed successfully");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(transcript);
            outputStream.writeObject(json);
        } catch (IOException e) {
            throw new Exception("Could not update transcripts on master" + e + e.getMessage());
        }
    }

    public void loadTranscripts() throws Exception
    {
        // load a translation db - treat as stream in distribution
        String filePath = FILE_PATH;
        File file = new File(filePath);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            String json = (String) objectInputStream.readObject();
            Gson gson = new Gson();
            transcript = gson.fromJson(json, LingualTranscript.class);
            System.out.println("Transcript was loaded successfully.");
            System.out.println("Aqui: " + transcript);
        } catch (IOException | ClassNotFoundException e) {
            this.saveTranscript();
            //throw new Exception("Could not load translation files: " + e.getMessage());
        }
    }


    public void setFile(String filePath){this.FILE_PATH = filePath;}
}

