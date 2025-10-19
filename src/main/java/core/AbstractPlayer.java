package core;

import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import players.PlayerParameters;

import java.util.List;
import java.util.Random;

/**
 * Minimal base player implementation to restore compilation.
 * Concrete players should extend this and implement copy() and _getAction(...).
 */
public abstract class AbstractPlayer {

    // Internal ID assigned by Game.reset()
    int playerID = -1;

    protected String name;
    protected AbstractForwardModel forwardModel;
    public PlayerParameters parameters;
    protected Random rnd = new Random();

    // Optional action decorators chain (kept for compatibility)
    protected List<IPlayerDecorator> decorators;

    protected AbstractPlayer(PlayerParameters params, String name) {
        this.parameters = (params != null) ? params : new PlayerParameters();
        this.name = (name != null) ? name : getClass().getSimpleName();
    }

    public final AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions) {
        // Hook for subclasses/observers if needed
        registerUpdatedObservation(observation);
        return _getAction(observation, actions);
    }

    // Subclasses must implement decision logic
    public abstract AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions);

    // Deep copy of the player (including parameters/seed if applicable)
    public abstract AbstractPlayer copy();

    public void initializePlayer(AbstractGameState initialObservation) {
        // Default no-op; subclasses may override
        if (parameters != null && parameters.resetSeedEachGame) {
            rnd = new Random(parameters.getRandomSeed());
        }
    }

    public void registerUpdatedObservation(AbstractGameState observation) {
        // Default no-op; subclasses may override
    }

    public void finalizePlayer(AbstractGameState finalObservation) {
        // Default no-op; subclasses may override
    }

    // Optional event hook used by some agents/listeners; default no-op
    public void onEvent(evaluation.metrics.Event event) { }

    public int getPlayerID() {
        return playerID;
    }

    public void setForwardModel(AbstractForwardModel forwardModel) {
        this.forwardModel = forwardModel;
    }

    public AbstractForwardModel getForwardModel() {
        return forwardModel;
    }

    public PlayerParameters getParameters() {
        if (parameters == null) parameters = new PlayerParameters();
        return parameters;
    }

    public void setParameters(PlayerParameters parameters) {
        this.parameters = parameters;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Accessors used by algorithms
    public Random getRnd() { return rnd; }

    public java.util.Map<core.actions.AbstractAction, java.util.Map<String, Object>> getDecisionStats() { return java.util.Collections.emptyMap(); }

    @Override
    public String toString() {
        return name != null ? name : getClass().getSimpleName();
    }
}
