package ru.shemplo.mcmc.graph;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.mcmc.impl.MCMC;
import ru.shemplo.mcmc.impl.MCMCJoinOrLeave;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class Graph {
    
    private final double alphaV, alphaE;
    
    @Getter @Setter private GraphSignals signals = new GraphSignals ();
    @Getter private List <Vertex> vertices = new ArrayList <> ();
    @Getter private List <Edge> edges = new ArrayList <> ();
    
    @Getter
    private final Set <String> orientier = new HashSet <> ();
    
    private final Map <String, Vertex> names = new HashMap <> ();
    
    public GraphDescriptor getEmptyDescriptor (boolean useSignal) {
        GraphDescriptor descriptor = new GraphDescriptor (alphaV, alphaE, useSignal, this);
        Collections.shuffle (edges);
        return descriptor.addEdge (edges.get (0))
             . commit ();
    }
    
    public GraphDescriptor getFixedDescriptor (int verts, boolean useSignal) {
        MCMC mcmc = new MCMCJoinOrLeave (getEmptyDescriptor (useSignal), 1000000);
        mcmc.makeIteration (true);
        
        while (mcmc.getCurrentGraph ().getVertices ().size () < verts 
                    && !mcmc.finishedWork ()) {
            mcmc.makeIteration (true);
        }
        
        return mcmc.getCurrentGraph ();
    }
    
    public GraphDescriptor getFullDescriptor (boolean useSignal) {
        GraphDescriptor descriptor = new GraphDescriptor (alphaV, alphaE, useSignal, this);
        descriptor.getVertices ().addAll (vertices); 
        descriptor.getEdges ().addAll (edges);
        
        return descriptor;
    }
    
    public GraphDescriptor getFinalDescriptor (int size, Map <Vertex, Double> occurrences) {
        GraphDescriptor graph = new GraphDescriptor (alphaV, alphaE, false, this);
        Set <Vertex> basis = occurrences.entrySet ().stream ()
                           . map     (Pair::fromMapEntry)
                           //. filter  (pair -> !pair.getF ().getName ().equals ("UBC"))
                           . sorted  ((a, b) -> -Double.compare (a.S, b.S))
                           . limit   (size)
                           . map     (Pair::getF)
                           . collect (Collectors.toSet ());
        Set <Vertex> copy = new HashSet <> (basis);
        Vertex start = basis.iterator ().next ();
        basis.remove (start);
        
        Queue <Vertex> queue = new LinkedList <> ();
        queue.add (start);
        
        Map <Vertex, List <Edge>> ways = new HashMap <> ();
        ways.put (start, new ArrayList <> ());
        
        while (!basis.isEmpty ()) {
            Vertex vertex = queue.poll ();
            
            for (Pair <Vertex, Edge> edge : vertex.getEdgesList ()) {
                List <Edge> way = new ArrayList <> (ways.get (vertex));
                way.add (edge.S);
                
                if (!ways.containsKey (edge.F) || (ways.get (edge.F).size () > way.size ())) {
                    ways.put (edge.F, way);
                    queue.add (edge.F);
                }
            }
            
            basis.remove (vertex);
        }
        
        Set <Vertex> top = occurrences.entrySet ().stream ()
                         . map     (Pair::fromMapEntry)
                         . sorted  ((a, b) -> -Double.compare (a.S, b.S))
                         . limit   (100)
                         . map     (Pair::getF)
                         . collect (Collectors.toSet ());
        
        for (Vertex vertex : copy) {
            for (Edge edge : ways.get (vertex)) {
                graph.addEdge (edge);
            }
            
            for (Pair <Vertex, Edge> edge : vertex.getEdgesList ()) {
                if (top.contains (edge.F)) {
                    graph.addEdge (edge.S);
                }
            }
        }
        
        return graph.commit ();
    }
    
    public void regenerateVerticesWeight (Function <Vertex, Double> generator) {
        vertices.forEach (vertex -> {
            vertex.setWeight (generator.apply (vertex));
        });
    }
    
    public Vertex addVertex (int id, Double weight) {
        return addVertex (new Vertex (id, weight));
    }
    
    public Vertex addVertex (Vertex vertex) {
        int id = vertex.getId ();
        while (id >= vertices.size ()) {
            vertices.add (null);
        }
        
        if (vertex.getName () != null) {
            names.put (vertex.getName (), vertex);
        }
        vertices.set (id, vertex);
        return vertex;
    }
    
    public Vertex getVertexByName (String name) {
        return names.get (name);
    }
    
    public void addEdge (int a, int b, Double weight) {
        addEdge (vertices.get (a), vertices.get (b), weight);
    }
    
    public void addEdge (Vertex a, Vertex b, Double weight) {
        addEdge (new Edge (a, b, weight));
    }
    
    public void addEdge (Edge edge) {
        addVertex (edge.F);
        /*
        if (vertices.get (edge.F.getId ()) == null) {
            addVertex (edge.F);
        }
        */
        
        addVertex (edge.S);
        /*
        if (vertices.get (edge.S.getId ()) == null) {
            addVertex (edge.S);
        }
        */
        
        edge.F.addEdge (edge);
        edge.S.addEdge (edge);
        edges.add (edge);
    }
    
    public double getNthEdgeWeight (int n) {
        if (edges.size () < n) {
            System.err.println ("No " + n + "th pvalue in edges");
            return edges.get (edges.size () - 1).getWeight ();
        }
        
        return edges.stream ()
             . map       (Edge::getWeight)
             . sorted    ().distinct ().skip (n)
             . findFirst ().get ();
    }
    
    public double getNthVertexWeight (int n) {
        if (vertices.size () < n) {
            System.err.println ("No " + n + "th pvalue in vertices");
            return vertices.get (vertices.size () - 1).getWeight ();
        }
        
        return vertices.stream ()
             . map       (Vertex::getWeight)
             . sorted    ().distinct ().skip (n)
             . findFirst ().get ();
    }
    
}
