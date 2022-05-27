package com.jetbrains.qodana.sarif.model.streaming;

import com.jetbrains.qodana.sarif.model.Result;

public class IndexedResult {
    /**
     * Result's index in Run object
     */
    private final int index;
    /**
     * Result itself
     */
    private final Result result;

    public IndexedResult(int index, Result result) {
        this.index = index;
        this.result = result;
    }

    /**
     *
     * @return index of Result in Run object
     */
    public int getIndex() {
        return index;
    }

    /**
     *
     * @return result itself
     */
    public Result getResult() {
        return result;
    }
}
