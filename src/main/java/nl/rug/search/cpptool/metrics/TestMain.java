package nl.rug.search.cpptool.metrics;

import nl.rug.search.cpptool.api.*;
import nl.rug.search.cpptool.api.data.CxxFunction;
import nl.rug.search.cpptool.api.data.CxxRecord;
import nl.rug.search.cpptool.api.data.CxxVariable;
import nl.rug.search.cpptool.api.io.Assembler;
import nl.rug.search.cpptool.api.util.IterTools;
import nl.rug.search.cpptool.runtime.data.CxxFunctionData;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestMain {

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

        rootHeight ++;  //increment for the next level
        Iterator<Declaration> i2 = IterTools.stream(r.selfContext().get().declarations()).filter(T::isCxxClass).iterator();
        while(i2.hasNext())
        {
            calcHeight(i2.next(), cl, rootHeight);      // call recursively for all class children of the current root
        }
    }


    public static void main(String args[]) throws InterruptedException {
        final Assembler assembler = Assembler.create();

        for (File f : checkNotNull(new File(args[0]).listFiles())) {
            assembler.read(f);
        }

        final DeclContainer result = assembler.build();
        //Alternatively for non-blocking:
//        Futures.addCallback(assembler.deferBuild(), new FutureCallback<DeclContainer>() {
//            @Override
//            public void onSuccess(DeclContainer result) {
//                //Process results
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//
//            }
//        });


        //Validate structure
        //StateValidator.validateGraph(result);
        //StateValidator.validateState(result);

        //Dump structure
        //result.context().dump(System.out);

        //Dump all include relations
        //result.includes().forEach(System.out::println);

        //Find and print all C++ classes in the declaration tree
        //IterTools.stream(result).filter(T::isCxxClass).forEach(System.out::println);




        //*****************************\\
        //*********EXPERIMENTAL********\\
        //*****************************\\

        //PRINT ALL FUNCTIONS
        //IterTools.stream(result).filter(T.filterDeclType(DeclType.FUNCTION)).forEach((System.out::println));

        //DECL. TREE
        //result.context();

        //CLASS FILTER RESULT
        //Stream<Declaration> classes= IterTools.stream(result).filter(T::isCxxClass);


        //USE OF ITERATOR
        /*
        Iterator<DeclContext> it = result.context().children().iterator();
        while(it.hasNext()){
            DeclContext dc = it.next();

        }
        */

        //USE OF FOREACH
        /*
            IterTools.stream(result).filter(T::isCxxClass).forEach( s -> {
            s.data();

        });
        */

        //FOREACH WITH FIRST CHILDREN
        /*
        result.context().children().forEach(s -> {
            System.out.println(s.name());
        });

        ---

        result.context().children().forEach(s->{
            s.declarations().forEach(t->{
                System.out.println(t.name() + "\t" + t.declarationType() + "\t" + t.location() + "\t" + t.parentContext().name());
            });
        });

        */

        //**************************\\
        //********  DANIEL  ********\\
        //**************************\\

        /*
        Stream<Declaration> classes = IterTools.stream(result).filter(T::isCxxClass);
        Stream<Declaration> methods = IterTools.stream(result).filter(T::isClassMember).filter(T::isFunction);

        classes.forEach(c -> {
            System.out.println(c.name());
            CxxRecord d = c.data(CxxRecord.class).get();

            System.out.println("\tPARENTS");
            d.parents().forEach(p -> System.out.println("\t" + p.name()));

            System.out.println("\tMETHODS");
            IterTools.stream(result).filter(T::isClassMember).filter(T::isFunction).filter(f -> f.data(CxxFunction.class).get().parentClass() == d.type()).forEach(m -> System.out.println("\t" + m.name()));
        });


        // FINAL VERSION
        classes.forEach(c -> {
            System.out.println(c.name());
            CxxRecord c_data = c.data(CxxRecord.class).get(); // Get the c++ class data

            System.out.println("\tPARENTS");
            c_data.parents().forEach(p -> System.out.println("\t" + p.name())); // Print parents names

            System.out.println("\tMETHODS");
            IterTools.stream(result)
                    .filter(d -> d.declarationType() == DeclType.FUNCTION && d.has(CxxFunction.class))
                    .filter(f -> f.data(CxxFunction.class).get().parentClass() == c_data.type())
                    .forEach(m -> {
                        CxxFunction m_data = m.data(CxxFunction.class).get();
                        System.out.println("\t" + m.name());
                        m_data.params().params().forEach(p -> System.out.println("\t\t" + p.toString()));
                    });
        });

        */

        //FILTER OUT STRUCTS
        /*
            IterTools.stream(result)
                .filter(T::isCxxClass)
                .filter(s -> s.data(CxxRecord.class).get().variant().name() != "STRUCT")
                .forEach(r -> {
                    System.out.println(r.name() + "\t" + r.data(CxxRecord.class).get().variant().name());
                });

        */

        //VARIANT - DECLARATIONS - FUNCTIONS - CLASSES - CHILDREN

        IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(r -> {
                    System.out.println(r.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + r.selfContext().get().name().get()
                            +"\t\t\t\tParent: " + r.selfContext().get().parent().name().get());

                    System.out.println("\nDECLARATIONS");
                    r.selfContext().get().declarations().forEach(d -> {
                        System.out.println(d.name().get());


                    });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations()).count());

                    System.out.println("\nFUNCTIONS");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).forEach( f-> {
                        System.out.println(f.name().get());

                    });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations()).filter(T::isFunction).count());

                    System.out.println("\nCLASSES");
                    IterTools.stream(r.selfContext().get().declarations()).filter(T::isCxxClass).forEach(f -> {
                        System.out.println(f.name().get());

                    });
                    System.out.println(IterTools.stream(r.selfContext().get().declarations()).filter(T::isCxxClass).count());

                    System.out.println("\nCHILDREN");

                    //IterTools.stream(r.selfContext().get().declarations()).filter()
                    r.selfContext().get().children().forEach(c -> {

                        System.out.println(c.name().get());
                    });
                    System.out.println(IterTools.stream(r.selfContext().get().children()).count());

                    System.out.println("------\n");

                });




