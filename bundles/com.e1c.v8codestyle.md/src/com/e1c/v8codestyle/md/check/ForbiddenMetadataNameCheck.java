/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EClass;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/** Reserved metadata names: exact whole-name matches, not arbitrary SQL keywords. */
public final class ForbiddenMetadataNameCheck extends BasicCheck
{
    public static final String CHECK_ID = "forbidden-metadata-name"; //$NON-NLS-1$

    private static final Pattern NAMES = Pattern.compile(
        "AccountingRegister|AccountingRegisters|AccumulationRegister|AccumulationRegisters" //$NON-NLS-1$
        + "|BusinessProcess|BusinessProcesses|CalculationRegister|CalculationRegisters|Catalog|Catalogs" //$NON-NLS-1$
        + "|ChartOfAccounts|ChartOfCalculationTypes|ChartOfCharacteristicTypes|ChartsOfAccounts" //$NON-NLS-1$
        + "|ChartsOfCalculationTypes|ChartsOfCharacteristicTypes|Constant|Constants|Document|DocumentJournal" //$NON-NLS-1$
        + "|DocumentJournals|Documents|Enum|Enums|ExchangePlan|ExchangePlans|FilterCriteria|FilterCriterion" //$NON-NLS-1$
        + "|InformationRegister|InformationRegisters|Task|Tasks|БизнесПроцесс|БизнесПроцессы|Документ|Документы" //$NON-NLS-1$
        + "|ЖурналДокументов|ЖурналыДокументов|Задача|Задачи|Константа|Константы|КритерииОтбора|КритерийОтбора" //$NON-NLS-1$
        + "|Перечисление|Перечисления|ПланВидовРасчета|ПланВидовХарактеристик|ПланОбмена|ПланСчетов" //$NON-NLS-1$
        + "|ПланыВидовРасчета|ПланыВидовХарактеристик|ПланыОбмена|ПланыСчетов|РегистрБухгалтерии" //$NON-NLS-1$
        + "|РегистрНакопления|РегистрРасчета|РегистрСведений|РегистрыБухгалтерии|РегистрыНакопления" //$NON-NLS-1$
        + "|РегистрыРасчета|РегистрыСведений|Справочник|Справочники", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final IV8ProjectManager projectManager;

    @Inject
    public ForbiddenMetadataNameCheck(IV8ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ForbiddenMetadataNameCheck_title)
            .description(Messages.ForbiddenMetadataNameCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());
        for (EClass type : MetadataNameScope.FORBIDDEN_TYPES)
        {
            builder.topObject(type).checkTop().features(MD_OBJECT__NAME)
                .containment(MD_OBJECT).features(MD_OBJECT__NAME);
        }
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        for (EClass type : new EClass[] { MdClassPackage.Literals.ACCOUNTING_FLAG,
            MdClassPackage.Literals.EXT_DIMENSION_ACCOUNTING_FLAG })
        {
            definition.addGenericModelAssociationContextCollector((object, event, session) ->
                session.addFullCheck(object), type, MdClassPackage.Literals.CHART_OF_ACCOUNTS);
            definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
                if (feature == MD_OBJECT__NAME)
                {
                    // Refresh all name anchors of this owner: flag-only delivery
                    // can be consumed by the platform's derived flag update.
                    session.addFullCheck(object);
                }
            }, type, MdClassPackage.Literals.CHART_OF_ACCOUNTS);
        }
        definition.addModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MdClassPackage.Literals.CHART_OF_ACCOUNTS__ACCOUNTING_FLAGS
                || feature == MdClassPackage.Literals.CHART_OF_ACCOUNTS__EXT_DIMENSION_ACCOUNTING_FLAGS)
            {
                session.addFullCheck(object);
            }
        }, MdClassPackage.Literals.CHART_OF_ACCOUNTS);
    }

    @Override
    protected void check(Object object, ResultAcceptor result, ICheckParameters parameters, IProgressMonitor monitor)
    {
        MdObject target = (MdObject)object;
        if (monitor.isCanceled() || !(projectManager.getProject(target) instanceof IConfigurationProject))
        {
            return;
        }
        MdObject owner = target;
        while (owner.eContainer() instanceof MdObject parent)
        {
            owner = parent;
        }
        if (MetadataNameScope.FORBIDDEN_TYPES.contains(owner.eClass())
            && (target == owner || isStorageField(owner, target)))
        {
            checkName(target, result);
        }
    }

    private boolean isStorageField(MdObject owner, MdObject target)
    {
        if (MetadataNameScope.attributes(owner).contains(target))
        {
            return true;
        }
        for (MdObject section : MetadataNameScope.sections(owner))
        {
            if (section == target || MetadataNameScope.attributes(section).contains(target))
            {
                return true;
            }
        }
        return false;
    }

    private void checkName(MdObject object, ResultAcceptor result)
    {
        if (object.getName() != null && NAMES.matcher(object.getName()).matches())
        {
            result.addIssue(MessageFormat.format(Messages.ForbiddenMetadataNameCheck_message,
                object.getName()), object, MD_OBJECT__NAME);
        }
    }
}
