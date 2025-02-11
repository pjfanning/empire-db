/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.dbms.derby;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBTableColumn;

public class DerbyDDLGenerator extends DBDDLGenerator<DBMSHandlerDerby>
{
    public DerbyDDLGenerator(DBMSHandlerDerby dbms)
    {
        super(dbms);
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets Derby specific data types
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_BOOLEAN = "SMALLINT";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
            { // Auto increment
                super.appendColumnDataType(type, size, c, sql);
                if (!dbms.isUseSequenceTable())
                    sql.append(" GENERATED ALWAYS AS IDENTITY");
                break;
            }
            default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
    
    @Override
    protected void appendColumnDesc(DBTableColumn c, boolean alter, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        if (alter) {
            sql.append(" SET DATA TYPE ");
        } else {
            sql.append(" ");
        }
        // Unknown data type
        if (!appendColumnDataType(c.getDataType(), c.getSize(), c, sql))
            return;
        // Default Value
        if (isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(dbms.getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
    }
    
}
