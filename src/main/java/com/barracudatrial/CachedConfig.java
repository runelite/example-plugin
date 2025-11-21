package com.barracudatrial;

import lombok.Getter;

import java.awt.Color;

/**
 * Caches config values for performance
 * Config method calls are slow, so we cache values and update on config changes
 */
public class CachedConfig
{
	private final BarracudaTrialConfig config;

	// Path settings
	@Getter private boolean showOptimalPath;
	@Getter private Color pathColor;
	@Getter private Color tracerColor;
	@Getter private int pathWidth;
	@Getter private boolean showPathTracer;
	@Getter private BarracudaTrialConfig.StartingDirection startingDirection;
	@Getter private int pathLookahead;

	// Lost supplies settings
	@Getter private boolean highlightLostSupplies;
	@Getter private Color lostSuppliesColor;
	@Getter private boolean showLostSuppliesTile;
	@Getter private boolean highlightRumLocations;
	@Getter private Color rumLocationColor;

	// Cloud settings
	@Getter private boolean highlightClouds;
	@Getter private Color cloudColor;
	@Getter private int cloudDangerRadius;

	// Hazard settings
	@Getter private boolean highlightRocks;
	@Getter private Color rockColor;
	@Getter private boolean highlightSpeedBoosts;
	@Getter private Color speedBoostColor;

	// Debug settings
	@Getter private boolean debugMode;
	@Getter private boolean showIDs;

	public CachedConfig(BarracudaTrialConfig config)
	{
		this.config = config;
		updateCache();
	}

	/**
	 * Updates all cached values from the config
	 * Should be called on plugin startup and when config changes
	 */
	public void updateCache()
	{
		showOptimalPath = config.showOptimalPath();
		pathColor = config.pathColor();
		tracerColor = config.tracerColor();
		pathWidth = config.pathWidth();
		showPathTracer = config.showPathTracer();
		startingDirection = config.startingDirection();
		pathLookahead = config.pathLookahead();

		highlightLostSupplies = config.highlightLostSupplies();
		lostSuppliesColor = config.lostSuppliesColor();
		showLostSuppliesTile = config.showLostSuppliesTile();
		highlightRumLocations = config.highlightRumLocations();
		rumLocationColor = config.rumLocationColor();

		highlightClouds = config.highlightClouds();
		cloudColor = config.cloudColor();
		cloudDangerRadius = config.cloudDangerRadius();

		highlightRocks = config.highlightRocks();
		rockColor = config.rockColor();
		highlightSpeedBoosts = config.highlightSpeedBoosts();
		speedBoostColor = config.speedBoostColor();

		debugMode = config.debugMode();
		showIDs = config.showIDs();
	}
}
