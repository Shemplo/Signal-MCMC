package ru.shemplo.metagennet;

import java.util.HashMap;
import java.util.Map;

import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.Vertex;

public class Test {
    
    public static void main (String ... args) {
        Map <Vertex, Integer> map = new HashMap <> ();
        
        Vertex va = new Vertex (0, 4.76),
               vb = new Vertex (0, 4.76);
        map.put (va, 54);
        
        System.out.println (map.get (vb));
        
        Map <Edge, Integer> map2 = new HashMap <> ();
        
        Edge ea = new Edge (new Vertex (1, 34.3), new Vertex (3, 5.3), 435.3),
             eb = new Edge (new Vertex (1, 34.3), new Vertex (3, 5.3), 435.3);
        map2.put (ea, 62);
        
        System.out.println (map2.get (eb));
    }
    
}
