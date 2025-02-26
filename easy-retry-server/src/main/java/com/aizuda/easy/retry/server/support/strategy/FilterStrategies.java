package com.aizuda.easy.retry.server.support.strategy;

import cn.hutool.core.lang.Pair;
import com.aizuda.easy.retry.server.dto.RegisterNodeInfo;
import com.aizuda.easy.retry.server.support.handler.ServerNodeBalance;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.aizuda.easy.retry.common.core.context.SpringContext;
import com.aizuda.easy.retry.common.core.log.LogUtils;
import com.aizuda.easy.retry.template.datasource.persistence.mapper.ServerNodeMapper;
import com.aizuda.easy.retry.template.datasource.persistence.po.RetryTask;
import com.aizuda.easy.retry.template.datasource.persistence.po.ServerNode;
import com.aizuda.easy.retry.server.support.FilterStrategy;
import com.aizuda.easy.retry.server.support.IdempotentStrategy;
import com.aizuda.easy.retry.server.support.RetryContext;
import com.aizuda.easy.retry.server.support.cache.CacheGroupRateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 生成 {@link FilterStrategy} 实例.
 *
 * @author: www.byteblogs.com
 * @date : 2021-11-30 10:03
 */
@Slf4j
public class FilterStrategies {

    private FilterStrategies() {
    }

    /**
     * 触发时间过滤策略
     *
     * @return {@link TriggerAtFilterStrategies} 触发时间过滤策略
     */
    public static FilterStrategy triggerAtFilter() {
        return new TriggerAtFilterStrategies();
    }

    /**
     * 使用BitSet幂等的过滤策略
     *
     * @return {@link BitSetIdempotentFilterStrategies} BitSet幂等的过滤策略
     */
    public static FilterStrategy bitSetIdempotentFilter(IdempotentStrategy<String, Integer> idempotentStrategy) {
        return new BitSetIdempotentFilterStrategies(idempotentStrategy);
    }

    /**
     * 场景黑名单策略
     *
     * @return {@link SceneBlackFilterStrategies} 场景黑名单策略
     */
    public static FilterStrategy sceneBlackFilter() {
        return new SceneBlackFilterStrategies();
    }

    /**
     * 检查是否存在存活的客户端POD
     *
     * @return {@link CheckAliveClientPodFilterStrategies} 客户端存活POD检查策略
     */
    public static FilterStrategy checkAliveClientPodFilter() {
        return new CheckAliveClientPodFilterStrategies();
    }

    /**
     * 检查分配的客户端是否达到限流阈值
     *
     * @return {@link RateLimiterFilterStrategies} 检查分配的客户端是否达到限流阈值
     */
    public static FilterStrategy rateLimiterFilter() {
        return new RateLimiterFilterStrategies();
    }

    /**
     * 正在rebalance时不允许下发重试流量
     *
     * @return {@link ReBalanceFilterStrategies} 正在rebalance时不允许下发重试流量
     */
    public static FilterStrategy rebalanceFilterStrategies() {
        return new ReBalanceFilterStrategies();
    }

    /**
     * 触发时间过滤策略
     * <p>
     * 根据延迟等级的时间计算下次触发时间是否小于当前时间，满足则返回true 否则返回false
     */
    private static final class TriggerAtFilterStrategies implements FilterStrategy {

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RetryTask retryTask = retryContext.getRetryTask();
            LocalDateTime nextTriggerAt =retryTask.getNextTriggerAt();

