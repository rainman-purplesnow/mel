package org.tieland.mel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.temporal.ChronoUnit;

/**
 * 延时VO
 * @author zhouxiang
 * @date 2020/5/28 11:10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MelDelay {

    /**
     * 延时值
     */
    private long delay;

    /**
     * 延时时间unit
     */
    private ChronoUnit unit;

}
