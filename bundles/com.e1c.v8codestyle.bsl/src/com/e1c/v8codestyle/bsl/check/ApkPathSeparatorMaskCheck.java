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
 *     malikov-pro - port of the APK check 01167 (path separator and all-files mask)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.OPERATOR_STYLE_CREATOR;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: разделитель пути и маска всех файлов не указываются вручную.
 * <p>
 * Перенос проверки АПК_01167 (статья 723 стандарта 1С, п. 2.5.2). В
 * литеральных строковых параметрах методов поиска файлов фиксируются:
 * <ul>
 * <li>маска всех файлов «*.*» — используйте функцию
 * «ПолучитьМаскуВсеФайлы» (GetAllFilesMask);</li>
 * <li>разделитель пути «\» или «/» — используйте функцию
 * «ПолучитьРазделительПути» (GetPathSeparator).</li>
 * </ul>
 * Проверяются вызовы «НайтиФайлы»/«FindFiles» (параметры 1 и 2),
 * «НачатьПоискФайлов»/«BeginFindingFiles» (параметры 2 и 3) и конструктор
 * «Новый Файл»/«New File» (параметр 1) — как в алгоритме АПК, где поиск по
 * тексту «НАЙТИФАЙЛЫ(», «НАЧАТЬПОИСКФАЙЛОВ(» и «НОВЫЙФАЙЛ(» ведётся после
 * удаления пробелов. Замечание — одно на каждый нарушающий параметр; маска
 * проверяется первой, как в АПК.
 * <p>
 * Реализация — AST (Invocation/OperatorStyleCreator с известными именами и
 * литеральными строковыми аргументами). Отклонение от алгоритма АПК:
 * параметры-переменные и выражения не проверяются — текстовый поиск АПК
 * помечал и вхождения разделителя в неконтролируемых выражениях, для AST
 * подхода это ложные срабатывания. В решениях на БСП используйте функции
 * общих модулей ОбщегоНазначения и ОбщегоНазначенияКлиент.
 *
 * @author malikov-pro
 */
public class ApkPathSeparatorMaskCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01167-path-separator-mask"; //$NON-NLS-1$

    /** Методы поиска файлов: параметры 1 и 2 (каталог, маска), рус/англ. */
    private static final Set<String> FIND_FILES_METHODS = Set.of("НайтиФайлы", "FindFiles"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Асинхронные методы поиска файлов: параметры 2 и 3 (каталог, маска), рус/англ. */
    private static final Set<String> BEGIN_FIND_FILES_METHODS = Set.of("НачатьПоискФайлов", "BeginFindingFiles"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Имена типа «Файл» для конструктора «Новый Файл». */
    private static final Set<String> FILE_TYPE_NAMES = Set.of("Файл", "File"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final String ALL_FILES_MASK = "*.*"; //$NON-NLS-1$

    private static final String KEYWORD_NEW_UPPER_RU = "НОВЫЙ "; //$NON-NLS-1$
    private static final String KEYWORD_NEW_UPPER_EN = "NEW "; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkPathSeparatorMaskCheck()
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
        builder.title(Messages.ApkPathSeparatorMaskCheck_title)
            .description(Messages.ApkPathSeparatorMaskCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION, OPERATOR_STYLE_CREATOR);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (progressMonitor.isCanceled())
        {
            return;
        }
        if (object instanceof Invocation invocation)
        {
            checkInvocation(invocation, resultAceptor);
        }
        else if (object instanceof OperatorStyleCreator creator)
        {
            checkFileCreator(creator, resultAceptor);
        }
    }

    private void checkInvocation(Invocation invocation, ResultAcceptor resultAceptor)
    {
        FeatureAccess methodAccess = invocation.getMethodAccess();
        if (methodAccess == null || methodAccess.getName() == null)
        {
            return;
        }
        List<Integer> paramIndexes;
        if (containsName(FIND_FILES_METHODS, methodAccess.getName()))
        {
            paramIndexes = List.of(0, 1);
        }
        else if (containsName(BEGIN_FIND_FILES_METHODS, methodAccess.getName()))
        {
            paramIndexes = List.of(1, 2);
        }
        else
        {
            return;
        }
        List<Expression> params = invocation.getParams();
        for (Integer index : paramIndexes)
        {
            checkParam(params, index, resultAceptor);
        }
    }

    private void checkFileCreator(OperatorStyleCreator creator, ResultAcceptor resultAceptor)
    {
        if (!isFileCreator(creator))
        {
            return;
        }
        checkParam(creator.getParams(), 0, resultAceptor);
    }

    /**
     * Проверяет литеральный строковый параметр: маска «*.*» — ошибка маски,
     * иначе разделитель «\» или «/» в тексте — ошибка разделителя пути.
     */
    private void checkParam(List<Expression> params, int index, ResultAcceptor resultAceptor)
    {
        if (index >= params.size() || !(params.get(index) instanceof StringLiteral literal))
        {
            return;
        }
        // строки литерала хранятся вместе с кавычками (см. IsInRoleCheck) — снимаем их для сравнения
        String text = String.join("\n", literal.getLines()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (ALL_FILES_MASK.equals(text.strip()))
        {
            resultAceptor.addIssue(Messages.ApkPathSeparatorMaskCheck_Manual_all_files_mask, literal);
        }
        else if (text.indexOf('\\') >= 0 || text.indexOf('/') >= 0)
        {
            resultAceptor.addIssue(Messages.ApkPathSeparatorMaskCheck_Manual_path_separator, literal);
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
     * Конструктор «Новый Файл»/«New File»: по разрешившемуся типу, при
     * неразрешившемся типе — по тексту конструктора.
     */
    private boolean isFileCreator(OperatorStyleCreator creator)
    {
        Type type = creator.getType();
        if (type != null)
        {
            String typeName = McoreUtil.getTypeName(type);
            String typeNameRu = McoreUtil.getTypeNameRu(type);
            if ((typeName != null && containsName(FILE_TYPE_NAMES, typeName))
                || (typeNameRu != null && containsName(FILE_TYPE_NAMES, typeNameRu)))
            {
                return true;
            }
        }
        // Резерв для неразрешившегося типа: по тексту конструктора «Новый Файл(...)»
        INode node = NodeModelUtils.findActualNodeFor(creator);
        if (node == null)
        {
            return false;
        }
        String text = node.getText().strip().toUpperCase(Locale.ROOT);
        if (text.startsWith(KEYWORD_NEW_UPPER_RU))
        {
            text = text.substring(KEYWORD_NEW_UPPER_RU.length()).strip();
        }
        else if (text.startsWith(KEYWORD_NEW_UPPER_EN))
        {
            text = text.substring(KEYWORD_NEW_UPPER_EN.length()).strip();
        }
        return text.startsWith("ФАЙЛ(") || text.startsWith("FILE("); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
