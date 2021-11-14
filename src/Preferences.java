import java.util.HashSet;
import java.util.Set;

public class Preferences {

    private SearchAlgorithm searchAlgorithm;
    private SearchDirection searchDirection;
    private final Set<Heuristic> heuristics;
    private int maxLinksAtNode;

    public Preferences(){

        searchAlgorithm = SearchAlgorithm.breadth_first_search;
        searchDirection = SearchDirection.unidirectional;
        heuristics = new HashSet<Heuristic>();

        maxLinksAtNode = 10; /* range [5; 500] */
    }

    public void setPref(String variableString, String valueString){

        switch(variableString){
            case "max_links_at_node":
                int newValue = Integer.parseInt(valueString);

                if(newValue < 5){
                    newValue = 5;
                }else if(newValue > 500){
                    newValue = 500;
                }

                maxLinksAtNode = newValue;
                return;
            case "search_direction":
                searchDirection = SearchDirection.valueOf(valueString);
            case "search_algorithm":
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

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(Main.indentation + "search_algorithm: " + searchAlgorithm + "\n");
        sb.append(Main.indentation + "search_direction: " + searchDirection + "\n");
        sb.append(Main.indentation + "max_links_at_node: " + maxLinksAtNode + "\n");

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

    public int getMaxLinksAtNode(){
        return maxLinksAtNode;
    }
}
