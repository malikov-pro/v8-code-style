/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DeclareStatement;
import com._1c.g5.v8.dt.bsl.model.ExplicitVariable;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.GotoStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports identifiers that mix Cyrillic and Latin letters. Ported from the
 * pinned BSL LS diagnostic with its default parameters: identifiers shorter than
 * two characters are ignored, names that start in one alphabet and end in the
 * other are allowed (unless the option is disabled), and the configured
 * platform compound names are excluded.
 * <p>
 * v1 scope: method names, formal parameters, module and local variables,
 * annotation names and their named parameters, region names and goto label
 * targets, and assignment targets of plain variables.
 */
public final class LatinAndCyrillicSymbolInWordCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "latin-and-cyrillic-symbol-in-word"; //$NON-NLS-1$

    public static final String PARAM_EXCLUDE_WORDS = "excludeWords"; //$NON-NLS-1$
    public static final String PARAM_ALLOW_TRAILING = "allowTrailingPartsInAnotherLanguage"; //$NON-NLS-1$

    private static final int MINIMAL_WORD_LEN = 2;
    private static final int MINIMAL_WORD_LEN_TWO_LANG = 4;

    private static final String DEFAULT_EXCLUDE_WORDS = "ЧтениеXML, ЧтениеJSON, ЗаписьXML, ЗаписьJSON, ComОбъект, " //$NON-NLS-1$
        + "ФабрикаXDTO, ОбъектXDTO, СоединениеFTP, HTTPСоединение, HTTPЗапрос, HTTPСервисОтвет, SMSСообщение, WSПрокси"; //$NON-NLS-1$

    private static final Pattern RU_LANG = Pattern.compile("[а-яё]", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern EN_LANG = Pattern.compile("[a-z]", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RU_EN_LANG = Pattern.compile(
        "[A-Z][A-Za-z]+[A-Za-z1-9_]*[А-ЯЁ][А-Яа-яЁё]+[А-Яа-я1-9Ёё_]*|" //$NON-NLS-1$
            + "[А-ЯЁ][А-Яа-яЁё]+[А-Яа-я1-9Ёё_]*[A-Z][A-Za-z]+[A-Za-z1-9_]*"); //$NON-NLS-1$

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-zА-Яа-яЁё_][A-Za-zА-Яа-яЁё0-9_]*"); //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.LatinAndCyrillicSymbolInWordCheck_title)
            .description(Messages.LatinAndCyrillicSymbolInWordCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE)
            .parameter(PARAM_EXCLUDE_WORDS, String.class, DEFAULT_EXCLUDE_WORDS,
                Messages.LatinAndCyrillicSymbolInWordCheck_excludeWords)
            .parameter(PARAM_ALLOW_TRAILING, Boolean.class, "true", //$NON-NLS-1$
                Messages.LatinAndCyrillicSymbolInWordCheck_allowTrailing);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        Pattern excludeWords = excludeWords(parameters.getString(PARAM_EXCLUDE_WORDS));
        boolean allowTrailing = parameters.getBoolean(PARAM_ALLOW_TRAILING);

        for (Method method : EcoreUtil2.getAllContentsOfType(module, Method.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkName(method.getName(), method, excludeWords, allowTrailing, acceptor);
            List<FormalParam> params = method.getFormalParams();
            for (FormalParam param : params)
            {
                checkName(param.getName(), param, excludeWords, allowTrailing, acceptor);
            }
            checkPragmas(method.getPragmas(), module, excludeWords, allowTrailing, acceptor);
        }
        for (DeclareStatement declare : EcoreUtil2.getAllContentsOfType(module, DeclareStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            for (ExplicitVariable variable : declare.getVariables())
            {
                checkName(variable.getName(), variable, excludeWords, allowTrailing, acceptor);
            }
            checkPragmas(declare.getPragmas(), module, excludeWords, allowTrailing, acceptor);
        }
        for (RegionPreprocessor region : EcoreUtil2.getAllContentsOfType(module, RegionPreprocessor.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkName(region.getName(), region, excludeWords, allowTrailing, acceptor);
        }
        for (GotoStatement goTo : EcoreUtil2.getAllContentsOfType(module, GotoStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkGotoLabel(goTo, module, excludeWords, allowTrailing, acceptor);
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(module, SimpleStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (statement.getLeft() instanceof StaticFeatureAccess left && left.getName() != null
                && mixed(left.getName(), excludeWords, allowTrailing))
            {
                // StaticFeatureAccess is not a NamedElement: anchor on the identifier leaf.
                addLeafIssue(left, left.getName(), module, acceptor);
            }
        }
    }

    private void checkPragmas(List<Pragma> pragmas, Module module, Pattern excludeWords, boolean allowTrailing,
        ResultAcceptor acceptor)
    {
        for (Pragma pragma : pragmas)
        {
            String symbol = pragma.getSymbol();
            if (mixed(symbol, excludeWords, allowTrailing))
            {
                addLeafIssue(pragma, symbol, module, acceptor);
            }
            String value = pragma.getValue();
            if (value == null || value.isEmpty())
            {
                continue;
            }
            String[] parts = value.split(","); //$NON-NLS-1$
            for (String part : parts)
            {
                int equals = part.indexOf('=');
                String name = (equals < 0 ? part : part.substring(0, equals)).trim();
                if (!IDENTIFIER.matcher(name).matches() || !mixed(name, excludeWords, allowTrailing))
                {
                    continue;
                }
                addLeafIssue(pragma, name, module, acceptor);
            }
        }
    }

    private void checkName(String name, EObject owner, Pattern excludeWords, boolean allowTrailing,
        ResultAcceptor acceptor)
    {
        if (name != null && mixed(name, excludeWords, allowTrailing))
        {
            acceptor.addIssue(Messages.LatinAndCyrillicSymbolInWordCheck_message, owner,
                McorePackage.Literals.NAMED_ELEMENT__NAME);
        }
    }

    private boolean mixed(String name, Pattern excludeWords, boolean allowTrailing)
    {
        if (name == null || name.length() < MINIMAL_WORD_LEN || excludeWords.matcher(name).matches())
        {
            return false;
        }
        if (!RU_LANG.matcher(name).find() || !EN_LANG.matcher(name).find())
        {
            return false;
        }
        return !allowTrailing || name.length() < MINIMAL_WORD_LEN_TWO_LANG || !RU_EN_LANG.matcher(name).matches();
    }

    private void checkGotoLabel(GotoStatement goTo, Module module, Pattern excludeWords, boolean allowTrailing,
        ResultAcceptor acceptor)
    {
        INode node = NodeModelUtils.getNode(goTo);
        if (node == null)
        {
            return;
        }
        boolean afterTilde = false;
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            String text = leaf.getText();
            if (afterTilde)
            {
                issue(leaf, text, 0, module, excludeWords, allowTrailing, acceptor);
                return;
            }
            if (text.startsWith("~") && text.length() > 1) //$NON-NLS-1$
            {
                issue(leaf, text.substring(1), 1, module, excludeWords, allowTrailing, acceptor);
                return;
            }
            afterTilde = "~".equals(text); //$NON-NLS-1$
        }
    }

    private void issue(ILeafNode leaf, String name, int shift, Module module, Pattern excludeWords,
        boolean allowTrailing, ResultAcceptor acceptor)
    {
        if (mixed(name, excludeWords, allowTrailing))
        {
            acceptor.addIssue(new BslDirectLocationIssue(Messages.LatinAndCyrillicSymbolInWordCheck_message,
                new DirectLocation(leaf.getOffset() + shift, name.length(), leaf.getStartLine(), module)));
        }
    }

    private void addLeafIssue(EObject owner, String text, Module module, ResultAcceptor acceptor)
    {
        INode node = NodeModelUtils.getNode(owner);
        if (node == null)
        {
            return;
        }
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden() && text.equals(leaf.getText()))
            {
                acceptor.addIssue(new BslDirectLocationIssue(Messages.LatinAndCyrillicSymbolInWordCheck_message,
                    new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module)));
                return;
            }
        }
    }

    private static Pattern excludeWords(String words)
    {
        StringJoiner joiner = new StringJoiner("|"); //$NON-NLS-1$
        if (words != null)
        {
            for (String word : words.split(",")) //$NON-NLS-1$
            {
                if (!word.trim().isEmpty())
                {
                    joiner.add(Pattern.quote(word.trim()));
                }
            }
        }
        return Pattern.compile("(?:^" + joiner + ")", //$NON-NLS-1$ //$NON-NLS-2$
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
