package com.luckybreak.events;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A weighted pool of events.  Events with higher weights are more likely to be chosen.
 */
public class EventPool {

    private final List<LuckyEvent> events = new ArrayList<>();
    private final List<Integer> weights = new ArrayList<>();
    private int totalWeight = 0;

    public void add(LuckyEvent event, int weight) {
        if (weight <= 0) return;
        events.add(event);
        weights.add(weight);
        totalWeight += weight;
    }

    /** Picks a random event proportional to its weight. */
    public LuckyEvent pick(RandomSource random) {
        if (events.isEmpty()) return null;
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < events.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) return events.get(i);
        }
        return events.get(events.size() - 1);
    }

    public boolean isEmpty() { return events.isEmpty(); }
    public int size() { return events.size(); }
}
