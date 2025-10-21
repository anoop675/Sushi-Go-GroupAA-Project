package core.actions;

import core.AbstractGameState;
import games.loveletter.LoveLetterForwardModel;

/**
 * <p>See {@link LoveLetterForwardModel#_computeAvailableActions(AbstractGameState, ActionSpace)}
 * for example implementations of structured action spaces.</p>
 *
 * <p>These are used within the forward model to compute the available actions for a given game state.
 * Each call to this function can request different lists of actions for different types of action spaces.
 * Ideally, the same action space should be kept throughout a game (otherwise deep action spaces might not receive the
 * correct sequence of decisions required).</p>
 *
 * @param flexibility TODO: no agents to take advantage of this yet, not supported in any games
 */
public record ActionSpace(Structure structure, Flexibility flexibility, Context context) {
    public static ActionSpace Default = new ActionSpace();

    public final boolean isDefault() {
        return this.equals(Default);
    }

    public ActionSpace() {
        this(Structure.Default, Flexibility.Default, Context.Default);
    }

    public ActionSpace(Structure structure) {
        this(structure, Flexibility.Default, Context.Default);
    }

    public ActionSpace(Flexibility flexibility) {
        this(Structure.Default, flexibility, Context.Default);
    }

    public ActionSpace(Context context) {
        this(Structure.Default, Flexibility.Default, context);
    }

    /**
     * Defines the structure of the action space. Current options supported in some games:
     * <ul>
     *     <li>Default: whichever option the game considers by default, usually 'Flat'. Used when no other structure specified.</li>
     *     <li>Flat: combinatorial list of full actions, considering all combinations of parameter values.</li>
     *     <li>Deep: Actions with multiple parameters/decisions required are split into several decisions.</li>
     * </ul>
     */
    public enum Structure {
        Default,
        Flat,
        Deep
    }

    /**
     * Defines the flexibility of the action space. The options are currently NOT supported in any of the games.
     * <ul>
     *     <li>Default: whichever option the game considers by default, usually 'Rigid'. Used when no other flexibility specified.</li>
     *     <li>Rigid: if multiple decisions need to be taken for an action, the order for these decisions is fixed.</li>
     *     <li>Elastic: if multiple decisions need to be taken for an action, the agent has an additional choice to make, for
     *     the order in which the decisions will be taken. Useful for e.g. decision trees to maximise use of information gain.</li>
     * </ul>
     */
    public enum Flexibility {
        Default,
        Rigid,
        Elastic
    }

    /**
     * Defines the context of the action space. Current options supported in some games:
     * <ul>
     *     <li>Default: whichever option the game considers by default, usually 'Independent'. Used when no other context specified.</li>
     *     <li>Independent: the actions are self-contained and have the same effect in any game state they are applied to (e.g. play card 'King').</li>
     *     <li>Dependent: actions require context from the game state, and effect will differ (e.g. play 3rd card in hand).</li>
     * </ul>
     */
    public enum Context {
        Default,
        Independent,
        Dependent
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionSpace that)) return false;
        return structure == that.structure && flexibility == that.flexibility && context == that.context;
    }

}
