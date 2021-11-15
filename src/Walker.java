
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Walker {

    /* administrative */
    private final Preferences prefs;
    private final WalkerUtils walkerUtils;

    private final Node startNode;
    private final Node endNode;
    private final Map<Node, Boolean> explored;

    private final AtomicBoolean found;

    public Walker(String startPoint, String endPoint, Preferences prefs){

        startNode = new Node(startPoint, 0);
        endNode = new Node(endPoint);

        this.prefs = prefs;
        this.walkerUtils = new WalkerUtils(prefs, startNode, endNode);

        if(walkerUtils.pageNotFound(startPoint)){
            throw new RuntimeException("ERROR: page '" + startPoint + "' couldn't be found!");
        }
        else if(walkerUtils.pageNotFound(endPoint)){
            throw new RuntimeException("ERROR: page '" + endPoint + "' couldn't be found!");
        }

        this.found = new AtomicBoolean();

        /* suitable for uni- and bidirectional search */
        explored = new ConcurrentHashMap<>();

        walk();
    }

    private void walk (){
        if(prefs.getSearchDirection() == SearchDirection.uni){
            unidirectional_walk(Direction.forward);
        }else {
            bidirectional_walk();
        }

        if(found.get()){
            walkerUtils.printSolution();
        }
    }

    private void unidirectional_walk(Direction direction){

        Queue<Node> queue = null;
        /* depend on the direction */
        Node startNode = direction == Direction.forward ? this.startNode : this.endNode;
        Node endNode = direction == Direction.forward ? this.endNode : this.startNode;

        if(prefs.getSearchAlgorithm() == SearchAlgorithm.bfs){
            queue = new LinkedList<>();
        }else if(prefs.getSearchAlgorithm() == SearchAlgorithm.gbfs){
            // TODO: add comparator later
            queue = new PriorityQueue<>();
        }

        queue.add(startNode);

        while(!queue.isEmpty() && !found.get() && !walkerUtils.maxRequestsReached()){

            Node current = queue.poll();
            explored.put(current, true);

            List<Node> expandedNodes = walkerUtils.expandAt(current, direction);

            for(Node expanded : expandedNodes){
                /* goal test */
                if(expanded.equals(endNode)){
                    expanded.setParent(current);
                    endNode.setParent(current);
                    found.set(true);
                    return;
                }

                if(explored.get(expanded) == null){
                    expanded.setParent(current);
                    queue.add(expanded);
                }
                else {
                    /* already explored - from the other side ? */
                    Direction counterPart = direction.getCounterPart();

                    if(expanded.getDirection() == counterPart){
                        found.set(true);
                        return;
                    }
                }
            }
        }
        if(!found.get()){
            if(queue.isEmpty()){
                System.out.println(Main.indentation + "[DEAD END]");
            }
            else if(walkerUtils.maxRequestsReached()){
                System.out.println(Main.indentation + "[TIME OUT]");
            }
        }
    }

    private void bidirectional_walk(){
        // TODO: implement as backward chaining
        Thread forward_search = new Thread(() -> unidirectional_walk(Direction.forward));
        Thread backward_search = new Thread(() -> unidirectional_walk(Direction.backward));

        forward_search.start();
        backward_search.start();

        try {
            forward_search.join();
            backward_search.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
