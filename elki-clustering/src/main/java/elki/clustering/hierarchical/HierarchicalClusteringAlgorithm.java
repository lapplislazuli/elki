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
package elki.clustering.hierarchical;

import elki.Algorithm;
import elki.database.Database;

/**
 * Interface for hierarchical clustering algorithms.
 * <p>
 * This interface allows the algorithms to be used by, e.g.,
 * {@link elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters}
 * and {@link elki.clustering.hierarchical.extraction.CutDendrogramByHeight}.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - PointerHierarchyResult
 */
public interface HierarchicalClusteringAlgorithm extends Algorithm {
  @Override
  default PointerHierarchyResult autorun(Database database) {
    return (PointerHierarchyResult) Algorithm.Utils.autorun(this, database);
  }
}
