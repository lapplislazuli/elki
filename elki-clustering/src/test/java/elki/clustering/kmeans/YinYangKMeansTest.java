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
 * Regression test for Yin-Yang k-means.
 *
 * @author Erich Schubert
 */
public class YinYangKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMeansYinYang() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<YinYangKMeans<DoubleVector>>(YinYangKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(YinYangKMeans.Par.T_ID, 2) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

  @Test
  public void testKMeansYinYangOne() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<YinYangKMeans<DoubleVector>>(YinYangKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(YinYangKMeans.Par.T_ID, 1) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.998005);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }

    // Tests Issue 87
    @Test
    public void testKMeansYinYangOne_SingleCluster_shouldPutAllInOneCluster() {
        Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
        Clustering<?> result = new ELKIBuilder<YinYangKMeans<DoubleVector>>(YinYangKMeans.class) //
                .with(KMeans.K_ID, 1) //
                .with(YinYangKMeans.Par.T_ID, 1) //
                .with(KMeans.SEED_ID, 7) //
                .build().autorun(db);
        assertClusterSizes(result, new int[] { 1000 });
    }
}
