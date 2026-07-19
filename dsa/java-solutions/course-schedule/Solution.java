import java.util.*;

public class Solution {
    public boolean canFinish(int numCourses, int[][] prerequisites) { 
    List<List<Integer>> graph = new ArrayList<>(); 
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>()); 
    int[] inDegree = new int[numCourses]; 
    for (int[] prereq : prerequisites) { 
    graph.get(prereq[1]).add(prereq[0]); 
    inDegree[prereq[0]]++; 
    } 
    Queue<Integer> queue = new LinkedList<>(); 
    for (int i = 0; i < numCourses; i++) if (inDegree[i] == 0) 
    queue.add(i); 
    int count = 0; 
    while (!queue.isEmpty()) { 
    int course = queue.poll(); 
    count++; 
    for (int next : graph.get(course)) { 
    if (--inDegree[next] == 0) queue.add(next); 
    } 
    } 
    return count == numCourses; 
    }
}
