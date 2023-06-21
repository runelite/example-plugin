package com.RuneLingual;

import net.runelite.api.ChatMessageType;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;

import java.util.Locale;

public class ChatMessageTranslator
{
    /***Manages the ChatBox itself, not the actual chat widget (seen on onGameTick)
     *
     * new ChatMessage is the event***/

    private ChatMessageType type;
    private String sender;
    private String contents;
    private MessageNode node;
    private int time;

    private String selectedLang;
    private String TRANSCRIPT_FOLDER_PATH;
    private String MASTER_CHAT_GENERAL_TRANSCRIPT_SUFIX = new String("MASTER_CHAT_GENERAL_TRANSCRIPT.json");
    private String TRANSLATED_CHAT_GENERAL_TRANSCRIPT_SUFIX = new String("_CHAT_GENERAL_TRANSCRIPT.json");
    private TranscriptsDatabaseManager messageMaster = new TranscriptsDatabaseManager();
    private TranscriptsDatabaseManager translatedMessages = new TranscriptsDatabaseManager();
    private boolean allowDynamic;  // enable dynamic translation (with api requests)
    private boolean requiresKey;

    // if said type of message should be translated
    private boolean allowAUTOTYPER;
    private boolean allowBROADCAST;
    private boolean allowCHALREQ_CLANCHAT;
    private boolean allowCHALREQ_FRIENDSCHAT;
    private boolean allowCHALREQ_TRADE;
    private boolean allowCLAN_CHAT;
    private boolean allowCLAN_CREATION_INVITATION;
    private boolean allowCLAN_GIM_CHAT;
    private boolean allowCLAN_GIM_FORM_GROUP;
    private boolean allowCLAN_GIM_GROUP_WITH;
    private boolean allowCLAN_GIM_MESSAGE;
    private boolean allowCLAN_GUEST_CHAT;
    private boolean allowCLAN_GUEST_MESSAGE;
    private boolean allowCLAN_MESSAGE;
    private boolean allowCONSOLE;
    private boolean allowENGINE;
    private boolean allowFRIENDNOTIFICATION;
    private boolean allowFRIENDSCHAT;
    private boolean allowFRIENDSCHATNOTIFICATION;
    private boolean allowGAMEMESSAGE;
    private boolean allowIGNORENOTIFICATION;
    private boolean allowITEM_EXAMINE = true;
    private boolean allowLOGOUTNOTIFICATION;
    private boolean allowMODAUTOTYPER;
    private boolean allowMODCHAT;
    private boolean allowMODPRIVATECHAT;
    private boolean allowNPC_EXAMINE;
    private boolean allowOBJECT_EXAMINE;
    private boolean allowPRIVATECHAT;
    private boolean allowPRIVATECHATOUT;
    private boolean allowPUBLICCHAT;
    private boolean allowSNAPSHOTFEEDBACK;
    private boolean allowSPAM;
    private boolean allowTENSECTIMEOUT;
    private boolean allowTRADE;
    private boolean allowTRADE_SENT;
    private boolean allowTRADEREQ;
    private boolean allowUNKWNOWN;
    private boolean allowWELCOME;

