package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Vector;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

     /**
     * The data constructure that holds for eacg player thier tokens
     */
   // protected Vector<Vector<Integer>> playerTokens;//i add 


    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {
      //  playerTokens = new Vector<Vector<Integer>>();
        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;

    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) { 
        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
      
        
    }
 
    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {

      synchronized(this){////shouldnt we care for countdown.?

        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

       // TODO implement

       env.ui.placeCard(card, slot);
    }
       
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public  synchronized void  removeCard(int slot) {
       
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        Integer card =slotToCard[slot];
        if(card!=null){
            cardToSlot[card]=null;
            slotToCard[slot]=null;
            env.ui.removeCard(slot);

        
    }}

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        if(player<= env.config.players && slotToCard[slot]!=null){
            env.ui.placeToken(player,slot);
 
        } 

    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public synchronized boolean removeToken(int player, int slot) {
        // TODO implement
        if(env.config.players>=player &&slot>=0 && slot<=env.config.tableSize && slotToCard[slot]!=null ){
             env.ui.removeToken(player,slot);
             //player.
                 return true;
            }
        
    
        return false;
      }
      
public void removeTokens(){

    env.ui.removeTokens();
}







public boolean legalSetTable(){//checking if there is a legal set in the table
    synchronized(this){
           List<Integer> legalSet = new ArrayList<>();
            for(int slot:cardToSlot){
                legalSet.add(slot);}

            List<int[]> sets = env.util.findSets(legalSet, 1);
                return !sets.isEmpty();    
        }
    }



}






































// package bguspl.set.ex;

// import bguspl.set.Env;
// import java.util.Vector;
// import java.util.ArrayList;
// //import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Objects;
// import java.util.stream.Collectors;

// /**
//  * This class contains the data that is visible to the player.
//  *
//  * @inv slotToCard[x] == y iff cardToSlot[y] == x
//  */
// public class Table {

//     /**
//      * The game environment object.
//      */
//     private final Env env;

//     /**
//      * Mapping between a slot and the card placed in it (null if none).
//      */
//     protected final Integer[] slotToCard; // card per slot (if any)

//     public Vector<Vector<Integer>> playerTokens;



//     /**
//      * Mapping between a card and the slot it is in (null if none).
//      */
//     protected final Integer[] cardToSlot; // slot per card (if any)

//     /**
//      * Constructor for testing.
//      *
//      * @param env        - the game environment objects.
//      * @param slotToCard - mapping between a slot and the card placed in it (null if none).
//      * @param cardToSlot - mapping between a card and the slot it is in (null if none).
//      */
   
//     public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

//         this.env = env;
//         this.slotToCard = slotToCard;
//         this.cardToSlot = cardToSlot;

//     }

//     /**
//      * Constructor for actual usage.
//      *
//      * @param env - the game environment objects.
//      */
//     public Table(Env env) {
// //a fiels in config  the total number of players;
//         this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
//          playerTokens = new Vector<Vector<Integer>>();
//          for(int i=0;i<playerTokens.size();i++){
//             Vector tepm=playerTokens.get(i);
//             tepm =new Vector<Vector<Integer>>();
//          }
        
//     }

//     /**
//      * This method prints all possible legal sets of cards that are currently on the table.
//      */
//     //? filter saves the object that are not null
//     public void hints() {
//         List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
//         env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
//             StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
//             List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
//             int[][] features = env.util.cardsToFeatures(set);
//             System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
//         });
//     }

//     /**
//      * Count the number of cards currently on the table.
//      *
//      * @return - the number of cards on the table.
//      */
//     public int countCards() {
//         int cards = 0;
//         for (Integer card : slotToCard)
//             if (card != null)
//                 ++cards;
//         return cards;
//     }

//     /**
//      * Places a card on the table in a grid slot.
//      * @param card - the card id to place in the slot.
//      * @param slot - the slot in which the card should be placed.
//      *
//      * @post - the card placed is on the table, in the assigned slot.
//      */
//     public synchronized void placeCard(int card, int slot) {//! should we do synchronized
//         try {
//             //The number of milliseconds to delay before removing/placing a card on the table
//             Thread.sleep(env.config.tableDelayMillis);
//         } catch (InterruptedException ignored) {}

//         // TODO implement
//             cardToSlot[card]=slot;
//             slotToCard[slot]=card;
//             env.ui.placeCard(card, slot);

//     }

//     /**
//      * Removes a card from a grid slot on the table.
//      * @param slot - the slot from which to remove the card.
//      */
//     public synchronized void removeCard(int slot) {//synchronized becuase player cant choose card that should be removen
//         try {
//             Thread.sleep(env.config.tableDelayMillis);
//         } catch (InterruptedException ignored) {}
//         // TODO implement
//         Integer card = slotToCard[slot];//slot=,cardId=3 -->slotTocard[slot]=3
//         if (card != null) {
//             cardToSlot[card] = null;
//             slotToCard[slot] = null;
//             env.ui.removeCard(slot);
//     }
// }

//     /**
//      * Places a player token on a grid slot.
//      * @param player - the player the token belongs to.
//      * @param slot   - the slot on which to place the token.
//      */
//     public void placeToken(int player, int slot) {
//         // TODO implement
//        if(player<=env.config.players && slotToCard[slot]!=null){
//             env.ui.placeToken(player, slot);//env contains a field ui which implements the UserInterface then I can write this 
//             (playerTokens.get(player)).add(slot);

//         }
        
//     }
   

//     /**
//      * Removes a token of a player from a grid slot.
//      * @param player - the player the token belongs to.
//      * @param slot   - the slot from which to remove the token.
//      * @return       - true iff a token was successfully removed.
//      */
//     public boolean removeToken(int player, int slot) {
//         // TODO implement
//         if(env.config.players>=player && slotToCard[slot]!=null && playerTokens.get(player).size()>0){
//             for(int i=0 ; i<playerTokens.get(player).size(); i++ ){
//              if((playerTokens.get(player)).get(i)==slot)
//              playerTokens.get(player).remove(slot);
//                  env.ui.removeToken(player,slot);
//                  return true;
//             }
//         }

//         return false;
//       }
    



//     public boolean checkIfTokened(int player, int slot){//checking if the player took this slot

//         Vector< Integer> currplayerTokens =playerTokens.get(player);//the tokens of the player
    
//         for( Integer playerTokens : currplayerTokens ){
    
//          if(playerTokens==slot){
//           return true;
    
//          }}
    
//        return false;
       
//         }
    
//         public int[] arrOfCards(int player){//convert the tokens of the player to array
//         int [] temp= { 
//             slotToCard[(playerTokens.get(player)).get(0)],
//             slotToCard[(playerTokens.get(player)).get(1)],
//             slotToCard[(playerTokens.get(player)).get(2)] };
    
//            return temp;
//         }



//         public boolean legalSetTable(){//checking if there is a legal set in the table
//            List<Integer> legalSet = new ArrayList<>();
//             for(int slot:cardToSlot){
//                 legalSet.add(slot);}

//             List<int[]> sets = env.util.findSets(legalSet, 1);
//                 return !sets.isEmpty();    
//         }
       



//         public void cleanPlyerTokens (){
//             for(int i=0 ; i< playerTokens.size(); i++){
//                  playerTokens.get(i).clear();
//            }
//               }

//   }
    



