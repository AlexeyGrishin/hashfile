package io.github.alexeygrishin;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.common.Files;
import io.github.alexeygrishin.common.RealFiles;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.hashfile.NameBasedStorageFactory;
import io.github.alexeygrishin.hashfile.NamedStorage;
import org.apache.commons.cli.*;

import java.io.*;


public class CommandLineAPI {

    private NameBasedStorageFactory factory;
    private Files files;
    private CommandLineParser parser = new BasicParser();
    private Options options = new Options();
    private String header = "", footer = "";

    public CommandLineAPI(NameBasedStorageFactory factory, Files files) throws IOException {
        this.factory = factory;
        this.files = files;
        options.addOption("l", "list", false, "Shows all keys");
        options.addOption(OptionBuilder
                .withLongOpt("new")
                .hasOptionalArg()
                .withDescription("Creates new storage with provided options: [page=1024,][cache=1000,][truncate=trailing|leading]")
                .create("n")
        );
        options.addOption("k", "key", true, "Provides a key to operate with");
        options.addOption("e", "export-to", true, "Extracts data to the specified file.");
        options.addOption("i", "import-from", true, "Imports data from the specified file.");
        options.addOption("d", "delete", false, "Deletes data for the specified key");
        options.addOption("c", "check", false, "Checks is the data for this key exist or not");
        boolean isHeader = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("help.txt")))) {
            String str;
            while ((str = reader.readLine()) != null) {
                if (str.equals("<options>")) {
                    isHeader = false;
                }
                else if (isHeader) {
                    header += str;
                }
                else {
                    footer += str;
                }
            }
        }
    }

    public void process(String args[], PrintStream out) throws ParseException, FileNotFoundException {
        if (args.length == 0) {
            showHelp();
            return;
        }
        String storageName = args[0];
        String[] withoutStorage = new String[args.length - 1];
        System.arraycopy(args, 1, withoutStorage, 0, withoutStorage.length);
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("new")) {
            doCreate(storageName, cmd.getOptionValue("new"));
            out.println("Done");
        }
        else {
            try (NamedStorage storage = factory.load(storageName)) {
                String key = cmd.getOptionValue("key");
                if (cmd.hasOption("list") || cmd.getOptions().length == 0) {
                    boolean empty = true;
                    for (String ekey: storage) {
                        out.println(ekey);
                        empty = false;
                    }
                    if (empty) {
                        out.println("<Empty>");
                    }

                }
                else if (cmd.hasOption("import-from")) {
                    String filePath = cmd.getOptionValue("import-from");
                    if (key == null) key = files.toKey(filePath);
                    out.print("Importing `" + key + "`...");
                    storage.saveFrom(key, files.getInputStream(filePath));
                    out.println("Ok!");
                }
                else if (cmd.hasOption("export-to")) {
                    checkKey(key);
                    String filePath = cmd.getOptionValue("export-to");
                    out.print("Exporting `" + key + "`...");
                    if (storage.getInto(key, files.getOutputStream(filePath))) {
                        out.println("Ok!");
                    }
                    else {
                        out.println("<Not found>");
                    }
                }
                else if (cmd.hasOption("delete")) {
                    checkKey(key);
                    out.print("Deleting `" + key + "`...");
                    storage.delete(key);
                    out.println("Ok!");
                }
                else if (cmd.hasOption("check")) {
                    checkKey(key);
                    out.println(storage.contains(key) ? "Yes" : "No");
                }
                else if (cmd.hasOption("key")) {
                    if (!storage.getInto(key, out)) {
                        out.println("<Not found>");
                    }
                }
                else {
                    showHelp();
                }
            }

        }

    }

    private void doCreate(String storageName, String options) throws ParseException {
        Integer blockSize = null, cacheSize = null;
        KeyTruncateMethod method = null;
        if (options != null && options.length() != 0) {
            for (String pair: options.split(",")) {
                String[] keyValue = pair.split("=");
                switch (keyValue[0]) {
                    case "block":
                        blockSize = Integer.parseInt(keyValue[1]);
                        break;
                    case "cache":
                        cacheSize = Integer.parseInt(keyValue[1]);
                        break;
                    case "truncate":
                        method = KeyTruncateMethod.valueOf(keyValue[1].toUpperCase());
                        break;
                    default:
                        throw new ParseException("Unknown option `" + keyValue[0] + "` - the supported ones are `block`, `cache` and `truncate`");
                }
            }
        }
        factory.create(storageName, blockSize, cacheSize, method).close();
    }

    private void checkKey(String key) throws ParseException {
        if (key == null) {
            throw new ParseException("Options `--export-to`, `--check`, `--delete` require `--key` option as well");
        }
    }


    private void showHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("hashfile.jar", header, options, footer);
    }

    public static void main(String args[]) throws ParseException {
        try {
            CommandLineAPI api = new CommandLineAPI(new BTreeBasedFactory(), new RealFiles());
            api.process(args, System.out);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

}
