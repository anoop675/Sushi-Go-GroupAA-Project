/*
    Custom Expected Utility Estimation Heuristic with Opponent Modelling for Greedy Rollout Policy
 */
package groupAA;

import java.util.*;
import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import games.sushigo.cards.SGCard;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;

public class GroupAAHeuristic implements IStateHeuristic {

    private static final double MAX_POSSIBLE = 40.0; //maximum achievable score (score difference) that a player could get in a round or in the entire game, just an approximation to clamp the heuristic (utility) value within [-1,1]
    private static final double PROB_ONE_CARD_AWAY = 0.5; //base probability for completing a set when one card away (can be tuned)
    private static final double PROB_TWO_CARDS_AWAY = 0.1; //base probability for completing a set when two cards away

    public GroupAAHeuristic() {
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs; //get current game state (rolled out game state)
        int nPlayers = state.getNPlayers(); //get no.of players in the current game state

        Map<SGCard.SGCardType, Counter>[] played = state.getPlayedCardTypes(); //get the played cards of each player on the table for every turn (as it updates)

        double myRaw = state.getPlayerScore()[playerId].getValue(); //returns the root player's (our MCTS agent's) raw score
        double myPotential = calculateRoundPotential(state, played, playerId); //returns the root player's (our MCTS agent's) potential score

        double[] myMakiRewards = calculateMakiRewards(state, played, playerId, nPlayers); //returns the expected Maki rewards of the highest and second-highest players for the current state
        double myMakiReward = myMakiRewards[0]; //our agent's expected maki reward is always the first element!

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

        return Math.max(-MAX_POSSIBLE, Math.min(MAX_POSSIBLE, scoreDifferential)) / MAX_POSSIBLE;
    }

    /** Calculates the potential score from incomplete Tempura, Sashimi, Dumpling, and Wasabi for a given player. */
    private double calculateRoundPotential(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int player) {
        double roundPotential = 0.0; //Expected Value of completing all sets of each card type, along with wasabi & nigiri combos
        SGParameters params = (SGParameters) state.getGameParameters();

        int tempuraCount = played[player].get(SGCard.SGCardType.Tempura).getValue();
        int sashimiCount = played[player].get(SGCard.SGCardType.Sashimi).getValue();
        int dumplingCount = played[player].get(SGCard.SGCardType.Dumpling).getValue();
        int wasabiCount = played[player].get(SGCard.SGCardType.Wasabi).getValue();
        int eggNigiri = played[player].get(SGCard.SGCardType.EggNigiri).getValue();
        int salmonNigiri = played[player].get(SGCard.SGCardType.SalmonNigiri).getValue();
        int squidNigiri = played[player].get(SGCard.SGCardType.SquidNigiri).getValue();

        //Calculating the Expected Value of completing a full tempura set (Tempura Potential) and adding it to the Round Potential
        int tempuraRemainder = tempuraCount % 2;
        if (tempuraRemainder == 1) { // One away from a pair
            roundPotential += PROB_ONE_CARD_AWAY * params.valueTempuraPair;
        }

        // --- Sashimi Potential ---
        int sashimiRemainder = sashimiCount % 3;
        if (sashimiRemainder == 1) { // Two away from a triple
            roundPotential += PROB_TWO_CARDS_AWAY * params.valueSashimiTriple;
        } else if (sashimiRemainder == 2) { // One away from a triple
            roundPotential += PROB_ONE_CARD_AWAY * params.valueSashimiTriple;
        }

        // --- Dumpling Potential ---
        int[] dumplingVals = params.valueDumpling;
        int currentDIdx = Math.min(dumplingCount, dumplingVals.length - 1);
        int nextDIdx = Math.min(currentDIdx + 1, dumplingVals.length - 1);
        int currentTheoretical = dumplingVals[currentDIdx];
        int theoreticalNext = dumplingVals[nextDIdx];
        int marginal = theoreticalNext - currentTheoretical;
        if (marginal > 0) {
            roundPotential += PROB_ONE_CARD_AWAY * marginal;
        }

        // --- Wasabi + Nigiri Potential ---
        if (wasabiCount > 0) {
            int totalNigiri = eggNigiri + salmonNigiri + squidNigiri;
            double avgNigiriValue = (double)(eggNigiri * params.valueEggNigiri + salmonNigiri * params.valueSalmonNigiri + squidNigiri * params.valueSquidNigiri);
            avgNigiriValue = totalNigiri > 0 ? (avgNigiriValue / totalNigiri) : (params.valueSalmonNigiri); // Fallback to Salmon value

            int wasabiMultiplier = params.multiplierWasabi;
            roundPotential += wasabiCount * (wasabiMultiplier - 1) * avgNigiriValue * PROB_ONE_CARD_AWAY;
        }

        return roundPotential;
    }

    /** Calculates the expected Maki reward for the specified player relative to all others.
     * Returns: [Player's Reward, Max Opponent Reward]
     */
    private double[] calculateMakiRewards(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int playerId, int nPlayers) {
        int[] makiCounts = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            makiCounts[p] = played[p].get(SGCard.SGCardType.Maki).getValue();
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
            puddingCounts[p] = sgState.getPlayedCardTypesAllGame()[p].get(SGCard.SGCardType.Pudding).getValue();
        }

        SGParameters params = (SGParameters) state.getGameParameters();
        int mostScore = params.valuePuddingMost;
        int leastScore = params.valuePuddingLeast;

        final double PROB_FINISH = 0.2; //small expected probability multiplier for non-terminal states

        int maxP = Arrays.stream(puddingCounts).max().orElse(0);
        int minP = Arrays.stream(puddingCounts).min().orElse(0);

        double myPuddingReward = 0.0;
        double maxOppPuddingReward = 0.0;
        double[] allRewards = new double[nPlayers];

        // --- Calculate rewards for ALL players ---
        for (int p = 0; p < nPlayers; p++) {
            double pReward = 0.0;
            //MOST reward
            if (puddingCounts[p] == maxP && maxP > 0) {
                pReward += (double)mostScore / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == maxP).count()) * PROB_FINISH;
            }
            //LEAST penalty
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
}