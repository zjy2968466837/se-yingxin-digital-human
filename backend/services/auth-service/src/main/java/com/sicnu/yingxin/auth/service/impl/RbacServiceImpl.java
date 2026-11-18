package com.sicnu.yingxin.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicnu.yingxin.auth.dto.GroupRequest;
import com.sicnu.yingxin.auth.dto.PrivRequest;
import com.sicnu.yingxin.auth.dto.RegisterRequest;
import com.sicnu.yingxin.auth.entity.AuthAuditLog;
import com.sicnu.yingxin.auth.entity.AuthGroup;
import com.sicnu.yingxin.auth.entity.AuthGroupPriv;
import com.sicnu.yingxin.auth.entity.AuthMethod;
import com.sicnu.yingxin.auth.entity.AuthModule;
import com.sicnu.yingxin.auth.entity.AuthUser;
import com.sicnu.yingxin.auth.entity.AuthUserGroup;
import com.sicnu.yingxin.auth.error.AuthErrorCode;
import com.sicnu.yingxin.auth.mapper.AuthAuditLogMapper;
import com.sicnu.yingxin.auth.mapper.AuthGroupMapper;
import com.sicnu.yingxin.auth.mapper.AuthGroupPrivMapper;
import com.sicnu.yingxin.auth.mapper.AuthMethodMapper;
import com.sicnu.yingxin.auth.mapper.AuthModuleMapper;
import com.sicnu.yingxin.auth.mapper.AuthUserGroupMapper;
import com.sicnu.yingxin.auth.mapper.AuthUserMapper;
import com.sicnu.yingxin.auth.mapper.RbacQueryMapper;
import com.sicnu.yingxin.auth.service.RbacService;
import com.sicnu.yingxin.auth.vo.AuditVO;
import com.sicnu.yingxin.auth.vo.GroupAdminVO;
import com.sicnu.yingxin.auth.vo.GroupVO;
import com.sicnu.yingxin.auth.vo.MatrixVO;
import com.sicnu.yingxin.auth.vo.SessionVO;
import com.sicnu.yingxin.auth.vo.TraceVO;
import com.sicnu.yingxin.auth.vo.UserAdminVO;
import com.sicnu.yingxin.common.core.constant.CacheKeys;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 禅道式 RBAC 实现。
 *
 * <p>与禅道鉴权保持同一套判定语义：超级组 admin 免判直通 → 组授权表四表联查 → 403 拒绝。
 * 权限矩阵的每次勾选都是对 auth_group_priv 一行的增删，下一次请求立即生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    /** 超级组编码：免判直通，对应禅道 admin 组 */
    public static final String SUPER_GROUP = "admin";

    /** 注册该组需要管理员审批后才能登录使用 */
    private static final Set<String> NEED_APPROVE = Set.of("college_admin");

    /** 登录失败锁定阈值与时长 */
    private static final int FAIL_LIMIT = 5;
    private static final long LOCK_SECONDS = 600;

    /** 受保护路由清单（/api/auth/authz/trace 展示用） */
    private static final Map<String, String> ROUTES = new LinkedHashMap<>();

    static {
        ROUTES.put("迎新首页", "home:view");
        ROUTES.put("查看报到清单", "checkin:view");
        ROUTES.put("报到打卡", "checkin:checkin");
        ROUTES.put("智能问答", "qa:ask");
        ROUTES.put("问答库管理", "qa:manage");
        ROUTES.put("用户列表", "user:list");
        ROUTES.put("注册审批", "user:approve");
        ROUTES.put("变更用户组", "user:role");
        ROUTES.put("查看权限矩阵", "perm:matrix");
        ROUTES.put("调整角色权限", "perm:grant");
        ROUTES.put("审计日志", "audit:view");
        ROUTES.put("统计看板", "stats:view");
    }

    private final RbacQueryMapper rbacQueryMapper;
    private final AuthUserMapper authUserMapper;
    private final AuthUserGroupMapper authUserGroupMapper;
    private final AuthGroupMapper authGroupMapper;
    private final AuthModuleMapper authModuleMapper;
    private final AuthMethodMapper authMethodMapper;
    private final AuthGroupPrivMapper authGroupPrivMapper;
    private final AuthAuditLogMapper authAuditLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /* ==================== 判定 ==================== */

    @Override
    public boolean inSuperGroup(Long userId) {
        return rbacQueryMapper.inSuperGroup(userId) > 0;
    }

    @Override
    public boolean hasPerm(Long userId, String methodCode) {
        if (userId == null) {
            return false;
        }
        if (inSuperGroup(userId)) {
            return true;
        }
        return rbacQueryMapper.countPerm(userId, methodCode) > 0;
    }

    @Override
    public List<String> permsOf(Long userId) {
        return rbacQueryMapper.selectPermCodes(userId);
    }

    @Override
    public List<GroupVO> groupsOf(Long userId) {
        return rbacQueryMapper.selectGroupCodes(userId).stream()
                .map(code -> {
                    AuthGroup group = authGroupMapper.selectByCode(code);
                    return new GroupVO(code, group == null ? code : group.getName());
                })
                .toList();
    }

    /* ==================== 注册与审批 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request, String operator) {
        if (SUPER_GROUP.equals(request.getGroupCode())) {
            throw new BizException(AuthErrorCode.SUPER_ADMIN_PROTECTED, "不允许注册超级管理员组");
        }
        AuthGroup group = authGroupMapper.selectByCode(request.getGroupCode());
        if (group == null) {
            throw new BizException(AuthErrorCode.GROUP_NOT_FOUND, "用户组不存在");
        }
        if (authUserMapper.selectByStudentNo(request.getAccount()) != null) {
            throw new BizException(AuthErrorCode.ACCOUNT_EXISTS, "账号已被注册");
        }

        AuthUser user = new AuthUser();
        user.setStudentNo(request.getAccount());
        user.setRealName(request.getRealName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        user.setApproveStatus(NEED_APPROVE.contains(request.getGroupCode()) ? 0 : 1);
        authUserMapper.insert(user);

        AuthUserGroup userGroup = new AuthUserGroup();
        userGroup.setUserId(user.getId());
        userGroup.setGroupId(group.getId());
        authUserGroupMapper.insert(userGroup);

        audit(operator, "register", request.getAccount(),
                "注册用户组=" + request.getGroupCode() + " 审批状态=" + user.getApproveStatus());
        log.info("用户注册成功 account={} group={} approveStatus={}",
                request.getAccount(), request.getGroupCode(), user.getApproveStatus());
        return user.getId();
    }

    @Override
    public List<UserAdminVO> pendingUsers() {
        return authUserMapper.selectList(new LambdaQueryWrapper<AuthUser>()
                        .eq(AuthUser::getApproveStatus, 0))
                .stream()
                .map(this::toUserAdminVO)
                .toList();
    }

    @Override
    public List<UserAdminVO> listUsers(String keyword) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AuthUser::getStudentNo, keyword.trim())
                    .or()
                    .like(AuthUser::getRealName, keyword.trim());
        }
        return authUserMapper.selectList(wrapper).stream()
                .map(this::toUserAdminVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long userId, boolean approvePass, String operator) {
        AuthUser user = requireUser(userId);
        if (inSuperGroup(userId)) {
            throw new BizException(AuthErrorCode.SUPER_ADMIN_PROTECTED, "超级管理员账号无需审批");
        }
        user.setApproveStatus(approvePass ? 1 : 2);
        authUserMapper.updateById(user);
        audit(operator, "approve", user.getStudentNo(), approvePass ? "审批通过" : "审批驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserGroups(Long userId, List<String> groupCodes, String operator) {
        AuthUser user = requireUser(userId);
        if (inSuperGroup(userId)) {
            throw new BizException(AuthErrorCode.SUPER_ADMIN_PROTECTED, "不允许变更超级管理员的角色");
        }
        if (groupCodes.contains(SUPER_GROUP)) {
            throw new BizException(AuthErrorCode.SUPER_ADMIN_PROTECTED, "不允许将用户加入超级管理员组");
        }
        List<Long> groupIds = new ArrayList<>();
        for (String code : groupCodes) {
            AuthGroup group = authGroupMapper.selectByCode(code);
            if (group == null) {
                throw new BizException(AuthErrorCode.GROUP_NOT_FOUND, "用户组不存在: " + code);
            }
            groupIds.add(group.getId());
        }

        List<AuthUserGroup> existing = authUserGroupMapper.selectList(new LambdaQueryWrapper<AuthUserGroup>()
                .eq(AuthUserGroup::getUserId, userId));
        for (AuthUserGroup ug : existing) {
            authUserGroupMapper.deleteById(ug.getId());
        }
        for (Long groupId : groupIds) {
            AuthUserGroup ug = new AuthUserGroup();
            ug.setUserId(userId);
            ug.setGroupId(groupId);
            authUserGroupMapper.insert(ug);
        }

        // 换到敏感组（如学院管理员）需要重新走审批
        if (groupCodes.stream().anyMatch(NEED_APPROVE::contains)) {
            user.setApproveStatus(0);
            authUserMapper.updateById(user);
        }
        audit(operator, "user_group", user.getStudentNo(), "变更用户组为 " + String.join(",", groupCodes));
    }

    /* ==================== 组与矩阵 ==================== */

    @Override
    public List<GroupAdminVO> listGroups() {
        return authGroupMapper.selectList(new LambdaQueryWrapper<AuthGroup>()
                        .orderByAsc(AuthGroup::getSort))
                .stream()
                .map(group -> {
                    GroupAdminVO vo = new GroupAdminVO();
                    vo.setId(group.getId());
                    vo.setCode(group.getCode());
                    vo.setName(group.getName());
                    vo.setBuiltin(group.getBuiltin() != null && group.getBuiltin() == 1);
                    vo.setUserCount(rbacQueryMapper.countMembers(group.getId()));
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createGroup(GroupRequest request, String operator) {
        if (authGroupMapper.selectByCode(request.getCode()) != null) {
            throw new BizException(ErrorCode.DATA_CONFLICT, "组编码已存在");
        }
        AuthGroup group = new AuthGroup();
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setBuiltin(0);
        group.setSort(99);
        authGroupMapper.insert(group);
        audit(operator, "group_create", request.getCode(), "新建用户组 " + request.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId, String operator) {
        AuthGroup group = authGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BizException(AuthErrorCode.GROUP_NOT_FOUND, "用户组不存在");
        }
        if (group.getBuiltin() != null && group.getBuiltin() == 1) {
            throw new BizException(AuthErrorCode.BUILTIN_PROTECTED, "内置用户组不允许删除");
        }
        if (rbacQueryMapper.countMembers(groupId) > 0) {
            throw new BizException(ErrorCode.DATA_CONFLICT, "组内仍有成员，先移出成员再删除");
        }
        if (authGroupPrivMapper.countByGroup(groupId) > 0) {
            throw new BizException(ErrorCode.DATA_CONFLICT, "组上仍有授权记录，先清空权限再删除");
        }
        authGroupMapper.deleteById(groupId);
        audit(operator, "group_delete", group.getCode(), "删除用户组 " + group.getName());
    }

    @Override
    public MatrixVO matrix() {
        List<AuthModule> modules = authModuleMapper.selectList(new LambdaQueryWrapper<AuthModule>()
                .orderByAsc(AuthModule::getSort));
        List<AuthMethod> methods = authMethodMapper.selectList(new LambdaQueryWrapper<AuthMethod>()
                .orderByAsc(AuthMethod::getSort));
        List<AuthGroup> groups = authGroupMapper.selectList(new LambdaQueryWrapper<AuthGroup>()
                .orderByAsc(AuthGroup::getSort));

        Map<Long, List<AuthMethod>> methodsByModule = new LinkedHashMap<>();
        for (AuthMethod method : methods) {
            methodsByModule.computeIfAbsent(method.getModuleId(), k -> new ArrayList<>()).add(method);
        }

        MatrixVO vo = new MatrixVO();
        List<MatrixVO.ModuleNode> moduleNodes = new ArrayList<>();
        for (AuthModule module : modules) {
            MatrixVO.ModuleNode node = new MatrixVO.ModuleNode();
            node.setCode(module.getCode());
            node.setName(module.getName());
            node.setMethods(methodsByModule.getOrDefault(module.getId(), List.of()).stream()
                    .map(m -> {
                        MatrixVO.MethodNode mn = new MatrixVO.MethodNode();
                        mn.setCode(m.getCode());
                        mn.setName(m.getName());
                        return mn;
                    })
                    .toList());
            moduleNodes.add(node);
        }
        vo.setModules(moduleNodes);
        vo.setGroups(groups.stream()
                .map(g -> {
                    MatrixVO.GroupNode gn = new MatrixVO.GroupNode();
                    gn.setCode(g.getCode());
                    gn.setName(g.getName());
                    gn.setBuiltin(g.getBuiltin() != null && g.getBuiltin() == 1);
                    return gn;
                })
                .toList());

        List<String> grants = new ArrayList<>();
        for (AuthGroup group : groups) {
            for (String methodCode : authGroupPrivMapper.selectMethodCodes(group.getId())) {
                grants.add(group.getCode() + "|" + methodCode);
            }
        }
        vo.setGrants(grants);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(PrivRequest request, String operator) {
        AuthGroup group = authGroupMapper.selectByCode(request.getGroupCode());
        if (group == null) {
            throw new BizException(AuthErrorCode.GROUP_NOT_FOUND, "用户组不存在");
        }
        if (SUPER_GROUP.equals(group.getCode())) {
            throw new BizException(AuthErrorCode.BUILTIN_PROTECTED, "超级管理员组权限只读");
        }
        AuthMethod method = authMethodMapper.selectByCode(request.getPriv());
        if (method == null) {
            throw new BizException(AuthErrorCode.GROUP_NOT_FOUND, "权限点不存在");
        }

        AuthGroupPriv existing = authGroupPrivMapper.selectOne(new LambdaQueryWrapper<AuthGroupPriv>()
                .eq(AuthGroupPriv::getGroupId, group.getId())
                .eq(AuthGroupPriv::getMethodId, method.getId())
                .last("LIMIT 1"));

        if (Boolean.TRUE.equals(request.getGrant())) {
            if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
                // 已授权，无需变更
            } else if (authGroupPrivMapper.reactivate(group.getId(), method.getId()) == 0) {
                // 没有可激活的历史行才插入，避免与逻辑删除的旧行撞唯一键
                AuthGroupPriv gp = new AuthGroupPriv();
                gp.setGroupId(group.getId());
                gp.setModuleId(method.getModuleId());
                gp.setMethodId(method.getId());
                authGroupPrivMapper.insert(gp);
            }
        } else if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
            authGroupPrivMapper.deleteById(existing.getId());
        }
        audit(operator, request.getGrant() ? "grant" : "revoke",
                group.getCode() + "/" + request.getPriv(),
                (request.getGrant() ? "授予 " : "回收 ") + group.getName() + " 的 " + request.getPriv());
        log.info("权限矩阵调整 group={} priv={} grant={} operator={}",
                group.getCode(), request.getPriv(), request.getGrant(), operator);
    }

    /* ==================== 审计与会话 ==================== */

    @Override
    public void audit(String operator, String action, String target, String detail) {
        AuthAuditLog auditLog = new AuthAuditLog();
        auditLog.setOperator(operator);
        auditLog.setAction(action);
        auditLog.setTarget(target);
        auditLog.setDetail(detail);
        auditLog.setTs(LocalDateTime.now());
        authAuditLogMapper.insert(auditLog);
    }

    @Override
    public PageResult<AuditVO> auditPage(long page, long size) {
        Page<AuthAuditLog> result = authAuditLogMapper.selectPage(Page.of(page, Math.min(size, 100)),
                new LambdaQueryWrapper<AuthAuditLog>().orderByDesc(AuthAuditLog::getTs));
        List<AuditVO> list = result.getRecords().stream()
                .map(item -> {
                    AuditVO vo = new AuditVO();
                    vo.setOperator(item.getOperator());
                    vo.setAction(item.getAction());
                    vo.setTarget(item.getTarget());
                    vo.setDetail(item.getDetail());
                    vo.setTs(item.getTs());
                    return vo;
                })
                .toList();
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public List<SessionVO> listSessions() {
        Set<String> keys = redisTemplate.keys(CacheKeys.AUTH_SESSION + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<SessionVO> sessions = new ArrayList<>();
        for (String key : keys) {
            String jti = key.substring(CacheKeys.AUTH_SESSION.length());
            String value = redisTemplate.opsForValue().get(key);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (value == null) {
                continue;
            }
            Long userId = null;
            String role = null;
            try {
                var node = objectMapper.readTree(value);
                userId = node.has("userId") ? node.get("userId").asLong() : null;
                role = node.has("role") ? node.get("role").asText() : null;
            } catch (Exception ignored) {
                // 会话值解析失败按匿名会话展示
            }
            sessions.add(new SessionVO(jti, userId, accountOf(userId), role, ttl));
        }
        return sessions;
    }

    /* ==================== 链路追踪 ==================== */

    @Override
    public List<TraceVO> trace(Long userId) {
        List<TraceVO> traces = new ArrayList<>();
        for (Map.Entry<String, String> entry : ROUTES.entrySet()) {
            traces.add(new TraceVO(entry.getKey(), entry.getValue(), hasPerm(userId, entry.getValue())));
        }
        return traces;
    }

    @Override
    public List<String> allPermCodes() {
        return authMethodMapper.selectList(new LambdaQueryWrapper<AuthMethod>())
                .stream().map(AuthMethod::getCode).toList();
    }

    /* ==================== 登录辅助 ==================== */

    @Override
    public boolean isLocked(String account) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CacheKeys.AUTH_LOCK + account));
    }

    @Override
    public void recordLoginFailure(String account) {
        Long fails = redisTemplate.opsForValue().increment(CacheKeys.AUTH_FAIL + account);
        if (fails != null && fails == 1) {
            redisTemplate.expire(CacheKeys.AUTH_FAIL + account, LOCK_SECONDS, TimeUnit.SECONDS);
        }
        if (fails != null && fails >= FAIL_LIMIT) {
            redisTemplate.opsForValue().set(CacheKeys.AUTH_LOCK + account, "1", LOCK_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(CacheKeys.AUTH_FAIL + account);
            log.warn("账号连续登录失败已达阈值，临时锁定 account={}", account);
        }
    }

    @Override
    public void clearLoginFailure(String account) {
        redisTemplate.delete(CacheKeys.AUTH_FAIL + account);
    }

    /* ==================== 私有辅助 ==================== */

    private AuthUser requireUser(Long userId) {
        AuthUser user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(AuthErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserAdminVO toUserAdminVO(AuthUser user) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setAccount(user.getStudentNo());
        vo.setRealName(user.getRealName());
        vo.setStatus(user.getStatus());
        vo.setApproveStatus(user.getApproveStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setGroups(groupsOf(user.getId()));
        return vo;
    }

    private String accountOf(Long userId) {
        if (userId == null) {
            return null;
        }
        AuthUser user = authUserMapper.selectById(userId);
        return user != null ? user.getStudentNo() : String.valueOf(userId);
    }
}
