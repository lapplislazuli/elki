package de.lmu.ifi.dbs.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.logging.LogLevel;
import de.lmu.ifi.dbs.logging.ProgressLogRecord;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * DBSCAN provides the DBSCAN algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DBSCAN<O extends DatabaseObject, D extends Distance<D>> extends
		DistanceBasedAlgorithm<O, D> implements Clustering<O> {

	/**
	 * Parameter for epsilon.
	 */
	public static final String EPSILON_P = "epsilon";

	/**
	 * Description for parameter epsilon.
	 */
	public static final String EPSILON_D = "the maximum radius of the neighborhood to "
			+ "be considered, must be suitable to the "
			+ "distance function specified";

	/**
	 * Parameter minimum points.
	 */
	public static final String MINPTS_P = "minpts";

	/**
	 * Description for parameter minimum points.
	 */
	public static final String MINPTS_D = "threshold for minumum number of points in the epsilon-"
			+ "neighborhood of a point";

	/**
	 * Epsilon.
	 */
	protected String epsilon;

	/**
	 * Minimum points.
	 */
	protected int minpts;

	/**
	 * Holds a list of clusters found.
	 */
	protected List<List<Integer>> resultList;

	/**
	 * Provides the result of the algorithm.
	 */
	protected ClustersPlusNoise<O> result;

	/**
	 * Holds a set of noise.
	 */
	protected Set<Integer> noise;

	/**
	 * Holds a set of processed ids.
	 */
	protected Set<Integer> processedIDs;

	/**
	 * Sets epsilon and minimum points to the optionhandler additionally to the
	 * parameters provided by super-classes. 
	 */
	public DBSCAN() {
		super();
		PatternParameter eps = new PatternParameter(EPSILON_P,EPSILON_D);
		//TODO constraint mit distance function
		optionHandler.put(EPSILON_P, eps);
		// TODO default minpts??
		optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P,MINPTS_D,new GreaterConstraint(0)));
	}

	/**
	 * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
	 */
	protected void runInTime(Database<O> database) {
		if (isVerbose()) {
			verbose("");
		}
		Progress progress = new Progress("Clustering", database.size());
		resultList = new ArrayList<List<Integer>>();
		noise = new HashSet<Integer>();
		processedIDs = new HashSet<Integer>(database.size());
		getDistanceFunction().setDatabase(database, isVerbose(), isTime());
		if (isVerbose()) {
			verbose("\nClustering:");
		}
		if (database.size() >= minpts) {
			for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
				Integer id = iter.next();
				if (!processedIDs.contains(id)) {
					expandCluster(database, id, progress);
					if (processedIDs.size() == database.size()
							&& noise.size() == 0) {
						break;
					}
				}
				if (isVerbose()) {
					progress.setProcessed(processedIDs.size());
					progress(new ProgressLogRecord(LogLevel.PROGRESS, Util
							.status(progress, resultList.size()), progress
							.getTask(), progress.status()));
				}
			}
		} else {
			for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
				Integer id = iter.next();
				noise.add(id);
				if (isVerbose()) {
					progress.setProcessed(noise.size());
					progress(new ProgressLogRecord(LogLevel.PROGRESS, Util
							.status(progress, resultList.size()), progress
							.getTask(), progress.status()));
				}
			}
		}

		Integer[][] resultArray = new Integer[resultList.size() + 1][];
		int i = 0;
		for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter
				.hasNext(); i++) {
			resultArray[i] = resultListIter.next().toArray(new Integer[0]);
		}

		resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
		result = new ClustersPlusNoise<O>(resultArray, database);
		if (isVerbose()) {
			verbose("");
		}
	}

	/**
	 * DBSCAN-function expandCluster. <p/> Border-Objects become members of the
	 * first possible cluster.
	 * 
	 * @param database
	 *            the database on which the algorithm is run
	 * @param startObjectID
	 *            potential seed of a new potential cluster
	 */
	protected void expandCluster(Database<O> database, Integer startObjectID,
			Progress progress) {
		List<QueryResult<D>> seeds = database.rangeQuery(startObjectID,
				epsilon, getDistanceFunction());

		// startObject is no core-object
		if (seeds.size() < minpts) {
			noise.add(startObjectID);
			processedIDs.add(startObjectID);
			if (isVerbose()) {
				progress.setProcessed(processedIDs.size());
				progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(
						progress, resultList.size()), progress.getTask(),
						progress.status()));
			}
			return;
		}

		// try to expand the cluster
		List<Integer> currentCluster = new ArrayList<Integer>();
		for (QueryResult seed : seeds) {
			Integer nextID = seed.getID();
			if (!processedIDs.contains(nextID)) {
				currentCluster.add(nextID);
				processedIDs.add(nextID);
			} else if (noise.contains(nextID)) {
				currentCluster.add(nextID);
				noise.remove(nextID);
			}
		}
		seeds.remove(0);

		while (seeds.size() > 0) {
			Integer o = seeds.remove(0).getID();
			List<QueryResult<D>> neighborhood = database.rangeQuery(o, epsilon,
					getDistanceFunction());

			if (neighborhood.size() >= minpts) {
				for (QueryResult<D> neighbor : neighborhood) {
					Integer p = neighbor.getID();
					boolean inNoise = noise.contains(p);
					boolean unclassified = !processedIDs.contains(p);
					if (inNoise || unclassified) {
						if (unclassified) {
							seeds.add(neighbor);
						}
						currentCluster.add(p);
						processedIDs.add(p);
						if (inNoise) {
							noise.remove(p);
						}
					}
				}
			}

			if (isVerbose()) {
				progress.setProcessed(processedIDs.size());
				int numClusters = currentCluster.size() > minpts ? resultList
						.size() + 1 : resultList.size();
				progress(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(
						progress, numClusters), progress.getTask(), progress
						.status()));
			}

			if (processedIDs.size() == database.size() && noise.size() == 0) {
				break;
			}
		}
		if (currentCluster.size() >= minpts) {
			resultList.add(currentCluster);
		} else {
			for (Integer id : currentCluster) {
				noise.add(id);
			}
			noise.add(startObjectID);
			processedIDs.add(startObjectID);
		}
	}

	/**
	 * @see Algorithm#getDescription()
	 */
	public Description getDescription() {
		return new Description(
				"DBSCAN",
				"Density-Based Clustering of Applications with Noise",
				"Algorithm to find density-connected sets in a database based on the parameters "
						+ MINPTS_P
						+ " and "
						+ EPSILON_P
						+ " (specifying a volume). "
						+ "These two parameters determine a density threshold for clustering.",
				"M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: "
						+ "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. "
						+ "In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996.");
	}

	/**
	 * Sets the parameters epsilon and minpts additionally to the parameters set
	 * by the super-class' method. Both epsilon and minpts are required
	 * parameters.
	 * 
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		epsilon = optionHandler.getOptionValue(EPSILON_P);
		try {
			// test whether epsilon is compatible with distance function
			getDistanceFunction().valueOf(epsilon);
		} catch (IllegalArgumentException e) {
			throw new WrongParameterValueException(EPSILON_P, epsilon,
					EPSILON_D);
		}

		// minpts
		String minptsString = optionHandler.getOptionValue(MINPTS_P);
		try {
			minpts = Integer.parseInt(minptsString);
			if (minpts <= 0) {
				throw new WrongParameterValueException(MINPTS_P, minptsString,
						MINPTS_D);
			}
		} catch (NumberFormatException e) {
			throw new WrongParameterValueException(MINPTS_P, minptsString,
					MINPTS_D, e);
		}
		setParameters(args, remainingParameters);
		return remainingParameters;
	}

	/**
	 * Returns the parameter setting of this algorithm.
	 * 
	 * @return the parameter setting of this algorithm
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super.getAttributeSettings();

		AttributeSettings mySettings = attributeSettings.get(0);
		mySettings.addSetting(EPSILON_P, epsilon);
		mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

		return attributeSettings;
	}

	/**
	 * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
	 */
	public ClustersPlusNoise<O> getResult() {
		return result;
	}
	
	public Option[] getOptions(){
		return optionHandler.getOptions();
	}

}