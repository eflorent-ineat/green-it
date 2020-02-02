package ineat.demo;

import java.io.IOException;
import java.util.Arrays;

public class AppMain {

    public static String server = null;

    public static void main(String[] args) {
        handleArgs(args);
        StorageClient storageClient = new StorageClient(server);
        try {
            storageClient.demo();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void handleArgs(String[] args) {
        if (args!=null && Arrays.asList(args).contains("--help")) {
            help(args);
        }
        if (args!=null && Arrays.asList(args).contains("--server")) {
            int index = Arrays.asList(args).indexOf("--server") + 1;
            server = args[index];
        } else {
            help(args);
        }

    }

    private static void help(String[] args) {
        String me = new java.io.File(AppMain.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
        System.out.println("usage: " + me +" --server url");
        System.exit(0);
    }

}

