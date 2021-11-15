import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WalkerUtils {

    private final HttpClient client;
    private final Preferences prefs;
    private final Node startNode, endNode;

    private final Random random;
    private final AtomicInteger numberOfRequests;

    private final Map<Node, Boolean> explored;

    public Set<String> spamMarkers;

    public WalkerUtils(Preferences prefs, Map<Node, Boolean> explored, Node startNode, Node endNode){

        this.client = HttpClient.newHttpClient();
        this.random = new Random(System.currentTimeMillis());
        this.numberOfRequests = new AtomicInteger();

        this.prefs = prefs;
        this.explored = explored;
        this.startNode = startNode;
        this.endNode = endNode;

        spamMarkers = new HashSet<>(List.of("identifiers", "clean up", "all", "articles", "video", "description", "redirects", "pages", "wiki"));
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
                            + node.getURLTitle() + "&format=json&prop=" + prop + "&pllimit=max"))
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

        for (int i = 0; i < links.size(); i++){

            HashMap<String, Object> map = (HashMap<String, Object>) (links.get(i));

            if(map.get("ns") == (Integer) 0) {
                String title = (String) map.get("title");

                Node expandedNode = new Node(title);

                /* already seen - no further need to expand */
                if(explored.containsKey(expandedNode)){
                    continue;
                }

                /* new value - go on */
                expandedNode.setParent(node);
                expandedNode.setDirection(direction);

                if(prefs.mostCategoriesMatchingEnabled()){

                    setCategoriesTo(expandedNode);
                }
                assignCostsTo(expandedNode, direction);
                expandedNodes.add(expandedNode);
            }
        }
        return expandedNodes;
    }

    public void setCategoriesTo(Node node){

        HttpResponse response = null;
        try {
            /* asking for ALL categories not that more expensive, keep most valuable */
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://en.wikipedia.org/w/api.php?action=query&titles="
                            + node.getURLTitle() + "&format=json&prop=categories&cllimit=max"))
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

        /* shuffle to not always get the alphabetical order*/
        Collections.shuffle(categories, random);

        /* filter out non-categories and generic categories e.g. category: article with video */
        Set<String> categoryObjects =   categories.stream()
                .map(obj -> (HashMap<String, Object>) obj)
                .filter(obj -> obj.get("ns") == (Integer)14)
                .filter(obj -> {
                    String title = ((String)obj.get("title")).toLowerCase();

                    for(String marker : spamMarkers){
                        if(title.contains(marker))
                            return false;
                    }
                    return true;
                }).map(obj -> ((String)obj.get("title"))).limit(prefs.getMaxCategories()).collect(Collectors.toSet());

        // add categories all at ones
        node.setCategories(categoryObjects);
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
        int linksPresent = Math.min(links.size(), prefs.getMaxLinks());
        links  = links.subList(0, linksPresent);

        return links;
    }

    public void assignCostsTo(Node node, Direction direction){

        double cost = 0.0;
        double hits = 0.0;

        // TODO: optimize the heuristic and the cost function
        if(prefs.longestSubstringEnabled()){
            hits +=  getLongestSubstring(node, getEndNodeFrom(direction));
        }

        if(prefs.mostCategoriesMatchingEnabled()){

            hits += getNumberCategoriesMatching(node, getEndNodeFrom(direction));
        }

        if(prefs.hammingEnabled()){

            /* prefer small hamming distance */
            int hammingDistance = getHammingDistance(node, getEndNodeFrom(direction));
            hits += (1.0 / hammingDistance);
        }

        node.setCost(1000.0 / (hits * hits));
    }

    public int getNumberCategoriesMatching(Node node, Node goalNode){
        int num = 0;

        for(String goalCategory : goalNode.getCategories()){
            if(node.getCategories().contains(goalCategory)){
                num++;
            }
        }
        return num;
    }

    public int getHammingDistance(Node node, Node goalNode){

        String nodeTitle = node.getCanonicalTitle();
        String goalTitle = goalNode.getCanonicalTitle();

        int compareLength = Math.min(nodeTitle.length(), goalTitle.length());
        int lengthDifference = Math.max(nodeTitle.length(), goalTitle.length()) - compareLength;

        int hammingDistance = 0;
        for(int i = 0; i < compareLength; i++){
            hammingDistance += nodeTitle.charAt(i) != goalTitle.charAt(i) ? 1 : 0;
        }
        return hammingDistance + lengthDifference;
    }

    public int getLongestSubstring(Node node, Node goalNode){

        return longestSubstr(node.getCanonicalTitle(), goalNode.getCanonicalTitle());
    }

    public Node getStartNodeFrom(Direction direction){
        return direction == Direction.forward ? this.startNode : this.endNode;
    }

    public Node getEndNodeFrom(Direction direction){
        return direction == Direction.forward ? this.endNode : this.startNode;
    }

    public Comparator<Node> getCostComparator(){
        return new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Double.compare(o1.getCost(), o2.getCost());
            }
        };
    }

    /* COPIED FROM WIKIBOOKS - minimize number of calls to this function */
    private int longestSubstr(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl][sl];

        for (int i = 0; i < fl; i++) {
            for (int j = 0; j < sl; j++) {
                if (first.charAt(i) == second.charAt(j)) {
                    if (i == 0 || j == 0) {
                        table[i][j] = 1;
                    }
                    else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
            }
        }
        return maxLen;
    }
}
