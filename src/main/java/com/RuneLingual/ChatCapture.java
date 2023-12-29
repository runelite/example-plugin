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
    private TranslationHandler localTranslationService;
    private TranslationHandler onlineTranslationService;
    private MessageReplacer overheadReplacer;
    
    @Inject
    ChatCapture(RuneLingualConfig config, Client client)
    {
        this.config = config;
        this.client = client;
        
        // TODO: change to false later on
        this.debugPrints = true;
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
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.BROADCAST) && allowGame)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CHAT) && allowClan)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_CHAT) && allowClan)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_CHAT) && allowClan)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHAT) && allowFriends)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODAUTOTYPER) && allowPublic)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODCHAT) && allowPublic)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.MODPRIVATECHAT) && allowFriends)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHAT) && allowFriends)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PRIVATECHATOUT) && allowFriends)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.PUBLICCHAT) && allowPublic)
            {
                onlineTranslatorCaller(message, messageNode);
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
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADE) && allowGame)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.UNKNOWN) && allowPublic)
            {
                onlineTranslatorCaller(message, messageNode);
            }
            else
            {
                // messages whose translations were not allowed by user - do not translate these messages
                log.log("pasou2" + event.getMessage());
            }
        }
        else
        {
            if(type.equals(ChatMessageType.CHALREQ_CLANCHAT) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_FRIENDSCHAT) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CHALREQ_TRADE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_CREATION_INVITATION) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_FORM_GROUP) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_GROUP_WITH) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GIM_MESSAGE) && allowClan)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_GUEST_MESSAGE) && allowClan)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CLAN_MESSAGE) && allowClan)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.CONSOLE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ENGINE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDNOTIFICATION) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.FRIENDSCHATNOTIFICATION) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.GAMEMESSAGE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.IGNORENOTIFICATION) && allowFriends)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.ITEM_EXAMINE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.LOGINLOGOUTNOTIFICATION) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.NPC_EXAMINE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.OBJECT_EXAMINE) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.SNAPSHOTFEEDBACK) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TENSECTIMEOUT) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADE_SENT) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.TRADEREQ) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else if(type.equals(ChatMessageType.WELCOME) && allowGame)
            {
                localTranslatorCaller(message, messageNode);
            }
            else
            {
                // messages whose translations were not allowed by user
                // do not translate these messages
                log.log("pasou" + event.getMessage());
            }
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
                log.log("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                String originalContents = node.getValue();
                log.log("Could not replace contents for '" + originalContents + "', exception ocurred: " + e.getMessage());
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
                log.log("Translation found! Replaced item '" + message + "'.");
            }
        }
        catch (Exception e)
        {
            if(debugPrints)
            {
                String originalContents = node.getValue();
                log.log("Could not replace contents for '" + originalContents + "', exception ocurred: " + e.getMessage());
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
    public void setLocalTranslationService(TranslationHandler newHandler) {this.localTranslationService = newHandler;}
    public void setOnlineTranslations(boolean newValue) {this.allowOnlineTranslations = newValue;}
    public void setOverHeads(boolean newValue) {this.allowOverHeads = newValue;}
}
