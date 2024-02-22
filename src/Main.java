import java.util.*;

public class Main {
    public static void main(String[] args) {
//        int profit = maxProfit(new int[]{11, 2, 7, 1, 4});
//        System.out.println(profit);
        //System.out.println(bestClosingTime("YYNY"));
        int[][] matrix = {{-48}};
        System.out.println(minFallingPathSum(matrix));
    }

    static int maxProfit(int[]  prices){
        int maxVal = Integer.MIN_VALUE;
        int minVal = Integer.MAX_VALUE;
        int maxIdx = 0; int minIdx = 0;
        for(int i = 0; i<prices.length; i++){
            if(prices[i]>=maxVal){
                maxVal = prices[i];
                maxIdx = i;
            }

            if(prices[i]<minVal){
                minVal = prices[i];
                minIdx = i;
            }
        }

        if(maxIdx>minIdx) return maxVal - minVal;

        else{
            int maxDiff = 0;
            for(int i = 0; i<maxIdx; i++){
                int diff = maxVal - prices[i];
                if(diff>maxDiff) maxDiff = diff;
            }

            for(int i = maxIdx; i<minIdx; i++){
                for(int j = i; j<minIdx; j++){
                    if((prices[j] - prices[i])>maxDiff) maxDiff = prices[j] - prices[i];
                }
            }

            for(int i = minIdx; i<prices.length; i++){
                int diff = prices[i] - minVal;
                if(diff>maxDiff) maxDiff = diff;
            }
            return maxDiff;
        }
    }

    public static boolean closeStrings(String word1, String word2) {
        if(word1.length() != word2.length()) return false;
        HashMap<Character, Integer> charCount1 = new HashMap<>();
        HashMap<Character, Integer> charCount2 = new HashMap<>();
        for(int i = 0; i<word1.length(); i++){
            char curr = word1.charAt(i);
            if(charCount1.containsKey(curr)){
                charCount1.put(curr, charCount1.get(curr)+1);
            }

            else{
                charCount1.put(curr, 1);
            }
        }

        for(int i = 0; i<word2.length(); i++){
            char curr = word2.charAt(i);
            if(!charCount1.containsKey(curr)) return false;
            if(charCount2.containsKey(curr)){
                charCount2.put(curr, charCount2.get(curr)+1);
            }

            else{
                charCount2.put(curr, 1);
            }

        }
        if(charCount1.size()!=charCount2.size()) return false;
        LinkedList<Integer>counts = new LinkedList<>(charCount1.values());
        for(Integer i: charCount2.values()){
            if(!counts.contains(i)) return false;
            else{
                counts.remove(i);
            }
        }
        if(counts.size()!=0) return false;
        return true;

    }

    public static int bestClosingTime(String customers) {
        int reward = 0;
        int bestHour = 0;
        int maxReward = 0;
        for(int i = 0; i<customers.length(); i++){
            if(customers.charAt(i)=='Y') reward++;
            else if (customers.charAt(i)=='N') {
                reward--;
            }
            if(reward>maxReward){
                maxReward = reward;
                bestHour = i+1;
            }
        }
        return bestHour;
    }

    public static List<List<Integer>> findWinners(int[][] matches) {
            HashSet<Integer> winners = new HashSet<>();
            HashSet<Integer> lostOne = new HashSet<>();
            HashSet<Integer> others = new HashSet<>();

            for(int[]tuple: matches){
                int winner = tuple[0];
                int loser = tuple[1];
                if(!(lostOne.contains(winner) || others.contains(winner))){
                    winners.add(winner);
                }

                if(others.contains(loser)){
                    continue;
                }

                else if(winners.contains(loser)){
                    winners.remove(loser);
                    lostOne.add(loser);
                }

                else if(lostOne.contains(loser)){
                    lostOne.remove(loser);
                    others.add(loser);
                }

                else{
                    lostOne.add(loser);
                }
            }

            List<List<Integer>> ans = new ArrayList<>(2);
            ArrayList<Integer> winnersList = new ArrayList<>(winners);
            Collections.sort(winnersList);
            ans.add(winnersList);
            ArrayList<Integer> lostOneList = new ArrayList<>(lostOne);
            Collections.sort(lostOneList);
            ans.add(lostOneList);
            return ans;
    }

//    static int minSum = Integer.MAX_VALUE;
//    static int numRows, numCols;
//    public static int minFallingPathSum(int[][] matrix) {
//        numRows = matrix.length;
//        numCols = matrix[0].length;
//        for(int i = 0; i<numCols; i++){
//            checkSum(0, i, 0, matrix);
//        }
//        return minSum;
//    }
//
//    static void checkSum(int r, int c, int currentSum, int[][] matrix){
//        currentSum += matrix[r][c];
//        if(currentSum<minSum && r==numRows-1) minSum = currentSum;
//
//        if(r+1<numRows){
//            if(c-1>=0)checkSum(r+1, c-1, currentSum, matrix);
//
//            checkSum(r+1, c, currentSum, matrix);
//
//            if(c+1<numCols) checkSum(r+1, c+1, currentSum, matrix);
//        }
//    }

    //slightly better
//public static int minFallingPathSum(int[][] matrix) {
//    int numRows = matrix.length;
//    int numCols = matrix[0].length;
//    int[][] memo = new int[numRows][numCols];
//
//    int minSum = Integer.MAX_VALUE;
//    for (int i = 0; i < numCols; i++) {
//        minSum = Math.min(minSum, checkSum(0, i, matrix, memo));
//    }
//
//    return minSum;
//}
//
//    static int checkSum(int r, int c, int[][] matrix, int[][] memo) {
//        if (r == matrix.length - 1) {
//            return matrix[r][c];
//        }
//
//        if (memo[r][c] != 0) {
//            return memo[r][c];
//        }
//
//        int minBelow = checkSum(r + 1, c, matrix, memo);
//        int minLeft = (c > 0) ? checkSum(r + 1, c - 1, matrix, memo) : Integer.MAX_VALUE;
//        int minRight = (c < matrix[0].length - 1) ? checkSum(r + 1, c + 1, matrix, memo) : Integer.MAX_VALUE;
//
//        int currentSum = matrix[r][c] + Math.min(minBelow, Math.min(minLeft, minRight));
//        memo[r][c] = currentSum;
//
//        return currentSum;
//    }
    public static int minFallingPathSum(int[][] matrix) {
        int numRows = matrix.length;
        int numCols = matrix[0].length;

        // Create a memoization table to store intermediate results
        int[][] dp = new int[numRows][numCols];

        // Copy the first row of the matrix to the memoization table
        System.arraycopy(matrix[0], 0, dp[0], 0, numCols);

        // Iterate through the matrix starting from the second row
        for (int r = 1; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                // Calculate the minimum falling path sum for the current cell
                dp[r][c] = matrix[r][c] + Math.min(
                        dp[r - 1][c],
                        Math.min(dp[r - 1][Math.max(0, c - 1)], dp[r - 1][Math.min(numCols - 1, c + 1)])
                );
            }
        }

        // Find the minimum value in the last row of the memoization table
        int minSum = dp[numRows - 1][0];
        for (int c = 1; c < numCols; c++) {
            minSum = Math.min(minSum, dp[numRows - 1][c]);
        }

        return minSum;
    }


}
