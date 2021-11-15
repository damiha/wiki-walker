public class Statistics {

    // TODO: add more KPIs later
    private int numberOfRequests;

    public Statistics(){

    }

    public void setNumberOfRequests(int value){
        numberOfRequests = value;
    }

    public void printStats(){
        System.out.println(Main.indentation + "# requests: " + numberOfRequests);
    }
}
