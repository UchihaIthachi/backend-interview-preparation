import java.util.*;

public class Solution {
    public boolean isPalindrome(String s) { 
    int left = 0, right = s.length() - 1; 
    while (left < right) { 
    if (s.charAt(left++) != s.charAt(right--)) return false; 
    } 
    return true; 
    }
}
