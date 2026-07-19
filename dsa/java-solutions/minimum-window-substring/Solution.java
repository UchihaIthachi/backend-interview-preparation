import java.util.*;

public class Solution {
    public String minWindow(String s, String t) { 
    if (s.length() < t.length()) return ""; 
    Map<Character, Integer> map = new HashMap<>(); 
    for (char c : t.toCharArray()) map.put(c, map.getOrDefault(c, 0) + 1); 
    int left = 0, count = 0, minLen = Integer.MAX_VALUE, start = 0; 
    for (int right = 0; right < s.length(); right++) { 
    char c = s.charAt(right); 
    if (map.containsKey(c)) { 
    map.put(c, map.get(c) - 1); 
    if (map.get(c) >= 0) count++; 
    } 
    while (count == t.length()) { 
    if (right - left + 1 < minLen) { 
    minLen = right - left + 1; 
    start = left; 
    } 
    char lc = s.charAt(left++); 
    if (map.containsKey(lc)) { 
     
    map.put(lc, map.get(lc) + 1); 
    if (map.get(lc) > 0) count--; 
    } 
    } 
    } 
    return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + 
    minLen); 
    }
}
