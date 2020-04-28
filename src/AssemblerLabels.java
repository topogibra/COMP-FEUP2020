import java.util.HashMap;

public class AssemblerLabels {
    private HashMap<String,Integer> map;

    public AssemblerLabels() {
        this.map = new HashMap<>();
    }

    public String getLabel(String label){
        if(map.containsKey(label)){
            int number = map.get(label);
            map.put(label,number+1);
            return label + "_" + number;
        } else{
            map.put(label,1);
            return label+ "_" + 0;
        }
    }
}
