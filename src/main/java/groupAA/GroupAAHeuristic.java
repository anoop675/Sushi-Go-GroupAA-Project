/*
    Custom Expected Utility Maximization/Estimation (EUM) Heuristic with Opponent Modelling for Greedy Rollout Policy
 */
package groupAA;

import java.util.*;
import core.AbstractGameState;
import core.components.Counter;
import core.components.Deck;
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

        //This is the Expected Utility for our agent in this current state, this heuristic guides the agent to maximize the difference between its score and the best opponent's score.
        double scoreDifferential = myTotalScoreEstimate - maxOpponentScoreEstimate;

        return Math.max(-MAX_POSSIBLE, Math.min(MAX_POSSIBLE, scoreDifferential)) / MAX_POSSIBLE;
    }

    /**
     * Count how many cards of a given SGCardType are currently in the specified player's hand.
     * Uses direct casting to SGCard and checks the public 'type' field (SGCard has no getCardType()).
     */
    private int countInHand(SGGameState state, int player, SGCard.SGCardType type) {
        try {
            List<Deck<SGCard>> hands = state.getPlayerHands();
            if (hands == null || hands.size() <= player) return 0;
            Deck<SGCard> hand = hands.get(player);
            if (hand == null) return 0;
            int cnt = 0;
            // Deck#getComponents() returns a list of the component cards
            for (Object o : hand.getComponents()) {
                if (o instanceof SGCard) {
                    SGCard c = (SGCard) o;
                    if (c.type == type) cnt++;
                }
            }
            return cnt;
        } catch (Throwable t) {
            // Safe fallback
            return 0;
        }
    }


    /** Calculates the potential score from incomplete Tempura, Sashimi, Dumpling, and Wasabi for a given player. */
    private double calculateRoundPotential(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int player) {
        double roundPotential = 0.0; //Expected Value of completing all sets of each card type, along with wasabi & nigiri combos
        SGParameters params = (SGParameters) state.getGameParameters();

        int tempuraCount = played[player].get(SGCard.SGCardType.Tempura).getValue();
        int sashimiCount = played[player].get(SGCard.SGCardType.Sashimi).getValue();
        int dumplingCount = played[player].get(SGCard.SGCardType.Dumpling).getValue();
        int wasabiCount = played[player].get(SGCard.SGCardType.Wasabi).getValue();
        int eggNigiriCount = played[player].get(SGCard.SGCardType.EggNigiri).getValue();
        int salmonNigiriCount = played[player].get(SGCard.SGCardType.SalmonNigiri).getValue();
        int squidNigiriCount = played[player].get(SGCard.SGCardType.SquidNigiri).getValue();
        int chopsticksCount = played[player].get(SGCard.SGCardType.Chopsticks).getValue();

        // >>> FIX A applied below: use hand-aware checks to avoid over-confident Chopsticks assumptions

        //Calculating the Expected Marginal Value of completing a full tempura set (Tempura Potential) and adding it to the Round Potential
        int tempuraRemainder = tempuraCount % 2;
        if (tempuraRemainder == 1) {
            // >>> FIX A: If Chopsticks present, only assume near-certain completion if we actually hold a Tempura in hand.
            int tempInHand = countInHand(state, player, SGCard.SGCardType.Tempura); // >>> FIX A
            double prob;
            if (chopsticksCount > 0) {
                prob = tempInHand > 0 ? 0.95 : PROB_ONE_CARD_AWAY; // >>> FIX A: high certainty only if matching card exists
            } else {
                prob = PROB_ONE_CARD_AWAY;
            }
            roundPotential += prob * params.valueTempuraPair;
        }

        //Calculating the Expected Marginal Value of completing a full sashimi set (Sashimi Potential) and adding it to the Round Potential
        int sashimiRemainder = sashimiCount % 3;
        if (sashimiRemainder == 1) {
            // Need two sashimi to complete. If we have Chopsticks, check hand presence.
            int sashimiInHand = countInHand(state, player, SGCard.SGCardType.Sashimi); // >>> FIX A
            double prob;
            if (chopsticksCount > 0) {
                if (sashimiInHand >= 2) {
                    prob = 0.95; // likely
                } else if (sashimiInHand == 1) {
                    prob = 0.7; // somewhat likely (one in hand + one drawn)
                } else {
                    prob = PROB_TWO_CARDS_AWAY; // fallback
                }
            } else {
                prob = PROB_TWO_CARDS_AWAY;
            }
            roundPotential += prob * params.valueSashimiTriple;
        } else if (sashimiRemainder == 2) {
            // One away from triple
            int sashimiInHand = countInHand(state, player, SGCard.SGCardType.Sashimi); // >>> FIX A
            double prob;
            if (chopsticksCount > 0) {
                prob = sashimiInHand >= 1 ? 0.95 : PROB_ONE_CARD_AWAY; // >>> FIX A
            } else {
                prob = PROB_ONE_CARD_AWAY;
            }
            roundPotential += prob * params.valueSashimiTriple;
        }

        //Calculating the Expected Marginal Value of playing one or two more Dumpling cards (Dumpling Potential) and adding it to the Round Potential
        int[] dumplingVals = params.valueDumpling;
        int currentDIdx = Math.min(dumplingCount, dumplingVals.length - 1);
        int nextDIdx = Math.min(currentDIdx + 1, dumplingVals.length - 1);
        int nextNextDIdx = Math.min(currentDIdx + 2, dumplingVals.length - 1);
        int marginalOne = dumplingVals[nextDIdx] - dumplingVals[currentDIdx];
        int marginalTwo = dumplingVals[nextNextDIdx] - dumplingVals[currentDIdx];

        // >>> FIX A: check dumplings actually present in hand before assuming two-card gain via Chopsticks
        int dumplingsInHand = countInHand(state, player, SGCard.SGCardType.Dumpling); // >>> FIX A
        if (chopsticksCount > 0) {
            if (dumplingsInHand >= 2) {
                roundPotential += marginalTwo * 0.9; // high confidence if 2 dumplings in-hand (>>> FIX A)
            } else if (dumplingsInHand == 1) {
                roundPotential += marginalOne * 0.7; // moderate if only one in-hand (>>> FIX A)
            } else {
                // no dumpling in hand, fallback to single-card-away heuristic (less optimistic)
                if (marginalOne > 0) roundPotential += marginalOne * PROB_ONE_CARD_AWAY;
            }
        } else {
            if (marginalOne > 0) roundPotential += marginalOne * PROB_ONE_CARD_AWAY;
        }

        // --- Wasabi + Nigiri Potential ---
        if (wasabiCount > 0) {
            int totalNigiri = eggNigiriCount + salmonNigiriCount + squidNigiriCount;
            double avgNigiriValue = (double)(eggNigiriCount * params.valueEggNigiri + salmonNigiriCount * params.valueSalmonNigiri + squidNigiriCount * params.valueSquidNigiri);
            avgNigiriValue = totalNigiri > 0 ? (avgNigiriValue / totalNigiri) : (params.valueSalmonNigiri); // Fallback to Salmon value

            int wasabiMultiplier = params.multiplierWasabi;
            // >>> FIX A: only assume a high chance of pairing wasabi with a nigiri if there is at least one nigiri actually in hand
            int nigiriInHand = countInHand(state, player, SGCard.SGCardType.EggNigiri)
                    + countInHand(state, player, SGCard.SGCardType.SalmonNigiri)
                    + countInHand(state, player, SGCard.SGCardType.SquidNigiri); // >>> FIX A

            double prob = (chopsticksCount > 0) ? (nigiriInHand > 0 ? 0.95 : PROB_ONE_CARD_AWAY) : PROB_ONE_CARD_AWAY; // >>> FIX A
            roundPotential += wasabiCount * (wasabiMultiplier - 1) * avgNigiriValue * prob;
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

        int chopsticksCount = played[playerId].get(SGCard.SGCardType.Chopsticks).getValue();
        double chopsticksBoost = 1.0 + 0.2 * chopsticksCount; //small bonus per Chopsticks

        double[] allRewards = new double[nPlayers];

        int max1 = Arrays.stream(makiCounts).max().orElse(0);
        if (max1 > 0) {
            long topCount = Arrays.stream(makiCounts).filter(x -> x == max1).count();
            int max2 = Arrays.stream(makiCounts).filter(x -> x < max1).max().orElse(0);
            long secondCount = Arrays.stream(makiCounts).filter(x -> x == max2).count();

            for (int p = 0; p < nPlayers; p++) {
                if (makiCounts[p] == max1) {
                    allRewards[p] = ((double) mostScore / topCount) * ((p == playerId) ? chopsticksBoost : 1.0);
                } else if (makiCounts[p] == max2 && max2 > 0) {
                    allRewards[p] = ((double) secondScore / secondCount) * ((p == playerId) ? chopsticksBoost : 1.0);
                }
            }
        }

        double myMakiReward = allRewards[playerId];
        double maxOppMakiReward = 0.0;
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

        int chopsticksCount = sgState.getPlayedCardTypes()[playerId].get(SGCard.SGCardType.Chopsticks).getValue();
        double chopsticksBoost = 1.0 + 0.1 * chopsticksCount; //Chopsticks boost

        int maxP = Arrays.stream(puddingCounts).max().orElse(0);
        int minP = Arrays.stream(puddingCounts).min().orElse(0);

        double[] allRewards = new double[nPlayers];

        for (int p = 0; p < nPlayers; p++) {
            double pReward = 0.0;
            if (puddingCounts[p] == maxP && maxP > 0) {
                pReward += ((double) mostScore / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == maxP).count()) * PROB_FINISH);
                if (p == playerId) pReward *= chopsticksBoost;
            }
            if (puddingCounts[p] == minP && nPlayers > 1) {
                pReward -= ((double) leastScore / Math.max(1, Arrays.stream(puddingCounts).filter(x -> x == minP).count()) * PROB_FINISH);
            }
            allRewards[p] = pReward;
        }

        double myPuddingReward = allRewards[playerId];
        double maxOppPuddingReward = 0.0;
        for (int p = 0; p < nPlayers; p++) {
            if (p != playerId) maxOppPuddingReward = Math.max(maxOppPuddingReward, allRewards[p]);
        }

        return new double[]{myPuddingReward, maxOppPuddingReward};
    }
}
