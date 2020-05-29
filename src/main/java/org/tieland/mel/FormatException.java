package org.tieland.mel;

/**
 * 表达式格式Exception
 * @author zhouxiang
 * @date 2020/5/28 10:24
 */
public class FormatException extends RuntimeException {

    public FormatException(String message){
        super(message);
    }

    public FormatException(String message, Throwable throwable){
        super(message, throwable);
    }

}
