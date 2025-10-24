package groupAA;

import java.util.*;

import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import core.components.Deck;
import games.sushigo.cards.SGCard;
import games.sushigo.SGGameState;
import org.json.simple.JSONObject;
import games.sushigo.SGParameters;

public class GroupAAHeuristic implements IStateHeuristic {

    // Tweak this to control how strongly the heuristic maps to [-1,1]
    private static final double MAX_POSSIBLE = 40.0;

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
        SGGameState state = (SGGameState) gs;
        double raw = state.getPlayerScore()[playerId].getValue();

        int nPlayers = state.getNPlayers();

        Map<SGCard.SGCardType, Counter>[] played = state.getPlayedCardTypes(); //contains counts of cards played this round per player for calculating the estimated value of this state with complete/incomplete combo sets.
        Map<SGCard.SGCardType, Counter>[] pointsPerType = state.getPointsPerCardType(); //contains points already awarded for each card type for calculating the estimated value of this state with complete/incomplete combo sets.

        // Use counts as fallbacks when counters are missing
        int tempuraCount = safeGetCounter(played, playerId, SGCard.SGCardType.Tempura);
        int sashimiCount = safeGetCounter(played, playerId, SGCard.SGCardType.Sashimi);
        int dumplingCount = safeGetCounter(played, playerId, SGCard.SGCardType.Dumpling);
        int wasabiCount = safeGetCounter(played, playerId, SGCard.SGCardType.Wasabi);
        int eggNigiri = safeGetCounter(played, playerId, SGCard.SGCardType.EggNigiri);
        int salmonNigiri = safeGetCounter(played, playerId, SGCard.SGCardType.SalmonNigiri);
        int squidNigiri = safeGetCounter(played, playerId, SGCard.SGCardType.SquidNigiri);


        double potential = 0.0; //estimate value from incomplete combo sets to guide the towards complete combo sets

        // Tempura: if odd (one-away from pair) estimate probability of completing the pair
        int tempuraRemainder = tempuraCount % 2;
        if (tempuraRemainder == 1) {
            // one card away -> chance to complete in future (tune probability, 0.4 is conservative)
            double probComplete = 0.4;
            int pairValue = ((SGParameters) state.getGameParameters()).valueTempuraPair;
            potential += probComplete * pairValue;
        }

        // Sashimi: similar logic for triplets
        int sashimiRemainder = sashimiCount % 3;
        if (sashimiRemainder != 0) {
            double probComplete = 0.25; // harder to finish a triplet -> lower probability
            int tripleValue = ((SGParameters) state.getGameParameters()).valueSashimiTriple;
            potential += probComplete * tripleValue;
        }

        // Dumpling: engine already awards dumpling points on reveal. We estimate marginal value to next threshold.
        int[] dumplingVals = ((SGParameters) state.getGameParameters()).valueDumpling;
        int nextDIdx = Math.min(dumplingCount, dumplingVals.length - 1);
        // compute current dumpling points already awarded for current count via pointsPerType if present
        int awardedDumplingPoints = safeGetPoints(pointsPerType, playerId, SGCard.SGCardType.Dumpling);
        int theoreticalNext = (nextDIdx + 1 < dumplingVals.length) ? dumplingVals[nextDIdx + 1] : dumplingVals[dumplingVals.length - 1];
        int currentTheoretical = dumplingVals[Math.max(0, nextDIdx)];
        int marginal = theoreticalNext - currentTheoretical;
        if (marginal > 0) {
            double prob = 0.3; // conservative
            potential += prob * marginal;
        }

