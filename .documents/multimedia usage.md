# Multimedia Plugin - Class Documentation

### WARNING! 
anything schedule or threading related may be different for folia! if you know how to use folia's systems, this shouldnt be a big problem!

## 1. ArmorStands

**Purpose**: Create and manage armor stands for displays, holograms, and decorations with fluent API.

### Basic Creation

```java
// Create armor stand
ArmorStand stand = ArmorStands.create(location);

// Create invisible armor stand
ArmorStand invisible = ArmorStands.createInvisible(location);

// Create marker (no hitbox, small, invisible)
ArmorStand marker = ArmorStands.createMarker(location);

// Create small armor stand
ArmorStand small = ArmorStands.createSmall(location);
```

### Holograms

```java
// Create simple hologram
ArmorStand holo = ArmorStands.createHologram(location, "&6Welcome to the server!");

// Multi-line hologram
List<ArmorStand> holograms = ArmorStands.createMultiLineHologram(location,
    "&6&lServer Name",
    "&7Online Players: 50",
    "&aJoin now!"
);

// Floating item display
ItemStack diamond = new ItemStack(Material.DIAMOND);
ArmorStand display = ArmorStands.createFloatingItem(location, diamond);
```

### Fluent Builder API

```java
// Build custom armor stand
ArmorStand stand = ArmorStands.builder(location)
    .invisible()
    .noGravity()
    .small()
    .arms()
    .noBasePlate()
    .customName("&cBoss")
    .helmet(Material.DIAMOND_HELMET)
    .chestplate(Material.DIAMOND_CHESTPLATE)
    .build();

// Complete example
ArmorStand npc = ArmorStands.builder(location)
    .customName("&6Guard")
    .helmet(Material.IRON_HELMET)
    .chestplate(Material.IRON_CHESTPLATE)
    .leggings(Material.IRON_LEGGINGS)
    .boots(Material.IRON_BOOTS)
    .mainHand(Material.IRON_SWORD)
    .arms()
    .build();
```

### Poses

```java
// Apply preset poses
ArmorStand stand = ArmorStands.create(location);
ArmorStands.setPose(stand, ArmorStands.Pose.SITTING);
ArmorStands.setPose(stand, ArmorStands.Pose.WALKING);
ArmorStands.setPose(stand, ArmorStands.Pose.POINTING);
ArmorStands.setPose(stand, ArmorStands.Pose.WAVING);

// Custom poses (degrees)
ArmorStands.setHeadPose(stand, 45, 0, 0); // Tilt head
ArmorStands.setRightArmPose(stand, -90, 0, 0); // Point right arm
ArmorStands.setLeftLegPose(stand, 20, 0, 0); // Bend left leg

// Available preset poses:
// DEFAULT, SITTING, WALKING, RUNNING, POINTING, WAVING, BLOCKING, CROUCHING
```

### Equipment

```java
// Set armor pieces
ArmorStands.setHelmet(stand, new ItemStack(Material.DIAMOND_HELMET));
ArmorStands.setChestplate(stand, new ItemStack(Material.IRON_CHESTPLATE));
ArmorStands.setLeggings(stand, new ItemStack(Material.LEATHER_LEGGINGS));
ArmorStands.setBoots(stand, new ItemStack(Material.CHAINMAIL_BOOTS));

// Set items in hands
ArmorStands.setItemInHand(stand, new ItemStack(Material.DIAMOND_SWORD));
ArmorStands.setItemInOffHand(stand, new ItemStack(Material.SHIELD));
```

### Properties

```java
// Visibility and physics
ArmorStands.setVisible(stand, false);
ArmorStands.setGravity(stand, false);
ArmorStands.setSmall(stand, true);

// Features
ArmorStands.setBasePlate(stand, false);
ArmorStands.setArms(stand, true);
ArmorStands.setMarker(stand, true); // No hitbox
ArmorStands.setGlowing(stand, true);

// Custom name
ArmorStands.setCustomName(stand, "&6Display Name");
```

### Management

```java
// Remove armor stand
ArmorStands.remove(stand);

// Remove all nearby armor stands
int removed = ArmorStands.removeNearby(location, 10.0);

// Get nearby armor stands
List<ArmorStand> nearby = ArmorStands.getNearby(location, 20.0);
```

### Complete Examples

```java
// Shop NPC
public ArmorStand createShopNPC(Location location) {
    return ArmorStands.builder(location)
        .customName("&6&lShop Keeper")
        .helmet(Material.PLAYER_HEAD)
        .chestplate(Material.LEATHER_CHESTPLATE)
        .leggings(Material.LEATHER_LEGGINGS)
        .boots(Material.LEATHER_BOOTS)
        .mainHand(Material.EMERALD)
        .arms()
        .pose(ArmorStands.Pose.WAVING)
        .build();
}

// Animated display
public void createRotatingDisplay(Location location, ItemStack item) {
    ArmorStand stand = ArmorStands.builder(location)
        .invisible()
        .noGravity()
        .small()
        .marker()
        .helmet(item)
        .build();
    
    // Rotate every tick
    SchedulerHelper.runTimer(plugin, () -> {
        Location loc = stand.getLocation();
        loc.setYaw(loc.getYaw() + 5);
        stand.teleport(loc);
    }, 0, 1);
}

// Scoreboard hologram
public List<ArmorStand> createScoreboard(Location location, List<String> lines) {
    return ArmorStands.createMultiLineHologram(location, lines.toArray(new String[0]));
}

// Statue with pose
public ArmorStand createStatue(Location location, Player player) {
    ArmorStand statue = ArmorStands.builder(location)
        .helmet(new ItemStack(Material.PLAYER_HEAD))
        .chestplate(player.getInventory().getChestplate())
        .leggings(player.getInventory().getLeggings())
        .boots(player.getInventory().getBoots())
        .mainHand(player.getInventory().getItemInMainHand())
        .arms()
        .basePlate(false)
        .pose(ArmorStands.Pose.WALKING)
        .build();
    
    // Set player head
    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
    skull.getItemMeta().setOwningPlayer(player);
    statue.setHelmet(skull);
    
    return statue;
}
```

---

## 2. AttributeEditor

**Purpose**: Modify entity attributes like max health, speed, damage, armor, and more across all Minecraft versions.

### Max Health

```java
// Set max health
AttributeEditor.setMaxHealth(zombie, 100.0); // Tanky zombie

// Get max health
double maxHealth = AttributeEditor.getMaxHealth(zombie);

// Reset to default
AttributeEditor.resetMaxHealth(zombie);
```

### Movement Speed

```java
// Set movement speed (default: 0.7 for mobs, 0.1 for players)
AttributeEditor.setMovementSpeed(zombie, 0.5); // 50% faster
AttributeEditor.setMovementSpeed(player, 0.15); // Faster player

// Get current speed
double speed = AttributeEditor.getMovementSpeed(zombie);

// Reset to default
AttributeEditor.resetMovementSpeed(zombie);
```

### Attack Damage

```java
// Set attack damage
AttributeEditor.setAttackDamage(zombie, 10.0); // High damage

// Get attack damage
double damage = AttributeEditor.getAttackDamage(zombie);

// Reset to default
AttributeEditor.resetAttackDamage(zombie);
```

### Attack Speed (1.9+)

```java
// Set attack speed (default: 4.0)
AttributeEditor.setAttackSpeed(player, 8.0); // Faster attacks

// Get attack speed
double attackSpeed = AttributeEditor.getAttackSpeed(player);

// Reset to default
AttributeEditor.resetAttackSpeed(player);
```

### Armor & Toughness

```java
// Set armor value (each point = 0.5 armor icons)
AttributeEditor.setArmor(zombie, 20.0); // 10 armor icons

// Set armor toughness (1.9+)
AttributeEditor.setArmorToughness(zombie, 8.0);

// Get values
double armor = AttributeEditor.getArmor(zombie);
double toughness = AttributeEditor.getArmorToughness(zombie);

// Reset
AttributeEditor.resetArmor(zombie);
AttributeEditor.resetArmorToughness(zombie);
```

### Knockback Resistance

```java
// Set knockback resistance (0.0 - 1.0)
AttributeEditor.setKnockbackResistance(zombie, 0.5); // 50% resistance
AttributeEditor.setKnockbackResistance(zombie, 1.0); // Immune to knockback

// Get resistance
double resistance = AttributeEditor.getKnockbackResistance(zombie);

// Reset
AttributeEditor.resetKnockbackResistance(zombie);
```

### Player Movement Speeds

```java
// Walking speed (-1.0 to 1.0, default: 0.2)
AttributeEditor.setWalkingSpeed(player, 0.3f); // 50% faster walking

// Flying speed (-1.0 to 1.0, default: 0.1)
AttributeEditor.setFlyingSpeed(player, 0.2f); // Double flying speed

// Get speeds
float walkSpeed = AttributeEditor.getWalkingSpeed(player);
float flySpeed = AttributeEditor.getFlyingSpeed(player);

// Reset
AttributeEditor.resetWalkingSpeed(player);
AttributeEditor.resetFlyingSpeed(player);
```

### Luck (1.9+)

```java
// Set luck (affects loot table chances)
AttributeEditor.setLuck(player, 10.0); // Better loot

// Get luck
double luck = AttributeEditor.getLuck(player);

// Reset
AttributeEditor.resetLuck(player);
```

### Follow Range

```java
// Set how far mob can detect targets (default: 32.0)
AttributeEditor.setFollowRange(zombie, 64.0); // Detect from far away

// Get follow range
double range = AttributeEditor.getFollowRange(zombie);

// Reset
AttributeEditor.resetFollowRange(zombie);
```

### Reset All Attributes

```java
// Reset all attributes to defaults
AttributeEditor.resetAllAttributes(zombie);
AttributeEditor.resetAllAttributes(player);
```

### Generic Attribute Access

```java
// Check if entity has attribute
if (AttributeEditor.hasAttribute(zombie, Attribute.GENERIC_MAX_HEALTH)) {
    // Has max health attribute
}

// Get any attribute value
double value = AttributeEditor.getAttributeValue(zombie, Attribute.GENERIC_ARMOR);

// Set any attribute value
AttributeEditor.setAttributeValue(zombie, Attribute.GENERIC_LUCK, 5.0);
```

### Complete Examples

```java
// Create boss mob
public Zombie createBoss(Location location) {
    Zombie boss = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
    
    // Super tanky
    AttributeEditor.setMaxHealth(boss, 500.0);
    boss.setHealth(500.0);
    
    // High damage
    AttributeEditor.setAttackDamage(boss, 15.0);
    
    // Faster movement
    AttributeEditor.setMovementSpeed(boss, 0.35);
    
    // Knockback resistant
    AttributeEditor.setKnockbackResistance(boss, 0.8);
    
    // Long detection range
    AttributeEditor.setFollowRange(boss, 100.0);
    
    boss.setCustomName("&4&lBOSS");
    boss.setCustomNameVisible(true);
    
    return boss;
}

// Speed boost item
public void giveSpeedBoost(Player player, int duration) {
    // Increase walking speed
    float originalSpeed = AttributeEditor.getWalkingSpeed(player);
    AttributeEditor.setWalkingSpeed(player, originalSpeed * 1.5f);
    
    player.sendMessage("&aSpeed boost activated!");
    
    // Reset after duration
    SchedulerHelper.runLater(plugin, () -> {
        AttributeEditor.resetWalkingSpeed(player);
        player.sendMessage("&cSpeed boost expired");
    }, duration * 20L);
}

// Difficulty scaler
public void scaleMonsterDifficulty(LivingEntity entity, int difficultyLevel) {
    double multiplier = 1.0 + (difficultyLevel * 0.2); // 20% per level
    
    // Scale health
    double baseHealth = AttributeEditor.getMaxHealth(entity);
    AttributeEditor.setMaxHealth(entity, baseHealth * multiplier);
    entity.setHealth(baseHealth * multiplier);
    
    // Scale damage
    double baseDamage = AttributeEditor.getAttackDamage(entity);
    AttributeEditor.setAttackDamage(entity, baseDamage * multiplier);
    
    // Scale speed slightly
    double baseSpeed = AttributeEditor.getMovementSpeed(entity);
    AttributeEditor.setMovementSpeed(entity, baseSpeed * (1.0 + difficultyLevel * 0.05));
}

// Custom mob spawner
public void spawnCustomMob(Location location, String type) {
    switch (type.toLowerCase()) {
        case "tank":
            Zombie tank = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
            AttributeEditor.setMaxHealth(tank, 200.0);
            tank.setHealth(200.0);
            AttributeEditor.setArmor(tank, 20.0);
            AttributeEditor.setKnockbackResistance(tank, 1.0);
            AttributeEditor.setMovementSpeed(tank, 0.15); // Slow
            break;
            
        case "runner":
            Zombie runner = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
            AttributeEditor.setMovementSpeed(runner, 0.5); // Very fast
            AttributeEditor.setFollowRange(runner, 80.0);
            break;
            
        case "glass-cannon":
            Zombie cannon = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
            AttributeEditor.setMaxHealth(cannon, 10.0);
            cannon.setHealth(10.0);
            AttributeEditor.setAttackDamage(cannon, 20.0);
            break;
    }
}
```

---

## 3. Base64

**Purpose**: Encode and decode Base64 strings, files, and resources with ease.

### Basic Usage

```java
// Encode/decode strings
String encoded = Base64.encode("Hello World");
String decoded = Base64.decodeToString(encoded);

// Encode/decode bytes
byte[] data = "test".getBytes();
String base64 = Base64.encodeBytes(data);
byte[] original = Base64.decodeToBytes(base64);
```

### File Operations

```java
// Read file and encode to Base64
Path file = Paths.get("data.txt");
String encodedFile = Base64.encodeFileToBase64(file);

// Decode Base64 and write to file
Base64.decodeToFile(encodedData, outputPath);

// Safe file operations (returns null on error instead of throwing)
String safeEncoded = Base64.encodeFileToBase64Safe(file);
```

### Resource Handling

```java
// Encode a resource from your plugin JAR
String resourceData = Base64.encodeResourceToBase64(plugin, "config.yml");

// Decode and save resource
Base64.decodeToResource(base64Data, plugin, "saved.yml");
```

### Data URI Support

```java
// Create data URI from file
String dataUri = Base64.encodeFileToDataUri(file, "image/png");
// Result: "data:image/png;base64,iVBORw0KG..."

// Decode data URI back to bytes
byte[] imageData = Base64.decodeDataUri(dataUri);
```

### Plugin Data Folder Shortcuts

```java
// Encode file from plugin's data folder
String encoded = Base64.encodeFromDataFolder(plugin, "players.dat");

// Decode to plugin's data folder
Base64.decodeToDataFolder(plugin, encoded, "backup.dat");
```

---

## 4. BiomeFinder

**Purpose**: Get, set, and analyze biomes in the world with advanced search and distribution tools.

### Basic Biome Operations

```java
// Get biome at location
Biome biome = BiomeFinder.getBiome(location);

// Set biome at location
BiomeFinder.setBiome(location, Biome.DESERT);

// Set biome in area (cuboid)
BiomeFinder.setBiomeArea(corner1, corner2, Biome.JUNGLE);

// Set biome in circular radius
BiomeFinder.setBiomeRadius(center, 50, Biome.PLAINS); // 50 block radius
```

### Finding Biomes

```java
// Find nearest biome of specific type
Location desertLoc = BiomeFinder.findNearestBiome(playerLoc, Biome.DESERT, 1000);
if (desertLoc != null) {
    player.teleport(desertLoc);
}

// Find all locations of a biome in radius
List<Location> jungleLocs = BiomeFinder.findAllBiomesInRadius(center, Biome.JUNGLE, 200);
player.sendMessage("Found " + jungleLocs.size() + " jungle blocks nearby");
```

### Biome Analysis

```java
// Get biome distribution in area
Map<Biome, Integer> distribution = BiomeFinder.getBiomeDistribution(corner1, corner2);
for (Map.Entry<Biome, Integer> entry : distribution.entrySet()) {
    player.sendMessage(entry.getKey() + ": " + entry.getValue() + " blocks");
}

// Get dominant biome in area
Biome dominant = BiomeFinder.getDominantBiome(corner1, corner2);
player.sendMessage("Most common biome: " + dominant);

// Get all unique biomes in area
List<Biome> allBiomes = BiomeFinder.getAllBiomesInArea(corner1, corner2);
player.sendMessage("Found " + allBiomes.size() + " different biomes");
```

### Biome Type Checking

```java
// Check if location matches specific biomes
if (BiomeFinder.isBiome(location, Biome.DESERT, Biome.DESERT_HILLS)) {
    player.sendMessage("You're in a desert!");
}

// Check biome categories
if (BiomeFinder.isDesertBiome(location)) {
    player.damage(1.0); // Desert heat damage
}

if (BiomeFinder.isOceanBiome(location)) {
    player.sendMessage("You're near water!");
}

if (BiomeFinder.isSnowBiome(location)) {
    player.sendMessage("Brr, it's cold!");
}

if (BiomeFinder.isJungleBiome(location)) {
    // Jungle-specific logic
}

if (BiomeFinder.isMountainBiome(location)) {
    // Mountain-specific logic
}

// Check dimension
if (BiomeFinder.isNetherBiome(location)) {
    player.sendMessage("You're in the Nether!");
}

if (BiomeFinder.isEndBiome(location)) {
    player.sendMessage("You're in the End!");
}
```

### Biome Statistics

```java
// Count specific biome blocks in area
int desertBlocks = BiomeFinder.countBiomeBlocks(corner1, corner2, Biome.DESERT);
player.sendMessage("Desert blocks: " + desertBlocks);

// Get percentage of specific biome
double percentage = BiomeFinder.getBiomePercentage(corner1, corner2, Biome.JUNGLE);
player.sendMessage("Jungle coverage: " + percentage + "%");
```

### Practical Examples

```java
// Biome finder command
CommandHelper.register(plugin, "findbiome", (sender, label, args) -> {
    if (!(sender instanceof Player)) return true;
    Player player = (Player) sender;
    
    if (args.length < 1) {
        player.sendMessage("Usage: /findbiome <biome>");
        return true;
    }
    
    try {
        Biome targetBiome = Biome.valueOf(args[0].toUpperCase());
        Location found = BiomeFinder.findNearestBiome(player.getLocation(), targetBiome, 5000);
        
        if (found != null) {
            int distance = (int) player.getLocation().distance(found);
            player.sendMessage("Found " + targetBiome + " at " + distance + " blocks away!");
        } else {
            player.sendMessage("Could not find " + targetBiome + " within 5000 blocks");
        }
    } catch (IllegalArgumentException e) {
        player.sendMessage("Invalid biome name!");
    }
    
    return true;
});

// Create biome analysis report
public void analyzeBiomes(Player player, Location corner1, Location corner2) {
    Map<Biome, Integer> distribution = BiomeFinder.getBiomeDistribution(corner1, corner2);
    Biome dominant = BiomeFinder.getDominantBiome(corner1, corner2);
    
    player.sendMessage("&6&lBiome Analysis Report");
    player.sendMessage("&eDominant Biome: &f" + dominant);
    player.sendMessage("&eUnique Biomes: &f" + distribution.size());
    player.sendMessage("");
    player.sendMessage("&eDistribution:");
    
    distribution.entrySet().stream()
        .sorted(Map.Entry.<Biome, Integer>comparingByValue().reversed())
        .limit(5)
        .forEach(entry -> {
            double percent = BiomeFinder.getBiomePercentage(corner1, corner2, entry.getKey());
            player.sendMessage("  " + entry.getKey() + ": " + String.format("%.1f%%", percent));
        });
}

// Biome-based event trigger
@EventHandler
public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location to = event.getTo();
    
    if (BiomeFinder.isDesertBiome(to) && !player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
        // Apply desert effects
        if (Math.random() < 0.01) { // 1% chance per move
            player.sendMessage("&cThe desert heat is overwhelming!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
        }
    }
}
```

---

## 5. BookMaker

**Purpose**: Create written books with pages, colors, authors, and custom content easily.

### Basic Book Creation

```java
// Create simple book
BookMaker.create("&6My Book")
    .setAuthor("Server")
    .addPage("&lWelcome!", "", "This is the first page")
    .addPage("&9Chapter 2", "More content here")
    .give(player);

// Open book without giving it
BookMaker.create("&cRules")
    .setAuthor("Admin")
    .addPage("1. No griefing", "2. Be respectful")
    .open(player);
```

### Multiple Pages

```java
// Add multiple pages
BookMaker.Book book = BookMaker.create("&aGuide");
book.addPage("&l&nIntroduction", "", "Welcome to the server!");
book.addPage("&l&nGetting Started", "1. Type /spawn", "2. Claim land");
book.addPage("&l&nAdvanced Tips", "Use /help for commands");
book.setAuthor("Helper Bot");
book.give(player);
```

### Page Management

