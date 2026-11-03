package com.sicnu.yingxin.qa.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sicnu.yingxin.qa.entity.QaSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话返回对象。Entity 不出接口（开发规范 §2.1）。
 */
@Data
@Schema(description = "问答会话")
public class QaSessionVO {

    /** 雪花 ID 是 19 位，超过 JS 的 2^53，必须序列化成字符串，否则前端会末位丢精度。 */
    @Schema(description = "会话 id（雪花 ID，字符串传输）", example = "3300000000000000001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "学生用户 id（字符串传输）", example = "1000123")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "来源渠道：1 学生端网页 / 2 管理后台", example = "1")
    private Integer channel;

    @Schema(description = "会话开始时间", example = "2026-10-27 16:30:00")
    private LocalDateTime startTime;

    @Schema(description = "会话结束时间，未结束为 null")
    private LocalDateTime endTime;

    @Schema(description = "消息条数", example = "0")
    private Integer messageCount;

    @Schema(description = "满意度：0 未评价 / 1 满意 / 2 不满意", example = "0")
    private Integer satisfied;

    @Schema(description = "创建时间（持久层自动填充）", example = "2026-10-27 16:30:00")
    private LocalDateTime createTime;

    public static QaSessionVO from(QaSession entity) {
        if (entity == null) {
            return null;
        }
        QaSessionVO vo = new QaSessionVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setChannel(entity.getChannel());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setMessageCount(entity.getMessageCount());
        vo.setSatisfied(entity.getSatisfied());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
