/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.atlasdb.keyvalue.cassandra.sweep;

import java.util.List;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.palantir.atlasdb.keyvalue.api.CandidateCellForSweepingRequest;
import com.palantir.atlasdb.keyvalue.api.RangeRequests;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.cassandra.CqlExecutor;
import com.palantir.atlasdb.keyvalue.cassandra.paging.RowGetter;
import com.palantir.common.base.ClosableIterator;

public class CandidateRowsForSweepingIterator extends AbstractIterator<List<CandidateRowForSweeping>>
        implements ClosableIterator<List<CandidateRowForSweeping>> {

    private final ValuesLoader valuesLoader;
    private final CqlExecutor cqlExecutor;
    private final RowGetter rowGetter;
    private final TableReference table;
    private final CandidateCellForSweepingRequest request;

    byte[] nextStartRow;

    public CandidateRowsForSweepingIterator(
            ValuesLoader valuesLoader,
            CqlExecutor cqlExecutor,
            RowGetter rowGetter,
            TableReference table,
            CandidateCellForSweepingRequest request) {
        this.valuesLoader = valuesLoader;
        this.cqlExecutor = cqlExecutor;
        this.rowGetter = rowGetter;
        this.table = table;
        this.request = request;

        nextStartRow = request.startRowInclusive();
    }

    @Override
    protected List<CandidateRowForSweeping> computeNext() {
        List<CandidateRowForSweeping> batch = getCandidateCellsForSweepingBatch();
        if (batch.isEmpty()) {
            return endOfData();
        }

        nextStartRow = RangeRequests.nextLexicographicName(
                Iterables.getLast(batch).rowName());

        return batch;
    }

    private List<CandidateRowForSweeping> getCandidateCellsForSweepingBatch() {
        return new GetCandidateRowsForSweeping(valuesLoader, cqlExecutor, rowGetter, table,
                request.withStartRow(nextStartRow)).execute();
    }
}