```java
// Create book and modify pages
BookMaker.Book book = BookMaker.create("Story");
book.addPage("Chapter 1...");
book.addPage("Chapter 2...");
book.setPage(1, "Chapter 2 (revised)"); // Replace page 1
book.removePage(0); // Remove first page
book.clearPages(); // Remove all pages
book.addPage("New content");

// Get page info
int pageCount = book.getPageCount();
String firstPage = book.getPage(0);
List<String> allPages = book.getAllPages();
```

### Pre-Made Book Templates

```java
// Tutorial book
BookMaker.tutorial("&6Server Tutorial",
    "Press F to open your inventory",
    "Type /spawn to return to spawn",
    "Use /help to see all commands"
).give(player);

// Rules book (auto-formats into pages)
BookMaker.rulesBook("MyServer",
    "No griefing or stealing",
    "Be respectful to all players",
    "No cheating or exploits",
    "Follow staff instructions",
    "Have fun!"
).give(player);

// Story book (auto-splits long text into pages)
String story = "Once upon a time, in a land far away... [long story text]";
BookMaker.storyBook("&5The Legend", "Author Name", story)
    .give(player);

// Guide book with sections
BookMaker.guideBook("&bPlayer Guide", "Server",
    "&l&nGetting Started\n\nWelcome to the server!",
    "&l&nCommands\n\n/spawn - Teleport to spawn",
    "&l&nRules\n\nBe nice and have fun!"
).give(player);

// Info book
BookMaker.infoBook("&eServer Info",
    "&6Server: &fMyServer",
    "&6IP: &fplay.example.com",
    "&6Discord: &fdiscord.gg/..."
).give(player);
```

### Book Generation Control

```java
// Set book generation (original, copy of original, copy of copy, tattered)
BookMaker.create("Ancient Tome")
    .setAuthor("Unknown")
    .addPage("Ancient text...")
    .setGeneration(BookMeta.Generation.TATTERED)
    .give(player);
```

### Working with Existing Books

```java
// Load book from ItemStack
ItemStack bookItem = player.getInventory().getItemInMainHand();
if (BookMaker.isBook(bookItem)) {
    BookMaker.Book book = BookMaker.fromItemStack(bookItem);
    book.addPage("New page added!");
    player.getInventory().setItemInMainHand(book.build());
}

// Get book info
String title = BookMaker.getBookTitle(bookItem);
String author = BookMaker.getBookAuthor(bookItem);
List<String> pages = BookMaker.getBookPages(bookItem);
int pageCount = BookMaker.getPageCount(bookItem);

// Clone book
ItemStack copy = BookMaker.cloneBook(originalBook);
```

### Utility Methods

```java
// Quick give/open
BookMaker.giveBook(player, "&6Title", "Author", "Page 1", "Page 2", "Page 3");
BookMaker.openBook(player, "&cWarning", "Server", "Read carefully!");

// Create simple book
ItemStack book = BookMaker.createSimpleBook("Title", "Author", Arrays.asList(
    "Page 1 content",
    "Page 2 content"
));

// Check if item is a book
if (BookMaker.isBook(item)) {
    // It's a written book
}

if (BookMaker.isBookAndQuill(item)) {
    // It's a writable book
}
```

### Complete Examples

```java
// Welcome book on join
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    if (!player.hasPlayedBefore()) {
        BookMaker.create("&6&lWelcome to " + serverName)
            .setAuthor("Server")
            .addPage(
                "&l&nWelcome!",
                "",
                "&7Thank you for joining",
                "&7" + serverName + "!",
                "",
                "&eRead this book to",
                "&elearn the basics."
            )
            .addPage(
                "&l&nGetting Started",
                "",
                "&61. &7Type &f/spawn",
                "&62. &7Read &f/rules",
                "&63. &7Ask for &f/help"
            )
            .addPage(
                "&l&nImportant Links",
                "",
                "&eWebsite:",
                "&fwww.example.com",
                "",
                "&eDiscord:",
                "&fdiscord.gg/..."
            )
            .give(player);
    }
}

// Quest book system
public void giveQuestBook(Player player, String questName, String... objectives) {
    BookMaker.Book book = BookMaker.create("&6Quest: " + questName);
    book.setAuthor("Quest Master");
    
    book.addPage("&l&n" + questName, "", "&7A new adventure awaits!");
    
    StringBuilder objectivesPage = new StringBuilder("&l&nObjectives\n\n");
    for (int i = 0; i < objectives.length; i++) {
        objectivesPage.append("&e").append(i + 1).append(". &7")
            .append(objectives[i]).append("\n");
    }
    book.addPage(objectivesPage.toString());
    
    book.addPage("&l&nRewards", "", "&6Gold: &f100", "&6XP: &f50", "&6Item: &fDiamond Sword");
    
    book.give(player);
}

// Dynamic book with player stats
public void giveStatsBook(Player player) {
    BookMaker.create("&b" + player.getName() + "'s Stats")
        .setAuthor("Server")
        .addPage(
            "&l&nPlayer Statistics",
            "",
            "&eLevel: &f" + player.getLevel(),
            "&eHealth: &f" + player.getHealth(),
            "&eFood: &f" + player.getFoodLevel(),
            "&eXP: &f" + player.getTotalExperience()
        )
        .addPage(
            "&l&nLocation",
            "",
            "&eWorld: &f" + player.getWorld().getName(),
            "&eX: &f" + player.getLocation().getBlockX(),
            "&eY: &f" + player.getLocation().getBlockY(),
            "&eZ: &f" + player.getLocation().getBlockZ()
        )
        .open(player);
}
```

---

## 6. BuildGUI

**Purpose**: Advanced GUI builder for creating interactive inventory menus with click handlers, pagination, and animations.

### Initialization

```java
@Override
public void onEnable() {
    // Initialize BuildGUI (required before use)
    BuildGUI.init(this);
}
```

### Basic GUI Creation

```java
// Create simple GUI (title, rows 1-6)
BuildGUI.create("My Shop", 3)
    .setItem(13, diamondItem)
    .open(player);

// Create with exact slots
BuildGUI.createWithSlots("Custom GUI", 27)
    .setItem(10, item1)
    .setItem(16, item2)
    .open(player);
```

### Clickable Items

```java
// Add item with click handler
ItemStack diamond = new ItemStack(Material.DIAMOND);
BuildGUI.create("Shop", 3)
    .setItem(13, diamond, player -> {
        player.sendMessage("You clicked diamond!");
        buyDiamond(player);
    })
    .open(player);

// Multiple clickable items
BuildGUI.create("Admin Panel", 3)
    .setItem(10, healItem, p -> p.setHealth(20))
    .setItem(11, feedItem, p -> p.setFoodLevel(20))
    .setItem(12, flyItem, p -> p.setAllowFlight(true))
    .open(player);
```

### Filling and Borders

```java
// Fill empty slots
ItemStack glass = new ItemStack(Material.GLASS_PANE);
BuildGUI.create("Shop", 5)
    .fill(glass)
    .setItem(22, shopItem)
    .open(player);

// Fill border only
BuildGUI.create("GUI", 4)
    .fillBorder(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
    .setItem(13, centerItem)
    .open(player);

// Fill specific area
BuildGUI.create("GUI", 6)
    .fillArea(0, 8, topRowItem)  // Fill top row
    .fillArea(45, 53, bottomRowItem)  // Fill bottom row
    .open(player);
```

### Close Handlers

```java
// Execute code when GUI is closed
BuildGUI.create("Confirmation", 3)
    .setItem(11, confirmItem, p -> confirmAction(p))
    .setItem(15, cancelItem, p -> p.closeInventory())
    .onClose(player -> {
        player.sendMessage("Thanks for visiting!");
        logGUIClose(player);
    })
    .open(player);
```

### Paginated GUIs

```java
// Create paginated GUI for multiple items
List<ItemStack> items = getAllPlayerHeads(); // 100+ items

BuildGUI.paginated("Player List", items, 54)
    .open(player);
```

### Advanced Pagination

```java
// Customize pagination
List<ItemStack> rewards = getRewards();

BuildGUI.paginated("Rewards", rewards, 54)
    .setItemsPerPage(45)  // 45 items, 9 slots for navigation
    .setStaticItem(49, closeButton, p -> p.closeInventory())
    .onClose(player -> saveProgress(player))
    .open(player);

// Custom navigation buttons
ItemStack nextBtn = createItem(Material.ARROW, "&aNext →");
ItemStack prevBtn = createItem(Material.ARROW, "&c← Previous");

BuildGUI.paginated("Items", items, 54)
    .setNavigationItems(nextBtn, 53, prevBtn, 45)
    .open(player);
```

### Dynamic GUI Updates

```java
// Update items in open GUI
BuildGUI.GUIInstance gui = BuildGUI.create("Live Stats", 3)
    .setItem(13, statsItem)
    .open(player);

// Later, update the GUI
SchedulerHelper.runTimerSeconds(plugin, () -> {
    ItemStack updated = createStatsItem(player);
    gui.updateItem(13, updated);
    gui.refresh();
}, 0, 1); // Update every second
```

### GUI Management

```java
// Check if player has GUI open
if (BuildGUI.hasGUIOpen(player)) {
    player.sendMessage("Close your current GUI first!");
}

// Close player's GUI
BuildGUI.close(player);

// Close all GUIs
BuildGUI.closeAll();

// Get player's active GUI
BuildGUI.GUIInstance gui = BuildGUI.getGUI(player);
if (gui != null) {
    gui.updateItem(10, newItem);
}
```

### Complete Shop Example

```java
public void openShop(Player player) {
    ItemStack diamond = createShopItem(Material.DIAMOND, "&bDiamond", 100);
    ItemStack emerald = createShopItem(Material.EMERALD, "&aEmerald", 50);
    ItemStack gold = createShopItem(Material.GOLD_INGOT, "&eGold", 25);
    
    BuildGUI.create("Item Shop", 5)
        .fillBorder(new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
        .setItem(20, diamond, p -> buyItem(p, "diamond", 100))
        .setItem(22, emerald, p -> buyItem(p, "emerald", 50))
        .setItem(24, gold, p -> buyItem(p, "gold", 25))
        .setItem(40, infoItem)
        .setItem(44, closeItem, p -> p.closeInventory())
        .onClose(p -> saveShopData(p))
        .open(player);
}

private void buyItem(Player player, String item, int cost) {
    if (Economy.hasMoney(player, cost)) {
        Economy.removeMoney(player, cost);
        player.sendMessage("Purchased " + item + " for $" + cost);
        player.closeInventory();
    } else {
        player.sendMessage("Not enough money!");
    }
}
```

### Complete Paginated Example

```java
public void showAllPlayers(Player viewer) {
    List<ItemStack> playerHeads = new ArrayList<>();
    
    for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(op);
        meta.setDisplayName(ColorConverter.colorize("&e" + op.getName()));
        meta.setLore(Arrays.asList(
            ColorConverter.colorize("&7Last seen: " + getLastSeen(op)),
            ColorConverter.colorize("&7Click to teleport")
        ));
        head.setItemMeta(meta);
        playerHeads.add(head);
    }
    
    BuildGUI.PaginatedGUIInstance paginatedGUI = BuildGUI.paginated("All Players", playerHeads, 54)
        .setItemsPerPage(45)
        .setStaticItem(49, createCloseButton(), p -> p.closeInventory())
        .open(viewer);
    
    // Can navigate pages later
    // paginatedGUI.openPage(viewer, 2); // Go to page 3
}
```

### Best Practices

```java
// Always initialize BuildGUI in onEnable
BuildGUI.init(this);

// Clean up on disable (optional, auto-handled)
@Override
public void onDisable() {
    BuildGUI.closeAll();
}

// Use colors in titles
BuildGUI.create("&6&lAWESOME SHOP", 3) // Auto-colorizes
    .open(player);

// Cancel clicks to prevent item theft
BuildGUI.create("Display", 3)
    .cancelClicks(true)  // Default: true
    .open(player);
```

---

## 7. ColorConverter

**Purpose**: Convert and manage color codes for Minecraft chat, supporting both legacy (`&`) and hex colors (`&#RRGGBB`).

### Basic Color Conversion

```java
// Convert & codes to § (section sign)
String colored = ColorConverter.translateAmpersandToSection("&aHello &bWorld");
// Result: "§aHello §bWorld"

// Convert § back to &
String ampersand = ColorConverter.translateSectionToAmpersand("§aHello §bWorld");
// Result: "&aHello &bWorld"
```

### Hex Color Support (1.16+)

```java
// Convert hex colors &#RRGGBB to Minecraft format
String hexColored = ColorConverter.colorize("&#FF5733This is orange &#00FF00and this is green");
// Also converts & codes automatically

// Example with gradient
String gradient = ColorConverter.colorize("&#FF0000R&#FF7F00a&#FFFF00i&#00FF00n&#0000FFb&#4B0082o&#9400D3w");
```

### Strip Colors

```java
// Remove all color codes from text
String plain = ColorConverter.stripColor("§aHello §bWorld");
// Result: "Hello World"

// Works with hex colors too
String stripped = ColorConverter.stripColor("§x§F§F§5§7§3§3Orange text");
// Result: "Orange text"
```

### Common Use Cases

```java
// Colorize config messages
String msg = ColorConverter.colorize(config.getString("messages.welcome"));
player.sendMessage(msg);

// Compare text without colors
String input = "§aApple";
String compare = "Apple";
if (ColorConverter.stripColor(input).equalsIgnoreCase(compare)) {
    // Match!
}

// Store colored text in config (use & format)
String stored = ColorConverter.translateSectionToAmpersand(coloredText);
config.set("message", stored);
```

---

## 8. CommandExecuter

**Purpose**: Execute commands as console or players, with support for silent execution and command validation.

### Basic Command Execution

```java
// Run command as console
CommandExecuter.runAsConsole("say Hello from console!");
CommandExecuter.runAsConsole("give PlayerName diamond 64");

// Run command as player
Player player = ...;
CommandExecuter.runAsPlayer(player, "spawn");
CommandExecuter.runAsPlayer(player, "kit starter");
```

### Silent Execution

```java
// Run command without console output
CommandExecuter.runConsoleSilently("say This won't show in console");

// Run player command silently
CommandExecuter.runPlayerSilently(player, "tp 0 100 0");

// Generic silent execution
CommandExecuter.runSilently(sender, "command");
```

### Command Validation

```java
// Check if command exists
if (CommandExecuter.commandExists("spawn")) {
    // Command is registered
}

// Check if player can execute command
if (CommandExecuter.canExecute(player, "gamemode")) {
    CommandExecuter.runAsPlayer(player, "gamemode creative");
}
```

### Batch Execution

```java
// Run multiple commands at once
List<String> commands = Arrays.asList(
    "say Starting server setup...",
    "time set day",
    "weather clear",
    "say Setup complete!"
);
CommandExecuter.runBatch(commands, true); // true = as console
```

### Command History

```java
// Enable command tracking
CommandExecuter.enableHistory();

// Get command history
List<String> history = CommandExecuter.getHistory();
for (String cmd : history) {
    ConsoleLog.info("Executed: " + cmd);
}

// Clear history
CommandExecuter.clearHistory();
```

### Advanced Features

```java
// Get command sender type
CommandSender sender = ...;
if (CommandExecuter.isConsole(sender)) {
    // Sender is console
}

// Run command with result check
boolean success = CommandExecuter.runAsConsole("reload");
if (success) {
    ConsoleLog.info("Reload successful");
} else {
    ConsoleLog.warn("Reload failed");
}
```

### Batch Command Execution

```java
// Run multiple commands as console
int count = CommandExecuter.runMultipleAsConsole(
    "gamemode creative Steve",
    "tp Steve 0 100 0",
    "give Steve diamond 64"
);
ConsoleLog.info(count + " commands executed successfully");

// Run multiple commands as player
Player player = ...;
int executed = CommandExecuter.runMultipleAsPlayer(player,
    "spawn",
    "heal",
    "feed"
);

// Run multiple commands silently (no output)
int silent = CommandExecuter.runMultipleSilently(player,
    "clear @s",
    "effect clear @s"
);
```

### History Management

```java
// Control history tracking
CommandExecuter.setHistoryTracking(true);  // Enable tracking
CommandExecuter.setHistoryTracking(false); // Disable tracking

// Check if tracking is enabled
if (CommandExecuter.isHistoryTracking()) {
    ConsoleLog.info("Command history is being tracked");
}

// Get full command history
List<String> fullHistory = CommandExecuter.getCommandHistory();
ConsoleLog.info("Total commands executed: " + fullHistory.size());

// Get last N commands
List<String> recent = CommandExecuter.getLastCommands(10);
for (String cmd : recent) {
    ConsoleLog.info("Recent: " + cmd);
}
```

### Command Introspection

```java
// Get all registered commands
List<String> allCommands = CommandExecuter.getAllCommands();
ConsoleLog.info("Available commands: " + allCommands.size());

// Get command aliases
List<String> aliases = CommandExecuter.getCommandAliases("gamemode");
// Returns: ["gm", "gamemode"]

// Generic command dispatch
CommandSender sender = ...;
boolean dispatched = CommandExecuter.dispatchCommand(sender, "help 1");
```

---

## 9. CommandHelper

**Purpose**: Simplified command registration with lambda support, eliminating CommandExecutor boilerplate.

### Basic Command Registration

```java
@Override
public void onEnable() {
    // Register command with lambda
    CommandHelper.register(this, "multimedia", (sender, label, args) -> {
        sender.sendMessage("Hello from Multimedia!");
        return true;
    });
}
```

### Handling Arguments

```java
CommandHelper.register(this, "teleport", (sender, label, args) -> {
    if (args.length < 3) {
        sender.sendMessage("Usage: /teleport <x> <y> <z>");
        return false;
    }
    
    if (!(sender instanceof Player)) {
        sender.sendMessage("Only players can teleport!");
        return false;
    }
    
    Player player = (Player) sender;
    try {
        double x = Double.parseDouble(args[0]);
        double y = Double.parseDouble(args[1]);
        double z = Double.parseDouble(args[2]);
        
        Location loc = new Location(player.getWorld(), x, y, z);
        player.teleport(loc);
        player.sendMessage("Teleported to " + x + ", " + y + ", " + z);
        return true;
    } catch (NumberFormatException e) {
        sender.sendMessage("Invalid coordinates!");
        return false;
    }
});
```

### Subcommands

```java
CommandHelper.register(this, "admin", (sender, label, args) -> {
    if (args.length == 0) {
        sender.sendMessage("Usage: /admin <reload|info|clear>");
        return false;
    }
    
    switch (args[0].toLowerCase()) {
        case "reload":
            reloadConfig();
            sender.sendMessage("Config reloaded!");
            return true;
            
        case "info":
            sender.sendMessage("Server version: " + Bukkit.getVersion());
            sender.sendMessage("Players online: " + Bukkit.getOnlinePlayers().size());
            return true;
            
        case "clear":
            if (sender instanceof Player) {
                ((Player) sender).getInventory().clear();
                sender.sendMessage("Inventory cleared!");
            }
            return true;
            
        default:
            sender.sendMessage("Unknown subcommand: " + args[0]);
            return false;
    }
});
```

### Permission Checks

```java
CommandHelper.register(this, "op-command", (sender, label, args) -> {
    if (!sender.hasPermission("plugin.admin")) {
        sender.sendMessage(ChatColor.RED + "No permission!");
        return false;
    }
    
    // Admin-only logic here
    sender.sendMessage("Admin command executed!");
    return true;
});
```

### Error Handling

```java
// CommandHelper automatically catches exceptions and logs them
CommandHelper.register(this, "risky", (sender, label, args) -> {
    // If this throws an exception, it will be caught and logged
    performRiskyOperation();
    sender.sendMessage("Operation completed!");
    return true;
});
```

### Requirements

**Important**: Commands must be declared in `plugin.yml` before registration:

```yaml
commands:
  multimedia:
    description: Main plugin command
    usage: /<command>
  teleport:
    description: Teleport command
    usage: /teleport <x> <y> <z>
  admin:
    description: Admin commands
    usage: /admin <subcommand>
```

---

## 10. ConfigHelp

**Purpose**: Simplified config.yml management with automatic defaults and type-safe getters.

### Initialization

```java
@Override
public void onEnable() {
    // Initialize ConfigHelp (loads or creates config.yml)
    ConfigHelp.init(this);
}
```

### Setting Defaults

```java
// Create default config values
Map<String, Object> defaults = new HashMap<>();
defaults.put("messages.welcome", "&aWelcome to the server!");
defaults.put("messages.goodbye", "&cGoodbye!");
defaults.put("settings.max-players", 100);
defaults.put("settings.enabled", true);
defaults.put("settings.cooldown", 5.5);

// Apply defaults (won't overwrite existing values)
ConfigHelp.ensureDefaults(defaults);

// Single default
ConfigHelp.ensureDefault("prefix", "[Server]");
```

### Reading Values

```java
// String with fallback
String welcome = ConfigHelp.getString("messages.welcome", "Default welcome");

// Integer with fallback
int maxPlayers = ConfigHelp.getInt("settings.max-players", 50);

// Boolean with fallback
boolean enabled = ConfigHelp.getBoolean("settings.enabled", true);

// Double with fallback
double cooldown = ConfigHelp.getDouble("settings.cooldown", 3.0);

// String list
List<String> allowedWorlds = ConfigHelp.getStringList("allowed-worlds");
```

