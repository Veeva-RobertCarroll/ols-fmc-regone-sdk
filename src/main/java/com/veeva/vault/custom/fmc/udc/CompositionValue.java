package com.veeva.vault.custom.fmc.udc;

import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;

import java.math.BigDecimal;

@UserDefinedClassInfo
public class CompositionValue {
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal target;
    private boolean range;
    private String rangeValue;

    public CompositionValue(BigDecimal min, BigDecimal max, BigDecimal target) {
        this.min = min;
        this.max = max;
        this.target = target;
        if (min != null && max != null) {
            this.setRange(true);
        } else if (min != null) {
            this.setRange(true);
            this.setMax(new BigDecimal(100));
        } else if (max != null) {
            this.setRange(true);
            this.setMin(BigDecimal.ZERO);
        }
        if (this.isRange()) {
            this.setRangeValue(this.getMin() + "-" + this.getMax());
        }
    }

    public BigDecimal getMin() {
        return min;
    }

    private void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    private void setMax(BigDecimal max) {
        this.max = max;
    }

    public BigDecimal getTarget() {
        return target;
    }

    private void setTarget(BigDecimal target) {
        this.target = target;
    }

    public boolean isRange() {
        return range;
    }

    private void setRange(boolean range) {
        this.range = range;
    }

    public String getRangeValue() {
        return rangeValue;
    }

    private void setRangeValue(String rangeValue) {
        this.rangeValue = rangeValue;
    }
}