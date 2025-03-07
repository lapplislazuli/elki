/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.kmeans;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a single-assignment kMeans run, and compares the result with a
 * clustering derived from the data set labels. This test ensures that KMeans's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SingleAssignmentKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSingleAssignmentKMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<SingleAssignmentKMeans<DoubleVector>>(SingleAssignmentKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 4) //
        .build().autorun(db);
    // Unsurprisingly, these results are much worse than normal k-means
    assertFMeasure(db, result, 0.622655);
    assertClusterSizes(result, new int[] { 27, 56, 117, 376, 424 });
  }
}
