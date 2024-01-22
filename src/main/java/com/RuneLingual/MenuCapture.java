package com.RuneLingual;

import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.awt.*;

public class MenuCapture
{
	@Inject
	private Client client;
	@Inject
	private RuneLingualConfig config;
	
	private TranscriptManager actionTranslator;
	private TranscriptManager npcTranslator;
	private TranscriptManager objectTranslator;
	private TranscriptManager itemTranslator;
	
	private LogHandler log;
	private boolean debugMessages = true;
	
	// TODO: as is the menu title 'Chose Options' seems to not be directly editable
	
	public void handleMenuEvent(MenuEntryAdded event)
	{
		// called whenever a right click menu is opened
		MenuEntry currentMenu = event.getMenuEntry();
		String menuAction = currentMenu.getOption();
		String menuTarget = currentMenu.getTarget();
		
		// some possible targets
		NPC targetNpc = currentMenu.getNpc();
		Player targetPlayer = currentMenu.getPlayer();
		int targetItem = currentMenu.getItemId();
		MenuAction menuType = currentMenu.getType();
		
		try
		{
			if(isPlayerMenu(menuType))
			{
				// translates menu action
				try
				{
					String newAction = actionTranslator.getTranslatedText("playeractions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						log.log("Could not translate player action: " + menuAction);
					}
				}
			}
			else if(isNpcMenu(menuType))
			{
				translateMenuAction("npcactions", event, menuAction);
				
				// translates npc name
				try
				{
					int combatLevel = targetNpc.getCombatLevel();
					
					if(combatLevel > 0)
					{
						// attackable npcs
						int levelIndicatorIndex = menuTarget.indexOf('(');
						
						if(levelIndicatorIndex != -1)
						{  // npc has a combat level
							String actualName = menuTarget.substring(0, levelIndicatorIndex);
							String newName = npcTranslator.getTranslatedName(actualName, true);
							
							String levelIndicator = actionTranslator.getTranslatedText("npcactions", "level", true);
							newName += " (" + levelIndicator + "-" + combatLevel + ")";
							event.getMenuEntry().setTarget(newName);
						}
						else
						{  // npc does not have a combat level
							String newName = npcTranslator.getTranslatedName(menuTarget, true);
							event.getMenuEntry().setTarget(newName);
						}
					}
					else
					{
						// non attackable npcs
						String newName = npcTranslator.getTranslatedName(menuTarget, true);
						event.getMenuEntry().setTarget(newName);
					}
					
				}
				catch(Exception f)
				{
					if(debugMessages)
					{
						log.log("Could not translate npc name: " + menuTarget + " - " + f.getMessage());
					}
				}
				
			}
			else if(isObjectMenu(menuType))
			{
				translateItemName("objects", event, menuTarget);
				translateMenuAction("objectactions", event, menuAction);
			}
			else if(isItemMenu(menuType))
			{  // ground item
				translateItemName("items", event, menuTarget);
				translateMenuAction("itemactions", event, menuAction);
			}
			else if(targetItem != -1)
			{  // inventory item
				translateItemName("items", event, menuTarget);
				translateMenuAction("iteminterfaceactions", event, menuAction);
			}
			else
			{
				// nor a player or npc
				log.log("Menu action:" + menuAction + " - Menu target:" + menuTarget + "type:" + event.getMenuEntry().getType());
				
				if(menuType.equals(MenuAction.CANCEL))
				{
					// tries to translate action
					try
					{
						String newAction = actionTranslator.getTranslatedText("generalactions", menuAction, true);
						event.getMenuEntry().setOption(newAction);
					}
					catch(Exception f)
					{
						if(debugMessages)
						{
							log.log("Could not translate action: " + f.getMessage());
						}
					}
				}
				else if(menuType.equals(MenuAction.WALK))
				{
					// tries to translate action
					try
					{
						String newAction = actionTranslator.getTranslatedText("generalactions", menuAction, true);
						event.getMenuEntry().setOption(newAction);
					}
					catch(Exception f)
					{
						if(debugMessages)
						{
							log.log("Could not translate action: " + f.getMessage());
						}
					}
				}
				else
				{
					// other menus
				}
				
				/*
				// tries to translate general actions
				try
				{
					String newAction = actionTranslator.getTranslatedText("generalactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
				
					log.log("Could not translate action: " + f.getMessage());
					
				}*/
				
			}
			
		}
		catch (Exception e)
		{
			if(debugMessages)
			{
				log.log("Critical error happened while processing right click menus: " + e.getMessage());
			}
		}
	}
	
	private void translateItemName(String source, MenuEntryAdded entryAdded, String target)
	{
		if(target.length() == 0)
		{
			return;
		}
		
		// translates item name
		try
		{
			String newName = target;
			if(source.equals("items"))
			{
				newName = itemTranslator.getTranslatedText(source, target, true);
			}
			else if(source.equals("objects"))
			{
				newName = objectTranslator.getTranslatedText(source, target, true);
			}
			
			entryAdded.getMenuEntry().setTarget(newName);
		}
		catch(Exception f)
		{
			if(debugMessages)
			{
				log.log("Could not translate '"
		            + source
			        + "' name: "
		            + target
					+ " - "
					+ f.getMessage());
			}
		}
	}
	private void translateMenuAction(String source, MenuEntryAdded entryAdded, String target)
	{
		// translates menu action
		try
		{
			String newAction = actionTranslator.getTranslatedText(source, target, true);
			entryAdded.getMenuEntry().setOption(newAction);
		}
		catch(Exception f)
		{
			if(!source.equals("generalactions"))
			{
				try
				{
					translateMenuAction("generalactions", entryAdded, target);
				}
				catch(Exception g)
				{
					if(debugMessages)
					{
						log.log("Could not translate menu '"
					        + source
					        + "' action: "
					        + target
					        + " - "
					        + f.getMessage()
							+ " - "
							+ g.getMessage());
					}
				}
			}
		}
	}
	
	private boolean isObjectMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_OBJECT))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GAME_OBJECT_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isNpcMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_NPC))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.NPC_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isItemMenu(MenuAction action)
	{
		if(action.equals(MenuAction.EXAMINE_ITEM_GROUND))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.GROUND_ITEM_FIFTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	private boolean isPlayerMenu(MenuAction action)
	{
		if(action.equals(MenuAction.PLAYER_FIRST_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SECOND_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_THIRD_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_FOURTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_FIFTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SIXTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_SEVENTH_OPTION))
		{
			return true;
		}
		else if(action.equals(MenuAction.PLAYER_EIGHTH_OPTION))
		{
			return true;
		}
		return false;
	}
	
	public void setDebug(boolean newValue) {this.debugMessages = newValue;}
	public void setLogger(LogHandler logger) {this.log = logger;}
	public void setActionTranslator(TranscriptManager newTranslator) {this.actionTranslator = newTranslator;}
	public void setNpcTranslator(TranscriptManager newTranslator) {this.npcTranslator = newTranslator;}
	public void setObjectTranslator(TranscriptManager newTranslator) {this.objectTranslator = newTranslator;}
	public void setItemTranslator(TranscriptManager newTranslator) {this.itemTranslator = newTranslator;}
}
