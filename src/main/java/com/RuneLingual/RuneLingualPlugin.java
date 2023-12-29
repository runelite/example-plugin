package com.RuneLingual;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;

import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;

@Slf4j
@PluginDescriptor(
	// Plugin name shown at plugin hub
	name = "RuneLingual",
	description = "All-in-one translation plugin for OSRS."
)
public class RuneLingualPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualConfig config;
	
	private LangCodeSelectableList targetLanguage;
	
	String NPC_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_DIALOG_TRANSCRIPT.json");
	String GAME_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_GAME_MESSAGE_TRANSCRIPT.json");
	String TRANSCRIPT_FOLDER_PATH = new String("transcript\\");
	
	@Inject
	private ChatCapture chatTranslator;
	
	@Inject
	private DialogCapture dialogTranslator;
	
	private boolean changesDetected = false;
	
	public void pluginLog(String contents) {log.info(contents);}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");

		// plugin startup
		targetLanguage = config.presetLang();
		log.info(targetLanguage.getCode());
		
		chatTranslator.setLogger(this::pluginLog);
		dialogTranslator.setLogger(this::pluginLog);
		
		log.info("RuneLingual started!");
	}
	
	/*
	@Subscribe
	public void onGameTick(GameTick event) throws Exception
	{
	}
	*/

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) throws Exception
	{
		// retrieves the portion of the chatbox widget tree
		// that may have some dialog on it and pass it forward to the dialog module
		Widget chatbox = client.getWidget(ComponentID.CHATBOX_MESSAGES);
		dialogTranslator.handleDialogs(chatbox);
	}
	
	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		chatTranslator.onChatMessage(event);
	}
	
	@Override
	protected void shutDown() throws Exception
	{
		log.info("RuneLingual plugin stopped!");
	}

	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}
	
	private ChatMessage googleTranslateMessage(ChatMessage event, String langCode)
	{
		// on-the-fly translation for in game messages
		String messageContent = event.getMessage();
		/*
		try {
			Detection detection = translate.detect(messageContent);
			String detectedLanguage = detection.getLanguage();  // detected language code

			Translation translation =
					translate.translate(
							messageContent,
							TranslateOption.sourceLanguage(detectedLanguage),
							TranslateOption.targetLanguage(langCode));

			String newMessageText = translation.getTranslatedText();
			String newName = String.format("%s (%s)", event.getName(),detectedLanguage);
			ChatMessage newMessage = event;
			newMessage.setName(newName);
			newMessage.setMessage(newMessageText);

			return newMessage;
		} catch (Exception e) {
			log.error(e.getMessage());
			return event;
		}

		//event.getMessageNode().setValue();
		//event.getMessageNode().setValue("Essa mensagem só está disponível para macacos gold.");
		*/
		return null;
	}
}

