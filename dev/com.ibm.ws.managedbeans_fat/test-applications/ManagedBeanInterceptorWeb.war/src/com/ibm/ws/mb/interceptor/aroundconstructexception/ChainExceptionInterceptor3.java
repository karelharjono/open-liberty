/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
package com.ibm.ws.mb.interceptor.aroundconstructexception;

import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.aroundconstructexception.ChainExceptionManagedBean.ChainExceptionTestType;

/**
 *
 */
public class ChainExceptionInterceptor3 {

    private static final String CLASS_NAME = ChainExceptionInterceptor3.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) throws ConstructorException, InterceptorException {
        try {
            ChainExceptionTestType testType = ChainExceptionManagedBean.getTestType();

            Object o = null;

            svLogger.info("Chain 3 before proceed");
            switch (testType) {
                case CHAIN3THROWNEW:
                    try {
                        o = inv.proceed();
                        // WELD calls all interceptors on recovery; does not resume where left off like WAS
                        // fail("Constructor did not throw exception");
                    } catch (Exception e) {
                        throw new InterceptorException();
                    }
                    break;
                case CHAIN1RECOVER:
                default:
                    inv.proceed();

                    // WELD calls all interceptors on recovery; does not resume where left off like WAS
                    // fail("Chain3 after .proceed() reached after exception was thrown up to Chain1 and it recovered");

                    break;
            }
            svLogger.info("Chain 3 after proceed");

            return o;

        } catch (ConstructorException ce) {
            throw ce;
        } catch (InterceptorException ie) {
            throw ie;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }
}
