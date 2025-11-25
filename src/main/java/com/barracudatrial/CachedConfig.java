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
	@Getter private RouteOptimization routeOptimization;
	@Getter private Color pathColor;
	@Getter private int pathWidth;
	@Getter private int pathLookahead;

	// Objective settings
	@Getter private boolean highlightObjectives;
	@Getter private Color objectivesColorCurrentWaypoint;
	@Getter private Color objectivesColorCurrentLap;
	@Getter private Color objectivesColorLaterLaps;

	// Object highlighting settings
	@Getter private boolean highlightSpeedBoosts;
	@Getter private Color speedBoostColor;
	@Getter private boolean highlightClouds;
	@Getter private Color cloudColor;
	@Getter private int cloudDangerRadius;
	@Getter private boolean highlightFetidPools;
	@Getter private Color fetidPoolColor;

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
		routeOptimization = config.routeOptimization();
		pathColor = config.pathColor();
		pathWidth = config.pathWidth();
		pathLookahead = config.pathLookahead();

		highlightObjectives = config.highlightObjectives();
		objectivesColorCurrentWaypoint = config.objectivesColorCurrentWaypoint();
		objectivesColorCurrentLap = config.objectivesColorCurrentLap();
		objectivesColorLaterLaps = config.objectivesColorLaterLaps();

		highlightSpeedBoosts = config.highlightSpeedBoosts();
		speedBoostColor = config.speedBoostColor();
		highlightClouds = config.highlightClouds();
		cloudColor = config.cloudColor();
		cloudDangerRadius = config.cloudDangerRadius();
		highlightFetidPools = config.highlightFetidPools();
		fetidPoolColor = config.fetidPoolColor();

		debugMode = config.debugMode();
		showIDs = config.showIDs();
	}
}
