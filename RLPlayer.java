import java.util.*;
import java.util.zip.*;
import java.io.*;

public class RLPlayer implements PLBadugiPlayer {

    private static final double TDMCMIX = 0.5; // 0.0 is full TD, 1.0 is full MC
    private static double learningRate = 0.01;
    private double temperature = 0.1;
    private static int nextLRUpdate = 20_000;
    private double softExp = Math.E;
    
    private static int[][] choose = new int[14][14];
    static {
        for(int n = 0; n < 14; n++) {
            choose[n][1] = n;
            choose[n][n] = choose[n][0] = 1;
        }
        for(int n = 2; n < 14; n++) {
            for(int k = 2; k < n; k++) {
                choose[n][k] = choose[n-1][k-1] + choose[n-1][k];
            }
        }
    }
    
    private static String[] dealerLines = {
        "CF", "CC", "CRF", "CRC", "CRRF", "CRRC", "RF", "RC", "RRF", "RRC", "RRR"
    };
    private static int[] dealerLineCount = new int[dealerLines.length];
    
    private static String[] oppoLinesCheck = {
        "C", "RF", "RC", "RRF", "RRC"
    };
    private static int[] oppoLineCheckCount = new int[oppoLinesCheck.length];
    
    private static String[] oppoLinesBet = {
        "F", "C", "RF", "RC", "RR"
    };
    private static int[] oppoLineBetCount = new int[oppoLinesBet.length];
    
    private static final int RANKS;
    private static final int BLUFFCATCHER, EIGHTHIGH, SIXHIGH;
    private static List<Integer> rankThres = new ArrayList<Integer>();
    static {
        int idx = 0, max = choose[13][4] + choose[13][3] + choose[13][2] + choose[13][1];
        int step = 1;
        int count = 0;
        while(idx < max) {
            rankThres.add(idx);
            idx += step;
            if(++count == 4) { step++; count = 0;}
        }
        RANKS = rankThres.size() + 1;
        int[] catcher = { 4, 3, 2 };
        BLUFFCATCHER = approxRank(catcher);
        int[] eightHigh = { 8, 7, 6, 5 };
        EIGHTHIGH = approxRank(eightHigh);
        int[] sixHigh = { 6, 5, 4, 3 };
        SIXHIGH = approxRank(sixHigh);
        System.out.println("Distinguishing between " + RANKS + " equivalence classes out of " + max + ".");
    }
    
    public static void printLineCounts() {
        System.out.println(Arrays.toString(dealerLineCount));
        System.out.println(Arrays.toString(oppoLineCheckCount));
        System.out.println(Arrays.toString(oppoLineBetCount));
    }
        
    private static final int RAISES = PLBadugiRunner.MAX_RAISES + 2;
    
    private static double[][] dealerLine3 = new double[RANKS][dealerLines.length];
    private static double[][][][] dealerLine2 = new double[3][RANKS][RAISES][dealerLines.length];
    private static double[][][][] dealerLine1 = new double[3][RANKS][RAISES * 2][dealerLines.length];
    private static double[][][][] dealerLine0 = new double[3][RANKS][RAISES * 3][dealerLines.length];
    
    private static double[][] oppoLineC3 = new double[RANKS][oppoLinesCheck.length];
    private static double[][][][] oppoLineC2 = new double[3][RANKS][RAISES][oppoLinesCheck.length];
    private static double[][][][] oppoLineC1 = new double[3][RANKS][RAISES * 2][oppoLinesCheck.length];
    private static double[][][][] oppoLineC0 = new double[3][RANKS][RAISES * 3][oppoLinesCheck.length];
    
    private static double[][] oppoLineB3 = new double[RANKS][oppoLinesBet.length];
    private static double[][][][] oppoLineB2 = new double[3][RANKS][RAISES][oppoLinesBet.length];
    private static double[][][][] oppoLineB1 = new double[3][RANKS][RAISES * 2][oppoLinesBet.length];
    private static double[][][][] oppoLineB0 = new double[3][RANKS][RAISES * 3][oppoLinesBet.length];
    
    private static int nonZero = 0;
    
    private static void dump(double[] table, PrintWriter out) {
        for(double e: table) { 
            if(e != 0) { nonZero++; }
            out.printf("%.5f", e); out.print(" "); 
        }
    }
    
