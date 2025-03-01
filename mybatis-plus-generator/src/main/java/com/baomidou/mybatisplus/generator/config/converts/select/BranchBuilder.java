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
package com.baomidou.mybatisplus.generator.config.converts.select;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 分支构建者
 *
 * @author hanchunlin
 * Created at 2020/6/11 17:22
 */
public interface BranchBuilder<P, T> {

    /**
     * 使用一个值工厂构造出一个分支
     *
     * @param factory 值工厂
     * @return 返回分支
     */
    Branch<P, T> then(Function<P, T> factory);

    /**
     * 从值构建出一个分支
     *
     * @param value 值
     * @return 返回一个分支
     */
    default Branch<P, T> then(T value) {
        return then(p -> value);
    }

    /**
     * 工厂函数，用于创建分支构建者
     *
     * @param tester 测试器
     * @param <P>    参数类型
     * @param <T>    返回值类型
     * @return 返回一个分支创建者
     */
    static <P, T> BranchBuilder<P, T> of(Predicate<P> tester) {
        return factory -> Branch.of(tester, factory);
    }

}
