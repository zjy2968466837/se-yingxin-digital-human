package com.sicnu.yingxin.auth.vo;

import lombok.Data;

import java.util.List;

/** 权限矩阵（组 × 权限点）。 */
@Data
public class MatrixVO {

    private List<ModuleNode> modules;

    private List<GroupNode> groups;

    /** 已授权集合，元素格式「组编码|module:priv」 */
    private List<String> grants;

    @Data
    public static class ModuleNode {

        private String code;

        private String name;

        private List<MethodNode> methods;
    }

    @Data
    public static class MethodNode {

        private String code;

        private String name;
    }

    @Data
    public static class GroupNode {

        private String code;

        private String name;

        private Boolean builtin;
    }
}