### Writing Values

```java
// Set value and save immediately
ConfigHelp.set("last-restart", System.currentTimeMillis());
ConfigHelp.set("motd", "&6Welcome to &bMy Server");

// Check if key exists
if (ConfigHelp.has("custom-setting")) {
    // Key exists in config
}
```

### Reload Config

```java
// Reload config from disk
ConfigHelp.reload();

// Save config manually
ConfigHelp.save();
```

### Complete Example

```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Initialize
        ConfigHelp.init(this);
        
        // Setup defaults
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("messages.welcome", "&aWelcome!");
        defaults.put("messages.goodbye", "&cSee you later!");
        defaults.put("settings.spawn-protection", 16);
        defaults.put("settings.pvp-enabled", true);
        defaults.put("features.fly", false);
        defaults.put("features.godmode", false);
        
        ConfigHelp.ensureDefaults(defaults);
        
        // Use config values
        boolean pvp = ConfigHelp.getBoolean("settings.pvp-enabled", true);
        getLogger().info("PVP enabled: " + pvp);
    }
    
    public void sendWelcome(Player player) {
        String msg = ConfigHelp.getString("messages.welcome", "Welcome!");
        String colored = ColorConverter.colorize(msg);
        player.sendMessage(colored);
    }
}
```

### Best Practices

```java
// Always provide fallback values
String value = ConfigHelp.getString("path", "fallback");

// Use descriptive paths
ConfigHelp.set("economy.starting-balance", 1000);
ConfigHelp.set("world.pvp-worlds", Arrays.asList("world", "world_nether"));

// Check existence before reading
if (ConfigHelp.has("optional-feature")) {
    int value = ConfigHelp.getInt("optional-feature", 0);
    // Use value
}
```

---

## 11. ConsoleLog

**Purpose**: Simplified logging with log levels and formatted output.

### Initialization

```java
@Override
public void onEnable() {
    // Initialize with your plugin
    ConsoleLog.init(this);
}
```

### Basic Logging

```java
// Info messages
ConsoleLog.info("Plugin started successfully");

// Warnings
ConsoleLog.warn("Config file is outdated");

// Errors
ConsoleLog.error("Failed to connect to database");

// Error with exception
try {
    riskyOperation();
} catch (Exception e) {
    ConsoleLog.error("Operation failed", e);
}
```

### Formatted Logging

```java
// Printf-style formatting
ConsoleLog.infof("Loaded %d players in %.2f seconds", count, time);
ConsoleLog.warnf("Player %s exceeded limit: %d/%d", name, current, max);
ConsoleLog.errorf("Invalid value: %s (expected: %s)", value, expected);
```

### Log Levels

```java
// Set minimum log level
ConsoleLog.setLevel(ConsoleLog.LogLevel.INFO);    // Default: logs everything
ConsoleLog.setLevel(ConsoleLog.LogLevel.WARNING); // Only warnings and errors
ConsoleLog.setLevel(ConsoleLog.LogLevel.ERROR);   // Only errors

// Get current level
ConsoleLog.LogLevel level = ConsoleLog.getLevel();
```

### Complete Example

```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        ConsoleLog.init(this);
        ConsoleLog.info("Starting initialization...");
        
        try {
            loadConfig();
            ConsoleLog.info("Config loaded successfully");
            
            connectDatabase();
            ConsoleLog.info("Database connected");
            
            int playerCount = loadPlayers();
            ConsoleLog.infof("Loaded %d players", playerCount);
            
        } catch (Exception e) {
            ConsoleLog.error("Initialization failed", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        ConsoleLog.info("Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        ConsoleLog.info("Plugin disabled");
    }
}
```

---

## 12. CooldownManager

**Purpose**: Manage player cooldowns with flexible time units and formatted display.

### Basic Cooldown Usage

```java
// Set cooldown in various time units
CooldownManager.setCooldownSeconds(player, "daily-reward", 86400); // 24 hours
CooldownManager.setCooldownMinutes(player, "vote", 60); // 1 hour
CooldownManager.setCooldownHours(player, "skill", 2); // 2 hours
CooldownManager.setCooldownDays(player, "weekly-bonus", 7); // 7 days

// Set with TimeUnit
CooldownManager.setCooldown(player, "custom", 30, TimeUnit.MINUTES);
```

### Checking Cooldowns

```java
// Check if player has active cooldown
if (CooldownManager.hasCooldown(player, "daily-reward")) {
    player.sendMessage("You already claimed your daily reward!");
    return;
}

// Get remaining time
long remainingSeconds = CooldownManager.getRemainingSeconds(player, "vote");
long remainingMinutes = CooldownManager.getRemainingMinutes(player, "skill");
long remainingMillis = CooldownManager.getRemainingMillis(player, "custom");

// Get formatted time string
String timeLeft = CooldownManager.getRemainingFormatted(player, "daily-reward");
player.sendMessage("Come back in " + timeLeft);
// Output examples: "5s", "2m 30s", "1h 45m", "3d 12h"
```

### Resetting Cooldowns

```java
// Reset specific cooldown
CooldownManager.resetCooldown(player, "daily-reward");

// Reset all cooldowns for a player
CooldownManager.resetAllCooldowns(player);

// Clear all players from a specific cooldown key
CooldownManager.clearKey("vote");

// Clear all cooldowns for all players
CooldownManager.clearAll();
```

### Check and Set Pattern

```java
// Check cooldown and set it if not present (returns false if on cooldown)
if (!CooldownManager.checkAndSetCooldown(player, "command", 5, TimeUnit.SECONDS)) {
    String remaining = CooldownManager.getRemainingFormatted(player, "command");
    player.sendMessage("Please wait " + remaining + " before using this again");
    return;
}

// Command executed successfully
player.sendMessage("Command executed!");
```

### Advanced Features

```java
// Get all active cooldowns for a player
Map<String, Long> cooldowns = CooldownManager.getAllCooldowns(player);
for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
    String key = entry.getKey();
    String timeLeft = CooldownManager.getRemainingFormatted(player, key);
    player.sendMessage(key + ": " + timeLeft);
}

// Count active cooldowns
int activeCount = CooldownManager.getActiveCooldownCount(player);
player.sendMessage("You have " + activeCount + " active cooldowns");

// Get cooldown progress (for progress bars)
double progress = CooldownManager.getProgressPercent(player, "skill");
// Returns 0-100 representing percentage complete

// Cleanup expired cooldowns (automatic, but can call manually)
CooldownManager.cleanup();
```

### UUID-Based Operations

```java
// All methods work with UUIDs directly
UUID uuid = player.getUniqueId();

CooldownManager.setCooldown(uuid, "key", 60, TimeUnit.SECONDS);
boolean hasCooldown = CooldownManager.hasCooldown(uuid, "key");
long remaining = CooldownManager.getRemainingMillis(uuid, "key");
String formatted = CooldownManager.getRemainingFormatted(uuid, "key");
CooldownManager.resetCooldown(uuid, "key");
```

### Practical Examples

```java
// Daily reward command
CommandHelper.register(plugin, "daily", (sender, label, args) -> {
    if (!(sender instanceof Player)) return true;
    Player player = (Player) sender;
    
    if (CooldownManager.hasCooldown(player, "daily-reward")) {
        String timeLeft = CooldownManager.getRemainingFormatted(player, "daily-reward");
        player.sendMessage(ColorConverter.colorize(
            "&cYou already claimed your daily reward! Come back in &e" + timeLeft
        ));
        return true;
    }
    
    // Give reward
    player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
    player.sendMessage(ColorConverter.colorize("&a+5 Diamonds! Daily reward claimed!"));
    
    // Set 24 hour cooldown
    CooldownManager.setCooldownHours(player, "daily-reward", 24);
    return true;
});

// Combat logging prevention
private final Set<UUID> recentCombat = new HashSet<>();

@EventHandler
public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Set 15 second combat tag
        CooldownManager.setCooldownSeconds(attacker, "combat", 15);
        CooldownManager.setCooldownSeconds(victim, "combat", 15);
    }
}

@EventHandler
public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    
    if (CooldownManager.hasCooldown(player, "combat")) {
        // Player left during combat
        player.setHealth(0); // Kill player
        Bukkit.broadcastMessage(player.getName() + " logged out during combat!");
    }
}

// Skill cooldown system
public void useSkill(Player player, String skillName, int cooldownSeconds, Runnable skillAction) {
    String cooldownKey = "skill-" + skillName.toLowerCase();
    
    if (CooldownManager.hasCooldown(player, cooldownKey)) {
        String timeLeft = CooldownManager.getRemainingFormatted(player, cooldownKey);
        player.sendMessage(ColorConverter.colorize(
            "&cSkill on cooldown! Wait " + timeLeft
        ));
        return;
    }
    
    // Use skill
    skillAction.run();
    player.sendMessage(ColorConverter.colorize("&aSkill used!"));
    
    // Set cooldown
    CooldownManager.setCooldownSeconds(player, cooldownKey, cooldownSeconds);
}

// Usage:
useSkill(player, "Fireball", 30, () -> {
    player.launchProjectile(Fireball.class);
});

// Teleport warmup with cooldown
public void teleportWithWarmup(Player player, Location destination) {
    if (CooldownManager.hasCooldown(player, "tp-cooldown")) {
        String timeLeft = CooldownManager.getRemainingFormatted(player, "tp-cooldown");
        player.sendMessage("Teleport on cooldown: " + timeLeft);
        return;
    }
    
    if (CooldownManager.hasCooldown(player, "tp-warmup")) {
        player.sendMessage("Teleport already in progress!");
        return;
    }
    
    // Set 3 second warmup
    CooldownManager.setCooldownSeconds(player, "tp-warmup", 3);
    player.sendMessage("Teleporting in 3 seconds... Don't move!");
    
    Location startLoc = player.getLocation().clone();
    
    SchedulerHelper.runLaterSeconds(plugin, () -> {
        if (player.getLocation().distance(startLoc) > 0.5) {
            player.sendMessage("Teleport cancelled - you moved!");
            CooldownManager.resetCooldown(player, "tp-warmup");
            return;
        }
        
        player.teleport(destination);
        player.sendMessage("Teleported!");
        
        CooldownManager.resetCooldown(player, "tp-warmup");
        CooldownManager.setCooldownMinutes(player, "tp-cooldown", 5); // 5 min cooldown
    }, 3);
}

// Show all cooldowns GUI
public void showCooldownsGUI(Player player) {
    Map<String, Long> cooldowns = CooldownManager.getAllCooldowns(player);
    
    if (cooldowns.isEmpty()) {
        player.sendMessage("You have no active cooldowns!");
        return;
    }
    
    BuildGUI.create("&6Your Cooldowns", 3)
        .fill(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
    
    int slot = 10;
    for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
        String key = entry.getKey();
        String timeLeft = CooldownManager.getRemainingFormatted(player, key);
        
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorConverter.colorize("&e" + key));
        meta.setLore(Arrays.asList(
            ColorConverter.colorize("&7Time remaining:"),
            ColorConverter.colorize("&a" + timeLeft)
        ));
        item.setItemMeta(meta);
        
        BuildGUI.setItem(slot++, item);
    }
    
    BuildGUI.open(player);
}
```

### Best Practices

```java
// Use consistent naming for cooldown keys
String DAILY_REWARD = "daily-reward";
String COMBAT_TAG = "combat";
String TP_COOLDOWN = "tp-cooldown";

// Clean up cooldowns periodically
SchedulerHelper.runTimerMinutes(plugin, () -> {
    CooldownManager.cleanup();
}, 5, 5); // Every 5 minutes

// Reset cooldowns on player logout if needed
@EventHandler
public void onQuit(PlayerQuitEvent event) {
    // Keep combat tags, remove everything else
    Player player = event.getPlayer();
    Map<String, Long> cooldowns = CooldownManager.getAllCooldowns(player);
    
    for (String key : cooldowns.keySet()) {
        if (!key.equals("combat")) {
            CooldownManager.resetCooldown(player, key);
        }
    }
}
```

---

## 13. ErrorHandler

**Purpose**: Centralized error handling with detailed stack trace logging.

### Basic Error Handling

```java
// Handle exception with context
try {
    loadData();
} catch (Exception e) {
    ErrorHandler.handle(plugin, "Failed to load data", e);
}

// Handle exception without context
try {
    saveData();
} catch (Exception e) {
    ErrorHandler.handle(plugin, e);
}
```

### Warning Messages

```java
// Log warning with exception
try {
    optionalFeature();
} catch (Exception e) {
    ErrorHandler.warn(plugin, "Optional feature unavailable", e);
}
```

### Info Messages

```java
// Log informational message
ErrorHandler.info(plugin, "Data migration completed");
```

### Complete Example

```java
public void processPlayerData(Player player) {
    try {
        // Risky operation
        PlayerData data = loadPlayerData(player.getUniqueId());
        data.update();
        savePlayerData(data);
        
    } catch (IOException e) {
        ErrorHandler.handle(plugin, "IO error while processing player data", e);
        player.sendMessage(ChatColor.RED + "An error occurred. Please contact an admin.");
        
    } catch (Exception e) {
        ErrorHandler.handle(plugin, "Unexpected error in player data processing", e);
    }
}

public void initializeFeature() {
    try {
        setupFeature();
        ErrorHandler.info(plugin, "Feature initialized successfully");
    } catch (UnsupportedOperationException e) {
        ErrorHandler.warn(plugin, "Feature not supported on this server version", e);
    } catch (Exception e) {
        ErrorHandler.handle(plugin, "Failed to initialize feature", e);
        throw e; // Re-throw if critical
    }
}
```

### Stack Trace Details

ErrorHandler automatically logs:
- Full exception message
- Complete stack trace
- Cause chain (if present)
- Package context for easier debugging

---

## 14. EntityHelper

**Purpose**: Entity spawn, manipulation, health management, AI control, and proximity utilities.

### Spawning Entities

```java
// Spawn entity at location
Location loc = player.getLocation();
Zombie zombie = EntityHelper.spawn(loc, EntityType.ZOMBIE);

// Spawn with AI disabled
Creeper creeper = EntityHelper.spawnWithAI(loc, EntityType.CREEPER, false);

// Spawn custom entity with properties
Sheep sheep = EntityHelper.spawnCustom(loc, EntityType.SHEEP, entity -> {
    entity.setCustomName("§6Golden Sheep");
    entity.setCustomNameVisible(true);
});
```

### Custom Names

```java
// Set custom name and visibility
EntityHelper.setCustomName(zombie, "§cBoss Zombie", true);

// Get custom name
String name = EntityHelper.getCustomName(zombie);
// Returns: "§cBoss Zombie"

// Show/hide name
EntityHelper.setNameVisible(zombie, false);
```

### Health Management

```java
// Set health
EntityHelper.setHealth(zombie, 50.0);

// Set max health
EntityHelper.setMaxHealth(zombie, 100.0);

// Heal entity
EntityHelper.heal(zombie); // Full heal
EntityHelper.heal(zombie, 20.0); // Heal by amount

// Get health
double health = EntityHelper.getHealth(zombie);
double maxHealth = EntityHelper.getMaxHealth(zombie);
```

### Entity Properties

```java
// Make entity invulnerable
EntityHelper.makeInvulnerable(zombie, true);

// Silent entity (no sounds)
EntityHelper.setSilent(zombie, true);

// Disable gravity
EntityHelper.setGravity(zombie, false);

// Make entity glow
EntityHelper.setGlowing(zombie, true);

// Make invisible
EntityHelper.makeInvisible(zombie, true);
```

### AI Control

```java
// Remove AI completely
EntityHelper.removeAI(zombie);

// Set aggressive (for mobs)
EntityHelper.setAggressive(zombie, true);

// Set target
EntityHelper.setTarget(zombie, player);
```

### Entity Removal

```java
// Remove single entity
EntityHelper.remove(zombie);

// Remove nearby entities of type
int removed = EntityHelper.removeNearby(loc, EntityType.ZOMBIE, 50.0);

// Remove all entities in radius
int total = EntityHelper.removeInRadius(loc, 20.0);
```

### Queries

```java
// Get entities in radius
List<Entity> entities = EntityHelper.getEntitiesInRadius(loc, 30.0);

// Get nearby entities of type
List<Zombie> zombies = EntityHelper.getNearbyEntities(loc, EntityType.ZOMBIE, 50.0);

// Check if entity has custom name
boolean isCustom = EntityHelper.isCustomEntity(zombie);
```

### Movement Speed

```java
// Set movement speed
EntityHelper.setMovementSpeed(zombie, 0.5); // Double speed

// Get movement speed
double speed = EntityHelper.getMovementSpeed(zombie);
```

### Complete Example

```java
// Spawn custom boss
Location spawnLoc = player.getLocation().add(10, 0, 0);

Zombie boss = EntityHelper.spawnCustom(spawnLoc, EntityType.ZOMBIE, zombie -> {
    // Appearance
    EntityHelper.setCustomName(zombie, "§4§lBoss Zombie", true);
    EntityHelper.setGlowing(zombie, true);
    
    // Stats
    EntityHelper.setMaxHealth(zombie, 200.0);
    EntityHelper.setHealth(zombie, 200.0);
    EntityHelper.setMovementSpeed(zombie, 0.4);
    
    // Properties
    EntityHelper.makeInvulnerable(zombie, false);
    EntityHelper.setAggressive(zombie, true);
    
    // Equipment
    zombie.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
});

// Cleanup after 5 minutes
SchedulerHelper.runLater(plugin, () -> {
    if (boss != null && !boss.isDead()) {
        EntityHelper.remove(boss);
    }
}, 6000L);
```

---

## 15. DiscordWebhooks

**Purpose**: Send formatted messages and rich embeds to Discord webhooks for server notifications and logging.

### Basic Usage

```java
// Send simple text message
DiscordWebhooks.send("https://discord.com/api/webhooks/...", "Server started!");

// Send with custom username
DiscordWebhooks.send(webhookUrl, "Player joined!", "Server Bot");

// Send with custom username and avatar
DiscordWebhooks.send(webhookUrl, "Hello Discord!", "Bot", "https://i.imgur.com/avatar.png");
```

### Rich Embeds

```java
// Create rich embed
DiscordWebhooks.embed(webhookUrl)
    .title("Server Status")
    .description("The server is now online!")
    .color(Color.GREEN)
    .timestamp()
    .send();

// Embed with fields
DiscordWebhooks.embed(webhookUrl)
    .title("Player Statistics")
    .addField("Players Online", "15/100", true)
    .addField("TPS", "19.8", true)
    .addField("Memory", "2GB/4GB", true)
    .color(Color.CYAN)
    .timestamp()
    .send();
```

### Embed Builder Options

```java
// Complete embed example
DiscordWebhooks.embed(webhookUrl)
    .title("Server Event", "https://yourserver.com")
    .description("A special event has started!")
    .color(255, 215, 0)  // RGB color
    .author("Server Admin", "https://example.com", "https://i.imgur.com/admin.png")
    .thumbnail("https://i.imgur.com/event.png")
    .image("https://i.imgur.com/banner.png")
    .addField("Event Name", "Summer Festival", false)
    .addField("Duration", "24 hours", true)
    .addField("Rewards", "Special items", true)
    .footer("Server Events", "https://i.imgur.com/icon.png")
    .timestamp()
    .send();
```

### Color Options

```java
// Using java.awt.Color
DiscordWebhooks.embed(webhookUrl)
    .title("Error")
    .color(Color.RED)
    .send();

// Using RGB
DiscordWebhooks.embed(webhookUrl)
    .title("Warning")
    .color(255, 165, 0)  // Orange
    .send();

// Using hex
DiscordWebhooks.embed(webhookUrl)
    .title("Info")
    .colorHex(0x00FF00)  // Green
    .send();
```

### Preset Notifications

```java
// Server lifecycle
DiscordWebhooks.sendServerStart(webhookUrl);
DiscordWebhooks.sendServerStop(webhookUrl);

// Player events
DiscordWebhooks.sendPlayerJoin(webhookUrl, "Notch");
DiscordWebhooks.sendPlayerLeave(webhookUrl, "Notch");

// Status messages
DiscordWebhooks.sendSuccess(webhookUrl, "Backup completed successfully");
DiscordWebhooks.sendError(webhookUrl, "Database connection failed");
DiscordWebhooks.sendWarning(webhookUrl, "Server TPS is low");
DiscordWebhooks.sendInfo(webhookUrl, "Maintenance scheduled for tomorrow");
```

