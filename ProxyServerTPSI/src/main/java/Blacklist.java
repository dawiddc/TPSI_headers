import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class Blacklist {

    private static List<String> blacklist = Collections.emptyList();

    static List<String> getBlacklist() { prepareBlacklist();
    return blacklist; }

    private static void prepareBlacklist() {
        try (FileInputStream inputStream = new FileInputStream("src/main/resources/blacklist.txt")) {
            String everything = IOUtils.toString(inputStream, "UTF-8");
            String[] hosts = everything.split("\\r?\\n");
            blacklist = new ArrayList<>(Arrays.asList(hosts));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
