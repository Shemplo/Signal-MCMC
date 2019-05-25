package ru.shemplo.mcmc.graph;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.mcmc.graph.GraphSignals.GraphSignal;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.stuctures.Trio;

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public class GraphDescriptor implements Cloneable {

    private final Random R = new Random ();
    private final double betaAV, betaAE;
    
    private final boolean signal;
    private final Graph graph;
    
    private final Deque <Trio <Integer, Edge, Double>> history 
          = new LinkedList <> ();
    
    @Getter private final Map <GraphSignal, Set <Vertex>> signals = new HashMap <> ();
    @Getter private final Set <Vertex> vertices = new LinkedHashSet <> ();
    @Getter private final Set <Edge> edges  = new LinkedHashSet <> (),
                                     bedges = new HashSet <> ();
    
    @Getter @Setter private double tauE = 1, tauV = 1;
    @Getter private double ratio = 1;
    
    public GraphDescriptor setTausFromGraph (int vn, int en) {
        if (vn < 1 || en < 1) {
            String message = "Tau selection parameters must be positive";
            throw new IllegalArgumentException (message);
        }
        
        tauV = graph.getNthVertexWeight (vn - 1);
        tauE = graph.getNthEdgeWeight (en - 1);
        
        System.out.println (String.format ("Tau: v[%d]=%e, e[%d]=%e", 
                                        vn - 1 ,tauV, en - 1, tauE));
        return this;
    }
    
    @Override
    public String toString () {
        StringJoiner sj = new StringJoiner ("\n");
        
        sj.add (String.format ("Graph #%d", hashCode ()));
        sj.add (String.format (" Verticies    : %s", toVerticesString ()));
        
        List <String> eds = edges.stream ().map (Edge::toString)
                          . collect (Collectors.toList ());
        sj.add (String.format (" Edges  (%4d): %s", eds.size (), eds.toString ()));
        
        if (bedges.size () <= 100) {
            eds = bedges.stream ().map (Edge::toString)
                . collect (Collectors.toList ());
            sj.add (String.format (" BEdges (%4d): %s", eds.size (), eds.toString ()));
        }
        /*
        sj.add ("");
        */
        
        return sj.toString ();
    }
    
    public String toVerticesString () {
        List <String> verts = vertices.stream ()
                            . sorted  ((a, b) -> Integer.compare (a.getId (), b.getId ()))
                            . map     (v -> v.getName () != null 
                                          ? String.format ("%s (%d)", v.getName (), v.getId ()) 
                                          : "" + v.getId ())
                            . collect (Collectors.toList ());
        return verts.toString ();
    }
    
    public String toDot () {
        StringJoiner sj = new StringJoiner ("\n");
        sj.add ("graph finite_state_machine {");
        sj.add ("    rankdir=LR;");
        //sj.add ("    size=\"64\";");
        sj.add ("    node [shape = doublecircle];");
        sj.add ("    node [color = red];");
        for (Vertex vertex : vertices) {
            if (vertex == null) { continue; }
            sj.add (String.format ("    V%d;", vertex.getId ()));
        }
        
        sj.add ("    node [shape = circle];");
        sj.add ("    node [color = black];");
        
        for (Edge edge : graph.getEdges ()) {
            if (edge.F.getId () > edge.S.getId ()) { continue; }
            
            String appendix = "";
            if (vertices.contains (edge.F) && vertices.contains (edge.S)
                    && edges.contains (edge)) {
                appendix = "[color = red]";
            }
            sj.add (String.format ("    V%d -- V%d [label = \"%f\"]%s;", 
                    edge.F.getId (), edge.S.getId (),
                    edge.getWeight (), appendix));
        }
        
        sj.add ("}");
        
        return sj.toString ();
    }
    
    public double getLogLikelihood () {
        double likelihood = 1;
        for (Vertex vertex : vertices) {
            likelihood *= Math.pow (vertex.getWeight (), 0.125);
        }
        for (Edge edge : edges) {
            likelihood *= Math.pow (edge.getWeight (), 0.125);
        }
        
        return Math.log (likelihood);
    }
    
    public GraphDescriptor commit () {
        //System.out.println ("Commit");
        while (!history.isEmpty ()) {
            history.pollLast ();
        }
        
        this.ratio = 1.0d;
        return this;
    }
    
    public GraphDescriptor rollback () {
        //System.out.println ("Rollback");
        while (!history.isEmpty ()) {
            Trio <Integer, Edge, Double> event = history.pollLast ();
            //System.out.println (event);
            if (event.F == 0) {
                applyRemove (event.S);
                //bedges.add (event.S);
            } else if (event.F == 1) {
                applyAdd (event.S);
                //bedges.remove (event.S);
            }
            
            ratio = event.T;
        }
        
        if (!signal) { ratio = 1; }
        return this;
    }
    
    public GraphDescriptor addEdge (Edge edge) {
        return addEdge (edge, 0, 0);
    }
    
    public GraphDescriptor addEdge (Edge edge, int i, int total) {
        if (edges.contains (edge)) { return this; }
        history.add (Trio.mt (0, edge, ratio));
        applyAdd (edge, i, total);
        return this;
    }
    
    private void applyAdd (Edge edge) {
        applyAdd (edge, 0, 0);
    }
    
    private void applyAdd (Edge edge, int i, int total) {
        edges.add (edge);
        
        final double w = edge.getWeight (i, total);
        ratio *= Math.pow (w / tauE, betaAE - 1);
        
        applyVertexAdd (edge.F, edge, i, total);
        applyVertexAdd (edge.S, edge, i, total);
        bedges.removeAll (edges);
    }
    
    private void applyVertexAdd (Vertex vertex, Edge edge, int i, int total) {
        if (!vertices.contains (vertex)) {
            vertices.add (vertex);
            applyVertex (vertex, edge, 0, i, total);
            
            if (!signal) {
                final double w = vertex.getWeight (i, total);
                ratio *= Math.pow (w / tauV, betaAV - 1);
            } else {
                GraphSignal signal = graph.getSignals ().getSignal (vertex);
                if (!signals.containsKey (signal)) {
                    signals.put (signal, new HashSet <> ());
                }
                
                final Set <Vertex> set = signals.get (signal);
                if (set.isEmpty ()) {
                    final double w = vertex.getWeight (i, total);
                    ratio *= Math.pow (w / tauV, betaAV - 1);
                }
                
                set.add (vertex);
            }
        }
    }
    
    public GraphDescriptor removeEdge (Edge edge) {
        return removeEdge (edge, 0, 0);
    }
    
    public GraphDescriptor removeEdge (Edge edge, int i, int total) {
        history.add (Trio.mt (1, edge, ratio));
        applyRemove (edge, i, total);
        return this;
    }
    
    private void applyRemove (Edge edge) {
        applyRemove (edge, 0, 0);
    }
    
    private void applyRemove (Edge edge, int i, int total) {
        edges.remove (edge);
        bedges.add (edge);
        
        final double w = edge.getWeight (i, total);
        ratio /= Math.pow (w / tauE, betaAE - 1);
        
        applyVertex (edge.F, edge, 1, i, total);
        applyVertex (edge.S, edge, 1, i, total);
        bedges.removeAll (edges);
    }
    
    private void applyVertex (Vertex vertex, Edge edge, int action, int i, int total) {
        int neis = 0;
        if (action == 0) {
            // Extra edge (that is adding) will be removed later
            bedges.addAll (vertex.getEdges ().values ());
            bedges.remove (edge);
        } else if (action == 1) {
            for (Pair <Vertex, Edge> nei : vertex.getEdgesList ()) {
                if (edges.contains (nei.S)) { neis += 1; }
            }
        }
        
        //System.out.println (vertex + " / " + neis + " / " + action);
        if (action == 1 && neis == 0) { 
            for (Pair <Vertex, Edge> nei : vertex.getEdgesList ()) {
                if (!vertices.contains (nei.F)) {
                    bedges.remove (nei.S);
                } else {
                    bedges.add (nei.S);
                }
            }
            vertices.remove (vertex); 
            
            if (!signal) {
                final double w = vertex.getWeight (i, total);
                ratio /= Math.pow (w / tauV, betaAV - 1);
            } else {
                GraphSignal module = graph.getSignals ().getSignal (vertex);
                Set <Vertex> set = signals.get (module);
                set.remove (vertex);
                //System.out.println (vertex + " " + module + " " + set);
                
                if (set.isEmpty ()) {
                    final double w = vertex.getWeight (i, total);
                    ratio /= Math.pow (w / tauV, betaAV - 1);
                }
            }
        }
    }
    
    public boolean isConnected () {
        if (vertices.size () == 0) { return true; }
        
        final Queue <Vertex> queue = new LinkedList <> ();
        queue.add (vertices.iterator ().next ());
        
        Set <Integer> visited = new HashSet<> ();
        visited.add (queue.peek ().getId ());
        
        int iters = 10000;
        while (!queue.isEmpty () && --iters >= 0) {
            Vertex vertex = queue.poll ();
            for (Pair <Vertex, Edge> nei : vertex.getEdgesList ()) {
                if (!edges.contains (nei.S)) { continue; }
                if (!visited.contains (nei.F.getId ())) {
                    visited.add (nei.F.getId ());
                    queue.add (nei.F);
                }
            }
        }
        //System.out.println ("Iters: " + iters);
        
        return vertices.size () == visited.size ();
    } 
    
    public Edge getRandomInnerEdge () {
        int index = R.nextInt (edges.size ());
        for (Edge edge : edges) {
            if (index-- == 0) { return edge; }
        }
        
        return null;
    }
    
    public int getInnerEdges () {
        return edges.size ();
    }
    
    public Edge getRandomBorderEdge () {        
        int index = R.nextInt (bedges.size ());
        for (Edge edge : bedges) {
            if (index-- == 0) { return edge; }
        }
        
        return null;
    }
    
    public int getBorderVertices () {
        Set <Vertex> set = new HashSet <> ();
        for (Edge edge : bedges) {
            if (!vertices.contains (edge.F)) {
                set.add (edge.F);
            }
            
            if (!vertices.contains (edge.S)) {
                set.add (edge.S);
            }
        }
        
        return set.size ();
    }
    
    public int getBorderEdges () {
        return bedges.size ();
    }
    
    public Edge getRandomBorderOrInnerEdge () {
        int index = R.nextInt (bedges.size () + edges.size ());
        if (index < bedges.size ()) {
            for (Edge edge : bedges) {
                if (index-- == 0) { return edge; }
            }
        } else {
            index -= bedges.size ();
            for (Edge edge : edges) {
                if (index-- == 0) { return edge; }
            }
        }
        
        return null;
    }
    
    public Edge getRandomGraphEdge (boolean shouldBeConnected) {
        if (!shouldBeConnected) {
            int index = R.nextInt (graph.getEdges ().size ());
            return graph.getEdges ().get (index);
        }
        
        int range = graph.getEdges ().size ();
        Edge edge = null;
        do    { edge = graph.getEdges ().get (R.nextInt (range)); } 
        while (!vertices.contains (edge.F) && !vertices.contains (edge.S)
           && vertices.size () > 0);
        
        return edge;
    }
    
}
