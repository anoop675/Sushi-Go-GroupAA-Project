package groupAA;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import org.json.simple.JSONObject;
import core.components.Deck;
import games.sushigo.SGCard;
import games.sushigo.SushiGoGameState;

public class GroupAAHeuristic implements IStateHeuristic {

    // No-arg constructor (used by default)
    public GroupAAHeuristic() {
        System.out.println("GroupAAHeuristic initialized (no-arg constructor)");
    }

    // JSON constructor (used by JSONUtils)
    public GroupAAHeuristic(JSONObject json) {
        System.out.println("GroupAAHeuristic initialized from JSON: " + json.toJSONString());
        // Optional: parse fields from JSON if needed in the future
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // Simple placeholder heuristic
        SushiGoGameState state = (SushiGoGameState) gs;

        double score = 0;
        score += state.getPlayerScore(playerId);

        // Deck<SGCard> = List<SGCard>
        // List of cards on table
        Deck<SGCard> table = state.getPlayerCards(playerId);

        int count_tempura = 0;
        int count_sashimi = 0;
        int count_dumpling = 0;
        int count_wasabi = 0;
        int nigiri_value = 0;
        int makiRolls = 0;

        for (SGCard c : table.getComponents()) {
            switch (c.cardType) {
                case Tempura: count_tempura++; break;
                case Sashimi: count_sashimi++; break;
                case Dumpling -> count_dumpling++; break;
                case Wasabi -> count_wasabi++; break;
            }

        // Rewards
        score += (tempuraCount / 2) * 5;
        score += (sashimiCount / 3) * 10;
        score += Math.min(15, dumplingHeuristic(dumplingCount));

        // normalize score in range [-1,1] for heuristics
        return Math.max(-1, Math.min(1, score * 2 - 1));
    }

    private double dumplingHeuristic(int count) {
        switch (count) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 3;
            case 3: return 6;
            case 4: return 10;
            default: return 15;
        }
    }
    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }
}
