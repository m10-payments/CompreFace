package com.exadel.frs.commonservice.config;

import org.hibernate.dialect.PostgreSQL10Dialect;

public class CustomizedPostgreSQL10Dialect extends PostgreSQL10Dialect {

    public CustomizedPostgreSQL10Dialect() {
        super();
        this.registerFunction(JsonObjectAggFunction.JSON_OBJECT_AGG, new JsonObjectAggFunction());
    }
}
