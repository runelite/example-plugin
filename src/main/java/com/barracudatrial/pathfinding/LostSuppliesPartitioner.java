package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.*;

public class LostSuppliesPartitioner
{
	public List<List<Set<WorldPoint>>> generatePartitions(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<List<Set<WorldPoint>>> partitions = new ArrayList<>();

		if (numLaps == 1)
		{
			List<Set<WorldPoint>> singleLap = new ArrayList<>();
			singleLap.add(new HashSet<>(lostSupplies));
			partitions.add(singleLap);
			return partitions;
		}

		partitions.add(equalDistribution(lostSupplies, numLaps));
		partitions.add(roundRobinDistribution(lostSupplies, numLaps));
		partitions.add(spatialClusteringX(lostSupplies, numLaps));
		partitions.add(spatialClusteringY(lostSupplies, numLaps));

		if (numLaps >= 2)
		{
			partitions.add(alternatingSpatial(lostSupplies, numLaps));
		}

		return partitions;
	}

	private List<Set<WorldPoint>> equalDistribution(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<Set<WorldPoint>> partition = new ArrayList<>();
		for (int i = 0; i < numLaps; i++)
		{
			partition.add(new HashSet<>());
		}

		int suppliesPerLap = (int) Math.ceil((double) lostSupplies.size() / numLaps);
		int idx = 0;

		for (int lap = 0; lap < numLaps; lap++)
		{
			int endIdx = Math.min(idx + suppliesPerLap, lostSupplies.size());
			for (int i = idx; i < endIdx; i++)
			{
				partition.get(lap).add(lostSupplies.get(i));
			}
			idx = endIdx;
		}

		return partition;
	}

	private List<Set<WorldPoint>> roundRobinDistribution(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<Set<WorldPoint>> partition = new ArrayList<>();
		for (int i = 0; i < numLaps; i++)
		{
			partition.add(new HashSet<>());
		}

		for (int i = 0; i < lostSupplies.size(); i++)
		{
			partition.get(i % numLaps).add(lostSupplies.get(i));
		}

		return partition;
	}

	private List<Set<WorldPoint>> spatialClusteringX(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<WorldPoint> sorted = new ArrayList<>(lostSupplies);
		sorted.sort(Comparator.comparingInt(WorldPoint::getX));
		return equalDistribution(sorted, numLaps);
	}

	private List<Set<WorldPoint>> spatialClusteringY(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<WorldPoint> sorted = new ArrayList<>(lostSupplies);
		sorted.sort(Comparator.comparingInt(WorldPoint::getY));
		return equalDistribution(sorted, numLaps);
	}

	private List<Set<WorldPoint>> alternatingSpatial(List<WorldPoint> lostSupplies, int numLaps)
	{
		List<Set<WorldPoint>> partition = new ArrayList<>();
		for (int i = 0; i < numLaps; i++)
		{
			partition.add(new HashSet<>());
		}

		int centerX = lostSupplies.stream().mapToInt(WorldPoint::getX).sum() / lostSupplies.size();
		int centerY = lostSupplies.stream().mapToInt(WorldPoint::getY).sum() / lostSupplies.size();

		List<WorldPoint> sortedByDistanceFromCenter = new ArrayList<>(lostSupplies);
		sortedByDistanceFromCenter.sort((a, b) -> {
			double distA = Math.sqrt(Math.pow(a.getX() - centerX, 2) + Math.pow(a.getY() - centerY, 2));
			double distB = Math.sqrt(Math.pow(b.getX() - centerX, 2) + Math.pow(b.getY() - centerY, 2));
			return Double.compare(distA, distB);
		});

		for (int i = 0; i < sortedByDistanceFromCenter.size(); i++)
		{
			int lapIdx = (i / (sortedByDistanceFromCenter.size() / numLaps + 1)) % numLaps;
			partition.get(lapIdx).add(sortedByDistanceFromCenter.get(i));
		}

		return partition;
	}
}
