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
 * Regression test for Annulus k-means.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AnnulusKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansAnnulus() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<AnnulusKMeans<DoubleVector>>(AnnulusKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

    // Covers Issue 87
    @Test
    public void testKMeansAnnulus_SingleCluster_shouldPutAllInOneCluster() {
        Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
        Clustering<?> result = new ELKIBuilder<AnnulusKMeans<DoubleVector>>(AnnulusKMeans.class) //
            .with(KMeans.K_ID, 1) //
            .with(KMeans.SEED_ID, 7) //
            .build().autorun(db);
        assertClusterSizes(result, new int[] { 1000 });
    }
}
