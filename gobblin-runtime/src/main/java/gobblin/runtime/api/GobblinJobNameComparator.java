package gobblin.runtime.api;

import java.util.Comparator;

public class GobblinJobNameComparator implements Comparator<GobblinJob> {

  @Override
  public int compare(GobblinJob o1, GobblinJob o2) {
    return o1.getJobName().compareTo(o2.getJobName());
  }

}
