package eu.amidst.core.Estimators.ConditionalEstimators;

import eu.amidst.core.Estimators.UnConditionalEstimators.GaussianEstimator;
import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticDataBase.DataInstance;

/**
 * Created by afa on 03/07/14.
 */
public class C_D_ConditionalEstimator implements ConditionalEstimator {
    private GaussianEstimator[] estim;

    @Override
    public void updateSufficientStatistics(DataInstance instance) {

    }

    @Override
    public void updateExpectedSufficientStatistics(DataInstance instance, Potential pot) {

    }

    @Override
    public Potential getRestrictedPotential(DataInstance instance) {
        return null;
    }
}
