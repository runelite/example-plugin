package com.barracudatrial.game;

import com.barracudatrial.game.route.JubblyJiveConfig;
import com.barracudatrial.game.route.TemporTantrumConfig;
import com.barracudatrial.game.route.TrialConfig;
import com.barracudatrial.game.route.TrialType;
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

		var title = client.getWidget(InterfaceID.SailingBtHud.BT_TITLE);
		if (title != null && !title.isHidden())
		{
			var children = title.getChildren();
			String trialName = null;
			if (children != null)
			{
				for (var child : children) {
					if (child == null || child.isHidden())
						continue;
					var text = child.getText();
					if (text != null && !text.isEmpty())
					{
						trialName = text;
						break;
					}
				}
			}

			if (trialName != null && !trialName.equals(state.getCurrentTrialName()))
			{
				log.info("Detected trial: {}", trialName);
				state.setCurrentTrialName(trialName);

				TrialType trialType = TrialType.fromDisplayName(trialName);
				if (trialType != null) {
					TrialConfig trialConfig = createTrialConfig(trialType);
					state.setCurrentTrial(trialConfig);
					log.info("Initialized trial config for: {}", trialType);
				}
			}
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
			parseLostSuppliesProgressText(lostSuppliesProgressText);
		}

		if (state.getLastKnownDifficulty() > 0 && state.getRumsNeeded() > 0
			&& state.getRumsNeeded() != state.getLastKnownDifficulty())
		{
			log.info("Difficulty changed from {} to {} rums - clearing persistent storage",
				state.getLastKnownDifficulty(), state.getRumsNeeded());
			state.clearPersistentStorage();
			state.setCurrentLap(1);
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

	private void parseLostSuppliesProgressText(String lostSuppliesProgressText)
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

	/**
	 * Creates the appropriate trial configuration based on the trial type
	 */
	private TrialConfig createTrialConfig(TrialType trialType)
	{
		switch (trialType)
		{
			case TEMPOR_TANTRUM:
				return new TemporTantrumConfig();
			case JUBBLY_JIVE:
				return new JubblyJiveConfig();
			case GWENITH_GLIDE:
				// TODO: Implement GwenithGlideConfig when needed
				log.warn("Gwenith Glide config not yet implemented, using Tempor Tantrum as fallback");
				return new TemporTantrumConfig();
			default:
				log.warn("Unknown trial type: {}, using Tempor Tantrum as fallback", trialType);
				return new TemporTantrumConfig();
		}
	}
}
