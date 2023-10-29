package com.RuneLingual;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;


public class DialogTranslator extends TranslationBuffer
{
    /***Manages the npc/player/quest dialog
     * Happens on a separate widget than the chat itself***/

    // chat box dialog widgets
    private Widget dialogOption;
    private Widget dialogSpriteText;
    private Widget dialogText;
    private Widget dialogLabel;
    private Widget dialogPlayerText;
    private Widget playerContinueButton;
    private Widget npcContinueButton;
    private Widget otherContinueButton;
    private Widget otherDialog;

    private String userName;  // player username
    private boolean hasChat;  // if the chat contents are changed, a widget update is needed
    private boolean allowGame;  // game text translations
    private boolean allowName;  // name translations

    private ArrayList<String> stringSymbols = new ArrayList<>(Arrays.asList("%NUMBER%", "%MONKEY_TALK%", "%USERNAME%"));


    public void translateAndReplace(Client client) throws Exception
    {
        /* TODO:
            - do NOT store translated text!
            - fix options getting duped on master files
            - use sprite "1181" (std spell book icon) as translation button
            - show something if a translation is not found
            - lookup chisel to scrape dialog lines
            - lookup string symbols
            - use npc ids
            - sprite texts
        */

        // resets dialog control variables
        hasChat = false;
        hasTranslation = false;

        if(dialogLabel != null && dialogText != null)
        {  // npc dialog
            String npcName = dialogLabel.getText().replace("<br>", "");
            String npcText = dialogText.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText(npcName, npcText);
                String newName = translated.transcript.getTranslatedText(npcName, "name");

                updateBuffer(npcName, newName);
                updateBuffer(npcText, newText);

                // replaces the dialog text and label
                if(allowGame) dialogText.setText(newText);
                if(allowName) dialogLabel.setText(newName);

                hasTranslation = true;
                hasChat = true;
            } catch (Exception e)
            {
                // if the given dialog does not have a found translation,
                // and it hasn't already been translated
                // adds it to the master transcript file

                if(!translatedContent.contains(npcText))
                {
                    master.transcript.addTranscript(npcName, npcText);
                }
            }

        } else if(dialogPlayerText != null)
        {  // player dialog

            String playerText = dialogPlayerText.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText("player", playerText);

                // updates last translated message
                updateBuffer("player", playerText);

                // replaces the dialog text
                if(allowGame) dialogPlayerText.setText(newText);

                hasTranslation = true;
                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(!translatedContent.contains(playerText))

                    master.transcript.addTranscript("player", playerText);
            }
        } else if(dialogOption != null)
        {   // selectable dialog list
            Widget[] optionWidgets = dialogOption.getParent().getChildren();

            for(Widget option: optionWidgets)
            { // gets all option texts

                if(option != null)
                {
                    String optionText = option.getText().replace("<br>", "");

                    if(translatedContent.contains(optionText))
                    {
                        // if this line is found on the buffer
                        // it probably was already translated,
                        // so it skips to the next iteration
                        continue;
                    }

                    try
                    {
                        String newText = translated.transcript.getTranslatedText("dialogOption", optionText);

                        updateBuffer("dialogOption", optionText);

                        // replaces the widget text
                        if(allowGame) option.setText(newText);

                        hasTranslation = true;
                        hasChat = true;
                    } catch (Exception e)
                    {
                        // if the dialog line is not found, adds it to database
                        if(!translatedContent.contains(optionText))
                            master.transcript.addTranscript("dialogOption", optionText);
                    }
                }
            }
        } else if(otherDialog != null)
        {   // other dialogs that does not have a sender (like quest info)
            String otherText = otherDialog.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText("other", otherText);

                // updates last translated message
                updateBuffer("other", otherText);

                // replaces the dialog text
                if(allowGame) this.otherDialog.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(!translatedContent.contains(otherText))
                    master.transcript.addTranscript("other", otherText);
            }
        }

        // continue buttons (various widgets)
        if(playerContinueButton != null)
        {
            String buttonText = playerContinueButton.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText("dialogFlow", buttonText);
                updateBuffer(newText, buttonText);

                // replaces the dialog text
                if(allowGame) playerContinueButton.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                if(!translatedContent.contains(buttonText))
                    master.transcript.addTranscript("dialogFlow", buttonText);
            }
        } else if(npcContinueButton != null)
        {
            String buttonText = npcContinueButton.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText("dialogFlow", buttonText);

                updateBuffer(newText, buttonText);

                // replaces the dialog text
                if(allowGame) npcContinueButton.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(!translatedContent.contains(buttonText))
                    master.transcript.addTranscript("dialogFlow", buttonText);
            }
        } else if(otherContinueButton != null)
        {
            String buttonText = otherContinueButton.getText().replace("<br>", "");
            try
            {
                String newText = translated.transcript.getTranslatedText("dialogFlow", buttonText);

                updateBuffer(newText, buttonText);

                // replaces the dialog text
                if(allowGame) otherContinueButton.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(!translatedContent.contains(buttonText))
                    master.transcript.addTranscript("dialogFlow", buttonText);
            }
        }


        if(this.dialogSpriteText != null)
        {
            System.out.println("new dialog sprite text: " + this.dialogOption.getText());
        }

        if(hasChat) client.refreshChat();
    }

    public void startup() throws Exception
    {
        /*** Load files and start translation API service if enabled ***/
        String langCodeUpper =  selectedLang.toUpperCase(Locale.ROOT);

        //this.setTRANSCRIPT_FOLDER_PATH(transcriptsFolderPath);
        //this.master.loadTranscripts();

        //this.translatedDialog.setFile(translationFilePath);
        //this.translatedDialog.loadTranscripts();
    }

    public void shutdown() throws Exception
    {
        if(newTranscriptsFound)
        {
            //this.dialogMaster.saveTranscript();
        }
        // if no changes were found, no need to save
    }
    public void updateWidgets(Client client)
    {
        /*** Updates internal widgets ***/
        this.dialogOption = client.getWidget(WidgetInfo.DIALOG_OPTION);
        //this.dialogOptionOptions = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
        this.dialogSpriteText = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
        this.dialogLabel = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
        this.dialogText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        this.dialogPlayerText = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);
        this.otherDialog = client.getWidget(WidgetID.CHATBOX_GROUP_ID, 1);

        // no WidgetInfo for the dialog continue button i guess...
        this.playerContinueButton = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 5);
        this.npcContinueButton = client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 5);
        this.otherContinueButton = client.getWidget(WidgetID.CHATBOX_GROUP_ID,2);
    }

    public void setAllowName(boolean allowName){this.allowName = allowName;}
    public void setAllowGame(boolean allowGame){this.allowGame = allowGame;}

}
