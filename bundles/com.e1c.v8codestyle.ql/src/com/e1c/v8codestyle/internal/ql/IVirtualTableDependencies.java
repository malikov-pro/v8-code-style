/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.ql;

/** Lifecycle registration for current-project virtual-table dependency tracking. */
public interface IVirtualTableDependencies
{
    String SERVICE_NAME = "V8CodeStyleVirtualTableDependencies"; //$NON-NLS-1$

    /** Attach projects already loaded before this service was registered. */
    void start();

    /** Release all project listeners when the bundle stops. */
    void stop();
}
