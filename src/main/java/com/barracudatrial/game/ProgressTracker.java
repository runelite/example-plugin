package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

/**
 * Handles widget parsing and progress tracking for Barracuda Trial
 * Tracks rum collection, crate collection, and trial area state
 */
@Slf4j
public class ProgressTracker
{
	private final Client client;
	private final State state;

	// Widget IDs
	private static final int BARRACUDA_TRIALS_HUD = 931;
	private static final int WIDGET_BT_HUD = 3;
	private static final int WIDGET_RUM_PROGRESS = 24;
	private static final int WIDGET_CRATE_PROGRESS = 25;

	public ProgressTracker(Client client, State state)
	{
		this.client = client;
		this.state = state;
	}

	/**
	 * Checks if the player is in the trial area by checking HUD widget visibility
	 * @return true if trial area state changed
	 */
	public boolean checkTrialArea()
	{
		// Check if Barracuda Trial HUD widget is visible
		Widget hudWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_BT_HUD);

		boolean wasInTrialArea = state.isInTrialArea();
		boolean nowInTrialArea = hudWidget != null && !hudWidget.isHidden();

		state.setInTrialArea(nowInTrialArea);

		if (!wasInTrialArea && nowInTrialArea)
		{
			log.debug("Entered Barracuda Trial");
			return true;
		}
		else if (wasInTrialArea && !nowInTrialArea)
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
	public boolean updateTrialProgress()
	{
		if (!state.isInTrialArea())
		{
			return false;
		}

		// Parse rum progress widget (format: "0 / 1")
		Widget rumWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_RUM_PROGRESS);
		if (rumWidget != null && !rumWidget.isHidden())
		{
			String rumText = rumWidget.getText();
			parseRumProgress(rumText);
		}

		// Parse crate progress widget
		Widget crateWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_CRATE_PROGRESS);
		if (crateWidget != null && !crateWidget.isHidden())
		{
			String crateText = crateWidget.getText();
			parseCrateProgress(crateText);
		}

		// Detect difficulty change - clear persistent storage if rumsNeeded changes
		if (state.getLastKnownDifficulty() > 0 && state.getRumsNeeded() > 0
			&& state.getRumsNeeded() != state.getLastKnownDifficulty())
		{
			log.info("Difficulty changed from {} to {} rums - clearing persistent storage",
				state.getLastKnownDifficulty(), state.getRumsNeeded());
			state.clearPersistentStorage();
			state.setCurrentLap(0); // Reset lap counter
		}
		state.setLastKnownDifficulty(state.getRumsNeeded());

		// Detect when rum count increases - need to return rum to north
		boolean rumCountIncreased = false;
		if (state.getRumsCollected() > state.getLastRumCount())
		{
			log.debug("Rum collected! Need to return to north");
			rumCountIncreased = true;
		}
		state.setLastRumCount(state.getRumsCollected());

		return rumCountIncreased;
	}

	/**
	 * Parses rum progress widget text
	 * Format: "0 / 1" (collected / needed)
	 */
	private void parseRumProgress(String text)
	{
		// Format: "0 / 1"
		try
		{
			String[] parts = text.split("/");
			if (parts.length == 2)
			{
				state.setRumsCollected(Integer.parseInt(parts[0].trim()));
				state.setRumsNeeded(Integer.parseInt(parts[1].trim()));
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse rum progress: {}", text);
		}
	}

	/**
	 * Parses crate progress widget text
	 * Format: "2 / 14" (collected / total)
	 */
	private void parseCrateProgress(String text)
	{
		// Format: "2 / 14" (collected / total)
		try
		{
			String[] parts = text.split("/");
			if (parts.length == 2)
			{
				state.setCratesCollected(Integer.parseInt(parts[0].trim()));
				state.setCratesTotal(Integer.parseInt(parts[1].trim()));
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse crate progress: {}", text);
		}
	}

	/**
	 * Returns the number of crates still remaining to collect
	 */
	public int getCratesRemaining()
	{
		return state.getCratesTotal() - state.getCratesCollected();
	}
}
