import java.util.*;
import java.text.*;

// VERSION OF FEBRUARY 4, 2017, FIXED VARIOUS BUGS AND ADDED COMMENTS

public class IlkkaPlayer3 implements PLBadugiPlayer {
    
    private static final double MAXAGGRO = 5;
    
    private Random rng = new Random();
    private int position;
    
    // State variables for the current hand, meaning exactly what the names say. 
    private int lastBetWasBluff = -1;
    private int lastDrawWasBluff = -1;
    private int weFoldedToRaise = -1;
    private int weRaisedLast = -1;
    private int ourLastDraw = 0;
    
    // How many hands have reached the given round against this opponent.
    private int[] handCount;
    // Our rough guesstimate of the chances for this hand.
    private double chances = 1.0;
    // The name of this agent.
    private String name;
    // Chances multipliers depending on how many cards the opponent drew.
    private double[] drawMult = {1.1, 1.3, 1.5, 1.9};
    // Tracker of how aggressive the opponent has been on each drawing round.
    private int[] opponentAggro = new int[4];
    // Tracker of how aggressive we have been on each drawing round.
    private int[] ourAggro = new int[4];
    
    private static DecimalFormat df = new DecimalFormat();
    static {
        df.setMaximumFractionDigits(3);
    }
    
    // Counter of how many IlkkaPlayer3 objects have been created.
    private static int id = 0;
    
    public IlkkaPlayer3(String name) {
        this.name = name;
    }
    
    public IlkkaPlayer3() {
        this.name = "Badugi Boogie " + (++id);
    }
    
    // Threshold hands to estimate the value of our hand for the drawing rounds
    private PLBadugiHand[][] thresholds = {
        // Thresholds for 0 draws remaining
        { new PLBadugiHand("kh7h4c2d"), new PLBadugiHand("qh5s2h2d"), new PLBadugiHand("th9c4das"), new PLBadugiHand("7d6c3sah") },
        // Thresholds for 1 draws remaining
        { new PLBadugiHand("qh9d5c4c"), new PLBadugiHand("7s7c6c2d"), new PLBadugiHand("jh9h7s2c"), new PLBadugiHand("jd7c5s4h") },
        // Thresholds for 2 draws remaining
        { new PLBadugiHand("kstc7c2s"), new PLBadugiHand("js8s4d3d"), new PLBadugiHand("js6d4c2s"), new PLBadugiHand("ks7d4h2c") },
        // Thresholds for 3 draws remaining
        { new PLBadugiHand("khks7h2d"), new PLBadugiHand("Qs8h7s5c"), new PLBadugiHand("Th6h4s2c"), new PLBadugiHand("KhQsJdTc")}
    };
    
    // Initialize the counters at the start of each hand.
    public void startNewMatch(int handsToGo) {
        handCount = new int[4];
        opponentAggro[0] = opponentAggro[1] = opponentAggro[2] = opponentAggro[3] = 0;
        ourAggro[0] = ourAggro[1] = ourAggro[2] = ourAggro[3] = 0;
    }    
    
    // Initialize the hand state variables at the start of the new hand.
    public void startNewHand(int position, int handsToGo, int currentScore) {
        lastBetWasBluff = -1;
        lastDrawWasBluff = -1;
        weFoldedToRaise = -1;
        weRaisedLast = -1;
        chances = 1.0;
        this.position = position;
    }
    
