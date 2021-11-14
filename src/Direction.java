public enum Direction {
    forward, backward;

    public Direction getCounterPart(){
        return this == Direction.forward ? Direction.backward : Direction.forward;
    }
}
