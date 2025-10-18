/*
  TODO: Define Game Tree structure along with the expansion, selection, simulation (rollout) and backpropagation (update) phases for MCTS
*/
package groupAA;

import java.util.*;
import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import static java.util.stream.Collectors.*;
import static players.PlayerConstants.*;
import static utilities.Utils.noise;

class TreeNode { 
  protected TreeNode(SushiGoAgentGroupAA player, TreeNode parent, AbstractGameState state, Random rand) {
    //TODO
    
  }

  private TreeNode treePolicy() {
    //TODO
    return null;
  }

  private void setState(AbstractGameState newState) {
    //TODO
  }

  private List<AbstractAction> unexpandedActions() {
    //TODO
    return null;
  }
  
  private TreeNode expand() {
    //TODO
    return null;
  }
  
  private void advance(AbstractGameState gs, AbstractAction act) {
    //TODO
  }

  private AbstractAction ucb() 
    //TODO
    return null;
  }

  private double rollOut() {
    //TODO
    return null;
  }

  private boolean finishRollout(AbstractGameState rollerState, int depth) {
    //TODO
    return null;
  }

  private void backUp(double result) {
    //TODO
  }
    
  AbstractAction bestAction() {
    //TODO
    return null;
  }
  
}
