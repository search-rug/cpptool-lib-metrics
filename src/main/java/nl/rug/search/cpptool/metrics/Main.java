package nl.rug.search.cpptool.metrics;

import nl.rug.search.cpptool.api.DeclContainer;
import nl.rug.search.cpptool.api.DeclType;
import nl.rug.search.cpptool.api.Declaration;
import nl.rug.search.cpptool.api.T;
import nl.rug.search.cpptool.api.data.CxxFunction;
import nl.rug.search.cpptool.api.data.CxxRecord;
import nl.rug.search.cpptool.api.io.Assembler;
import nl.rug.search.cpptool.api.util.IterTools;
import nl.rug.search.cpptool.runtime.data.CxxRecordData;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class Main {
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
        Stream<Declaration> classes = IterTools.stream(result).filter(T::isCxxClass);

        classes.forEach(c -> {
            System.out.println(c.name());
            CxxRecord c_data = c.data(CxxRecord.class).get(); // Get the c++ class data

            System.out.println("\tPARENTS");
            c_data.parents().forEach(p -> System.out.println("\t" + p.name())); // Print parents names

            System.out.println("\tMETHODS");
            c.selfContext().ifPresent(c_ctx -> {
                c_ctx.declarations().forEach(c_decl -> {
                    if (c_decl.has(CxxFunction.class)) {
                        CxxFunction m_data = c_decl.data(CxxFunction.class).get();
                        System.out.println("\t" + c_decl.name());
                        m_data.params().params().forEach(p -> System.out.println("\t\t" + p.toString()));
                    }
                });
            });
        });
    }
}
