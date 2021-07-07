package com.xiaohansong.codemaker.templates;

import com.google.common.base.CaseFormat;
import com.xiaohansong.codemaker.ClassEntry;
import com.xiaohansong.codemaker.CodeTemplate;
import com.xiaohansong.codemaker.TemplateLanguage;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
abstract public class BaseTemplateEngine implements TemplateEngine {

    abstract protected TemplateLanguage supportedLanguage();

    @Override
    public GeneratedSource evaluate(CodeTemplate template, List<ClassEntry> selectClasses, ClassEntry currentClass) {
        if (template.getTemplateLanguage() != supportedLanguage())
            throw new IllegalArgumentException("unsupported language: " + template.getTemplateLanguage());
        final Environment environment = createEnvironment(template, selectClasses, currentClass);
        final String source = doEvaluate(template, environment);
        return new GeneratedSource(environment.className, source, template);
    }

    abstract protected String doEvaluate(CodeTemplate template, Environment environment);

    protected Environment createEnvironment(CodeTemplate template, List<ClassEntry> selectClasses, ClassEntry currentClass) {
        Map<String, Object> map = new HashMap<>();
        if (CollectionUtils.isNotEmpty(selectClasses)) {
            for (int i = 0; i < selectClasses.size(); i++) {
                map.put("class" + i, selectClasses.get(i));
            }
        } else {
            map.put("class" + 0, currentClass);
        }


        Date now = new Date();
        map.put("currentClassPath", currentClass.getPackageName());
        String assignRelationPath = template.getAssignRelationPath();
        if (StringUtils.isNotBlank(assignRelationPath)) {
            assignRelationPath = assignRelationPath.substring(assignRelationPath.indexOf("com"));
            assignRelationPath = assignRelationPath.replaceAll("\\/", "\\.");
            map.put("targetClassPath", assignRelationPath);
        }

        map.put("class", currentClass);
        map.put("classList", selectClasses);
        map.put("YEAR", DateFormatUtils.format(now, "yyyy"));
        map.put("TIME", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        map.put("USER", System.getProperty("user.name"));
        map.put("utils", new Utils());
        map.put("BR", "\n");
        map.put("QT", "\"");
        String className = generateClassNameAndHandleErrors(template, map);
        map.put(getClassNameKey(), className);
        return new Environment(className, map);
    }

    private String generateClassNameAndHandleErrors(CodeTemplate template, Map<String, Object> map) {
        String className;
        try {
            className = generateClassName(template.getClassNameVm(), map);
        } catch (Exception e) {
            final RuntimeException reported = new RuntimeException(String.format("Failed to generate class name:\n%s\n%s", e.getClass().getName(), e.getMessage()), e);
            reported.setStackTrace(e.getStackTrace());
            throw reported;
        }
        return className;
    }

    @NotNull
    protected String getClassNameKey() {
        return "ClassName";
    }

    abstract protected String generateClassName(String classNameTemplate, Map<String, Object> environment);

    @Data
    protected static class Environment {
        public final String className;

        public final Map<String, Object> bindings;
    }

    public static class Utils {


        public String camelCase(String prefix, String name) {
            if (name == null || name.isEmpty())
                return name;
            String identifier = scala.removeBackticks(name);
            return scala.removeBackticks(prefix) + identifier.substring(0, 1).toUpperCase() + identifier.substring(1);
        }

        public String camelToUrl(String camelCase) {
            if (StringUtils.isBlank(camelCase)) {
                return "";
            }
            String underScore = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camelCase.toUpperCase());
            return underScore.replaceAll("_", "/");
        }

        public String toSnake(String str) {
            if (StringUtils.isBlank(str)) {
                return "";
            }
            return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
        }

        @Getter
        private final ScalaUtils scala = new ScalaUtils();

        public static class ScalaUtils {
            /**
             * backticks are sometimes used in scala identifiers to escape reserved words like `type`, `object`, etc.
             */
            public String removeBackticks(String str) {
                if (str == null) return str;
                else return str.replace("`", "");
            }
        }
    }
}
