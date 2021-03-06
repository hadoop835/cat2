package org.unidal.cat.core.config.page.update;

import org.unidal.cat.core.config.page.ConfigPage;
import org.unidal.web.mvc.ActionContext;
import org.unidal.web.mvc.ActionPayload;
import org.unidal.web.mvc.payload.annotation.FieldMeta;

public class Payload implements ActionPayload<ConfigPage, Action> {
   private ConfigPage m_page;

   @FieldMeta("op")
   private Action m_action;

   @FieldMeta("content")
   private String m_content;

   @FieldMeta("update")
   private boolean m_update;

   private String m_report;

   @Override
   public Action getAction() {
      return m_action;
   }

   public String getContent() {
      return m_content;
   }

   @Override
   public ConfigPage getPage() {
      return m_page;
   }

   public String getReport() {
      return m_report;
   }

   public boolean isUpdate() {
      return m_update;
   }

   public void setAction(String action) {
      m_action = Action.getByName(action, Action.VIEW);
   }

   @Override
   public void setPage(String page) {
      m_page = ConfigPage.getByName(page, ConfigPage.UPDATE);
   }

   @Override
   public void validate(ActionContext<?> ctx) {
      if (m_action == null) {
         m_action = Action.VIEW;
      }

      m_report = ctx.getRequestContext().getAction();

      if (m_report != null && m_report.length() == 0) {
         m_report = null;
      }
   }
}
