/*
    Custom Expected Utility Maximization/Estimation (EUM) Heuristic with Opponent Modelling for Greedy Rollout Policy
 */
package groupAA;

import java.util.*;
import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import games.sushigo.cards.SGCard;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import core.components.Deck;

public class GroupAAHeuristic implements IStateHeuristic {

    private static final double MAX_POSSIBLE = 40.0; //maximum achievable score (score difference) that a player could get in a round or in the entire game, just an approximation to clamp the heuristic (utility) value within [-1,1]
    private static final double PROB_ONE_CARD_AWAY = 0.5; //base probability for completing a set when one card away (can be tuned)
    private static final double PROB_TWO_CARDS_AWAY = 0.1; //base probability for completing a set when two cards away
    private static final double MIN_HAND_SIZE_FOR_CHOPSTICKS = 3.0; // Minimum cards left in hand for Chopsticks to be likely usable

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
        double myPuddingReward = myPuddingRewards[0]; //our agent's expected reward is always the first element!

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
        //Using hyperbolic tangent to map scoreDifferential within range [-1, 1] instead of clamping for efficient smoothing and signal preservation when scoreDifferentials are high
        //return Math.tanh(scoreDifferential / (MAX_POSSIBLE / 2.0)); //the maximum achievable score is the scaling factor
    }

    /**
     * Helper method to count a specific card type in the player's current hand.
     * @param hand The player's hand (Deck).
     * @param type The card type to count.
     * @return The number of cards of that type in the hand.
     */
    private int countCardTypeInHand(Deck<SGCard> hand, SGCard.SGCardType type) {
        int count = 0;
        if (hand == null) return 0;
        for (SGCard card : hand.getComponents()) {
            if (card.type == type) {
                count++;
            }
        }
        return count;
    }

    /** Calculates the potential score from incomplete Tempura, Sashimi, Dumpling, and Wasabi for a given player. */
    private double calculateRoundPotential(SGGameState state, Map<SGCard.SGCardType, Counter>[] played, int player) {
        // Access the player's current hand
        List<Deck<SGCard>> hands = state.getPlayerHands();
        Deck<SGCard> myHand = hands.get(player);

        // Count necessary cards in hand
        int handTempura = countCardTypeInHand(myHand, SGCard.SGCardType.Tempura);
        int handSashimi = countCardTypeInHand(myHand, SGCard.SGCardType.Sashimi);
        int handDumpling = countCardTypeInHand(myHand, SGCard.SGCardType.Dumpling);
        int handChopsticks = countCardTypeInHand(myHand, SGCard.SGCardType.Chopsticks);

        // Determine if Chopsticks is available to play (i.e., not just in the "played" area)
        boolean chopsticksAvailable = handChopsticks >= 1 && myHand.getSize() >= 2;

        double roundPotential = 0.0; //Expected Value of completing all sets of each card type, along with wasabi & nigiri combos
        SGParameters params = (SGParameters) state.getGameParameters();

        int tempuraCount = played[player].get(SGCard.SGCardType.Tempura).getValue();
        int sashimiCount = played[player].get(SGCard.SGCardType.Sashimi).getValue();
        int dumplingCount = played[player].get(SGCard.SGCardType.Dumpling).getValue();
        int wasabiCount = played[player].get(SGCard.SGCardType.Wasabi).getValue();
        int eggNigiriCount = played[player].get(SGCard.SGCardType.EggNigiri).getValue();
        int salmonNigiriCount = played[player].get(SGCard.SGCardType.SalmonNigiri).getValue();
        int squidNigiriCount = played[player].get(SGCard.SGCardType.SquidNigiri).getValue();
        int chopsticksCount = played[player].get(SGCard.SGCardType.Chopsticks).getValue(); // Count of played chopsticks (for set completion)

        // --- Tempura Potential ---
        int tempuraRemainder = tempuraCount % 2;
        if (tempuraRemainder == 1) { // One away from a pair
            double prob = PROB_ONE_CARD_AWAY;

            // Hand Check: Can we complete the pair with the card in hand this turn?
            if (handTempura >= 1) {
                prob = 1.0;
            }
            // Hand Check: Can we use Chopsticks to play 2 Tempura and complete the pair? (Need 2 Tempura in hand).
            else if (chopsticksAvailable && handTempura >= 2) {
                // If we play 2 cards, one is a Tempura to complete the pair, the second is the Chopsticks (used to play the first Tempura and the second Tempura card)
                // This scenario means the player has 1 Tempura played and 2 Tempura in hand
                prob = 1.0;
            }
            roundPotential += prob * params.valueTempuraPair;
        }

        // --- Sashimi Potential ---
        int sashimiRemainder = sashimiCount % 3;
        if (sashimiRemainder == 1) { // Two away from a triple
            double prob = PROB_TWO_CARDS_AWAY;

            // Hand Check: Can we complete it this turn with Chopsticks? (Need 2 Sashimi in hand).
            if (chopsticksAvailable && handSashimi >= 2) {
                prob = 1.0;
            }
            roundPotential += prob * params.valueSashimiTriple;
        } else if (sashimiRemainder == 2) { // One away from a triple
            double prob = PROB_ONE_CARD_AWAY;

            // Hand Check: Can we complete it with the card in hand this turn?
            if (handSashimi >= 1) {
                prob = 1.0;
            }
            roundPotential += prob * params.valueSashimiTriple;
        }

        // --- Dumpling Potential ---
        // Calculating the Expected Marginal Value of playing one more Dumpling card (Dumpling Potential)
        int[] dumplingVals = params.valueDumpling; //retrieves the scoring table for Dumplings.
        int currentDIdx = Math.min(dumplingCount, dumplingVals.length - 1);
        int nextDIdx = Math.min(currentDIdx + 1, dumplingVals.length - 1);
        int currentTheoretical = dumplingVals[currentDIdx];
        int theoreticalNext = dumplingVals[nextDIdx];
        int marginal = theoreticalNext - currentTheoretical;

        if (marginal > 0) {
            double prob = PROB_ONE_CARD_AWAY;
            // Hand Check: Is a Dumpling in hand?
            if (handDumpling >= 1) {
                prob = 1.0;
            }
            roundPotential += prob * marginal;
        }

        // --- Wasabi + Nigiri Potential ---
        if (wasabiCount > 0) {
            int totalNigiri = eggNigiriCount + salmonNigiriCount + squidNigiriCount;
            double avgNigiriValue = (double)(eggNigiriCount * params.valueEggNigiri + salmonNigiriCount * params.valueSalmonNigiri + squidNigiriCount * params.valueSquidNigiri);
            avgNigiriValue = totalNigiri > 0 ? (avgNigiriValue / totalNigiri) : (params.valueSalmonNigiri); // Fallback to Salmon value

            int wasabiMultiplier = params.multiplierWasabi;

            // Hand Check is more complex here: we need to estimate the value of getting a Nigiri.
            // Since we can't fully model the two-card play, we keep it simple:
            // Use PROB_ONE_CARD_AWAY for getting a Nigiri to pair with Wasabi.
            roundPotential += wasabiCount * (wasabiMultiplier - 1) * avgNigiriValue * PROB_ONE_CARD_AWAY;
        }

        // --- Chopsticks Bonus ---
        // Explicitly modeling the best second card (extra action) if Chopsticks is available.
        if (chopsticksAvailable) {
            // Assign the expected value of a high-scoring card (Salmon Nigiri) as the bonus for the extra move.
            // This replaces the old 'cardMultiplier' approximation.
            // We use a certainty factor (1.0) if the hand has any nigiri cards to pair

            int handNigiri = countCardTypeInHand(myHand, SGCard.SGCardType.EggNigiri) +
                    countCardTypeInHand(myHand, SGCard.SGCardType.SalmonNigiri) +
                    countCardTypeInHand(myHand, SGCard.SGCardType.SquidNigiri);

            double bonusValue = params.valueSquidNigiri; // Highest Nigiri value as a proxy for 'best second card'
            double prob = (handNigiri >= 1) ? 1.0 : PROB_ONE_CARD_AWAY;

            roundPotential += prob * bonusValue;
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

        final double PROB_FINISH; //small expected probability multiplier for non-terminal states

        if (sgState.getRoundCounter() == 1)
            PROB_FINISH = 0.5;

        else if (sgState.getRoundCounter() == 2)
            PROB_FINISH = 0.75;

        else PROB_FINISH = 1.0;

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