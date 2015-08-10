package nl.rug.search.cpptool.metrics;

import com.sun.net.httpserver.Filter;
import nl.rug.search.cpptool.api.*;
import nl.rug.search.cpptool.api.data.*;
import nl.rug.search.cpptool.api.util.IterTools;

import java.lang.annotation.Inherited;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * Calculation of following 11 design metrics in functions with respective names
 *      Design      |   Derived Design
 *     Property     |       Metric
 * -----------------|------------------------------
 * Design Size      - DSC - Design Size in Classes
 * Hierarchies      - NOH - Number of Hierarchies
 * Abstraction      - ANA - Average Number of Ancestors
 * Encapsulation    - DAM - Data Access Metric
 * Coupling         - DCC - Direct Class Coupling
 * Cohesion         - CAM - Cohesion Among Methods in Class
 * Composition      - MOA - Measure of Aggregation
 * Inheritance      - MFA - Measure of Functional Abstraction
 * Polymorphism     - NOP - Number of Polymorphic Methods
 * Messaging        - CIS - Class Interface Size
 * Complexity       - NOM - Number of Methods
 *
 * Calculation of following QMOOD quality attributes:
 * 1-)Reusability
 * 2-)Flexibity
 * 3-)Understandability
 * 4-)Functionality
 * 5-)Extendibility
 * 6-)Effectiveness*
 *
 *
 * @author Can Berker Cikis <canberker@sabanciuniv.edu>
 * @since 2015.07.21
 */
public class Metrics {

