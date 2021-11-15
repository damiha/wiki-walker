import java.util.HashSet;
import java.util.Set;

public class Preferences {

    private SearchAlgorithm searchAlgorithm;
    private SearchDirection searchDirection;
    private final Set<Heuristic> heuristics;

    private boolean verbose;

    private int maxLinks;
    private int maxCategories;
    private int maxReq;

    public Preferences(){

        searchAlgorithm = SearchAlgorithm.gbfs;
        searchDirection = SearchDirection.uni;
        heuristics = new HashSet<Heuristic>();

        heuristics.add(Heuristic.hamming);

        maxCategories = 3; /* range  [2; 5] more is not feasible */
        maxLinks = 10; /* range [5; 500] */
        maxReq = 200; /* range [100; 1000] */
        verbose = true;
    }

    public void setPref(String variableString, String valueString){

        switch(variableString){
            case "verbose":
                verbose = Boolean.parseBoolean(valueString);
                return;
            case "max_links":
                maxLinks = mapToRange(Integer.parseInt(valueString), 5, 500);
                return;
            case "max_categories":
                maxCategories = mapToRange(Integer.parseInt(valueString), 2, 10);
                return;
            case "max_req":
                maxReq = mapToRange(Integer.parseInt(valueString), 100, 1000);
                return;
            case "search_dir":
                searchDirection = SearchDirection.valueOf(valueString);
                return;
            case "search":
                searchAlgorithm = SearchAlgorithm.valueOf(valueString);
                return;
        }

        /* assigment must concern heuristics */
        boolean addHeuristic = Boolean.valueOf(valueString);

        if(addHeuristic){
            heuristics.add(Heuristic.valueOf(variableString));
        }
        else {
            heuristics.remove(Heuristic.valueOf(variableString));
        }
    }

    private int mapToRange(int value, int from, int to){
        if(value < from){
            value = from;
        }else if(value > to){
            value = to;
        }
        return value;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(Main.indentation + "verbose: " + verbose + "\n");
        sb.append(Main.indentation + "search: " + searchAlgorithm + "\n");
        sb.append(Main.indentation + "search_dir: " + searchDirection + "\n");
        sb.append(Main.indentation + "max_links: " + maxLinks + "\n");
        sb.append(Main.indentation + "max_categories: " + maxCategories + "\n");
        sb.append(Main.indentation + "max_req: " + maxReq + "\n");

        sb.append("\n" + Main.indentation + "heuristics:\n");
        for(Heuristic heuristic : Heuristic.values()){

            boolean status = heuristics.contains(heuristic);
            sb.append(Main.indentation + " - " + heuristic.toString().toLowerCase() + ": " + status + "\n");
        }
        return sb.toString();
    }

    public SearchAlgorithm getSearchAlgorithm() {
        return searchAlgorithm;
    }

    public SearchDirection getSearchDirection() {
        return searchDirection;
    }

    public int getMaxLinks(){
        return maxLinks;
    }

    public int getMaxReq(){
        return maxReq;
    }

    public boolean mostCategoriesMatchingEnabled(){
        return heuristics.contains(Heuristic.most_categories);
    }

    public boolean longestSubstringEnabled(){
        return heuristics.contains(Heuristic.longest_substring);
    }

    public boolean hammingEnabled(){
        return heuristics.contains(Heuristic.hamming);
    }

    public boolean isVerbose(){
        return verbose;
    }

    public int getMaxCategories(){
        return maxCategories;
    }
}
