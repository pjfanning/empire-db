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
package org.apache.empire.db;

import java.util.Collection;
import java.util.Set;

import org.apache.empire.commons.ObjectUtils;
import org.apache.empire.commons.OptionEntry;
// java
import org.apache.empire.data.DataType;
import org.apache.empire.dbms.DBMSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstract class is the base class for all database expression classes (e.g. DBAliasExpr or DBCalsExpr)
 * <P>
 * 
 *
 */
public abstract class DBExpr extends DBObject
{
    // *Deprecated* private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DBExpr.class);
  
    // SQL Context Flags
    public static final long CTX_DEFAULT       = 7;  // Default: FullyQualified + Value
    public static final long CTX_ALL           = 15; // All Flags set (except exclusions)

    public static final long CTX_NAME          = 1;  // Unqualified Name
    public static final long CTX_FULLNAME      = 2;  // Fully Qualified Name
    public static final long CTX_VALUE         = 4;  // Value Only
    public static final long CTX_ALIAS         = 8;  // Rename expression
    public static final long CTX_NOPARENTHESES = 16; // No Parentheses
    
    /**
     * Used to build the SQL command. SQL for this expression must be appended to StringBuilder.
     * 
     * @param buf the string buffer used to build the sql command
     * @param context context flag for this SQL-Command (see CTX_??? constants).
     */
    public abstract void addSQL(StringBuilder buf, long context);

    /**
     * Internal function to obtain all DBColumnExpr-objects used by this expression. 
     * 
     * @param list list to which all used column expressions must be added
     */
    public abstract void addReferencedColumns(Set<DBColumn> list);

    /**
     * Returns the sql representation of a value.
     * 
     * @param dataType the DataType
     * @param value an DBExpr object, array or a basis data type(e.g. int, String)
     * @param context the context of the DBColumnExpr object
     * @param arraySep the separator value
     * @return the new SQL-Command
     */
    protected String getObjectValue(DataType dataType, Object value, long context, String arraySep)
    {
        // it's an Object
        if (value instanceof DBExpr)
        { // it's an expression
            StringBuilder buf = new StringBuilder();
            ((DBExpr) value).addSQL(buf, context);
            return buf.toString();
        } 
        // check option entry
        if (value instanceof OptionEntry)
        {   // option value
            value = ((OptionEntry)value).getValue();
        }
        // check enum
        if (value instanceof Enum<?>)
        {   // check enum
            value = ObjectUtils.getEnumValue((Enum<?>)value, dataType.isNumeric());
        }
        else if (value instanceof Collection<?>)
        {   // collection 2 array
        	value = ((Collection<?>)value).toArray();
        }
        // Check whether it is an array
        if (value!=null && value.getClass().isArray())
        {
            StringBuilder buf = new StringBuilder();
            // An Array of Objects
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++)
            { // Array Separator
                if (i > 0 && arraySep != null)
                    buf.append(arraySep);
                // Append Value
                buf.append(getObjectValue(dataType, array[i], context, arraySep));
            }
            return buf.toString();
        } 
        else
        {   // Scalar Value from DB
            DBMSHandler dbms = getDatabase().getDbms();
            if (dbms==null)
            {   // Convert to String
                log.warn("No dbms set for getting object value. Using default!");
                return String.valueOf(value);
            }
            // Get Value Expression from dmbs
            return dbms.getValueString(value, dataType);
        }
    }    
}