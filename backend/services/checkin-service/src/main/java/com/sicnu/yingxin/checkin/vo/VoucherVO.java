package com.sicnu.yingxin.checkin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报到凭证视图。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherVO {

    /** 凭证编号 */
    private String certNo;

    /** 二维码令牌 */
    private String qrToken;

    /** 签发时间 */
    private LocalDateTime issuedTime;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
