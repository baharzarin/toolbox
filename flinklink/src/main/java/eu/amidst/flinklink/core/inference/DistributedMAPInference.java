package eu.amidst.flinklink.core.inference;

import eu.amidst.core.inference.MAPInferenceRobustNew;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by dario on 27/4/16.
 */
public class DistributedMAPInference {

    private BayesianNetwork model;
    private List<Variable> MAPVariables;
    private Assignment evidence;

    private int seed = 0;
    private int numberOfIterations = 100;
    private int numberOfStartingPoints = 50;
    private int sampleSizeEstimatingProbabilities = 100;
    private int samplingSize = 5000;

    private Assignment MAPEstimate;
    private double MAPEstimateLogProbability;

    private int numberOfCoresToUse = 2;

    public void setNumberOfCores(int numberOfCoresToUse) {
        this.numberOfCoresToUse = numberOfCoresToUse;
    }

    public void setModel(BayesianNetwork model) {
        this.model = model;
    }

    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
    }

    public void setMAPVariables(List<Variable> MAPVariables) {
        this.MAPVariables = MAPVariables;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    public void setNumberOfStartingPoints(int numberOfStartingPoints) {
        this.numberOfStartingPoints = numberOfStartingPoints;
    }

    public void setSampleSizeEstimatingProbabilities(int sampleSizeEstimatingProbabilities) {
        this.sampleSizeEstimatingProbabilities = sampleSizeEstimatingProbabilities;
    }

    public void setSampleSize(int samplingSize) {
        this.samplingSize = samplingSize;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public Assignment getEstimate() {
        return MAPEstimate;
    }

    public double getLogProbabilityOfEstimate() {
        return MAPEstimateLogProbability;
    }

    public void runInference() throws Exception {
        this.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);
    }

    public void runInference(MAPInferenceRobustNew.SearchAlgorithm searchAlgorithm) throws Exception {

        //MAPVariables = new ArrayList<>(1);
        //MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        BasicConfigurator.configure(new NullAppender());

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


        env.setParallelism(this.numberOfCoresToUse);
        env.getConfig().disableSysoutLogging();



        int endSequence = numberOfStartingPoints;
        if (searchAlgorithm.equals(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING)) {
            endSequence = 2 * numberOfCoresToUse;
        }

        final int numberOfSamplesOrStartingPoints = searchAlgorithm.equals(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING) ? samplingSize / (2 * numberOfCoresToUse) : 1;
        Tuple2<Assignment, Double> MAPResult = env.generateSequence(1, endSequence)
                .map(new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, numberOfSamplesOrStartingPoints, sampleSizeEstimatingProbabilities))
//                        aa -> {
//                    if(searchAlgorithm.equals(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING))
//                        return new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, numberOfStartingPoints/(2*numberOfCoresToUse));
//                    else
//                        return new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, 1);
//                      })
                .reduce(new ReduceFunction<Tuple2<Assignment, Double>>() {
                    public Tuple2<Assignment, Double> reduce(Tuple2<Assignment, Double> tuple1, Tuple2<Assignment, Double> tuple2) {
                        if (tuple1.f1 > tuple2.f1) {
                            return tuple1;
                        } else {
                            return tuple2;
                        }
                    }
                }).collect().get(0);

        this.seed = new Random(seed).nextInt();
        MAPEstimate = MAPResult.f0;
        MAPEstimateLogProbability = MAPResult.f1;

    }


    static class LocalMAPInference implements MapFunction<Long, Tuple2<Assignment, Double>> {

        private BayesianNetwork model;
        private List<Variable> MAPVariables;
        private Assignment evidence;
        private int seed;
        private int numberOfIterations;
        private int numberOfStartingPoints;
        private int sampleSizeForEstimatingProbabilities;
        private int samplingSize;
        private String searchAlgorithm;


        public LocalMAPInference(BayesianNetwork model, List<Variable> MAPVariables, MAPInferenceRobustNew.SearchAlgorithm searchAlgorithm, Assignment evidence, int numberOfIterations, int seed, int numberOfStartingPoints, int sampleSizeForEstimatingProbabilities) {
            this.model = model;
            this.MAPVariables = MAPVariables;
            this.evidence = evidence;
            this.numberOfIterations = numberOfIterations;
            this.seed = seed;
            this.searchAlgorithm = searchAlgorithm.name();
            this.sampleSizeForEstimatingProbabilities = sampleSizeForEstimatingProbabilities;

            if(searchAlgorithm==MAPInferenceRobustNew.SearchAlgorithm.SAMPLING) {
                this.numberOfStartingPoints = 0;
                this.samplingSize = numberOfStartingPoints;
            }
            else {
                this.numberOfStartingPoints = numberOfStartingPoints;
                this.samplingSize = 0;
            }
        }

        @Override
        public Tuple2<Assignment, Double> map(Long value) throws Exception {

            MAPInferenceRobustNew localMAPInference = new MAPInferenceRobustNew();
            localMAPInference.setModel(this.model);
            localMAPInference.setMAPVariables(this.MAPVariables);
            localMAPInference.setSeed(this.seed + value.intValue());

            localMAPInference.setNumberOfStartingPoints(this.numberOfStartingPoints);
            localMAPInference.setSampleSize(this.samplingSize);

            localMAPInference.setNumberOfIterations(this.numberOfIterations);
            localMAPInference.setSampleSizeEstimatingProbabilities(this.sampleSizeForEstimatingProbabilities);

            localMAPInference.setParallelMode(false);
            localMAPInference.setEvidence(this.evidence);

            localMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.valueOf(this.searchAlgorithm));

            Assignment MAPEstimate = localMAPInference.getEstimate();
            double logProbMAPEstimate = localMAPInference.getLogProbabilityOfEstimate();

            return new Tuple2<>(MAPEstimate, logProbMAPEstimate);
        }
    }

    public static void main(String[] args) throws Exception {

        // Bayesian network model
        BayesianNetworkGenerator.setNumberOfMultinomialVars(8, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(30);
        BayesianNetwork model = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(model);

        DistributedMAPInference distributedMAPInference =  new DistributedMAPInference();
        distributedMAPInference.setModel(model);

        // MAP Variables
        List<Variable> MAPVariables = new ArrayList<>(1);
        MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        distributedMAPInference.setMAPVariables(MAPVariables);

        // Evidence
        Assignment evidence = new HashMapAssignment(2);
        evidence.setValue(model.getVariables().getVariableByName("DiscreteVar0"),0);
        evidence.setValue(model.getVariables().getVariableByName("GaussianVar0"),3);

        distributedMAPInference.setEvidence(evidence);


        // Set parameters and run inference
        distributedMAPInference.setSeed(28235);
        distributedMAPInference.setNumberOfIterations(300);
        distributedMAPInference.setNumberOfStartingPoints(40);

        distributedMAPInference.runInference();

        // Show results
        System.out.println(distributedMAPInference.getEstimate().outputString(MAPVariables));
        System.out.println("Estimated probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
        System.out.println("Estimated log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

    }

}
