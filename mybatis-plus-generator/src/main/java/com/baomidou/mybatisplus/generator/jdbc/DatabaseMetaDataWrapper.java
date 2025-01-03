/*
 * Copyright (c) 2011-2024, baomidou (jobob@qq.com).
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
package com.baomidou.mybatisplus.generator.jdbc;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 数据库数据元包装类
 *
 * @author nieqiurong 2021/2/8.
 * @since 3.5.0
 */
public class DatabaseMetaDataWrapper {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetaDataWrapper.class);

    @Getter
    private final Connection connection;

    private final DatabaseMetaData databaseMetaData;

    //TODO 暂时只支持一种
    private final String catalog;

    //TODO 暂时只支持一种
    private final String schema;

    public DatabaseMetaDataWrapper(Connection connection, String schemaName) {
        try {
            if (null == connection) {
                throw new RuntimeException("数据库连接不能为空");
            }
            this.connection = connection;
            this.databaseMetaData = connection.getMetaData();
            this.catalog = connection.getCatalog();
            this.schema = schemaName;
        } catch (SQLException e) {
            throw new RuntimeException("获取元数据错误:", e);
        }
    }

    public void closeConnection() {
        Optional.ofNullable(connection).ifPresent((con) -> {
            try {
                con.close();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        });
    }

    public Map<String, Column> getColumnsInfo(String tableNamePattern, boolean queryPrimaryKey) {
        return getColumnsInfo(this.catalog, this.schema, tableNamePattern, queryPrimaryKey);
    }

    public List<DatabaseMetaDataWrapper.Index> getIndex(String tableName) {
        List<DatabaseMetaDataWrapper.Index> indexList = new ArrayList<>();
        try (ResultSet resultSet = databaseMetaData.getIndexInfo(catalog, schema, tableName, false, false)) {
            while (resultSet.next()) {
                Index index = new Index(resultSet);
                // skip function index
                if (StringUtils.isNotBlank(index.getColumnName())) {
                    indexList.add(new Index(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("读取索引信息:" + tableName + "错误:", e);
        }
        return indexList;
    }

    /**
     * 获取表字段信息
     *
     * @return 表字段信息 (小写字段名->字段信息)
     */
    public Map<String, Column> getColumnsInfo(String catalog, String schema, String tableName, boolean queryPrimaryKey) {
        Set<String> primaryKeys = new HashSet<>();
        if (queryPrimaryKey) {
            try (ResultSet primaryKeysResultSet = databaseMetaData.getPrimaryKeys(catalog, schema, tableName)) {
                while (primaryKeysResultSet.next()) {
                    String columnName = primaryKeysResultSet.getString("COLUMN_NAME");
                    primaryKeys.add(columnName);
                }
                if (primaryKeys.size() > 1) {
                    logger.warn("当前表:{}，存在多主键情况！", tableName);
                }
            } catch (SQLException e) {
                throw new RuntimeException("读取表主键信息:" + tableName + "错误:", e);
            }
        }
        Map<String, Column> columnsInfoMap = new LinkedHashMap<>();
        try (ResultSet resultSet = databaseMetaData.getColumns(catalog, schema, tableName, "%")) {
            while (resultSet.next()) {
                Column column = new Column();
                String name = resultSet.getString("COLUMN_NAME");
                column.name = name;
                column.primaryKey = primaryKeys.contains(name);
                column.typeName = resultSet.getString("TYPE_NAME");
                int dataType = resultSet.getInt("DATA_TYPE");
                JdbcType jdbcType = JdbcType.forCode(dataType);
                if (jdbcType == null) {
                    // 不标准的类型,统一转为OTHER
                    jdbcType = JdbcType.OTHER;
                }
                column.jdbcType = jdbcType;
                column.length = resultSet.getInt("COLUMN_SIZE");
                column.scale = resultSet.getInt("DECIMAL_DIGITS");
                column.remarks = formatComment(resultSet.getString("REMARKS"));
                column.defaultValue = resultSet.getString("COLUMN_DEF");
                column.nullable = resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                column.generatedColumn = isGeneratedOrAutoIncrementColumn(resultSet, "IS_GENERATEDCOLUMN");
                column.autoIncrement = isGeneratedOrAutoIncrementColumn(resultSet, "IS_AUTOINCREMENT");
                columnsInfoMap.put(name.toLowerCase(), column);
            }
            return Collections.unmodifiableMap(columnsInfoMap);
        } catch (SQLException e) {
            throw new RuntimeException("读取表字段信息:" + tableName + "错误:", e);
        }
    }

    private boolean isGeneratedOrAutoIncrementColumn(ResultSet resultSet, String columnLabel) {
        try {
            return "YES".equals(resultSet.getString(columnLabel));
        } catch (SQLException e) {
            // ignore
        }
        return false;
    }

    public String formatComment(String comment) {
        return StringUtils.isBlank(comment) ? StringPool.EMPTY : comment.replaceAll("\r\n", "\t");
    }

    public Table getTableInfo(String tableName) {
        return getTableInfo(this.catalog, this.schema, tableName);
    }

    public List<Table> getTables(String tableNamePattern, String[] types) {
        return getTables(this.catalog, this.schema, tableNamePattern, types);
    }

    public List<Table> getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) {
        List<Table> tables = new ArrayList<>();
        try (ResultSet resultSet = databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types)) {
            Table table;
            while (resultSet.next()) {
                table = new Table();
                table.name = resultSet.getString("TABLE_NAME");
                table.remarks = formatComment(resultSet.getString("REMARKS"));
                table.tableType = resultSet.getString("TABLE_TYPE");
                tables.add(table);
            }
        } catch (SQLException e) {
            throw new RuntimeException("读取数据库表信息出现错误", e);
        }
        return tables;
    }

    public Table getTableInfo(String catalog, String schema, String tableName) {
        Table table = new Table();
        //TODO 后面要根据表是否为视图来查询，后面重构表查询策略。
        try (ResultSet resultSet = databaseMetaData.getTables(catalog, schema, tableName, new String[]{"TABLE", "VIEW"})) {
            table.name = tableName;
            while (resultSet.next()) {
                table.remarks = formatComment(resultSet.getString("REMARKS"));
                table.tableType = resultSet.getString("TABLE_TYPE");
            }
        } catch (SQLException e) {
            throw new RuntimeException("读取表信息:" + tableName + "错误:", e);
        }
        return table;
    }

    @Getter
    public static class Table {

        private String name;

        private String remarks;

        private String tableType;

        public boolean isView() {
            return "VIEW".equals(tableType);
        }

    }

    @Getter
    public static class Column {

        private boolean primaryKey;

        private boolean autoIncrement;

        private String name;

        private int length;

        private boolean nullable;

        private String remarks;

        private String defaultValue;

        private int scale;

        private JdbcType jdbcType;

        @Setter
        private String typeName;

        private boolean generatedColumn;

    }

    @Getter
    @ToString
    public static class Index {

        /**
         * 索引名
         */
        private final String name;

        /**
         * 是否唯一索引
         */
        private final boolean unique;

        /**
         * 索引字段
         */
        private final String columnName;

        /**
         * 排序方式 (A OR D OR null)
         */
        private final String ascOrDesc;

        private final int cardinality;

        private final int ordinalPosition;

        public Index(ResultSet resultSet) throws SQLException {
            this.unique = !resultSet.getBoolean("NON_UNIQUE");
            this.name = resultSet.getString("INDEX_NAME");
            this.columnName = resultSet.getString("COLUMN_NAME");
            this.ascOrDesc = resultSet.getString("ASC_OR_DESC");
            this.cardinality = resultSet.getInt("CARDINALITY");
            this.ordinalPosition = resultSet.getInt("ORDINAL_POSITION");
        }

    }
}
