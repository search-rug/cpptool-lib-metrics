package nl.rug.search.cpptool.metrics;

import nl.rug.search.cpptool.api.Declaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by canberker on 10.08.2015.
 * Utility Node for the metrics. Holds declarations, parent-child-inheritance relations, metric results, QMOOD results
 */
public class Node {
    public Declaration dec;
    public List<Integer> children = new ArrayList<Integer>();
    public List<Integer> parents = new ArrayList<Integer>();
    public List<Integer> rootHeight = new ArrayList<Integer>();
    public Set<Declaration> inherited = new HashSet<Declaration>();

    // DSC Design Size - Design Size in Classes
    // NOH Hierarchies - Number of Hierarchies
    public Double ANA;  // Abstraction - Average Number of Ancestors
    public Double DAM;  // Encapsulation - Data Access Metric
    public Long DCC; // Coupling - Direct Class Coupling
    public Double CAM;  // Cohesion - Cohesion Among Methods in Class
    public Long MOA; // Composition - Measure of Aggregation
    public Double MFA;  // Inheritance - Measure of Functional Abstraction
    public Long NOP;    // Polymorphism - Number of Polymorphic Methods
    public Long CIS; // Messaging - Class Interface Size
    public Long NOM;    // Complexity - Number of Methods

    public Double Reusability;
    public Double Flexibility;
    public Double Understandability;
    public Double Functionality;
    public Double Extendibility;
    public Double Effectiveness;

    Node( ){ }
    Node(Declaration d)
    {
        dec = d;
    }
    Node(Node n)
    {
        dec = n.dec;
        children = new ArrayList<Integer>(n.children);
        parents = new ArrayList<Integer>(n.parents);
        rootHeight = new ArrayList<Integer>(n.rootHeight);

        ANA = n.ANA;
        DAM = n.DAM;
        DCC = n.DCC;
        CAM = n.CAM;
        MOA = n.MOA;
        MFA = n.MFA;
        NOP = n.NOP;
        CIS = n.CIS;
        NOM = n.NOM;

        Reusability = n.Reusability;
        Flexibility = n.Flexibility;
        Understandability = n.Understandability;
        Functionality = n.Functionality;
        Extendibility = n.Extendibility;
        Effectiveness = n.Effectiveness;
    }
}