    public boolean doesMessageRequiresKey()
    {
        /*** Checks if the message requires an API key for translating ***/
        if(type.equals(ChatMessageType.AUTOTYPER)
            || type.equals(ChatMessageType.BROADCAST)
            || type.equals(ChatMessageType.CLAN_CHAT)
            || type.equals(ChatMessageType.CLAN_GIM_CHAT)
            || type.equals(ChatMessageType.CLAN_GUEST_CHAT)
            || type.equals(ChatMessageType.BROADCAST)
            || type.equals(ChatMessageType.MODCHAT)
            || type.equals(ChatMessageType.MODPRIVATECHAT)
            || type.equals(ChatMessageType.PRIVATECHAT)
            || type.equals(ChatMessageType.PUBLICCHAT)
            || type.equals(ChatMessageType.SPAM)
            || type.equals(ChatMessageType.UNKNOWN))
        {
            this.requiresKey = true;
            return true;
        }
        this.requiresKey = false;
        return false;
    }
    public void translateAndReplace() throws Exception {
        // tries to translate and replace chat messages by their given message node
        if(requiresKey == true && allowDynamic)  // if the current translation iteration requires an api key
        {
            // TODO: api translation here
            if(type.equals(ChatMessageType.AUTOTYPER) && allowAUTOTYPER)
            {

            } else if(type.equals(ChatMessageType.BROADCAST) && allowBROADCAST)
            {

            } else if(type.equals(ChatMessageType.CLAN_CHAT) && allowCLAN_CHAT)
            {

            } else if(type.equals(ChatMessageType.CLAN_GIM_CHAT) && allowCLAN_GIM_CHAT)
            {

            } else if(type.equals(ChatMessageType.CLAN_GUEST_CHAT) && allowCLAN_GUEST_CHAT)
            {

            } else if(type.equals(ChatMessageType.FRIENDSCHAT) && allowFRIENDSCHAT)
            {

            } else if(type.equals(ChatMessageType.MODAUTOTYPER) && allowMODAUTOTYPER)
            {

            } else if(type.equals(ChatMessageType.MODCHAT) && allowMODCHAT)
            {

            } else if(type.equals(ChatMessageType.MODPRIVATECHAT) && allowMODPRIVATECHAT)
            {

            } else if(type.equals(ChatMessageType.PRIVATECHAT) && allowPRIVATECHAT)
            {

            } else if(type.equals(ChatMessageType.PRIVATECHATOUT) && allowPRIVATECHATOUT)
            {

            } else if(type.equals(ChatMessageType.PUBLICCHAT) && allowPUBLICCHAT)
            {

            } else if(type.equals(ChatMessageType.SPAM) && allowSPAM)
            {

            } else if(type.equals(ChatMessageType.TRADE) && allowTRADE)
            {

            } else if(type.equals(ChatMessageType.UNKNOWN) && allowUNKWNOWN)
            {

            } else
            {
                // messages whose translations were not allowed by user - do not translate these messages
            }
        } else if(allowDynamic == false)
        {
            // TODO: other game translations that does not require an api key
            // if the given string is not found raise an exception, then add to the master database
            if(type.equals(ChatMessageType.CHALREQ_CLANCHAT) && allowCHALREQ_CLANCHAT)
            {
                replaceOrAdd("CHALREQ_CLANCHAT");
            } else if(type.equals(ChatMessageType.CHALREQ_FRIENDSCHAT) && allowCHALREQ_FRIENDSCHAT)
            {
                replaceOrAdd("CHALREQ_FRIENDSCHAT");
            } else if(type.equals(ChatMessageType.CHALREQ_TRADE) && allowCHALREQ_TRADE)
            {
                replaceOrAdd("CHALREQ_TRADE");
            } else if(type.equals(ChatMessageType.CLAN_CREATION_INVITATION) && allowCLAN_CREATION_INVITATION)
            {
                replaceOrAdd("CLAN_CREATION_INVITATION");
            } else if(type.equals(ChatMessageType.CLAN_GIM_FORM_GROUP) && allowCLAN_GIM_FORM_GROUP)
            {
                replaceOrAdd("CLAN_GIM_FORM_GROUP");
            } else if(type.equals(ChatMessageType.CLAN_GIM_GROUP_WITH) && allowCLAN_GIM_GROUP_WITH)
            {
                replaceOrAdd("CLAN_GIM_GROUP_WITH");
            } else if(type.equals(ChatMessageType.CLAN_GIM_MESSAGE) && allowCLAN_GIM_MESSAGE)
            {
                replaceOrAdd("CLAN_GIM_MESSAGE");
            } else if(type.equals(ChatMessageType.CLAN_GUEST_MESSAGE) && allowCLAN_GUEST_MESSAGE)
            {
                replaceOrAdd("CLAN_GUEST_MESSAGE");
            } else if(type.equals(ChatMessageType.CLAN_MESSAGE) && allowCLAN_MESSAGE)
            {
                replaceOrAdd("CLAN_MESSAGE");
            } else if(type.equals(ChatMessageType.CONSOLE) && allowCONSOLE)
            {
                replaceOrAdd("CONSOLE");
            } else if(type.equals(ChatMessageType.ENGINE) && allowENGINE)
            {
                replaceOrAdd("ENGINE");
            } else if(type.equals(ChatMessageType.FRIENDNOTIFICATION) && allowFRIENDNOTIFICATION)
            {
                replaceOrAdd("FRIENDNOTIFICATION");
            } else if(type.equals(ChatMessageType.FRIENDSCHATNOTIFICATION) && allowFRIENDSCHATNOTIFICATION)
            {
                replaceOrAdd("FRIENDSCHATNOTIFICATION");
            } else if(type.equals(ChatMessageType.GAMEMESSAGE) && allowGAMEMESSAGE)
            {
                replaceOrAdd("GAMEMESSAGE");
            } else if(type.equals(ChatMessageType.IGNORENOTIFICATION) && allowIGNORENOTIFICATION)
            {
                replaceOrAdd("IGNORENOTIFICATION");
            } else if(type.equals(ChatMessageType.ITEM_EXAMINE) && allowITEM_EXAMINE)
            {
                replaceOrAdd("ITEM_EXAMINE");
            } else if(type.equals(ChatMessageType.LOGINLOGOUTNOTIFICATION) && allowLOGOUTNOTIFICATION)
            {
                replaceOrAdd("LOGINLOGOUTNOTIFICATION");
            } else if(type.equals(ChatMessageType.NPC_EXAMINE) && allowNPC_EXAMINE)
            {
                replaceOrAdd("NPC_EXAMINE");
            } else if(type.equals(ChatMessageType.OBJECT_EXAMINE) && allowOBJECT_EXAMINE)
            {
                replaceOrAdd("OBJECT_EXAMINE");
            } else if(type.equals(ChatMessageType.SNAPSHOTFEEDBACK) && allowSNAPSHOTFEEDBACK)
            {
                replaceOrAdd("SNAPSHOTFEEDBACK");
            } else if(type.equals(ChatMessageType.TENSECTIMEOUT) && allowTENSECTIMEOUT)
            {
                replaceOrAdd("TENSECTIMEOUT");
            } else if(type.equals(ChatMessageType.TRADE_SENT) && allowTRADE_SENT)
            {
                replaceOrAdd("TRADE_SENT");
            } else if(type.equals(ChatMessageType.TRADEREQ) && allowTRADEREQ)
            {
                replaceOrAdd("TRADEREQ");
            } else if(type.equals(ChatMessageType.WELCOME) && allowWELCOME)
            {
                replaceOrAdd("WELCOME");
            } else
            {
                // messages whose translations were not allowed by user - do not translate these messages
            }

        } else
        {
            // messages whose translations were not allowed by user - do not translate these messages
        }

    }

