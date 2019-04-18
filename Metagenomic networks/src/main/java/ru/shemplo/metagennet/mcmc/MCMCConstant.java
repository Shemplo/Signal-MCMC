package ru.shemplo.metagennet.mcmc;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.GraphDescriptor;

public class MCMCConstant extends AbsMCMC {
    
    public MCMCConstant (GraphDescriptor initialGraph, int iterations) {
        super (initialGraph, iterations);
    }

    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        if (iteration == 0) {
            currentGraph = initialGraph;
            iteration += 1; 
            commits += 1;
            return;
        }
        
        @SuppressWarnings ("unused")
        double pS = currentGraph.getRatio ();
        //System.out.println (pS);
        
        //System.out.println (currentGraph);
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        //System.out.println (currentGraph);
        //System.out.println (candidatIn + " / " + candidatOut);
        
        double qS2Ss = 0, qSs2S = 0;
        qS2Ss = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
        
        final Edge candidateBorder = currentGraph.getRandomBorderEdge ();
        final Edge candidateIn     = currentGraph.getRandomInnerEdge ();
        currentGraph.addEdge    (candidateBorder, iteration, iterations)
                    .removeEdge (candidateIn, iteration, iterations);
        //System.out.println ("rm " + candidatIn + " / add " + candidatBorder);
        //System.out.println ("X>> " + currentGraph);
        if (!currentGraph.isConnected ()) {
            //System.out.println ("Not connected");
            currentGraph.rollback (); 
            //iteration += 1;
            
            //System.out.println (">>> " + currentGraph);
            return;
        }
        qSs2S = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getRatio ();
        double mod = qSs2S / qS2Ss;
        //System.out.println (pS + " " + pSs + " " + (pSs / pS));
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, pSs * mod);
        //System.out.println ("Rho: " + rho);
        
        //System.out.println (rho);
        if (RANDOM.nextDouble () <= rho) {
            //System.out.println ("Rho: " + rho + ", ps: " + pS + " / " + pSs);
            //System.out.println ("#! " + currentGraph.toVerticesString ());
            //System.out.println ("Applied");
            currentGraph.commit ();
            commits += 1;
        } else {
            currentGraph.rollback ();
        }
        
        //System.out.println (currentGraph.toVerticesString ());
        //System.out.println (">>> " + currentGraph);
        iteration += 1;
    }
    
}
