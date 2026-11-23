package com.sicnu.yingxin.qa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息。
 */
@Data
public class MessageVO {

    /** 1 用户 / 2 数字人 */
    private Integer role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createTime;

    /** 命中的问答条目 ID，可空 */
    private Long questionId;

    public MessageVO(Integer role, String content, LocalDateTime createTime, Long questionId) {
        this.role = role;
        this.content = content;
        this.createTime = createTime;
        this.questionId = questionId;
    }
}
