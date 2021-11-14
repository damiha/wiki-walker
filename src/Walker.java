import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Walker {

    /* administrative */
    private final Preferences prefs;
    private final HttpClient client;
    private final Random random;

    private final Node startNode;
    private Node endNode;

    public Walker(String startPoint, String endPoint, Preferences prefs){

        this.prefs = prefs;
        this.client = HttpClient.newHttpClient();
        this.random = new Random(0);

        startPoint = format(startPoint);
        endPoint = format(endPoint);


        if(pageNotFound(startPoint)){
            throw new RuntimeException("ERROR: page '" + startPoint + "' couldn't be found!");
        }
        else if(pageNotFound(endPoint)){
            throw new RuntimeException("ERROR: page '" + endPoint + "' couldn't be found!");
        }

        startNode = new Node(startPoint, 0);
        endNode = new Node(endPoint);

        walk();
    }

    private String format(String string){
        return string.replace(" ", "_");
    }

    private boolean pageNotFound(String title){

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
            unidirectional_walk();
        }else {
            bidirectional_walk();
        }
    }

    private void unidirectional_walk(){
        System.out.println(expandAt(startNode));
    }

    private void bidirectional_walk(){
        // TODO: implement as backward chaining
    }

    private List<Node> expandAt(Node node){

        List<Node> expandedNodes = new ArrayList<Node>();

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://en.wikipedia.org/w/api.php?action=parse&page="
                        + node.getTitle() + "&format=json&prop=links&"))
                .header("accept", "application/json")
                .build();

        HttpResponse response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            /* return an empty list != null */
            return expandedNodes;
        }

        JSONObject req = new JSONObject(response.body().toString());
        JSONObject parse = req.getJSONObject("parse");

        /* list of HashMaps<String, Object> */
        List<Object> links = parse.getJSONArray("links").toList();

        /* shuffle to not always get the alphabetical order, use seed to ensure reproducible behaviour */
        Collections.shuffle(links, random);

        int nodesAdded = 0;

        for (int i = 0; i < links.size(); i++){

            if(nodesAdded > prefs.getMaxLinksAtNode()){
                break;
            }

            HashMap<String, Object> map = (HashMap<String, Object>) (links.get(i));

            if(map.get("ns") == (Integer) 0) {
                String title = (String) map.get("*");
                double cost = node.getCost() + 1.0;

                Node expandedNode = new Node(title, cost);
                expandedNodes.add(expandedNode);

                nodesAdded++;
            }
        }
        return expandedNodes;
    }
}
