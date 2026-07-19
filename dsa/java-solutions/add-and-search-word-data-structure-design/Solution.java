import java.util.*;

class WordDictionary { 
private TrieNode root; 
public WordDictionary() { 
root = new TrieNode(); 
} 
public void addWord(String word) { 
TrieNode node = root; 
for (char c : word.toCharArray()) { 
if (!node.containsKey(c)) node.put(c, new TrieNode()); 
node = node.get(c); 
} 
node.setEnd(); 
} 
public boolean search(String word) { 
return search(word, 0, root); 
} 
private boolean search(String word, int index, TrieNode node) { 
 
if (index == word.length()) return node.isEnd(); 
char c = word.charAt(index); 
if (c == '.') { 
for (char ch = 'a'; ch <= 'z'; ch++) { 
if (node.containsKey(ch) && search(word, index + 1, 
node.get(ch))) return true; 
} 
return false; 
} else { 
return node.containsKey(c) && search(word, index + 1, 
node.get(c)); 
} 
} 
}


class TrieNode { 
private TrieNode[] links; 
private final int R = 26; 
private boolean isEnd; 
public TrieNode() { 
links = new TrieNode[R]; 
} 
public boolean containsKey(char ch) { 
return links[ch - 'a'] != null; 
} 
public TrieNode get(char ch) { 
return links[ch - 'a']; 
} 
public void put(char ch, TrieNode node) { 
links[ch - 'a'] = node; 
} 
public void setEnd() { 
isEnd = true; 
} 
public boolean isEnd() { 
return isEnd; 
} 
} 
