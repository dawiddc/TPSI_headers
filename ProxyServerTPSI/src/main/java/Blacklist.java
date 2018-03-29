import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class Blacklist {

    ArrayList<String> blacklist = null;

    public ArrayList<String> getBlacklist() { return blacklist; }

    public void prepareBlacklist() {
        try (FileInputStream inputStream = new FileInputStream("foo.txt")) {
            String everything = IOUtils.toString(inputStream);
            String[] hosts = everything.split("\\r?\\n");
            blacklist = new ArrayList<String>(Arrays.asList(hosts));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
