package org.horizon.config.parsing;

import org.horizon.config.CacheLoaderManagerConfig;
import org.horizon.config.Configuration;
import org.horizon.eviction.EvictionStrategy;
import org.horizon.loader.CacheStoreConfig;
import org.horizon.loader.decorators.SingletonStoreConfig;
import org.horizon.loader.jdbc.TableManipulation;
import org.horizon.loader.jdbc.connectionfactory.ConnectionFactoryConfig;
import org.horizon.loader.jdbc.connectionfactory.PooledConnectionFactory;
import org.horizon.loader.jdbc.stringbased.JdbcStringBasedCacheStore;
import org.horizon.loader.jdbc.stringbased.JdbcStringBasedCacheStoreConfig;
import org.horizon.lock.IsolationLevel;
import org.horizon.transaction.GenericTransactionManagerLookup;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

@Test(groups = "unit", testName = "config.parsing.ConfigurationParserTest")
public class ConfigurationParserTest {

   public void testLocking() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<locking\n" +
            "            isolationLevel=\"REPEATABLE_READ\"\n" +
            "            lockAcquisitionTimeout=\"200000\"\n" +
            "            writeSkewCheck=\"true\"\n" +
            "            concurrencyLevel=\"5\"/>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureLocking(e, c);

      assert c.getIsolationLevel() == IsolationLevel.REPEATABLE_READ;
      assert c.getLockAcquisitionTimeout() == 200000;
      assert c.isWriteSkewCheck();
      assert c.getConcurrencyLevel() == 5;
   }

   public void testTransactions() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<transaction\n" +
            "            transactionManagerLookupClass=\"org.blah.Blah\"\n" +
            "            syncRollbackPhase=\"true\"\n" +
            "            syncCommitPhase=\"true\"/>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureTransaction(e, c);

      assert c.getTransactionManagerLookupClass().equals("org.blah.Blah");
      assert c.isSyncCommitPhase();
      assert c.isSyncRollbackPhase();
   }

   public void testTransactionsDefaults() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<transaction />";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureTransaction(e, c);

      assert c.getTransactionManagerLookupClass().equals(GenericTransactionManagerLookup.class.getName());
      assert !c.isSyncCommitPhase();
      assert !c.isSyncRollbackPhase();
   }

   public void testCacheJmxStatistics() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<jmxStatistics enabled=\"true\"/>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureCacheJmxStatistics(e, c);

      assert c.isExposeJmxStatistics();
   }

   public void testLazyDeserialization() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<lazyDeserialization enabled=\"true\"/>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureLazyDeserialization(e, c);

      assert c.isExposeJmxStatistics();
   }

   public void testJmxStatisticsDefaults() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<jmxStatistics />";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureCacheJmxStatistics(e, c);

      assert c.isExposeJmxStatistics();
   }

   public void testInvocationBatching() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<invocationBatching enabled=\"true\"/>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureInvocationBatching(e, c);

      assert c.isInvocationBatchingEnabled();
   }

   public void testInvocationBatchingDefaults() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<invocationBatching />";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureInvocationBatching(e, c);

      assert c.isInvocationBatchingEnabled();
   }

   public void testClustering() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<clustering mode=\"invalidation\">\n" +
            "         <stateRetrieval timeout=\"20000\" fetchInMemoryState=\"false\"/>\n" +
            "         <async useReplQueue=\"true\" replQueueInterval=\"10000\" replQueueMaxElements=\"500\"/>\n" +
            "      </clustering>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureClustering(e, c);

      assert c.getCacheMode() == Configuration.CacheMode.INVALIDATION_ASYNC;
      assert c.getStateRetrievalTimeout() == 20000;
      assert !c.isFetchInMemoryState();
      assert c.isUseReplQueue();
      assert c.getReplQueueInterval() == 10000;
      assert c.getReplQueueMaxElements() == 500;
   }

   public void testClusteringDefaults() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<clustering />";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureClustering(e, c);

      assert c.getCacheMode() == Configuration.CacheMode.REPL_SYNC;
      assert c.getStateRetrievalTimeout() == 10000;
      assert c.isFetchInMemoryState();
      assert !c.isUseReplQueue();
   }

   public void testCacheLoaders() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml =
            "      <loaders passivation=\"true\" shared=\"true\" preload=\"true\">\n" +
            "         <loader class=\"org.horizon.loader.jdbc.stringbased.JdbcStringBasedCacheStore\" fetchPersistentState=\"true\"\n" +
            "                 ignoreModifications=\"true\" purgeOnStartup=\"true\">\n" +
            "            <properties>\n" +
            "               <property name=\"connectionFactoryClass\" value=\"org.horizon.loader.jdbc.connectionfactory.PooledConnectionFactory\"/>\n" +
            "               <property name=\"connectionUrl\" value=\"jdbc://some-url\"/>\n" +
            "               <property name=\"userName\" value=\"root\"/>\n" +
            "               <property name=\"driverClass\" value=\"org.dbms.Driver\"/>\n" +
            "               <property name=\"idColumnType\" value=\"VARCHAR2(256)\"/>\n" +
            "               <property name=\"dataColumnType\" value=\"BLOB\"/>\n" +
            "               <property name=\"dropTableOnExit\" value=\"true\"/>\n" +
            "               <property name=\"createTableOnStart\" value=\"false\"/>\n" +
            "            </properties>\n" +
            "            <singletonStore enabled=\"true\" pushStateWhenCoordinator=\"true\" pushStateTimeout=\"20000\"/>\n" +
            "            <async enabled=\"true\" batchSize=\"15\"/>\n" +
            "         </loader>\n" +
            "      </loaders>      ";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureCacheLoaders(e, c);

      CacheLoaderManagerConfig clc = c.getCacheLoaderManagerConfig();
      assert clc != null;
      assert clc.isFetchPersistentState();
      assert clc.isPassivation();
      assert clc.isShared();
      assert clc.isPreload();

      CacheStoreConfig iclc = (CacheStoreConfig) clc.getFirstCacheLoaderConfig();
      assert iclc.getCacheLoaderClassName().equals(JdbcStringBasedCacheStore.class.getName());
      assert iclc.getAsyncStoreConfig().isEnabled();
      assert iclc.getAsyncStoreConfig().getBatchSize() == 15;
      assert iclc.getAsyncStoreConfig().getPollWait() == 100;
      assert iclc.getAsyncStoreConfig().getQueueSize() == 10000;
      assert iclc.getAsyncStoreConfig().getThreadPoolSize() == 1;
      assert iclc.isFetchPersistentState();
      assert iclc.isIgnoreModifications();
      assert iclc.isPurgeOnStartup();

      assert clc.getCacheLoaderConfigs().size() == 1;
      JdbcStringBasedCacheStoreConfig csConf = (JdbcStringBasedCacheStoreConfig) clc.getFirstCacheLoaderConfig();
      assert csConf.getCacheLoaderClassName().equals("org.horizon.loader.jdbc.stringbased.JdbcStringBasedCacheStore");
      assert csConf.isFetchPersistentState();
      assert csConf.isIgnoreModifications();
      assert csConf.isPurgeOnStartup();
      TableManipulation tableManipulation = csConf.getTableManipulation();
      ConnectionFactoryConfig cfc = csConf.getConnectionFactoryConfig();
      assert cfc.getConnectionFactoryClass().equals(PooledConnectionFactory.class.getName());
      assert cfc.getConnectionUrl().equals("jdbc://some-url");
      assert cfc.getUserName().equals("root");
      assert cfc.getDriverClass().equals("org.dbms.Driver");
      assert tableManipulation.getIdColumnType().equals("VARCHAR2(256)");
      assert tableManipulation.getDataColumnType().equals("BLOB");
      assert tableManipulation.isDropTableOnExit();
      assert !tableManipulation.isCreateTableOnStart();


      SingletonStoreConfig ssc = iclc.getSingletonStoreConfig();
      assert ssc.isSingletonStoreEnabled();
      assert ssc.isPushStateWhenCoordinator();
      assert ssc.getPushStateTimeout() == 20000;
   }

   public void testCacheLoadersDefaults() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<loaders>\n" +
            "         <loader class=\"org.horizon.loader.jdbc.binary.JdbcBinaryCacheStore\">\n" +
            "            <properties />\n" +
            "         </loader>\n" +
            "      </loaders>";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();
      parser.configureCacheLoaders(e, c);

      CacheLoaderManagerConfig clc = c.getCacheLoaderManagerConfig();
      assert clc != null;
      assert !clc.isFetchPersistentState();
      assert !clc.isPassivation();
      assert !clc.isShared();
      assert !clc.isPreload();

      CacheStoreConfig iclc = (CacheStoreConfig) clc.getFirstCacheLoaderConfig();
      assert iclc.getCacheLoaderClassName().equals("org.horizon.loader.jdbc.binary.JdbcBinaryCacheStore");
      assert !iclc.getAsyncStoreConfig().isEnabled();
      assert !iclc.isFetchPersistentState();
      assert !iclc.isIgnoreModifications();
      assert !iclc.isPurgeOnStartup();

      SingletonStoreConfig ssc = iclc.getSingletonStoreConfig();
      assert !ssc.isSingletonStoreEnabled();
   }

   public void testDefaultEviction() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String xml = "<eviction />";
      Element e = XmlConfigHelper.stringToElement(xml);

      Configuration c = new Configuration();

      parser.configureEviction(e, c);
      parser.configureExpiration(null, c);

      assert c.getEvictionMaxEntries() == -1;
      assert c.getEvictionStrategy() == EvictionStrategy.NONE;
      assert c.getEvictionWakeupInterval() == 5000;
      assert c.getExpirationLifespan() == -1;
      assert c.getExpirationMaxIdle() == -1;
   }

   public void testEviction() throws Exception {
      XmlConfigurationParserImpl parser = new XmlConfigurationParserImpl();
      String evictionXml = "<eviction strategy=\"LRU\" " +
            "wakeUpInterval=\"750\" maxEntries=\"7000\" />";
      String expirationXml = "<expiration lifespan=\"2000\" maxIdle=\"500\"/>";

      Element evictionElement = XmlConfigHelper.stringToElement(evictionXml);
      Element expirationElement = XmlConfigHelper.stringToElement(expirationXml);

      Configuration c = new Configuration();

      parser.configureEviction(evictionElement, c);
      parser.configureExpiration(expirationElement, c);

      assert c.getEvictionStrategy() == EvictionStrategy.LRU;
      assert c.getEvictionMaxEntries() == 7000;
      assert c.getEvictionWakeupInterval() == 750;
      assert c.getExpirationLifespan() == 2000;
      assert c.getExpirationMaxIdle() == 500;
   }
}
