package nl.rug.search.cpptool.metrics;

import com.sun.net.httpserver.Filter;
import nl.rug.search.cpptool.api.*;
import nl.rug.search.cpptool.api.data.*;
import nl.rug.search.cpptool.api.util.IterTools;

import java.lang.annotation.Inherited;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by canberker on 21.07.2015.
 */
public class Metrics {

    /**
     * 1. DSC - Design Size in Classes
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
     * Utility Node & Function for the following metric 2.NOH
     */

    public static class node {
        public Declaration dec;
        public List<Integer> children = new ArrayList<Integer>();
        public List<Integer> parents = new ArrayList<Integer>();
        public List<Integer> rootHeight = new ArrayList<Integer>();
        public Set<Declaration> inherited = new HashSet<Declaration>();

        node( ){ }
        node(Declaration d)
        {
            dec = d;
        }
        node(Declaration d, node p)
        {
            dec = d;
            //parents.add(p);
        }
        node(node n)
        {
            dec = n.dec;
            children = new ArrayList<Integer>(n.children);
            parents = new ArrayList<Integer>(n.parents);
            rootHeight = new ArrayList<Integer>(n.rootHeight);
        }

    }

    /**
     *
     * @param list  -   List to search in
     * @param dc    -   Declaration to be searched in the nodes of the list
     * @return      -   Index of the node containing the declaration, -1 if not found
     */
    public static int searchList(List <node> list, Declaration dc)
    {
        int index = -1;
        Iterator<node> i = list.iterator();
        while(i.hasNext())
        {
            index ++;
            node n = i.next();
            if(n.dec.equals(dc))
            {
                return index;
            }
        }
        index = -1;
        return index;
    }

    /**
     * Function recursively called to set height values to all classes
     * @param root  - current class under operation
     * @param classes   - list containing information on all classes
     * @param rootHeight    - current level of height
     */

    public static void setHeight(node root, List<node> classes, int rootHeight)
    {
        // set height value of the current root
        node n = new node(root);
        n.rootHeight.add(rootHeight);
        int classIndex = searchList(classes, root.dec);
        classes.set(classIndex, n);

        rootHeight++;
        Iterator<Integer> i1 = root.children.iterator();
        while(i1.hasNext())
        {
            Integer childIndex = i1.next();
            setHeight(classes.get(childIndex), classes, rootHeight);
        }
    }

    /**
     * Function recursively called to determine which functions are truly inherited by the class
     * @param root  - current class under operation
     * @param classes   - list containing information on all classes
     * @param inheritChain  - functions are being inherited down from parents-grandparents to children
     * @param privateFound  - variable to check if parent was defined as private, hence causing a break in the inheritance chain
     */

