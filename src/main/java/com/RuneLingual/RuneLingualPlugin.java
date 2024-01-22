package com.RuneLingual;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.HashTable;
import net.runelite.api.MenuEntry;
import net.runelite.api.Node;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.*;

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
	private TranscriptsDatabaseManager dialogTranscriptManager = new TranscriptsDatabaseManager();
	private TranscriptsDatabaseManager actionTranscriptManager = new TranscriptsDatabaseManager();
	private TranscriptsDatabaseManager objectTranscriptManager = new TranscriptsDatabaseManager();
	private TranscriptsDatabaseManager itemTrancriptManager = new TranscriptsDatabaseManager();
	
	// main modules
	@Inject
	private ChatCapture chatTranslator;
	@Inject
	private DialogCapture dialogTranslator;
	@Inject
	private MenuCapture menuTranslator;
	@Inject
	private GroundItems groundItemsTranslator;
	
	private boolean changesDetected = false;
	
	public void pluginLog(String contents)
	{
		log.info(contents);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting...");

		// plugin startup
		targetLanguage = config.presetLang();
		log.info(targetLanguage.getCode());
		
		/* General dialog transcript
		* used for npc conversations action interfaces,
		* pretty much everything that happens withing the
		* dialog box widget
		*/
		dialogTranscriptManager.setLogger(this::pluginLog);
		String dialogFilePath = "/npc_dialog_" + targetLanguage.getCode() + ".json";
		dialogTranscriptManager.setFile(dialogFilePath);
		dialogTranscriptManager.loadTranscripts();
		
		// main dialog widget manager
		dialogTranslator.setLogger(this::pluginLog);
		dialogTranslator.setLocalTextTranslationService(dialogTranscriptManager.transcript);
		
		// chat translator handles game messages, contained also by the dialog transcript
		chatTranslator.setLogger(this::pluginLog);
		chatTranslator.setLocalTranslationService(dialogTranscriptManager.transcript);
		//chatTranslator.setOnlineTranslationService(this::temporaryTranslator);
		
		actionTranscriptManager.setLogger(this::pluginLog);
		String actionFilePath = "/actions_" + targetLanguage.getCode() + ".json";
		actionTranscriptManager.setFile(actionFilePath);
		actionTranscriptManager.loadTranscripts();
		
		objectTranscriptManager.setLogger(this::pluginLog);
		String objectFilePath = "/objects_" + targetLanguage.getCode() + ".json";
		objectTranscriptManager.setFile(objectFilePath);
		objectTranscriptManager.loadTranscripts();
		
		itemTrancriptManager.setLogger(this::pluginLog);
		String itemFilePath = "/items_" + targetLanguage.getCode() + ".json";
		itemTrancriptManager.setFile(itemFilePath);
		itemTrancriptManager.loadTranscripts();
		
		menuTranslator.setLogger(this::pluginLog);
		menuTranslator.setActionTranslator(actionTranscriptManager.transcript);
		menuTranslator.setNpcTranslator(dialogTranscriptManager.transcript);
		menuTranslator.setObjectTranslator(objectTranscriptManager.transcript);
		menuTranslator.setItemTranslator(itemTrancriptManager.transcript);
		
		log.info("RuneLingual started!");
	}
	
	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		// this should be done on the onWidgetLoaded event
		// but something seems to change the contents right back
		// somewhere before the rendering process actually happens
		// so having this happen every game tick instead
		// of every client tick is actually less resource intensive
		dialogTranslator.handleDialogs();
	}
	
	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
	{
		groundItemsTranslator.handleGroundItems();
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		menuTranslator.handleMenuEvent(event);
	}
	
	@Subscribe
	public void onChatMessage(ChatMessage event) throws Exception
	{
		chatTranslator.onChatMessage(event);
	}
	
	@Subscribe
	public void onGameStateChanged(GameStateChanged event) throws Exception
	{
		//transcriptManager.saveTranscript();
	}
	
	@Override
	protected void shutDown() throws Exception
	{
		//transcriptManager.saveTranscript();
		log.info("RuneLingual plugin stopped!");
	}

	@Provides
	RuneLingualConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneLingualConfig.class);
	}
}

