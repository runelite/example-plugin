package com.barracudatrial.game.route;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public interface TrialConfig
{
	TrialType getTrialType();

	// Shipment/lost supplies (universal across all trials, but different object IDs)
	Set<Integer> getShipmentBaseIds();
	int getShipmentImpostorId();

	// Trial-specific objective locations (rum pickup/dropoff for Tempor, toad pillars for Jubbly, etc)
	Set<Integer> getPrimaryObjectiveIds();
	Set<Integer> getSecondaryObjectiveIds();
	WorldPoint getPrimaryObjectiveLocation();
	WorldPoint getSecondaryObjectiveLocation();

	// Hazards (storm clouds for Tempor, fetid pools for Jubbly, etc)
	Set<Integer> getHazardNpcIds();
	boolean hasMovingHazards();

	// Obstacles (rocks)
	Set<Integer> getRockIds();

	// Exclusion zone offsets (relative to primary objective location)
	int getExclusionMinXOffset();
	int getExclusionMaxXOffset();
	int getExclusionMinYOffset();
	int getExclusionMaxYOffset();

	// Chat message patterns for objective completion
	Pattern getPrimaryObjectivePickupPattern();
	Pattern getSecondaryObjectiveCompletionPattern();

	// Routes
	List<RouteWaypoint> getRoute(Difficulty difficulty);
}
