package com.exadel.frs.core.trainservice.config.repository;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.HashMap;
import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.Type;

public class JsonObjectAggFunction extends StandardSQLFunction {

    public static final String JSON_OBJECT_AGG = "json_object_agg";

    public JsonObjectAggFunction() {
        super(
                JSON_OBJECT_AGG,
                new JsonBinaryType(HashMap.class)
        );
    }

    @Override
    public String render(Type firstArgumentType, List arguments, org.hibernate.engine.spi.SessionFactoryImplementor factory) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(String.format("The %s function must be passed exactly 2 arguments", JSON_OBJECT_AGG));
        }
        // see https://dba.stackexchange.com/questions/241541/json-object-agg-errors-on-null-in-field-name
        return String.format(
                "coalesce(%s(%s, %s) FILTER (WHERE %s IS NOT NULL), '{}'::json)",
                JSON_OBJECT_AGG,
                arguments.get(0),
                arguments.get(1),
                arguments.get(0)
        );
    }
}
