package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.EMClusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author Arthur Zimek
 */
public class SubspaceEM<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V>
{
    /**
     * Small value to increment diagonally of a matrix
     * in order to avoid singularity befor building the inverse.
     */
    private static final double SINGULARITY_CHEAT = 1E-9;
    
    /**
     * Parameter k.
     */
    public static final String K_P = "k";

    /**
     * Description for parameter k.
     */
    public static final String K_D = "k - the number of clusters to find (positive integer)";

    /**
     * Parameter for k.
     * Constraint greater 0.
     */
    private static final IntParameter K_PARAM = new IntParameter(K_P, K_D, new GreaterConstraint(0));
    
    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;
    
    /**
     * Parameter delta.
     */
    public static final String DELTA_P = "delta";
    
    /**
     * Description for parameter delta.
     */
    public static final String DELTA_D = "delta - the termination criterion for maximization of E(M): E(M) - E(M') < delta";
    
    /**
     * Parameter for delta.
     * GreaterEqual 0.0.
     * (Default: 0.0).
     */
    private static final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_P, DELTA_D, new GreaterEqualConstraint(0.0));
    static{
        DELTA_PARAM.setDefaultValue(0.0);
    }
    
    /**
     * Keeps delta - a small value as termination criterion in expectation maximization
     */
    private double delta;

    /**
     * Stores the result.
     */
    private Clusters<V> result;
    
    /**
     * 
     */
    public SubspaceEM()
    {
        super();
        debug = true;
        optionHandler.put(K_P, K_PARAM);
        optionHandler.put(DELTA_P, DELTA_PARAM);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.clustering.Clustering#getResult()
     */
    public ClusteringResult<V> getResult()
    {
        return result;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("SubspaceEM","SubspaceEM","","");
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    public void runInTime(Database<V> database) throws IllegalStateException
    {
        if(database.size() == 0)
        {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
        // initial models
        if(isVerbose())
        {
            verbose("initializing "+k+" models");
        }
        
        List<V> means = initialMeans(database);
        int dimensionality = means.get(0).getDimensionality();
        Matrix[] eigensystems = initialEigensystems(dimensionality);
        Matrix selectionWeak = Matrix.zeroMatrix(dimensionality);
        selectionWeak.set(dimensionality-1, dimensionality-1, 1);
        Matrix selectionStrong = Matrix.unitMatrix(dimensionality);
        selectionStrong.set(dimensionality-1, dimensionality-1, 0);
        
        double[] standardDeviation = new double[k];
        double[] normDistributionFactor = new double[k];
        double[] clusterWeight = new double[k];
        
        for(int i = 0; i < k; i++)
        {
            standardDeviation[i] = 1;
            clusterWeight[i] = 1.0 / k;
            normDistributionFactor[i] = 1.0 / (standardDeviation[i] * Math.sqrt(2*Math.PI));

        }
        // assign probabilities to database objects
        assignProbabilities(database, normDistributionFactor, standardDeviation, clusterWeight, means, eigensystems, selectionStrong);
        double emNew = expectationOfMixture(database);
        // iteration unless no change
        if(isVerbose())
        {
            verbose("iterating subspace EM");
        }
        double em;
        int it = 0;
        do{
            it++;
            if(isVerbose())
            {
                verbose("iteration "+it+" - expectation value: "+emNew);
            }
            em = emNew;
            
            // recompute models
            List<V> meanSums = new ArrayList<V>(k);
            double[] sumOfClusterProbabilities = new double[k];
            Matrix[] covarianceMatrix = new Matrix[k];
            for(int i = 0; i < k; i++)
            {
                clusterWeight[i] = 0.0;
                meanSums.add(means.get(i).nullVector());
                covarianceMatrix[i] = Matrix.zeroMatrix(dimensionality);
            }
            // weights and means
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                
                for(int i = 0; i < k; i++)
                {
                    sumOfClusterProbabilities[i] += clusterProbabilities.get(i);
                    V summand = database.get(id).multiplicate(clusterProbabilities.get(i));
                    V currentMeanSum = meanSums.get(i).plus(summand);
                    meanSums.set(i, currentMeanSum);
                }
            }
            int n = database.size();
            for(int i = 0; i < k; i++)
            {
                clusterWeight[i] = sumOfClusterProbabilities[i] / n;
                V newMean = meanSums.get(i).multiplicate(1 / sumOfClusterProbabilities[i]);
                means.set(i, newMean);
            }
            // covariance matrices
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                V instance = database.get(id);
                for(int i = 0; i < k; i++)
                {
                    V difference = instance.plus(means.get(i).negativeVector());
                    Matrix newCovMatr = covarianceMatrix[i].plus(difference.getColumnVector().times(difference.getRowVector()).times(clusterProbabilities.get(i)));
                    covarianceMatrix[i] = newCovMatr;
                }
            }
            // eigensystems and standard deviations
            for(int i = 0; i < k; i++)
            {
                covarianceMatrix[i] = covarianceMatrix[i].times(1 / sumOfClusterProbabilities[i]).cheatToAvoidSingularity(SINGULARITY_CHEAT);
                eigensystems[i] = covarianceMatrix[i].eig().getV();
                // standard deviation for the points from database again weighted accordingly to their cluster probability?
                double variance = 0;
                for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
                {
                    Integer id = iter.next();
                    List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                    double distance = SubspaceEM.distance(database.get(id),means.get(i),eigensystems[i].times(selectionStrong));
                    variance += distance * distance;// * clusterProbabilities.get(i);
                }
                standardDeviation[i] = Math.sqrt(variance / n);
                if(debug)
                {
                    if(standardDeviation[i] == 0)
                    {
                        debugFine(i+": "+standardDeviation[i]);
                    }
                }
                normDistributionFactor[i] = 1.0 / (standardDeviation[i] * Math.sqrt(2*Math.PI));
            }
            // reassign probabilities
            assignProbabilities(database, normDistributionFactor, standardDeviation, clusterWeight, means, eigensystems, selectionStrong);
            
            // new expectation
            emNew = expectationOfMixture(database);
            
        }while(Math.abs(em - emNew) > delta);
        
        if(isVerbose())
        {
            verbose("\nassigning clusters");
        }
        
        // fill result with clusters and models
        List<List<Integer>> hardClusters = new ArrayList<List<Integer>>(k);
        for(int i = 0; i < k; i++)
        {
            hardClusters.add(new LinkedList<Integer>());
        }
        
        // provide a hard clustering
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
            int maxIndex = 0;
            double currentMax = 0.0;
            for(int i = 0; i < k; i++)
            {
                if(clusterProbabilities.get(i) > currentMax)
                {
                    maxIndex = i;
                    currentMax = clusterProbabilities.get(i);
                }
            }
            hardClusters.get(maxIndex).add(id);
        }
        Integer[][] resultClusters = new Integer[k][];
        for(int i = 0; i < k; i++)
        {
            resultClusters[i] = hardClusters.get(i).toArray(new Integer[hardClusters.get(i).size()]);
        }
        result = new EMClusters<V>(resultClusters, database);
        result.associate(SimpleClassLabel.class);
        // provide models within the result
        for(int i = 0; i < k; i++)
        {
            SimpleClassLabel label = new SimpleClassLabel();
            label.init(result.canonicalClusterLabel(i));
            Matrix transposedWeakEigenvectors = eigensystems[i].times(selectionWeak).transpose();
            Matrix vTimesMean = transposedWeakEigenvectors.times(means.get(i).getColumnVector());
            double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
            double[][] we = transposedWeakEigenvectors.getArray();
            double[] b = vTimesMean.getColumn(0).getRowPackedCopy();
            System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());
            LinearEquationSystem lq = new LinearEquationSystem(a, b);
            lq.solveByTotalPivotSearch();
            CorrelationAnalysisSolution<V> solution = new CorrelationAnalysisSolution<V>(lq, database, eigensystems[i].times(selectionStrong),eigensystems[i].times(selectionWeak),eigensystems[i].times(selectionWeak).times(eigensystems[i].transpose()),means.get(i).getColumnVector());
            result.appendModel(label, solution);
        }
        // TODO: instead of hard clustering: overlapping subspace clusters assigned with dist < 3*sigma

    }
    
    /**
     * The expectation value of the current mixture of distributions.
     * 
     * Computed as the sum of the logarithms of the prior probability of each instance.
     * 
     * @param database the database where the prior probability of each instance is associated
     * @return the expectation value of the current mixture of distributions
     */
    protected double expectationOfMixture(Database<V> database)
    {
        double sum = 0.0;
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            double priorProbX = (Double) database.getAssociation(AssociationID.PROBABILITY_X, id);
            double logP = Math.log(priorProbX);
            sum += logP;
            if(debug && false)
            {
                debugFine("\nid="+id+"\nP(x)="+priorProbX+"\nlogP="+logP+"\nsum="+sum);
            }             
        }
        return sum;
    }
    
    protected void assignProbabilities(Database<V> database, double[] normDistributionFactor, double[] standardDeviation, double[] clusterWeight,  List<V> means, Matrix[] eigensystems, Matrix selectionStrong)
    {
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            V x = database.get(id);
            List<Double> probabilities = new ArrayList<Double>(k);
            for(int i = 0; i < k; i++)
            {
                double distance = SubspaceEM.distance(x, means.get(i), eigensystems[i].times(selectionStrong));
                probabilities.add(normDistributionFactor[i] * Math.exp(-0.5 * distance * distance / (standardDeviation[i] * standardDeviation[i])));
            }
            database.associate(AssociationID.PROBABILITY_X_GIVEN_CLUSTER_I, id, probabilities);
            double priorProbability = 0.0;
            for(int i = 0; i < k; i++)
            {
                priorProbability += probabilities.get(i) * clusterWeight[i];
            }
            database.associate(AssociationID.PROBABILITY_X, id, priorProbability);
            List<Double> clusterProbabilities = new ArrayList<Double>(k);
            for(int i = 0; i < k; i++)
            {
                clusterProbabilities.add(probabilities.get(i) / priorProbability * clusterWeight[i]);
            }
            database.associate(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id, clusterProbabilities);
        }
    }
    
    protected static <V extends RealVector<V,?>> double distance(V p, V mean, Matrix strongEigenvectors)
    {
        Matrix p_minus_a = p.getColumnVector().minus(mean.getColumnVector());
        Matrix proj = p_minus_a.projection(strongEigenvectors);
        return p_minus_a.minus(proj).euclideanNorm(0);
    }

    /**
     * Creates {@link #k k} random points distributed uniformly within the
     * attribute ranges of the given database.
     * 
     * @param database the database must contain enough points in order to
     *        ascertain the range of attribute values. Less than two points
     *        would make no sense. The content of the database is not touched
     *        otherwise.
     * @return a list of {@link #k k} random points distributed uniformly within
     *         the attribute ranges of the given database
     */
    protected List<V> initialMeans(Database<V> database)
    {
        Random random = new Random();
        if(database.size() > 0)
        {
            // needs normalization to ensure the randomly generated means
            // are in the same range as the vectors in the database
            // XXX perhaps this can be done more conveniently?
            V randomBase = database.get(database.iterator().next());
            AttributeWiseRealVectorNormalization<V> normalization = new AttributeWiseRealVectorNormalization<V>();
            List<V> list = new ArrayList<V>(database.size());
            for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
            {
                list.add(database.get(dbIter.next()));
            }
            try
            {
                normalization.normalize(list);
            }
            catch(NonNumericFeaturesException e)
            {
                warning(e.getMessage());
            }
            List<V> means = new ArrayList<V>(k);
            if(isVerbose())
            {
                verbose("initializing random vectors");
            }
            for(int i = 0; i < k; i++)
            {
                V randomVector = randomBase.randomInstance(random);
                try
                {
                    means.add(normalization.restore(randomVector));
                }
                catch(NonNumericFeaturesException e)
                {
                    warning(e.getMessage());
                    means.add(randomVector);
                }
            }
            return means;
        }
        else
        {
            return new ArrayList<V>(0);
        }
    }
    
    protected Matrix[] initialEigensystems(int dimensionality)
    {
        Random random = new Random();
        Matrix[] eigensystems = new Matrix[k];
        for(int i = 0; i < k; i++)
        {
            double[][] vec = new double[dimensionality][1];
            for(int d = 0; d < dimensionality; d++)
            {
                vec[d][0] = random.nextDouble() * 2 - 1;
            }
            Matrix eig = new Matrix(vec);
            eig = eig.appendColumns(eig.completeToOrthonormalBasis());
            eigensystems[i] = eig;
            
        }
        return eigensystems;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        k = optionHandler.getParameterValue(K_PARAM);

        delta = optionHandler.getParameterValue(DELTA_PARAM);
        
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

}
