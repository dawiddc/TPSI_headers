import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;

class Logger {

    static void logValues(int requestCount, int bytesSent, int bytesReceived) {
        File f = new File("src/main/resources/log.txt");
        if (f.exists() && !f.isDirectory()) {
            try (FileInputStream inputStream = new FileInputStream("src/main/resources/log.txt")) {
                String everything = IOUtils.toString(inputStream, "UTF-8");
                String[] stringValues = everything.split(",");
                int[] intValues = Arrays.stream(stringValues).mapToInt(Integer::parseInt).toArray();
                intValues[0] += requestCount;
                intValues[1] += bytesSent;
                intValues[2] += bytesReceived;
                FileOutputStream output = new FileOutputStream("src/main/resources/log.txt", false);
                String log = String.valueOf(intValues[0]) + ',' + String.valueOf(intValues[1]) + ',' + String.valueOf(intValues[2]);
                IOUtils.write(log, output, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            int[] intValues = new int[3];
            intValues[0] = requestCount;
            intValues[1] = bytesSent;
            intValues[2] = bytesReceived;
            String log = String.valueOf(intValues[0]) + ',' + String.valueOf(intValues[1]) + ',' + String.valueOf(intValues[2]);
            File logFile = new File("src/main/resources/log.txt");
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(logFile, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                IOUtils.write(log, output, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
