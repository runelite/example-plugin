package com.RuneLingual;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
	description = "A translation plugin for osrs"
)
public class RuneLingualPlugin extends Plugin
{
	// translation options
	private LangCode targetLanguage; // defaults to en-US on startup

	String NPC_TRANSCRIPT_FILE_NAME = new String("MASTER_NPC_DIALOG_TRANSCRIPT.json");

	private TranscriptsDatabaseManager npcDialogMaster = new TranscriptsDatabaseManager();
	private boolean changesDetected = false;

	@Inject
	private Client client;

	@Inject
	private RuneLingualConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");
		targetLanguage = config.presetLang();
		log.info(targetLanguage.getCode());
		log.info("Loading transcripts...");
		npcDialogMaster.setFile(NPC_TRANSCRIPT_FILE_NAME);
		npcDialogMaster.loadTranscripts();
		changesDetected = true;

		//npcDialogMaster.transcript.addTranscript("Hans", "Test");
		log.info("RuneLingual started!");

		//npcDialogMaster.saveTranscript();
		//log.info(npcDialogMaster.transcript.getTranslatedText("Hans","Test"));
		//log.info(npcDialogMaster.transcript.getTranslatedName("Hans"));
		//log.info(npcDialogMaster.transcript.getTranslatedName("Jorge"));
		//log.info(npcDialogMaster.transcript.getTranslatedText("Jorge", "bom dia"));
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event){
	}

	@Subscribe
	public void onGameTick(GameTick event) throws Exception{
		Widget dialogText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
		Widget dialogLabel = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
		if (dialogText != null && dialogLabel != null)
		{
			String npcText = dialogText.getText();
			String npcName = dialogLabel.getText();

			dialogText.setText(npcDialogMaster.transcript.getTranslatedText(npcName, npcText));
			dialogLabel.setText(npcDialogMaster.transcript.getTranslatedName(npcName));
			//log.info("name: " + npcName + "text: " + npcText);
			// npcDialog.setText("Mensagem traduzida aqui");  // replaces npc text
			//client.refreshChat()
		}

	}

	@Subscribe
	public void onChatMessage(ChatMessage event){
		// intercepts all chat messages
		ChatMessageType messageType = event.getType();

		if(messageType.equals(ChatMessageType.PUBLICCHAT)){
			// public chat messages

			// translates the message itself

			/*String actorname = event.getName();
			ChatMessage newMessage = googleTranslateMessage(event, "pt");
			event.getMessageNode().setValue(newMessage.getMessage());
			event.getMessageNode().setName(newMessage.getName());*/

			// looks for the actor to replace the overhead text

		} else if (messageType.equals(ChatMessageType.GAMEMESSAGE)) {
			// game messages (such as welcome, errors, kill count)
		} else if (messageType.equals(ChatMessageType.FRIENDSCHAT)) {
			// friends
		} else if (messageType.equals(ChatMessageType.CLAN_MESSAGE)) {
			//
		} else if (messageType.equals(ChatMessageType.DIALOG)) {
			// NPC DIALOG
			log.info(event.getSender() + event.getMessage() + event.getClass());
			event.getMessageNode().setValue("teste1");
			event.setMessage("teste2");
			client.refreshChat();
		} else {
			// for any other chat message
			// log.info(event.getSender() + " - " + event.getName() + " - " + event.getMessage() + " - " + event.getType());
		}



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
