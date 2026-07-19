import java.util.*;

public class Solution {
    public int countComponents(int n, int[][] edges) { 
    UnionFind uf = new UnionFind(n); 
    for (int[] edge : edges) uf.union(edge[0], edge[1]); 
    Set<Integer> uniqueParents = new HashSet<>(); 
    for (int i = 0; i < n; i++) uniqueParents.add(uf.find(i)); 
    return uniqueParents.size(); 
    }
}


class UnionFind { 
private int[] parent; 
public UnionFind(int n) { 
parent = new int[n]; 
for (int i = 0; i < n; i++) parent[i] = i; 
} 
public int find(int x) { 
if (parent[x] != x) parent[x] = find(parent[x]); 
return parent[x]; 
} 
public boolean union(int x, int y) { 
int rootX = find(x), rootY = find(y); 
if (rootX == rootY) return false; 
parent[rootX] = rootY; 
return true; 
} 
} 
