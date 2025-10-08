package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /** The game environment object. */
    private final Env env;

    /** Mapping between a slot and the card placed in it (null if none). */
    protected final Integer[] slotToCard; // card per slot (if any)

    /** Mapping between a card and the slot it is in (null if none). */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Constructor for testing.
     *
     * @param env        game environment
     * @param slotToCard mapping between a slot and the card placed in it (null if none)
     * @param cardToSlot mapping between a card and the slot it is in (null if none)
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {
        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
    }

    /**
     * Constructor for actual usage.
     *
     * @param env game environment
     */
    public Table(Env env) {
        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /** Print all possible legal sets of cards currently on the table. */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard)
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set)
                                        .mapToObj(card -> cardToSlot[card])
                                        .sorted()
                                        .collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots)
                                 .append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /** @return number of cards currently on the table. */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard) {
            if (card != null) ++cards;
        }
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card the card id to place
     * @param slot the target slot
     * @post the card is on the table in the assigned slot
     */
    public synchronized void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot the slot to clear
     */
    public synchronized void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        Integer card = slotToCard[slot];
        if (card != null) {
            cardToSlot[card] = null;
            slotToCard[slot] = null;
            env.ui.removeCard(slot);
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player the player index
     * @param slot   the table slot
     */
    public synchronized void placeToken(int player, int slot) {
        // basic validation: player index in range, slot index in range, and slot has a card
        if (player >= 0 && player < env.config.players &&
            slot   >= 0 && slot   < env.config.tableSize &&
            slotToCard[slot] != null) {
            env.ui.placeToken(player, slot);
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player the player index
     * @param slot   the table slot
     * @return true iff a token was successfully removed
     */
    public synchronized boolean removeToken(int player, int slot) {
        if (player >= 0 && player < env.config.players &&
            slot   >= 0 && slot   < env.config.tableSize &&
            slotToCard[slot] != null) {
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;
    }

    /** Removes all tokens from the UI/table. */
    public synchronized void removeTokens() {
        env.ui.removeTokens();
    }

    /** @return true if there exists at least one legal set on the current table. */
    public synchronized boolean legalSetTable() {
        // Build deck of card IDs currently on table (ignore nulls)
        List<Integer> deck = Arrays.stream(slotToCard)
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList());
        List<int[]> sets = env.util.findSets(deck, 1);
        return !sets.isEmpty();
    }
}
