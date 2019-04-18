package ru.shemplo.metagennet.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.*;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
@ToString (exclude = {"edges", "edgesList"})
public class Vertex {
    
    @Getter private final List <Pair <Vertex, Edge>> edgesList = new ArrayList <> ();
    @Getter private final Map <Vertex, Edge> edges = new LinkedHashMap <> ();
    
    @Getter private final int id;
    
    @Setter 
    private String name;
    
    public String getName () {
        if (name != null) {
            return name;
        }
        
        return "V" + id;
    }
    
    @Getter @Setter
    @NonNull private Double weight;
    
    public double getWeight (int iteration, int total) {
        return iteration < total / 3
             ? Math.pow (weight, -16)
             : iteration < total * 2 / 3
             ? Math.pow (weight, -8)
             : weight;
    }
    
    public void addEdge (Edge edge) {
        Vertex vertex = edge.F.equals (this) ? edge.S : edge.F;
        edgesList.add (Pair.mp (vertex, edge));
        edges.put (vertex, edge);
    }
    
    @Override
    public boolean equals (Object obj) {
        if (obj == null) { return false; }
        return hashCode () == obj.hashCode ();
    }
    
    @Override
    public int hashCode () { return id; }
    
}
