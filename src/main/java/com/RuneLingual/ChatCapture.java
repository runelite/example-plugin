package com.RuneLingual;

import lombok.extern.java.Log;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;

public class ChatCapture
{
    // Captures chat messages from any source
    @Inject
    private Client client;
    @Inject
    private RuneLingualConfig config;
    
    private boolean allowOnlineTranslations;
    private boolean allowOverHeads;
    private boolean debugPrints;
    private LogHandler log;
    private TranscriptManager localTranslationService;
    private TranslationHandler onlineTranslationService;
    private MessageReplacer overheadReplacer;
    
    @Inject
    ChatCapture(RuneLingualConfig config, Client client)
    {
        this.config = config;
        this.client = client;
        
        // TODO: change to false later on
        this.debugPrints = false;
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
    
    public void onChatMessage(ChatMessage event) throws Exception {
        // tries to translate and replace chat messages by their given message node
        ChatMessageType type = event.getType();
        MessageNode messageNode = event.getMessageNode();
        String message = event.getMessage();
        
        boolean allowGame = config.getAllowGame();
        boolean allowPublic = config.getAllowPublic();
        boolean allowClan = config.getAllowClan();
        boolean allowFriends = config.getAllowFriends();
        
        if(messageTypeRequiresKey(type) && allowOnlineTranslations)  // if the current translation iteration requires an api key
        {
            //TODO: fix configs here
            if(type.equals(ChatMessageType.AUTOTYPER) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.BROADCAST) && allowGame)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CHAT) && allowClan)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_CHAT) && allowClan)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_CHAT) && allowClan)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHAT) && allowFriends)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODAUTOTYPER) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODCHAT) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODPRIVATECHAT) && allowFriends)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHAT) && allowFriends)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHATOUT) && allowFriends)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PUBLICCHAT) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
                if(allowOverHeads)
                {
                    // avoids duplicate translation requests
                    // looks for the player that sent the message and translates it
                    String newMessage = messageNode.getValue();
                    overheadTranslatorCaller(message, newMessage);
                }
            }
            else if(type.equals(ChatMessageType.SPAM) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADE) && allowGame)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.UNKNOWN) && allowPublic)
            {
                onlineTextTranslatorCaller(message, messageNode);
            }
            else
            {
                // messages whose translations were not allowed by user - do not translate these messages
            }
        }
        else
        {
            if(type.equals(ChatMessageType.CHALREQ_CLANCHAT) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_FRIENDSCHAT) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_TRADE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CREATION_INVITATION) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_FORM_GROUP) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_GROUP_WITH) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_MESSAGE) && allowClan)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_MESSAGE) && allowClan)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_MESSAGE) && allowClan)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CONSOLE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ENGINE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDNOTIFICATION) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHATNOTIFICATION) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.GAMEMESSAGE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.IGNORENOTIFICATION) && allowFriends)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ITEM_EXAMINE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.LOGINLOGOUTNOTIFICATION) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.NPC_EXAMINE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.OBJECT_EXAMINE) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.SNAPSHOTFEEDBACK) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TENSECTIMEOUT) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADE_SENT) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADEREQ) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.WELCOME) && allowGame)
            {
                localTextTranslatorCaller(message, messageNode);
            }
            else
            {
                // messages whose translations were not allowed by user
                // do not translate these messages
                log.log("pasou" + event.getMessage());
            }
        }
    }
    
    private void localTextTranslatorCaller(String message, MessageNode node)
    {
        try
        {
            String translatedMessage = localTranslationService.getTranslatedText("game", message, true);
            node.setValue(translatedMessage);
            
            if(debugPrints)
            {
                log.log("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(e.getMessage() == "LineNotFound")
            {
                try
                {
                    localTranslationService.addTranscript("game", message);
                    return;
                }
                catch(Exception unknownException)
                {
                    log.log("Could not add '"
                        + message
                        + "'line to transcript: "
                        + unknownException.getMessage());
                }
            }
            
            if(debugPrints)
            {
                String originalContents = node.getValue();
                log.log("Could not replace contents for '" + originalContents + "', exception ocurred: " + e.getMessage());
            }
        }
    }
    
    private void onlineTextTranslatorCaller(String message, MessageNode node)
    {
        try
        {
            String translatedMessage = onlineTranslationService.translate("online", message);
            node.setValue(translatedMessage);
            
            if(debugPrints)
            {
                log.log("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                String originalContents = node.getValue();
                log.log("Could not replace contents for '" + originalContents + "', exception occurred: " + e.getMessage());
            }
        }
    }
    
    private void overheadTranslatorCaller(String currentMessage, String newMessage)
    {
        try
        {
            String translatedMessage = overheadReplacer.replace(currentMessage, newMessage);
            
            if(debugPrints)
            {
                log.log("Translation found! Overhead replaced for item '" + currentMessage + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                log.log("Could not replace contents for '" + currentMessage + "', exception ocurred: " + e.getMessage());
            }
        }
    }
    
    public void setLogger(LogHandler logger) {this.log = logger;}
    public void setOverheadReplacer(MessageReplacer overheadReplacer) {this.overheadReplacer = overheadReplacer;}
    public void setOnlineTranslationService(TranslationHandler newHandler) {this.onlineTranslationService = newHandler;}
    public void setLocalTranslationService(TranscriptManager newHandler) {this.localTranslationService = newHandler;}
    public void setOnlineTranslations(boolean newValue) {this.allowOnlineTranslations = newValue;}
    public void setOverHeads(boolean newValue) {this.allowOverHeads = newValue;}
}
