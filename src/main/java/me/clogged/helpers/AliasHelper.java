package me.clogged.helpers;

import org.apache.commons.text.WordUtils;

public class AliasHelper {

    // The collection log category name to be used in the KC lookup
    public static String KCAlias(String boss) {
        String lowerBoss = boss.toLowerCase();
        switch (lowerBoss) {
            // Brimhaven Agility Arena
            case "brimhaven agility arena":
                return "Agility Arena";

            // Ape Atoll Agility Course
            case "monkey backpacks":
                return "Ape Atoll Agility";

            // Barbarian Outpost
            case "barbarian assault":
                return "Barbarian Outpost";

            // dks
            case "prime":
                return "Dagannoth Prime";
            case "rex":
                return "Dagannoth Rex";
            case "supreme":
                return "Dagannoth Supreme";

            // hunter rumour variants
            case "hunter guild":
                return "Hunter Rumours";

            // lunar chest variants
            case "moons of peril":
                return "Lunar Chest";

            // sol heredit
            case "fortis colosseum":
                return "Sol Heredit";

            case "the inferno":
                return "TzKal-Zuk";

            case "the fight caves":
                return "TzTok-Jad";

            default:
                return WordUtils.capitalize(boss); // Assuming WordUtils is available
        }
    }
}
