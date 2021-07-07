package com.xiaohansong.codemaker.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.xiaohansong.codemaker.ClassEntry;
import com.xiaohansong.codemaker.CodeMakerSettingsGroup;
import com.xiaohansong.codemaker.CodeTemplate;
import com.xiaohansong.codemaker.templates.GeneratedSource;
import com.xiaohansong.codemaker.templates.PolyglotTemplateEngine;
import com.xiaohansong.codemaker.templates.TemplateEngine;
import com.xiaohansong.codemaker.util.CodeMakerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author hansong.xhs
 * @version $Id: CodeMakerAction.java, v 0.1 2017-01-28 9:23 hansong.xhs Exp $$
 */
public class CodeMakerActionGroup extends CodeMakerAction implements DumbAware {

    private static final Logger log = Logger.getInstance(CodeMakerActionGroup.class);

    private CodeMakerSettingsGroup settingsGroup;

    private String templateGroupKey;

    private TemplateEngine templateEngine = new PolyglotTemplateEngine();

    CodeMakerActionGroup(String templateKey) {
        super(templateKey);
        this.settingsGroup = ServiceManager.getService(CodeMakerSettingsGroup.class);
        this.templateGroupKey = templateKey;
        getTemplatePresentation().setDescription("description");
        getTemplatePresentation().setText(templateKey, false);
    }

    /**
     * @see AnAction#actionPerformed(AnActionEvent)
     */
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
            dumbService.showDumbModeNotification("CodeMaker plugin is not available during indexing");
            return;
        }
        if (settingsGroup == null) {
            settingsGroup = new CodeMakerSettingsGroup();
            settingsGroup.getCodeTemplateGroups();
        }
        List<CodeTemplate> codeTemplateGroup = settingsGroup.getCodeTemplateGroup(templateGroupKey);

        PsiElement psiElement = anActionEvent.getData(LangDataKeys.PSI_ELEMENT);
        if (!(psiElement instanceof PsiClass)) {
            Messages.showMessageDialog(project, "Please focus on a class", "Generate Failed", null);
            return;
        }
        log.info("current pisElement: " + psiElement.getClass().getName() + "(" + psiElement + ")");

        PsiClass psiClass = (PsiClass) psiElement;

        ClassEntry currentClass = ClassEntry.create(psiClass);
        List<GeneratedSource> generatedSources = generateSource(codeTemplateGroup, currentClass);

        generatedSources.forEach(generated -> {
            VirtualFile sourceRoot = findSourceRoot(currentClass, project, psiElement);
            String sourcePath = sourceRoot.getPath();
            CodeTemplate codeTemplate = generated.getCodeTemplate();
            String language = codeTemplate.getTargetLanguage();
            String assignRelationPath = codeTemplate.getAssignRelationPath();
            if (StringUtils.isNotBlank(assignRelationPath)) {
                sourcePath = assignRelationPath;
            }
            final String targetPath = CodeMakerUtil.generateClassPath(sourcePath, generated.className, language);

            VirtualFileManager manager = VirtualFileManager.getInstance();
            VirtualFile virtualFile = manager
                    .refreshAndFindFileByUrl(VfsUtil.pathToUrl(targetPath));
            //若当前文件已存在则在文末添加新生成的class内容
            StringBuilder originContentBuilder = new StringBuilder();
            if (Objects.nonNull(virtualFile) && virtualFile.exists()) {
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(virtualFile.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        originContentBuilder.append(line);
                        originContentBuilder.append("\n");
                        line = bufferedReader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String content = mixOriginAndNew(originContentBuilder.toString(), generated.content);

            //  if (showSource(project, codeTemplate.getTargetLanguage(), generated.className, content)) {
            saveToFile(anActionEvent, language, generated.className, content, targetPath,
                    sourceRoot, codeTemplate.getFileEncoding());
            // }


        });
    }

    @NotNull
    private List<GeneratedSource> generateSource(List<CodeTemplate> codeTemplates, ClassEntry currentClass) {

        return codeTemplates.stream().map(e -> templateEngine.evaluate(e, null, currentClass))
                .collect(Collectors.toList());
    }


}
