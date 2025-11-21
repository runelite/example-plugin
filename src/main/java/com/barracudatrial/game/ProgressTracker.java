package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Handles widget parsing and progress tracking for Barracuda Trial
 * Tracks rum collection, lost supplies collection, and trial area state
 */
@Slf4j
public class ProgressTracker
{
	private final Client client;
	private final State state;

	public ProgressTracker(Client client, State state)
	{
		this.client = client;
		this.state = state;
	}

	/**
	 * Checks if the player is in the trial area by checking HUD widget visibility
	 * @return true if trial area state changed
	 */
	public boolean checkIfPlayerIsInTrialArea()
	{
		Widget barracudaTrialHudWidget = client.getWidget(InterfaceID.SailingBtHud.BARRACUDA_TRIALS);

		boolean wasInTrialAreaBefore = state.isInTrialArea();
		boolean isInTrialAreaNow = barracudaTrialHudWidget != null && !barracudaTrialHudWidget.isHidden();

		state.setInTrialArea(isInTrialAreaNow);

		if (!wasInTrialAreaBefore && isInTrialAreaNow)
		{
			log.debug("Entered Barracuda Trial");
			return true;
		}
		else if (wasInTrialAreaBefore && !isInTrialAreaNow)
		{
			log.debug("Left Barracuda Trial");
			return true;
		}

		return false;
	}

	/**
	 * Updates trial progress by parsing widget text
	 * Detects difficulty changes and triggers state reset when needed
	 * @return true if rum count increased
	 */
	public boolean updateTrialProgressFromWidgets()
	{
		if (!state.isInTrialArea())
		{
			return false;
		}

		Widget rumProgressWidget = client.getWidget(InterfaceID.SailingBtHud.BT_TRACKER_PROGRESS);
		if (rumProgressWidget != null && !rumProgressWidget.isHidden())
		{
			String rumProgressText = rumProgressWidget.getText();
			parseRumProgressText(rumProgressText);
		}

		Widget lostSuppliesProgressWidget = client.getWidget(InterfaceID.SailingBtHud.BT_OPTIONAL_PROGRESS);
		if (lostSuppliesProgressWidget != null && !lostSuppliesProgressWidget.isHidden())
		{
			String lostSuppliesProgressText = lostSuppliesProgressWidget.getText();
			parselostSuppliesProgressText(lostSuppliesProgressText);
		}

		if (state.getLastKnownDifficulty() > 0 && state.getRumsNeeded() > 0
			&& state.getRumsNeeded() != state.getLastKnownDifficulty())
		{
			log.info("Difficulty changed from {} to {} rums - clearing persistent storage",
				state.getLastKnownDifficulty(), state.getRumsNeeded());
			state.clearPersistentStorage();
			state.setCurrentLap(0);
		}
		state.setLastKnownDifficulty(state.getRumsNeeded());

		boolean didRumCountIncrease = false;
		if (state.getRumsCollected() > state.getPreviousRumCount())
		{
			log.debug("Rum collected! Need to return to north");
			didRumCountIncrease = true;
		}
		state.setPreviousRumCount(state.getRumsCollected());

		return didRumCountIncrease;
	}

	private void parseRumProgressText(String rumProgressText)
	{
		try
		{
			String[] parts = rumProgressText.split("/");
			if (parts.length == 2)
			{
				state.setRumsCollected(Integer.parseInt(parts[0].trim()));
				state.setRumsNeeded(Integer.parseInt(parts[1].trim()));
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse rum progress: {}", rumProgressText);
		}
	}

	private void parselostSuppliesProgressText(String lostSuppliesProgressText)
	{
		try
		{
			String[] parts = lostSuppliesProgressText.split("/");
			if (parts.length == 2)
			{
				state.setLostSuppliesCollected(Integer.parseInt(parts[0].trim()));
				state.setlostSuppliesTotal(Integer.parseInt(parts[1].trim()));
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse lost supplies progress: {}", lostSuppliesProgressText);
		}
	}

	public int calculateRemainingLostSupplies()
	{
		return state.getLostSuppliesTotal() - state.getLostSuppliesCollected();
	}
}