    public static void setInherited(node root, List<node> classes, List<Declaration> inheritChain, boolean privateFound)
    {

        if (privateFound == true)    // private declaration was found in parent, inherited function list should be cleared
        {
            inheritChain.clear();
            privateFound = false;
        }

        // ADD INHERITED & NOT OVERRIDDEN FUNCTIONS FROM INHERITEDCHAIN
        // check if the method in carried in inheritance chain in the hierarchy has been overridden by the class
        Iterator<Declaration> i3 = inheritChain.iterator();
        while (i3.hasNext())
        {
            boolean override = false;
            Declaration chainIndex = i3.next();
            // Methods owned by the class
            Iterator<Declaration> i4 = IterTools.stream(root.dec.selfContext().get().declarations()).filter(T::isFunction).iterator();
            while (i4.hasNext()) {
                Declaration selfIndex = i4.next();
                if (chainIndex.name().get().equals(selfIndex.name().get())) {
                    override = true;
                    // method has been overridden, so it won't be counted as +1 to inherited methods -- do nothing
                }
            }
            if (override == false) {
                root.inherited.add(chainIndex);
            } else {
                //do nothing as explained above
            }
        }

        // ADD OWNED & NOT OVERRIDDEN METHODS TO INHERITED CHAIN, REPLACE THE OVERRIDDEN METHODS W/ NEW INSTANCES
        // Methods owned by the class
        Iterator<Declaration> i1 = IterTools.stream(root.dec.selfContext().get().declarations()).filter(T::isFunction).iterator();
        while (i1.hasNext())
        {
            boolean override = false;
            Declaration ownedMethod = i1.next();
            Iterator<Declaration> i2 = inheritChain.iterator();
            while (i2.hasNext())
            {
                Declaration chainIndex = i2.next();
                if (chainIndex.name().get().equals(ownedMethod.name().get())) {
                    override = true;
                    //if overridden, write the new instance over the old one
                    inheritChain.set(inheritChain.indexOf(chainIndex), ownedMethod);
                }
            }
            if (override == false)  // if method is not overridden, it must be a new element in the inheritance chain
            {
                inheritChain.add(ownedMethod);
            }
        }

        // to prevent extra removals or additions of methods to inherited list of methods.
        // inherited methods list is restored, prior to visiting the next sibling,
        // to prevent bugs that may be caused by the actions of the previous sibling.
        List<Declaration> ChainBackup = new ArrayList<>(inheritChain);
        Iterator<Integer> i5 = root.children.iterator();
        while (i5.hasNext()) {
            inheritChain = new ArrayList<Declaration>(ChainBackup);
            Integer childIndex = i5.next();

            Iterator<CxxRecordParent> i6 = classes.get(childIndex).dec.data(CxxRecord.class).get().parents().iterator();
            while (i6.hasNext()) {
                CxxRecordParent parentIndex = i6.next();
                // find the current root as the parent of the child under investigation
                if (parentIndex.type().name()
                        .equals(root.dec.data(CxxRecord.class).get().type().name())) {
                    if (parentIndex.access() == Access.PRIVATE) {
                        privateFound = true;
                    }
                }
            }
            setInherited(classes.get(childIndex), classes, inheritChain, privateFound);
        }
    }

    /**
     * initialize an empty list in the global / main context to give as input to metric functions after calling buildInheritance
     * @param classes   -
     * @param result
     */

    public static void buildInheritance(List<node> classes, DeclContainer result)
    {
        // initialize class nodes
        IterTools.stream(result).filter(T::isCxxClass).forEach(cl -> {
            classes.add(new node(cl));
        });

        // set child-parent relations
        classes.forEach(cl -> {
            cl.dec.data(CxxRecord.class).get().parents().forEach(p -> {
                Integer parentIndex = searchList(classes, p.type().declaration().get());
                cl.parents.add(parentIndex);

                Integer childIndex = searchList(classes, cl.dec);
                node n = new node(classes.get(parentIndex));
                n.children.add(childIndex);
                classes.set(parentIndex, n);
            });
        });

        // set heights in hierarchy
        Iterator<node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            int heightCounter = 0;
            node classIndex = i1.next();
            if(classIndex.parents.size() == 0)  // it it's a root class
            {
                setHeight(classIndex, classes, heightCounter);
            }
        }

