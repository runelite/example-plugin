package com.barracudatrial.game.route;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrialType
{
	TEMPOR_TANTRUM("Tempor Tantrum"),
	JUBBLY_JIVE("Jubbly Jive"),
	GWENITH_GLIDE("Gwenith Glide");

	private final String displayName;

	public static TrialType fromDisplayName(String name)
	{
		if (name == null)
		{
			return null;
		}

		for (TrialType type : values())
		{
			if (type.displayName.equalsIgnoreCase(name))
			{
				return type;
			}
		}
		return null;
	}
}
