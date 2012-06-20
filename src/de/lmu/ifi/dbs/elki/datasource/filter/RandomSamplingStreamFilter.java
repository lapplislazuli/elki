package de.lmu.ifi.dbs.elki.datasource.filter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Random;

import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Subsampling stream filter.
 * 
 * @author Erich Schubert
 */
public class RandomSamplingStreamFilter extends AbstractStreamFilter {
  /**
   * Probability
   */
  protected double prob;

  /**
   * Random generator
   */
  protected Random random;

  /**
   * Constructor.
   * 
   * @param prob Probability
   * @param seed Random seed
   */
  public RandomSamplingStreamFilter(double prob, Long seed) {
    super();
    this.prob = prob;
    this.random = (seed != null) ? new Random(seed) : new Random();
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        return ev;
      case NEXT_OBJECT:
        if(random.nextDouble() < prob) {
          return ev;
        }
        continue;
      }
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for sampling probability
     */
    private static final OptionID PROB_ID = OptionID.getOrCreateOptionID("sampling.p", "Sampling probability. Each object has a chance of being samples with this probability.");

    /**
     * Option ID for random seed
     */
    private static final OptionID SEED_ID = OptionID.getOrCreateOptionID("sampling.seed", "Random generator seed for sampling.");

    /**
     * Probability
     */
    protected double prob;

    /**
     * Random seed
     */
    protected Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter probP = new DoubleParameter(PROB_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1.0, IntervalBoundary.CLOSE));
      if(config.grab(probP)) {
        prob = probP.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected RandomSamplingStreamFilter makeInstance() {
      return new RandomSamplingStreamFilter(prob, seed);
    }
  }
}
