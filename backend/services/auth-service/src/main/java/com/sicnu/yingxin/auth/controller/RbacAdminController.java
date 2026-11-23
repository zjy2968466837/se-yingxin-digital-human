package com.sicnu.yingxin.auth.controller;

import com.sicnu.yingxin.auth.dto.ApproveRequest;
import com.sicnu.yingxin.auth.dto.GroupRequest;
import com.sicnu.yingxin.auth.dto.PrivRequest;
import com.sicnu.yingxin.auth.dto.UserGroupRequest;
import com.sicnu.yingxin.auth.service.RbacService;
import com.sicnu.yingxin.auth.vo.AuditVO;
import com.sicnu.yingxin.auth.vo.GroupAdminVO;
import com.sicnu.yingxin.auth.vo.MatrixVO;
import com.sicnu.yingxin.auth.vo.SessionVO;
import com.sicnu.yingxin.auth.vo.UserAdminVO;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限中心管理接口（禅道式 RBAC，业务阶段落地）。
 *
 * <p>每个接口对应一个权限点（module:priv），后台超级账号（role=ADMIN）直通，
 * 其余用户按 auth_group_priv 判定——与禅道 grouppriv 语义一致。
 */
@Tag(name = "权限中心", description = "注册审批、用户组、权限矩阵、审计日志")
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class RbacAdminController {

    private final RbacService rbacService;

    /* ---------- 注册审批（user:approve） ---------- */

    @Operation(summary = "待审批列表")
    @GetMapping("/pending")
    public Result<List<UserAdminVO>> pending() {
        requirePerm("user:approve");
        return Result.ok(rbacService.pendingUsers());
    }

    @Operation(summary = "审批注册申请")
    @PostMapping("/approve")
    public Result<Void> approve(@Valid @RequestBody ApproveRequest request) {
        requirePerm("user:approve");
        rbacService.approve(request.getUserId(), Boolean.TRUE.equals(request.getApprove()), operator());
        return Result.ok();
    }

    /* ---------- 用户与用户组（user:list / user:role） ---------- */

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<List<UserAdminVO>> users(@RequestParam(required = false) String keyword) {
        requirePerm("user:list");
        return Result.ok(rbacService.listUsers(keyword));
    }

    @Operation(summary = "变更用户组")
    @PostMapping("/user-group")
    public Result<Void> setUserGroup(@Valid @RequestBody UserGroupRequest request) {
        requirePerm("user:role");
        rbacService.setUserGroups(request.getUserId(), request.getGroupCodes(), operator());
        return Result.ok();
    }

    /* ---------- 组管理（user:role） ---------- */

    @Operation(summary = "用户组列表")
    @GetMapping("/groups")
    public Result<List<GroupAdminVO>> groups() {
        requirePerm("user:list");
        return Result.ok(rbacService.listGroups());
    }

    @Operation(summary = "新建用户组")
    @PostMapping("/group")
    public Result<Void> createGroup(@Valid @RequestBody GroupRequest request) {
        requirePerm("user:role");
        rbacService.createGroup(request, operator());
        return Result.ok();
    }

    @Operation(summary = "删除用户组")
    @DeleteMapping("/group")
    public Result<Void> deleteGroup(@RequestParam Long id) {
        requirePerm("user:role");
        rbacService.deleteGroup(id, operator());
        return Result.ok();
    }

    /* ---------- 权限矩阵（perm:matrix / perm:grant） ---------- */

    @Operation(summary = "权限矩阵")
    @GetMapping("/matrix")
    public Result<MatrixVO> matrix() {
        requirePerm("perm:matrix");
        return Result.ok(rbacService.matrix());
    }

    @Operation(summary = "调整角色权限")
    @PostMapping("/priv")
    public Result<Void> priv(@Valid @RequestBody PrivRequest request) {
        requirePerm("perm:grant");
        rbacService.grant(request, operator());
        return Result.ok();
    }

    /* ---------- 审计与会话（audit:view） ---------- */

    @Operation(summary = "审计日志分页")
    @GetMapping("/audit")
    public Result<PageResult<AuditVO>> audit(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        requirePerm("audit:view");
        return Result.ok(rbacService.auditPage(page, size));
    }

    @Operation(summary = "在线会话列表")
    @GetMapping("/sessions")
    public Result<List<SessionVO>> sessions() {
        requirePerm("audit:view");
        return Result.ok(rbacService.listSessions());
    }

    /* ---------- 私有辅助 ---------- */

    /** 权限判定：超级账号直通；普通用户走组权限（依据网关注入的用户 ID，防伪造头）。 */
    private void requirePerm(String methodCode) {
        var ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        boolean isAdminAccount = Roles.ADMIN.equals(ctx.getRole());
        if (!isAdminAccount && !rbacService.hasPerm(ctx.getUserId(), methodCode)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "无权限执行该操作，需要权限点 " + methodCode);
        }
    }

    private String operator() {
        var ctx = UserContextHolder.get();
        return ctx == null || ctx.getUserId() == null ? "unknown" : String.valueOf(ctx.getUserId());
    }
}