        // set inherited functions
        Iterator<node> i2 = classes.iterator();
        while(i2.hasNext())
        {
            node classIndex = i2.next();
            List<Declaration> inheritChain = new ArrayList<Declaration>();
            boolean privateFound = false;
            if(classIndex.parents.size() == 0 )  // it it's a root class
            {
                setInherited(classIndex, classes, inheritChain, privateFound);
            }
        }

    }


    /**
     * 2. NOH - Number of Hierarchies
     * @output Names of roots of hierarchies, Number of hierarchies
     * @param classes  -  Requires a classes list built by the buildInheritance function
     * @return Number of hierarchies = Number of root classes(no parents) with at least 1 children
     */

    public static int NOH(List<node> classes)
    {
        System.out.println("\nNOH");
        System.out.println("ROOTS OF HIERARCHIES: ");
        Iterator<node> i1 = classes.iterator();
        int rootCounter = 0;
        while(i1.hasNext())
        {
            node classIndex = i1.next();
            if(classIndex.parents.size() == 0 && classIndex.children.size() != 0)  // it it's a root class with children
            {
                rootCounter++;
                System.out.println(classIndex.dec.name().get());
            }
        }
        System.out.println("Number of Hierarchies: " + rootCounter + "\n");

        return rootCounter;
    }


    /**
     * 3. ANA - Average Number of Ancestors
     * @output ANA value for each individual class
     * @param classes  -  Requires a classes list built by the buildInheritance function
     */

    public static void ANA(List<node> classes)
    {
        System.out.println("\nANA");
        System.out.println("ROOTS OF HIERARCHIES: ");

        Iterator<node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            node classIndex = i1.next();
            System.out.println(classIndex.dec.name().get());
            Iterator<Integer> i2 = classIndex.rootHeight.iterator();
            double heightSum = 0;
            while (i2.hasNext()) {
                heightSum += i2.next().doubleValue();
            }
            double average = heightSum / classIndex.rootHeight.size();
            System.out.println("Average Number of Ancestors(ANA): " + average + "\n");
        }
    }

    /**
     * 4. DAM - Data Metric Access - Private/All Attributes
     * @output Number of Private Variables -  Public Variables - Rate of Private/All
     * @param result
     */
    public static void DAM(DeclContainer result)
    {
        System.out.println("\nDAM");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(c -> {
                    System.out.println(c.data((CxxRecord.class)).get().variant().name()
                            + "\t\t\t\tName: " + c.selfContext().get().name().get());

                    double publicCount = 0;
                    double privateCount = 0;
                    Iterator<Declaration> i = IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).iterator();
                    while (i.hasNext())
                    {
                        Declaration index = i.next();
                        if (index.data(Field.class).get().access() == Access.PUBLIC) {
                            publicCount++;
                        }
                        else
                        {
                            privateCount++;
                        }
                    }

                    if(publicCount + privateCount != 0) {
                        System.out.println("Private Variable Count: " + privateCount + "\t\t" + "Public Variable Count: " + publicCount
                                + "\nDAM: " + privateCount / (privateCount + publicCount) + "\n");
                    }
                    else        // prevent division by 0 -- (0/0)
                    {
                        System.out.println("Private Variable Count: " + privateCount + "\t\t" + "Public Variable Count: " + publicCount
                                + "\nDAM: " + "0.0\n");
                    }
                });
    }

    /**
     * 5. DCC - Direct Class Coupling
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
                            if ((!p.type().isBuiltin()
                                    && !p.type().declaration().isPresent()) // user defined classes like std::string -> has location but no declaration
                                    || (p.type().declaration().isPresent()  // user defined classes with proper location and declaration as RECORD
                                    && p.type().declaration().get().declarationType() == DeclType.RECORD
                                    && p.type().declaration().get().has(CxxRecord.class))) {
                                relatedClass.add(p.type().name());
                            }
                        });
                    });

                    // CHECK ALL VARIABLE DEFINITIONS FOR CLASS RELATION
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                        if ((!v.data(Field.class).get().type().isBuiltin()
                                && !v.data(Field.class).get().type().declaration().isPresent()) // user defined classes like std::string -> has location but no declaration
                                || (v.data(Field.class).get().type().declaration().isPresent()  // user defined classes with proper location and declaration as RECORD
                                && v.data(Field.class).get().type().declaration().get().declarationType() == DeclType.RECORD
                                && v.data(Field.class).get().type().declaration().get().has(CxxRecord.class))) {
                            relatedClass.add(v.data(Field.class).get().type().name());
                        }
                    });

                    System.out.println("Names of Related Classes:\t" + relatedClass
                            + "\nNumber of Related Classes: \t" + relatedClass.size());
                    // for output format, seperator between classes
                    System.out.println("\n---------------------------------------------------------------------------\n");
                });
    }


    /**
     * 6. CAM - Cohesion Among Methods of Class
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
                        if(funcCount + classCount != 0) {
                            System.out.println("List of all parameter types in the function:\t " + f.name().get() + "\n"
                                    + funcParams + "\n"
                                    + funcCount + "\n"
                                    + "CAM ratio:\t" + (funcCount / classCount)
                                    + "\n");
                        }
                        else
                        {
                            System.out.println("List of all parameter types in the function:\t " + f.name().get() + "\n"
                                    + funcParams + "\n"
                                    + funcCount + "\n"
                                    + "CAM ratio:\t" + "0.0"
                                    + "\n");
                        }
                    });
                    // for output format, seperator between classes
                    System.out.println("---------------------------------------------------------------------------\n");

                });
    }


    /**
     * 7. MOA - Measure of Aggregation
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
                    if ((!v.data(Field.class).get().type().isBuiltin()
                            && !v.data(Field.class).get().type().declaration().isPresent()) // user defined classes like std::string -> has location but no declaration
                            || (v.data(Field.class).get().type().declaration().isPresent()  // user defined classes with proper location and declaration as RECORD
                            && v.data(Field.class).get().type().declaration().get().declarationType() == DeclType.RECORD
                            && v.data(Field.class).get().type().declaration().get().has(CxxRecord.class)))
                    {
                        userDefined.add(v.data(Field.class).get().type().name());
                    }
                });

                System.out.println(userDefined + "\n" + userDefined.size());
                // for output format, seperator between classes
                System.out.println("---------------------------------------------------------------------------\n");
            });
    }


    /**
     * 8. MFA - Measure of Functional Abstraction
     * @output MFA value for each individual class
     * @param classes  -  Requires a classes list built by the buildInheritance function
     */

    public static void MFA(List<node> classes)
    {
        System.out.println("\nMFA");

        Iterator<node> i = classes.iterator();
        while(i.hasNext())
        {
            node classIndex = i.next();
            System.out.println(classIndex.dec.name().get());

            Stream<Declaration> selfMethods = IterTools.stream(classIndex.dec.selfContext().get().declarations()).filter(T::isFunction);

            double inheritCount = classIndex.inherited.size();
            double selfCount =  selfMethods.count();
            System.out.println("Number of Inherited Methods: " + inheritCount
                    + "\nNumber of All Accessible Methods: " + (inheritCount + selfCount)
                    + "\nMFA: " + (inheritCount / (inheritCount + selfCount)) + "\n");
        }
    }


        /**
         * 9. NOP - Number of Polymorphic Methods
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

                    System.out.println("VIRTUAL FUNCTIONS");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction)
                            .filter(d -> d.data(CxxFunction.class).get().isVirtual())
                            .forEach(f -> {
                                System.out.println(f.name().get());

                            });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations())
                            .filter(T::isFunction)
                            .filter(d -> d.data(CxxFunction.class).get().isVirtual())
                            .count());

                    // for output format, seperator between classes
                    System.out.println("\n---------------------------------------------------------------------------\n");

                });

    }


    /**
     * 10. CIS - Class Interface Size - Number of public methods
     * @param result
     */
    public static void CIS(DeclContainer result)
    {
        System.out.println("\nCIS");
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(r -> {
                    System.out.println(r.data((CxxRecord.class)).get().variant().name()
                            + "\t\t\t\tName: " + r.selfContext().get().name().get()
                            + "\t\t\t\tParent: " + r.selfContext().get().parent().name().get());

                    int count = 0;
                    System.out.println("PUBLIC FUNCTIONS");
                    Iterator<Declaration> i = IterTools.stream(r.selfContext().get().declarations())
                            .filter(T::isFunction)
                            .filter(f -> f.data(CxxFunction.class).get().access() == Access.PUBLIC).iterator();

                    while (i.hasNext()) {
                        Declaration index = i.next();
                        System.out.println(index.name().get());
                        count ++;
                    }
                    System.out.println("Number of Public Functions: " + count + "\n");
                });

    }

    /**
     * 11. NOM - Number of Methods
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

                    System.out.println("FUNCTIONS");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).forEach( f-> {
                        System.out.println(f.name().get());

                    });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).count() + "\n");
                });
    }

}
