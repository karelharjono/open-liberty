/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer.test.mdb;

import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.cci.Record;

public class NoMethodIntBeanParent {

    private final static Logger svLogger = Logger.getLogger("NoMethodIntBeanParent");

    public Record REMOVE(Record record) throws ResourceException {
        svLogger.info("NoMethodIntBean.REMOVE record = " + record);
        return record;
    }
}
