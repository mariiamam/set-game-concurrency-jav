package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data.
 */
public class Dealer implements Runnable {

    // ----------------------
    // Runtime state
    // ----------------------
    public volatile boolean setFound;
    private volatile boolean terminate;

    private long time;                  // per-turn countdown
    private int[] cardsToRemove;        // the current candidate set (3 cards)

    // ----------------------
    // Game context
    // ----------------------
    private final Env env;
    private final Table table;
    private final Player[] players;

    // Deck: remaining card ids
    private final List<Integer> deck;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;

        this.setFound = false;
        this.cardsToRemove = new int[3];
        this.terminate = false;

        this.deck = IntStream.range(0, env.config.deckSize)
                             .boxed()
                             .collect(Collectors.toList());
    }

    /** Dealer main loop. */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

        // Start all player threads
        BlockingQueue<Thread> threads = new LinkedBlockingQueue<>();
        for (Player p : players) threads.add(new Thread(p));
        for (Thread t : threads) t.start();

        while (!shouldFinish()) {
            time = env.config.turnTimeoutMillis;

            placeCardsOnTable();
            if (env.config.hints) table.hints();

            timerLoop();

            // Re-enable player keypress after the countdown phase
            for (Player p : players) p.cantPress();
        }

        // End of game
        env.ui.removeTokens();
        terminate();
        announceWinners();

        long endPause = env.config.endGamePauseMillies;
        try { Thread.sleep(endPause); } catch (InterruptedException ignored) {}

        env.ui.dispose();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /** Inner countdown loop for a single turn. */
    private void timerLoop() {
        while (time > 0) {
            long start = System.currentTimeMillis();
            long elapsed = 0;

            if (setFound) {
                removeCardsFromTable();
                placeCardsOnTable();
                elapsed = System.currentTimeMillis() - start;
            }

            if (time <= env.config.turnTimeoutWarningMillis && time > 0) {
                // red, millisecond-resolution timer
                updateTimerDisplay(true);
                long sleepTime = Math.max(10 - elapsed, 0);
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
                time -= 10;
            } else {
                // regular, second-resolution timer
                updateTimerDisplay(false);
                long sleepTime = Math.max(1000 - elapsed, 0);
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
                time -= 1000;
            }
        }

        // When time ends
        for (Player p : players) p.cantPress();

        removeAllCardsFromTable();
        table.removeTokens();
    }

    /** Request termination of the whole game. */
    public void terminate() {
        if (shouldFinish() || deck.isEmpty() || env.config.turnTimeoutWarningMillis < 0) {
            for (Player p : players) p.terminate();
            terminate = true;
        }
    }

    /** @return true iff no more sets exist in the deck. */
    private boolean shouldFinish() {
        return env.util.findSets(deck, 1).isEmpty();
    }

    /** Remove cards of the last found set from table (and players’ tokens on them). */
    private void removeCardsFromTable() {
        if (!setFound) return;

        // Remove any tokens players placed on the cards being removed
        for (Player player : players) {
            for (int card : cardsToRemove) {
                Integer slot = table.cardToSlot[card];
                if (slot != null && player.checkIfTokened(slot)) {
                    player.removePlayerToken(slot);
                    table.removeToken(player.id, slot);
                }
            }
        }

        // Remove the cards from table
        for (int card : cardsToRemove) {
            Integer slot = table.cardToSlot[card];
            if (slot != null) table.removeCard(slot);
        }

        // Align player token queues with their counters
        for (Player p : players) p.checkTokensQueue();

        setFound = false;
        time = env.config.turnTimeoutMillis; // reset timer
    }

    /** Place cards from deck onto free slots. */
    private void placeCardsOnTable() {
        for (int i = 0; i < env.config.tableSize && !deck.isEmpty(); i++) {
            if (table.slotToCard[i] == null) {
                int idx = (int) (Math.random() * deck.size());
                int card = deck.remove(idx);
                table.placeCard(card, i);
            }
        }
    }

    /** Update the UI countdown. */
    private void updateTimerDisplay(boolean reset) {
        env.ui.setCountdown(time, reset);
    }

    /** Return all cards from table to deck and clear players’ tokens/state. */
    private void removeAllCardsFromTable() {
        table.removeTokens(); // clear tokens from UI

        for (Player p : players) p.removeAllPlayreTokens();

        for (int slot = 0; slot < env.config.tableSize; slot++) {
            Integer card = table.slotToCard[slot];
            if (card != null) {
                deck.add(card);         // return card to deck
                table.removeCard(slot); // clear from table
            }
        }

        for (Player p : players) p.newturn(); // reset per-turn player state
    }

    /** Compute winners and display them. */
    private void announceWinners() {
        int max = -1;
        for (Player p : players) max = Math.max(max, p.score());

        int count = 0;
        for (Player p : players) if (p.score() == max) count++;

        int[] winners = new int[count];
        int i = 0;
        for (Player p : players) if (p.score() == max) winners[i++] = p.id;

        env.ui.announceWinner(winners);
    }

    /** Check the current player's 3 chosen ca**
