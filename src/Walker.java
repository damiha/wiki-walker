import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Walker {

    private HttpClient client;
    private String startPoint, endPoint;

    public Walker(String startPoint, String endPoint){

        client = HttpClient.newHttpClient();

        this.startPoint = getFormatted(startPoint);
        this.endPoint = getFormatted(endPoint);

        if(!pageFound(this.startPoint)){
            throw new RuntimeException("ERROR: page '" + startPoint + "' couldn't be found!");
        }
        else if(!pageFound(this.endPoint)){
            throw new RuntimeException("ERROR: page '" + endPoint + "' couldn't be found!");
        }
    }

    private String getFormatted(String string){
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    private boolean pageFound(String title){

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://en.wikipedia.org/w/api.php?action=parse&page="+title))
                .header("accept", "application/json")
                .build();

        // use the client to send the request
        HttpResponse response;
        try{
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch(IOException | InterruptedException e){
            return false;
        }
        return response.statusCode() == 200;
    }

    public static void printLinksFromWikiTitle(String title) throws IOException, InterruptedException {

        /*
        JSONObject req = new JSONObject(response.body());
        JSONObject parse = req.getJSONObject("parse");

        JSONArray links = parse.getJSONArray("links");

        // the response:
        for (int i = 0; i < links.length(); i++){
            JSONObject jsonObject = (JSONObject)(links.get(i));

            if(jsonObject.getInt("ns") == 0) {
                System.out.println(jsonObject.getString("*"));
            }
        }
         */
    }
}
