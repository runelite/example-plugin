package com.RuneLingual;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TranscriptsDatabaseManager
{
    private String filePath;
    private String currentLang;
    public TranscriptManager transcript = new TranscriptManager();
    
    private boolean debugPrints;
    private LogHandler logger;
    private boolean hasCrashed;
    
    public TranscriptsDatabaseManager()
    {
        // TODO: change this to false sometime later
        debugPrints = true;
        
        this.filePath = "/master.json";
        this.transcript.setLogger(this.logger);
    }
    
    public void saveTranscript() throws Exception
    {
        try
        {
            URL resourceUrl = getClass().getResource(filePath);
            OutputStream outputStream = new FileOutputStream(new File(resourceUrl.toURI()));
            
            if(outputStream == null)
            {
                throw new Exception("Could not access or modify file!");
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            
            writer.setIndent("\t");
            gson.toJson(transcript, TranscriptManager.class, writer);
            
            if(debugPrints)
            {
                logger.log("File updated successfully");
            }
            
            writer.close();
            //outputStream.close();
            
        }
        catch (IOException e)
        {
            throw new Exception("Could not update transcripts on master: " + e.getMessage());
        }
    }
    
    public void loadTranscripts() throws Exception
    {
        InputStream inputStream = getClass().getResourceAsStream(filePath);
        logger.log("Now trying to load file: " + filePath);
        if(inputStream != null)
        {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
            {
                StringBuilder jsonBuilder = new StringBuilder();
                
                String line;
                
                while((line = reader.readLine()) != null)
                {
                    jsonBuilder.append(line);
                }
                
                String json = jsonBuilder.toString();
                if(json.trim().isEmpty())
                {
                    if(debugPrints)
                    {
                        logger.log("Empty JSON file");
                    }
                    
                    transcript = new TranscriptManager();
                }
                else
                {
                    // Manually check if JSON is a valid object
                    if(json.startsWith("{") && json.endsWith("}"))
                    {
                        Gson gson = new Gson();
                        transcript = gson.fromJson(json, TranscriptManager.class);
                        if(debugPrints)
                        {
                            logger.log("Transcript was loaded successfully.");
                        }
                    }
                    else
                    {
                        if(debugPrints)
                        {
                            logger.log("Invalid JSON format. Expected an object.");
                        }
                        
                        transcript = new TranscriptManager();
                    }
                }
            }
            catch(IOException e)
            {
                throw new Exception("Could not load translation files: " + e.getMessage());
            }
        }
        else
        {
            logger.log("Empty file");
            transcript = new TranscriptManager();
        }
    }
    
    public void setLogger(LogHandler logger)
    {
        this.logger = logger;
        this.transcript.setLogger(logger);
    }
    
    public void setFile(String filePath) {this.filePath = filePath;}
    
    public void setTargetLang(String newLang) {this.currentLang = newLang;}
}
