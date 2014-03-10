package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.blockalloc.BlockToModify;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;
import io.github.alexeygrishin.btree.blocks.Page;
import io.github.alexeygrishin.btree.blocks.PageInfo;
import io.github.alexeygrishin.btree.blocks.TreeEntry;
import io.github.alexeygrishin.btree.blocks.TreeInfo;
import io.github.alexeygrishin.common.Check;
import io.github.alexeygrishin.common.Locker;
import io.github.alexeygrishin.common.Pointer;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements B-Tree on byte blocks of identical size. Operates with 256-bytes entries so block size shall be big enough
 * to include tens-hundreds of records for faster operations.
 *
 * Each tree entry contains the following:
 * - key's hash
 * - key's part (110 characters)
 * - data pointer
 *
 * Comparison is performed this way: first hashes are compared, then key parts and only then (if all identical) the whole key
 * is obtained from external storage.
 *
 * Search/Deletion/Insertion use binary search so these operations take O(ln(block_size)*ln(n))
 * Iteration takes O(n) and order is not defined.
 *
 * Thread-safe, no concurrent access.
 */
public class BTree implements Iterable<String>, AutoCloseable {

    public static final int ENTRY_SIZE = 256;
    public static final int KEY_PART_SIZE = 220;
    public static final int KEY_PART_LENGTH = KEY_PART_SIZE / 2;
    private final Allocator allocator;
    private final TreeNameHelper helper;
    private int t, minAmount, maxAmount;
    private long totalCount = 0;
    private int firstPageBlockIdx, metaBlockIdx;
    private final ReadWriteLock treeLock = new ReentrantReadWriteLock();

    private volatile int modCount = 0;

    private final Comparator TREE_ENTRY_COMPARATOR = new Comparator() {

        @Override
        public int compare(Object treeEntry, Object treeEntryKey) {
            TreeEntry entry = (TreeEntry) treeEntry;
            TreeEntryKey key = (TreeEntryKey) treeEntryKey;
            return -BTree.this.compare(key.hash, key.truncatedKey, key.fullKey, entry);
        }
    } ;

    public BTree(Allocator allocator) {
        this(allocator, new DefaultNameHelper());
    }

    public BTree(Allocator allocator, TreeNameHelper helper) {
        this.allocator = allocator;
        this.helper = helper;
        int size = Serializers.INSTANCE.getSize(TreeEntry.class);
        int pageInfoSize = Serializers.INSTANCE.getSize(PageInfo.class);
        int sizeForEntries = allocator.getBlockSize() - pageInfoSize;
        if (sizeForEntries % size != 0) {
            throw new IllegalArgumentException("Block shall contain integer amount of entries (entry size = " + size + ", block size = " + allocator.getBlockSize());
        }
        maxAmount = sizeForEntries / size;
        if (maxAmount % 2 != 1) {
            throw new IllegalArgumentException("Block shall contain even nmber of entries (now " + maxAmount + ")");
        }
        if (maxAmount < 3) {
            throw new IllegalArgumentException("Block shall have at least " + (size*3 + pageInfoSize) + ": " + (size * 3) + "bytes (data) + " + pageInfoSize + " bytes (meta)");
        }
        t = (maxAmount+1) / 2;
        minAmount = t - 1;
        maxAmount = 2*t - 1;
        assert((maxAmount - 1) / 2 == minAmount);
        //so 1 is reserved for page info
        if (allocator.getBlocksCount() == 0) {
            initialize();
        }
        else {
            load();
        }

    }

    private void load() {
        metaBlockIdx = 0;
        TreeInfo meta = allocator.get(metaBlockIdx, TreeInfo.class);
        this.totalCount = meta.totalCount;
        this.firstPageBlockIdx = meta.rootPageIdx;
    }

