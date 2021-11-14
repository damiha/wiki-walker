import java.util.HashSet;
import java.util.Set;

enum SearchDirection{
    UNIDIRECTIONAL, BIDIRECTIONAL;
}

enum Heuristic{
    MOST_CATEGORIES_MATCHING;
}

public class Preferences {

    private SearchDirection searchDirection;
    private Set<Heuristic> heuristics;

    public Preferences(){
        searchDirection = SearchDirection.UNIDIRECTIONAL;
        heuristics = new HashSet<Heuristic>();
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(Main.indentation + "search dir: " + searchDirection + "\n");

        sb.append("\n" + Main.indentation + "heuristics:\n");
        for(Heuristic heuristic : Heuristic.values()){

            boolean status = heuristics.contains(heuristic);
            sb.append(Main.indentation + " - " + heuristic.toString().toLowerCase() + ": " + status + "\n");
        }
        return sb.toString();
    }
}
