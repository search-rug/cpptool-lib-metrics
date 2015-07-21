package nl.rug.search.cpptool.metrics;

import nl.rug.search.cpptool.api.*;
import nl.rug.search.cpptool.api.io.Assembler;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestMain {

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


        Metrics.DSC(result);
        Metrics.NOH(result);
        Metrics.ANA(result);
        //Metrics.DAM(result);
        Metrics.DCC(result);
        Metrics.CAM(result);
        Metrics.MOA(result);
        //Metrics.MFA(result);
        Metrics.NOP(result);
        //Metrics.CIS(result);
        Metrics.NOM(result);


    }
}
