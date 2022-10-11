package com.lordnoisy.swanseaauthenticator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public class DataSource {
    private HikariConfig config = new HikariConfig();
    private HikariDataSource dataSource;

    public DataSource(String url, String user, String password) {
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaxLifetime(900000);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        dataSource = new HikariDataSource( config );
    }

    public Connection getDatabaseConnection() throws SQLException {
        return dataSource.getConnection();
    }


}