    private void initialize() {
        try (BlockToModify<TreeInfo> metaBlock = allocator.allocateToModify(TreeInfo.class);
             BlockToModify<Page> firstPageBlock = allocator.allocateToModify(Page.class)
        )
        {
            metaBlockIdx = metaBlock.getBlockId();
            firstPageBlockIdx = firstPageBlock.getBlockId();
            metaBlock.getBlock().totalCount = 0;
            metaBlock.getBlock().rootPageIdx = firstPageBlock.getBlockId();
            firstPageBlock.getBlock().pageInfo.countOfEntries = 0;
            firstPageBlock.getBlock().pageInfo.lastChildPtr = Pointer.NULL_PTR;
        }
    }

    public long remove(String key) {
        Check.notNull(key);
        TreeEntry entry;
        try (Locker ignore = writeLock()) {
            entry = findAndDelete(firstPageBlockIdx, key, helper.truncate(key, KEY_PART_LENGTH), hash(key));
            if (entry != null) modCount++;
            Page firstPage = allocator.get(firstPageBlockIdx, Page.class);
            if (firstPage.getCount() == 0 && Pointer.isValidNext(firstPage.pageInfo.lastChildPtr)) {
                allocator.free(firstPageBlockIdx);
                firstPageBlockIdx = firstPage.pageInfo.lastChildPtr;
            }
        }
        return entry != null ? entry.data : Pointer.NULL_PTR;
    }

    public long size() {
        try (Locker ignore = readLock()) {
            return totalCount;
        }
    }


    @SuppressWarnings("unused")
    public void dump(PrintStream stream) {
        stream.println("B-tree, total = " + totalCount + ", t = " + t);
        int innerTotal = dump(firstPageBlockIdx, stream, " ");
        boolean ok = innerTotal == totalCount;
        stream.println("Calculated total is " + innerTotal + " " + (ok ? "ok" : (" != " + totalCount)));
        stream.println();
        if (!ok) {
            throw new RuntimeException("Tree is broken");
        }
    }


    public void put(String key, long data) {
        put(key, new LongData(data));
    }

    public void put(String key, TreeData data) {
        Check.notNull(key, data);
        try (Locker ignore = writeLock()) {
            InsertionResult result = findAndInsert(firstPageBlockIdx, new TreeEntryKey(key, helper.truncate(key, KEY_PART_LENGTH), hash(key)), data);
            if (result.requiresParentModification()) {
                try (BlockToModify<Page> newFirstPageBlock = allocator.allocateToModify(Page.class)) {
                    PageInfo pageInfo = newFirstPageBlock.getBlock().pageInfo;
                    pageInfo.countOfEntries = 1;
                    pageInfo.lastChildPtr = result.newBlockId;
                    newFirstPageBlock.getBlock().entries[0] = result.middlePointForParent;
                    result.middlePointForParent.childPtr = result.oldBlockId;
                    firstPageBlockIdx = newFirstPageBlock.getBlockId();
                }
            }
        }

    }

    public void close() {
        try (Locker ignore = writeLock()) {
            try (BlockToModify<TreeInfo> treeInfo = allocator.getToModify(metaBlockIdx, TreeInfo.class)) {
                treeInfo.getBlock().totalCount = totalCount;
                treeInfo.getBlock().rootPageIdx = firstPageBlockIdx;
            }
        }
        allocator.close();
    }

    public boolean contains(String key) {
        Check.notNull(key);
        try (Locker ignore = readLock()) {
            return find(firstPageBlockIdx, key, helper.truncate(key, KEY_PART_LENGTH), hash(key)) != null;
        }
    }

    public long get(String key) {
        Check.notNull(key);
        TreeEntry treeEntry;
        try (Locker ignore = readLock()) {
            treeEntry = find(firstPageBlockIdx, key, helper.truncate(key, KEY_PART_LENGTH), hash(key));
        }
        return treeEntry != null ? treeEntry.data : Pointer.NULL_PTR;
    }

    public Iterator<String> iterator() {
        try (Locker ignore = readLock()) {
            return new KeysIterator(modCount);
        }
    }

