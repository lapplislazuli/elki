package de.lmu.ifi.dbs.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.data.ExternalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides a parser for parsing one distance value per line. <p/> A line must
 * have the follwing format: id1 id2 distanceValue, where id1 and is2 are
 * integers representing the two ids belonging to the distance value. Lines
 * starting with &quot;#&quot; will be ignored.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NumberDistanceParser extends AbstractParser<ExternalObject>
implements DistanceParser<ExternalObject, NumberDistance> {

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function " +
                                                   Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) +
                                                   ".";

  /**
   * The distance function.
   */
  private DistanceFunction<ExternalObject, NumberDistance> distanceFunction;

  /**
   * Provides a parser for parsing one double distance per line. A line must
   * have the follwing format: id1 id2 distanceValue, where id1 and is2 are
   * integers representing the two ids belonging to the distance value, the
   * distance value is a double value. Lines starting with &quot;#&quot; will
   * be ignored.
   */
  public NumberDistanceParser() {
    super();
    optionHandler.put(DISTANCE_FUNCTION_P, new ClassParameter(DISTANCE_FUNCTION_P,DISTANCE_FUNCTION_D,DistanceFunction.class));
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  public ParsingResult<ExternalObject> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<ObjectAndLabels<ExternalObject>> objectAndLabelsList = new ArrayList<ObjectAndLabels<ExternalObject>>();

    Set<Integer> ids = new HashSet<Integer>();
    Map<Integer, Map<Integer, NumberDistance>> distanceCache = new HashMap<Integer, Map<Integer, NumberDistance>>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (this.debug && lineNumber % 10000 == 0) {
        	debugFine("parse " + lineNumber / 10000);
//          logger.fine("parse " + lineNumber / 10000);
        }
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          if (entries.length != 3)
            throw new IllegalArgumentException(
            "Line "
            + lineNumber
            + " does not have the "
            + "required input format: id1 id2 distanceValue! "
            + line);

          Integer id1, id2;
          try {
            id1 = Integer.parseInt(entries[0]);
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in line "
                                               + lineNumber + ": id1 is no integer!");
          }

          try {
            id2 = Integer.parseInt(entries[1]);
          }
          catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in line "
                                               + lineNumber + ": id2 is no integer!");
          }

          try {
            NumberDistance distance = distanceFunction.valueOf(entries[2]);
            put(id1, id2, distance, distanceCache);
            ids.add(id1);
            ids.add(id2);
          }
          catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error in line "
                                               + lineNumber + ":" + e.getMessage());
          }
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line "
                                         + lineNumber + ".");
    }

    if (this.debug) {
    	debugFine("check");
    }

    // check if all distance values are specified
    for (Integer id1 : ids) {
      for (Integer id2 : ids) {
        if (id2 < id1)
          continue;
        if (! containsKey(id1, id2, distanceCache))
          throw new IllegalArgumentException("Distance value for "
                                             + id1 + " - " + id2 + " is missing!");
      }
    }

    if (this.debug) {
    	debugFine("add to objectAndLabelsList");
    }
    for (Integer id : ids) {
      objectAndLabelsList.add(new ObjectAndLabels<ExternalObject>(new ExternalObject(id), new ArrayList<String>()));
    }

    return new DistanceParsingResult<ExternalObject, NumberDistance>(objectAndLabelsList, distanceCache);
  }

  /**
   * Returns the distance function of this parser.
   *
   * @return the distance function of this parser
   */
  public DistanceFunction<ExternalObject, NumberDistance> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(NumberDistanceParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description
    .append("id1 id2 distanceValue, where id1 and is2 are integers representing "
            + "the two ids belonging to the distance value.\n"
            + " The ids and the distance value are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored.\n");

    return usage(description.toString());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
      distanceFunction = Util.instantiate(DistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
    }

    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(DISTANCE_FUNCTION_P, distanceFunction.getClass().getSimpleName());

    attributeSettings.addAll(distanceFunction.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Puts the specified distance value for the given ids to the distance cache.
   *
   * @param id1      the first id
   * @param id2      the second id
   * @param distance the distance value
   * @param cache    the distance cache
   */
  private void put(Integer id1, Integer id2, NumberDistance distance,
                   Map<Integer, Map<Integer, NumberDistance>> cache) {
    // the smaller id is the first key
    if (id1 > id2) {
      put(id2, id1, distance, cache);
    }

    Map<Integer, NumberDistance> distances = cache.get(id1);
    if (distances == null) {
      distances = new HashMap<Integer, NumberDistance>();
      cache.put(id1, distances);
    }

    NumberDistance oldDistance = distances.put(id2, distance);

    if (oldDistance != null) {
      throw new IllegalArgumentException("Distance value for specified ids is already assigned!");
    }
  }

  /**
   * Returns <tt>true</tt> if the specified distance cache contains a distance
   * value for the specified ids.
   *
   * @param id1   the first id
   * @param id2   the second id
   * @param cache the distance cache
   * @return <tt>true</tt> if this cache contains a distance value for the specified
   *         ids, false otherwise
   */
  public boolean containsKey(Integer id1, Integer id2, Map<Integer, Map<Integer, NumberDistance>> cache) {
    if (id1 > id2) {
      return containsKey(id2, id1, cache);
    }

    Map<Integer, NumberDistance> distances = cache.get(id1);
    if (distances == null) {
      return false;
    }
    return distances.containsKey(id2);
  }

}
