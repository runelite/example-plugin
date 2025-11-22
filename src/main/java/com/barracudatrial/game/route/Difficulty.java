package com.barracudatrial.game.route;

import lombok.Getter;

/**
 * Represents the difficulty levels for Barracuda Trial.
 * Each difficulty determines the number of laps and rum shipments required.
 */
@Getter
public enum Difficulty
{
	SWORDFISH(1),
	SHARK(2),
	MARLIN(3);

	public final int rumsRequired;

	Difficulty(int rumsRequired)
	{
		this.rumsRequired = rumsRequired;
	}

	/**
	 * Get the difficulty level from the number of rums required.
	 * @param rumsRequired The number of rum shipments needed (1, 2, or 3)
	 * @return The corresponding Difficulty, or null if not found
	 */
	public static Difficulty fromRumsRequired(int rumsRequired)
	{
		for (Difficulty difficulty : values())
		{
			if (difficulty.rumsRequired == rumsRequired)
			{
				return difficulty;
			}
		}
		return null;
	}
}