    private Locker readLock() {
        return new Locker(treeLock.readLock());
    }

    private Locker writeLock() {
        return new Locker(treeLock.writeLock());
    }

    private int hash(String key) {
        return key.hashCode();
    }

    private int dump(int page, PrintStream stream, String prefix) {
        int total = 0;
        Page pageBlock = allocator.get(page, Page.class);
        int count = pageBlock.pageInfo.countOfEntries;
        total += count;
        stream.println(prefix + "Node entries: " + count + "  <" + page + ">" );
        for (int i = 0; i < count; i++) {
            TreeEntry entry = pageBlock.entries[i];
            stream.println(prefix + i + ": " + entry.keyPart + " [#" + entry.hash + "]");
            if (entry.isBroken()) {
                stream.println(" --- BROKEN ---");
                throw new RuntimeException("Tree is broken");
            }
            if (entry.hasChildren()) {
                total += dump(entry.childPtr, stream, prefix + "  ");
            }
        }
        if (pageBlock.pageInfo.hasLastChild()) {
            stream.println(prefix + "E: last child");
            total += dump(pageBlock.pageInfo.lastChildPtr ,stream, prefix + "  ");
        }
        return total;
    }

    private TreeEntry findAndDelete(int page, String key, String truncatedKey, int hash) {
        TreeEntry deleted = null;
        try (BlockToModify<Page> pageBlock = allocator.getToModify(page, Page.class)) {
            Page pageStruct = pageBlock.getBlock();
            PageInfo pageInfo = pageStruct.pageInfo;
            int count = pageInfo.countOfEntries;
            int pos = Arrays.binarySearch(pageStruct.entries, 0, count, new TreeEntryKey(key, truncatedKey, hash), TREE_ENTRY_COMPARATOR);
            int childPagePtr;
            if (pos >= 0) {
                deleted = deleteEntryAt(pageStruct, pos, false);
                childPagePtr = pageStruct.child(pos);
            }
            else {
                pos = -pos-1;
                childPagePtr = pageStruct.child(pos);

                if (Pointer.isValidNext(childPagePtr)) {
                    deleted = findAndDelete(childPagePtr, key, truncatedKey, hash);
                }
            }
            if (childPagePtr == pageStruct.pageInfo.lastChildPtr && pos > 0) {
                pos-=1;
                childPagePtr = pageStruct.child(pos);
            }
            int nextChildPagePtr = pageStruct.nextChild(pos);
            if (Pointer.isValidNext(childPagePtr) && Pointer.isValidNext(nextChildPagePtr)) {
                rebalanceNode(pageStruct, pageInfo, pos, count, childPagePtr, nextChildPagePtr);
            }
        }
        return deleted;
    }

    private TreeEntry deleteEntryAt(Page pageStruct, int index, boolean detachOnly) {
        int count = pageStruct.getCount();
        TreeEntry entryToDelete = pageStruct.entries[index];
        if (!entryToDelete.hasChildren() || detachOnly) {
            deleteEntryFromPage(pageStruct, index, count);
            if (!detachOnly) {
                totalCount--;
            }
        }
        else {
            int childPagePtr = pageStruct.child(index);
            int nextChildPagePtr = pageStruct.nextChild(index);
            Page childPage = allocator.get(childPagePtr, Page.class);
            Page nextChildPage = Pointer.isValidNext(nextChildPagePtr) ? allocator.get(nextChildPagePtr, Page.class) : null;
            TreeEntry replacement;
            if (nextChildPage == null || childPage.getCount() > nextChildPage.getCount()) {
                replacement = deleteEntryAt(childPage, childPage.getCount() - 1, false);
                allocator.saveModifications(childPagePtr, childPage);
            }
            else {
                replacement = deleteEntryAt(nextChildPage, 0, false);
                allocator.saveModifications(nextChildPagePtr, nextChildPage);
            }
            pageStruct.entries[index] = replacement;
            replacement.childPtr = childPagePtr;
            entryToDelete.childPtr = Pointer.NULL_PTR;

            rebalanceNode(pageStruct, pageStruct.pageInfo, index, count, childPagePtr, nextChildPagePtr);

        }

        return entryToDelete;
    }

