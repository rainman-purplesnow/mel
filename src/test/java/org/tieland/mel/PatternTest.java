package org.tieland.mel;

import org.junit.Assert;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhouxiang
 * @date 2020/5/27 15:37
 */
public class PatternTest {

    @Test
    public void test2(){
        MelExpression expression = new MelExpression("0,1m,1h,12h");
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(1));
        Assert.assertEquals(MelDelay.builder().delay(1).unit(ChronoUnit.MINUTES).build(),expression.calcDelay(2));
        Assert.assertEquals(MelDelay.builder().delay(1).unit(ChronoUnit.HOURS).build(),expression.calcDelay(3));
        Assert.assertEquals(MelDelay.builder().delay(12).unit(ChronoUnit.HOURS).build(),expression.calcDelay(4));
    }

    @Test(expected = FormatException.class)
    public void test3(){
        MelExpression expression = new MelExpression("0,1m,1h,12h,");
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(1));
    }

    @Test
    public void test4(){
        MelExpression expression = new MelExpression("0,1m,1h,12h+");
        Assert.assertEquals(MelDelay.builder().delay(12).unit(ChronoUnit.HOURS).build(),expression.calcDelay(10));
    }

    @Test
    public void test5(){
        MelExpression expression = new MelExpression("12h+");
        Assert.assertEquals(MelDelay.builder().delay(12).unit(ChronoUnit.HOURS).build(),expression.calcDelay(1));
        Assert.assertEquals(MelDelay.builder().delay(12).unit(ChronoUnit.HOURS).build(),expression.calcDelay(2));
    }

    @Test
    public void test6(){
        MelExpression expression = new MelExpression("0+");
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(1));
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(2));
    }

    @Test
    public void test7(){
        MelExpression expression = new MelExpression("0/2,1m/3");
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(2));
        Assert.assertEquals(MelDelay.builder().delay(1).unit(ChronoUnit.MINUTES).build(),expression.calcDelay(5));
    }

    @Test(expected = CalculationException.class)
    public void test8(){
        MelExpression expression = new MelExpression("0/2,1m/3");
        Assert.assertEquals(MelDelay.builder().delay(0).unit(ChronoUnit.SECONDS).build(),expression.calcDelay(6));
    }

    @Test(expected = FormatException.class)
    public void test9(){
        MelExpression expression = new MelExpression("0/2,1m/3+");
        Assert.assertEquals(MelDelay.builder().delay(1).unit(ChronoUnit.MINUTES).build(),expression.calcDelay(6));
    }

    @Test(expected = FormatException.class)
    public void test10(){
        MelExpression expression = new MelExpression("0/2a,1m/3");
        Assert.assertEquals(MelDelay.builder().delay(1).unit(ChronoUnit.MINUTES).build(),expression.calcDelay(6));
    }

}