### Player Notifications

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    DiscordWebhooks.embed(webhookUrl)
        .title("📥 Player Joined")
        .description("**" + player.getName() + "** joined the server")
        .thumbnail("https://minotar.net/avatar/" + player.getName() + "/100.png")
        .addField("UUID", player.getUniqueId().toString(), false)
        .addField("IP", player.getAddress().getHostString(), true)
        .addField("Gamemode", player.getGameMode().toString(), true)
        .color(Color.GREEN)
        .timestamp()
        .send();
}
```

### Error Logging

```java
try {
    performRiskyOperation();
} catch (Exception e) {
    DiscordWebhooks.embed(webhookUrl)
        .title("❌ Server Error")
        .description("```" + e.getMessage() + "```")
        .addField("Exception Type", e.getClass().getSimpleName(), false)
        .addField("Stack Trace", getStackTrace(e), false)
        .color(Color.RED)
        .footer("Error Log")
        .timestamp()
        .send();
}
```

### Async Sending

```java
// Send asynchronously to avoid blocking main thread
DiscordWebhooks.embed(webhookUrl)
    .title("Heavy Operation")
    .description("Processing large data...")
    .color(Color.BLUE)
    .sendAsync();  // Non-blocking
```

### Complete Monitoring Example

```java
public class DiscordMonitor {
    private final String webhookUrl;
    
    public DiscordMonitor(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public void sendServerStats() {
        JavaUtilities.MemoryInfo mem = JavaUtilities.getMemoryUsage();
        int players = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        
        DiscordWebhooks.embed(webhookUrl)
            .title("📊 Server Statistics")
            .addField("Players", players + "/" + maxPlayers, true)
            .addField("Memory", mem.getUsedMB() + "MB / " + mem.getMaxMB() + "MB", true)
            .addField("Usage", String.format("%.1f%%", mem.getUsedPercent()), true)
            .addField("Uptime", getUptime(), true)
            .addField("TPS", String.format("%.2f", getTPS()), true)
            .addField("Worlds", String.valueOf(Bukkit.getWorlds().size()), true)
            .color(getStatusColor(mem.getUsedPercent()))
            .thumbnail("https://i.imgur.com/server-icon.png")
            .footer("Auto-update every 5 minutes")
            .timestamp()
            .sendAsync();
    }
    
    private Color getStatusColor(double memoryPercent) {
        if (memoryPercent > 90) return Color.RED;
        if (memoryPercent > 75) return Color.ORANGE;
        return Color.GREEN;
    }
    
    public void sendChatLog(Player player, String message) {
        DiscordWebhooks.embed(webhookUrl)
            .author(player.getName(), null, 
                "https://minotar.net/avatar/" + player.getName() + "/100.png")
            .description(message)
            .color(Color.WHITE)
            .timestamp()
            .sendAsync();
    }
    
    public void sendPunishment(String staff, String player, String reason, String type) {
        DiscordWebhooks.embed(webhookUrl)
            .title("🔨 " + type)
            .addField("Player", player, true)
            .addField("Staff", staff, true)
            .addField("Reason", reason, false)
            .color(Color.RED)
            .timestamp()
            .send();
    }
}
```

### Best Practices

```java
// Store webhook URL in config
String webhookUrl = ConfigHelp.getString("discord.webhook-url", "");

// Always send async for non-critical messages
DiscordWebhooks.embed(webhookUrl)
    .title("Player Achievement")
    .sendAsync();  // Don't block server

// Use embeds for rich information
// Use plain text for simple notifications

// Handle errors gracefully
boolean sent = DiscordWebhooks.send(webhookUrl, "Test");
if (!sent) {
    ConsoleLog.warn("Failed to send Discord notification");
}
```

---

## 16. InventoryHelper

**Purpose**: Simplified inventory and item management with version-aware material handling.

### Creating Inventories

```java
// Create custom inventory
Inventory inv = InventoryHelper.createInventory(player, "My Custom GUI", 3); // 3 rows

// Open inventory for player
InventoryHelper.openInventory(player, inv);
```

### Giving Items

```java
// Give item to player (drops excess if inventory full)
ItemStack diamond = new ItemStack(Material.DIAMOND, 64);
boolean allAdded = InventoryHelper.giveItem(player, diamond);

if (!allAdded) {
    player.sendMessage("Some items were dropped!");
}

// Give with leftover handling
ItemStack leftover = InventoryHelper.giveItemAndReturnLeftover(player, diamond);
if (leftover != null) {
    player.sendMessage("Couldn't fit " + leftover.getAmount() + " items");
}
```

### Taking Items

```java
// Take specific amount of material
ItemStack taken = InventoryHelper.takeItem(player, Material.GOLD_INGOT, 10);
if (taken != null) {
    player.sendMessage("Took " + taken.getAmount() + " gold ingots");
}

// Check if player has enough
if (InventoryHelper.hasItem(player, Material.DIAMOND, 5)) {
    // Player has at least 5 diamonds
}

// Remove all of a material
int removed = InventoryHelper.removeAll(player, Material.DIRT);
player.sendMessage("Removed " + removed + " dirt blocks");
```

### Inventory Queries

```java
// Find first empty slot
int emptySlot = InventoryHelper.findFirstEmptySlot(inventory);

// Find slots containing an item
List<Integer> slots = InventoryHelper.findSlots(inventory, itemStack);

// Count items
int count = InventoryHelper.countItem(player, Material.EMERALD);
player.sendMessage("You have " + count + " emeralds");

// Check if inventory is full
if (InventoryHelper.isInventoryFull(player)) {
    player.sendMessage("Your inventory is full!");
}
```

### Item Serialization

```java
// Serialize item to Base64
ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
item.getItemMeta().setDisplayName("Legendary Sword");
String serialized = InventoryHelper.serializeItem(item);

// Save to config
config.set("items.legendary-sword", serialized);

// Deserialize back
String data = config.getString("items.legendary-sword");
ItemStack restored = InventoryHelper.deserializeItem(data);

// Serialize entire inventory
String invData = InventoryHelper.serializeInventory(player.getInventory());

// Deserialize and restore
InventoryHelper.deserializeInventory(invData, player.getInventory());
```

### Material Handling

```java
// Get material with version fallbacks
Material mat = InventoryHelper.getMaterial("OAK_PLANKS"); // Works on 1.7-1.20+

// Create ItemStack with version-aware material
ItemStack item = InventoryHelper.createItem("PLAYER_HEAD", 1);
```

### GUI Helpers

```java
// Fill inventory with item
ItemStack glass = new ItemStack(Material.GLASS_PANE);
InventoryHelper.fillInventory(inventory, glass);

// Fill border with item
InventoryHelper.fillBorder(inventory, glass);

// Set item at slot with click prevention
InventoryHelper.setGuiItem(inventory, 13, displayItem);
```

### Complete GUI Example

```java
public void openShop(Player player) {
    Inventory shop = InventoryHelper.createInventory(player, "Item Shop", 3);
    
    // Create shop items
    ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
    ItemMeta meta = diamond.getItemMeta();
    meta.setDisplayName(ChatColor.AQUA + "Diamond - $100");
    diamond.setItemMeta(meta);
    
    ItemStack gold = new ItemStack(Material.GOLD_INGOT, 1);
    ItemMeta goldMeta = gold.getItemMeta();
    goldMeta.setDisplayName(ChatColor.GOLD + "Gold Ingot - $50");
    gold.setItemMeta(goldMeta);
    
    // Add to inventory
    shop.setItem(11, diamond);
    shop.setItem(13, gold);
    
    // Fill borders
    ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    InventoryHelper.fillBorder(shop, border);
    
    // Open for player
    InventoryHelper.openInventory(player, shop);
}
```

---

## 17. ItemBuilder

**Purpose**: Fluent API for creating and customizing ItemStacks with names, lore, enchantments, and more.

### Basic Item Creation

```java
// Create simple item
ItemStack sword = ItemBuilder.of(Material.DIAMOND_SWORD)
    .name("§cFlaming Blade")
    .build();

// Create with amount
ItemStack apples = ItemBuilder.of(Material.APPLE)
    .amount(16)
    .build();
```

### Display Names and Lore

```java
ItemStack item = ItemBuilder.of(Material.STICK)
    .name("§6Magic Wand")
    .lore("§7A powerful magical item", "§7Right-click to cast spells")
    .build();

// Add lore lines dynamically
ItemBuilder builder = ItemBuilder.of(Material.BOOK)
    .name("§aInfo Book")
    .addLoreLine("§7Line 1")
    .addLoreLine("§7Line 2");

// Clear lore
builder.clearLore();
```

### Enchantments

```java
// Add enchantments
ItemStack enchantedSword = ItemBuilder.of(Material.DIAMOND_SWORD)
    .name("§6Legendary Sword")
    .enchant(Enchantment.DAMAGE_ALL, 5)
    .enchant(Enchantment.FIRE_ASPECT, 2)
    .enchant(Enchantment.KNOCKBACK, 2)
    .build();

// Add unsafe enchantments (beyond normal limits)
ItemStack godSword = ItemBuilder.of(Material.DIAMOND_SWORD)
    .addEnchant(Enchantment.DAMAGE_ALL, 10, true) // Force level 10
    .build();

// Remove enchantments
builder.removeEnchant(Enchantment.DAMAGE_ALL);

// Clear all enchantments
builder.clearEnchants();

// Make item glow (enchantment effect without enchants)
ItemStack glowingItem = ItemBuilder.of(Material.STICK)
    .name("§eGlowing Stick")
    .glow()
    .build();
```

### Item Flags

```java
// Hide enchantments
ItemStack cleanSword = ItemBuilder.of(Material.DIAMOND_SWORD)
    .enchant(Enchantment.DAMAGE_ALL, 5)
    .addFlags(ItemFlag.HIDE_ENCHANTS)
    .build();

// Hide all attributes
ItemStack item = ItemBuilder.of(Material.DIAMOND_CHESTPLATE)
    .hideAll()
    .build();

// Multiple flags
builder.addFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
```

### Properties

```java
// Make unbreakable
ItemStack tool = ItemBuilder.of(Material.DIAMOND_PICKAXE)
    .name("§bEternal Pickaxe")
    .unbreakable(true)
    .build();

// Set durability
ItemStack damaged = ItemBuilder.of(Material.DIAMOND_SWORD)
    .durability((short) 100)
    .build();

// Custom model data (1.14+)
ItemStack custom = ItemBuilder.of(Material.STICK)
    .customModelData(1001)
    .build();
```

### Special Items

```java
// Player skull
ItemStack skull = ItemBuilder.skull("Notch")
    .name("§6Notch's Head")
    .build();

// Skull with texture (Base64)
ItemStack customSkull = ItemBuilder.of(Material.PLAYER_HEAD)
    .name("§aCustom Head")
    .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy4...")
    .build();

// Potion
ItemStack potion = ItemBuilder.potion(PotionType.SPEED)
    .name("§bSpeed Potion")
    .build();

// Potion with custom effect
ItemStack customPotion = ItemBuilder.of(Material.POTION)
    .name("§dSuper Potion")
    .setPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2))
    .setColor(Color.PURPLE)
    .build();
```

### Build and Distribution

```java
// Build single item
ItemStack item = builder.build();

// Build list of items
List<ItemStack> items = builder.buildList(5); // 5 copies

// Give to player
builder.give(player);
builder.give(player, 3); // Give 3 copies

// Clone builder
ItemBuilder copy = builder.clone();
```

### Complete Examples

```java
// Custom weapon
ItemStack legendary = ItemBuilder.of(Material.DIAMOND_SWORD)
    .name("§6§lLegendary Blade")
    .lore(
        "§7A weapon of immense power",
        "§7forged in ancient times.",
        "",
        "§c+50 Attack Damage",
        "§e+10 Fire Damage"
    )
    .enchant(Enchantment.DAMAGE_ALL, 7)
    .enchant(Enchantment.FIRE_ASPECT, 3)
    .enchant(Enchantment.SWEEPING_EDGE, 4)
    .enchant(Enchantment.LOOTING, 5)
    .unbreakable(true)
    .addFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES)
    .glow()
    .build();

// Currency item
ItemStack coin = ItemBuilder.of(Material.GOLD_NUGGET)
    .name("§6Gold Coin")
    .lore("§7Server currency")
    .amount(64)
    .customModelData(100)
    .build();

// Quest item
ItemStack questScroll = ItemBuilder.of(Material.PAPER)
    .name("§e§lQuest: Dragon Slayer")
    .lore(
        "§7Defeat the Ender Dragon",
        "§7to complete this quest.",
        "",
        "§aReward: §f10000 coins"
    )
    .enchant(Enchantment.DURABILITY, 1)
    .addFlags(ItemFlag.HIDE_ENCHANTS)
    .build();

// Give items to player
ItemBuilder.of(legendary).give(player);
player.sendMessage("§aYou received the Legendary Blade!");
```

---

## 18. JavaUtilities

**Purpose**: System monitoring, memory management, and thread utilities.

### Memory Information

```java
// Get current memory usage
JavaUtilities.MemoryInfo info = JavaUtilities.getMemoryUsage();

ConsoleLog.info("Max Memory: " + info.getMaxMB() + "MB");
ConsoleLog.info("Used Memory: " + info.getUsedMB() + "MB");
ConsoleLog.info("Free Memory: " + info.getFreeMB() + "MB");
ConsoleLog.info("Usage: " + info.getUsedPercent() + "%");

// Get formatted memory string
String memStr = JavaUtilities.getMemoryString();
// Output: "Used: 512MB / 2048MB (25.0%)"
```

### Garbage Collection

```java
// Force garbage collection (use sparingly - can cause lag!)
boolean success = JavaUtilities.forceGC();
if (success) {
    ConsoleLog.info("Garbage collection triggered");
}
```

### Memory Warning System

```java
@Override
public void onEnable() {
    JavaUtilities.init(this);
    
    // Register memory warning at 80% usage
    JavaUtilities.registerMemoryWarning(80.0, (info) -> {
        ConsoleLog.warn("Memory usage is high: " + info.getUsedPercent() + "%");
        ConsoleLog.warn("Used: " + info.getUsedMB() + "MB / " + info.getMaxMB() + "MB");
        
        // Take action
        clearCaches();
        JavaUtilities.forceGC();
    });
    
    // Start monitoring (checks every 30 seconds)
    JavaUtilities.startMemoryMonitor();
}
```

### Thread Information

```java
// Get thread count
int threads = JavaUtilities.getThreadCount();
ConsoleLog.info("Active threads: " + threads);

// Get detailed thread information for all threads
List<JavaUtilities.ThreadDetail> threadList = JavaUtilities.getThreadDetails();
for (JavaUtilities.ThreadDetail thread : threadList) {
    ConsoleLog.info(thread.getName() + " - " + thread.getState());
}
```

### System Information

```java
// Get system info
JavaUtilities.SystemInfo sysInfo = JavaUtilities.getSystemInfo();

ConsoleLog.info("OS: " + sysInfo.getOsName());
ConsoleLog.info("OS Version: " + sysInfo.getOsVersion());
ConsoleLog.info("Java Version: " + sysInfo.getJavaVersion());
ConsoleLog.info("CPU Cores: " + sysInfo.getAvailableProcessors());
ConsoleLog.info("Max Memory: " + sysInfo.getMaxMemoryMB() + "MB");

// Get formatted system info
String systemString = JavaUtilities.getSystemInfoString();
ConsoleLog.info(systemString);
```

### Async Execution

```java
// Run task asynchronously
JavaUtilities.runAsync(() -> {
    // Heavy computation here
    processLargeDataset();
});

// Run with result
CompletableFuture<String> future = JavaUtilities.supplyAsync(() -> {
    return calculateExpensiveValue();
});

future.thenAccept(result -> {
    ConsoleLog.info("Result: " + result);
});
```

### Complete Monitoring Example

```java
public class MonitoringPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        JavaUtilities.init(this);
        
        // Setup memory warnings
        JavaUtilities.registerMemoryWarning(75.0, this::handleMemoryWarning);
        JavaUtilities.registerMemoryWarning(90.0, this::handleCriticalMemory);
        JavaUtilities.startMemoryMonitor();
        
        // Log system info
        logSystemInfo();
        
        // Schedule periodic status check
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, 
            this::logStatus, 0L, 20L * 60 * 5); // Every 5 minutes
    }
    
    private void handleMemoryWarning(JavaUtilities.MemoryInfo info) {
        ConsoleLog.warn("Memory warning: " + info.getUsedPercent() + "%");
        clearNonEssentialCaches();
    }
    
    private void handleCriticalMemory(JavaUtilities.MemoryInfo info) {
        ConsoleLog.error("CRITICAL: Memory at " + info.getUsedPercent() + "%");
        performEmergencyCleanup();
        JavaUtilities.forceGC();
    }
    
    private void logSystemInfo() {
        JavaUtilities.SystemInfo info = JavaUtilities.getSystemInfo();
        ConsoleLog.info("=== System Information ===");
        ConsoleLog.info("OS: " + info.getOsName() + " " + info.getOsVersion());
        ConsoleLog.info("Java: " + info.getJavaVersion());
        ConsoleLog.info("CPU Cores: " + info.getAvailableProcessors());
        ConsoleLog.info("Max Memory: " + info.getMaxMemoryMB() + "MB");
    }
    
    private void logStatus() {
        String mem = JavaUtilities.getMemoryString();
        int threads = JavaUtilities.getThreadCount();
        ConsoleLog.info("Status - " + mem + " | Threads: " + threads);
    }
}
```

---

## 19. Mathematics

**Purpose**: Common mathematical operations and utilities for game development.

### Basic Math

```java
// Clamp value between min and max
int health = Mathematics.clamp(playerHealth, 0, 20);
double damage = Mathematics.clamp(calculatedDamage, 0.0, 100.0);

// Linear interpolation
double progress = Mathematics.lerp(0.0, 100.0, 0.5); // Result: 50.0

// Map value from one range to another
double scaled = Mathematics.map(50, 0, 100, 0, 1); // Result: 0.5

// Approximate equality with epsilon
if (Mathematics.approxEquals(3.14159, Math.PI, 0.001)) {
    // Values are approximately equal
}
```

### Integer Operations

```java
// Greatest common divisor
long gcd = Mathematics.gcd(48, 18); // Result: 6

// Least common multiple
long lcm = Mathematics.lcm(12, 18); // Result: 36

// Prime check
if (Mathematics.isPrime(17)) {
    // 17 is prime
}
```

### Combinatorics

```java
// Factorial
BigInteger fact = Mathematics.factorial(10); // 10! = 3,628,800

// Permutation (nPr)
BigInteger perm = Mathematics.permutation(10, 3); // 10P3 = 720

// Combination (nCr)
BigInteger comb = Mathematics.combination(10, 3); // 10C3 = 120
```

### Random Utilities

```java
// Random integer in range [min, max]
int roll = Mathematics.randomInt(1, 6); // Dice roll

// Random double in range [min, max]
double chance = Mathematics.randomDouble(0.0, 1.0);

// Random from list
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
String random = Mathematics.randomFromList(names);

// Weighted random selection
Map<String, Integer> weights = new HashMap<>();
weights.put("common", 70);
weights.put("rare", 25);
weights.put("legendary", 5);
String rarity = Mathematics.weightedRandom(weights);
```

### Number Formatting

```java
// Format with commas
String formatted = Mathematics.formatNumber(1234567); // "1,234,567"

// Format as percentage
String percent = Mathematics.formatPercent(0.756); // "75.6%"

// Format with decimal places
String precise = Mathematics.formatDecimal(3.14159, 2); // "3.14"

// Abbreviate large numbers
String abbrev = Mathematics.abbreviateNumber(1500000); // "1.5M"
```

### Angle & Geometry

```java
// Convert degrees to radians
double rad = Mathematics.toRadians(180.0); // PI

// Convert radians to degrees
double deg = Mathematics.toDegrees(Math.PI); // 180.0

// Normalize angle to [0, 360)
double normalized = Mathematics.normalizeAngle(450.0); // 90.0

// Calculate distance between points (2D)
double dist = Mathematics.distance(x1, y1, x2, y2);

// Calculate distance between points (3D)
double dist3d = Mathematics.distance3D(x1, y1, z1, x2, y2, z2);
```

### Safe Operations

```java
// Safe division (returns 0 if divisor is 0)
double result = Mathematics.safeDivide(10.0, 0.0); // 0.0 instead of exception

// Safe parse integer
int value = Mathematics.parseIntSafe("123", 0); // 123
int invalid = Mathematics.parseIntSafe("abc", -1); // -1 (fallback)

// Safe parse double
double dValue = Mathematics.parseDoubleSafe("3.14", 0.0); // 3.14
```

### Game Development Examples

```java
// Damage calculation with random variance
public double calculateDamage(double base, double variance) {
    double min = base - variance;
    double max = base + variance;
    return Mathematics.randomDouble(min, max);
}

// Experience curve
public int getExpForLevel(int level) {
    return (int) Mathematics.map(level, 1, 100, 0, 1000000);
}

// Drop chance system
public boolean shouldDrop(double dropChance) {
    return Mathematics.randomDouble(0, 100) < dropChance;
}

// Health regeneration with interpolation
public void regenerateHealth(Player player, long ticks) {
    double progress = Mathematics.clamp(ticks / 200.0, 0.0, 1.0);
    double health = Mathematics.lerp(1.0, 20.0, progress);
    player.setHealth(health);
}

