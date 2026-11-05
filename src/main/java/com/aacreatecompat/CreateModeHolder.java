package com.aacreatecompat;

/** Implemented on AA's TileEntityAtomicReconstructor via mixin. */
public interface CreateModeHolder {
    boolean aacc$isCreateMode();
    void aacc$setCreateMode(boolean v);
    int aacc$getCooldown();
    void aacc$setCooldown(int v);
    long aacc$getLastBeltWork();
    void aacc$setLastBeltWork(long v);
    ReconstructorBeltProcessing aacc$getProcessing();
}
