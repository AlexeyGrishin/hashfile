package io.github.alexeygrishin;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.common.Files;
import io.github.alexeygrishin.common.Source;
import io.github.alexeygrishin.hashfile.NameBasedStorageFactory;
import io.github.alexeygrishin.hashfile.NamedStorage;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
    private NameBasedStorageFactory factory;
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
    public void list() throws FileNotFoundException, ParseException {
        when(storageMock.iterator()).thenReturn(Arrays.asList("a", "b").iterator());
        assertOutput(lines("a", "b"), "path1", "--list");
        verify(factory).load("path1");
    }

    @Test
    public void empty() throws FileNotFoundException, ParseException {
        when(storageMock.iterator()).thenReturn(Arrays.<String>asList().iterator());
        assertOutput(lines("<Empty>"), "path1", "--list");
        verify(factory).load("path1");
    }

    @Test
    public void create() throws FileNotFoundException, ParseException {
        assertOutput(lines("Done"), "path1", "--new");
        verify(factory).create("path1", null, null, null);
    }

    @Test
    public void create_blockSize() throws FileNotFoundException, ParseException {
        assertOutput(lines("Done"), "path1", "--new", "block=2");
        verify(factory).create("path1", 2, null, null);
    }

    @Test
    public void create_cacheSize() throws FileNotFoundException, ParseException {
        assertOutput(lines("Done"), "path1", "--new", "cache=2");
        verify(factory).create("path1", null, 2, null);
    }

    @Test
    public void create_truncate() throws FileNotFoundException, ParseException {
        assertOutput(lines("Done"), "path1", "--new", "cache=4,truncate=trailing");
        verify(factory).create("path1", null, 4, KeyTruncateMethod.TRAILING);
    }

    @Test
    public void importFrom_withoutKey() throws FileNotFoundException, ParseException {
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
    public void importFrom_withKey() throws FileNotFoundException, ParseException {
        processArgs("path1", "--import-from", "file1", "--key", "key1");
        verify(files).getSources("file1");
        verify(source1).openInputStream();
        verify(factory).load("path1");
        verify(storageMock).saveFrom("key1", inputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }
    //TODO: check invalid values

    @Test
    public void importFrom_folder_withoutKey() throws FileNotFoundException, ParseException {
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

    @Test
    public void importFrom_folder_withKey() throws FileNotFoundException, ParseException {
        when(files.getSources("folder1")).thenReturn(new Source[] {source1, source2});
        processArgs("path1", "--import-from", "folder1", "--key", "key1");
        verify(storageMock).saveFrom(DEFAULT_KEY, inputStream);
        verify(storageMock).saveFrom(DEFAULT_KEY_2, inputStream);
        //key is ignored
    }

    @Test
    public void exportTo() throws FileNotFoundException, ParseException {
        processArgs("path1", "--export-to", "file1", "--key", "key1");
        verify(files).getSources("file1");
        verify(source1).openOutputStream();
        verify(factory).load("path1");
        verify(storageMock).getInto("key1", outputStream);
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, source1, storageMock);
    }

    @Test
    public void check() throws FileNotFoundException, ParseException {
        when(storageMock.contains("key1")).thenReturn(true);
        assertOutput(lines("Yes"), "path1", "--key", "key1", "--check");
        verify(factory).load("path1");
        verify(storageMock).contains("key1");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void delete() throws FileNotFoundException, ParseException {
        processArgs("path1", "--key", "key2", "--delete");
        verify(factory).load("path1");
        verify(storageMock).delete("key2");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void truncate_standalone() throws FileNotFoundException, ParseException {
        processArgs("path1", "--optimize");
        verify(factory).load("path1");
        verify(factory).truncate("path1");
        verify(storageMock).close();
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    @Test
    public void truncate_delete() throws FileNotFoundException, ParseException {
        processArgs("path1", "--key", "key1", "--delete", "--optimize");
        verify(factory).load("path1");
        verify(storageMock).delete("key1");
        verify(storageMock).close();
        verify(factory).truncate("path1");
        verifyNoMoreInteractions(files, factory, storageMock);
    }

    //TODO: InvalidSyntaxException,

    private void assertOutput(String expectation, String... params) throws FileNotFoundException, ParseException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        api.process(params, new PrintStream(stream));
        assertEquals(expectation, stream.toString());
    }

    private void processArgs(String... params) throws FileNotFoundException, ParseException {
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
