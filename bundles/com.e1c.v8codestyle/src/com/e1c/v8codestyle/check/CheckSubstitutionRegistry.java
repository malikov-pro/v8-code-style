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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.check;

import java.util.List;

/**
 * The registry of check substitutions: pairs where a finer check fully covers
 * the invariant of a coarser check. When both are enabled for a project, the
 * coarser check produces duplicate markers; the coarse one is superseded.
 *
 * Used by {@link CheckSettingsDeduplicator} to auto-disable the coarse check
 * (opt-in, see its preference) and as the documented catalog of substitutions
 * made by the substitution policy: a ported diagnostic that is strictly more
 * precise replaces the existing v8-cs check, which is turned off by default.
 *
 * @author malikov-pro
 */
public final class CheckSubstitutionRegistry
{

    /**
     * An immutable substitution pair: the fine (substituting) check supersedes
     * the coarse (substituted) check.
     */
    public static final class Substitution
    {
        private final String fineCheckId;

        private final String coarseCheckId;

        /**
         * Instantiates a new substitution.
         *
         * @param fineCheckId the check id of the finer, substituting check, cannot be {@code null}
         * @param coarseCheckId the check id of the coarser, substituted check, cannot be {@code null}
         */
        public Substitution(String fineCheckId, String coarseCheckId)
        {
            this.fineCheckId = fineCheckId;
            this.coarseCheckId = coarseCheckId;
        }

        /**
         * Returns the check id of the finer, substituting check.
         *
         * @return the fine check id, never {@code null}
         */
        public String getFineCheckId()
        {
            return fineCheckId;
        }

        /**
         * Returns the check id of the coarser, substituted check.
         *
         * @return the coarse check id, never {@code null}
         */
        public String getCoarseCheckId()
        {
            return coarseCheckId;
        }
    }

    /**
     * {@code apk-00126-md-no-yo-letter} (see {@code MdObjectYoLetterCheck})
     * checks the letter "ё" in any case in names, synonyms (in every language)
     * and comments of metadata objects, including nested ones; it supersedes
     * {@code mdo-ru-name-unallowed-letter} (see
     * {@code MdObjectNameUnallowedLetterCheck}), which is limited to the
     * Russian locale and top objects.
     */
    public static final Substitution MD_YO_LETTER =
        new Substitution("apk-00126-md-no-yo-letter", "mdo-ru-name-unallowed-letter"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code create-query-in-cycle} (see {@code CreateQueryInCycleCheck})
     * covers query and query builder creation in loops, including nested loops
     * and the "While" loop of the {@code query-in-loop} check (see
     * {@code QueryInLoopCheck}).
     */
    public static final Substitution CREATE_QUERY_IN_CYCLE =
        new Substitution("create-query-in-cycle", "query-in-loop"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final List<Substitution> SUBSTITUTIONS = List.of(MD_YO_LETTER, CREATE_QUERY_IN_CYCLE);

    /**
     * Returns all known substitutions.
     *
     * @return an unmodifiable list of substitutions, never {@code null}
     */
    public static List<Substitution> substitutions()
    {
        return SUBSTITUTIONS;
    }

    private CheckSubstitutionRegistry()
    {
        throw new IllegalAccessError("Utility class"); //$NON-NLS-1$
    }

}
