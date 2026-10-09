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
 *     malikov-pro - implementation of the upstream issue 1C-Company/v8-code-style#757
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверяет, что блокировка данных, созданная оператором «Новый
 * БлокировкаДанных» / «New DataLock» и заполненная элементами через
 * «Добавить()» / «Add()», где-то в том же методе блокируется вызовом
 * «Заблокировать()» / «Lock()». Созданная, но ни разу не заблокированная
 * блокировка не защищает данные от конкурентного изменения.
 * <p>
 * Отслеживание лексическое, в пределах одного метода: переменной присвоен
 * конструктор блокировки, у переменной есть вызов «Добавить()», и в методе
 * нет ни одного вызова «Заблокировать()» у этой переменной. Если блокировка
 * передана в другой метод, который её блокирует, замечание всё равно
 * выдаётся. Требование «вызов Заблокировать() должен быть в попытке»
 * проверяется отдельной проверкой «Метод Заблокировать() вне блока
 * Попытка-Исключение» (lock-out-of-try). Случаи, когда конструктор не
 * присваивается локальной переменной, не отслеживаются.
 * <p>
 * Реализация замечания из issue 1C-Company/v8-code-style#757
 * (стандарт 499, п. 1.3).
 *
 * @author malikov-pro
 */
public class LockWithoutLockCallCheck
    extends BasicCheck
{

    /** Идентификатор проверки (по номеру issue апстрима). */
    public static final String CHECK_ID = "up-757-lock-without-lock-call"; //$NON-NLS-1$

    private static final String NAME_DATA_LOCK = "DataLock"; //$NON-NLS-1$
    private static final String NAME_ADD = "Add"; //$NON-NLS-1$
    private static final String NAME_ADD_RU = "Добавить"; //$NON-NLS-1$
    private static final String NAME_LOCK = "Lock"; //$NON-NLS-1$
    private static final String NAME_LOCK_RU = "Заблокировать"; //$NON-NLS-1$

    private static final Pattern PATTERN_DATA_LOCK_TYPE = Pattern
        .compile("\\b(?:Новый|New)\\s+(БлокировкаДанных\\w*|DataLock)\\b", //$NON-NLS-1$
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Instantiates a new check.
     */
    public LockWithoutLockCallCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.LockWithoutLockCall_title)
            .description(Messages.LockWithoutLockCall_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Method method))
        {
            return;
        }

        List<DynamicFeatureAccess> featureAccesses = EcoreUtil2.getAllContentsOfType(method, DynamicFeatureAccess.class);

        for (OperatorStyleCreator creator : EcoreUtil2.getAllContentsOfType(method, OperatorStyleCreator.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!isDataLockCreator(creator))
            {
                continue;
            }
            String variableName = variableNameOfStatement(creator);
            if (variableName == null)
            {
                continue;
            }
            if (hasMethodCallOnVariable(featureAccesses, variableName, NAME_ADD, NAME_ADD_RU)
                && !hasMethodCallOnVariable(featureAccesses, variableName, NAME_LOCK, NAME_LOCK_RU))
            {
                resultAceptor.addIssue(Messages.LockWithoutLockCall_Lock_method_is_not_called, creator);
            }
        }
    }

    /**
     * Конструктор создаёт «БлокировкаДанных» / «DataLock»: по разрешённому
     * типу либо — если тип не разрешился — по тексту «Новый &lt;Тип&gt;».
     */
    private static boolean isDataLockCreator(OperatorStyleCreator creator)
    {
        if (creator.getType() != null && NAME_DATA_LOCK.equals(McoreUtil.getTypeName(creator.getType())))
        {
            return true;
        }
        String text = NodeModelUtils.findActualNodeFor(creator).getText();
        Matcher matcher = PATTERN_DATA_LOCK_TYPE.matcher(text);
        return matcher.find();
    }

    /**
     * Имя локальной переменной оператора
     * {@code Блокировка = Новый БлокировкаДанных}.
     */
    private static String variableNameOfStatement(OperatorStyleCreator creator)
    {
        EObject ancestor = creator.eContainer();
        while (ancestor != null && !(ancestor instanceof Statement))
        {
            ancestor = ancestor.eContainer();
        }
        if (ancestor instanceof SimpleStatement statement && statement.getLeft() instanceof StaticFeatureAccess left)
        {
            return left.getName();
        }
        return null;
    }

    /**
     * В методе есть вызов {@code <переменная>.<метод>()}: вызов метода с
     * одним из указанных имён у переменной с данным именем.
     */
    private static boolean hasMethodCallOnVariable(List<DynamicFeatureAccess> featureAccesses, String variableName,
        String... methodNames)
    {
        for (DynamicFeatureAccess featureAccess : featureAccesses)
        {
            if (BslUtil.getInvocation(featureAccess) == null)
            {
                continue;
            }
            String name = featureAccess.getName();
            boolean nameMatches = false;
            for (String methodName : methodNames)
            {
                if (methodName.equalsIgnoreCase(name))
                {
                    nameMatches = true;
                    break;
                }
            }
            if (!nameMatches)
            {
                continue;
            }
            if (featureAccess.getSource() instanceof StaticFeatureAccess source
                && source.getName().equalsIgnoreCase(variableName))
            {
                return true;
            }
        }
        return false;
    }

}
