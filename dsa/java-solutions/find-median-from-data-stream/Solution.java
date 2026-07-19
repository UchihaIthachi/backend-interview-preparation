import java.util.*;

class MedianFinder { 
private PriorityQueue<Integer> small = new 
PriorityQueue<>(Collections.reverseOrder()); 
private PriorityQueue<Integer> large = new PriorityQueue<>(); 
public void addNum(int num) { 
small.add(num); 
large.add(small.poll()); 
if (small.size() < large.size()) small.add(large.poll()); 
 
} 
public double findMedian() { 
if (small.size() > large.size()) return small.peek(); 
return (small.peek() + large.peek()) / 2.0; 
} 
}
