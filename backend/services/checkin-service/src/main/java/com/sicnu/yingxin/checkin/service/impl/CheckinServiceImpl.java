package com.sicnu.yingxin.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sicnu.yingxin.checkin.dto.StepCreateRequest;
import com.sicnu.yingxin.checkin.dto.StepUpdateRequest;
import com.sicnu.yingxin.checkin.entity.CheckinCertificate;
import com.sicnu.yingxin.checkin.entity.CheckinMaterialItem;
import com.sicnu.yingxin.checkin.entity.CheckinProgress;
import com.sicnu.yingxin.checkin.entity.CheckinStepTemplate;
import com.sicnu.yingxin.checkin.error.CheckinErrorCode;
import com.sicnu.yingxin.checkin.mapper.CheckinCertificateMapper;
import com.sicnu.yingxin.checkin.mapper.CheckinMaterialItemMapper;
import com.sicnu.yingxin.checkin.mapper.CheckinProgressMapper;
import com.sicnu.yingxin.checkin.mapper.CheckinStepTemplateMapper;
import com.sicnu.yingxin.checkin.service.CheckinService;
import com.sicnu.yingxin.checkin.vo.CheckinResultVO;
import com.sicnu.yingxin.checkin.vo.MaterialVO;
import com.sicnu.yingxin.checkin.vo.ProgressVO;
import com.sicnu.yingxin.checkin.vo.StepAdminVO;
import com.sicnu.yingxin.checkin.vo.StepProgressVO;
import com.sicnu.yingxin.checkin.vo.StepVO;
import com.sicnu.yingxin.checkin.vo.VoucherVO;
import com.sicnu.yingxin.common.core.constant.Roles;
import com.sicnu.yingxin.common.core.error.BizException;
import com.sicnu.yingxin.common.core.error.ErrorCode;
import com.sicnu.yingxin.common.core.result.PageResult;
import com.sicnu.yingxin.common.web.context.UserContextHolder;
import com.sicnu.yingxin.common.web.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报到服务实现。
 *
 * <p>打卡走「先查后插 + 唯一键冲突兜底」的幂等模式；
 * 凭证签发同理，并发重复请求靠 uk_cert_no / uk_user 唯一键约束收敛为单条记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    /** 步骤状态：待办 */
    private static final int STATUS_TODO = 1;
    /** 步骤状态：已完成 */
    private static final int STATUS_DONE = 2;
    /** 打卡渠道：自助确认 */
    private static final int CHANNEL_SELF = 1;
    /** 启用开关值 */
    private static final int ENABLED_ON = 1;
    /** 凭证编号前缀（演示批次：YX2026） */
    private static final String CERT_NO_PREFIX = "YX2026";
    /** 凭证有效期（天），演示用 */
    private static final long CERT_VALID_DAYS = 30;
    /** 签发凭证写入的报到批次（一期固定批次；正式应读 admin_config 批次定义） */
    private static final long BATCH_ID = 2026L;

    private final CheckinStepTemplateMapper stepTemplateMapper;
    private final CheckinMaterialItemMapper materialItemMapper;
    private final CheckinProgressMapper progressMapper;
    private final CheckinCertificateMapper certificateMapper;
    private final EventPublisher eventPublisher;

    /* ==================== 学生端 ==================== */

    @Override
    public List<StepVO> listEnabledSteps() {
        List<CheckinStepTemplate> steps = stepTemplateMapper.selectList(
                new LambdaQueryWrapper<CheckinStepTemplate>()
                        .eq(CheckinStepTemplate::getEnabled, ENABLED_ON)
                        .orderByAsc(CheckinStepTemplate::getSeq));
        return steps.stream().map(this::toStepVO).toList();
    }

    @Override
    public List<MaterialVO> listMaterials(String stepCode) {
        CheckinStepTemplate step = getStepByCode(stepCode);
        if (step == null) {
            throw new BizException(CheckinErrorCode.STEP_NOT_FOUND, "报到步骤不存在");
        }
        List<CheckinMaterialItem> items = materialItemMapper.selectList(
                new LambdaQueryWrapper<CheckinMaterialItem>()
                        .eq(CheckinMaterialItem::getStepCode, step.getStepCode())
                        .orderByAsc(CheckinMaterialItem::getSeq));
        return items.stream()
                .map(item -> MaterialVO.builder()
                        .name(item.getName())
                        .required(item.getRequired() != null && item.getRequired() == 1)
                        .remark(item.getRemark())
                        .build())
                .toList();
    }

    @Override
    public ProgressVO getProgress() {
        Long userId = requireUserId();

        // 以启用步骤模板为全集
        List<CheckinStepTemplate> steps = listEnabledTemplates();
        List<CheckinProgress> records = listProgressOfSteps(userId,
                steps.stream().map(CheckinStepTemplate::getStepCode).toList());
        Map<String, CheckinProgress> recordMap = records.stream()
                .collect(Collectors.toMap(CheckinProgress::getStepCode, Function.identity(), (a, b) -> a));

        List<StepProgressVO> items = steps.stream()
                .map(step -> {
                    CheckinProgress record = recordMap.get(step.getStepCode());
                    int status = record == null || record.getStatus() == null ? STATUS_TODO : record.getStatus();
                    return StepProgressVO.builder()
                            .stepCode(step.getStepCode())
                            .stepName(step.getStepName())
                            .seq(step.getSeq())
                            .status(status)
                            .checkinTime(record == null ? null : record.getCheckinTime())
                            .build();
                })
                .toList();

        int total = items.size();
        int done = (int) items.stream().filter(item -> item.getStatus() == STATUS_DONE).count();
        int percent = total == 0 ? 100 : (int) Math.round(done * 100.0 / total);
        return ProgressVO.builder()
                .steps(items)
                .doneCount(done)
                .totalCount(total)
                .percent(percent)
                .build();
    }

    @Override
    public CheckinResultVO checkin(String stepCode) {
        Long userId = requireUserId();

        CheckinStepTemplate step = getStepByCode(stepCode);
        if (step == null || step.getEnabled() == null || step.getEnabled() != ENABLED_ON) {
            // 不存在或已停用统一按步骤不可用处理
            throw new BizException(CheckinErrorCode.STEP_NOT_FOUND, "报到步骤不存在或已停用");
        }

        // 先查后插兜底：已有完成记录直接返回幂等结果
        CheckinProgress existing = selectProgress(userId, step.getStepCode());
        if (existing != null && existing.getStatus() != null && existing.getStatus() == STATUS_DONE) {
            return new CheckinResultVO(true, step.getStepCode(), existing.getCheckinTime());
        }

        LocalDateTime now = LocalDateTime.now();
        CheckinProgress progress = new CheckinProgress();
        progress.setUserId(userId);
        progress.setStepCode(step.getStepCode());
        progress.setStatus(STATUS_DONE);
        progress.setCheckinTime(now);
        progress.setCheckinChannel(CHANNEL_SELF);
        try {
            progressMapper.insert(progress);
        } catch (DuplicateKeyException e) {
            // 并发重复打卡：uk_user_step 冲突，按幂等成功处理，不发事件
            log.info("重复打卡命中唯一键 userId={} stepCode={}", userId, step.getStepCode());
            CheckinProgress record = selectProgress(userId, step.getStepCode());
            return new CheckinResultVO(true, step.getStepCode(),
                    record == null ? now : record.getCheckinTime());
        }

        // 首次打卡成功，发布报到打卡事件（eventType=3，渠道 1 小程序）
        eventPublisher.publish(3, userId, "checkin", step.getId(), CHANNEL_SELF,
                Map.of("stepCode", step.getStepCode()));
        log.info("报到打卡成功 userId={} stepCode={}", userId, step.getStepCode());
        return new CheckinResultVO(false, step.getStepCode(), now);
    }

    @Override
    public VoucherVO getVoucher() {
        Long userId = requireUserId();

        // 全部启用步骤完成才可领取
        List<CheckinStepTemplate> steps = listEnabledTemplates();
        List<String> stepCodes = steps.stream().map(CheckinStepTemplate::getStepCode).toList();
        long doneCount;
        if (stepCodes.isEmpty()) {
            doneCount = 0;
        } else {
            doneCount = progressMapper.selectCount(
                    new LambdaQueryWrapper<CheckinProgress>()
                            .eq(CheckinProgress::getUserId, userId)
                            .eq(CheckinProgress::getStatus, STATUS_DONE)
                            .in(CheckinProgress::getStepCode, stepCodes));
        }
        if (doneCount < stepCodes.size()) {
            throw new BizException(CheckinErrorCode.CERT_NOT_READY, "还有报到步骤未完成，无法领取报到凭证");
        }

        CheckinCertificate existing = certificateMapper.selectOne(
                new LambdaQueryWrapper<CheckinCertificate>()
                        .eq(CheckinCertificate::getUserId, userId)
                        .last("LIMIT 1"));
        if (existing != null) {
            return toVoucherVO(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        CheckinCertificate certificate = new CheckinCertificate();
        certificate.setUserId(userId);
        certificate.setBatchId(BATCH_ID);
        certificate.setCertNo(CERT_NO_PREFIX + userId);
        certificate.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        certificate.setIssuedTime(now);
        certificate.setExpireTime(now.plusDays(CERT_VALID_DAYS));
        certificate.setVerifyStatus(1);
        try {
            certificateMapper.insert(certificate);
        } catch (DuplicateKeyException e) {
            // 并发签发：uk_cert_no / uk_user 冲突，回查已有凭证
            log.info("并发领取凭证命中唯一键 userId={}", userId);
            CheckinCertificate record = certificateMapper.selectOne(
                    new LambdaQueryWrapper<CheckinCertificate>()
                            .eq(CheckinCertificate::getUserId, userId)
                            .last("LIMIT 1"));
            if (record != null) {
                return toVoucherVO(record);
            }
            throw e;
        }
        log.info("报到凭证签发 userId={} certNo={}", userId, certificate.getCertNo());
        return toVoucherVO(certificate);
    }

    /* ==================== 管理端 ==================== */

    @Override
    public PageResult<StepAdminVO> pageSteps(int page, int size, String keyword) {
        requireAdmin();

        LambdaQueryWrapper<CheckinStepTemplate> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CheckinStepTemplate::getStepName, keyword.trim());
        }
        wrapper.orderByAsc(CheckinStepTemplate::getSeq)
                .orderByAsc(CheckinStepTemplate::getId);

        Page<CheckinStepTemplate> result = stepTemplateMapper.selectPage(new Page<>(page, size), wrapper);
        List<StepAdminVO> list = result.getRecords().stream().map(this::toStepAdminVO).toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StepAdminVO createStep(StepCreateRequest request) {
        requireAdmin();

        CheckinStepTemplate existing = getStepByCode(request.getStepCode());
        if (existing != null) {
            throw new BizException(CheckinErrorCode.STEP_CODE_CONFLICT, "步骤编码已存在");
        }

        Long operator = UserContextHolder.getUserId();
        CheckinStepTemplate step = new CheckinStepTemplate();
        step.setStepCode(request.getStepCode().trim());
        step.setStepName(request.getStepName().trim());
        step.setSeq(request.getSeq());
        step.setLocation(request.getLocation());
        step.setOfficeHours(request.getOfficeHours());
        step.setContact(request.getContact());
        step.setEnabled(ENABLED_ON);
        try {
            stepTemplateMapper.insert(step);
        } catch (DuplicateKeyException e) {
            // 并发新建：uk_step_code 冲突
            throw new BizException(CheckinErrorCode.STEP_CODE_CONFLICT, "步骤编码已存在");
        }
        log.info("新建报到步骤 stepCode={} operator={}", step.getStepCode(), operator);
        return toStepAdminVO(step);
    }

    @Override
    public StepAdminVO updateStep(Long id, StepUpdateRequest request) {
        requireAdmin();

        CheckinStepTemplate step = stepTemplateMapper.selectById(id);
        if (step == null) {
            throw new BizException(CheckinErrorCode.STEP_NOT_FOUND, "报到步骤不存在");
        }

        if (request.getStepName() != null) {
            step.setStepName(request.getStepName().trim());
        }
        if (request.getSeq() != null) {
            step.setSeq(request.getSeq());
        }
        if (request.getLocation() != null) {
            step.setLocation(request.getLocation());
        }
        if (request.getOfficeHours() != null) {
            step.setOfficeHours(request.getOfficeHours());
        }
        if (request.getContact() != null) {
            step.setContact(request.getContact());
        }
        if (request.getEnabled() != null) {
            step.setEnabled(request.getEnabled());
        }
        stepTemplateMapper.updateById(step);
        log.info("修改报到步骤 id={} operator={}", id, UserContextHolder.getUserId());
        return toStepAdminVO(step);
    }

    /* ==================== 私有方法 ==================== */

    /** 当前登录用户 ID，未登录抛未授权异常 */
    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 校验当前用户为管理员，否则抛无权限异常 */
    private void requireAdmin() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!Roles.ADMIN.equals(UserContextHolder.getRole())) {
            throw new BizException(ErrorCode.NO_PERMISSION);
        }
    }

    /** 按编码查步骤（逻辑删除自动过滤） */
    private CheckinStepTemplate getStepByCode(String stepCode) {
        if (stepCode == null || stepCode.isBlank()) {
            return null;
        }
        return stepTemplateMapper.selectOne(
                new LambdaQueryWrapper<CheckinStepTemplate>()
                        .eq(CheckinStepTemplate::getStepCode, stepCode.trim())
                        .last("LIMIT 1"));
    }

    /** 全部启用步骤，按 seq 升序 */
    private List<CheckinStepTemplate> listEnabledTemplates() {
        return stepTemplateMapper.selectList(
                new LambdaQueryWrapper<CheckinStepTemplate>()
                        .eq(CheckinStepTemplate::getEnabled, ENABLED_ON)
                        .orderByAsc(CheckinStepTemplate::getSeq));
    }

    /** 查询单条用户进度记录 */
    private CheckinProgress selectProgress(Long userId, String stepCode) {
        return progressMapper.selectOne(
                new LambdaQueryWrapper<CheckinProgress>()
                        .eq(CheckinProgress::getUserId, userId)
                        .eq(CheckinProgress::getStepCode, stepCode)
                        .last("LIMIT 1"));
    }

    /** 查询用户在给定步骤集合上的全部进度记录 */
    private List<CheckinProgress> listProgressOfSteps(Long userId, List<String> stepCodes) {
        if (stepCodes.isEmpty()) {
            return List.of();
        }
        return progressMapper.selectList(
                new LambdaQueryWrapper<CheckinProgress>()
                        .eq(CheckinProgress::getUserId, userId)
                        .in(CheckinProgress::getStepCode, stepCodes));
    }

    private StepVO toStepVO(CheckinStepTemplate step) {
        return StepVO.builder()
                .id(step.getId())
                .stepCode(step.getStepCode())
                .stepName(step.getStepName())
                .seq(step.getSeq())
                .location(step.getLocation())
                .officeHours(step.getOfficeHours())
                .contact(step.getContact())
                .build();
    }

    private StepAdminVO toStepAdminVO(CheckinStepTemplate step) {
        return StepAdminVO.builder()
                .id(step.getId())
                .stepCode(step.getStepCode())
                .stepName(step.getStepName())
                .seq(step.getSeq())
                .location(step.getLocation())
                .officeHours(step.getOfficeHours())
                .contact(step.getContact())
                .enabled(step.getEnabled())
                .createTime(step.getCreateTime())
                .updateTime(step.getUpdateTime())
                .build();
    }

    private VoucherVO toVoucherVO(CheckinCertificate certificate) {
        return new VoucherVO(certificate.getCertNo(), certificate.getQrToken(),
                certificate.getIssuedTime(), certificate.getExpireTime());
    }
}
