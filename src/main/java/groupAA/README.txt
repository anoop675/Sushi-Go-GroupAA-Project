Description about the proposed heuristic for the rollout policy

Purpose of a Heuristic in MCTS:
* Each node (game state) needs to be evaluated to decide which branch (action) to explore.
* Classical MCTS, uses random rollouts to terminal states to estimate value (reward).
  But for a complex state-action tree representation (if real-world problem/game is complex), with long horizon/branching factor (high branching factor = many stochastic outcomes in the real world)
    -> full rollouts are expensive! (needs to fully rollout for each node and its siblings)
    -> full rollouts can be noisy (random plays don’t reflect real player strategies)
* So, instead of rolling out to the end, using a heuristic evaluation function h(s) (a way to approximate the expected value of a non-terminal state s)
  Note: expected value of a state - The expected long-term return (reward) you’d get if you started from that node and played optimally (or according to your current policy) until the end.
        [provided the heuristic function is admissible - returns an estimate value that does not overestimate the expected value (reward)) of subsequent states]

  If you roll out to the terminal state (end of the game), you can compute the actual return (win/loss, score difference, etc.).
  That’s the true value of the state — because you’ve simulated the complete future.

Theoretical significance
*
