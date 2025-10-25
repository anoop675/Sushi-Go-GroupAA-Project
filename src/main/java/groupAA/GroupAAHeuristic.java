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

        Deck<SGCard> playerTable = state.getNPlayers(playerId);

        int count_tempura = 0;
        int count_sashimi = 0;
        int count_dumpling = 0;
        int count_wasabi = 0;
        int count_chopsticks = 0;

        List<String> nigiris = new ArrayList<>();

        for (Card c : playerTable.getComponents()) {
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
                case "EggNigiri":
                    nigiris.add("Egg");
                    break;
                case "SalmonNigiri":
                    nigiris.add("Salmon");
                    break;
                case "SquidNigiri":
                    nigiris.add("Squid");
                    break;
                case "MakiRoll1":
                    count_maki += 1;
                    break;
                case "MakiRoll2":
                    count_maki += 2;
                    break;
                case "MakiRoll3":
                    count_maki += 3;
                    break;
                case "Pudding":
                    count_pudding++;
                    break;
                case "Chopsticks":
                    count_chopsticks++;
                    break;
                default:
                    break;
            }
        }

        // Rewards
        score += (count_tempura / 2) * 5;
        score += (count_sashimi / 3) * 10;
        score += dumplingHeuristic(count_dumpling);
        score += nigiriHeuristic(nigiris, count_wasabi);

        // Maki Rules: Assume top place get 6pts.
        // Assume getting 10 Makis will lane you top rank (changable value)
        double makiRatio = (double) count_maki / 10.0;
        score += makiRatio * 6;

        // Bias to encourage picking up pudding (changable value)
        score += count_pudding * 1.5;

        // Bias to encourage picking up chopstic (changable value)
        score += count_chopsticks * 2.0;

        return score;
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

    private double nigiriHeuristic(List<String> nigiris, int count_wasabi){
        double score = 0;
        for (String nigiri : nigiris) {
            double base;

            switch (nigiri) {
                case "Egg": base = 1;
                case "Salmon": base = 2;
                case "Squid": base = 3;
                default: base = 0;
            }
            if (count_wasabi > 0) {
                score += base * 3;
                count_wasabi--;
            } else {
                score += base;
            }

        return score;
    }

    @Override
    public double minValue() { return -1; }

    @Override
    public double maxValue() { return 1; }
}
