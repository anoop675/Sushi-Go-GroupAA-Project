/*
    custom heuristic based on Expected Utility Estimation with Opponent Modelling for greedy rollout policy
 */
package groupAA;

import java.util.*;

import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import games.sushigo.cards.SGCard;
import games.sushigo.SGGameState;
import org.json.simple.JSONObject;
import games.sushigo.SGParameters;

public class GroupAAHeuristic implements IStateHeuristic {

    // Tweak this to control how strongly the heuristic maps to [-1,1]
    private static final double MAX_POSSIBLE = 40.0;
    // Base probability for completing a set when one card away (can be tuned)
    private static final double PROB_ONE_AWAY = 0.5;
    // Base probability for completing a set when two cards away
    private static final double PROB_TWO_AWAY = 0.1;

    public GroupAAHeuristic() {
        // System.out.println("GroupAAHeuristic initialized (no-arg constructor)");
    }

    public GroupAAHeuristic(JSONObject json) {
        // System.out.println("GroupAAHeuristic initialized from JSON: " + json.toJSONString());
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs;
        int nPlayers = state.getNPlayers();

        Map<SGCard.SGCardType, Counter>[] played = state.getPlayedCardTypes();

        double myRaw = state.getPlayerScore()[playerId].getValue(); //returns the root player's raw score
        double myPotential = calculateRoundPotential(state, played, playerId); //returns the root player's potential score

        double[] myMakiRewards = calculateMakiRewards(state, played, playerId, nPlayers);
        double myMakiReward = myMakiRewards[0]; // My expected reward is always the first element

        double[] myPuddingRewards = {0.0};
        if (state.isNotTerminal()) {
            myPuddingRewards = calculatePuddingRewards(state, playerId, nPlayers);
        }
        double myPuddingReward = myPuddingRewards[0]; // My expected reward is always the first element

        double myTotalScoreEstimate = myRaw + myPotential + myMakiReward + myPuddingReward;


        //Calculating the opponent's maximum threat score
        double maxOpponentScoreEstimate = -Double.MAX_VALUE;

        for (int opponentId = 0; opponentId < nPlayers; opponentId++) {
            if (opponentId == playerId) continue;

            // Calculate this specific opponent's estimated score components
            double oppRaw = state.getPlayerScore()[opponentId].getValue();
            double oppPotential = calculateRoundPotential(state, played, opponentId);

            // Note: Maki and Pudding helpers return my rewards and then the opponent rewards
            // We need to re-call them to get the correct rewards for the current opponentId relative to others.
            double[] oppMakiRewards = calculateMakiRewards(state, played, opponentId, nPlayers);
            double oppMakiReward = oppMakiRewards[0];

            double[] oppPuddingRewards = {0.0};
            if (state.isNotTerminal()) {
                oppPuddingRewards = calculatePuddingRewards(state, opponentId, nPlayers);
            }
            double oppPuddingReward = oppPuddingRewards[0];

            double oppTotalScoreEstimate = oppRaw + oppPotential + oppMakiReward + oppPuddingReward;

            // Track the maximum estimated score among all opponents
            maxOpponentScoreEstimate = Math.max(maxOpponentScoreEstimate, oppTotalScoreEstimate);
        }

        // --- 3. Return the Score Differential against the MAX Threat ---
        // This heuristic guides the agent to maximize the difference between its score and the best opponent's score.
        double scoreDifferential = myTotalScoreEstimate - maxOpponentScoreEstimate;

        return normalize(scoreDifferential);
    }

    /** Calculates the potential score from incomplete Tempura, Sashimi, Dumpling, and Wasabi for a given player. */
    private double calculateRoundPotential(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int player) {
        double potential = 0.0;
        SGParameters params = (SGParameters) state.getGameParameters();

        int tempuraCount = safeGetCounter(played, player, SGCard.SGCardType.Tempura);
        int sashimiCount = safeGetCounter(played, player, SGCard.SGCardType.Sashimi);
        int dumplingCount = safeGetCounter(played, player, SGCard.SGCardType.Dumpling);
        int wasabiCount = safeGetCounter(played, player, SGCard.SGCardType.Wasabi);
        int eggNigiri = safeGetCounter(played, player, SGCard.SGCardType.EggNigiri);
        int salmonNigiri = safeGetCounter(played, player, SGCard.SGCardType.SalmonNigiri);
        int squidNigiri = safeGetCounter(played, player, SGCard.SGCardType.SquidNigiri);

        // --- Tempura Potential ---
        int tempuraRemainder = tempuraCount % 2;
        if (tempuraRemainder == 1) { // One away from a pair
            potential += PROB_ONE_AWAY * params.valueTempuraPair;
        }

        // --- Sashimi Potential ---
        int sashimiRemainder = sashimiCount % 3;
        if (sashimiRemainder == 1) { // Two away from a triple
            potential += PROB_TWO_AWAY * params.valueSashimiTriple;
        } else if (sashimiRemainder == 2) { // One away from a triple
            potential += PROB_ONE_AWAY * params.valueSashimiTriple;
        }

        // --- Dumpling Potential ---
        int[] dumplingVals = params.valueDumpling;
        int currentDIdx = Math.min(dumplingCount, dumplingVals.length - 1);
        int nextDIdx = Math.min(currentDIdx + 1, dumplingVals.length - 1);
        int currentTheoretical = dumplingVals[currentDIdx];
        int theoreticalNext = dumplingVals[nextDIdx];
        int marginal = theoreticalNext - currentTheoretical;
        if (marginal > 0) {
            potential += PROB_ONE_AWAY * marginal;
        }

        // --- Wasabi + Nigiri Potential ---
        if (wasabiCount > 0) {
            int totalNigiri = eggNigiri + salmonNigiri + squidNigiri;
            double avgNigiriValue = (double)(eggNigiri * params.valueEggNigiri + salmonNigiri * params.valueSalmonNigiri + squidNigiri * params.valueSquidNigiri);
            avgNigiriValue = totalNigiri > 0 ? (avgNigiriValue / totalNigiri) : (params.valueSalmonNigiri); // Fallback to Salmon value

            int wasabiMultiplier = params.multiplierWasabi;
            potential += wasabiCount * (wasabiMultiplier - 1) * avgNigiriValue * PROB_ONE_AWAY;
        }

        return potential;
    }

