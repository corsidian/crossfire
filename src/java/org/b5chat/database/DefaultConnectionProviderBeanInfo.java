/**
 * $RCSfile$
 * $Revision: 11608 $
 * $Date: 2010-02-07 16:03:12 -0500 (Sun, 07 Feb 2010) $
 *
 * Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
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

package org.b5chat.database;

import org.b5chat.util.GlobalBeanInfo;

/**
 * BeanInfo class for the DefaultConnectionProvider class.
 *
 * @author B5Chat Community
 */
public class DefaultConnectionProviderBeanInfo extends GlobalBeanInfo {

    public static final String[] PROPERTY_NAMES = {
        "driver",
        "serverURL",
        "username",
        "password",
        "minConnections",
        "maxConnections",
        "connectionTimeout"
    };

    public DefaultConnectionProviderBeanInfo() {
        super();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.b5chat.util.B5ChatBeanInfo#getBeanClass()
	 */
    @Override
    public Class getBeanClass() {
        return org.b5chat.database.DefaultConnectionProvider.class;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.b5chat.util.B5ChatBeanInfo#getPropertyNames()
	 */
    @Override
    public String[] getPropertyNames() {
        return PROPERTY_NAMES;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.b5chat.util.B5ChatBeanInfo#getName()
	 */
    @Override
    public String getName() {
        return "DefaultConnectionProvider";
    }
}