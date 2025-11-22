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
			state.resetAllTemporaryState();
			return true;
		}

		return false;
	}

	/**
	 * Updates trial progress by parsing widget text
	 * Detects difficulty changes and triggers state reset when needed
	 */
	public void updateTrialProgressFromWidgets()
	{
		if (!state.isInTrialArea())
		{
			return;
		}

		// Detect and store trial name
		String trialName = detectTrialType();
		if (trialName != null && !trialName.equals(state.getCurrentTrialName()))
		{
			log.debug("Detected trial: {}", trialName);
			state.setCurrentTrialName(trialName);
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
				state.setLostSuppliesTotal(Integer.parseInt(parts[1].trim()));
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

	/**
	 * Detect the current trial type from HUD widget text.
	 * Looks for trial names: "Tempor Tantrum", "Jubbly Jive", "Gwenith Glide"
	 * @return The trial name if detected, or null if not found
	 */
	public String detectTrialType()
	{
		if (!state.isInTrialArea())
		{
			return null;
		}

		// Check main HUD widget for trial name
		Widget barracudaTrialHudWidget = client.getWidget(InterfaceID.SailingBtHud.BARRACUDA_TRIALS);
		if (barracudaTrialHudWidget != null && !barracudaTrialHudWidget.isHidden())
		{
			String trialName = checkWidgetForTrialName(barracudaTrialHudWidget);
			if (trialName != null)
			{
				return trialName;
			}

			// Check child widgets if trial name not found in parent
			Widget[] children = barracudaTrialHudWidget.getChildren();
			if (children != null)
			{
				for (Widget child : children)
				{
					if (child != null && !child.isHidden())
					{
						trialName = checkWidgetForTrialName(child);
						if (trialName != null)
						{
							return trialName;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Check if a widget's text contains a trial name.
	 */
	private String checkWidgetForTrialName(Widget widget)
	{
		String text = widget.getText();
		if (text == null || text.isEmpty())
		{
			return null;
		}

		// Check for known trial names
		if (text.contains("Tempor Tantrum"))
		{
			return "Tempor Tantrum";
		}
		else if (text.contains("Jubbly Jive"))
		{
			return "Jubbly Jive";
		}
		else if (text.contains("Gwenith Glide"))
		{
			return "Gwenith Glide";
		}

		return null;
	}
}
