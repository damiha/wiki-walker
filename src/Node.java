import java.util.*;

public class Node {

    private final String title;
    private final String canonicalTitle;
    private Set<String> categories;
    private double cost;

    private Node parent;
    private Direction direction;

    public Node(String title){
        this.title = title;
        this.canonicalTitle = this.title.toLowerCase();
        this.categories = new HashSet<>();

        this.cost = Double.POSITIVE_INFINITY;
    }
    public Node(String title,double cost){
        this(title);
        this.cost = cost;
    }

    public Set<String> getCategories(){
        return categories;
    }

    public void setCost(double cost){
        this.cost = cost;
    }

    public void setCategories(Set<String> categories){
        this.categories = categories;
    }

    public void setParent(Node parent){
        this.parent = parent;
    }

    public void setDirection(Direction direction){
        this.direction = direction;
    }

    public Node getParent(){
        return this.parent;
    }

    public Direction getDirection(){
        return this.direction;
    }

    /* disregard cost, nodes are still equal */
    public boolean equals(Object other){
        return other instanceof Node && ((Node) other).title.equals(this.title);
    }

    public int hashCode(){
        return title.hashCode();
    }

    public String getTitle(){
        return title;
    }

    public String getURLTitle(){
        return Main.format(title);
    }

    public String getCanonicalTitle(){
        return canonicalTitle;
    }

    public double getCost(){
        return cost;
    }

    public String toString(){
        return getTitle();
    }
}
