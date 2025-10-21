package groupAA;

import java.util.*;
import java.util.logging.Logger;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;
//import players.basicMCTS.BasicTreeNode;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import static java.util.stream.Collectors.toList;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;
//import players.basicMCTS.BasicMCTSPlayer;
//import players.basicMCTS.BasicTreeNode;

class GroupAATreeNode {

    private static final Logger LOGGER = Logger.getLogger(GroupAATreeNode.class.getName());

    GroupAATreeNode root; //root node of the tree
    GroupAATreeNode parent; //parent of the current node
    Map<AbstractAction, GroupAATreeNode> children = new HashMap<>(); //children of current node
    final int depth; //depth of current node
    private double t; //total value of this node
    private int n; //no.of times current node is visited
    private int fmCalls; //no.of Forward Model calls and state copies up until current node

    private SushiGoAgentGroupAA player;
    private Random rand;
    private RandomPlayer randomPlayer = new RandomPlayer(); //TODO: Use a heuristic instead of random rollouts

    private AbstractGameState state; //current state for this current node

    protected GroupAATreeNode(SushiGoAgentGroupAA player, GroupAATreeNode  parent,
                              AbstractGameState state, Random rand) {
        this.player = player;
        this.root = parent == null ? this : parent.root;
        this.parent = parent;
        depth = parent != null ? parent.depth + 1 : 0;
        this.fmCalls = 0;
        t = 0.0; //init total value of this node as 0
        setState(state); //setting current state
        this.rand = rand;
        randomPlayer.setForwardModel(player.getForwardModel());
        LOGGER.info("GroupAATreeNode initialized!");
    }

    private AbstractAction ucb() {
        // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
        AbstractAction bestAction = null;
        double bestValue = Double.MAX_VALUE;
        PlayerParameters params = player.getParameters();

        LOGGER.info("Performing selection using UCB");

        for (AbstractAction action : children.keySet()) {
            GroupAATreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("Should not be here");
            else if (bestAction == null)
                bestAction = action;

            // Find child value
            double hvVal = child.t;
            double childValue = hvVal / (child.n + params.epsilon);

            // default to standard UCB
            double explorationTerm = params.K * Math.sqrt(Math.log(this.n + 1) / (child.n + params.epsilon));
            // unless we are using a variant

            // Find 'UCB' value
            // If 'we' are taking a turn we use classic UCB
            // If it is an opponent's turn, then we assume they are trying to minimise our score (with exploration)
            boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
            double uctValue = iAmMoving ? childValue : -childValue;
            uctValue += explorationTerm;

            // Apply small noise to break ties randomly
            uctValue = noise(uctValue,params.epsilon, player.getRnd().nextDouble());

            // Assign value
            if (uctValue > bestValue) {
                bestAction = action;
                LOGGER.info("Selecting best action: " + bestAction);
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

        root.fmCalls++;  // log one iteration complete
        return bestAction;
    }

    void mctsSearch() {
        PlayerParameters params = player.getParameters();

        // Variables for tracking time budget
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int remainingLimit = params.breakMS;
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        // Tracking number of iterations for iteration budget
        int numIters = 0;

        boolean stop = false;

        LOGGER.info("SushiGoAgentGroupAA performing search iteration: " + numIters+1);
        while (!stop) {
            // New timer for this iteration
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // Selection + expansion: navigate tree until a node not fully expanded is found, add a new node to the tree
            GroupAATreeNode selected = treePolicy();
            // Monte carlo rollout: return value of MC rollout from the newly added node
            double delta = selected.rollOut();
            // Back up the value of the rollout through the tree
            selected.backUp(delta);
            // Finished iteration
            numIters++;

            // Check stopping condition
            PlayerConstants budgetType = params.budgetType;
            if (budgetType == BUDGET_TIME) {
                // Time budget
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            } else if (budgetType == BUDGET_ITERATIONS) {
                // Iteration budget
                stop = numIters >= params.budget;
            } else if (budgetType == BUDGET_FM_CALLS) {
                // FM calls budget
                stop = fmCalls > params.budget;
            }
        }
    }

    private GroupAATreeNode treePolicy() {
        GroupAATreeNode currentNode = this;

        LOGGER.info("Executing rollout policy");
        //keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (currentNode.state.isNotTerminal() && currentNode.depth < player.getParameters().maxTreeDepth) {
            if (!currentNode.unexpandedActions().isEmpty()) {
                // We have an unexpanded action
                currentNode = currentNode.expand();
                return currentNode;
            } else {
                // Move to next child given by UCT function
                AbstractAction actionChosen = currentNode.ucb();
                currentNode = currentNode.children.get(actionChosen);
            }
        }
        return currentNode;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal())
            for (AbstractAction action : player.getForwardModel().computeAvailableActions(state, player.getParameters().actionSpace)) {
                children.put(action, null); // mark a new node to be expanded
            }
    }

    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    //Expands the node by creating a new random child node and adding to the tree.
    private GroupAATreeNode expand() {
        // Find random child not already created
        Random r = new Random(player.getParameters().getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(r.nextInt(notChosen.size()));

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();
        advance(nextState, chosen.copy());

        // then instantiate a new node
        GroupAATreeNode tn = new GroupAATreeNode(player, this, nextState, rand);
        children.put(chosen, tn);
        return tn;
    }

    /**
     * Advance the current game state with the given action, count the FM call and compute the next available actions.
     *
     * @param gs  - current game state
     * @param act - action to apply
     */
    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCalls++;
    }

    //Performs the rollout phase in MCTS
    //TODO: Use heuristic field in AMAF_Params, in this function
    private double rollOut() {
        int rolloutDepth = 0; // counting from end of tree

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        if (player.getParameters().rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                //TODO: Use a heuristic rollout policy instead of random rollouts
                AbstractAction next = randomPlayer.getAction(rolloutState, randomPlayer.getForwardModel().computeAvailableActions(rolloutState, randomPlayer.parameters.actionSpace));
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }
        // Evaluate final state and return normalised score
        double value = player.getParameters().getStateHeuristic().evaluateState(rolloutState, player.getPlayerID());
        if (Double.isNaN(value))
            throw new AssertionError("Illegal heuristic value - should be a number");
        return value;
    }

    //Checks if rollout is finished. Rollouts end on maximum length, or if game ended.
    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        if (depth >= player.getParameters().rolloutLength)
            return true;

        // End of game
        return !rollerState.isNotTerminal();
    }

    private void backUp(double result) {
        GroupAATreeNode currentNode = this;
        while (currentNode != null) {
            currentNode.n++;
            currentNode.t += result;
            currentNode = currentNode.parent;
        }
    }

    //Calculates the best action from the root according to the most visited node
    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                GroupAATreeNode childNode = children.get(action);
                double childValue = childNode.n;

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.getParameters().epsilon, player.getRnd().nextDouble());

                // Save best value (highest visit count)
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
        }

        if (bestAction == null) {
            throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

}
