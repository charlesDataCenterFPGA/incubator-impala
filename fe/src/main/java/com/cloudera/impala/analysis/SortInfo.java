// Copyright 2012 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.impala.analysis;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Encapsulates all the information needed to compute ORDER BY
 * This doesn't contain aliases or positional exprs.
 */
public class SortInfo {
  private final List<Expr> orderingExprs_;
  private final List<Boolean> isAscOrder_;
  // True if "NULLS FIRST", false if "NULLS LAST", null if not specified.
  private final List<Boolean> nullsFirstParams_;
  // The single tuple that is materialized, sorted and output by a sort operator
  // (i.e. SortNode or TopNNode)
  private TupleDescriptor sortTupleDesc_;
  // Input expressions materialized into sortTupleDesc_. One expr per slot in
  // sortTupleDesc_.
  private List<Expr> sortTupleSlotExprs_;

  public SortInfo(List<Expr> orderingExprs, List<Boolean> isAscOrder,
      List<Boolean> nullsFirstParams) {
    Preconditions.checkArgument(orderingExprs.size() == isAscOrder.size());
    Preconditions.checkArgument(orderingExprs.size() == nullsFirstParams.size());
    orderingExprs_ = orderingExprs;
    isAscOrder_ = isAscOrder;
    nullsFirstParams_ = nullsFirstParams;
  }

  void setMaterializedTupleInfo(TupleDescriptor tupleDesc, List<Expr> tupleSlotExprs) {
    sortTupleDesc_ = tupleDesc;
    sortTupleSlotExprs_ = tupleSlotExprs;
  }
  public List<Expr> getOrderingExprs() { return orderingExprs_; }
  public List<Boolean> getIsAscOrder() { return isAscOrder_; }
  public List<Boolean> getNullsFirstParams() { return nullsFirstParams_; }
  public List<Expr> getSortTupleSlotExprs() { return sortTupleSlotExprs_; }
  public TupleDescriptor getSortTupleDescriptor() { return sortTupleDesc_; }

  /**
   * Gets the list of booleans indicating whether nulls come first or last, independent
   * of asc/desc.
   */
  public List<Boolean> getNullsFirst() {
    List<Boolean> nullsFirst = Lists.newArrayList();
    for (int i = 0; i < orderingExprs_.size(); ++i) {
      nullsFirst.add(OrderByElement.nullsFirst(nullsFirstParams_.get(i),
          isAscOrder_.get(i)));
    }
    return nullsFirst;
  }

  /**
   * Materializes the slots in sortTupleDesc_ referenced in the ordering exprs.
   * Materializes the slots referenced by the corresponding sortTupleSlotExpr after
   * applying the 'smap'.
   */
  public void materializeRequiredSlots(Analyzer analyzer, Expr.SubstitutionMap smap) {
    Preconditions.checkNotNull(sortTupleDesc_);
    Preconditions.checkNotNull(sortTupleSlotExprs_);
    Preconditions.checkState(sortTupleDesc_.getIsMaterialized());
    analyzer.materializeSlots(orderingExprs_);
    List<SlotDescriptor> sortTupleSlotDescs = sortTupleDesc_.getSlots();
    List<Expr> materializedExprs = Lists.newArrayList();
    for (int i = 0; i < sortTupleSlotDescs.size(); ++i) {
      if (sortTupleSlotDescs.get(i).isMaterialized()) {
        materializedExprs.add(sortTupleSlotExprs_.get(i));
      }
    }
    analyzer.materializeSlots(Expr.cloneList(materializedExprs, smap));
  }

}