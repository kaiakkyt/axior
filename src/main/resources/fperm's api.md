# FoliaPerms API (usage guide)

This document explains how other plugins can obtain and use the public FoliaPerms API exposed by the FoliaPerms plugin.

Summary
- Service interface: `kaiakk.foliaPerms.api.FoliaPermsAPI`
- Registered with Bukkit's `ServicesManager` during plugin `onEnable()`.
- Thread-safety: API read operations are safe to call from async threads (Folia-friendly). The API returns defensive snapshots where appropriate.

Quick example — obtain the API provider

Use the ServicesManager to obtain the provider (recommended once during your plugin's `onEnable`):

```java
import kaiakk.foliaPerms.api.FoliaPermsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public void setupPermissionsAPI() {
		RegisteredServiceProvider<FoliaPermsAPI> rsp = Bukkit.getServer().getServicesManager().getRegistration(FoliaPermsAPI.class);
		if (rsp != null) {
				FoliaPermsAPI api = rsp.getProvider();
				// cache 'api' locally for later calls
		}
}
```

Compile-time vs Runtime (what plugin authors should know)

How Axior (or any other plugin) "gets the class" at compile time
- Compile time: Your plugin's code must see the `FoliaPermsAPI` interface during compilation so the Java compiler knows the method signatures (for example, `hasPermission` returns a boolean).
- Typical setup: add the `FoliaPerms` JAR (or just the `FoliaPermsAPI.class`) as a compile-only dependency in your project/IDE. This does not bundle FoliaPerms implementation into your plugin — it only provides the interface for compilation.

Example Gradle (compile-only) snippet you can use in your plugin project:

```gradle
dependencies {
	compileOnly(files("libs/FoliaPerms.jar")) // or use a published coordinate if available
}
```

Or in Maven use `provided`/`compile` scope accordingly so the runtime server provides the implementation.

How Axior "gets the class" at runtime (server startup)
- Runtime: Place both `Axior.jar` and `FoliaPerms.jar` into the server's `plugins/` folder.
- The server's plugin loader loads both JARs. The `FoliaPermsAPI` interface must be present to satisfy class references compiled into Axior; normally the interface is included in the FoliaPerms JAR you used at compile time.

The API handshake (ServicesManager) — how Axior obtains the implementation
- FoliaPerms registers its implementation instance during `onEnable()`:

```java
getServer().getServicesManager().register(FoliaPermsAPI.class, this, this, ServicePriority.Normal);
```

- Axior then asks the ServicesManager for a registration:

```java
RegisteredServiceProvider<FoliaPermsAPI> rsp = Bukkit.getServer().getServicesManager().getRegistration(FoliaPermsAPI.class);
if (rsp != null) {
	FoliaPermsAPI api = rsp.getProvider();
}
```

Soft depend and ordering
- To ensure FoliaPerms is loaded before your plugin (so the service is registered by the time your plugin looks it up), add a `softdepend` entry to your `plugin.yml`:
- Here's an example from my Axior plugin:

```yaml
name: Axior
version: '1.0.0'
main: kaiakk.axior.Axior
softdepend:
  - FoliaPerms
```

- `softdepend` is recommended but not strictly required; if FoliaPerms loads later, your lookup may return `null` and you should handle that case gracefully.

Direct communication and lifetime
- Once Axior receives the provider instance, it holds a direct reference and can call API methods.
- Do not serialize or persist the provider reference across restarts — it's valid only during the running JVM instance.

What if the provider is null at runtime?
- If `rsp` is `null`, it means no plugin registered `FoliaPermsAPI` at the moment of lookup. Common causes:
  - `FoliaPerms.jar` is missing from `plugins/`.
  - FoliaPerms failed to enable (check server log for errors during its `onEnable`).
  - You tried to lookup the service too early (before FoliaPerms registered it).
  - A package name mismatch or classloader conflict (rare if you used the same interface JAR as at compile time).

Safe fallback pattern

```java
RegisteredServiceProvider<FoliaPermsAPI> rsp = Bukkit.getServer().getServicesManager().getRegistration(FoliaPermsAPI.class);
if (rsp != null) {
	this.permsAPI = rsp.getProvider();
} else {
	getLogger().warning("FoliaPerms API not found: falling back to defaults.");
	this.permsAPI = null; // handle null throughout your plugin
}
```

Troubleshooting checklist
- Verify `FoliaPerms.jar` is present in `plugins/` and that the server log shows "FoliaPerms enabled".
- If `rsp` is `null`, restart the server after placing both JARs in `plugins/` and ensure `softdepend` is set.
- Check that the interface package is `kaiakk.foliaPerms.api` (no typos) and that you used that same interface at compile time.


API surface (current)
- `boolean hasPermission(Player player, String permissionNode)`
	- Check whether a `Player` has the given node.
- `boolean hasPermission(UUID playerUuid, String permissionNode)`
	- Check by UUID if you don't have a `Player` object.
- `Set<String> getPlayerGroups(Player player)`
	- Returns an unmodifiable snapshot of the group names the player belongs to (lowercase). May be empty.
- `String getPrimaryGroup(Player player)`
	- Returns one group name or `null` if none. This is a simple convenience; there is no weight/order logic yet.

Examples

Check permission for a player instance:

```java
FoliaPermsAPI api = /* get via ServicesManager as above */;
boolean allowed = api.hasPermission(somePlayer, "myplugin.use");
```

Check permission for an offline UUID (no Player object):

```java
UUID uuid = UUID.fromString("...");
boolean allowed = api.hasPermission(uuid, "myplugin.use");
```

Get a player's groups:

```java
Set<String> groups = api.getPlayerGroups(somePlayer);
// groups is an unmodifiable snapshot; iterate or copy as needed
```

Threading and Folia runtime notes
- The API read methods are safe to call from asynchronous threads in Folia (the plugin uses concurrent collections internally).
- The plugin performs file writes using a background Java thread (snapshot + write) when persistence is triggered; this keeps main-thread IO-free.
- Do not assume the returned set is mutable — treat it as read-only.

Persistence behavior
- Changes made via the plugin's `/fperm` admin commands are saved asynchronously (snapshot + background write).
- Data is also saved synchronously on plugin `onDisable()` to ensure persistence during shutdown.

Mutation and extending the API
- The current public API is read-only. If your plugin needs to programmatically create groups or modify permissions, use one of these approaches:
	- Use the `/fperm` command (with appropriate permission) from console or programmatically trigger it.
	- Request that FoliaPerms expose mutation methods on the API (they can be added). If you want this, tell me which mutation methods you need (e.g., `createGroup`, `addGroupPermission`, `addUserPermission`, `saveAsync`).

Best practices for plugin authors
- Cache the `FoliaPermsAPI` instance (from ServicesManager) during your plugin's `onEnable` rather than calling `getRegistration` repeatedly.
- Use the UUID-based `hasPermission(UUID, ...)` when running from async tasks or when you don't have a `Player` instance.
- Treat the API as eventually consistent: the permission service saves asynchronously, so a freshly-written change may appear slightly later to other processes.

Troubleshooting
- If `rsp` from `getRegistration(FoliaPermsAPI.class)` is `null`:
	- Verify `FoliaPerms` plugin is enabled and registered the service (check server log for "FoliaPerms enabled" and service registration messages).
- If permissions don't appear to take effect:
	- Ensure you called the API with the correct UUID or Player object.
	- Remember that permission checks are simple (explicit nodes + basic wildcard). More advanced features like inheritance or contexts are not implemented yet.

Contact / Extending
- If you need additional API functionality (mutation methods, events, or synchronous guarantees), open an issue or tell me what you need and I can add those methods and document them.

---

Generated by KaiakK (documentation)
