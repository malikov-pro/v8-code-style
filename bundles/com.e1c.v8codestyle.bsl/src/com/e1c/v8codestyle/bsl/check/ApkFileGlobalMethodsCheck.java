/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check 01149
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: использование методов глобального контекста для работы с файлами.
 * Фиксируются вызовы глобальных методов «ПоместитьФайл»,
 * «НачатьПомещениеФайла», «ПоместитьФайлы», «НачатьПомещениеФайлов»,
 * «ПолучитьФайл», «ПолучитьФайлы», «НачатьПолучениеФайлов» (и их английские
 * имена), а также вызов «Показать» у переменной, которой в методе присвоен
 * «Новый ДиалогВыбораФайла» (отслеживание на один уровень, как в АПК).
 * В решениях на БСП используйте программный интерфейс
 * «ФайловаяСистемаКлиент» (ЗагрузитьФайл, СохранитьФайлы, ВыбратьКаталог
 * и т.д.).
 * <p>
 * Перенос проверки АПК_01149 (статья 700 стандарта 1С, п. 1.3). Исключение
 * АПК: общие модули «ФайловаяСистемаКлиент» и
 * «ФайловаяСистемаСлужебныйКлиент» не проверяются.
 * <p>
 * Отклонение от алгоритма АПК: предусловие «в решении есть подсистема
 * ТехнологияСервиса/БСП» статически не воспроизводится, поэтому проверка
 * сделана аудит-классом — по умолчанию выключена (пользователь включает
 * её для решений на БСП).
 *
 * @author malikov-pro
 */
public class ApkFileGlobalMethodsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01149-file-global-methods"; //$NON-NLS-1$

    /** Глобальные методы работы с файлами (рус/англ, по глобальному контексту платформы). */
    private static final Set<String> FILE_GLOBAL_METHODS = Set.of(
        "ПоместитьФайл", "PutFile", //$NON-NLS-1$ //$NON-NLS-2$
        "НачатьПомещениеФайла", "BeginPutFile", //$NON-NLS-1$ //$NON-NLS-2$
        "ПоместитьФайлы", "PutFiles", //$NON-NLS-1$ //$NON-NLS-2$
        "НачатьПомещениеФайлов", "BeginPuttingFiles", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолучитьФайл", "GetFile", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолучитьФайлы", "GetFiles", //$NON-NLS-1$ //$NON-NLS-2$
        "НачатьПолучениеФайлов", "BeginGettingFiles"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Имена типа «ДиалогВыбораФайла». */
    private static final Set<String> FILE_DIALOG_TYPE_NAMES = Set.of("ДиалогВыбораФайла", "FileDialog"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Имена метода «Показать» диалога выбора файла. */
    private static final Set<String> SHOW_METHOD_NAMES = Set.of("Показать", "Show"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Общие модули, в которых обращение не проверяется (исключение АПК). */
    private static final Set<String> EXCLUDED_OWNER_MODULES =
        Set.of("ФайловаяСистемаКлиент", "ФайловаяСистемаСлужебныйКлиент"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final String KEYWORD_NEW_UPPER = "НОВЫЙ "; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkFileGlobalMethodsCheck()
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
        builder.title(Messages.ApkFileGlobalMethodsCheck_title)
            .description(Messages.ApkFileGlobalMethodsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (progressMonitor.isCanceled() || !(object instanceof Invocation invocation))
        {
            return;
        }
        FeatureAccess methodAccess = invocation.getMethodAccess();
        if (methodAccess == null || methodAccess.getName() == null || isExcludedOwnerModule(invocation))
        {
            return;
        }
        if (methodAccess instanceof StaticFeatureAccess
            && containsName(FILE_GLOBAL_METHODS, methodAccess.getName()))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ApkFileGlobalMethodsCheck_Global_file_method,
                methodAccess.getName()), methodAccess);
        }
        else if (methodAccess instanceof DynamicFeatureAccess dynamicAccess
            && containsName(SHOW_METHOD_NAMES, dynamicAccess.getName())
            && dynamicAccess.getSource() instanceof StaticFeatureAccess variable
            && isFileDialogVariable(variable, invocation))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ApkFileGlobalMethodsCheck_File_dialog_show,
                variable.getName()), methodAccess);
        }
    }

    private boolean containsName(Set<String> names, String name)
    {
        for (String candidate : names)
        {
            if (candidate.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Отслеживание на один уровень: переменная, которой в методе присвоен
     * «Новый ДиалогВыбораФайла(...».
     */
    private boolean isFileDialogVariable(StaticFeatureAccess variable, Invocation invocation)
    {
        String variableName = variable.getName();
        if (variableName == null)
        {
            return false;
        }
        Method method = EcoreUtil2.getContainerOfType(invocation, Method.class);
        if (method == null)
        {
            return false;
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
        {
            if (statement.getLeft() instanceof StaticFeatureAccess left && left.getName() != null
                && left.getName().equalsIgnoreCase(variableName) && isFileDialogCreator(statement.getRight()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isFileDialogCreator(Expression expression)
    {
        if (!(expression instanceof OperatorStyleCreator creator))
        {
            return false;
        }
        Type type = creator.getType();
        if (type != null)
        {
            String typeName = McoreUtil.getTypeName(type);
            String typeNameRu = McoreUtil.getTypeNameRu(type);
            if ((typeName != null && containsName(FILE_DIALOG_TYPE_NAMES, typeName))
                || (typeNameRu != null && containsName(FILE_DIALOG_TYPE_NAMES, typeNameRu)))
            {
                return true;
            }
        }
        // Резерв для неразрешившегося типа: по тексту конструктора «Новый ДиалогВыбораФайла(...)»
        INode node = NodeModelUtils.findActualNodeFor(creator);
        if (node == null)
        {
            return false;
        }
        String text = node.getText().strip().toUpperCase(Locale.ROOT);
        if (text.startsWith(KEYWORD_NEW_UPPER))
        {
            text = text.substring(KEYWORD_NEW_UPPER.length()).strip();
        }
        return text.startsWith("ДИАЛОГВЫБОРАФАЙЛА(") || text.startsWith("FILEDIALOG("); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Исключение АПК: общие модули «ФайловаяСистемаКлиент» и
     * «ФайловаяСистемаСлужебныйКлиент» не проверяются.
     */
    private boolean isExcludedOwnerModule(EObject context)
    {
        Module module = EcoreUtil2.getContainerOfType(context, Module.class);
        if (module == null || module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return false;
        }
        if (module.getOwner() instanceof MdObject owner && owner.getName() != null)
        {
            return containsName(EXCLUDED_OWNER_MODULES, owner.getName());
        }
        return false;
    }
}
