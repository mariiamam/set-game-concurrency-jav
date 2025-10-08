package bguspl.set.ex;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import bguspl.set.Env;

/**
 * Manages a single player's state and threads.
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    // ----------------------
    // Runtime state flags
    // ----------------------
    private boolean theDealerIsRemoving; // when dealer is removing cards, player should not keypress
    private boolean foundset;            // true once player has placed 3 tokens (candidate set)
    private boolean badToken;            // marks that a bad selection happened (after penalty)
    private boolean inFreeze;            // player is frozen (point/penalty)
    private volatile boolean terminate;  // termination signal

    // ----------------------
    // Player token state
    // ----------------------
    private final int[] playerSlots;          // tracked slots selected by this player (-1 = empty)
    private int tokensCounter;                // number of active tokens for this player (0..3)
    private final BlockingQueue<Integer> queue; // keypress queue (capacity 3)

    // ----------------------
    // Game context
    // ----------------------
    private final Env env;
    private final Table table;
    private final Dealer dealer;

    // ----------------------
    // Identity & threading
    // ----------------------
    public final int id;
    private final boolean human;
    private Thread playerThread;
    private Thread aiThread;

    // ----------------------
    // Score
    // ----------------------
    private int score;

    /**
     * Constructor.
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;

        this.theDealerIsRemoving = false;
        this.foundset = false;
        this.badToken = false;
        this.inFreeze = false;
        this.terminate = false;

        this.playerSlots = new int[3];
        for (int i = 0; i < 3; i++) playerSlots[i] = -1;

        this.queue = new LinkedBlockingDeque<>(3);
        this.tokensCounter = 0;
        this.score = 0;
    }

    /**
     * Main player loop.
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + playerThread.getName() + " starting.");

        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // Once player has 3 tokens and is not frozen, handle set check path
            if (tokensCounter == 3 && !inFreeze && !badToken) {
                if (dealer.setFound) {
                    // If another player's set is being processed, wait it out
                    if (!dealer.ifCommon(id)) {
                        while (dealer.setFound) {
                            if (terminate) break;
                            // busy-wait as in original logic
                        }
                        if (terminate) break;

                        if (dealer.checkIfSet(id)) {
                            queue.clear();
                            point();
                        } else {
                            penalty();
                            if (!human) {
                                // remove a random token (same behavior as your code)
                                int randomIdx = (int) (Math.random() * playerSlots.length);
                                table.removeToken(id, playerSlots[randomIdx]);
                                removePlayerToken(playerSlots[randomIdx]);
                            }
                        }
                    }
                } else {
                    if (dealer.checkIfSet(id)) {
                        point();
                        queue.clear();
                    } else {
                        penalty();
                        if (!human) {
                            int randomIdx = (int) (Math.random() * playerSlots.length);
                            table.removeToken(id, playerSlots[randomIdx]);
                            removePlayerToken(playerSlots[randomIdx]);
                        }
                    }
                }
            }

            foundset = false;

            // Wake AI so it can continue after point/penalty
            if (!human) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }

        if (!human) {
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {}
        }

        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * AI thread that simulates key presses for computer players.
     */
    private void createArtificialIntelligence() {
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // generate keys while fewer than 3 tokens
                while (!terminate && tokensCounter < 3) {
                    int randomSlot = (int) (Math.random() * env.config.tableSize);
                    keyPressed(randomSlot);
                }

                try {
                    synchronized (this) {
                        wait(); // wait for point/penalty handling to finish
                    }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Request player termination.
     */
    public void terminate() {
        terminate = true;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Called when a key (slot) is pressed for this player.
     */
    public void keyPressed(int slot) {
        // Bounds & state checks
        if (slot < 0 || slot >= env.config.tableSize) return;

        if (table.slotToCard[slot] != null && !inFreeze && !foundset && !theDealerIsRemoving) {
            if (tokensCounter >= 0 && tokensCounter < 3) {
                if (checkIfTokened(slot)) {
                    table.removeToken(id, slot);
                    removePlayerToken(slot);
                } else {
                    if (queue.size() < 3) {
                        table.placeToken(id, slot);
                        placePlayerToken(slot);
                        badToken = false;
                    }
                }
            } else { // tokensCounter == 3
                if (checkIfTokened(slot)) {
                    table.removeToken(id, slot);
                    removePlayerToken(slot);
                }
            }
        }
    }

    /**
     * Build a card array matching the currently selected player slots.
     */
    public int[] returnArray() {
        int[] cards = new int[3];
        for (int i = 0; i < playerSlots.length; i++) {
            if (playerSlots[i] != -1 && table.slotToCard[playerSlots[i]] != null)
                cards[i] = table.slotToCard[playerSlots[i]];
        }
        return cards;
    }

    /**
     * @return true if player has a token on the given slot.
     */
    public boolean checkIfTokened(int slot) {
        for (int s : playerSlots) {
            if (s != -1 && s == slot) return true;
        }
        return false;
    }

    /**
     * Remove a token from player state and queue.
     */
    public void removePlayerToken(int slot) {
        for (int i = 0; i < playerSlots.length; i++) {
            if (playerSlots[i] == slot && tokensCounter > 0) {
                playerSlots[i] = -1;
                tokensCounter--;
                queue.remove(slot);
            }
        }
    }

    /**
     * Remove all player tokens (local state only).
     */
    public void removeAllPlayreTokens() {
        for (int i = 0; i < playerSlots.length; i++) {
            if (playerSlots[i] != -1) {
                playerSlots[i] = -1;
                tokensCounter--;
            }
        }
    }

    /**
     * Place a player token in local state & queue.
     */
    public void placePlayerToken(int slot) {
        boolean placed = false;
        for (int i = 0; i < playerSlots.length && !placed; i++) {
            if (playerSlots[i] == -1) {
                playerSlots[i] = slot;
                placed = true;
                tokensCounter++;
                queue.add(slot);
            }
        }
        if (tokensCounter == 3) foundset = true;
    }

    /**
     * Award a point and freeze player according to config.
     * @post score increases by 1 and UI updated.
     */
    public void point() {
        inFreeze = true;
        score++;
        long freeze = env.config.pointFreezeMillis;
        env.ui.setScore(id, score);

        while (freeze > 0) {
            synchronized (Thread.currentThread()) {
                env.ui.setFreeze(id, freeze);
                if (freeze < 1000) {
                    try { Thread.sleep(freeze); } catch (InterruptedException ignored) {}
                    freeze = 0;
                } else {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    freeze -= 1000;
                }
            }
        }

        env.ui.setFreeze(id, 0);
        int ignored = table.countCards(); // as in original code (for unit tests)
        inFreeze = false;
    }

    /**
     * Apply penalty and freeze player according to config.
     */
    public void penalty() {
        inFreeze = true;
        badToken = true;
        long freeze = env.config.penaltyFreezeMillis;

        while (freeze > 0) {
            synchronized (Thread.currentThread()) {
                env.ui.setFreeze(id, freeze);
                if (freeze < 1000) {
                    try { Thread.sleep(freeze); } catch (InterruptedException ignored) {}
                    freeze = 0;
                } else {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    freeze -= 1000;
                }
            }
        }

        env.ui.setFreeze(id, 0);
        int ignored = table.countCards(); // as in original code (for unit tests)
        inFreeze = false;
    }

    public int score() {
        return score;
    }

    /** Called when a new turn starts (e.g., after the minute ends). */
    public void newturn() {
        removeAllPlayreTokens();
        queue.clear();
        inFreeze = false;
        badToken = false;
        tokensCounter = 0;
    }

    /** Toggle the "dealer is removing" flag. */
    public void cantPress() {
        theDealerIsRemoving = !theDealerIsRemoving;
    }

    /** Keep queue size aligned with tokensCounter (as in original design intent). */
    public void checkTokensQueue() {
        while (queue.size() > tokensCounter) queue.remove();
    }
}
