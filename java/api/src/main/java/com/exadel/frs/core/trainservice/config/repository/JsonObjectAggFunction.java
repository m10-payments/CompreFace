package com.exadel.frs.core.trainservice.config.repository;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.HashMap;
import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.Type;

public class JsonObjectAggFunction extends StandardSQLFunction {

    public JsonObjectAggFunction() {
        super(
                "json_object_agg",
                new JsonBinaryType(HashMap.class)
        );
    }

    @Override
    public String render(Type firstArgumentType, List arguments, org.hibernate.engine.spi.SessionFactoryImplementor factory) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("The json_object_agg function must be passed exactly 2 arguments");
        }
        // see https://dba.stackexchange.com/questions/241541/json-object-agg-errors-on-null-in-field-name
        return String.format(
                "coalesce(json_object_agg(%s, %s) FILTER (WHERE %s IS NOT NULL), '{}'::json)",
                arguments.get(0),
                arguments.get(1),
                arguments.get(0)
        );
    }
}