    private void rebalanceNode(Page pageStruct, PageInfo pageInfo, int pos, int count, int childPagePtr, int nextChildPagePtr) {
        if (!Pointer.isValidNext(nextChildPagePtr)) return;
        Page childPage = allocator.get(childPagePtr, Page.class);
        Page nextChildPage = allocator.get(nextChildPagePtr, Page.class);
        DeletionResult res = ensureCapacity(pageStruct.entries[pos], childPage, nextChildPage);
        switch (res.action) {
            case CHANGE_NEXT_CHILD_LINK:
                pageInfo.countOfEntries = deleteEntryFromPage(pageStruct, pos, count);
                pageStruct.setChild(pos, childPagePtr);
                break;
            case REPLACE_PARENT:
                pageStruct.entries[pos] = res.replacementEntry;
                break;
        }
        allocator.saveModifications(childPagePtr, childPage);
        allocator.saveModifications(nextChildPagePtr, nextChildPage);
    }


    private static enum AfterDelete {
        NOTHING,
        REPLACE_PARENT,
        CHANGE_NEXT_CHILD_LINK
    }

    private static class DeletionResult {
        final TreeEntry replacementEntry;
        final AfterDelete action;

        private DeletionResult(TreeEntry replacementEntry, AfterDelete action) {
            this.replacementEntry = replacementEntry;
            this.action = action;
        }

        public static DeletionResult done() {
            return new DeletionResult(null, AfterDelete.NOTHING);
        }

        public static DeletionResult replaceParent(TreeEntry replacementEntry) {
            return new DeletionResult(replacementEntry, AfterDelete.REPLACE_PARENT);
        }

        public static DeletionResult deleteParent() {
            return new DeletionResult(null, AfterDelete.CHANGE_NEXT_CHILD_LINK);
        }
    }

    private DeletionResult ensureCapacity(TreeEntry parent, Page childLeft, Page childRight) {
        if (childLeft.getCount() >= minAmount && childRight.getCount() >= minAmount) return DeletionResult.done();
        int oldPtr = parent.childPtr;
        if (childLeft.getCount() + childRight.getCount() + 1 <= minAmount + minAmount) {
            //unite
            childLeft.entries[childLeft.pageInfo.countOfEntries] = parent;
            parent.childPtr = childLeft.pageInfo.lastChildPtr;
            System.arraycopy(childRight.entries, 0, childLeft.entries, childLeft.pageInfo.countOfEntries + 1, childRight.pageInfo.countOfEntries);
            childLeft.pageInfo.countOfEntries += 1 + childRight.pageInfo.countOfEntries;
            childLeft.pageInfo.lastChildPtr = childRight.pageInfo.lastChildPtr;
            return DeletionResult.deleteParent();
        }
        else  {
            TreeEntry newParent;
            if (childLeft.getCount() < minAmount) {
                //shift one item from right
                childLeft.entries[childLeft.pageInfo.countOfEntries] = parent;
                childLeft.pageInfo.countOfEntries++;
                parent.childPtr = childLeft.pageInfo.lastChildPtr;
                newParent = deleteEntryAt(childRight, 0, true);
            } else {    // == childRight.getCount() < minAmount
                //shift one from left
                System.arraycopy(childRight.entries, 0, childRight.entries, 1, childRight.getCount());
                childRight.entries[0] = parent;
                childRight.pageInfo.countOfEntries++;
                parent.childPtr = childLeft.pageInfo.lastChildPtr;
                newParent = deleteEntryAt(childLeft, childLeft.getCount() - 1, true);
            }
            childLeft.pageInfo.lastChildPtr = newParent.childPtr;
            newParent.childPtr = oldPtr;
            return DeletionResult.replaceParent(newParent);
        }
    }


