
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Walker {

    /* administrative */
    private final Preferences prefs;
    private final Statistics stats;
    private final WalkerUtils walkerUtils;

    private final Node startNode;
    private final Node endNode;
    private final Map<Node, Boolean> explored;

    private final AtomicBoolean found;

    public Walker(Preferences prefs, Statistics stats, String startPoint, String endPoint){


        /* suitable for multi-threaded search */
        this.found = new AtomicBoolean();
        explored = new ConcurrentHashMap<>();

        startNode = new Node(startPoint);
        endNode = new Node(endPoint);

        this.prefs = prefs;
        this.stats = stats;
        this.walkerUtils = new WalkerUtils(prefs, explored, startNode, endNode);

        if(walkerUtils.pageNotFound(startPoint)){
            throw new RuntimeException("ERROR: page '" + startPoint + "' couldn't be found!");
        }
        else if(walkerUtils.pageNotFound(endPoint)){
            throw new RuntimeException("ERROR: page '" + endPoint + "' couldn't be found!");
        }

        if(prefs.mostCategoriesMatchingEnabled()){
            walkerUtils.setCategoriesTo(endNode);

            /* when bidirectional, startNode's categories must be also considered */
            if(prefs.getSearchDirection() == SearchDirection.bi){
                walkerUtils.setCategoriesTo(startNode);
            }
        }

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
        stats.setNumberOfRequests(walkerUtils.getNumberRequests());
    }

    private void unidirectional_walk(Direction direction){

        Queue<Node> queue = null;
        /* depend on the direction */
        Node startNode = walkerUtils.getStartNodeFrom(direction);
        Node endNode = walkerUtils.getEndNodeFrom(direction);

        if(prefs.getSearchAlgorithm() == SearchAlgorithm.bfs){
            queue = new LinkedList<>();
        }else if(prefs.getSearchAlgorithm() == SearchAlgorithm.gbfs){

            queue = new PriorityQueue<>(walkerUtils.getCostComparator());
        }

        queue.add(startNode);

        while(!queue.isEmpty() && !found.get() && !walkerUtils.maxRequestsReached()){

            Node current = queue.poll();
            explored.put(current, true);

            if(prefs.isVerbose()){
                System.out.println(Main.indentation + "\\" + current);
            }

            List<Node> expandedNodes = walkerUtils.expandAt(current, direction);

            for(Node expanded : expandedNodes){

                if(explored.get(expanded) != null){
                    /* already explored - from the other side of bidirectional search ? */
                    Direction counterPart = direction.getCounterPart();

                    if(expanded.getDirection() == counterPart){
                        found.set(true);
                        return;
                    }
                }
                /* goal test */
                else if(expanded.equals(endNode)){
                    expanded.setParent(current);
                    endNode.setParent(current);
                    found.set(true);
                    return;
                }
                /* neither explored nor goal - add */
                else{
                    expanded.setParent(current);
                    queue.add(expanded);
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
