package pubsher.talexsoultech.extensions;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** A source-free remote declaration returned by the cloud manifest endpoint. */
public record ExtensionDescriptor(
        ExtensionManifest manifest,
        long revision,
        String sha256,
        boolean enabled
) {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    public ExtensionDescriptor {
        manifest = Objects.requireNonNull(manifest, "manifest");
        if (revision < 0L) {
            throw new IllegalArgumentException("Extension revision is invalid");
        }
        sha256 = Objects.requireNonNull(sha256, "sha256").toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(sha256).matches()) {
            throw new IllegalArgumentException("Extension sha256 is invalid");
        }
    }

    public boolean matches(ExtensionDescriptor other) {
        return other != null
                && revision == other.revision
                && sha256.equals(other.sha256)
                && manifest.equals(other.manifest)
                && enabled == other.enabled;
    }
}