    private int deleteEntryFromPage(Page page, int index, int count) {
        System.arraycopy(page.entries, index + 1, page.entries, index, count - 1 - index);
        page.pageInfo.countOfEntries--;
        return page.pageInfo.countOfEntries;
    }


    private TreeEntry find(int page, String key, String truncatedKey, int hash) {
        Page pageStruct = allocator.get(page, Page.class);
        PageInfo pageInfo = pageStruct.pageInfo;
        int count = pageInfo.countOfEntries;
        int pos = Arrays.binarySearch(pageStruct.entries, 0, count, new TreeEntryKey(key, truncatedKey, hash), TREE_ENTRY_COMPARATOR);
        if (pos >= 0) {
            return pageStruct.entries[pos];
        }
        else {
            int insertionPoint = -pos-1;
            int nextPage = insertionPoint != count ? pageStruct.entries[insertionPoint].childPtr : pageInfo.lastChildPtr;

            if (Pointer.isValidNext(nextPage)) {
                return find(nextPage, key, truncatedKey, hash);
            }
            else {
                return null;
            }
        }
    }


    private InsertionResult findAndInsert(int page, TreeEntryKey key, TreeData data) {
        Page pageStruct = allocator.get(page, Page.class);
        PageInfo pageInfo = pageStruct.pageInfo;
        int count = pageInfo.countOfEntries;
        int pos = Arrays.binarySearch(pageStruct.entries, 0, count, key, TREE_ENTRY_COMPARATOR);
        if (pos >= 0) {
            TreeEntry entry = pageStruct.entries[pos];
            entry.data = data.updateData(entry.data);
            allocator.saveModifications(page, pageStruct);
            return InsertionResult.DONE;

        }
        else {
            int insertionPoint = -pos-1;
            boolean isLast = insertionPoint == count;
            int nextPage = isLast ? pageInfo.lastChildPtr : pageStruct.entries[insertionPoint].childPtr;

            InsertionResult result = insertHereOrChild(page, pageStruct, key, data, nextPage, insertionPoint, count);
            if (result.requiresParentModification()) {
                if (isLast) {
                    pageInfo.lastChildPtr = result.newBlockId;
                }
                else {
                    pageStruct.entries[insertionPoint+1].childPtr = result.newBlockId;
                }
                allocator.saveModifications(page, pageStruct);
            }

        }
        return ensureCapacity(page, pageStruct);

    }

    private InsertionResult ensureCapacity(int page, Page pageStruct) {
        InsertionResult result = InsertionResult.DONE;
        if (pageStruct.pageInfo.countOfEntries == maxAmount) {
            TreeEntry middlePoint = pageStruct.entries[t - 1];
            pageStruct.pageInfo.countOfEntries = minAmount; //truncate size
            try (BlockToModify<Page> newPageBlock = allocator.allocateToModify(Page.class)) {
                Page newPage = newPageBlock.getBlock();
                newPage.pageInfo.countOfEntries = minAmount;
                newPage.pageInfo.lastChildPtr = pageStruct.pageInfo.hasLastChild() ? pageStruct.pageInfo.lastChildPtr : Pointer.NULL_PTR;
                pageStruct.pageInfo.lastChildPtr = middlePoint.hasChildren() ? middlePoint.childPtr : Pointer.NULL_PTR;
                System.arraycopy(pageStruct.entries, t, newPage.entries, 0, minAmount);
                result = new InsertionResult(middlePoint, page, newPageBlock.getBlockId());
            }
            allocator.saveModifications(page, pageStruct);
        }
        return result;
    }

