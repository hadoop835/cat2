package org.unidal.cat.spi.analysis.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.unidal.cat.spi.Report;
import org.unidal.cat.spi.ReportConfiguration;
import org.unidal.cat.spi.ReportManager;
import org.unidal.cat.spi.ReportManagerManager;
import org.unidal.cat.spi.analysis.MessageAnalyzer;
import org.unidal.cat.spi.analysis.MessageRoutingStrategy;
import org.unidal.helper.Threads;
import org.unidal.lookup.ContainerHolder;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.extension.RoleHintEnabled;

import com.dianping.cat.message.spi.MessageTree;

public abstract class AbstractPipeline extends ContainerHolder implements Pipeline, RoleHintEnabled, LogEnabled {
   @Inject(StrategyConstants.DOMAIN_HASH)
   private MessageRoutingStrategy m_strategy;

   @Inject
   private ReportManagerManager m_rmm;

   @Inject
   private ReportConfiguration m_config;

   private String m_name;

   private int m_hour;

   private List<MessageAnalyzer> m_analyzers = new ArrayList<MessageAnalyzer>();

   private Logger m_logger;

   protected void afterCheckpoint() throws Exception {
      // to be overridden
   }

   @Override
   public boolean analyze(MessageTree tree) {
      if (!m_analyzers.isEmpty()) {
         MessageRoutingStrategy strategy = getRoutingStrategy();
         int index = strategy.getIndex(tree, m_analyzers.size());
         MessageAnalyzer analyzer = m_analyzers.get(index);

         return analyzer.handle(tree);
      } else {
         return false;
      }
   }

   protected void beforeCheckpoint() throws Exception {
      // to be overridden
   }

   @Override
   public void checkpoint(boolean atEnd) throws Exception {
      beforeCheckpoint();
      doCheckpoint(atEnd);
      afterCheckpoint();
   }

   @Override
   public void destroy() {
      for (MessageAnalyzer messageAnalyzer : m_analyzers) {
         super.release(messageAnalyzer);
         messageAnalyzer.destroy();
      }
   }

   protected ReportManager<Report> getReportManager() {
      return m_rmm.getReportManager(getName());
   }

   protected void doCheckpoint(final boolean atEnd) throws Exception {
      if (hasAnalyzer()) {
         for (MessageAnalyzer analyzer : m_analyzers) {
            analyzer.doCheckpoint(atEnd);
         }
      } else {
         getReportManager().doCheckpoint(getHour(), 0);
      }
   }

   @Override
   public void enableLogging(Logger logger) {
      m_logger = logger;
   }

   @Override
   public void enableRoleHint(String roleHint) {
      m_name = roleHint;
   }

   protected int getHour() {
      return m_hour;
   }

   @Override
   public String getName() {
      return m_name;
   }

   protected MessageRoutingStrategy getRoutingStrategy() {
      return m_strategy;
   }

   protected boolean hasAnalyzer() {
      return true;
   }

   @Override
   public void initialize(int hour) {
      m_hour = hour;

      if (hasAnalyzer()) {
         int size = m_config.getAnanlyzerCount(m_name);

         for (int i = 0; i < size; i++) {
            MessageAnalyzer analyzer = lookup(MessageAnalyzer.class, m_name);

            try {
               analyzer.initialize(i, hour);
               m_analyzers.add(analyzer);
               Threads.forGroup("Cat").start(analyzer);
            } catch (Throwable e) {
               String msg = String.format("Error when initializing analyzer %s!", analyzer);

               m_logger.error(msg, e);
            }
         }
      }
   }
}