            boolean result = nextTriggerAt.isBefore(LocalDateTime.now());
            StringBuilder description = new StringBuilder();
            if (!result) {
                description.append(MessageFormat.format("未到触发时间. uniqueId:[{0}]", retryTask.getUniqueId()));
            }

            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 0;
        }
    }

    /**
     * 使用BitSet幂等的过滤策略
     * <p>
     * 判断BitSet中是否存在，若存在则放回false 否则返回true
     */
    private static final class BitSetIdempotentFilterStrategies implements FilterStrategy {

        private IdempotentStrategy<String, Integer> idempotentStrategy;

        public BitSetIdempotentFilterStrategies(IdempotentStrategy<String, Integer> idempotentStrategy) {
            this.idempotentStrategy = idempotentStrategy;
        }

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RetryTask retryTask = retryContext.getRetryTask();

            boolean result = !idempotentStrategy.isExist(retryTask.getGroupName(), retryTask.getId().intValue());
            StringBuilder description = new StringBuilder();
            if (!result) {
                description.append(MessageFormat.format("存在执行中的任务.uniqueId:[{0}]", retryTask.getUniqueId()));
            }

            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 1;
        }
    }

    /**
     * 场景黑名单策略
     * <p>
     * 如果重试的数据在黑名单中的则返回false 否则为true
     */
    private static final class SceneBlackFilterStrategies implements FilterStrategy {

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RetryTask retryTask = retryContext.getRetryTask();

            boolean result = !retryContext.getSceneBlacklist().contains(retryTask.getSceneName());

            StringBuilder description = new StringBuilder();
            if (!result) {
                description.append(MessageFormat.format("场景:[{0}]在黑名单中, 不允许执行.", retryTask.getSceneName()));
            }

            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 2;
        }
    }

    /**
     * 检查是否存在存活的客户端POD
     */
    private static final class CheckAliveClientPodFilterStrategies implements FilterStrategy {

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RetryTask retryTask = retryContext.getRetryTask();
            RegisterNodeInfo serverNode = retryContext.getServerNode();

            StringBuilder description = new StringBuilder();
            if (Objects.isNull(serverNode)) {
                return Pair.of(Boolean.FALSE, description.append(MessageFormat.format("没有可执行的客户端节点. uniqueId:[{0}]", retryTask.getUniqueId())));
            }

            ServerNodeMapper serverNodeMapper = SpringContext.getBeanByType(ServerNodeMapper.class);
            boolean result = 1 == serverNodeMapper.selectCount(new LambdaQueryWrapper<ServerNode>().eq(ServerNode::getHostId, serverNode.getHostId()));
            if (!result) {
                description.append(MessageFormat.format("DB中未查询到客户端节点. hostId:[{0}] uniqueId:[{1}]",  serverNode.getHostId(), retryTask.getUniqueId()));
            }

            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 3;
        }
    }

    /**
     * 检查是否存在存活的客户端POD
     */
    private static final class RateLimiterFilterStrategies implements FilterStrategy {

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RegisterNodeInfo serverNode = retryContext.getServerNode();
            RetryTask retryTask = retryContext.getRetryTask();

            StringBuilder description = new StringBuilder();
            Boolean result = Boolean.TRUE;
            RateLimiter rateLimiter = CacheGroupRateLimiter.getRateLimiterByKey(serverNode.getHostId());
            if (Objects.nonNull(rateLimiter) && !rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                LogUtils.error(log, "该POD:[{}]已到达最大限流阈值，本次重试不执行", serverNode.getHostId());
                description.append(MessageFormat.format("该POD:[{0}]已到达最大限流阈值，本次重试不执行.uniqueId:[{1}]", serverNode.getHostId(), retryTask.getUniqueId()));
                result = Boolean.FALSE;
            }

            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 4;
        }
    }

    /**
     * rebalance中数据不进行重试
     */
    private static final class ReBalanceFilterStrategies implements FilterStrategy {

        @Override
        public Pair<Boolean /*是否符合条件*/, StringBuilder/*描述信息*/> filter(RetryContext retryContext) {
            RetryTask retryTask = retryContext.getRetryTask();
            boolean result = !ServerNodeBalance.RE_BALANCE_ING.get();
            StringBuilder description = new StringBuilder();
            if (!result) {
                description.append(MessageFormat.format("系统Rebalancing中数据无法重试.uniqueId:[{0}]", retryTask.getUniqueId()));
            }
            return Pair.of(result, description);
        }

        @Override
        public int order() {
            return 1;
        }
    }



}
