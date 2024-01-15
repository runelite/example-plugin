package com.RuneLingual;

import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;

import java.awt.*;

public class MenuCapture
{
	private TranscriptManager actionTranslator;
	private TranscriptManager npcTranslator;
	private TranscriptManager objectTranslator;
	private TranscriptManager itemTranslator;
	
	private LogHandler log;
	private boolean debugMessages;
	
	public void handleMenuEvent(MenuEntryAdded event)
	{
		// called whenever a right click menu is opened
		MenuEntry currentMenu = event.getMenuEntry();
		String menuAction = currentMenu.getOption();
		String menuTarget = currentMenu.getTarget();
		
		// some possible targets
		NPC targetNpc = currentMenu.getNpc();
		Player targetPlayer = currentMenu.getPlayer();
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
					log.log("Could not translate player action: " + menuAction);
				}
			}
			else if(isNpcMenu(menuType))
			{
				// translates menu action
				try
				{
					String newAction = actionTranslator.getTranslatedText("npcactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
					log.log("Could not translate npc action: " + menuAction);
				}
				
				// translates npc name
				try
				{
					int combatLevel = targetNpc.getCombatLevel();
					
					if(combatLevel > 0)
					{
						// attackable npcs
						int levelIndicatorIndex = menuTarget.indexOf('(');
						
						if(levelIndicatorIndex != -1)
						{
							String actualName = menuTarget.substring(0, levelIndicatorIndex);
							String newName = npcTranslator.getTranslatedName(actualName, true);
							newName += " (nível-" + combatLevel + ")";
							event.getMenuEntry().setTarget(newName);
						}
						else
						{
							// npc is attackable but does not show combat level
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
					log.log("Could not translate npc name: " + menuTarget);
				}
				
			}
			else if(isObjectMenu(menuType))
			{
				// translates menu action
				try
				{
					String newAction = actionTranslator.getTranslatedText("objectactions", menuAction, true);
					event.getMenuEntry().setOption(newAction);
				}
				catch(Exception f)
				{
					log.log("Could not translate object action: " + menuAction);
				}
				
				// translates npc name
				try
				{
					String newName = objectTranslator.getTranslatedText("objects", menuTarget, true);
					event.getMenuEntry().setTarget(newName);
				}
				catch(Exception f)
				{
					log.log("Could not translate object name: " + menuTarget);
				}
			}
			else if(isItemMenu(menuType))
			{
				// item translator
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
						log.log("Could not translate action: " + f.getMessage());
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
						log.log("Could not translate action: " + f.getMessage());
					}
				}
				else
				{
					// unknown actions
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
			log.log("Critical error happened while translating right click menus: " + e.getMessage());
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
	
	public void setLogger(LogHandler logger) {this.log = logger;}
	public void setActionTranslator(TranscriptManager newTranslator) {this.actionTranslator = newTranslator;}
	public void setNpcTranslator(TranscriptManager newTranslator) {this.npcTranslator = newTranslator;}
	public void setObjectTranslator(TranscriptManager newTranslator) {this.objectTranslator = newTranslator;}
}
