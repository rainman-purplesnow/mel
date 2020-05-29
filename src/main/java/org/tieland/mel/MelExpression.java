package org.tieland.mel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>mel表达式用于计算延时</p>
 * <p>
 *     {0,1s,1m,1h} 或 {0} 或 {0+} 或 {0,1s,1m,1h+} 或
 *     {0,1s/3,1m/2,1h} 或 {0,1s/3,1m/2,1h+}
 * </p>
 * @author zhouxiang
 * @date 2020/5/27 10:46
 */
public class MelExpression {

    /** 表达式 */
    private final String expression;

    /** {0,1s,1m,1h} 其中0/1s/1m/1h都是一个Node */
    private final NodeChain chain;

    public MelExpression(String expression){
        this.expression = expression;
        this.chain = new ExpressionParser().parse();
    }

    /**
     * 计算出nextTime对应的延迟时间
     * @param nextTime
     * @return
     */
    public MelDelay calcDelay(final Integer nextTime) throws CalculationException{
        //判断是否大于明确的频次
        if(chain.getAllRates() >= nextTime){
            Node node = chain.head;
            Integer surplus = nextTime;
            while(node != null){
                surplus = surplus - node.rate;
                if(surplus > 0){
                    node = node.next;
                }else{
                    return MelDelay.builder().delay(node.value)
                            .unit(node.unitEnum.unit).build();
                }
            }
        }

        //是否无限频次
        if(chain.isUnlimited()){
            return MelDelay.builder().delay(chain.tail.value)
                    .unit(chain.tail.unitEnum.unit).build();
        }

        throw new CalculationException(String.format(" expression trigger is limited. %s", chain.getAllRates()));
    }

    /**
     * 计算相对当前时间戳对应的nextTime对应的触发时间（时间戳）
     * @param nextTime
     * @return
     */
    public Instant calcTriggerInstant(Integer nextTime) throws CalculationException{
        return calcTriggerInstant(Instant.now(), nextTime);
    }

    /**
     * 计算相对instant（时间戳）对应的nextTime对应的触发时间（时间戳）
     * @param instant  相对instant（时间戳）
     * @param nextTime 第几次触发
     * @return
     * @throws CalculationException
     */
    public Instant calcTriggerInstant(Instant instant, Integer nextTime) throws CalculationException{
        MelDelay delay = calcDelay(nextTime);
        Instant triggerInstant = instant.plus(delay.getDelay(), delay.getUnit());
        return triggerInstant;
    }

    /**
     * expression表达式解析器
     */
    private class ExpressionParser {

        /** 匹配 0 | 100ms | 100s | 100m | 100h | 100d  */
        private final Pattern SIMPLE_LIMITED = Pattern.compile("(^[1-9]\\d*[ms|s|m|h|d]|0)$");

        /** 匹配 0+ | 100ms+ | 100s+ | 100m+ | 100h+ | 100d+ */
        private final Pattern SIMPLE_UNLIMITED = Pattern.compile("(^[1-9]\\d*[ms|s|m|h|d]|0)\\+$");

        /** 匹配 0/10 | 100ms/10 | 100s/10 | 100m/10 | 100h/10 | 100d/10 */
        private final Pattern MULTIPLE_LIMITED = Pattern.compile("(^[1-9]\\d*[ms|s|m|h|d]|0)/[1-9]\\d*$");

        /** 匹配 0/10+ | 100ms/10+ | 100s/10+ | 100m/10+ | 100h/10+ | 100d/10+ */
        private final Pattern MULTIPLE_UNLIMITED = Pattern.compile("(^[1-9]\\d*[ms|s|m|h|d]|0)/[1-9]\\d*\\+$");

        /** 匹配格式 */
        private final Pattern COMMON_PATTERN = Pattern.compile("^[0-9]\\S*[0-9|+|ms|s|m|h|d]$");

        /** expression 分隔符 */
        private final String MEL_EXPR_SEPARATOR = ",";

        /** 多重表达式结尾 */
        private final String MULTIPLE_END = "+";

        /** 表示"0" */
        private final String MEL_EXPR_ZERO = "0";

        /** 次数分隔符 */
        private final String MEL_RATE_SEPARATOR = "/";

        private ExpressionParser(){
            //
        }

        /**
         * 解析expression
         * @return
         */
        public NodeChain parse(){
            if(StringUtils.isBlank(expression)){
                throw new IllegalArgumentException(" expression must not blank ");
            }

            if(!COMMON_PATTERN.matcher(expression).matches()){
                throw new FormatException(String.format("%s is not correct format", expression));
            }

            //单Node模式
            if(!StringUtils.contains(expression, MEL_EXPR_SEPARATOR)){
                Node node = parseNode(expression);
                return new NodeChain(node, node);
            }

            //多Node模式
            String[] terms = StringUtils.split(expression, MEL_EXPR_SEPARATOR);
            List<Node> nodes = new ArrayList<>();
            Node head = null;
            Node tail = null;
            for(int i=0;i<terms.length;i++){
                Node node = parseNode(StringUtils.trim(terms[i]));
                nodes.add(node);

                if(i-1>=0){
                    Node pre = nodes.get(i-1);
                    pre.next = node;
                    node.pre = pre;
                }

                if(i==0){
                    head = node;
                }

                if(i==terms.length-1){
                    tail = node;
                }
            }

            return new NodeChain(head, tail);
        }

