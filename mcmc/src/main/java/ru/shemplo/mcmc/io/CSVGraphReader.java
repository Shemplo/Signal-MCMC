package ru.shemplo.mcmc.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ru.shemplo.mcmc.graph.Edge;
import ru.shemplo.mcmc.graph.Graph;
import ru.shemplo.mcmc.graph.GraphSignals;
import ru.shemplo.mcmc.graph.Vertex;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class CSVGraphReader implements GraphReader {
    
    @Override
    public Graph readGraph (String filenamePrefix) throws IOException {
        Pair <List <String>, List <List <String>>> verticesCSV = readCSV ("runtime/" + filenamePrefix + "vertices.csv");
        Pair <List <String>, List <List <String>>> edgesCSV = readCSV ("runtime/" + filenamePrefix + "edges.csv");
        
        Graph graph = new Graph (0.133, 0.195); // gatom_
        //Graph graph = new Graph (0.18, 1.0); // paper_
        //Graph graph = new Graph (0.363, 0.431); // generated_
        
        int nameColumnV = verticesCSV.F.indexOf ("\"geneSymbol\"") != -1
                        ? verticesCSV.F.indexOf ("\"geneSymbol\"")
                        : verticesCSV.F.indexOf ("\"name\"");
        int pvalColumnV = verticesCSV.F.indexOf ("\"pval\"");
        AtomicInteger counter = new AtomicInteger ();
        final Map <String, Vertex> vertices = verticesCSV.S.stream ()
            . map     (lst -> Pair.mp (lst.get (nameColumnV), lst))
            . map     (p -> p.applyS (lst -> {
                final int index = counter.getAndIncrement ();
                Double weight = null;
                try   { weight = Double.parseDouble (lst.get (pvalColumnV).replace (',', '.')); } 
                catch (NumberFormatException nfe) { weight = 1D; }
                
                Vertex vertex = new Vertex (index, weight);
                vertex.setName (p.F.replace ("\"", ""));
                graph.addVertex (vertex);
                
                return vertex;
            }))
            . collect (Collectors.toMap (Pair::getF, Pair::getS));
        
        int fromColumnE = edgesCSV.F.indexOf ("\"from\""),
            toColumnR   = edgesCSV.F.indexOf ("\"to\"");
        int pvalColumnE = edgesCSV.F.indexOf ("\"weight\"") != -1 
                        ? edgesCSV.F.indexOf ("\"weight\"")
                        : edgesCSV.F.indexOf ("\"pval\"");
        edgesCSV.S.forEach (edge -> {
            String from = edge.get (fromColumnE).replaceAll ("\\(\\d+\\)", ""), 
                   to   = edge.get (toColumnR).replaceAll ("\\(\\d+\\)", "");
            Double weight = null;
            
            try   { weight = Double.parseDouble (edge.get (pvalColumnE).replace (',', '.')); } 
            catch (NumberFormatException nfe) { weight = 1D; }
            
            Edge edgeI = new Edge (vertices.get (from), vertices.get (to), weight);
            graph.addEdge (edgeI);
        });
        
        //graph.setSignals (GraphSignals.assignUnique (graph));
        graph.setSignals (GraphSignals.splitGraph (graph));
        
        if (filenamePrefix.equals ("paper_")) {
            graph.getOrientier ().addAll (Arrays.asList (
                "APEX1", "BCL2", "BCL6", "CD44", "CCND2", "CREBBP", 
                "HCK", "HDAC1", "IL16", "LYN", "IRF4", "MYBL1", 
                "MME", "PTK2", "PIM1", "MAPK10", "PTPN2", "PTPN1", 
                "TGFBR2", "WEE1", "VCL", "BLNK", "BMF", "SH3BP5", 
                "KCNA3"
            ));
        } /* else if (filenamePrefix.equals ("generated_")) {
            graph.getOrientier ().addAll (Arrays.asList (
                "V8", "V9", "V17", "V18", "V21", "V24", "V27", "V37", 
                "V38", "V39", "V47", "V48", "V56", "V61", "V64", "V74",
                "V86", "V89", "V90", "V92"
            ));
        } */
        
        return graph;
    }
    
    protected Pair <List <String>, List <List <String>>> readCSV (String filepath) throws IOException {
        final Path path = Paths.get (filepath);
        
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            List <String> titles = Arrays.asList (br.readLine ().split ("(\\s+|;)"));
            
            String line = null;
            List <List <String>> rows = new ArrayList <> ();
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                rows.add (Arrays.asList (line.split ("(\\s+|;)")));
            }
            
            return Pair.mp (titles, rows);
        }
    }
    
}
