package Server;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class IdGenerator {
    private static final Set ids = new HashSet();
    private static final Random randomGenerator = new Random();

    public static int newId() {
        int id;
        do {
            id = randomGenerator.nextInt();
        } while (ids.contains(id));
        ids.add(id);
        return id;
    }
}
