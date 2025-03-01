/*
 * Copyright (c) 2011-2025, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.extension.conditions.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.AbstractChainWrapper;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author miemie
 * @since 2018-12-19
 */
@SuppressWarnings({"serial"})
public class LambdaQueryChainWrapper<T> extends AbstractChainWrapper<T, SFunction<T, ?>, LambdaQueryChainWrapper<T>, LambdaQueryWrapper<T>>
    implements ChainQuery<T>, Query<LambdaQueryChainWrapper<T>, T, SFunction<T, ?>> {

    private final BaseMapper<T> baseMapper;

    public LambdaQueryChainWrapper(BaseMapper<T> baseMapper) {
        super();
        this.baseMapper = baseMapper;
        super.wrapperChildren = new LambdaQueryWrapper<>();
    }

    public LambdaQueryChainWrapper(Class<T> entityClass) {
        super();
        this.baseMapper = null;
        super.wrapperChildren = new LambdaQueryWrapper<>(entityClass);
    }

    public LambdaQueryChainWrapper(BaseMapper<T> baseMapper, T entity) {
        super();
        this.baseMapper = baseMapper;
        super.wrapperChildren = new LambdaQueryWrapper<>(entity);
    }

    public LambdaQueryChainWrapper(BaseMapper<T> baseMapper, Class<T> entityClass) {
        super();
        this.baseMapper = baseMapper;
        super.wrapperChildren = new LambdaQueryWrapper<>(entityClass);
    }

    public LambdaQueryChainWrapper(BaseMapper<T> baseMapper, LambdaQueryWrapper<T> wrapperChildren) {
        super();
        this.baseMapper = baseMapper;
        super.wrapperChildren = wrapperChildren;
    }

    @Override
    public LambdaQueryChainWrapper<T> select(boolean condition, List<SFunction<T, ?>> columns) {
        return doSelect(condition, columns);
    }

    @Override
    @SafeVarargs
    public final LambdaQueryChainWrapper<T> select(SFunction<T, ?>... columns) {
        return doSelect(true, CollectionUtils.toList(columns));
    }

    @Override
    @SafeVarargs
    public final LambdaQueryChainWrapper<T> select(boolean condition, SFunction<T, ?>... columns) {
        return doSelect(condition, CollectionUtils.toList(columns));
    }

    /**
     * @since 3.5.4
     */
    protected LambdaQueryChainWrapper<T> doSelect(boolean condition, List<SFunction<T, ?>> columns) {
        wrapperChildren.select(condition, columns);
        return typedThis;
    }

    @Override
    public LambdaQueryChainWrapper<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        wrapperChildren.select(entityClass, predicate);
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        throw ExceptionUtils.mpe("can not use this method for \"%s\"", "getSqlSelect");
    }

    @Override
    public BaseMapper<T> getBaseMapper() {
        return baseMapper;
    }

    @Override
    public Class<T> getEntityClass() {
        return super.wrapperChildren.getEntityClass();
    }
}
