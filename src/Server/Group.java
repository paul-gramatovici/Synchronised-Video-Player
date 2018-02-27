package Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group {
  private static final Map<Integer, Group> groups = new HashMap<>();
  private int groupId;
  private List<ServerInstance> sessions;

  private Group(int groupId) {
    this.groupId = groupId;
    sessions = new ArrayList<>();
  }

  public static Group addToGroup(int groupId, ServerInstance session) {
    if(groups.containsKey(groupId)) {
      Group group = groups.get(groupId);
      group.sessions.add(session);
      return group;
    } else {
      Group group = new Group(groupId);
      group.sessions.add(session);
      groups.put(groupId, group);
      return group;
    }
  }
  public void signalPlay(int time) {
    for (ServerInstance session: sessions) {
      session.signalPlay(time);
    }
  }

  public void signalPause(int time) {
    for (ServerInstance session: sessions) {
      session.signalPause(time);
    }
  }
}
