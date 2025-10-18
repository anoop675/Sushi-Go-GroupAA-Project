package gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractGUIManager {
    protected Game game;
    protected final GamePanel parent;
    protected final ActionController ac;
    protected final Set<Integer> humanPlayers;
    protected final Set<Integer> humanPlayerIds; // alias used by some GUIs

    // Common dimensions and defaults
    public static int defaultItemSize = 50;
    public static int defaultActionPanelHeight = 100;
    public static int defaultInfoPanelHeight = 180;
    public static int defaultDisplayWidth = 800;
    public static int defaultDisplayHeight = 600;
    public static int defaultCardHeight = 50;

    // Common UI elements exposed to subclasses
    protected JLabel gameStatus = new JLabel();
    protected JLabel playerStatus = new JLabel();
    protected JLabel playerScores = new JLabel();
    protected JLabel gamePhase = new JLabel();
    protected JLabel turn = new JLabel();
    protected JLabel currentPlayer = new JLabel();
    protected JTextArea historyInfo = new JTextArea();
    protected JScrollPane historyContainer;

    // Action button bank
    protected ActionButton[] actionButtons;
    protected int maxActionSpace = 50;

    // Canvas size some GUIs expect to set
    protected int width = defaultDisplayWidth;
    protected int height = 600;

    protected AbstractGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanPlayers) {
        this.parent = parent;
        this.game = game;
        this.ac = ac;
        this.humanPlayers = humanPlayers;
        this.humanPlayerIds = humanPlayers;
        this.maxActionSpace = getMaxActionSpace();

        // default history widgets
        historyInfo.setEditable(false);
        historyInfo.setLineWrap(true);
        historyInfo.setWrapStyleWord(true);
        historyContainer = new JScrollPane(historyInfo);
    }

    public void update(AbstractPlayer player, AbstractGameState gameState) {
        _update(player, gameState);
        parent.repaint();
    }

    // Some frontends call an overload with a boolean flag; ignore it and delegate
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean unused) {
        update(player, gameState);
    }

    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

    public int getMaxActionSpace() { return 50; }

    protected JPanel createGameStateInfoPanel(String title, AbstractGameState state, int width, int height) {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(new JLabel("<html><h1>" + title + "</h1></html>"));
        updateGameStateInfo(state);
        left.add(gameStatus);
        left.add(playerStatus);
    left.add(playerScores);
        left.add(gamePhase);
        left.add(turn);
        left.add(currentPlayer);
        left.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(left);

        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.setPreferredSize(new Dimension(width / 2 - 10, height));
        wrapper.add(historyContainer);

        return wrapper;
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState state) {
        String title = state == null || state.getGameType() == null ? "Game" : state.getGameType().toString();
        return createGameStateInfoPanel(title, state, 900, defaultInfoPanelHeight);
    }

    protected void updateGameStateInfo(AbstractGameState state) {
        if (state == null) return;
        gameStatus.setText("Status: " + state.getGameStatus());
        gamePhase.setText("Phase: " + (state.getGamePhase() == null ? "-" : state.getGamePhase().toString()));
        turn.setText("Turn: " + state.getTurnCounter());
        currentPlayer.setText("Current Player: " + state.getCurrentPlayer());
    }

    // Convenience overloads used across many GUI managers
    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height) {
        return createActionPanel(highlights, width, height, false, false, true, null, null, null);
    }

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean boxLayout) {
        return createActionPanel(highlights, width, height, boxLayout, false, true, null, null, null);
    }

    // Alias expected by some GUIs
    protected JComponent createActionPanelOpaque(IScreenHighlight[] highlights, int width, int height, boolean boxLayout) {
        return createActionPanel(highlights, width, height, boxLayout, true, true, null, null, null);
    }

    // Overload used where only a selection callback is needed
    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height,
                                           java.util.function.Consumer<ActionButton> onActionSelected) {
        return createActionPanel(highlights, width, height, false, false, true, onActionSelected, null, null);
    }

    protected JComponent createActionPanel(
            IScreenHighlight[] highlights,
            int width,
            int height,
            boolean boxLayout,
            boolean opaque,
            java.util.function.Consumer<ActionButton> onActionSelected,
            java.util.function.Consumer<ActionButton> onMouseEnter,
            java.util.function.Consumer<ActionButton> onMouseExit
    ) {
        return createActionPanel(highlights, width, height, boxLayout, opaque, true, onActionSelected, onMouseEnter, onMouseExit);
    }

    // Generic createActionPanel used by many GUIs
    protected JComponent createActionPanel(
            IScreenHighlight[] highlights,
            int width,
            int height,
            boolean verticalLayout,
            boolean opaque,
            boolean scrollable,
            Consumer<ActionButton> onActionSelected,
            Consumer<ActionButton> onMouseEnter,
            Consumer<ActionButton> onMouseExit
    ) {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(opaque);
        actionPanel.setLayout(new BoxLayout(actionPanel, verticalLayout ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights, onActionSelected, onMouseEnter, onMouseExit);
            ab.setVisible(false);
            actionButtons[i] = ab;
            actionPanel.add(ab);
        }
        for (ActionButton ab : actionButtons) ab.informAllActionButtons(actionButtons);

        if (scrollable) {
            JScrollPane scrollPane = new JScrollPane(actionPanel);
            scrollPane.setPreferredSize(new Dimension(width, height));
            scrollPane.setMinimumSize(new Dimension(width, height));
            scrollPane.setOpaque(opaque);
            scrollPane.getViewport().setOpaque(opaque);
            scrollPane.setHorizontalScrollBarPolicy(verticalLayout ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(verticalLayout ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            return scrollPane;
        }
        actionPanel.setPreferredSize(new Dimension(width, height));
        return actionPanel;
    }

    // Default action buttons refresh that many GUIs call
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState == null || actionButtons == null) return;
        java.util.List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState, player.getParameters().actionSpace);
        int i = 0;
        for (; i < actions.size() && i < actionButtons.length; i++) {
            actionButtons[i].setVisible(true);
            actionButtons[i].setEnabled(true);
            actionButtons[i].setButtonAction(actions.get(i), gameState);
            actionButtons[i].setBackground(Color.white);
            actionButtons[i].setForeground(Color.BLACK);
        }
        for (; i < actionButtons.length; i++) {
            actionButtons[i].setVisible(false);
            actionButtons[i].setButtonAction(null, "");
        }
    }

    protected void resetActionButtons() {
        if (actionButtons == null) return;
        for (ActionButton ab : actionButtons) {
            ab.setVisible(false);
            ab.setButtonAction(null, "");
        }
    }

    public Set<Integer> getHumanPlayerIds() { return humanPlayerIds; }

    // Some GUIs expect to build an action history panel
    protected JPanel createActionHistoryPanel(int width, int height, Set<Integer> humanPlayers) {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(width, height));
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.add(sp);
        return p;
    }

    protected JPanel createActionHistoryPanel(int width, int height) {
        return createActionHistoryPanel(width, height, this.humanPlayers);
    }

    // Expose ActionController for some GUI components
    public ActionController getAC() { return ac; }

    // Inherited nested action button, available by simple name in subclasses
    public static class ActionButton extends JButton {
        private core.actions.AbstractAction buttonAction;

        public ActionButton(Object ac, IScreenHighlight[] highlights,
                            java.util.function.Consumer<ActionButton> onActionSelected,
                            java.util.function.Consumer<ActionButton> onMouseEnter,
                            java.util.function.Consumer<ActionButton> onMouseExit) {
            super();
            setPreferredSize(new Dimension(120, 28));
            addActionListener(e -> { if (onActionSelected != null) onActionSelected.accept(this); });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (onMouseEnter != null) onMouseEnter.accept(ActionButton.this); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { if (onMouseExit != null) onMouseExit.accept(ActionButton.this); }
            });
        }

        public ActionButton(Object ac, IScreenHighlight[] highlights) { this(ac, highlights, null, null, null); }

        public void setButtonAction(core.actions.AbstractAction action, AbstractGameState state) {
            setButtonAction(action, action == null ? "" : action.getString(state));
        }

        public void setButtonAction(core.actions.AbstractAction action, String label) {
            this.buttonAction = action;
            setText(label);
            setEnabled(action != null);
            setVisible(action != null);
        }

    public void informAllActionButtons(ActionButton[] all) { /* no-op placeholder for compatibility */ }

        public core.actions.AbstractAction getButtonAction() { return buttonAction; }
    }
}
