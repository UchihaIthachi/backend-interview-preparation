import java.util.*;

class MyStack { 
Queue<Integer> queue = new LinkedList<>(); 
public void push(int x) { 
queue.add(x); 
for (int i = 1; i < queue.size(); i++) { 
queue.add(queue.poll()); 
} 
} 
public int pop() { 
return queue.poll(); 
} 
public int top() { 
return queue.peek(); 
} 
public boolean empty() { 
return queue.isEmpty(); 
} 
}