    public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises, int toCall,
                             int minRaise, int maxRaise, int opponentDrew) 
    {
        // Increment the counter for hands that have reached this drawing round.
        if(raises < 2) { handCount[drawsRemaining]++; }
        // Opponent raising lowers our chances.
        if(raises > 0) { chances = (1 + chances) / 2; }
        // Compute the opponent aggro and our aggro on this drawing round based on previous hands.
        double opAg, ourAg;
        if(handCount[drawsRemaining] > 50) { // Use history only after 50 hands
            opAg= opponentAggro[drawsRemaining] / (3 + (double)(handCount[drawsRemaining]));
            ourAg = ourAggro[drawsRemaining] / (3 + (double)(handCount[drawsRemaining]));
        }
        else { opAg = 0.3; ourAg = 0.3; }
        weRaisedLast = -1;
        // Pot odds often affect what we do this betting round.
        double potOdds = toCall / (double) pot;
        if(toCall > 0) { // Opponent made a raise in this drawing round.
            opponentAggro[drawsRemaining] += raises;
        }
        // If opponent did not bet ahead of us, our chances improve.
        if(position == 1 && toCall == 0) { chances *= (1.1 + rng.nextDouble() * 0.5); }
        // Compare the hand to the threshold hands for the current betting round.
        for(PLBadugiHand other: thresholds[drawsRemaining]) {
            if(hand.compareTo(other) > 0) { chances *= 1.05; }
            else { break; }
        }
        // Adjust our chances by our inactive cards and by how many card the opponent drew.
        if(hand.getInactiveCards().size() > 1) { chances = 0.8 * chances; }
        for(int i = 0; i < opponentDrew; i++) { chances *= 1.2 + rng.nextDouble() * 0.3; }

        // If our last draw was a bluff, raise now.
        if(lastDrawWasBluff > -1) {
            lastDrawWasBluff = rng.nextDouble() < .5 ? -1 : drawsRemaining;
            return maxRaise - rng.nextInt(maxRaise - minRaise + 1) / 2;
        }
        
        //System.out.printf("Chances for %s feel like %.3f.\n", name, chances);
        if(chances < 3 * potOdds / (1 - opAg) && hand.compareTo(thresholds[0][thresholds[0].length - 1]) < 0) {
            if(raises < 2 && rng.nextDouble() * 0.5 < 1 - ourAg) {
                lastBetWasBluff = drawsRemaining;
                return maxRaise - rng.nextInt(maxRaise - minRaise + 1) / 3;
            }
            else if(drawsRemaining > 0 && hand.getActiveCards().size() == 4 && opponentDrew > 0 && rng.nextDouble() < ourAggro[drawsRemaining] ) {
                return toCall;
            }
            else { 
                if(toCall > 0) { weFoldedToRaise = drawsRemaining; }
                return 0;
            }
        }
        
        double action = chances + (0.2 + 0.3 * rng.nextDouble()) * (1 - opAg) - 0.3;
        if(action < 3 * potOdds * drawMult[drawsRemaining] * (1 - ourAg)) { return toCall; }
        else if(opponentDrew == 0 && drawsRemaining == 0) { return toCall; }
        else if(opponentDrew < 2 && drawsRemaining == 0 && hand.getInactiveCards().size() > 0) { return toCall; } 
        else {
            int amount = (int)(maxRaise - (maxRaise - minRaise) * (rng.nextDouble() * 0.7 + 0.3));
            weRaisedLast = drawsRemaining;
            ourAggro[drawsRemaining]++;
            return amount;
        }
        
    }
    
    public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
        List<Card> allCards = hand.getAllCards();
        List<Card> inactiveCards = hand.getInactiveCards();
        List<Card> pitch = new ArrayList<Card>();
        // Don't break a made badugi when the opponent is drawing.
        if(inactiveCards.size() == 0 && drawsRemaining < 2 && dealerDrew > 0) { ourLastDraw = 0; return pitch; }
        // If we are bluffing, are we going to keep bluffing?
        if(lastDrawWasBluff > -1 && dealerDrew != 0 || Math.max(dealerDrew, 0) * rng.nextDouble() * (1 - chances) 
        * (opponentAggro[drawsRemaining] / (double) handCount[drawsRemaining]) > 0.4) {
            ourLastDraw = 0; lastDrawWasBluff = drawsRemaining; return pitch;
        }
        // Pitch the inactive cards and also the active cards that are too high in rank.
        for(Card c: allCards) {
            if(c.getRank() > 12 + dealerDrew - drawsRemaining || inactiveCards.contains(c)) {
                pitch.add(c);
            }
        }
        ourLastDraw = pitch.size();
        return pitch;
    }
    
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int totalPot) {
        lastBetWasBluff = -1;
        weRaisedLast = -1;
        weFoldedToRaise = -1;
    }
    
    public String getAgentName() { return name; }
    
    public String getAuthor() { return "Kokkarinen, Ilkka"; }
}
