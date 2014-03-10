package io.github.alexeygrishin;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.common.Files;
import io.github.alexeygrishin.common.Source;
import io.github.alexeygrishin.hashfile.NamedStorageFactory;
import io.github.alexeygrishin.hashfile.NamedStorage;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CommandLineApiTest {

    public static final String DEFAULT_KEY = "dkey1";
    public static final String DEFAULT_KEY_2 = "dkey2";
    @Mock
    private NamedStorage storageMock;
    @Mock
    private NamedStorageFactory factory;
    @Mock
    private Files files;
    @Mock
    private Source source1, source2;
    @Mock
    private InputStream inputStream;
    @Mock
    private OutputStream outputStream;

    private CommandLineAPI api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        api = new CommandLineAPI(factory, files);
        when(factory.create(anyString(), any(Integer.class), any(Integer.class), any(KeyTruncateMethod.class))).thenReturn(storageMock);
        when(factory.load(anyString())).thenReturn(storageMock);
        when(source1.openInputStream()).thenReturn(inputStream);
        when(source1.openOutputStream()).thenReturn(outputStream);
        when(source2.openInputStream()).thenReturn(inputStream);
        when(source2.openOutputStream()).thenReturn(outputStream);
        when(source1.toKey()).thenReturn(DEFAULT_KEY);
        when(source2.toKey()).thenReturn(DEFAULT_KEY_2);
        when(files.getSources(anyString())).thenReturn(new Source[] {source1});
    }

    @Test
    public void list() throws Exception {
        when(storageMock.iterator()).thenReturn(Arrays.asList("a", "b").iterator());
        assertOutput(lines("a", "b"), "path1", "--list");
        verify(factory).load("path1");
    }

    @Test
    public void empty() throws Exception {
        when(storageMock.iterator()).thenReturn(Arrays.<String>asList().iterator());
        assertOutput(lines("<Empty>"), "path1", "--list");
        verify(factory).load("path1");
    }

    @Test
    public void create() throws Exception {
        assertOutput(lines("Done"), "path1", "--new");
        verify(factory).create("path1", null, null, null);
    }

    @Test
    public void create_blockSize() throws Exception {
        assertOutput(lines("Done"), "path1", "--new", "block=2");
        verify(factory).create("path1", 2, null, null);
    }

    @Test
    public void create_cacheSize() throws Exception {
        assertOutput(lines("Done"), "path1", "--new", "cache=2");
        verify(factory).create("path1", null, 2, null);
    }

    @Test
    public void create_truncate_trailing() throws Exception {
        assertOutput(lines("Done"), "path1", "--new", "cache=4,truncate=trailing");
        verify(factory).create("path1", null, 4, KeyTruncateMethod.TRAILING);
    }

    @Test
    public void create_truncate_leading() throws Exception {
        assertOutput(lines("Done"), "path1", "--new", "cache=4,truncate=leading");
        verify(factory).create("path1", null, 4, KeyTruncateMethod.LEADING);
    }

    @Test
    public void importFrom_withoutKey() throws Exception {
        processArgs("path1", "--import-from", "file1");
        verify(files).getSources("file1");
        verify(source1).openInputStream();
        verify(source1).toKey();
        verify(factory).load("path1");
        verify(storageMock).saveFrom(DEFAULT_KEY, inputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }

    @Test
    public void importFrom_withKey() throws Exception {
        processArgs("path1", "--import-from", "file1", "--key", "key1");
        verify(files).getSources("file1");
        verify(source1).openInputStream();
        verify(factory).load("path1");
        verify(storageMock).saveFrom("key1", inputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }

    @Test
    public void importFrom_folder_withoutKey() throws Exception {
        when(files.getSources("folder1")).thenReturn(new Source[] {source1, source2});
        processArgs("path1", "--import-from", "folder1");
        verify(factory).load("path1");
        verify(files).getSources("folder1");
        verify(source1).openInputStream();
        verify(source2).openInputStream();
        verify(source1).toKey();
        verify(source2).toKey();
        verify(storageMock).saveFrom(DEFAULT_KEY, inputStream);
        verify(storageMock).saveFrom(DEFAULT_KEY_2, inputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }

    @Test(expected = InvalidSyntax.class)
    public void importFrom_folder_withKey() throws Exception{
        when(files.getSources("folder1")).thenReturn(new Source[] {source1, source2});
        processArgs("path1", "--import-from", "folder1", "--key", "key1");
    }

    @Test
    public void exportTo() throws Exception {
        processArgs("path1", "--export-to", "file1", "--key", "key1");
        verify(files).getSources("file1");
        verify(source1).openOutputStream();
        verify(factory).load("path1");
        verify(storageMock).getInto("key1", outputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }

    @Test
    public void check() throws Exception {
        when(storageMock.contains("key1")).thenReturn(true);
        assertOutput(lines("Yes"), "path1", "--key", "key1", "--check");
        verify(factory).load("path1");
        verify(storageMock).contains("key1");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void delete() throws Exception {
        processArgs("path1", "--key", "key2", "--delete");
        verify(factory).load("path1");
        verify(storageMock).delete("key2");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void truncate_standalone() throws Exception {
        processArgs("path1", "--optimize");
        verify(factory).load("path1");
        verify(factory).truncate("path1");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void truncate_delete() throws Exception {
        processArgs("path1", "--key", "key1", "--delete", "--optimize");
        verify(factory).load("path1");
        verify(storageMock).delete("key1");
        verify(storageMock).close();
        verify(factory).truncate("path1");
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test(expected = InvalidSyntax.class)
    public void exportTo_withoutKey() throws Exception {
        processArgs("path1", "--export-to", "file1");
    }

    @Test(expected = InvalidSyntax.class)
    public void check_withoutKey() throws Exception {
        processArgs("path1", "--check");
    }

    @Test(expected = InvalidSyntax.class)
    public void delete_withotKey() throws Exception {
        processArgs("path1", "--delete");
    }

    @Test(expected = InvalidSyntax.class)
    public void new_unknownOption() throws Exception {
        processArgs("path1", "--new", "unknown=3");
    }

    @Test(expected = InvalidSyntax.class)
    public void new_truncateInvalid() throws Exception {
        processArgs("path1", "--new", "truncate=leadin");
    }

    private void assertOutput(String expectation, String... params) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        api.process(params, new PrintStream(stream));
        assertEquals(expectation, stream.toString());
    }

    private void processArgs(String... params) throws Exception {
        api.process(params, toNull());
    }

    private PrintStream toNull() {
        return new PrintStream(new ByteArrayOutputStream());
    }

    private String lines(String... lines) {
        StringBuilder bld = new StringBuilder();
        for (String line: lines) {
            bld.append(line).append(System.getProperty("line.separator"));
        }
        return bld.toString();
    }
}
