package com.RuneLingual;

import com.google.protobuf.Message;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

@FunctionalInterface
interface TranslationHandler
{
    String translate(String text);
}

public class ChatCapture extends RuneChatTypes
{
    // Captures chat messages from any source
    private Client clientReference;
    private boolean allowOnlineTranslations;
    private boolean allowOverHeads;
    private boolean debugPrints;
    private TranslationHandler localTranslationService;
    private TranslationHandler onlineTranslationService;
    
    ChatCapture(
            Client clientReference,
            TranslationHandler localTranslator,
            TranslationHandler onlineTranslator,
            boolean debugPrints)
    {
        this.clientReference = clientReference;
        this.localTranslationService = localTranslator;
        this.debugPrints = debugPrints;
    }
    
    public boolean messageTypeRequiresKey(ChatMessageType type)
    {
        // Checks if the message requires an API key for translating
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
            return true;
        }
        return false;
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage event) throws Exception {
        // tries to translate and replace chat messages by their given message node
        ChatMessageType type = event.getType();
        MessageNode messageNode = event.getMessageNode();
        String message = event.getMessage();
        
        if(messageTypeRequiresKey(type) && allowOnlineTranslations)  // if the current translation iteration requires an api key
        {
            // TODO: api translation here
            if(type.equals(ChatMessageType.AUTOTYPER) && allowAUTOTYPER())
            {
                onlineTranslatorCaller(message, messageNode);
            } else if(type.equals(ChatMessageType.BROADCAST) && allowBROADCAST())
            {

            } else if(type.equals(ChatMessageType.CLAN_CHAT) && allowCLAN_CHAT())
            {

            } else if(type.equals(ChatMessageType.CLAN_GIM_CHAT) && allowCLAN_GIM_CHAT())
            {

            } else if(type.equals(ChatMessageType.CLAN_GUEST_CHAT) && allowCLAN_GUEST_CHAT())
            {

            } else if(type.equals(ChatMessageType.FRIENDSCHAT) && allowFRIENDSCHAT())
            {

            } else if(type.equals(ChatMessageType.MODAUTOTYPER) && allowMODAUTOTYPER())
            {

            } else if(type.equals(ChatMessageType.MODCHAT) && allowMODCHAT())
            {

            } else if(type.equals(ChatMessageType.MODPRIVATECHAT) && allowMODPRIVATECHAT())
            {

            } else if(type.equals(ChatMessageType.PRIVATECHAT) && allowPRIVATECHAT())
            {

            } else if(type.equals(ChatMessageType.PRIVATECHATOUT) && allowPRIVATECHATOUT())
            {

            } else if(type.equals(ChatMessageType.PUBLICCHAT) && allowPUBLICCHAT())
            {
                String translatedText;

                if(allowOverHeads){
                    // changes overhead text from players

                }

            } else if(type.equals(ChatMessageType.SPAM) && allowSPAM())
            {

            } else if(type.equals(ChatMessageType.TRADE) && allowTRADE())
            {

            } else if(type.equals(ChatMessageType.UNKNOWN) && allowUNKWNOWN())
            {

            } else
            {
                // messages whose translations were not allowed by user - do not translate these messages
            }
        } else if(allowOnlineTranslations == false)
        {
            // TODO: handle prefix and sufixes for player names and items

            // if the given string is not found raise an exception, then add to the master database
            if(type.equals(ChatMessageType.CHALREQ_CLANCHAT) && allowCHALREQ_CLANCHAT())
            {
                replaceOrAdd("CHALREQ_CLANCHAT");
            } else if(type.equals(ChatMessageType.CHALREQ_FRIENDSCHAT) && allowCHALREQ_FRIENDSCHAT())
            {
                replaceOrAdd("CHALREQ_FRIENDSCHAT");
            } else if(type.equals(ChatMessageType.CHALREQ_TRADE) && allowCHALREQ_TRADE())
            {
                replaceOrAdd("CHALREQ_TRADE");
            } else if(type.equals(ChatMessageType.CLAN_CREATION_INVITATION) && allowCLAN_CREATION_INVITATION())
            {
                replaceOrAdd("CLAN_CREATION_INVITATION");
            } else if(type.equals(ChatMessageType.CLAN_GIM_FORM_GROUP) && allowCLAN_GIM_FORM_GROUP())
            {
                replaceOrAdd("CLAN_GIM_FORM_GROUP");
            } else if(type.equals(ChatMessageType.CLAN_GIM_GROUP_WITH) && allowCLAN_GIM_GROUP_WITH())
            {
                replaceOrAdd("CLAN_GIM_GROUP_WITH");
            } else if(type.equals(ChatMessageType.CLAN_GIM_MESSAGE) && allowCLAN_GIM_MESSAGE())
            {
                replaceOrAdd("CLAN_GIM_MESSAGE");
            } else if(type.equals(ChatMessageType.CLAN_GUEST_MESSAGE) && allowCLAN_GUEST_MESSAGE())
            {
                replaceOrAdd("CLAN_GUEST_MESSAGE");
            } else if(type.equals(ChatMessageType.CLAN_MESSAGE) && allowCLAN_MESSAGE())
            {
                replaceOrAdd("CLAN_MESSAGE");
            } else if(type.equals(ChatMessageType.CONSOLE) && allowCONSOLE())
            {
                replaceOrAdd("CONSOLE");
            } else if(type.equals(ChatMessageType.ENGINE) && allowENGINE())
            {
                replaceOrAdd("ENGINE");
            } else if(type.equals(ChatMessageType.FRIENDNOTIFICATION) && allowFRIENDNOTIFICATION())
            {
                replaceOrAdd("FRIENDNOTIFICATION");
            } else if(type.equals(ChatMessageType.FRIENDSCHATNOTIFICATION) && allowFRIENDSCHATNOTIFICATION())
            {
                replaceOrAdd("FRIENDSCHATNOTIFICATION");
            } else if(type.equals(ChatMessageType.GAMEMESSAGE) && allowGAMEMESSAGE())
            {
                replaceOrAdd("GAMEMESSAGE");
            } else if(type.equals(ChatMessageType.IGNORENOTIFICATION) && allowIGNORENOTIFICATION())
            {
                replaceOrAdd("IGNORENOTIFICATION");
            } else if(type.equals(ChatMessageType.ITEM_EXAMINE) && allowITEM_EXAMINE())
            {
                replaceOrAdd("ITEM_EXAMINE");
            } else if(type.equals(ChatMessageType.LOGINLOGOUTNOTIFICATION) && allowLOGOUTNOTIFICATION())
            {
                replaceOrAdd("LOGINLOGOUTNOTIFICATION");
            } else if(type.equals(ChatMessageType.NPC_EXAMINE) && allowNPC_EXAMINE())
            {
                replaceOrAdd("NPC_EXAMINE");
            } else if(type.equals(ChatMessageType.OBJECT_EXAMINE) && allowOBJECT_EXAMINE())
            {
                replaceOrAdd("OBJECT_EXAMINE");
            } else if(type.equals(ChatMessageType.SNAPSHOTFEEDBACK) && allowSNAPSHOTFEEDBACK())
            {
                replaceOrAdd("SNAPSHOTFEEDBACK");
            } else if(type.equals(ChatMessageType.TENSECTIMEOUT) && allowTENSECTIMEOUT())
            {
                replaceOrAdd("TENSECTIMEOUT");
            } else if(type.equals(ChatMessageType.TRADE_SENT) && allowTRADE_SENT())
            {
                replaceOrAdd("TRADE_SENT");
            } else if(type.equals(ChatMessageType.TRADEREQ) && allowTRADEREQ())
            {
                replaceOrAdd("TRADEREQ");
            } else if(type.equals(ChatMessageType.WELCOME) && allowWELCOME())
            {
                replaceOrAdd("WELCOME");
            } else
            {
                // messages whose translations were not allowed by user - do not translate these messages
            }
        } else {
            // messages whose translations were not allowed by user - do not translate these messages
        }
    }
    
    private void localTranslatorCaller(String message, MessageNode node)
    {
        try
        {
            String translatedMessage = localTranslationService.translate(message);
            node.setValue(translatedMessage);
            
            if(debugPrints)
            {
                System.out.println("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                String originalContents = node.getValue();
                System.out.println("Could not replace contents for '" + originalContents + "', exception ocurred: " + e.getMessage());
            }
        }
    }
    
    private void onlineTranslatorCaller(String message, MessageNode node)
    {
        try
        {
            String translatedMessage = onlineTranslationService.translate(message);
            node.setValue(translatedMessage);
            
            if(debugPrints)
            {
                System.out.println("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                String originalContents = node.getValue();
                System.out.println("Could not replace contents for '" + originalContents + "', exception ocurred: " + e.getMessage());
            }
        }
    }
    public void setAllowOnlineTranslations(boolean newValue) {this.allowOnlineTranslations = newValue;}
    public void setAllowOverHeads(boolean newValue) {this.allowOverHeads = newValue;}
}
