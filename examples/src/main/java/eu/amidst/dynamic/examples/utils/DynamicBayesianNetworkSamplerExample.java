package eu.amidst.dynamic.examples.utils;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.util.Random;

/**
 * This example shows how to use the DynamicBayesianNetworkSampler class to randomly generate a data sample
 * for a given Dynamic Bayesian network.
 *
 * Created by ana@cs.aau.dk on 02/12/15.
 */
public class DynamicBayesianNetworkSamplerExample {
    public static void main(String[] args) throws Exception{

        //We first generate a DBN with 3 continuous and 3 discrete variables with 2 states
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();
        dbnGenerator.setNumberOfContinuousVars(3);
        dbnGenerator.setNumberOfDiscreteVars(3);
        dbnGenerator.setNumberOfStates(2);

        //Create a NB-like structure with temporal links in the children (leaves) and 2 states for
        //the class variable
        DynamicBayesianNetwork network = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(
                new Random(0), 2, true);

        //Create the sampler from this network
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
        sampler.setSeed(0);

        //Sample a dataStream of 3 sequences of 1000 samples each
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(3,1000);

        //Save the created data sample in a file
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/dnb-samples.arff");
    }
}
