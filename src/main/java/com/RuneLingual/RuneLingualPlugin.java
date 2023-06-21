package com.RuneLingual;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.GameTick;

// google cloud translating
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

@Slf4j
@PluginDescriptor(
	// Plugin name shown at plugin hub
	name = "RuneLingual",
	description = "A translation plugin for OSRS."
)
public class RuneLingualPlugin extends Plugin
{
	// translation options
	private LangCode targetLanguage; // defaults to en-US on startup

	String NPC_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_NPC_DIALOG_TRANSCRIPT.json");
	String GAME_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_GAME_MESSAGE_TRANSCRIPT.json");
	String TRANSCRIPT_FOLDER_PATH = new String("transcript\\");

	private TranscriptsDatabaseManager npcDialogMaster = new TranscriptsDatabaseManager();
	private ChatMessageTranslator chatTranslator = new ChatMessageTranslator();

	private boolean changesDetected = false;

	@Inject
	private Client client;

	@Inject
	private RuneLingualConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");

		// plugin startup
		targetLanguage = config.presetLang();
		log.info(targetLanguage.getCode());


		// loading files
		log.info("Loading transcripts...");

		npcDialogMaster.setFile(NPC_MASTER_TRANSCRIPT_FILE_NAME);
		npcDialogMaster.loadTranscripts();

		chatTranslator.setTranscriptFolder(TRANSCRIPT_FOLDER_PATH);
		chatTranslator.setAllowDynamic(config.allowAPI());
		chatTranslator.setLang(targetLanguage.getLangCode());
		chatTranslator.startup();

		changesDetected = true;  // TODO: change this to actual changes being detected

		//npcDialogMaster.transcript.addTranscript("Hans", "Test");
		log.info("RuneLingual started!");
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		//TODO: change npc overheads as they appear
	}

	@Subscribe
	public void onGameTick(GameTick event) throws Exception
	{
		/***Occurs once every game tick***/

		// TODO: detect player name strings in the middle of sentences to keep them intact
		// TODO: lookup widget info for more dialog types
		boolean hasChat = false;
		Widget dialogText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
		Widget dialogLabel = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
		Widget dialogPlayerText = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);  // player messages occur on a separate widget
		if (dialogText != null && dialogLabel != null)
		{
			String npcText = dialogText.getText();
			String npcName = dialogLabel.getText();

			dialogText.setText(npcDialogMaster.transcript.getTranslatedText(npcName, npcText));
			dialogLabel.setText(npcDialogMaster.transcript.getTranslatedName(npcName));
			hasChat = true;

		} else if (dialogPlayerText != null)
		{
			// player talking to a npc
			String playerText = dialogPlayerText.getText();

			dialogPlayerText.setText(npcDialogMaster.transcript.getTranslatedText("player", playerText));
			hasChat = true;
		}
		if(hasChat) client.refreshChat();

	}

	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		this.chatTranslator.setMessage(event);
		this.chatTranslator.translateAndReplace();
	}


	@Override
	protected void shutDown() throws Exception
	{
		log.info("RuneLingual Stopped!");
		npcDialogMaster.saveTranscript();
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) throws Exception
	{
		GameState newGameState = gameStateChanged.getGameState();
		if (newGameState == GameState.LOGGED_IN)
		{
			// when logging in

			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Set api key: " + config.getAPIKey(), null);
		} else if (newGameState == GameState.LOGIN_SCREEN) {
			// when at the login screen
			chatTranslator.shutdown(changesDetected);
			if(changesDetected)
			{
				npcDialogMaster.saveTranscript();

			}

		}
	}

	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}

	/*private HashMap loadTranscript() throws Exception {
		// loads local transcript databases

	}*/
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
