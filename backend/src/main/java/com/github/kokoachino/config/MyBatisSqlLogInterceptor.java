package com.github.kokoachino.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 精简 MyBatis 日志：
 * 1. DEBUG 输出实际 SQL（已替换参数）
 * 2. INFO 输出执行结果概要（行数 + 耗时）
 *
 * @author kokoachino
 * @date 2026-03-14
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
})
public class MyBatisSqlLogInterceptor implements Interceptor {

    private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final int SQL_LOG_MAX_LENGTH = 4000;
    private static final String SQL_LOG_TRUNCATED_SUFFIX = " ...[truncated]";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameterObject = args.length > 1 ? args[1] : null;
        BoundSql boundSql = args.length == 6 ? (BoundSql) args[5] : mappedStatement.getBoundSql(parameterObject);
        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (log.isDebugEnabled()) {
            String executableSql = buildExecutableSql(mappedStatement.getConfiguration(), boundSql, parameterObject);
            log.debug("MyBatis SQL | id={} | type={} | sql={}",
                    mappedStatement.getId(), commandType, truncateSql(executableSql));
        }
        long startNanos = System.nanoTime();
        try {
            Object result = invocation.proceed();
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("MyBatis Result | id={} | type={} | {} | costMs={}",
                    mappedStatement.getId(), commandType, buildSummary(commandType, result), costMs);
            return result;
        } catch (Throwable throwable) {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("MyBatis Result | id={} | type={} | status=FAILED | costMs={}",
                    mappedStatement.getId(), commandType, costMs, throwable);
            throw throwable;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {}

    private String buildSummary(SqlCommandType commandType, Object result) {
        if (SqlCommandType.SELECT.equals(commandType)) {
            return "rows=" + resolveQueryRows(result) + " | status=OK";
        }
        int affectedRows = result instanceof Number ? ((Number) result).intValue() : -1;
        return "affectedRows=" + affectedRows + " | status=OK";
    }

    private int resolveQueryRows(Object result) {
        if (result == null) {
            return 0;
        }
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        return 1;
    }

    private String buildExecutableSql(Configuration configuration, BoundSql boundSql, Object parameterObject) {
        String sql = WHITE_SPACE_PATTERN.matcher(boundSql.getSql()).replaceAll(" ").trim();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            return sql;
        }
        List<String> formattedParameters = resolveParameters(configuration, boundSql, parameterObject, parameterMappings);
        String executableSql = sql;
        for (String formattedParameter : formattedParameters) {
            executableSql = executableSql.replaceFirst("\\?", Matcher.quoteReplacement(formattedParameter));
        }
        return executableSql;
    }

    private List<String> resolveParameters(Configuration configuration,
                                           BoundSql boundSql,
                                           Object parameterObject,
                                           List<ParameterMapping> parameterMappings) {
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
        List<String> parameters = new ArrayList<>();
        for (ParameterMapping parameterMapping : parameterMappings) {
            if (parameterMapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            String propertyName = parameterMapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else if (metaObject != null && metaObject.hasGetter(propertyName)) {
                value = metaObject.getValue(propertyName);
            } else {
                value = null;
            }
            parameters.add(formatParameterValue(value));
        }
        return parameters;
    }

    private String formatParameterValue(Object value) {
        return switch (value) {
            case null -> "NULL";
            case Number ignored -> value.toString();
            case Boolean boolValue -> boolValue ? "1" : "0";
            case Date dateValue -> "'" + DATE_FORMATTER.get().format(dateValue) + "'";
            case TemporalAccessor ignored1 -> "'" + value + "'";
            case byte[] bytes -> "'[BINARY " + bytes.length + " bytes]'";
            default -> "'" + value.toString().replace("'", "''") + "'";
        };
    }

    private String truncateSql(String sql) {
        if (sql.length() <= SQL_LOG_MAX_LENGTH) {
            return sql;
        }
        return sql.substring(0, SQL_LOG_MAX_LENGTH - SQL_LOG_TRUNCATED_SUFFIX.length()) + SQL_LOG_TRUNCATED_SUFFIX;
    }
}
