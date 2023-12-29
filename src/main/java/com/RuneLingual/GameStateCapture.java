package com.RuneLingual;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.widgets.Widget;

public class GameStateCapture
{

	private Client clientReference;
	
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) throws Exception
	{
		// TODO: this
		
		/*
		GameState newGameState = gameStateChanged.getGameState();
		if(newGameState == GameState.LOGGED_IN)
		{
			clientReference.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "RuneLingual is running.", null);
		}
		else if(newGameState == GameState.LOGIN_SCREEN)
		{
			Widget loginScreen = clientReference.getWidget(WidgetInfo.LOGIN_CLICK_TO_PLAY_SCREEN);
			System.out.println("no menu:" + loginScreen.getChildren());
			
			//chatTranslator.shutdown(changesDetected);
			//dialogTranslator.shutdown();
		}*/
	}
}
