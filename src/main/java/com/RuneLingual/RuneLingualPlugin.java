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


@Slf4j
@PluginDescriptor(
	// Plugin name shown at plugin hub
	name = "RuneLingual",
	description = "A translation plugin for OSRS."
)
public class RuneLingualPlugin extends Plugin
{
	// translation options
	private LangCodeSelectableList targetLanguage; // defaults to en-US on startup

	String NPC_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_NPC_DIALOG_TRANSCRIPT.json");
	String GAME_MASTER_TRANSCRIPT_FILE_NAME = new String("MASTER_GAME_MESSAGE_TRANSCRIPT.json");
	String TRANSCRIPT_FOLDER_PATH = new String("transcript\\");

	private ChatMessageTranslator chatTranslator = new ChatMessageTranslator();
	private DialogTranslator dialogTranslator = new DialogTranslator();

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

		// translation settings - TODO: check about spam and unknown messages
		chatTranslator.setAllowAUTOTYPER(config.getAllowPublic());
		chatTranslator.setAllowBROADCAST(config.getAllowGame());
		chatTranslator.setAllowCHALREQ_CLANCHAT(config.getAllowGame());
		chatTranslator.setAllowCHALREQ_FRIENDSCHAT(config.getAllowGame());
		chatTranslator.setAllowCHALREQ_TRADE(config.getAllowGame());
		chatTranslator.setAllowCLAN_GIM_GROUP_WITH(config.getAllowGame());
		chatTranslator.setAllowCLAN_CHAT(config.getAllowClan());
		chatTranslator.setAllowCLAN_CREATION_INVITATION(config.getAllowGame());
		chatTranslator.setAllowCLAN_GIM_CHAT(config.getAllowClan());
		chatTranslator.setAllowCLAN_GIM_FORM_GROUP(config.getAllowGame());
		chatTranslator.setAllowCLAN_GIM_MESSAGE(config.getAllowGame());
		chatTranslator.setAllowCLAN_GUEST_CHAT(config.getAllowClan());
		chatTranslator.setAllowCLAN_GUEST_MESSAGE(config.getAllowGame());
		chatTranslator.setAllowCLAN_MESSAGE(config.getAllowGame());
		chatTranslator.setAllowCONSOLE(config.getAllowGame());
		chatTranslator.setAllowENGINE(config.getAllowGame());
		chatTranslator.setAllowFRIENDNOTIFICATION(config.getAllowGame());
		chatTranslator.setAllowFRIENDSCHAT(config.getAllowFriends());
		chatTranslator.setAllowFRIENDSCHATNOTIFICATION(config.getAllowGame());
		chatTranslator.setAllowGAMEMESSAGE(config.getAllowGame());
		chatTranslator.setAllowIGNORENOTIFICATION(config.getAllowGame());
		chatTranslator.setAllowITEM_EXAMINE(config.getAllowGame());
		chatTranslator.setAllowLOGOUTNOTIFICATION(config.getAllowGame());
		chatTranslator.setAllowMODAUTOTYPER(config.getAllowPublic());
		chatTranslator.setAllowMODCHAT(config.getAllowPublic());
		chatTranslator.setAllowMODPRIVATECHAT(config.getAllowFriends());
		chatTranslator.setAllowNPC_EXAMINE(config.getAllowGame());
		chatTranslator.setAllowOBJECT_EXAMINE(config.getAllowGame());
		chatTranslator.setAllowPRIVATECHAT(config.getAllowFriends());
		chatTranslator.setAllowPRIVATECHATOUT(config.getAllowLocal());
		chatTranslator.setAllowPUBLICCHAT(config.getAllowPublic());
		chatTranslator.setAllowSNAPSHOTFEEDBACK(config.getAllowGame());
		chatTranslator.setAllowTENSECTIMEOUT(config.getAllowGame());
		chatTranslator.setAllowTRADE(config.getAllowGame());
		chatTranslator.setAllowTRADE_SENT(config.getAllowGame());
		chatTranslator.setAllowTRADEREQ(config.getAllowGame());
		chatTranslator.setAllowWELCOME(config.getAllowGame());

		// loading files
		log.info("Loading transcripts...");

		dialogTranslator.setTranscriptFolder(TRANSCRIPT_FOLDER_PATH);
		dialogTranslator.setAllowGame(config.getAllowGame());
		dialogTranslator.setAllowName(config.getAllowName());
		dialogTranslator.setLang(targetLanguage.getLangCode());
		dialogTranslator.startup();

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
		dialogTranslator.updateWidgets(client);
		dialogTranslator.translateAndReplace(client);

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
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) throws Exception
	{
		GameState newGameState = gameStateChanged.getGameState();
		if (newGameState == GameState.LOGGED_IN)
		{
			// when logging in
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Set api key: " + config.getAPIKey(), null);
		} else if (newGameState == GameState.LOGIN_SCREEN) {
			// when at the login screen - only when logging out
			chatTranslator.shutdown(changesDetected);
			dialogTranslator.shutdown(changesDetected);
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