        /**
         * 解析节点
         * @param term
         * @return
         */
        private Node parseNode(final String term){
            if(StringUtils.isBlank(term)){
                throw new FormatException(" expression is not correct ");
            }

            if(!StringUtils.endsWith(term, MULTIPLE_END)){
                Node node = parseSingleNode(term);
                if(SIMPLE_LIMITED.matcher(term).matches() || MULTIPLE_LIMITED.matcher(term).matches()){
                    node.rateStrategy = RateStrategyEnum.LIMITED;
                    return node;
                }

                throw new FormatException(String.format("%s is not correct Format", term));
            }

            //解析以"+"结尾
            String newTerm = StringUtils.substringBefore(term, MULTIPLE_END);
            Node node = parseSingleNode(newTerm);
            if(SIMPLE_UNLIMITED.matcher(term).matches()){
                node.rateStrategy = RateStrategyEnum.UNLIMITED;
                return node;
            }

            throw new FormatException(String.format("%s is not correct Format", term));
        }

        /**
         * 解析单个Node
         * @param term
         * @return
         */
        private Node parseSingleNode(final String term){
            Node node = new Node();
            String prefixTerm = term;

            //解析是否含有"/"，有"/"则需要解析频次
            if(StringUtils.contains(term, MEL_RATE_SEPARATOR)){
                prefixTerm = StringUtils.substringBefore(term, MEL_RATE_SEPARATOR);
                String rate = StringUtils.substringAfter(term, MEL_RATE_SEPARATOR);
                try{
                    node.rate = Integer.parseInt(rate);
                }catch (Exception ex){
                    throw new FormatException(String.format("%s is not correct Format", term));
                }
            }else{
                node.rate = 1;
            }

            //判断是否为"0"
            if(StringUtils.equals(prefixTerm, MEL_EXPR_ZERO)){
                node.value = Long.valueOf(0);
                node.unitEnum = UnitEnum.S;
                return node;
            }

            //不为"0"时，则需要解析UnitEnum和相应value数值
            UnitEnum unitEnum = getUnitEnum(prefixTerm);
            if(unitEnum == null){
                throw new FormatException(String.format("%s is not correct Format", term));
            }

            node.unitEnum = unitEnum;
            String value = StringUtils.substringBefore(prefixTerm, unitEnum.symbol);
            try{
                node.value = Long.valueOf(value);
            }catch (Exception ex){
                throw new FormatException(String.format("%s is not correct Format", term));
            }

            return node;
        }



        /**
         *
         * @param term
         * @return
         */
        private UnitEnum getUnitEnum(String term){
            for(UnitEnum unitEnum:UnitEnum.values()){
                if(StringUtils.endsWith(term, unitEnum.symbol)){
                    return unitEnum;
                }
            }

            return null;
        }

    }

    private enum RateStrategyEnum{

        LIMITED, UNLIMITED

    }

    private enum UnitEnum {

        /**
         *
         */
        MS("ms", ChronoUnit.MILLIS),

        S("s", ChronoUnit.SECONDS),

        M("m", ChronoUnit.MINUTES),

        H("h", ChronoUnit.HOURS),

        D("d", ChronoUnit.DAYS);

        UnitEnum(final String symbol, ChronoUnit unit){
            this.symbol = symbol;
            this.unit = unit;
        }

        private ChronoUnit unit;

        private String symbol;

    }

    private class NodeChain{

        private final Node head;

        private final Node tail;

        public NodeChain(Node head, Node tail){
            this.head = head;
            this.tail = tail;
        }

        /**
         * 是否无限可调用
         * @return
         */
        public boolean isUnlimited(){
            return tail.rateStrategy == RateStrategyEnum.UNLIMITED;
        }

        /**
         * 获取所有node明确的频次总和
         * @return
         */
        public Integer getAllRates(){
            Integer allRates = 0;
            Node node = head;
            while(node != null){
                allRates = allRates + node.rate;
                node = node.next;
            }

            return allRates;
        }

        /**
         * 所有node数量
         * @return
         */
        public Integer size(){
            if(head == tail){
                return 1;
            }

            Integer size = 0;
            Node node = head;
            while(node != null){
                size = size + 1;
                node = node.next;
            }

            return size;
        }
    }

    @Data
    @ToString
    @EqualsAndHashCode
    private class Node {

        /**
         * ChronoUnit单位
         */
        private UnitEnum unitEnum;

        /**
         * 数值
         */
        private Long value;

        /**
         * 频率次数
         */
        private Integer rate;

        /**
         * 频率类型enum
         */
        private RateStrategyEnum rateStrategy;

        /**
         * 下一个节点
         */
        private Node next;

        /**
         * 上一个节点
         */
        private Node pre;

    }


}
