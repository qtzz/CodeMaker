package com.xiaohansong.codemaker;

import com.intellij.openapi.util.text.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hansong.xhs
 * @version $Id: CodeTemplate.java, v 0.1 2017-01-28 9:41 hansong.xhs Exp $$
 */
@Data
@AllArgsConstructor
public class CodeTemplate {

    public static final String DEFAULT_ENCODING = "UTF-8";

    public CodeTemplate() {
    }

    public CodeTemplate(String name, String classNameVm, String codeTemplate, int classNumber, String fileEncoding, TemplateLanguage templateLanguage, String targetLanguage) {
        this.name = name;
        this.classNameVm = classNameVm;
        this.codeTemplate = codeTemplate;
        this.classNumber = classNumber;
        this.fileEncoding = fileEncoding;
        this.templateLanguage = templateLanguage;
        this.targetLanguage = targetLanguage;
    }

    public CodeTemplate(String name, String classNameVm, String codeTemplate, String assignRelationPath) {
        this.name = name;
        this.classNameVm = classNameVm;
        this.codeTemplate = codeTemplate;
        this.assignRelationPath = assignRelationPath;
    }

    public CodeTemplate(String name, String classNameVm, String codeTemplate, String targetLanguage, String assignRelationPath) {
        this.name = name;
        this.classNameVm = classNameVm;
        this.codeTemplate = codeTemplate;
        this.targetLanguage = targetLanguage;
        this.assignRelationPath = assignRelationPath;
    }

    /**
     * template name
     */
    private String name;

    /**
     * the generated class name, support velocity
     */
    private String classNameVm;

    /**
     * code template in velocity
     */
    private String codeTemplate;

    /**
     * the number of template context class
     */
    private int classNumber = 1;

    /**
     * the encoding of the generated file
     */
    private String fileEncoding = CodeTemplate.DEFAULT_ENCODING;

    private TemplateLanguage templateLanguage = TemplateLanguage.vm;

    private String targetLanguage = "java";

    /**
     * assigned path if null ==currentClass path
     */
    private String assignRelationPath;

    public TemplateLanguage getTemplateLanguage() {
        return templateLanguage == null ? TemplateLanguage.vm : templateLanguage;
    }

    public void setTemplateLanguage(TemplateLanguage templateLanguage) {
        if (templateLanguage == null) {
            templateLanguage = TemplateLanguage.vm;
        }
        this.templateLanguage = templateLanguage;
    }

    public boolean isValid() {
        return StringUtil.isNotEmpty(getClassNameVm()) && StringUtil.isNotEmpty(getName())
                && StringUtil.isNotEmpty(getCodeTemplate()) && classNumber != -1 && StringUtil.isNotEmpty(getFileEncoding());
    }

    public static CodeTemplate empty(String title) {
        return new CodeTemplate(title, "", "", 1, DEFAULT_ENCODING, TemplateLanguage.vm, "java");
    }

    public String getTargetLanguage() {
        return targetLanguage == null ? "java" : targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage == null ? "java" : targetLanguage;
    }
}
