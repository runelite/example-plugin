package com.RuneLingual;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.util.Text;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.ArrayList;


public class DialogTranslator
{
    /***Manages the ChatBox itself, not the actual chat widget (seen on onGameTick)
     *
     * new ChatMessage is the event***/

    private Widget dialogOption;
    private Widget dialogOptionOptions;
    private Widget dialogSpriteText;
    private Widget dialogText;
    private Widget dialogLabel;
    private Widget dialogPlayerText;
    private Widget playerContinueButton;
    private Widget npcContinueButton;
    private ArrayList<Widget> selectableOptions;

    private String currentMessage;  // the last translated message
    private String currentSender;  // the last translated message sender
    private String flowMessage;
    private String flowSender;
    private String selectedLang;
    private String TRANSCRIPT_FOLDER_PATH;
    private String MASTER_NPC_DIALOG_TRANSCRIPT_SUFIX = new String("MASTER_NPC_DIALOG_TRANSCRIPT.json");
    private String TRANSLATED_NPC_DIALOG_TRANSCRIPT_SUFIX = new String("_NPC_DIALOG_TRANSCRIPT.json");
    private TranscriptsDatabaseManager dialogMaster = new TranscriptsDatabaseManager();
    private TranscriptsDatabaseManager translatedDialog = new TranscriptsDatabaseManager();

    // if said type of message should be translated
    private boolean hasChat;
    private boolean allowGame;
    private boolean allowName;
    public void translateAndReplace(Client client) throws Exception
    {
        // TODO: do NOT store translated text!
        // TODO: click here to continue - Id 15138821
        // TODO: use sprite "1181" as translation button

        hasChat = false;  // resets

        // tries to translate and replace chat messages
        if(this.dialogLabel != null && this.dialogText != null)
        {  // npc dialog
            String senderName = this.dialogLabel.getText();
            String dialogText = this.dialogText.getText().replace("<br>", "");
            try
            {
                String newText = this.translatedDialog.transcript.getTranslatedText(senderName, dialogText);
                String newName = this.translatedDialog.transcript.getTranslatedText(senderName, "name");

                // updates last translated message
                currentMessage = newText;
                currentSender = newName;

                // replaces the dialog text and label
                if(allowGame) this.dialogText.setText(newText);
                if(allowName) this.dialogLabel.setText(newName);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, and it hasn't already been translated - adds it to database
                if(dialogText != currentMessage && senderName != currentSender)
                    this.dialogMaster.transcript.addTranscript(senderName, dialogText);
            }

        } else if(this.dialogPlayerText != null)
        {  // player messages

            String dialogText = this.dialogPlayerText.getText().replace("<br>", "");
            try
            {
                String newText = this.translatedDialog.transcript.getTranslatedText("player", dialogText);

                // updates last translated message
                currentSender = "player";
                currentMessage = newText;

                // replaces the dialog text
                if(allowGame) this.dialogPlayerText.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(dialogText != currentMessage)

                    this.dialogMaster.transcript.addTranscript("player", dialogText);
            }
        } else if(this.dialogOption != null)
        {   // selectable dialog list
            Widget[] widgets = this.dialogOption.getParent().getChildren();
            boolean alreadyTranslated = false;

            for(Widget widget: widgets)
            {
                if(widget.getText() == currentMessage)
                    alreadyTranslated = true;
            }

            // TODO: fix options getting duped on master files
            for(Widget widget: widgets)
            { // gets all option texts
                if(widget != null)
                {
                    String dialogText = widget.getText().replace("<br>", "");
                    try
                    {
                        String newText = this.translatedDialog.transcript.getTranslatedText("playerOption", dialogText);

                        // updates last translated message
                        currentSender = "playerOption";
                        currentMessage = newText;

                        // replaces the dialog text
                        if(allowGame) widget.setText(newText);

                        hasChat = true;
                    } catch (Exception e)
                    {
                        // if the dialog line is not found, adds it to database
                        if(alreadyTranslated == false)
                            this.dialogMaster.transcript.addTranscript("playerOption", dialogText);
                    }
                }
            }
        }

        // dialog flow
        if(this.playerContinueButton != null)
        {
            String buttonText = this.playerContinueButton.getText().replace("<br>", "");
            try
            {
                String newText = this.translatedDialog.transcript.getTranslatedText("dialogFlow", buttonText);

                // updates last translated message
                flowSender = "dialogFlow";
                flowMessage = newText;

                // replaces the dialog text
                if(allowGame) this.playerContinueButton.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(buttonText != flowMessage)
                    this.dialogMaster.transcript.addTranscript("dialogFlow", buttonText);
            }
        } else if(this.npcContinueButton != null)
        {
            String buttonText = this.npcContinueButton.getText().replace("<br>", "");
            try
            {
                String newText = this.translatedDialog.transcript.getTranslatedText("dialogFlow", buttonText);

                // updates last translated message
                flowSender = "dialogFlow";
                flowMessage = newText;

                // replaces the dialog text
                if(allowGame) this.npcContinueButton.setText(newText);

                hasChat = true;
            } catch (Exception e)
            {
                // if the dialog line is not found, adds it to database
                if(buttonText != flowMessage)
                    this.dialogMaster.transcript.addTranscript("dialogFlow", buttonText);
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
        /*** Load files and start translation API service ***/

        String masterFilePath = this.TRANSCRIPT_FOLDER_PATH + this.MASTER_NPC_DIALOG_TRANSCRIPT_SUFIX;
        String translationFilePath = this.TRANSCRIPT_FOLDER_PATH + this.selectedLang.toUpperCase(Locale.ROOT) + this.TRANSLATED_NPC_DIALOG_TRANSCRIPT_SUFIX;

        this.dialogMaster.setFile(masterFilePath);
        this.dialogMaster.loadTranscripts();

        this.translatedDialog.setFile(translationFilePath);
        this.translatedDialog.loadTranscripts();
    }

    public void shutdown(boolean changesDetected) throws Exception
    {
        if(changesDetected)
            this.dialogMaster.saveTranscript();
    }
    public void updateWidgets(Client client)
    {
        /*** Updates internal widgets ***/
        this.dialogOption = client.getWidget(WidgetInfo.DIALOG_OPTION);
        this.dialogOptionOptions = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
        this.dialogSpriteText = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
        this.dialogLabel = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
        this.dialogText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        this.dialogPlayerText = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

        // no WidgetInfo for the dialog continue button i guess...
        this.playerContinueButton = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 5);
        this.npcContinueButton = client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 5);
    }

    // setter methods for initialization
    public void setTranscriptFolder(String TRANSCRIPT_FOLDER_PATH){this.TRANSCRIPT_FOLDER_PATH = TRANSCRIPT_FOLDER_PATH;}
    public void setLang(String lang){this.selectedLang = lang;}
    public void setAllowName(boolean allowName){this.allowName = allowName;}
    public void setAllowGame(boolean allowGame){this.allowGame = allowGame;}

}
