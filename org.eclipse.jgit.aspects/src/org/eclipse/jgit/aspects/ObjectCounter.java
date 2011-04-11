package org.eclipse.jgit.aspects;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectCounter<KeyType> {
  Map<KeyType, int[]> key2Count = new HashMap<KeyType, int[]>();

  public void inc(KeyType k) {
    inc(k, 1);
  }

  public void inc(KeyType k, int increment) {
    int[] cnt = key2Count.get(k);
    if (cnt == null) {
      cnt = new int[] {0};
      key2Count.put(k, cnt);
    }
    cnt[0] += increment;
  }

  public LinkedHashMap<KeyType, Integer> sortByCount() {
    List<Map.Entry<KeyType, int[]>> sortedEntries =
        new LinkedList<Map.Entry<KeyType, int[]>>(key2Count.entrySet());
    Collections.sort(sortedEntries,
        new Comparator<Map.Entry<KeyType, int[]>>() {
          public int compare(Entry<KeyType, int[]> o1, Entry<KeyType, int[]> o2) {
            if (o1.getValue()[0] < o2.getValue()[0])
              return -1;
            else
              return (o1.getValue()[0] == o2.getValue()[0]) ? 0 : 1;
          }
        });
    LinkedHashMap<KeyType, Integer> ret = new LinkedHashMap<KeyType, Integer>();
    for (Map.Entry<KeyType, int[]> s : sortedEntries)
      ret.put(s.getKey(), Integer.valueOf(s.getValue()[0]));
    return ret;
  }
}