// Circle of players (spawn positions)
public List<Location> getCirclePositions(Location center, double radius, int players) {
    List<Location> positions = new ArrayList<>();
    for (int i = 0; i < players; i++) {
        double angle = 2 * Math.PI * i / players;
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);
        positions.add(new Location(center.getWorld(), x, center.getY(), z));
    }
    return positions;
}
```

---

## 20. PermissionHandler

**Purpose**: Runtime permission management without depending on permission plugins.

### Initialization

```java
@Override
public void onEnable() {
    // Initialize PermissionHandler
    PermissionHandler.init(this);
}
```

### Basic Permission Management

```java
// Give permission to player
PermissionHandler.givePermission(player, "myplugin.vip");

// Remove permission
PermissionHandler.removePermission(player, "myplugin.vip");

// Set permission (true = grant, false = revoke)
PermissionHandler.setPermission(player, "myplugin.admin", true);

// Check if player has permission
if (PermissionHandler.hasPermission(player, "myplugin.fly")) {
    // Player has fly permission
}
```

### Permission Checks with Op

```java
// Check permission (ops automatically pass if allowOp is true)
boolean canUse = PermissionHandler.hasPermission(player, "myplugin.command", true);

// Strict check (ops must have explicit permission)
boolean strictCheck = PermissionHandler.hasPermission(player, "myplugin.admin", false);
```

### Multiple Permissions

```java
// Give multiple permissions at once
PermissionHandler.givePermissions(player, 
    "myplugin.command1", 
    "myplugin.command2", 
    "myplugin.feature"
);

// Set multiple permissions with map
Map<String, Boolean> perms = new HashMap<>();
perms.put("myplugin.build", true);
perms.put("myplugin.break", true);
perms.put("myplugin.pvp", false);
PermissionHandler.setPermissions(player, perms);
```

### Group Permissions

```java
// Define permission groups
Map<String, Boolean> vipPerms = new HashMap<>();
vipPerms.put("myplugin.fly", true);
vipPerms.put("myplugin.kit.vip", true);
vipPerms.put("myplugin.homes.5", true);

// Apply group (clearExisting = false keeps other permissions)
PermissionHandler.setGroupPermissions(player, vipPerms, false);

// Replace all permissions with group (clearExisting = true)
PermissionHandler.setGroupPermissions(player, vipPerms, true);
```

### Permission Queries

```java
// List all permissions for player
List<String> perms = PermissionHandler.getPlayerPermissions(player);
for (String perm : perms) {
    ConsoleLog.info("Player has: " + perm);
}

// Check multiple permissions at once
List<String> required = Arrays.asList("perm1", "perm2", "perm3");
if (PermissionHandler.hasAllPermissions(player, required)) {
    // Player has all required permissions
}

// Check if player has any of the permissions
if (PermissionHandler.hasAnyPermission(player, required)) {
    // Player has at least one permission
}
```

### Clearing Permissions

```java
// Clear all custom permissions from player
PermissionHandler.clearPermissions(player);

// Clear specific permission
PermissionHandler.removePermission(player, "myplugin.temp");
```

### Complete Example - Rank System

```java
public class RankManager {
    
    private final Map<String, Map<String, Boolean>> ranks = new HashMap<>();
    
    public RankManager() {
        // Define ranks
        Map<String, Boolean> member = new HashMap<>();
        member.put("myplugin.chat", true);
        member.put("myplugin.home", true);
        ranks.put("member", member);
        
        Map<String, Boolean> vip = new HashMap<>();
        vip.put("myplugin.chat", true);
        vip.put("myplugin.home", true);
        vip.put("myplugin.fly", true);
        vip.put("myplugin.kit.vip", true);
        ranks.put("vip", vip);
        
        Map<String, Boolean> admin = new HashMap<>();
        admin.put("myplugin.*", true);
        ranks.put("admin", admin);
    }
    
    public void setRank(Player player, String rank) {
        Map<String, Boolean> perms = ranks.get(rank.toLowerCase());
        if (perms == null) {
            ConsoleLog.warn("Unknown rank: " + rank);
            return;
        }
        
        // Clear existing permissions and apply rank
        PermissionHandler.clearPermissions(player);
        PermissionHandler.setGroupPermissions(player, perms, true);
        
        player.sendMessage(ColorConverter.colorize(
            "&aYour rank has been set to &e" + rank
        ));
        ConsoleLog.info("Set rank " + rank + " for " + player.getName());
    }
    
    public void grantTemporaryPermission(Player player, String permission, long seconds) {
        PermissionHandler.givePermission(player, permission);
        player.sendMessage("Granted temporary permission: " + permission);
        
        // Remove after delay
        SchedulerHelper.runLaterSeconds(plugin, () -> {
            PermissionHandler.removePermission(player, permission);
            player.sendMessage("Temporary permission expired: " + permission);
        }, seconds);
    }
}
```

### Permission Registration

```java
// Register permissions with Bukkit (optional, for /permissions command)
PermissionHandler.registerPermission("myplugin.admin", 
    "Admin permission", 
    PermissionDefault.OP
);

PermissionHandler.registerPermission("myplugin.vip", 
    "VIP features", 
    PermissionDefault.FALSE
);
```

---

## 21. PlayerGather

**Purpose**: Simplified player lookup, teleportation, and inventory management.

### Getting Players

```java
// Get all online players
List<Player> online = PlayerGather.getOnlinePlayers();

// Get online player by name
Player player = PlayerGather.getOnlinePlayer("Notch");

// Get online player by UUID
UUID uuid = UUID.fromString("...");
Player player = PlayerGather.getOnlinePlayer(uuid);

// Get online player count
int count = PlayerGather.getOnlinePlayerCount();

// Get list of online player names
List<String> names = PlayerGather.getOnlinePlayerNames();
```

### Offline Players

```java
// Get offline player by name
OfflinePlayer offline = PlayerGather.getOfflinePlayer("Notch");

// Get offline player by UUID
OfflinePlayer offline = PlayerGather.getOfflinePlayer(uuid);

// Get all offline players
OfflinePlayer[] allOffline = PlayerGather.getAllOfflinePlayers();

// Get as list
List<OfflinePlayer> offlineList = PlayerGather.getAllOfflinePlayersList();

// Check if player has played before
if (PlayerGather.hasPlayedBefore("Notch")) {
    // Player has joined the server before
}
```

### Player Search

```java
// Find players with names starting with prefix
List<Player> matches = PlayerGather.findOnlinePlayersStartingWith("Not");
// Returns players like "Notch", "NotchDev", etc.

// Find players with names containing substring
List<Player> contains = PlayerGather.findOnlinePlayersContaining("otch");
// Returns players like "Notch", "Scotch", etc.
```

### Inventory Management

```java
// Clear player inventory
PlayerGather.clearInventory(player);

// Clear inventory and armor
PlayerGather.clearInventoryAndArmor(player);

// Give items
ItemStack diamond = new ItemStack(Material.DIAMOND, 64);
ItemStack gold = new ItemStack(Material.GOLD_INGOT, 32);
PlayerGather.giveItems(player, diamond, gold);

// Give items from list
List<ItemStack> rewards = getRewards();
PlayerGather.giveItems(player, rewards);
```

### Teleportation

```java
// Teleport to location
Location spawn = new Location(world, 0, 100, 0);
PlayerGather.teleport(player, spawn);

// Teleport to coordinates
PlayerGather.teleport(player, world, 100, 64, 200);

// Teleport to another player
Player target = PlayerGather.getOnlinePlayer("Target");
PlayerGather.teleportToPlayer(player, target);

// Teleport with safety check (finds safe location)
boolean safe = PlayerGather.teleportSafely(player, location);
if (!safe) {
    player.sendMessage("No safe location found!");
}
```

### Player State Checks

```java
// Check if player is flying
if (PlayerGather.isFlying(player)) {
    // Player is flying
}

// Check if player is sneaking
if (PlayerGather.isSneaking(player)) {
    // Player is sneaking
}

// Check if player is sprinting
if (PlayerGather.isSprinting(player)) {
    // Player is sprinting
}

// Check if player is in water
if (PlayerGather.isInWater(player)) {
    // Player is in water
}
```

### Player Properties

```java
// Get player health
double health = PlayerGather.getHealth(player);

// Set player health
PlayerGather.setHealth(player, 20.0);

// Get player food level
int food = PlayerGather.getFoodLevel(player);

// Set player food level
PlayerGather.setFoodLevel(player, 20);

// Get player experience
int exp = PlayerGather.getExperience(player);

// Get player level
int level = PlayerGather.getLevel(player);

// Set player level
PlayerGather.setLevel(player, 50);
```

### Inventory Utilities

```java
// Get player inventory
Inventory inv = PlayerGather.getInventory(player);

// Check if player has specific item
ItemStack diamond = new ItemStack(Material.DIAMOND, 5);
if (PlayerGather.hasItem(player, diamond)) {
    ConsoleLog.info("Player has diamonds!");
}

// Count specific items in inventory
int count = PlayerGather.countItem(player, new ItemStack(Material.EMERALD));
ConsoleLog.info("Player has " + count + " emeralds");

// Remove specific item from inventory
PlayerGather.removeItem(player, new ItemStack(Material.DIRT, 64));
```

### Advanced Teleportation

```java
// Teleport to world spawn
PlayerGather.teleportToSpawn(player, world);

// Teleport to specific world's spawn by name
PlayerGather.teleportToWorldSpawn(player, "world_nether");

// Teleport multiple players to location
List<Player> team = Arrays.asList(player1, player2, player3);
int teleported = PlayerGather.teleportAll(team, destination);
ConsoleLog.info("Teleported " + teleported + " players");

// Teleport all online players to location
int count = PlayerGather.teleportAllOnline(eventLocation);
ConsoleLog.info("Teleported all " + count + " online players");
```

### Player Permissions & Status

```java
// Check if player has permission
if (PlayerGather.hasPermission(player, "myplugin.admin")) {
    // Grant admin access
}

// Check if player is online (by name)
if (PlayerGather.isOnline("Notch")) {
    ConsoleLog.info("Notch is online!");
}

// Check if player is online (by UUID)
if (PlayerGather.isOnline(uuid)) {
    ConsoleLog.info("Player is online!");
}

// Check if player is OP
if (PlayerGather.isOp(player)) {
    player.sendMessage("You have operator status");
}
```

### Player Healing & Effects

```java
// Set player health to specific value
PlayerGather.setHealth(player, 10.0); // Half health

// Fully heal player (health + food + saturation)
PlayerGather.heal(player);

// Set player food level
PlayerGather.setFoodLevel(player, 10); // Half food bar

// Set game mode
PlayerGather.setGameMode(player, org.bukkit.GameMode.CREATIVE);

// Control flight
PlayerGather.setFlyMode(player, true, true); // Allow flight + set flying
PlayerGather.setFlyMode(player, false, false); // Disable flight
```

### Complete Example - Admin Tools

```java
public class AdminTools {
    
    public void healAll() {
        List<Player> players = PlayerGather.getOnlinePlayers();
        for (Player player : players) {
            PlayerGather.setHealth(player, 20.0);
            PlayerGather.setFoodLevel(player, 20);
            player.sendMessage(ColorConverter.colorize("&aYou have been healed!"));
        }
        ConsoleLog.info("Healed " + players.size() + " players");
    }
    
    public void teleportAll(Location destination) {
        List<Player> players = PlayerGather.getOnlinePlayers();
        int count = 0;
        for (Player player : players) {
            if (PlayerGather.teleport(player, destination)) {
                count++;
            }
        }
        ConsoleLog.info("Teleported " + count + " players");
    }
    
    public void clearInventories() {
        for (Player player : PlayerGather.getOnlinePlayers()) {
            if (player.hasPermission("admin.bypass")) continue;
            PlayerGather.clearInventoryAndArmor(player);
            player.sendMessage("Your inventory has been cleared!");
        }
    }
    
    public void giveStarterKit(Player player) {
        List<ItemStack> kit = Arrays.asList(
            new ItemStack(Material.STONE_SWORD),
            new ItemStack(Material.STONE_PICKAXE),
            new ItemStack(Material.COOKED_BEEF, 32),
            new ItemStack(Material.TORCH, 64)
        );
        PlayerGather.giveItems(player, kit);
        player.sendMessage("Starter kit given!");
    }
    
    public void findPlayer(CommandSender sender, String search) {
        List<Player> matches = PlayerGather.findOnlinePlayersContaining(search);
        if (matches.isEmpty()) {
            sender.sendMessage("No players found matching: " + search);
        } else {
            sender.sendMessage("Found " + matches.size() + " player(s):");
            for (Player p : matches) {
                sender.sendMessage("  - " + p.getName());
            }
        }
    }
}
```

---

## 22. ProxyListener

**Purpose**: BungeeCord/Velocity proxy integration for multi-server setups.

### Initialization

```java
@Override
public void onEnable() {
    // Initialize proxy listener
    ProxyListener.init(this);
}

@Override
public void onDisable() {
    // Cleanup
    ProxyListener.cleanup();
}
```

### Proxy Detection

```java
// Check if server is behind a proxy
if (ProxyListener.isBehindProxy()) {
    ConsoleLog.info("Server is behind BungeeCord/Velocity");
} else {
    ConsoleLog.info("Server is standalone");
}

// Get current server name
String serverName = ProxyListener.getCurrentServer();
ConsoleLog.info("Current server: " + serverName);
```

### Server Communication

```java
// Send player to another server
boolean sent = ProxyListener.connectPlayerToServer(player, "lobby");
if (sent) {
    player.sendMessage("Connecting to lobby...");
} else {
    player.sendMessage("Failed to connect to lobby");
}

// Request list of servers from proxy
ProxyListener.requestServerList();

// Get cached server list (after request)
List<String> servers = ProxyListener.getServerList();
for (String server : servers) {
    ConsoleLog.info("Available server: " + server);
}
```

### Player Information

```java
// Get player's server
String playerServer = ProxyListener.getPlayerServer(player);

// Get player count on server
int count = ProxyListener.getPlayerCount("lobby");
ConsoleLog.info("Players on lobby: " + count);

// Get player count on current server
int localCount = ProxyListener.getLocalPlayerCount();

// Get total network player count
int totalPlayers = ProxyListener.getTotalPlayerCount();
```

### Plugin Messages

```java
// Send custom plugin message
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeUTF("CustomChannel");
out.writeUTF("Hello from server!");
ProxyListener.sendPluginMessage(player, "MySubChannel", out.toByteArray());

// Forward message to another server
ProxyListener.forwardToServer(player, "lobby", "CustomData", data);

// Forward message to all servers
ProxyListener.forwardToAllServers(player, "Broadcast", message);

// Send subchannel message (simplified)
ProxyListener.sendSubchannel(player, "GetServers");
```

### Advanced Player Operations

```java
// Request player's IP address from proxy
ProxyListener.requestPlayerIP(player);

// Get cached player info
ProxyListener.ProxyPlayerInfo info = ProxyListener.getPlayerInfo(player);
if (info != null) {
    ConsoleLog.info("Player server: " + info.getServer());
    ConsoleLog.info("Player IP: " + info.getIpAddress());
}

// Remove cached player info
ProxyListener.removePlayerInfo(player);
```

### Request Operations

```java
// Request server name
ProxyListener.requestServerName();

// Request player count for specific server
ProxyListener.requestPlayerCount("lobby");
```

### Complete Example - Multi-Server Hub

```java
public class NetworkManager {
    
    private Map<String, Integer> serverPlayerCounts = new HashMap<>();
    
    public void initialize() {
        ProxyListener.init(plugin);
        
        if (!ProxyListener.isBehindProxy()) {
            ConsoleLog.warn("Not behind proxy - network features disabled");
            return;
        }
        
        // Update server list every 30 seconds
        SchedulerHelper.runTimerSeconds(plugin, () -> {
            ProxyListener.requestServerList();
            updatePlayerCounts();
        }, 0, 30);
    }
    
    public void openServerSelector(Player player) {
        List<String> servers = ProxyListener.getServerList();
        if (servers.isEmpty()) {
            player.sendMessage("No servers available");
            return;
        }
        
        Inventory gui = InventoryHelper.createInventory(player, "Select Server", 3);
        
        int slot = 0;
        for (String server : servers) {
            ItemStack icon = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = icon.getItemMeta();
            
            meta.setDisplayName(ColorConverter.colorize("&a" + server));
            int playerCount = serverPlayerCounts.getOrDefault(server, 0);
            meta.setLore(Arrays.asList(
                ColorConverter.colorize("&7Players: &e" + playerCount),
                ColorConverter.colorize("&7Click to connect")
            ));
            
            icon.setItemMeta(meta);
            gui.setItem(slot++, icon);
        }
        
        player.openInventory(gui);
    }
    
    public void connectToLobby(Player player) {
        if (ProxyListener.connectPlayerToServer(player, "lobby")) {
            player.sendMessage(ColorConverter.colorize("&aConnecting to lobby..."));
        } else {
            player.sendMessage(ColorConverter.colorize("&cFailed to connect to lobby"));
        }
    }
    
    public void broadcastNetworkMessage(String message) {
        // Send to all servers via proxy
        Player anyPlayer = Bukkit.getOnlinePlayers().iterator().next();
        if (anyPlayer != null) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(message);
            ProxyListener.forwardToAllServers(anyPlayer, "NetworkBroadcast", out.toByteArray());
        }
    }
    
    private void updatePlayerCounts() {
        List<String> servers = ProxyListener.getServerList();
        for (String server : servers) {
            int count = ProxyListener.getPlayerCount(server);
            serverPlayerCounts.put(server, count);
        }
    }
}
```

### Event Handling

```java
// Listen for player server switch
@EventHandler
public void onPluginMessage(PluginMessageEvent event) {
    if (event.getTag().equals("BungeeCord")) {
        // Handle incoming proxy message
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        
        if (subchannel.equals("PlayerCount")) {
            String server = in.readUTF();
            int count = in.readInt();
            ConsoleLog.info(server + " has " + count + " players");
        }
    }
}
```

---

## 23. SchedulerHelper

**Purpose**: Simplified task scheduling without BukkitRunnable boilerplate.

### Initialization

```java
// Set plugin instance (optional, for some methods)
SchedulerHelper.setPlugin(this);
```

### Time Conversion

```java
// Convert time units to ticks
long ticks = SchedulerHelper.secondsToTicks(5.0); // 100 ticks
long ticks = SchedulerHelper.minutesToTicks(2.0); // 2400 ticks
long ticks = SchedulerHelper.hoursToTicks(1.0);   // 72000 ticks

// Convert ticks to time units
double seconds = SchedulerHelper.ticksToSeconds(100); // 5.0
double minutes = SchedulerHelper.ticksToMinutes(2400); // 2.0
```

### Synchronous Tasks

```java
// Run immediately (next tick)
SchedulerHelper.run(plugin, () -> {
    // Code runs on main thread
    player.sendMessage("Hello!");
});

// Run with delay
SchedulerHelper.runLater(plugin, () -> {
    player.sendMessage("Delayed message!");
}, 100L); // 100 ticks = 5 seconds

// Run with delay in seconds
SchedulerHelper.runLaterSeconds(plugin, () -> {
    player.sendMessage("5 seconds later...");
}, 5.0);

// Run with delay in minutes
SchedulerHelper.runLaterMinutes(plugin, () -> {
    ConsoleLog.info("10 minutes have passed");
}, 10.0);
```

### Repeating Tasks

```java
// Run repeatedly
BukkitTask task = SchedulerHelper.runTimer(plugin, () -> {
    // Runs every second
    broadcastMessage("Tick!");
}, 0L, 20L); // delay=0, period=20 ticks

// Run repeatedly in seconds
SchedulerHelper.runTimerSeconds(plugin, () -> {
    updateScoreboard();
}, 0.0, 1.0); // Runs every second

// Run repeatedly in minutes
SchedulerHelper.runTimerMinutes(plugin, () -> {
    saveData();
}, 5.0, 5.0); // Runs every 5 minutes
```

### Asynchronous Tasks

```java
// Run async immediately
SchedulerHelper.runAsync(plugin, () -> {
    // Heavy computation off main thread
    processLargeDataset();
});

// Run async with delay
SchedulerHelper.runAsyncLater(plugin, () -> {
    fetchFromDatabase();
}, 100L);

// Run async with delay in seconds
SchedulerHelper.runAsyncLaterSeconds(plugin, () -> {
    downloadFile();
}, 5.0);

// Run async repeatedly
SchedulerHelper.runAsyncTimer(plugin, () -> {
    checkExternalAPI();
}, 0L, 1200L); // Every 60 seconds

// Run async repeatedly in seconds
SchedulerHelper.runAsyncTimerSeconds(plugin, () -> {
    updateWebStats();
}, 0.0, 30.0); // Every 30 seconds
```

### Task Tracking

```java
// Run with automatic tracking
BukkitTask task = SchedulerHelper.runTracked(plugin, () -> {
    doSomething();
});

// Run repeating task with tracking
BukkitTask timer = SchedulerHelper.runTimerTracked(plugin, () -> {
    updateData();
}, 0L, 20L);

