package com.dataiku.dctc.dispatch;

import com.dataiku.dip.datalayer.Column;
import com.dataiku.dip.datalayer.Row;

public interface SplitFunction {
    public String split(Row row, Column selectedColumn);
}
