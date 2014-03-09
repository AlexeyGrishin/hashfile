package io.github.alexeygrishin;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.common.Files;
import io.github.alexeygrishin.common.RealFiles;
import io.github.alexeygrishin.common.Source;
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
        Option newOpt = new Option("n", "new", true,"Creates new storage with provided options: [page=1024,][cache=64,][truncate=trailing|leading]");
        newOpt.setOptionalArg(true);
        options.addOption(newOpt);
        options.addOption("k", "key", true, "Provides a key to operate with. Without other options just prints corresponding data to STDOUT");
        options.addOption("e", "export-to", true, "Extracts data to the specified file (requires --key option)");
        options.addOption("i", "import-from", true, "Imports data from the specified file/folder. If --key option is not specified then file's name is used as key. \n" +
                "If folder specified then all files from the folder will be imported recursively using their relative paths as keys. --key option is ignored in this case.\n" +
                "If data for this key was already stored then it will be overwritten with new data.");
        options.addOption("d", "delete", false, "Deletes data for the specified key (requires --key option)");
        options.addOption("c", "check", false, "Checks is the data for this key exist or not (requires --key option)");
        options.addOption("o", "optimize", false, "Removes old data blocks, reorganizes file for less fragmentation");
        options.addOption("p", "copy-from", true, "Copies all items from specified storage");
        options.addOption("f", "info", false, "Shows storage info");
        readHelpHeaderFooter();
    }

    private void readHelpHeaderFooter() throws IOException {
        boolean isHeader = true;
        String CR = System.getProperty("line.separator");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("help.txt")))) {
            String str;
            while ((str = reader.readLine()) != null) {
                if (str.equals("<options>")) {
                    isHeader = false;
                }
                else if (isHeader) {
                    header += str + CR;
                }
                else {
                    footer += str + CR;
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
        else if (cmd.hasOption("info")) {
            factory.printInfo(storageName, out);
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
                    Source[] sources = files.getSources(filePath);
                    if (sources.length == 1) {
                        if (key == null) key = sources[0].toKey();
                        importFile(out, storage, key, sources[0]);
                    }
                    else {
                        if (key != null) {
                            throw new InvalidSyntax("When importing from folder the file paths are used as keys so --key value is ignored");
                        }
                        for (Source src: sources) {
                            importFile(out, storage, src.toKey(), src);
                        }
                    }
                }
                else if (cmd.hasOption("export-to")) {
                    checkKey(key);
                    String filePath = cmd.getOptionValue("export-to");
                    out.print("Exporting `" + key + "`...");
                    if (storage.getInto(key, files.getSources(filePath)[0].openOutputStream())) {
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
                else if (cmd.hasOption("copy-from")) {
                    NamedStorage anotherOne = factory.load(cmd.getOptionValue("copy-from"));
                    out.print("Copying...");
                    anotherOne.cloneTo(storage);
                    out.println("Ok!");
                }
                else if (cmd.hasOption("key")) {
                    if (!storage.getInto(key, out)) {
                        out.println("<Not found>");
                    }
                }
                else if (!cmd.hasOption("optimize")) {
                    showHelp();
                }
            }
            if (cmd.hasOption("optimize")) {
                out.print("Data optimization...");
                factory.truncate(storageName);
                out.println("Ok!");
            }
        }

    }

    private void importFile(PrintStream out, NamedStorage storage, String key, Source src) {
        out.print("Importing `" + key + "`...");
        storage.saveFrom(key, src.openInputStream());
        out.println("Ok!");
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
                        try {
                            method = KeyTruncateMethod.valueOf(keyValue[1].toUpperCase());
                        }
                        catch (IllegalArgumentException e) {
                            throw new InvalidSyntax("Unknown truncate method `" + keyValue[1] + "`. Supported are `leading` and `trailing`");
                        }
                        break;
                    default:
                        throw new InvalidSyntax("Unknown option `" + keyValue[0] + "` - the supported ones are `block`, `cache` and `truncate`");
                }
            }
        }
        factory.create(storageName, blockSize, cacheSize, method).close();
    }

    private void checkKey(String key) throws ParseException {
        if (key == null) {
            throw new InvalidSyntax("Options `--export-to`, `--check`, `--delete` require `--key` option as well");
        }
    }


    private void showHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar hashfile.jar STORAGE [options]", header, options, "");
        System.out.println(footer);
    }

    public static void main(String args[]) throws ParseException {
        try {
            CommandLineAPI api = new CommandLineAPI(new BTreeBasedFactory(), new RealFiles());
            api.process(args, System.out);
        }
        catch (InvalidSyntax e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

}
