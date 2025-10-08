package bguspl.set.ex;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import bguspl.set.Env;
import bguspl.set.UserInterface; 

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {
    boolean theDealerIsRemoving;//cant keypress if true(the dealer is removing cards)
    boolean foundset;// waiting until the dealer check the set
    boolean badToken;// to change one of the bad selections of the player after penalty
    public int[] playerSlots;
    boolean inFreeze;
    private BlockingQueue<Integer> queue;
 

    protected int tokensCounter;

    
    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private Dealer dealer;// check this

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        theDealerIsRemoving= false;
        foundset = false;
        playerSlots = new int[3];
        for (int i = 0; i < 3; i++) {
            playerSlots[i] = -1;
        }
        badToken = false;
        inFreeze = false;
        queue = new LinkedBlockingDeque<Integer>(3);
        tokensCounter = 0;
        this.dealer = dealer;
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;

    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human)
            createArtificialIntelligence();
        // boolean once= false;
        while (!terminate) {
            // TODO implement main player loop
            if (tokensCounter == 3 && !inFreeze && !badToken) {
                if(dealer.setFound){
                   if(!dealer.ifCommon(id)){
                    while( dealer.setFound){
                    }
                    if (dealer.checkIfSet(id)) {
                        queue.clear();
                        point();
                    } else {
                       
                        penalty();
                        if(!human){
                            int randomkey;
                            randomkey = (int) (Math.random() * playerSlots.length) +0;
                            table.removeToken(id, playerSlots[randomkey]);
                            removePlayerToken(playerSlots[randomkey]);
                
                    }
                    }
                   }
                }
                else{
                
                if (dealer.checkIfSet(id)) {
                    point();
                    queue.clear();
                } else {
                   
                    penalty();
                    if(!human){
                        int randomkey;
                        randomkey = (int) (Math.random() * playerSlots.length) +0;
                        table.removeToken(id, playerSlots[randomkey]);
                        removePlayerToken(playerSlots[randomkey]);
            
                }
                }
            } 
            }
            foundset = false;
            if (!human) {
                synchronized(this){
                    // queue.clear();
                    notifyAll();
    
                }
            }
           
        
        }

        if (!human)
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {
            }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate)   {
                // TODO implement player key press simulator
                while( tokensCounter<3){
                    if(terminate)  
                       break;
                int randomkey;
                randomkey = (int) (Math.random() * env.config.tableSize) +0;
                keyPressed(randomkey);
            }

                try {
                    synchronized (this) {
                        wait();/////for what, point or penaltty
                    }
                } catch (InterruptedException ignored) {
                }
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
        synchronized(this){
            notifyAll();
        }

    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        //// we sould check if the player is in plenty

        if (table.slotToCard[slot] != null && !inFreeze && !foundset && !theDealerIsRemoving ) {
            if (tokensCounter < 3 && tokensCounter>=0) {
                if (checkIfTokened(slot)) {
                    table.removeToken(id, slot);
                    removePlayerToken(slot);
                    // queue.remove(slot);


                } else {
                    if(queue.size()<3){
                    table.placeToken(id, slot);
                    placePlayerToken(slot);
                    // queue.add(slot);
                    //queue.put(slot);
                    badToken = false;
                }
                }
            } 
            else {
                if (checkIfTokened(slot)) {
                    table.removeToken(id, slot);
                    removePlayerToken(slot);
                    // queue.remove(slot);

                    
                }
            }

        }

    }

    public int[] returnArray() {
        int[] cards = new int[3];
        for (int i = 0; i < playerSlots.length; i++) {
            if(playerSlots[i]!=-1 && table.slotToCard[playerSlots[i]] != null)
                cards[i] = table.slotToCard[playerSlots[i]];

        }
        return cards;
    }

    public boolean checkIfTokened(int slot) {

        for (Integer currSlot : playerSlots) {
            if (currSlot!=-1 && currSlot == slot)
                return true;
        }
        return false;
    }

    public void removePlayerToken(int slot) {
        for (int i = 0; i < playerSlots.length; i++) {
            if (playerSlots[i] == slot && tokensCounter>0){
                playerSlots[i] = -1;
                tokensCounter--;
                queue.remove(slot);
            }
           
        }
       
       
    }

    public void removeAllPlayreTokens() {
        for (int i = 0; i < playerSlots.length; i++) {
            if(playerSlots[i]!=-1 ){
                playerSlots[i] = -1;
                tokensCounter--;
            }
             
        }

    }

    public void placePlayerToken(int slot) {
        boolean foundPlace = false;

        for (int i = 0; i < playerSlots.length && !foundPlace; i++) {
            if (playerSlots[i] == -1) {
                playerSlots[i] = slot;
                foundPlace = true;
                tokensCounter++;
                queue.add(slot);
            }
        }
      
        if (tokensCounter == 3)
            foundset = true;

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        inFreeze = true;
        score++;
        long freeze = env.config.pointFreezeMillis;
        env.ui.setScore(id, score);

        // after scoring a point the player should be frozen
        while (freeze > 0) {

            synchronized (Thread.currentThread()) {
                env.ui.setFreeze(id, freeze);

                if (freeze < 1000 && freeze > 0) {
                    try {

                        Thread.sleep(freeze);

                    } catch (InterruptedException ignored) {
                    }
                    freeze = 0;
                    env.ui.setFreeze(id, freeze);
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    freeze = freeze - 1000;
                }
            }
        }
        // inPenalty=false;
        env.ui.setFreeze(id, freeze);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        inFreeze = false;
       

    }

    public void penalty() {
        // TODO implement
        inFreeze = true;
        badToken = true;
        long freeze = env.config.penaltyFreezeMillis;

        // after scoring a point the player should be frozen
        while (freeze > 0) {

            synchronized (Thread.currentThread()) {
                env.ui.setFreeze(id, freeze);

                if (freeze < 1000 && freeze > 0) {
                    try {

                        Thread.sleep(freeze);

                    } catch (InterruptedException ignored) {
                    }
                    freeze = 0;
                    env.ui.setFreeze(id, freeze);
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    freeze = freeze - 1000;
                }
            }
        }
        // inPenalty=false;
        env.ui.setFreeze(id, freeze);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        inFreeze = false;
       
}

    public int score() {
        return score;
    }


    ///// when the 1 minute end 
    public void newturn() {
       
        removeAllPlayreTokens();
        queue.clear();
        inFreeze = false;
        badToken = false;
        tokensCounter=0;
        
    }

    
    public void cantPress(){
        if(theDealerIsRemoving){
        theDealerIsRemoving=false;}
        else{
        theDealerIsRemoving=true;}

    }





    public void checkTokensQueue(){
        while(queue.size()>tokensCounter)
            queue.remove();
  
  
      }

}


































































