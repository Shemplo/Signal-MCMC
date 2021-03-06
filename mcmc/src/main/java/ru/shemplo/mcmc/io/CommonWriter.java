package ru.shemplo.mcmc.io;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.mcmc.graph.*;
import ru.shemplo.snowball.stuctures.Pair;

public class CommonWriter {
    
    public void saveLikelihoods (String filepath, List <Double> likelihoods) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            pw.println ("iteration\tlikelihood");
            for (int i = 0; i < likelihoods.size (); i++) {
                pw.println (String.format ("%d\t%e", i, likelihoods.get (i)));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }
    
    public void saveMap (String filepath, String parameter, 
            Map <Vertex, Double> occurrences, Graph graph) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            pw.println ("gene\tdegree\tpval\tsignal\t" + parameter);
            occurrences.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . sorted  ((a, b) -> -Double.compare (a.S, b.S))
            . forEach (p -> {
                int signal = graph.getSignals ().getSignal (p.F).getId ();
                int degree = p.F.getEdgesList ().size ();
                
                String name = p.F.getName ();
                String weight = p.F.getWeight () < 1e-7
                              ? String.format ("%.6e", p.F.getWeight ())
                              : String.format ("%.10f", p.F.getWeight ());
                pw.println (String.format ("%9s\t%4d\t%s\t%6d\t%.6f", 
                            name, degree, weight, signal, p.S));
            });
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }
    
    public void saveNontrivialSignalsMap (String filepath, String parameter, 
            Map <Vertex, Double> occurrences, Graph graph) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            pw.println ("gene\tdegree\tpval\tsignal\t" + parameter);
            
            GraphSignals gsignals = graph.getSignals ();
            Map <Integer, List <Vertex>> signals = occurrences.entrySet ().stream ()
              . map     (Pair::fromMapEntry)
              . map     (Pair::getF)
              . collect (Collectors.groupingBy (v -> gsignals.getSignal (v).getId ()));
            signals.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . filter  (pair -> pair.S.size () > 1)
            . sorted  ((a, b) -> Double.compare (a.S.get (0).getWeight (), b.S.get (0).getWeight ()))
            . forEach (pair -> {
                pair.S.sort ((a, b) -> -Double.compare (occurrences.get (a), occurrences.get (b)));
                pair.S.forEach (vertex -> {
                    double occurences = occurrences.get (vertex);
                    int degree = vertex.getEdgesList ().size ();
                    
                    String name = vertex.getName ();
                    String weight = vertex.getWeight () < 1e-7
                                  ? String.format ("%.6e", vertex.getWeight ())
                                  : String.format ("%.10f", vertex.getWeight ());
                    pw.println (String.format ("%9s\t%4d\t%s\t%6d\t%.6f", 
                                name, degree, weight, pair.F, occurences));
                });
            });
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }
    
    public void saveGradientDOTFile (String filepath, GraphDescriptor graph, 
            Map <Vertex, Double> occurrences, Color maxColor, Color minColor) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            final Set <Vertex> vertices = graph.getVertices ();
            final Set <Edge> edges = graph.getEdges ();
            pw.println (makeGradientString (vertices, edges, 
                          maxColor, minColor, occurrences));
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        };
    }
    
    private String makeGradientString (Set <Vertex> vertices, Set <Edge> edges,
            Color maxColor, Color minColor, Map <Vertex, Double> occurrences) {
        Map <Vertex, Double> map = new HashMap <> (occurrences);
        double max = occurrences.entrySet ().stream ()
                   . map         (Pair::fromMapEntry)
                   . mapToDouble (Pair::getS)
                   . max ().orElse (1);
        for (Vertex vertex : map.keySet ()) {
            map.compute (vertex, (__, v) -> v / max);
        }
        
        StringJoiner sj = new StringJoiner ("\n");
        sj.add ("graph prof {");
        //sj.add ("    rankdir=LR;");
        //sj.add ("    size=\"64\";");
        sj.add ("    overlap=false;");
        sj.add ("    splines=true;");
        sj.add ("    ratio=fill;");
        sj.add ("    node [style = filled];");
        for (Vertex vertex : vertices) {
            if (vertex == null) { continue; }
            
            final double weight = map.get (vertex);
            String appendix = makeGradientColor (weight, maxColor, minColor);
            sj.add (String.format ("    \"%s\" [color = \"%s\"];", 
                                    vertex.getName (), appendix));
        }
        
        sj.add ("    node [shape = circle];");
        sj.add ("    node [color = black];");
        
        for (Edge edge : edges) {
            if (edge.F.getId () > edge.S.getId ()) { continue; }
            
            String appendix = "";
            /*
            if (vertices.contains (edge.F) && vertices.contains (edge.S)
                    && edges.contains (edge)) {
                appendix = "[color = red]";
            }
            */
            sj.add (String.format ("    \"%s\" -- \"%s\" %s;", 
                         edge.F.getName (), edge.S.getName (),
                         appendix));
        }
        
        sj.add ("}");
        
        return sj.toString ();
    }
    
    private String makeGradientColor (double weight, Color maxColor, Color minColor) {
        int r = minColor.getRed   () + (int) ((maxColor.getRed   () - minColor.getRed   ()) * weight);
        int g = minColor.getGreen () + (int) ((maxColor.getGreen () - minColor.getGreen ()) * weight);
        int b = minColor.getBlue  () + (int) ((maxColor.getBlue  () - minColor.getBlue  ()) * weight);
        
        return String.format ("#%02x%02x%02x", r, g, b);
    }
    
}
