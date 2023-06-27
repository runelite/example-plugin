package com.RuneLingual;

import java.util.LinkedList;

public abstract class TranslationBuffer
{
    protected String selectedLang;
    protected String TRANSCRIPT_FOLDER_PATH;
    protected String FILE_NAME;
    protected TranscriptsDatabaseManager master = new TranscriptsDatabaseManager();
    protected TranscriptsDatabaseManager translated = new TranscriptsDatabaseManager();
    protected boolean hasTranslation;
    protected boolean newTranscriptsFound;
    protected LinkedList<String> translatedContent;
    protected LinkedList<String> originalContent;
    protected int bufferSize = 20;  // translation buffer size

    protected void updateBuffer(String original, String translated)
    {
        // assuming both internal buffers always have the same size
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

    protected void setLang(String selectedLang) {this.selectedLang = selectedLang;}
    protected void setTRANSCRIPT_FOLDER_PATH(String TRANSCRIPT_FOLDER_PATH) {this.TRANSCRIPT_FOLDER_PATH = TRANSCRIPT_FOLDER_PATH;}
    protected void setFILE_NAME(String FILE_NAME) {this.FILE_NAME = FILE_NAME;}

}