    private static void dump(double[][] tables, PrintWriter out) {
        for(double[] e: tables) { dump(e, out); }
    }
    
    private static void dump(double[][][] tabless, PrintWriter out) {
        for(double[][] e: tabless) { dump(e, out); }
    }
    
    private static void dump(double[][][][] tablesss, PrintWriter out) {
        for(double[][][] e: tablesss) { dump(e, out); }
    }
    
    public static void saveTables(String filename) throws IOException {
        PrintWriter out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(filename)));
        nonZero = 0;
        
        dump(dealerLine3, out);
        dump(dealerLine2, out);
        dump(dealerLine1, out);
        dump(dealerLine0, out);
        
        dump(oppoLineC3, out);
        dump(oppoLineC2, out);
        dump(oppoLineC1, out);
        dump(oppoLineC0, out);
        
        dump(oppoLineB3, out);
        dump(oppoLineB2, out);
        dump(oppoLineB1, out);
        dump(oppoLineB0, out);
        
        System.out.println("Wrote the file " + filename + " with " + nonZero + " nonzero entries.");
        out.close();
    }
    
    private static void read(double[] table, Scanner s) {
        for(int i = 0; i < table.length; i++) {
            table[i] = s.nextDouble();
            if(table[i] != 0) { nonZero++; }
        }
    }
    
    private static void read(double[][] tables, Scanner s) {
        for(int i = 0; i < tables.length; i++) {
            read(tables[i], s);
        }
    }
    
    private static void read(double[][][] tabless, Scanner s) {
        for(int i = 0; i < tabless.length; i++) {
            read(tabless[i], s);
        }
    }
    
    private static void read(double[][][][] tablesss, Scanner s) {
        for(int i = 0; i < tablesss.length; i++) {
            read(tablesss[i], s);
        }
    }
    
    public static void peek() {
        System.out.println(Arrays.toString(dealerLine3[20]));
        System.out.println(Arrays.toString(dealerLine3[10]));
        System.out.println(Arrays.toString(dealerLine1[1][12][1]));
        System.out.println(Arrays.toString(dealerLine1[0][12][0]));
        System.out.println(Arrays.toString(dealerLine0[1][30][6]));
        System.out.println(Arrays.toString(oppoLineC2[2][5][1]));
        System.out.println(Arrays.toString(oppoLineB1[1][15][4]));
    }
    
    public static void loadTables(String filename) throws IOException {
        Scanner s = new Scanner(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
        nonZero = 0;
        
        read(dealerLine3, s);
        read(dealerLine2, s);
        read(dealerLine1, s);
        read(dealerLine0, s);
        
        read(oppoLineC3, s);
        read(oppoLineC2, s);
        read(oppoLineC1, s);
        read(oppoLineC0, s);
        
        read(oppoLineB3, s);
        read(oppoLineB2, s);
        read(oppoLineB1, s);
        read(oppoLineB0, s);
 
        s.close();
        System.out.println("Finished reading the tables " + filename + " with " + nonZero + " nonzero entries.");
        System.out.flush();
    }
    
    private double adjusted(double v, String action) {
        double adjusted = v;
        /*
        if(action.equals("CF") || action.equals("RF") || action.equals("RRF") || action.equals("F")) {
            adjusted /= (oppoAggro / (double)(ownAggro));
        }
        */
        return adjusted;
    }
    
    private static Random rng = new Random();
    public int softMax(double[] a, String[] actions, int drawsRemaining) {
        double temp = temperature * (1.0 + 0.02 * drawsRemaining);
        double sum_exp = 0; 
        for(int i = 0; i < a.length; i++) {
            sum_exp += adjusted(Math.pow(softExp, a[i] / temp), actions[i]); 
        }
        double thres = rng.nextDouble() * sum_exp;
        int idx = 0;
        while(idx < a.length - 1) {
            double ad = adjusted(Math.pow(softExp, a[idx] / temp), actions[idx]);
            if(thres < ad) { break; }
            thres -= ad;
            idx++;
        }
        return idx;
    }
    
    public static int badugiRank(int[] ranks) {
        if(ranks.length == 4) {
            return choose[ranks[0] - 1][4] // first card lower
            + choose[ranks[1]- 1][3] // first card same, second card lower
            + choose[ranks[2] - 1][2] // first and second card same, third card lower
            + choose[ranks[3] - 1][1]; // first, second and third card same, fourth card lower
        }
        else if(ranks.length == 3) {
            return choose[13][4] // all four card badugis 
            + choose[ranks[0] - 1][3] // first card lower
            + choose[ranks[1] - 1][2] // first card same, second card lower
            + choose[ranks[2] - 1][1]; // first and second same, third card lower
        }
        else if(ranks.length == 2) {
            return choose[13][4] + choose[13][3] // all four and three card badugis
            + choose[ranks[0] - 1][2] // first card lower
            + choose[ranks[1] - 1][1]; // first card same, second card lower
        }
        else if(ranks.length == 1) {
            return choose[13][4] + choose[13][3] + choose[13][2] // all four, three and two card badugis
            + choose[ranks[0] - 1][1]; // first card lower
        }
        else {  // any hand is better than nothing
            return choose[13][4] + choose[13][3] + choose[13][2] + choose[13][1];
        }
    }
    
    public static int approxRank(int[] ranks) {
        int trueRank = badugiRank(ranks);
        int idx = 0;
        while(idx < rankThres.size() && rankThres.get(idx) < trueRank) { idx++; }
        return idx;
    }
    
    private static long totalHandsPlayed = 0;
    
    private static int nextId = 0;
    private int id;
    
    public RLPlayer() {
        this.id = ++nextId;
    }
    
    private int oppoAggro = 0, ownAggro = 0;
    public void startNewMatch(int handsToGo) { 
        oppoAggro = 100;
        ownAggro = 100;
    }
    
    private int[] chosenLines = new int[4];
    private double[][] chosenTables = new double[4][1];
    String currentLine;
    private int position;
    private int totalRaises;
    private int lastStreet;
    private boolean lastSnow;
    private boolean nextSnow;
    
    public void startNewHand(int position, int handsToGo, int currentScore) {
        this.position = position;
        this.totalRaises = 0;
        this.lastStreet = 3;
        this.lastSnow = false;
        this.nextSnow = false;
        if(++totalHandsPlayed == nextLRUpdate) {
            nextLRUpdate = 3 * nextLRUpdate / 2;
            learningRate *= 0.98;
            temperature *= 0.99;
        }
    }
    
    public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises, int toCall,
                             int minRaise, int maxRaise, int opponentDrew)
    {
        int opD = Math.max(Math.min(opponentDrew, 2), 0);
        if(toCall / (double)pot > 0.1) { 
            oppoAggro++; 
            totalRaises++;
        }
        if(lastStreet > drawsRemaining) { lastStreet = drawsRemaining; }
        int rank = approxRank(hand.getActiveRanks());
        if(position == 0 && raises == 0) { // choose dealer line for us for this betting round
            double[] q;
            if(drawsRemaining == 3) { q = dealerLine3[rank]; }
            else if(drawsRemaining == 2) { q = dealerLine2[opD][rank][totalRaises]; }
            else if(drawsRemaining == 1) { q = dealerLine1[opD][rank][totalRaises]; }
            else { q = dealerLine0[opD][rank][totalRaises]; }
            int idx = softMax(q, dealerLines, drawsRemaining );
            dealerLineCount[idx]++;
            chosenLines[drawsRemaining] = idx;
            chosenTables[drawsRemaining] = q;
            currentLine = dealerLines[idx];
        }
        else if(position == 1 && raises < 2) { // choose oppo line for us for this betting round
            double[] q;
            int idx = 0;
            if(raises == 0) {
                if(drawsRemaining == 3) { q = oppoLineC3[rank]; }
                else if(drawsRemaining == 2) { q = oppoLineC2[opD][rank][totalRaises]; }
                else if(drawsRemaining == 1) { q = oppoLineC1[opD][rank][totalRaises]; }
                else { q = oppoLineC0[opD][rank][totalRaises]; }
                idx = softMax(q, oppoLinesCheck, drawsRemaining);
                oppoLineCheckCount[idx]++;
                currentLine = oppoLinesCheck[idx];
            }
            else { 
                if(drawsRemaining == 3) { q = oppoLineB3[rank]; }
                else if(drawsRemaining == 2) { q = oppoLineB2[opD][rank][totalRaises]; }
                else if(drawsRemaining == 1) { q = oppoLineB1[opD][rank][totalRaises]; }
                else { q = oppoLineB0[opD][rank][totalRaises]; }
                idx = softMax(q, oppoLinesBet, drawsRemaining);
                oppoLineBetCount[idx]++;
                currentLine = oppoLinesBet[idx];
            }
            chosenLines[drawsRemaining] = idx;
            chosenTables[drawsRemaining] = q;
        }
        
        char act = 'C';
        if(currentLine.length() > 0) {
            act = currentLine.charAt(0);
            currentLine = currentLine.substring(1); 
        }
        if(act == 'F' && raises < 2 && drawsRemaining == 0 && rank <= EIGHTHIGH) { act = 'C'; }
        
        boolean weRaiseNow = (act == 'R' || lastSnow);
        
        
        if(drawsRemaining == 0 && position == 1) {
            weRaiseNow |= (toCall == 0 && opponentDrew > 0 && rank <= EIGHTHIGH);
        }
        weRaiseNow |= (drawsRemaining == 0 && position == 0 && opponentDrew > 1 && rank <= EIGHTHIGH);
        weRaiseNow |= (drawsRemaining == 0 && position == 0 && raises > 0 && rank <= SIXHIGH);
        weRaiseNow |= (drawsRemaining == 0 && rank < 4);
        
        if(drawsRemaining == 0) {
            if(opponentDrew == 0 && raises > 1 && rank > SIXHIGH) { weRaiseNow = false; }
            if((totalRaises > 3 && raises > 0 || raises > 1) && opponentDrew < 2 && rank > BLUFFCATCHER) { 
                weRaiseNow = false; act = 'F'; 
            }
        }
        
        if(drawsRemaining == 1 && raises > 0 && totalRaises > 3) {
            if(opponentDrew == 0 && rank > SIXHIGH) { weRaiseNow = false; }
            if(opponentDrew == 1 && !lastSnow && rank > BLUFFCATCHER) { weRaiseNow = false; } 
        }
        
        
        lastSnow = false;
        if(weRaiseNow) {
            totalRaises++; ownAggro++;
            if(maxRaise - minRaise == 0) { return minRaise; }
            else {  
                return maxRaise - rng.nextInt(maxRaise - minRaise) / 2;
            }
        }
        if(act == 'F') {
            if(toCall < 0.05 * pot) { return toCall; }
            if(raises < 2 && drawsRemaining == 3 && rank > RANKS - 3) { 
                nextSnow = true;
                totalRaises++; ownAggro++;
                return maxRaise;
            }
            return 0;
        }
        
        return toCall;
    }
    
    public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
        List<Card> pitch = new ArrayList<Card>();
        List<Card> active = hand.getActiveCards();
        if(nextSnow) { // snow on the first betting round
            lastSnow = true;
            return pitch;
        }
        if(position == 0 && drawsRemaining > 1) { // snow on the remaining rounds
            double bad = approxRank(hand.getActiveRanks()) / (double) RANKS;
            if(bad > 0.98 && rng.nextDouble() > 0.95) { lastSnow = true; return pitch; } 
        }
        else {
            if(dealerDrew > 0 && active.size() == 4 && drawsRemaining < 2) { return pitch; }
        }
        for(Card c: hand.getAllCards()) {
            if(c.getRank() > 12 - drawsRemaining || !active.contains(c)) {
                pitch.add(c);
            }
        }
        return pitch;
    }
    
    public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {
        double res = result == 0 ? 0 : (result < 0 ? -Math.log(-result) : Math.log(result));
        //res = res < 0 ? -(Math.pow(-res, EXPLOSE)) : Math.pow(res, EXPWIN);
        //double res = result;
        double tgt = res;
        double wt = learningRate;
        for(int i = lastStreet; i < 4; i++) {
            double[] table = chosenTables[i];
            chosenTables[i] = null;
            int idx = chosenLines[i];
            double change = wt * ((1 - TDMCMIX) * (tgt - table[idx]) + TDMCMIX * (res - table[idx]));
            table[idx] += change;
            //wt = wt * 0.7;
            tgt = table[idx];
        }
    }
    
    public String getAgentName() {
        return "RLPlayer #" + id;
    }
    
    public String getAuthor() {
        return "Kokkarinen, Ilkka";
    }
    
    static {
        try { RLPlayer.loadTables("tt.gz"); }
        catch(Exception e) {
            System.out.println("Unable to read file tt.gz");
        }
    }
}