    /**
     *
     * @param list  -   List to search in
     * @param dc    -   Declaration to be searched in the Nodes of the list
     * @return      -   Index of the Node containing the declaration, -1 if not found
     */
    public static int searchList(List <Node> list, Declaration dc)
    {
        int index = -1;
        Iterator<Node> i = list.iterator();
        while(i.hasNext())
        {
            index ++;
            Node n = i.next();
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
    public static void setHeight(Node root, List<Node> classes, int rootHeight)
    {
        // set height value of the current root
        Node n = new Node(root);
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
    public static void setInherited(Node root, List<Node> classes, List<Declaration> inheritChain, boolean privateFound)
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
            if(chainIndex.data(CxxFunction.class).get().isVirtual()) // if method is not virtual, it's definetely new and unique(not overridden)
            {
                // Methods owned by the class
                Iterator<Declaration> i4 = IterTools.stream(root.dec.selfContext().get().declarations()).filter(T::isFunction).iterator();
                while (i4.hasNext()) {
                    Declaration selfIndex = i4.next();
                    if (chainIndex.name().get().equals(selfIndex.name().get())) {
                        override = true;
                        // method has been overridden, so it won't be counted as +1 to inherited methods -- do nothing
                    }
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
            if(ownedMethod.data(CxxFunction.class).get().isVirtual())       // if method is not virtual, it's definetely new and unique(not overridden)
            {
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
     * initialize an empty list in the global-main context to give as input to metric functions after calling buildClasses
     * @param classes   -
     * @param result
     */
    public static void buildClasses(List<Node> classes, DeclContainer result)
    {
        // initialize class Nodes
        IterTools.stream(result).filter(T::isCxxClass).forEach(cl -> {
            classes.add(new Node(cl));
        });

        // set child-parent relations
        classes.forEach(cl -> {
            cl.dec.data(CxxRecord.class).get().parents().forEach(p -> {
                if(p.type().declaration().isPresent()) {
                    Integer parentIndex = searchList(classes, p.type().declaration().get());
                    cl.parents.add(parentIndex);

                    Integer childIndex = searchList(classes, cl.dec);
                    Node n = new Node(classes.get(parentIndex));
                    n.children.add(childIndex);
                    classes.set(parentIndex, n);
                }
                else
                {
                    System.out.println("Class \"" + cl.dec.name().get() + "\" has a parent \"" + p.type().name() +  "\" which is a reference to an outside class(a class with no declaration, no .hpp/.cpp provided)"
                            + "\nNo methods will be inherited from the mentioned parent class to child in the inheritance model of the cpptool-lib, as they are not provided to the compiler.");
                }
            });
        });

        // set heights in hierarchy
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            int heightCounter = 0;
            Node classIndex = i1.next();
            if(classIndex.parents.size() == 0)  // it it's a root class
            {
                setHeight(classIndex, classes, heightCounter);
            }
        }

        // set inherited functions
        Iterator<Node> i2 = classes.iterator();
        while(i2.hasNext())
        {
            Node classIndex = i2.next();
            List<Declaration> inheritChain = new ArrayList<Declaration>();
            boolean privateFound = false;
            if(classIndex.parents.size() == 0 )  // it it's a root class
            {
                setInherited(classIndex, classes, inheritChain, privateFound);
            }
        }
    }

    /**
     * 1. DSC - Design Size in Classes
     *
     * Structs with constructors are inclusive
     *
     * @output Names of all classes, Total count of all classes
     * @param classes
     * @return Total count of all classes
     */
    public static long DSC(List<Node> classes)
    {
        System.out.println("\nDSC");
        //Names of Classes
        classes.forEach(c -> {
            System.out.println(c.dec.name().get() + "\t" + c.dec.data(CxxRecord.class).get().variant().name());
        });
        // Number of Classes
        int count = classes.size();
        System.out.println(count);
        return count;
    }

    /**
     * 2. NOH - Number of Hierarchies
     * @output Names of roots of hierarchies, Number of hierarchies
     * @param classes  -  Requires a classes list built by the buildInheritance function
     * @return Number of hierarchies = Number of root classes(no parents) with at least 1 children
     */
    public static int NOH(List<Node> classes)
    {
        System.out.println("\nNOH");
        System.out.println("ROOTS OF HIERARCHIES: ");
        Iterator<Node> i1 = classes.iterator();
        int rootCounter = 0;
        while(i1.hasNext())
        {
            Node classIndex = i1.next();
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
    public static void ANA(List<Node> classes)
    {
        System.out.println("\nANA");
        System.out.println("ROOTS OF HIERARCHIES: ");

        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.name().get());
            Iterator<Integer> i2 = c.rootHeight.iterator();
            double heightSum = 0;
            while (i2.hasNext()) {
                heightSum += i2.next().doubleValue();
            }
            double average = heightSum / c.rootHeight.size();
            System.out.println("Average Number of Ancestors(ANA): " + average + "\n");
            c.ANA = average;
        }
    }

    /**
     * 4. DAM - Data Metric Access - Private/All Attributes
     * @output Number of Private Variables -  Public Variables - Rate of Private/All
     * @param classes
     */
    public static void DAM(List<Node> classes)
    {
        System.out.println("\nDAM");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get());

            double publicCount = 0;
            double privateCount = 0;
            Iterator<Declaration> i = IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isVariable).iterator();
            while (i.hasNext()) {
                Declaration index = i.next();
                if (index.data(Field.class).get().access() == Access.PUBLIC) {
                    publicCount++;
                } else {
                    privateCount++;
                }
            }

            if (publicCount + privateCount != 0) {
                System.out.println("Private Variable Count: " + privateCount + "\t\t" + "Public Variable Count: " + publicCount
                        + "\nDAM: " + privateCount / (privateCount + publicCount) + "\n");
                c.DAM = privateCount / (privateCount + publicCount);

            } else        // prevent division by 0 -- (0/0)
            {
                System.out.println("Private Variable Count: " + privateCount + "\t\t" + "Public Variable Count: " + publicCount
                        + "\nDAM: " + "0.0\n");
                c.DAM = 0.0;
            }
        }
    }

    /**
     * 5. DCC - Direct Class Coupling
     * @param classes
     */
    public static void DCC(List<Node> classes)
    {
        System.out.println("\nDCC");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get());

            Set relatedClass = new HashSet();
            // CHECK ALL FUNCTION PARAMETERS FOR CLASS RELATION, STRINGS INCLUSIVE( +1)
            IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction).forEach(f -> {
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
            IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
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
            c.DCC = (long)relatedClass.size();
            // for output format, seperator between classes
            System.out.println("\n---------------------------------------------------------------------------\n");
        }
    }


    /**
     * 6. CAM - Cohesion Among Methods of Class
     * @param classes
     */
    public static void CAM(List<Node> classes)
    {
        System.out.println("\nCAM");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get());

            Set allParams = new HashSet();   //all parameters in a class
            IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction).forEach(f -> {
                f.data(CxxFunction.class).get().params().params().forEach(p -> {
                    allParams.add(p.type().name());
                });
            });
            double classParamCount = allParams.size();
            System.out.println("List of all parameter types in the class:\n"
                    + allParams + "\n"
                    + classParamCount
                    + "\n");