    private InsertionResult insertHereOrChild(int page, Page pageStruct, TreeEntryKey key, TreeData data, int nextPage, int index, int count) {
        InsertionResult result = InsertionResult.DONE;
        if (Pointer.isValidNext(nextPage)) {
            result = findAndInsert(nextPage, key, data);
            if (result.requiresParentModification()) {
                insertBefore(pageStruct, index, new TreeEntryKey(getWholeKey(result.middlePointForParent), result.middlePointForParent.keyPart, result.middlePointForParent.hash), result.middlePointForParent.data, count, result.oldBlockId);
                pageStruct.pageInfo.countOfEntries++;
                allocator.saveModifications(page, pageStruct);
            }
        }
        else {
            insertBefore(pageStruct, index, key, data.createData(), count, Pointer.NULL_PTR);
            pageStruct.pageInfo.countOfEntries++;
            modCount++;
            totalCount++;
            allocator.saveModifications(page, pageStruct);
        }
        return result;
    }

    private String getWholeKey(TreeEntry entry) {
        return entry.isWholeKey() ? entry.keyPart : helper.getFullName(entry.data);
    }


    private void insertBefore(Page pageStruct, int index, TreeEntryKey key, long data, int count, int childPtr) {
        if (index < count) {
            System.arraycopy(pageStruct.entries, index, pageStruct.entries, index + 1, count - index);
        }
        pageStruct.entries[index] = new TreeEntry(key.fullKey, key.hash, data, helper, childPtr);
    }

    private int compare(int hash, String truncatedKey, String key, TreeEntry entry) {
        int result = hash == entry.hash ? 0 : (hash > entry.hash ? 1 : -1);
        if (result == 0) {
            result = truncatedKey.compareTo(entry.keyPart);
        }
        if (result == 0) {
            result = key.compareTo(getWholeKey(entry));
        }
        return result;
    }



    private class TreeEntryKey {

        private String fullKey;
        private String truncatedKey;
        private int hash;

        private TreeEntryKey(String fullKey, String truncatedKey, int hash) {
            this.fullKey = fullKey;
            this.truncatedKey = truncatedKey;
            this.hash = hash;
        }

    }

    private static class InsertionResult {
        public final TreeEntry middlePointForParent;
        public final int oldBlockId;
        public final int newBlockId;

        InsertionResult(TreeEntry middlePointForParent, int oldBlockId, int newBlockId) {
            this.middlePointForParent = middlePointForParent;
            this.oldBlockId = oldBlockId;
            this.newBlockId = newBlockId;
        }

        static InsertionResult DONE = new InsertionResult(null, Pointer.NULL_PTR, Pointer.NULL_PTR);

        public boolean requiresParentModification() {
            return middlePointForParent != null;
        }
    }


    private class KeysIterator implements Iterator<String> {

        private final int currentModCount;

        public KeysIterator(int currentModCount) {
            this.currentModCount = currentModCount;
            pagesToInspect = new HashSet<>();
            goTo(firstPageBlockIdx);
            goNext();
        }

        private void ensureNotModified() {
            if (currentModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }

        private Page currentPage;
        private int count;
        private int index;
        private String nextString;
        private Set<Integer> pagesToInspect;

        private void goTo(int pointer) {
            currentPage = allocator.get(pointer, Page.class);
            PageInfo pageInfo = currentPage.pageInfo;
            count = pageInfo.countOfEntries;
            if (pageInfo.hasLastChild()) {
                pagesToInspect.add(pageInfo.lastChildPtr);
            }
            index = -1;
            pagesToInspect.remove(pointer);
        }

        @Override
        public boolean hasNext() {
            return nextString != null;
        }

        private void goNext() {
            ensureNotModified();
            index++;
            String next = null;
            while (index >= count && !pagesToInspect.isEmpty()) {
                int first = pagesToInspect.iterator().next();
                goTo(first);
                index++;
            }
            if (index < count) {
                TreeEntry entry = currentPage.entries[index];
                next = getWholeKey(entry);
                if (entry.hasChildren()) {
                    pagesToInspect.add(entry.childPtr);
                }
            }
            nextString = next;
        }

        @Override
        public String next() {
            if (nextString == null) {
                throw new NoSuchElementException();
            }
            String ret = nextString;
            goNext();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
