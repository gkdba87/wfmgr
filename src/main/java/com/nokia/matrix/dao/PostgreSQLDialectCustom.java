package com.nokia.matrix.dao;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author 1226211
 * 
 * Custom Postgre dialect to register blob type
 *
 */
public class PostgreSQLDialectCustom extends PostgreSQL82Dialect{

public PostgreSQLDialectCustom()
{
    super();
    registerColumnType(Types.BLOB, "bytea");
}

 @Override
 public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
    if (Types.CLOB == sqlTypeDescriptor.getSqlType()) {
      return LongVarcharTypeDescriptor.INSTANCE;
    }
    return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
  }
}