import java.util.ArrayList;
import java.util.List;

class InvertedInfo{

    private int tf;
    private List<Integer> positions;

    public InvertedInfo(){

        this.tf = 0;
        this.positions = new ArrayList<>();

    }

    public void incrementTf(){ this.tf++; }

    public void addPosition(int pos){ this.positions.add(pos); }

    public int getTf(){ return tf; }

    public List<Integer> getPositions(){ return positions; }

}
