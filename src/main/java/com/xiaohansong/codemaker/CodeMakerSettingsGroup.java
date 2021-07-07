package com.xiaohansong.codemaker;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Setter;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hansong.xhs
 * @version $Id: CodeMakerSettings.java, v 0.1 2017-01-28 9:30 hansong.xhs Exp $$
 */
@State(name = "CodeMakerSettingsGroup", storages = {@Storage("$APP_CONFIG$/CodeMaker-setting-group.xml")})
public class CodeMakerSettingsGroup implements PersistentStateComponent<CodeMakerSettingsGroup> {

    /**
     * The constant LOGGER.
     */
    private static final Logger LOGGER = Logger.getInstance(CodeMakerSettingsGroup.class);

    public CodeMakerSettingsGroup() {
    }

    private void loadDefaultSettingsGroup() {
        try {
            Map<String, List<CodeTemplate>> codeTemplateGroups = new LinkedHashMap<>();

            //repository crud ext batch insert update insertOrUpdate
            List<CodeTemplate> codeTemplates = Lists.newArrayList();
            codeTemplates.add(createCodeTemplate("repository", "${class0.className}Repository",
                    "Repository.vm",
                    "/Users/qintao/IdeaProjects/ktt-gameplay/ktt-gameplay-repository/src/main/java/com/pdd/api/ktt/gameplay/repository/mybatis/repository"));


            codeTemplates.add(createCodeTemplate("repositoryImpl", "${class0.className}RepositoryImpl",
                    "RepositoryImpl.vm",
                    "/Users/qintao/IdeaProjects/ktt-gameplay/ktt-gameplay-repository/src/main/java/com/pdd/api/ktt/gameplay/repository/mybatis/repository/impl"));

            codeTemplates.add(createCodeTemplate("mapperExt", "${class0.className}MapperExt",
                    "MapperExt.vm",
                    "/Users/qintao/IdeaProjects/ktt-gameplay/ktt-gameplay-repository/src/main/java/com/pdd/api/ktt/gameplay/repository/mybatis/mapper/primary/ext"));


            codeTemplates.add(createCodeTemplate("mapperExtXml", "${class0.className}MapperExt",
                    "MapperExtXml.vm", "xml",
                    "/Users/qintao/IdeaProjects/ktt-gameplay/ktt-gameplay-repository/src/main/resources/base/com/pdd/api/ktt/gameplay/repository/mybatis/mapper/primary/ext"));
            codeTemplateGroups.put("repository", codeTemplates);
            this.codeTemplateGroups = codeTemplateGroups;
        } catch (Exception e) {
            LOGGER.error("loadDefaultSettings failed", e);
        }
    }

    // @NotNull
    private CodeTemplate createCodeTemplate(String name, String classNameVm, String sourceTemplateName,
                                            String targetLanguage,
                                            String path) throws IOException {
        String velocityTemplate = FileUtil.loadTextAndClose(CodeMakerSettingsGroup.class.getResourceAsStream("/template/" + sourceTemplateName));
        return new CodeTemplate(name, classNameVm, velocityTemplate, targetLanguage, path);
    }

    //@NotNull
    private CodeTemplate createCodeTemplate(String name, String classNameVm, String sourceTemplateName,
                                            String path) throws IOException {
        String velocityTemplate = FileUtil.loadTextAndClose(CodeMakerSettingsGroup.class.getResourceAsStream("/template/" + sourceTemplateName));
        return new CodeTemplate(name, classNameVm, velocityTemplate, path);
    }

    /**
     * Getter method for property <tt>codeTemplates</tt>.
     *
     * @return property value of codeTemplates
     */
    public Map<String, List<CodeTemplate>> getCodeTemplateGroups() {
        if (codeTemplateGroups == null) {
            loadDefaultSettingsGroup();
        }
        return codeTemplateGroups;
    }

    @Setter
    private Map<String, List<CodeTemplate>> codeTemplateGroups;

    @Nullable
    @Override
    public CodeMakerSettingsGroup getState() {
        if (this.codeTemplateGroups == null) {
            loadDefaultSettingsGroup();
        }
        return this;
    }

    @Override
    public void loadState(@NotNull CodeMakerSettingsGroup codeMakerSettings) {
        XmlSerializerUtil.copyBean(codeMakerSettings, this);
    }

    public List<CodeTemplate> getCodeTemplateGroup(String template) {
        return codeTemplateGroups.get(template);
    }

    public void removeCodeTemplateGroup(String template) {
        codeTemplateGroups.remove(template);
    }

    public void addTemplate(String key, List<CodeTemplate> templateGroup) {
        codeTemplateGroups.put(key, templateGroup);
    }
}
