package io.github.alexeygrishin.hashfile.btreebased;

import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.blockalloc.BlockToModify;
import io.github.alexeygrishin.btree.NoSuchElement;
import io.github.alexeygrishin.common.LockMap;
import io.github.alexeygrishin.common.Pointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DataStorage implements DataContainer{

    private Allocator allocator;
    private LockMap<Integer> pageLocker = new LockMap<>();

    public DataStorage(Allocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public String getFullName(int blockIdx) {
        try (LockMap.AutoLock ignore = pageLocker.lockRead(blockIdx)) {
            DataPage firstBlock = getNonDeletedPage(blockIdx);
            byte[] nameAsBytes = new byte[firstBlock.wholeNameLen];
            int offset = 0;
            DataPage currentBlock = firstBlock;
            while (currentBlock.hasNamePart()) {
                offset = currentBlock.getNamePart(nameAsBytes, offset);
                int next = currentBlock.nextPage;

                if (!Pointer.isValid(next)) {
                    break;
                }
                currentBlock = getNonDeletedPage(next);
            }
            return new String(nameAsBytes, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void update(int blockIdx, InputStream stream) {
        try (LockMap.AutoLock ignore = pageLocker.lockWrite(blockIdx)) {
            DataPage currentBlock = getNonDeletedPage(blockIdx);
            int next = findFirstDataBlockIdx(blockIdx, currentBlock);
            saveDataStartingFrom(stream, getNonDeletedPageToModify(next));
        } catch (IOException e) {
            throw new DataException(e);
        }
    }

    @Override
    public void select(int blockIdx, OutputStream stream) {
        try (LockMap.AutoLock ignore = pageLocker.lockRead(blockIdx)) {
            DataPage currentBlock = getNonDeletedPage(blockIdx);
            int next = findFirstDataBlockIdx(blockIdx, currentBlock);
            while (Pointer.isValid(next)) {
                currentBlock = getNonDeletedPage(next);
                currentBlock.sendData(stream);
                next = currentBlock.nextPage;
            }
            stream.close();
        } catch (IOException e) {
            throw new DataException(e);
        }

    }

    @Override
    public int insert(String fullName, InputStream stream) {
        byte[] nameInBytes = fullName.getBytes();
        int createdBlockId = Pointer.NULL_PTR;
        try {   //no need for lock here - allocator shall be thread-safe for adding
            BlockToModify<DataPage> firstBlock = allocator.allocateToModify(DataPage.class);
            firstBlock.getBlock().nextPage = Pointer.NULL_PTR;
            createdBlockId = firstBlock.getBlockId();
            firstBlock.getBlock().wholeNameLen = fullName.length();
            int nextNamePos = 0;
            BlockToModify<DataPage> current = firstBlock;
            do {
                nextNamePos = saveName(nameInBytes, nextNamePos, current.getBlock());
                if (nextNamePos != -1) {
                    BlockToModify<DataPage> next = allocator.allocateToModify(DataPage.class);
                    next.getBlock().nextPage = Pointer.NULL_PTR;
                    current.getBlock().nextPage = next.getBlockId();
                    current.close();
                    current = next;
                }
            } while (nextNamePos != -1);

            saveDataStartingFrom(stream, current);
            return firstBlock.getBlockId();
        }
        catch (IOException e) {
            if (Pointer.isValid(createdBlockId))
                delete(createdBlockId);
            throw new DataException(e);
        }
    }


    @Override
    public void delete(int blockIdx) {
        int current = blockIdx;
        try (LockMap.AutoLock ignore = pageLocker.lockWrite(blockIdx)) {
            do {
                int next;
                //do not use getNonDeletedPageToModify here - it throws exception if page is deleted, but we may ignore it
                try (BlockToModify<DataPage> data = allocator.getToModify(current, DataPage.class)) {
                    data.getBlock().deleted = 1;
                    next = data.getBlock().nextPage;
                }
                allocator.free(current);
                current = next;
            } while (Pointer.isValidNext(current));
        }
    }

    public void close() {
        allocator.close();
    }

    private int findFirstDataBlockIdx(int blockIdx, DataPage currentBlock) {
        int next = blockIdx;
        while (!currentBlock.hasDataPart()) {
            next = currentBlock.nextPage;
            assert Pointer.isValidNext(next);
            currentBlock = getNonDeletedPage(next);
        }
        return next;
    }

    private DataPage getNonDeletedPage(int idx) {
        DataPage page = allocator.get(idx, DataPage.class);
        page.ensureNotDeleted();
        return page;
    }

    private BlockToModify<DataPage> getNonDeletedPageToModify(int idx) {
        BlockToModify<DataPage> page = allocator.getToModify(idx, DataPage.class);
        page.getBlock().ensureNotDeleted();
        return page;
    }

    private void saveDataStartingFrom(InputStream stream, BlockToModify<DataPage> current) throws IOException {
        Status status;
        status = saveData(stream, current.getBlock());
        while (status == Status.CONTINUE) {
            DataPage nextPage = new DataPage();
            nextPage.bytes = new byte[current.getBlock().bytes.length];
            status = saveData(stream, nextPage);
            if (status != Status.EMPTY) {
                BlockToModify<DataPage> next;
                if (Pointer.isNullNext(current.getBlock().nextPage)) {
                    next = allocator.allocateToModify(DataPage.class);
                    next.getBlock().nextPage = Pointer.NULL_PTR;
                    current.getBlock().nextPage = next.getBlockId();
                }
                else {
                    next = getNonDeletedPageToModify(current.getBlock().nextPage);
                }
                next.getBlock().setData(nextPage);
                current.close();
                current = next;
            }
        }
        if (Pointer.isValidNext(current.getBlock().nextPage)) {
            delete(current.getBlock().nextPage);
            current.getBlock().nextPage = Pointer.NULL_PTR;
        }
        current.close();
    }

    private enum Status {
        CONTINUE, DONE, EMPTY
    }

    private Status saveData(InputStream stream, DataPage page) throws IOException {
        if (page.dataPortionOffset == 0) {
            page.dataPortionLength = page.bytes.length;
        }
        Status status = Status.CONTINUE;
        if (page.dataPortionLength == 0) {
            return status;
        }
        int result = stream.read(page.bytes, page.dataPortionOffset, page.dataPortionLength);
        if (result == -1) {
            status = Status.EMPTY;
        }
        else if (result != page.dataPortionLength) {
            status = Status.DONE;
        }
        page.dataPortionLength = result;
        return status;
    }

    private int saveName(byte[] nameInBytes, int copyFrom, DataPage page) {
        int bytesToSave = Math.min(nameInBytes.length - copyFrom, page.bytes.length);
        System.arraycopy(nameInBytes, copyFrom, page.bytes, 0, bytesToSave);
        page.dataPortionOffset = bytesToSave;
        page.dataPortionLength = page.bytes.length - bytesToSave;
        int nextPortion = copyFrom + bytesToSave;
        if (nextPortion == nameInBytes.length) nextPortion = -1;
        return nextPortion;
    }



    public static class DataPage {
        public byte deleted;
        public int nextPage;
        public int wholeNameLen;
        public int dataPortionOffset;
        public int dataPortionLength;
        public byte[] bytes;

        public boolean hasNamePart() {
            return dataPortionOffset != 0;
        }

        public boolean hasDataPart() {
            return dataPortionLength != 0;
        }

        public int getNamePart(byte[] nameAsBytes, int offset) {
            System.arraycopy(bytes, 0, nameAsBytes, offset, dataPortionOffset);
            return offset + dataPortionOffset;
        }

        public void sendData(OutputStream stream) throws IOException {
            stream.write(bytes, dataPortionOffset, dataPortionLength);
        }

        public void setData(DataPage page) {
            this.bytes = page.bytes;
            this.dataPortionLength = page.dataPortionLength;
            this.dataPortionOffset = page.dataPortionOffset;
        }

        public void ensureNotDeleted() {
            if (deleted > 0) {
                throw new NoSuchElement();
            }
        }
    }





}
