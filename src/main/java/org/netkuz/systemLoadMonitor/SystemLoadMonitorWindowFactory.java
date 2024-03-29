package org.netkuz.systemLoadMonitor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class SystemLoadMonitorWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        SystemLoadMonitorWindow systemLoadMonitorWindow = SystemLoadMonitorWindow.getInstance(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(systemLoadMonitorWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