            double paramSum = 0;
            double funcCount = 0;
            Iterator<Declaration> i2 = IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction).iterator();
            while (i2.hasNext()) {
                Declaration f = i2.next();
                funcCount++;
                Set funcParams = new HashSet();   //all parameters in a function
                f.data(CxxFunction.class).get().params().params().forEach(p -> {
                    funcParams.add(p.type().name());
                });

                double funcParamCount = funcParams.size();
                paramSum += funcParamCount;
                if (funcParamCount + classParamCount != 0) {
                    System.out.println("List of all parameter types in the function:\t " + f.name().get() + "\n"
                            + funcParams + "\n"
                            + funcParamCount + "\n"
                            + "\n");
                } else {
                    System.out.println("List of all parameter types in the function:\t " + f.name().get() + "\n"
                            + funcParams + "\n"
                            + funcParamCount + "\n"
                            + "\n");
                }
            }

            if (classParamCount == 0)
            {
                System.out.println("CAM ratio:\t 0.0");
                c.CAM = 0.0;
            }
            else
            {
                System.out.println("CAM ratio:\t" + paramSum / (funcCount * classParamCount));
                c.CAM = paramSum / (funcCount * classParamCount);
            }
            // for output format, seperator between classes
            System.out.println("\n---------------------------------------------------------------------------\n");

        }
    }


    /**
     * 7. MOA - Measure of Aggregation
     * @param classes
     */
    public static void MOA(List<Node> classes)
    {
        System.out.println("\nMOA");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get());

            Set userDefined = new HashSet();
            // # OF VARIABLES WITH USER DEFINED TYPES, STRINGS EXCLUDED
            IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                if ((!v.data(Field.class).get().type().isBuiltin()
                        && !v.data(Field.class).get().type().declaration().isPresent()) // user defined classes like std::string -> has location but no declaration
                        || (v.data(Field.class).get().type().declaration().isPresent()  // user defined classes with proper location and declaration as RECORD
                        && v.data(Field.class).get().type().declaration().get().declarationType() == DeclType.RECORD
                        && v.data(Field.class).get().type().declaration().get().has(CxxRecord.class))) {
                    userDefined.add(v.data(Field.class).get().type().name());
                }
            });

            System.out.println("Variables with user-defined types:\n" + userDefined + "\n" + userDefined.size());
            c.MOA = (long)userDefined.size();
            // for output format, seperator between classes
            System.out.println("---------------------------------------------------------------------------\n");
        }
    }

    /**
     * 8. MFA - Measure of Functional Abstraction
     * @output MFA value for each individual class
     * @param classes  -  Requires a classes list built by the buildInheritance function
     */
    public static void MFA(List<Node> classes)
    {
        System.out.println("\nMFA");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.name().get());

            Stream<Declaration> selfMethods = IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction);

            double inheritCount = c.inherited.size();
            double selfCount =  selfMethods.count();
            if(inheritCount + selfCount == 0)
            {
                System.out.println("Number of Inherited Methods: " + inheritCount
                        + "\nNumber of All Accessible Methods: " + (inheritCount + selfCount)
                        + "\nMFA: 0.0\n");
                c.MFA = 0.0;
            }
            else    // prevent 0/0 -> NaN
            {
                System.out.println("Number of Inherited Methods: " + inheritCount
                        + "\nNumber of All Accessible Methods: " + (inheritCount + selfCount)
                        + "\nMFA: " + (inheritCount / (inheritCount + selfCount)) + "\n");
                c.MFA = (inheritCount / (inheritCount + selfCount));
            }
        }
    }


    /**
     * 9. NOP - Number of Polymorphic Methods
     * @param classes
     */
    public static void NOP(List<Node> classes)
    {
        System.out.println("\nNOP");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
                System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                        +"\t\t\t\tName: " + c.dec.selfContext().get().name().get()
                        +"\t\t\t\tParent: " + c.dec.selfContext().get().parent().name().get());

                System.out.println("VIRTUAL FUNCTIONS");
                IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction)
                        .filter(d -> d.data(CxxFunction.class).get().isVirtual())
                        .forEach(f -> {
                            System.out.println(f.name().get());
                        });

                long virtualCount = IterTools.stream(c.dec.selfContext().get().declarations())
                        .filter(T::isFunction)
                        .filter(d -> d.data(CxxFunction.class).get().isVirtual())
                        .count();

                System.out.println(virtualCount);
                c.NOP = virtualCount;

                // for output format, seperator between classes
                System.out.println("\n---------------------------------------------------------------------------\n");
            }
    }


    /**
     * 10. CIS - Class Interface Size - Number of public methods
     * @param classes
     */
    public static void CIS(List<Node> classes)
    {
        System.out.println("\nCIS");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get()
                    + "\t\t\t\tParent: " + c.dec.selfContext().get().parent().name().get());

            long count = 0;
            System.out.println("PUBLIC FUNCTIONS");
            Iterator<Declaration> i = IterTools.stream(c.dec.selfContext().get().declarations())
                    .filter(T::isFunction)
                    .filter(f -> f.data(CxxFunction.class).get().access() == Access.PUBLIC).iterator();

            while (i.hasNext()) {
                Declaration index = i.next();
                System.out.println(index.name().get());
                count++;
            }
            System.out.println("Number of Public Functions: " + count + "\n");
            c.CIS = count;
        }
    }

    /**
     * 11. NOM - Number of Methods
     * @param classes
     */
    public static void NOM(List<Node> classes)
    {
        System.out.println("\nNOM");
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            System.out.println(c.dec.data((CxxRecord.class)).get().variant().name()
                    + "\t\t\t\tName: " + c.dec.selfContext().get().name().get()
                    + "\t\t\t\tParent: " + c.dec.selfContext().get().parent().name().get());

            System.out.println("FUNCTIONS");
            IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction).forEach(f -> {
                System.out.println(f.name().get());

            });
            long funcCount = IterTools.stream(c.dec.selfContext().get().declarations()).filter(T::isFunction).count();

            System.out.println(funcCount + "\n");
            c.NOM = funcCount;
        }
    }

    /**
     * DSC and NOH values are calculated and returned by the functions with the respective names.
     * These values are calculated by taking all classes into calculation as a whole.
     * They are not stored as a part of each class to avoid redundancy, as they have the same value for all classes.
     * @param classes
     * @param DSC
     * @param NOH
     */
    public static void QMOOD(List<Node> classes, Long DSC, Integer NOH)
    {
        System.out.println("\nQMOOD");
        // DSC - Design Size - Design Size in Classes
        // NOH - Hierarchies - Number of Hierarchies
        // ANA - Abstraction - Average Number of Ancestors
        // DAM - Encapsulation - Data Access Metric
        // DCC - Coupling - Direct Class Coupling
        // CAM - Cohesion - Cohesion Among Methods in Class
        // MOA - Composition - Measure of Aggregation
        // MFA - Inheritance - Measure of Functional Abstraction
        // NOP - Polymorphism - Number of Polymorphic Methods
        // CIS - Messaging - Class Interface Size
        // NOM - Complexity - Number of Methods
        Iterator<Node> i1 = classes.iterator();
        while(i1.hasNext())
        {
            Node c = i1.next();
            double Reusability = ((-0.25)*c.DCC) + ((0.25)*c.CAM) + ((0.5)*c.CIS) + ((0.5)*DSC);
            c.Reusability = Reusability;
            double Flexibility = ((0.25)*c.DAM) + ((-0.25)*c.DCC) + ((0.5)*c.MOA) + ((0.5)*c.NOP);
            c.Flexibility = Flexibility;
            double Understandability = ((-0.33)*c.ANA) + ((0.33)*c.DAM) + ((-0.33)*c.DCC) + ((0.33)*c.CAM) + ((-0.33)*c.NOP) + ((-0.33)*c.NOM) + ((-0.33)*DSC);
            c.Understandability = Understandability;
            double Functionality = ((0.12)*c.CAM) + ((0.22)*c.NOP) + ((0.22)*c.CIS) + ((0.22)*DSC) + ((0.22)*c.NOP);
            c.Functionality = Functionality;
            double Extendibility = ((0.5)*c.ANA) + ((-0.5)*c.DCC) + ((0.5)*c.MFA) + ((0.5)*c.NOP);
            c.Extendibility = Extendibility;
            double Effectiveness = ((0.2)*c.ANA) + ((0.2)*c.DAM) + ((0.2)*c.MOA) + ((0.2)*c.MFA) + ((0.2)*c.NOP);
            c.Effectiveness = Effectiveness;

            System.out.println("Class: " + c.dec.name().get()
                    + "\nReusability: " + Reusability
                    + "\nFlexibility: " + Flexibility
                    + "\nUnderstandability: " + Understandability
                    + "\nFunctionality: " + Functionality
                    + "\nExtendibility: " + Extendibility
                    + "\nEffectiveness: " + Effectiveness
                    + "\n"
                    + "\n---------------------------------------------------------------------------\n");
        }
    }
}
