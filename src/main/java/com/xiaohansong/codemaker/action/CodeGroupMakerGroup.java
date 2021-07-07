package com.xiaohansong.codemaker.action;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.xiaohansong.codemaker.CodeMakerSettingsGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hansong.xhs
 * @version $Id: CodeMakerGroup.java, v 0.1 2017-01-28 9:25 hansong.xhs Exp $$
 */
public class CodeGroupMakerGroup extends ActionGroup implements DumbAware {


    private CodeMakerSettingsGroup codeMakerSettingsGroup;

    public CodeGroupMakerGroup() {
        codeMakerSettingsGroup = ServiceManager.getService(CodeMakerSettingsGroup.class);
    }

    /**
     * @see ActionGroup#getChildren(AnActionEvent)
     */
    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        if (anActionEvent == null) {
            return AnAction.EMPTY_ARRAY;
        }
        Project project = PlatformDataKeys.PROJECT.getData(anActionEvent.getDataContext());
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        if (codeMakerSettingsGroup == null) {
            codeMakerSettingsGroup = new CodeMakerSettingsGroup();
        }
        final List<AnAction> children = new ArrayList<>();
        this.codeMakerSettingsGroup.getCodeTemplateGroups().forEach((k, v) -> children.add(getOrCreateActionV2(k)));
        return children.toArray(new AnAction[children.size()]);
    }


    private AnAction getOrCreateActionV2(String key) {
        final String actionId = "CodeGroupMaker.Menu.Action." + key;
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action == null) {
            action = new CodeMakerActionGroup(key);
            ActionManager.getInstance().registerAction(actionId, action);
        }
        return action;
    }
}