    public void startup() throws Exception
    {
        /*** Load files and start translation API service ***/

        String masterFilePath = this.TRANSCRIPT_FOLDER_PATH + this.MASTER_CHAT_GENERAL_TRANSCRIPT_SUFIX;
        String translationFilePath = this.TRANSCRIPT_FOLDER_PATH + this.selectedLang.toUpperCase(Locale.ROOT) + this.TRANSLATED_CHAT_GENERAL_TRANSCRIPT_SUFIX;

        this.messageMaster.setFile(masterFilePath);
        this.messageMaster.loadTranscripts();

        this.translatedMessages.setFile(translationFilePath);
        this.translatedMessages.loadTranscripts();
    }

    public void shutdown(boolean changesDetected) throws Exception
    {
        if(changesDetected)
            this.messageMaster.saveTranscript();
    }
    public void setMessage(ChatMessage event)
    {
        /*** updates internal chat message variables ***/
        this.type = event.getType();
        this.sender = event.getSender();
        this.contents = event.getMessage();
        this.node = event.getMessageNode();
        this.time = event.getTimestamp();
    }

    // setter methods for initialization
    public void setTranscriptFolder(String TRANSCRIPT_FOLDER_PATH){this.TRANSCRIPT_FOLDER_PATH = TRANSCRIPT_FOLDER_PATH;}
    public void setLang(String lang){this.selectedLang = lang;}
    public void setAllowAUTOTYPER(boolean allowAUTOTYPER){this.allowAUTOTYPER = allowAUTOTYPER;}
    public void setAllowBROADCAST(boolean allowBROADCAST){this.allowBROADCAST = allowBROADCAST;}
    public void setAllowCHALREQ_CLANCHAT(boolean allowCHALREQ_CLANCHAT){this.allowCHALREQ_CLANCHAT = allowCHALREQ_CLANCHAT;}
    public void setAllowCHALREQ_FRIENDSCHAT(boolean allowCHALREQ_FRIENDSCHAT) {this.allowCHALREQ_FRIENDSCHAT = allowCHALREQ_FRIENDSCHAT;}
    public void setAllowCHALREQ_TRADE(boolean allowCHALREQ_TRADE){this.allowCHALREQ_TRADE = allowCHALREQ_TRADE;}
    public void setAllowCLAN_GIM_GROUP_WITH(boolean allowCLAN_GIM_GROUP_WITH){this.allowCLAN_GIM_GROUP_WITH = allowCLAN_GIM_GROUP_WITH;}
    public void setAllowCLAN_CHAT(boolean allowCLAN_CHAT){this.allowCLAN_CHAT = allowCLAN_CHAT;}
    public void setAllowCLAN_CREATION_INVITATION(boolean allowCLAN_CREATION_INVITATION){this.allowCLAN_CREATION_INVITATION = allowCLAN_CREATION_INVITATION;}
    public void setAllowCLAN_GIM_CHAT(boolean allowCLAN_GIM_CHAT){this.allowCLAN_GIM_CHAT = allowCLAN_GIM_CHAT;}
    public void setAllowCLAN_GIM_FORM_GROUP(boolean allowCLAN_GIM_FORM_GROUP){this.allowCLAN_GIM_FORM_GROUP = allowCLAN_GIM_FORM_GROUP;}
    public void setAllowCLAN_GIM_MESSAGE(boolean allowCLAN_GIM_MESSAGE){this.allowCLAN_GIM_MESSAGE = allowCLAN_GIM_MESSAGE;}
    public void setAllowCLAN_GUEST_CHAT(boolean allowCLAN_GUEST_CHAT){this.allowCLAN_GUEST_CHAT = allowCLAN_GUEST_CHAT;}
    public void setAllowCLAN_GUEST_MESSAGE(boolean allowCLAN_GUEST_MESSAGE){this.allowCLAN_GUEST_MESSAGE = allowCLAN_GUEST_MESSAGE;}
    public void setAllowCLAN_MESSAGE(boolean allowCLAN_MESSAGE){this.allowCLAN_MESSAGE = allowCLAN_MESSAGE;}
    public void setAllowCONSOLE(boolean allowCONSOLE){this.allowCONSOLE = allowCONSOLE;}
    public void setAllowENGINE(boolean allowENGINE){this.allowENGINE = allowENGINE;}
    public void setAllowFRIENDNOTIFICATION(boolean allowFRIENDNOTIFICATION){this.allowFRIENDNOTIFICATION = allowFRIENDNOTIFICATION;}
    public void setAllowFRIENDSCHAT(boolean allowFRIENDSCHAT){this.allowFRIENDSCHAT = allowFRIENDSCHAT;}
    public void setAllowFRIENDSCHATNOTIFICATION(boolean allowFRIENDSCHATNOTIFICATION){this.allowFRIENDSCHATNOTIFICATION = allowFRIENDSCHATNOTIFICATION;}
    public void setAllowGAMEMESSAGE(boolean allowGAMEMESSAGE){this.allowGAMEMESSAGE = allowGAMEMESSAGE;}
    public void setAllowIGNORENOTIFICATION(boolean allowIGNORENOTIFICATION){this.allowIGNORENOTIFICATION = allowIGNORENOTIFICATION;}
    public void setAllowITEM_EXAMINE(boolean allowITEM_EXAMINE){this.allowITEM_EXAMINE = allowITEM_EXAMINE;}
    public void setAllowLOGOUTNOTIFICATION(boolean allowLOGOUTNOTIFICATION){this.allowLOGOUTNOTIFICATION = allowLOGOUTNOTIFICATION;}
    public void setAllowMODAUTOTYPER(boolean allowMODAUTOTYPER){this.allowMODAUTOTYPER = allowMODAUTOTYPER;}
    public void setAllowMODCHAT(boolean allowMODCHAT){this.allowMODCHAT = allowMODCHAT;}
    public void setAllowMODPRIVATECHAT(boolean allowMODPRIVATECHAT){this.allowMODPRIVATECHAT = allowMODPRIVATECHAT;}
    public void setAllowNPC_EXAMINE(boolean allowNPC_EXAMINE){this.allowNPC_EXAMINE = allowNPC_EXAMINE;}
    public void setAllowOBJECT_EXAMINE(boolean allowOBJECT_EXAMINE){this.allowOBJECT_EXAMINE = allowOBJECT_EXAMINE;}
    public void setAllowPRIVATECHAT(boolean allowPRIVATECHAT){this.allowPRIVATECHAT = allowPRIVATECHAT;}
    public void setAllowPRIVATECHATOUT(boolean allowPRIVATECHATOUT){this.allowPRIVATECHATOUT = allowPRIVATECHATOUT;}
    public void setAllowPUBLICCHAT(boolean allowPUBLICCHAT){this.allowPUBLICCHAT = allowPUBLICCHAT;}
    public void setAllowSNAPSHOTFEEDBACK(boolean allowSNAPSHOTFEEDBACK){this.allowSNAPSHOTFEEDBACK = allowSNAPSHOTFEEDBACK;}
    public void setAllowSPAM(boolean allowSPAM){this.allowSPAM = allowSPAM;}
    public void setAllowTENSECTIMEOUT(boolean allowTENSECTIMEOUT){this.allowTENSECTIMEOUT = allowTENSECTIMEOUT;}
    public void setAllowTRADE(boolean allowTRADE){this.allowTRADE = allowTRADE;}
    public void setAllowTRADE_SENT(boolean allowTRADE_SENT){this.allowTRADE_SENT = allowTRADE_SENT;}
    public void setAllowTRADEREQ(boolean allowTRADEREQ){this.allowTRADEREQ = allowTRADEREQ;}
    public void setAllowUNKWNOWN(boolean allowUNKWNOWN){this.allowUNKWNOWN = allowUNKWNOWN;}
    public void setAllowWELCOME(boolean allowWELCOME){this.allowWELCOME = allowWELCOME;}
    public void setAllowDynamic(boolean dynamicTranslations){this.allowDynamic = dynamicTranslations;}


    private void replaceOrAdd(String itemName) throws Exception
    {
        try
        {
            String translatedLine = this.translatedMessages.transcript.getTranslatedText(itemName, this.contents);
            this.node.setValue(translatedLine);
            System.out.println("translation was found and replaced");
        } catch(Exception e)
        {
            this.messageMaster.transcript.addTranscript(itemName, this.contents);
            System.out.println("translation was not found - trying to add to master - " + this.contents);
        }
    }
}
