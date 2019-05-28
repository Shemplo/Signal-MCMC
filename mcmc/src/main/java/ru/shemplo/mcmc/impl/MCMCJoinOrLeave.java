package ru.shemplo.mcmc.impl;

import static ru.shemplo.mcmc.RunMCMC.*;

import ru.shemplo.mcmc.graph.Edge;
import ru.shemplo.mcmc.graph.GraphDescriptor;

public class MCMCJoinOrLeave extends AbsMCMC {
    
    public MCMCJoinOrLeave (GraphDescriptor initialGraph, int iterations) {
        super (initialGraph, iterations);
    }

    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        //long start = System.currentTimeMillis ();
        if (iteration == 0) {
            currentGraph = initialGraph.setTausFromGraph (TAU_V_N, TAU_E_N);
            if (idling) { // initial state for next MCMC
                currentGraph.setTauV (1.0);
                currentGraph.setTauE (1.0);
            }
            
            iteration += 1; commits += 1;
            return;
        }
        
        @SuppressWarnings ("unused")
        double pS = currentGraph.getRatio ();
        //System.out.println (pS);
        
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        Edge candidate = currentGraph.getRandomGraphEdge (true);
        //System.out.println (candidat);
        //System.out.println (currentGraph);
        
        double qS2Ss = 0, qSs2S = 0;
        if (currentGraph.getEdges ().contains (candidate)) {
            //System.out.println ("remove");
            //qS2Ss = 1.0 / Math.max (currentGraph.getInnerEdges (), 1);
            qS2Ss = 1.0 / Math.max (currentGraph.getEdges ().size (), 1);
            //System.out.println ("IE " + currentGraph.getEdges ());
            //System.out.println ("R in " + currentGraph.getInnerEdges ());
            if (!currentGraph.removeEdge (candidate).isConnected ()) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            //qSs2S = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            qSs2S = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            //System.out.println ("BE " + currentGraph.getBedges ());
            //System.out.println ("R bord " + currentGraph.getBorderEdges ());
        } else {
            //System.out.println ("add");
            //qS2Ss = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            qS2Ss = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            //System.out.println ("BE " + currentGraph.getBedges ());
            //System.out.println ("A bord " + currentGraph.getBorderEdges ());
            if (!currentGraph.addEdge (candidate).isConnected ()) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            //qSs2S = 1.0 / Math.max (currentGraph.getInnerEdges (), 1);
            qSs2S = 1.0 / Math.max (currentGraph.getEdges ().size (), 1);
            //System.out.println ("A in " + currentGraph.getInnerEdges ());
            //System.out.println ("IE " + currentGraph.getEdges ());
        }
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getRatio ();
        //System.out.println (pS + " " + pSs + " " + (pSs / pS));
        double mod = qSs2S / qS2Ss;
        //System.out.println (qSs2S + " " + qS2Ss + " " + mod);
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, pSs * mod);
        //System.out.println ("Rho: " + rho);
        
        //System.out.println (rho);
        if (RANDOM.nextDouble () <= rho) {
            //System.out.println ("Commit");
            //System.out.println ("Applied");
            currentGraph.commit ();
            commits += 1;
        } else {
            //System.out.println ("Rollback");
            currentGraph.rollback ();
        }
        
        //System.out.println (currentGraph.toVerticesString ());
        //System.out.println (">>> " + currentGraph);
        iteration += 1;
        
        //long end = System.currentTimeMillis ();
        //System.out.println (iteration + " " + (end - start) + " ms");
        //System.out.println (currentGraph);
    }
    
}
