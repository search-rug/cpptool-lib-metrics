package nl.rug.search.cpptool.metrics;

import nl.rug.search.cpptool.api.DeclContainer;
import nl.rug.search.cpptool.api.Declaration;
import nl.rug.search.cpptool.api.T;
import nl.rug.search.cpptool.api.data.CxxFunction;
import nl.rug.search.cpptool.api.data.CxxRecord;
import nl.rug.search.cpptool.api.data.CxxVariable;
import nl.rug.search.cpptool.api.util.IterTools;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by canberker on 21.07.2015.
 */
public class Metrics {

    public static class node {
        public String name;     // keeping names of each class
        public List<Integer> height = new ArrayList<Integer>();    // keeping all height values for different hierarchies in a list
    }

    public static void calcHeight(Declaration r, List<node> cl, int rootHeight)
    {
        boolean found = false;      //to check whether the classes is included in the list previously
        Iterator<node> i = cl.iterator();
        while(i.hasNext())
        {
            node index = i.next();
            if(index.name == r.name().get())    // try to match class' name with previous names of the list
            {
                index.height.add(rootHeight);
                found = true;
            }
        }
        if(found == false)      //if no matches found, add to list
        {
            node n = new node();
            n.name = r.name().get();
            n.height.add(rootHeight);
            cl.add(n);
        }

        rootHeight ++;  //increment for the next level - children of the current root
        Iterator<Declaration> i2 = IterTools.stream(r.selfContext().get().declarations()).filter(T::isCxxClass).iterator();
        while(i2.hasNext())
        {
            calcHeight(i2.next(), cl, rootHeight);      // call recursively for all class children of the current root
        }
    }

    /**
     * DSC - Design Size in Classes
     *
     * Structs with constructors are inclusive
     *
     * @output Names of all classes, Total count of all classes
     * @param result
     * @return Total count of all classes
     */
    public static long DSC(DeclContainer result)
    {
        System.out.println("\nDSC");
        //Names of Classes
        Stream<Declaration> classes = IterTools.stream(result).filter(T::isCxxClass);
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .filter(c -> IterTools.stream((c.selfContext().get().declarations())).filter(T::isFunction).count() != 0)
                .forEach(r -> {
                    System.out.println(r.name().get() + "\t" + r.data(CxxRecord.class).get().variant().name());
                });
        // Number of Classes
        long count = IterTools.stream(result)
                .filter(T::isCxxClass)
                .filter(c -> IterTools.stream((c.selfContext().get().declarations())).filter(T::isFunction).count() != 0)
                .count();
        System.out.println(count);
        return count;
    }

    /**
     * NOH - Number of Hierarchies
     * @output Names of roots of hierarchies, Number of hierarchies
     * @param result
     * @return Number of all hierarchies calculated from top -> bottom, root classes with no children are ignored
     */
    public static int NOH(DeclContainer result)
    {
        System.out.println("\nNOH");
        // GET CLASS CHILDREN OF NAMESPACES -- ROOTS
        List<Declaration> classes = new ArrayList<Declaration>();

        result.context().children().forEach(ns -> {
            IterTools.stream(ns.declarations())
                    .filter(T::isCxxClass)
                    .filter(c -> IterTools.stream(c.selfContext().get().declarations()).filter(T::isCxxClass).count() > 0)
                    .forEach(c -> {
                        classes.add(c);
                    });

        });
        System.out.println("ROOTS OF HIERARCHIES: ");
        classes.forEach(c -> {
            System.out.println(c.name().get());
        });
        System.out.println(classes.size());
        return classes.size();
    }

    /**
     * ANA - Average Number of Ancestors
     * @output ANA value for each individual class
     * @param result
     */
    public static void ANA(DeclContainer result)
    {
        System.out.println("\nANA");
        // class container for recursive calls
        List<node> classes = new ArrayList<node>();

        // recursively call height calculation for all root classes
        result.context().children().forEach(ns -> {
            IterTools.stream(ns.declarations())
                    .filter(T::isCxxClass)
                    .forEach(r -> {
                        calcHeight(r, classes, 0);
                    });
        });

        Iterator<node> c = classes.iterator();
        while(c.hasNext())
        {
            node cl = c.next();
            System.out.println(cl.name);
            Iterator<Integer> i = cl.height.iterator();
            double sum = 0;
            while (i.hasNext()) {
                sum += i.next().doubleValue();
            }
            double ancestor = sum / cl.height.size();
            System.out.println("Average Number of Ancestors(ANA): " + ancestor + "\n");
        }
    }


    // 4.
    // Data Metric Access - DAM - Private/All Attributes
    // private public ????

