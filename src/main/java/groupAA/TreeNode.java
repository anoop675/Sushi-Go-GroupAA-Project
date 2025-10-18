/*
  MCTS Game Tree structure with expansion, selection, simulation (rollout) and backpropagation (update) phases
*/
package groupAA;

import java.util.*;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

class TreeNode {
    private final SushiGoAgentGroupAA player;
    private final TreeNode parent;
    private final AbstractGameState state;
    private final Random rnd;
    private final Map<AbstractAction, TreeNode> children;
    private final List<AbstractAction> untriedActions;

    private int visits;
    private double totalValue;
    private final int playerID;

    private static final double EXPLORATION_CONSTANT = Math.sqrt(2);

    protected TreeNode(SushiGoAgentGroupAA player, TreeNode parent, AbstractGameState state, Random rand) {
        this.player = player;
        this.parent = parent;
        this.state = state.copy();
        this.rnd = rand;
        this.children = new LinkedHashMap<>();
        this.visits = 0;
        this.totalValue = 0.0;
        this.playerID = state.getCurrentPlayer();

        this.untriedActions = new ArrayList<>(player.getForwardModel()
                .computeAvailableActions(state, state.getCoreGameParameters().actionSpace));
    }

    private TreeNode treePolicy() {
        TreeNode current = this;

        while (!current.isTerminal()) {
            if (current.hasUntriedActions()) {
                return current.expand();
            } else if (current.hasChildren()) {
                current = current.selectBestChild();
            } else {
                return current;
            }
        }
        return current;
    }

    private void setState(AbstractGameState newState) {
        // State is immutable in this implementation - each node has its own copy
    }

    private List<AbstractAction> unexpandedActions() {
        return new ArrayList<>(untriedActions);
    }

    private TreeNode expand() {
        if (untriedActions.isEmpty()) {
            return this;
        }

        AbstractAction action = selectUntriedAction();
        untriedActions.remove(action);

        AbstractGameState nextState = state.copy();
        advance(nextState, action);

        TreeNode childNode = new TreeNode(player, this, nextState, rnd);
        children.put(action, childNode);

        return childNode;
    }

    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act.copy());
    }

    private AbstractAction ucb() {
        return children.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> calculateUCB(entry.getValue())))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("No children available for UCB selection"));
    }

    private double calculateUCB(TreeNode child) {
        if (child.visits == 0) {
            return Double.MAX_VALUE;
        }

        double exploitation = child.totalValue / child.visits;
        double exploration = EXPLORATION_CONSTANT * Math.sqrt(Math.log(this.visits) / child.visits);

        return exploitation + exploration;
    }

    private double rollOut() {
        AbstractGameState rollerState = state.copy();
        RandomPlayer randomPlayer = new RandomPlayer(rnd);
        int depth = 0;

        while (!finishRollout(rollerState, depth)) {
            List<AbstractAction> actions = player.getForwardModel()
                    .computeAvailableActions(rollerState, rollerState.getCoreGameParameters().actionSpace);

            if (actions.isEmpty()) {
                break;
            }

            AbstractAction action = randomPlayer.getAction(rollerState, actions);
            advance(rollerState, action);
            depth++;
        }

        return evaluateState(rollerState);
    }

    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        int maxDepth = ((AMAF_Params) player.getParameters()).getRolloutDepth();
        return !rollerState.isNotTerminal() || depth >= maxDepth;
    }

    private double evaluateState(AbstractGameState rollerState) {
        if (!rollerState.isNotTerminal()) {
            return rollerState.getPlayerResults()[playerID] == CoreConstants.GameResult.WIN_GAME ? 1.0 : 0.0;
        }
        return rollerState.getGameScore(playerID);
    }

    private void backUp(double result) {
        TreeNode current = this;

        while (current != null) {
            current.visits++;
            current.totalValue += result;
            current = current.parent;
        }
    }

    AbstractAction bestAction() {
        return children.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().visits))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean isTerminal() {
        return !state.isNotTerminal();
    }

    private boolean hasUntriedActions() {
        return !untriedActions.isEmpty();
    }

    private boolean hasChildren() {
        return !children.isEmpty();
    }

    private AbstractAction selectUntriedAction() {
        return untriedActions.get(rnd.nextInt(untriedActions.size()));
    }

    private TreeNode selectBestChild() {
        AbstractAction bestAction = ucb();
        return children.get(bestAction);
    }

    public void runMCTS(int iterations) {
        for (int i = 0; i < iterations; i++) {
            TreeNode selected = treePolicy();
            double result = selected.rollOut();
            selected.backUp(result);
        }
    }
}
