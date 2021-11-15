import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WalkerUtils {

    private final HttpClient client;
    private final Preferences prefs;
    private final Node startNode, endNode;

    private final Random random;
    private final AtomicInteger numberOfRequests;

    public WalkerUtils(Preferences prefs, Node startNode, Node endNode){

        this.client = HttpClient.newHttpClient();
        this.random = new Random(0);
        this.numberOfRequests = new AtomicInteger();

        this.prefs = prefs;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public boolean pageNotFound(String title){

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

    public boolean maxRequestsReached(){
        return numberOfRequests.get() >= prefs.getMaxReq();
    }

    public int getNumberRequests(){
        return numberOfRequests.get();
    }

    public Stack<String> getSolution(){
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

    public void printSolution(){

        Stack<String> solution = getSolution();
        StringBuilder sb = new StringBuilder();

        while(!solution.isEmpty()){
            sb.append(Main.indentation + solution.pop());

            if(!solution.isEmpty()){
                sb.append(" > \n");
            }
        }
        System.out.println(sb);
    }

    public List<Node> expandAt(Node node, Direction direction){

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

                if(prefs.mostCategoriesMatchingEnabled()){
                    // TODO: DON'T SET CATEGORIES WHEN ALREADY IN THE EXPLORED SET
                    setCategoriesTo(expandedNode);
                }

                expandedNodes.add(expandedNode);

                nodesAdded++;
            }
        }
        return expandedNodes;
    }

    private void setCategoriesTo(Node node){

        HttpResponse response = null;
        try {

            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://en.wikipedia.org/w/api.php?action=query&titles="
                            + node.getURLTitle() + "&format=json&prop=categories&cllimit=" + prefs.getMaxCategories()))
                    .header("accept", "application/json")
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            numberOfRequests.incrementAndGet();

        } catch (InterruptedException | IOException | RuntimeException e) {
            /* something went totally wrong */
            return;
        }

        JSONObject req = new JSONObject(response.body().toString());
        JSONObject query = req.getJSONObject("query");
        JSONObject pages = query.getJSONObject("pages");
        JSONObject page = pages.getJSONObject(pages.keys().next());

        /* page not found ? */
        if(!page.has("categories")){
            return;
        }
        List<Object> categories = page.getJSONArray("categories").toList();

        int categoriesAdded = 0;

        // add category
        for (int i = 0; i < categories.size(); i++){

            if(categoriesAdded > prefs.getMaxCategories()){
                break;
            }

            HashMap<String, Object> map = (HashMap<String, Object>) (categories.get(i));

            // TODO: filter out generic categories e.g. category: article with video

            if(map.get("ns") == (Integer) 14) {

                node.addCategory((String) map.get("title"));
                categoriesAdded++;
            }
        }
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

    public Comparator<Node> getCostComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Double.compare(o1.getCost(), o2.getCost());
            }
        };
    }
}