        // Wasabi + nigiri: engine consumes wasabi on reveal; remaining wasabi indicates unused boosting potential
        int totalNigiri = eggNigiri + salmonNigiri + squidNigiri;
        // estimate average nigiri value (fallback 2)
        double avgNigiriValue = 1.0 * eggNigiri + 2.0 * salmonNigiri + 3.0 * squidNigiri;
        avgNigiriValue = totalNigiri > 0 ? (avgNigiriValue / totalNigiri) : 2.0;
        // engine parameter multiplier (usually 3)
        int wasabiMultiplier = ((SGParameters) state.getGameParameters()).multiplierWasabi;
        // only add extra value for *unused* wasabi (wasabiCount is unused wasabi tokens)
        if (wasabiCount > 0) {
            // potential extra from each wasabi ≈ (multiplier-1)*avgNigiriValue * probability we get a nigiri to use it
            double probGetNigiri = 0.5;
            potential += wasabiCount * (wasabiMultiplier - 1) * avgNigiriValue * probGetNigiri;
        }

        // Maki and Pudding - use engine counters across players (they are relative)
        int[] makiCounts = new int[nPlayers];
        int[] puddingCounts = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            makiCounts[p] = safeGetCounter(played, p, SGCard.SGCardType.Maki);
            puddingCounts[p] = safeGetCounter(state.getPlayedCardTypesAllGame(), p, SGCard.SGCardType.Pudding);
        }

        // Maki expected reward (if round not yet scored, estimate expected reward at round end)
        // Use actual game parameters to compute top/second splitting
        double myMakiReward = 0.0;
        int myMaki = makiCounts[playerId];
        int max1 = Arrays.stream(makiCounts).max().orElse(0);
        long topCount = Arrays.stream(makiCounts).filter(x -> x == max1).count();
        if (myMaki == max1 && max1 > 0) {
            int mostScore = ((SGParameters) state.getGameParameters()).valueMakiMost;
            myMakiReward = (double) mostScore / Math.max(1, topCount);
        } else {
            int max2 = Arrays.stream(makiCounts).filter(x -> x < max1).max().orElse(0);
            long secondCount = Arrays.stream(makiCounts).filter(x -> x == max2).count();
            if (myMaki == max2 && max2 > 0) {
                int secondScore = ((SGParameters) state.getGameParameters()).valueMakiSecond;
                myMakiReward = (double) secondScore / Math.max(1, secondCount);
            }
        }

        // Pudding: use all-game counters. If terminal, the engine will have applied pudding scoring to playerScore; if not terminal,
        // we add an expected pudding reward (small) to account for potential end-of-game pudding outcome.
        double myPuddingReward = 0.0;
        int myPuddings = puddingCounts[playerId];
        if (state.isNotTerminal()) {
            // add a small expected-value for puddings (tunable)
            // compute relative standing
            int maxP = Arrays.stream(puddingCounts).max().orElse(0);
            int minP = Arrays.stream(puddingCounts).min().orElse(0);
            if (myPuddings == maxP && maxP > 0) {
                myPuddingReward += ((double) ((SGParameters) state.getGameParameters()).valuePuddingMost) / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == maxP).count()) * 0.2; // 20% chance
            } else if (myPuddings == minP && state.getNPlayers() > 2) {
                myPuddingReward -= ((double) ((SGParameters) state.getGameParameters()).valuePuddingLeast) / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == minP).count()) * 0.2;
            }
        }

        // Combine base + expected potentials + maki/pudding expected
        raw += potential + myMakiReward + myPuddingReward;

        return normalize(raw);
    }

    private int safeGetCounter(Map<SGCard.SGCardType, Counter>[] played, int player, SGCard.SGCardType type) {
        try {
            if (played == null || played[player] == null || played[player].get(type) == null) return 0;
            return played[player].get(type).getValue();
        } catch (Throwable t) {
            return 0;
        }
    }

    private int safeGetPoints(Map<SGCard.SGCardType, Counter>[] pointsPerType, int player, SGCard.SGCardType type) {
        try {
            if (pointsPerType == null || pointsPerType[player] == null || pointsPerType[player].get(type) == null) return 0;
            return pointsPerType[player].get(type).getValue();
        } catch (Throwable t) {
            return 0;
        }
    }

    private double normalize(double raw) {
        final double MAX_POSSIBLE = 40.0; // tune to your game length and scoring range
        double clipped = Math.max(-MAX_POSSIBLE, Math.min(MAX_POSSIBLE, raw));
        return clipped / MAX_POSSIBLE;
    }
}