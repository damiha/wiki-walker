import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Main {

    public static String indentation = "       ";
    private final Scanner scanner;
    private boolean running;

    private Preferences prefs;

    public Main(){
        scanner = new Scanner(System.in);
        running = true;

        prefs = new Preferences();
    }

    public void printHeader(){
        System.out.println("--- WIKI-WALKER ---");
    }

    private void printHelp(){
        System.out.println(indentation + "help:" + indentation + "prints this message.");
        System.out.println(indentation + "walk:" + indentation + "reads start- and endpoint. executes the wiki-walk.");
        System.out.println(indentation + "pref:" + indentation + "prints the current preferences.");
        System.out.println(indentation + "stat:" + indentation + "prints metrics (e.g. #requests, execution time) of the last walk.");
        System.out.println(indentation + "quit:" + indentation + "quits the application.");
    }

    private void quit(){
        running = false;
    }

    private void walk(){
        try{
            String startPoint = getStartPoint();
            String endPoint = getEndPoint();

            Walker walker = new Walker(startPoint, endPoint);

        }catch(RuntimeException e){
            System.out.println(indentation + e.getMessage());
        }
    }

    private void printStats(){
        System.out.println("printing stats...");
    }

    private void printPrefs(){
        System.out.println(prefs);
    }

    public boolean isRunning(){
        return running;
    }

    public Command getCommandFrom(String inputString){
        switch (inputString){
            case "help":
                return this::printHelp;
            case "walk":
                return this::walk;
            case "stat":
                return this::printStats;
            case "pref":
                return this::printPrefs;
            case "quit":
                return this::quit;
            default:
                return () -> System.out.println(indentation + "ERROR: invalid command. Type 'help'.");
        }
    }

    private String getStartPoint(){
        System.out.print(indentation + "start: ");
        return scanner.nextLine();
    }

    private String getEndPoint(){
        System.out.print(indentation + "end: ");
        return scanner.nextLine();
    }

    public String getInputString(){
        System.out.print("$: ");
        return scanner.nextLine();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Main main = new Main();
        main.printHeader();

        while(main.isRunning()){
            String inputString = main.getInputString();
            Command command = main.getCommandFrom(inputString);
            command.execute();
        }
    }
}