// Cancel specific tracked task
SchedulerHelper.cancelTask(task);

// Cancel all tracked tasks for plugin
SchedulerHelper.cancelAllTrackedTasks(plugin);

// Get all tracked tasks
List<BukkitTask> tasks = SchedulerHelper.getTrackedTasks();
```

### Task Cancellation

```java
// Cancel specific task
BukkitTask task = SchedulerHelper.runTimer(plugin, () -> {
    // Repeating task
}, 0L, 20L);

// Cancel later
task.cancel();

// Or use helper
SchedulerHelper.cancelTask(task);

// Cancel all tasks for plugin
SchedulerHelper.cancelAllTasks(plugin);

// Check if task is running
if (SchedulerHelper.isTaskRunning(task)) {
    // Task is still active
}
```

### Countdown Helper

```java
// Countdown timer
SchedulerHelper.countdown(plugin, 10, (remaining) -> {
    // Called each second
    Bukkit.broadcastMessage("Starting in " + remaining + "...");
}, () -> {
    // Called when countdown finishes
    Bukkit.broadcastMessage("Go!");
    startGame();
});

// Countdown with custom interval
SchedulerHelper.countdownSeconds(plugin, 60.0, 5.0, (remaining) -> {
    // Called every 5 seconds
    ConsoleLog.info(remaining + " seconds remaining");
}, () -> {
    ConsoleLog.info("Time's up!");
});
```

### Complete Example - Game Manager

```java
public class GameManager {
    
    private BukkitTask gameTask;
    private BukkitTask countdownTask;
    private int gameTime = 0;
    
    public void startGameCountdown() {
        SchedulerHelper.countdown(plugin, 10, (remaining) -> {
            Bukkit.broadcastMessage(ColorConverter.colorize(
                "&e&lGame starting in &c" + remaining + " &e&lseconds!"
            ));
            
            // Play sound
            for (Player p : Bukkit.getOnlinePlayers()) {
                SoundPlayer.play(p, "BLOCK_NOTE_BLOCK_PLING", 1f, 1f);
            }
        }, this::startGame);
    }
    
    private void startGame() {
        Bukkit.broadcastMessage(ColorConverter.colorize("&a&lGAME STARTED!"));
        gameTime = 0;
        
        // Game timer (updates every second)
        gameTask = SchedulerHelper.runTimerSeconds(plugin, () -> {
            gameTime++;
            updateGameScoreboard();
            
            // End game after 5 minutes
            if (gameTime >= 300) {
                endGame();
            }
        }, 0.0, 1.0);
        
        // Async stat updates (every 10 seconds)
        SchedulerHelper.runAsyncTimerSeconds(plugin, () -> {
            updateDatabaseStats();
        }, 10.0, 10.0);
    }
    
    private void endGame() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        Bukkit.broadcastMessage(ColorConverter.colorize("&c&lGAME ENDED!"));
        
        // Award prizes after delay
        SchedulerHelper.runLaterSeconds(plugin, () -> {
            awardPrizes();
        }, 3.0);
    }
    
    public void scheduleSave() {
        // Auto-save every 5 minutes
        SchedulerHelper.runTimerMinutes(plugin, () -> {
            SchedulerHelper.runAsync(plugin, () -> {
                saveAllData();
                ConsoleLog.info("Auto-save completed");
            });
        }, 5.0, 5.0);
    }
}
```

---

## 24. ScoreBoards

**Purpose**: Easy scoreboard creation and management for per-player scoreboards.

### Creating Scoreboards

```java
// Create basic scoreboard
Objective objective = ScoreBoards.createScoreboard(player, "&6&lMy Server");

// Create with initial lines
ScoreBoards.createScoreboard(player, "&6&lMy Server",
    "&7",
    "&eKills: &f0",
    "&eDeaths: &f0",
    "&7",
    "&bplay.server.com"
);
```

### Managing Lines

```java
// Set a line (line 1 = bottom, higher = up)
ScoreBoards.setLine(player, 5, "&ePlayer: &f" + player.getName());
ScoreBoards.setLine(player, 4, "&7");
ScoreBoards.setLine(player, 3, "&aCoins: &f" + coins);
ScoreBoards.setLine(player, 2, "&cKills: &f" + kills);
ScoreBoards.setLine(player, 1, "&7play.server.com");

// Update existing line
ScoreBoards.updateLine(player, 3, "&aCoins: &f" + newCoins);

// Remove a line
ScoreBoards.removeLine(player, 4);
```

### Scoreboard Title

```java
// Change title
ScoreBoards.setTitle(player, "&c&lNew Title");

// Animated title example
SchedulerHelper.runTimerSeconds(plugin, new Runnable() {
    private int state = 0;
    private final String[] titles = {
        "&6&lMy Server",
        "&e&lMy Server",
        "&6&lMy Server"
    };
    
    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ScoreBoards.setTitle(p, titles[state]);
        }
        state = (state + 1) % titles.length;
    }
}, 0.0, 0.5);
```

### Visibility Management

```java
// Show scoreboard to player
ScoreBoards.showToPlayer(player);

// Hide scoreboard from player
ScoreBoards.hideFromPlayer(player);

// Toggle visibility
boolean visible = ScoreBoards.isVisible(player);
if (visible) {
    ScoreBoards.hideFromPlayer(player);
} else {
    ScoreBoards.showToPlayer(player);
}
```

### Clearing & Removal

```java
// Clear all lines (keeps scoreboard)
ScoreBoards.clearLines(player);

// Remove scoreboard completely
ScoreBoards.removeScoreboard(player);

// Clear scoreboard (alias for clearLines)
ScoreBoards.clearScoreboard(player);
```

### Queries

```java
// Check if player has scoreboard
if (ScoreBoards.hasScoreboard(player)) {
    // Player has active scoreboard
}

// Get line content
String lineText = ScoreBoards.getLine(player, 3);

// Get all lines
Map<Integer, String> lines = ScoreBoards.getAllLines(player);
```

### Batch Operations

```java
// Set multiple lines at once
Map<Integer, String> lines = new HashMap<>();
lines.put(5, "&ePlayer: &f" + player.getName());
lines.put(4, "&7");
lines.put(3, "&aCoins: &f" + coins);
lines.put(2, "&cKills: &f" + kills);
lines.put(1, "&7play.server.com");
ScoreBoards.setLines(player, lines);

// Toggle visibility (show if hidden, hide if shown)
ScoreBoards.toggleVisibility(player);

// Clear scoreboard (same as clearLines)
ScoreBoards.clearScoreboard(player);

// Clear all scoreboards for all players
ScoreBoards.clearAllScoreboards();
```

### Scoreboard Information

```java
// Get scoreboard title
String title = ScoreBoards.getTitle(player);
ConsoleLog.info("Player's scoreboard title: " + title);

// Get line count
int lineCount = ScoreBoards.getLineCount(player);
ConsoleLog.info("Scoreboard has " + lineCount + " lines");

// Get all lines as map
Map<Integer, String> allLines = ScoreBoards.getLines(player);
for (Map.Entry<Integer, String> entry : allLines.entrySet()) {
    ConsoleLog.info("Line " + entry.getKey() + ": " + entry.getValue());
}

// Get scoreboard object
Scoreboard scoreboard = ScoreBoards.getScoreboard(player);

// Get objective
Objective objective = ScoreBoards.getObjective(player);
```

### Advanced Animations

```java
// Animate single line
List<String> texts = Arrays.asList(
    "&aLoading.",
    "&aLoading..",
    "&aLoading..."
);
ScoreBoards.animateLine(player, 5, texts, 10L); // 10 ticks between frames

// Animate title
SchedulerHelper.runTimerSeconds(plugin, new Runnable() {
    private int frame = 0;
    private final List<String> titles = Arrays.asList(
        "&6&lMY SERVER",
        "&e&lMY SERVER",
        "&6&lMY SERVER"
    );
    
    @Override
    public void run() {
        ScoreBoards.animateTitle(player, titles, 10L);
        frame++;
    }
}, 0.0, 0.5);
```

### Animated Scoreboards

```java
// Create animated scoreboard system
public class AnimatedScoreboard {
    
    private int updateTicks = 0;
    
    public void start() {
        SchedulerHelper.runTimerSeconds(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
            updateTicks++;
        }, 0.0, 0.5); // Update every 0.5 seconds
    }
    
    private void updateScoreboard(Player player) {
        if (!ScoreBoards.hasScoreboard(player)) {
            ScoreBoards.createScoreboard(player, getAnimatedTitle());
        }
        
        // Animated title
        ScoreBoards.setTitle(player, getAnimatedTitle());
        
        // Dynamic lines
        ScoreBoards.setLine(player, 10, "&7" + getDate());
        ScoreBoards.setLine(player, 9, "&7");
        ScoreBoards.setLine(player, 8, "&ePlayer: &f" + player.getName());
        ScoreBoards.setLine(player, 7, "&7");
        ScoreBoards.setLine(player, 6, "&aCoins: &f" + getCoins(player));
        ScoreBoards.setLine(player, 5, "&cKills: &f" + getKills(player));
        ScoreBoards.setLine(player, 4, "&9Deaths: &f" + getDeaths(player));
        ScoreBoards.setLine(player, 3, "&7");
        ScoreBoards.setLine(player, 2, "&6Rank: " + getRank(player));
        ScoreBoards.setLine(player, 1, "&7");
        ScoreBoards.setLine(player, 0, getAnimatedIP());
    }
    
    private String getAnimatedTitle() {
        String[] colors = {"&6", "&e", "&6", "&e"};
        int index = (updateTicks / 2) % colors.length;
        return colors[index] + "&lMY SERVER";
    }
    
    private String getAnimatedIP() {
        String[] frames = {
            "&bplay.server.com",
            "&3play.server.com",
            "&bplay.server.com"
        };
        int index = (updateTicks / 4) % frames.length;
        return frames[index];
    }
}
```

### Complete Example - Game Stats

```java
public class GameStatsBoard {
    
    public void createStatsBoard(Player player) {
        ScoreBoards.createScoreboard(player, "&6&lBED WARS");
        updateStats(player);
    }
    
    public void updateStats(Player player) {
        GameStats stats = getPlayerStats(player);
        
        ScoreBoards.setLine(player, 10, "&7" + getCurrentDate());
        ScoreBoards.setLine(player, 9, "&r");
        ScoreBoards.setLine(player, 8, "&fTeam: " + getTeamColor(player) + getTeamName(player));
        ScoreBoards.setLine(player, 7, "&r");
        ScoreBoards.setLine(player, 6, "&fKills: &a" + stats.kills);
        ScoreBoards.setLine(player, 5, "&fFinal Kills: &c" + stats.finalKills);
        ScoreBoards.setLine(player, 4, "&fBeds Broken: &e" + stats.bedsBroken);
        ScoreBoards.setLine(player, 3, "&r");
        
        // Show alive teams
        int line = 2;
        for (Team team : getAliveTeams()) {
            String status = team.hasBed() ? "&a✓" : "&c✗";
            ScoreBoards.setLine(player, line--, 
                team.getColor() + team.getName() + " " + status);
        }
        
        ScoreBoards.setLine(player, 1, "&r");
        ScoreBoards.setLine(player, 0, "&eplay.server.com");
    }
    
    public void removeStatsBoard(Player player) {
        ScoreBoards.removeScoreboard(player);
    }
}
```

---

## 25. SoundPlayer

**Purpose**: Version-aware sound playing with automatic fallbacks for older Minecraft versions.

### Basic Sound Playing

```java
// Play sound to player
SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP");

// Play with volume and pitch
SoundPlayer.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.5f);
// volume: 1.0 = normal, pitch: 1.0 = normal (higher = faster/higher pitch)

// Play sound from Sound enum
Sound sound = Sound.ENTITY_PLAYER_LEVELUP;
SoundPlayer.play(player, sound);
SoundPlayer.play(player, sound, 0.5f, 1.0f); // Quieter
```

### Playing at Locations

```java
// Play sound at location (all nearby players hear it)
Location loc = player.getLocation();
SoundPlayer.playAt(loc, "BLOCK_ANVIL_USE", 1.0f, 1.0f);

// Play sound at coordinates
SoundPlayer.playAt(player.getWorld(), 100, 64, 200, "ENTITY_LIGHTNING_BOLT_THUNDER");

// Play for all players in radius
SoundPlayer.playInRadius(loc, "ENTITY_FIREWORK_ROCKET_BLAST", 50.0, 1.0f, 1.0f);
```

### Sound Categories

```java
// Play with specific category (1.11+)
SoundPlayer.play(player, "ENTITY_VILLAGER_YES", SoundCategory.VOICE, 1.0f, 1.0f);
SoundPlayer.play(player, "MUSIC_DISC_CAT", SoundCategory.MUSIC, 0.5f, 1.0f);
SoundPlayer.play(player, "ENTITY_ZOMBIE_AMBIENT", SoundCategory.HOSTILE, 1.0f, 0.8f);
```

### Sound from Config

```java
// Play sound configured in config.yml
// Config: sounds.join: "ENTITY_PLAYER_LEVELUP"
SoundPlayer.playFromConfig(plugin, player, "sounds.join");

// With volume and pitch from config
// Config: sounds.error: {sound: "BLOCK_ANVIL_LAND", volume: 1.0, pitch: 0.5}
SoundPlayer.playFromConfig(plugin, player, "sounds.error");
```

### Stop Sounds

```java
// Stop specific sound for player
SoundPlayer.stopSound(player, "MUSIC_DISC_CAT");

// Stop all sounds for player
SoundPlayer.stopAllSounds(player);

// Stop sounds in category
SoundPlayer.stopSounds(player, SoundCategory.MUSIC);
```

### Sound Utilities

```java
// Get Sound enum from string (with fallbacks for old versions)
Sound sound = SoundPlayer.getSound("ENTITY_PLAYER_LEVELUP");
if (sound != null) {
    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
}

// List all available sounds
List<String> sounds = SoundPlayer.getAllSoundNames();
for (String soundName : sounds) {
    ConsoleLog.info("Available: " + soundName);
}

// Check if sound exists
if (SoundPlayer.soundExists("ENTITY_PLAYER_LEVELUP")) {
    // Sound is available in this version
}
```

### Complete Example - Sound Manager

```java
public class SoundManager {
    
    private final Map<String, SoundConfig> configuredSounds = new HashMap<>();
    
    public void loadSounds() {
        // Load from config
        configuredSounds.put("success", new SoundConfig(
            "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f
        ));
        configuredSounds.put("error", new SoundConfig(
            "BLOCK_ANVIL_LAND", 1.0f, 0.5f
        ));
        configuredSounds.put("click", new SoundConfig(
            "UI_BUTTON_CLICK", 0.5f, 1.0f
        ));
        configuredSounds.put("purchase", new SoundConfig(
            "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.2f
        ));
    }
    
    public void playSuccess(Player player) {
        playConfiguredSound(player, "success");
    }
    
    public void playError(Player player) {
        playConfiguredSound(player, "error");
    }
    
    public void playClick(Player player) {
        playConfiguredSound(player, "click");
    }
    
    private void playConfiguredSound(Player player, String key) {
        SoundConfig config = configuredSounds.get(key);
        if (config != null) {
            SoundPlayer.play(player, config.sound, config.volume, config.pitch);
        }
    }
    
    public void playCountdown(Player player, int seconds) {
        for (int i = seconds; i > 0; i--) {
            final int count = i;
            SchedulerHelper.runLaterSeconds(plugin, () -> {
                float pitch = 1.0f + (0.1f * (seconds - count));
                SoundPlayer.play(player, "BLOCK_NOTE_BLOCK_PLING", 1.0f, pitch);
            }, seconds - i);
        }
        
        // Final sound
        SchedulerHelper.runLaterSeconds(plugin, () -> {
            SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.5f);
        }, seconds);
    }
    
    public void playLevelUpSequence(Player player) {
        SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 0.8f);
        SchedulerHelper.runLater(plugin, () -> {
            SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
        }, 5L);
        SchedulerHelper.runLater(plugin, () -> {
            SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
        }, 10L);
    }
    
    private static class SoundConfig {
        String sound;
        float volume;
        float pitch;
        
        SoundConfig(String sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
```

### Common Sound Examples

```java
// UI Sounds
SoundPlayer.play(player, "UI_BUTTON_CLICK"); // Button click
SoundPlayer.play(player, "BLOCK_NOTE_BLOCK_PLING"); // Success ding
SoundPlayer.play(player, "BLOCK_ANVIL_LAND", 1.0f, 0.5f); // Error thud

// Player Events
SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP"); // Level up
SoundPlayer.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP"); // Collect item
SoundPlayer.play(player, "ENTITY_PLAYER_HURT"); // Damage taken
SoundPlayer.play(player, "ENTITY_PLAYER_DEATH"); // Death

// Combat
SoundPlayer.play(player, "ENTITY_ARROW_SHOOT"); // Arrow shot
SoundPlayer.play(player, "ENTITY_ARROW_HIT"); // Arrow hit
SoundPlayer.play(player, "ENTITY_PLAYER_ATTACK_STRONG"); // Critical hit
SoundPlayer.play(player, "ITEM_SHIELD_BLOCK"); // Block attack

// World
SoundPlayer.play(player, "ENTITY_LIGHTNING_BOLT_THUNDER"); // Thunder
SoundPlayer.play(player, "ENTITY_GENERIC_EXPLODE"); // Explosion
SoundPlayer.play(player, "BLOCK_PORTAL_TRAVEL"); // Portal
SoundPlayer.play(player, "BLOCK_CHEST_OPEN"); // Open chest

// Ambient
SoundPlayer.play(player, "ENTITY_VILLAGER_YES", 1.0f, 1.2f); // Success
SoundPlayer.play(player, "ENTITY_VILLAGER_NO", 1.0f, 0.8f); // Failure
SoundPlayer.play(player, "ENTITY_ENDERMAN_TELEPORT"); // Teleport
```

---

## 26. TabCompleter

**Purpose**: Easy tab completion registration for commands.

### Basic Tab Completion

```java
// Simple static completions
TabCompleter.register(plugin, "mycmd", "start", "stop", "reload", "help");

// Player types "/mycmd st" → suggests "start", "stop"
```

### Dynamic Completions

```java
// Completion based on sender
TabCompleter.register(plugin, "mycmd", sender -> {
    if (sender.hasPermission("admin")) {
        return Arrays.asList("start", "stop", "reload", "admin");
    } else {
        return Arrays.asList("start", "help");
    }
});
```

### Argument-Based Completions

```java
// Different completions for each argument
TabCompleter.register(plugin, "teleport", (sender, args) -> {
    if (args.length == 1) {
        // First argument: player names
        return PlayerGather.getOnlinePlayerNames();
    } else if (args.length == 2) {
        // Second argument: world names
        return Bukkit.getWorlds().stream()
            .map(World::getName)
            .collect(Collectors.toList());
    }
    return Collections.emptyList();
});
```

### Context-Aware Completions

```java
TabCompleter.register(plugin, "item", (sender, args) -> {
    if (!(sender instanceof Player)) {
        return Collections.emptyList();
    }
    
    Player player = (Player) sender;
    
    if (args.length == 1) {
        // Subcommands
        return Arrays.asList("give", "take", "clear", "list");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
        // Material names for "give" subcommand
        return Arrays.stream(Material.values())
            .map(Material::name)
            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
            .limit(20)
            .collect(Collectors.toList());
    } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
        // Amount suggestions
        return Arrays.asList("1", "16", "32", "64");
    }
    
    return Collections.emptyList();
});
```

### Advanced Filtering

```java
// Custom completer with filtering
TabCompleter.register(plugin, "warp", (sender, args) -> {
    if (args.length != 1) return Collections.emptyList();
    
    String partial = args[0].toLowerCase();
    List<String> warps = getAvailableWarps(sender);
    
    // Filter by partial input
    return warps.stream()
        .filter(warp -> warp.toLowerCase().startsWith(partial))
        .sorted()
        .collect(Collectors.toList());
});
```

### Complete Example - Admin Command

```java
public class AdminCommand {
    
    public void registerCommand() {
        // Register command
        CommandHelper.register(plugin, "admin", this::handleCommand);
        
        // Register tab completion
        TabCompleter.register(plugin, "admin", this::tabComplete);
    }
    
    private boolean handleCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /admin <subcommand>");
            return false;
        }
        
        switch (args[0].toLowerCase()) {
            case "teleport":
                return handleTeleport(sender, args);
            case "gamemode":
                return handleGamemode(sender, args);
            case "give":
                return handleGive(sender, args);
            case "ban":
                return handleBan(sender, args);
            default:
                sender.sendMessage("Unknown subcommand: " + args[0]);
                return false;
        }
    }
    
    private List<String> tabComplete(CommandSender sender, String[] args) {
        // First argument: subcommands
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                "teleport", "gamemode", "give", "ban", "kick", "reload"
            );
            return filterByInput(subcommands, args[0]);
        }
        
        // Second argument depends on subcommand
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "teleport":
                case "gamemode":
                case "give":
                case "ban":
                case "kick":
                    // Player names
                    return filterByInput(PlayerGather.getOnlinePlayerNames(), args[1]);
            }
        }
        
        // Third argument
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "gamemode":
                    // Gamemode names
                    return filterByInput(
                        Arrays.asList("survival", "creative", "adventure", "spectator"),
                        args[2]
                    );
                case "give":
                    // Material names (limited)
                    return Arrays.stream(Material.values())
                        .map(Material::name)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .limit(20)
                        .collect(Collectors.toList());
            }
        }
        
        // Fourth argument for give: amount
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "16", "32", "64");
        }
        
        return Collections.emptyList();
    }
    
    private List<String> filterByInput(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
            .filter(opt -> opt.toLowerCase().startsWith(lower))
            .collect(Collectors.toList());
    }
}
```

### Helper Methods

```java
// Create simple completer from array
TabCompleter.ArgsCompleter completer = TabCompleter.simple("option1", "option2", "option3");

