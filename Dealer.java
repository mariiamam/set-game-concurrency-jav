package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    //private Vector<Integer> sameTokens;// vector to check if there is a same token for some players
    public volatile boolean setFound;
    private long time;
    private int[] cardsToRemove;/// was a vector
    BlockingQueue<Thread> currThread ;
//     private BlockingQueue<Integer> setQueue;


    // private Queue<Integer>

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        setFound = false;
        //time = env.config.turnTimeoutMillis;// 60000
        cardsToRemove = new int[3];
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        currThread = new LinkedBlockingQueue<>();
        //setAlreadyFound = false;
       // setQueue=new LinkedBlockingQueue<>(); 
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

        BlockingQueue<Thread> currThread = new LinkedBlockingQueue<>();
        for (int i = 0; i < players.length; i++) {
            currThread.add(new Thread(players[i]));
        }
        for (Thread a : currThread)
            a.start();

        while (!shouldFinish()) {
            time = env.config.turnTimeoutMillis;
            
            placeCardsOnTable();
            if(env.config.hints){
                table.hints();}//!!!!!from config file
            timerLoop();
            // removeAllCardsFromTable();
            for(int m=0 ; m<players.length;m++){//to enable the players keypress
                players[m].cantPress();
               }
        }
        env.ui.removeTokens();//now
        terminate();
        announceWinners();
        long endTime = env.config.endGamePauseMillies;
        try {
            Thread.sleep(endTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        env.ui.dispose();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        // while (!terminate && System.currentTimeMillis() < reshuffleTime) {//// the
        // one minute
        while (time > 0) {
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;

            if (setFound) {
                removeCardsFromTable();
                placeCardsOnTable();
                elapsedTime = System.currentTimeMillis() - startTime;
            }

           if(time <= env.config.turnTimeoutWarningMillis&& time>0) {
                updateTimerDisplay(true);
                synchronized (Thread.currentThread()) {
                    try {
                        long sleepTime= Math.max(10-elapsedTime, 0);
                       Thread.sleep(sleepTime);
                    //    Thread.sleep(elapsedTime);
                    } catch (InterruptedException e) {
                    }
                    time = time - 10;
                }
        
            }

            /// check if should finish
            else {
                
                updateTimerDisplay(false);
                synchronized (this) {
                    try {
                        long sleepTime= Math.max(1000-elapsedTime, 0);
                        Thread.sleep(sleepTime);

                    } catch (InterruptedException e) {
                    }
                }
                time = time - 1000;
            }
            
         }

       
        for(int m=0 ; m<players.length;m++){
            players[m].cantPress();
           }


      removeAllCardsFromTable();
      table.removeTokens();
      //System.out.println(env.util.findSets(deck, 1).size());
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        if (shouldFinish() || deck.size() == 0 || env.config.turnTimeoutWarningMillis<0) {
            for (int i = 0; i < players.length; i++) {
                players[i].terminate();
            }
            terminate = true;
        }

        // if(time==0)
        // terminate=true;

    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    // private void removeCardsFromTable() {
    //     /// we shuold removeall the tokens that wason the card
    //     // TODO implement
    //     //System.out.println("in remove");
    //     //if (setFound&&foundSetinPlayer) {//!!!!!!!!!!!!!!!!!!!!!!
    //         if (setFound) {
    //         for (int i = 0; i < players.length; i++) {
    //             for (int p = 0; p < cardsToRemove.length; p++) {
    //                 if(table.cardToSlot[cardsToRemove[p]] !=null)
    //                  if(players[i].checkIfTokened(table.cardToSlot[cardsToRemove[p]])) {
    //                     players[i].removePlayerToken(table.cardToSlot[cardsToRemove[p]]);
    //                     table.removeToken(i, table.cardToSlot[cardsToRemove[p]]);
    //                 }
    //             }
    //         }

    //         for (int i = 0; i < cardsToRemove.length; i++) {
    //             if(table.cardToSlot[cardsToRemove[i]]!=null)
    //             table.removeCard(table.cardToSlot[cardsToRemove[i]]);
                
    //         }
    //         //placeCardsOnTable();//!!!!!!!!!!!!!!!!!!1

    //         setFound = false;
    //         time = env.config.turnTimeoutMillis;//reset the time(60 second)

    //     }

    // }



    private void removeCardsFromTable() {
        /// we shuold removeall the tokens that wason the card
        // TODO implement
        //System.out.println("in remove");
        //if (setFound&&foundSetinPlayer) {//!!!!!!!!!!!!!!!!!!!!!!
            if (setFound) {
            for (int i = 0; i < players.length; i++) {
                for (int p = 0; p < cardsToRemove.length; p++) {
                    if(table.cardToSlot[cardsToRemove[p]] !=null)
                     if(players[i].checkIfTokened(table.cardToSlot[cardsToRemove[p]])) {
                        players[i].removePlayerToken(table.cardToSlot[cardsToRemove[p]]);
                        table.removeToken(i, table.cardToSlot[cardsToRemove[p]]);
                    }
                }
            }

            for (int i = 0; i < cardsToRemove.length; i++) {
                if(table.cardToSlot[cardsToRemove[i]]!=null)
                table.removeCard(table.cardToSlot[cardsToRemove[i]]);
                
            }
            //placeCardsOnTable();//!!!!!!!!!!!!!!!!!!1
            for (int p = 0; p < players.length; p++) {
                players[p].checkTokensQueue();
            }
            setFound = false;
            time = env.config.turnTimeoutMillis;//reset the time(60 second)

        }

    }











    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */

    private void placeCardsOnTable() {
        // TODO implement

        for (int i = 0; i < env.config.tableSize && !deck.isEmpty(); i++) {///// WARNING NOT RANDOMLY
            if (table.slotToCard[i] == null) {
                int index;
                index = (int) (Math.random() * deck.size()) + 0;
                table.placeCard(deck.get(index), i);
                deck.remove(index);
            }
        }

    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement


    }

    // /**
    // * Set the countdown time to the specified number of milliseconds.
    // * @param millies - the milliseconds to be shown.
    // * @param warn - if true, the timer will be painted in red and will display
    // milliseconds
    // */
    // void setCountdown(long millies, boolean warn);

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        env.ui.setCountdown(time, reset);

    }

    /**
     * Returns all the cards from the table to the deck.
     */
   
    private void removeAllCardsFromTable() {
        // TODO implement

        table.removeTokens();// removing the names of the players

        for (int p = 0; p < players.length; p++) {
            players[p].removeAllPlayreTokens();
        }

        for (int i = 0; i < env.config.tableSize; i++) {
            if (table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);// return the cards to the deck
                table.removeCard(i);
            }
        }
        
            for(int k=0;k<players.length;k++){
                (players[k]).newturn();//removing the old tokens of the players
            }

    
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {

        // TODO implement
        int winnersNum = 0;
        int maxScore = -1;
        for (Player p : players) {
            if (p.score() > maxScore) {
                maxScore = p.score();
            }
        }

        for (Player p : players) {
            if (p.score() == maxScore)
                winnersNum++;
        }

        int[] winners = new int[winnersNum];
        int i = 0;

        for (Player p : players) {
            if (p.score() == maxScore) {
                winners[i] = p.id;
                i++;
            }
        }
        env.ui.announceWinner(winners);
    }



    // public boolean checkIfSet(int Player) {/// should be synch
    //      //synchronized (this) {
    //         cardsToRemove = (players[Player]).returnArray();
    //         setFound = env.util.testSet(cardsToRemove);
    //         // notifyAll();

    //         return setFound;

    //     // }

    // // }

//?the correct
    public boolean checkIfSet(int Player) {/// should be synch
        Player currentPlayer = players[Player];
        
        synchronized (currentPlayer) {
            if(!setFound){
           cardsToRemove = (players[Player]).returnArray();
           setFound = env.util.testSet(cardsToRemove);
      
            }
           return setFound;}

            
        }
    


//function that check if there is a common token from the legal set
public  boolean ifCommon(int id){
  //  cardsToRemove = (players[player]).returnArray();//the tokens of the first player that choose a legal set
  boolean ans=false;
    for(int i=0;i<cardsToRemove.length;i++){
        for(int x=0;x<players[id].playerSlots.length;x++){
            if( table.cardToSlot[cardsToRemove[i]]!=null && players[id].playerSlots[x]!=-1  && table.cardToSlot[cardsToRemove[i]]==players[id].playerSlots[x]){
            table.removeToken(id, x);
            players[id].removePlayerToken(x);
            ans=true;
        }
            //setQueue.remove(id);//!!!new 
        }
    }
    return ans;
}
}

















































































