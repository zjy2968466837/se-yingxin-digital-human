package com.sicnu.yingxin.auth.service;

import com.sicnu.yingxin.auth.dto.GroupRequest;
import com.sicnu.yingxin.auth.dto.RegisterRequest;
import com.sicnu.yingxin.auth.dto.PrivRequest;
import com.sicnu.yingxin.auth.vo.AuditVO;
import com.sicnu.yingxin.auth.vo.GroupAdminVO;
import com.sicnu.yingxin.auth.vo.GroupVO;
import com.sicnu.yingxin.auth.vo.MatrixVO;
import com.sicnu.yingxin.auth.vo.SessionVO;
import com.sicnu.yingxin.auth.vo.TraceVO;
import com.sicnu.yingxin.auth.vo.UserAdminVO;
import com.sicnu.yingxin.common.core.result.PageResult;

import java.util.List;

/**
 * 禅道式 RBAC 权限中心服务（业务阶段落地，替代脚手架阶段的开发夹具）。
 *
 * <p>权限模型：user → usergroup → group → grouppriv(module, method)。
 * 权限只挂在组上，超级组 admin 免判直通，矩阵调整即时生效。
 */
public interface RbacService {

    boolean inSuperGroup(Long userId);

    boolean hasPerm(Long userId, String methodCode);

    List<String> permsOf(Long userId);

    List<GroupVO> groupsOf(Long userId);

    Long register(RegisterRequest request, String operator);

    List<UserAdminVO> pendingUsers();

    List<UserAdminVO> listUsers(String keyword);

    void approve(Long userId, boolean approvePass, String operator);

    void setUserGroups(Long userId, List<String> groupCodes, String operator);

    List<GroupAdminVO> listGroups();

    void createGroup(GroupRequest request, String operator);

    void deleteGroup(Long groupId, String operator);

    MatrixVO matrix();

    void grant(PrivRequest request, String operator);

    void audit(String operator, String action, String target, String detail);

    PageResult<AuditVO> auditPage(long page, long size);

    List<SessionVO> listSessions();

    List<TraceVO> trace(Long userId);

    List<String> allPermCodes();

    boolean isLocked(String account);

    void recordLoginFailure(String account);

    void clearLoginFailure(String account);
}
