package com.RuneLingual;

import java.util.LinkedList;

public abstract class TranslationBuffer
{
    protected String selectedLang = null;
    protected String TRANSCRIPT_FOLDER_PATH = null;

    protected TranscriptsDatabaseManager master = new TranscriptsDatabaseManager();
    protected TranscriptsDatabaseManager translated = new TranscriptsDatabaseManager();

    protected boolean hasTranslation;
    protected boolean newTranscriptsFound;

    protected LinkedList<String> translatedContent;
    protected LinkedList<String> originalContent;

    protected int bufferSize = 50;  // translation buffer size

    protected void updateBuffer(String original, String translated)
    {
        // Assuming both internal buffers always have the same size
        if(translatedContent.size() >= bufferSize)
        {
            translatedContent.removeFirst();
            translatedContent.addLast(translated);

            originalContent.removeFirst();
            originalContent.addLast(original);
        } else
        {
            translatedContent.addLast(translated);
            originalContent.addLast(original);
        }
    }

    protected void setLang(String newLang) {
        this.selectedLang = newLang;
    }

    protected void setTranslationFilePath(String translationFilePath) throws Exception {
        // Requires a language to be previously set to load files
        if (this.selectedLang == null)
            throw new Exception("undefinedLang");

        // updates folder path and open transcript files
        this.TRANSCRIPT_FOLDER_PATH = translationFilePath.toUpperCase();

        String masterFilePath = "MASTER_ " + translationFilePath.toUpperCase();
    }
}
