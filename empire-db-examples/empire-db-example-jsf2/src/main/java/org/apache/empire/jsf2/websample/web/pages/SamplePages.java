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
package org.apache.empire.jsf2.websample.web.pages;

import org.apache.empire.jsf2.pages.PageDefinition;
import org.apache.empire.jsf2.pages.PageDefinitions;

public final class SamplePages extends PageDefinitions
{
    // *Deprecated* private static final long serialVersionUID = 1L;

    public static final PageDefinition LoginPage          = new PageDefinition("/pages/loginPage.xhtml", LoginPage.class);

    public static final PageDefinition EmployeeListPage   = new PageDefinition("/pages/employeeListPage.xhtml", EmployeeListPage.class, SamplePages.LoginPage);
    public static final PageDefinition EmployeeDetailPage = new PageDefinition("/pages/employeeDetailPage.xhtml", EmployeeDetailPage.class, SamplePages.EmployeeListPage);

}