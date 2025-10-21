package groupAA;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import org.json.simple.JSONObject;
import core.components.Deck;
import core.components.Card;
// import games.sushigo.SushiGoGameState; // removed

public class GroupAAHeuristic implements IStateHeuristic {

    // Remove unused fake type markers
    // private static final Object Dumpling = new Object();
    // private static final Object Sashimi = new Object();
    // private static final Object Wasabi = new Object();
    // private static final Object Tempura = new Object();

    public GroupAAHeuristic() {
        System.out.println("GroupAAHeuristic initialized (no-arg constructor)");
    }

    public GroupAAHeuristic(JSONObject json) {
        System.out.println("GroupAAHeuristic initialized from JSON: " + json.toJSONString());
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // SushiGoGameState state = (SushiGoGameState) gs; // removed
        AbstractGameState state = gs; // use base type

        double score = 0;
        score += state.getGameScore(playerId);

        int table = state.getNPlayers(playerId);

        int count_tempura = 0;
        int count_sashimi = 0;
        int count_dumpling = 0;
        int count_wasabi = 0;

        for (Card c : table.getComponents()) {
            String name = c.getComponentName(); // e.g., "Tempura", "Sashimi", "Dumpling", "Wasabi", "SquidNigiri", etc.
            switch (name) {
                case "Tempura":
                    count_tempura++;
                    break;
                case "Sashimi":
                    count_sashimi++;
                    break;
                case "Dumpling":
                    count_dumpling++;
                    break;
                case "Wasabi":
                    count_wasabi++;
                    break;
                default:
                    break;
            }
        }

        // Rewards
        score += (count_tempura / 2) * 5;
        score += (count_sashimi / 3) * 10;
        score += Math.min(15, dumplingHeuristic(count_dumpling));

        // normalize score to [-1, 1]
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
    public double minValue() { return -1; }

    @Override
    public double maxValue() { return 1; }
}
