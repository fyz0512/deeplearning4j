package org.datavec.api.records.reader.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.dbutils.DbUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.datavec.api.conf.Configuration;
import org.datavec.api.records.listener.RecordListener;
import org.datavec.api.records.listener.impl.LogRecordListener;
import org.datavec.api.records.reader.impl.jdbc.JDBCRecordReader;
import org.datavec.api.split.CollectionInputSplit;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDBCRecordReaderTest {

    Connection conn;
    EmbeddedDataSource dataSource;

    private final String dbName = "datavecTests";
    private final String driverClassName = "org.apache.derby.jdbc.EmbeddedDriver";

    @Before
    public void setUp() throws Exception {
        dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(dbName);
        dataSource.setCreateDatabase("create");
        conn = dataSource.getConnection();

        TestDb.dropTables(conn);
        TestDb.buildCoffeeTable(conn);
    }

    @After
    public void tearDown() throws Exception {
        DbUtils.closeQuietly(conn);
    }

    @Test
    public void testSimpleIter() throws Exception {
        JDBCRecordReader reader = getInitializedReader("SELECT * FROM Coffee");
        List<List<Writable>> records = new ArrayList<>();
        while (reader.hasNext()) {
            List<Writable> values = reader.next();
            records.add(values);
        }
        reader.close();
        assertFalse(records.isEmpty());

        List<Writable> first = records.get(0);
        assertEquals(new Text("Bolivian Dark"), first.get(0));
        assertEquals(new Text("14-001"), first.get(1));
        assertEquals(new DoubleWritable(8.95), first.get(2));
    }

    @Test
    public void testSimpleWithListener() throws Exception {
        JDBCRecordReader reader = getInitializedReader("SELECT * FROM Coffee");
        RecordListener recordListener = new LogRecordListener();
        reader.setListeners(recordListener);
        reader.next();
        reader.close();
        assertTrue(recordListener.invoked());
    }

    @Test
    public void testReset() throws Exception {
        JDBCRecordReader reader = getInitializedReader("SELECT * FROM Coffee");
        List<List<Writable>> records = new ArrayList<>();
        records.add(reader.next());
        reader.reset();
        records.add(reader.next());
        reader.close();

        assertEquals(2, records.size());
        assertEquals(new Text("Bolivian Dark"), records.get(0).get(0));
        assertEquals(new Text("Bolivian Dark"), records.get(1).get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testLackingDataSourceShouldFail() throws Exception {
        JDBCRecordReader reader = new JDBCRecordReader("SELECT * FROM Coffee");
        reader.initialize(null);
    }

    @Test
    public void testConfigurationDataSourceInitialization() throws Exception {
        JDBCRecordReader reader = new JDBCRecordReader("SELECT * FROM Coffee");
        Configuration conf = new Configuration();
        conf.set(JDBCRecordReader.JDBC_URL, "jdbc:derby:"+dbName+";create=true");
        conf.set(JDBCRecordReader.JDBC_DRIVER_CLASS_NAME, driverClassName);
        reader.initialize(conf, null);
        assertTrue(reader.hasNext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitConfigurationMissingParametersShouldFail() throws Exception {
        JDBCRecordReader reader = new JDBCRecordReader("SELECT * FROM Coffee");
        Configuration conf = new Configuration();
        conf.set(JDBCRecordReader.JDBC_URL, "should fail anyway");
        reader.initialize(conf, null);
    }

    private JDBCRecordReader getInitializedReader(String query) throws Exception {
        JDBCRecordReader reader = new JDBCRecordReader(query, dataSource);
        reader.setTrimStrings(true);
        reader.initialize(null);
        return reader;
    }
}