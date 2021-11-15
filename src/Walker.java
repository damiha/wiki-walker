import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Walker {

    /* administrative */
    private final Preferences prefs;
    private final HttpClient client;
    private final Random random;

    private final Node startNode;
    private final Node endNode;
    private final Map<Node, Boolean> explored;

    private final AtomicBoolean found;
    private AtomicInteger numberOfRequests;

    public Walker(String startPoint, String endPoint, Preferences prefs){

        this.prefs = prefs;
        this.client = HttpClient.newHttpClient();
        this.random = new Random(0);

        if(pageNotFound(startPoint)){
            throw new RuntimeException("ERROR: page '" + startPoint + "' couldn't be found!");
        }
        else if(pageNotFound(endPoint)){
            throw new RuntimeException("ERROR: page '" + endPoint + "' couldn't be found!");
        }

        startNode = new Node(startPoint, 0);
        endNode = new Node(endPoint);

        this.found = new AtomicBoolean();
        this.numberOfRequests = new AtomicInteger();

        /* suitable for uni- and bidirectional search */
        explored = new ConcurrentHashMap<>();

        walk();
    }

    private boolean pageNotFound(String title){

        title = Main.format(title);

        /* "HEAD" reduces data being sent from the server */
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://en.wikipedia.org/wiki/" + title))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response;

        try{
            response = client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch(IOException | InterruptedException e){
            return true;
        }

        return response.statusCode() != 200;
    }

    private void walk (){
        if(prefs.getSearchDirection() == SearchDirection.uni){
            unidirectional_walk(Direction.forward);
        }else {
            bidirectional_walk();
        }

        if(found.get()){
            printSolution(getSolution());
        }
    }

    private Stack<String> getSolution(){
        /* where to begin*/
        Node pointedTo = this.endNode.getParent() != null ? this.endNode : this.startNode;
        Stack<String> path = new Stack<>();

        while(pointedTo.getParent() != null){

            path.push(pointedTo.getTitle());
            pointedTo = pointedTo.getParent();
        }
        path.push(pointedTo.getTitle());
        return path;
    }

    private void printSolution(Stack<String> solution){
        StringBuilder sb = new StringBuilder();

        while(!solution.isEmpty()){
            sb.append(Main.indentation + solution.pop());

            if(!solution.isEmpty()){
                sb.append(" > \n");
            }
        }
        System.out.println(sb);
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

        while(!queue.isEmpty() && !found.get() && !(numberOfRequests.get() >= prefs.getMaxReq())){

            Node current = queue.poll();
            explored.put(current, true);

            List<Node> expandedNodes = expandAt(current, direction);

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
            else if(numberOfRequests.get() >= prefs.getMaxReq()){
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

    private List<Node> expandAt(Node node, Direction direction){

        List<Node> expandedNodes = new ArrayList<Node>();
        String prop = direction == Direction.forward ? "links" : "linkshere";

        HttpResponse response = null;
        try {

            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://en.wikipedia.org/w/api.php?action=query&titles="
                            + node.getURLTitle() + "&format=json&prop=" + prop + "&pllimit=" + prefs.getMaxLinks()))
                    .header("accept", "application/json")
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            numberOfRequests.incrementAndGet();

        } catch (InterruptedException | IOException | RuntimeException e) {
            /* return an empty list != null */
            return expandedNodes;
        }

        List<Object> links = getLinksFromResponse(response, direction);

        if(links == null){
            return expandedNodes;
        }

        int nodesAdded = 0;

        for (int i = 0; i < links.size(); i++){

            if(nodesAdded > prefs.getMaxLinks()){
                break;
            }

            HashMap<String, Object> map = (HashMap<String, Object>) (links.get(i));

            if(map.get("ns") == (Integer) 0) {
                String title = (String) map.get("title");
                double cost = node.getCost() + 1.0;

                Node expandedNode = new Node(title, cost);
                expandedNode.setParent(node);
                expandedNode.setDirection(direction);
                expandedNodes.add(expandedNode);

                nodesAdded++;
            }
        }
        return expandedNodes;
    }

    /* forward and backward responses have to be parsed differently */

    private List<Object> getLinksFromResponse(HttpResponse response, Direction direction){

        /* list of HashMaps<String, Object> */
        List<Object> links = null;

        if(direction == Direction.forward){
            JSONObject req = new JSONObject(response.body().toString());
            JSONObject query = req.getJSONObject("query");
            JSONObject pages = query.getJSONObject("pages");
            JSONObject page = pages.getJSONObject(pages.keys().next());

            /* page not found ? */
            if(!page.has("links")){
                return links;
            }

            links = page.getJSONArray("links").toList();
        }

        else{
            // TODO: implement for going backward ('linkshere') as well
            JSONObject req = new JSONObject(response.body().toString());
            JSONObject query = req.getJSONObject("query");
            JSONObject pages = query.getJSONObject("pages");
            JSONObject page = pages.getJSONObject(pages.keys().next());

            /* page not found ? */
            if(!page.has("linkshere")){
                return links;
            }

            links = page.getJSONArray("linkshere").toList();
        }

        /* shuffle to not always get the alphabetical order, use seed to ensure reproducible behaviour */
        Collections.shuffle(links, random);

        return links;
    }
}
