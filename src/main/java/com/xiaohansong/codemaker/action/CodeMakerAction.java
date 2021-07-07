package com.xiaohansong.codemaker.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.PackageWrapper;
import com.xiaohansong.codemaker.ClassEntry;
import com.xiaohansong.codemaker.CodeMakerSettings;
import com.xiaohansong.codemaker.CodeTemplate;
import com.xiaohansong.codemaker.CreateFileAction;
import com.xiaohansong.codemaker.templates.GeneratedSource;
import com.xiaohansong.codemaker.templates.PolyglotTemplateEngine;
import com.xiaohansong.codemaker.templates.TemplateEngine;
import com.xiaohansong.codemaker.ui.Editors;
import com.xiaohansong.codemaker.util.CodeMakerUtil;
import org.apache.commons.lang.StringUtils;
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
public class CodeMakerAction extends AnAction implements DumbAware {

    private static final Logger log = Logger.getInstance(CodeMakerAction.class);

    private CodeMakerSettings settings;

    private String templateKey;

    private TemplateEngine templateEngine = new PolyglotTemplateEngine();

    CodeMakerAction(String templateKey) {
        this.settings = ServiceManager.getService(CodeMakerSettings.class);
        this.templateKey = templateKey;
        getTemplatePresentation().setDescription("description");
        getTemplatePresentation().setText(templateKey, false);
    }

    /**
     * @see com.intellij.openapi.actionSystem.AnAction#actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent)
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
        CodeTemplate codeTemplate = settings.getCodeTemplate(templateKey);

        PsiElement psiElement = anActionEvent.getData(LangDataKeys.PSI_ELEMENT);
        if (!(psiElement instanceof PsiClass)) {
            Messages.showMessageDialog(project, "Please focus on a class", "Generate Failed", null);
            return;
        }
        log.info("current pisElement: " + psiElement.getClass().getName() + "(" + psiElement + ")");

        PsiClass psiClass = (PsiClass) psiElement;
        String language = psiElement.getLanguage().getID().toLowerCase();
        List<ClassEntry> selectClasses = getClasses(project, psiClass);

        if (selectClasses.size() < 1) {
            Messages.showMessageDialog(project, "No Classes found", "Generate Failed", null);
            return;
        }

        try {
            ClassEntry currentClass = selectClasses.get(0);
            GeneratedSource generated = generateSource(codeTemplate, selectClasses, currentClass);


            VirtualFile sourceRoot = findSourceRoot(currentClass, project, psiElement);
            final String sourcePath = sourceRoot.getPath();
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

            if (showSource(project, codeTemplate.getTargetLanguage(), generated.className, content)) {
                saveToFile(anActionEvent, language, generated.className, content, null,
                        sourceRoot, codeTemplate.getFileEncoding());
            }
            // DestinationChooser.Destination destination = chooseDestination(currentClass, project, psiElement);
            // if (destination instanceof DestinationChooser.FileDestination) {
            //  saveToFile(anActionEvent, language, generated.className, generated.content, currentClass,
            //    (DestinationChooser.FileDestination) destination, codeTemplate.getFileEncoding());
            // } else if (destination == DestinationChooser.ShowSourceDestination) {
            //     showSource(project, codeTemplate.getTargetLanguage(), generated.className, generated.content);
            // }

        } catch (Exception e) {
            Messages.showMessageDialog(project, e.getMessage(), "Generate Failed", null);
        }
    }

    @NotNull
    private GeneratedSource generateSource(CodeTemplate codeTemplate, List<ClassEntry> selectClasses, ClassEntry currentClass) {
        return templateEngine.evaluate(codeTemplate, selectClasses, currentClass);
    }

    public void saveToFile(AnActionEvent anActionEvent, String language, String className, String content,
                           String targetPath, VirtualFile file, String encoding) {
        if (StringUtils.isBlank(targetPath)) {
            final String sourcePath = file.getPath();

            targetPath = CodeMakerUtil.generateClassPath(sourcePath, className, language);
        }
        // async write action
        ApplicationManager.getApplication().runWriteAction(
                new CreateFileAction(targetPath, content, encoding, anActionEvent
                        .getDataContext()));

    }

    public String mixOriginAndNew(String origin, String newStr) {
        if (StringUtils.isBlank(origin)) {
            return newStr;
        }
        int start = newStr.indexOf("{");
        int end = newStr.lastIndexOf("}");
        int originEnd = origin.lastIndexOf("}");
        return origin.substring(0, originEnd) + newStr.substring(start + 1, end + 1);
    }


    private boolean showSource(Project project, String language, String className, String content) {
        final Editor editor = Editors.createSourceEditor(project, language, content, true);
        try {
            final DialogBuilder builder = new DialogBuilder(project);
            builder.addCloseButton().setText("Save");
            builder.addCancelAction().setText("Cancel");
            builder.setCenterPanel(editor.getComponent());
            builder.setTitle(className);
            return builder.showAndGet();
        } finally {
            Editors.release(editor);
        }
    }

    /**
     * allow user to select the generated code source root
     */
    private DestinationChooser.Destination chooseDestination(ClassEntry classEntry, Project project, PsiElement psiElement) {
        String packageName = classEntry.getPackageName();
        final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(project), packageName);
        List<VirtualFile> suitableRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(project);
        return DestinationChooser.chooseDestination(targetPackage, suitableRoots,
                psiElement.getContainingFile().getContainingDirectory());
    }

    private boolean userConfirmedOverride() {
        return Messages.showYesNoDialog("Overwrite?", "File Exists", null) == Messages.YES;
    }


    @NotNull
    public List<ClassEntry> getClasses(Project project, PsiClass defaultPsiClass) {
        List<PsiClass> psiClasses = CodeMakerUtil.chooseClass(project, defaultPsiClass);
        return psiClasses.stream().map(psiClass -> ClassEntry.create(psiClass)).collect(Collectors.toList());
    }

    public VirtualFile findSourceRoot(ClassEntry classEntry, Project project, PsiElement psiElement) {
        String packageName = classEntry.getPackageName();
        final PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(project), packageName);
        List<VirtualFile> suitableRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(project);
        if (suitableRoots.size() > 1) {
            // return MoveClassesOrPackagesUtil.chooseSourceRoot(targetPackage, suitableRoots,
            //         psiElement.getContainingFile().getContainingDirectory());
            return psiElement.getContainingFile().getVirtualFile().getParent();
        } else if (suitableRoots.size() == 1) {
            return suitableRoots.get(0);
        }
        return null;
    }
}
