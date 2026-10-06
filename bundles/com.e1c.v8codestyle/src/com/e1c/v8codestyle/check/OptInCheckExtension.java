/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - extension for opt-in (disabled by default) checks
 *******************************************************************************/
package com.e1c.v8codestyle.check;

import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.components.IBasicCheckExtension;

/**
 * Расширение проверки, выключенной по умолчанию: включается пользователем
 * вручную в настройках проверок. Аналог {@code activatedByDefault = false}
 * в BSL Language Server. В отличие от {@link CommonSenseCheckExtension}
 * не регистрируется в общем реестре «обычных» проверок и не включается
 * групповым переключателем.
 *
 * @author malikov-pro
 */
public class OptInCheckExtension
    implements IBasicCheckExtension
{

    /**
     * Instantiates a new opt-in check extension.
     */
    public OptInCheckExtension()
    {
        super();
    }

    @Override
    public void configureContextCollector(ICheckDefinition definition)
    {
        definition.setEnabled(false);
    }
}