/*
        // GET CLASS CHILDREN OF NAMESPACES -- ROOTS
        List<Declaration> classes = new ArrayList<Declaration>();

        result.context().children().forEach(ns -> {
            IterTools.stream(ns.declarations())
                    .filter(T::isCxxClass)
                    .forEach(c -> {
                        classes.add(c);
                    });

        });
        classes.forEach(c -> {System.out.println(c.name().get());});
        System.out.println(classes.size());
*/





        //*****************************\\
        //************METRICS**********\\
        //*****************************\\

        // 1.
        // Design Size in Classes - DSC
        /*
        //Names of Classes
        Stream<Declaration> classes = IterTools.stream(result).filter(T::isCxxClass);
        IterTools.stream(result)
                .filter(T::isCxxClass)
                .filter(c -> IterTools.stream((c.selfContext().get().declarations())).filter(T::isFunction).count() != 0)
                .forEach(r -> {
                    System.out.println(r.name().get() + "\t" + r.data(CxxRecord.class).get().variant().name());
                });
        // Number of Classes
        System.out.println(
                IterTools.stream(result)
                        .filter(T::isCxxClass)
                        .filter(c -> IterTools.stream((c.selfContext().get().declarations())).filter(T::isFunction).count() != 0)
                        .count()
        );
        */





        // 2.
        // Number of Hierarchies - NOH

        // VERSION 1 - CALCULATED TOP - BOTTOM
        /*
        // GET CLASS CHILDREN OF NAMESPACES -- ROOTS
        List<Declaration> classes = new ArrayList<Declaration>();

        result.context().children().forEach(ns -> {
            IterTools.stream(ns.declarations())
                    .filter(T::isCxxClass)
                    .forEach(c -> {
                        classes.add(c);
                    });

        });
        classes.forEach(c -> {System.out.println(c.name().get());});
        System.out.println(classes.size());

        */

        // VERSION 2 - CALCULATED BOTTOM - TOP
        /*
        // filter classes,
        // filter classes with no subclass declarations -- classes at the bottom
        Stream<Declaration> rootClasses =
                IterTools.stream(result)
                        .filter(T::isCxxClass)
                        .filter(c -> IterTools.stream(c.selfContext().get().declarations()).filter(T::isCxxClass).count() == 0);

        System.out.println(rootClasses.count());

        Stream<Declaration> classes = IterTools.stream(result).filter(T::isCxxClass);
        System.out.println(classes.count());
        */




        // 3.
        // Average Number of Ancestors - ANA
         /*
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
        double allSum = 0;
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
            allSum += ancestor;
        }
        double allAncestor = allSum / classes.size();
        System.out.println("Average Number of Ancestors(ANA) of all " + classes.size() + " classes: " + allAncestor);

        */

        // UTILITY OF 3. ANA -- place outside main
        /*
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

            rootHeight ++;  //increment for the next level
            Iterator<Declaration> i2 = IterTools.stream(r.selfContext().get().declarations()).filter(T::isCxxClass).iterator();
            while(i2.hasNext())
            {
                calcHeight(i2.next(), cl, rootHeight);      // call recursively for all class children of the current root
            }
        }
        */





        // 4.
        // Data Metric Access - DAM - Private/All Attributes
        // private public ????

        // 5.
        // Direct Class Coupling - DCC
        /*
           IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(c -> {
                    System.out.println(c.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + c.selfContext().get().name().get());

                    Set relatedClass = new HashSet();
                    // CHECK ALL FUNCTION PARAMETERS FOR CLASS RELATION, STRINGS INCLUSIVE( +1)
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isFunction).forEach( f-> {
                        f.data(CxxFunction.class).get().params().params().forEach(p -> {
                            if(!p.type().isBuiltin()) {
                                relatedClass.add(p.type().name());
                            }
                        });
                    });

                    // CHECK ALL VARIABLE DEFINITIONS FOR CLASS RELATION
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                        if(!v.data(CxxVariable.class).get().type().isBuiltin()) {
                            relatedClass.add(v.data(CxxVariable.class).get().type().name());
                        }
                        });

                    System.out.println(relatedClass + "\n" + relatedClass.size());
                    System.out.println("---------------------------------------------------------------------------\n");
                    });
                    // for output format, seperator between classes
         */





        // 6.
        // Cohesion Among Methods of Class - CAM
        /*
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
        */





        // 7.
        // Measure of Aggregation - MOA
        /*
            IterTools.stream(result)
                .filter(T::isCxxClass)
                .forEach(c -> {
                    System.out.println(c.data((CxxRecord.class)).get().variant().name()
                            +"\t\t\t\tName: " + c.selfContext().get().name().get());

                    Set userDefined = new HashSet();
                    // # OF VARIABLES WITH USER DEFINED TYPES, STRINGS EXCLUDED
                    IterTools.stream(c.selfContext().get().declarations()).filter(T::isVariable).forEach(v -> {
                        if(!v.data(CxxVariable.class).get().type().isBuiltin()
                                && !v.data(CxxVariable.class).get().type().name().equals("::std::__1::string")) {
                        userDefined.add(v.data(CxxVariable.class).get().type().name());
                        }
                    });

                    System.out.println(userDefined + "\n" + userDefined.size());
                    System.out.println("---------------------------------------------------------------------------\n");
                });
        // for output format, seperator between classes

         */







        // 8.
        // Measure of Functional Abstraction - MFA






        // 9.
        // Number of Polymorphic Methods - NOP - Virtuals in Cxx
        /*
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
        */







        // 10.
        // Class Interface Size - CIS - # of public methods








        // 11.
        // Number of Methods - NOM - # of all methods
        /*
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
        */
    }
}
