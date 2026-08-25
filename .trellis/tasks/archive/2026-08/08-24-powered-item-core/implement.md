# Powered item core implementation

1. Add immutable contracts and catalog validation.
2. Add typed PDC storage with clone-and-replace slot helpers and legacy migration.
3. Add one generic portable prototype and 47 validated catalog specs.
4. Add central service lifecycle/event shell with no per-item tasks.
5. Adapt the legacy industrial energy cell to `RechargeableItem`.
6. Curate parent spec/research context, validate the child, and start it before source edits.

Do not register the catalog in CategoryManager or activate gameplay yet; parent integration owns that cutover after all behavior slices exist.
