import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Walker {

    /* administrative */
    private final Preferences prefs;
    private final HttpClient client;
    private final Random random;

    private final Node startNode;
    private final Node endNode;
    private final Map<Node, Boolean> explored;

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
        if(prefs.getSearchDirection() == SearchDirection.unidirectional){
            unidirectional_walk(Direction.forward);
        }else {
            bidirectional_walk();
        }
    }

    private void unidirectional_walk(Direction direction){
        Queue<Node> queue = null;

        if(prefs.getSearchAlgorithm() == SearchAlgorithm.breadth_first_search){
            queue = new LinkedList<>();
        }else if(prefs.getSearchAlgorithm() == SearchAlgorithm.greedy_best_first_search){
            // TODO: add comparator later
            queue = new PriorityQueue<>();
        }
        queue.add(startNode);

        while(!queue.isEmpty()){

            Node current = queue.poll();
            explored.put(current, true);

            System.out.println(current);

            List<Node> expandedNodes = expandAt(current, direction);

            for(Node expanded : expandedNodes){
                /* goal test */
                if(expanded.equals(endNode)){
                    System.out.println("HEUREKA: found " + expanded);
                    return;
                }

                if(explored.get(expanded) == null){
                    expanded.setParent(current);
                    expanded.setDirection(direction);
                    queue.add(expanded);
                }
                else {
                    Direction counterPart = direction.getCounterPart();

                    if(expanded.getDirection() == counterPart){
                        System.out.println("FOUND CONNECTION");
                        return;
                    }
                }
            }
        }
    }

    private void bidirectional_walk(){
        // TODO: implement as backward chaining
    }

    private List<Node> expandAt(Node node, Direction direction){

        List<Node> expandedNodes = new ArrayList<Node>();
        String prop = direction == Direction.forward ? "links" : "linkshere";

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://en.wikipedia.org/w/api.php?action=query&titles="
                        + node.getURLTitle() + "&format=json&prop=" + prop + "&pllimit=" + prefs.getMaxLinksAtNode()))
                .header("accept", "application/json")
                .build();

        HttpResponse response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            /* return an empty list != null */
            return expandedNodes;
        }

        List<Object> links = getLinksFromResponse(response, direction);

        if(links == null){
            return expandedNodes;
        }

        int nodesAdded = 0;

        for (int i = 0; i < links.size(); i++){

            if(nodesAdded > prefs.getMaxLinksAtNode()){
                break;
            }

            HashMap<String, Object> map = (HashMap<String, Object>) (links.get(i));

            if(map.get("ns") == (Integer) 0) {
                String title = (String) map.get("title");
                double cost = node.getCost() + 1.0;

                Node expandedNode = new Node(title, cost);
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
        }

        /* shuffle to not always get the alphabetical order, use seed to ensure reproducible behaviour */
        Collections.shuffle(links, random);

        return links;
    }
}
