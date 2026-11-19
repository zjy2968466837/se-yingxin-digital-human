package com.sicnu.yingxin.checkin.service;

import com.sicnu.yingxin.checkin.dto.StepCreateRequest;
import com.sicnu.yingxin.checkin.dto.StepUpdateRequest;
import com.sicnu.yingxin.checkin.vo.CheckinResultVO;
import com.sicnu.yingxin.checkin.vo.MaterialVO;
import com.sicnu.yingxin.checkin.vo.ProgressVO;
import com.sicnu.yingxin.checkin.vo.StepAdminVO;
import com.sicnu.yingxin.checkin.vo.StepVO;
import com.sicnu.yingxin.checkin.vo.VoucherVO;
import com.sicnu.yingxin.common.core.result.PageResult;

import java.util.List;

/**
 * 报到服务接口。
 */
public interface CheckinService {

    /**
     * 查询启用的报到步骤列表（按 seq 升序）。
     *
     * @return 步骤列表
     */
    List<StepVO> listEnabledSteps();

    /**
     * 查询指定步骤的材料清单（按 seq 升序）。
     *
     * @param stepCode 步骤编码
     * @return 材料列表
     */
    List<MaterialVO> listMaterials(String stepCode);

    /**
     * 查询当前用户的报到总进度。
     *
     * @return 进度视图
     */
    ProgressVO getProgress();

    /**
     * 当前用户对指定步骤打卡（幂等：重复打卡返回 isDuplicated=true，不重复发事件）。
     *
     * @param stepCode 步骤编码
     * @return 打卡结果
     */
    CheckinResultVO checkin(String stepCode);

    /**
     * 查询/签发当前用户的报到凭证（须完成全部启用步骤）。
     *
     * @return 凭证视图
     */
    VoucherVO getVoucher();

    /**
     * 管理端：分页查询全部步骤（含停用），keyword 模糊匹配步骤名称。
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 名称关键字，可空
     * @return 分页结果
     */
    PageResult<StepAdminVO> pageSteps(int page, int size, String keyword);

    /**
     * 管理端：新建报到步骤。
     *
     * @param request 新建请求
     * @return 新建后的步骤
     */
    StepAdminVO createStep(StepCreateRequest request);

    /**
     * 管理端：修改报到步骤（PATCH，只更新传入字段）。
     *
     * @param id      步骤 ID
     * @param request 修改请求
     * @return 修改后的步骤
     */
    StepAdminVO updateStep(Long id, StepUpdateRequest request);
}
