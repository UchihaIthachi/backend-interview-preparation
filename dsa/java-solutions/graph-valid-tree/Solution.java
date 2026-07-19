import java.util.*;

public class Solution {
    public boolean validTree(int n, int[][] edges) { 
if (edges.length != n - 1) return false; 
UnionFind uf = new UnionFind(n); 
for (int[] edge : edges) { 
if (!uf.union(edge[0], edge[1])) return false; 
} 
return true; 
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
