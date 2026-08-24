package pubsher.talexsoultech.extensions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtensionManifestTest {

    private static final String SHA256 = "a".repeat(64);

    @Test
    void parsesOnlyValidatedEngineDependencyAndPermissionDeclarations() {
        ExtensionManifest manifest = ExtensionJson.manifestResponse(response(
                "catalog-tools",
                "LUA",
                "bootstrap.start",
                "[\"core-api\",\"catalog\"]",
                "[\"kv\",\"catalog\"]"
        )).getFirst().manifest();

        assertAll(
                () -> assertEquals(ExtensionManifest.Engine.LUA, manifest.engine()),
                () -> assertEquals("lua", manifest.sourceExtension()),
                () -> assertEquals(List.of("core-api", "catalog"), manifest.dependencies()),
                () -> assertEquals(
                        Set.of(ExtensionManifest.Capability.KV, ExtensionManifest.Capability.CATALOG),
                        manifest.permissions()
                )
        );
    }

    @Test
    void rejectsManifestDeclarationsThatCouldEscapeTheirDeclaredContract() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "Catalog-Tools", "lua", "boot", "[]", "[]"
                )), "extension ids must remain lowercase URL-safe identifiers"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "python", "boot", "[]", "[]"
                )), "unsupported engines must not be accepted"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot()", "[]", "[]"
                )), "entry points must be language identifiers rather than expressions"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot", "[\"catalog-tools\"]", "[]"
                )), "an extension cannot depend on itself"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot", "[\"core-api\",\"core-api\"]", "[]"
                )), "duplicate dependencies must be rejected"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot", "[\"invalid dependency\"]", "[]"
                )), "dependency ids use the same safe identifier contract"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot", "[]", "[\"world-edit\"]"
                )), "unknown capabilities must not silently grant access"),
                () -> assertThrows(IllegalArgumentException.class, () -> ExtensionJson.manifestResponse(response(
                        "catalog-tools", "lua", "boot", "[]", "[\"kv\",\"kv\"]"
                )), "duplicate capabilities must not be normalized away"
                )
        );
    }

    private static String response(
            String id,
            String engine,
            String entry,
            String dependencies,
            String permissions
    ) {
        return """
                {"extensions":[{
                  "manifest":{
                    "id":"%s",
                    "name":"Catalog tools",
                    "version":"1.0.0",
                    "engine":"%s",
                    "entry":"%s",
                    "dependencies":%s,
                    "permissions":%s
                  },
                  "revision":7,
                  "sha256":"%s",
                  "enabled":true
                }]}
                """.formatted(id, engine, entry, dependencies, permissions, SHA256);
    }
}
