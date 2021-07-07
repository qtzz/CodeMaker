package com.xiaohansong.codemaker.templates;

import com.xiaohansong.codemaker.CodeTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneratedSource {
    public final String className;

    public final String content;

    private CodeTemplate codeTemplate;

    public GeneratedSource(String className, String content) {
        this.className = className;
        this.content = content;
    }
}
