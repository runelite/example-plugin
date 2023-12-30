package com.RuneLingual;

import static com.RuneLingual.WidgetsUtil.getAllWidgets;

import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;

public class DialogCapture
{
    // Dialog happens in a separate widget than the ChatBox itself
    // not limited to npc conversations themselves, but also chat actions
    @Inject
    private Client client;
    @Inject
    private RuneLingualConfig config;
    
    private LogHandler log;
    private MessageReplacer overheadReplacer;
    private TranslationHandler localTranslationService;
    private boolean debugPrints;
    private boolean logEveryTranslation;

    @Inject
    public DialogCapture(RuneLingualConfig config, Client client)
    {
        this.client = client;
        this.config = config;
        
        // TODO: change to false later on
        this.debugPrints = true;
        this.logEveryTranslation = true;
    }
    
    public void handleDialogs(Widget event)
    {
        // gets all children widgets from chatbox (other than chat messages)
        Widget[] widgetsLoaded = getAllWidgets(event);

        if(widgetsLoaded.length == 0)
        {
            if(debugPrints)
            {
                log.log("Empty dialog widget list.");
            }
            return;
        }

        // update/validate configs
        // TODO: move this to its own private method
        // TODO: probably needs adding more settings entries for this one
        boolean translateNames = config.getAllowName();
        boolean translateGame = config.getAllowGame();

        // iterates through widget array
        for(Widget widget : widgetsLoaded)
        {
            // ignores null widgets
            if(widget == null)
            {
                if(debugPrints)
                {
                    log.log("Found empty dialog widget! Jumping to next iteration");
                }
                continue;
            }

            // translate widget names
            // TODO: probably should check if widget ID is equal to something
            // documented by ComponentID or InterfaceID
            // such as npc names, etc
            if(translateNames)
            {
                String currentName = widget.getName();
                if(!currentName.isEmpty())
                {
                    String newName = localTranslationService.translate(currentName);
                    widget.setName(newName);
                }
            }

            // translate widget main text
            if(translateGame)
            {
                String currentContents = widget.getText();
                if(!currentContents.isEmpty())
                {
                    String newContents = localTranslationService.translate(currentContents);
                    widget.setText(newContents);
                }
            }
        }
    }
    
    private void localTranslatorCaller(String currentMessage, MessageNode messageNode)
    {
        try
        {
            String newMessage = localTranslationService.translate(currentMessage);
            messageNode.setValue(newMessage);
            
            if(debugPrints && logEveryTranslation)
            {
                log.log("Dialog message '"
                    + currentMessage
                    + "' was translated and replaced for '"
                    + newMessage
                    + "'.");
            }
        }
        catch(Exception e)
        {
            if(debugPrints)
            {
                log.log("Could not translate dialog message '"
                    + currentMessage
                    + "'! Exception captured: "
                    + e.getMessage());
            }
        }
    }
    private void overheadTranslatorCaller(String currentMessage, String newMessage)
    {
        try
        {
            overheadReplacer.replace(currentMessage, newMessage);
            
            if(debugPrints)
            {
                log.log("Found and replaced overhead message for '" + currentMessage + "'.");
            }
        }
        catch(Exception e)
        {
            if(debugPrints)
            {
                log.log("Could not replace overhead message '"
                    + currentMessage
                    + "'! Exception captured: "
                    + e);
            }
        }
    }
    
    private void updateConfigs()
    {
        // update internal settings from plugin configuration file
        // should be handled and updated by base client
        boolean translateGame = config.getAllowGame();
        boolean translateNames = config.getAllowName();
    }
    
    public void setLocalTranslationService(TranslationHandler newHandler) {this.localTranslationService = newHandler;}
    public void setOverheadReplacer(MessageReplacer overheadReplacer) {this.overheadReplacer = overheadReplacer;}
    public void setLogger(LogHandler logger) {this.log = logger;}
}