    /**
     * DCC - Direct Class Coupling
     * @param result
     */
    public static void DCC(DeclContainer result)
    {
        System.out.println("\nDCC");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(c -> {
                    System.out.println(c.data((CxxRecord.class)).get().variant().name()
                            + "\t\t\t\tName: " + c.selfContext().get().name().get());

                    Set relatedClass = new HashSet();
                    // CHECK ALL FUNCTION PARAMETERS FOR CLASS RELATION, STRINGS INCLUSIVE( +1)
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isFunction).forEach(f -> {
                        f.data(CxxFunction.class).get().params().params().forEach(p -> {
                            if (!p.type().isBuiltin()) {
                                relatedClass.add(p.type().name());
                            }
                        });
                    });

                    // CHECK ALL VARIABLE DEFINITIONS FOR CLASS RELATION
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                        if (!v.data(CxxVariable.class).get().type().isBuiltin()) {
                            relatedClass.add(v.data(CxxVariable.class).get().type().name());
                        }
                    });

                    System.out.println(relatedClass + "\n" + relatedClass.size());
                    System.out.println("---------------------------------------------------------------------------\n");
                });
                // for output format, seperator between classes
    }


    /**
     * CAM - Cohesion Among Methods of Class
     * @param result
     */
    public static void CAM(DeclContainer result)
    {
        System.out.println("\nCAM");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(c -> {
                    System.out.println(c.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + c.selfContext().get().name().get());

                    Set allParams = new HashSet();   //all parameters in a class
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isFunction).forEach( f-> {
                        f.data(CxxFunction.class).get().params().params().forEach(p -> {
                            allParams.add(p.type().name());
                        });
                    });
                    double classCount = allParams.size();
                    System.out.println("List of all parameter types in the class:\n"
                            + allParams + "\n"
                            + classCount
                            + "\n");

                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isFunction).forEach(f -> {
                        Set funcParams = new HashSet();   //all parameters in a function
                        f.data(CxxFunction.class).get().params().params().forEach(p -> {
                                funcParams.add(p.type().name());
                        });
                        double funcCount = funcParams.size();
                        System.out.println("List of all parameter types in the function:\t " + f.name().get() + "\n"
                                + funcParams + "\n"
                                + funcCount + "\n"
                                + "CAM ratio:\t" + (funcCount / classCount)
                                + "\n");
                    });
                    // for output format, seperator between classes
                    System.out.println("---------------------------------------------------------------------------\n");

                });
    }


    /**
     * MOA - Measure of Aggregation
     * @param result
     */
    public static void MOA(DeclContainer result)
    {
        System.out.println("\nMOA");
        IterTools.stream(result)
            .filter(T::isCxxClass)
            .forEach(c -> {
                System.out.println(c.data((CxxRecord.class)).get().variant().name()
                        + "\t\t\t\tName: " + c.selfContext().get().name().get());

                Set userDefined = new HashSet();
                // # OF VARIABLES WITH USER DEFINED TYPES, STRINGS EXCLUDED
                IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                    if (!v.data(CxxVariable.class).get().type().isBuiltin()
                            && !v.data(CxxVariable.class).get().type().name().equals("::std::__1::string")) {
                        userDefined.add(v.data(CxxVariable.class).get().type().name());
                    }
                });

                System.out.println(userDefined + "\n" + userDefined.size());
                // for output format, seperator between classes
                System.out.println("---------------------------------------------------------------------------\n");
            });
    }



    // 8.
    // Measure of Functional Abstraction - MFA


    /**
     * NOP - Number of Polymorphic Methods
     * @param result
     */
    public static void NOP(DeclContainer result)
    {
        System.out.println("\nNOP");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(r -> {
                    System.out.println(r.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + r.selfContext().get().name().get()
                            +"\t\t\t\tParent: " + r.selfContext().get().parent().name().get());

                    System.out.println("\nVIRTUAL FUNCTIONS");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction)
                            .filter(d -> d.data(CxxFunction.class).get().isStatic())
                            .forEach(f -> {
                                System.out.println(f.name().get());

                            });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations())
                            .filter(T::isFunction)
                            .filter(d -> d.data(CxxFunction.class).get().isStatic())
                            .count());

                });
    }



    // 10.
    // Class Interface Size - CIS - # of public methods


    /**
     * NOM - Number of Methods
     * @param result
     */
    public static void NOM(DeclContainer result)
    {
        System.out.println("\nNOM");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(r -> {
                    System.out.println(r.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + r.selfContext().get().name().get()
                            +"\t\t\t\tParent: " + r.selfContext().get().parent().name().get());

                    System.out.println("\nFUNCTIONS");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).forEach( f-> {
                        System.out.println(f.name().get());

                    });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).count());
                });
    }



}
