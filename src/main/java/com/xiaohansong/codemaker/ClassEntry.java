package com.xiaohansong.codemaker;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.xiaohansong.codemaker.util.CodeMakerUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author hansong.xhs
 * @version $Id: ClassEntry.java, v 0.1 2017-01-22 9:53 hansong.xhs Exp $$
 */
@Data
@AllArgsConstructor
public class ClassEntry {

    private String className;

    private String packageName;

    private List<String> importList;

    private List<Field> fields;

    private List<Field> allFields;

    private List<Method> methods;

    private List<Method> allMethods;

    private List<String> typeParams = Collections.emptyList();

    private List<Annotation> classAnnotationList;

    public ClassEntry(String className, String packageName, List<String> importList, List<Field> fields, List<Field> allFields, List<Method> methods, List<Method> allMethods, List<String> typeParams) {
        this.className = className;
        this.packageName = packageName;
        this.importList = importList;
        this.fields = fields;
        this.allFields = allFields;
        this.methods = methods;
        this.allMethods = allMethods;
        this.typeParams = typeParams;
    }

    @Data
    @AllArgsConstructor
    public static class Method {
        /**
         * method name
         */
        private String name;

        /**
         * the method modifier, like "private",or "@Setter private" if include annotations
         */
        private String modifier;

        /**
         * the method returnType
         */
        private String returnType;

        /**
         * the method params, like "(String name)"
         */
        private String params;

        /**
         * the method params, like "String"
         */
        private List<String> paramsTypes;

        /**
         * the method annotation
         */
        private List<Annotation> annotationList;

        public Method(String name, String modifier, String returnType, String params) {
            this.name = name;
            this.modifier = modifier;
            this.returnType = returnType;
            this.params = params;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Annotation {

        private String name;

        private Map<String, Object> annotationParams;
    }

    @Data
    @AllArgsConstructor
    public static class Field {
        /**
         * field type
         */
        private String type;

        /**
         * field name
         */
        private String name;

        /**
         * the field modifier, like "private",or "@Setter private" if include annotations
         */
        private String modifier;

        /**
         * field doc comment
         */
        private String comment;

    }

    private ClassEntry() {

    }

    public static ClassEntry create(PsiClass psiClass) {
        PsiFile psiFile = psiClass.getContainingFile();
        ClassEntry classEntry = new ClassEntry();
        classEntry.setClassName(psiClass.getName());
        classEntry.setPackageName(((PsiClassOwner) psiFile).getPackageName());
        if (psiFile instanceof PsiJavaFile) {
            classEntry.setFields(CodeMakerUtil.getFields(psiClass));
            classEntry.setImportList(CodeMakerUtil.getImportList((PsiJavaFile) psiFile));
            classEntry.setAllFields(CodeMakerUtil.getAllFields(psiClass));
        }
        // } else if (psiClass instanceof ScClass) {
        //   //  ScClass scalaClass = (ScClass) psiClass;
        //     //classEntry.setFields(CodeMakerUtil.getScalaClassFields(scalaClass));
        //     //classEntry.setImportList(CodeMakerUtil.getScalaImportList((ScalaFile) psiFile));
        // }


        classEntry.setMethods(CodeMakerUtil.getMethods(psiClass));
        classEntry.setAllMethods(CodeMakerUtil.getAllMethods(psiClass));
        classEntry.setTypeParams(CodeMakerUtil.getClassTypeParameters(psiClass));
        return classEntry;
    }

}
