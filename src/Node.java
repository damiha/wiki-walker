public class Node {

    private final String title;
    private double cost;

    public Node(String title){
        this.title = title;
        this.cost = Double.POSITIVE_INFINITY;
    }
    public Node(String title,double cost){
        this.title = title;
        this.cost = cost;
    }

    public String getTitle(){
        return title;
    }

    public double getCost(){
        return cost;
    }

    public String toString(){
        return "title: " + getTitle() + ", cost: " + getCost();
    }
}
