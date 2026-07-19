import java.util.*;

public class Solution {
    public int longestConsecutive(int[] nums) { 
    Set<Integer> set = new HashSet<>(); 
    for (int num : nums) set.add(num); 
    int maxStreak = 0; 
    for (int num : nums) { 
    if (!set.contains(num - 1)) { 
    int currentNum = num; 
    int streak = 1; 
    while (set.contains(currentNum + 1)) { 
    currentNum++; 
    streak++; 
    } 
    maxStreak = Math.max(maxStreak, streak); 
    } 
    } 
    return maxStreak; 
    }
}