// Use the completer
TabCompleter.register(plugin, "mycmd", completer);
```

### Multi-Level Completions

```java
public class EconomyCommand {
    
    public void registerCompletion() {
        TabCompleter.register(plugin, "economy", (sender, args) -> {
            // /economy <give|take|set|balance> <player> [amount]
            
            if (args.length == 1) {
                return filterMatches(
                    Arrays.asList("give", "take", "set", "balance", "top"),
                    args[0]
                );
            }
            
            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("give") || sub.equals("take") || 
                    sub.equals("set") || sub.equals("balance")) {
                    return filterMatches(PlayerGather.getOnlinePlayerNames(), args[1]);
                }
            }
            
            if (args.length == 3) {
                String sub = args[0].toLowerCase();
                if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
                    return Arrays.asList("100", "500", "1000", "5000", "10000");
                }
            }
            
            return Collections.emptyList();
        });
    }
    
    private List<String> filterMatches(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
            .filter(opt -> opt.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
}
```

---

## 27. TimeFormat

**Purpose**: Time formatting, parsing, duration display, and conversion utilities.

### Duration Formatting

```java
// Format milliseconds to readable duration
long millis = 125000; // 2 minutes 5 seconds
String formatted = TimeFormat.formatDuration(millis);
// Returns: "2 minutes 5 seconds"

// Short format
String shortFormat = TimeFormat.formatShort(millis);
// Returns: "2m 5s"

// Long format
String longFormat = TimeFormat.formatLong(millis);
// Returns: "2 minutes, 5 seconds"
```

### Parsing Durations

```java
// Parse text to milliseconds
long millis = TimeFormat.parseDuration("1h 30m");
// Returns: 5400000 (90 minutes in milliseconds)

// Parse to milliseconds
long ms = TimeFormat.parseToMillis("5d 12h");
// Returns: 475200000 (5.5 days in milliseconds)

// Supported units: s/sec/second, m/min/minute, h/hr/hour, d/day, w/week
```

### Relative Time

```java
// Get relative time string
long timestamp = System.currentTimeMillis() - 3600000; // 1 hour ago
String relative = TimeFormat.getRelativeTime(timestamp);
// Returns: "1 hour ago"

// Time until future timestamp
long future = System.currentTimeMillis() + 7200000; // 2 hours from now
String until = TimeFormat.getTimeUntil(future);
// Returns: "in 2 hours"

// Time ago
String ago = TimeFormat.getTimeAgo(timestamp);
// Returns: "1 hour ago"
```

### Date Formatting

```java
// Format date
long timestamp = System.currentTimeMillis();
String date = TimeFormat.formatDate(timestamp);
// Returns: "2024-01-15"

// Format date and time
String dateTime = TimeFormat.formatDateTime(timestamp);
// Returns: "2024-01-15 14:30:00"

// Parse date string
long parsed = TimeFormat.parseDate("2024-01-15");
```

### Tick Conversion

```java
// Convert Minecraft ticks to seconds
int ticks = 200; // 10 seconds
int seconds = TimeFormat.ticksToSeconds(ticks);
// Returns: 10

// Convert seconds to ticks
int tickCount = TimeFormat.secondsToTicks(30);
// Returns: 600

// Format ticks to time string
String timeStr = TimeFormat.ticksToTime(1200);
// Returns: "1 minute"
```

### Time Components

```java
long millis = 90061000; // 1 day, 1 hour, 1 minute, 1 second

// Get individual components
int days = TimeFormat.getDays(millis);       // 1
int hours = TimeFormat.getHours(millis);     // 1
int minutes = TimeFormat.getMinutes(millis); // 1
int seconds = TimeFormat.getSeconds(millis); // 1
```

### Comparisons

```java
// Check if timestamp expired
long expiry = System.currentTimeMillis() - 1000;
boolean expired = TimeFormat.isExpired(expiry);
// Returns: true

// Get remaining time
long futureTime = System.currentTimeMillis() + 3600000;
long remaining = TimeFormat.getRemainingTime(futureTime);
// Returns: 3600000 (milliseconds remaining)
```

### Unix Timestamp

```java
// Get current Unix timestamp (seconds)
long timestamp = TimeFormat.getCurrentTimestamp();

// Create timestamp from milliseconds
long unixTime = TimeFormat.fromTimestamp(System.currentTimeMillis());
```

### Complete Examples

```java
// Cooldown system with TimeFormat
Map<UUID, Long> cooldowns = new HashMap<>();

public void setCooldown(Player player, long duration) {
    long expiry = System.currentTimeMillis() + duration;
    cooldowns.put(player.getUniqueId(), expiry);
    
    String timeStr = TimeFormat.formatDuration(duration);
    player.sendMessage("§eCooldown active for " + timeStr);
}

public boolean hasCooldown(Player player) {
    Long expiry = cooldowns.get(player.getUniqueId());
    if (expiry == null) return false;
    
    if (TimeFormat.isExpired(expiry)) {
        cooldowns.remove(player.getUniqueId());
        return false;
    }
    
    long remaining = TimeFormat.getRemainingTime(expiry);
    String timeStr = TimeFormat.formatShort(remaining);
    player.sendMessage("§cCooldown active: " + timeStr + " remaining");
    return true;
}

// Task scheduler with readable times
public void scheduleTask(Plugin plugin, String taskName, String duration) {
    long millis = TimeFormat.parseToMillis(duration);
    int ticks = TimeFormat.secondsToTicks((int) (millis / 1000));
    
    SchedulerHelper.runLater(plugin, () -> {
        ConsoleLog.info("Task '" + taskName + "' executed!");
    }, ticks);
    
    ConsoleLog.info("Task '" + taskName + "' scheduled for " + TimeFormat.formatDuration(millis));
}

// Usage
scheduleTask(plugin, "Backup", "1h 30m");
scheduleTask(plugin, "Restart Warning", "5m");

// Player playtime tracker
public void showPlaytime(Player player, long joinTime) {
    long playtime = System.currentTimeMillis() - joinTime;
    
    String formatted = TimeFormat.formatLong(playtime);
    String relative = TimeFormat.getRelativeTime(joinTime);
    
    player.sendMessage("§aYou've been playing for: " + formatted);
    player.sendMessage("§aYou joined: " + relative);
}
```

---

## 28. UUIDhelp

**Purpose**: UUID/username lookup with caching and Mojang API integration.

### Basic UUID Operations

```java
// Get UUID from player name
UUID uuid = UUIDhelp.getUUID("Notch");
if (uuid != null) {
    ConsoleLog.info("UUID: " + uuid.toString());
}

// Get username from UUID
UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
String name = UUIDhelp.getUsername(uuid);
ConsoleLog.info("Username: " + name);
```

### Async Operations

```java
// Get UUID asynchronously (doesn't block main thread)
UUIDhelp.getUUIDAsync("Notch", uuid -> {
    if (uuid != null) {
        ConsoleLog.info("Found UUID: " + uuid);
        // Continue with UUID
        loadPlayerData(uuid);
    } else {
        ConsoleLog.warn("Player not found");
    }
});

// Get username asynchronously
UUID uuid = player.getUniqueId();
UUIDhelp.getUsernameAsync(uuid, name -> {
    if (name != null) {
        player.sendMessage("Your username: " + name);
    }
});
```

### UUID Validation

```java
// Check if string is valid UUID format
if (UUIDhelp.isValidUUID("069a79f4-44e9-4726-a5be-fca90e38aaf5")) {
    // Valid UUID format
}

// Parse UUID string (returns null if invalid)
UUID uuid = UUIDhelp.parseUUID("069a79f4-44e9-4726-a5be-fca90e38aaf5");

// Parse without dashes
UUID uuid = UUIDhelp.parseUUID("069a79f444e94726a5befca90e38aaf5");
```

### Offline UUID

```java
// Generate offline mode UUID (for offline servers)
UUID offlineUuid = UUIDhelp.getOfflineUUID("PlayerName");

// This UUID is deterministic for the same name
UUID uuid1 = UUIDhelp.getOfflineUUID("Steve");
UUID uuid2 = UUIDhelp.getOfflineUUID("Steve");
// uuid1.equals(uuid2) == true
```

### Mojang API

```java
// Fetch from Mojang API directly
UUID mojangUuid = UUIDhelp.fetchUUIDFromMojang("Notch");

// Fetch username from Mojang
String mojangName = UUIDhelp.fetchUsernameFromMojang(uuid);

// Check if Mojang API is available
if (UUIDhelp.isMojangAPIAvailable()) {
    // Can query Mojang
}
```

### Cache Management

```java
// Cache is automatic, but you can manage it

// Manually cache a UUID
UUIDhelp.cacheUUID("Notch", uuid);

// Manually cache a username
UUIDhelp.cacheUsername(uuid, "Notch");

// Clear entire cache
UUIDhelp.clearCache();

// Clear specific player from cache
UUIDhelp.clearPlayerCache("Notch");

// Get cache size
int size = UUIDhelp.getCacheSize();
ConsoleLog.info("Cache contains " + size + " entries");
```

### Bulk Operations

```java
// Get multiple UUIDs at once
List<String> names = Arrays.asList("Notch", "jeb_", "Dinnerbone");
Map<String, UUID> results = UUIDhelp.bulkGetUUIDs(names);

for (Map.Entry<String, UUID> entry : results.entrySet()) {
    ConsoleLog.info(entry.getKey() + " -> " + entry.getValue());
}

// Get multiple usernames at once
List<UUID> uuids = Arrays.asList(uuid1, uuid2, uuid3);
Map<UUID, String> usernames = UUIDhelp.bulkGetUsernames(uuids);
```

### Complete Example - Player Profile System

```java
public class PlayerProfileManager {
    
    public void loadProfile(String playerName, Consumer<PlayerProfile> callback) {
        // Get UUID asynchronously
        UUIDhelp.getUUIDAsync(playerName, uuid -> {
            if (uuid == null) {
                ConsoleLog.warn("Player not found: " + playerName);
                callback.accept(null);
                return;
            }
            
            // Load profile from database
            SchedulerHelper.runAsync(plugin, () -> {
                PlayerProfile profile = loadFromDatabase(uuid);
                
                // Update username in profile if changed
                String currentName = UUIDhelp.getUsername(uuid);
                if (currentName != null && !currentName.equals(profile.getName())) {
                    profile.setName(currentName);
                    updateDatabase(profile);
                }
                
                // Return on main thread
                SchedulerHelper.run(plugin, () -> {
                    callback.accept(profile);
                });
            });
        });
    }
    
    public void findPlayerByUUID(CommandSender sender, String uuidString) {
        // Validate UUID
        if (!UUIDhelp.isValidUUID(uuidString)) {
            sender.sendMessage(ColorConverter.colorize("&cInvalid UUID format"));
            return;
        }
        
        UUID uuid = UUIDhelp.parseUUID(uuidString);
        
        // Get username
        UUIDhelp.getUsernameAsync(uuid, name -> {
            if (name != null) {
                sender.sendMessage(ColorConverter.colorize(
                    "&aPlayer: &f" + name + "\n&aUUID: &f" + uuid
                ));
            } else {
                sender.sendMessage(ColorConverter.colorize("&cPlayer not found"));
            }
        });
    }
    
    public void migrateOfflineToOnline() {
        ConsoleLog.info("Starting UUID migration...");
        
        // Get all offline players
        List<String> playerNames = getAllStoredPlayerNames();
        
        int migrated = 0;
        for (String name : playerNames) {
            // Get offline UUID
            UUID offlineUuid = UUIDhelp.getOfflineUUID(name);
            
            // Get online UUID
            UUID onlineUuid = UUIDhelp.getUUID(name);
            
            if (onlineUuid != null && !onlineUuid.equals(offlineUuid)) {
                migratePlayerData(offlineUuid, onlineUuid);
                migrated++;
            }
        }
        
        ConsoleLog.info("Migrated " + migrated + " player profiles");
    }
    
    public void showPlayerHistory(Player viewer, String targetName) {
        UUIDhelp.getUUIDAsync(targetName, uuid -> {
            if (uuid == null) {
                viewer.sendMessage("Player not found");
                return;
            }
            
            // Get name history from Mojang
            List<String> nameHistory = UUIDhelp.getNameHistory(uuid);
            
            viewer.sendMessage(ColorConverter.colorize("&6Name History for " + targetName));
            viewer.sendMessage(ColorConverter.colorize("&7UUID: " + uuid));
            
            for (int i = 0; i < nameHistory.size(); i++) {
                String prefix = (i == nameHistory.size() - 1) ? "&a→ " : "  ";
                viewer.sendMessage(ColorConverter.colorize(prefix + nameHistory.get(i)));
            }
        });
    }
}
```

### UUID Formatting

```java
// Convert UUID to string with dashes
UUID uuid = player.getUniqueId();
String formatted = UUIDhelp.formatUUID(uuid);
// "069a79f4-44e9-4726-a5be-fca90e38aaf5"

// Convert to string without dashes
String compact = UUIDhelp.compactUUID(uuid);
// "069a79f444e94726a5befca90e38aaf5"

// Parse both formats
UUID parsed1 = UUIDhelp.parseUUID("069a79f4-44e9-4726-a5be-fca90e38aaf5");
UUID parsed2 = UUIDhelp.parseUUID("069a79f444e94726a5befca90e38aaf5");
// Both work correctly
```

---

## 29. VersionDetector

**Purpose**: Detect server platform and Minecraft version.

### Platform Detection

```java
// Detect server platform
VersionDetector.ServerPlatform platform = VersionDetector.detectPlatform();

switch (platform) {
    case PAPER:
        ConsoleLog.info("Running on Paper");
        break;
    case SPIGOT:
        ConsoleLog.info("Running on Spigot");
        break;
    case PURPUR:
        ConsoleLog.info("Running on Purpur");
        break;
    case PUFFERFISH:
        ConsoleLog.info("Running on Pufferfish");
        break;
    case LEAF:
        ConsoleLog.info("Running on Leaf");
        break;
    case CRAFTBUKKIT:
        ConsoleLog.info("Running on CraftBukkit");
        break;
    default:
        ConsoleLog.warn("Unknown platform");
}
```

### Platform Checks

```java
// Check specific platforms
if (VersionDetector.isPaper()) {
    // Use Paper-specific features
    enablePaperFeatures();
}

if (VersionDetector.isPurpur()) {
    // Use Purpur-specific features
    enablePurpurFeatures();
}

if (VersionDetector.isSpigot()) {
    // Spigot compatible
}

if (VersionDetector.isPufferfish()) {
    // Pufferfish optimizations
}

if (VersionDetector.isLeaf()) {
    // Leaf features
}
```

### Minecraft Version

```java
// Get Minecraft version
String mcVersion = VersionDetector.getMinecraftVersion();
ConsoleLog.info("Minecraft version: " + mcVersion); // e.g., "1.20.1"

// Parse version for comparison
String[] parts = mcVersion.split("\\.");
int major = Integer.parseInt(parts[0]); // 1
int minor = Integer.parseInt(parts[1]); // 20
int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0; // 1

// Check if version is 1.16+
if (minor >= 16) {
    // Hex colors supported
    enableHexColors();
}
```

### Complete Example - Feature Manager

```java
public class FeatureManager {
    
    private boolean hexColorsSupported = false;
    private boolean componentsSupported = false;
    private boolean persistentDataSupported = false;
    
    public void initialize() {
        detectFeatures();
        logServerInfo();
    }
    
    private void detectFeatures() {
        String version = VersionDetector.getMinecraftVersion();
        
        // Parse version
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            try {
                int minor = Integer.parseInt(parts[1]);
                
                // Hex colors: 1.16+
                hexColorsSupported = minor >= 16;
                
                // Modern components: 1.13+
                componentsSupported = minor >= 13;
                
                // PersistentDataContainer: 1.14+
                persistentDataSupported = minor >= 14;
                
            } catch (NumberFormatException e) {
                ConsoleLog.warn("Failed to parse version: " + version);
            }
        }
        
        // Check platform-specific features
        if (VersionDetector.isPaper()) {
            // Paper has additional features
            ConsoleLog.info("Paper detected - enabling Paper features");
        }
    }
    
    private void logServerInfo() {
        ConsoleLog.info("=== Server Information ===");
        ConsoleLog.info("Platform: " + VersionDetector.detectPlatform());
        ConsoleLog.info("Minecraft: " + VersionDetector.getMinecraftVersion());
        ConsoleLog.info("Hex Colors: " + (hexColorsSupported ? "Yes" : "No"));
        ConsoleLog.info("Components: " + (componentsSupported ? "Yes" : "No"));
        ConsoleLog.info("PDC: " + (persistentDataSupported ? "Yes" : "No"));
    }
    
    public String colorize(String text) {
        if (hexColorsSupported) {
            // Use hex color support
            return ColorConverter.colorize(text);
        } else {
            // Strip hex colors, use legacy only
            String noHex = text.replaceAll("&#[a-fA-F0-9]{6}", "");
            return ColorConverter.translateAmpersandToSection(noHex);
        }
    }
    
    public void sendMessage(Player player, String message) {
        if (componentsSupported && VersionDetector.isPaper()) {
            // Use Paper's modern component system
            sendComponentMessage(player, message);
        } else {
            // Use legacy chat
            player.sendMessage(colorize(message));
        }
    }
    
    public boolean canUsePersistentData() {
        return persistentDataSupported;
    }
}
```

### Platform-Specific Features

```java
public class PlatformFeatures {
    
    public void enablePlatformFeatures() {
        if (VersionDetector.isPaper()) {
            // Paper features
            enableAsyncChunkLoading();
            enableTimings();
        }
        
        if (VersionDetector.isPurpur()) {
            // Purpur has all Paper features plus more
            enablePurpurConfig();
            enablePurpurGameplay();
        }
        
        if (VersionDetector.isPufferfish()) {
            // Pufferfish optimizations
            ConsoleLog.info("Pufferfish optimizations active");
        }
    }
    
    public void checkCompatibility() {
        VersionDetector.ServerPlatform platform = VersionDetector.detectPlatform();
        
        switch (platform) {
            case PAPER:
            case PURPUR:
            case PUFFERFISH:
            case LEAF:
                ConsoleLog.info("✓ Full compatibility");
                break;
            case SPIGOT:
                ConsoleLog.warn("⚠ Limited features on Spigot");
                break;
            case CRAFTBUKKIT:
                ConsoleLog.error("✗ CraftBukkit not recommended");
                break;
            default:
                ConsoleLog.warn("? Unknown platform - may have issues");
        }
    }
}
```

---

## 30. VisualCreator

**Purpose**: Create visual effects including particles, fireworks, titles, action bars, and boss bars.

### Particles

```java
// Spawn particle at location
Location loc = player.getLocation();
VisualCreator.spawnParticle(loc, Particle.FLAME, 10);

// Spawn with offset (spread)
VisualCreator.spawnParticle(loc, Particle.HEART, 5, 0.5, 0.5, 0.5);

// Spawn with speed
VisualCreator.spawnParticle(loc, Particle.EXPLOSION_LARGE, 1, 0, 0, 0, 0.1);

// Using particle name (version-aware)
VisualCreator.spawnParticle(loc, "FLAME", 10);
```

### Particle Shapes

```java
// Spawn particles in circle
Location center = player.getLocation().add(0, 1, 0);
VisualCreator.spawnParticleCircle(center, Particle.FLAME, 2.0, 20);
// radius = 2.0, points = 20

// Spawn particles in line
Location start = player.getLocation();
Location end = player.getLocation().add(10, 0, 0);
VisualCreator.spawnParticleLine(start, end, Particle.REDSTONE, 0.5);
// spacing = 0.5 blocks

// Spawn particles in sphere
VisualCreator.spawnParticleSphere(center, Particle.ENCHANTMENT_TABLE, 2.0, 50);

// Spawn particles in helix
VisualCreator.spawnParticleHelix(center, Particle.PORTAL, 2.0, 3.0, 50);
// radius = 2.0, height = 3.0, points = 50
```

### Fireworks

```java
// Create firework effect
FireworkEffect effect = FireworkEffect.builder()
    .with(FireworkEffect.Type.BALL_LARGE)
    .withColor(Color.RED, Color.YELLOW)
    .withFade(Color.ORANGE)
    .withFlicker()
    .withTrail()
    .build();

