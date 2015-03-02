/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.util.diff.differ;

import java.io.IOException;
import java.util.Objects;

import ch.algotrader.util.diff.define.CsvColumn;
import ch.algotrader.util.diff.filter.CsvLineFilter;
import ch.algotrader.util.diff.reader.CsvLine;
import ch.algotrader.util.diff.reader.CsvReader;
import ch.algotrader.util.diff.reader.FilterReader;

/**
 * Selects a subset of expected and/or actual lines based on filters criteria similar
 * to a where clause in an SQL statement. Lines that are accepted by the filters
 * are asserted by a delegate asserter.
 * <p>
 * A {@link #Builder} is used to define filters and create a {@code FilterDiffer} instance.
 */
public class FilterDiffer implements CsvDiffer {

    private final CsvDiffer delegate;
    private final CsvLineFilter expFilter;
    private final CsvLineFilter actFilter;

    private FilterDiffer(CsvDiffer delegate, CsvLineFilter expFilter, CsvLineFilter actFilter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.expFilter = expFilter;
        this.actFilter = actFilter;
    }

    @Override
    public void diffLines(CsvReader expectedReader, CsvReader actualReader) throws IOException {
        final CsvReader expReader = expFilter == null ? expectedReader : new FilterReader(expectedReader, expFilter);
        final CsvReader actReader = actFilter == null ? actualReader : new FilterReader(actualReader, actFilter);
        delegate.diffLines(expReader, actReader);
    }

    /**
     * Builder to create filters and eventually a {@link FilterDiffer}.
     */
    public static class Builder {
        private CsvLineFilter expFilter;
        private CsvLineFilter actFilter;

        public Builder acceptExpected(CsvColumn column, Object... values) {
            return filterExpected(createFilter(column, true, values));
        }

        public Builder rejectExpected(CsvColumn column, Object... values) {
            return filterExpected(createFilter(column, false, values));
        }

        public Builder filterExpected(CsvLineFilter csvLineFilter) {
            expFilter = and(expFilter, csvLineFilter);
            return this;
        }

        public Builder acceptActual(CsvColumn column, Object... values) {
            return filterActual(createFilter(column, true, values));
        }

        public Builder rejectActual(CsvColumn column, Object... values) {
            return filterActual(createFilter(column, false, values));
        }

        public Builder filterActual(CsvLineFilter csvLineFilter) {
            actFilter = and(actFilter, csvLineFilter);
            return this;
        }

        public FilterDiffer build(CsvDiffer delegate) {
            return new FilterDiffer(delegate, expFilter, actFilter);
        }

        private CsvLineFilter createFilter(CsvColumn column, boolean accept, Object[] values) {
            if (values.length == 0) {
                return null;
            }
            CsvLineFilter csvLineFilter = createFilter(column, accept, values[0]);
            for (int i = 1; i < values.length; i++) {
                csvLineFilter = or(csvLineFilter, createFilter(column, accept, values[i]));
            }
            return csvLineFilter;
        }

        private CsvLineFilter createFilter(final CsvColumn column, final boolean accept, final Object value) {
            return new CsvLineFilter() {
                @Override
                public boolean accept(CsvLine line) {
                    return accept == Objects.equals(value, line.getValues().get(column));
                }
            };
        }

        private CsvLineFilter and(final CsvLineFilter filter1, final CsvLineFilter filter2) {
            if (filter1 == null)
                return filter2;
            if (filter2 == null)
                return filter1;
            return new CsvLineFilter() {
                @Override
                public boolean accept(CsvLine line) {
                    return filter1.accept(line) && filter2.accept(line);
                }
            };
        }

        private CsvLineFilter or(final CsvLineFilter filter1, final CsvLineFilter filter2) {
            if (filter1 == null)
                return filter2;
            if (filter2 == null)
                return filter1;
            return new CsvLineFilter() {
                @Override
                public boolean accept(CsvLine line) {
                    return filter1.accept(line) || filter2.accept(line);
                }
            };
        }

    }

}
