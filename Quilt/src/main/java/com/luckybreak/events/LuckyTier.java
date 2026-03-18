package com.luckybreak.events;

import net.minecraft.util.RandomSource;

/**
 * The three outcome tiers for a Lucky Block break.
 * Default weights: UNLUCKY=30%, AVERAGE=60%, LUCKY=10%.
 * Configurable via data/luckybreak/lucky_events/config.json.
 */
public enum LuckyTier {
    UNLUCKY("unlucky"),
    AVERAGE("average"),
    LUCKY("lucky");

    private final String id;

    LuckyTier(String id) { this.id = id; }

    public String getId() { return id; }

    /**
     * Rolls the tier using the given percentages.
     *
     * @param random         source of randomness
     * @param luckyChance    0-100 percent chance of LUCKY
     * @param averageChance  0-100 percent chance of AVERAGE (remainder = UNLUCKY)
     */
    public static LuckyTier roll(RandomSource random, int luckyChance, int averageChance) {
        int r = random.nextInt(100);
        if (r < luckyChance)                     return LUCKY;
        if (r < luckyChance + averageChance)     return AVERAGE;
        return UNLUCKY;
    }
}