    /** Calculates the expected Maki reward for the specified player relative to all others.
     * Returns: [Player's Reward, Max Opponent Reward]
     */
    private double[] calculateMakiRewards(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int playerId, int nPlayers) {
        int[] makiCounts = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            makiCounts[p] = safeGetCounter(played, p, SGCard.SGCardType.Maki);
        }

        SGParameters params = (SGParameters) state.getGameParameters();
        int mostScore = params.valueMakiMost;
        int secondScore = params.valueMakiSecond;

        double myMakiReward = 0.0;
        double maxOppMakiReward = 0.0;

        int myMaki = makiCounts[playerId];
        int max1 = Arrays.stream(makiCounts).max().orElse(0);

        // --- Calculate rewards for ALL players ---
        double[] allRewards = new double[nPlayers];

        if (max1 > 0) {
            // Find top and second scores
            long topCount = Arrays.stream(makiCounts).filter(x -> x == max1).count();
            int max2 = Arrays.stream(makiCounts).filter(x -> x < max1).max().orElse(0);
            long secondCount = Arrays.stream(makiCounts).filter(x -> x == max2).count();

            for (int p = 0; p < nPlayers; p++) {
                if (makiCounts[p] == max1) {
                    allRewards[p] = (double) mostScore / topCount;
                } else if (makiCounts[p] == max2 && max2 > 0) {
                    allRewards[p] = (double) secondScore / secondCount;
                }
            }
        }

        // --- Extract My Reward and Max Opponent Reward ---
        myMakiReward = allRewards[playerId];

        for (int p = 0; p < nPlayers; p++) {
            if (p != playerId) {
                maxOppMakiReward = Math.max(maxOppMakiReward, allRewards[p]);
            }
        }

        return new double[]{myMakiReward, maxOppMakiReward};
    }

    /** Calculates the expected Pudding reward for the current player relative to all others.
     * Returns: [Player's Reward, Max Opponent Reward]
     */
    private double[] calculatePuddingRewards(AbstractGameState state, int playerId, int nPlayers) {
        SGGameState sgState = (SGGameState) state;

        int[] puddingCounts = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            puddingCounts[p] = safeGetCounter(sgState.getPlayedCardTypesAllGame(), p, SGCard.SGCardType.Pudding);
        }

        SGParameters params = (SGParameters) state.getGameParameters();
        int mostScore = params.valuePuddingMost;
        int leastScore = params.valuePuddingLeast;

        // Use a small expected probability multiplier for non-terminal states
        final double PROB_FINISH = 0.2;

        int maxP = Arrays.stream(puddingCounts).max().orElse(0);
        int minP = Arrays.stream(puddingCounts).min().orElse(0);

        double myPuddingReward = 0.0;
        double maxOppPuddingReward = 0.0;
        double[] allRewards = new double[nPlayers];

        // --- Calculate rewards for ALL players ---
        for (int p = 0; p < nPlayers; p++) {
            double pReward = 0.0;
            // MOST reward
            if (puddingCounts[p] == maxP && maxP > 0) {
                pReward += (double)mostScore / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == maxP).count()) * PROB_FINISH;
            }
            // LEAST penalty
            if (puddingCounts[p] == minP && nPlayers > 1) {
                pReward -= (double)leastScore / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == minP).count()) * PROB_FINISH;
            }
            allRewards[p] = pReward;
        }

        // --- Extract My Reward and Max Opponent Reward ---
        myPuddingReward = allRewards[playerId];
        for (int p = 0; p < nPlayers; p++) {
            if (p != playerId) {
                maxOppPuddingReward = Math.max(maxOppPuddingReward, allRewards[p]);
            }
        }

        return new double[]{myPuddingReward, maxOppPuddingReward};
    }

    private int safeGetCounter(Map<SGCard.SGCardType, Counter>[] played, int player, SGCard.SGCardType type) {
        try {
            if (played == null || played[player] == null || played[player].get(type) == null) return 0;
            return played[player].get(type).getValue();
        } catch (Throwable t) {
            return 0;
        }
    }

    private double normalize(double raw) {
        double clipped = Math.max(-MAX_POSSIBLE, Math.min(MAX_POSSIBLE, raw));
        return clipped / MAX_POSSIBLE;
    }
}