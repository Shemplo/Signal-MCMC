package ru.shemplo.metagennet.graph;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.shemplo.snowball.stuctures.Pair;

public class Edge extends Pair <Vertex, Vertex> {

    private static final long serialVersionUID = -33766014367150868L;
    
    @Getter @Setter
    @NonNull private Double weight;
    
    public double getWeight (int iteration, int total) {
        return iteration < total / 3
             ? Math.pow (weight, -16)
             : iteration < total * 2 / 3
             ? Math.pow (weight, -8)
             : weight;
    }
    
    public Edge (Vertex F, Vertex S, double weight) { 
        super (F.getId () <= S.getId () ? F : S, 
               F.getId () <= S.getId () ? S : F); 
        this.weight = weight;
    }
    
    @Override
    public String toString () {
        return String.format ("%d -> %d", F.getId (), S.getId ());
    }
    
    @Override
    public Edge swap () { return new Edge (S, F, weight); }
    
    @Override
    public boolean equals (Object obj) {
        if (obj == null) { return false; }
        return hashCode () == obj.hashCode ();
    }
    
    /**
     * Assumed that vertices don't have <b>id</b> more than 2<sup>15</sup>
     */
    @Override
    public int hashCode () {
        return (F.getId () << 15) | S.getId ();
    }
    
}