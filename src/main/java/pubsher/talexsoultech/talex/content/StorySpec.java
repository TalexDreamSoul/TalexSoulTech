package pubsher.talexsoultech.talex.content;

import java.util.Objects;

/** Narrative metadata is retained for guide rendering but never participates in unlocks. */
public record StorySpec(int order, String text, String anchorReason) {
    public StorySpec {
        text = Objects.requireNonNull(text, "text");
        anchorReason = Objects.requireNonNull(anchorReason, "anchorReason");
    }
}
