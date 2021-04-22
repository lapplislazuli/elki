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
package elki.index.tree.metrical.vptree;

import org.junit.Test;

import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the {@link VPTree}.
 *
 * @author Robert Gehde
 */
public class VPTreeTest extends AbstractIndexStructureTest {
  @Test
  public void testVPTree() {
    VPTree.Factory<?> factory = new ELKIBuilder<>(VPTree.Factory.class) //
        .with(VPTree.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class).build();
    assertExactEuclidean(factory, VPTree.VPTreeKNNSearcher.class, VPTree.VPTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, VPTree.VPTreePrioritySearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
