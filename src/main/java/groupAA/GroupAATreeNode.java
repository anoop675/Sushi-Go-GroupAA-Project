package groupAA;

import java.util.*;
import java.util.logging.Logger;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import static java.util.stream.Collectors.toList;
import static players.PlayerConstants.*;
import core.interfaces.IStateHeuristic;
import static utilities.Utils.noise;


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
    private RandomPlayer randomPlayer = new RandomPlayer();

    private AbstractGameState state; //current state for this current node
    private GroupAARolloutPolicy rolloutPolicy;

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
        this.rolloutPolicy = new GroupAAGreedyRolloutPolicy(player);
        randomPlayer.setForwardModel(player.getForwardModel());
        LOGGER.info("GroupAATreeNode initialized!");
    }

    private AbstractAction ucb() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        GroupAAParams params = player.getParameters();

        LOGGER.info("Performing selection using standard UCB");

        for (AbstractAction action : children.keySet()) {
            GroupAATreeNode child = children.get(action);
            if (child == null)
                throw new AssertionError("Child node should not be null");

            // Average reward of child (guard against divide-by-zero)
            double childValue = child.t / (child.n + params.epsilon);

            // Standard UCB exploration term
            double explorationTerm = params.K * Math.sqrt(Math.log(this.n + 1.0) / (child.n + params.epsilon));

            // UCB value: exploitation + exploration
            double uctValue = childValue + explorationTerm;

            // Small noise to break ties
            uctValue += params.epsilon * player.getRnd().nextDouble();

            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }

        if (bestAction == null)
            throw new AssertionError("No action selected in UCB");

        root.fmCalls++;
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

        LOGGER.info("---------Performing tree policy - selection then expansion----------");
        //keep iterating while the state reached is not terminal and the depth of the tree is not exceeded
        while (currentNode.state.isNotTerminal() && currentNode.depth < player.getParameters().maxTreeDepth) {
            if (!currentNode.unexpandedActions().isEmpty()) { //if this node has actions yet to be expanded
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
        //Random r = new Random(player.getParameters().getRandomSeed());
        //System.out.println("SEED OF OUR PLAYER: " + player.getParameters().getRandomSeed());
        // pick a random unchosen action
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(rand.nextInt(notChosen.size()));

        // copy the current state and advance it using the chosen action
        // we first copy the action so that the one stored in the node will not have any state changes
        AbstractGameState nextState = state.copy();
        advance(nextState, chosen.copy());

        // then instantiate a new node
        GroupAATreeNode tn = new GroupAATreeNode(player, this, nextState, rand);
        LOGGER.info("Expanding node");
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
    private double rollOut() {
        int rolloutDepth = 0; // counting from end of tree

        LOGGER.info("-------Performing rollout policy--------");

        // If rollouts are enabled, select actions for the rollout in line with the rollout policy
        AbstractGameState rolloutState = state.copy();
        if (player.getParameters().rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                List<AbstractAction> availableActions = player.getForwardModel().computeAvailableActions(rolloutState, player.getParameters().actionSpace); //for one simulation-step lookahead
                AbstractAction next;
                if (rolloutPolicy != null) {
                    LOGGER.info("Performing rollout using heuristic");
                    next = rolloutPolicy.chooseAction(rolloutState, availableActions, player.getPlayerID(), rand);
                } else { //if the rolloutPolicy (heuristic rollout policy) is not defined then fallback to random rollout
                    LOGGER.info("Performing random rollout");
                    next = randomPlayer.getAction(rolloutState, randomPlayer.getForwardModel().computeAvailableActions(rolloutState, randomPlayer.parameters.actionSpace));
                }
                if (next == null) break;
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