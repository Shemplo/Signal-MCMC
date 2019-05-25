package ru.shemplo.mcmc.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.shemplo.mcmc.graph.Graph;
import ru.shemplo.mcmc.graph.GraphSignals;
import ru.shemplo.mcmc.graph.Vertex;
import ru.shemplo.mcmc.graph.GraphSignals.GraphSignal;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;
import ru.shemplo.snowball.utils.fp.StreamUtils;

public class MelanomaAdvGraphReader extends MelanomaGraphReader {
    
    @Override
    public Graph readGraph (String filename) throws IOException {
        Map <String, Double> genesDesc = super.readGenes ("runtime/melanoma", 1);
        genesDesc.keySet ().forEach (gene -> {
            genesDesc.compute (gene, (__, ___) -> -1.0D);
        });
        genesDesc.putAll (readGenes ("runtime/" + filename, 1)); // will override existing
        
        List <Pair <String, String>> edgesDesc = readEdges ();
        Graph graph = new Graph (0.386, 1.0);
        
        edgesDesc = edgesDesc.stream ()
                  . filter  (pair -> genesDesc.containsKey (pair.F)
                                  && genesDesc.containsKey (pair.S))
                  . collect (Collectors.toList ());
        List <String> genesVerts = edgesDesc.stream ()
                                 . flatMap  (pair -> Stream.of (pair.F, pair.S))
                                 . distinct ().sorted ()
                                 . collect  (Collectors.toList ());
        Map <String, Integer> gene2index = new HashMap <> ();
        for (int i = 0; i < genesVerts.size (); i++) {
            String name = genesVerts.get (i);
            Vertex v = graph.addVertex (i, genesDesc.get (name));
            v.setStable (v.getWeight () != -1.0D); 
            v.setName (name);
            
            gene2index.put (name, i);
        }
        
        Set <String> remove = new HashSet <> (Arrays.asList ("UBC"));
        
        for (Pair <String, String> edge : edgesDesc) {
            if (remove.contains (edge.F) || remove.contains (edge.S)) {
                continue;
            }
            
            graph.addEdge (gene2index.get (edge.F), gene2index.get (edge.S), 1.0);
        }
        
        graph.setSignals (GraphSignals.splitGraph (graph));
        ///*
        GraphSignals modules = graph.getSignals ();
        modules.getSignals ().values ().stream ().distinct ()
        . filter  (signal -> signal.getVertices ().size () > 1)
        . sorted  (Comparator.comparing (GraphSignal::getLikelihood))
        . forEach (signal -> {
            Set <Vertex> vertices = signal.getVertices ();
            Vertex vertex = vertices.iterator ().next ();
            System.out.print (String.format ("Signal |S = %-2d| {W = %.3e} ", vertices.size (), vertex.getWeight ()));
            System.out.print (signal.getVertices ().stream ().limit (49).map (Vertex::getName)
                              . collect (Collectors.joining (", ", "[", "")));
            if (signal.getVertices ().size () < 50) {
                System.out.println ("]");
            } else { System.out.println (", ...]"); }
        });
        //*/
        
        graph.getOrientier ().addAll (Arrays.asList (
            "CDKN2A", "MTAP", "MX2", "PARP1", "ARNT", "SETDB1",
            "ATM", "CCND1", "PLA2G6", "RAD23B", "TMEM38B", "FT0",
            "AGR3", "RMDN2", "CASP8", "CDKAL1", "DIP2B", "EBF3",
            "PPP1R15A", "CTR9", "ZNF490", "B2M", "GRAMD3",
            "TOR1AIP1", "MTTP", "ATRIP", "TAF1", "ELAVL1"
        ));
        
        return graph;
    }
    
    @Override
    protected Map <String, Double> readGenes (String filename, int shift) throws IOException {
        final Map <String, Double> genes = new HashMap <> ();
        Path filepath = Paths.get (filename);
        try (
            BufferedReader br = Files.newBufferedReader (filepath);
        ) {
            br.readLine (); // titles
            
            Map <String, Set <String>> classes = new HashMap <> ();
            Map <String, Double> pvals = new HashMap <> ();
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final StringTokenizer st = new StringTokenizer (line);
                List <String> tokens = StreamUtils.whilst (StringTokenizer::hasMoreTokens, 
                                                           StringTokenizer::nextToken, st)
                                     . collect (Collectors.toList ());
                
                final String eqClass = tokens.get (4);
                classes.putIfAbsent (eqClass, new LinkedHashSet <> ());
                
                final String [] genesArray = tokens.get (6).split (";");
                classes.get (eqClass).addAll (Arrays.asList (genesArray));
                
                final double pvalue = Double.parseDouble (tokens.get (3));
                pvals.compute (eqClass, (__, v) -> v == null ? pvalue : Math.min (v, pvalue));
            }
            
            classes.forEach ((eq, genesSet) -> {
                final double pvalue = pvals.get (eq);
                genesSet.forEach (gene -> genes.put (gene, pvalue));
            });
        }
        
        return genes;
    }
    
}
