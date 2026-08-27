package pubsher.talexsoultech.talex.content.items;

import pubsher.talexsoultech.talex.content.ContentManifest;
import pubsher.talexsoultech.talex.content.ContentManifestLoader;

/**
 * One lifecycle entry point for BaseTalex/CategoryManager composition roots.
 * It deliberately does not maintain a second live item registry.
 */
public final class ContentRegistryLifecycle {

    private ContentRegistryLifecycle() {
    }

    public static ContentRegistry installBundled() {
        return install(ContentManifestLoader.loadBundled());
    }

    public static ContentRegistry install(ContentManifest manifest) {
        return new ContentRegistry(manifest).install();
    }

    public static ContentRegistry install(ContentRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Content registry must not be null");
        }
        return registry.install();
    }

    public static void uninstall(ContentRegistry registry) {
        if (registry != null) {
            registry.uninstall();
        }
    }
}
