package io;

import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.blockalloc.Cache;
import io.github.alexeygrishin.btree.BTree;
import io.github.alexeygrishin.bytestorage.*;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.hashfile.NamedStorage;
import io.github.alexeygrishin.blockalloc.serializers.Limited;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Test1 {

    public static class MetaInfo  {
        public int version;
        @Limited(size = 100)
        public String firstName;
        @Limited(size = 10)
        public String lastName;
    }

    public static void main(String args[]) throws IOException {

        String storageName = "storage1.stg";
        NamedStorage storage = new BTreeBasedFactory().create(storageName);
        System.out.println("List of entries");
        for (String name: storage) {
            System.out.println("-- " + name);
        }
        System.out.println("Save entries");
        for (File child: new File(".").listFiles()) {
            if (child.isFile() && !child.getName().equals(storageName)) {
                System.out.println(child.getName());
                storage.saveFrom(child.getAbsolutePath(), new FileInputStream(child));
            }
        }
        storage.close();


        if (true) return;
        new File("tree1.bin").delete();
        Counter counter = new Counter(new FileBytesContainer(new RandomAccessFile("tree1.bin", "rw").getChannel()));
        Cache storage2 = new Cache(new BlockAllocator(counter, 1024*1024), 1024);
        BTree tree = new BTree(storage2);
        /*tree.put("b", 4);
        tree.dump(System.out);
        //System.in.read();
        tree.put("a", 3);
       // System.in.read();
        tree.put("d", 5);
        tree.put("c", 77);
        tree.put("k", 77);
        //System.in.read();
        tree.put("l", 77);
        tree.dump(System.out);
        tree.put("e", 77);
        tree.dump(System.out);
       // System.in.read();
        tree.put("p", 77);
        tree.put("f", 77);
        tree.put("z", 77);
        tree.dump(System.out);*/
        //System.in.read();
        for (int i = 0; i < 1000000; i++) {
            tree.put(String.format("x%d",i), i);
        }
        counter.dump(System.out);
        counter.resetCounters();
        tree.put("x44556677z", 3);
        //storage.dump(System.out);
        tree.close();
        counter.dump(System.out);
        /*
        tree.dump(System.out);
        tree.remove("x7");
        tree.remove("x6");
        tree.dump(System.out);
        tree.remove("x5");
        tree.dump(System.out);
        tree.remove("x3");
        tree.dump(System.out);
        tree.remove("x0");
        tree.dump(System.out);
        for (String key: tree) {
            System.out.println(key);
        } */
        //tree.dump(System.out);
        //tree.dump(System.out);
       // System.in.read();



    }
}