// Spawn firework
VisualCreator.spawnFirework(player.getLocation(), effect);

// Spawn instant firework (explodes immediately)
VisualCreator.spawnInstantFirework(player.getLocation(), effect);

// Predefined firework effects
VisualCreator.spawnRandomFirework(player.getLocation());
VisualCreator.spawnColorfulFirework(player.getLocation(), Color.BLUE, Color.WHITE);
```

### Titles

```java
// Send title to player
VisualCreator.sendTitle(player, "&6&lWelcome!", "&eEnjoy your stay");

// With timing (fadeIn, stay, fadeOut in ticks)
VisualCreator.sendTitle(player, "&a&lVICTORY!", "&7You won the game", 10, 70, 20);

// Title only
VisualCreator.sendTitle(player, "&c&lWARNING!");

// Subtitle only
VisualCreator.sendSubtitle(player, "&7This is a subtitle");

// Clear title
VisualCreator.clearTitle(player);
```

### Action Bar

```java
// Send action bar message
VisualCreator.sendActionBar(player, "&aHealth: &c❤ &f20/20");

// Update action bar repeatedly
BukkitTask task = SchedulerHelper.runTimerSeconds(plugin, () -> {
    double health = player.getHealth();
    double maxHealth = player.getMaxHealth();
    String bar = createHealthBar(health, maxHealth);
    VisualCreator.sendActionBar(player, bar);
}, 0, 0.5); // Update every 0.5 seconds
```

### Boss Bars

```java
// Create boss bar
BossBar bar = VisualCreator.createBossBar(
    "&c&lBoss Fight", 
    BarColor.RED, 
    BarStyle.SEGMENTED_10
);

// Add player to see boss bar
bar.addPlayer(player);

// Update boss bar
bar.setProgress(0.75); // 75%
bar.setTitle(ColorConverter.colorize("&c&lBoss: 75% HP"));

// Remove boss bar
bar.removeAll();

// Boss bar with automatic management
VisualCreator.showBossBar(player, "bossbar-key", 
    "&c&lBoss Fight", BarColor.RED, BarStyle.SOLID, 1.0);

// Update managed boss bar
VisualCreator.updateBossBar(player, "bossbar-key", 0.5); // 50%

// Remove managed boss bar
VisualCreator.removeBossBar(player, "bossbar-key");
```

### Color Utilities

```java
// Parse color from hex
Color color = VisualCreator.fromHex("#FF5733");

// Create RGB color
Color custom = VisualCreator.fromRGB(255, 87, 51);

// Predefined colors
Color red = VisualCreator.RED;
Color green = VisualCreator.GREEN;
Color blue = VisualCreator.BLUE;
```

### Complete Example - Effect Manager

```java
public class EffectManager {
    
    public void playLevelUpEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Particle circle
        VisualCreator.spawnParticleCircle(loc, Particle.VILLAGER_HAPPY, 1.5, 30);
        
        // Sound
        SoundPlayer.play(player, "ENTITY_PLAYER_LEVELUP");
        
        // Title
        VisualCreator.sendTitle(player, "&6&lLEVEL UP!", "&eYou reached level " + player.getLevel());
        
        // Firework
        FireworkEffect effect = FireworkEffect.builder()
            .with(FireworkEffect.Type.STAR)
            .withColor(Color.YELLOW, Color.ORANGE)
            .withFlicker()
            .build();
        VisualCreator.spawnInstantFirework(loc, effect);
    }
    
    public void playTeleportEffect(Location from, Location to) {
        // Departure effect
        VisualCreator.spawnParticleHelix(from.clone().add(0, 0.5, 0), 
            Particle.PORTAL, 1.0, 2.0, 40);
        SoundPlayer.playAt(from, "ENTITY_ENDERMAN_TELEPORT");
        
        // Arrival effect (delayed)
        SchedulerHelper.runLater(plugin, () -> {
            VisualCreator.spawnParticleSphere(to.clone().add(0, 1, 0),
                Particle.PORTAL, 1.5, 30);
            SoundPlayer.playAt(to, "ENTITY_ENDERMAN_TELEPORT", 1.0f, 1.2f);
        }, 10L);
    }
    
    public void showBossHealthBar(Player player, String bossName, double health, double maxHealth) {
        double progress = Mathematics.clamp(health / maxHealth, 0.0, 1.0);
        
        // Color based on health
        BarColor color;
        if (progress > 0.66) {
            color = BarColor.GREEN;
        } else if (progress > 0.33) {
            color = BarColor.YELLOW;
        } else {
            color = BarColor.RED;
        }
        
        String title = ColorConverter.colorize(
            "&c&l" + bossName + " &7- &f" + (int)health + "/" + (int)maxHealth
        );
        
        VisualCreator.showBossBar(player, "boss", title, color, BarStyle.SOLID, progress);
    }
    
    public void playWinEffect(Player winner) {
        Location loc = winner.getLocation().add(0, 2, 0);
        
        // Multiple fireworks
        for (int i = 0; i < 5; i++) {
            SchedulerHelper.runLater(plugin, () -> {
                FireworkEffect effect = FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(
                        Color.RED, Color.BLUE, Color.GREEN, 
                        Color.YELLOW, Color.PURPLE
                    )
                    .withFlicker()
                    .withTrail()
                    .build();
                VisualCreator.spawnFirework(loc, effect);
            }, i * 10L);
        }
        
        // Title
        VisualCreator.sendTitle(winner, "&6&lVICTORY!", "&e&lYou won the game!", 10, 80, 20);
        
        // Particle effect
        SchedulerHelper.runTimer(plugin, new BukkitRunnable() {
            private int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 60) {
                    cancel();
                    return;
                }
                Location particleLoc = winner.getLocation().add(0, 2, 0);
                VisualCreator.spawnParticleCircle(particleLoc, Particle.FIREWORKS_SPARK, 1.5, 20);
            }
        }, 0L, 2L);
    }
}
```

---

## 31. WorldEditor

**Purpose**: Simplify block placement, area filling, world management, and coordinate tracking.

### Basic Block Placement

```java
// Set block at location
Location loc = new Location(world, 100, 64, 200);
WorldEditor.setBlock(loc, Material.STONE);

// Set block with material name (version-aware)
WorldEditor.setBlock(loc, "STONE");

// Set block at coordinates
WorldEditor.setBlock(world, 100, 64, 200, Material.DIAMOND_BLOCK);
WorldEditor.setBlock(world, 100, 64, 200, "DIAMOND_BLOCK");
```

### Area Filling

```java
// Fill rectangular area (coordinates are normalized automatically)
int count = WorldEditor.fillArea(world, 
    0, 64, 0,    // corner 1
    10, 70, 10,  // corner 2
    Material.GLASS
);
ConsoleLog.info("Placed " + count + " blocks");

// Fill with material name
WorldEditor.fillArea(world, x1, y1, z1, x2, y2, z2, "STONE");

// Fill between two locations
Location pos1 = player.getLocation();
Location pos2 = player.getTargetBlock(null, 100).getLocation();
WorldEditor.fillArea(pos1, pos2, Material.STONE);
```

### Hollow Structures

```java
// Create hollow box (only walls, no interior)
WorldEditor.fillHollowBox(world, 
    0, 64, 0,
    10, 70, 10,
    Material.GLASS
);

// Create room with hollow box
WorldEditor.fillHollowBox(world, x1, y1, z1, x2, y2, z2, "STONE_BRICKS");
```

### Replacing Blocks

```java
// Replace specific material in area
int replaced = WorldEditor.replace(world, 
    x1, y1, z1, x2, y2, z2,
    Material.STONE,     // Replace this
    Material.DIAMOND_ORE // With this
);

// Replace multiple materials
WorldEditor.replaceMultiple(world, x1, y1, z1, x2, y2, z2,
    Arrays.asList(Material.DIRT, Material.GRASS_BLOCK),
    Material.STONE
);
```

### World Management

```java
// Get all worlds
List<World> worlds = WorldEditor.getAllWorlds();
for (World w : worlds) {
    ConsoleLog.info("World: " + w.getName());
}

// Get world by name
World world = WorldEditor.getWorld("world_nether");

// Get main world
World mainWorld = WorldEditor.getMainWorld();

// Create new world
World newWorld = WorldEditor.createWorld("custom_world", World.Environment.NORMAL);
```

### Player Locations

```java
// Get all player locations
Map<Player, Location> locations = WorldEditor.getAllPlayerLocations();
for (Map.Entry<Player, Location> entry : locations.entrySet()) {
    Player p = entry.getKey();
    Location loc = entry.getValue();
    ConsoleLog.info(p.getName() + " at " + loc.getBlockX() + "," + loc.getBlockZ());
}

// Get players in specific world
List<Player> playersInWorld = WorldEditor.getPlayersInWorld(world);

// Get players in radius
List<Player> nearby = WorldEditor.getPlayersInRadius(location, 50.0);
```

### Block Information

```java
// Get block at location
Block block = WorldEditor.getBlockAt(world, x, y, z);

// Get block type
Material type = WorldEditor.getBlockType(location);

// Check if block is air
if (WorldEditor.isAir(location)) {
    // Location is empty
}

// Check if block is solid
if (WorldEditor.isSolid(location)) {
    // Block is solid
}

// Get highest block at coordinates
Location highest = WorldEditor.getHighestBlock(world, x, z);
```

### Distance & Region Checks

```java
// Calculate distance between locations
double distance = WorldEditor.distance(loc1, loc2);

// Calculate 2D distance (ignoring Y)
double distance2D = WorldEditor.distance2D(loc1, loc2);

// Check if location is in region
boolean inRegion = WorldEditor.isInRegion(location, corner1, corner2);

// Check if location is in sphere
boolean inSphere = WorldEditor.isInSphere(location, center, radius);

// Get all blocks in radius
List<Block> blocks = WorldEditor.getBlocksInRadius(center, 5.0);
```

### Complete Example - Build Tools

```java
public class BuildTools {
    
    public void createPlatform(Player player, int size) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY() - 1;
        int z = center.getBlockZ();
        
        // Create platform
        int placed = WorldEditor.fillArea(world,
            x - size, y, z - size,
            x + size, y, z + size,
            Material.STONE
        );
        
        player.sendMessage(ColorConverter.colorize(
            "&aCreated platform with " + placed + " blocks"
        ));
    }
    
    public void createHouse(Location corner) {
        World world = corner.getWorld();
        int x = corner.getBlockX();
        int y = corner.getBlockY();
        int z = corner.getBlockZ();
        
        // Floor
        WorldEditor.fillArea(world, x, y, z, x+10, y, z+10, "STONE");
        
        // Walls (hollow)
        WorldEditor.fillHollowBox(world, x, y+1, z, x+10, y+5, z+10, "STONE_BRICKS");
        
        // Roof
        WorldEditor.fillArea(world, x, y+6, z, x+10, y+6, z+10, "OAK_PLANKS");
        
        // Door
        WorldEditor.setBlock(world, x+5, y+1, z, Material.AIR);
        WorldEditor.setBlock(world, x+5, y+2, z, Material.AIR);
        
        ConsoleLog.info("House created at " + x + "," + y + "," + z);
    }
    
    public void clearArea(CommandSender sender, Location pos1, Location pos2) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            sender.sendMessage("Locations must be in same world!");
            return;
        }
        
        sender.sendMessage("Clearing area...");
        
        // Clear async to avoid lag
        SchedulerHelper.runAsync(plugin, () -> {
            int count = WorldEditor.fillArea(pos1, pos2, Material.AIR);
            
            // Send result on main thread
            SchedulerHelper.run(plugin, () -> {
                sender.sendMessage(ColorConverter.colorize(
                    "&aCleared " + count + " blocks"
                ));
            });
        });
    }
    
    public void copyRegion(Location from1, Location from2, Location to) {
        World fromWorld = from1.getWorld();
        World toWorld = to.getWorld();
        
        // Calculate dimensions
        int minX = Math.min(from1.getBlockX(), from2.getBlockX());
        int maxX = Math.max(from1.getBlockX(), from2.getBlockX());
        int minY = Math.min(from1.getBlockY(), from2.getBlockY());
        int maxY = Math.max(from1.getBlockY(), from2.getBlockY());
        int minZ = Math.min(from1.getBlockZ(), from2.getBlockZ());
        int maxZ = Math.max(from1.getBlockZ(), from2.getBlockZ());
        
        int offsetX = to.getBlockX() - minX;
        int offsetY = to.getBlockY() - minY;
        int offsetZ = to.getBlockZ() - minZ;
        
        // Copy blocks
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material type = WorldEditor.getBlockType(
                        new Location(fromWorld, x, y, z)
                    );
                    WorldEditor.setBlock(toWorld, 
                        x + offsetX, y + offsetY, z + offsetZ, 
                        type
                    );
                }
            }
        }
    }
    
    public void createCircle(Location center, int radius, Material material) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    WorldEditor.setBlock(world, 
                        centerX + x, centerY, centerZ + z, 
                        material
                    );
                }
            }
        }
    }
    
    public void createSphere(Location center, int radius, Material material) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z <= radius*radius) {
                        WorldEditor.setBlock(world, 
                            centerX + x, centerY + y, centerZ + z, 
                            material
                        );
                    }
                }
            }
        }
    }
    
    public void replaceInRegion(Player player, Location pos1, Location pos2, 
                                Material find, Material replace) {
        player.sendMessage("Replacing blocks...");
        
        SchedulerHelper.runAsync(plugin, () -> {
            int replaced = WorldEditor.replace(pos1.getWorld(),
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                find, replace
            );
            
            SchedulerHelper.run(plugin, () -> {
                player.sendMessage(ColorConverter.colorize(
                    "&aReplaced " + replaced + " blocks"
                ));
            });
        });
    }
}
```

### World Utilities

```java
// Set world spawn
WorldEditor.setWorldSpawn(world, new Location(world, 0, 100, 0));

// Get world spawn
Location spawn = WorldEditor.getWorldSpawn(world);

// Set world time
WorldEditor.setTime(world, 0); // Day
WorldEditor.setTime(world, 13000); // Night

// Set weather
WorldEditor.clearWeather(world);
WorldEditor.setStorm(world, true);
WorldEditor.setThundering(world, true);

// World border
WorldEditor.setWorldBorder(world, 1000.0); // Radius in blocks
WorldEditor.setWorldBorderCenter(world, 0, 0);
```

### Region Tools

```java
// Count blocks in region
int count = WorldEditor.countBlocks(world, x1, y1, z1, x2, y2, z2, Material.DIAMOND_ORE);

// Get block distribution in region
Map<Material, Integer> distribution = WorldEditor.getBlockDistribution(
    world, x1, y1, z1, x2, y2, z2
);

for (Map.Entry<Material, Integer> entry : distribution.entrySet()) {
    ConsoleLog.info(entry.getKey() + ": " + entry.getValue());
}

// Check if region is empty
if (WorldEditor.isRegionEmpty(world, x1, y1, z1, x2, y2, z2)) {
    // Region contains only air
}
```

---

## 32. WorldLocations

**Purpose**: Location utilities for distance calculations, safety checks, and spatial operations.

### Block Centering

```java
// Center location in block
Location loc = player.getLocation();
Location centered = WorldLocations.getCenter(loc);
// Sets X and Z to block center (x.5, z.5)

// Center with yaw/pitch preserved
Location centeredYaw = WorldLocations.getCenterWithYaw(loc);
```

### Safety Checks

```java
// Check if location is safe (solid block below, 2 air blocks above)
Location loc = new Location(world, 100, 64, 200);
boolean safe = WorldLocations.isSafe(loc);

// Get nearest safe location
Location safeLoc = WorldLocations.getSafeLocation(loc);

// Get highest safe block at coordinates
Location highest = WorldLocations.getHighestSafeBlock(world, 100, 200);
```

### Block Queries

```java
// Get highest block (non-air)
Location highestBlock = WorldLocations.getHighestBlock(loc);

// Get blocks in radius
List<Block> blocks = WorldLocations.getBlocksInRadius(loc, 5.0);

// Get nearby blocks of specific type
List<Block> diamonds = WorldLocations.getNearbyBlocks(loc, Material.DIAMOND_ORE, 10.0);
```

### Player Queries

```java
// Get nearby players
List<Player> nearbyPlayers = WorldLocations.getNearbyPlayers(loc, 20.0);

// Get players in radius (includes distance check)
List<Player> players = WorldLocations.getPlayersInRadius(loc, 15.0);
```

### Distance Calculations

```java
Location loc1 = new Location(world, 0, 64, 0);
Location loc2 = new Location(world, 10, 64, 10);

// Get distance
double distance = WorldLocations.getDistance(loc1, loc2);

// Get distance squared (faster, no sqrt)
double distSq = WorldLocations.getDistanceSquared(loc1, loc2);

// Check if within distance
boolean isNear = WorldLocations.isWithinDistance(loc1, loc2, 20.0);
```

### Cuboid Operations

```java
Location corner1 = new Location(world, 0, 0, 0);
Location corner2 = new Location(world, 10, 10, 10);

// Check if location is in cuboid
boolean inside = WorldLocations.isInCuboid(loc, corner1, corner2);

// Get all blocks in cuboid
List<Block> cuboidBlocks = WorldLocations.getCuboidBlocks(corner1, corner2);
```

### Random Locations

```java
// Random location in radius
Location randomLoc = WorldLocations.randomInRadius(center, 10.0);

// Random location in cuboid
Location randomCuboid = WorldLocations.randomInCuboid(corner1, corner2);
```

### Direction Utilities

```java
// Get block player is facing
Location facing = WorldLocations.getFacing(player.getLocation());

// Get relative location
Location relative = WorldLocations.getRelative(loc, BlockFace.NORTH, 5);

// Directional offsets
Location forward = WorldLocations.getForward(loc, 3);
Location backward = WorldLocations.getBackward(loc, 3);
Location left = WorldLocations.getLeft(loc, 2);
Location right = WorldLocations.getRight(loc, 2);
```

### Circle Generation

```java
// Get circle of locations (filled)
List<Location> circle = WorldLocations.getCircleLocations(center, 5.0);

// Get hollow circle (outline only)
List<Location> hollowCircle = WorldLocations.getHollowCircle(center, 5.0);
```

### Sphere Generation

```java
// Get sphere of locations (filled)
List<Location> sphere = WorldLocations.getSphereLocations(center, 5.0);

// Get hollow sphere (shell only)
List<Location> hollowSphere = WorldLocations.getHollowSphere(center, 5.0);
```

### Complete Examples

```java
// Safe teleportation system
public boolean safeTeleport(Player player, Location destination) {
    Location safeLoc = WorldLocations.getSafeLocation(destination);
    
    if (safeLoc == null) {
        player.sendMessage("§cNo safe location found!");
        return false;
    }
    
    Location centered = WorldLocations.getCenterWithYaw(safeLoc);
    player.teleport(centered);
    player.sendMessage("§aTeleported safely!");
    return true;
}

// Area effect spell
public void castAreaSpell(Player caster, double radius) {
    Location center = caster.getLocation();
    
    // Get all players in radius
    List<Player> targets = WorldLocations.getPlayersInRadius(center, radius);
    
    // Visual effect - create particle circle
    List<Location> circle = WorldLocations.getHollowCircle(center, radius);
    for (Location loc : circle) {
        VisualCreator.spawnParticle(loc, "FLAME", 1);
    }
    
    // Apply effect to targets
    for (Player target : targets) {
        if (target.equals(caster)) continue;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
        target.sendMessage("§cYou've been affected by a spell!");
    }
}

// Random spawn system
public Location findRandomSpawn(World world, Location center, double radius) {
    for (int i = 0; i < 10; i++) { // Try 10 times
        Location random = WorldLocations.randomInRadius(center, radius);
        Location highest = WorldLocations.getHighestBlock(random);
        
        if (WorldLocations.isSafe(highest)) {
            return WorldLocations.getCenterWithYaw(highest);
        }
    }
    return null; // No safe location found
}

// Mining laser (removes blocks in line)
public void fireLaser(Player player, int distance) {
    Location start = player.getEyeLocation();
    Location current = start.clone();
    
    for (int i = 0; i < distance; i++) {
        current = WorldLocations.getForward(current, 1);
        Block block = current.getBlock();
        
        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR);
            VisualCreator.spawnParticle(current, "EXPLOSION_NORMAL", 5);
        }
    }
}

// Sphere builder
public void createSphere(Location center, Material material, double radius, boolean hollow) {
    List<Location> locations;
    
    if (hollow) {
        locations = WorldLocations.getHollowSphere(center, radius);
    } else {
        locations = WorldLocations.getSphereLocations(center, radius);
    }
    
    for (Location loc : locations) {
        WorldEditor.setBlock(loc, material);
    }
    
    String type = hollow ? "hollow" : "filled";
    ConsoleLog.info("Created " + type + " sphere with " + locations.size() + " blocks");
}
```

---